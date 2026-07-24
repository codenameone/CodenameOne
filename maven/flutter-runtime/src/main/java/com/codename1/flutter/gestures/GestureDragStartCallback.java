package com.codename1.flutter.gestures;

/**
 * Signature for a drag-start callback — Flutter's {@code GestureDragStartCallback}
 * ({@code void Function(DragStartDetails)}). A single-abstract-method interface so
 * transpiled Dart closures and method references bind as Java lambdas.
 */
public interface GestureDragStartCallback {
    void call(DragStartDetails details);
}
