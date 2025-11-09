package net.oxcodsnet.roadarchitect.util;

import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;

/**
 * Utility to guard verbose diagnostic logging behind the debug config toggle.
 */
public final class DebugLog {
    private DebugLog() {
    }

    public static boolean isEnabled() {
        return RoadArchitect.CONFIG.debugVerboseLogs();
    }

    public static boolean isCacheEnabled() {
        return RoadArchitect.CONFIG.debugCacheLogs();
    }

    public static void info(Logger logger, String message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        logger.info(message, args);
    }

    public static void cache(Logger logger, String message, Object... args) {
        if (!isCacheEnabled()) {
            return;
        }
        logger.info("[cache] " + message, args);
    }
}
