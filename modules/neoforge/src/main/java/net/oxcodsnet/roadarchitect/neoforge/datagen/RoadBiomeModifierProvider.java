package net.oxcodsnet.roadarchitect.neoforge.datagen;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Generates a biome modifier that adds the road feature to all biomes.
 */
public final class RoadBiomeModifierProvider extends JsonCodecProvider<BiomeModifier> {
    private final CompletableFuture<Provider> registries;

    public RoadBiomeModifierProvider(PackOutput output, CompletableFuture<Provider> registries, ExistingFileHelper helper) {
        super(output, PackOutput.Target.DATA_PACK, RoadArchitect.MOD_ID, PackType.SERVER_DATA, BiomeModifier.DIRECT_CODEC, registries, "neoforge/biome_modifier", helper);
        this.registries = registries;
    }

    @Override
    protected void gather() {
        Provider lookup = registries.join();
        HolderSet.Named<Biome> overworldBiomes = lookup.lookupOrThrow(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD);
        HolderSet.Direct<PlacedFeature> prepFeature = HolderSet.direct(lookup.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(RoadFeatureRegistry.ROAD_PREP_PLACED_FEATURE_KEY));
        HolderSet.Direct<PlacedFeature> finalFeature = HolderSet.direct(lookup.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(RoadFeatureRegistry.ROAD_FINAL_PLACED_FEATURE_KEY));

        BiomeModifier prepModifier = new BiomeModifiers.AddFeaturesBiomeModifier(overworldBiomes, prepFeature, GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        BiomeModifier finalModifier = new BiomeModifiers.AddFeaturesBiomeModifier(overworldBiomes, finalFeature, GenerationStep.Decoration.VEGETAL_DECORATION);

        unconditional(ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "add_road_prepare_feature"), prepModifier);
        unconditional(ResourceLocation.fromNamespaceAndPath(RoadArchitect.MOD_ID, "add_road_finalize_feature"), finalModifier);
    }

    @Override
    public String getName() {
        return "Road Architect Biome Modifiers";
    }
}
