v1.6.6 — <em>Cache flush race fix</em>

### Fixes
- 🧵 **No more `NoSuchFileException` spam in the log when the cache is busy.** Two background workers could try to save the same region at the same moment and step on each other's temporary file, leaving a stack trace in the log every time it happened. Each save now uses its own private temp file, and any leftovers from a previous crashed run are cleaned up at startup.

---

v1.6.5 — <em>Config robustness</em>

### Fixes
- 🛡️ **Broken selector lists no longer crash the mod.** A truncated or partially-corrupt config file (e.g. after a game crash mid-write) sometimes left `null` entries inside `structureSelectors` / `dimensionSelectors` / `forbiddenBiomes.selectors`. These are now filtered out at load time and the mod keeps running on whatever entries are still valid.
- 💬 **Helpful hint when a structure id is mistyped.** Invalid selectors used to log a flat `Structure selector 'X' is invalid`. The mod now searches the registry for the closest-matching id or tag and adds `did you mean 'Y'?` — handy when adding ids from Mo' Structures, Tectonic, etc.

---

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

