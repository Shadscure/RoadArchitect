package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tracks road-building tasks for loaded chunks.
 * <p>Road segments are generated via {@link net.oxcodsnet.roadarchitect.worldgen.RoadFeature}.</p>
 */
public class RoadBuilderManager {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadBuilderManager.class.getSimpleName());

    /**
     * Формирует задачи на строительство для переданных путей.
     * <p>Queues building tasks for the given paths.</p>
     */
    static void queueSegments(ServerLevel world, Map<String, List<BlockPos>> paths) {
        RoadBuilderStorage storage = RoadBuilderStorage.get(world);

        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String key = entry.getKey();
            List<BlockPos> path = entry.getValue();
            if (path == null || path.size() < 2) {
                DebugLog.info(LOGGER, "Skipped road construction {} ({} steps) because path too short", key, path == null ? 0 : path.size());
                continue;
            }
            int i = 0;
            while (i < path.size()) {
                ChunkPos chunk = new ChunkPos(path.get(i));
                int start = i;
                do {
                    i++;
                } while (i < path.size() && new ChunkPos(path.get(i)).equals(chunk));
                storage.addSegment(chunk, key, start, i);
            }
            DebugLog.info(LOGGER, "Queued road construction {} ({} steps)", key, path.size());
        }
    }
}
