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
package dart.math;

import java.util.Random;

/**
 * Dart's dart:math library: statics over java.lang.Math plus Random with
 * Dart semantics.
 */
public final class DartMath {

    private DartMath() {
    }

    public static final double pi = Math.PI;
    public static final double e = Math.E;

    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static double pow(double x, double exponent) {
        return Math.pow(x, exponent);
    }

    /** Dart's pow with int args and non-negative int exponent stays int. */
    public static long powInt(long x, long exponent) {
        long result = 1;
        long base = x;
        long exp = exponent;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result *= base;
            }
            base *= base;
            exp >>= 1;
        }
        return result;
    }

    public static double sqrt(double x) {
        return Math.sqrt(x);
    }

    public static double sin(double x) {
        return Math.sin(x);
    }

    public static double cos(double x) {
        return Math.cos(x);
    }

    public static double tan(double x) {
        return Math.tan(x);
    }

    public static double asin(double x) {
        return Math.asin(x);
    }

    public static double acos(double x) {
        return Math.acos(x);
    }

    public static double atan(double x) {
        return Math.atan(x);
    }

    public static double atan2(double a, double b) {
        return Math.atan2(a, b);
    }

    public static double exp(double x) {
        return Math.exp(x);
    }

    public static double log(double x) {
        return Math.log(x);
    }

    /**
     * Dart's Random. nextInt(max) returns 0..max-1.
     */
    public static final class DartRandom {
        private final Random impl;

        public DartRandom() {
            impl = new Random();
        }

        public DartRandom(long seed) {
            impl = new Random(seed);
        }

        public long nextInt(long max) {
            if (max <= 0) {
                throw new dart.core.RangeError("max must be in range 0 < max ≤ 2^32, was " + max);
            }
            return impl.nextInt((int) max);
        }

        public double nextDouble() {
            return impl.nextDouble();
        }

        public boolean nextBool() {
            return impl.nextBoolean();
        }
    }
}
