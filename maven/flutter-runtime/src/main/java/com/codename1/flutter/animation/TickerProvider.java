package com.codename1.flutter.animation;

/**
 * Flutter's TickerProvider: the object that vends the frame ticks an
 * {@link AnimationController} uses to advance. In this runtime the controller
 * self-drives from a CN1 timer, so the provider is a marker type — passing a
 * State (mixed with {@link SingleTickerProviderStateMixin}) as {@code vsync}
 * simply satisfies the API shape.
 */
public interface TickerProvider {
}
