package com.codename1.flutter.animation;

/**
 * Animates its own size along one axis, clipping its {@code child} — Flutter's
 * {@code SizeTransition}. The {@code sizeFactor} animation drives the visible
 * fraction (0..1) and {@code axisAlignment} anchors the reveal. This pass hosts
 * the child at full size; the animated clip is deferred (see
 * {@link AnimatedChildWidget}).
 */
public class SizeTransition extends AnimatedChildWidget {

    private Object axis;
    private Animation<Double> sizeFactor;
    private Double axisAlignment;

    public void axis(Object v) {
        this.axis = v;
    }

    public void sizeFactor(Animation<Double> v) {
        this.sizeFactor = v;
    }

    public void axisAlignment(double v) {
        this.axisAlignment = v;
    }

    public Animation<Double> getSizeFactor() {
        return sizeFactor;
    }
}
