package net.oxcodsnet.roadarchitect.util;

import java.util.Collection;

/**
 * Tiny Levenshtein-distance helpers used to suggest the nearest valid
 * value when a user-provided string (config selector, tag id, …) does
 * not match anything in the registry.
 *
 * <p>The implementation is intentionally dependency-free and small:
 * config-time validation runs once per selector and cache misses are
 * rare, so the {@code O(n·m)} dynamic-programming variant is good
 * enough — no need to pull in commons-text or similar.
 */
public final class StringSimilarity {
    private StringSimilarity() {}

    /**
     * Edit distance between {@code a} and {@code b}.
     */
    public static int levenshtein(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("inputs must be non-null");
        }
        if (a.equals(b)) return 0;
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            char ai = a.charAt(i - 1);
            for (int j = 1; j <= b.length(); j++) {
                int cost = ai == b.charAt(j - 1) ? 0 : 1;
                int del = prev[j] + 1;
                int ins = curr[j - 1] + 1;
                int sub = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(del, ins), sub);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    /**
     * Returns the single best match in {@code candidates} for {@code input},
     * or {@code null} when nothing is within {@code maxDistance} edits.
     */
    public static String bestMatch(String input, Collection<String> candidates, int maxDistance) {
        if (input == null || candidates == null || candidates.isEmpty() || maxDistance < 0) {
            return null;
        }
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            if (candidate == null) continue;
            int d = levenshtein(input, candidate);
            if (d < bestDist) {
                bestDist = d;
                best = candidate;
                if (d == 0) break;
            }
        }
        return bestDist <= maxDistance ? best : null;
    }

    /**
     * Suggestion threshold tuned for IDs/tag paths: 2 edits for short
     * strings, ~25% of the length for longer ones — catches typos and
     * one-letter mix-ups without yielding wild guesses.
     */
    public static int defaultThresholdFor(String input) {
        if (input == null || input.isEmpty()) return 0;
        return Math.max(2, input.length() / 4);
    }
}
