package net.oxcodsnet.roadarchitect.worldgen.style.decoration;


import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Буй: еловое бревно → забор → факел.
 * Ставится прямо в клетку дороги-воды (лог замещает воду),
 * без “ноги” до дна — как просили.
 */
public final class BuoyDecoration implements Decoration {
    private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState();
    private static final BlockState FENCE = Blocks.SPRUCE_FENCE.defaultBlockState();
    private static final BlockState TORCH = Blocks.TORCH.defaultBlockState();

    @Override
    public void place(WorldGenLevel world, BlockPos basePos, RandomSource random) {
        // ── Если в клетке не воздух/вода — не рискуем ──
        if (!world.getBlockState(basePos).getFluidState().isSource()) return;

        // Ставим бревно на уровень дороги-воды, fence и факел — выше
        world.setBlock(basePos, LOG, Block.UPDATE_NEIGHBORS);
        world.setBlock(basePos.above(), FENCE, Block.UPDATE_NEIGHBORS);
        world.setBlock(basePos.above(2), TORCH, Block.UPDATE_NEIGHBORS);
    }
}

