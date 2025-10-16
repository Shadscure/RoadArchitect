package net.oxcodsnet.roadarchitect.util;

import net.oxcodsnet.roadarchitect.util.model.AABB;

import java.util.*;

/**
 * A simple spatial index for paths using a grid hash.
 * This helps to quickly find nearby paths without a linear scan.
 */
public final class PathSpatialIndex {

    private static final int CELL_SIZE = 128; // Grid cell size in blocks, can be tuned

    private final Map<Long, List<String>> grid = new HashMap<>();

    /**
     * Adds a path's key to the spatial index based on its bounding box.
     *
     * @param pathKey The key of the path to add.
     * @param aabb    The Axis-Aligned Bounding Box of the path.
     */
    public void add(String pathKey, AABB aabb) {
        int minX = aabb.x1() / CELL_SIZE;
        int minZ = aabb.z1() / CELL_SIZE;
        int maxX = aabb.x2() / CELL_SIZE;
        int maxZ = aabb.z2() / CELL_SIZE;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = pack(x, z);
                grid.computeIfAbsent(key, k -> new ArrayList<>()).add(pathKey);
            }
        }
    }

    /**
     * Queries the index to find all path keys that might intersect the given AABB.
     *
     * @param aabb The query AABB.
     * @return A set of candidate path keys.
     */
    public Set<String> query(AABB aabb) {
        Set<String> candidates = new HashSet<>();
        int minX = aabb.x1() / CELL_SIZE;
        int minZ = aabb.z1() / CELL_SIZE;
        int maxX = aabb.x2() / CELL_SIZE;
        int maxZ = aabb.z2() / CELL_SIZE;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = pack(x, z);
                List<String> pathsInCell = grid.get(key);
                if (pathsInCell != null) {
                    candidates.addAll(pathsInCell);
                }
            }
        }
        return candidates;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
