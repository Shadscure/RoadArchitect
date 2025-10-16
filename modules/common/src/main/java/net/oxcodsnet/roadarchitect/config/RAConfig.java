package net.oxcodsnet.roadarchitect.config;

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
     * The maximum number of entries to hold in the world data cache (heights, biomes, etc.).
     * A larger value improves performance at the cost of higher memory usage.
     */
    int cacheMaxSize();
}
