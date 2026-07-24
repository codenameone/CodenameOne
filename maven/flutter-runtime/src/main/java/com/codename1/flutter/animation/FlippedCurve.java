package com.codename1.flutter.animation;

/**
 * Runs a {@link Curve} in reverse: {@code transform(t) == 1 - curve(1 - t)} —
 * Flutter's {@code FlippedCurve} / {@code Curve.flipped}.
 */
public class FlippedCurve extends Curve {

    private final Curve curve;

    public FlippedCurve(Curve curve) {
        this.curve = curve;
    }

    @Override
    protected double transformInternal(double t) {
        return 1.0 - curve.transform(1.0 - t);
    }
}
