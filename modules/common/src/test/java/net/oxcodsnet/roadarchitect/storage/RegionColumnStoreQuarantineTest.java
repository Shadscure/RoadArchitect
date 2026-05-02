package net.oxcodsnet.roadarchitect.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RegionColumnStoreQuarantineTest {

    @Test
    void quarantine_renames_file_with_corrupt_suffix(@TempDir Path dir) throws IOException {
        Path region = dir.resolve("region_0_0.nbt");
        Files.write(region, "corrupted-payload".getBytes(StandardCharsets.UTF_8));

        RegionColumnStore.quarantineCorruptedRegion(region);

        assertFalse(Files.exists(region), "original corrupt file must be moved away");
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> survivors = files.toList();
            assertEquals(1, survivors.size(), "expected exactly one quarantined file");
            Path target = survivors.get(0);
            assertTrue(target.getFileName().toString().startsWith("region_0_0.nbt.corrupt-"),
                    "quarantined file must keep the original name and gain a corrupt-{ts} suffix, got: " + target.getFileName());
            assertEquals("corrupted-payload",
                    Files.readString(target, StandardCharsets.UTF_8),
                    "quarantine must preserve the original bytes for offline diagnosis");
        }
    }

    @Test
    void quarantine_is_noop_when_file_does_not_exist(@TempDir Path dir) {
        Path missing = dir.resolve("region_42_-7.nbt");
        assertDoesNotThrow(() -> RegionColumnStore.quarantineCorruptedRegion(missing));
        assertFalse(Files.exists(missing));
    }

    @Test
    void quarantine_tolerates_null(@TempDir Path dir) {
        // Defensive: Caffeine wiring can in principle hand a null path through.
        assertDoesNotThrow(() -> RegionColumnStore.quarantineCorruptedRegion(null));
    }
}
