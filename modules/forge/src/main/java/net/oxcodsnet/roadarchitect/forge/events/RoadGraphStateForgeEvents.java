package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = RoadArchitect.MOD_ID)
public class RoadGraphStateForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadGraphStateEvents");

    @SubscribeEvent
    public static void onWorldLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
        if (event.getLevel().isClient()) {
            return;
        }
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphState state = RoadGraphState.get(world);
            state.markDirty();
        }

        LOGGER.debug("RoadGraphState loaded for world {}", event.getLevel().getDimension().toString());
    }

    @SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClient()) {
            return;
        }
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphState state = RoadGraphState.get(world);
            state.markDirty();
        }

        LOGGER.debug("Saved RoadGraphState for world {} on unload", event.getLevel().getDimension().toString());
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        for (var level : event.getServer().getWorlds()) {
            if (level.isClient()) {
                continue;
            }
            RoadGraphState state = RoadGraphState.get(level);
            state.markDirty();
        }
        LOGGER.debug("Server stopping, all RoadGraphStates marked dirty");
    }
}