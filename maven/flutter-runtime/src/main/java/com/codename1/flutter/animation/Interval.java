package com.codename1.flutter.animation;

/**
 * A curve that is 0 until {@code begin}, 1 after {@code end}, and applies an
 * inner {@code curve} across [begin, end] — Flutter's {@code Interval}. Used to
 * stagger sub-animations off a single controller.
 */
public class Interval extends Curve {

    private final double begin;
    private final double end;
    private Curve curve = Curves.linear;

    public Interval(double begin, double end) {
        this.begin = begin;
        this.end = end;
    }

    /** Named-parameter setter for the Dart {@code curve:} argument. */
    public void curve(Curve v) {
        this.curve = v == null ? Curves.linear : v;
    }

    @Override
    protected double transformInternal(double t) {
        double span = end - begin;
        double p = span <= 0.0 ? (t < begin ? 0.0 : 1.0) : (t - begin) / span;
        if (p < 0.0) {
            p = 0.0;
        } else if (p > 1.0) {
            p = 1.0;
        }
        return curve.transform(p);
    }
}
