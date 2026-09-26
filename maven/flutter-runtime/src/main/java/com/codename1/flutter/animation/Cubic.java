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
