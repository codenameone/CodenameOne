package com.codename1.flutter.animation;

/**
 * The lifecycle phase of an {@link Animation}, mirroring Flutter's
 * {@code AnimationStatus} enum so {@code switch (controller.status)} and
 * {@code status == AnimationStatus.completed} transpile directly.
 */
public enum AnimationStatus {
    /** Stopped at the beginning (lowerBound). */
    dismissed,
    /** Running from beginning toward end. */
    forward,
    /** Running from end back toward beginning. */
    reverse,
    /** Stopped at the end (upperBound). */
    completed
}
