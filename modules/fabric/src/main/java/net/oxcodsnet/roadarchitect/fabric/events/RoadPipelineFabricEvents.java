package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
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
        // 0) Инициализация контроллера (кеш селекторов)
        RoadPipelineController.init();

        final boolean dhPresent = FabricLoader.getInstance().isModLoaded("distanthorizons");
        if (dhPresent) {
            LOGGER.debug("Distant Horizons detected: deferring INIT until first player join");
        }

        // 1) Первая генерация спавн-чанка (и вообще генерация чанка)
        ServerChunkEvents.CHUNK_LOAD.register((ServerWorld world, WorldChunk chunk) -> {
            // При наличии Distant Horizons отложим INIT, чтобы избежать стопа загрузки мира при первом старте
            if (!dhPresent) {
                RoadPipelineController.onSpawnChunkGenerated(world, chunk);
            }
            RoadPipelineController.onChunkGenerated(world, chunk);
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onChunkLoad(world, chunk.getPos());
        });

        // 3) Игрок зашёл
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RoadPipelineController.onPlayerJoin(handler.getPlayer()));

        // 4) Периодика по тикам
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            RoadPipelineController.onServerTick(server);
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onServerTick(server);
        });

        // 5) Остановка сервера
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RoadPipelineController.onServerStopping());
    }
}
