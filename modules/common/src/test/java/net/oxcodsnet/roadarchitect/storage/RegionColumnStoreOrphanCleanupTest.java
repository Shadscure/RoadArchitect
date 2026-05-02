package net.oxcodsnet.roadarchitect.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RegionColumnStoreOrphanCleanupTest {

    @Test
    void cleanOrphanedTempFiles_removes_only_temp_artifacts(@TempDir Path dir) throws IOException {
        Path keep1 = Files.writeString(dir.resolve("region_0_0.nbt"), "real-region");
        Path keep2 = Files.writeString(dir.resolve("region_-3_4.nbt.corrupt-1234567"), "quarantined");
        Path keep3 = Files.writeString(dir.resolve("metadata.json"), "{}");
        Path orphan1 = Files.writeString(dir.resolve("region_0_0.nbt.tmp." + UUID.randomUUID()), "stale-1");
        Path orphan2 = Files.writeString(dir.resolve("region_-2_5.nbt.tmp.another"), "stale-2");

        RegionColumnStore.cleanOrphanedTempFiles(dir);

        assertTrue(Files.exists(keep1), "real region must survive cleanup");
        assertTrue(Files.exists(keep2), "quarantined file must survive cleanup");
        assertTrue(Files.exists(keep3), "unrelated files must survive cleanup");
        assertFalse(Files.exists(orphan1), "uuid-suffixed tmp must be removed");
        assertFalse(Files.exists(orphan2), "any region_*.nbt.tmp.* must be removed");
    }

    @Test
    void cleanOrphanedTempFiles_is_noop_on_missing_directory(@TempDir Path dir) {
        Path missing = dir.resolve("nonexistent-cache");
        assertDoesNotThrow(() -> RegionColumnStore.cleanOrphanedTempFiles(missing));
    }

    @Test
    void cleanOrphanedTempFiles_tolerates_null() {
        assertDoesNotThrow(() -> RegionColumnStore.cleanOrphanedTempFiles(null));
    }

    @Test
    void cleanOrphanedTempFiles_handles_empty_directory(@TempDir Path dir) throws IOException {
        RegionColumnStore.cleanOrphanedTempFiles(dir);
        try (Stream<Path> entries = Files.list(dir)) {
            assertEquals(List.of(), entries.toList());
        }
    }
}
