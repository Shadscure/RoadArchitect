v1.6.6 — <em>Feature parity with the 1.21.x line</em>

This release brings the 1.20.1 build up to the same feature surface the 1.21.x branches have shipped over v1.6.2-v1.6.6 — same cache rework, same UX toggles, same diagnostic overlay. Existing v1.6.1 saves and configs keep working.

### Cache (foundation rework)
- 🛡️ **Corrupted cache files no longer poison your world.** If a region cache file ever ends up unreadable (after a server kill, disk hiccup, or any other failure), it is now safely set aside as `region_*.nbt.corrupt-{ts}` and the cache rebuilds from scratch — instead of silently spreading the damage across new writes.
- 💾 **Hardened cache writes.** Cache files are now flushed to disk before being committed, so a server kill or power loss while saving no longer leaves you with a `ZipException: invalid stored block lengths` on the next launch.
- 🚀 **Memory budget actually respected.** The cache previously underestimated its own size and could keep growing past the configured RAM limit, dragging TPS down. The accounting is fixed — the limit you set in the Cache config now reflects reality.
- ⚡ **No more multi-second freezes when loading into a world.** Cache writes triggered by chunk loads are now handled on a background worker. Returning to a world with hundreds of chunks streaming in at once no longer stalls the server tick for seconds while region files decompress.
- 🧵 **No more `NoSuchFileException` spam in the log when the cache is busy.** Two background workers could try to save the same region at the same moment and step on each other's temporary file. Each save now uses its own private temp file, and any leftovers from a previous crashed run are cleaned up at startup.
- ⚖️ **Smarter default RAM split.** Default Cache budgets retuned for typical 2-4 GB heap setups: runtime 256 → 128 MiB, snapshots 64 → 32 MiB, pages 128 → 192 MiB. Total drops from 448 to 352 MiB while the main page cache grows by 50 % — hot regions stay in RAM instead of being read from disk on every chunk load. Existing configs are untouched; only fresh installs pick up the new values (use Cache → Reset to defaults if you want them).

### Config & UX
- 🎛️ **Hide the scanning progress bar.** New *Debug → Show Scanning Bar* toggle removes the road scanning overlay from the world loading screen for players who find it noisy. On by default.
- 🗺️ **Disable the road graph debug map.** New *Debug → Enable Debug Map* toggle gates the keybind that opens the in-game graph view. On by default; turn it off if you don't want the map (or its draw cost) at all.
- ⚙️ **Choose how many cores RoadArchitect can use.** New *Debug → Async Worker Threads* setting controls the size of the background worker pool. `0` keeps the auto-sized default (`CPU cores − 2`, leaving headroom for the OS, the main game thread, and vanilla worldgen workers). Setting an explicit number above 0 caps it at that value. Useful for capping RA on shared servers or under Distant Horizons-heavy setups. Requires a game restart.
- 🛡️ **Broken selector lists no longer crash the mod.** A truncated or partially-corrupt config file (e.g. after a game crash mid-write) sometimes left `null` entries inside `structureSelectors` / `dimensionSelectors` / `forbiddenBiomes.selectors`. These are now filtered out at load time and the mod keeps running on whatever entries are still valid.
- 💬 **Helpful hint when a structure id is mistyped.** Invalid selectors used to log a flat `Structure selector 'X' is invalid`. The mod now searches the registry for the closest-matching id and adds `did you mean 'Y'?` — handy when adding ids from Mo' Structures, Tectonic, etc.

### Cache UI
- 📊 **F3 cache stats overlay.** Toggle *Debug → Show Cache Stats Overlay* to surface per-dimension runtime / snapshot / persisted-page usage and prefill state on the vanilla F3 screen.

### Compatibility
- ✅ **Existing v1.6.1 configs preserved.** New fields (cache budgets, debug toggles) populate with safe defaults the first time you launch v1.6.6.
- ✅ **Existing worlds load unchanged.** The legacy `road_cache.dat` file is moved to `road_cache.dat.legacy` on first open of an upgraded world; the new region-paged cache rebuilds on demand from chunk loads.
