package net.oxcodsnet.roadarchitect.fabric.client.hook;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.oxcodsnet.roadarchitect.client.gui.LoadingOverlayRenderer;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;

public final class LoadingOverlayHook {
    private LoadingOverlayHook() {}

    public static void init() {
        // AFTER_INIT -> на КАЖДЫЙ инстанс экрана вешаем afterRender
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof LevelLoadingScreen)) return;

            // Регистрируем post-render на ЭТОТ экран
            ScreenEvents.afterRender(screen).register((scr, ctx, mouseX, mouseY, tickDelta) -> {
                if (!RAConfigHolder.get().debugShowScanningBar()) {
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                LoadingOverlayRenderer.render(
                        ctx,
                        mc.getWindow().getGuiScaledWidth(),
                        mc.getWindow().getGuiScaledHeight()
                );
            });
        });
    }
}
