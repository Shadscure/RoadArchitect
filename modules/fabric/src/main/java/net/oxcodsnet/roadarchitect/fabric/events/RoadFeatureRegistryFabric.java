package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * Fabric-бридж: регистрирует Feature и добавляет PlacedFeature во все биомы.
 */
public final class RoadFeatureRegistryFabric {
    private RoadFeatureRegistryFabric() {}

    public static void register() {
        // 1) Регистрируем сам Feature (экземпляр) по ключу
        Registry.register(
                BuiltInRegistries.FEATURE,
                RoadFeatureRegistry.ROAD_FEATURE_KEY.location(),
                RoadFeatureRegistry.ROAD_FEATURE
        );

        // 2) Втыкаем подготовительную фазу на LOCAL_MODIFICATIONS
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                RoadFeatureRegistry.ROAD_PREP_PLACED_FEATURE_KEY
        );

        // 3) Финишный проход на VEGETAL_DECORATION
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                RoadFeatureRegistry.ROAD_FINAL_PLACED_FEATURE_KEY
        );
    }
}
