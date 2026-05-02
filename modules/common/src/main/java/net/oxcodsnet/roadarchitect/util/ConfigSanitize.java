package net.oxcodsnet.roadarchitect.util;

import java.util.List;
import java.util.Objects;

/**
 * Defensive helpers for cleaning user-editable config values before they
 * reach the runtime. Cloth-Config can produce a {@code null}-laden list
 * when a JSON file is partially corrupt (e.g. truncated by a crash mid-write,
 * see PeachyKeen's report on Discord 2025-12-30) — the rest of the mod
 * happily NPEs from there. Filtering once at the bridge keeps the rest of
 * the codebase free of {@code null}/blank guards.
 */
public final class ConfigSanitize {
    private ConfigSanitize() {}

    /**
     * Returns an immutable list with {@code null} and blank entries
     * removed and remaining strings trimmed. {@code null} input maps to
     * an empty list — never propagates upward.
     */
    public static List<String> cleanList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
