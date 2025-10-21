package net.oxcodsnet.roadarchitect.util.cache;

/**
 * Immutable wrapper around a flattened chunk heightmap.
 */
public final class ChunkHeightSnapshot {
    private final int[] heights;
    private final int chunkSide;

    public ChunkHeightSnapshot(int[] heights, int chunkSide) {
        this.heights = heights;
        this.chunkSide = chunkSide;
    }

    public int get(int localX, int localZ) {
        return heights[(localZ * chunkSide) + localX];
    }

    public int chunkSide() {
        return chunkSide;
    }
}
