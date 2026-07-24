package com.codename1.flutter.foundation;

/**
 * The error type the Flutter framework (and app assertions) throw — Flutter's
 * {@code FlutterError}. Modelled as a {@link RuntimeException} so transpiled
 * {@code throw FlutterError(...)} statements compile and propagate like any Dart
 * throw. {@link #reportError(Object)} routes a caught error to the current
 * handler; this pass logs it.
 */
public class FlutterError extends RuntimeException {

    public FlutterError(String message) {
        super(message);
    }

    /** Dart's {@code FlutterError.reportError(details)}. */
    public static void reportError(Object details) {
        if (details != null) {
            System.err.println("FlutterError.reportError: " + details);
        }
    }

    @Override
    public String toString() {
        return "FlutterError: " + getMessage();
    }
}
