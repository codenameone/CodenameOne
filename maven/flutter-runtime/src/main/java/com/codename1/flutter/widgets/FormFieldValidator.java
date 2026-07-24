package com.codename1.flutter.widgets;

/**
 * Validates a form field's value, returning an error message or {@code null}
 * when valid — Flutter's {@code FormFieldValidator<T>} typedef
 * ({@code String? Function(T? value)}). A single-abstract-method interface so
 * transpiled Dart closures and method references bind as Java lambdas.
 *
 * @param <T> the field's value type
 */
public interface FormFieldValidator<T> {
    String call(T value);
}
