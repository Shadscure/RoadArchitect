package net.oxcodsnet.roadarchitect.util;

import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Global asynchronous executor backed by a single {@link ForkJoinPool}.
 * Provides helper methods for submitting tasks without creating
 * additional thread pools across the project.
 *
 * <p>The pool is built lazily on first use via the {@link Holder} idiom so
 * that platform code has a chance to publish the configured
 * {@link net.oxcodsnet.roadarchitect.config.RAConfig} before the pool size
 * is sampled. Changing {@code asyncThreads} at runtime therefore requires
 * a game restart — the GUI marks the option {@code @RequiresRestart} so
 * Cloth Config surfaces that to players.
 */
public final class AsyncExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadArchitect/AsyncExecutor");

    private AsyncExecutor() {
    }

    private static final class Holder {
        private static final ForkJoinPool POOL = createPool();

        private static ForkJoinPool createPool() {
            int configured = RAConfigHolder.get().debugAsyncThreads();
            int parallelism;
            if (configured > 0) {
                parallelism = configured;
            } else {
                // auto: leave 2 cores for the OS / MC main thread / vanilla
                // worldgen workers. Previously hardcoded to N-1, which
                // pegged 16-core machines to 100 % under Distant Horizons
                // (see GH #29).
                parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
            }
            LOGGER.info("AsyncExecutor pool initialized with parallelism={} (configured={}, available={})",
                    parallelism, configured, Runtime.getRuntime().availableProcessors());
            return new ForkJoinPool(parallelism);
        }
    }

    /**
     * Submits a value-producing task to the shared pool.
     */
    public static <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, Holder.POOL);
    }

    /**
     * Executes a fire-and-forget task on the shared pool.
     */
    public static void execute(Runnable task) {
        Holder.POOL.execute(task);
    }
}

