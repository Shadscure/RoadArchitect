package net.oxcodsnet.roadarchitect.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSanitizeTest {

    @Test
    void cleanList_returns_empty_for_null_input() {
        assertEquals(List.of(), ConfigSanitize.cleanList(null));
    }

    @Test
    void cleanList_returns_empty_for_empty_input() {
        assertEquals(List.of(), ConfigSanitize.cleanList(List.of()));
    }

    @Test
    void cleanList_drops_null_entries() {
        List<String> input = Arrays.asList("a", null, "b", null);
        assertEquals(List.of("a", "b"), ConfigSanitize.cleanList(input));
    }

    @Test
    void cleanList_drops_blank_entries_and_trims() {
        List<String> input = Arrays.asList("  minecraft:village  ", "", "   ", "\tmostructures:tavern_1\n");
        assertEquals(List.of("minecraft:village", "mostructures:tavern_1"), ConfigSanitize.cleanList(input));
    }

    @Test
    void cleanList_preserves_order_and_duplicates() {
        List<String> input = Arrays.asList("x", "y", "x");
        assertEquals(List.of("x", "y", "x"), ConfigSanitize.cleanList(input));
    }
}
