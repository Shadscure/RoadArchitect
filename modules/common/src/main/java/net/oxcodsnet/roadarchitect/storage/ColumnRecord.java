package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.Objects;

/**
 * Immutable column cache entry that may contain a subset of cached values.
 */
final class ColumnRecord {
    static final ColumnRecord EMPTY = new ColumnRecord(null, null, null);

    private final Integer height;
    private final Double stability;
    private final Holder<Biome> biome;

    ColumnRecord(Integer height, Double stability, Holder<Biome> biome) {
        this.height = height;
        this.stability = stability;
        this.biome = biome;
    }

    Integer height() {
        return height;
    }

    boolean hasHeight() {
        return height != null;
    }

    Double stability() {
        return stability;
    }

    boolean hasStability() {
        return stability != null;
    }

    Holder<Biome> biome() {
        return biome;
    }

    boolean hasBiome() {
        return biome != null;
    }

    ColumnRecord withHeight(int value) {
        if (Objects.equals(height, value)) {
            return this;
        }
        return new ColumnRecord(value, stability, biome);
    }

    ColumnRecord withStability(double value) {
        if (Objects.equals(stability, value)) {
            return this;
        }
        return new ColumnRecord(height, value, biome);
    }

    ColumnRecord withBiome(Holder<Biome> value) {
        if (Objects.equals(biome, value)) {
            return this;
        }
        return new ColumnRecord(height, stability, value);
    }

    boolean isEmpty() {
        return height == null && stability == null && biome == null;
    }

    int weightBytes() {
        int weight = 16; // base object/overhead estimate
        if (height != null) {
            weight += 8;
        }
        if (stability != null) {
            weight += 16;
        }
        if (biome != null) {
            weight += 48;
        }
        return weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ColumnRecord other)) return false;
        return Objects.equals(height, other.height)
                && Objects.equals(stability, other.stability)
                && Objects.equals(biome, other.biome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, stability, biome);
    }
}
