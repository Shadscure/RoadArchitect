package net.oxcodsnet.roadarchitect.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryBuilder;
import net.oxcodsnet.roadarchitect.datagen.RACommonDatagen;
import net.oxcodsnet.roadarchitect.fabric.worldgen.RoadWorldgenProvider;

public class RoadArchitectDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void buildRegistry(RegistryBuilder registryBuilder) {
        RACommonDatagen.buildRegistries(registryBuilder);
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(RoadWorldgenProvider::new);
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "en_us"));
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "ru_ru"));
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "es_es"));
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "fr_fr"));
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "de_de"));
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "zh_cn"));
        pack.addProvider((FabricDataOutput output) -> new RoadLanguageProvider(output, "uk_ua"));
    }
}
