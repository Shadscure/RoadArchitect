package net.oxcodsnet.roadarchitect.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Region-paged persistence layer for cached column data.
 */
final class RegionColumnStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/RegionColumnStore");
    private static final String LIST_KEY = "columns";
    private static final String VERSION_KEY = "version";
    private static final int FORMAT_VERSION = 1;

    private final Path regionDir;
    private final HolderLookup.RegistryLookup<Biome> biomeRegistry;
    private final boolean persistHeights;
    private final boolean persistStabilities;
    private final boolean persistBiomes;
    private final int regionSizeChunks;
    private final Cache<Long, RegionData> regions;
    private final long budgetBytes;

    RegionColumnStore(ServerLevel world,
                      boolean persistHeights,
                      boolean persistStabilities,
                      boolean persistBiomes,
                      int regionSizeChunks,
                      long persistedBudgetBytes) {
        this.regionDir = DimensionPaths.resolveCacheDirectory(world);
        this.biomeRegistry = world.registryAccess().lookupOrThrow(Registries.BIOME);
        this.persistHeights = persistHeights;
        this.persistStabilities = persistStabilities;
        this.persistBiomes = persistBiomes;
        this.regionSizeChunks = Math.max(4, regionSizeChunks);
        this.budgetBytes = Math.max(persistedBudgetBytes, 32L * 1024L * 1024L);
        long maxWeight = this.budgetBytes;
        this.regions = Caffeine.newBuilder()
                .maximumWeight(maxWeight)
                .weigher((Long key, RegionData value) -> value.weightBytes())
                .removalListener(this::onRegionRemoval)
                .build();
    }

    ColumnRecord read(long columnKey) {
        RegionData region = regions.get(regionKey(columnKey), this::loadRegion);
        return region.get(columnKey);
    }

    void write(long columnKey, ColumnRecord record) {
        long regionKey = regionKey(columnKey);
        RegionData region = regions.get(regionKey, this::loadRegion);
        ColumnRecord persisted = filterPersisted(record);
        region.put(columnKey, persisted);
    }

    void flush() {
        regions.asMap().forEach(this::flushRegion);
    }

    long weightBytes() {
        return regions.policy().eviction()
                .map(eviction -> eviction.weightedSize().orElse(0L))
                .orElseGet(() -> Math.max(1L, regions.estimatedSize()) * 512L);
    }

    private void onRegionRemoval(Long key, RegionData value, RemovalCause cause) {
        if (key == null || value == null) {
            return;
        }
        flushRegion(key, value);
    }

    private RegionData loadRegion(long regionKey) {
        Path path = regionPath(regionKey);
        RegionData data = new RegionData();
        if (!Files.exists(path)) {
            return data;
        }
        try (var in = Files.newInputStream(path)) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            if (tag == null) {
                return data;
            }
            ListTag list = tag.getList(LIST_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                long columnKey = entry.getLong("k");
                Integer height = persistHeights && entry.contains("h", Tag.TAG_INT) ? entry.getInt("h") : null;
                Double stability = persistStabilities && entry.contains("s", Tag.TAG_DOUBLE)
                        ? entry.getDouble("s") : null;
                Holder<Biome> biome = null;
                if (persistBiomes && entry.contains("b", Tag.TAG_STRING)) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getString("b"));
                    if (id != null) {
                        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
                        biome = biomeRegistry.get(key).orElse(null);
                    }
                }
                ColumnRecord record = new ColumnRecord(height, stability, biome);
                if (!record.isEmpty()) {
                    data.putLoaded(columnKey, record);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load cached region {}", path, e);
        }
        data.markClean();
        return data;
    }

    private void flushRegion(long regionKey, RegionData region) {
        Long2ObjectMap<ColumnRecord> snapshot = region.snapshotIfDirty();
        if (snapshot == null) {
            return;
        }
        Path path = regionPath(regionKey);
        if (snapshot.isEmpty()) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete empty cache region {}", path, e);
            } finally {
                region.markClean();
            }
            return;
        }

        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_KEY, FORMAT_VERSION);
        ListTag list = new ListTag();
        for (Long2ObjectMap.Entry<ColumnRecord> entry : snapshot.long2ObjectEntrySet()) {
            ColumnRecord record = entry.getValue();
            if (record == null || record.isEmpty()) {
                continue;
            }
            CompoundTag columnTag = new CompoundTag();
            columnTag.putLong("k", entry.getLongKey());
            if (persistHeights && record.hasHeight()) {
                columnTag.putInt("h", record.height());
            }
            if (persistStabilities && record.hasStability()) {
                columnTag.putDouble("s", record.stability());
            }
            if (persistBiomes && record.hasBiome()) {
                record.biome().unwrapKey().map(ResourceKey::location).ifPresent(id -> columnTag.putString("b", id.toString()));
            }
            list.add(columnTag);
        }
        root.put(LIST_KEY, list);

        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try (var out = Files.newOutputStream(tmp)) {
            NbtIo.writeCompressed(root, out);
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Failed to write cache region {}", path, e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
            return;
        }
        region.markClean();
    }

    private ColumnRecord filterPersisted(ColumnRecord record) {
        if (record == null || record.isEmpty()) {
            return ColumnRecord.EMPTY;
        }
        Integer height = persistHeights && record.hasHeight() ? record.height() : null;
        Double stability = persistStabilities && record.hasStability() ? record.stability() : null;
        Holder<Biome> biome = persistBiomes && record.hasBiome() ? record.biome() : null;
        if (Objects.equals(height, record.height())
                && Objects.equals(stability, record.stability())
                && Objects.equals(biome, record.biome())) {
            return record;
        }
        return new ColumnRecord(height, stability, biome);
    }

    private long regionKey(long columnKey) {
        int blockX = (int) (columnKey >> 32);
        int blockZ = (int) columnKey;
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int regionX = Math.floorDiv(chunkX, regionSizeChunks);
        int regionZ = Math.floorDiv(chunkZ, regionSizeChunks);
        return ((long) regionX << 32) | (regionZ & 0xFFFF_FFFFL);
    }

    private Path regionPath(long regionKey) {
        int regionX = (int) (regionKey >> 32);
        int regionZ = (int) regionKey;
        String fileName = "region_" + regionX + "_" + regionZ + ".nbt";
        return regionDir.resolve(fileName);
    }

    private static final class RegionData {
        private final Long2ObjectMap<ColumnRecord> columns = new Long2ObjectOpenHashMap<>();
        private boolean dirty;

        synchronized ColumnRecord get(long key) {
            return columns.get(key);
        }

        synchronized void put(long key, ColumnRecord record) {
            if (record == null || record.isEmpty()) {
                if (columns.remove(key) != null) {
                    dirty = true;
                }
                return;
            }
            ColumnRecord previous = columns.put(key, record);
            if (!record.equals(previous)) {
                dirty = true;
            }
        }

        synchronized void putLoaded(long key, ColumnRecord record) {
            if (record == null || record.isEmpty()) {
                return;
            }
            columns.put(key, record);
        }

        synchronized void markClean() {
            dirty = false;
        }

        synchronized Long2ObjectMap<ColumnRecord> snapshotIfDirty() {
            if (!dirty) {
                return null;
            }
            return new Long2ObjectOpenHashMap<>(columns);
        }

        synchronized int weightBytes() {
            return Math.max(1, columns.size()) * 64;
        }
    }
}
