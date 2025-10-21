package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.oxcodsnet.roadarchitect.util.NbtUtils;

public class CacheStorage extends PersistentState {
    private static final String KEY = "road_cache";
    private static final String HEIGHTS_KEY = "heights";
    private static final String STABILITIES_KEY = "stabilities";
    private static final String BIOMES_KEY = "biomes";
    private final ConcurrentMap<Long, Integer> heights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> stabilities = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RegistryEntry<Biome>> biomes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> pendingBiomeIds = new ConcurrentHashMap<>();

    public static CacheStorage get(ServerWorld world) {
        CacheStorage storage = PersistentStateUtil.get(world, CacheStorage::new, CacheStorage::fromNbt, KEY);
        storage.attachWorld(world.getRegistryManager());
        return storage;
    }

    public static CacheStorage fromNbt(NbtCompound tag) {
        CacheStorage storage = new CacheStorage();
        NbtList hList = tag.getList(HEIGHTS_KEY, NbtElement.COMPOUND_TYPE);
        NbtUtils.fillLongIntMap(hList, storage.heights);

        NbtList sList = tag.getList(STABILITIES_KEY, NbtElement.COMPOUND_TYPE);
        NbtUtils.fillLongDoubleMap(sList, storage.stabilities);
        NbtList bList = tag.getList(BIOMES_KEY, NbtElement.COMPOUND_TYPE);
        NbtUtils.fillLongStringMap(bList, storage.pendingBiomeIds);
        return storage;
    }

    public NbtCompound writeNbt(NbtCompound tag) {
        tag.put(HEIGHTS_KEY, NbtUtils.toLongIntList(heights));

        tag.put(STABILITIES_KEY, NbtUtils.toLongDoubleList(stabilities));

        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(biomes.size() + pendingBiomeIds.size());
        for (Map.Entry<Long, RegistryEntry<Biome>> entry : biomes.entrySet()) {
            Identifier id = entry.getValue().getKey().map(RegistryKey::getValue).orElse(null);
            if (id != null) {
                biomeIds.put(entry.getKey(), id.toString());
            }
        }
        pendingBiomeIds.forEach(biomeIds::putIfAbsent);
        tag.put(BIOMES_KEY, NbtUtils.toLongStringList(biomeIds));
        return tag;
    }

    public synchronized void attachWorld(DynamicRegistryManager manager) {
        net.minecraft.registry.Registry<Biome> registry = manager.get(RegistryKeys.BIOME);
        if (registry == null) {
            return;
        }
        java.util.Iterator<Map.Entry<Long, String>> iterator = pendingBiomeIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, String> entry = iterator.next();
            Identifier id = Identifier.tryParse(entry.getValue());
            if (id == null) {
                continue;
            }
            RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, id);
            registry.getEntry(key).ifPresent(registryEntry -> biomes.put(entry.getKey(), registryEntry));
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

    public ConcurrentMap<Long, RegistryEntry<Biome>> biomes() {
        return biomes;
    }
}
