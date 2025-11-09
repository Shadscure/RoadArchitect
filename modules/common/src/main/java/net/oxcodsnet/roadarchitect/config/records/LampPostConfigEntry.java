package net.oxcodsnet.roadarchitect.config.records;

import java.util.List;

/**
 * Immutable DTO describing a lamp post override defined in the config.
 */
public record LampPostConfigEntry(List<String> biomeSelectors, String baseBlock, String postBlock, String lampBlock) {
    public LampPostConfigEntry {
        biomeSelectors = biomeSelectors == null ? List.of() : List.copyOf(biomeSelectors);
        baseBlock = baseBlock == null ? "" : baseBlock;
        postBlock = postBlock == null ? "" : postBlock;
        lampBlock = lampBlock == null ? "" : lampBlock;
    }
}
