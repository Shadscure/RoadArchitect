package net.oxcodsnet.roadarchitect.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bridges Forge Config with the common {@link RAConfigHolder}.
 */
public final class RAConfigForgeBridge {
    private static final Logger LOG = LoggerFactory.getLogger("RoadArchitect/ConfigBridge");
    private static ForgeConfigSpec CONFIG;
    
    // Main config values
    private static ForgeConfigSpec.IntValue INIT_SCAN_RADIUS;
    private static ForgeConfigSpec.IntValue CHUNK_GENERATE_SCAN_RADIUS;
    private static ForgeConfigSpec.IntValue MAX_CONNECTION_DISTANCE;
    private static ForgeConfigSpec.IntValue PIPELINE_INTERVAL_SECONDS;
    private static ForgeConfigSpec.IntValue LAMP_INTERVAL;
    private static ForgeConfigSpec.IntValue SIDE_DECORATION_INTERVAL;
    private static ForgeConfigSpec.IntValue BUOY_INTERVAL;
    private static ForgeConfigSpec.IntValue MASK_EROSION;
    private static ForgeConfigSpec.BooleanValue DETERMINISTIC_DECORATIONS;
    private static ForgeConfigSpec.ConfigValue<List<String>> STRUCTURE_SELECTORS;
    
    // Terrain Analyzer settings
    private static ForgeConfigSpec.BooleanValue TERRAIN_ANALYZER_ENABLED;
    private static ForgeConfigSpec.IntValue TERRAIN_ROUGH_RADIUS;
    private static ForgeConfigSpec.IntValue TERRAIN_ROUGH_STRIDE;
    private static ForgeConfigSpec.IntValue TERRAIN_RANGE_THRESHOLD;
    private static ForgeConfigSpec.DoubleValue TERRAIN_PENALTY_SCALE;
    
    // Pathfinding settings
    private static ForgeConfigSpec.BooleanValue PREFER_LAND_OVER_WATER;
    private static ForgeConfigSpec.DoubleValue WATER_STEP_PENALTY;
    private static ForgeConfigSpec.IntValue COAST_AVOID_BUFFER_BLOCKS;
    private static ForgeConfigSpec.DoubleValue COAST_PROXIMITY_PENALTY;
    private static ForgeConfigSpec.BooleanValue ACCEPT_HIGH_PROGRESS_PARTIAL;
    private static ForgeConfigSpec.IntValue PARTIAL_PROGRESS_PERCENT;
    
    // Forbidden biomes settings
    private static ForgeConfigSpec.ConfigValue<List<String>> FORBIDDEN_BIOME_SELECTORS;
    private static ForgeConfigSpec.IntValue FORBIDDEN_BIOME_BUFFER_BLOCKS;
    private static ForgeConfigSpec.DoubleValue FORBIDDEN_BIOME_PROXIMITY_PENALTY;

    private RAConfigForgeBridge() {
    }

    public static void bootstrap() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        setupConfig(builder);
        CONFIG = builder.build();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG);

        RAConfigHolder.set(new RAConfig() {
            @Override
            public int initScanRadius() {
                return INIT_SCAN_RADIUS.get();
            }

            @Override
            public int chunkGenerateScanRadius() {
                return CHUNK_GENERATE_SCAN_RADIUS.get();
            }

            @Override
            public int maxConnectionDistance() {
                return MAX_CONNECTION_DISTANCE.get();
            }

            @Override
            public int pipelineIntervalSeconds() {
                return PIPELINE_INTERVAL_SECONDS.get();
            }

            @Override
            public int lampInterval() {
                return LAMP_INTERVAL.get();
            }

            @Override
            public int sideDecorationInterval() {
                return SIDE_DECORATION_INTERVAL.get();
            }

            @Override
            public int buoyInterval() {
                return BUOY_INTERVAL.get();
            }

            @Override
            public int maskErosion() {
                return MASK_EROSION.get();
            }

            @Override
            public boolean deterministicDecorations() {
                return DETERMINISTIC_DECORATIONS.get();
            }

            @Override
            public List<String> structureSelectors() {
                return STRUCTURE_SELECTORS.get();
            }

            // Terrain Analyzer
            @Override
            public boolean terrainAnalyzerEnabled() {
                return TERRAIN_ANALYZER_ENABLED.get();
            }

            @Override
            public int terrainRoughRadius() {
                return TERRAIN_ROUGH_RADIUS.get();
            }

            @Override
            public int terrainRoughStride() {
                return TERRAIN_ROUGH_STRIDE.get();
            }

            @Override
            public int terrainRangeThreshold() {
                return TERRAIN_RANGE_THRESHOLD.get();
            }

            @Override
            public double terrainPenaltyScale() {
                return TERRAIN_PENALTY_SCALE.get();
            }

            // Pathfinding preferences
            @Override
            public boolean preferLandOverWater() {
                return PREFER_LAND_OVER_WATER.get();
            }

            @Override
            public double waterStepPenalty() {
                return WATER_STEP_PENALTY.get();
            }

            @Override
            public int coastAvoidBufferBlocks() {
                return COAST_AVOID_BUFFER_BLOCKS.get();
            }

            @Override
            public double coastProximityPenalty() {
                return COAST_PROXIMITY_PENALTY.get();
            }

            // Forbidden biomes
            @Override
            public List<String> forbiddenBiomeSelectors() {
                return FORBIDDEN_BIOME_SELECTORS.get();
            }

            @Override
            public int forbiddenBiomeBufferBlocks() {
                return FORBIDDEN_BIOME_BUFFER_BLOCKS.get();
            }

            @Override
            public double forbiddenBiomeProximityPenalty() {
                return FORBIDDEN_BIOME_PROXIMITY_PENALTY.get();
            }

            @Override
            public boolean acceptPartialPaths() {
                return ACCEPT_HIGH_PROGRESS_PARTIAL.get();
            }

            @Override
            public double partialProgressThreshold() {
                int pct = PARTIAL_PROGRESS_PERCENT.get();
                if (pct <= 0) return 0.0;
                if (pct >= 100) return 1.0;
                return pct / 100.0;
            }
        });

        RoadPipelineController.refreshStructureSelectorCache();
        LOG.info("[RoadArchitect] forge-config bridge initialized");
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        // Main settings
        builder.push("general");
        INIT_SCAN_RADIUS = builder
                .comment("Initial scan radius in chunks when the world first loads")
                .defineInRange("initScanRadius", 125, 1, 500);
        
        CHUNK_GENERATE_SCAN_RADIUS = builder
                .comment("Radius in chunks scanned when new chunks generate")
                .defineInRange("chunkGenerateScanRadius", 20, 1, 100);
        
        MAX_CONNECTION_DISTANCE = builder
                .comment("Maximum distance in blocks between two structures to connect them")
                .defineInRange("maxConnectionDistance", 715, 100, 2000);
        
        PIPELINE_INTERVAL_SECONDS = builder
                .comment("Delay in seconds between pipeline runs")
                .defineInRange("pipelineIntervalSeconds", 120, 10, 3600);
        
        LAMP_INTERVAL = builder
                .comment("Interval in blocks along the path for placing lamps")
                .defineInRange("lampInterval", 30, 5, 100);
        
        SIDE_DECORATION_INTERVAL = builder
                .comment("Interval in blocks along the path for placing side decorations (fences, shrubs, etc.)")
                .defineInRange("sideDecorationInterval", 12, 2, 50);
        
        BUOY_INTERVAL = builder
                .comment("Interval in blocks along the path for placing buoys on water segments")
                .defineInRange("buoyInterval", 18, 5, 100);
        
        MASK_EROSION = builder
                .comment("Erosion in points/blocks for suitability masks near water/land transitions (0-8)")
                .defineInRange("maskErosion", 1, 0, 8);
        
        DETERMINISTIC_DECORATIONS = builder
                .comment("Whether to use deterministic, chunk-agnostic placement for decorations")
                .define("deterministicDecorations", true);
        
        STRUCTURE_SELECTORS = builder
                .comment("List of structure selectors that roads will connect")
                .define("structureSelectors", List.of("#minecraft:village"));
        builder.pop();
        
        // Terrain Analyzer settings
        builder.push("terrainAnalyzer");
        TERRAIN_ANALYZER_ENABLED = builder
                .comment("Enable terrain analysis for mountain/roughness avoidance")
                .define("enabled", false);
        
        TERRAIN_ROUGH_RADIUS = builder
                .comment("Radius for terrain roughness analysis")
                .defineInRange("roughRadius", 12, 4, 64);
        
        TERRAIN_ROUGH_STRIDE = builder
                .comment("Sampling stride in blocks within the window")
                .defineInRange("roughStride", 3, 1, 16);
        
        TERRAIN_RANGE_THRESHOLD = builder
                .comment("Height range threshold that triggers roughness penalty")
                .defineInRange("roughRangeThreshold", 12, 0, 64);
        
        TERRAIN_PENALTY_SCALE = builder
                .comment("Scale factor for roughness penalties")
                .defineInRange("roughPenaltyScale", 15.0, 0.0, 100.0);
        builder.pop();
        
        // Pathfinding settings
        builder.push("pathfinding");
        PREFER_LAND_OVER_WATER = builder
                .comment("Prefer land paths over water paths when possible")
                .define("preferLandOverWater", true);
        
        WATER_STEP_PENALTY = builder
                .comment("Penalty for pathfinding steps in water")
                .defineInRange("waterStepPenalty", 200.0, 0.0, 1000.0);
        
        COAST_AVOID_BUFFER_BLOCKS = builder
                .comment("Distance in blocks to avoid coastlines")
                .defineInRange("coastAvoidBufferBlocks", 16, 0, 64);
        
        COAST_PROXIMITY_PENALTY = builder
                .comment("Penalty for being near coastlines")
                .defineInRange("coastProximityPenalty", 180.0, 0.0, 1000.0);
        
        ACCEPT_HIGH_PROGRESS_PARTIAL = builder
                .comment("Whether to accept a partial path if A* fails but convergence is high")
                .define("acceptHighProgressPartial", true);
        
        PARTIAL_PROGRESS_PERCENT = builder
                .comment("Convergence threshold percentage to accept a partial path (0-100)")
                .defineInRange("partialProgressPercent", 80, 0, 100);
        builder.pop();
        
        // Forbidden biomes settings
        builder.push("forbiddenBiomes");
        FORBIDDEN_BIOME_SELECTORS = builder
                .comment("List of biome selectors to avoid when generating roads")
                .define("selectors", List.of("#minecraft:is_ocean", "#minecraft:is_deep_ocean"));
        
        FORBIDDEN_BIOME_BUFFER_BLOCKS = builder
                .comment("Buffer distance in blocks around forbidden biomes")
                .defineInRange("bufferBlocks", 16, 0, 64);
        
        FORBIDDEN_BIOME_PROXIMITY_PENALTY = builder
                .comment("Penalty for being near forbidden biomes")
                .defineInRange("proximityPenalty", 500.0, 0.0, 2000.0);
        builder.pop();
    }
}