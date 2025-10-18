package net.oxcodsnet.roadarchitect.datagen;

import java.util.function.BiConsumer;

/**
 * Все ключи/значения локализаций лежат в common.
 * Платформенный провайдер просто вызывает fill(locale, builder::add).
 */
public final class RALanguage {
    private RALanguage() {
    }
    public static void fill(String code, BiConsumer<String, String> add) {
        switch (code) {
            case "en_us": {
                add.accept("key.roadarchitect.debug", "Road Graph Debug");
                add.accept("screen.roadarchitect.debug.dimension", "Dimension");
                add.accept("screen.roadarchitect.debug.dimension_label", "Dimension: %s");
                add.accept("category.roadarchitect", "Road Architect");
                add.accept("text.autoconfig.roadarchitect.category.default", "General Settings");
                add.accept("roadarchitect.stage.initialisation", "Initialising");
                add.accept("roadarchitect.stage.scanning", "Scanning Structures");
                add.accept("roadarchitect.stage.pathfinding", "Path Finding");
                add.accept("roadarchitect.stage.postprocess", "Post Processing");
                add.accept("roadarchitect.stage.complete", "Complete");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Initial Scan Radius");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "Radius in chunks to scan for structures when the world is first loaded.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius", "Chunk Generation Scan Radius");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Radius in chunks scanned when new chunks generate.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance", "Max Connection Distance");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Maximum distance in blocks between two structures to connect them.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds", "Pipeline Interval Seconds");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Delay in seconds between pipeline runs.");
                add.accept("text.config.roadarchitect.option.structureSelectors", "Structure Selectors");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "List of structure selectors that roads will connect.");
                add.accept("text.config.roadarchitect.option.dimensionSelectors", "Dimension Selectors");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "List of dimension identifiers where roads should operate.");
                add.accept("text.autoconfig.roadarchitect.title", "Road Architect Config");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "Initial Scan Radius");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "Radius in chunks to scan for structures when the world is first loaded.");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius", "Chunk Generation Scan Radius");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Radius in chunks scanned when new chunks generate.");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance", "Max Connection Distance");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Maximum distance in blocks between two structures to connect them.");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds", "Pipeline Interval Seconds");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Delay in seconds between pipeline runs.");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors", "Structure Selectors");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "List of structure selectors that roads will connect.");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors", "Dimension Selectors");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "List of dimension identifiers where roads should operate.");
                // Deterministic decorations (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "Lamp Interval");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "Distance in blocks along the path between lamp posts.");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "Side Decoration Interval");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "Distance in blocks between side decorations along the path.");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "Buoy Interval");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "Distance in blocks along water path between buoys.");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "Mask Erosion");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "Symmetric erosion near land/water transitions; excludes E points near edges.");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "Deterministic Decorations");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "Place lamps, buoys and sides using a global marker grid (chunk-agnostic).");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "Lamp Posts");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "Enable Custom Lamp Posts");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "Toggle to apply the editable entries below; disable to restore the built-in presets.");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "Lamp Post Entries");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "Define custom lamp post styles. Each entry lists biomes it applies to; if multiple entries cover a biome, a random style is picked.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "Lamp Post Style");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "Biome Selectors");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "Biome IDs or #tags where this style can spawn. Leave empty to turn it into a global fallback.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "Base Block Identifier");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "Block identifier used for the ground support portion (typically a wall).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "Post Block Identifier");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "Block identifier used for the vertical post and arm (typically a fence).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "Lamp Block Identifier");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "Block identifier used for the hanging light (must support hanging lantern placement).");
                // Terrain Analyzer (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "Terrain Analyzer (Beta)");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "Enable Terrain Analyzer");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "Steer roads around rough/mountainous terrain by penalizing height variance.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "Roughness Radius");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "Window radius in blocks to measure height range.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "Roughness Stride");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "Sampling step in blocks within the window.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "Range Threshold");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "Minimum height range before applying penalty.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "Penalty Scale");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "Penalty per block of height range above threshold.");
                // Pathfinding
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "Pathfinding");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "Prefer Land Over Water");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "Adds extra cost to water steps and near-coast cells so land routes are preferred.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "Water Step Penalty");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "Additional cost added on each step in ocean/river biomes when preference is enabled.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "Coast Avoid Buffer (blocks)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "Radius in blocks around water biomes that incurs a proximity penalty.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "Coast Proximity Penalty");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "Penalty applied when within the coast buffer to avoid hugging shorelines.");
                // Partial acceptance
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "Accept High-Progress Partial Paths");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "If A* fails but reaches good convergence (>= threshold), accept the best partial path to increase success rate.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "Partial Acceptance Threshold (%)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "Minimum convergence (in %) to accept a partial path when A* doesn't reach the goal.");
                // Forbidden biomes
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "Forbidden Biomes");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "Forbidden Biome Selectors");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "List of biome selectors (IDs or #tags) that roads cannot traverse.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "Forbidden Proximity Buffer (blocks)");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "Radius around forbidden biomes that adds an extra penalty.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "Forbidden Proximity Penalty");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "Penalty applied when near a forbidden biome.");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "Travel around the world without barriers: RoadArchitect automatically scans your world, finds villages, and other structures, and then lays a network of roads between them.");
                break;
            }
            case "ru_ru": {
                add.accept("key.roadarchitect.debug", "Отладка графа дорог");
                add.accept("screen.roadarchitect.debug.dimension", "Измерение");
                add.accept("screen.roadarchitect.debug.dimension_label", "Измерение: %s");
                add.accept("category.roadarchitect", "Архитектор дорог");
                add.accept("text.autoconfig.roadarchitect.category.default", "Основные настройки");
                add.accept("roadarchitect.stage.initialisation", "Инициализация");
                add.accept("roadarchitect.stage.scanning", "Сканирование структур");
                add.accept("roadarchitect.stage.pathfinding", "Поиск пути");
                add.accept("roadarchitect.stage.postprocess", "Постобработка");
                add.accept("roadarchitect.stage.complete", "Завершено");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Начальный радиус сканирования");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "Радиус в чанках для поиска структур при первом запуске мира.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Радиус сканирования при генерации чанков");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Радиус в чанках, который сканируется при генерации новых чанков.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Максимальная дистанция соединения");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Максимальное расстояние в блоках между двумя структурами для соединения дорогой.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Интервал конвейера (сек)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Задержка в секундах между запусками конвейера.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Селекторы структур");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "Список селекторов структур, которые будут соединяться дорогами.");
                add.accept("text.config.roadarchitect.option.dimensionSelectors",
                        "Селекторы измерений");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Список идентификаторов измерений, где должны работать дороги.");
                add.accept("text.autoconfig.roadarchitect.title", "Конфиг Road Architect");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "Начальный радиус сканирования");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "Радиус в чанках для поиска структур при первом запуске мира.");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius",
                        "Радиус сканирования при генерации чанков");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Радиус в чанках, который сканируется при генерации новых чанков.");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance",
                        "Максимальная дистанция соединения");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Максимальное расстояние в блоках между двумя структурами для соединения дорогой.");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds",
                        "Интервал конвейера (сек)");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Задержка в секундах между запусками конвейера.");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors",
                        "Селекторы структур");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "Список селекторов структур, которые будут соединяться дорогами.");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors",
                        "Селекторы измерений");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Список идентификаторов измерений, где должны работать дороги.");
                // Детерминированные украшения (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "Интервал фонарей");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "Расстояние в блоках вдоль пути между фонарями.");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "Интервал боковых украшений");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "Расстояние в блоках между боковыми украшениями вдоль пути.");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "Интервал буйков");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "Расстояние в блоках вдоль водного участка между буйками.");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "Эрозия маски");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "Симметрическая эрозия у переходов суша/вода; исключает E точек около границ.");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "Детерминированные украшения");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "Размещение по глобальной сетке маркеров (не зависит от чанков).");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "Фонарные столбы");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "Включить пользовательские фонари");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "Включите, чтобы применять настраиваемые записи ниже; отключите, чтобы вернуть встроенные пресеты.");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "Записи фонарей");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "Настройте отдельные стили фонарей. Каждая запись задаёт список биомов; если несколько записей покрывают один биом, стиль выбирается случайно.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "Стиль фонарного столба");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "Селекторы биомов");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "ID биомов или #теги, где применяется стиль. Оставьте пустым, чтобы сделать запись глобальным запасным вариантом.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "Идентификатор блока основания");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "Блок, используемый для опоры на земле (обычно стена).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "Идентификатор блока стойки");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "Блок для вертикальной стойки и кронштейна (обычно забор).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "Идентификатор блока фонаря");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "Блок подвесного света (должен поддерживать подвешенный фонарь).");
                // Анализ рельефа (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "Анализ рельефа (Бета)");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "Включить анализ рельефа");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "Отклонять дороги от неровной/гористой местности с помощью штрафа за разброс высот.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "Радиус окна неровности");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "Радиус в блоках окна для измерения диапазона высот.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "Шаг выборки");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "Шаг выборки в блоках внутри окна.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "Порог диапазона");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "Минимальный разброс высот до включения штрафа.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "Масштаб штрафа");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "Штраф за каждый блок диапазона выше порога.");
                // Поиск пути
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "Поиск пути");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "Предпочитать сушу воде");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "Добавляет доп. стоимость шагам по воде и рядом с побережьем, чтобы отдавать приоритет суше.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "Штраф за шаг по воде");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "Дополнительная стоимость за каждый шаг в биомах океана/реки, когда опция включена.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "Буфер обхода побережья (блоки)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "Радиус в блоках вокруг водных биомов, внутри которого применяется штраф близости.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "Штраф близости к побережью");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "Штраф при нахождении в радиусе буфера, чтобы не прижиматься к береговой линии.");
                // Частичное принятие
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "Принимать частичный путь при высоком прогрессе");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "Если A* не дошёл до цели, но достиг хорошей сходимости (>= порога), принять лучший частичный путь для повышения успешности.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "Порог прогресса для частичного пути (%)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "Минимальный прогресс (в %) для принятия частичного пути, когда цель не достигнута.");
                // Запрещённые биомы
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "Запрещённые биомы");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "Селекторы запрещённых биомов");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "Список селекторов биомов (ID или #теги), по которым дороги не строятся.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "Буфер близости к запрещённым (блоки)");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "Радиус вокруг запрещённых биомов, добавляющий дополнительный штраф.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "Штраф близости к запрещённым");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "Штраф при нахождении рядом с запрещённым биомом.");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "Путешествуйте по миру без барьеров: RoadArchitect автоматически сканирует ваш мир, находит деревни и другие структуры, а затем прокладывает между ними сеть дорог.");
                break;
            }
            case "es_es": {
                add.accept("key.roadarchitect.debug", "Depuración del grafo de carreteras");
                add.accept("screen.roadarchitect.debug.dimension", "Dimensión");
                add.accept("screen.roadarchitect.debug.dimension_label", "Dimensión: %s");
                add.accept("category.roadarchitect", "Arquitecto de Carreteras");
                add.accept("text.autoconfig.roadarchitect.category.default", "Configuración general");
                add.accept("roadarchitect.stage.initialisation", "Inicialización");
                add.accept("roadarchitect.stage.scanning", "Escaneando estructuras");
                add.accept("roadarchitect.stage.pathfinding", "Búsqueda de rutas");
                add.accept("roadarchitect.stage.postprocess", "Postprocesamiento");
                add.accept("roadarchitect.stage.complete", "Completado");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Radio de exploración inicial");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "Radio en chunks para buscar estructuras cuando se carga el mundo por primera vez.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Radio de exploración al generar chunks");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Radio en chunks que se examina al generar nuevos chunks.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Distancia máxima de conexión");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Distancia máxima en bloques entre dos estructuras para conectarlas.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalo del pipeline (segundos)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Retraso en segundos entre ejecuciones del pipeline.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Selectores de estructuras");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "Lista de selectores de estructuras que se conectarán con carreteras.");
                add.accept("text.config.roadarchitect.option.dimensionSelectors",
                        "Selectores de dimensiones");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Lista de identificadores de dimensiones donde deben operar las carreteras.");
                // Búsqueda de rutas
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "Búsqueda de rutas");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "Preferir tierra sobre agua");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "Añade coste extra a pasos por agua y cerca de la costa para preferir rutas terrestres.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "Penalización por paso en agua");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "Coste adicional por cada paso en biomas de océano/río cuando la opción está activada.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "Búfer de costa (bloques)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "Radio en bloques alrededor de biomas acuáticos que aplica una penalización de proximidad.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "Penalización por proximidad a la costa");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "Penalización al estar dentro del búfer para evitar bordear la orilla.");
                // Aceptación parcial
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "Aceptar caminos parciales con alto progreso");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "Si A* falla pero alcanza buena convergencia (>= umbral), aceptar el mejor camino parcial para aumentar la tasa de éxito.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "Umbral de aceptación parcial (%)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "Convergencia mínima (en %) para aceptar un camino parcial cuando A* no llega al objetivo.");
                // Biomas prohibidos
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "Biomas prohibidos");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "Selectores de biomas prohibidos");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "Lista de selectores de biomas (IDs o #etiquetas) por los que las carreteras no pueden pasar.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "Búfer de proximidad prohibida (bloques)");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "Radio alrededor de biomas prohibidos que añade una penalización extra.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "Penalización por proximidad prohibida");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "Penalización al estar cerca de un bioma prohibido.");
                add.accept("text.autoconfig.roadarchitect.title", "Configuración de Road Architect");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "Radio de exploración inicial");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "Radio en chunks para buscar estructuras cuando se carga el mundo por primera vez.");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius",
                        "Radio de exploración al generar chunks");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Radio en chunks que se examina al generar nuevos chunks.");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance",
                        "Distancia máxima de conexión");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Distancia máxima en bloques entre dos estructuras para conectarlas.");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalo del pipeline (segundos)");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Retraso en segundos entre ejecuciones del pipeline.");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors",
                        "Selectores de estructuras");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "Lista de selectores de estructuras que se conectarán con carreteras.");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors",
                        "Selectores de dimensiones");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Lista de identificadores de dimensiones donde deben operar las carreteras.");
                // Decoraciones deterministas (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "Intervalo de farolas");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "Distancia en bloques a lo largo del camino entre farolas.");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "Intervalo de decoraciones laterales");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "Distancia en bloques entre decoraciones laterales a lo largo del camino.");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "Intervalo de boyas");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "Distancia en bloques a lo largo del tramo acuático entre boyas.");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "Erosión de máscara");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "Erosión simétrica cerca de transiciones tierra/agua; excluye E puntos cerca de bordes.");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "Decoraciones deterministas");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "Colocación mediante una cuadrícula global de marcadores (independiente de chunks).");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "Farolas");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "Habilitar farolas personalizadas");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "Activa para aplicar las entradas editables; desactiva para volver a los ajustes integrados.");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "Entradas de farolas");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "Define estilos de farola personalizados. Cada entrada lista los biomas donde se aplica; si varias entradas cubren un mismo bioma, se elige un estilo al azar.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "Estilo de farola");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "Selectores de bioma");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "ID de biomas o #etiquetas donde puede generarse este estilo. Déjalo vacío para usarlo como estilo global de reserva.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "Identificador del bloque base");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "Bloque usado para el soporte en el suelo (normalmente un muro).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "Identificador del bloque del poste");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "Bloque usado para el poste vertical y el brazo (normalmente una valla).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "Identificador del bloque de la lámpara");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "Bloque usado para la luz colgante (debe admitir linternas colgantes).");
                // Analizador de Terreno (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "Analizador de Terreno (Beta)");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "Activar analizador de terreno");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "Desvía carreteras del terreno abrupto/montañoso penalizando la variación de altura.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "Radio de rugosidad");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "Radio (bloques) de la ventana para medir el rango de alturas.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "Paso de muestreo");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "Paso (bloques) dentro de la ventana.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "Umbral de rango");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "Rango mínimo de altura antes de aplicar penalización.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "Escala de penalización");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "Penalización por bloque de rango por encima del umbral.");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "Viaja por el mundo sin barreras: RoadArchitect escanea automáticamente tu mundo, encuentra aldeas y otras estructuras, y luego tiende una red de carreteras entre ellas.");
                break;
            }
            case "fr_fr": {
                add.accept("key.roadarchitect.debug", "Débogage du graphe routier");
                add.accept("screen.roadarchitect.debug.dimension", "Dimension");
                add.accept("screen.roadarchitect.debug.dimension_label", "Dimension : %s");
                add.accept("category.roadarchitect", "Architecte routier");
                add.accept("text.autoconfig.roadarchitect.category.default", "Paramètres généraux");
                add.accept("roadarchitect.stage.initialisation", "Initialisation");
                add.accept("roadarchitect.stage.scanning", "Analyse des structures");
                add.accept("roadarchitect.stage.pathfinding", "Recherche de chemin");
                add.accept("roadarchitect.stage.postprocess", "Post-traitement");
                add.accept("roadarchitect.stage.complete", "Terminé");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Rayon de balayage initial");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "Rayon en chunks pour rechercher des structures lors du premier chargement du monde.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Rayon de balayage de génération de chunks");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Rayon en chunks analysé lors de la génération de nouveaux chunks.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Distance maximale de connexion");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Distance maximale en blocs entre deux structures à relier.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalle du pipeline (secondes)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Délai en secondes entre les exécutions du pipeline.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Sélecteurs de structures");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "Liste des sélecteurs de structures que les routes relieront.");
                add.accept("text.config.roadarchitect.option.dimensionSelectors",
                        "Sélecteurs de dimensions");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Liste des identifiants de dimensions où les routes doivent fonctionner.");
                // Recherche d'itinéraire
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "Recherche d'itinéraire");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "Préférer la terre à l'eau");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "Ajoute un coût supplémentaire aux pas sur l'eau et près des côtes pour privilégier la terre.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "Pénalité par pas sur l'eau");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "Coût supplémentaire pour chaque pas dans les biomes océan/rivière lorsque l'option est activée.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "Marge d'évitement de côte (blocs)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "Rayon en blocs autour des biomes aquatiques appliquant une pénalité de proximité.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "Pénalité de proximité de la côte");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "Pénalité appliquée dans la marge pour éviter de longer le rivage.");
                // Acceptation partielle
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "Accepter les chemins partiels à fort progrès");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "Si A* échoue mais atteint une bonne convergence (>= seuil), accepter le meilleur chemin partiel pour augmenter le taux de réussite.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "Seuil d’acceptation partielle (%)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "Convergence minimale (en %) pour accepter un chemin partiel lorsque A* n’atteint pas l’objectif.");
                // Biomes interdits
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "Biomes interdits");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "Sélecteurs de biomes interdits");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "Liste des sélecteurs de biomes (IDs ou #tags) que les routes ne peuvent pas traverser.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "Marge de proximité interdite (blocs)");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "Rayon autour des biomes interdits ajoutant une pénalité supplémentaire.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "Pénalité de proximité interdite");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "Pénalité appliquée à proximité d'un biome interdit.");
                add.accept("text.autoconfig.roadarchitect.title", "Configuration de Road Architect");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "Rayon de balayage initial");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "Rayon en chunks pour rechercher des structures lors du premier chargement du monde.");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius",
                        "Rayon de balayage de génération de chunks");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Rayon en chunks analysé lors de la génération de nouveaux chunks.");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance",
                        "Distance maximale de connexion");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Distance maximale en blocs entre deux structures à relier.");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalle du pipeline (secondes)");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Délai en secondes entre les exécutions du pipeline.");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors",
                        "Sélecteurs de structures");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "Liste des sélecteurs de structures que les routes relieront.");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors",
                        "Sélecteurs de dimensions");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Liste des identifiants de dimensions où les routes doivent fonctionner.");
                // Décorations déterministes (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "Intervalle des lampadaires");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "Distance en blocs le long de la route entre les lampadaires.");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "Intervalle des décorations latérales");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "Distance en blocs entre les décorations latérales le long de la route.");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "Intervalle des bouées");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "Distance en blocs le long du parcours aquatique entre les bouées.");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "Érosion du masque");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "Érosion symétrique près des transitions terre/eau; exclut E points près des bords.");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "Décorations déterministes");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "Placement via une grille de marqueurs globale (indépendante des chunks).");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "Lampadaires");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "Activer les lampadaires personnalisés");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "Activez pour appliquer les entrées éditables ci-dessous ; désactivez pour revenir aux préréglages intégrés.");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "Entrées de lampadaires");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "Définissez des styles de lampadaire personnalisés. Chaque entrée énumère les biomes concernés ; si plusieurs entrées couvrent un même biome, un style est choisi aléatoirement.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "Style de lampadaire");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "Sélecteurs de biome");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "ID de biome ou #tags où ce style peut apparaître. Laissez vide pour en faire un repli global.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "Identifiant du bloc de base");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "Bloc utilisé pour le support au sol (généralement un mur).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "Identifiant du bloc de poteau");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "Bloc utilisé pour le poteau vertical et le bras (généralement une clôture).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "Identifiant du bloc de lampe");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "Bloc utilisé pour la lumière suspendue (doit supporter les lanternes suspendues).");
                // Analyse du relief (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "Analyse du relief (Bêta)");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "Activer l’analyse du relief");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "Éloigne les routes des zones accidentées/montagneuses via une pénalisation de l’écart d’altitude.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "Rayon de rugosité");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "Rayon (en blocs) de la fenêtre pour mesurer l’étendue d’altitude.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "Pas d’échantillonnage");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "Pas (en blocs) d’échantillonnage dans la fenêtre.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "Seuil d’étendue");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "Étendue minimale d’altitude avant pénalisation.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "Échelle de pénalité");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "Pénalité par bloc d’étendue au‑delà du seuil.");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "Voyagez dans le monde sans barrières : RoadArchitect analyse automatiquement votre monde, trouve les villages et autres structures, puis trace un réseau de routes entre eux.");
                break;
            }
            case "de_de": {
                add.accept("key.roadarchitect.debug", "Straßengraph-Debug");
                add.accept("screen.roadarchitect.debug.dimension", "Dimension");
                add.accept("screen.roadarchitect.debug.dimension_label", "Dimension: %s");
                add.accept("category.roadarchitect", "Straßenarchitekt");
                add.accept("text.autoconfig.roadarchitect.category.default", "Allgemeine Einstellungen");
                add.accept("roadarchitect.stage.initialisation", "Initialisierung");
                add.accept("roadarchitect.stage.scanning", "Strukturen scannen");
                add.accept("roadarchitect.stage.pathfinding", "Wegfindung");
                add.accept("roadarchitect.stage.postprocess", "Nachbearbeitung");
                add.accept("roadarchitect.stage.complete", "Abgeschlossen");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Anfänglicher Scanradius");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "Radius in Chunks zum Suchen nach Strukturen beim ersten Laden der Welt.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Scanradius bei Chunk-Generierung");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Radius in Chunks, der beim Generieren neuer Chunks durchsucht wird.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Maximale Verbindungsdistanz");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Maximaler Abstand in Blöcken zwischen zwei Strukturen, die verbunden werden.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Pipeline-Intervall (Sekunden)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Verzögerung in Sekunden zwischen Pipeline-Durchläufen.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Strukturauswahlen");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "Liste von Strukturauswahlen, die Straßen verbinden.");
                add.accept("text.config.roadarchitect.option.dimensionSelectors",
                        "Dimensionsauswahlen");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Liste von Dimensions-IDs, in denen Straßen arbeiten sollen.");
                add.accept("text.autoconfig.roadarchitect.title", "Road Architect Konfiguration");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "Anfänglicher Scanradius");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "Radius in Chunks zum Suchen nach Strukturen beim ersten Laden der Welt.");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius",
                        "Scanradius bei Chunk-Generierung");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Radius in Chunks, der beim Generieren neuer Chunks durchsucht wird.");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance",
                        "Maximale Verbindungsdistanz");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Maximaler Abstand in Blöcken zwischen zwei Strukturen, die verbunden werden.");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds",
                        "Pipeline-Intervall (Sekunden)");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Verzögerung in Sekunden zwischen Pipeline-Durchläufen.");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors",
                        "Strukturauswahlen");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "Liste von Strukturauswahlen, die Straßen verbinden.");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors",
                        "Dimensionsauswahlen");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Liste von Dimensions-IDs, in denen Straßen arbeiten sollen.");
                // Deterministische Dekorationen (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "Laternenintervall");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "Abstand in Blöcken entlang des Pfads zwischen Laternen.");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "Seiten-Dekor-Intervall");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "Abstand in Blöcken zwischen seitlichen Dekorationen entlang des Pfads.");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "Bojenintervall");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "Abstand in Blöcken entlang des Wasserpfads zwischen Bojen.");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "Maskenerosion");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "Symmetrische Erosion nahe Land/Wasser-Übergängen; schließt E Punkte an den Rändern aus.");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "Deterministische Dekorationen");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "Platzierung über globales Markerraster (chunk-unabhängig).");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "Laternenmasten");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "Benutzerdefinierte Lampen aktivieren");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "Aktiviere, um die bearbeitbaren Einträge unten zu verwenden; deaktiviere für die eingebauten Presets.");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "Laternen-Einträge");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "Definiere individuelle Laternenstile. Jede Eingabe listet Biome auf; decken mehrere Einträge dasselbe Biom ab, wird ein Stil zufällig gewählt.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "Laternenstil");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "Biom-Selektoren");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "Biome-IDs oder #Tags, in denen dieser Stil erscheinen darf. Leer lassen, um ihn als globalen Fallback zu nutzen.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "Blockkennung für Fundament");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "Block für den Bodensockel (meist eine Mauer).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "Blockkennung für Pfosten");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "Block für den vertikalen Pfosten und Ausleger (meist ein Zaun).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "Blockkennung für Lampe");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "Block für die hängende Leuchte (muss hängende Laternen unterstützen).");
                // Reliefanalyse (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "Reliefanalyse (Beta)");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "Reliefanalyse aktivieren");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "Leitet Straßen um unebenes/bergiges Gelände, indem Höhenunterschiede bestraft werden.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "Rauheitsradius");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "Fensterradius (Blöcke) zur Messung der Höhenspanne.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "Abtastschritt");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "Abtastschritt (Blöcke) innerhalb des Fensters.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "Spannenschwelle");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "Minimale Höhenspan­ne, bevor eine Strafe gilt.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "Strafskala");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "Strafe pro Block Spannweite über dem Schwellenwert.");
                // Pfadsuche
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "Pfadsuche");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "Land gegenüber Wasser bevorzugen");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "Fügt zusätzliche Kosten für Schritte auf Wasser und in Küstennähe hinzu, um Landrouten zu bevorzugen.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "Wasser-Schrittstrafe");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "Zusätzliche Kosten pro Schritt in Ozean-/Flussbiomen, wenn aktiviert.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "Küsten-Puffer (Blöcke)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "Radius in Blöcken um Wasserbiome, der eine Näherungsstrafe anwendet.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "Nähe-Strafe zur Küste");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "Strafe innerhalb des Puffers, um das Entlanglaufen der Küste zu vermeiden.");
                // Partielle Annahme
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "Teilpfade bei hohem Fortschritt akzeptieren");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "Wenn A* scheitert, aber eine gute Konvergenz erreicht (>= Schwelle), den besten Teilpfad akzeptieren, um die Erfolgsrate zu erhöhen.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "Schwelle für Teilpfad (%)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "Mindestfortschritt (in %), um einen Teilpfad zu akzeptieren, wenn das Ziel nicht erreicht wird.");
                // Verbotene Biome
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "Verbotene Biome");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "Selektoren verbotener Biome");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "Liste von Biom-Selektoren (IDs oder #Tags), durch die keine Straßen verlaufen dürfen.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "Puffer für verbotene Nähe (Blöcke)");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "Radius um verbotene Biome, der eine zusätzliche Strafe hinzufügt.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "Strafe für verbotene Nähe");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "Strafe in der Nähe eines verbotenen Bioms.");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "Reisen Sie barrierefrei durch die Welt: RoadArchitect scannt automatisch Ihre Welt, findet Dörfer und andere Strukturen und legt anschließend ein Straßennetz zwischen ihnen an.");
                break;
            }
            case "zh_cn": {
                add.accept("key.roadarchitect.debug", "道路网络调试");
                add.accept("screen.roadarchitect.debug.dimension", "维度");
                add.accept("screen.roadarchitect.debug.dimension_label", "维度：%s");
                add.accept("category.roadarchitect", "道路架构师");
                add.accept("text.autoconfig.roadarchitect.category.default", "常规设置");
                add.accept("roadarchitect.stage.initialisation", "初始化");
                add.accept("roadarchitect.stage.scanning", "扫描结构");
                add.accept("roadarchitect.stage.pathfinding", "路径搜索");
                add.accept("roadarchitect.stage.postprocess", "后期处理");
                add.accept("roadarchitect.stage.complete", "完成");
                add.accept("text.config.roadarchitect.option.initScanRadius", "初始扫描半径");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "在世界首次加载时扫描结构的区块半径。");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius", "区块生成扫描半径");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "在新生成区块时扫描的区块半径。");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance", "最大连接距离");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "两结构间允许连接的最大方块距离。");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds", "管线间隔（秒）");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "每次管线运行之间的秒数。");
                add.accept("text.config.roadarchitect.option.structureSelectors", "结构选择器");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "将被道路连接的结构选择器列表。");
                add.accept("text.config.roadarchitect.option.dimensionSelectors", "维度选择器");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "道路应当运行的维度标识符列表。");
                add.accept("text.autoconfig.roadarchitect.title", "Road Architect 配置");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "初始扫描半径");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "在世界首次加载时扫描结构的区块半径。");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius", "区块生成扫描半径");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "在新生成区块时扫描的区块半径。");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance", "最大连接距离");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "两结构间允许连接的最大方块距离。");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds", "管线间隔（秒）");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "每次管线运行之间的秒数。");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors", "结构选择器");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "将被道路连接的结构选择器列表。");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors", "维度选择器");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "道路应当运行的维度标识符列表。");
                // 确定性装饰 (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "灯间距");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "沿路径的灯之间的方块距离。");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "侧边装饰间距");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "沿路径的侧边装饰之间的方块距离。");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "浮标间距");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "沿水路的浮标之间的方块距离。");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "遮罩侵蚀");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "在陆地/水域过渡附近的对称侵蚀；排除边缘附近的 E 个点。");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "确定性装饰");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "使用全局标记网格进行放置（与区块无关）。");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "路灯");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "启用自定义路灯");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "启用以使用下方可编辑的条目；禁用则恢复内置预设。");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "路灯条目");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "配置自定义路灯样式。每个条目列出生效的生物群系；当多个条目覆盖同一生物群系时，将随机选择样式。");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "灯柱样式");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "生物群系选择器");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "该样式可生成的生物群系 ID 或 #标签；留空则作为全局备用样式。");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "基础方块标识符");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "用于地面支撑的方块（通常是墙）。");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "立柱方块标识符");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "用于竖直立柱和横臂的方块（通常是栅栏）。");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "灯体方块标识符");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "用于悬挂灯的方块（必须支持悬挂灯笼）。");
                // 地形分析 (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "地形分析（测试版）");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "启用地形分析");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "通过惩罚高度变化，让道路绕开崎岖/山地地形。");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "粗糙度半径");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "用于测量高度范围的窗口半径（方块）。");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "采样步长");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "窗口内的采样步长（方块）。");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "范围阈值");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "应用惩罚前的最小高度范围。");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "惩罚系数");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "超过阈值的每格高度范围所施加的惩罚。");
                // 路径搜索
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "路径搜索");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "优先选择陆路而非水路");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "为水面步进和近海岸格子增加额外代价，从而偏向陆路。");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "水面步进惩罚");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "启用后，在海洋/河流生物群系每一步增加的额外代价。");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "海岸避让缓冲（方块）");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "围绕水域生物群系的半径（方块），在其中会应用邻近惩罚。");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "海岸邻近惩罚");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "位于缓冲区内时施加的惩罚，避免沿着海岸线行进。");
                // 部分接受
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "在高进度时接受部分路径");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "当 A* 未到达目标，但收敛良好（>= 阈值）时，接受最佳的部分路径以提高成功率。");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "部分路径接受阈值（%）");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "当未达目标时，接受部分路径所需的最小收敛百分比。");
                // 禁止生物群系
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "禁止的生物群系");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "禁止生物群系选择器");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "生物群系选择器列表（ID 或 #标签），道路不能穿过这些群系。");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "禁止邻近缓冲（方块）");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "围绕禁止生物群系的半径，添加额外惩罚。");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "禁止邻近惩罚");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "靠近禁止生物群系时施加的惩罚。");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "畅游无障碍的世界：RoadArchitect 会自动扫描你的世界，找到村庄和其他结构，然后在它们之间铺设道路网络。");
                break;
            }
            case "uk_ua": {
                add.accept("key.roadarchitect.debug", "Налагодження графіка доріг");
                add.accept("screen.roadarchitect.debug.dimension", "Вимір");
                add.accept("screen.roadarchitect.debug.dimension_label", "Вимір: %s");
                add.accept("category.roadarchitect", "Road Architect");
                add.accept("text.autoconfig.roadarchitect.category.default", "Загальні налаштування");
                add.accept("roadarchitect.stage.initialisation", "Ініціалізація…");
                add.accept("roadarchitect.stage.scanning", "Сканування структур…");
                add.accept("roadarchitect.stage.pathfinding", "Пошук шляху…");
                add.accept("roadarchitect.stage.postprocess", "Постобробка…");
                add.accept("roadarchitect.stage.complete", "Завершення…");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Початковий радіус сканування");
                add.accept("text.config.roadarchitect.option.initScanRadius.@Tooltip",
                        "Радіус у чанках для пошуку структур під час першого завантаження світу.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius", "Радіус сканування генерації чанків");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Радіус у чанках, який сканується під час генерації нових чанків.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance", "Максимальна відстань з’єднання");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Максимальна відстань у блоках між двома структурами для їх з’єднання.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds", "Секунди інтервалу конвеєра");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Затримка в секундах між запусками конвеєра.");
                add.accept("text.config.roadarchitect.option.structureSelectors", "Селектори структур");
                add.accept("text.config.roadarchitect.option.structureSelectors.@Tooltip",
                        "Список селекторів структур, які з'єднуватимуть дороги.");
                add.accept("text.config.roadarchitect.option.dimensionSelectors", "Селектори вимірів");
                add.accept("text.config.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Список ідентифікаторів вимірів, у яких мають працювати дороги.");
                add.accept("text.autoconfig.roadarchitect.title", "Налаштування Road Architect");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius", "Початковий радіус сканування");
                add.accept("text.autoconfig.roadarchitect.option.initScanRadius.@Tooltip",
                        "Радіус у чанках для пошуку структур під час першого завантаження світу.");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius", "Радіус сканування генерації чанків");
                add.accept("text.autoconfig.roadarchitect.option.chunkGenerateScanRadius.@Tooltip",
                        "Радіус у чанках, який сканується під час генерації нових чанків.");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance", "Максимальна відстань з’єднання");
                add.accept("text.autoconfig.roadarchitect.option.maxConnectionDistance.@Tooltip",
                        "Максимальна відстань у блоках між двома структурами для їх з’єднання.");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds", "Секунди інтервалу конвеєра");
                add.accept("text.autoconfig.roadarchitect.option.pipelineIntervalSeconds.@Tooltip",
                        "Затримка в секундах між запусками конвеєра.");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors", "Селектори структур");
                add.accept("text.autoconfig.roadarchitect.option.structureSelectors.@Tooltip",
                        "Список селекторів структур, які з'єднуватимуть дороги.");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors", "Селектори вимірів");
                add.accept("text.autoconfig.roadarchitect.option.dimensionSelectors.@Tooltip",
                        "Список ідентифікаторів вимірів, у яких мають працювати дороги.");
                // Детерміновані прикраси (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.option.lampInterval", "Інтервал ліхтарів");
                add.accept("text.autoconfig.roadarchitect.option.lampInterval.@Tooltip",
                        "Відстань у блоках уздовж дороги між ліхтарними стовпами.");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval", "Інтервал декорацій парканів");
                add.accept("text.autoconfig.roadarchitect.option.sideDecorationInterval.@Tooltip",
                        "Відстань у блоках між прикрасами парканів вздовж дороги.");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval", "Інтервал буйків");
                add.accept("text.autoconfig.roadarchitect.option.buoyInterval.@Tooltip",
                        "Відстань у блоках між буйками на воді.");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion", "Ерозія маски");
                add.accept("text.autoconfig.roadarchitect.option.maskErosion.@Tooltip",
                        "Симетрична ерозія біля переходів землі/води; виключає точки Е біля країв.");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations", "Детерміновані прикраси");
                add.accept("text.autoconfig.roadarchitect.option.deterministicDecorations.@Tooltip",
                        "Розміщує ліхтарі, буйки та паркани, використовуючи глобальну сітку маркерів (незалежно від чанків).");
                add.accept("text.autoconfig.roadarchitect.category.lampPosts", "Ліхтарні стовпи");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled", "Увімкнути користувацькі ліхтарі");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.enabled.@Tooltip",
                        "Увімкніть, щоб застосувати записи, які можна редагувати нижче; вимкніть, щоб повернути вбудовані пресети.");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides", "Записи ліхтарів");
                add.accept("text.autoconfig.roadarchitect.option.lampPosts.overrides.@Tooltip",
                        "Налаштуйте окремі стилі ліхтарів. Кожна запис має список біомів; якщо кілька записів охоплюють один біом, стиль обирається випадково.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition", "Стиль ліхтарного стовпа");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors", "Селектори біомів");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.biomeSelectors.@Tooltip",
                        "ID біомів або #теги, де може з’явитися цей стиль. Залиште порожнім, щоб зробити запис глобальним запасним варіантом.");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock", "Ідентифікатор блока основи");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.baseBlock.@Tooltip",
                        "Блок для опори на землі (зазвичай стіна).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock", "Ідентифікатор блока стійки");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.postBlock.@Tooltip",
                        "Блок для вертикальної стійки та кронштейна (зазвичай паркан).");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock", "Ідентифікатор блока ліхтаря");
                add.accept("text.autoconfig.roadarchitect.option.LampPostDefinition.lampBlock.@Tooltip",
                        "Блок підвісного світла (має підтримувати підвішені ліхтарі).");
                // Аналіз рельєфу (AutoConfig)
                add.accept("text.autoconfig.roadarchitect.category.terrainAnalyzer", "Аналіз рельєфу (Бета)");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled", "Увімкнути аналіз рельєфу");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.enabled.@Tooltip",
                        "Відхиляє дороги від нерівної/гірської місцевості, штрафуючи розкид висот.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius", "Радіус шорсткості");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRadius.@Tooltip",
                        "Радіус (у блоках) вікна для вимірювання діапазону висот.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride", "Крок вибірки");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughStride.@Tooltip",
                        "Крок (у блоках) вибірки всередині вікна.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold", "Поріг діапазону");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughRangeThreshold.@Tooltip",
                        "Мінімальний діапазон висот перед застосуванням штрафу.");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale", "Масштаб штрафу");
                add.accept("text.autoconfig.roadarchitect.option.terrainAnalyzer.roughPenaltyScale.@Tooltip",
                        "Штраф за кожен блок діапазону понад поріг.");
                // Пошук шляху
                add.accept("text.autoconfig.roadarchitect.category.pathfinding", "Пошук шляху");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater", "Надавати перевагу суші над водою");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.preferLandOverWater.@Tooltip",
                        "Додає додаткову вартість крокам по воді та поруч з узбережжям, щоб надавати перевагу суші.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty", "Штраф за крок по воді");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.waterStepPenalty.@Tooltip",
                        "Додаткова вартість за кожен крок в океані/річці, коли опцію увімкнено.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks", "Буфер обходу узбережжя (блоки)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastAvoidBufferBlocks.@Tooltip",
                        "Радіус у блоках навколо водних біомів, де застосовується штраф близькості.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty", "Штраф близькості до узбережжя");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.coastProximityPenalty.@Tooltip",
                        "Штраф, що застосовується в межах буфера, щоб не йти вздовж берега.");
                // Часткове прийняття
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial", "Приймати частковий шлях за високого прогресу");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.acceptHighProgressPartial.@Tooltip",
                        "Якщо A* не дійшов до цілі, але досяг хорошої сходимості (>= порога), приймати найкращий частковий шлях, щоб підвищити успішність.");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent", "Поріг прогресу для часткового шляху (%)");
                add.accept("text.autoconfig.roadarchitect.option.pathfinding.partialProgressPercent.@Tooltip",
                        "Мінімальний прогрес (у %), щоб прийняти частковий шлях, коли ціль не досягнута.");
                // Заборонені біоми
                add.accept("text.autoconfig.roadarchitect.category.forbiddenBiomes", "Заборонені біоми");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors", "Селектори заборонених біомів");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.selectors.@Tooltip",
                        "Список селекторів біомів (ID або #теги), через які дороги не прокладаються.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks", "Буфер близькості до заборонених (блоки)");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.bufferBlocks.@Tooltip",
                        "Радіус навколо заборонених біомів, що додає додатковий штраф.");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty", "Штраф близькості до заборонених");
                add.accept("text.autoconfig.roadarchitect.option.forbiddenBiomes.proximityPenalty.@Tooltip",
                        "Штраф при знаходженні поруч із забороненим біомом.");
                add.accept("modmenu.descriptionTranslation.roadarchitect",
                        "Подорожуйте навколо світу без бар'єрів: RoadArchitect автоматично сканує ваш світ, знаходить села та інші структури, а потім прокладає мережу доріг між ними");
                break;
            }
            default: {
                break;
            }
        }
    }
}
