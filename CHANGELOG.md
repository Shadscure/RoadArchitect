v1.6.6 — <em>Feature parity with the 1.21.x line</em>

### Fixes
- 🛡️ Corrupted cache files quarantined instead of poisoning new writes
- 💾 Hardened cache writes (fsync before commit)
- 🚀 Memory budget actually respected
- ⚡ No more multi-second freezes on world load
- 🧵 No more `NoSuchFileException` spam from concurrent flushes
- 🛡️ Broken selector lists no longer crash the mod
- 💬 "Did you mean ..." hint for mistyped structure ids

### Added
- 🎛️ Hide the scanning progress bar (*Debug → Show Scanning Bar*)
- 🗺️ Disable the road graph debug map (*Debug → Enable Debug Map*)
- ⚙️ Async Worker Threads setting (*Debug → Async Worker Threads*, requires restart)
- 📊 F3 cache stats overlay (*Debug → Show Cache Stats Overlay*)

### Tuning
- ⚖️ Smarter default RAM split (runtime 128 / snapshot 32 / pages 192 MiB)

### Compatibility
- Existing v1.6.1 configs preserved with safe defaults
- Legacy `road_cache.dat` auto-moved to `.legacy` on first open; new region cache rebuilds from chunk loads
