package com.codename1.flutter;

import com.codename1.flutter.material.TimeOfDay;

/**
 * A restorable {@link TimeOfDay} property ({@code RestorableTimeOfDay} in
 * Flutter) — new_gallery's picker demo holds the selected time in one. The value
 * lives in a field; setting it notifies listeners. Restoration is not persisted.
 */
public class RestorableTimeOfDay extends RestorableProperty<TimeOfDay> {

    private TimeOfDay current;

    public RestorableTimeOfDay(TimeOfDay defaultValue) {
        this.current = defaultValue;
    }

    public TimeOfDay value() {
        return current;
    }

    public void value(TimeOfDay v) {
        boolean changed = current == null ? v != null : !current.equals(v);
        if (changed) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public TimeOfDay createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(TimeOfDay value) {
        this.current = value;
    }
}
