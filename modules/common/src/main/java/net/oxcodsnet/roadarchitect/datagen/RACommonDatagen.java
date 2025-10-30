package net.oxcodsnet.roadarchitect.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * Платформо-независимый хелпер для регистрации динамических реестров в датагене.
 * Вызывается из Fabric/NeoForge-адаптеров.
 */
public final class RACommonDatagen {
    private RACommonDatagen() {
    }

    public static void buildRegistries(RegistrySetBuilder builder) {
        builder.add(Registries.CONFIGURED_FEATURE, RoadFeatureRegistry::bootstrapConfigured);
        builder.add(Registries.PLACED_FEATURE, RoadFeatureRegistry::bootstrapPlaced);
    }
}
