package net.oxcodsnet.roadarchitect.util;

import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.config.RoadStyleConfigEntry;
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
}
