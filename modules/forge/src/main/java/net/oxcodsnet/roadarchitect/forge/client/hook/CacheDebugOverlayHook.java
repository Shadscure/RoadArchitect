package net.oxcodsnet.roadarchitect.forge.client.hook;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

/**
 * Forge port of {@code CacheDebugOverlayNeoForge} (1.21.1) — pipes the
 * F3 cache stats overlay through the same {@code DebugText} event that
 * NeoForge uses, so common-side rendering code is shared.
 */
@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class CacheDebugOverlayHook {
    private CacheDebugOverlayHook() {
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        CacheDebugOverlayRenderer.render(event.getGuiGraphics());
    }
}
