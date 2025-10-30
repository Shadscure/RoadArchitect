package net.oxcodsnet.roadarchitect.storage;

import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.oxcodsnet.roadarchitect.util.NbtUtils;

public class CacheStorage extends SavedData {
    private static final String KEY = "road_cache";
    private static final String HEIGHTS_KEY = "heights";
    private static final String STABILITIES_KEY = "stabilities";
    private static final String BIOMES_KEY = "biomes";
    private final ConcurrentMap<Long, Integer> heights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> stabilities = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Holder<Biome>> biomes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> pendingBiomeIds = new ConcurrentHashMap<>();

    public static CacheStorage get(ServerLevel world) {
        CacheStorage storage = PersistentStateUtil.get(world, CacheStorage::new, CacheStorage::fromNbt, KEY);
        storage.attachWorld(world.registryAccess());
        return storage;
    }

    public static CacheStorage fromNbt(CompoundTag tag) {
        CacheStorage storage = new CacheStorage();
        ListTag hList = tag.getList(HEIGHTS_KEY, Tag.TAG_COMPOUND);
        NbtUtils.fillLongIntMap(hList, storage.heights);

        ListTag sList = tag.getList(STABILITIES_KEY, Tag.TAG_COMPOUND);
        NbtUtils.fillLongDoubleMap(sList, storage.stabilities);
        ListTag bList = tag.getList(BIOMES_KEY, Tag.TAG_COMPOUND);
        NbtUtils.fillLongStringMap(bList, storage.pendingBiomeIds);
        return storage;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.put(HEIGHTS_KEY, NbtUtils.toLongIntList(heights));

        tag.put(STABILITIES_KEY, NbtUtils.toLongDoubleList(stabilities));

        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(biomes.size() + pendingBiomeIds.size());
        for (Map.Entry<Long, Holder<Biome>> entry : biomes.entrySet()) {
            ResourceLocation id = entry.getValue().unwrapKey().map(ResourceKey::location).orElse(null);
            if (id != null) {
                biomeIds.put(entry.getKey(), id.toString());
            }
        }
        pendingBiomeIds.forEach(biomeIds::putIfAbsent);
        tag.put(BIOMES_KEY, NbtUtils.toLongStringList(biomeIds));
        return tag;
    }

    public synchronized void attachWorld(RegistryAccess manager) {
        net.minecraft.core.Registry<Biome> registry = manager.registryOrThrow(Registries.BIOME);
        if (registry == null) {
            return;
        }
        java.util.Iterator<Map.Entry<Long, String>> iterator = pendingBiomeIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, String> entry = iterator.next();
            ResourceLocation id = ResourceLocation.tryParse(entry.getValue());
            if (id == null) {
                continue;
            }
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
            registry.getHolder(key).ifPresent(registryEntry -> biomes.put(entry.getKey(), registryEntry));
            if (biomes.containsKey(entry.getKey())) {
                iterator.remove();
            }
        }
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
