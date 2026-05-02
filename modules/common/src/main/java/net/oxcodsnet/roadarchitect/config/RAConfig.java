package net.oxcodsnet.roadarchitect.config;

import net.oxcodsnet.roadarchitect.config.records.CacheSettings;
import net.oxcodsnet.roadarchitect.config.records.LampPostConfigEntry;
import net.oxcodsnet.roadarchitect.config.records.RoadStyleConfigEntry;

import java.util.List;

/**
 * Кроссплатформенный интерфейс конфига.
 * Платформенные слои (Fabric / NeoForge) должны предоставить реализацию
 * и вызвать RAConfigHolder#set(...) при инициализации.
 */
public interface RAConfig {
    int initScanRadius();

    int chunkGenerateScanRadius();

    int maxConnectionDistance();

    int pipelineIntervalSeconds();

    int lampInterval();

    /**
     * Road width (in blocks across) when placing generated segments.
     */
    int roadWidth();

    /**
     * Interval (in blocks along the path metric) for placing buoys on water segments.
     */
    int buoyInterval();

    /**
     * Interval (in blocks along the path metric) for placing side decorations (fences, shrubs, etc.) on land.
     */
    int sideDecorationInterval();

    /**
     * Erosion in points/blocks for suitability masks near water/land transitions.
     * 0 disables erosion; 1 removes immediate transition points, etc.
     */
    int maskErosion();

    /**
     * Whether to use deterministic, chunk-agnostic placement for decorations.
     */
    boolean deterministicDecorations();

    List<String> structureSelectors();

    /**
     * Dimensions (world identifiers) where the mod should operate.
     */
    List<String> dimensionSelectors();

    // Terrain Analyzer (mountain/roughness avoidance)
    boolean terrainAnalyzerEnabled();
    int terrainRoughRadius();
    int terrainRoughStride();
    int terrainRangeThreshold();
    double terrainPenaltyScale();

    // Pathfinding: land vs water preference
    boolean preferLandOverWater();
    double waterStepPenalty();
    int coastAvoidBufferBlocks();
    double coastProximityPenalty();

    // Pathfinding: forbidden biomes (block traversal)
    java.util.List<String> forbiddenBiomeSelectors();
    int forbiddenBiomeBufferBlocks();
    double forbiddenBiomeProximityPenalty();

    // Pathfinding: partial acceptance when convergence is high
    /**
     * Whether to accept a partial path if A* fails but convergence is high.
     */
    boolean acceptPartialPaths();

    /**
     * Convergence threshold in [0..1] to accept a partial path.
     * For example, 0.8 means 80% progress towards the L1 goal distance.
     */
    double partialProgressThreshold();

    /**
     * Custom lamp post definitions loaded from the configuration. The list can be empty when
     * overrides are disabled on the current platform implementation.
     */
    List<LampPostConfigEntry> lampPostOverrides();

    /**
     * Custom road surface styles loaded from the configuration. The list can be empty to fall back
     * to the built-in defaults.
     */
    List<RoadStyleConfigEntry> roadStyleOverrides();

    /**
     * Biomes O' Plenty specific road surface styles pulled from configuration.
     */
    List<RoadStyleConfigEntry> bopRoadStyleOverrides();

    /**
     * Whether verbose diagnostic logging should be emitted.
     */
    boolean debugVerboseLogs();

    /**
     * Whether the pipeline profiler is enabled.
     */
    boolean debugPipelineProfiler();

    /**
     * Whether [cache]-prefixed diagnostic logging should be emitted.
     * Off by default; platforms may flip via the Debug & Diagnostics tab.
     */
    default boolean debugCacheLogs() {
        return false;
    }

    /**
     * Cache budgets and persistence flags for the column store.
     * Default returns {@link CacheSettings#DEFAULT}; platforms may override
     * to expose this in their AutoConfig surface.
     */
    default CacheSettings cache() {
        return CacheSettings.DEFAULT;
    }
}
