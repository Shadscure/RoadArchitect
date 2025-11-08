v1.6.2 — <em>Cache stack overhaul</em>

### Highlights
- 🧠 **Tiered cache backend**: Replaced the old SavedData maps with a WTinyLFU+Caffeine runtime cache backed by paged region stores per dimension. Chunk snapshots now live in their own cache with separate budgets, prefill works chunk-by-chunk with budget guards, and legacy `road_cache.dat` files are quarantined automatically while data is rebuilt on demand.
- 🛠️ **Dedicated cache controls**: Added a Cache tab to the config on all platforms covering runtime/snapshot/persisted RAM budgets, region page size, async prefill limits, and per-dataset persistence flags. Every budget is clamped to at most 45% of the JVM heap (never below 64 MB) before any cache starts so memory usage stays predictable.
- 🧾 **Debug visibility**: Two new toggles, `enableCacheLogs` and `showCacheStatsOverlay`, gate the cache-specific log spam and the new F3 overlay (HudRenderCallback / RenderGuiOverlayEvent.Post) rendered by `CacheDebugOverlayRenderer`, showing runtime/snapshot/page usage, limits, and prefill status in real time.

Compatibility: No breaking world changes; caches migrate automatically and legacy cache files are moved aside safely.
