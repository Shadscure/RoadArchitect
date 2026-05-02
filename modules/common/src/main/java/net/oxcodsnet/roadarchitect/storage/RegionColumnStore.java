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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

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
        cleanOrphanedTempFiles(this.regionDir);
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
        if (!Files.exists(path)) {
            return new RegionData();
        }
        RegionData data = new RegionData();
        try (var in = Files.newInputStream(path)) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            if (tag == null) {
                data.markClean();
                return data;
            }
            tag.getList(LIST_KEY).ifPresent(list -> {
                for (int i = 0; i < list.size(); i++) {
                    list.getCompound(i).ifPresent(entry -> {
                        Long columnKeyBoxed = entry.getLong("k").orElse(null);
                        if (columnKeyBoxed == null) {
                            return;
                        }
                        long columnKey = columnKeyBoxed;
                        Integer height = persistHeights ? entry.getInt("h").orElse(null) : null;
                        Double stability = persistStabilities ? entry.getDouble("s").orElse(null) : null;
                        Holder<Biome> biome = null;
                        if (persistBiomes) {
                            String biomeId = entry.getString("b").orElse(null);
                            if (biomeId != null) {
                                ResourceLocation id = ResourceLocation.tryParse(biomeId);
                                if (id != null) {
                                    ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
                                    biome = biomeRegistry.get(key).orElse(null);
                                }
                            }
                        }
                        ColumnRecord record = new ColumnRecord(height, stability, biome);
                        if (!record.isEmpty()) {
                            data.putLoaded(columnKey, record);
                        }
                    });
                }
            });
            data.markClean();
            return data;
        } catch (IOException | RuntimeException e) {
            // Corrupt or unreadable region: a partial RegionData would otherwise be
            // cached and (since it stays dirty=false) silently overwrite the file
            // on the next flush, propagating the corruption forever.
            LOGGER.warn("Cached region {} is corrupted, quarantining and rebuilding from scratch", path, e);
            quarantineCorruptedRegion(path);
            return new RegionData();
        }
    }

    /**
     * Sweeps stray {@code region_X_Z.nbt.tmp.*} files left over from a
     * previous JVM that crashed mid-write. Safe to run once at startup —
     * by definition no other thread is touching the cache directory yet.
     */
    static void cleanOrphanedTempFiles(Path regionDir) {
        if (regionDir == null || !Files.isDirectory(regionDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(regionDir)) {
            stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("region_") && name.contains(".nbt.tmp");
                    })
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                            LOGGER.debug("Removed orphaned cache temp file {}", p);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to remove orphaned cache temp file {}", p, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to scan cache directory for orphaned temp files: {}", regionDir, e);
        }
    }

    static void quarantineCorruptedRegion(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (!Files.exists(path)) {
                return;
            }
            Path target = path.resolveSibling(path.getFileName().toString() + ".corrupt-" + System.currentTimeMillis());
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Quarantined corrupted region {} -> {}", path, target);
        } catch (IOException e) {
            LOGGER.error("Failed to quarantine corrupted region {}, attempting delete", path, e);
            try {
                Files.deleteIfExists(path);
            } catch (IOException deleteException) {
                LOGGER.error("Failed to delete corrupted region {}", path, deleteException);
            }
        }
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

        // Buffer the GZIP'ed NBT in memory so we can fsync the on-disk file
        // before atomically renaming it. Without force(true) the rename can
        // succeed while the data block is still in the page cache — a kill -9
        // or power loss then leaves a non-empty file with garbage payload,
        // surfacing as ZipException("invalid stored block lengths") on the
        // next read.
        byte[] payload;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
            NbtIo.writeCompressed(root, buffer);
            payload = buffer.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Failed to serialize cache region {}", path, e);
            return;
        }

        // Unique tmp suffix per call: Caffeine's removalListener runs on
        // ForkJoinPool.commonPool, so the same regionKey can be flushed by
        // two workers concurrently. With a shared "${name}.tmp" filename
        // the second worker's ATOMIC_MOVE failed with NoSuchFileException
        // because the first one already consumed the file. UUID isolates
        // workers; the orphan cleanup at construction time picks up any
        // file the JVM died on.
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp." + UUID.randomUUID());
        try (FileChannel channel = FileChannel.open(
                tmp,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer view = ByteBuffer.wrap(payload);
            while (view.hasRemaining()) {
                channel.write(view);
            }
            channel.force(true);
        } catch (IOException e) {
            LOGGER.error("Failed to write cache region {}", path, e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
            return;
        }

        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Failed to atomically move cache region {} -> {}", tmp, path, e);
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
            // Approx. per-entry footprint: long key (8) + ColumnRecord (height 4 +
            // stability 8 + biome Holder ~16) + Long2ObjectMap.Entry overhead (~24)
            // + ResourceLocation string accounted in serialized form (~60).
            // 64 b/entry was a wild underestimate — Caffeine's maximumWeight then
            // failed to evict regions, letting the cache grow unbounded.
            return Math.max(1, columns.size()) * 128;
        }
    }
}
