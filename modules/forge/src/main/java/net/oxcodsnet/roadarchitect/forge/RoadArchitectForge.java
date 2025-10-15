package net.oxcodsnet.roadarchitect.forge;

import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.forge.events.RoadPipelineForgeEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RoadArchitect.MOD_ID)
public class RoadArchitectForge {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    public RoadArchitectForge() {
        RoadPipelineForgeEvents.register();
    }
}