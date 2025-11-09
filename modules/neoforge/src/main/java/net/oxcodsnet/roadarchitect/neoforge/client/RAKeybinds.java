package net.oxcodsnet.roadarchitect.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.lwjgl.glfw.GLFW;

public final class RAKeybinds {
    private static final ResourceLocation CATEGORY_ID = ResourceLocation.fromNamespaceAndPath(
            RoadArchitect.MOD_ID, "category.roadarchitect");
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(CATEGORY_ID);

    public static KeyMapping OPEN;

    private RAKeybinds() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        if (OPEN == null) {
            OPEN = new KeyMapping(
                    "key.roadarchitect.debug",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    CATEGORY
            );
        }
        event.register(OPEN);
    }
}
