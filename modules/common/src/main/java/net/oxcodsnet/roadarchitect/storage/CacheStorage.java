package net.oxcodsnet.roadarchitect.storage;

import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.oxcodsnet.roadarchitect.util.NbtUtils;

public class CacheStorage extends SavedData {
    private static final String KEY = "road_cache";
    private static final String HEIGHTS_KEY = "heights";
    private static final String STABILITIES_KEY = "stabilities";
    private static final String BIOMES_KEY = "biomes";
    private static final String ENTRY_KEY = "k";
    private static final String ENTRY_VALUE = "v";

    public static final Factory<CacheStorage> TYPE = new Factory<>(CacheStorage::new, CacheStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    private final ConcurrentMap<Long, Integer> heights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> stabilities = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Holder<Biome>> biomes = new ConcurrentHashMap<>();

    public static CacheStorage get(ServerLevel world) {
        return PersistentStateUtil.get(world, TYPE, KEY);
    }

    public static CacheStorage fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
        CacheStorage storage = new CacheStorage();
        ListTag hList = tag.getList(HEIGHTS_KEY, Tag.TAG_COMPOUND);
        NbtUtils.fillLongIntMap(hList, storage.heights);

        ListTag sList = tag.getList(STABILITIES_KEY, Tag.TAG_COMPOUND);
        NbtUtils.fillLongDoubleMap(sList, storage.stabilities);
        ListTag bList = tag.getList(BIOMES_KEY, Tag.TAG_COMPOUND);
        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(bList.size());
        NbtUtils.fillLongStringMap(bList, biomeIds);
        HolderLookup.RegistryLookup<Biome> registry = lookup.lookupOrThrow(Registries.BIOME);
        for (java.util.Map.Entry<Long, String> e : biomeIds.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getValue());
            if (id == null) continue;
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
            registry.get(key).ifPresent(entry -> storage.biomes.put(e.getKey(), entry));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.put(HEIGHTS_KEY, NbtUtils.toLongIntList(heights));

        tag.put(STABILITIES_KEY, NbtUtils.toLongDoubleList(stabilities));

        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(biomes.size());
        for (Map.Entry<Long, Holder<Biome>> entry : biomes.entrySet()) {
            ResourceLocation id = entry.getValue().unwrapKey().map(ResourceKey::location).orElse(null);
            if (id != null) {
                biomeIds.put(entry.getKey(), id.toString());
            }
        }
        tag.put(BIOMES_KEY, NbtUtils.toLongStringList(biomeIds));
        return tag;
    }

    public ConcurrentMap<Long, Integer> heights() {
        return heights;
    }

    public ConcurrentMap<Long, Double> stabilities() {
        return stabilities;
    }

    public ConcurrentMap<Long, Holder<Biome>> biomes() {
        return biomes;
    }
}
