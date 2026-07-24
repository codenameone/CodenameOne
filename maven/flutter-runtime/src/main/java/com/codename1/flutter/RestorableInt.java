package com.codename1.flutter;

/**
 * A restorable non-null integer ({@code RestorableInt} in Flutter). Dart
 * {@code int} maps to Java {@code long}. Restoration is not persisted.
 */
public class RestorableInt extends RestorableProperty<Long> {

    private long current;

    public RestorableInt(long defaultValue) {
        this.current = defaultValue;
    }

    public long value() {
        return current;
    }

    public void value(long v) {
        if (current != v) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public Long createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(Long value) {
        this.current = value == null ? 0L : value.longValue();
    }
}
