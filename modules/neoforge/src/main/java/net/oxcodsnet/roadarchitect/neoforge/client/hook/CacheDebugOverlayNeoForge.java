package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiOverlay;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

@EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CacheDebugOverlayNeoForge {
    private CacheDebugOverlayNeoForge() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.DEBUG_TEXT.id())) {
            return;
        }
        CacheDebugOverlayRenderer.render(event.getGuiGraphics());
    }
}
