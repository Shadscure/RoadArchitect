package net.oxcodsnet.roadarchitect.worldgen.style;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadDecorationType;
import net.oxcodsnet.roadarchitect.config.RoadStyleConfigEntry;
import net.oxcodsnet.roadarchitect.config.RoadStyleDefaults;
import net.oxcodsnet.roadarchitect.handlers.compat.BopCompat;
import net.oxcodsnet.roadarchitect.util.BiomeSelectorUtil;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.Decoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.FenceDecoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides biome specific road styles compiled from configuration data.
 */
public final class RoadStyles {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/RoadStyles");

    private static final AtomicInteger VERSION = new AtomicInteger();
    private static final Map<Registry<Biome>, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static volatile List<ParsedStyle> STYLES = List.of();
    private static volatile RoadStyle FALLBACK = buildFallbackFromDefaults();

    static {
        RAConfigHolder.listen(RoadStyles::reload);
    }

    private RoadStyles() {
    }

    public static RoadStyle forBiome(Registry<Biome> registry, RegistryEntry<Biome> biomeEntry) {
        if (registry == null || biomeEntry == null) {
            return FALLBACK;
        }
        CacheEntry cache = CACHE.compute(registry, (reg, existing) -> {
            int current = VERSION.get();
            if (existing != null && existing.version == current) {
                return existing;
            }
            List<CompiledStyle> compiled = new ArrayList<>(STYLES.size());
            for (ParsedStyle parsed : STYLES) {
                List<String> selectors = parsed.selectors();
                List<RegistryEntryList<Biome>> compiledSelectors = selectors.isEmpty()
                        ? List.of()
                        : BiomeSelectorUtil.compile(reg, selectors);
                compiled.add(new CompiledStyle(parsed.style(), compiledSelectors));
            }
            return new CacheEntry(current, List.copyOf(compiled));
        });

        RoadStyle fallback = FALLBACK;
        for (CompiledStyle compiled : cache.styles()) {
            List<RegistryEntryList<Biome>> selectors = compiled.selectors();
            if (selectors.isEmpty()) {
                fallback = compiled.style();
                continue;
            }
            if (BiomeSelectorUtil.matches(biomeEntry, selectors)) {
                return compiled.style();
            }
        }
        return fallback;
    }

    private static void reload(RAConfig config) {
        List<RoadStyleConfigEntry> entries = config.roadStyleOverrides();
        List<RoadStyleConfigEntry> baseSource = (entries == null || entries.isEmpty())
                ? RoadStyleDefaults.entries()
                : entries;
        List<RoadStyleConfigEntry> bopEntries = List.of();
        if (BopCompat.isPresent()) {
            List<RoadStyleConfigEntry> bopOverrides = config.bopRoadStyleOverrides();
            if (bopOverrides != null && !bopOverrides.isEmpty()) {
                bopEntries = bopOverrides;
            }
        }
        ArrayList<RoadStyleConfigEntry> source = new ArrayList<>(baseSource.size() + bopEntries.size());
        source.addAll(baseSource);
        source.addAll(bopEntries);

        ArrayList<ParsedStyle> parsed = new ArrayList<>(source.size());
        RoadStyle fallback = null;

        for (RoadStyleConfigEntry entry : source) {
            if (entry == null) {
                continue;
            }
            RoadStyle style = buildStyle(entry);
            if (style == null) {
                LOGGER.warn("Skipping road style override for selectors {} due to invalid palette", entry.biomeSelectors());
                continue;
            }
            parsed.add(new ParsedStyle(entry.biomeSelectors(), style));
            if (entry.biomeSelectors().isEmpty()) {
                fallback = style;
            }
        }

        if (fallback == null) {
            fallback = buildFallbackFromDefaults();
        }

        STYLES = List.copyOf(parsed);
        FALLBACK = fallback;
        VERSION.incrementAndGet();
        CACHE.clear();
    }

    private static RoadStyle buildFallbackFromDefaults() {
        RoadStyleConfigEntry defaultEntry = RoadStyleDefaults.entries().getFirst();
        RoadStyle style = buildStyle(defaultEntry);
        if (style != null) {
            return style;
        }
        BlockPalette palette = BlockPalette.builder()
                .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                .add(Blocks.DIRT_PATH.getDefaultState(), 2)
                .add(Blocks.COBBLESTONE.getDefaultState(), 2)
                .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                .add(Blocks.GRAVEL.getDefaultState(), 1)
                .build();
        return new RoadStyle(palette, List.of());
    }

    private static RoadStyle buildStyle(RoadStyleConfigEntry entry) {
        BlockPalette palette = buildPalette(entry.palette());
        if (palette == null) {
            return null;
        }
        List<Decoration> decorations = buildDecorations(entry.decorations());
        return new RoadStyle(palette, decorations);
    }

    private static BlockPalette buildPalette(List<RoadStyleConfigEntry.SurfaceBlockEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        BlockPalette.Builder builder = BlockPalette.builder();
        int added = 0;
        for (RoadStyleConfigEntry.SurfaceBlockEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            int weight = entry.weight();
            if (weight <= 0) {
                continue;
            }
            String raw = entry.block();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (raw.startsWith("#")) {
                String tagName = raw.substring(1);
                Identifier id = Identifier.tryParse(tagName);
                if (id == null) {
                    LOGGER.warn("Road style palette tag '{}' is invalid", raw);
                    continue;
                }
                TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, id);
                Optional<RegistryEntryList.Named<Block>> optional = Registries.BLOCK.getEntryList(tag);
                if (optional.isEmpty()) {
                    LOGGER.warn("Road style palette tag '{}' resolved to nothing", raw);
                    continue;
                }
                RegistryEntryList<Block> list = optional.get();
                int before = added;
                for (RegistryEntry<Block> blockEntry : list) {
                    Block block = blockEntry.value();
                    builder.add(block.getDefaultState(), weight);
                    added++;
                }
                if (added == before) {
                    LOGGER.warn("Road style palette tag '{}' had no resolvable blocks", raw);
                }
            } else {
                Identifier id = Identifier.tryParse(raw);
                if (id == null) {
                    LOGGER.warn("Road style palette block '{}' is invalid", raw);
                    continue;
                }
                Optional<Block> optional = Registries.BLOCK.getOrEmpty(id);
                if (optional.isEmpty()) {
                    LOGGER.warn("Road style palette block '{}' is not registered", raw);
                    continue;
                }
                builder.add(optional.get().getDefaultState(), weight);
                added++;
            }
        }
        if (added == 0) {
            return null;
        }
        return builder.build();
    }

    private static List<Decoration> buildDecorations(List<RoadStyleConfigEntry.DecorationEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        ArrayList<Decoration> list = new ArrayList<>();
        for (RoadStyleConfigEntry.DecorationEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            RoadDecorationType type = entry.type();
            if (type == null) {
                continue;
            }
            switch (type) {
                case FENCE -> {
                    BlockState state = resolveBlockState(entry.block(), "fence decoration");
                    if (state != null) {
                        list.add(new FenceDecoration(state));
                    }
                }
                case NONE -> {
                    // Explicit opt-out: ignore entry.
                }
                default -> LOGGER.warn("Unknown road decoration type '{}'", type.id());
            }
        }
        return list.isEmpty() ? List.of() : List.copyOf(list);
    }

    private static BlockState resolveBlockState(String raw, String role) {
        if (raw == null || raw.isBlank()) {
            LOGGER.warn("Road style {} is empty", role);
            return null;
        }
        if (raw.startsWith("#")) {
            String tagName = raw.substring(1);
            Identifier id = Identifier.tryParse(tagName);
            if (id == null) {
                LOGGER.warn("Road style {} tag '{}' is invalid", role, raw);
                return null;
            }
            TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, id);
            Optional<RegistryEntryList.Named<Block>> optional = Registries.BLOCK.getEntryList(tag);
            if (optional.isEmpty()) {
                LOGGER.warn("Road style {} tag '{}' resolved to nothing", role, raw);
                return null;
            }
            RegistryEntryList<Block> list = optional.get();
            for (RegistryEntry<Block> blockEntry : list) {
                return blockEntry.value().getDefaultState();
            }
            LOGGER.warn("Road style {} tag '{}' had no blocks", role, raw);
            return null;
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            LOGGER.warn("Road style {} '{}' is invalid", role, raw);
            return null;
        }
        Optional<Block> optional = Registries.BLOCK.getOrEmpty(id);
        if (optional.isEmpty()) {
            LOGGER.warn("Road style {} '{}' is not registered", role, raw);
            return null;
        }
        return optional.get().getDefaultState();
    }

    private record ParsedStyle(List<String> selectors, RoadStyle style) {
    }

    private record CompiledStyle(RoadStyle style, List<RegistryEntryList<Biome>> selectors) {
    }

    private record CacheEntry(int version, List<CompiledStyle> styles) {
    }
}
