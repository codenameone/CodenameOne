package com.codename1.flutter.animation;

/**
 * The getter extensions Flutter defines on {@link AnimationStatus}
 * ({@code isDismissed} / {@code isCompleted} / {@code isAnimating} /
 * {@code isForwardOrCompleted}). Supplied as static helpers because the
 * transpiler resolves Dart extension getters to static calls of the form
 * {@code AnimationStatusExtensions.isDismissed(status)}.
 */
public final class AnimationStatusExtensions {

    private AnimationStatusExtensions() {
    }

    /** Whether the animation is stopped at the beginning. */
    public static boolean isDismissed(AnimationStatus status) {
        return status == AnimationStatus.dismissed;
    }

    /** Whether the animation is stopped at the end. */
    public static boolean isCompleted(AnimationStatus status) {
        return status == AnimationStatus.completed;
    }

    /** Whether the animation is currently running (forward or reverse). */
    public static boolean isAnimating(AnimationStatus status) {
        return status == AnimationStatus.forward || status == AnimationStatus.reverse;
    }

    /** Whether the animation is running forward or has completed. */
    public static boolean isForwardOrCompleted(AnimationStatus status) {
        return status == AnimationStatus.forward || status == AnimationStatus.completed;
    }
}
