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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses and caches biome selector strings (e.g., "#minecraft:is_ocean" or "minecraft:badlands").
 */
public final class BiomeSelectorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/BiomeSelectorUtil");

    private BiomeSelectorUtil() {
    }

    /** Cache: registry -> (selector -> compiled list).
     *  Accessed from parallel pathfinding jobs — must be thread-safe. */
    private static final Map<Registry<Biome>, Map<String, RegistryEntryList<Biome>>> CACHE = new ConcurrentHashMap<>();

    public static List<RegistryEntryList<Biome>> compile(Registry<Biome> registry, List<String> selectors) {
        // Ensure per-registry cache map is concurrent
        Map<String, RegistryEntryList<Biome>> local = CACHE.computeIfAbsent(registry, r -> new ConcurrentHashMap<>());

        RegistryPredicateArgumentType<Biome> argType = new RegistryPredicateArgumentType<>(RegistryKeys.BIOME);
        List<RegistryEntryList<Biome>> compiled = new ArrayList<>(selectors.size());
        for (String raw : selectors) {
            if (raw == null || raw.isBlank()) continue;
            RegistryEntryList<Biome> list = local.get(raw);
            if (list == null) {
                try {
                    RegistryPredicateArgumentType.RegistryPredicate<Biome> predicate = argType.parse(new StringReader(raw));
                    RegistryEntryList<Biome> parsed = predicate.getKey()
                            .map(key -> registry.getEntry(key).map(RegistryEntryList::of), registry::getEntryList)
                            .orElse(null);
                    if (parsed != null) {
                        // Avoid race: if another thread put the same key meanwhile, use that value
                        RegistryEntryList<Biome> prev = local.putIfAbsent(raw, parsed);
                        list = (prev != null) ? prev : parsed;
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
