package net.oxcodsnet.roadarchitect.config;

import net.minecraft.util.StringIdentifiable;

/**
 * Supported decoration kinds for configurable road styles.
 */
public enum RoadDecorationType implements StringIdentifiable {
    NONE("none"),
    FENCE("fence");

    private final String id;

    RoadDecorationType(String id) {
        this.id = id;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public String id() {
        return this.id;
    }
}
