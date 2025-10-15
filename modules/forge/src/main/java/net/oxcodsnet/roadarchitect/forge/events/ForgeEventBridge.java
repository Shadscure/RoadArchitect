package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.CacheManager;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = RoadArchitect.MOD_ID)
public class ForgeEventBridge {


    // ===================== CacheManager =====================
    @SubscribeEvent
    public static void onCacheWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClient()) {
            return;
        }
        CacheManager.onWorldLoad((ServerWorld) event.getLevel());
    }

    @SubscribeEvent
    public static void onCacheWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClient()) {
            return;
        }
        CacheManager.onWorldUnload((ServerWorld) event.getLevel());
    }

    @SubscribeEvent
    public static void onCacheServerStopping(ServerStoppingEvent event) {
        CacheManager.onServerStopping(event.getServer());
    }
}