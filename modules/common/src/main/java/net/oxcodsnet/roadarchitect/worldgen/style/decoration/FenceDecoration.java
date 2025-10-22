package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FenceDecoration implements Decoration {
    private static final int MAX_SUPPORT_DEPTH = 3;   // максимум «воздуха» под столбом
    private static final int MAX_UP_SEARCH     = 3;   // максимум поиска свободного места вверх
    private static final int PLACE_FLAGS       = Block.UPDATE_ALL | Block.UPDATE_INVISIBLE;

    private final BlockState fenceState;

    public FenceDecoration(BlockState fenceState) {
        this.fenceState = fenceState;
    }

    @Override
    public void place(WorldGenLevel world, BlockPos basePos, RandomSource random) {
        BlockPos top = computeTop(world, basePos);
        if (top == null) return;
        placeFromTopDown(world, top);
    }

    /**
     * Считает «верх» столба по правилам:
     * - если стартовый блок НЕ твёрдый → ищем опору вниз ≤ 3 и ставим над ней;
     * - если стартовый блок твёрдый → поднимаем позицию вверх и ищем свободный слот ≤ 3.
     * Возвращает null, если условия не выполнены.
     */
    private BlockPos computeTop(WorldGenLevel world, BlockPos base) {
        int bottomY = world.getMinBuildHeight();

        BlockState baseState = world.getBlockState(base);
        boolean baseSolid = baseState.isRedstoneConductor(world, base); // твёрдый ли стартовый блок

        if (!baseSolid) {
            // Ищем опору вниз не дальше чем на 3 блока
            BlockPos probe = base.below();
            int depth = 0;
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isRedstoneConductor(world, probe)) {
                probe = probe.below();
                depth++;
            }
            if (!world.getBlockState(probe).isRedstoneConductor(world, probe)) {
                return null; // опоры нет в пределах 3
            }

            // Ставим верх столба над найденной опорой
            BlockPos top = probe.above();
            if (!world.getBlockState(top).canBeReplaced()) {
                return null; // место занято чем-то незаменяемым
            }
            return top;
        } else {
            // Стартовый блок твёрдый — ищем свободный слот вверх не дальше 3
            BlockPos top = base.above();
            int rise = 0;
            while (rise < MAX_UP_SEARCH && !world.getBlockState(top).canBeReplaced()) {
                top = top.above();
                rise++;
            }
            if (!world.getBlockState(top).canBeReplaced()) {
                return null; // свободного места в пределах 3 нет
            }

            // Доп. валидация: под top должна быть опора не дальше 3
            int depth = 0;
            BlockPos probe = top.below();
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isRedstoneConductor(world, probe)) {
                probe = probe.below();
                depth++;
            }
            if (!world.getBlockState(probe).isRedstoneConductor(world, probe)) {
                return null; // «висим» в воздухе глубже чем на 3
            }
            return top;
        }
    }

    /** Ставит забор в точке top и тянет вниз до опоры, но не глубже 3. */
    private void placeFromTopDown(WorldGenLevel world, BlockPos top) {
        world.setBlock(top, this.fenceState, PLACE_FLAGS);

        BlockPos cur = top.below();
        int depth = 0;
        while (cur.getY() >= world.getMinBuildHeight()
                && depth < MAX_SUPPORT_DEPTH
                && !world.getBlockState(cur).isRedstoneConductor(world, cur)) {
            world.setBlock(cur, this.fenceState, PLACE_FLAGS);
            cur = cur.below();
            depth++;
        }
    }

    /** Линейка столбов с той же вертикальной логикой. */
    public void placeFenceStripe(WorldGenLevel world, List<BlockPos> stripe) {
        for (BlockPos base : stripe) {
            BlockPos top = computeTop(world, base);
            if (top != null) {
                placeFromTopDown(world, top);
            }
        }

        // Доп. проход: перерасчёт форм (если хочешь руками подтолкнуть коннекты)
        for (BlockPos base : stripe) {
            for (int dy = 0; dy <= MAX_SUPPORT_DEPTH; dy++) {
                BlockPos p = base.above(dy);                // на случай, если верх ушёл выше base
                BlockState st = world.getBlockState(p);
                if (!st.is(fenceState.getBlock())) continue;

                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos n = p.relative(d);
                    st = st.updateShape(d, world.getBlockState(n), world, p, n);
                }
                world.setBlock(p, st, PLACE_FLAGS);
            }
        }
    }
}
