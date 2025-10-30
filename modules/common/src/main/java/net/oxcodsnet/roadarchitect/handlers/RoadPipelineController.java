package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
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
    private static final Set<ResourceKey<Level>> INITIALIZED = ConcurrentHashMap.newKeySet();

    /**
     * Кеш селекторов (ID и теги) из конфигурации, для быстрых проверок.
     */
    private static final Set<ResourceLocation> TARGET_IDS = new HashSet<>();
    private static final Set<TagKey<Structure>> TARGET_TAGS = new HashSet<>();
    private static final Set<ResourceLocation> TARGET_DIMENSION_IDS = new HashSet<>();

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
        DebugLog.info(LOGGER, "RoadPipelineController initialized (selectors cached)");
    }

    /**
     * Вызывать при обновлении конфига — перекешируем селекторы.
     */
    public static void refreshStructureSelectorCache() {
        cacheStructureSelectors();
        DebugLog.info(LOGGER, "RoadPipelineController reloaded selectors from config");
    }

    /* ───────────────────────── Точные кейсы из исходного register() ───────────────────────── */

    /**
     * 1) Генерация спавн-чанка ВПЕРВЫЕ → INIT.
     */
    public static void onSpawnChunkGenerated(ServerLevel world, ChunkAccess chunk) {
        if (!isDimensionEnabled(world.dimension())) return;

        ChunkPos spawnChunk = new ChunkPos(world.getRespawnData().pos());
        if (!chunk.getPos().equals(spawnChunk)) return;

        if (INITIALIZED.add(world.dimension())) {
            DebugLog.info(LOGGER, "Spawn chunk {} generated in {}, starting INIT pipeline",
                    chunk.getPos(), world.dimension().location());
            PipelineRunner.runPipeline(world, world.getRespawnData().pos(), PipelineRunner.PipelineMode.INIT);
        }
    }

    /**
     * 2) Генерация ЛЮБОГО чанка; если внутри есть целевая структура → CHUNK.
     */
    public static void onChunkGenerated(ServerLevel world, ChunkAccess chunk) {
        if (!isDimensionEnabled(world.dimension())) return;
        if (!containsTargetStructure(world, chunk)) return;

        BlockPos center = chunk.getPos().getMiddleBlockPosition(0);
        DebugLog.info(LOGGER, "Chunk {} generated with target structure, starting CHUNK pipeline", chunk.getPos());
        PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.CHUNK);
    }

    /**
     * 3) Игрок вошёл на сервер → PERIODIC (как в исходнике).
     */
    public static void onPlayerJoin(ServerPlayer player) {
        ServerLevel world = player.level();
        if (!isDimensionEnabled(world.dimension())) return;

        BlockPos pos = player.blockPosition();
        DebugLog.info(LOGGER, "Player {} joined at {}, starting PERIODIC pipeline",
                player.getName().getString(), pos);
        PipelineRunner.runPipeline(world, pos, PipelineRunner.PipelineMode.PERIODIC);
    }

    /**
     * 4) Периодический триггер раз в N секунд (из конфига) – START_SERVER_TICK.
     */
    public static void onServerTick(MinecraftServer server) {
        int intervalTicks = Math.max(1, RoadArchitect.CONFIG.pipelineIntervalSeconds() * 20);
        tickCounter++;
        if (tickCounter < intervalTicks) return;
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel w = player.level();
            if (!isDimensionEnabled(w.dimension())) continue;

            BlockPos pos = player.blockPosition();
            DebugLog.info(LOGGER, "Periodic trigger at player {} pos {}, starting PERIODIC pipeline",
                    player.getName().getString(), pos);
            PipelineRunner.runPipeline(w, pos, PipelineRunner.PipelineMode.PERIODIC);
        }
    }

    /**
     * 5) Остановка сервера → чистим флаг и состояние контроллера.
     */
    public static void onServerStopping() {
        INITIALIZED.clear();
        tickCounter = 0;
        DebugLog.info(LOGGER, "Server stopping, state cleared");
    }

    /* ─────────────────────────── Вспомогательное ─────────────────────────── */

    private static void cacheStructureSelectors() {
        TARGET_IDS.clear();
        TARGET_TAGS.clear();
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        for (String sel : selectors) {
            if (sel.startsWith("#")) {
                TARGET_TAGS.add(TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(sel.substring(1))));
            } else {
                TARGET_IDS.add(ResourceLocation.parse(sel));
            }
        }

        TARGET_DIMENSION_IDS.clear();
        List<String> dimensionSelectors = RoadArchitect.CONFIG.dimensionSelectors();
        if (dimensionSelectors == null || dimensionSelectors.isEmpty()) {
            TARGET_DIMENSION_IDS.add(Level.OVERWORLD.location());
        } else {
            for (String selector : dimensionSelectors) {
                if (selector.startsWith("#")) {
                    LOGGER.warn("Dimension selector tags are not supported (skipping '{}')", selector);
                    continue;
                }
                try {
                    TARGET_DIMENSION_IDS.add(ResourceLocation.parse(selector));
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Skipping invalid dimension selector '{}': {}", selector, ex.getMessage());
                }
            }
            if (TARGET_DIMENSION_IDS.isEmpty()) {
                TARGET_DIMENSION_IDS.add(Level.OVERWORLD.location());
            }
        }
    }

    static boolean isDimensionEnabled(ResourceKey<Level> key) {
        if (TARGET_DIMENSION_IDS.isEmpty()) {
            return key == Level.OVERWORLD;
        }
        return TARGET_DIMENSION_IDS.contains(key.location());
    }

    private static boolean containsTargetStructure(ServerLevel world, ChunkAccess chunk) {
        if (!chunk.hasAnyStructureReferences()) return false;

        Registry<Structure> registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (StructureStart start : chunk.getAllStarts().values()) {
            Structure structure = start.getStructure();
            ResourceLocation id = registry.getKey(structure);
            if (id != null && TARGET_IDS.contains(id)) return true;

            Holder<Structure> entry = registry.wrapAsHolder(structure);
            if (entry != null) {
                for (TagKey<Structure> tag : TARGET_TAGS) {
                    if (entry.is(tag)) return true;
                }
            }
        }
        return false;
    }
}
