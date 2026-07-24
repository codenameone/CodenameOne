package com.codename1.flutter;

/**
 * A restorable nullable string ({@code RestorableStringN} in Flutter). Restoration
 * is not persisted; the value is held in a field.
 */
public class RestorableStringN extends RestorableProperty<String> {

    private String current;

    public RestorableStringN(String defaultValue) {
        this.current = defaultValue;
    }

    public String value() {
        return current;
    }

    public void value(String v) {
        if (current == null ? v != null : !current.equals(v)) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public String createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(String value) {
        this.current = value;
    }
}
