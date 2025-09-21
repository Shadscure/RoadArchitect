package net.oxcodsnet.roadarchitect.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;

import java.util.function.IntBinaryOperator;

/**
 * Terrain analysis helpers used by the pathfinder to better avoid
 * uneven, mountainous foothill areas without relying solely on biome tags.
 *
 * The core idea is to treat “mountainous terrain” as areas with large
 * local height variance (max-min within a small radius). This makes
 * mountains "feel" wider to the pathfinder and helps steer roads around
 * rough bases rather than cutting across them.
 */
public final class TerrainAnalyzer {

    private TerrainAnalyzer() {
    }

    /**
     * Computes stability/roughness cost at (x, z) using world heights.
     * Returns Double.MAX_VALUE to mark the position as invalid when the
     * immediate neighborhood is too steep.
     */
    public static double stabilityCost(ServerWorld world, int x, int z, int y) {
        // Quick local steepness guard (cardinal neighbors). Blocks clearly unstable spots.
        int local = 0;
        for (Direction d : Direction.Type.HORIZONTAL) {
            int ny = CacheManager.getHeight(world, x + d.getOffsetX(), z + d.getOffsetZ());
            local += Math.abs(y - ny);
            if (local > 3) {
                return Double.MAX_VALUE;
            }
        }

        // Base cost from local unevenness (kept compatible with previous behavior).
        double baseCost = local * 16.0;

        // Read config and optionally skip roughness penalty entirely.
        RAConfig cfg = RAConfigHolder.get();
        if (!cfg.terrainAnalyzerEnabled()) {
            return baseCost;
        }

        int radius = Math.max(1, cfg.terrainRoughRadius());
        int stride = Math.max(1, cfg.terrainRoughStride());
        int threshold = Math.max(0, cfg.terrainRangeThreshold());
        double scale = Math.max(0.0, cfg.terrainPenaltyScale());

        // Add broader roughness penalty so mountainous regions appear wider.
        IntBinaryOperator H = (ix, iz) -> CacheManager.getHeight(world, ix, iz);
        int range = heightRange(H, x, z, radius, stride);
        double roughPenalty = roughnessPenalty(range, threshold, scale);

        return baseCost + roughPenalty;
    }

    /**
     * Computes max-min height range inside a square window centered at (x,z).
     * This method is pure and suitable for unit-testing with synthetic height functions.
     */
    public static int heightRange(IntBinaryOperator heightFn, int x, int z, int radius, int stride) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx += stride) {
            for (int dz = -radius; dz <= radius; dz += stride) {
                int h = heightFn.applyAsInt(x + dx, z + dz);
                if (h < min) min = h;
                if (h > max) max = h;
            }
        }
        return max - min;
    }

    /**
     * Converts a height range to a smooth penalty. Ranges below threshold pay no penalty.
     * Above threshold, the penalty grows linearly; the scale is tuned to be
     * significant but not overpowering compared to other costs.
     */
    public static double roughnessPenalty(int heightRange) {
        RAConfig cfg = RAConfigHolder.get();
        return roughnessPenalty(heightRange, Math.max(0, cfg.terrainRangeThreshold()), Math.max(0.0, cfg.terrainPenaltyScale()));
    }

    public static double roughnessPenalty(int heightRange, int threshold, double scale) {
        if (heightRange <= threshold) {
            return 0.0;
        }
        int excess = heightRange - threshold;
        return excess * scale;
    }
}
