package net.oxcodsnet.roadarchitect.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.oxcodsnet.roadarchitect.config.defaults.BopRoadStyleDefaults;
import net.oxcodsnet.roadarchitect.config.defaults.LampPostDefaults;
import net.oxcodsnet.roadarchitect.config.defaults.RoadStyleDefaults;

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
    public int roadWidth = 3; // numeric field (blocks across)

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

    @ConfigEntry.Category("roadStyles")
    @ConfigEntry.Gui.TransitiveObject
    public RoadStyleSettings roadStyles = new RoadStyleSettings();

    @ConfigEntry.Category("bopRoadStyles")
    @ConfigEntry.Gui.TransitiveObject
    public BopRoadStyleSettings bopRoadStyles = new BopRoadStyleSettings();

    @ConfigEntry.Category("lampPosts")
    @ConfigEntry.Gui.TransitiveObject
    public LampPostSettings lampPosts = new LampPostSettings();

    @ConfigEntry.Gui.Tooltip
    public List<String> structureSelectors = List.of("#minecraft:village");

    @ConfigEntry.Gui.Tooltip
    public List<String> dimensionSelectors = List.of("minecraft:overworld");

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

    // Pathfinding preferences (separate tab)
    @ConfigEntry.Category("pathfinding")
    @ConfigEntry.Gui.TransitiveObject
    public PathfindingSettings pathfinding = new PathfindingSettings();

    public static final class PathfindingSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean preferLandOverWater = true;

        @ConfigEntry.Gui.Tooltip
        public double waterStepPenalty = 200.0;

        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
        @ConfigEntry.Gui.Tooltip
        public int coastAvoidBufferBlocks = 16;

        @ConfigEntry.Gui.Tooltip
        public double coastProximityPenalty = 180.0;

        @ConfigEntry.Gui.Tooltip
        public boolean acceptHighProgressPartial = true;

        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        @ConfigEntry.Gui.Tooltip
        public int partialProgressPercent = 80;
    }

    public static final class LampPostSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public List<LampPostDefinition> overrides = LampPostDefaults.createDefinitionCopies();
    }

    public static final class RoadStyleSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public List<RoadStyleDefinition> overrides = RoadStyleDefaults.createDefinitionCopies();
    }

    public static final class BopRoadStyleSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public List<RoadStyleDefinition> overrides = BopRoadStyleDefaults.createDefinitionCopies();
    }

    public static final class RoadStyleDefinition {
        @ConfigEntry.Gui.Tooltip
        public List<String> biomeSelectors = new java.util.ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public List<RoadPaletteEntry> palette = new java.util.ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public List<RoadDecorationEntry> decorations = new java.util.ArrayList<>();
    }

    public static final class RoadPaletteEntry {
        @ConfigEntry.Gui.Tooltip
        public String block = "";

        @ConfigEntry.Gui.Tooltip
        public int weight = 1;
    }

    public static final class RoadDecorationEntry {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public RoadDecorationType type = RoadDecorationType.FENCE;

        @ConfigEntry.Gui.Tooltip
        public String block = "";
    }

    public static final class LampPostDefinition {
        @ConfigEntry.Gui.Tooltip
        public List<String> biomeSelectors = new java.util.ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public String baseBlock = "minecraft:cobblestone_wall";

        @ConfigEntry.Gui.Tooltip
        public String postBlock = "minecraft:oak_fence";

        @ConfigEntry.Gui.Tooltip
        public String lampBlock = "minecraft:lantern";
    }

    // Forbidden biome rules (separate tab)
    @ConfigEntry.Category("forbiddenBiomes")
    @ConfigEntry.Gui.TransitiveObject
    public ForbiddenBiomeSettings forbiddenBiomes = new ForbiddenBiomeSettings();

    public static final class ForbiddenBiomeSettings {
        @ConfigEntry.Gui.Tooltip
        public List<String> selectors = List.of(
                "#minecraft:is_ocean",
                "#minecraft:is_deep_ocean"
        );

        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
        @ConfigEntry.Gui.Tooltip
        public int bufferBlocks = 16;

        @ConfigEntry.Gui.Tooltip
        public double proximityPenalty = 500.0;
    }

    // Debug & diagnostics (placed last intentionally)
    @ConfigEntry.Category("debug")
    @ConfigEntry.Gui.TransitiveObject
    public DebugSettings debug = new DebugSettings();

    public static final class DebugSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean enableVerboseLogs = false;

        @ConfigEntry.Gui.Tooltip
        public boolean enablePipelineProfiler = false;
    }
}
