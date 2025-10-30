package net.oxcodsnet.roadarchitect.api.core;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * Read-only view over computed paths and their statuses.
 */
public interface PathView {
    List<BlockPos> path(String key);

    PathStatus status(String key);

    Map<String, PathStatus> allStatuses();

    List<String> pendingForChunk(ChunkPos chunk);
}
