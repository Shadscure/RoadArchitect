package net.oxcodsnet.roadarchitect.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.oxcodsnet.roadarchitect.client.gui.CacheDebugHudEntry;
import net.oxcodsnet.roadarchitect.fabric.client.hook.DebugGraphScreenHook;
import net.oxcodsnet.roadarchitect.fabric.client.hook.LoadingOverlayHook;

/**
 * Клиентская сторона мода.
 * <p>Client side entry point of the mod.</p>
 */
public class RoadArchitectClientFabric implements ClientModInitializer {

    private static KeyMapping debugKey;

    @Override
    public void onInitializeClient() {
        LoadingOverlayHook.init();
        DebugGraphScreenHook.init();
        CacheDebugHudEntry.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> CacheDebugHudEntry.bindToDebugList());
    }
}
