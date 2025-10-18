package net.oxcodsnet.roadarchitect.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in lamp post style definitions used when the user does not override them.
 */
public final class LampPostDefaults {
    private static final List<LampPostConfigEntry> DEFAULTS = List.of(
            entry("minecraft:cobblestone_wall", "minecraft:oak_fence", "minecraft:lantern",
                    "minecraft:plains"),
            entry("minecraft:cobblestone_wall", "minecraft:oak_fence", "minecraft:lantern",
                    "minecraft:stony_shore"),
            entry("minecraft:sandstone_wall", "minecraft:oak_fence", "minecraft:lantern",
                    "minecraft:beach"),
            entry("minecraft:mossy_cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:old_growth_pine_taiga"),
            entry("minecraft:mossy_cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:old_growth_spruce_taiga"),
            entry("minecraft:mossy_cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:taiga"),
            entry("minecraft:cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:snowy_taiga"),
            entry("minecraft:cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:snowy_beach"),
            entry("minecraft:cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:snowy_plains"),
            entry("minecraft:cobblestone_wall", "minecraft:spruce_fence", "minecraft:lantern",
                    "minecraft:snowy_slopes"),
            entry("minecraft:mossy_cobblestone_wall", "minecraft:oak_fence", "minecraft:lantern",
                    "minecraft:swamp"),
            entry("minecraft:mossy_cobblestone_wall", "minecraft:mangrove_fence", "minecraft:lantern",
                    "minecraft:mangrove_swamp"),
            entry("minecraft:cobblestone_wall", "minecraft:cherry_fence", "minecraft:lantern",
                    "minecraft:cherry_grove"),
            entry("minecraft:mossy_cobblestone_wall", "minecraft:dark_oak_fence", "minecraft:lantern",
                    "minecraft:dark_forest"),
            entry("minecraft:mud_brick_wall", "minecraft:acacia_fence", "minecraft:lantern",
                    "minecraft:savanna"),
            entry("minecraft:sandstone_wall", "minecraft:birch_fence", "minecraft:lantern",
                    "minecraft:desert"),
            entry("minecraft:red_sandstone_wall", "minecraft:acacia_fence", "minecraft:lantern",
                    "minecraft:badlands"),
            entry("minecraft:red_sandstone_wall", "minecraft:acacia_fence", "minecraft:lantern",
                    "minecraft:wooded_badlands"),
            entry("minecraft:red_sandstone_wall", "minecraft:acacia_fence", "minecraft:lantern",
                    "minecraft:eroded_badlands"),
            // Fallback style for any biome not covered above.
            entry("minecraft:cobblestone_wall", "minecraft:oak_fence", "minecraft:lantern")
    );

    private LampPostDefaults() {
    }

    /**
     * Returns the immutable list of built-in lamp post definitions.
     */
    public static List<LampPostConfigEntry> entries() {
        return DEFAULTS;
    }

    /**
     * Creates mutable config definitions mirroring the built-in defaults.
     */
    public static ArrayList<RoadArchitectConfigData.LampPostDefinition> createDefinitionCopies() {
        ArrayList<RoadArchitectConfigData.LampPostDefinition> list = new ArrayList<>();
        for (LampPostConfigEntry entry : DEFAULTS) {
            RoadArchitectConfigData.LampPostDefinition def = new RoadArchitectConfigData.LampPostDefinition();
            def.biomeSelectors = new ArrayList<>(entry.biomeSelectors());
            def.baseBlock = entry.baseBlock();
            def.postBlock = entry.postBlock();
            def.lampBlock = entry.lampBlock();
            list.add(def);
        }
        return list;
    }

    private static LampPostConfigEntry entry(String base, String post, String lamp, String... selectors) {
        return new LampPostConfigEntry(List.of(selectors), base, post, lamp);
    }
}
