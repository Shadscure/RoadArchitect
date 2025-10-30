package net.oxcodsnet.roadarchitect.util;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Small helpers for safe NBT reads to reduce boilerplate.
 */
public final class NbtUtils {
    private NbtUtils() {
    }

    /**
     * Reads an enum value from NBT. If the key is missing or the value is invalid,
     * returns the provided default.
     */
    public static <E extends Enum<E>> E getEnumOrDefault(CompoundTag tag, String key, Class<E> type, E def) {
        String raw = tag.getStringOr(key, "");
        try {
            return raw.isEmpty() ? def : Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    /* ===================================================================== */
    /* Primitive array helpers                                               */
    /* ===================================================================== */

    /**
     * Serializes a double array into an {@link ListTag} of {@link DoubleTag}.
     */
    public static ListTag toDoubleList(double[] values) {
        ListTag list = new ListTag();
        if (values == null) return list;
        for (double v : values) list.add(DoubleTag.valueOf(v));
        return list;
    }

    /**
     * Deserializes a list of numbers (double or string) into a double array.
     * Accepts {@link DoubleTag} or {@link net.minecraft.nbt.StringTag} elements.
     */
    public static double[] readDoubleList(ListTag list) {
        if (list == null || list.isEmpty()) return new double[0];
        double[] out = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Tag el = list.get(i);
            if (el instanceof DoubleTag d) {
                out[i] = d.doubleValue();
            } else {
                // Unknown/legacy element type — default to 0.0 (will be recomputed later)
                out[i] = 0.0;
            }
        }
        return out;
    }

    /**
     * Serializes a byte array into an {@link ListTag} of {@link ByteTag}.
     */
    public static ListTag toByteList(byte[] values) {
        ListTag list = new ListTag();
        if (values == null) return list;
        for (byte b : values) list.add(ByteTag.valueOf(b));
        return list;
    }

    /**
     * Deserializes a list into a byte array. Accepts {@link ByteTag} or
     * string-encoded numbers for forward/legacy compatibility.
     */
    public static byte[] readByteList(ListTag list) {
        if (list == null || list.isEmpty()) return new byte[0];
        byte[] out = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Tag el = list.get(i);
            if (el instanceof ByteTag b) {
                out[i] = b.byteValue();
            } else {
                // Unknown/legacy element type — default to 0
                out[i] = 0;
            }
        }
        return out;
    }

    /* ===================================================================== */
    /* Long↔value list helpers (k/v fields)                                   */
    /* ===================================================================== */

    private static final String K = "k";
    private static final String V = "v";

    public static net.minecraft.nbt.ListTag toLongIntList(java.util.Map<Long, Integer> map) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (java.util.Map.Entry<Long, Integer> e : map.entrySet()) {
            net.minecraft.nbt.CompoundTag elem = new net.minecraft.nbt.CompoundTag();
            elem.putLong(K, e.getKey());
            elem.putInt(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongIntMap(net.minecraft.nbt.ListTag list, java.util.Map<Long, Integer> out) {
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag elem = list.getCompoundOrEmpty(i);
            out.put(elem.getLongOr(K, 0L), elem.getIntOr(V, 0));
        }
    }

    public static net.minecraft.nbt.ListTag toLongDoubleList(java.util.Map<Long, Double> map) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (java.util.Map.Entry<Long, Double> e : map.entrySet()) {
            net.minecraft.nbt.CompoundTag elem = new net.minecraft.nbt.CompoundTag();
            elem.putLong(K, e.getKey());
            elem.putDouble(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongDoubleMap(net.minecraft.nbt.ListTag list, java.util.Map<Long, Double> out) {
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag elem = list.getCompoundOrEmpty(i);
            out.put(elem.getLongOr(K, 0L), elem.getDoubleOr(V, 0.0));
        }
    }

    public static net.minecraft.nbt.ListTag toLongStringList(java.util.Map<Long, String> map) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (java.util.Map.Entry<Long, String> e : map.entrySet()) {
            net.minecraft.nbt.CompoundTag elem = new net.minecraft.nbt.CompoundTag();
            elem.putLong(K, e.getKey());
            elem.putString(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongStringMap(net.minecraft.nbt.ListTag list, java.util.Map<Long, String> out) {
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag elem = list.getCompoundOrEmpty(i);
            out.put(elem.getLongOr(K, 0L), elem.getStringOr(V, ""));
        }
    }
}