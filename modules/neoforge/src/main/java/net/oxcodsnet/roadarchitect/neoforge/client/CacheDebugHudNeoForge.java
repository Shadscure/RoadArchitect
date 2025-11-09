package net.oxcodsnet.roadarchitect.neoforge.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugHudEntry;

/**
 * NeoForge-specific bootstrap that waits for the Minecraft client to finish constructing
 * before binding our debug entry into the vanilla {@code F3} overlay registry.
 */
public final class CacheDebugHudNeoForge {
    private static boolean installed;
    private static boolean bound;

    private CacheDebugHudNeoForge() {
    }

    public static void bootstrap() {
        if (installed) {
            return;
        }
        installed = true;
        NeoForge.EVENT_BUS.addListener(CacheDebugHudNeoForge::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (bound) {
            return;
        }
        if (CacheDebugHudEntry.bindToDebugList()) {
            bound = true;
        }
    }
}
