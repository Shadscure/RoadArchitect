package net.oxcodsnet.roadarchitect.util.profiler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight profiler for the road generation pipeline.
 * Collects timing samples and counters across multiple cooperating classes.
 */
public final class PipelineProfiler implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RoadArchitect.MOD_ID + "/" + PipelineProfiler.class.getSimpleName()
    );

    private static final AtomicReference<PipelineProfiler> ACTIVE = new AtomicReference<>();

    private final String trigger;
    private final String worldId;
    private final BlockPos origin;
    private final long startedNanos;
    private final Instant startedWallClock;
    private final ConcurrentMap<String, TimingStat> timings = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ValueStat> values = new ConcurrentHashMap<>();

    private PipelineProfiler(String trigger, String worldId, BlockPos origin) {
        this.trigger = trigger;
        this.worldId = worldId;
        this.origin = origin == null ? null : origin.toImmutable();
        this.startedNanos = System.nanoTime();
        this.startedWallClock = Instant.now();
    }

    /**
     * Begins a new profiler session, replacing any previous active session.
     */
    public static PipelineProfiler start(String trigger, String worldId, BlockPos origin) {
        PipelineProfiler profiler = new PipelineProfiler(trigger, worldId, origin);
        PipelineProfiler previous = ACTIVE.getAndSet(profiler);
        if (previous != null) {
            LOGGER.warn("Previous profiler session {} -> {} leaked, overriding", previous.trigger, trigger);
        }
        return profiler;
    }

    /**
     * @return current profiler session, if any.
     */
    public static Optional<PipelineProfiler> current() {
        return Optional.ofNullable(ACTIVE.get());
    }

    /**
     * Opens a timed section on the active profiler. No-op if the profiler is disabled.
     */
    public static Section openSection(String name) {
        PipelineProfiler profiler = ACTIVE.get();
        return profiler == null ? Section.NOOP : profiler.section(name);
    }

    /**
     * Increments a counter on the active profiler.
     */
    public static void increment(String name) {
        increment(name, 1L);
    }

    /**
     * Adds {@code delta} to a counter on the active profiler.
     */
    public static void increment(String name, long delta) {
        PipelineProfiler profiler = ACTIVE.get();
        if (profiler == null) {
            return;
        }
        profiler.addCounter(name, delta);
    }

    /**
     * Records a numeric value sample on the active profiler.
     */
    public static void recordValue(String name, double value) {
        PipelineProfiler profiler = ACTIVE.get();
        if (profiler == null) {
            return;
        }
        profiler.addValue(name, value);
    }

    /**
     * Records an explicit duration in nanoseconds on the active profiler.
     */
    public static void recordDuration(String name, long nanos) {
        PipelineProfiler profiler = ACTIVE.get();
        if (profiler == null) {
            return;
        }
        profiler.addTiming(name, nanos);
    }

    public Section section(String name) {
        return new Section(this, name);
    }

    public void addCounter(String name, long delta) {
        counters.computeIfAbsent(name, key -> new LongAdder()).add(delta);
    }

    public void addValue(String name, double value) {
        values.computeIfAbsent(name, key -> new ValueStat()).add(value);
    }

    public void addTiming(String name, long nanos) {
        timings.computeIfAbsent(name, key -> new TimingStat()).add(nanos);
    }

    /**
     * Creates a snapshot of the profiler state without closing it.
     */
    public ProfilerReport snapshot() {
        long now = System.nanoTime();
        return snapshot(now - startedNanos);
    }

    private ProfilerReport snapshot(long totalNanos) {
        List<TimingEntry> timingEntries = timings.entrySet().stream()
                .map(e -> new TimingEntry(e.getKey(), e.getValue().count.longValue(), e.getValue().total.longValue(),
                        e.getValue().max.longValue()))
                .sorted(Comparator.comparingLong(TimingEntry::totalNanos).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        List<CounterEntry> counterEntries = counters.entrySet().stream()
                .map(e -> new CounterEntry(e.getKey(), e.getValue().longValue()))
                .sorted(Comparator.comparing(CounterEntry::name))
                .collect(Collectors.toCollection(ArrayList::new));

        List<ValueEntry> valueEntries = values.entrySet().stream()
                .map(e -> new ValueEntry(
                        e.getKey(),
                        e.getValue().count.longValue(),
                        e.getValue().sum.doubleValue(),
                        e.getValue().min.get(),
                        e.getValue().max.get()))
                .sorted(Comparator.comparing(ValueEntry::name))
                .collect(Collectors.toCollection(ArrayList::new));

        return new ProfilerReport(trigger, worldId, origin, startedWallClock, totalNanos,
                Collections.unmodifiableList(timingEntries),
                Collections.unmodifiableList(counterEntries),
                Collections.unmodifiableList(valueEntries));
    }

    @Override
    public void close() {
        long totalNanos = System.nanoTime() - startedNanos;
        ACTIVE.compareAndSet(this, null);
        ProfilerReport report = snapshot(totalNanos);
        logReport(report);
    }

    private void logReport(ProfilerReport report) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Pipeline profiler summary [trigger=").append(report.trigger())
                .append(", world=").append(report.worldId());
        if (report.origin() != null) {
            sb.append(", origin=").append(report.origin());
        }
        sb.append(", wallClock=").append(report.startedWallClock()).append("]\n");
        sb.append("  Total: ").append(formatMillis(report.totalNanos())).append(" ms\n");

        if (!report.timings().isEmpty()) {
            sb.append("  Timings:\n");
            for (TimingEntry timing : report.timings()) {
                double avgMs = timing.count() == 0
                        ? 0.0
                        : (double) timing.totalNanos() / timing.count() / 1_000_000.0;
                sb.append("    - ").append(timing.name())
                        .append(": total=").append(formatMillis(timing.totalNanos()))
                        .append(" ms, avg=")
                        .append(String.format(Locale.ROOT, "%.3f", avgMs))
                        .append(" ms, max=").append(formatMillis(timing.maxNanos()))
                        .append(" ms (samples=").append(timing.count()).append(")\n");
            }
        }

        if (!report.counters().isEmpty()) {
            sb.append("  Counters:\n");
            for (CounterEntry counter : report.counters()) {
                sb.append("    - ").append(counter.name())
                        .append(": ").append(counter.value()).append('\n');
            }
        }

        if (!report.values().isEmpty()) {
            sb.append("  Values:\n");
            for (ValueEntry value : report.values()) {
                double avg = value.count() == 0 ? 0.0 : value.sum() / value.count();
                sb.append("    - ").append(value.name())
                        .append(": avg=").append(String.format(Locale.ROOT, "%.3f", avg))
                        .append(", min=").append(formatDouble(value.min()))
                        .append(", max=").append(formatDouble(value.max()))
                        .append(", samples=").append(value.count()).append('\n');
            }
        }

        LOGGER.info(sb.toString());
    }

    private static String formatMillis(long nanos) {
        double ms = nanos / 1_000_000.0;
        return String.format(Locale.ROOT, "%.3f", ms);
    }

    private static String formatDouble(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static final class TimingStat {
        private final LongAdder total = new LongAdder();
        private final LongAdder count = new LongAdder();
        private final LongAccumulator max = new LongAccumulator(Long::max, 0L);

        void add(long nanos) {
            total.add(nanos);
            count.increment();
            max.accumulate(nanos);
        }
    }

    private static final class ValueStat {
        private final DoubleAdder sum = new DoubleAdder();
        private final LongAdder count = new LongAdder();
        private final DoubleAccumulator min = new DoubleAccumulator(Math::min, Double.POSITIVE_INFINITY);
        private final DoubleAccumulator max = new DoubleAccumulator(Math::max, Double.NEGATIVE_INFINITY);

        void add(double value) {
            sum.add(value);
            count.increment();
            min.accumulate(value);
            max.accumulate(value);
        }
    }

    /**
     * AutoCloseable timing section.
     */
    public static final class Section implements AutoCloseable {
        private static final Section NOOP = new Section();

        private final PipelineProfiler owner;
        private final String name;
        private final long started;

        private Section() {
            this.owner = null;
            this.name = "noop";
            this.started = 0L;
        }

        private Section(PipelineProfiler owner, String name) {
            this.owner = owner;
            this.name = name;
            this.started = System.nanoTime();
        }

        @Override
        public void close() {
            if (owner == null) {
                return;
            }
            long duration = System.nanoTime() - started;
            owner.addTiming(name, duration);
        }
    }

    /**
     * Immutable snapshot of profiler results.
     */
    public record ProfilerReport(
            String trigger,
            String worldId,
            BlockPos origin,
            Instant startedWallClock,
            long totalNanos,
            List<TimingEntry> timings,
            List<CounterEntry> counters,
            List<ValueEntry> values
    ) {}

    public record TimingEntry(String name, long count, long totalNanos, long maxNanos) {}

    public record CounterEntry(String name, long value) {}

    public record ValueEntry(String name, long count, double sum, double min, double max) {}
}
