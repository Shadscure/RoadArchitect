package net.oxcodsnet.roadarchitect.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Utility methods for working with {@link SavedData}.
 */
public final class PersistentStateUtil {
    private PersistentStateUtil() {
    }

    /**
     * Retrieves or creates a {@link SavedData} for the given world.
     *
     * @param world the server world
     * @param type  the state type
     * @param key   the storage key
     * @return existing or newly created persistent state
     */
    public static <T extends SavedData> T get(ServerLevel world, SavedData.Factory<T> type, String key) {
        DimensionDataStorage manager = world.getDataStorage();
        return manager.computeIfAbsent(type, key);
    }
}
