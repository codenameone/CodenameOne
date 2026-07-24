package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;

import dart.core.Duration;

/**
 * Animates its own size to fit its child when the child changes size —
 * Flutter's {@code AnimatedSize}. This pass hosts the child and sizes to it
 * immediately; the size tween is deferred.
 */
public class AnimatedSize extends AnimatedChildWidget {

    private Duration duration;
    private Curve curve;
    private Alignment alignment;

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }
}
