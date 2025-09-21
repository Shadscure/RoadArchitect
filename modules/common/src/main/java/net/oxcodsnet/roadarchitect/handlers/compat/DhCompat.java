package net.oxcodsnet.roadarchitect.handlers.compat;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized integration shim for Distant Horizons (DH) world-start behavior.
 *
 * Goal: start Road Architect INIT early (during world loading) without blocking
 * other mods that run heavy pre-generation on first load (like DH). We achieve this by
 * scheduling INIT with allowChunkLoads=false and feeding a heartbeat on any chunk load.
 *
 * Platform loaders (Fabric/NeoForge/Quilt) should call these helpers whenever DH is detected.
 */
public final class DhCompat {

    public static final String DH_MOD_ID = "distanthorizons";
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/DhCompat");

    private DhCompat() {
    }

    /**
     * Called on world load when DH is present. Ensures an early INIT is scheduled once per world.
     * Center is taken as the current spawn position at the moment of launch.
     */
    public static void onServerWorldLoad(ServerWorld world) {
        LOGGER.debug("DH compat: world load {} — ensuring early INIT scheduled", world.getRegistryKey().getValue());
        RoadPipelineController.ensureInitScheduledDhAware(world, 0);
    }

    /**
     * Called on any server chunk load when DH is present. Feeds activity heartbeat and
     * ensures early INIT is scheduled (once per world).
     */
    public static void onServerChunkLoad(ServerWorld world, ChunkPos pos) {
        RoadPipelineController.onAnyChunkLoad(world);
        RoadPipelineController.ensureInitScheduledDhAware(world, 0);
        // Если грузится спавн-чанк и INIT ещё не отрабатывал — запустим INIT немедленно
        if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
            ChunkPos spawnChunk = new ChunkPos(world.getSpawnPos());
            if (spawnChunk.equals(pos)) {
                BlockPos center = world.getSpawnPos();
                LOGGER.debug("DH compat: spawn chunk {} loaded — starting INIT immediately at {}", pos, center);
                RoadPipelineController.runInitNow(world, center);
            }
        }
    }
}
