package net.oxcodsnet.roadarchitect.util;

import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.config.RoadDecorationType;
import net.oxcodsnet.roadarchitect.config.records.RoadStyleConfigEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RoadStyleDefaultUtil {
    @NotNull
    public static ArrayList<RoadArchitectConfigData.RoadStyleDefinition> getRoadStyleDefinitions(List<RoadStyleConfigEntry> defaults) {
        ArrayList<RoadArchitectConfigData.RoadStyleDefinition> list = new ArrayList<>();
        for (RoadStyleConfigEntry entry : defaults) {
            RoadArchitectConfigData.RoadStyleDefinition def = new RoadArchitectConfigData.RoadStyleDefinition();
            def.biomeSelectors = new ArrayList<>(entry.biomeSelectors());
            def.palette = new ArrayList<>();
            for (RoadStyleConfigEntry.SurfaceBlockEntry blockEntry : entry.palette()) {
                RoadArchitectConfigData.RoadPaletteEntry paletteEntry = new RoadArchitectConfigData.RoadPaletteEntry();
                paletteEntry.block = blockEntry.block();
                paletteEntry.weight = blockEntry.weight();
                def.palette.add(paletteEntry);
            }
            def.decorations = new ArrayList<>();
            for (RoadStyleConfigEntry.DecorationEntry decorationEntry : entry.decorations()) {
                RoadArchitectConfigData.RoadDecorationEntry deco = new RoadArchitectConfigData.RoadDecorationEntry();
                deco.type = decorationEntry.type();
                deco.block = decorationEntry.block();
                def.decorations.add(deco);
            }
            list.add(def);
        }
        return list;
    }

    public static RoadStyleConfigEntry entry(List<String> selectors, List<RoadStyleConfigEntry.SurfaceBlockEntry> palette, List<RoadStyleConfigEntry.DecorationEntry> decorations) {
        return new RoadStyleConfigEntry(selectors, palette, decorations);
    }

    public static List<RoadStyleConfigEntry.SurfaceBlockEntry> palette(RoadStyleConfigEntry.SurfaceBlockEntry... entries) {
        return List.of(entries);
    }

    public static List<RoadStyleConfigEntry.DecorationEntry> decorations(RoadStyleConfigEntry.DecorationEntry... entries) {
        return List.of(entries);
    }

    public static RoadStyleConfigEntry.SurfaceBlockEntry block(String id, int weight) {
        return new RoadStyleConfigEntry.SurfaceBlockEntry(id, weight);
    }

    public static RoadStyleConfigEntry.DecorationEntry fence(String blockId) {
        return new RoadStyleConfigEntry.DecorationEntry(RoadDecorationType.FENCE, blockId);
    }
}