package net.oxcodsnet.roadarchitect.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.forge.config.RAConfigForgeBridge;
import net.oxcodsnet.roadarchitect.forge.events.RoadFeatureRegistryForge;
import net.oxcodsnet.roadarchitect.forge.events.RoadPipelineForgeEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RoadArchitect.MOD_ID)
public class RoadArchitectForge {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    public RoadArchitectForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        RAConfigForgeBridge.bootstrap();
        RoadArchitect.init();
        RoadFeatureRegistryForge.register(modEventBus);
    }
}