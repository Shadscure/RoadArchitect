package net.oxcodsnet.roadarchitect.util.cache;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
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

    public static ChunkHeightSnapshot generate(ServerWorld world,
                                               WorldCacheState state,
                                               CacheStorage storage,
                                               ChunkPos chunkPos,
                                               int chunkSide,
                                               int columnsPerChunk) {
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

        int horizontalCellCount = Math.max(1, chunkSide / horizontalBlockSize);
        int verticalCellCount = MathHelper.floorDiv(shape.height(), verticalBlockSize);
        if (verticalCellCount <= 0) {
            return null;
        }

        int[] heights = new int[columnsPerChunk];
        Arrays.fill(heights, state.minWorldY());
        boolean[] resolved = new boolean[columnsPerChunk];
        int remaining = columnsPerChunk;
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
            storage.markDirty();
        }

        return new ChunkHeightSnapshot(heights, chunkSide);
    }

    private static AquiferSampler.FluidLevelSampler createFluidSampler(ChunkGeneratorSettings settings) {
        AquiferSampler.FluidLevel lava = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
        int seaLevel = settings.seaLevel();
        AquiferSampler.FluidLevel sea = new AquiferSampler.FluidLevel(seaLevel, settings.defaultFluid());
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

    private static DensityFunctionTypes.Beardifying noBeard() {
        return LazyBeardifyingHolder.INSTANCE;
    }

    private static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
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
}
