package com.codename1.flutter;

/**
 * A description of a gradient that can produce a {@link Shader} for a given
 * rectangle — Flutter's {@code Gradient}. Concrete subtypes are
 * {@link LinearGradient}, {@link RadialGradient} and {@link SweepGradient}.
 *
 * <p>Only the color list is retained by the Codename One runtime for this
 * milestone; geometry (begin/end/center/stops) is held but not yet painted.</p>
 */
public abstract class Gradient {

    Object begin;
    Object end;
    Object center;
    Object colors;
    Object stops;
    TileMode tileMode = TileMode.clamp;
    Object transform;

    public void begin(Object v) {
        this.begin = v;
    }

    public void end(Object v) {
        this.end = v;
    }

    public void center(Object v) {
        this.center = v;
    }

    public void colors(Object v) {
        this.colors = v;
    }

    public void stops(Object v) {
        this.stops = v;
    }

    public void tileMode(TileMode v) {
        this.tileMode = v == null ? TileMode.clamp : v;
    }

    public void transform(Object v) {
        this.transform = v;
    }

    public Object getColors() {
        return colors;
    }

    /** Produces a shader painting this gradient over {@code rect}. */
    public Shader createShader(Rect rect, Object textDirection) {
        return new GradientShader(this, rect);
    }

    /** A concrete {@link Shader} bound to a gradient and a rectangle. */
    static final class GradientShader extends Shader {
        final Gradient gradient;
        final Rect rect;

        GradientShader(Gradient gradient, Rect rect) {
            this.gradient = gradient;
            this.rect = rect;
        }
    }
}
