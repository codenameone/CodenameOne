package com.codename1.flutter.gestures;

/**
 * Signature for a drag-end callback — Flutter's {@code GestureDragEndCallback}
 * ({@code void Function(DragEndDetails)}). A single-abstract-method interface so
 * transpiled Dart closures and method references bind as Java lambdas.
 */
public interface GestureDragEndCallback {
    void call(DragEndDetails details);
}
