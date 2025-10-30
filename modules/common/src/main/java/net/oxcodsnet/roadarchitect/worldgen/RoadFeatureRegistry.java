package net.oxcodsnet.roadarchitect.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureConfig.GenerationPhase;

/**
 * Holds registry keys and bootstrap logic for road worldgen features.
 */
public final class RoadFeatureRegistry {
    /**
     * The raw feature used for road placement.
     */
    public static final RoadFeature ROAD_FEATURE = new RoadFeature(RoadFeatureConfig.CODEC);

    /**
     * Key for the road feature instance.
     */
    public static final ResourceKey<Feature<?>> ROAD_FEATURE_KEY = ResourceKey.create(Registries.FEATURE,
            ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "road"));

    /**
     * Key for the configured road feature.
     */
    public static final ResourceKey<ConfiguredFeature<?, ?>> ROAD_PREP_CONFIGURED_FEATURE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "road_prepare"));

    /**
     * Key for the configured road feature during the finishing pass.
     */
    public static final ResourceKey<ConfiguredFeature<?, ?>> ROAD_FINAL_CONFIGURED_FEATURE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "road_finalize"));

    /**
     * Key for the placed road feature used during the preparation pass.
     */
    public static final ResourceKey<PlacedFeature> ROAD_PREP_PLACED_FEATURE_KEY = ResourceKey.create(
            Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "road_prepare"));

    /**
     * Key for the placed road feature used during the finishing pass.
     */
    public static final ResourceKey<PlacedFeature> ROAD_FINAL_PLACED_FEATURE_KEY = ResourceKey.create(
            Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "road_finalize"));

    private RoadFeatureRegistry() {
    }

    /**
     * Bootstrap for configured features used during data generation.
     *
     * @param ctx registerable context for configured features
     */
    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(ROAD_PREP_CONFIGURED_FEATURE_KEY,
                new ConfiguredFeature<>(ROAD_FEATURE, new RoadFeatureConfig(3, 1, GenerationPhase.PREPARE)));
        ctx.register(ROAD_FINAL_CONFIGURED_FEATURE_KEY,
                new ConfiguredFeature<>(ROAD_FEATURE, new RoadFeatureConfig(3, 1, GenerationPhase.FINALIZE)));
    }

    /**
     * Bootstrap for placed features used during data generation.
     *
     * @param ctx registerable context for placed features
     */
    public static void bootstrapPlaced(net.minecraft.data.worldgen.BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> lookup = ctx.lookup(Registries.CONFIGURED_FEATURE);
        ctx.register(ROAD_PREP_PLACED_FEATURE_KEY,
                new PlacedFeature(lookup.getOrThrow(ROAD_PREP_CONFIGURED_FEATURE_KEY), java.util.List.of(InSquarePlacement.spread())));
        ctx.register(ROAD_FINAL_PLACED_FEATURE_KEY,
                new PlacedFeature(lookup.getOrThrow(ROAD_FINAL_CONFIGURED_FEATURE_KEY), java.util.List.of(InSquarePlacement.spread())));
    }
}
