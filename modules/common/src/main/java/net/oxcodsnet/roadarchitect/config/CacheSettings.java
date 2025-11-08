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
    private static final double HEAP_FRACTION = 0.45;
    private static final int MIN_CACHE_MB = 16;

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

    public CacheSettings clampToRuntime() {
        long heapBytes = Runtime.getRuntime().maxMemory();
        long heapMb = Math.max(128, heapBytes / (1024L * 1024L));
        long capMb = Math.max(64, (long) Math.floor(heapMb * HEAP_FRACTION));
        return new CacheSettings(
                clampBudget(runtimeBudgetMb, capMb),
                clampBudget(snapshotBudgetMb, capMb),
                clampBudget(persistedBudgetMb, capMb),
                clampedRegionSize(),
                enablePrefill,
                clampedPrefillMaxChunks(),
                persistHeights,
                persistStabilities,
                persistBiomes
        );
    }

    private static int clampBudget(int configuredMb, long capMb) {
        int sanitized = Math.max(MIN_CACHE_MB, configuredMb);
        long clamped = Math.min(sanitized, capMb);
        if (clamped > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) clamped;
    }

    private static long megabytesToBytes(int value) {
        long safe = Math.max(1, value);
        return safe * 1024L * 1024L;
    }
}
