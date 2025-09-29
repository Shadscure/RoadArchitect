package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.util.NbtUtils;

public class CacheStorage extends PersistentState {
    private static final String KEY = "road_cache";
    private static final String HEIGHTS_KEY = "heights";
    private static final String STABILITIES_KEY = "stabilities";
    private static final String BIOMES_KEY = "biomes";
    private static final String ENTRY_KEY = "k";
    private static final String ENTRY_VALUE = "v";

    private final ConcurrentMap<Long, Integer> heights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> stabilities = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RegistryEntry<Biome>> cachedBiomes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Identifier> biomeIds = new ConcurrentHashMap<>();

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
        Map<Long, String> stored = new java.util.HashMap<>(bList.size());
        NbtUtils.fillLongStringMap(bList, stored);
        for (Map.Entry<Long, String> entry : stored.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getValue());
            if (id != null) {
                storage.biomeIds.put(entry.getKey(), id);
            }
        }
        return storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag.put(HEIGHTS_KEY, NbtUtils.toLongIntList(heights));

        tag.put(STABILITIES_KEY, NbtUtils.toLongDoubleList(stabilities));

        java.util.HashMap<Long, String> toWrite = new java.util.HashMap<>(biomeIds.size());
        for (Map.Entry<Long, Identifier> entry : biomeIds.entrySet()) {
            toWrite.put(entry.getKey(), entry.getValue().toString());
        }
        // include any cached entries missing from biomeIds
        for (Map.Entry<Long, RegistryEntry<Biome>> entry : cachedBiomes.entrySet()) {
            Identifier id = entry.getValue().getKey().map(RegistryKey::getValue).orElse(null);
            if (id != null) {
                toWrite.put(entry.getKey(), id.toString());
            }
        }
        tag.put(BIOMES_KEY, NbtUtils.toLongStringList(toWrite));
        return tag;
    }

    public ConcurrentMap<Long, Integer> heights() {
        return heights;
    }

    public ConcurrentMap<Long, Double> stabilities() {
        return stabilities;
    }

    public void putBiome(long key, RegistryEntry<Biome> biome) {
        cachedBiomes.put(key, biome);
        biome.getKey().map(RegistryKey::getValue).ifPresent(id -> biomeIds.put(key, id));
        markDirty();
    }

    public RegistryEntry<Biome> getBiome(ServerWorld world, long key, Supplier<RegistryEntry<Biome>> loader) {
        return cachedBiomes.computeIfAbsent(key, k -> {
            RegistryEntry<Biome> resolved = resolveBiome(world.getRegistryManager(), biomeIds.get(k));
            if (resolved == null) {
                resolved = loader.get();
            }
            resolved.getKey().map(RegistryKey::getValue).ifPresentOrElse(
                    id -> biomeIds.put(k, id),
                    () -> biomeIds.remove(k)
            );
            markDirty();
            return resolved;
        });
    }

    private void attachWorld(DynamicRegistryManager registryManager) {
        // attempt to hydrate cached biomes lazily so they are available immediately
        biomeIds.forEach((key, id) -> {
            cachedBiomes.computeIfAbsent(key, k -> resolveBiome(registryManager, id));
        });
    }

    private RegistryEntry<Biome> resolveBiome(DynamicRegistryManager registryManager, Identifier id) {
        if (id == null) {
            return null;
        }
        return registryManager.get(RegistryKeys.BIOME)
                .getEntry(RegistryKey.of(RegistryKeys.BIOME, id))
                .orElse(null);
    }
}
