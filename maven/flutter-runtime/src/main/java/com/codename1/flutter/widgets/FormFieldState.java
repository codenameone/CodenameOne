package com.codename1.flutter.widgets;

/**
 * The state of a single {@link FormField} — Flutter's {@code FormFieldState<T>}.
 * Reached through a {@code GlobalKey<FormFieldState<T>>().currentState}; the
 * text-field demo reads/writes {@link #value()} and drives
 * didChange/validate/save/reset.
 *
 * @param <T> the field's value type
 */
public class FormFieldState<T> {

    private T value;
    private String errorText;

    public T value() {
        return value;
    }

    public boolean hasError() {
        return errorText != null;
    }

    public boolean isValid() {
        return errorText == null;
    }

    public String errorText() {
        return errorText;
    }

    public void didChange(T value) {
        this.value = value;
    }

    public boolean validate() {
        return errorText == null;
    }

    public void save() {
    }

    public void reset() {
        this.value = null;
        this.errorText = null;
    }
}
