package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

public final class RoadFeatureRegistryForge {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            RegistryKeys.FEATURE,
            RoadArchitect.MOD_ID
    );
    public static final RegistryObject<Feature<?>> ROAD_FEATURE = FEATURES.register(
            "road",
            () -> RoadFeatureRegistry.ROAD_FEATURE
    );

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}