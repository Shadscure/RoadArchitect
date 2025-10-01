package net.oxcodsnet.roadarchitect.util;

//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
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
    private static DensityFunctionTypes.Beardifying noBeard() {
        return LazyBeardifyingHolder.INSTANCE;
    }

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
        ChunkHeightSnapshot snapshot = ensureChunkSnapshot(world, state, storage, key);
        if (snapshot != null) {
            int x = (int) (key >> 32);
            int z = (int) key;
            int localX = x & (CHUNK_SIDE - 1);
            int localZ = z & (CHUNK_SIDE - 1);
            int value = snapshot.get(localX, localZ);
            storage.heights().putIfAbsent(key, value);
            PipelineProfiler.increment("cache.height.hits");
            return value;
        }
        // Fast path for unloaded chunks: avoid generating full chunk snapshot.
        // Compute just this column via generator and cache it.
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

    private static ChunkHeightSnapshot ensureChunkSnapshot(ServerWorld world,
                                                           WorldState state,
                                                           CacheStorage storage,
                                                           long key) {
        int x = (int) (key >> 32);
        int z = (int) key;
        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
        long chunkKey = chunkPos.toLong();
        ChunkHeightSnapshot cached = state.chunkHeights().get(chunkKey);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<ChunkHeightSnapshot> future = new CompletableFuture<>();
        CompletableFuture<ChunkHeightSnapshot> existing = state.chunkComputations.putIfAbsent(chunkKey, future);
        if (existing != null) {
            try {
                return existing.join();
            } catch (RuntimeException e) {
                LOGGER.debug("Height snapshot future failed for chunk {}", chunkPos, e);
                return null;
            }
        }

        try {
            ChunkHeightSnapshot generated = generateChunkSnapshot(world, state, storage, chunkPos);
            if (generated != null) {
                state.putChunkSnapshot(chunkKey, generated);
            }
            future.complete(generated);
            return generated;
        } catch (RuntimeException e) {
            future.completeExceptionally(e);
            LOGGER.error("Failed to compute height snapshot for chunk {}", chunkPos, e);
            return null;
        } finally {
            state.chunkComputations.remove(chunkKey);
        }
    }

    private static ChunkHeightSnapshot generateChunkSnapshot(ServerWorld world,
                                                             WorldState state,
                                                             CacheStorage storage,
                                                             ChunkPos chunkPos) {
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        if (!(generator instanceof NoiseChunkGenerator noiseGenerator)) {
            return null;
        }

        NoiseConfig noiseConfig = world.getChunkManager().getNoiseConfig();
        ChunkGeneratorSettings settings = noiseGenerator.getSettings().value();
        GenerationShapeConfig shape = settings.generationShapeConfig().trimHeight(world);
        int horizontalBlockSize = shape.horizontalCellBlockCount();
        int verticalBlockSize = shape.verticalCellBlockCount();
        if (horizontalBlockSize <= 0 || verticalBlockSize <= 0) {
            return null;
        }

        int horizontalCellCount = Math.max(1, 16 / horizontalBlockSize);
        int verticalCellCount = MathHelper.floorDiv(shape.height(), verticalBlockSize);
        if (verticalCellCount <= 0) {
            return null;
        }

        int[] heights = new int[COLUMNS_PER_CHUNK];
        Arrays.fill(heights, state.minWorldY());
        boolean[] resolved = new boolean[COLUMNS_PER_CHUNK];
        int remaining = COLUMNS_PER_CHUNK;
        Predicate<BlockState> predicate = Heightmap.Type.WORLD_SURFACE_WG.getBlockPredicate();
        BlockState defaultBlock = settings.defaultBlock();

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int minCellY = MathHelper.floorDiv(shape.minimumY(), verticalBlockSize);

        AccessibleChunkNoiseSampler sampler = new AccessibleChunkNoiseSampler(
                horizontalCellCount,
                noiseConfig,
                startX,
                startZ,
                shape,
                noBeard(),
                settings,
                createFluidSampler(settings),
                Blender.getNoBlending()
        );

        sampler.sampleStartDensity();
        long startNanos = System.nanoTime();
        int noiseColumns;

        outer:
        for (int cellX = 0; cellX < horizontalCellCount; cellX++) {
            sampler.sampleEndDensity(cellX);
            for (int cellZ = 0; cellZ < horizontalCellCount; cellZ++) {
                for (int cellY = verticalCellCount - 1; cellY >= 0; cellY--) {
                    sampler.onSampledCellCorners(cellY, cellZ);
                    for (int voxelY = verticalBlockSize - 1; voxelY >= 0; voxelY--) {
                        int absoluteY = (minCellY + cellY) * verticalBlockSize + voxelY;
                        double fracY = (double) voxelY / verticalBlockSize;
                        sampler.interpolateY(absoluteY, fracY);
                        for (int voxelX = horizontalBlockSize - 1; voxelX >= 0; voxelX--) {
                            int globalX = startX + cellX * horizontalBlockSize + voxelX;
                            double fracX = (double) voxelX / horizontalBlockSize;
                            sampler.interpolateX(globalX, fracX);
                            for (int voxelZ = horizontalBlockSize - 1; voxelZ >= 0; voxelZ--) {
                                int globalZ = startZ + cellZ * horizontalBlockSize + voxelZ;
                                double fracZ = (double) voxelZ / horizontalBlockSize;
                                sampler.interpolateZ(globalZ, fracZ);
                                int localX = globalX & (CHUNK_SIDE - 1);
                                int localZ = globalZ & (CHUNK_SIDE - 1);
                                int index = localZ * CHUNK_SIDE + localX;
                                BlockState stateAtPos = sampler.sampleBlockStateDirect();
                                if (resolved[index]) {
                                    continue;
                                }
                                if (stateAtPos == null) {
                                    stateAtPos = defaultBlock;
                                }
                                if (predicate.test(stateAtPos)) {
                                    heights[index] = absoluteY + 1;
                                    resolved[index] = true;
                                    remaining--;
                                    if (remaining == 0) {
                                        break outer;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        sampler.stopInterpolation();
        noiseColumns = COLUMNS_PER_CHUNK - remaining;
        long noiseDuration = System.nanoTime() - startNanos;
        recordLoadDurationPerColumn(noiseDuration, noiseColumns);
        if (noiseColumns > 0) {
            PipelineProfiler.increment("cache.height.loads", noiseColumns);
        }

        if (remaining > 0) {
            for (int index = 0; index < COLUMNS_PER_CHUNK; index++) {
                if (resolved[index]) {
                    continue;
                }
                int localX = index % CHUNK_SIDE;
                int localZ = index / CHUNK_SIDE;
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                PipelineProfiler.increment("cache.height.loads");
                try (PipelineProfiler.Section section = PipelineProfiler.openSection("cache.height.load_time")) {
                    int value = generator.getHeight(worldX, worldZ, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
                    heights[index] = value;
                }
            }
        }

        for (int value : heights) {
            PipelineProfiler.recordValue("cache.height.loaded_value", value);
        }

        boolean dirty = false;
        int writeIndex = 0;
        for (int localZ = 0; localZ < CHUNK_SIDE; localZ++) {
            for (int localX = 0; localX < CHUNK_SIDE; localX++, writeIndex++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                long columnKey = hash(worldX, worldZ);
                int value = heights[writeIndex];
                Integer previous = storage.heights().put(columnKey, value);
                if (previous == null || previous.intValue() != value) {
                    dirty = true;
                }
            }
        }
        if (dirty) {
            storage.markDirty();
        }

        return new ChunkHeightSnapshot(heights);
    }

    private static AquiferSampler.FluidLevelSampler createFluidSampler(ChunkGeneratorSettings settings) {
        AquiferSampler.FluidLevel lava = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
        int seaLevel = settings.seaLevel();
        AquiferSampler.FluidLevel sea = new AquiferSampler.FluidLevel(seaLevel, settings.defaultFluid());
        AquiferSampler.FluidLevel air = new AquiferSampler.FluidLevel(DimensionType.MIN_HEIGHT * 2, Blocks.AIR.getDefaultState());
        int cutoff = Math.min(-54, seaLevel);
        return (x, y, z) -> y < cutoff ? lava : sea;
    }

    private static void recordLoadDurationPerColumn(long totalNanos, int columns) {
        if (columns <= 0 || totalNanos <= 0L) {
            return;
        }
        long base = totalNanos / columns;
        long remainder = totalNanos % columns;
        for (int i = 0; i < columns; i++) {
            long sample = base + (i < remainder ? 1L : 0L);
            PipelineProfiler.recordDuration("cache.height.load_time", sample);
        }
    }

    private static final class LazyBeardifyingHolder {
        private static final DensityFunctionTypes.Beardifying INSTANCE = new DensityFunctionTypes.Beardifying() {
            @Override
            public double sample(DensityFunction.NoisePos pos) {
                return 0.0D;
            }

            @Override
            public void fill(double[] densities, DensityFunction.EachApplier applier) {
                Arrays.fill(densities, 0.0D);
            }

            @Override
            public double minValue() {
                return 0.0D;
            }

            @Override
            public double maxValue() {
                return 0.0D;
            }

            @Override
            public CodecHolder<? extends DensityFunction> getCodecHolder() {
                return CodecHolder.of(MapCodec.unit(DensityFunctionTypes.constant(0.0D)));
            }
        };

        private LazyBeardifyingHolder() {
        }
    }

    private static final class AccessibleChunkNoiseSampler extends ChunkNoiseSampler {
        AccessibleChunkNoiseSampler(int horizontalCellCount,
                                    NoiseConfig noiseConfig,
                                    int startBlockX,
                                    int startBlockZ,
                                    GenerationShapeConfig generationShapeConfig,
                                    DensityFunctionTypes.Beardifying beardifying,
                                    ChunkGeneratorSettings chunkGeneratorSettings,
                                    AquiferSampler.FluidLevelSampler fluidLevelSampler,
                                    Blender blender) {
            super(horizontalCellCount, noiseConfig, startBlockX, startBlockZ, generationShapeConfig,
                    beardifying, chunkGeneratorSettings, fluidLevelSampler, blender);
        }

        BlockState sampleBlockStateDirect() {
            return super.sampleBlockState();
        }
    }

    private static final class WorldState {
        private final CacheStorage storage;
        private final ConcurrentHashMap<Long, ChunkHeightSnapshot> chunkHeights = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, CompletableFuture<ChunkHeightSnapshot>> chunkComputations = new ConcurrentHashMap<>();
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
            chunkComputations.remove(chunkKey);
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
