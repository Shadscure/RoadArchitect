v1.5.0 — <em>Smarter pathfinding, Terrain Analyzer (beta)</em>

### Highlights

- 🧭 Terrain Analyzer (beta): clearer recognition of mountainous terrain, with new config options and full translations
- 🧠 Pathfinding controls: configurable land preference and water behavior (acceptable water level, coastal buffer, max coastal speed)
- 🛣️ Partial paths: when a full route can’t be completed, high-progress searches can return the best partial path instead
- 🌊 Safer defaults: oceans are no longer hard-blocked; default “prohibited biomes” now include <code>#minecraft:is_ocean</code> and <code>#minecraft:is_deep_ocean</code>
- 🔒 Stability: thread-safe biome selector cache (eliminates race conditions in concurrent scans)
- 🔧 Fixes: correct player direction on the debug map and reliable point teleportation packet handling; RA config tweaks
- 🧩 Compatibility: improved boot behavior when Distant Horizons is installed
- 🌐 Localization & data: updated category labels, new keys for fresh options, translations split and refreshed (EN/RU/ES/FR/DE/ZH/UK)

**Compatibility:** No world resets needed. Defaults for water/ocean behavior changed — review new pathfinding options if you rely on custom configs.