package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.api.storage.PersistentStore;
import net.oxcodsnet.roadarchitect.api.core.CoreApi;
import net.oxcodsnet.roadarchitect.api.core.CoreApiImpl;
import net.oxcodsnet.roadarchitect.api.storage.AddonPersistentStorage;
import net.oxcodsnet.roadarchitect.util.DebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Core addon API entry and trigger service facade exposed to other mods.
 */
public final class RoadAddons {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/addons");

    private static final List<RoadAddon> ADDONS = new ArrayList<>();

    private RoadAddons() {}

    /**
     * Registers an addon implementation. Call during your mod init.
     */
    public static void registerAddon(RoadAddon addon) {
        try {
            ADDONS.add(addon);
            addon.onRegister(new ContextImpl(addon.id()));
            DebugLog.info(LOGGER, "Registered addon {}", addon.id());
        } catch (Throwable t) {
            LOGGER.error("Addon registration failed: {}", addon.id(), t);
        }
    }

    // ===== Pipeline hooks (invoked by common when path becomes READY) =====

    public static void onPathReady(ServerLevel world, String pathKey, List<BlockPos> refinedPath) {
        for (RoadAddon addon : ADDONS) {
            try {
                addon.onPathReady(world, pathKey, refinedPath);
            } catch (Throwable t) {
                LOGGER.error("Addon {} onPathReady failure", addon.id(), t);
            }
        }
    }

    // ===== Runtime hooks driven by platform event bridges =====

    /**
     * Called from platform events each server tick. Forwards to addons.
     */
    public static void onServerTick(MinecraftServer server) {
        for (RoadAddon addon : ADDONS) {
            try {
                addon.onServerTick(server);
            } catch (Throwable t) {
                LOGGER.error("Addon {} onServerTick failure", addon.id(), t);
            }
        }
    }

    /**
     * Called from platform events on chunk load. Forwards to addons.
     */
    public static void onChunkLoad(ServerLevel world, ChunkPos pos) {
        for (RoadAddon addon : ADDONS) {
            try {
                addon.onChunkLoad(world, pos);
            } catch (Throwable t) {
                LOGGER.error("Addon {} onChunkLoad failure at {}", addon.id(), pos, t);
            }
        }
    }

    /**
     * Internal bootstrap for built-in addons. Call from loader init once.
     */
    public static void initBuiltins() {
        // no built-ins; external addons should register themselves
    }

    // ===== Addon-scoped persistent storage =====

    /**
     * Returns the persistent store for the given addon in the given world.
     */
    public static PersistentStore persistent(ResourceLocation addonId, ServerLevel world) {
        AddonPersistentStorage state = AddonPersistentStorage.get(world, addonId);
        return new StoreView(state);
    }

    private record ContextImpl(ResourceLocation addonId) implements AddonContext {
        private static Logger makeLogger(ResourceLocation id) {
            return LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/addons/" + id);
        }

        @Override
        public PersistentStore persistent(ServerLevel world) {
            return RoadAddons.persistent(addonId, world);
        }

        @Override
        public Logger logger() {
            return makeLogger(addonId);
        }

        @Override
        public CoreApi core() {
            return CoreApiImpl.INSTANCE;
        }
    }

    private static final class StoreView implements PersistentStore {
        private final AddonPersistentStorage state;

        private StoreView(AddonPersistentStorage state) {
            this.state = state;
        }

        @Override
        public Optional<CompoundTag> get(ResourceLocation key) {
            return state.get(key);
        }

        @Override
        public void put(ResourceLocation key, CompoundTag value) {
            state.put(key, value);
            state.setDirty();
        }

        @Override
        public void remove(ResourceLocation key) {
            state.remove(key);
            state.setDirty();
        }

        @Override
        public Set<ResourceLocation> keys() {
            return state.keys();
        }
    }
}
