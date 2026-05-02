package net.oxcodsnet.roadarchitect.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.config.records.CacheSettings;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;
import net.oxcodsnet.roadarchitect.util.cache.ChunkHeightGenerator;
import net.oxcodsnet.roadarchitect.util.cache.ChunkHeightSnapshot;
import net.oxcodsnet.roadarchitect.util.cache.WorldCacheState;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.OptionalInt;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Centralized, thread-safe caches for world generation data.
 * Reused across multiple PathFinder instances to avoid cache warm-up on each search.
 */
public final class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + CacheManager.class.getSimpleName());

    private static final Map<ResourceKey<Level>, WorldCacheState> STATES = new ConcurrentHashMap<>();
    private static final int CHUNK_SIDE = 16;
    private static final int COLUMNS_PER_CHUNK = CHUNK_SIDE * CHUNK_SIDE;

    private CacheManager() {
        // no-op
    }

    /**
     * Called by platform hooks when a server world loads (server side).
     * Ensures the cache state is allocated and attached to the world.
     *
     * @param world server world
     */
    public static void onWorldLoad(ServerLevel world) {
        if (world.isClientSide()) return;
        load(world);
    }

    /**
     * Called by platform hooks when a server world unloads.
     * Flushes and detaches the cache state from the internal map.
     *
     * @param world server world
     */
    public static void onWorldUnload(ServerLevel world) {
        if (world.isClientSide()) return;
        save(world);
    }

    /**
     * Called by platform hooks when the server is stopping.
     * Flushes all cached states for all worlds.
     *
     * @param server minecraft server
     */
    public static void onServerStopping(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            save(world);
        }
    }

    private static WorldCacheState state(ServerLevel world) {
        return STATES.computeIfAbsent(world.dimension(), k -> {
            CacheStorage storage = CacheStorage.open(world);
            return new WorldCacheState(world, storage, storage.settings());
        });
    }

    private static void load(ServerLevel world) {
        CacheStorage storage = CacheStorage.open(world);
        CacheSettings settings = storage.settings();
        STATES.put(world.dimension(), new WorldCacheState(world, storage, settings));
        DebugLog.info(LOGGER, "Cache loaded for world {}", world.dimension().location());
        DebugLog.cache(LOGGER, "Cache budgets for {} -> runtime={}MiB snapshot={}MiB persisted={}MiB",
                world.dimension().location(),
                settings.runtimeBudgetMb(),
                settings.snapshotBudgetMb(),
                settings.persistedBudgetMb());
    }

    private static void save(ServerLevel world) {
        WorldCacheState state = STATES.remove(world.dimension());
        if (state != null) {
            state.flush();
            DebugLog.info(LOGGER, "Cache saved for world {}", world.dimension().location());
            DebugLog.cache(LOGGER, "Cache flushed for world {}", world.dimension().location());
        }
    }

    /**
     * Prefills height and biome caches asynchronously over the given XZ area.
     *
     * @param world server world
     * @param minX  min X (blocks)
     * @param minZ  min Z (blocks)
     * @param maxX  max X (blocks)
     * @param maxZ  max Z (blocks)
     */
    public static void prefill(ServerLevel world, int minX, int minZ, int maxX, int maxZ) {
        WorldCacheState state = state(world);
        CacheStorage storage = state.storage();
        CacheSettings cacheSettings = storage.settings();
        if (!cacheSettings.enablePrefill()) {
            DebugLog.info(LOGGER, "Skipping prefill for {} because it is disabled via config", world.dimension().location());
            DebugLog.cache(LOGGER, "Prefill skipped for {}: disabled", world.dimension().location());
            return;
        }
        long runtimeBudget = cacheSettings.runtimeBudgetBytes();
        long current = storage.runtimeWeightBytes();
        if (current >= runtimeBudget * 0.9) {
            DebugLog.info(LOGGER, "Skipping prefill for {} because runtime cache is at {} of {}", world.dimension().location(), current, runtimeBudget);
            DebugLog.cache(LOGGER, "Prefill skipped for {}: runtime usage {} of {}", world.dimension().location(), current, runtimeBudget);
            return;
        }

        int minChunkX = Math.floorDiv(minX, CHUNK_SIDE);
        int maxChunkX = Math.floorDiv(maxX, CHUNK_SIDE);
        int minChunkZ = Math.floorDiv(minZ, CHUNK_SIDE);
        int maxChunkZ = Math.floorDiv(maxZ, CHUNK_SIDE);
        if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
            return;
        }
        int totalChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        int limit = cacheSettings.clampedPrefillMaxChunks();
        if (limit <= 0 || totalChunks <= 0) {
            DebugLog.info(LOGGER, "Prefill skipped: no chunks selected or limit is zero");
            DebugLog.cache(LOGGER, "Prefill skipped for {}: no eligible chunks (limit={})", world.dimension().location(), limit);
            return;
        }
        int target = Math.min(totalChunks, limit);
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        RandomState randomState = world.getChunkSource().randomState();
        Climate.Sampler sampler = randomState.sampler();
        BiomeSource biomeSource = generator.getBiomeSource();
        int stride = Math.max(1, PathFinder.GRID_STEP);

        int scheduled = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX && scheduled < target; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ && scheduled < target; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                scheduled++;
                AsyncExecutor.execute(() -> {
                    warmChunk(world, state, storage, pos);
                    prefillBiomes(world, storage, biomeSource, sampler, pos, stride);
                });
            }
        }
        DebugLog.info(LOGGER, "Scheduled {} chunk prefill tasks within [{}..{}]×[{}..{}]",
                scheduled, minX, maxX, minZ, maxZ);
        DebugLog.cache(LOGGER, "Prefill scheduled {} chunks for {} (limit={}, runtime={}MiB)",
                scheduled, world.dimension().location(), limit, cacheSettings.runtimeBudgetMb());
    }

    private static void warmChunk(ServerLevel world, WorldCacheState state, CacheStorage storage, ChunkPos pos) {
        long key = hash(pos.getMinBlockX(), pos.getMinBlockZ());
        ensureChunkSnapshot(world, state, storage, key);
    }

    private static void prefillBiomes(ServerLevel world,
                                      CacheStorage storage,
                                      BiomeSource biomeSource,
                                      Climate.Sampler sampler,
                                      ChunkPos pos,
                                      int stride) {
        int baseX = pos.getMinBlockX();
        int baseZ = pos.getMinBlockZ();
        for (int localX = 0; localX < CHUNK_SIDE; localX += stride) {
            for (int localZ = 0; localZ < CHUNK_SIDE; localZ += stride) {
                int worldX = baseX + localX;
                int worldZ = baseZ + localZ;
                long key = hash(worldX, worldZ);
                if (storage.getBiome(key) != null) {
                    continue;
                }
                Holder<Biome> biome = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(worldX), 316,
                        QuartPos.fromBlock(worldZ), sampler);
                storage.putBiome(key, biome);
            }
        }
    }

    /**
     * Gets or computes world surface height for the cached key.
     *
     * @param world  server world
     * @param key    cache key (hash of x,z)
     * @param loader fallback loader if value missing
     */
    public static int getHeight(ServerLevel world, long key, IntSupplier loader) {
        PipelineProfiler.increment("cache.height.requests");
        WorldCacheState state = state(world);
        CacheStorage storage = state.storage();
        Integer cached = storage.getHeight(key);
        if (cached != null) {
            PipelineProfiler.increment("cache.height.hits");
            return cached;
        }
        Integer chunkCached = state.lookupHeight(key, CHUNK_SIDE);
        if (chunkCached != null) {
            storage.putHeightIfAbsent(key, chunkCached);
            PipelineProfiler.increment("cache.height.hits");
            return chunkCached;
        }
        ChunkHeightSnapshot snapshot = ensureChunkSnapshot(world, state, storage, key);
        if (snapshot != null) {
            int x = (int) (key >> 32);
            int z = (int) key;
            int localX = x & (CHUNK_SIDE - 1);
            int localZ = z & (CHUNK_SIDE - 1);
            int value = snapshot.get(localX, localZ);
            storage.putHeightIfAbsent(key, value);
            PipelineProfiler.increment("cache.height.hits");
            return value;
        }
        // Fast path for unloaded chunks: avoid generating full chunk snapshot.
        // Compute just this column via generator and cache it.
        return storage.computeHeightIfAbsent(key, () -> {
            PipelineProfiler.increment("cache.height.loads");
            try (PipelineProfiler.Section section = PipelineProfiler.openSection("cache.height.load_time")) {
                int value = loader.getAsInt();
                PipelineProfiler.recordValue("cache.height.loaded_value", value);
                return value;
            }
        });
    }

    /**
     * Gets or computes world surface height at block coordinates.
     */
    public static int getHeight(ServerLevel world, int x, int z) {
        long key = hash(x, z);
        OptionalInt prepared = RoadFeature.lookupPreparedSurface(x, z);
        if (prepared.isPresent()) {
            int value = prepared.getAsInt();
            WorldCacheState state = state(world);
            state.storage().putHeightIfAbsent(key, value);
            return value;
        }
        return getHeight(world, key, () -> {
            ChunkGenerator gen = world.getChunkSource().getGenerator();
            RandomState cfg = world.getChunkSource().randomState();
            return gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, world, cfg);
        });
    }

    /**
     * Gets or computes terrain stability metric for the key.
     */
    public static double getStability(ServerLevel world, long key, DoubleSupplier loader) {
        PipelineProfiler.increment("cache.stability.requests");
        WorldCacheState state = state(world);
        CacheStorage storage = state.storage();
        Double cached = storage.getStability(key);
        if (cached != null) {
            PipelineProfiler.increment("cache.stability.hits");
            return cached;
        }
        return storage.computeStabilityIfAbsent(key, () -> {
            PipelineProfiler.increment("cache.stability.loads");
            try (PipelineProfiler.Section section = PipelineProfiler.openSection("cache.stability.load_time")) {
                double value = loader.getAsDouble();
                PipelineProfiler.recordValue("cache.stability.loaded_value", value);
                return value;
            }
        });
    }

    /**
     * Gets or computes biome entry for the key.
     */
    public static Holder<Biome> getBiome(ServerLevel world, long key, Supplier<Holder<Biome>> loader) {
        PipelineProfiler.increment("cache.biome.requests");
        WorldCacheState state = state(world);
        CacheStorage storage = state.storage();
        Holder<Biome> cached = storage.getBiome(key);
        if (cached != null) {
            PipelineProfiler.increment("cache.biome.hits");
            return cached;
        }
        return storage.computeBiomeIfAbsent(key, () -> {
            PipelineProfiler.increment("cache.biome.loads");
            try (PipelineProfiler.Section section = PipelineProfiler.openSection("cache.biome.load_time")) {
                Holder<Biome> value = loader.get();
                return value;
            }
        });
    }

    /**
     * Packs X and Z into a single long key.
     */
    public static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    /**
     * Unpacks X and Z from a long key into {@link BlockPos} (Y=0).
     */
    public static BlockPos keyToPos(long k) {
        return new BlockPos((int) (k >> 32), 0, (int) k);
    }

    /**
     * Populates the runtime cache with the latest heightmap snapshot for a chunk when it becomes available.
     */
    public static void onChunkLoad(ServerLevel world, ChunkAccess chunk) {
        if (world.isClientSide()) {
            return;
        }
        WorldCacheState state = STATES.get(world.dimension());
        if (state == null) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        int startX = pos.getMinBlockX();
        int startZ = pos.getMinBlockZ();
        int minY = state.minWorldY();

        Heightmap wg = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
        if (wg == null && surface == null) {
            return;
        }
        if (wg == null) {
            wg = surface;
        }
        if (surface == null) {
            surface = wg;
        }

        // Read the heightmap synchronously while the chunk is guaranteed
        // present on the server thread; the resulting int[] is a self-contained
        // copy and is safe to consume from a worker.
        int[] snapshot = new int[COLUMNS_PER_CHUNK];
        for (int localZ = 0; localZ < CHUNK_SIDE; localZ++) {
            for (int localX = 0; localX < CHUNK_SIDE; localX++) {
                int idx = localZ * CHUNK_SIDE + localX;
                int height = wg.getFirstAvailable(localX, localZ);
                if (height <= minY) {
                    height = surface.getFirstAvailable(localX, localZ);
                }
                snapshot[idx] = height;
            }
        }

        // Defer the 256 column writes and the snapshot publication to the
        // async pool. Each storage.putHeight() goes through CacheStorage.mutate
        // → RegionColumnStore.write → regions.get(loadRegion), which on a
        // cold cache miss synchronously GZIP-decodes a ~tens-of-MiB region
        // file. Doing that 256× per chunk on the server thread, multiplied
        // by 200+ chunk-loads/second on world reload, was the cause of the
        // multi-second tick freezes. Caffeine's runtime + region caches and
        // the snapshot map are all thread-safe, so deferring is a drop-in.
        long chunkKey = pos.toLong();
        ResourceKey<Level> dimensionKey = world.dimension();
        AsyncExecutor.execute(() -> {
            CacheStorage storage = state.storage();
            for (int localZ = 0; localZ < CHUNK_SIDE; localZ++) {
                for (int localX = 0; localX < CHUNK_SIDE; localX++) {
                    int idx = localZ * CHUNK_SIDE + localX;
                    long key = hash(startX + localX, startZ + localZ);
                    storage.putHeight(key, snapshot[idx]);
                }
            }
            state.putChunkSnapshot(chunkKey, new ChunkHeightSnapshot(snapshot, CHUNK_SIDE));
            DebugLog.cache(LOGGER, "Chunk snapshot refreshed for {} ({})", pos, dimensionKey.location());
        });
    }

    /**
     * Removes cached heightmap data when a chunk is unloaded to free memory.
     */
    public static void onChunkUnload(ServerLevel world, ChunkPos pos) {
        if (world.isClientSide()) {
            return;
        }
        WorldCacheState state = STATES.get(world.dimension());
        if (state != null) {
            state.removeChunkSnapshot(pos.toLong());
            DebugLog.cache(LOGGER, "Chunk snapshot released for {} ({})", pos, world.dimension().location());
        }
    }

    private static ChunkHeightSnapshot ensureChunkSnapshot(ServerLevel world,
                                                           WorldCacheState state,
                                                           CacheStorage storage,
                                                           long key) {
        int x = (int) (key >> 32);
        int z = (int) key;
        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
        long chunkKey = chunkPos.toLong();
        ChunkHeightSnapshot cached = state.getChunkSnapshot(chunkKey);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<ChunkHeightSnapshot> future = new CompletableFuture<>();
        CompletableFuture<ChunkHeightSnapshot> existing = state.chunkComputations().putIfAbsent(chunkKey, future);
        if (existing != null) {
            try {
                return existing.join();
            } catch (RuntimeException e) {
                DebugLog.info(LOGGER, "Height snapshot future failed for chunk {}", chunkPos, e);
                return null;
            }
        }

        try {
            ChunkHeightSnapshot generated = ChunkHeightGenerator.generate(
                    world,
                    state,
                    storage,
                    chunkPos,
                    CHUNK_SIDE,
                    COLUMNS_PER_CHUNK
            );
            if (generated != null) {
                state.putChunkSnapshot(chunkKey, generated);
                DebugLog.cache(LOGGER, "Snapshot computed for chunk {} ({} columns)", chunkPos, generated.columns());
            }
            future.complete(generated);
            return generated;
        } catch (RuntimeException e) {
            future.completeExceptionally(e);
            LOGGER.error("Failed to compute height snapshot for chunk {}", chunkPos, e);
            return null;
        } finally {
            state.chunkComputations().remove(chunkKey);
        }
    }
    public static CacheStats stats(ServerLevel world) {
        WorldCacheState state = STATES.get(world.dimension());
        if (state == null) {
            return CacheStats.UNAVAILABLE;
        }
        CacheStorage storage = state.storage();
        CacheSettings settings = storage.settings();
        return new CacheStats(
                true,
                storage.runtimeWeightBytes(),
                settings.runtimeBudgetBytes(),
                state.snapshotWeightBytes(),
                settings.snapshotBudgetBytes(),
                storage.regionWeightBytes(),
                settings.persistedBudgetBytes(),
                settings.enablePrefill(),
                settings.clampedPrefillMaxChunks()
        );
    }

    public record CacheStats(
            boolean available,
            long runtimeUsedBytes,
            long runtimeBudgetBytes,
            long snapshotUsedBytes,
            long snapshotBudgetBytes,
            long persistedUsedBytes,
            long persistedBudgetBytes,
            boolean prefillEnabled,
            int prefillMaxChunks
    ) {
        public static final CacheStats UNAVAILABLE = new CacheStats(
                false,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                false,
                0
        );
    }
}
