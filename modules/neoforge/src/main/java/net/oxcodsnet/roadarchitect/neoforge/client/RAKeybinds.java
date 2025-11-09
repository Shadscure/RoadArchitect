package net.oxcodsnet.roadarchitect.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class RAKeybinds {
    public static KeyMapping OPEN;

    private RAKeybinds() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN = new KeyMapping(
                "key.roadarchitect.debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.roadarchitect"
        );
        event.register(OPEN);
    }
}
