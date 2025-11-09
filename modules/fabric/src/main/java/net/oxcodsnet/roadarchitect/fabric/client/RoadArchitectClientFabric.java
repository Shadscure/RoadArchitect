package net.oxcodsnet.roadarchitect.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.oxcodsnet.roadarchitect.fabric.client.hook.CacheDebugOverlayHook;
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
        CacheDebugOverlayHook.init();
    }
}
