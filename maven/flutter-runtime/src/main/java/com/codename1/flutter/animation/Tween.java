package com.codename1.flutter.animation;

/**
 * Linearly interpolates between a {@code begin} and {@code end} value —
 * Flutter's {@code Tween<T>}. The base class handles the numeric case
 * ({@code begin + (end - begin) * t}) for {@link Number} values; typed
 * subclasses ({@link ColorTween}, {@link IntTween}) override {@link #lerp} for
 * their own interpolation. For opaque value types (border radius, matrices)
 * where no interpolation is wired this pass, {@link #lerp} steps at the
 * midpoint — a correct-shape, minimal fallback.
 */
public class Tween<T> extends Animatable<T> {

    private T beginValue;
    private T endValue;

    public Tween() {
    }

    /** Named-parameter setter for the Dart {@code begin:} argument. */
    public void begin(T v) {
        this.beginValue = v;
    }

    /** Named-parameter setter for the Dart {@code end:} argument. */
    public void end(T v) {
        this.endValue = v;
    }

    /** Dart getter {@code tween.begin}. */
    public T begin() {
        return beginValue;
    }

    /** Dart getter {@code tween.end}. */
    public T end() {
        return endValue;
    }

    /** Interpolates at {@code t} (0..1). Override for typed interpolation. */
    @SuppressWarnings("unchecked")
    public T lerp(double t) {
        if (beginValue instanceof Number && endValue instanceof Number) {
            double b = ((Number) beginValue).doubleValue();
            double e = ((Number) endValue).doubleValue();
            return (T) Double.valueOf(b + (e - b) * t);
        }
        if (t < 0.5) {
            return beginValue;
        }
        return endValue;
    }

    @Override
    public T transform(double t) {
        if (t == 0.0) {
            return beginValue;
        }
        if (t == 1.0) {
            return endValue;
        }
        return lerp(t);
    }
}
