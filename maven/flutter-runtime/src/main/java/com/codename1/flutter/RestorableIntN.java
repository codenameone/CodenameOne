package com.codename1.flutter;

/**
 * A restorable nullable integer ({@code RestorableIntN} in Flutter). Restoration
 * is not persisted; the value is held in a field.
 */
public class RestorableIntN extends RestorableProperty<Long> {

    private Long current;

    public RestorableIntN(Long defaultValue) {
        this.current = defaultValue;
    }

    public Long value() {
        return current;
    }

    public void value(Long v) {
        if (current == null ? v != null : !current.equals(v)) {
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
        this.current = value;
    }
}
