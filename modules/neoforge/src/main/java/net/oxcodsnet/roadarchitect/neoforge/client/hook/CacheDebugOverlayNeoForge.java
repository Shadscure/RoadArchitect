package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.DebugText;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

@EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class CacheDebugOverlayNeoForge {
    private CacheDebugOverlayNeoForge() {
    }

    @SubscribeEvent
    public static void onDebugText(DebugText event) {
        CacheDebugOverlayRenderer.render(event.getGuiGraphics());
    }
}
