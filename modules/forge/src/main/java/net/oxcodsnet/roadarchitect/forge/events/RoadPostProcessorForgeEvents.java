package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.handlers.RoadPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = RoadArchitect.MOD_ID)
public class RoadPostProcessorForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPostProcessor");

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClient() || event.phase != TickEvent.Phase.START) {
            return;
        }
        
        if (event.level instanceof ServerWorld world) {
            RoadPostProcessor.onStartWorldTick(world);
        }
    }
}