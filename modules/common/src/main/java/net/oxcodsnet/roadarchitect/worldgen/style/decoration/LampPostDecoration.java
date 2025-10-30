package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;

public final class LampPostDecoration implements Decoration {
    private static final int PLACE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_INVISIBLE;
    private static final int MAX_SUPPORT_DEPTH = 3;
    private static final int MAX_UP_SEARCH = 3;
    private static final int HEIGHT = 3;

    private final BlockState baseState; // нижний блок-основание (стена)
    private final BlockState postState; // вертикальные стойки/крюк (обычно забор)
    private final BlockState lampState;
    private final Direction facing; // куда вылетает «крюк»

    /**
     * Новый вариант: основание (чаще — стена) и стойки/крюк (чаще — деревянный забор) заданы отдельно.
     */
    public LampPostDecoration(BlockState baseState, BlockState postState, BlockState lampState) {
        this(baseState, postState, lampState, Direction.NORTH);
    }

    private LampPostDecoration(BlockState baseState, BlockState postState, BlockState lampState, Direction facing) {
        this.baseState = baseState;
        this.postState = postState;
        this.lampState = lampState;
        this.facing = facing;
    }

    public LampPostDecoration facing(Direction facing) {
        return new LampPostDecoration(this.baseState, this.postState, this.lampState, facing);
    }

    @Override
    public void place(WorldGenLevel world, BlockPos basePos, RandomSource random) {
        tryPlace(world, basePos, random);
    }

    /** Ставит фонарь «Г» и возвращает true при успехе. */
    public boolean tryPlace(WorldGenLevel world, BlockPos basePos, RandomSource random) {
        BlockPos top = computeTop(world, basePos);
        if (top == null) return false;

        // Проверяем стойку (включая уровень armBase)
        for (int i = 1; i <= HEIGHT; i++) {
            if (!world.getBlockState(top.above(i)).canBeReplaced()) return false;
        }

        // Вылет с уровня верхней стойки
        BlockPos armBase = top.above(HEIGHT);
        BlockPos hook    = armBase.relative(this.facing);
        if (!world.getBlockState(hook).canBeReplaced()) return false;

        BlockPos lantern = hook.below();
        if (!world.getBlockState(lantern).canBeReplaced()) return false;

        // Установка
        placeSupport(world, top);

        // 1..HEIGHT-1 — голая стойка
        for (int i = 1; i < HEIGHT; i++) {
            world.setBlock(top.above(i), this.postState, PLACE_FLAGS);
        }

        // ВЕРХ СТОЙКИ: задаём соединение в сторону «крюка»
        BlockState armBaseState = withFenceConnection(this.postState, this.facing);
        world.setBlock(armBase, armBaseState, PLACE_FLAGS);

        // КРЮК: задаём соединение назад, к стойке
        BlockState hookState = withFenceConnection(this.postState, this.facing.getOpposite());
        world.setBlock(hook, hookState, PLACE_FLAGS);

        // Подвесной фонарь
        BlockState ls = this.lampState;
        if (ls.hasProperty(LanternBlock.HANGING)) {
            ls = ls.setValue(LanternBlock.HANGING, true);
        }
        world.setBlock(lantern, ls, PLACE_FLAGS);

        return true;
    }

    /** Для заборов включает BooleanProperty направления (N/E/S/W).
     *  Для стен проставляет соответствующий EnumProperty<WallShape> (LOW/TALL).
     *  Для иных блоков возвращает исходное состояние. */
    private static BlockState withFenceConnection(BlockState state, Direction dir) {
        // 1) Заборы/решётки/панели — булевы свойства направлений
        BooleanProperty fenceProp = switch (dir) {
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case EAST  -> BlockStateProperties.EAST;
            case WEST  -> BlockStateProperties.WEST;
            default    -> null;
        };
        if (fenceProp != null && state.hasProperty(fenceProp)) {
            return state.setValue(fenceProp, true);
        }

        // 2) Стены — EnumProperty<WallShape> по направлениям
        EnumProperty<WallSide> wallProp = switch (dir) {
            case NORTH -> BlockStateProperties.NORTH_WALL;
            case SOUTH -> BlockStateProperties.SOUTH_WALL;
            case EAST  -> BlockStateProperties.EAST_WALL;
            case WEST  -> BlockStateProperties.WEST_WALL;
            default    -> null;
        };
        if (wallProp != null && state.hasProperty(wallProp)) {
            // Можно выбрать LOW или TALL. Обычно LOW выглядит «горизонтальной» полкой,
            // TALL даёт высокий упор. Поставим LOW как более универсальный вариант.
            return state.setValue(wallProp, WallSide.LOW);
        }

        // 3) Иные блоки — без изменений
        return state;
    }

    private void placeSupport(WorldGenLevel world, BlockPos top) {
        // верх основания — стенка (или иной «тяжёлый» блок)
        world.setBlock(top, this.baseState, PLACE_FLAGS);
        BlockPos cur = top.below();
        int depth = 0;
        while (cur.getY() >= world.getMinY()
                && depth < MAX_SUPPORT_DEPTH
                && !world.getBlockState(cur).isRedstoneConductor(world, cur)) {
            world.setBlock(cur, this.baseState, PLACE_FLAGS);
            cur = cur.below();
            depth++;
        }
    }

    private BlockPos computeTop(WorldGenLevel world, BlockPos base) {
        int bottomY = world.getMinY();
        boolean baseSolid = world.getBlockState(base).isRedstoneConductor(world, base);

        if (!baseSolid) {
            BlockPos probe = base.below();
            int depth = 0;
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isRedstoneConductor(world, probe)) {
                probe = probe.below();
                depth++;
            }
            if (!world.getBlockState(probe).isRedstoneConductor(world, probe)) return null;
            return probe.above();
        } else {
            BlockPos top = base.above();
            int rise = 0;
            while (rise < MAX_UP_SEARCH && !world.getBlockState(top).canBeReplaced()) {
                top = top.above(); rise++;
            }
            if (!world.getBlockState(top).canBeReplaced()) return null;

            int depth = 0;
            BlockPos probe = top.below();
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isRedstoneConductor(world, probe)) {
                probe = probe.below(); depth++;
            }
            if (!world.getBlockState(probe).isRedstoneConductor(world, probe)) return null;
            return top;
        }
    }
}
