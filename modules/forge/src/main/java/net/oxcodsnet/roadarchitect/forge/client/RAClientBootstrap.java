package net.oxcodsnet.roadarchitect.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.forge.config.RAConfigForgeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RAClientBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RAClientBootstrap.class.getSimpleName());

    private RAClientBootstrap() {
    }

    public static void init() {
        registerConfigScreen();
    }

    /**
     * Wires the AutoConfig screen into the Forge mods list. Without this,
     * Cloth Config 11.x falls back to its bundled {@code ClothConfigForgeDemo}
     * which crashes under mojmap dev environments with NoSuchMethodError on
     * {@code Component.translatable} (SRG-mapped {@code m_237115_}). Same
     * symptom would hit production Forge if anyone ever clicks the Config
     * button next to RoadArchitect in the mod list.
     */
    private static void registerConfigScreen() {
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> RAConfigForgeBridge.createScreen(parent)
                    )
            );
            LOGGER.info("Forge config screen registered");
        } catch (Throwable t) {
            LOGGER.warn("Failed to register Forge config screen — falling back to default handler", t);
        }
    }
}
