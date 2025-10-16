package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.util.GeometryUtils;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import net.oxcodsnet.roadarchitect.util.NodeSpatialIndex;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сохраняет узлы и рёбра дорог как {@link PersistentState}.
 * <p>Stores road nodes and edges as a {@link PersistentState}.</p>
 */
public class RoadGraphState extends PersistentState {
    private static final String KEY = "road_graph";
    private static final String NODES_KEY = "nodes";
    private static final String EDGES_KEY = "edges";
    private static final String RADIUS_KEY = "radius";

    private final NodeStorage nodeStorage;
    private final EdgeStorage edgeStorage;
    private final transient NodeSpatialIndex nodeIndex;

    /**
     * Creates a new road graph state with the given connection radius.
     */
    public RoadGraphState(double radius) {
        this(new NodeStorage(), new EdgeStorage(radius));
    }

    /**
     * Internal constructor using the provided storages.
     */
    private RoadGraphState(NodeStorage nodes, EdgeStorage edges) {
        this.nodeStorage = nodes;
        this.edgeStorage = edges;
        this.nodeIndex = new NodeSpatialIndex();
        // Populate the index with existing nodes
        for (Node node : nodes.all().values()) {
            this.nodeIndex.add(node);
        }
    }


    /**
     * Gets or creates the road graph state for the given world.
     */
    public static RoadGraphState get(ServerWorld world) {
        return PersistentStateUtil.get(world,
                () -> new RoadGraphState(RoadArchitect.CONFIG.maxConnectionDistance()),
                RoadGraphState::fromNbt,
                KEY);
    }

    /*========== helpers ==========*/

    /**
     * Restores the road graph state from NBT.
     */
    public static RoadGraphState fromNbt(NbtCompound tag) {
        double radius = tag.getDouble(RADIUS_KEY);
        NodeStorage nodes = NodeStorage.fromNbt(tag.getList(NODES_KEY, NbtElement.COMPOUND_TYPE));
        EdgeStorage edges = EdgeStorage.fromNbt(tag.getCompound(EDGES_KEY), radius);
        return new RoadGraphState(nodes, edges);
    }

    /**
     * Returns the node storage.
     */
    public NodeStorage nodes() {
        return nodeStorage;
    }

    /**
     * Returns the edge storage.
     */
    public EdgeStorage edges() {
        return edgeStorage;
    }

    /**
     * Adds a new node and efficiently connects it to existing nodes within radius.
     *
     * @param pos position for the new node
     * @param type structure type of the new node
     * @return the created node
     */
    public Node addNode(BlockPos pos, String type) {
        Node newNode = this.nodeStorage.add(pos, type);
        this.nodeIndex.add(newNode);

        // Query the spatial index for potential neighbors instead of iterating all nodes
        for (Node other : this.nodeIndex.query(newNode.pos(), this.edgeStorage.radius())) {
            connect(newNode, other);
        }
        this.markDirty();
        return newNode;
    }

    /**
     * Removes a node and its associated edges from the graph.
     *
     * @param nodeId The ID of the node to remove.
     */
    public void removeNode(String nodeId) {
        Node nodeToRemove = nodeStorage.all().get(nodeId);
        if (nodeToRemove != null) {
            // Remove from spatial index
            nodeIndex.remove(nodeToRemove);

            // Remove from node storage
            nodeStorage.remove(nodeId);

            // Find and remove all connected edges
            List<String> edgesToRemove = edgeStorage.all().keySet().stream()
                    .filter(key -> key.contains(nodeId))
                    .collect(Collectors.toList());
            edgesToRemove.forEach(edgeStorage::remove);

            markDirty();
        }
    }

    /**
     * Attempts to connect two nodes, checking for radius and intersections.
     */
    public void connect(Node nodeA, Node nodeB) {
        if (nodeA == null || nodeB == null || nodeA.id().equals(nodeB.id())) {
            return;
        }

        // 1) Check radius
        if (nodeA.pos().getSquaredDistance(nodeB.pos()) > edgeStorage.radius() * edgeStorage.radius()) {
            return;
        }

        // 2) Check if edge already exists
        if (edgeStorage.all().containsKey(KeyUtil.edgeKey(nodeA.id(), nodeB.id()))) {
            return;
        }

        // 3) Check for intersections with existing edges
        for (EdgeStorage.Edge existingEdge : edgeStorage.all().values()) {
            // Skip edges connected to the current nodes
            if (existingEdge.connects(nodeA.id()) || existingEdge.connects(nodeB.id())) continue;

            Node n1 = nodeStorage.all().get(existingEdge.nodeA());
            Node n2 = nodeStorage.all().get(existingEdge.nodeB());
            if (n1 == null || n2 == null) continue;

            if (GeometryUtils.segmentsIntersect2D(nodeA.pos(), nodeB.pos(), n1.pos(), n2.pos())) {
                return; // Intersection found, do not add the new edge
            }
        }

        // 4) All checks passed, add the edge
        if (edgeStorage.add(nodeA, nodeB)) {
            this.markDirty();
        }
    }

    /**
     * Writes this state into an NBT compound.
     */
    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag.putDouble(RADIUS_KEY, edgeStorage.radius());
        tag.put(NODES_KEY, nodeStorage.toNbt());
        tag.put(EDGES_KEY, edgeStorage.toNbt());
        return tag;
    }
}
