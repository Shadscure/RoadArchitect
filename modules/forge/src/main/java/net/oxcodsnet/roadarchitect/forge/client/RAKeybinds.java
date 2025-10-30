package net.oxcodsnet.roadarchitect.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RAKeybinds {
    public static KeyMapping OPEN;

    private RAKeybinds() {
    }

    @SubscribeEvent
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
