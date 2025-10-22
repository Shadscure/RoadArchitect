package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит просчитанные пути между узлами как {@link SavedData}.
 * <p>Stores computed paths between nodes as a {@link SavedData}.</p>
 */
public class PathStorage extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + PathStorage.class.getSimpleName());
    private static final String KEY = "road_paths";
    private static final String PATHS_KEY = "paths";
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String POS_KEY = "pos";
    private static final String STATUS_KEY = "status";

    public static final Factory<PathStorage> TYPE = new Factory<>(PathStorage::new, PathStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);
    private final Map<String, List<BlockPos>> paths = new ConcurrentHashMap<>();
    private final Map<String, Status> statuses = new ConcurrentHashMap<>();

    /**
     * Возвращает экземпляр хранилища путей для указанного мира.
     * <p>Gets the {@code PathStorage} instance for the given world.</p>
     *
     * @param world серверный мир / server world
     * @return хранилище путей / path storage instance
     */
    public static PathStorage get(ServerLevel world) {
        return PersistentStateUtil.get(world, TYPE, KEY);
    }

    /**
     * Восстанавливает хранилище из NBT.
     * <p>Recreates the storage from an NBT compound.</p>
     */
    public static PathStorage fromNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        PathStorage storage = new PathStorage();
        ListTag list = tag.getList(PATHS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String from = entry.getString(FROM_KEY);
            String to = entry.getString(TO_KEY);
            String key = KeyUtil.pathKey(from, to);
            ListTag posList = entry.getList(POS_KEY, Tag.TAG_LONG);
            List<BlockPos> positions = new ArrayList<>();
            for (Tag nbtElement : posList) {
                positions.add(BlockPos.of(((LongTag) nbtElement).getAsLong()));
            }
            storage.paths.put(key, positions);
            Status status = net.oxcodsnet.roadarchitect.util.NbtUtils.getEnumOrDefault(entry, STATUS_KEY, Status.class, Status.READY);
            storage.statuses.put(key, status);
        }
        return storage;
    }

    /**
     * Сохраняет все пути в NBT.
     * <p>Serializes all paths into an NBT compound.</p>
     */
    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String[] ids = KeyUtil.parsePathKey(entry.getKey());
            if (ids.length != 2) continue;
            CompoundTag elem = new CompoundTag();
            elem.putString(FROM_KEY, ids[0]);
            elem.putString(TO_KEY, ids[1]);
            ListTag posList = new ListTag();
            for (BlockPos pos : entry.getValue()) {
                posList.add(LongTag.valueOf(pos.asLong()));
            }
            elem.put(POS_KEY, posList);
            Status st = statuses.getOrDefault(entry.getKey(), Status.PENDING);
            elem.putString(STATUS_KEY, st.name());
            list.add(elem);
        }
        tag.put(PATHS_KEY, list);
        return tag;
    }

    /**
     * Сохраняет путь между двумя узлами.
     * <p>Stores a path connecting two nodes.</p>
     */
    public void putPath(String from, String to, List<BlockPos> path, Status status) {
        String key = KeyUtil.pathKey(from, to);
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
        setDirty();
    }

    public void updatePath(String key, List<BlockPos> path, Status status) {
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
        setDirty();
    }

    /**
     * Возвращает сохраненный путь между узлами.
     * <p>Returns the stored path between the two nodes.</p>
     */
    public List<BlockPos> getPath(String from, String to) {
        return paths.getOrDefault(KeyUtil.pathKey(from, to), List.of());
    }

    public List<BlockPos> getPath(String key) {
        return paths.getOrDefault(key, List.of());
    }

    public Status getStatus(String key) {
        return statuses.getOrDefault(key, Status.PENDING);
    }

    public void setStatus(String key, Status status) {
        statuses.put(key, status);
        setDirty();
    }

    public Map<String, Status> allStatuses() {
        return Map.copyOf(statuses);
    }

    public boolean tryMarkProcessing(String key) {
        synchronized (statuses) {
            Status cur = statuses.get(key);
            if (cur != Status.PENDING) {
                return false;
            }
            statuses.put(key, Status.PROCESSING);
            setDirty();
            return true;
        }
    }

    public List<String> getPendingForChunk(ChunkPos chunk) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<BlockPos>> e : paths.entrySet()) {
            if (getStatus(e.getKey()) != Status.PENDING) continue;
            for (BlockPos p : e.getValue()) {
                if (new ChunkPos(p).equals(chunk)) {
                    out.add(e.getKey());
                    break;
                }
            }
        }
        return out;
    }

    public enum Status {
        PENDING,
        PROCESSING,
        READY,
        FAILED
    }
}
