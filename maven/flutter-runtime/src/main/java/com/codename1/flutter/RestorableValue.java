package com.codename1.flutter;

/**
 * A restorable holding a single value with a read/write {@code value} accessor,
 * mirroring Flutter's {@code RestorableValue<T>}. The value is created lazily
 * from {@link #createDefaultValue()} on first access; assigning it notifies
 * listeners.
 *
 * @param <T> the held value type
 */
public class RestorableValue<T> extends RestorableProperty<T> {

    private T current;
    private boolean initialized;

    public T value() {
        if (!initialized) {
            current = createDefaultValue();
            initialized = true;
        }
        return current;
    }

    public void value(T newValue) {
        this.current = newValue;
        this.initialized = true;
        notifyListeners();
    }

    @Override
    public void initWithValue(T value) {
        this.current = value;
        this.initialized = true;
    }
}
