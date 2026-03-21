package bsh.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal CN1-safe cache implementation used by the embedded BeanShell fork.
 *
 * The original implementation relied on reference queues and concurrent
 * utilities that are not available in Codename One common code. For the
 * playground we only need a small in-memory cache with the same surface API.
 */
public abstract class ReferenceCache<K, V> {
    public enum Type {
        Weak,
        Soft,
        Hard
    }

    private final Map<K, V> cache;

    public ReferenceCache(Type keyType, Type valueType) {
        this(keyType, valueType, 0);
    }

    public ReferenceCache(Type keyType, Type valueType, int initialSize) {
        cache = initialSize > 0 ? new HashMap<K, V>(initialSize) : new HashMap<K, V>();
    }

    protected abstract V create(K key);

    public synchronized V get(K key) {
        if (key == null) {
            return null;
        }
        V value = cache.get(key);
        if (value == null) {
            value = create(key);
            if (value != null) {
                cache.put(key, value);
            }
        }
        return value;
    }

    public synchronized void init(K key) {
        get(key);
    }

    public synchronized boolean remove(K key) {
        return cache.remove(key) != null;
    }

    public synchronized int size() {
        return cache.size();
    }

    public synchronized void clear() {
        cache.clear();
    }
}
