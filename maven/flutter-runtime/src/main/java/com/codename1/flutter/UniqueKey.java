package com.codename1.flutter;

/**
 * A key that is unique across the entire application — Flutter's
 * {@code UniqueKey}. It is never equal to any other key (identity equality is
 * intentional: {@link Object}'s default {@code equals}/{@code hashCode}), so a
 * widget carrying a UniqueKey always forces the framework to inflate a fresh
 * element rather than update an existing one.
 */
public class UniqueKey extends Key {

    public UniqueKey() {
    }

    @Override
    public String toString() {
        return "UniqueKey#" + Integer.toHexString(System.identityHashCode(this));
    }
}
