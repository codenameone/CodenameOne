package com.codename1.flutter.widgets;

/**
 * Persists a form field's value when the form is saved — Flutter's
 * {@code FormFieldSetter<T>} typedef ({@code void Function(T? newValue)}). A
 * single-abstract-method interface so transpiled Dart closures and method
 * references bind as Java lambdas.
 *
 * @param <T> the field's value type
 */
public interface FormFieldSetter<T> {
    void call(T value);
}
