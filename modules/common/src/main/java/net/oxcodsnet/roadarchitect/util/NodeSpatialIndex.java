package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;

import java.util.*;

/**
 * A simple spatial index for graph nodes using a grid hash.
 * This helps to quickly find nearby nodes without a linear scan.
 */
public final class NodeSpatialIndex {

    private static final int CELL_SIZE = 256; // Grid cell size in blocks, can be tuned

    private final Map<Long, List<Node>> grid = new HashMap<>();

    public void add(Node node) {
        long key = pack(node.pos().getX() / CELL_SIZE, node.pos().getZ() / CELL_SIZE);
        grid.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
    }

    public void remove(Node node) {
        long key = pack(node.pos().getX() / CELL_SIZE, node.pos().getZ() / CELL_SIZE);
        List<Node> nodesInCell = grid.get(key);
        if (nodesInCell != null) {
            nodesInCell.remove(node);
            if (nodesInCell.isEmpty()) {
                grid.remove(key);
            }
        }
    }

    public Set<Node> query(BlockPos center, double radius) {
        Set<Node> candidates = new HashSet<>();
        int rInCells = (int) Math.ceil(radius / CELL_SIZE);
        int centerX = center.getX() / CELL_SIZE;
        int centerZ = center.getZ() / CELL_SIZE;

        for (int x = centerX - rInCells; x <= centerX + rInCells; x++) {
            for (int z = centerZ - rInCells; z <= centerZ + rInCells; z++) {
                long key = pack(x, z);
                List<Node> nodesInCell = grid.get(key);
                if (nodesInCell != null) {
                    candidates.addAll(nodesInCell);
                }
            }
        }
        return candidates;
    }
    
    public void clear() {
        grid.clear();
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
