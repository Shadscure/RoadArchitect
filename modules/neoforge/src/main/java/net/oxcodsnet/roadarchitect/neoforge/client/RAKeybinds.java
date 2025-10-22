package net.oxcodsnet.roadarchitect.neoforge.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class RAKeybinds {
    public static KeyBinding OPEN;

    private RAKeybinds() {
    }

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
