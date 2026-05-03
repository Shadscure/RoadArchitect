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
    // 1.21.1 wires this up via mc.gui.getDebugOverlay().getSystemInformation();
    // 1.20.1 has no Gui#getDebugOverlay accessor — DebugScreenOverlay is created
    // ad-hoc inside Gui.renderHud — so we fall back to a fixed top-right offset.
    // 130 px ≈ 10 lines × ~12 px line height + padding, comfortably clearing the
    // vanilla system info block (Java + Mem + Allocation rate + Allocated +
    // CPU + GPU display + 2-3 OpenGL/extensions lines + version footer). On
    // exotic setups with extra GPU diagnostic lines the user can scroll the
    // overlay down by toggling debugCacheOverlay off/on once a setting changes.
    private static final int FIXED_RIGHT_COLUMN_TOP = 130;

    private CacheDebugOverlayRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }
        if (!RoadArchitect.CONFIG.debugCacheOverlay()) {
            return;
        }
        // mc.options is non-null while a level is loaded; renderDebug is the
        // 1.20.1 flag for the vanilla F3 overlay.
        if (mc.options == null || !mc.options.renderDebug) {
            return;
        }
        List<Component> lines = collectLines(mc);
        if (lines.isEmpty()) {
            return;
        }
        int x = graphics.guiWidth() - 4;
        int y = computeRightColumnTop(mc);
        for (Component line : lines) {
            int width = mc.font.width(line);
            graphics.drawString(mc.font, line, x - width, y, 0xff896c, false);
            y += mc.font.lineHeight;
        }
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
        lines.add(Component.literal("RoadArchitect cache (" + dimensionId + ")"));
        lines.add(Component.literal("  runtime: " + formatUsage(stats.runtimeUsedBytes(), stats.runtimeBudgetBytes())));
        lines.add(Component.literal("  snapshots: " + formatUsage(stats.snapshotUsedBytes(), stats.snapshotBudgetBytes())));
        lines.add(Component.literal("  pages: " + formatUsage(stats.persistedUsedBytes(), stats.persistedBudgetBytes())));
        lines.add(Component.literal("  prefill: " + (stats.prefillEnabled() ? "ON" : "OFF") + " limit=" + stats.prefillMaxChunks()));
        return lines;
    }

    private static int computeRightColumnTop(Minecraft mc) {
        return FIXED_RIGHT_COLUMN_TOP;
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
