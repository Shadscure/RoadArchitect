package net.oxcodsnet.roadarchitect.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.StructurePresence;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Параллельный локатор структур для Minecraft 1.21.1 (Yarn).
 * <p>
 * Архитектура в две фазы:
 * <ol>
 *   <li><b>Планирование кандидатов (off-thread)</b> — чистая математика по placement’ам, вычисляет набор ChunkPos для проверки.</li>
 *   <li><b>Разрешение кандидатов (main thread)</b> — батчево проверяет presence и при необходимости один раз грузит чанк
 *       для нескольких структур, минимизируя обращения к миру.</li>
 * </ol>
 * Потокобезопасность: любая операция, потенциально ведущая к загрузке чанка или доступу к {@link StructureAccessor},
 * выполняется только на главном треде сервера.
 */
public final class StructureLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + StructureLocator.class.getSimpleName());

    private static final DynamicCommandExceptionType INVALID_STRUCTURE_EXCEPTION =
            new DynamicCommandExceptionType(id -> Text.translatable("commands.locate.structure.invalid", id));

    private StructureLocator() {}

    // Управление интенсивностью планирования для кольцевых плейсментов
    private static final int MAX_RING_CANDIDATES_PER_CELL = 16; // можно поднять для большей надёжности

    /* ───────────────────────────── Selector cache ───────────────────────────── */

    /** Кэш: реестр -> (строка селектора -> готовый список структур). */
    private static final Map<Registry<Structure>, Map<String, RegistryEntryList<Structure>>> SELECTOR_CACHE = new WeakHashMap<>();

    private static List<RegistryEntryList<Structure>> compileSelectors(Registry<Structure> registry, List<String> selectors) {
        Map<String, RegistryEntryList<Structure>> cache =
                SELECTOR_CACHE.computeIfAbsent(registry, r -> new HashMap<>(selectors.size() * 2));

        RegistryPredicateArgumentType<Structure> argType = new RegistryPredicateArgumentType<>(RegistryKeys.STRUCTURE);
        List<RegistryEntryList<Structure>> compiled = new ArrayList<>(selectors.size());

        for (String raw : selectors) {
            RegistryEntryList<Structure> list = cache.get(raw);
            if (list == null) {
                try {
                    var predicate = argType.parse(new StringReader(raw));
                    list = getStructureList(predicate, registry)
                            .orElseThrow(() -> INVALID_STRUCTURE_EXCEPTION.create(raw));
                    cache.put(raw, list);
                } catch (CommandSyntaxException ex) {
                    LOGGER.warn("Structure selector '{}' is invalid: {}", raw, ex.getMessage());
                    continue;
                }
            }
            compiled.add(list);
        }
        return compiled;
    }

    /* ───────────────────────────── Public API ───────────────────────────── */

    /**
     * Сканирует область сеткой; планирование — параллельно, проверка — на главном треде; затем сохраняет найденные узлы.
     * Возвращает список найденных (позиция, id структуры).
     */
    public static List<Pair<BlockPos, String>> scanGridAsync(ServerWorld world, BlockPos origin, int overallRadius, int scanRadius, List<String> structureSelectors) {
        return scanGridAsync(world, origin, overallRadius, scanRadius, structureSelectors, true);
    }

    /**
     * Сканирует область сеткой с опцией запрета загрузки чанков при разрешении кандидатов.
     */
    public static List<Pair<BlockPos, String>> scanGridAsync(ServerWorld world, BlockPos origin, int overallRadius,
                                                             int scanRadius, List<String> structureSelectors,
                                                             boolean allowChunkLoads) {
        PipelineProfiler.increment("structure_locator.invocations");
        PipelineProfiler.recordValue("structure_locator.selector_input", structureSelectors.size());
        PipelineProfiler.recordValue("structure_locator.overall_radius", overallRadius);
        PipelineProfiler.recordValue("structure_locator.scan_radius", scanRadius);
        PipelineProfiler.increment("structure_locator.allow_chunk_loads." + (allowChunkLoads ? "enabled" : "disabled"));

        try (PipelineProfiler.Section total = PipelineProfiler.openSection("structure_locator.total")) {
            final Registry<Structure> registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
            final List<RegistryEntryList<Structure>> compiledSelectors;
            try (PipelineProfiler.Section compile = PipelineProfiler.openSection("structure_locator.compile_selectors")) {
                compiledSelectors = compileSelectors(registry, structureSelectors);
            }
            PipelineProfiler.recordValue("structure_locator.compiled_selector_groups", compiledSelectors.size());
            if (compiledSelectors.isEmpty()) {
                return Collections.emptyList();
            }

            final PlacementIndex index;
            try (PipelineProfiler.Section indexSection = PipelineProfiler.openSection("structure_locator.build_index")) {
                index = buildPlacementIndex(world, compiledSelectors);
            }

            final int baseStep = scanRadius * 2 + 1; // чанки
            final int step = computeGridStepChunks(index, baseStep);

            final int originChunkX = origin.getX() >> 4;
            final int originChunkZ = origin.getZ() >> 4;

            ArrayList<Cell> cells = new ArrayList<>();
            for (int dx = -overallRadius; dx <= overallRadius; dx += step) {
                for (int dz = -overallRadius; dz <= overallRadius; dz += step) {
                    int cx = originChunkX + dx;
                    int cz = originChunkZ + dz;
                    BlockPos cellCenter = new BlockPos((cx << 4) + 8, origin.getY(), (cz << 4) + 8);
                    cells.add(new Cell(cx, cz, cellCenter));
                }
            }
            PipelineProfiler.recordValue("structure_locator.grid_cells", cells.size());

            List<Candidate> planned;
            try (PipelineProfiler.Section planning = PipelineProfiler.openSection("structure_locator.plan_candidates")) {
                planned = ForkJoinPool.commonPool().submit(() ->
                        cells.parallelStream()
                                .flatMap(cell -> planCandidatesForCell(index, cell, scanRadius))
                                .collect(Collectors.toCollection(ArrayList::new))
                ).join();
            }
            PipelineProfiler.recordValue("structure_locator.planned_candidates", planned.size());

            List<Pair<BlockPos, String>> found;
            try (PipelineProfiler.Section resolve = PipelineProfiler.openSection("structure_locator.resolve_candidates")) {
                found = resolveCandidatesOnMainThread(world, registry, index, planned, allowChunkLoads);
            }
            PipelineProfiler.recordValue("structure_locator.resolved_structures", found.size());

            schedulePersistence(world, found);
            return found;
        }
    }

    /* ───────────────────────────── Placement index & planning ───────────────────────────── */

    private record PlacementIndex(
            Map<RandomSpreadStructurePlacement, Set<RegistryEntry<Structure>>> randomGroups,
            Map<ConcentricRingsStructurePlacement, Set<RegistryEntry<Structure>>> ringGroups,
            Map<ConcentricRingsStructurePlacement, List<ChunkPos>> ringPositions,
            long structureSeed
    ) {}

    private static PlacementIndex buildPlacementIndex(ServerWorld world, List<RegistryEntryList<Structure>> compiledSelectors) {
        StructurePlacementCalculator calc = world.getChunkManager().getStructurePlacementCalculator();

        Map<RandomSpreadStructurePlacement, Set<RegistryEntry<Structure>>> randomGroups = new HashMap<>();
        Map<ConcentricRingsStructurePlacement, Set<RegistryEntry<Structure>>> ringGroups = new HashMap<>();
        Map<ConcentricRingsStructurePlacement, List<ChunkPos>> ringPositions = new HashMap<>();

        for (RegistryEntryList<Structure> list : compiledSelectors) {
            for (RegistryEntry<Structure> entry : list) {
                for (StructurePlacement p : calc.getPlacements(entry)) {
                    if (p instanceof RandomSpreadStructurePlacement rsp) {
                        randomGroups.computeIfAbsent(rsp, __ -> new HashSet<>()).add(entry);
                    } else if (p instanceof ConcentricRingsStructurePlacement cr) {
                        ringGroups.computeIfAbsent(cr, __ -> new HashSet<>()).add(entry);
                    }
                }
            }
        }

        // Предвычислить список позиций колец для каждой кольцевой схемы
        for (ConcentricRingsStructurePlacement cr : ringGroups.keySet()) {
            List<ChunkPos> positions = calc.getPlacementPositions(cr);
            if (positions == null) throw new IllegalStateException("Missing ring placement positions");
            ringPositions.put(cr, positions);
        }

        return new PlacementIndex(randomGroups, ringGroups, ringPositions, calc.getStructureSeed());
    }

    private record Cell(int centerChunkX, int centerChunkZ, BlockPos worldCenter) {}

    private record Candidate(ChunkPos pos, int prioritySq, StructurePlacement placement, Set<RegistryEntry<Structure>> structs) {}

    private static Stream<Candidate> planCandidatesForCell(PlacementIndex index, Cell cell, int radius) {
        List<Candidate> out = new ArrayList<>();

        // RandomSpread: точно повторяем геометрию обхода границы «квадратных колец»
        for (Map.Entry<RandomSpreadStructurePlacement, Set<RegistryEntry<Structure>>> e : index.randomGroups.entrySet()) {
            RandomSpreadStructurePlacement placement = e.getKey();
            int spacing = placement.getSpacing();

            for (int k = 0; k <= radius; k++) {
                for (int dj = -k; dj <= k; dj++) {
                    boolean edgeZ = (dj == -k) || (dj == k);
                    for (int di = -k; di <= k; di++) {
                        boolean edgeX = (di == -k) || (di == k);
                        if (!(edgeX || edgeZ)) continue; // только граница кольца

                        int l = cell.centerChunkX + spacing * di;
                        int m = cell.centerChunkZ + spacing * dj;
                        ChunkPos start = placement.getStartChunk(index.structureSeed, l, m);
                        int px = (start.getStartX() + 8);
                        int pz = (start.getStartZ() + 8);
                        int dist2 = sq(px - cell.worldCenter.getX()) + sq(pz - cell.worldCenter.getZ());
                        out.add(new Candidate(start, dist2, placement, e.getValue()));
                    }
                }
            }
        }

        // ConcentricRings: берём несколько ближайших к ячейке позиций из предвычисленного списка
        for (Map.Entry<ConcentricRingsStructurePlacement, Set<RegistryEntry<Structure>>> e : index.ringGroups.entrySet()) {
            ConcentricRingsStructurePlacement placement = e.getKey();
            List<ChunkPos> positions = index.ringPositions.get(placement);
            if (positions == null || positions.isEmpty()) continue;

            positions.stream()
                    .map(cp -> {
                        int px = (ChunkSectionPos.getOffsetPos(cp.x, 8));
                        int pz = (ChunkSectionPos.getOffsetPos(cp.z, 8));
                        int d2 = sq(px - cell.worldCenter.getX()) + sq(pz - cell.worldCenter.getZ());
                        return new Object[]{cp, d2};
                    })
                    .sorted(Comparator.comparingInt(o -> (int) o[1]))
                    .limit(MAX_RING_CANDIDATES_PER_CELL)
                    .forEach(o -> out.add(new Candidate((ChunkPos) o[0], (int) o[1], placement, e.getValue())));
        }

        // Чем ближе к центру ячейки — тем выше приоритет
        out.sort(Comparator.comparingInt(Candidate::prioritySq));
        return out.stream();
    }

    private static int sq(int v) { return v * v; }

    /* ───────────────────────────── Resolution (main thread only) ───────────────────────────── */

    private static List<Pair<BlockPos, String>> resolveCandidatesOnMainThread(ServerWorld world,
                                                                              Registry<Structure> registry,
                                                                              PlacementIndex index,
                                                                              List<Candidate> candidates,
                                                                              boolean allowChunkLoads) {
        // Дедуп результатов по XZ
        final LongOpenHashSet seenXZ = new LongOpenHashSet();
        final ArrayList<Pair<BlockPos, String>> found = new ArrayList<>(Math.min(128, candidates.size()));

        // Негативный кэш: (ChunkPos + placement) -> «здесь ничего нет» для текущего сида
        final LongOpenHashSet neg = getOrCreateNegativeCache(world);

        final StructureAccessor accessor = world.getStructureAccessor();

        PipelineProfiler.recordValue("structure_locator.resolve_candidates_input", candidates.size());

        for (Candidate c : candidates) {
            PipelineProfiler.increment("structure_locator.candidate_examined");
            long negKey = packNegKey(c.pos, c.placement);
            if (neg.contains(negKey)) {
                PipelineProfiler.increment("structure_locator.negative_cache_hit");
                continue;
            }

            boolean needChunk = false;
            PipelineProfiler.recordValue("structure_locator.structures_per_candidate", c.structs.size());
            // Быстрый проход по presence
            for (RegistryEntry<Structure> s : c.structs) {
                StructurePresence presence;
                PipelineProfiler.increment("structure_locator.presence_checks");
                try (PipelineProfiler.Section presenceTimer = PipelineProfiler.openSection(
                        "structure_locator.presence_check")) {
                    presence = accessor.getStructurePresence(c.pos, s.value(), c.placement, true);
                }
                if (presence == StructurePresence.START_PRESENT) {
                    PipelineProfiler.increment("structure_locator.presence_positive");
                    BlockPos hit = c.placement.getLocatePos(c.pos);
                    long keyXZ = BlockPos.asLong(hit.getX(), 0, hit.getZ());
                    if (seenXZ.add(keyXZ)) {
                        Identifier id = registry.getId(s.value());
                        found.add(Pair.of(new BlockPos(hit.getX(), CacheManager.getHeight(world, hit.getX(), hit.getZ()), hit.getZ()),
                                id == null ? "unknown" : id.toString()));
                        PipelineProfiler.increment("structure_locator.found_structures");
                    }
                    // Не добавляем в негативный кэш — тут как раз что-то есть
                    needChunk = false; // на всякий случай
                    continue;
                }
                if (presence != StructurePresence.START_NOT_PRESENT) {
                    needChunk = true; // может быть, но нужно подтверждение
                    PipelineProfiler.increment("structure_locator.chunk_confirmation_needed");
                }
            }

            if (!needChunk) {
                neg.add(negKey);
                PipelineProfiler.increment("structure_locator.negative_cache_store");
                continue;
            }

            if (!allowChunkLoads) {
                DebugLog.info(LOGGER, "Skipping chunk load for candidate {} due to allowChunkLoads=false", c.pos);
                PipelineProfiler.increment("structure_locator.chunk_loads_skipped");
                // Не трогаем негативный кэш, чтобы кандидат мог быть проверен позже
                continue;
            }

            // Единоразовая загрузка чанка до STRUCTURE_STARTS для всей группы
            PipelineProfiler.increment("structure_locator.chunk_load_requests");
            net.minecraft.world.chunk.Chunk chunk;
            try (PipelineProfiler.Section chunkTimer = PipelineProfiler.openSection("structure_locator.chunk_load")) {
                chunk = world.getChunk(c.pos.x, c.pos.z,
                        net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS);
            }
            PipelineProfiler.increment("structure_locator.chunk_loads_completed");
            var secPos = ChunkSectionPos.from(chunk);

            boolean any = false;
            for (RegistryEntry<Structure> s : c.structs) {
                StructureStart start = accessor.getStructureStart(secPos, s.value(), chunk);
                if (start != null && start.hasChildren()) {
                    PipelineProfiler.increment("structure_locator.chunk_resolve_success");
                    BlockPos hit = c.placement.getLocatePos(start.getPos());
                    long keyXZ = BlockPos.asLong(hit.getX(), 0, hit.getZ());
                    if (seenXZ.add(keyXZ)) {
                        Identifier id = registry.getId(s.value());
                        found.add(Pair.of(new BlockPos(hit.getX(), CacheManager.getHeight(world, hit.getX(), hit.getZ()), hit.getZ()),
                                id == null ? "unknown" : id.toString()));
                        PipelineProfiler.increment("structure_locator.found_structures");
                    }
                    any = true;
                }
            }
            if (!any) {
                neg.add(negKey);
                PipelineProfiler.increment("structure_locator.negative_cache_store");
            }
        }

        return found;
    }

    /* ───────────────────────────── Negative cache ───────────────────────────── */

    private static final Map<MinecraftServer, NegCache> NEGATIVE_CACHE = new WeakHashMap<>();

    private static LongOpenHashSet getOrCreateNegativeCache(ServerWorld world) {
        MinecraftServer server = world.getServer();
        long seed = world.getSeed();
        NegCache nc = NEGATIVE_CACHE.get(server);
        if (nc == null || nc.seed != seed) {
            nc = new NegCache(seed);
            NEGATIVE_CACHE.put(server, nc);
        }
        return nc.set;
    }

    private static long packNegKey(ChunkPos pos, StructurePlacement placement) {
        long a = ChunkPos.toLong(pos.x, pos.z);
        int b = System.identityHashCode(placement);
        return a ^ (((long) b) << 1);
    }

    private static final class NegCache {
        final long seed;
        final LongOpenHashSet set = new LongOpenHashSet();
        NegCache(long seed) { this.seed = seed; }
    }

    /* ───────────────────────────── Persistence ───────────────────────────── */

    private static void schedulePersistence(ServerWorld world, List<Pair<BlockPos, String>> found) {
        if (found.isEmpty()) return;
        MinecraftServer server = world.getServer();
        server.execute(() -> {
            RoadGraphState graph = RoadGraphState.get(world);
            for (Pair<BlockPos, String> pair : found) {
                Node node = graph.addNodeWithEdges(pair.getFirst(), pair.getSecond());
                DebugLog.info(LOGGER, "Added node {} at {} ({})", node.id(), node.pos(), pair.getSecond());
            }
            graph.markDirty();
        });
    }

    /* ───────────────────────────── Helpers ───────────────────────────── */

    private static Optional<? extends RegistryEntryList<Structure>> getStructureList(RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> registry) {
        return predicate.getKey().map(
                key -> registry.getEntry(key.getValue()).map(RegistryEntryList::of),
                tag -> {
                    List<RegistryEntry<Structure>> entries = StreamSupport.stream(registry.iterateEntries(tag).spliterator(), false).toList();
                    return Optional.of(RegistryEntryList.of(entries));
                }
        );
    }

    /**
     * Вычисляет оптимальный шаг сетки (в чанках) с учётом минимального spacing среди RandomSpread-плейсментов.
     */
    private static int computeGridStepChunks(PlacementIndex index, int fallbackStep) {
        int minSpacing = Integer.MAX_VALUE;
        for (RandomSpreadStructurePlacement rsp : index.randomGroups.keySet()) {
            minSpacing = Math.min(minSpacing, rsp.getSpacing());
        }
        if (minSpacing != Integer.MAX_VALUE) {
            int step = Math.max(fallbackStep, minSpacing);
            DebugLog.info(LOGGER, "Grid step optimization: fallbackStep={}, minSpacing={}, chosenStep={}", fallbackStep, minSpacing, step);
            return step;
        }
        return fallbackStep;
    }
}
