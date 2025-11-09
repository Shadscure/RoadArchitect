package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.oxcodsnet.roadarchitect.client.gui.LoadingOverlayRenderer;

public final class LoadingOverlaySubscriber {
    private LoadingOverlaySubscriber() {
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof LevelLoadingScreen)) {
            return;
        }

        var mc = Minecraft.getInstance();
        var ctx = event.getGuiGraphics(); // в Yarn это DrawContext
        LoadingOverlayRenderer.render(
                ctx,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight()
        );
    }
}
