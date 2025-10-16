package net.oxcodsnet.roadarchitect.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple, thread-safe, size-limited LRU (Least Recently Used) cache.
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public final class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    /**
     * Constructs a new, empty <tt>LRUCache</tt> with the specified maximum size.
     *
     * @param maxSize the maximum number of entries that the cache can hold.
     */
    public LRUCache(int maxSize) {
        super(maxSize > 0 ? (int) Math.ceil(maxSize / 0.75f) + 1 : 16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    /**
     * Returns a thread-safe view of this LRU cache.
     *
     * @return a synchronized (thread-safe) map backed by this cache.
     */
    public static <K, V> Map<K, V> synchronizedOf(int maxSize) {
        return Collections.synchronizedMap(new LRUCache<>(maxSize));
    }
}
