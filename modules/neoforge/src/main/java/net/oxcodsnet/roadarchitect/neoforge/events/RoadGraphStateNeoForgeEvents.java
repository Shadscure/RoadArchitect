package net.oxcodsnet.roadarchitect.neoforge.events;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge event handlers for {@link RoadGraphState} lifecycle.
 */
public final class RoadGraphStateNeoForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadGraphStateEvents");

    private RoadGraphStateNeoForgeEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(RoadGraphStateNeoForgeEvents.class);
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel world) {
            RoadGraphState.get(world);
            DebugLog.info(LOGGER, "RoadGraphState loaded for world {}", world.dimension().location());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel world) {
            RoadGraphState state = RoadGraphState.get(world);
            state.setDirty();
            DebugLog.info(LOGGER, "Saved RoadGraphState for world {} on unload", world.dimension().location());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel world : event.getServer().getAllLevels()) {
            RoadGraphState state = RoadGraphState.get(world);
            state.setDirty();
        }
        DebugLog.info(LOGGER, "Server stopping, all RoadGraphStates marked dirty");
    }
}
