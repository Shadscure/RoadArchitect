package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.DebugText;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugOverlayRenderer;

import java.util.List;

@EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class CacheDebugOverlayNeoForge {
    private CacheDebugOverlayNeoForge() {
    }

    @SubscribeEvent
    public static void onDebugText(DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        List<Component> lines = CacheDebugOverlayRenderer.collectLines(mc);
        if (lines.isEmpty()) {
            return;
        }
        lines.stream()
                .map(Component::getString)
                .forEach(event.getRight()::add);
    }
}
