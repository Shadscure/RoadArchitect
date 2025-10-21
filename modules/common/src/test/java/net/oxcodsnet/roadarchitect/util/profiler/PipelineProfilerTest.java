package net.oxcodsnet.roadarchitect.util.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PipelineProfilerTest {

    @AfterEach
    void tearDown() {
        PipelineProfiler.current().ifPresent(PipelineProfiler::close);
    }

    @Test
    void snapshotContainsRecordedData() {
        try (PipelineProfiler profiler = PipelineProfiler.start("test_trigger", "test_world", BlockPos.ORIGIN)) {
            try (PipelineProfiler.Section ignored = PipelineProfiler.openSection("timing.section")) {
                PipelineProfiler.recordDuration("timing.nested", 2_000_000L);
            }
            PipelineProfiler.recordDuration("timing.manual", 1_000_000L);
            PipelineProfiler.increment("counter.total", 3L);
            PipelineProfiler.increment("counter.total");
            PipelineProfiler.increment("counter.other");
            PipelineProfiler.recordValue("value.length", 4.5);
            PipelineProfiler.recordValue("value.length", 5.5);

            PipelineProfiler.ProfilerReport report = profiler.snapshot();

            assertEquals("test_trigger", report.trigger());
            assertEquals("test_world", report.worldId());
            assertEquals(BlockPos.ORIGIN, report.origin());
            assertFalse(report.timings().isEmpty());
            assertFalse(report.counters().isEmpty());
            assertFalse(report.values().isEmpty());

            PipelineProfiler.TimingEntry manual = report.timings().stream()
                    .filter(entry -> entry.name().equals("timing.manual"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(manual);
            assertEquals(1L, manual.count());
            assertEquals(1_000_000L, manual.totalNanos());

            PipelineProfiler.CounterEntry counter = report.counters().stream()
                    .filter(entry -> entry.name().equals("counter.total"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(counter);
            assertEquals(4L, counter.value());

            PipelineProfiler.ValueEntry valueEntry = report.values().stream()
                    .filter(entry -> entry.name().equals("value.length"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(valueEntry);
            assertEquals(2L, valueEntry.count());
            assertEquals(10.0, valueEntry.sum(), 1e-6);
        }
        assertTrue(PipelineProfiler.current().isEmpty());
    }
}
