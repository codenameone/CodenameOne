package com.codename1.flutter.animation;

/**
 * A mapping of the unit interval to itself — Flutter's {@code Curve}. Concrete
 * curves ({@link Cubic}, {@link Interval}, and the {@link Curves} constants)
 * override {@link #transform}. {@code t} is clamped to [0, 1].
 */
public abstract class Curve {

    /** Maps {@code t} (0..1) to an eased value; endpoints are pinned to 0 and 1. */
    public double transform(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        return transformInternal(t);
    }

    /** The eased value strictly inside (0, 1). */
    protected abstract double transformInternal(double t);

    /** The curve that runs this one in reverse ({@code 1 - curve(1 - t)}). */
    public Curve flipped() {
        return new FlippedCurve(this);
    }
}
