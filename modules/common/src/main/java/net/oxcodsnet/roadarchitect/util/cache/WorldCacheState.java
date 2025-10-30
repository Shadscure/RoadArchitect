package net.oxcodsnet.roadarchitect.util.cache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable, thread-safe state container for cache-related data that is scoped per world.
 */
public final class WorldCacheState {
    private final CacheStorage storage;
    private final ConcurrentHashMap<Long, ChunkHeightSnapshot> chunkHeights = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<ChunkHeightSnapshot>> chunkComputations = new ConcurrentHashMap<>();
    private final int minWorldY;

    public WorldCacheState(ServerLevel world, CacheStorage storage) {
        this.storage = storage;
        this.minWorldY = world.getMinY();
    }

    public CacheStorage storage() {
        return storage;
    }

    public ConcurrentHashMap<Long, ChunkHeightSnapshot> chunkHeights() {
        return chunkHeights;
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
        ChunkHeightSnapshot snapshot = chunkHeights.get(chunkKey);
        if (snapshot == null) {
            return null;
        }
        int mask = chunkSide - 1;
        int localX = x & mask;
        int localZ = z & mask;
        return snapshot.get(localX, localZ);
    }

    public void putChunkSnapshot(long chunkKey, ChunkHeightSnapshot snapshot) {
        chunkHeights.put(chunkKey, snapshot);
    }

    public void removeChunkSnapshot(long chunkKey) {
        chunkHeights.remove(chunkKey);
        chunkComputations.remove(chunkKey);
    }
}
