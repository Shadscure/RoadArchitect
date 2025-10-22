package net.oxcodsnet.roadarchitect.forge.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RAKeybinds {
    public static KeyBinding OPEN;

    private RAKeybinds() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN = new KeyBinding(
                "key.roadarchitect.debug",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.roadarchitect"
        );
        event.register(OPEN);
    }
}
