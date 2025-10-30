package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.server.level.ServerLevel;

/**
 * Read-only access point for Road Architect core data structures.
 * Implementations return safe snapshots or unmodifiable views.
 */
public interface CoreApi {
    RoadGraphView graph(ServerLevel world);

    PathView paths(ServerLevel world);

    BuildQueueView buildQueue(ServerLevel world);

    DecorView decor(ServerLevel world);
}

