package com.codename1.flutter;

/**
 * A restorable nullable boolean ({@code RestorableBoolN} in Flutter). Restoration
 * is not persisted; the value is held in a field.
 */
public class RestorableBoolN extends RestorableProperty<Boolean> {

    private Boolean current;

    public RestorableBoolN(Boolean defaultValue) {
        this.current = defaultValue;
    }

    public Boolean value() {
        return current;
    }

    public void value(Boolean v) {
        if (current == null ? v != null : !current.equals(v)) {
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
        this.current = value;
    }
}
