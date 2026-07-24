package com.codename1.flutter;

/**
 * A {@link Key} that is equal to another only when they wrap the identical
 * object (reference equality, Dart's {@code identical}) — Flutter's
 * {@code ObjectKey}. The mail preview cards key themselves by their backing
 * email instance so the framework preserves element state as the list reorders.
 */
public class ObjectKey extends Key {

    private final Object value;

    public ObjectKey(Object value) {
        this.value = value;
    }

    public Object value() {
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
        return ((ObjectKey) o).value == this.value;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(value);
    }

    @Override
    public String toString() {
        return "ObjectKey(" + value + ")";
    }
}
