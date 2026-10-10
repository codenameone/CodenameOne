/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package javafx.animation;

/// The pacing of an animation between two values: it maps the fraction
/// of time that has passed to the fraction of the way the value has
/// gone.
///
/// Numbers are blended, and so are values that implement
/// [Interpolatable]. Any other value cannot be blended: it stays the
/// start value until the curve reaches its end and is the end value from
/// then on.
public abstract class Interpolator {

    private static final double EPSILON = 1e-12;

    /// Stays at the start value until the very end.
    public static final Interpolator DISCRETE = new Builtin(Builtin.KIND_DISCRETE, 0, 0, 0, 0);

    /// Moves at a constant speed.
    public static final Interpolator LINEAR = new Builtin(Builtin.KIND_LINEAR, 0, 0, 0, 0);

    /// Starts slowly, runs at a constant speed and ends slowly; the
    /// first and the last fifth of the time are spent accelerating and
    /// decelerating.
    public static final Interpolator EASE_BOTH = new Builtin(Builtin.KIND_EASE_BOTH, 0, 0, 0, 0);

    /// Starts slowly and then runs at a constant speed.
    public static final Interpolator EASE_IN = new Builtin(Builtin.KIND_EASE_IN, 0, 0, 0, 0);

    /// Runs at a constant speed and ends slowly.
    public static final Interpolator EASE_OUT = new Builtin(Builtin.KIND_EASE_OUT, 0, 0, 0, 0);

    /// Creates an interpolator.
    protected Interpolator() {
    }

    /// Creates an interpolator that follows a cubic Bezier curve from
    /// (0, 0) to (1, 1) with the two given control points; all four
    /// coordinates lie between 0 and 1.
    public static Interpolator SPLINE(double x1, double y1, double x2, double y2) {
        return Builtin.spline(x1, y1, x2, y2);
    }

    /// Returns the value a fraction of the time between two values.
    /// Numbers and [Interpolatable] values are blended, anything else
    /// switches to the end value when the curve reaches its end.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object interpolate(Object startValue, Object endValue, double fraction) {
        if (startValue instanceof Number && endValue instanceof Number) {
            double start = ((Number) startValue).doubleValue();
            double end = ((Number) endValue).doubleValue();
            double value = start + (end - start) * curve(fraction);
            if (startValue instanceof Double || endValue instanceof Double) {
                return Double.valueOf(value);
            }
            if (startValue instanceof Float || endValue instanceof Float) {
                return Float.valueOf((float) value);
            }
            if (startValue instanceof Long || endValue instanceof Long) {
                return Long.valueOf(Math.round(value));
            }
            return Integer.valueOf((int) Math.round(value));
        }
        if (startValue instanceof Interpolatable && endValue instanceof Interpolatable) {
            return ((Interpolatable) startValue).interpolate(endValue, curve(fraction));
        }
        return Math.abs(curve(fraction) - 1.0) < EPSILON ? endValue : startValue;
    }

    /// Returns the start value until the curve reaches its end and the
    /// end value from then on.
    public boolean interpolate(boolean startValue, boolean endValue, double fraction) {
        return Math.abs(curve(fraction) - 1.0) < EPSILON ? endValue : startValue;
    }

    /// Returns the value a fraction of the time between two numbers.
    public double interpolate(double startValue, double endValue, double fraction) {
        return startValue + (endValue - startValue) * curve(fraction);
    }

    /// Returns the value a fraction of the time between two numbers,
    /// rounded.
    public int interpolate(int startValue, int endValue, double fraction) {
        return startValue + (int) Math.round((endValue - startValue) * curve(fraction));
    }

    /// Returns the value a fraction of the time between two numbers,
    /// rounded.
    public long interpolate(long startValue, long endValue, double fraction) {
        return startValue + Math.round((endValue - startValue) * curve(fraction));
    }

    /// Maps the fraction of time that has passed, 0 to 1, to the
    /// fraction of the way the value has gone.
    protected abstract double curve(double t);

    /// The interpolators JavaFX provides. The class has no state of its
    /// own to initialise, only constants the compiler folds, so the
    /// class above can create its instances while it is itself being
    /// initialised.
    private static final class Builtin extends Interpolator {

        private static final int KIND_DISCRETE = 0;
        private static final int KIND_LINEAR = 1;
        private static final int KIND_EASE_BOTH = 2;
        private static final int KIND_EASE_IN = 3;
        private static final int KIND_EASE_OUT = 4;
        private static final int KIND_SPLINE = 5;

        // The share of the time spent accelerating or decelerating.
        private static final double EASE = 0.2;


        private final int kind;
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        private Builtin(int kind, double x1, double y1, double x2, double y2) {
            this.kind = kind;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        static Interpolator spline(double x1, double y1, double x2, double y2) {
            if (!(x1 >= 0 && x1 <= 1 && y1 >= 0 && y1 <= 1 && x2 >= 0 && x2 <= 1 && y2 >= 0 && y2 <= 1)) {
                throw new IllegalArgumentException("Control point coordinates must all be in range [0,1]");
            }
            return new Builtin(KIND_SPLINE, x1, y1, x2, y2);
        }

        private static double clamp(double v) {
            return v < 0 ? 0 : v > 1 ? 1 : v;
        }

        /// One coordinate of the Bezier curve with end points 0 and 1.
        private static double bezier(double a, double b, double s) {
            double r = 1 - s;
            return 3 * r * r * s * a + 3 * r * s * s * b + s * s * s;
        }

        /// The curve is given by its parameter, the animation asks by
        /// x. x grows with the parameter because both control points lie
        /// between the ends, so halving the interval finds it.
        private double splineAt(double t) {
            double lo = 0;
            double hi = 1;
            double s = t;
            for (int i = 0; i < 60; i++) {
                double x = bezier(x1, x2, s);
                if (Math.abs(x - t) < 1e-14) {
                    break;
                }
                if (x < t) {
                    lo = s;
                } else {
                    hi = s;
                }
                s = (lo + hi) / 2;
            }
            return bezier(y1, y2, s);
        }

        @Override
        protected double curve(double t) {
            switch (kind) {
                case KIND_DISCRETE:
                    return Math.abs(t - 1.0) < EPSILON ? 1.0 : 0.0;
                case KIND_LINEAR:
                    return t;
                case KIND_EASE_BOTH:
                    // Constant acceleration over the first fifth, constant speed,
                    // constant deceleration over the last fifth: the top speed
                    // is 1 / (1 - EASE).
                    if (t < EASE) {
                        return clamp(3.125 * t * t);
                    } else if (t > 1 - EASE) {
                        return clamp(-3.125 * t * t + 6.25 * t - 2.125);
                    }
                    return clamp(1.25 * t - 0.125);
                case KIND_EASE_IN:
                    // Top speed 1 / (1 - EASE / 2) = 10 / 9.
                    if (t < EASE) {
                        return clamp(25.0 / 9.0 * t * t);
                    }
                    return clamp(10.0 / 9.0 * t - 1.0 / 9.0);
                case KIND_EASE_OUT:
                    if (t > 1 - EASE) {
                        return clamp(-25.0 / 9.0 * t * t + 50.0 / 9.0 * t - 16.0 / 9.0);
                    }
                    return clamp(10.0 / 9.0 * t);
                case KIND_SPLINE:
                    return splineAt(clamp(t));
                default:
                    return t;
            }
        }

        @Override
        public String toString() {
            switch (kind) {
                case KIND_DISCRETE:
                    return "Interpolator.DISCRETE";
                case KIND_LINEAR:
                    return "Interpolator.LINEAR";
                case KIND_EASE_BOTH:
                    return "Interpolator.EASE_BOTH";
                case KIND_EASE_IN:
                    return "Interpolator.EASE_IN";
                case KIND_EASE_OUT:
                    return "Interpolator.EASE_OUT";
                default:
                    return "Interpolator.SPLINE [x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2 + "]";
            }
        }
    }
}
