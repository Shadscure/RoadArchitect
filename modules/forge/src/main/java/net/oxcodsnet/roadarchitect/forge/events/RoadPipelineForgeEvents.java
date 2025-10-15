package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.compat.DhCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(bus = Bus.FORGE)
public class RoadPipelineForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadarchitect/ForgeEvents");

    private RoadPipelineForgeEvents() {
    }

    public static void register() {
        RoadPipelineController.init();
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        final boolean dhPresent = ModList.get().isLoaded(DhCompat.DH_MOD_ID);
        if (dhPresent) {
            LOGGER.debug("Distant Horizons detected: skipping INIT pregen; pipeline will start on player join");
        }
    }

    @SubscribeEvent
    public static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel().isClient()) {
            return;
        }
        
        final boolean dhPresent = ModList.get().isLoaded(DhCompat.DH_MOD_ID);
        if (event.getLevel() instanceof ServerWorld world) {
            if (!dhPresent) {
                RoadPipelineController.onSpawnChunkGenerated(world, event.getChunk());
            }
            RoadPipelineController.onChunkGenerated(world, event.getChunk());
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onChunkLoad(world, event.getChunk().getPos());
        }


    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getWorld().isClient()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            RoadPipelineController.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != Phase.START) {
            return;
        }
        RoadPipelineController.onServerTick(event.getServer());
        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RoadPipelineController.onServerStopping();
    }
}