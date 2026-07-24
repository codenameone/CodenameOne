package dart.core;

import dart.runtime.DartRuntime;

/**
 * Dart's {@code MapEntry<K,V>}: an immutable key/value pair. Produced by
 * {@code Map.entries} and consumed by {@code Map.fromEntries}.
 *
 * <p>Dart exposes {@code key}/{@code value} as getters, so the transpiler
 * emits them as no-arg method calls ({@link #key()} / {@link #value()}).</p>
 */
public final class MapEntry<K, V> {

    private final K key;
    private final V value;

    /** Dart's {@code MapEntry(key, value)}. */
    public MapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    @Override
    public String toString() {
        return "MapEntry(" + DartRuntime.str(key) + ": " + DartRuntime.str(value) + ")";
    }
}
