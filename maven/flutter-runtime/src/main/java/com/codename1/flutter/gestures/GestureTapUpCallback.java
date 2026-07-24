package com.codename1.flutter.gestures;

/**
 * Signature for a tap-up callback — Flutter's {@code GestureTapUpCallback}
 * ({@code void Function(TapUpDetails)}). A single-abstract-method interface so
 * transpiled Dart closures and method references bind as Java lambdas.
 */
public interface GestureTapUpCallback {
    void call(TapUpDetails details);
}
