package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Represents a decoration placed alongside a road.
 */
@FunctionalInterface
public interface Decoration {
    Decoration NONE = (world, pos, random) -> {
    };

    void place(WorldGenLevel world, BlockPos basePos, RandomSource random);
}

