package com.codename1.flutter;

/**
 * A {@link ValueKey} that also identifies a subtree's {@code PageStorage}
 * bucket — Flutter's {@code PageStorageKey}. new_gallery tags each home
 * carousel card with one so its scroll offset is preserved across rebuilds.
 * Equality follows {@link ValueKey}: same runtime class and equal value.
 *
 * @param <T> the wrapped value type
 */
public class PageStorageKey<T> extends ValueKey<T> {

    public PageStorageKey(T value) {
        super(value);
    }
}
