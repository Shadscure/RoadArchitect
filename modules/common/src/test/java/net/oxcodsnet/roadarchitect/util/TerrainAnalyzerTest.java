package net.oxcodsnet.roadarchitect.util;

import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainAnalyzerTest {

    @Test
    void heightRange_isZeroOnFlat() {
        IntBinaryOperator flat = (x, z) -> 64;
        int range = TerrainAnalyzer.heightRange(flat, 0, 0, 12, 3);
        assertEquals(0, range);
        assertEquals(0.0, TerrainAnalyzer.roughnessPenalty(range));
    }

    @Test
    void heightRange_detectsStepChange() {
        // Simple “mountain edge”: everything with x >= 0 is 84, otherwise 64
        IntBinaryOperator step = (x, z) -> x >= 0 ? 84 : 64;
        int rangeAtEdge = TerrainAnalyzer.heightRange(step, -2, 0, 12, 3);
        // Expect range ~20 across the window
        assertTrue(rangeAtEdge >= 20);
        double pen = TerrainAnalyzer.roughnessPenalty(rangeAtEdge);
        assertTrue(pen > 0.0);
    }
}

