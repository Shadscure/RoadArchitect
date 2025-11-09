package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

@EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class CacheDebugOverlayNeoForge {
    private static final ResourceLocation CACHE_OVERLAY_ID = ResourceLocation.fromNamespaceAndPath(
            RoadArchitect.MOD_ID,
            "cache_debug_overlay"
    );

    private CacheDebugOverlayNeoForge() {
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.DEBUG_OVERLAY,
                CACHE_OVERLAY_ID,
                (graphics, deltaTracker) -> CacheDebugOverlayRenderer.render(graphics)
        );
    }
}
