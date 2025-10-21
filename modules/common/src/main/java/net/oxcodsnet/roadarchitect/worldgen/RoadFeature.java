package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
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
    private static final BlockState PREPARATION_BLOCK = Blocks.BEDROCK.getDefaultState();
    private static final int PREPARATION_CLEARANCE_EXTRA = 2;
    private static final Map<Long, PreparationEntry> PREPARATION_BACKUP = new ConcurrentHashMap<>();

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

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }


    private static void buildRoadStripe(StructureWorldAccess world, List<BlockPos> pts, int halfWidth, Random random, GenerationPhase phase) {
        boolean finalizePhase = phase == GenerationPhase.FINALIZE;
        int clearanceHalfWidth = halfWidth + PREPARATION_CLEARANCE_EXTRA;
        Registry<Biome> biomeRegistry = finalizePhase ? world.getRegistryManager().get(RegistryKeys.BIOME) : null;
        for (int i = 0; i < pts.size(); i++) {
            BlockPos p = pts.get(i);
            int prevIdx = Math.max(0, i - 2);
            int nextIdx = Math.min(pts.size() - 1, i + 2);
            Vec3d dir = new Vec3d(
                    pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                    0.0D,
                    pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
            ).normalize();
            double nx = dir.x;
            double nz = dir.z;
            boolean diagonal = Math.abs(nx) > 0.001 && Math.abs(nz) > 0.001;

            for (int dx = -clearanceHalfWidth; dx <= clearanceHalfWidth; dx++) {
                for (int dz = -clearanceHalfWidth; dz <= clearanceHalfWidth; dz++) {
                    double dist = Math.abs(dx * nz - dz * nx);
                    int maxAbs = Math.max(Math.abs(dx), Math.abs(dz));
                    //boolean insideRoad = dist <= halfWidth + 0.01 || (diagonal && maxAbs <= halfWidth);
                    //boolean insideClearance = dist <= clearanceHalfWidth + 0.01 || (diagonal && maxAbs <= clearanceHalfWidth);

                    double pad = 0.70710678; // sqrt(0.5^2 + 0.5^2)
                    boolean insideRoad      = dist <= (halfWidth + pad);
                    boolean insideClearance = dist <= (clearanceHalfWidth + pad);

                    if (!insideClearance) continue;

                    BlockPos roadPos = p.add(dx, 0, dz);

                    long packedPos = roadPos.asLong();

                    if (!finalizePhase) {
                        if (!isNotWaterBlock(world, roadPos)) {
                            continue;
                        }
                        if (insideRoad) {
                            prepareCell(world, roadPos, PreparationRole.ROAD);
                            BlockPos topPos = roadPos.up();
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
                        RegistryEntry<Biome> biome = world.getBiome(roadPos);
                        RoadStyle style = RoadStyles.forBiome(biomeRegistry, biome);
                        BlockState roadState = style.palette().pick(random);
                        placeRoad(world, roadPos, roadState);
                        PREPARATION_BACKUP.remove(packedPos);
                        BlockPos topPos = roadPos.up();
                        long packedTop = topPos.asLong();
                        PreparationEntry topEntry = PREPARATION_BACKUP.remove(packedTop);
                        if (topEntry != null) {
                            restoreFromEntry(world, topPos, topEntry);
                        } else if (world.getBlockState(topPos).isOf(PREPARATION_BLOCK.getBlock())) {
                            world.removeBlock(topPos, false);
                        }
                    } else {
                        PreparationEntry entry = PREPARATION_BACKUP.remove(packedPos);
                        if (entry != null) {
                            restoreFromEntry(world, roadPos, entry);
                        } else if (world.getBlockState(roadPos).isOf(PREPARATION_BLOCK.getBlock())) {
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

    private static void decorateSide(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth, Decoration deco, Random random) {
        int side = random.nextBoolean() ? 1 : -1;
        int sx = (int) Math.round(-nz * side);
        int sz = (int) Math.round(nx * side);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);
        int length = 1 + random.nextInt(3);

        if (deco instanceof FenceDecoration fence) {
            List<BlockPos> stripe = new ArrayList<>();
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) {continue;}
                stripe.add(dpos);
            }
            fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) {continue;}
                deco.place(world, dpos, random);
            }
        }
    }

    private static void placeLampDet(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth,
                                     LampPostDecoration base, boolean leftFirst, Random random) {
        int sx = (int) Math.round(-nz);
        int sz = (int) Math.round(nx);

        BlockPos leftPos  = center.add( sx * (halfWidth + 1), 0,  sz * (halfWidth + 1));
        BlockPos rightPos = center.add(-sx * (halfWidth + 1), 0, -sz * (halfWidth + 1));
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

    private static void placeSideDet(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth,
                                     Decoration deco, boolean leftSide, int length, Random detRandom) {
        int sideMul = leftSide ? 1 : -1;
        int sx = (int) Math.round(-nz * sideMul);
        int sz = (int) Math.round(nx * sideMul);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);

        if (deco instanceof FenceDecoration fence) {
            List<BlockPos> stripe = new ArrayList<>();
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) { continue; }
                stripe.add(dpos);
            }
            if (!stripe.isEmpty()) fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
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

    private static void prepareCell(StructureWorldAccess world, BlockPos pos, PreparationRole role) {
        long key = pos.asLong();
        PREPARATION_BACKUP.compute(key, (k, existing) -> {
            BlockState currentState = world.getBlockState(pos);
            BlockState original = existing != null ? existing.originalState() : currentState;
            PreparationRole mergedRole = existing != null ? PreparationRole.merge(existing.role(), role) : role;
            if (!currentState.isOf(PREPARATION_BLOCK.getBlock())) {
                world.setBlockState(pos, PREPARATION_BLOCK, Block.NOTIFY_NEIGHBORS);
            }
            return new PreparationEntry(original, mergedRole);
        });
    }

    private static void restoreFromEntry(StructureWorldAccess world, BlockPos pos, PreparationEntry entry) {
        if (entry == null) {
            return;
        }
        BlockState original = entry.originalState();
        if (original == null || original.isAir()) {
            world.removeBlock(pos, false);
        } else {
            world.setBlockState(pos, original, Block.NOTIFY_NEIGHBORS);
        }
    }

    private static void placeRoad(StructureWorldAccess world, BlockPos pos, BlockState stateRoad) {
        if (!isNotWaterBlock(world, pos)) {return;}
        world.setBlockState(pos, stateRoad, Block.NOTIFY_NEIGHBORS);
        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_NEIGHBORS);
    }


    private static boolean isNotWaterBlock(StructureWorldAccess world, BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return true;
        return !world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
    }


    /**
     * Собираем точки суши из [from, to), но «съедаем» по 1 точке
     * с начала и конца каждого сухого прогона (эрозия на 1).
     */
    private static List<BlockPos> collectLandPoints(
            StructureWorldAccess world, List<BlockPos> pts, int from, int to
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
    public boolean generate(FeatureContext<RoadFeatureConfig> ctx) {
        ServerWorld serverWorld = ctx.getWorld().toServerWorld();
        if (serverWorld == null) {
            return false;
        }

        StructureWorldAccess world = ctx.getWorld();
        ChunkPos chunk = new ChunkPos(ctx.getOrigin());

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
        Random random = world.getRandom();
        GenerationPhase phase = ctx.getConfig().phase();
        boolean finalizePhase = phase == GenerationPhase.FINALIZE;
        Registry<Biome> biomeRegistry = world.getRegistryManager().get(RegistryKeys.BIOME);
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
                    net.minecraft.util.math.Vec3d dir = new net.minecraft.util.math.Vec3d(
                            pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                            0.0D,
                            pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
                    ).normalize();
                    double nx = dir.x;
                    double nz = dir.z;

                    // Seed left/right deterministically by (pathKey, ordinal)
                    boolean leftFirst = PathDecorUtil.detBool(pathKey, m.k());
                    RegistryEntry<Biome> biomeAtP = world.getBiome(p);
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
                    Vec3d dir = new Vec3d(
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

                    placeSideDet(world, p, nx, nz, halfWidth, chosen, leftSide, length, net.minecraft.util.math.random.Random.create(m.k() ^ pathKey.hashCode()));
                }
            }
        }
        return placedAny;
    }
}
