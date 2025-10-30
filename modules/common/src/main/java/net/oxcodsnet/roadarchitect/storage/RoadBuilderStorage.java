package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Хранит очереди сегментов дорог для каждого чанка.
 * <p>Stores pending road-building segments per chunk.</p>
 */
public class RoadBuilderStorage extends SavedData {
    private static final String KEY = "road_builder_tasks";
    private static final String SEGMENTS_KEY = "segments";
    private static final String CHUNK_KEY = "chunk";
    private static final String PATH_KEY = "path";
    private static final String START_KEY = "start";
    private static final String END_KEY = "end";

    public static final SavedDataType<RoadBuilderStorage> TYPE = new SavedDataType<>(
            KEY,
            ctx -> new RoadBuilderStorage(),
            ctx -> CompoundTag.CODEC.xmap(
                    tag -> fromNbt(tag, ctx.level().registryAccess()),
                    storage -> storage.writeNbt(new CompoundTag(), ctx.level().registryAccess())
            ),
            DataFixTypes.SAVED_DATA_SCOREBOARD
    );
    private final Map<ChunkPos, List<SegmentEntry>> segments = new ConcurrentHashMap<>();

    /**
     * Получает хранилище задач для указанного мира.
     * <p>Retrieves the storage of building tasks for the given world.</p>
     */
    public static RoadBuilderStorage get(ServerLevel world) {
        return PersistentStateUtil.get(world, TYPE);
    }

    /**
     * Загружает хранилище из NBT.
     * <p>Loads the storage from NBT.</p>
     */
    public static RoadBuilderStorage fromNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        RoadBuilderStorage storage = new RoadBuilderStorage();
        ListTag list = tag.getListOrEmpty(SEGMENTS_KEY);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            ChunkPos chunk = new ChunkPos(entry.getLongOr(CHUNK_KEY, 0L));
            String path = entry.getStringOr(PATH_KEY, "");
            int start = entry.getIntOr(START_KEY, 0);
            int end = entry.getIntOr(END_KEY, 0);
            storage.segments.computeIfAbsent(chunk, c -> new CopyOnWriteArrayList<>())
                    .add(new SegmentEntry(path, start, end));
        }
        return storage;
    }

    /**
     * Сохраняет все сегменты в NBT.
     * <p>Serializes all segments into an NBT compound.</p>
     */
    public CompoundTag writeNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (Map.Entry<ChunkPos, List<SegmentEntry>> entry : segments.entrySet()) {
            long pos = entry.getKey().toLong();
            for (SegmentEntry segment : entry.getValue()) {
                CompoundTag elem = new CompoundTag();
                elem.putLong(CHUNK_KEY, pos);
                elem.putString(PATH_KEY, segment.pathKey());
                elem.putInt(START_KEY, segment.start());
                elem.putInt(END_KEY, segment.end());
                list.add(elem);
            }
        }
        tag.put(SEGMENTS_KEY, list);
        return tag;
    }

    /**
     * Добавляет новый сегмент для строительства в указанном чанке.
     * <p>Adds a new segment to be built within the given chunk.</p>
     *
     * @param chunk целевой чанк / target chunk
     * @param key   ключ пути / path key
     * @param start индекс начала включительно / inclusive start index
     * @param end   индекс конца исключительно / exclusive end index
     */
    public void addSegment(ChunkPos chunk, String key, int start, int end) {
        segments.computeIfAbsent(chunk, c -> new CopyOnWriteArrayList<>())
                .add(new SegmentEntry(key, start, end));
        setDirty();
    }

    /**
     * Возвращает список сегментов, ожидающих постройки в чанке.
     * <p>Returns the list of queued segments for the chunk.</p>
     *
     * @param chunk чанк / chunk position
     */
    public List<SegmentEntry> getSegments(ChunkPos chunk) {
        return segments.getOrDefault(chunk, List.of());
    }

    /**
     * Удаляет указанный сегмент из очереди чанка.
     * <p>Removes the given segment from the chunk queue.</p>
     *
     * @param chunk чанк / chunk position
     * @param entry сегмент / segment entry to remove
     */
    public void removeSegment(ChunkPos chunk, SegmentEntry entry) {
        List<SegmentEntry> list = segments.get(chunk);
        if (list != null && list.remove(entry)) {
            if (list.isEmpty()) {
                segments.remove(chunk);
            }
            setDirty();
        }
    }

    /**
     * Один сегмент пути, находящийся внутри чанка.
     * <p>A single path segment contained within a chunk.</p>
     */
    public record SegmentEntry(String pathKey, int start, int end) {
    }
}
