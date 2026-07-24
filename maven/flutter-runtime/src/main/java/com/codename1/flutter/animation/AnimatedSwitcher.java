package com.codename1.flutter.animation;

import dart.core.Duration;

/**
 * Cross-fades between successive children over a {@link Duration} — Flutter's
 * {@code AnimatedSwitcher}. This pass shows the current child directly; the
 * in/out transition is deferred.
 */
public class AnimatedSwitcher extends AnimatedChildWidget {

    private Duration duration;
    private Duration reverseDuration;
    private Curve switchInCurve;
    private Curve switchOutCurve;

    public void duration(Duration v) {
        this.duration = v;
    }

    public void reverseDuration(Duration v) {
        this.reverseDuration = v;
    }

    public void switchInCurve(Curve v) {
        this.switchInCurve = v;
    }

    public void switchOutCurve(Curve v) {
        this.switchOutCurve = v;
    }
}
