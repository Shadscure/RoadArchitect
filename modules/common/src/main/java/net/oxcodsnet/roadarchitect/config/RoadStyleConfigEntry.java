package net.oxcodsnet.roadarchitect.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable DTO describing a configured road surface style.
 */
public record RoadStyleConfigEntry(List<String> biomeSelectors,
                                   List<SurfaceBlockEntry> palette,
                                   List<DecorationEntry> decorations) {
    public RoadStyleConfigEntry {
        biomeSelectors = biomeSelectors == null ? List.of() : List.copyOf(biomeSelectors);
        palette = palette == null ? List.of() : sanitizePalette(palette);
        decorations = decorations == null ? List.of() : sanitizeDecorations(decorations);
    }

    private static List<SurfaceBlockEntry> sanitizePalette(List<SurfaceBlockEntry> entries) {
        ArrayList<SurfaceBlockEntry> list = new ArrayList<>();
        for (SurfaceBlockEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            SurfaceBlockEntry normalized = new SurfaceBlockEntry(entry.block(), entry.weight());
            if (normalized.block().isBlank() || normalized.weight() <= 0) {
                continue;
            }
            list.add(normalized);
        }
        return List.copyOf(list);
    }

    private static List<DecorationEntry> sanitizeDecorations(List<DecorationEntry> entries) {
        ArrayList<DecorationEntry> list = new ArrayList<>();
        for (DecorationEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            DecorationEntry normalized = new DecorationEntry(entry.type(), entry.block());
            if (normalized.type().isBlank()) {
                continue;
            }
            list.add(normalized);
        }
        return List.copyOf(list);
    }

    /**
     * Single block selection entry for the surface palette.
     *
     * @param block  Block identifier or tag (prefixed with '#').
     * @param weight Relative weight/chance in the palette.
     */
    public record SurfaceBlockEntry(String block, int weight) {
        public SurfaceBlockEntry {
            block = block == null ? "" : block.trim();
            weight = Math.max(0, weight);
        }
    }

    /**
     * Decoration entry describing side ornaments.
     *
     * @param type  Decoration type identifier (e.g. {@code fence}).
     * @param block Block identifier or tag used by the decoration.
     */
    public record DecorationEntry(String type, String block) {
        public DecorationEntry {
            type = type == null ? "" : type.trim();
            block = block == null ? "" : block.trim();
        }
    }
}
