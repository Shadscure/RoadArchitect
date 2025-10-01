package net.oxcodsnet.roadarchitect.handlers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.StructureLocator;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Менеджер запуска сканирования структур.
 * <p>Manager responsible for initiating structure scans.</p>
 */
public class StructureScanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + StructureScanManager.class.getSimpleName());

    /**
     * Triggers a structure scan around the given position with default radius.
     *
     * @param world    server world
     * @param approach label of the caller/context used in logs
     * @param center   scan center position
     */
    static void scan(ServerWorld world, String approach, BlockPos center) {
        scan(world, approach, center, 1);
    }

    /**
     * Triggers a structure scan around the given position.
     *
     * @param world          server world
     * @param approach       label of the caller/context used in logs
     * @param center         scan center position
     * @param overallRadius  grid half-size in chunks for planning pass
     */
    static void scan(ServerWorld world, String approach, BlockPos center, int overallRadius) {
        int scanRadius = 1;
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        LOGGER.debug("[{}] Scan launch: overallRadius={}, scanRadius={}, selectors={}", approach, overallRadius, scanRadius, selectors);
        PipelineProfiler.increment("structure_scan.invocations");
        PipelineProfiler.recordValue("structure_scan.selector_count", selectors.size());
        PipelineProfiler.recordValue("structure_scan.overall_radius", overallRadius);
        PipelineProfiler.recordValue("structure_scan.scan_radius", scanRadius);
        List<Pair<BlockPos, String>> found;
        try (PipelineProfiler.Section section = PipelineProfiler.openSection("structure_scan.locator")) {
            found = StructureLocator.scanGridAsync(world, center, overallRadius, scanRadius, selectors);
        }
        PipelineProfiler.recordValue("structure_scan.found", found.size());
        LOGGER.debug("[{}] Scanning is completed. Found structures: {}", approach, found.size());
    }

    /**
     * Triggers a structure scan with control over whether chunk loads are allowed for resolution.
     *
     * @param world           server world
     * @param approach        label used in logs
     * @param center          scan center
     * @param overallRadius   grid half-size in chunks for planning pass
     * @param allowChunkLoads if false, skip loading chunks for ambiguous candidates
     */
    static void scan(ServerWorld world, String approach, BlockPos center, int overallRadius, boolean allowChunkLoads) {
        int scanRadius = 1;
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        LOGGER.debug("[{}] Scan launch: overallRadius={}, scanRadius={}, allowChunkLoads={}, selectors={}",
                approach, overallRadius, scanRadius, allowChunkLoads, selectors);
        PipelineProfiler.increment("structure_scan.invocations");
        PipelineProfiler.recordValue("structure_scan.selector_count", selectors.size());
        PipelineProfiler.recordValue("structure_scan.overall_radius", overallRadius);
        PipelineProfiler.recordValue("structure_scan.scan_radius", scanRadius);
        PipelineProfiler.increment("structure_scan.allow_chunk_loads" + (allowChunkLoads ? ".enabled" : ".disabled"));
        List<Pair<BlockPos, String>> found;
        try (PipelineProfiler.Section section = PipelineProfiler.openSection("structure_scan.locator")) {
            found = StructureLocator.scanGridAsync(world, center, overallRadius, scanRadius, selectors, allowChunkLoads);
        }
        PipelineProfiler.recordValue("structure_scan.found", found.size());
        LOGGER.debug("[{}] Scanning is completed. Found structures: {} (allowChunkLoads={})", approach, found.size(), allowChunkLoads);
    }
}
