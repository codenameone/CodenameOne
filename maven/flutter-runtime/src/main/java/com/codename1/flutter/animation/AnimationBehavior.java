package com.codename1.flutter.animation;

/**
 * Configures how an {@link AnimationController} behaves when animation is
 * disabled (e.g. by the platform's "reduce motion" accessibility setting) —
 * Flutter's {@code AnimationBehavior}. {@link #normal} lets the controller obey
 * the platform setting, while {@link #preserve} forces it to animate anyway
 * (the progress-indicator demo passes {@code preserve} so its spinner keeps
 * turning).
 */
public enum AnimationBehavior {
    normal,
    preserve
}
