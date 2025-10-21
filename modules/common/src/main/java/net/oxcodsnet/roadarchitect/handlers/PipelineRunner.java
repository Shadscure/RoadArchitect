package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.Locale;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;

/**
 * Executes the road generation pipeline.
 */
public final class PipelineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + PipelineRunner.class.getSimpleName());

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile PipelineStage currentStage = PipelineStage.SCANNING_STRUCTURES;

    private PipelineRunner() {
    }

    public static PipelineStage getCurrentStage() {
        return currentStage;
    }

    private static void setStage(PipelineStage stage) {
        currentStage = stage;
    }

    /**
     * Runs the pipeline in the given mode starting from the provided position.
     */
    public static void runPipeline(ServerWorld world, BlockPos center, PipelineMode mode) {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        PipelineProfiler profiler = null;
        try {
            if (RoadArchitect.CONFIG.debugPipelineProfiler()) {
                profiler = PipelineProfiler.start(mode.reason(), worldId, center);
            }
            PipelineProfiler.increment("pipeline.run.total");
            PipelineProfiler.increment("pipeline.run." + mode.name().toLowerCase(Locale.ROOT));
            setStage(PipelineStage.INITIALISATION);
            DebugLog.info(LOGGER, "Pipeline start: {}", mode.reason());
            switch (mode) {

                case INIT -> {
                    setStage(PipelineStage.SCANNING_STRUCTURES);
                    try (PipelineProfiler.Section stage = PipelineProfiler.openSection("stage.structure_scan")) {
                        StructureScanManager.scan(world, mode.reason(), center,
                                RoadArchitect.CONFIG.initScanRadius());
                    }

                    setStage(PipelineStage.PATH_FINDING);
                    try (PipelineProfiler.Section stage = PipelineProfiler.openSection("stage.pathfinding")) {
                        PathFinderManager.computePaths(world, 1000);
                    }

                    setStage(PipelineStage.POST_PROCESSING);
                    try (PipelineProfiler.Section stage = PipelineProfiler.openSection("stage.post_processing")) {
                        RoadPostProcessor.processPending(world);
                    }
                }
                default -> {
                    setStage(PipelineStage.SCANNING_STRUCTURES);
                    try (PipelineProfiler.Section stage = PipelineProfiler.openSection("stage.structure_scan")) {
                        StructureScanManager.scan(world, mode.reason(), center,
                                RoadArchitect.CONFIG.chunkGenerateScanRadius());
                    }

                    setStage(PipelineStage.PATH_FINDING);
                    try (PipelineProfiler.Section stage = PipelineProfiler.openSection("stage.pathfinding")) {
                        PathFinderManager.computePaths(world, 50,
                                RoadArchitect.CONFIG.maxConnectionDistance() * 5);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Pipeline failure", e);
        } finally {
            if (profiler != null) {
                profiler.close();
            }
            setStage(PipelineStage.COMPLETE);
            RUNNING.set(false);
            DebugLog.info(LOGGER, "Pipeline finished: {}", mode.reason());
        }
    }

    public enum PipelineMode {
        INIT("initial_chunk"),
        CHUNK("chunk_structure_trigger"),
        PERIODIC("player_periodic_trigger");

        private final String reason;

        PipelineMode(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
