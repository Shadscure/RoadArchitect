package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls execution of the road generation pipeline (platform-agnostic).
 * Fabric / NeoForge должны вызывать публичные onXxx(...) методы ниже,
 * сохраняя точные кейсы из исходного register().
 */
public final class RoadPipelineController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadPipelineController.class.getSimpleName());

    /**
     * Миры, для которых уже отработал INIT по событию генерации спавн-чанка.
     */
    private static final Set<RegistryKey<World>> INITIALIZED = ConcurrentHashMap.newKeySet();

    /**
     * Отложенные INIT по мирам: запуск через N тиков после генерации спавн‑чанка.
     */
    private static final class PendingInit {
        final BlockPos pos;
        int ticksLeft;
        PendingInit(BlockPos pos, int ticksLeft) { this.pos = pos; this.ticksLeft = ticksLeft; }
    }
    private static final Map<RegistryKey<World>, PendingInit> PENDING_INIT = new ConcurrentHashMap<>();

    /**
     * Кеш селекторов (ID и теги) из конфигурации, для быстрых проверок.
     */
    private static final Set<Identifier> TARGET_IDS = new HashSet<>();
    private static final Set<TagKey<Structure>> TARGET_TAGS = new HashSet<>();

    /**
     * Счётчик тиков для периодического триггера.
     */
    private static int tickCounter = 0;

    private RoadPipelineController() {
    }

    /**
     * Вызывать при старте сервера/мода (однократно), чтобы закешировать селекторы.
     */
    public static void init() {
        cacheStructureSelectors();
        tickCounter = 0;
        LOGGER.debug("RoadPipelineController initialized (selectors cached)");
    }

    /**
     * Вызывать при обновлении конфига — перекешируем селекторы.
     */
    public static void refreshStructureSelectorCache() {
        cacheStructureSelectors();
        LOGGER.debug("RoadPipelineController reloaded selectors from config");
    }

    /* ───────────────────────── Точные кейсы из исходного register() ───────────────────────── */

    /**
     * 1) Генерация спавн-чанка ВПЕРВЫЕ → INIT.
     */
    public static void onSpawnChunkGenerated(ServerWorld world, Chunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        ChunkPos spawnChunk = new ChunkPos(world.getSpawnPos());
        if (!chunk.getPos().equals(spawnChunk)) return;

        // Планируем отложенный INIT, чтобы не конкурировать с генерацией на самом старте мира.
        // Если уже есть план или мир инициализирован — ничего не делаем.
        if (INITIALIZED.contains(world.getRegistryKey())) return;
        if (PENDING_INIT.containsKey(world.getRegistryKey())) return;

        int delay = Math.max(20, RoadArchitect.CONFIG.pipelineIntervalSeconds() * 20); // минимум 1 секунда
        PENDING_INIT.put(world.getRegistryKey(), new PendingInit(world.getSpawnPos(), delay));
        LOGGER.debug("Scheduled deferred INIT for {} at {} ({} ticks)",
                world.getRegistryKey().getValue(), world.getSpawnPos(), delay);
    }

    /**
     * 2) Генерация ЛЮБОГО чанка; если внутри есть целевая структура → CHUNK.
     */
    public static void onChunkGenerated(ServerWorld world, Chunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) return;
        if (!containsTargetStructure(world, chunk)) return;

        BlockPos center = chunk.getPos().getCenterAtY(0);
        LOGGER.debug("Chunk {} generated with target structure, starting CHUNK pipeline", chunk.getPos());
        PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.CHUNK);
    }

    /**
     * 3) Игрок вошёл на сервер → PERIODIC (как в исходнике).
     */
    public static void onPlayerJoin(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return;

        // Если INIT ещё не выполнялся для этого мира (например, отложили из-за модов вроде Distant Horizons),
        // выполним его сейчас, когда мир стабильно загружен и игрок уже подключился.
        if (!INITIALIZED.contains(world.getRegistryKey())) {
            BlockPos spawn = world.getSpawnPos();
            LOGGER.debug("Player {} joined; INIT not done yet for {}. Running INIT at spawn {}",
                    player.getName().getString(), world.getRegistryKey().getValue(), spawn);
            // Снимаем возможный отложенный запуск, если был
            PENDING_INIT.remove(world.getRegistryKey());
            PipelineRunner.runPipeline(world, spawn, PipelineRunner.PipelineMode.INIT);
            INITIALIZED.add(world.getRegistryKey());
            return;
        }

        BlockPos pos = player.getBlockPos();
        LOGGER.debug("Player {} joined at {}, starting PERIODIC pipeline",
                player.getName().getString(), pos);
        PipelineRunner.runPipeline(world, pos, PipelineRunner.PipelineMode.PERIODIC);
    }

    /**
     * 4) Периодический триггер раз в N секунд (из конфига) – START_SERVER_TICK.
     */
    public static void onServerTick(MinecraftServer server) {
        int intervalTicks = Math.max(1, RoadArchitect.CONFIG.pipelineIntervalSeconds() * 20);
        tickCounter++;

        // Обработка отложенных INIT по мирам
        for (ServerWorld world : server.getWorlds()) {
            RegistryKey<World> key = world.getRegistryKey();
            PendingInit pending = PENDING_INIT.get(key);
            if (pending == null) continue;
            if (INITIALIZED.contains(key)) { PENDING_INIT.remove(key); continue; }
            if (--pending.ticksLeft <= 0) {
                LOGGER.debug("Running deferred INIT for {} at {}", key.getValue(), pending.pos);
                PipelineRunner.runPipeline(world, pending.pos, PipelineRunner.PipelineMode.INIT);
                INITIALIZED.add(key);
                PENDING_INIT.remove(key);
            }
        }

        if (tickCounter < intervalTicks) return;
        tickCounter = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            World w = player.getWorld();
            if (w.getRegistryKey() != World.OVERWORLD) continue;

            BlockPos pos = player.getBlockPos();
            LOGGER.debug("Periodic trigger at player {} pos {}, starting PERIODIC pipeline",
                    player.getName().getString(), pos);
            PipelineRunner.runPipeline((ServerWorld) w, pos, PipelineRunner.PipelineMode.PERIODIC);
        }
    }

    /**
     * 5) Остановка сервера → чистим флаг и состояние контроллера.
     */
    public static void onServerStopping() {
        INITIALIZED.clear();
        PENDING_INIT.clear();
        tickCounter = 0;
        LOGGER.debug("Server stopping, state cleared");
    }

    /* ─────────────────────────── Вспомогательное ─────────────────────────── */

    private static void cacheStructureSelectors() {
        TARGET_IDS.clear();
        TARGET_TAGS.clear();
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        for (String sel : selectors) {
            if (sel.startsWith("#")) {
                TARGET_TAGS.add(TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(sel.substring(1))));
            } else {
                TARGET_IDS.add(Identifier.of(sel));
            }
        }
    }

    private static boolean containsTargetStructure(ServerWorld world, Chunk chunk) {
        if (!chunk.hasStructureReferences()) return false;

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (StructureStart start : chunk.getStructureStarts().values()) {
            Structure structure = start.getStructure();
            Identifier id = registry.getId(structure);
            if (id != null && TARGET_IDS.contains(id)) return true;

            RegistryEntry<Structure> entry = registry.getEntry(structure);
            if (entry != null) {
                for (TagKey<Structure> tag : TARGET_TAGS) {
                    if (entry.isIn(tag)) return true;
                }
            }
        }
        return false;
    }
}
