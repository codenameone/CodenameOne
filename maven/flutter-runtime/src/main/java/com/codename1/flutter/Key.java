package com.codename1.flutter;

/**
 * Base class for widget keys. Keys control how the element reconciler matches
 * widgets across rebuilds: two widgets can only update the same element when
 * their runtime class matches and their keys are equal.
 */
public class Key {

    private final Object value;

    protected Key() {
        this.value = null;
    }

    /**
     * Dart's {@code Key(String value)} is a factory returning a value key; model it
     * as a concrete key carrying the value so {@code new Key(...)} instantiates.
     */
    public Key(Object value) {
        this.value = value;
    }

    public Object keyValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Key) || o.getClass() != getClass()) {
            return false;
        }
        Object ov = ((Key) o).value;
        return value == null ? ov == null : value.equals(ov);
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }
}
