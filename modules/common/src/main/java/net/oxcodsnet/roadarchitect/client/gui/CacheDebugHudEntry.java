package net.oxcodsnet.roadarchitect.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Debug HUD entry that mirrors the legacy cache overlay within the F3 screen.
 */
public final class CacheDebugHudEntry implements DebugScreenEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/CacheDebugHudEntry");
    private static final ResourceLocation ENTRY_ID = ResourceLocation.fromNamespaceAndPath(
            RoadArchitect.MOD_ID,
            "cache_debug_overlay"
    );
    private static final CacheDebugHudEntry INSTANCE = new CacheDebugHudEntry();
    private static final Method REGISTER_LOCATION = findRegisterMethod(ResourceLocation.class);
    private static final Method REGISTER_STRING = findRegisterMethod(String.class);
    private static volatile boolean registered;
    private static volatile boolean boundToDebugList;

    private CacheDebugHudEntry() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        synchronized (CacheDebugHudEntry.class) {
            if (registered) {
                return;
            }
            if (invokeRegister(REGISTER_LOCATION, ENTRY_ID) || invokeRegister(REGISTER_STRING, ENTRY_ID.toString())) {
                registered = true;
                return;
            }
            LOGGER.warn("Failed to register cache debug HUD entry {}; no compatible API found", ENTRY_ID);
        }
    }

    /**
     * Ensures the new entry is marked as visible in the vanilla debug overlay registry.
     * Mojang stores visibility in {@link net.minecraft.client.gui.components.debug.DebugScreenEntryList},
     * so we flip the toggle there once the Minecraft client is ready.
     *
     * @return {@code true} once the entry is bound and ready, {@code false} if Minecraft
     *         hasn't finished booting yet.
     */
    public static boolean bindToDebugList() {
        if (boundToDebugList) {
            return true;
        }
        if (!registered) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.debugEntries == null) {
            return false;
        }
        DebugScreenEntryStatus status = mc.debugEntries.getStatus(ENTRY_ID);
        if (status != DebugScreenEntryStatus.IN_F3) {
            mc.debugEntries.setStatus(ENTRY_ID, DebugScreenEntryStatus.IN_F3);
        }
        boundToDebugList = true;
        return true;
    }

    private static Method findRegisterMethod(Class<?> identifierType) {
        try {
            Method method = DebugScreenEntries.class.getDeclaredMethod("register", identifierType, DebugScreenEntry.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean invokeRegister(Method method, Object identifier) {
        if (method == null) {
            return false;
        }
        try {
            method.invoke(null, identifier, INSTANCE);
            return true;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IllegalArgumentException) {
                LOGGER.debug("Cache debug HUD entry {} already registered via {}", ENTRY_ID, method.getParameterTypes()[0].getSimpleName());
                return true;
            }
            LOGGER.warn("Cache debug HUD entry registration failed via {}", method.getParameterTypes()[0].getSimpleName(), cause);
            return false;
        } catch (ReflectiveOperationException error) {
            LOGGER.warn("Cache debug HUD entry registration failed via {}", method.getParameterTypes()[0].getSimpleName(), error);
            return false;
        }
    }

    @Override
    public void display(DebugScreenDisplayer displayer, Level level, LevelChunk clientChunk, LevelChunk chunk) {
        Minecraft mc = Minecraft.getInstance();
        List<Component> lines = CacheDebugOverlayRenderer.collectLines(mc);
        if (lines.isEmpty()) {
            return;
        }
        boolean header = true;
        for (Component component : lines) {
            String text = component.getString();
            if (header) {
                displayer.addPriorityLine(text);
                header = false;
            } else {
                displayer.addLine(text);
            }
        }
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return RoadArchitect.CONFIG.debugCacheOverlay();
    }

    @Override
    public DebugEntryCategory category() {
        return DebugEntryCategory.SCREEN_TEXT;
    }
}
