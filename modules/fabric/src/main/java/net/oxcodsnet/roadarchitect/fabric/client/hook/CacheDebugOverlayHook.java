package net.oxcodsnet.roadarchitect.fabric.client.hook;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.ResourceLocation;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

public final class CacheDebugOverlayHook {
    private static final ResourceLocation CACHE_OVERLAY_ID = ResourceLocation.fromNamespaceAndPath(
            RoadArchitect.MOD_ID,
            "cache_debug_overlay"
    );

    private CacheDebugOverlayHook() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.DEBUG,
                CACHE_OVERLAY_ID,
                (graphics, tickCounter) -> CacheDebugOverlayRenderer.render(graphics)
        );
    }
}
