package net.oxcodsnet.roadarchitect.neoforge.datagen;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.datagen.RACommonDatagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates configured and placed features for world generation.
 */
public class RoadWorldgenProvider extends DatapackBuiltinEntriesProvider {
    public RoadWorldgenProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, createBuilder(), Set.of(RoadArchitect.MOD_ID));
    }

    private static RegistrySetBuilder createBuilder() {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        RACommonDatagen.buildRegistries(builder);
        return builder;
    }
}

