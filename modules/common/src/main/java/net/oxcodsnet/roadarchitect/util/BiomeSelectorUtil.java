package net.oxcodsnet.roadarchitect.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and caches biome selector strings (e.g., "#minecraft:is_ocean" or "minecraft:badlands").
 */
public final class BiomeSelectorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/BiomeSelectorUtil");

    private BiomeSelectorUtil() {
    }

    /** Cache: registry -> (selector -> compiled list). */
    private static final Map<Registry<Biome>, Map<String, RegistryEntryList<Biome>>> CACHE = new HashMap<>();

    public static List<RegistryEntryList<Biome>> compile(Registry<Biome> registry, List<String> selectors) {
        Map<String, RegistryEntryList<Biome>> local = CACHE.computeIfAbsent(registry, r -> new HashMap<>(selectors.size() * 2));

        RegistryPredicateArgumentType<Biome> argType = new RegistryPredicateArgumentType<>(RegistryKeys.BIOME);
        List<RegistryEntryList<Biome>> compiled = new ArrayList<>(selectors.size());
        for (String raw : selectors) {
            if (raw == null || raw.isBlank()) continue;
            RegistryEntryList<Biome> list = local.get(raw);
            if (list == null) {
                try {
                    var predicate = argType.parse(new StringReader(raw));
                    list = predicate.getKey()
                            .map(key -> registry.getEntry(key).map(RegistryEntryList::of), registry::getEntryList)
                            .orElse(null);
                    if (list != null) {
                        local.put(raw, list);
                    } else {
                        LOGGER.warn("Biome selector '{}' resolved to nothing", raw);
                        continue;
                    }
                } catch (CommandSyntaxException ex) {
                    LOGGER.warn("Biome selector '{}' is invalid: {}", raw, ex.getMessage());
                    continue;
                }
            }
            compiled.add(list);
        }
        return compiled;
    }

    public static boolean matches(RegistryEntry<Biome> biome, List<RegistryEntryList<Biome>> lists) {
        for (RegistryEntryList<Biome> list : lists) {
            if (list.contains(biome)) {
                return true;
            }
        }
        return false;
    }
}
