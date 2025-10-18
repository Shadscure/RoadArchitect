package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.config.LampPostConfigEntry;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.LampPostDefaults;
import net.oxcodsnet.roadarchitect.util.BiomeSelectorUtil;
import net.oxcodsnet.roadarchitect.util.PathDecorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolves configured lamp post definitions to runtime decorations.
 */
public final class LampPostConfigResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/LampPostConfig");

    private static final AtomicInteger VERSION = new AtomicInteger();
    private static volatile List<Override> OVERRIDES = List.of();

    private static final Map<Registry<Biome>, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private LampPostConfigResolver() {
    }

    static {
        RAConfigHolder.listen(LampPostConfigResolver::reload);
    }

    private static void reload(RAConfig config) {
        List<LampPostConfigEntry> entries = config.lampPostOverrides();
        List<LampPostConfigEntry> source = (entries == null || entries.isEmpty())
                ? LampPostDefaults.entries()
                : entries;

        ArrayList<Override> parsed = new ArrayList<>(source.size());
        for (LampPostConfigEntry entry : source) {
            if (entry == null) continue;
            BlockState base = resolveBlockState(entry.baseBlock(), "base block");
            BlockState post = resolveBlockState(entry.postBlock(), "post block");
            BlockState lamp = resolveBlockState(entry.lampBlock(), "lamp block");
            if (base == null || post == null || lamp == null) {
                LOGGER.warn("Skipping lamp post override due to missing block: base={}, post={}, lamp={}", entry.baseBlock(), entry.postBlock(), entry.lampBlock());
                continue;
            }
            LampPostDecoration decoration = new LampPostDecoration(base, post, lamp);
            parsed.add(new Override(entry.biomeSelectors(), decoration));
        }
        OVERRIDES = List.copyOf(parsed);
        VERSION.incrementAndGet();
        CACHE.clear();
    }

    public static LampPostDecoration resolve(StructureWorldAccess world, RegistryEntry<Biome> biome,
                                             LampPostDecoration fallback, String pathKey, long ordinal) {
        List<Override> overrides = OVERRIDES;
        if (overrides.isEmpty()) {
            return fallback;
        }
        Registry<Biome> registry = world.getRegistryManager().get(RegistryKeys.BIOME);
        CacheEntry cache = CACHE.compute(registry, (reg, existing) -> {
            int current = VERSION.get();
            if (existing != null && existing.version == current) {
                return existing;
            }
            List<CompiledOverride> compiled = new ArrayList<>(overrides.size());
            for (Override override : overrides) {
                List<String> selectors = override.selectors();
                List<RegistryEntryList<Biome>> compiledSelectors = selectors.isEmpty()
                        ? List.of()
                        : BiomeSelectorUtil.compile(reg, selectors);
                compiled.add(new CompiledOverride(override.decoration(), compiledSelectors));
            }
            return new CacheEntry(current, compiled);
        });

        ArrayList<LampPostDecoration> matches = null;
        ArrayList<LampPostDecoration> fallbacks = null;

        for (CompiledOverride entry : cache.overrides) {
            List<RegistryEntryList<Biome>> selectors = entry.selectors();
            if (selectors.isEmpty()) {
                if (fallbacks == null) fallbacks = new ArrayList<>();
                fallbacks.add(entry.decoration());
            } else if (BiomeSelectorUtil.matches(biome, selectors)) {
                if (matches == null) matches = new ArrayList<>();
                matches.add(entry.decoration());
            }
        }

        if (matches != null && !matches.isEmpty()) {
            int choice = PathDecorUtil.detInt(pathKey, ordinal, matches.size());
            return matches.get(choice);
        }
        if (fallbacks != null && !fallbacks.isEmpty()) {
            int choice = PathDecorUtil.detInt(pathKey, ordinal ^ 0x5F3759DFL, fallbacks.size());
            return fallbacks.get(choice);
        }
        return fallback;
    }

    private static BlockState resolveBlockState(String raw, String role) {
        if (raw == null || raw.isBlank()) {
            LOGGER.warn("Lamp post override {} is empty", role);
            return null;
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            LOGGER.warn("Lamp post override {} '{}' is not a valid identifier", role, raw);
            return null;
        }
        return Registries.BLOCK.getOrEmpty(id)
                .map(Block::getDefaultState)
                .orElseGet(() -> {
                    LOGGER.warn("Lamp post override {} '{}' is not registered", role, raw);
                    return null;
                });
    }

    private record Override(List<String> selectors, LampPostDecoration decoration) {
        private Override {
            selectors = selectors == null ? List.of() : List.copyOf(selectors);
        }
    }

    private record CacheEntry(int version, List<CompiledOverride> overrides) {
    }

    private record CompiledOverride(LampPostDecoration decoration, List<RegistryEntryList<Biome>> selectors) {
    }
}
