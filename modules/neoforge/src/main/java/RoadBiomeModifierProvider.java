//package net.oxcodsnet.roadarchitect.neoforge.datagen;
//
//import net.minecraft.data.DataOutput;
//import net.minecraft.registry.tag.BiomeTags;
//import net.minecraft.resource.ResourceType;
//import net.minecraft.registry.RegistryKeys;
//import net.minecraft.registry.RegistryWrapper.WrapperLookup;
//import net.minecraft.registry.entry.RegistryEntryList;
//import net.minecraft.util.Identifier;
//import net.minecraft.world.biome.Biome;
//import net.minecraft.world.gen.GenerationStep;
//import net.minecraft.world.gen.feature.PlacedFeature;
////import net.neoforged.neoforge.common.data.ExistingFileHelper;
//import net.neoforged.neoforge.common.data.JsonCodecProvider;
//import net.neoforged.neoforge.common.world.BiomeModifier;
//import net.neoforged.neoforge.common.world.BiomeModifiers;
//import net.oxcodsnet.roadarchitect.RoadArchitect;
//import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;
//
//import java.util.concurrent.CompletableFuture;
//
///**
// * Generates a biome modifier that adds the road feature to all biomes.
// */
//public final class RoadBiomeModifierProvider extends JsonCodecProvider<BiomeModifier> {
//    private final CompletableFuture<WrapperLookup> registries;
//
//    public RoadBiomeModifierProvider(DataOutput output, CompletableFuture<WrapperLookup> registries, ExistingFileHelper helper) {
//        super(output, DataOutput.OutputType.DATA_PACK, RoadArchitect.MOD_ID, ResourceType.SERVER_DATA, BiomeModifier.DIRECT_CODEC, registries, "neoforge/biome_modifier", helper);
//        this.registries = registries;
//    }
//
//    @Override
//    protected void gather() {
//        WrapperLookup lookup = registries.join();
//        RegistryEntryList.Named<Biome> overworldBiomes = lookup.getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD);
//        RegistryEntryList.Direct<PlacedFeature> prepFeature = RegistryEntryList.of(lookup.getOrThrow(RegistryKeys.PLACED_FEATURE).getOrThrow(RoadFeatureRegistry.ROAD_PREP_PLACED_FEATURE_KEY));
//        RegistryEntryList.Direct<PlacedFeature> finalFeature = RegistryEntryList.of(lookup.getOrThrow(RegistryKeys.PLACED_FEATURE).getOrThrow(RoadFeatureRegistry.ROAD_FINAL_PLACED_FEATURE_KEY));
//
//        BiomeModifier prepModifier = new BiomeModifiers.AddFeaturesBiomeModifier(overworldBiomes, prepFeature, GenerationStep.Feature.LOCAL_MODIFICATIONS);
//        BiomeModifier finalModifier = new BiomeModifiers.AddFeaturesBiomeModifier(overworldBiomes, finalFeature, GenerationStep.Feature.VEGETAL_DECORATION);
//
//        unconditional(Identifier.of(RoadArchitect.MOD_ID, "add_road_prepare_feature"), prepModifier);
//        unconditional(Identifier.of(RoadArchitect.MOD_ID, "add_road_finalize_feature"), finalModifier);
//    }
//
//    @Override
//    public String getName() {
//        return "Road Architect Biome Modifiers";
//    }
//}
