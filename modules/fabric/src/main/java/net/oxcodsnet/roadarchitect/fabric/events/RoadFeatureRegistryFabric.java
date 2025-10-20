package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.GenerationStep;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * Fabric-бридж: регистрирует Feature и добавляет PlacedFeature во все биомы.
 */
public final class RoadFeatureRegistryFabric {
    private RoadFeatureRegistryFabric() {}

    public static void register() {
        // 1) Регистрируем сам Feature (экземпляр) по ключу
        Registry.register(
                Registries.FEATURE,
                RoadFeatureRegistry.ROAD_FEATURE_KEY.getValue(),
                RoadFeatureRegistry.ROAD_FEATURE
        );

        // 2) Втыкаем подготовительную фазу на LOCAL_MODIFICATIONS
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Feature.LOCAL_MODIFICATIONS,
                RoadFeatureRegistry.ROAD_PREP_PLACED_FEATURE_KEY
        );

        // 3) Финишный проход на TOP_LAYER_MODIFICATION
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Feature.TOP_LAYER_MODIFICATION,
                RoadFeatureRegistry.ROAD_FINAL_PLACED_FEATURE_KEY
        );
    }
}
