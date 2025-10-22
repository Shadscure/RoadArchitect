package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.registry.RegistryKeys;
import net.minecraftforge.registries.RegisterEvent;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

public final class RoadFeatureRegistryForge {
    private RoadFeatureRegistryForge() {
    }

    /**
     * Registers the road feature and injects the placed feature into all biomes.
     */
    public static void register(RegisterEvent event) {
        event.register(RegistryKeys.FEATURE,
                helper -> helper.register(RoadFeatureRegistry.ROAD_FEATURE_KEY, RoadFeatureRegistry.ROAD_FEATURE));
    }
}
