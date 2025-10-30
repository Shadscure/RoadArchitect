package net.oxcodsnet.roadarchitect.api.core;

import java.util.List;
import net.minecraft.world.level.ChunkPos;

/**
 * Read-only access to queued build segments per chunk.
 */
public interface BuildQueueView {
    List<BuildSegment> segments(ChunkPos chunk);
}
