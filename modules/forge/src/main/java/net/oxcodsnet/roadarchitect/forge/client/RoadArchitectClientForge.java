package net.oxcodsnet.roadarchitect.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.forge.RoadArchitectForge;
import net.oxcodsnet.roadarchitect.forge.client.hook.DebugGraphScreenHook;
import net.oxcodsnet.roadarchitect.forge.client.hook.LoadingOverlayHook;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RoadArchitectClientForge {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }
}