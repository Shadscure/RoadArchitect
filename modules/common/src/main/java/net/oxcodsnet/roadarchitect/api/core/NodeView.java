package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.core.BlockPos;

/**
 * Immutable node snapshot.
 */
public record NodeView(String id, BlockPos pos, String type) {}

