package com.codename1.flutter;

/**
 * A restorable non-null boolean ({@code RestorableBool} in Flutter). The value
 * lives in a field; setting it notifies listeners. Restoration is not persisted.
 */
public class RestorableBool extends RestorableProperty<Boolean> {

    private boolean current;

    public RestorableBool(boolean defaultValue) {
        this.current = defaultValue;
    }

    public boolean value() {
        return current;
    }

    public void value(boolean v) {
        if (current != v) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public Boolean createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(Boolean value) {
        this.current = value != null && value.booleanValue();
    }
}
