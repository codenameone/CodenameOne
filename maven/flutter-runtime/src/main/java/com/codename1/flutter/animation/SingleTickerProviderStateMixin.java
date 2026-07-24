package com.codename1.flutter.animation;

/**
 * Java surface of Flutter's {@code SingleTickerProviderStateMixin}. A Dart
 * {@code State with SingleTickerProviderStateMixin} transpiles to a Java class
 * that {@code implements} this interface, which extends {@link TickerProvider}
 * so {@code AnimationController(vsync: this)} type-checks. No members are
 * needed — controllers self-drive from a CN1 timer.
 */
public interface SingleTickerProviderStateMixin extends TickerProvider {
}
