package net.oxcodsnet.roadarchitect.util.model;

import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Simple, immutable Axis-Aligned Bounding Box on the XZ plane.
 */
public record AABB(int x1, int z1, int x2, int z2) {
    public static AABB of(List<BlockPos> pts) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : pts) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }
        return new AABB(minX, minZ, maxX, maxZ);
    }

    public AABB inflate(int r) {
        return new AABB(x1 - r, z1 - r, x2 + r, z2 + r);
    }

    public boolean intersects(AABB other) {
        return this.x1 <= other.x2 && this.x2 >= other.x1 && this.z1 <= other.z2 && this.z2 >= other.z1;
    }
}
