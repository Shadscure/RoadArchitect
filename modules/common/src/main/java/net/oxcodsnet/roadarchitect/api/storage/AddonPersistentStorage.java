package net.oxcodsnet.roadarchitect.api.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-addon key-value persistent storage (world scoped), storing values as NBT compounds keyed by Identifier.
 */
public final class AddonPersistentStorage extends SavedData {
    private static final String ROOT = "entries";
    private static final String KEY = "key";
    private static final String VAL = "val";

    private final Map<ResourceLocation, CompoundTag> data = new ConcurrentHashMap<>();

    private final ResourceLocation addonId;

    private AddonPersistentStorage() {
        this.addonId = ResourceLocation.fromNamespaceAndPath("roadarchitect", "unknown");
    }

    private AddonPersistentStorage(ResourceLocation addonId) {
        this.addonId = addonId;
    }

    public static AddonPersistentStorage get(ServerLevel world, ResourceLocation addonId) {
        String storageKey = storageKey(addonId);
        SavedDataType<AddonPersistentStorage> type = new SavedDataType<>(
                storageKey,
                ctx -> new AddonPersistentStorage(addonId),
                ctx -> CompoundTag.CODEC.xmap(
                        tag -> fromNbt(tag, ctx.level().registryAccess(), addonId),
                        storage -> storage.writeNbt(new CompoundTag(), ctx.level().registryAccess())
                ),
                // Use the same fixer domain as other storages for consistency
                DataFixTypes.SAVED_DATA_SCOREBOARD
        );
        return PersistentStateUtil.get(world, type);
    }

    private static String storageKey(ResourceLocation addonId) {
        // One state per addon id
        String safePath = addonId.getPath().replace('/', '_');
        return "ra_addon_" + addonId.getNamespace() + "_" + safePath;
    }

    public static AddonPersistentStorage fromNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        return fromNbt(tag, lookup, ResourceLocation.fromNamespaceAndPath("roadarchitect", "unknown"));
    }

    private static AddonPersistentStorage fromNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup, ResourceLocation addonId) {
        AddonPersistentStorage s = new AddonPersistentStorage(addonId);
        ListTag list = tag.getListOrEmpty(ROOT);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            String keyStr = e.getStringOr(KEY, "");
            if (keyStr.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(keyStr);
            if (id == null) continue;
            CompoundTag val = e.getCompoundOrEmpty(VAL);
            s.data.put(id, val.copy());
        }
        return s;
    }


    public CompoundTag writeNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, CompoundTag> en : data.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString(KEY, en.getKey().toString());
            e.put(VAL, en.getValue().copy());
            list.add(e);
        }
        tag.put(ROOT, list);
        return tag;
    }

    public Optional<CompoundTag> get(ResourceLocation key) {
        return Optional.ofNullable(data.get(key)).map(CompoundTag::copy);
    }

    public void put(ResourceLocation key, CompoundTag value) {
        data.put(key, value == null ? new CompoundTag() : value.copy());
        setDirty();
    }

    public void remove(ResourceLocation key) {
        if (data.remove(key) != null) {
            setDirty();
        }
    }

    public Set<ResourceLocation> keys() { return Collections.unmodifiableSet(data.keySet()); }
}
