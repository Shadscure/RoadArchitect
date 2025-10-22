package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.compat.DhCompat;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoadPipelineForgeEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadarchitect/ForgeEvents");
    private static boolean dhPresent;

    private RoadPipelineForgeEvents() {
    }

    public static void register() {
        RoadPipelineController.init();
        dhPresent = ModList.get().isLoaded(DhCompat.DH_MOD_ID);
        if (dhPresent) {
            DebugLog.info(LOGGER, "Distant Horizons detected: skipping INIT pregen; pipeline will start on player join");
        }
        MinecraftForge.EVENT_BUS.register(RoadPipelineForgeEvents.class);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerWorld world)) {
            return;
        }
        if (!(event.getChunk() instanceof WorldChunk)) {
            return;
        }
        WorldChunk chunk = (WorldChunk) event.getChunk();
        if (!event.isNewChunk()) {
            return;
        }
        if (!dhPresent) {
            RoadPipelineController.onSpawnChunkGenerated(world, chunk);
        }
        RoadPipelineController.onChunkGenerated(world, chunk);
        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onChunkLoad(world, chunk.getPos());
    }

    @SubscribeEvent
    public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            RoadPipelineController.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
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
