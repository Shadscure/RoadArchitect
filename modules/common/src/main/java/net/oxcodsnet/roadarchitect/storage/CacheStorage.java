package net.oxcodsnet.roadarchitect.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.oxcodsnet.roadarchitect.config.records.CacheSettings;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Runtime cache backed by a region-paged persistent store.
 */
public final class CacheStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/CacheStorage");
    private static final String LEGACY_FILE_NAME = "road_cache.dat";

    private final CacheSettings settings;
    private final Cache<Long, ColumnRecord> runtime;
    private final RegionColumnStore regionStore;
    private final boolean persistHeights;
    private final boolean persistStabilities;
    private final boolean persistBiomes;

    private CacheStorage(ServerLevel world, CacheSettings settings) {
        this.settings = settings;
        this.persistHeights = settings.persistHeights();
        this.persistStabilities = settings.persistStabilities();
        this.persistBiomes = settings.persistBiomes();
        this.regionStore = new RegionColumnStore(
                world,
                persistHeights,
                persistStabilities,
                persistBiomes,
                settings.clampedRegionSize(),
                settings.persistedBudgetBytes()
        );
        long maxWeight = Math.max(settings.runtimeBudgetBytes(), 16L * 1024L * 1024L);
        this.runtime = Caffeine.newBuilder()
                .maximumWeight(maxWeight)
                .weigher((Long key, ColumnRecord value) -> value == null ? 0 : value.weightBytes())
                .recordStats()
                .build();
        quarantineLegacyStore(world);
    }

    public static CacheStorage open(ServerLevel world) {
        CacheSettings settings = RAConfigHolder.get().cache().clampToRuntime();
        return new CacheStorage(world, settings);
    }

    public CacheSettings settings() {
        return settings;
    }

    public Integer getHeight(long key) {
        ColumnRecord record = getOrLoad(key);
        return record.hasHeight() ? record.height() : null;
    }

    public void putHeight(long key, int value) {
        mutate(key, existing -> existing.withHeight(value));
    }

    public void putHeightIfAbsent(long key, int value) {
        mutate(key, existing -> existing.hasHeight() ? existing : existing.withHeight(value));
    }

    public int computeHeightIfAbsent(long key, IntSupplier loader) {
        Integer cached = getHeight(key);
        if (cached != null) {
            return cached;
        }
        int value = loader.getAsInt();
        putHeightIfAbsent(key, value);
        return value;
    }

    public Double getStability(long key) {
        ColumnRecord record = getOrLoad(key);
        return record.hasStability() ? record.stability() : null;
    }

    public double computeStabilityIfAbsent(long key, DoubleSupplier loader) {
        Double cached = getStability(key);
        if (cached != null) {
            return cached;
        }
        double value = loader.getAsDouble();
        mutate(key, existing -> existing.hasStability() ? existing : existing.withStability(value));
        return value;
    }

    public Holder<Biome> getBiome(long key) {
        ColumnRecord record = getOrLoad(key);
        return record.hasBiome() ? record.biome() : null;
    }

    public Holder<Biome> computeBiomeIfAbsent(long key, Supplier<Holder<Biome>> loader) {
        Holder<Biome> cached = getBiome(key);
        if (cached != null) {
            return cached;
        }
        Holder<Biome> value = loader.get();
        if (value != null) {
            mutate(key, existing -> existing.hasBiome() ? existing : existing.withBiome(value));
        }
        return value;
    }

    public void putBiome(long key, Holder<Biome> biome) {
        if (biome == null) {
            return;
        }
        mutate(key, existing -> existing.withBiome(biome));
    }

    public void flush() {
        regionStore.flush();
    }

    public long runtimeWeightBytes() {
        return runtime.policy().eviction()
                .map(eviction -> eviction.weightedSize().orElse(0L))
                .orElseGet(() -> runtime.estimatedSize() * 32L);
    }

    public long regionWeightBytes() {
        return regionStore.weightBytes();
    }

    private ColumnRecord getOrLoad(long key) {
        ColumnRecord record = runtime.getIfPresent(key);
        if (record != null) {
            return record;
        }
        ColumnRecord persisted = regionStore.read(key);
        if (persisted != null && !persisted.isEmpty()) {
            runtime.put(key, merge(ColumnRecord.EMPTY, persisted));
            return persisted;
        }
        return ColumnRecord.EMPTY;
    }

    private ColumnRecord merge(ColumnRecord base, ColumnRecord persisted) {
        ColumnRecord merged = base;
        if (persisted.hasHeight() && !base.hasHeight()) {
            merged = merged.withHeight(persisted.height());
        }
        if (persisted.hasStability() && !base.hasStability()) {
            merged = merged.withStability(persisted.stability());
        }
        if (persisted.hasBiome() && !base.hasBiome()) {
            merged = merged.withBiome(persisted.biome());
        }
        return merged;
    }

    private void mutate(long key, java.util.function.UnaryOperator<ColumnRecord> operator) {
        ConcurrentMap<Long, ColumnRecord> map = runtime.asMap();
        map.compute(key, (k, existing) -> {
            ColumnRecord base = existing == null ? ColumnRecord.EMPTY : existing;
            ColumnRecord updated = operator.apply(base);
            if (updated == null || updated.isEmpty()) {
                if (!base.isEmpty()) {
                    regionStore.write(k, ColumnRecord.EMPTY);
                }
                return null;
            }
            if (!Objects.equals(base.height(), updated.height())
                    || !Objects.equals(base.stability(), updated.stability())
                    || !Objects.equals(base.biome(), updated.biome())) {
                boolean shouldPersist =
                        (persistHeights && !Objects.equals(base.height(), updated.height()))
                        || (persistStabilities && !Objects.equals(base.stability(), updated.stability()))
                        || (persistBiomes && !Objects.equals(base.biome(), updated.biome()));
                if (shouldPersist) {
                    regionStore.write(k, updated);
                }
            }
            return updated;
        });
    }

    private void quarantineLegacyStore(ServerLevel world) {
        Path dataDir = DimensionPaths.resolveDimensionRoot(world).resolve("data");
        Path legacy = dataDir.resolve(LEGACY_FILE_NAME);
        if (!Files.exists(legacy)) {
            return;
        }
        Path backup = legacy.resolveSibling(LEGACY_FILE_NAME + ".legacy");
        try {
            Files.createDirectories(backup.getParent());
            Files.move(legacy, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Legacy cache file '{}' was found and moved to '{}'. The new cache backend will rebuild data on demand.",
                    legacy, backup);
        } catch (IOException e) {
            LOGGER.error("Failed to move legacy cache file {}", legacy, e);
        }
    }
}
