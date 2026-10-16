/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.animation;

/**
 * The standard easing {@link Curve} constants — Flutter's {@code Curves}. The
 * Bezier-based curves use the same control points as Flutter; the analytic
 * ones (linear, decelerate, bounce, elastic) are computed directly.
 */
public final class Curves {

    private Curves() {
    }

    public static final Curve linear = new Curve() {
        @Override
        public double transform(double t) {
            return t;
        }

        @Override
        protected double transformInternal(double t) {
            return t;
        }
    };

    public static final Curve decelerate = new Curve() {
        @Override
        protected double transformInternal(double t) {
            double u = 1.0 - t;
            return 1.0 - u * u;
        }
    };

    public static final Cubic ease = new Cubic(0.25, 0.1, 0.25, 1.0);
    public static final Cubic easeIn = new Cubic(0.42, 0.0, 1.0, 1.0);
    public static final Cubic easeOut = new Cubic(0.0, 0.0, 0.58, 1.0);
    public static final Cubic easeInOut = new Cubic(0.42, 0.0, 0.58, 1.0);
    public static final Cubic easeInOutCubic = new Cubic(0.645, 0.045, 0.355, 1.0);
    public static final Cubic easeInCubic = new Cubic(0.55, 0.055, 0.675, 0.19);
    public static final Cubic easeOutCubic = new Cubic(0.215, 0.61, 0.355, 1.0);
    public static final Cubic easeInSine = new Cubic(0.47, 0.0, 0.745, 0.715);
    public static final Cubic easeOutSine = new Cubic(0.39, 0.575, 0.565, 1.0);
    public static final Cubic easeInOutSine = new Cubic(0.445, 0.05, 0.55, 0.95);
    public static final Cubic fastOutSlowIn = new Cubic(0.4, 0.0, 0.2, 1.0);
    public static final Cubic slowMiddle = new Cubic(0.15, 0.85, 0.85, 0.15);
    public static final Cubic fastLinearToSlowEaseIn = new Cubic(0.18, 1.0, 0.04, 1.0);

    public static final Curve bounceIn = new Curve() {
        @Override
        protected double transformInternal(double t) {
            return 1.0 - bounce(1.0 - t);
        }
    };

    public static final Curve bounceOut = new Curve() {
        @Override
        protected double transformInternal(double t) {
            return bounce(t);
        }
    };

    public static final Curve bounceInOut = new Curve() {
        @Override
        protected double transformInternal(double t) {
            if (t < 0.5) {
                return (1.0 - bounce(1.0 - t * 2.0)) * 0.5;
            }
            return bounce(t * 2.0 - 1.0) * 0.5 + 0.5;
        }
    };

    public static final Curve elasticIn = new Curve() {
        @Override
        protected double transformInternal(double t) {
            double p = 0.4;
            double s = p / 4.0;
            double m = t - 1.0;
            return -Math.pow(2.0, 10.0 * m) * Math.sin((m - s) * (2.0 * Math.PI) / p);
        }
    };

    public static final Curve elasticOut = new Curve() {
        @Override
        protected double transformInternal(double t) {
            double p = 0.4;
            double s = p / 4.0;
            return Math.pow(2.0, -10.0 * t) * Math.sin((t - s) * (2.0 * Math.PI) / p) + 1.0;
        }
    };

    private static double bounce(double t) {
        if (t < 1.0 / 2.75) {
            return 7.5625 * t * t;
        }
        if (t < 2.0 / 2.75) {
            double u = t - 1.5 / 2.75;
            return 7.5625 * u * u + 0.75;
        }
        if (t < 2.5 / 2.75) {
            double u = t - 2.25 / 2.75;
            return 7.5625 * u * u + 0.9375;
        }
        double u = t - 2.625 / 2.75;
        return 7.5625 * u * u + 0.984375;
    }
}
