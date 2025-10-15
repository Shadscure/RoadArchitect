package net.oxcodsnet.roadarchitect.forge.client.hook;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.LoadingOverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LoadingOverlayHook {
    private LoadingOverlayHook() {}

    @SubscribeEvent
    public static void onDrawScreenPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof LevelLoadingScreen)) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        LoadingOverlayRenderer.render(
                event.getGuiGraphics(),
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight()
        );
    }

}
