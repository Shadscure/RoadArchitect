package net.oxcodsnet.roadarchitect.forge.client.hook;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreenVanilla;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.forge.client.RAKeybinds;

import java.util.*;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class DebugGraphScreenHook {
    @SubscribeEvent
    public static void onKey(InputEvent.Key e) {
        if (RAKeybinds.OPEN != null && RAKeybinds.OPEN.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.screen instanceof RoadGraphDebugScreenVanilla) {
                mc.setScreen(null);
                return;
            }

            if (mc.getSingleplayerServer() == null || mc.level == null) {
                return;
            }

            List<RoadGraphDebugScreenVanilla.DimensionLayer> layers = new ArrayList<>();
            for (ResourceKey<Level> key : mc.getSingleplayerServer().levelKeys()) {
                ServerLevel world = mc.getSingleplayerServer().getLevel(key);
                if (world == null) {
                    continue;
                }
                RoadGraphState state = RoadGraphState.get(world);
                List<Node> nodes = new ArrayList<>(state.nodes().all().values());
                List<EdgeStorage.Edge> edges = new ArrayList<>(state.edges().all().values());
                layers.add(new RoadGraphDebugScreenVanilla.DimensionLayer(key, nodes, edges));
            }

            if (layers.isEmpty()) {
                return;
            }

            layers.sort(Comparator.comparing(layer -> layer.dimension().location().toString()));
            ResourceKey<Level> currentDim = mc.level.dimension();
            int idx = -1;
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i).dimension().equals(currentDim)) {
                    idx = i;
                    break;
                }
            }
            if (idx > 0) {
                Collections.swap(layers, 0, idx);
            }

            mc.setScreen(new RoadGraphDebugScreenVanilla(layers));
        }
    }
}
