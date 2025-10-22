package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.minecraft.client.MinecraftClient;
import net.neoforged.neoforge.client.event.InputEvent;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreenVanilla;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static net.oxcodsnet.roadarchitect.neoforge.client.RAKeybinds.OPEN;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class DebugGraphScreenHook {
    private DebugGraphScreenHook() {
    }

    public static void onKey(InputEvent.Key event) {
        if (OPEN == null || !OPEN.wasPressed()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.currentScreen instanceof RoadGraphDebugScreenVanilla) {
            mc.setScreen(null);
            return;
        }

        if (mc.getServer() == null || mc.world == null) {
            return;
        }

        List<RoadGraphDebugScreenVanilla.DimensionLayer> layers = new ArrayList<>();
        for (RegistryKey<World> key : mc.getServer().getWorldRegistryKeys()) {
            ServerWorld world = mc.getServer().getWorld(key);
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

        layers.sort(Comparator.comparing(layer -> layer.dimension().getValue().toString()));
        RegistryKey<World> currentDim = mc.world.getRegistryKey();
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
