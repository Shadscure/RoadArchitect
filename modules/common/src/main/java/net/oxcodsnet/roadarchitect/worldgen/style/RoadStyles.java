package net.oxcodsnet.roadarchitect.worldgen.style;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadDecorationType;
import net.oxcodsnet.roadarchitect.config.records.RoadStyleConfigEntry;
import net.oxcodsnet.roadarchitect.config.defaults.RoadStyleDefaults;
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

    public static RoadStyle forBiome(Registry<Biome> registry, Holder<Biome> biomeEntry) {
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
                List<HolderSet<Biome>> compiledSelectors = selectors.isEmpty()
                        ? List.of()
                        : BiomeSelectorUtil.compile(reg, selectors);
                compiled.add(new CompiledStyle(parsed.style(), compiledSelectors));
            }
            return new CacheEntry(current, List.copyOf(compiled));
        });

        RoadStyle fallback = FALLBACK;
        for (CompiledStyle compiled : cache.styles()) {
            List<HolderSet<Biome>> selectors = compiled.selectors();
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
        List<RoadStyleConfigEntry> defaults = RoadStyleDefaults.entries();
        if (defaults.isEmpty()) {
            throw new IllegalStateException("RoadStyleDefaults.entries() returned an empty list");
        }
        RoadStyleConfigEntry defaultEntry = defaults.get(0);
        RoadStyle style = buildStyle(defaultEntry);
        if (style != null) {
            return style;
        }
        BlockPalette palette = BlockPalette.builder()
                .add(Blocks.GRASS_BLOCK.defaultBlockState(), 7)
                .add(Blocks.DIRT_PATH.defaultBlockState(), 2)
                .add(Blocks.COBBLESTONE.defaultBlockState(), 2)
                .add(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 1)
                .add(Blocks.GRAVEL.defaultBlockState(), 1)
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
                ResourceLocation id = ResourceLocation.tryParse(tagName);
                if (id == null) {
                    LOGGER.warn("Road style palette tag '{}' is invalid", raw);
                    continue;
                }
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
                Optional<HolderSet.Named<Block>> optional = BuiltInRegistries.BLOCK.getTag(tag);
                if (optional.isEmpty()) {
                    LOGGER.warn("Road style palette tag '{}' resolved to nothing", raw);
                    continue;
                }
                HolderSet<Block> list = optional.get();
                int before = added;
                for (Holder<Block> blockEntry : list) {
                    Block block = blockEntry.value();
                    builder.add(block.defaultBlockState(), weight);
                    added++;
                }
                if (added == before) {
                    LOGGER.warn("Road style palette tag '{}' had no resolvable blocks", raw);
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id == null) {
                    LOGGER.warn("Road style palette block '{}' is invalid", raw);
                    continue;
                }
                Optional<Block> optional = BuiltInRegistries.BLOCK.getOptional(id);
                if (optional.isEmpty()) {
                    LOGGER.warn("Road style palette block '{}' is not registered", raw);
                    continue;
                }
                builder.add(optional.get().defaultBlockState(), weight);
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
            ResourceLocation id = ResourceLocation.tryParse(tagName);
            if (id == null) {
                LOGGER.warn("Road style {} tag '{}' is invalid", role, raw);
                return null;
            }
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
            Optional<HolderSet.Named<Block>> optional = BuiltInRegistries.BLOCK.getTag(tag);
            if (optional.isEmpty()) {
                LOGGER.warn("Road style {} tag '{}' resolved to nothing", role, raw);
                return null;
            }
            HolderSet<Block> list = optional.get();
            for (Holder<Block> blockEntry : list) {
                return blockEntry.value().defaultBlockState();
            }
            LOGGER.warn("Road style {} tag '{}' had no blocks", role, raw);
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            LOGGER.warn("Road style {} '{}' is invalid", role, raw);
            return null;
        }
        Optional<Block> optional = BuiltInRegistries.BLOCK.getOptional(id);
        if (optional.isEmpty()) {
            LOGGER.warn("Road style {} '{}' is not registered", role, raw);
            return null;
        }
        return optional.get().defaultBlockState();
    }

    private record ParsedStyle(List<String> selectors, RoadStyle style) {
    }

    private record CompiledStyle(RoadStyle style, List<HolderSet<Biome>> selectors) {
    }

    private record CacheEntry(int version, List<CompiledStyle> styles) {
    }
}
