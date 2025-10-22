package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Платформо-независимые хуки жизненного цикла для состояния графа дорог.
 * Регистрация на реальные события выполняется в fabric / neoforge модулях.
 */
public final class RoadGraphStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadGraphStateManager.class.getSimpleName());

    private RoadGraphStateManager() {
    }

    /**
     * Вызывается платформой при загрузке ServerWorld.
     */
    public static void onWorldLoad(ServerLevel world) {
        // Гарантируем, что стейт создан/поднят
        RoadGraphState.get(world);
        DebugLog.info(LOGGER, "RoadGraphState loaded for world {}", world.dimension().location());
    }

    /**
     * Вызывается платформой при выгрузке ServerWorld.
     */
    public static void onWorldUnload(ServerLevel world) {
        RoadGraphState state = RoadGraphState.get(world);
        state.setDirty();
        DebugLog.info(LOGGER, "Saved RoadGraphState for world {} on unload", world.dimension().location());
    }

    /**
     * Вызывается платформой при остановке сервера (до закрытия миров).
     */
    public static void onServerStopping(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            RoadGraphState.get(world).setDirty();
        }
        DebugLog.info(LOGGER, "Server stopping, all RoadGraphStates marked dirty");
    }
}
