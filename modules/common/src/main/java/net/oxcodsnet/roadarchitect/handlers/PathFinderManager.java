package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.AsyncExecutor;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Управляет вычислением путей между узлами при различных событиях сервера.
 * <p>Handles path calculation between nodes on various server events.</p>
 */
public class PathFinderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RoadArchitect.MOD_ID + "/PathFinderManager"
    );

    /**
     * Computes paths for all NEW edges and saves them into {@link net.oxcodsnet.roadarchitect.storage.PathStorage}.
     *
     * @param world            server world
     * @param preFillCacheZone half-size of the prefill square area in blocks (unused by default)
     * @param maxSteps         A* global step limit for this run
     */
    public static void computePaths(ServerWorld world, int preFillCacheZone, int maxSteps) {
        PipelineProfiler.increment("pathfinding.invocations");
        PipelineProfiler.recordValue("pathfinding.prefill_zone", preFillCacheZone);
        PipelineProfiler.recordValue("pathfinding.max_steps", maxSteps);
        RoadGraphState graph = RoadGraphState.get(world);
        PathStorage storage = PathStorage.get(world);

        // warm up caches once
        // CacheManager.prefill(world, -preFillCacheZone, -preFillCacheZone, preFillCacheZone,  preFillCacheZone);
        PathFinder finder = new PathFinder(graph.nodes(), world, maxSteps);

        List<CompletableFuture<PathJob>> futures = new ArrayList<>();
        int scheduledJobs = 0;
        try (PipelineProfiler.Section preparation = PipelineProfiler.openSection("pathfinding.prepare_jobs")) {
        for (Map.Entry<String, EdgeStorage.Status> entry : graph.edges().allWithStatus().entrySet()) {
            if (entry.getValue() != EdgeStorage.Status.NEW) continue;
            String edgeId = entry.getKey();
            String[] nodes = KeyUtil.parseEdgeKey(edgeId);
            if (nodes.length != 2) {
                LOGGER.debug("Invalid edge id: {}", edgeId);
                continue;
            }
            String from = nodes[0], to = nodes[1];

            CompletableFuture<PathJob> job = AsyncExecutor.submit(() -> {
                long start = System.nanoTime();
                List<BlockPos> path = finder.findPath(from, to);
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                return new PathJob(edgeId, from, to, path, ms);
            });
            futures.add(job);
            scheduledJobs++;
        }
        }
        PipelineProfiler.recordValue("pathfinding.jobs_scheduled", scheduledJobs);

        try (PipelineProfiler.Section await = PipelineProfiler.openSection("pathfinding.await_completion")) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        for (CompletableFuture<PathJob> future : futures) {
            try {
                PathJob job = future.get();
                PathStorage.Status st = job.path().isEmpty() ? PathStorage.Status.FAILED : PathStorage.Status.PENDING;
                storage.putPath(job.from(), job.to(), job.path(), st);
                PipelineProfiler.recordValue("pathfinding.job_duration_ms", job.durationMs());
                PipelineProfiler.recordValue("pathfinding.path_length", job.path().size());
                if (!job.path().isEmpty()) {
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.SUCCESS);
                    PipelineProfiler.increment("pathfinding.paths.success");
                    LOGGER.debug(
                            ">>> Computed path {} ({} ms)",
                            job.edgeId(), job.durationMs()
                    );
                } else {
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.FAILURE);
                    PipelineProfiler.increment("pathfinding.paths.failure");
                    LOGGER.debug(
                            "! No path for {} ({} ms)",
                            job.edgeId(), job.durationMs()
                    );
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Path computation failed", e);
                Thread.currentThread().interrupt();
                PipelineProfiler.increment("pathfinding.paths.exception");
            }
        }

        storage.markDirty();
        graph.markDirty();
        LOGGER.debug("Path calculation completed for world {}",
                world.getRegistryKey().getValue()
        );
    }

    // overloads for backwards compatibility
    /**
     * Backward-compat shortcut with default parameters.
     */
    public static void computePaths(ServerWorld world) {
        computePaths(world, 50, 10480);
    }

    /**
     * Backward-compat shortcut with default {@code maxSteps}.
     *
     * @param world            server world
     * @param preFillCacheZone half-size of the prefill square area in blocks (unused by default)
     */
    public static void computePaths(ServerWorld world, int preFillCacheZone) {
        computePaths(world, preFillCacheZone, 10480);
    }

    private record PathJob(
            String edgeId, String from, String to, List<BlockPos> path, double durationMs
    ) {
    }
}
