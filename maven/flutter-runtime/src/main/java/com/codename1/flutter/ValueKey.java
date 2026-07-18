package com.codename1.flutter;

/**
 * A key that uses value equality of the wrapped value, mirroring Flutter's
 * {@code ValueKey<T>}. Two ValueKeys are equal when they have the same
 * runtime class and equal values.
 */
public class ValueKey<T> extends Key {
    private final T value;

    public ValueKey(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        ValueKey<?> other = (ValueKey<?>) o;
        return value == other.value || (value != null && value.equals(other.value));
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public String toString() {
        return "ValueKey(" + value + ")";
    }
}
