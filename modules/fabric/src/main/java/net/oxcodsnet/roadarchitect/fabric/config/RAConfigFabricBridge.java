package net.oxcodsnet.roadarchitect.fabric.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.oxcodsnet.roadarchitect.config.defaults.BopRoadStyleDefaults;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.oxcodsnet.roadarchitect.config.records.LampPostConfigEntry;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.config.defaults.LampPostDefaults;
import net.oxcodsnet.roadarchitect.config.records.RoadStyleConfigEntry;
import net.oxcodsnet.roadarchitect.config.defaults.RoadStyleDefaults;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.compat.BopCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bridges Cloth Config with the common {@link RAConfigHolder}.
 */
public final class RAConfigFabricBridge {
    private static final Logger LOG = LoggerFactory.getLogger("RoadArchitect/ConfigBridge");
    private static ConfigHolder<RoadArchitectConfigData> holder;

    private RAConfigFabricBridge() {
    }

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
            public int sideDecorationInterval() {
                return holder.getConfig().sideDecorationInterval;
            }

            @Override
            public int buoyInterval() {
                return holder.getConfig().buoyInterval;
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
        });

        holder.registerSaveListener((h, cfg) -> {
            RoadPipelineController.refreshStructureSelectorCache();
            LOG.info("[RoadArchitect] config reloaded");
            return InteractionResult.PASS;
        });

        LOG.info("[RoadArchitect] cloth-config bridge initialized");
    }

    private static void registerBopInstallHint() {
        try {
            Method getGuiRegistry = AutoConfig.class.getMethod("getGuiRegistry", Class.class);
            GuiRegistry registry = (GuiRegistry) getGuiRegistry.invoke(null, RoadArchitectConfigData.class);
            if (registry == null) {
                LOG.warn("AutoConfig#getGuiRegistry returned null; skipping BOP install hint registration.");
                return;
            }
            registry.registerPredicateProvider(
                    (name, field, config, defaults, guiRegistry) -> java.util.List.of(
                            ConfigEntryBuilder.create()
                                    .startTextDescription(Component.translatable("text.autoconfig.roadarchitect.option.bopRoadStyles.installHint"))
                                    .build()
                    ),
                    field -> field.getDeclaringClass() == RoadArchitectConfigData.class
                            && field.getType() == RoadArchitectConfigData.BopRoadStyleSettings.class);
        } catch (NoSuchMethodException e) {
            LOG.warn("Cloth Config no longer exposes AutoConfig#getGuiRegistry; skipping BOP install hint.");
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.error("Failed to register BOP install hint with Cloth Config.", e);
        }
    }

    private static java.util.List<RoadStyleConfigEntry> compileRoadStyles(
            java.util.List<RoadArchitectConfigData.RoadStyleDefinition> definitions,
            java.util.List<RoadStyleConfigEntry> defaults) {
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

    public static Screen createScreen(Screen parent) {
        return AutoConfig.getConfigScreen(RoadArchitectConfigData.class, parent).get();
    }
}
