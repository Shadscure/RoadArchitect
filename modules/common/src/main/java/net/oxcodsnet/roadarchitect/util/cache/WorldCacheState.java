package net.oxcodsnet.roadarchitect.util.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.oxcodsnet.roadarchitect.config.records.CacheSettings;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable, thread-safe state container for cache-related data that is scoped per world.
 */
public final class WorldCacheState {
    private final CacheStorage storage;
    private final Cache<Long, ChunkHeightSnapshot> chunkSnapshots;
    private final ConcurrentHashMap<Long, CompletableFuture<ChunkHeightSnapshot>> chunkComputations = new ConcurrentHashMap<>();
    private final int minWorldY;

    public WorldCacheState(ServerLevel world, CacheStorage storage, CacheSettings settings) {
        this.storage = storage;
        this.minWorldY = world.getMinY();
        long maxWeight = Math.max(settings.snapshotBudgetBytes(), 16L * 1024L * 1024L);
        this.chunkSnapshots = Caffeine.newBuilder()
                .maximumWeight(maxWeight)
                .weigher((Long key, ChunkHeightSnapshot snapshot) -> snapshot == null ? 0 : snapshot.weightBytes())
                .build();
    }

    public CacheStorage storage() {
        return storage;
    }

    public ConcurrentHashMap<Long, CompletableFuture<ChunkHeightSnapshot>> chunkComputations() {
        return chunkComputations;
    }

    public int minWorldY() {
        return minWorldY;
    }

    public Integer lookupHeight(long key, int chunkSide) {
        int x = (int) (key >> 32);
        int z = (int) key;
        long chunkKey = ChunkPos.asLong(x >> 4, z >> 4);
        ChunkHeightSnapshot snapshot = chunkSnapshots.getIfPresent(chunkKey);
        if (snapshot == null) {
            return null;
        }
        int mask = chunkSide - 1;
        int localX = x & mask;
        int localZ = z & mask;
        return snapshot.get(localX, localZ);
    }

    public void putChunkSnapshot(long chunkKey, ChunkHeightSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        chunkSnapshots.put(chunkKey, snapshot);
    }

    public ChunkHeightSnapshot getChunkSnapshot(long chunkKey) {
        return chunkSnapshots.getIfPresent(chunkKey);
    }

    public void removeChunkSnapshot(long chunkKey) {
        chunkSnapshots.invalidate(chunkKey);
        chunkComputations.remove(chunkKey);
    }

    public long snapshotWeightBytes() {
        return chunkSnapshots.policy().eviction()
                .map(eviction -> eviction.weightedSize().orElse(0L))
                .orElse(0L);
    }

    public void flush() {
        storage.flush();
        chunkSnapshots.invalidateAll();
    }
}
