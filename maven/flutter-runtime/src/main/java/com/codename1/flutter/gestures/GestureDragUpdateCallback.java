package com.codename1.flutter.gestures;

/**
 * Signature for a drag-update callback — Flutter's {@code GestureDragUpdateCallback}
 * ({@code void Function(DragUpdateDetails)}). A single-abstract-method interface so
 * transpiled Dart closures and method references bind as Java lambdas.
 */
public interface GestureDragUpdateCallback {
    void call(DragUpdateDetails details);
}
