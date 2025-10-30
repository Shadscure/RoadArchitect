package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.server.level.ServerLevel;

/**
 * Static convenience facade to obtain core read-only views.
 */
public final class RoadCore {
    private RoadCore() {}

    public static RoadGraphView graph(ServerLevel world) {
        return CoreApiImpl.INSTANCE.graph(world);
    }

    public static PathView paths(ServerLevel world) {
        return CoreApiImpl.INSTANCE.paths(world);
    }

    public static BuildQueueView buildQueue(ServerLevel world) {
        return CoreApiImpl.INSTANCE.buildQueue(world);
    }

    public static DecorView decor(ServerLevel world) {
        return CoreApiImpl.INSTANCE.decor(world);
    }
}

