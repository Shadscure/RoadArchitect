package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoadGraphStateForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadGraphStateEvents");

    private RoadGraphStateForgeEvents() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphState.get(world);
            DebugLog.info(LOGGER, "RoadGraphState loaded for world {}", world.getRegistryKey().getValue());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphState state = RoadGraphState.get(world);
            state.markDirty();
            DebugLog.info(LOGGER, "Saved RoadGraphState for world {} on unload", world.getRegistryKey().getValue());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerWorld world : event.getServer().getWorlds()) {
            RoadGraphState state = RoadGraphState.get(world);
            state.markDirty();
        }
        DebugLog.info(LOGGER, "Server stopping, all RoadGraphStates marked dirty");
    }
}
