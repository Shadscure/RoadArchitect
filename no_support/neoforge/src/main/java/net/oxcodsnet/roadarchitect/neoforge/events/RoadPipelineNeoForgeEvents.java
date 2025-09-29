package net.oxcodsnet.roadarchitect.neoforge.events;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.ModList;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.compat.DhCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge adapter: subscribes to NeoForge events and delegates into common handlers.
 * Semantics mirror those of {@link RoadPipelineController} register logic.
 */
public final class RoadPipelineNeoForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadarchitect/NeoForgeEvents");
    private static boolean dhPresent;

    private RoadPipelineNeoForgeEvents() {
    }

    public static void register() {
        RoadPipelineController.init();
        dhPresent = ModList.get().isLoaded(DhCompat.DH_MOD_ID);
        if (dhPresent) {
            LOGGER.debug("Distant Horizons detected: skipping INIT pregen; pipeline will start on player join");
        }
        NeoForge.EVENT_BUS.register(RoadPipelineNeoForgeEvents.class);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk()) return;
        if (!(event.getLevel() instanceof ServerWorld world)) return;
        Chunk chunk = event.getChunk();
        if (!dhPresent) {
            RoadPipelineController.onSpawnChunkGenerated(world, chunk);
        }
        RoadPipelineController.onChunkGenerated(world, chunk);
        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onChunkLoad(world, chunk.getPos());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        RoadPipelineController.onPlayerJoin(player);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        RoadPipelineController.onServerTick(event.getServer());
        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RoadPipelineController.onServerStopping();
    }
}
