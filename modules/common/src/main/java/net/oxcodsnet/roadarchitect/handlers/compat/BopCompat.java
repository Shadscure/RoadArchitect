package net.oxcodsnet.roadarchitect.handlers.compat;

/**
 * Shared Biomes O' Plenty compatibility flags.
 */
public final class BopCompat {
    public static final String MOD_ID = "biomesoplenty";

    private static volatile boolean present;

    private BopCompat() {
    }

    public static void setPresent(boolean value) {
        present = value;
    }

    public static boolean isPresent() {
        return present;
    }
}

