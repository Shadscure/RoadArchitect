package net.oxcodsnet.roadarchitect.forge.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.oxcodsnet.roadarchitect.config.records.LampPostConfigEntry;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.config.records.RoadStyleConfigEntry;
import net.oxcodsnet.roadarchitect.config.defaults.BopRoadStyleDefaults;
import net.oxcodsnet.roadarchitect.config.defaults.LampPostDefaults;
import net.oxcodsnet.roadarchitect.config.defaults.RoadStyleDefaults;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.compat.BopCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bridges Cloth Config with the common {@link RAConfigHolder} on Forge.
 */
public final class RAConfigForgeBridge {
    private static final Logger LOG = LoggerFactory.getLogger("RoadArchitect/ConfigBridge");
    private static ConfigHolder<RoadArchitectConfigData> holder;

    private RAConfigForgeBridge() {}

    public static void bootstrap() {
        holder = AutoConfig.register(RoadArchitectConfigData.class, GsonConfigSerializer::new);
        if (!BopCompat.isPresent()) {
            registerBopInstallHint();
        }

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
            public int roadWidth() {
                return holder.getConfig().roadWidth;
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
                return net.oxcodsnet.roadarchitect.util.ConfigSanitize.cleanList(holder.getConfig().structureSelectors);
            }

            @Override
            public java.util.List<String> dimensionSelectors() {
                return net.oxcodsnet.roadarchitect.util.ConfigSanitize.cleanList(holder.getConfig().dimensionSelectors);
            }

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

            @Override
            public java.util.List<String> forbiddenBiomeSelectors() {
                return net.oxcodsnet.roadarchitect.util.ConfigSanitize.cleanList(holder.getConfig().forbiddenBiomes.selectors);
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
                if (settings == null) {
                    return LampPostDefaults.entries();
                }
                if (!settings.enabled) {
                    return LampPostDefaults.entries();
                }
                java.util.List<RoadArchitectConfigData.LampPostDefinition> defs = settings.overrides;
                if (defs == null || defs.isEmpty()) {
                    return LampPostDefaults.entries();
                }
                java.util.ArrayList<LampPostConfigEntry> out = new java.util.ArrayList<>(defs.size());
                for (RoadArchitectConfigData.LampPostDefinition def : defs) {
                    if (def == null) continue;
                    out.add(new LampPostConfigEntry(def.biomeSelectors, def.baseBlock, def.postBlock, def.lampBlock));
                }
                return java.util.List.copyOf(out);
            }

            @Override
            public java.util.List<RoadStyleConfigEntry> roadStyleOverrides() {
                RoadArchitectConfigData.RoadStyleSettings settings = holder.getConfig().roadStyles;
                if (settings == null || !settings.enabled) {
                    return RoadStyleDefaults.entries();
                }
                return compileRoadStyles(settings.overrides, RoadStyleDefaults.entries());
            }

            @Override
            public java.util.List<RoadStyleConfigEntry> bopRoadStyleOverrides() {
                if (!BopCompat.isPresent()) {
                    return java.util.List.of();
                }
                RoadArchitectConfigData.BopRoadStyleSettings settings = holder.getConfig().bopRoadStyles;
                if (settings == null || !settings.enabled) {
                    return BopRoadStyleDefaults.entries();
                }
                return compileRoadStyles(settings.overrides, BopRoadStyleDefaults.entries());
            }

            @Override
            public boolean debugVerboseLogs() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings != null && settings.enableVerboseLogs;
            }

            @Override
            public boolean debugPipelineProfiler() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings != null && settings.enablePipelineProfiler;
            }

            @Override
            public boolean debugCacheLogs() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings != null && settings.enableCacheLogs;
            }

            @Override
            public boolean debugCacheOverlay() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings != null && settings.showCacheStatsOverlay;
            }

            @Override
            public boolean debugShowScanningBar() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings == null || settings.showScanningBar;
            }

            @Override
            public boolean debugEnableMap() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings == null || settings.enableDebugMap;
            }

            @Override
            public int debugAsyncThreads() {
                RoadArchitectConfigData.DebugSettings settings = holder.getConfig().debug;
                return settings == null ? 0 : settings.asyncThreads;
            }

        });

        RoadPipelineController.refreshStructureSelectorCache();
        LOG.info("[RoadArchitect] cloth-config bridge initialized");
    }

    private static void registerBopInstallHint() {
        try {
            AutoConfig.getGuiRegistry(RoadArchitectConfigData.class)
                    .registerPredicateProvider(
                            (name, field, config, defaults, registry) -> {
                                LOG.warn("Failed to create Cloth Config hint entry for missing Biomes O' Plenty");
                                return java.util.List.of();
                            },
                            field -> field.getDeclaringClass() == RoadArchitectConfigData.class
                                    && field.getType() == RoadArchitectConfigData.BopRoadStyleSettings.class);
        } catch (NoSuchMethodError missingGuiRegistry) {
            LOG.warn("Cloth Config gui registry unavailable, skipping Biomes O' Plenty hint registration");
        }
    }

    private static java.util.List<RoadStyleConfigEntry> compileRoadStyles(java.util.List<RoadArchitectConfigData.RoadStyleDefinition> definitions, java.util.List<RoadStyleConfigEntry> defaults) {
        if (definitions == null || definitions.isEmpty()) {
            return defaults;
        }
        java.util.ArrayList<RoadStyleConfigEntry> out = new java.util.ArrayList<>(definitions.size());
        for (RoadArchitectConfigData.RoadStyleDefinition def : definitions) {
            if (def == null) {
                continue;
            }
            java.util.ArrayList<RoadStyleConfigEntry.SurfaceBlockEntry> palette = new java.util.ArrayList<>();
            if (def.palette != null) {
                for (RoadArchitectConfigData.RoadPaletteEntry entry : def.palette) {
                    if (entry == null) {
                        continue;
                    }
                    palette.add(new RoadStyleConfigEntry.SurfaceBlockEntry(entry.block, entry.weight));
                }
            }
            java.util.ArrayList<RoadStyleConfigEntry.DecorationEntry> decorations = new java.util.ArrayList<>();
            if (def.decorations != null) {
                for (RoadArchitectConfigData.RoadDecorationEntry entry : def.decorations) {
                    if (entry == null) {
                        continue;
                    }
                    decorations.add(new RoadStyleConfigEntry.DecorationEntry(entry.type, entry.block));
                }
            }
            RoadStyleConfigEntry compiled = new RoadStyleConfigEntry(def.biomeSelectors, palette, decorations);
            if (compiled.palette().isEmpty()) {
                LOG.warn("Skipping road style override with empty palette for selectors {}", def.biomeSelectors);
                continue;
            }
            out.add(compiled);
        }
        if (out.isEmpty()) {
            return defaults;
        }
        return java.util.List.copyOf(out);
    }
}
