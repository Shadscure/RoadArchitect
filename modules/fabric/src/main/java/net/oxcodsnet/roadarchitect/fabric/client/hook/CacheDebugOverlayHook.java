package net.oxcodsnet.roadarchitect.fabric.client.hook;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

public final class CacheDebugOverlayHook {
    private CacheDebugOverlayHook() {
    }

    public static void init() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> CacheDebugOverlayRenderer.render(graphics));
    }
}
