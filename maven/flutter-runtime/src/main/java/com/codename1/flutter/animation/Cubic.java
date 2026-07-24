package com.codename1.flutter.animation;

/**
 * A cubic Bezier easing curve through control points (a, b) and (c, d) —
 * Flutter's {@code Cubic}. Solves for the x parameter by bisection (the same
 * approach Flutter uses) then evaluates y.
 */
public class Cubic extends Curve {

    private static final double CUBIC_ERROR_BOUND = 0.001;

    private final double a;
    private final double b;
    private final double c;
    private final double d;

    public Cubic(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    private static double evaluateCubic(double a, double b, double m) {
        return 3 * a * (1 - m) * (1 - m) * m
                + 3 * b * (1 - m) * m * m
                + m * m * m;
    }

    @Override
    protected double transformInternal(double t) {
        double start = 0.0;
        double end = 1.0;
        while (true) {
            double midpoint = (start + end) / 2;
            double estimate = evaluateCubic(a, c, midpoint);
            if (Math.abs(t - estimate) < CUBIC_ERROR_BOUND) {
                return evaluateCubic(b, d, midpoint);
            }
            if (estimate < t) {
                start = midpoint;
            } else {
                end = midpoint;
            }
        }
    }
}
