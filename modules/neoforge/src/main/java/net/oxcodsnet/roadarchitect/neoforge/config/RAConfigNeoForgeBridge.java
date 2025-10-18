package net.oxcodsnet.roadarchitect.neoforge.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.oxcodsnet.roadarchitect.config.LampPostConfigEntry;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges Cloth Config with the common {@link RAConfigHolder} on NeoForge.
 */
public final class RAConfigNeoForgeBridge {
    private static final Logger LOG = LoggerFactory.getLogger("RoadArchitect/ConfigBridge");
    private static ConfigHolder<RoadArchitectConfigData> holder;

    private RAConfigNeoForgeBridge() {}

    public static void bootstrap() {
        holder = AutoConfig.register(RoadArchitectConfigData.class, GsonConfigSerializer::new);
        RAConfigHolder.set(new RAConfig() {
            @Override
            public int initScanRadius() {
                return holder.getConfig().initScanRadius;
            }

            @Override
            public int chunkGenerateScanRadius() {
                return holder.getConfig().chunkGenerateScanRadius;
            }

            @Override
            public int maxConnectionDistance() {
                return holder.getConfig().maxConnectionDistance;
            }

            @Override
            public int pipelineIntervalSeconds() {
                return holder.getConfig().pipelineIntervalSeconds;
            }

            @Override
            public int lampInterval() {
                return holder.getConfig().lampInterval;
            }

            @Override
            public int buoyInterval() {
                return holder.getConfig().buoyInterval;
            }

            @Override
            public int sideDecorationInterval() {
                return holder.getConfig().sideDecorationInterval;
            }

            @Override
            public int maskErosion() {
                return holder.getConfig().maskErosion;
            }

            @Override
            public boolean deterministicDecorations() {
                return holder.getConfig().deterministicDecorations;
            }


            @Override
            public java.util.List<String> structureSelectors() {
                return holder.getConfig().structureSelectors;
            }

            @Override
            public java.util.List<String> dimensionSelectors() {
                return holder.getConfig().dimensionSelectors;
            }

            // Terrain Analyzer
            @Override
            public boolean terrainAnalyzerEnabled() {
                return holder.getConfig().terrainAnalyzer.enabled;
            }

            @Override
            public int terrainRoughRadius() {
                return holder.getConfig().terrainAnalyzer.roughRadius;
            }

            @Override
            public int terrainRoughStride() {
                return holder.getConfig().terrainAnalyzer.roughStride;
            }

            @Override
            public int terrainRangeThreshold() {
                return holder.getConfig().terrainAnalyzer.roughRangeThreshold;
            }

            @Override
            public double terrainPenaltyScale() {
                return holder.getConfig().terrainAnalyzer.roughPenaltyScale;
            }

            // Pathfinding preferences
            @Override
            public boolean preferLandOverWater() {
                return holder.getConfig().pathfinding.preferLandOverWater;
            }

            @Override
            public double waterStepPenalty() {
                return holder.getConfig().pathfinding.waterStepPenalty;
            }

            @Override
            public int coastAvoidBufferBlocks() {
                return holder.getConfig().pathfinding.coastAvoidBufferBlocks;
            }

            @Override
            public double coastProximityPenalty() {
                return holder.getConfig().pathfinding.coastProximityPenalty;
            }

            // Forbidden biomes
            @Override
            public java.util.List<String> forbiddenBiomeSelectors() {
                return holder.getConfig().forbiddenBiomes.selectors;
            }

            @Override
            public int forbiddenBiomeBufferBlocks() {
                return holder.getConfig().forbiddenBiomes.bufferBlocks;
            }

            @Override
            public double forbiddenBiomeProximityPenalty() {
                return holder.getConfig().forbiddenBiomes.proximityPenalty;
            }

            @Override
            public boolean acceptPartialPaths() {
                return holder.getConfig().pathfinding.acceptHighProgressPartial;
            }

            @Override
            public double partialProgressThreshold() {
                int pct = holder.getConfig().pathfinding.partialProgressPercent;
                if (pct <= 0) return 0.0;
                if (pct >= 100) return 1.0;
                return pct / 100.0;
            }

            @Override
            public java.util.List<LampPostConfigEntry> lampPostOverrides() {
                RoadArchitectConfigData.LampPostSettings settings = holder.getConfig().lampPosts;
                if (settings == null || !settings.enabled || settings.overrides == null || settings.overrides.isEmpty()) {
                    return java.util.List.of();
                }
                java.util.ArrayList<LampPostConfigEntry> out = new java.util.ArrayList<>(settings.overrides.size());
                for (RoadArchitectConfigData.LampPostDefinition def : settings.overrides) {
                    if (def == null) continue;
                    out.add(new LampPostConfigEntry(def.biomeSelectors, def.baseBlock, def.postBlock, def.lampBlock));
                }
                return java.util.List.copyOf(out);
            }
        });
        RoadPipelineController.refreshStructureSelectorCache();
        LOG.info("[RoadArchitect] cloth-config bridge initialized");
    }

    public static Object createScreen(Object parent) {
        try {
            Class<?> screenClass;
            try {
                screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            } catch (ClassNotFoundException ignored) {
                screenClass = Class.forName("net.minecraft.client.gui.screen.Screen");
            }
            java.lang.reflect.Method method = AutoConfig.class
                    .getMethod("getConfigScreen", Class.class, screenClass);
            Object screen = method.invoke(null, RoadArchitectConfigData.class, parent);
            return screen.getClass().getMethod("get").invoke(screen);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create config screen", e);
        }
    }
}
