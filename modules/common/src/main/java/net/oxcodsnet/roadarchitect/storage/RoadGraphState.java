package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.util.GeometryUtils;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

/**
 * Сохраняет узлы и рёбра дорог как {@link SavedData}.
 * <p>Stores road nodes and edges as a {@link SavedData}.</p>
 */
public class RoadGraphState extends SavedData {
    private static final String KEY = "road_graph";
    private static final String NODES_KEY = "nodes";
    private static final String EDGES_KEY = "edges";
    private static final String RADIUS_KEY = "radius";

    private final NodeStorage nodeStorage;
    private final EdgeStorage edgeStorage;

    /**
     * Создает новое состояние графа дорог с указанным радиусом соединений.
     * <p>Creates a new road graph state with the given connection radius.</p>
     */
    public RoadGraphState(double radius) {
        this(new NodeStorage(), new EdgeStorage(radius));
    }

    /**
     * Внутренний конструктор с заданными хранилищами.
     * <p>Internal constructor using the provided storages.</p>
     */
    private RoadGraphState(NodeStorage nodes, EdgeStorage edges) {
        this.nodeStorage = nodes;
        this.edgeStorage = edges;
    }


    /**
     * Получает или создает состояние графа для мира.
     * <p>Gets or creates the road graph state for the given world.</p>
     */
    public static RoadGraphState get(ServerLevel world) {
        return PersistentStateUtil.get(world,
                () -> new RoadGraphState(RoadArchitect.CONFIG.maxConnectionDistance()),
                RoadGraphState::fromNbt,
                KEY);
    }

    /*========== helpers ==========*/

    /**
     * Восстанавливает состояние графа из NBT.
     * <p>Restores the road graph state from NBT.</p>
     */
    public static RoadGraphState fromNbt(CompoundTag tag) {
        double radius = tag.getDouble(RADIUS_KEY);
        NodeStorage nodes = NodeStorage.fromNbt(tag.getList(NODES_KEY, Tag.TAG_COMPOUND));
        EdgeStorage edges = EdgeStorage.fromNbt(tag.getCompound(EDGES_KEY), radius);
        return new RoadGraphState(nodes, edges);
    }

    /**
     * Возвращает хранилище узлов.
     * <p>Returns the node storage.</p>
     */
    public NodeStorage nodes() {
        return nodeStorage;
    }

    /**
     * Возвращает хранилище рёбер.
     * <p>Returns the edge storage.</p>
     */
    public EdgeStorage edges() {
        return edgeStorage;
    }

    /**
     * Добавляет новый узел и сразу строит с ним все допустимые рёбра
     *
     * @param pos позиция для нового узла
     * @return созданный узел
     */
    public Node addNodeWithEdges(BlockPos pos, String type) {
        Node newNode = this.nodeStorage.add(pos, type);
        for (Node other : this.nodeStorage.all().values()) {
            if (!other.id().equals(newNode.id())) {
                connect(newNode, other);
            }
        }
        this.setDirty();
        return newNode;
    }

    /**
     * Пытается соединить два узла, запрещая «крестовые» рёбра.
     */
    public void connect(Node nodeA, Node nodeB) {
        if (nodeA == null || nodeB == null) {
            return;
        }
        String idNodeA = nodeA.id();
        String idNodeB = nodeB.id();
        if (idNodeA.equals(idNodeB)) {
            return;
        }

        // 1) проверяем радиус
        double dx = nodeA.pos().getX() - nodeB.pos().getX();
        double dz = nodeA.pos().getZ() - nodeB.pos().getZ();
        double max = edgeStorage.radius() * 2.0;
        if (dx * dx + dz * dz > max * max) {
            return;
        }

        // 2) уже существует?
        if (edgeStorage.all().containsKey(KeyUtil.edgeKey(idNodeA, idNodeB))) {
            return;
        }

        // 3) пересекает ли новое ребро какие-нибудь существующие?
        for (EdgeStorage.Edge e : edgeStorage.all().values()) {
            if (e.connects(idNodeA) || e.connects(idNodeB)) continue;
            Node n1 = nodeStorage.all().get(e.nodeA());
            Node n2 = nodeStorage.all().get(e.nodeB());
            if (n1 == null || n2 == null) continue;

            if (GeometryUtils.segmentsIntersect2D(nodeA.pos(), nodeB.pos(), n1.pos(), n2.pos())) {
                return;
            }
        }

        // 4) всё чисто — делегируем фактическое создание
        boolean added = edgeStorage.add(nodeA, nodeB);
        if (added) this.setDirty();
    }

    /**
     * Сохраняет состояние в NBT.
     * <p>Writes this state into an NBT compound.</p>
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble(RADIUS_KEY, edgeStorage.radius());
        tag.put(NODES_KEY, nodeStorage.toNbt());
        tag.put(EDGES_KEY, edgeStorage.toNbt());
        return tag;
    }
}
