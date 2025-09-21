package net.oxcodsnet.roadarchitect.config;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Синглтон-холдер активного конфига + простые слушатели «перепривязки».
 */
public final class RAConfigHolder {
    private static final CopyOnWriteArrayList<Consumer<RAConfig>> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile RAConfig INSTANCE = new Defaults();

    private RAConfigHolder() {
    }

    public static RAConfig get() {
        return INSTANCE;
    }

    /**
     * Привязывает платформенную реализацию и уведомляет слушателей.
     */
    public static void set(RAConfig impl) {
        INSTANCE = Objects.requireNonNull(impl, "impl");
        for (var l : LISTENERS) l.accept(INSTANCE);
    }

    /**
     * Подписка на смену реализации (например, после перезагрузки конфига).
     */
    public static AutoCloseable listen(Consumer<RAConfig> listener) {
        LISTENERS.add(Objects.requireNonNull(listener));
        listener.accept(INSTANCE); // сразу отдать текущее
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Запасные значения по умолчанию, если платформа не подвязана.
     */
    private static final class Defaults implements RAConfig {
        @Override
        public int initScanRadius() {
            return 125;
        }

        @Override
        public int chunkGenerateScanRadius() {
            return 20;
        }

        @Override
        public int maxConnectionDistance() {
            return 715;
        }

        @Override
        public int pipelineIntervalSeconds() {
            return 120;
        }

        @Override
        public int lampInterval() {
            return 30;
        }

        @Override
        public int buoyInterval() {
            return 18;
        }

        @Override
        public int sideDecorationInterval() {
            return 12;
        }

        @Override
        public int maskErosion() {
            return 1;
        }

        @Override
        public boolean deterministicDecorations() {
            return true;
        }

        @Override
        public List<String> structureSelectors() {
            return List.of("#minecraft:village");
        }

        // Terrain Analyzer defaults
        @Override
        public boolean terrainAnalyzerEnabled() {
            return false;
        }

        @Override
        public int terrainRoughRadius() {
            return 12;
        }

        @Override
        public int terrainRoughStride() {
            return 3;
        }

        @Override
        public int terrainRangeThreshold() {
            return 12;
        }

        @Override
        public double terrainPenaltyScale() {
            return 15.0;
        }

        @Override
        public boolean preferLandOverWater() {
            return true;
        }

        @Override
        public double waterStepPenalty() {
            return 200.0;
        }

        @Override
        public int coastAvoidBufferBlocks() {
            return 16;
        }

        @Override
        public double coastProximityPenalty() {
            return 180.0;
        }

        @Override
        public List<String> forbiddenBiomeSelectors() {
            return List.of(
                    "#minecraft:is_ocean",
                    "#minecraft:is_deep_ocean"
            );
        }

        @Override
        public int forbiddenBiomeBufferBlocks() {
            return 16;
        }

        @Override
        public double forbiddenBiomeProximityPenalty() {
            return 500.0;
        }
    }
}
