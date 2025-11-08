package net.oxcodsnet.roadarchitect.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.CacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders cache diagnostics into the debug (F3) overlay when enabled.
 */
public final class CacheDebugOverlayRenderer {
    private CacheDebugOverlayRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }
        if (!mc.options.renderDebug || !RoadArchitect.CONFIG.debugCacheOverlay()) {
            return;
        }
        List<Component> lines = collectLines(mc);
        if (lines.isEmpty()) {
            return;
        }
        int x = graphics.guiWidth() - 4;
        int y = 48;
        for (Component line : lines) {
            int width = mc.font.width(line);
            graphics.drawString(mc.font, line, x - width, y, 0x80FFD5, false);
            y += mc.font.lineHeight;
        }
    }

    private static List<Component> collectLines(Minecraft mc) {
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
        lines.add(Component.literal("RoadArchitect cache (" + dimensionId + ")"));
        lines.add(Component.literal("  runtime: " + formatUsage(stats.runtimeUsedBytes(), stats.runtimeBudgetBytes())));
        lines.add(Component.literal("  snapshots: " + formatUsage(stats.snapshotUsedBytes(), stats.snapshotBudgetBytes())));
        lines.add(Component.literal("  pages: " + formatUsage(stats.persistedUsedBytes(), stats.persistedBudgetBytes())));
        lines.add(Component.literal("  prefill: " + (stats.prefillEnabled() ? "ON" : "OFF")
                + " limit=" + stats.prefillMaxChunks()));
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
}
