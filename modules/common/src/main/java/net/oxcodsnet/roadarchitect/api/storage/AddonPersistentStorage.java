package net.oxcodsnet.roadarchitect.api.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
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
        this.addonId = ResourceLocation.tryBuild("roadarchitect", "unknown");
    }

    private AddonPersistentStorage(ResourceLocation addonId) {
        this.addonId = addonId;
    }

    public static AddonPersistentStorage get(ServerLevel world, ResourceLocation addonId) {
        String storageKey = storageKey(addonId);
        return PersistentStateUtil.get(world,
                () -> new AddonPersistentStorage(addonId),
                tag -> fromNbt(tag, addonId),
                storageKey);
    }

    private static String storageKey(ResourceLocation addonId) {
        // One state per addon id
        String safePath = addonId.getPath().replace('/', '_');
        return "ra_addon_" + addonId.getNamespace() + "_" + safePath;
    }

    public static AddonPersistentStorage fromNbt(CompoundTag tag) {
        return fromNbt(tag, ResourceLocation.tryBuild("roadarchitect", "unknown"));
    }

    private static AddonPersistentStorage fromNbt(CompoundTag tag, ResourceLocation addonId) {
        AddonPersistentStorage s = new AddonPersistentStorage(addonId);
        ListTag list = tag.getList(ROOT, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(e.getString(KEY));
            if (id == null) continue;
            CompoundTag val = e.getCompound(VAL);
            s.data.put(id, val.copy());
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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

    public Optional<CompoundTag> get(ResourceLocation key) { return Optional.ofNullable(data.get(key)).map(CompoundTag::copy); }
    public void put(ResourceLocation key, CompoundTag value) { data.put(key, value == null ? new CompoundTag() : value.copy()); }
    public void remove(ResourceLocation key) { data.remove(key); }
    public Set<ResourceLocation> keys() { return Collections.unmodifiableSet(data.keySet()); }
}
