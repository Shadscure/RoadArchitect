package net.oxcodsnet.roadarchitect.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.CacheManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Collects cache diagnostics for the debug (F3) overlay when enabled.
 */
public final class CacheDebugOverlayRenderer {
    private static final int TEXT_COLOR = 0xFFFF896C;

    private CacheDebugOverlayRenderer() {
    }

    public static List<Component> collectLines(Minecraft mc) {
        if (mc == null || mc.level == null) {
            return List.of();
        }
        if (!RoadArchitect.CONFIG.debugCacheOverlay()) {
            return List.of();
        }
        if (mc.getSingleplayerServer() == null) {
            return List.of(Component.literal("RoadArchitect cache: remote server"));
        }
        ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
        if (serverLevel == null) {
            return List.of(Component.literal("RoadArchitect cache: world unavailable"));
        }
        CacheManager.CacheStats stats = CacheManager.stats(serverLevel);
        if (!stats.available()) {
            return List.of(Component.literal("RoadArchitect cache: inactive"));
        }
        ArrayList<Component> lines = new ArrayList<>();
        ResourceLocation dimensionId = serverLevel.dimension().location();
        lines.add(colored("RoadArchitect cache (" + dimensionId + ")"));
        lines.add(colored("  runtime: " + formatUsage(stats.runtimeUsedBytes(), stats.runtimeBudgetBytes())));
        lines.add(colored("  snapshots: " + formatUsage(stats.snapshotUsedBytes(), stats.snapshotBudgetBytes())));
        lines.add(colored("  pages: " + formatUsage(stats.persistedUsedBytes(), stats.persistedBudgetBytes())));
        lines.add(colored("  prefill: " + (stats.prefillEnabled() ? "ON" : "OFF") + " limit=" + stats.prefillMaxChunks()));
        return lines;
    }

    private static String formatUsage(long usedBytes, long budgetBytes) {
        double used = bytesToMiB(usedBytes);
        double budget = bytesToMiB(budgetBytes);
        double pct = budgetBytes > 0 ? (double) usedBytes / (double) budgetBytes * 100.0 : 0.0;
        return String.format(Locale.ROOT, "%.1f / %.1f MiB (%.0f%%)", used, budget, pct);
    }

    private static double bytesToMiB(long value) {
        return value / 1024.0 / 1024.0;
    }

    private static Component colored(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(TEXT_COLOR));
    }
}
