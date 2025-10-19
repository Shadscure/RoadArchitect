package net.oxcodsnet.roadarchitect.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in road style definitions for Biomes O' Plenty biomes.
 */
public final class BopRoadStyleDefaults {
    private static final List<RoadStyleConfigEntry> DEFAULTS = List.of(
            entry(List.of(
                            "biomesoplenty:bayou",
                            "biomesoplenty:bog",
                            "biomesoplenty:floodplain",
                            "biomesoplenty:fen",
                            "biomesoplenty:marsh",
                            "biomesoplenty:muskeg",
                            "biomesoplenty:wetland",
                            "biomesoplenty:moor"
                    ),
                    palette(
                            block("minecraft:mud", 5),
                            block("biomesoplenty:mossy_black_sand", 3),
                            block("minecraft:packed_mud", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("biomesoplenty:black_sandstone", 1)
                    ),
                    decorations(fence("biomesoplenty:willow_fence"))),

            entry(List.of(
                            "biomesoplenty:rainforest",
                            "biomesoplenty:rocky_rainforest",
                            "biomesoplenty:tropics",
                            "biomesoplenty:undergrowth",
                            "biomesoplenty:overgrown_greens"
                    ),
                    palette(
                            block("biomesoplenty:brimstone_bricks", 4),
                            block("biomesoplenty:brimstone", 3),
                            block("minecraft:moss_block", 2),
                            block("minecraft:andesite", 1),
                            block("minecraft:gravel", 1)
                    ),
                    decorations(fence("biomesoplenty:palm_fence"))),

            entry(List.of(
                            "biomesoplenty:cold_desert",
                            "biomesoplenty:lush_desert",
                            "biomesoplenty:lush_savanna",
                            "biomesoplenty:scrubland",
                            "biomesoplenty:dryland",
                            "biomesoplenty:shrubland",
                            "biomesoplenty:rocky_shrubland"
                    ),
                    palette(
                            block("biomesoplenty:orange_sand", 6),
                            block("biomesoplenty:orange_sandstone", 3),
                            block("biomesoplenty:cut_orange_sandstone", 2),
                            block("minecraft:packed_mud", 1),
                            block("minecraft:terracotta", 1)
                    ),
                    decorations(fence("biomesoplenty:mahogany_fence"))),

            entry(List.of(
                            "biomesoplenty:dune_beach",
                            "biomesoplenty:gravel_beach"
                    ),
                    palette(
                            block("biomesoplenty:white_sand", 6),
                            block("biomesoplenty:white_sandstone", 3),
                            block("biomesoplenty:black_sand", 2),
                            block("minecraft:smooth_sandstone", 1),
                            block("biomesoplenty:cut_white_sandstone", 1)
                    ),
                    decorations(fence("biomesoplenty:palm_fence"))),

            entry(List.of(
                            "biomesoplenty:highland",
                            "biomesoplenty:jade_cliffs",
                            "biomesoplenty:crag",
                            "biomesoplenty:hot_springs"
                    ),
                    palette(
                            block("biomesoplenty:thermal_calcite", 4),
                            block("minecraft:calcite", 3),
                            block("minecraft:stone", 2),
                            block("minecraft:cobbled_deepslate", 1),
                            block("biomesoplenty:chiseled_white_sandstone", 1)
                    ),
                    decorations(fence("biomesoplenty:pine_fence"))),

            entry(List.of(
                            "biomesoplenty:coniferous_forest",
                            "biomesoplenty:snowy_coniferous_forest",
                            "biomesoplenty:fir_clearing",
                            "biomesoplenty:snowy_fir_clearing"
                    ),
                    palette(
                            block("minecraft:stone_bricks", 5),
                            block("minecraft:stone", 3),
                            block("minecraft:gravel", 2),
                            block("minecraft:andesite", 1),
                            block("minecraft:mossy_cobblestone", 1)
                    ),
                    decorations(fence("biomesoplenty:fir_fence"))),

            entry(List.of(
                            "biomesoplenty:redwood_forest",
                            "biomesoplenty:old_growth_woodland",
                            "biomesoplenty:woodland"
                    ),
                    palette(
                            block("minecraft:stone", 6),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:stone_bricks", 1),
                            block("minecraft:granite", 1)
                    ),
                    decorations(fence("biomesoplenty:redwood_fence"))),

            entry(List.of(
                            "biomesoplenty:maple_woods",
                            "biomesoplenty:snowy_maple_woods",
                            "biomesoplenty:seasonal_forest",
                            "biomesoplenty:snowblossom_grove",
                            "biomesoplenty:orchard",
                            "biomesoplenty:pasture",
                            "biomesoplenty:pumpkin_patch"
                    ),
                    palette(
                            block("biomesoplenty:white_sandstone", 5),
                            block("minecraft:dirt_path", 3),
                            block("minecraft:coarse_dirt", 2),
                            block("minecraft:granite", 1),
                            block("minecraft:mossy_stone_bricks", 1)
                    ),
                    decorations(fence("biomesoplenty:maple_fence"))),

            entry(List.of(
                            "biomesoplenty:field",
                            "biomesoplenty:forested_field",
                            "biomesoplenty:flower_field",
                            "biomesoplenty:lavender_field",
                            "biomesoplenty:grassland",
                            "biomesoplenty:prairie"
                    ),
                    palette(
                            block("minecraft:dirt_path", 5),
                            block("biomesoplenty:origin_grass_block", 3),
                            block("minecraft:stone", 1),
                            block("minecraft:gravel", 1),
                            block("minecraft:smooth_stone", 1)
                    ),
                    decorations(fence("biomesoplenty:jacaranda_fence"))),

            entry(List.of(
                            "biomesoplenty:auroral_garden",
                            "biomesoplenty:mystic_grove",
                            "biomesoplenty:glowing_grotto",
                            "biomesoplenty:crystalline_chasm"
                    ),
                    palette(
                            block("biomesoplenty:glowing_moss_block", 4),
                            block("biomesoplenty:glowshroom_block", 3),
                            block("minecraft:amethyst_block", 2),
                            block("minecraft:chiseled_deepslate", 1),
                            block("minecraft:end_stone_bricks", 1)
                    ),
                    decorations(fence("biomesoplenty:magic_fence"))),

            entry(List.of(
                            "biomesoplenty:origin_valley",
                            "biomesoplenty:wintry_origin_valley"
                    ),
                    palette(
                            block("biomesoplenty:origin_grass_block", 6),
                            block("minecraft:moss_block", 2),
                            block("minecraft:smooth_stone", 2),
                            block("minecraft:calcite", 1),
                            block("biomesoplenty:thermal_calcite", 1)
                    ),
                    decorations(fence("biomesoplenty:empyreal_fence"))),

            entry(List.of(
                            "biomesoplenty:fungal_jungle",
                            "biomesoplenty:spider_nest"
                    ),
                    palette(
                            block("minecraft:mycelium", 4),
                            block("biomesoplenty:glowshroom_block", 3),
                            block("biomesoplenty:stringy_cobweb", 2),
                            block("minecraft:deepslate_tiles", 1),
                            block("minecraft:mud", 1)
                    ),
                    decorations(fence("biomesoplenty:umbran_fence"))),

            entry(List.of(
                            "biomesoplenty:ominous_woods",
                            "biomesoplenty:dead_forest",
                            "biomesoplenty:old_growth_dead_forest"
                    ),
                    palette(
                            block("minecraft:deepslate_bricks", 5),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:mud", 1),
                            block("minecraft:deepslate", 1),
                            block("minecraft:stone", 1)
                    ),
                    decorations(fence("biomesoplenty:dead_fence"))),

            entry(List.of(
                            "biomesoplenty:wasteland",
                            "biomesoplenty:wasteland_steppe"
                    ),
                    palette(
                            block("biomesoplenty:dried_salt", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:gravel", 2),
                            block("minecraft:mud", 1),
                            block("minecraft:cobbled_deepslate", 1)
                    ),
                    decorations(fence("biomesoplenty:dead_fence"))),

            entry(List.of(
                            "biomesoplenty:visceral_heap",
                            "biomesoplenty:withered_abyss"
                    ),
                    palette(
                            block("biomesoplenty:flesh", 5),
                            block("biomesoplenty:porous_flesh", 3),
                            block("biomesoplenty:flesh_tendons", 2),
                            block("minecraft:netherrack", 1),
                            block("minecraft:blackstone", 1)
                    ),
                    decorations(fence("biomesoplenty:hellbark_fence"))),

            entry(List.of(
                            "biomesoplenty:volcanic_plains",
                            "biomesoplenty:volcano",
                            "biomesoplenty:erupting_inferno"
                    ),
                    palette(
                            block("biomesoplenty:brimstone", 5),
                            block("biomesoplenty:brimstone_bricks", 3),
                            block("minecraft:blackstone", 2),
                            block("minecraft:magma_block", 1),
                            block("minecraft:polished_basalt", 1)
                    ),
                    decorations(fence("biomesoplenty:hellbark_fence"))),

            entry(List.of(
                            "biomesoplenty:end_corruption",
                            "biomesoplenty:end_reef",
                            "biomesoplenty:end_wilds"
                    ),
                    palette(
                            block("biomesoplenty:algal_end_stone", 4),
                            block("biomesoplenty:unmapped_end_stone", 3),
                            block("biomesoplenty:null_end_stone", 2),
                            block("minecraft:end_stone_bricks", 1),
                            block("minecraft:purpur_block", 1)
                    ),
                    List.of()),

            entry(List.of(
                            "biomesoplenty:tundra",
                            "biomesoplenty:snowblossom_grove"
                    ),
                    palette(
                            block("minecraft:snow_block", 4),
                            block("minecraft:packed_ice", 3),
                            block("minecraft:stone", 2),
                            block("minecraft:gravel", 1),
                            block("minecraft:stone_bricks", 1)
                    ),
                    decorations(fence("biomesoplenty:pine_fence"))),

            entry(List.of(
                            "biomesoplenty:aspen_glade",
                            "biomesoplenty:eucalyptus_forest",
                            "biomesoplenty:jacaranda_glade",
                            "biomesoplenty:mediterranean_forest",
                            "biomesoplenty:ebony_woods"
                    ),
                    palette(
                            block("biomesoplenty:white_sandstone", 4),
                            block("biomesoplenty:cut_white_sandstone", 3),
                            block("minecraft:smooth_stone", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:granite", 1)
                    ),
                    decorations(fence("biomesoplenty:jacaranda_fence")))

    );

    private BopRoadStyleDefaults() {
    }

    public static List<RoadStyleConfigEntry> entries() {
        return DEFAULTS;
    }

    public static ArrayList<RoadArchitectConfigData.RoadStyleDefinition> createDefinitionCopies() {
        ArrayList<RoadArchitectConfigData.RoadStyleDefinition> list = new ArrayList<>();
        for (RoadStyleConfigEntry entry : DEFAULTS) {
            RoadArchitectConfigData.RoadStyleDefinition def = new RoadArchitectConfigData.RoadStyleDefinition();
            def.biomeSelectors = new ArrayList<>(entry.biomeSelectors());
            def.palette = new ArrayList<>();
            for (RoadStyleConfigEntry.SurfaceBlockEntry blockEntry : entry.palette()) {
                RoadArchitectConfigData.RoadPaletteEntry paletteEntry = new RoadArchitectConfigData.RoadPaletteEntry();
                paletteEntry.block = blockEntry.block();
                paletteEntry.weight = blockEntry.weight();
                def.palette.add(paletteEntry);
            }
            def.decorations = new ArrayList<>();
            for (RoadStyleConfigEntry.DecorationEntry decorationEntry : entry.decorations()) {
                RoadArchitectConfigData.RoadDecorationEntry deco = new RoadArchitectConfigData.RoadDecorationEntry();
                deco.type = decorationEntry.type();
                deco.block = decorationEntry.block();
                def.decorations.add(deco);
            }
            list.add(def);
        }
        return list;
    }

    private static RoadStyleConfigEntry entry(List<String> selectors,
                                              List<RoadStyleConfigEntry.SurfaceBlockEntry> palette,
                                              List<RoadStyleConfigEntry.DecorationEntry> decorations) {
        return new RoadStyleConfigEntry(selectors, palette, decorations);
    }

    private static List<RoadStyleConfigEntry.SurfaceBlockEntry> palette(RoadStyleConfigEntry.SurfaceBlockEntry... entries) {
        return List.of(entries);
    }

    private static List<RoadStyleConfigEntry.DecorationEntry> decorations(RoadStyleConfigEntry.DecorationEntry... entries) {
        return List.of(entries);
    }

    private static RoadStyleConfigEntry.SurfaceBlockEntry block(String id, int weight) {
        return new RoadStyleConfigEntry.SurfaceBlockEntry(id, weight);
    }

    private static RoadStyleConfigEntry.DecorationEntry fence(String blockId) {
        return new RoadStyleConfigEntry.DecorationEntry(RoadDecorationType.FENCE, blockId);
    }
}
