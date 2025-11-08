package net.oxcodsnet.roadarchitect.config;

/**
 * Tunable cache parameters shared between runtime caches and persistence layer.
 *
 * @param runtimeBudgetMb    RAM budget for column-level runtime cache (in megabytes).
 * @param snapshotBudgetMb   RAM budget allocated for chunk snapshot cache (in megabytes).
 * @param persistedBudgetMb  RAM budget for region/page cache that buffers on-disk data (in megabytes).
 * @param regionSizeChunks   Edge length of a region page (in chunks).
 * @param enablePrefill      Whether asynchronous prefill is allowed to run.
 * @param prefillMaxChunks   Maximum number of chunks prefilling may touch per request.
 * @param persistHeights     Whether height columns are persisted between server restarts.
 * @param persistStabilities Whether stability metrics are persisted.
 * @param persistBiomes      Whether biome lookups are persisted.
 */
public record CacheSettings(
        int runtimeBudgetMb,
        int snapshotBudgetMb,
        int persistedBudgetMb,
        int regionSizeChunks,
        boolean enablePrefill,
        int prefillMaxChunks,
        boolean persistHeights,
        boolean persistStabilities,
        boolean persistBiomes
) {
    public static final CacheSettings DEFAULT = new CacheSettings(
            256,
            64,
            128,
            32,
            true,
            2048,
            true,
            true,
            true
    );

    public long runtimeBudgetBytes() {
        return megabytesToBytes(runtimeBudgetMb);
    }

    public long snapshotBudgetBytes() {
        return megabytesToBytes(snapshotBudgetMb);
    }

    public long persistedBudgetBytes() {
        return megabytesToBytes(persistedBudgetMb);
    }

    public int clampedRegionSize() {
        return Math.max(4, regionSizeChunks);
    }

    public int clampedPrefillMaxChunks() {
        return Math.max(0, prefillMaxChunks);
    }

    private static long megabytesToBytes(int value) {
        long safe = Math.max(1, value);
        return safe * 1024L * 1024L;
    }
}
