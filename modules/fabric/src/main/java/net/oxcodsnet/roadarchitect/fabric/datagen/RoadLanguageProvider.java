package net.oxcodsnet.roadarchitect.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.oxcodsnet.roadarchitect.datagen.RALanguage;

public class RoadLanguageProvider extends FabricLanguageProvider {
    private final String code;

    public RoadLanguageProvider(FabricDataOutput output, String code) {
        super(output, code);
        this.code = code;
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        // вся таблица ключей/значений теперь в common:
        RALanguage.fill(this.code, builder::add);
    }
}
