package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.util.AsyncExecutor;
import net.oxcodsnet.roadarchitect.util.CacheManager;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import net.oxcodsnet.roadarchitect.util.PathSpatialIndex;
import net.oxcodsnet.roadarchitect.util.model.AABB;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import net.oxcodsnet.roadarchitect.util.profiler.PipelineProfiler;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.OptionalInt;

/**
 * Post-processes raw A* paths into detailed block sequences
 * and applies parallel-road Y-merge post-optimization.
 * Дополнительно: нормализация высот + подробное логирование мест «иголок».
 */
public final class RoadPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadPostProcessor.class.getSimpleName());
    // ====== ПАРАМЕТРЫ (можно вынести в конфиг позже) ======
    private static final int TOLERANCE_BLOCKS = 45;          // близость, блоки
    private static final int ANGLE_THRESHOLD_DEG = 35;       // почти параллельные
    private static final int TAIL_ANGLE_MAX_DEG = 35;        // угол после схождения
    private static final double AVG_DIST_FACTOR = 1.5;       // < tolerance * factor
    private static final double SCORE_LIMIT = 50.0;          // угол + dist/10
    private static final int MAX_ITER = 5;                   // N-циклы (Для лучшего эффекта, должно быть нечётным)
    private static final boolean UNTIL_STABLE = true;        // до стабилизации
    private static final int TRIM_RADIUS_L1 = 50;        // L1-радиус обрезки (как в PathFinder)
    // ====== Нормализация высот (профиль) ======
    private static final int SMOOTH_MEDIAN_WINDOW = 5;       // нечётное: 3/5/7
    private static final int SMOOTH_GRAD_MAX = 1;            // макс. разница Y между соседями
    private static final int SMOOTH_PASSES = 2;              // число прогонов
    private static final int DESPIKE_DELTA = 2;              // чувствительность «иголки»
    private static final boolean LOG_GRAD_CLAMP = true;     // включить подробный лог клампа

    private static final Set<String> a = ConcurrentHashMap.newKeySet();

    private RoadPostProcessor() {
    }

    private static <T> T first(List<T> list) {
        return list.get(0);
    }

    private static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }

    // ====== Регистрация хуков ======
    public static void onStartWorldTick(ServerWorld world) {
        if (world.isClient()) return;
        if (!RoadPipelineController.isDimensionEnabled(world.getRegistryKey())) return;
        processPending(world);
    }


    // ====== Обрезка начала и конца пути по L1-радиусу (XZ) ======
    private static int manhattanXZ(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    /**
     * Обрезает последовательность вершин с начала и конца, удаляя точки,
     * находящиеся в пределах радиуса R (по Манхэттену в XZ) от ИСХОДНЫХ
     * крайних точек. Если после обрезки остаётся < 2 вершин — возвращаем
     * исходный путь без изменений.
     */
    private static List<BlockPos> trimByManhattan(List<BlockPos> path) {
        if (path == null || path.size() < 2) return path;

        BlockPos start0 = first(path);
        BlockPos end0 = last(path);

        int i = 0;
        while (i < path.size() && manhattanXZ(path.get(i), start0) <= TRIM_RADIUS_L1) i++;

        int j = path.size() - 1;
        while (j >= 0 && manhattanXZ(path.get(j), end0) <= TRIM_RADIUS_L1) j--;

        if (i <= 0 && j >= path.size() - 1) {
            // ничего не обрезали
            return path;
        }

        if (j - i + 1 < 2) {
            // по условию — продолжаем без обрезки
            return path;
        }

        return new ArrayList<>(path.subList(i, j + 1));
    }

    // ====== refine как было ======
    private static List<BlockPos> refine(ServerWorld world, List<BlockPos> verts) {
        if (verts.isEmpty()) return List.of();

        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = world.getChunkManager().getNoiseConfig();
        Map<Long, Integer> resolvedHeights = new HashMap<>();
        List<BlockPos> out = new ArrayList<>(verts.size() * PathFinder.GRID_STEP);
        for (int i = 0; i < verts.size() - 1; i++) {
            BlockPos a = verts.get(i);
            BlockPos b = verts.get(i + 1);
            out.add(adjustToGround(world, generator, noiseConfig, resolvedHeights, a));
            interpolate(world, generator, noiseConfig, a, b, resolvedHeights, out);
        }
        out.add(adjustToGround(world, generator, noiseConfig, resolvedHeights, last(verts)));
        return out;
    }

    private static void interpolate(ServerWorld world,
                                    ChunkGenerator generator,
                                    NoiseConfig noiseConfig,
                                    BlockPos a,
                                    BlockPos b,
                                    Map<Long, Integer> resolvedHeights,
                                    List<BlockPos> out) {
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int steps = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        for (int i = 1; i < steps; i++) {
            int nx = a.getX() + dx * i;
            int nz = a.getZ() + dz * i;
            int ny = resolveSurfaceHeight(world, generator, noiseConfig, resolvedHeights, nx, nz) - 1;
            out.add(new BlockPos(nx, ny, nz));
        }
    }

    private static BlockPos adjustToGround(ServerWorld world,
                                           ChunkGenerator generator,
                                           NoiseConfig noiseConfig,
                                           Map<Long, Integer> resolvedHeights,
                                           BlockPos pos) {
        int surface = resolveSurfaceHeight(world, generator, noiseConfig, resolvedHeights, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), surface - 1, pos.getZ());
    }

    private static int resolveSurfaceHeight(ServerWorld world,
                                            ChunkGenerator generator,
                                            NoiseConfig noiseConfig,
                                            Map<Long, Integer> resolvedHeights,
                                            int x,
                                            int z) {
        long key = CacheManager.hash(x, z);
        Integer cached = resolvedHeights.get(key);
        if (cached != null) {
            return cached;
        }

        OptionalInt prepared = RoadFeature.lookupPreparedSurface(x, z);
        int resolved = prepared.isPresent() ? prepared.getAsInt() : CacheManager.getHeight(world, x, z);
        int bottomGuard = world.getBottomY() + 1;
        if (resolved < bottomGuard) {
            resolved = bottomGuard;
        }

        if (generator != null && noiseConfig != null) {
            int generated = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
            if (generated > resolved) {
                resolved = generated;
            }
        }

        resolvedHeights.put(key, resolved);
        return resolved;
    }

    private static NormalizeResult normalizeHeights(List<BlockPos> refined) {
        if (refined.size() < 3) {
            return new NormalizeResult(refined, 0, 0);
        }

        int n = refined.size();
        int[] y = new int[n];
        for (int i = 0; i < n; i++) y[i] = refined.get(i).getY();

        int spikes = 0;
        int clamps = 0;

        for (int pass = 0; pass < SMOOTH_PASSES; pass++) {
            // 1) Медианный фильтр
            if (SMOOTH_MEDIAN_WINDOW >= 3 && (SMOOTH_MEDIAN_WINDOW & 1) == 1) {
                int r = SMOOTH_MEDIAN_WINDOW / 2;
                int[] tmp = Arrays.copyOf(y, n);
                int[] win = new int[SMOOTH_MEDIAN_WINDOW];
                for (int i = 0; i < n; i++) {
                    int s = Math.max(0, i - r);
                    int e = Math.min(n - 1, i + r);
                    int k = 0;
                    for (int j = s; j <= e; j++) win[k++] = y[j];
                    for (; k < win.length; k++) win[k] = (i < r) ? y[0] : y[n - 1];
                    Arrays.sort(win);
                    tmp[i] = win[win.length / 2];
                }
                y = tmp;
            }

            // 2) Ограничение градиента: вперёд
            for (int i = 1; i < n; i++) {
                int old = y[i];
                int lo = y[i - 1] - SMOOTH_GRAD_MAX;
                int hi = y[i - 1] + SMOOTH_GRAD_MAX;
                int clamped = Math.max(lo, Math.min(hi, old));
                if (clamped != old) {
                    clamps++;
                    if (LOG_GRAD_CLAMP) {
                        BlockPos p = refined.get(i);
                        DebugLog.info(LOGGER, "[PostProcess] Clamp↑ at {}: {} -> {} (ref={})", p, old, clamped, y[i - 1]);
                    }
                    y[i] = clamped;
                }
            }
            // 2b) Ограничение градиента: назад
            for (int i = n - 2; i >= 0; i--) {
                int old = y[i];
                int lo = y[i + 1] - SMOOTH_GRAD_MAX;
                int hi = y[i + 1] + SMOOTH_GRAD_MAX;
                int clamped = Math.max(lo, Math.min(hi, old));
                if (clamped != old) {
                    clamps++;
                    if (LOG_GRAD_CLAMP) {
                        BlockPos p = refined.get(i);
                        DebugLog.info(LOGGER, "[PostProcess] Clamp↓ at {}: {} -> {} (ref={})", p, old, clamped, y[i + 1]);
                    }
                    y[i] = clamped;
                }
            }

            // 3) Срез одиночных «иголок» (пик над обоими соседями)
            for (int i = 1; i < n - 1; i++) {
                int a = y[i - 1];
                int b = y[i + 1];
                int m = Math.max(a, b);
                if (y[i] - m >= DESPIKE_DELTA) {
                    BlockPos p = refined.get(i);
                    int old = y[i];
                    int neu = (a + b) / 2;
                    y[i] = neu;
                    spikes++;
                    LOGGER.warn("[PostProcess] Срезан пик высоты в {}: {} -> {} (соседи: {}/{})",
                            p, old, neu, a, b);
                }
            }
        }

        List<BlockPos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos p = refined.get(i);
            out.add(new BlockPos(p.getX(), y[i], p.getZ()));
        }
        return new NormalizeResult(out, spikes, clamps);
    }

    public static void processPending(ServerWorld world) {
        PathStorage storage = PathStorage.get(world);
        PipelineProfiler.increment("postprocess.invocations");

        // собрать pending
        List<String> pending = new ArrayList<>();
        int examined = 0;
        for (Map.Entry<String, PathStorage.Status> e : storage.allStatuses().entrySet()) {
            examined++;
            if (e.getValue() == PathStorage.Status.PENDING) pending.add(e.getKey());
        }
        if (pending.isEmpty()) {
            PipelineProfiler.recordValue("postprocess.entries_examined", examined);
            return;
        }

        // стратегия: маленький объём — инкрементально; большой — с пространственным индексом
        int toSchedule = Math.min(pending.size(), MAX_PER_TICK);
        Ctx ctx = pending.size() >= SPATIAL_THRESHOLD ? buildCtx(storage, pending) : null;

        int scheduled = 0;
        for (String key : pending) {
            if (storage.getStatus(key) != PathStorage.Status.PENDING) continue;
            schedule(world, storage, key, ctx);
            PipelineProfiler.increment("postprocess.entries_scheduled");
            if (++scheduled >= toSchedule) break; // распределить нагрузку по тикам
        }

        PipelineProfiler.recordValue("postprocess.entries_examined", examined);
        PipelineProfiler.recordValue("postprocess.entries_pending", pending.size());
        PipelineProfiler.recordValue("postprocess.entries_scheduled_this_tick", scheduled);
    }

    public static void processChunk(ServerWorld world, ChunkPos chunk) {
        PathStorage storage = PathStorage.get(world);
        List<String> keys = new ArrayList<>(storage.getPendingForChunk(chunk));
        if (keys.isEmpty()) return;

        int toSchedule = Math.min(keys.size(), MAX_PER_TICK);
        Ctx ctx = keys.size() >= SPATIAL_THRESHOLD ? buildCtx(storage, keys) : null;

        int scheduled = 0;
        for (String key : keys) {
            if (storage.getStatus(key) != PathStorage.Status.PENDING) continue;
            schedule(world, storage, key, ctx);
            if (++scheduled >= toSchedule) break;
        }
    }

    /**
     * ВАЖНО про статусы:
     * - baseKey: tryMarkProcessing(baseKey) -> PROCESSING; дальше либо updatePath(..., READY), либо FAILED.
     * - partnerKey: tryMarkProcessing(partner) -> PROCESSING; если отказались от слияния -> setStatus(partner, PENDING);
     * если слили -> updatePath(..., READY).
     * Никаких «скрытых» переводов статусов в finally.
     */
    private static void schedule(ServerWorld world, PathStorage storage, String baseKey) {
        if (!storage.tryMarkProcessing(baseKey)) return;
        final List<BlockPos> baseRawInitial = pendingPaths.get(baseKey);
        if (baseRawInitial == null) {
            storage.setStatus(baseKey, PathStorage.Status.FAILED);
            DebugLog.info("Path key {} was pending but has no data, failing.", baseKey);
            return;
        }
        PipelineProfiler.increment("postprocess.jobs_scheduled");

        AsyncExecutor.execute(() -> {
            PipelineProfiler.increment("postprocess.jobs_started");
            try (PipelineProfiler.Section section = PipelineProfiler.openSection("postprocess.job")) {
            String activeKey = baseKey;
            List<BlockPos> activeRaw = new ArrayList<>(baseRawInitial);
            // Применяем обрезку по Манхэттену к активному пути до любой обработки
            activeRaw = trimByManhattan(activeRaw);
            activeRaw = simplifyPath(activeRaw);

            final Set<String> partnersMarked = new HashSet<>();
            final Set<String> becameReady = new HashSet<>();
            final Map<String, List<BlockPos>> toBuild = new HashMap<>();

            try {
                int iter = 0;
                boolean changed;

                do {
                    changed = false;
                    // Повторно применяем обрезку в начале итерации (после возможных обновлений activeRaw)
                    //activeRaw = trimByManhattan(activeRaw, TRIM_RADIUS_L1);
                    iter++;

                    // 1) Ищем лучшего параллельного соседа среди PENDING
                    MergeCandidate cand = findBestParallelPartner(storage, activeKey, activeRaw, index, pendingPaths);
                    if (cand == null) break;

                    // 2) Пытаемся пометить соседа как PROCESSING
                    if (!storage.tryMarkProcessing(cand.otherKey)) {
                        continue;
                    }
                    partnersMarked.add(cand.otherKey);

                    // 3) Проверяем схождение
                    List<BlockPos> otherRaw = pendingPaths.get(cand.otherKey);

                    // Обрезаем путь партнёра до всех манипуляций
                    otherRaw = trimByManhattan(otherRaw);

                    Convergence conv = findConvergence(activeRaw, otherRaw);
                    if (conv == null) {
                        storage.setStatus(cand.otherKey, PathStorage.Status.PENDING);
                        partnersMarked.remove(cand.otherKey);
                        continue;
                    }
                    // 4) Строим Y
                    BuildResult br = buildY(world, activeKey, activeRaw, cand.otherKey, otherRaw, conv);

                    // 4.1) Ноги → refine → normalize → READY
                    for (Entry<String, List<BlockPos>> leg : br.legsRaw.entrySet()) {
                        List<BlockPos> refined = refine(world, leg.getValue());
                        NormalizeResult nr = normalizeHeights(refined);
                        storage.updatePath(leg.getKey(), nr.path(), PathStorage.Status.READY);
                        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onPathReady(world, leg.getKey(), nr.path());
                        toBuild.put(leg.getKey(), nr.path());

                        if (!nr.path().isEmpty()) {
                            BlockPos s = first(nr.path());
                            BlockPos t = last(nr.path());
                            DebugLog.info(
                                    LOGGER,
                                    "[PostProcess] READY (leg) key={} points={}, spikesCut={}, gradClamped={}, start={}, end={}",
                                    leg.getKey(), nr.path().size(), nr.spikesCut(), nr.gradClamped(), s, t
                            );
                        }
                        becameReady.add(leg.getKey());
                    }

                    // 4.2) Ствол (persist) → refine → normalize → READY
                    List<BlockPos> trunkRefined = refine(world, br.trunkRaw.path);
                    NormalizeResult trunkNR = normalizeHeights(trunkRefined);
                    storage.updatePath(br.trunkRaw.key, trunkNR.path(), PathStorage.Status.READY);
                    net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onPathReady(world, br.trunkRaw.key, trunkNR.path());
                    toBuild.put(br.trunkRaw.key, trunkNR.path());

                    if (!trunkNR.path().isEmpty()) {
                        BlockPos s = first(trunkNR.path());
                        BlockPos t = last(trunkNR.path());
                        DebugLog.info(
                                LOGGER,
                                "[PostProcess] READY (trunk) key={} points={}, spikesCut={}, gradClamped={}, start={}, end={}",
                                br.trunkRaw.key, trunkNR.path().size(), trunkNR.spikesCut(), trunkNR.gradClamped(), s, t
                        );
                    }
                    becameReady.add(br.trunkRaw.key);

                    // 4.3) Следующая итерация — уже по стволу
                    activeKey = br.trunkRaw.key;
                    activeRaw = br.trunkRaw.path;

                    changed = true;
                } while (iter < MAX_ITER && (!UNTIL_STABLE || changed || iter == 1));

                // Если ничего не объединили — просто дорисовываем исходный путь
                if (toBuild.isEmpty()) {
                    List<BlockPos> refined = refine(world, activeRaw);
                    NormalizeResult nr = normalizeHeights(refined);
                    storage.updatePath(activeKey, nr.path(), PathStorage.Status.READY);
                    net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onPathReady(world, activeKey, nr.path());
                    toBuild.put(activeKey, nr.path());

                    if (!nr.path().isEmpty()) {
                        BlockPos s = first(nr.path());
                        BlockPos t = last(nr.path());
                        DebugLog.info(
                                LOGGER,
                                "[PostProcess] READY (single) key={} points={}, spikesCut={}, gradClamped={}, start={}, end={}",
                                activeKey, nr.path().size(), nr.spikesCut(), nr.gradClamped(), s, t
                        );
                    }
                    becameReady.add(activeKey);
                }

                // Разом ставим задачи строителю
                RoadBuilderManager.queueSegments(world, toBuild);
                PipelineProfiler.recordValue("postprocess.paths_completed", becameReady.size());

            } catch (Exception ex) {
                LOGGER.error("Post-processing failed for {}", baseKey, ex);
                storage.setStatus(baseKey, PathStorage.Status.FAILED);
                for (String p : partnersMarked) {
                    if (!becameReady.contains(p)) {
                        storage.setStatus(p, PathStorage.Status.PENDING);
                    }
                }
                PipelineProfiler.increment("postprocess.jobs_failed");
                return;
            }

            // Гарантируем возврат статусов партнёров
            for (String p : partnersMarked) {
                if (!becameReady.contains(p) && storage.getStatus(p) == PathStorage.Status.PROCESSING) {
                    storage.setStatus(p, PathStorage.Status.PENDING);
                }
            }
        }});
    }

    private static MergeCandidate findBestParallelPartner(PathStorage storage, String baseKey, List<BlockPos> baseRaw, PathSpatialIndex index, Map<String, List<BlockPos>> pendingPaths) {
        AABB bbBase = AABB.of(baseRaw);
        Set<String> candidates = index.query(bbBase.inflate(TOLERANCE_BLOCKS * 2));

        double bestScore = Double.POSITIVE_INFINITY;
        String bestKey = null;

        for (String otherKey : candidates) {
            if (otherKey.equals(baseKey) || storage.getStatus(otherKey) != PathStorage.Status.PENDING) {
                continue;
            }

            List<BlockPos> otherRaw = pendingPaths.get(otherKey);
            if (otherRaw == null || otherRaw.size() < 2) continue;

            // Bbox check is implicitly handled by the spatial query, but a precise one is still good.
            AABB bbOther = AABB.of(otherRaw);
            if (!bbBase.inflate(TOLERANCE_BLOCKS * 2).intersects(bbOther)) continue;

            // почти параллельны?
            double angle = angleDeg(dir(baseRaw), dir(otherRaw));
            if (angle >= ANGLE_THRESHOLD_DEG) continue;

            double minDist = minPointToPointDist(baseRaw, otherRaw);
            if (minDist >= TOLERANCE_BLOCKS) continue;

            double score = angle + minDist * 0.5;
            if (score < bestScore) {
                bestScore = score;
                bestKey = otherKey;
            }
        }

        return bestKey == null ? null : new MergeCandidate(bestKey, bestScore);
    }

    // ====== Поиск точки схождения ======
    private static Convergence findConvergence(List<BlockPos> a, List<BlockPos> b) {
        int n = a.size(), m = b.size();
        if (n < 3 || m < 3) return null;

        double bestScore = Double.POSITIVE_INFINITY;
        int bestI = -1, bestJ = -1;

        for (int i = 1; i < n - 1; i++) {
            BlockPos ai = a.get(i);
            int jBest = -1;
            double dMin = Double.POSITIVE_INFINITY;
            for (int j = 1; j < m - 1; j++) {
                double d = hypot2D(ai, b.get(j));
                if (d < dMin) {
                    dMin = d;
                    jBest = j;
                }
            }
            if (jBest <= 0 || dMin >= TOLERANCE_BLOCKS * 2) continue;

            List<BlockPos> tailA = a.subList(i, n);
            List<BlockPos> tailB = b.subList(jBest, m);
            if (tailA.size() < 2 || tailB.size() < 2) continue;

            double angTail = angleDeg(dir(tailA), dir(tailB));
            double avgDist = minPointToPointDist(tailA, tailB);
            double score = angTail + (avgDist / 10.0);

            if (angTail < TAIL_ANGLE_MAX_DEG && avgDist < (AVG_DIST_FACTOR * TOLERANCE_BLOCKS) && score < bestScore) {
                bestScore = score;
                bestI = i;
                bestJ = jBest;
            }
        }

        if (bestScore >= SCORE_LIMIT || bestI < 0) return null;
        return new Convergence(bestI, bestJ);
    }

    // ====== Построение Y и persist-ствола ======
    private static BuildResult buildY(ServerWorld world,
                                      String keyA, List<BlockPos> a,
                                      String keyB, List<BlockPos> b,
                                      Convergence conv) {
        BlockPos pa = a.get(conv.i);
        BlockPos pb = b.get(conv.j);

        int jx = (int) Math.round((pa.getX() + pb.getX()) / 2.0);
        int jz = (int) Math.round((pa.getZ() + pb.getZ()) / 2.0);
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = world.getChunkManager().getNoiseConfig();
        Map<Long, Integer> resolvedHeights = new HashMap<>();
        BlockPos J = adjustToGround(world, generator, noiseConfig, resolvedHeights, new BlockPos(jx, pa.getY(), jz));

        List<BlockPos> legA = new ArrayList<>(a.subList(0, conv.i + 1));
        legA.set(legA.size() - 1, J);

        List<BlockPos> legB = new ArrayList<>(b.subList(0, conv.j + 1));
        legB.set(legB.size() - 1, J);

        List<BlockPos> tailA = a.subList(conv.i, a.size());
        List<BlockPos> tailB = b.subList(conv.j, b.size());

        List<BlockPos> chosenTail;
        String chosenKey;
        if (tailA.size() > tailB.size() || (tailA.size() == tailB.size() && conv.i <= conv.j)) {
            chosenTail = tailA;
            chosenKey = keyA;
        } else {
            chosenTail = tailB;
            chosenKey = keyB;
        }

        List<BlockPos> trunk = new ArrayList<>(chosenTail.size());
        trunk.add(J);
        trunk.addAll(chosenTail.subList(1, chosenTail.size()));
        String trunkKey = chosenKey + "#J@" + jx + "," + jz; // persist

        Map<String, List<BlockPos>> legs = new HashMap<>();
        legs.put(keyA, legA);
        legs.put(keyB, legB);

        return new BuildResult(legs, new Trunk(trunkKey, trunk));
    }

    private static List<BlockPos> simplifyPath(List<BlockPos> path) {
        if (path.size() < 3) {
            return path;
        }
        List<BlockPos> simplified = new ArrayList<>();
        simplified.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            BlockPos p0 = path.get(i - 1);
            BlockPos p1 = path.get(i);
            BlockPos p2 = path.get(i + 1);


            long dx1 = (long)p1.getX() - p0.getX();
            long dz1 = (long)p1.getZ() - p0.getZ();
            long dx2 = (long)p2.getX() - p1.getX();
            long dz2 = (long)p2.getZ() - p1.getZ();


            if (dx1 * dz2 - dz1 * dx2 != 0) {
                simplified.add(p1);
            }
        }

        simplified.add(path.get(path.size() - 1));
        return simplified;
    }

    private static double angleDeg(int[] v1, int[] v2) {
        double n1 = Math.hypot(v1[0], v1[1]);
        double n2 = Math.hypot(v2[0], v2[1]);
        if (n1 == 0 || n2 == 0) return 180;
        double dot = (v1[0] * v2[0] + v1[1] * v2[1]) / (n1 * n2);
        dot = Math.max(-1, Math.min(1, dot));
        double ang = Math.toDegrees(Math.acos(dot));
        return Math.min(ang, 180 - ang);
    }

    private static int[] dir(List<BlockPos> pts) {
        BlockPos s = first(pts), e = last(pts);
        return new int[]{e.getX() - s.getX(), e.getZ() - s.getZ()};
    }

    private static double minPointToPointDist(List<BlockPos> a, List<BlockPos> b) {
        double min = Double.POSITIVE_INFINITY;
        for (BlockPos pa : a)
            for (BlockPos pb : b) {
                double d = hypot2D(pa, pb);
                if (d < min) min = d;
            }
        return min;
    }

    private static double hypot2D(BlockPos p1, BlockPos p2) {
        int dx = p1.getX() - p2.getX();
        int dz = p1.getZ() - p2.getZ();
        return Math.hypot(dx, dz);
    }

    // ====== Нормализация высот с логированием ======
    private record NormalizeResult(List<BlockPos> path, int spikesCut, int gradClamped) {
    }

    // ====== Вспомогательные структуры и математика ======
    private record MergeCandidate(String otherKey, double score) {
    }

    private record Convergence(int i, int j) {
    }

    private record Trunk(String key, List<BlockPos> path) {
    }

    private static final class BuildResult {
        final Map<String, List<BlockPos>> legsRaw;
        final Trunk trunkRaw;

        BuildResult(Map<String, List<BlockPos>> legsRaw, Trunk trunkRaw) {
            this.legsRaw = legsRaw;
            this.trunkRaw = trunkRaw;
        }
    }
}
