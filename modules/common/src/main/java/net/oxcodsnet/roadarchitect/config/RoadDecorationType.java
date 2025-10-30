package net.oxcodsnet.roadarchitect.config;

import net.minecraft.util.StringRepresentable;

/**
 * Supported decoration kinds for configurable road styles.
 */
public enum RoadDecorationType implements StringRepresentable {
    NONE("none"),
    FENCE("fence");

    private final String id;

    RoadDecorationType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public String id() {
        return this.id;
    }
}
