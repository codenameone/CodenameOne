package com.codename1.flutter.animation;

import dart.core.Duration;

/**
 * Animates its child's opacity to a target value over a {@link Duration} —
 * Flutter's {@code AnimatedOpacity}. This pass hosts the child; opacity
 * compositing is deferred.
 */
public class AnimatedOpacity extends AnimatedChildWidget {

    private double opacity = 1.0;
    private Duration duration;
    private Curve curve;

    public void opacity(double v) {
        this.opacity = v;
    }

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }
}
