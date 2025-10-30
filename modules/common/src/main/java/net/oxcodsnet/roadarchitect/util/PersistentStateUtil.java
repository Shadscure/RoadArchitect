package net.oxcodsnet.roadarchitect.util;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
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
     * @param key   the storage key
     * @return existing or newly created persistent state
     */
    public static <T extends SavedData> T get(ServerLevel world,
                                                   Supplier<T> constructor,
                                                   Function<CompoundTag, T> reader,
                                                   String key) {
        DimensionDataStorage manager = world.getDataStorage();
        return manager.computeIfAbsent(reader, constructor, key);
    }
}
