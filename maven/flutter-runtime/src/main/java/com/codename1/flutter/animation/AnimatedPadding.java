package com.codename1.flutter.animation;

import com.codename1.flutter.EdgeInsets;

import dart.core.Duration;

/**
 * Animates changes to its padding over a {@link Duration} — Flutter's
 * {@code AnimatedPadding}. This pass hosts the child; the padding is applied
 * without the tween (final value).
 */
public class AnimatedPadding extends AnimatedChildWidget {

    private EdgeInsets padding;
    private Duration duration;
    private Curve curve;

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    public EdgeInsets getPadding() {
        return padding;
    }
}
