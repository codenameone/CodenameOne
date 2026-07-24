package com.codename1.flutter;

import dart.core.DateTime;

/**
 * A restorable {@code DateTime} — Flutter's {@code RestorableDateTime} (a
 * {@code RestorableValue<DateTime>}). The value is held as a
 * {@link dart.core.DateTime} so callers can read date components (e.g.
 * {@code value.millisecondsSinceEpoch}). Restoration is not persisted.
 */
public class RestorableDateTime extends RestorableProperty<DateTime> {

    private DateTime current;

    public RestorableDateTime(DateTime defaultValue) {
        this.current = defaultValue;
    }

    public DateTime value() {
        return current;
    }

    public void value(DateTime v) {
        if (current == null ? v != null : !current.equals(v)) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public DateTime createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(DateTime value) {
        this.current = value;
    }
}
