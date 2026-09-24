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

    /**
     * Dart's {@code min} over two nums whose static types are not both int or
     * both double. The result is one of the arguments, unconverted, so
     * {@code min(1, 2.5)} is the int 1 -- converting both to double printed
     * "1.0" and failed an {@code is int} test. As in Dart, NaN wins and -0.0 is
     * below 0.0.
     */
    public static Number minNum(Number a, Number b) {
        if (isIntegral(a) && isIntegral(b)) {
            return Long.valueOf(Math.min(a.longValue(), b.longValue()));
        }
        double x = a.doubleValue();
        double y = b.doubleValue();
        if (x != x) {
            return a;
        }
        if (y != y) {
            return b;
        }
        if (x != y) {
            return x < y ? a : b;
        }
        return x == 0 && 1 / y < 0 ? b : a;
    }

    /** Dart's {@code max} over two nums; see {@link #minNum}. 0.0 is above -0.0. */
    public static Number maxNum(Number a, Number b) {
        if (isIntegral(a) && isIntegral(b)) {
            return Long.valueOf(Math.max(a.longValue(), b.longValue()));
        }
        double x = a.doubleValue();
        double y = b.doubleValue();
        if (x != x) {
            return a;
        }
        if (y != y) {
            return b;
        }
        if (x != y) {
            return x > y ? a : b;
        }
        return x == 0 && 1 / x < 0 ? b : a;
    }

    /**
     * Dart's {@code pow} over nums: an int raised to a non-negative int stays an
     * int ({@code pow(2, 3)} is 8, not 8.0); anything else is a double.
     */
    public static Number powNum(Number x, Number exponent) {
        if (isIntegral(x) && isIntegral(exponent) && exponent.longValue() >= 0) {
            return Long.valueOf(powInt(x.longValue(), exponent.longValue()));
        }
        return Double.valueOf(Math.pow(x.doubleValue(), exponent.doubleValue()));
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
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

        private static final long FULL_RANGE = 1L << 32;

        public DartRandom() {
            impl = new Random();
        }

        public DartRandom(long seed) {
            impl = new Random(seed);
        }

        /// Dart's {@code Random.nextInt}, whose bound runs to {@code 1 << 32}
        /// INCLUSIVE -- the whole unsigned 32-bit range, which is what a random
        /// ARGB colour or a 32-bit identifier asks for. Java's nextInt(int) stops
        /// at Integer.MAX_VALUE, and narrowing a larger bound to int made it zero
        /// or negative, so {@code nextInt(1 << 32)} threw instead of answering.
        public long nextInt(long max) {
            if (max <= 0 || max > FULL_RANGE) {
                throw new dart.core.RangeError("max must be in range 0 < max <= 2^32, was " + max);
            }
            if (max <= Integer.MAX_VALUE) {
                return impl.nextInt((int) max);
            }
            // 32 unsigned bits, then rejection rather than a bare modulo: a
            // modulo over 2^32 favours the low values whenever max does not
            // divide it, and max here is never below 2^31, so the bias would be
            // up to a factor of two rather than negligible.
            long limit = FULL_RANGE - (FULL_RANGE % max);
            long bits;
            do {
                bits = impl.nextInt() & 0xFFFFFFFFL;
            } while (bits >= limit);
            return bits % max;
        }

        public double nextDouble() {
            return impl.nextDouble();
        }

        public boolean nextBool() {
            return impl.nextBoolean();
        }
    }
}
