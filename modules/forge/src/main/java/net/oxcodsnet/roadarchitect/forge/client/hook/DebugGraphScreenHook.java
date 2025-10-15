package net.oxcodsnet.roadarchitect.forge.client.hook;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreenVanilla;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class DebugGraphScreenHook {
    private static final KeyBinding debugKey = new KeyBinding(
            "key.roadarchitect.debug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.roadarchitect"
    );


    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (debugKey.wasPressed()) {

            if (mc.isInSingleplayer() && mc.getServer() != null) {
                ServerWorld world = mc.getServer().getOverworld();
                if (world == null) return;

                RoadGraphState state = RoadGraphState.get(world);

                List<Node> nodes = new ArrayList<>(state.nodes().all().values());
                Collection<EdgeStorage.Edge> edges = state.edges().all().values();
                mc.setScreen(new RoadGraphDebugScreenVanilla(nodes, edges));
            }
        }
    }
}
