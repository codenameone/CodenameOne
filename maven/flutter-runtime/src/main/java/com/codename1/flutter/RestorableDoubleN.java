package com.codename1.flutter;

/**
 * A restorable nullable double ({@code RestorableDoubleN} in Flutter). Restoration
 * is not persisted.
 */
public class RestorableDoubleN extends RestorableProperty<Double> {

    private Double current;

    public RestorableDoubleN(Double defaultValue) {
        this.current = defaultValue;
    }

    public Double value() {
        return current;
    }

    public void value(Double v) {
        if (current == null ? v != null : !current.equals(v)) {
            current = v;
            notifyListeners();
        }
    }

    @Override
    public Double createDefaultValue() {
        return current;
    }

    @Override
    public void initWithValue(Double value) {
        this.current = value;
    }
}
