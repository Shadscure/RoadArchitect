package net.oxcodsnet.roadarchitect.config.defaults;

import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.config.RoadDecorationType;
import net.oxcodsnet.roadarchitect.config.RoadStyleConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in road style definitions used as defaults and config seeds.
 */
public final class RoadStyleDefaults {
    private static final List<RoadStyleConfigEntry> DEFAULTS = List.of(
            entry(List.of(),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:cobblestone", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:gravel", 1)
                    ),
                    List.of()),

            entry(List.of("minecraft:river"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:sand", 1),
                            block("minecraft:gravel", 1)
                    ),
                    decorations(fence("minecraft:oak_fence"))),

            entry(List.of("minecraft:stony_shore"),
                    palette(
                            block("minecraft:stone", 7),
                            block("minecraft:cobblestone", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:gravel", 1)
                    ),
                    decorations(fence("minecraft:oak_fence"))),

            entry(List.of("minecraft:beach"),
                    palette(
                            block("minecraft:sand", 7),
                            block("minecraft:cobblestone", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:gravel", 1)
                    ),
                    decorations(fence("minecraft:oak_fence"))),

            entry(List.of("minecraft:old_growth_pine_taiga"),
                    palette(
                            block("minecraft:podzol", 7),
                            block("minecraft:cobblestone", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:gravel", 1),
                            block("minecraft:tuff", 1),
                            block("minecraft:andesite", 1)
                    ),
                    decorations(fence("minecraft:spruce_fence"))),

            entry(List.of("minecraft:old_growth_spruce_taiga"),
                    palette(
                            block("minecraft:podzol", 7),
                            block("minecraft:cobblestone", 2),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:gravel", 1),
                            block("minecraft:tuff", 1),
                            block("minecraft:andesite", 1)
                    ),
                    decorations(fence("minecraft:spruce_fence"))),

            entry(List.of("minecraft:taiga"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:moss_block", 1),
                            block("minecraft:stone", 1),
                            block("minecraft:cobblestone", 1)
                    ),
                    decorations(fence("minecraft:spruce_fence"))),

            entry(List.of("minecraft:snowy_taiga"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:moss_block", 1),
                            block("minecraft:stone", 1),
                            block("minecraft:cobblestone", 1)
                    ),
                    decorations(fence("minecraft:spruce_fence"))),

            entry(List.of("minecraft:snowy_beach"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:moss_block", 1),
                            block("minecraft:stone", 1),
                            block("minecraft:cobblestone", 1)
                    ),
                    decorations(fence("minecraft:spruce_fence"))),

            entry(List.of("minecraft:snowy_plains"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:moss_block", 1),
                            block("minecraft:stone", 1),
                            block("minecraft:cobblestone", 1)
                    ),
                    decorations(fence("minecraft:spruce_fence"))),

            entry(List.of("minecraft:snowy_slopes"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:moss_block", 1),
                            block("minecraft:stone", 1),
                            block("minecraft:cobblestone", 1)
                    ),
                    List.of()),

            entry(List.of("minecraft:swamp"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:tuff", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:cracked_stone_bricks", 1),
                            block("minecraft:moss_block", 1)
                    ),
                    decorations(fence("minecraft:oak_fence"))),

            entry(List.of("minecraft:mangrove_swamp"),
                    palette(
                            block("minecraft:mud", 7),
                            block("minecraft:tuff", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1),
                            block("minecraft:cracked_stone_bricks", 1),
                            block("minecraft:moss_block", 1)
                    ),
                    decorations(fence("minecraft:mangrove_fence"))),

            entry(List.of("minecraft:plains"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone", 1),
                            block("minecraft:andesite", 1),
                            block("minecraft:cracked_stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1)
                    ),
                    decorations(fence("minecraft:oak_fence"))),

            entry(List.of("minecraft:cherry_grove"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:stone", 1),
                            block("minecraft:andesite", 1),
                            block("minecraft:cracked_stone_bricks", 1),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:mossy_stone_bricks", 1)
                    ),
                    decorations(fence("minecraft:cherry_fence"))),

            entry(List.of("minecraft:dark_forest"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:mossy_cobblestone", 1),
                            block("minecraft:moss_block", 1),
                            block("minecraft:stone", 1),
                            block("minecraft:cobblestone", 1)
                    ),
                    decorations(fence("minecraft:dark_oak_fence"))),

            entry(List.of("minecraft:savanna"),
                    palette(
                            block("minecraft:dirt_path", 4),
                            block("minecraft:coarse_dirt", 3),
                            block("minecraft:coarse_dirt", 1),
                            block("minecraft:andesite", 1),
                            block("minecraft:gravel", 1)
                    ),
                    decorations(fence("minecraft:acacia_fence"))),

            entry(List.of("minecraft:desert"),
                    palette(
                            block("minecraft:smooth_sandstone", 7),
                            block("minecraft:suspicious_sand", 2),
                            block("minecraft:packed_mud", 2)
                    ),
                    decorations(fence("minecraft:sandstone_wall"))),

            entry(List.of("minecraft:badlands"),
                    palette(
                            block("minecraft:red_sandstone", 7),
                            block("minecraft:red_sand", 2),
                            block("minecraft:packed_mud", 2)
                    ),
                    decorations(fence("minecraft:red_sandstone_wall"))),

            entry(List.of("minecraft:wooded_badlands"),
                    palette(
                            block("minecraft:red_sandstone", 7),
                            block("minecraft:red_sand", 2),
                            block("minecraft:packed_mud", 2)
                    ),
                    decorations(fence("minecraft:red_sandstone_wall"))),

            entry(List.of("minecraft:eroded_badlands"),
                    palette(
                            block("minecraft:red_sandstone", 7),
                            block("minecraft:red_sand", 2),
                            block("minecraft:packed_mud", 2)
                    ),
                    decorations(fence("minecraft:red_sandstone_wall")))
    );

    private RoadStyleDefaults() {
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
