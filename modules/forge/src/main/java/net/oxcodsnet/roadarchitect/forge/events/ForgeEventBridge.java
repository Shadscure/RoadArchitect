package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.handlers.RoadGraphStateManager;
import net.oxcodsnet.roadarchitect.handlers.RoadPostProcessor;
import net.oxcodsnet.roadarchitect.util.CacheManager;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = RoadArchitect.MOD_ID)
public class ForgeEventBridge {
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphStateManager.onWorldLoad(world);
            CacheManager.onWorldLoad(world);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphStateManager.onWorldUnload(world);
            CacheManager.onWorldUnload(world);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerWorld world) {
            CacheManager.onChunkLoad(world, event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerWorld world) {
            CacheManager.onChunkUnload(world, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RoadGraphStateManager.onServerStopping(event.getServer());
        CacheManager.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public static void onStartWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerWorld world) {
            RoadPostProcessor.onStartWorldTick(world);
        }
    }
}