v1.6.4 — <em>Config & performance toggles</em>

### Added
- 🎛️ **Hide the scanning progress bar.** New *Debug → Show Scanning Bar* toggle removes the road scanning overlay from the world loading screen for players who find it noisy. On by default.
- 🗺️ **Disable the road graph debug map.** New *Debug → Enable Debug Map* toggle gates the keybind that opens the in-game graph view. On by default; turn it off if you don't want the map (or its draw cost) at all.
- ⚙️ **Choose how many cores RoadArchitect can use.** New *Debug → Async Worker Threads* setting controls the size of the background worker pool. `0` keeps the auto-sized default (`CPU cores − 2`, leaving headroom for the OS, the main game thread, and vanilla worldgen workers). Setting an explicit number above 0 caps it at that value. Useful for capping RA on shared servers or under Distant Horizons-heavy setups. Requires a game restart.

---

v1.6.3 — <em>Cache stability hotfix</em>

### Fixes
- 🛡️ **Corrupted cache files no longer poison your world.** If a region cache file ever ends up unreadable (after a server kill, disk hiccup, or any other failure), it is now safely set aside and the cache rebuilds from scratch — instead of silently spreading the damage across new writes.
- 💾 **Hardened cache writes.** Cache files are now flushed to disk before being committed, so a server kill or power loss while saving no longer leaves you with a `ZipException: invalid stored block lengths` on the next launch.
- 🚀 **Memory budget actually respected.** The cache previously underestimated its own size and could keep growing past the configured RAM limit, dragging TPS down. The accounting is fixed — the limit you set in the Cache config now reflects reality.

### Tuning
- ⚖️ **Smarter default RAM split.** Default Cache budgets retuned for typical 2–4 GB heap setups: runtime 256 → 128 MiB, snapshots 64 → 32 MiB, pages 128 → 192 MiB. Total drops from 448 to 352 MiB while the main page cache grows by 50 % — hot regions stay in RAM instead of being read from disk on every chunk load. Existing configs are untouched; only fresh installs pick up the new values (use Cache → Reset to defaults if you want them).
- ⚡ **No more multi-second freezes when loading into a world.** Cache writes triggered by chunk loads are now handled on a background worker. Returning to a world with hundreds of chunks streaming in at once no longer stalls the server tick for seconds while region files decompress.

Compatibility: existing caches are read as before, no world migration needed.

---

v1.6.2 — <em>Cache stack overhaul</em>

### Highlights
- 🧠 **Tiered cache backend**: Replaced the old SavedData maps with a WTinyLFU+Caffeine runtime cache backed by paged region stores per dimension. Chunk snapshots now live in their own cache with separate budgets, prefill works chunk-by-chunk with budget guards, and legacy `road_cache.dat` files are quarantined automatically while data is rebuilt on demand.
- 🛠️ **Dedicated cache controls**: Added a Cache tab to the config on all platforms covering runtime/snapshot/persisted RAM budgets, region page size, async prefill limits, and per-dataset persistence flags. Every budget is clamped to at most 65% of the JVM heap (never below 64 MB) before any cache starts so memory usage stays predictable.
- 🧾 **Debug visibility**: Two new toggles, `enableCacheLogs` and `showCacheStatsOverlay`, gate the cache-specific log spam and the new F3 overlay (HudRenderCallback / RenderGuiOverlayEvent.Post) rendered by `CacheDebugOverlayRenderer`, showing runtime/snapshot/page usage, limits, and prefill status in real time.

Compatibility: No breaking world changes; caches migrate automatically and legacy cache files are moved aside safely.
