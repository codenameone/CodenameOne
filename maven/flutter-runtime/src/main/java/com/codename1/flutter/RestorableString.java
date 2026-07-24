package com.codename1.flutter;

/**
 * A restorable non-null string ({@code RestorableString} in Flutter). Restoration
 * is not persisted.
 */
public class RestorableString extends RestorableProperty<String> {

    private String current;

    public RestorableString(String defaultValue) {
        this.current = defaultValue == null ? "" : defaultValue;
    }

    public String value() {
        return current;
    }

    public void value(String v) {
        String nv = v == null ? "" : v;
        if (!current.equals(nv)) {
            current = nv;
            notifyListeners();
        }
    }

    @Override
    public String createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(String value) {
        this.current = value == null ? "" : value;
    }
}
