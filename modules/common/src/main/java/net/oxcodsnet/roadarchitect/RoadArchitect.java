package net.oxcodsnet.roadarchitect;

import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadStyleConfigEntry;

import java.util.List;

public final class RoadArchitect {
    public static final String MOD_ID = "roadarchitect";

    // Фасад, делегирующий в актуальный провайдер из Holder
    public static final RAConfig CONFIG = new RAConfig() {
        @Override
        public int initScanRadius() {
            return RAConfigHolder.get().initScanRadius();
        }

        @Override
        public int chunkGenerateScanRadius() {
            return RAConfigHolder.get().chunkGenerateScanRadius();
        }

        @Override
        public int maxConnectionDistance() {
            return RAConfigHolder.get().maxConnectionDistance();
        }

        @Override
        public int pipelineIntervalSeconds() {
            return RAConfigHolder.get().pipelineIntervalSeconds();
        }

        @Override
        public int lampInterval() {
            return RAConfigHolder.get().lampInterval();
        }

        @Override
        public int roadWidth() {
            return RAConfigHolder.get().roadWidth();
        }

        @Override
        public int buoyInterval() {
            return RAConfigHolder.get().buoyInterval();
        }

        @Override
        public int sideDecorationInterval() {
            return RAConfigHolder.get().sideDecorationInterval();
        }

        @Override
        public int maskErosion() {
            return RAConfigHolder.get().maskErosion();
        }

        @Override
        public boolean deterministicDecorations() {
            return RAConfigHolder.get().deterministicDecorations();
        }

        @Override
        public List<String> structureSelectors() {
            return RAConfigHolder.get().structureSelectors();
        }

        @Override
        public List<String> dimensionSelectors() {
            return RAConfigHolder.get().dimensionSelectors();
        }

        @Override
        public boolean terrainAnalyzerEnabled() {
            return RAConfigHolder.get().terrainAnalyzerEnabled();
        }

        @Override
        public int terrainRoughRadius() {
            return RAConfigHolder.get().terrainRoughRadius();
        }

        @Override
        public int terrainRoughStride() {
            return RAConfigHolder.get().terrainRoughStride();
        }

        @Override
        public int terrainRangeThreshold() {
            return RAConfigHolder.get().terrainRangeThreshold();
        }

        @Override
        public double terrainPenaltyScale() {
            return RAConfigHolder.get().terrainPenaltyScale();
        }

        @Override
        public boolean preferLandOverWater() {
            return RAConfigHolder.get().preferLandOverWater();
        }

        @Override
        public double waterStepPenalty() {
            return RAConfigHolder.get().waterStepPenalty();
        }

        @Override
        public int coastAvoidBufferBlocks() {
            return RAConfigHolder.get().coastAvoidBufferBlocks();
        }

        @Override
        public double coastProximityPenalty() {
            return RAConfigHolder.get().coastProximityPenalty();
        }

        @Override
        public List<String> forbiddenBiomeSelectors() {
            return RAConfigHolder.get().forbiddenBiomeSelectors();
        }

        @Override
        public int forbiddenBiomeBufferBlocks() {
            return RAConfigHolder.get().forbiddenBiomeBufferBlocks();
        }

        @Override
        public double forbiddenBiomeProximityPenalty() {
            return RAConfigHolder.get().forbiddenBiomeProximityPenalty();
        }

        @Override
        public boolean acceptPartialPaths() {
            return RAConfigHolder.get().acceptPartialPaths();
        }

        @Override
        public double partialProgressThreshold() {
            return RAConfigHolder.get().partialProgressThreshold();
        }

        @Override
        public List<net.oxcodsnet.roadarchitect.config.LampPostConfigEntry> lampPostOverrides() {
            return RAConfigHolder.get().lampPostOverrides();
        }

        @Override
        public List<RoadStyleConfigEntry> roadStyleOverrides() {
            return RAConfigHolder.get().roadStyleOverrides();
        }

        @Override
        public List<RoadStyleConfigEntry> bopRoadStyleOverrides() {
            return RAConfigHolder.get().bopRoadStyleOverrides();
        }

        @Override
        public boolean debugVerboseLogs() {
            return RAConfigHolder.get().debugVerboseLogs();
        }

        @Override
        public boolean debugPipelineProfiler() {
            return RAConfigHolder.get().debugPipelineProfiler();
        }
    };

    private RoadArchitect() {
    }

    public static void init() {
        // Common init: bootstrap built-in addons
        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.initBuiltins();
    }
}
