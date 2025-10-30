package net.oxcodsnet.roadarchitect.api.storage;

import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Minimal key-value persistent storage for addons, scoped per world and addon id.
 * Values are stored as NBT compounds keyed by {@link ResourceLocation}.
 */
public interface PersistentStore {
    Optional<CompoundTag> get(ResourceLocation key);

    void put(ResourceLocation key, CompoundTag value);

    void remove(ResourceLocation key);

    Set<ResourceLocation> keys();
}

