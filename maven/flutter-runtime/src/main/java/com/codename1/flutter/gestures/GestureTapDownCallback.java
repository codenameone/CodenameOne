package com.codename1.flutter.gestures;

/**
 * Signature for a tap-down callback — Flutter's {@code GestureTapDownCallback}
 * ({@code void Function(TapDownDetails)}). A single-abstract-method interface so
 * transpiled Dart closures and method references bind as Java lambdas.
 */
public interface GestureTapDownCallback {
    void call(TapDownDetails details);
}
