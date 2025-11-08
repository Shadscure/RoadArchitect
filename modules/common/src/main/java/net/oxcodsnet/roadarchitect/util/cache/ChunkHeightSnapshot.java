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

    public int columns() {
        return heights.length;
    }

    public int weightBytes() {
        return Math.max(1, heights.length) * Integer.BYTES + 32;
    }
}
