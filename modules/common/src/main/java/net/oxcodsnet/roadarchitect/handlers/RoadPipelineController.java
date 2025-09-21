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
     * Флаг активной интеграции с Distant Horizons.
     */
    private static volatile boolean dhIntegrationActive = false;

    /**
     * Отложенные INIT по мирам: запуск через N тиков после генерации спавн‑чанка.
     * quietTicks > 0 означает: запускать только после "тишины" по загрузкам чанков
     * продолжительностью не менее quietTicks (для совместимости с предгеном DH).
     */
    private static final class PendingInit {
        final BlockPos pos; // может быть фикcированным центром; если spawnCentered=true — игнорируется
        int ticksLeft;
        final int quietTicks;
        final boolean spawnCentered; // true: взять центр как текущий world.getSpawnPos() в момент запуска
        PendingInit(BlockPos pos, int ticksLeft, int quietTicks) {
            this(pos, ticksLeft, quietTicks, false);
        }
        PendingInit(BlockPos pos, int ticksLeft, int quietTicks, boolean spawnCentered) {
            this.pos = pos;
            this.ticksLeft = ticksLeft;
            this.quietTicks = quietTicks;
            this.spawnCentered = spawnCentered;
        }
    }
    private static final Map<RegistryKey<World>, PendingInit> PENDING_INIT = new ConcurrentHashMap<>();

    /**
     * Глобальный счётчик тиков и отметка последней загрузки чанка по мирам.
     */
    private static int globalTick = 0;
    private static final Map<RegistryKey<World>, Integer> LAST_CHUNK_LOAD_TICK = new ConcurrentHashMap<>();

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
        globalTick = 0;
        LOGGER.debug("RoadPipelineController initialized (selectors cached)");
    }

    /**
     * Активировать/деактивировать поведение, специфичное для Distant Horizons.
     */
    public static void setDhIntegrationActive(boolean active) {
        if (dhIntegrationActive != active) {
            dhIntegrationActive = active;
            LOGGER.debug("DH integration active: {}", active);
        }
    }

    public static boolean isDhIntegrationActive() {
        return dhIntegrationActive;
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

        int delay = 20; // ~1 секунда базовая задержка
        PENDING_INIT.put(world.getRegistryKey(), new PendingInit(world.getSpawnPos(), delay, 0));
        LOGGER.debug("Scheduled deferred INIT for {} at {} ({} ticks)",
                world.getRegistryKey().getValue(), world.getSpawnPos(), delay);
    }

    /**
     * DH-aware: планируем INIT с требованием дождаться периода "тишины" по загрузкам чанков.
     */
    public static void onSpawnChunkGeneratedDhAware(ServerWorld world, Chunk chunk, int quietTicks) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        ChunkPos spawnChunk = new ChunkPos(world.getSpawnPos());
        if (!chunk.getPos().equals(spawnChunk)) return;

        if (INITIALIZED.contains(world.getRegistryKey())) return;
        if (PENDING_INIT.containsKey(world.getRegistryKey())) return;

        int delay = 1; // почти сразу, дальнейшее ожидание регулирует quietTicks
        PENDING_INIT.put(world.getRegistryKey(), new PendingInit(world.getSpawnPos(), delay, Math.max(0, quietTicks), true));
        LOGGER.debug("Scheduled DH-aware deferred INIT for {} at {} ({} ticks, quiet={} ticks)",
                world.getRegistryKey().getValue(), world.getSpawnPos(), delay, quietTicks);
    }

    /**
     * Гарантирует, что INIT запланирован (один раз) для мира — вариант для DH.
     * Не проверяет спавн-чанк; центр берём как текущий спавн при запуске.
     */
    public static void ensureInitScheduledDhAware(ServerWorld world, int quietTicks) {
        if (world.getRegistryKey() != World.OVERWORLD) return;
        if (INITIALIZED.contains(world.getRegistryKey())) return;
        if (PENDING_INIT.containsKey(world.getRegistryKey())) return;
        int delay = 1;
        PENDING_INIT.put(world.getRegistryKey(), new PendingInit(null, delay, Math.max(0, quietTicks), true));
        LOGGER.debug("Ensured DH-aware INIT scheduled for {} ({} ticks, quiet={} ticks)",
                world.getRegistryKey().getValue(), delay, quietTicks);
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
     * Уведомить контроллер о любой загрузке чанка для измерения "тишины" по миру.
     */
    public static void onAnyChunkLoad(ServerWorld world) {
        LAST_CHUNK_LOAD_TICK.put(world.getRegistryKey(), globalTick);
    }

    /**
     * 3) Игрок вошёл на сервер → PERIODIC (как в исходнике).
     */
    public static void onPlayerJoin(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        RegistryKey<World> key = world.getRegistryKey();
        if (key != World.OVERWORLD) return;

        if (!INITIALIZED.contains(key)) {
            PendingInit pending = PENDING_INIT.get(key);
            if (!dhIntegrationActive && pending != null) {
                LOGGER.debug("Player {} joined; INIT pending for {} (ticksLeft={}, quiet={} ticks) — waiting for scheduled run",
                        player.getName().getString(), key.getValue(), pending.ticksLeft, pending.quietTicks);
                return;
            }

            BlockPos center = world.getSpawnPos();
            if (pending != null && pending.pos != null && !pending.spawnCentered) {
                center = pending.pos;
            }

            LOGGER.debug("Player {} joined; INIT not done yet for {}. Running INIT at {} (dhIntegrationActive={}, pendingPresent={})",
                    player.getName().getString(), key.getValue(), center, dhIntegrationActive, pending != null);

            PENDING_INIT.remove(key);
            PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.INIT);
            INITIALIZED.add(key);
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
        globalTick++;
        tickCounter++;

        // Обработка отложенных INIT по мирам
        for (ServerWorld world : server.getWorlds()) {
            RegistryKey<World> key = world.getRegistryKey();
            PendingInit pending = PENDING_INIT.get(key);
            if (pending == null) continue;
            if (INITIALIZED.contains(key)) { PENDING_INIT.remove(key); continue; }
            if (--pending.ticksLeft <= 0) {
                if (pending.quietTicks > 0) {
                    int last = LAST_CHUNK_LOAD_TICK.getOrDefault(key, -1);
                    int since = (last < 0) ? Integer.MAX_VALUE : (globalTick - last);
                    if (since < pending.quietTicks) {
                        pending.ticksLeft = pending.quietTicks - since; // ждём тишину
                        continue;
                    }
                }
                BlockPos center = pending.spawnCentered ? world.getSpawnPos() : pending.pos;
                LOGGER.debug("Running deferred INIT for {} at {} (quiet={} ticks, spawnCentered={})",
                        key.getValue(), center, pending.quietTicks, pending.spawnCentered);
                PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.INIT);
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
        globalTick = 0;
        LAST_CHUNK_LOAD_TICK.clear();
        LOGGER.debug("Server stopping, state cleared");
    }

    /**
     * Немедленный запуск INIT для мира (если ещё не выполнялся).
     * Использовать для раннего старта при наличии DH, когда известен корректный центр (спавн).
     */
    public static void runInitNow(ServerWorld world, BlockPos center) {
        if (world.getRegistryKey() != World.OVERWORLD) return;
        RegistryKey<World> key = world.getRegistryKey();
        if (INITIALIZED.contains(key)) return;
        // снимаем отложенный, если был
        PENDING_INIT.remove(key);
        LOGGER.debug("Running immediate INIT for {} at {} (DH compat)", key.getValue(), center);
        PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.INIT);
        INITIALIZED.add(key);
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
