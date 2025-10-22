package net.oxcodsnet.roadarchitect.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/**
 * Parses and caches biome selector strings (e.g., "#minecraft:is_ocean" or "minecraft:badlands").
 */
public final class BiomeSelectorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/BiomeSelectorUtil");

    private BiomeSelectorUtil() {
    }

    /** Cache: registry -> (selector -> compiled list).
     *  Accessed from parallel pathfinding jobs — must be thread-safe. */
    private static final Map<Registry<Biome>, Map<String, HolderSet<Biome>>> CACHE = new ConcurrentHashMap<>();

    public static List<HolderSet<Biome>> compile(Registry<Biome> registry, List<String> selectors) {
        // Ensure per-registry cache map is concurrent
        Map<String, HolderSet<Biome>> local = CACHE.computeIfAbsent(registry, r -> new ConcurrentHashMap<>());

        ResourceOrTagKeyArgument<Biome> argType = new ResourceOrTagKeyArgument<>(Registries.BIOME);
        List<HolderSet<Biome>> compiled = new ArrayList<>(selectors.size());
        for (String raw : selectors) {
            if (raw == null || raw.isBlank()) continue;
            HolderSet<Biome> list = local.get(raw);
            if (list == null) {
                try {
                    ResourceOrTagKeyArgument.Result<Biome> predicate = argType.parse(new StringReader(raw));
                    HolderSet<Biome> parsed = predicate.unwrap()
                            .map(key -> registry.getHolder(key).map(HolderSet::direct), registry::getTag)
                            .orElse(null);
                    if (parsed != null) {
                        // Avoid race: if another thread put the same key meanwhile, use that value
                        HolderSet<Biome> prev = local.putIfAbsent(raw, parsed);
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

    public static boolean matches(Holder<Biome> biome, List<HolderSet<Biome>> lists) {
        for (HolderSet<Biome> list : lists) {
            if (list.contains(biome)) {
                return true;
            }
        }
        return false;
    }
}
