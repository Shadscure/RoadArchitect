package net.oxcodsnet.roadarchitect.forge.events;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * Forge桥接器：注册世界生成特征。
 */
@Mod.EventBusSubscriber(modid = RoadArchitect.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RoadFeatureRegistryForge {
    private RoadFeatureRegistryForge() {}

    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            RegistryKeys.FEATURE,
            RoadArchitect.MOD_ID
    );
    public static final RegistryObject<Feature<?>> ROAD_FEATURE = FEATURES.register(
            "road",
            () -> RoadFeatureRegistry.ROAD_FEATURE
    );
}