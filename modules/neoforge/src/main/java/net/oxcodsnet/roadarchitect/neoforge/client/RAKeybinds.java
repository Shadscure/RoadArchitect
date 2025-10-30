package net.oxcodsnet.roadarchitect.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = RoadArchitect.MOD_ID,
        value = Dist.CLIENT
)
public final class RAKeybinds {
    public static KeyMapping OPEN;


    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        OPEN = new KeyMapping("key.roadarchitect.debug",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H,
                KeyMapping.Category.register(ResourceLocation.parse("category.roadarchitect")));
        e.register(OPEN);
    }
}
