package dart.core;

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dart's Map&lt;K,V&gt;: insertion-ordered (Dart map literals preserve
 * insertion order, matching LinkedHashMap). Extends LinkedHashMap so it
 * interoperates with Java/CN1 APIs directly.
 */
public class DartMap<K, V> extends LinkedHashMap<K, V> {

    public DartMap() {
    }

    /** Literal helper for {@code {a: 1, b: 2}}: pairs of key, value. */
    @SuppressWarnings("unchecked")
    public static <K, V> DartMap<K, V> of(Object... pairs) {
        DartMap<K, V> m = new DartMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((K) pairs[i], (V) pairs[i + 1]);
        }
        return m;
    }

    /** Dart's map[key]. */
    public V idx(K key) {
        return get(key);
    }

    /** Dart's map[key] = value. */
    public V idxSet(K key, V value) {
        put(key, value);
        return value;
    }

    public long length() {
        return size();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public DartIterable<K> keys() {
        return DartIterable.wrap(keySet());
    }

    public DartIterable<V> valuesIterable() {
        return DartIterable.wrap(values());
    }

    public V putIfAbsentDart(K key, Funcs.Func0<V> ifAbsent) {
        if (containsKey(key)) {
            return get(key);
        }
        V v = ifAbsent.call();
        put(key, v);
        return v;
    }

    public void forEachDart(Funcs.VoidFunc2<K, V> action) {
        for (Map.Entry<K, V> e : entrySet()) {
            action.call(e.getKey(), e.getValue());
        }
    }

    /** Dart's Map.remove returns the removed value (or null). */
    public V removeDart(Object key) {
        return remove(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<K, V> e : entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(DartRuntime.str(e.getKey())).append(": ").append(DartRuntime.str(e.getValue()));
            first = false;
        }
        return sb.append("}").toString();
    }
}
