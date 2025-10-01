package net.oxcodsnet.roadarchitect.util;

//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Centralized, thread-safe caches for world generation data.
 * Reused across multiple PathFinder instances to avoid cache warm-up on each search.
 */
public final class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + CacheManager.class.getSimpleName());

    private static final Map<RegistryKey<World>, WorldState> STATES = new ConcurrentHashMap<>();
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
    public static void onWorldLoad(ServerWorld world) {
        if (world.isClient()) return;
        load(world);
    }

    /**
     * Called by platform hooks when a server world unloads.
     * Flushes and detaches the cache state from the internal map.
     *
     * @param world server world
     */
    public static void onWorldUnload(ServerWorld world) {
        if (world.isClient()) return;
        save(world);
    }

    /**
     * Called by platform hooks when the server is stopping.
     * Flushes all cached states for all worlds.
     *
     * @param server minecraft server
     */
    public static void onServerStopping(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            save(world);
        }
    }

    private static WorldState state(ServerWorld world) {
        return STATES.computeIfAbsent(world.getRegistryKey(), k -> new WorldState(world, CacheStorage.get(world)));
    }

    private static void load(ServerWorld world) {
        CacheStorage storage = CacheStorage.get(world);
        STATES.put(world.getRegistryKey(), new WorldState(world, storage));
        LOGGER.debug("Cache loaded for world {}", world.getRegistryKey().getValue());
    }

    private static void save(ServerWorld world) {
        WorldState state = STATES.remove(world.getRegistryKey());
        if (state != null) {
            state.chunkHeights().clear();
            state.storage().markDirty();
            LOGGER.debug("Cache saved for world {}", world.getRegistryKey().getValue());
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
    public static void prefill(ServerWorld world, int minX, int minZ, int maxX, int maxZ) {
        int step = PathFinder.GRID_STEP;
        WorldState state = state(world);
        CacheStorage storage = state.storage();
        AsyncExecutor.execute(() -> {
            ChunkGenerator gen = world.getChunkManager().getChunkGenerator();
            NoiseConfig cfg = world.getChunkManager().getNoiseConfig();
            MultiNoiseUtil.MultiNoiseSampler sampler = cfg.getMultiNoiseSampler();
            BiomeSource bsrc = gen.getBiomeSource();

            for (int x = minX; x <= maxX; x += step) {
                for (int z = minZ; z <= maxZ; z += step) {
                    long key = hash(x, z);
                    int finalX = x;
                    int finalZ = z;
                    AsyncExecutor.execute(() -> {
                        int h = gen.getHeight(finalX, finalZ, Heightmap.Type.WORLD_SURFACE_WG, world, cfg);
                        RegistryEntry<Biome> biome = bsrc.getBiome(
                                BiomeCoords.fromBlock(finalX), 316,
                                BiomeCoords.fromBlock(finalZ), sampler);
                        storage.heights().put(key, h);
                        storage.biomes().put(key, biome);
                    });
                }
            }
            LOGGER.debug("Prefill complete [{}..{}]×[{}..{}]",
                    minX, maxX, minZ, maxZ);
        });
    }

    /**
     * Gets or computes world surface height for the cached key.
     *
     * @param world  server world
     * @param key    cache key (hash of x,z)
     * @param loader fallback loader if value missing
     */
    public static int getHeight(ServerWorld world, long key, IntSupplier loader) {
        PipelineProfiler.increment("cache.height.requests");
        WorldState state = state(world);
        CacheStorage storage = state.storage();
        Integer cached = storage.heights().get(key);
        if (cached != null) {
            PipelineProfiler.increment("cache.height.hits");
            return cached;
        }
        Integer chunkCached = state.lookupHeight(key);
        if (chunkCached != null) {
            storage.heights().put(key, chunkCached);
            PipelineProfiler.increment("cache.height.hits");
            return chunkCached;
        }
        return storage.heights().computeIfAbsent(key, k -> {
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
    public static int getHeight(ServerWorld world, int x, int z) {
        long key = hash(x, z);
        return getHeight(world, key, () -> {
            ChunkGenerator gen = world.getChunkManager().getChunkGenerator();
            NoiseConfig cfg = world.getChunkManager().getNoiseConfig();
            return gen.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, cfg);
        });
    }

    /**
     * Gets or computes terrain stability metric for the key.
     */
    public static double getStability(ServerWorld world, long key, DoubleSupplier loader) {
        PipelineProfiler.increment("cache.stability.requests");
        WorldState state = state(world);
        CacheStorage storage = state.storage();
        Double cached = storage.stabilities().get(key);
        if (cached != null) {
            PipelineProfiler.increment("cache.stability.hits");
            return cached;
        }
        return storage.stabilities().computeIfAbsent(key, k -> {
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
    public static RegistryEntry<Biome> getBiome(ServerWorld world, long key, Supplier<RegistryEntry<Biome>> loader) {
        PipelineProfiler.increment("cache.biome.requests");
        WorldState state = state(world);
        CacheStorage storage = state.storage();
        RegistryEntry<Biome> cached = storage.biomes().get(key);
        if (cached != null) {
            PipelineProfiler.increment("cache.biome.hits");
            return cached;
        }
        return storage.biomes().computeIfAbsent(key, k -> {
            PipelineProfiler.increment("cache.biome.loads");
            try (PipelineProfiler.Section section = PipelineProfiler.openSection("cache.biome.load_time")) {
                RegistryEntry<Biome> value = loader.get();
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
    public static void onChunkLoad(ServerWorld world, Chunk chunk) {
        if (world.isClient()) {
            return;
        }
        WorldState state = STATES.get(world.getRegistryKey());
        if (state == null) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        int startX = pos.getStartX();
        int startZ = pos.getStartZ();
        int minY = state.minWorldY();

        Heightmap wg = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        Heightmap surface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        if (wg == null && surface == null) {
            return;
        }
        if (wg == null) {
            wg = surface;
        }
        if (surface == null) {
            surface = wg;
        }
        int[] snapshot = new int[COLUMNS_PER_CHUNK];
        boolean dirty = false;

        CacheStorage storage = state.storage();
        for (int localZ = 0; localZ < CHUNK_SIDE; localZ++) {
            for (int localX = 0; localX < CHUNK_SIDE; localX++) {
                int idx = localZ * CHUNK_SIDE + localX;
                int height = wg.get(localX, localZ);
                if (height <= minY) {
                    height = surface.get(localX, localZ);
                }
                snapshot[idx] = height;
                long key = hash(startX + localX, startZ + localZ);
                Integer previous = storage.heights().put(key, height);
                if (previous == null || previous.intValue() != height) {
                    dirty = true;
                }
            }
        }

        state.putChunkSnapshot(pos.toLong(), new ChunkHeightSnapshot(snapshot));
        if (dirty) {
            storage.markDirty();
        }
    }

    /**
     * Removes cached heightmap data when a chunk is unloaded to free memory.
     */
    public static void onChunkUnload(ServerWorld world, ChunkPos pos) {
        if (world.isClient()) {
            return;
        }
        WorldState state = STATES.get(world.getRegistryKey());
        if (state != null) {
            state.removeChunkSnapshot(pos.toLong());
        }
    }

    private static final class WorldState {
        private final CacheStorage storage;
        private final ConcurrentHashMap<Long, ChunkHeightSnapshot> chunkHeights = new ConcurrentHashMap<>();
        private final int minWorldY;

        WorldState(ServerWorld world, CacheStorage storage) {
            this.storage = storage;
            this.minWorldY = world.getBottomY();
        }

        CacheStorage storage() {
            return storage;
        }

        ConcurrentHashMap<Long, ChunkHeightSnapshot> chunkHeights() {
            return chunkHeights;
        }

        int minWorldY() {
            return minWorldY;
        }

        Integer lookupHeight(long key) {
            int x = (int) (key >> 32);
            int z = (int) key;
            long chunkKey = ChunkPos.toLong(x >> 4, z >> 4);
            ChunkHeightSnapshot snapshot = chunkHeights.get(chunkKey);
            if (snapshot == null) {
                return null;
            }
            int localX = x & (CHUNK_SIDE - 1);
            int localZ = z & (CHUNK_SIDE - 1);
            return snapshot.get(localX, localZ);
        }

        void putChunkSnapshot(long chunkKey, ChunkHeightSnapshot snapshot) {
            chunkHeights.put(chunkKey, snapshot);
        }

        void removeChunkSnapshot(long chunkKey) {
            chunkHeights.remove(chunkKey);
        }
    }

    private static final class ChunkHeightSnapshot {
        private final int[] heights;

        ChunkHeightSnapshot(int[] heights) {
            this.heights = heights;
        }

        int get(int localX, int localZ) {
            return heights[(localZ * CHUNK_SIDE) + localX];
        }
    }
}
