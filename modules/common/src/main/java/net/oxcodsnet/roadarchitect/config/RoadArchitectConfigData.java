package net.oxcodsnet.roadarchitect.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import java.util.List;

/**
 * Data model for Road Architect configuration.
 */
@Config(name = "roadarchitect")
public final class RoadArchitectConfigData implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public int initScanRadius = 125; // numeric field

    @ConfigEntry.Gui.Tooltip
    public int chunkGenerateScanRadius = 20; // numeric field

    @ConfigEntry.Gui.Tooltip
    public int maxConnectionDistance = 715; // numeric field

    @ConfigEntry.Gui.Tooltip
    public int pipelineIntervalSeconds = 120; // numeric field (seconds)

    @ConfigEntry.Gui.Tooltip
    public int lampInterval = 30; // numeric field (blocks)

    @ConfigEntry.Gui.Tooltip
    public int sideDecorationInterval = 12; // numeric field (blocks)

    @ConfigEntry.Gui.Tooltip
    public int buoyInterval = 18; // numeric field (blocks)

    // Small discrete range — keep slider for convenience (0..8)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 8)
    @ConfigEntry.Gui.Tooltip
    public int maskErosion = 1;

    // Boolean toggle (drop-down/toggle, not a slider)
    @ConfigEntry.Gui.Tooltip
    public boolean deterministicDecorations = true;

    @ConfigEntry.Gui.Tooltip
    public List<String> structureSelectors = List.of("#minecraft:village");

    // Terrain Analyzer category (separate tab)
    @ConfigEntry.Category("terrainAnalyzer")
    @ConfigEntry.Gui.TransitiveObject
    public TerrainAnalyzerSettings terrainAnalyzer = new TerrainAnalyzerSettings();

    public static final class TerrainAnalyzerSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;

        @ConfigEntry.BoundedDiscrete(min = 4, max = 64)
        @ConfigEntry.Gui.Tooltip
        public int roughRadius = 12;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
        @ConfigEntry.Gui.Tooltip
        public int roughStride = 3;

        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
        @ConfigEntry.Gui.Tooltip
        public int roughRangeThreshold = 12;

        @ConfigEntry.Gui.Tooltip
        public double roughPenaltyScale = 15.0;
    }
}
