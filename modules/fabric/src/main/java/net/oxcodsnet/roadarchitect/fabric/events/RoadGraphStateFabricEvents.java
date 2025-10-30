package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.level.ServerLevel;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoadGraphStateFabricEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadGraphStateEvents");

    private RoadGraphStateFabricEvents() {}

    public static void register() {
        // LOAD мира → ensure state инициализирован
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.isClientSide()) {
                RoadGraphState.get(world);
                DebugLog.info(LOGGER, "RoadGraphState loaded for world {}", world.dimension().location());
            }
        });

        // UNLOAD мира → markDirty
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!world.isClientSide()) {
                RoadGraphState state = RoadGraphState.get(world);
                state.setDirty();
                DebugLog.info(LOGGER, "Saved RoadGraphState for world {} on unload", world.dimension().location());
            }
        });

        // Остановка сервера → markDirty для всех миров
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerLevel world : server.getAllLevels()) {
                RoadGraphState state = RoadGraphState.get(world);
                state.setDirty();
            }
            DebugLog.info(LOGGER, "Server stopping, all RoadGraphStates marked dirty");
        });
    }
}
