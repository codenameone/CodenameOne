package com.codename1.flutter;

/**
 * A restorable non-null double ({@code RestorableDouble} in Flutter). Restoration
 * is not persisted.
 */
public class RestorableDouble extends RestorableProperty<Double> {

    private double current;

    public RestorableDouble(double defaultValue) {
        this.current = defaultValue;
    }

    public double value() {
        return current;
    }

    public void value(double v) {
        if (current != v) {
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
        this.current = value == null ? 0.0 : value.doubleValue();
    }
}
