package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.compat.DhCompat;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric-адаптер: подписывается на события Fabric и делегирует в common.
 * Семантика полностью соответствует прежнему register() в RoadPipelineController.
 */
public final class RoadPipelineFabricEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadarchitect/FabricEvents");

    private RoadPipelineFabricEvents() {
    }

    public static void register() {
        RoadPipelineController.init();

        final boolean dhPresent = FabricLoader.getInstance().isModLoaded(DhCompat.DH_MOD_ID);
        if (dhPresent) {
            DebugLog.info(LOGGER, "Distant Horizons detected: skipping INIT pregen; pipeline will start on player join");
        }

        ServerChunkEvents.CHUNK_LOAD.register((ServerWorld world, WorldChunk chunk) -> {
            if (!dhPresent) {
                RoadPipelineController.onSpawnChunkGenerated(world, chunk);
            }
            RoadPipelineController.onChunkGenerated(world, chunk);
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onChunkLoad(world, chunk.getPos());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RoadPipelineController.onPlayerJoin(handler.getPlayer()));

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            RoadPipelineController.onServerTick(server);
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onServerTick(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RoadPipelineController.onServerStopping());
    }
}
