package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.phys.Vec3;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.storage.PathDecorStorage;
import net.oxcodsnet.roadarchitect.util.PathDecorUtil;
import net.oxcodsnet.roadarchitect.worldgen.style.RoadStyle;
import net.oxcodsnet.roadarchitect.worldgen.style.RoadStyles;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.BuoyDecoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.Decoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.FenceDecoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.LampPostConfigResolver;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.LampPostDecoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureConfig.GenerationPhase;

/**
 * Feature that places road segments stored in {@link RoadBuilderStorage}.
 * <p>
 * Толстая линия реализована через проверку расстояния от клетки до центральной
 * прямой (|dx · dir.z − dz · dir.x| ≤ halfWidth). Такой подход избегает
 * «шахматных» дыр на диагоналях.
 */
public final class RoadFeature extends Feature<RoadFeatureConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadFeature.class.getSimpleName());

    private static final BuoyDecoration BUOY = new BuoyDecoration();
    private static final BlockState PREPARATION_BLOCK = Blocks.BEDROCK.defaultBlockState();
    private static final int PREPARATION_CLEARANCE_EXTRA = 2;
    private static final Map<Long, PreparationEntry> PREPARATION_BACKUP = new ConcurrentHashMap<>();
    private static final Map<Long, ColumnSnapshot> PREPARATION_COLUMNS = new ConcurrentHashMap<>();
    private static final double PIXEL_PADDING = Math.sqrt(0.5D);

    private enum PreparationRole {
        CLEARANCE(0),
        ROAD(1),
        CAP(2);

        private final int priority;

        PreparationRole(int priority) {
            this.priority = priority;
        }

        static PreparationRole merge(PreparationRole a, PreparationRole b) {
            return a.priority >= b.priority ? a : b;
        }
    }

    private record PreparationEntry(BlockState originalState, PreparationRole role) { }

    private static final class ColumnSnapshot {
        int height;
        int count;

        ColumnSnapshot(int height, int count) {
            this.height = height;
            this.count = count;
        }
    }

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }


    private static void buildRoadStripe(WorldGenLevel world, List<BlockPos> pts, int halfWidth, RandomSource random, GenerationPhase phase) {
        boolean finalizePhase = phase == GenerationPhase.FINALIZE;
        int clearanceHalfWidth = halfWidth + PREPARATION_CLEARANCE_EXTRA;
        Registry<Biome> biomeRegistry = finalizePhase ? world.registryAccess().lookupOrThrow(Registries.BIOME) : null;
        for (int i = 0; i < pts.size(); i++) {
            BlockPos p = pts.get(i);
            int prevIdx = Math.max(0, i - 2);
            int nextIdx = Math.min(pts.size() - 1, i + 2);
            Vec3 prevPoint = Vec3.atCenterOf(pts.get(prevIdx));
            Vec3 nextPoint = Vec3.atCenterOf(pts.get(nextIdx));
            double segDx = nextPoint.x - prevPoint.x;
            double segDz = nextPoint.z - prevPoint.z;
            double segmentLengthSq = segDx * segDx + segDz * segDz;
            if (segmentLengthSq < 1.0E-6) {
                segmentLengthSq = 1.0D;
                segDx = 1.0D;
                segDz = 0.0D;
            }
            double invLen = 1.0D / Math.sqrt(segmentLengthSq);
            Vec3 dir = new Vec3(segDx * invLen, 0.0D, segDz * invLen);
            double nx = dir.x;
            double nz = dir.z;

            for (int dx = -clearanceHalfWidth; dx <= clearanceHalfWidth; dx++) {
                for (int dz = -clearanceHalfWidth; dz <= clearanceHalfWidth; dz++) {
                    BlockPos roadPos = p.offset(dx, 0, dz);
                    Vec3 cellCenter = Vec3.atCenterOf(roadPos);
                    double dist = horizontalDistanceToSegment(cellCenter, prevPoint, segDx, segDz, segmentLengthSq);
                    boolean insideRoad = dist <= (halfWidth + PIXEL_PADDING);
                    boolean insideClearance = dist <= (clearanceHalfWidth + PIXEL_PADDING);

                    if (!insideClearance) continue;

                    long packedPos = roadPos.asLong();

                    if (!finalizePhase) {
                        if (!isNotWaterBlock(world, roadPos)) {
                            continue;
                        }
                        if (insideRoad) {
                            prepareCell(world, roadPos, PreparationRole.ROAD);
                            BlockPos topPos = roadPos.above();
                            prepareCell(world, topPos, PreparationRole.CAP);
                        } else {
                            prepareCell(world, roadPos, PreparationRole.CLEARANCE);
                        }
                        continue;
                    }

                    if (insideRoad) {
                        if (!isNotWaterBlock(world, roadPos)) {
                            continue;
                        }
                        Holder<Biome> biome = world.getBiome(roadPos);
                        RoadStyle style = RoadStyles.forBiome(biomeRegistry, biome);
                        BlockState roadState = style.palette().pick(random);
                        placeRoad(world, roadPos, roadState);
                        PreparationEntry groundEntry = PREPARATION_BACKUP.remove(packedPos);
                        if (groundEntry != null) {
                            releaseColumn(roadPos);
                        }
                        BlockPos topPos = roadPos.above();
                        long packedTop = topPos.asLong();
                        PreparationEntry topEntry = PREPARATION_BACKUP.remove(packedTop);
                        if (topEntry != null) {
                            restoreFromEntry(world, topPos, topEntry);
                        } else if (world.getBlockState(topPos).is(PREPARATION_BLOCK.getBlock())) {
                            world.removeBlock(topPos, false);
                        }
                    } else {
                        PreparationEntry entry = PREPARATION_BACKUP.remove(packedPos);
                        if (entry != null) {
                            restoreFromEntry(world, roadPos, entry);
                        } else if (world.getBlockState(roadPos).is(PREPARATION_BLOCK.getBlock())) {
                            world.removeBlock(roadPos, false);
                        }
                    }
                }
            }

            if (!finalizePhase) {
                continue;
            }

            RoadStyle style = RoadStyles.forBiome(biomeRegistry, world.getBiome(p));
            for (Decoration deco : style.decorations()) {
                if (deco instanceof LampPostDecoration) {
                    // handled via deterministic markers below
                } else if (!RoadArchitect.CONFIG.deterministicDecorations() && random.nextInt(18) == 0) {
                    if (!isNotWaterBlock(world, p)) {continue;}
                    decorateSide(world, p, nx, nz, halfWidth, deco, random);
                }
            }
        }
    }

    /* ======================  ВСПОМОГАТЕЛЬНОЕ  ==================== */

    private static void decorateSide(WorldGenLevel world, BlockPos center, double nx, double nz, int halfWidth, Decoration deco, RandomSource random) {
        int side = random.nextBoolean() ? 1 : -1;
        int sx = (int) Math.round(-nz * side);
        int sz = (int) Math.round(nx * side);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);
        int length = 1 + random.nextInt(3);

        if (deco instanceof FenceDecoration fence) {
            List<BlockPos> stripe = new ArrayList<>();
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.offset(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) {continue;}
                stripe.add(dpos);
            }
            fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.offset(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) {continue;}
                deco.place(world, dpos, random);
            }
        }
    }

    private static void placeLampDet(WorldGenLevel world, BlockPos center, double nx, double nz, int halfWidth,
                                     LampPostDecoration base, boolean leftFirst, RandomSource random) {
        int sx = (int) Math.round(-nz);
        int sz = (int) Math.round(nx);

        BlockPos leftPos  = center.offset( sx * (halfWidth + 1), 0,  sz * (halfWidth + 1));
        BlockPos rightPos = center.offset(-sx * (halfWidth + 1), 0, -sz * (halfWidth + 1));
        Direction leftFace  = directionFrom(-sx, -sz);
        Direction rightFace = directionFrom( sx,  sz);

        BlockPos firstPos     = leftFirst ? leftPos   : rightPos;
        Direction firstFacing = leftFirst ? leftFace  : rightFace;
        BlockPos secondPos     = leftFirst ? rightPos  : leftPos;
        Direction secondFacing = leftFirst ? rightFace : leftFace;

        if (isNotWaterBlock(world, firstPos)) {
            if (base.facing(firstFacing).tryPlace(world, firstPos, random)) {
                return;
            }
        }
        if (isNotWaterBlock(world, secondPos)) {
            base.facing(secondFacing).tryPlace(world, secondPos, random);
        }
    }

    private static void placeSideDet(WorldGenLevel world, BlockPos center, double nx, double nz, int halfWidth,
                                     Decoration deco, boolean leftSide, int length, RandomSource detRandom) {
        int sideMul = leftSide ? 1 : -1;
        int sx = (int) Math.round(-nz * sideMul);
        int sz = (int) Math.round(nx * sideMul);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);

        if (deco instanceof FenceDecoration fence) {
            List<BlockPos> stripe = new ArrayList<>();
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.offset(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) { continue; }
                stripe.add(dpos);
            }
            if (!stripe.isEmpty()) fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.offset(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) { continue; }
                deco.place(world, dpos, detRandom);
            }
        }
    }

    /** Определяем горизонтальное направление по (dx, dz). */
    static Direction directionFrom(int dx, int dz) {
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    private static void prepareCell(WorldGenLevel world, BlockPos pos, PreparationRole role) {
        long key = pos.asLong();
        BlockState currentState = world.getBlockState(pos);
        PREPARATION_BACKUP.compute(key, (k, existing) -> {
            boolean first = existing == null;
            BlockState original = first ? currentState : existing.originalState();
            PreparationRole mergedRole = first ? role : PreparationRole.merge(existing.role(), role);
            if (first) {
                retainColumn(world, pos, mergedRole, original);
            }
            if (!currentState.is(PREPARATION_BLOCK.getBlock())) {
                world.setBlock(pos, PREPARATION_BLOCK, Block.UPDATE_NEIGHBORS);
            }
            return new PreparationEntry(original, mergedRole);
        });
    }

    private static void restoreFromEntry(WorldGenLevel world, BlockPos pos, PreparationEntry entry) {
        if (entry == null) {
            return;
        }
        BlockState original = entry.originalState();
        if (original == null || original.isAir()) {
            world.removeBlock(pos, false);
        } else {
            world.setBlock(pos, original, Block.UPDATE_NEIGHBORS);
        }
        releaseColumn(pos);
    }

    private static void retainColumn(WorldGenLevel world, BlockPos pos, PreparationRole role, BlockState originalState) {
        long key = columnKey(pos.getX(), pos.getZ());
        PREPARATION_COLUMNS.compute(key, (k, snapshot) -> {
            int measuredHeight = measureSurfaceHeight(world, pos, originalState);
            if (snapshot == null) {
                snapshot = new ColumnSnapshot(measuredHeight, 0);
            }
            if (role != PreparationRole.CAP) {
                snapshot.height = Math.max(snapshot.height, measuredHeight);
            }
            snapshot.count += 1;
            return snapshot;
        });
    }

    private static void releaseColumn(BlockPos pos) {
        long key = columnKey(pos.getX(), pos.getZ());
        PREPARATION_COLUMNS.computeIfPresent(key, (k, snapshot) -> {
            snapshot.count -= 1;
            if (snapshot.count <= 0) {
                return null;
            }
            return snapshot;
        });
    }

    private static int measureSurfaceHeight(WorldGenLevel world, BlockPos pos, BlockState originalState) {
        if (originalState != null && !originalState.isAir()) {
            return pos.getY();
        }
        BlockPos.MutableBlockPos mutable = pos.mutable();
        int bottom = world.getMinY();
        while (mutable.getY() >= bottom) {
            BlockState state = world.getBlockState(mutable);
            if (!state.isAir()) {
                return mutable.getY();
            }
            mutable.move(Direction.DOWN);
        }
        return pos.getY();
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    public static OptionalInt lookupPreparedSurface(int x, int z) {
        ColumnSnapshot snapshot = PREPARATION_COLUMNS.get(columnKey(x, z));
        if (snapshot == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(snapshot.height);
    }

    private static double horizontalDistanceToSegment(Vec3 point, Vec3 start, double segDx, double segDz, double segmentLengthSq) {
        double px = point.x;
        double pz = point.z;
        double ax = start.x;
        double az = start.z;
        if (segmentLengthSq <= 1.0E-6) {
            double dx = px - ax;
            double dz = pz - az;
            return Math.sqrt(dx * dx + dz * dz);
        }
        double t = Mth.clamp(((px - ax) * segDx + (pz - az) * segDz) / segmentLengthSq, 0.0D, 1.0D);
        double closestX = ax + segDx * t;
        double closestZ = az + segDz * t;
        double dx = px - closestX;
        double dz = pz - closestZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void placeRoad(WorldGenLevel world, BlockPos pos, BlockState stateRoad) {
        if (!isNotWaterBlock(world, pos)) {return;}
        world.setBlock(pos, stateRoad, Block.UPDATE_NEIGHBORS);
        //world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_NEIGHBORS);
    }


    private static boolean isNotWaterBlock(WorldGenLevel world, BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        if (!world.hasChunk(cx, cz)) return true;
        return !world.getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }


    /**
     * Собираем точки суши из [from, to), но «съедаем» по 1 точке
     * с начала и конца каждого сухого прогона (эрозия на 1).
     */
    private static List<BlockPos> collectLandPoints(
            WorldGenLevel world, List<BlockPos> pts, int from, int to
    ) {
        int n = pts.size();
        // Расширяем окно на 1 с двух сторон, чтобы увидеть соседей за границей слайса
        int extFrom = Math.max(0, from - 1);
        int extTo   = Math.min(n, to + 1);

        // Маска суши на расширенном окне
        boolean[] landMask = new boolean[extTo - extFrom];
        for (int i = extFrom; i < extTo; i++) {
            landMask[i - extFrom] = isNotWaterBlock(world, pts.get(i));
        }

        // Собираем только «внутренние» сухие точки: у них и слева, и справа тоже суша
        List<BlockPos> out = new ArrayList<>(Math.max(0, to - from));
        for (int i = from; i < to; i++) {
            int k = i - extFrom;                // индекс внутри landMask
            if (!landMask[k]) continue;         // сама точка — вода
            boolean leftLand  = (k - 1 >= 0) && landMask[k - 1];
            boolean rightLand = (k + 1 < landMask.length) && landMask[k + 1];
            if (!leftLand || !rightLand) continue; // край прогона — пропускаем
            out.add(pts.get(i));
        }
        return out;
    }

    @Override
    public boolean place(FeaturePlaceContext<RoadFeatureConfig> ctx) {
        ServerLevel serverWorld = ctx.level().getLevel();
        if (serverWorld == null) {
            return false;
        }

        WorldGenLevel world = ctx.level();
        ChunkPos chunk = new ChunkPos(ctx.origin());

        RoadBuilderStorage builder = RoadBuilderStorage.get(serverWorld);
        PathStorage paths = PathStorage.get(serverWorld);
        PathDecorStorage decor = PathDecorStorage.get(serverWorld);
        List<RoadBuilderStorage.SegmentEntry> queue = new ArrayList<>(builder.getSegments(chunk));
        if (queue.isEmpty()) return false;

        int orthWidth = Math.max(1, RoadArchitect.CONFIG.roadWidth());
        if ((orthWidth & 1) == 0) {
            orthWidth -= 1;
        }
        int halfWidth = Math.max(0, orthWidth / 2);
        RandomSource random = world.getRandom();
        GenerationPhase phase = ctx.config().phase();
        boolean finalizePhase = phase == GenerationPhase.FINALIZE;
        Registry<Biome> biomeRegistry = world.registryAccess().lookupOrThrow(Registries.BIOME);
        boolean placedAny = false;

        for (RoadBuilderStorage.SegmentEntry entry : queue) {
            String[] ids = entry.pathKey().split("\\|", 2);
            if (ids.length != 2) {
                LOGGER.warn("Malformed path key '{}'; skipping", entry.pathKey());
                builder.removeSegment(chunk, entry);
                continue;
            }

            List<BlockPos> pts = paths.getPath(ids[0], ids[1]);
            if (pts.isEmpty()) {
                builder.removeSegment(chunk, entry);
                continue;
            }

            int from = Math.max(0, entry.start());
            int to = Math.min(pts.size(), entry.end());

            // Ensure prefix S and masks cache are up-to-date
            String pathKey = entry.pathKey();
            double[] S = PathDecorUtil.ensurePrefix(decor, pathKey, pts);

            int erosion = Math.max(0, RoadArchitect.CONFIG.maskErosion());
            int buoyInterval = Math.max(0, RoadArchitect.CONFIG.buoyInterval());
            int lampInterval = Math.max(0, RoadArchitect.CONFIG.lampInterval());
            int sideInterval = Math.max(0, RoadArchitect.CONFIG.sideDecorationInterval());
            boolean det = RoadArchitect.CONFIG.deterministicDecorations();

            // Masks updated for the current window only (avoids chunk loads)
            PathDecorUtil.fillGroundMask(decor, pathKey, world, pts, from, to);
            PathDecorUtil.fillWaterInteriorMask(decor, pathKey, world, pts, from, to);

            /* ---------- ФАЗА 1: вода / буйки (детерминированно) ---------- */
            if (finalizePhase && det && buoyInterval > 0) {
                int markerPhase = PathDecorUtil.phaseFor(pathKey, buoyInterval);
                List<PathDecorUtil.Marker> marks = PathDecorUtil.markersInWindow(S, from, to, buoyInterval, markerPhase);
                byte[] waterMask = decor.getWaterInteriorMask(pathKey);
                for (PathDecorUtil.Marker m : marks) {
                    int idx = m.index();
                    if (PathDecorUtil.erodedAccept(waterMask, idx, erosion, PathDecorUtil.BOOL_TRUE)) {
                        BUOY.place(world, pts.get(idx), random);
                    }
                }
            }

            /* ---------- ФАЗА 2: суша (дорога + фонари детерминированно) ---------- */
            List<BlockPos> landPts = collectLandPoints(world, pts, from, to);
            buildRoadStripe(world, landPts, halfWidth, random, phase);

            placedAny = true;

            if (!finalizePhase) {
                continue;
            }

            if (det && lampInterval > 0) {
                int markerPhase = PathDecorUtil.phaseFor(pathKey, lampInterval);
                List<PathDecorUtil.Marker> marks = PathDecorUtil.markersInWindow(S, from, to, lampInterval, markerPhase);
                byte[] landMask = decor.getGroundMask(pathKey);

                for (PathDecorUtil.Marker m : marks) {
                    int i = m.index();
                    BlockPos p = pts.get(i);
                    if (!PathDecorUtil.erodedAccept(landMask, i, erosion, PathDecorUtil.BOOL_TRUE)) continue;

                    int prevIdx = Math.max(0, i - 2);
                    int nextIdx = Math.min(pts.size() - 1, i + 2);
                    net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(
                            pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                            0.0D,
                            pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
                    ).normalize();
                    double nx = dir.x;
                    double nz = dir.z;

                    // Seed left/right deterministically by (pathKey, ordinal)
                    boolean leftFirst = PathDecorUtil.detBool(pathKey, m.k());
                    Holder<Biome> biomeAtP = world.getBiome(p);
                    LampPostDecoration resolved = LampPostConfigResolver.resolve(world, biomeAtP, null, pathKey, m.k());
                    if (resolved != null) {
                        placeLampDet(world, p, nx, nz, halfWidth, resolved, leftFirst, random);
                    }
                }
            }

            /* ---------- ФАЗА 3: суша / боковые украшения (детерминированно) ---------- */
            if (det && sideInterval > 0) {
                int markerPhase = PathDecorUtil.phaseFor(pathKey, sideInterval);
                List<PathDecorUtil.Marker> marks = PathDecorUtil.markersInWindow(S, from, to, sideInterval, markerPhase);
                byte[] landMask = decor.getGroundMask(pathKey);

                for (PathDecorUtil.Marker m : marks) {
                    int i = m.index();
                    if (!PathDecorUtil.erodedAccept(landMask, i, erosion, PathDecorUtil.BOOL_TRUE)) continue;

                    BlockPos p = pts.get(i);
                    // direction
                    int prevIdx = Math.max(0, i - 2);
                    int nextIdx = Math.min(pts.size() - 1, i + 2);
                    Vec3 dir = new Vec3(
                            pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                            0.0D,
                            pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
                    ).normalize();
                    double nx = dir.x;
                    double nz = dir.z;

                    // collect non-lamp decos for this biome
                    RoadStyle styleAtP = RoadStyles.forBiome(biomeRegistry, world.getBiome(p));
                    java.util.ArrayList<Decoration> sideDecos = new java.util.ArrayList<>();
                    for (Decoration d : styleAtP.decorations()) if (!(d instanceof LampPostDecoration)) sideDecos.add(d);
                    if (sideDecos.isEmpty()) continue;

                    int choice = PathDecorUtil.detInt(pathKey, m.k(), sideDecos.size());
                    Decoration chosen = sideDecos.get(choice);
                    boolean leftSide = PathDecorUtil.detBool(pathKey, m.k());
                    int length = 1 + PathDecorUtil.detInt(pathKey, m.k() ^ 0x55AA55AAL, 3);

                    placeSideDet(world, p, nx, nz, halfWidth, chosen, leftSide, length, net.minecraft.util.RandomSource.create(m.k() ^ pathKey.hashCode()));
                }
            }
        }
        return placedAny;
    }
}
