package net.oxcodsnet.roadarchitect.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringSimilarityTest {

    @Test
    void levenshtein_zero_for_equal_strings() {
        assertEquals(0, StringSimilarity.levenshtein("minecraft:village", "minecraft:village"));
    }

    @Test
    void levenshtein_one_for_single_typo() {
        assertEquals(1, StringSimilarity.levenshtein("village", "villag"));
        assertEquals(1, StringSimilarity.levenshtein("village", "villag3"));
        assertEquals(1, StringSimilarity.levenshtein("village", "villlage"));
    }

    @Test
    void levenshtein_handles_empty_strings() {
        assertEquals(0, StringSimilarity.levenshtein("", ""));
        assertEquals(5, StringSimilarity.levenshtein("", "abcde"));
        assertEquals(5, StringSimilarity.levenshtein("abcde", ""));
    }

    @Test
    void bestMatch_returns_closest_candidate_within_threshold() {
        List<String> candidates = List.of(
                "minecraft:village",
                "minecraft:pillager_outpost",
                "mostructures:tavern_1"
        );
        assertEquals("minecraft:village", StringSimilarity.bestMatch("minecraft:vilage", candidates, 2));
        assertEquals("mostructures:tavern_1", StringSimilarity.bestMatch("mostructures:tavern1", candidates, 2));
    }

    @Test
    void bestMatch_returns_null_when_nothing_close_enough() {
        List<String> candidates = List.of("minecraft:village", "minecraft:fortress");
        assertNull(StringSimilarity.bestMatch("totally:unrelated", candidates, 2));
    }

    @Test
    void bestMatch_tolerates_null_inputs() {
        assertNull(StringSimilarity.bestMatch(null, List.of("a"), 2));
        assertNull(StringSimilarity.bestMatch("a", null, 2));
        assertNull(StringSimilarity.bestMatch("a", List.of(), 2));
    }

    @Test
    void bestMatch_skips_null_candidates() {
        List<String> candidates = java.util.Arrays.asList(null, "minecraft:village", null);
        assertEquals("minecraft:village", StringSimilarity.bestMatch("minecraft:vilage", candidates, 2));
    }

    @Test
    void defaultThresholdFor_scales_with_length() {
        assertEquals(2, StringSimilarity.defaultThresholdFor("village"));
        assertEquals(2, StringSimilarity.defaultThresholdFor("a"));
        assertTrue(StringSimilarity.defaultThresholdFor("minecraft:pillager_outpost") >= 2);
    }
}
