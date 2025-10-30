package net.oxcodsnet.roadarchitect.util.cache;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Generates chunk-sized height snapshots using the noise generator pipeline.
 */
public final class ChunkHeightGenerator {
    private ChunkHeightGenerator() {
    }

    public static ChunkHeightSnapshot generate(ServerLevel world,
                                               WorldCacheState state,
                                               CacheStorage storage,
                                               ChunkPos chunkPos,
                                               int chunkSide,
                                               int columnsPerChunk) {
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            return null;
        }

        RandomState noiseConfig = world.getChunkSource().randomState();
        NoiseGeneratorSettings settings = noiseGenerator.generatorSettings().value();
        NoiseSettings shape = settings.noiseSettings().clampToHeightAccessor(world);
        int horizontalBlockSize = shape.getCellWidth();
        int verticalBlockSize = shape.getCellHeight();
        if (horizontalBlockSize <= 0 || verticalBlockSize <= 0) {
            return null;
        }

        int horizontalCellCount = Math.max(1, chunkSide / horizontalBlockSize);
        int verticalCellCount = Mth.floorDiv(shape.height(), verticalBlockSize);
        if (verticalCellCount <= 0) {
            return null;
        }

        int[] heights = new int[columnsPerChunk];
        Arrays.fill(heights, state.minWorldY());
        boolean[] resolved = new boolean[columnsPerChunk];
        int remaining = columnsPerChunk;
        Predicate<BlockState> predicate = Heightmap.Types.WORLD_SURFACE_WG.isOpaque();
        BlockState defaultBlock = settings.defaultBlock();

        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int minCellY = Mth.floorDiv(shape.minY(), verticalBlockSize);

        AccessibleChunkNoiseSampler sampler = new AccessibleChunkNoiseSampler(
                horizontalCellCount,
                noiseConfig,
                startX,
                startZ,
                shape,
                noBeard(),
                settings,
                createFluidSampler(settings),
                Blender.empty()
        );

        sampler.initializeForFirstCellX();
        long startNanos = System.nanoTime();
        int noiseColumns;

        outer:
        for (int cellX = 0; cellX < horizontalCellCount; cellX++) {
            sampler.advanceCellX(cellX);
            for (int cellZ = 0; cellZ < horizontalCellCount; cellZ++) {
                for (int cellY = verticalCellCount - 1; cellY >= 0; cellY--) {
                    sampler.selectCellYZ(cellY, cellZ);
                    for (int voxelY = verticalBlockSize - 1; voxelY >= 0; voxelY--) {
                        int absoluteY = (minCellY + cellY) * verticalBlockSize + voxelY;
                        double fracY = (double) voxelY / verticalBlockSize;
                        sampler.updateForY(absoluteY, fracY);
                        for (int voxelX = horizontalBlockSize - 1; voxelX >= 0; voxelX--) {
                            int globalX = startX + cellX * horizontalBlockSize + voxelX;
                            double fracX = (double) voxelX / horizontalBlockSize;
                            sampler.updateForX(globalX, fracX);
                            for (int voxelZ = horizontalBlockSize - 1; voxelZ >= 0; voxelZ--) {
                                int globalZ = startZ + cellZ * horizontalBlockSize + voxelZ;
                                double fracZ = (double) voxelZ / horizontalBlockSize;
                                sampler.updateForZ(globalZ, fracZ);
                                int localX = globalX & (chunkSide - 1);
                                int localZ = globalZ & (chunkSide - 1);
                                int index = localZ * chunkSide + localX;
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
        noiseColumns = columnsPerChunk - remaining;
        long noiseDuration = System.nanoTime() - startNanos;
        recordLoadDurationPerColumn(noiseDuration, noiseColumns);
        if (noiseColumns > 0) {
            PipelineProfiler.increment("cache.height.loads", noiseColumns);
        }

        if (remaining > 0) {
            for (int index = 0; index < columnsPerChunk; index++) {
                if (resolved[index]) {
                    continue;
                }
                int localX = index % chunkSide;
                int localZ = index / chunkSide;
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                PipelineProfiler.increment("cache.height.loads");
                try (PipelineProfiler.Section section = PipelineProfiler.openSection("cache.height.load_time")) {
                    int value = generator.getBaseHeight(worldX, worldZ, Heightmap.Types.WORLD_SURFACE_WG, world, noiseConfig);
                    heights[index] = value;
                }
            }
        }

        for (int value : heights) {
            PipelineProfiler.recordValue("cache.height.loaded_value", value);
        }

        boolean dirty = false;
        int writeIndex = 0;
        for (int localZ = 0; localZ < chunkSide; localZ++) {
            for (int localX = 0; localX < chunkSide; localX++, writeIndex++) {
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
            storage.setDirty();
        }

        return new ChunkHeightSnapshot(heights, chunkSide);
    }

    private static Aquifer.FluidPicker createFluidSampler(NoiseGeneratorSettings settings) {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int seaLevel = settings.seaLevel();
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(seaLevel, settings.defaultFluid());
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

    private static DensityFunctions.BeardifierOrMarker noBeard() {
        return LazyBeardifyingHolder.INSTANCE;
    }

    private static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    private static final class LazyBeardifyingHolder {
        private static final DensityFunctions.BeardifierOrMarker INSTANCE = new DensityFunctions.BeardifierOrMarker() {
            @Override
            public double compute(DensityFunction.FunctionContext pos) {
                return 0.0D;
            }

            @Override
            public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
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
            public KeyDispatchDataCodec<? extends DensityFunction> codec() {
                return KeyDispatchDataCodec.of(MapCodec.unit(DensityFunctions.constant(0.0D)));
            }
        };

        private LazyBeardifyingHolder() {
        }
    }

    private static final class AccessibleChunkNoiseSampler extends NoiseChunk {
        AccessibleChunkNoiseSampler(int horizontalCellCount,
                                    RandomState noiseConfig,
                                    int startBlockX,
                                    int startBlockZ,
                                    NoiseSettings generationShapeConfig,
                                    DensityFunctions.BeardifierOrMarker beardifying,
                                    NoiseGeneratorSettings chunkGeneratorSettings,
                                    Aquifer.FluidPicker fluidLevelSampler,
                                    Blender blender) {
            super(horizontalCellCount, noiseConfig, startBlockX, startBlockZ, generationShapeConfig,
                    beardifying, chunkGeneratorSettings, fluidLevelSampler, blender);
        }

        BlockState sampleBlockStateDirect() {
            return super.getInterpolatedState();
        }
    }
}
