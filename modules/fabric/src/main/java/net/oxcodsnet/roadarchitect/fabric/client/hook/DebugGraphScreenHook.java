package net.oxcodsnet.roadarchitect.fabric.client.hook;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreenVanilla;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DebugGraphScreenHook {
    private DebugGraphScreenHook() {}

    public static void init() {
        KeyMapping openDebugKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.roadarchitect.debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.roadarchitect"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openDebugKey.consumeClick()) {
                if (mc.screen instanceof RoadGraphDebugScreenVanilla) {
                    mc.setScreen(null);
                    continue;
                }

                if (mc.getSingleplayerServer() == null || mc.level == null) {
                    continue;
                }

                List<RoadGraphDebugScreenVanilla.DimensionLayer> layers = new ArrayList<>();
                for (ResourceKey<Level> key : mc.getSingleplayerServer().levelKeys()) {
                    ServerLevel serverWorld = mc.getSingleplayerServer().getLevel(key);
                    if (serverWorld == null) {
                        continue;
                    }
                    RoadGraphState state = RoadGraphState.get(serverWorld);
                    List<Node> nodes = new ArrayList<>(state.nodes().all().values());
                    List<EdgeStorage.Edge> edges = new ArrayList<>(state.edges().all().values());
                    layers.add(new RoadGraphDebugScreenVanilla.DimensionLayer(key, nodes, edges));
                }

                if (layers.isEmpty()) {
                    continue;
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

                Minecraft.getInstance().setScreen(new RoadGraphDebugScreenVanilla(layers));
            }
        });
    }
}
