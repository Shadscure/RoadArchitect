package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class DimensionPaths {
    private DimensionPaths() {
    }

    static Path resolveCacheDirectory(ServerLevel world) {
        Path dimRoot = resolveDimensionRoot(world);
        Path cacheDir = dimRoot.resolve("roadarchitect").resolve("cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create cache directory at " + cacheDir, e);
        }
        return cacheDir;
    }

    static Path resolveDimensionRoot(ServerLevel world) {
        Path root = world.getServer().getWorldPath(LevelResource.ROOT);
        ResourceKey<Level> key = world.dimension();
        if (key == Level.OVERWORLD) {
            return root;
        }
        if (key == Level.NETHER) {
            return root.resolve("DIM-1");
        }
        if (key == Level.END) {
            return root.resolve("DIM1");
        }
        ResourceLocation id = key.location();
        return root.resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath());
    }
}
