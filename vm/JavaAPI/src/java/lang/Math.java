/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package java.lang;
/**
 * The class Math contains methods for performing basic numeric operations.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Math{
    /**
     * The double value that is closer than any other to e, the base of the natural logarithms.
     * Since: CLDC 1.1 See Also:Constant Field Values
     */
    public static final double E=2.718281828459045d;

    /**
     * The double value that is closer than any other to
     * , the ratio of the circumference of a circle to its diameter.
     * Since: CLDC 1.1 See Also:Constant Field Values
     */
    public static final double PI=3.141592653589793d;

    /**
     * Returns the absolute value of a double value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned. Special cases: If the argument is positive zero or negative zero, the result is positive zero. If the argument is infinite, the result is positive infinity. If the argument is NaN, the result is NaN. In other words, the result is equal to the value of the expression:
     * Double.longBitsToDouble((Double.doubleToLongBits(a)<<1)>>>1)
     */
    public static native double abs(double a);

    /**
     * Returns the absolute value of a float value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned. Special cases: If the argument is positive zero or negative zero, the result is positive zero. If the argument is infinite, the result is positive infinity. If the argument is NaN, the result is NaN. In other words, the result is equal to the value of the expression:
     * Float.intBitsToFloat(0x7fffffff &amp; Float.floatToIntBits(a))
     */
    public static native float abs(float a);

    /**
     * Returns the absolute value of an int value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.
     * Note that if the argument is equal to the value of Integer.MIN_VALUE, the most negative representable int value, the result is that same value, which is negative.
     */
    public static native int abs(int a);

    /**
     * Returns the absolute value of a long value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.
     * Note that if the argument is equal to the value of Long.MIN_VALUE, the most negative representable long value, the result is that same value, which is negative.
     */
    public static native long abs(long a);

    /**
     * Returns the smallest (closest to negative infinity) double value that is not less than the argument and is equal to a mathematical integer. Special cases: If the argument value is already equal to a mathematical integer, then the result is the same as the argument. If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument. If the argument value is less than zero but greater than -1.0, then the result is negative zero. Note that the value of Math.ceil(x) is exactly the value of -Math.floor(-x).
     */
    public static native double ceil(double a);

    /**
     * Returns the largest (closest to positive infinity) double value that is not greater than the argument and is equal to a mathematical integer. Special cases: If the argument value is already equal to a mathematical integer, then the result is the same as the argument. If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument.
     */
    public static native double floor(double a);

    /**
     * Returns the greater of two double values. That is, the result is the argument closer to positive infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other negative zero, the result is positive zero.
     */
    public static native double max(double a, double b);

    public static native double pow(double a, double b);

    /**
     * Returns the greater of two float values. That is, the result is the argument closer to positive infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other negative zero, the result is positive zero.
     */
    public static native float max(float a, float b);

    /**
     * Returns the greater of two int values. That is, the result is the argument closer to the value of Integer.MAX_VALUE. If the arguments have the same value, the result is that same value.
     */
    public static native int max(int a, int b);

    /**
     * Returns the greater of two long values. That is, the result is the argument closer to the value of Long.MAX_VALUE. If the arguments have the same value, the result is that same value.
     */
    public static native long max(long a, long b);

    /**
     * Returns the smaller of two double values. That is, the result is the value closer to negative infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other is negative zero, the result is negative zero.
     */
    public static native double min(double a, double b);

    /**
     * Returns the smaller of two float values. That is, the result is the value closer to negative infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other is negative zero, the result is negative zero.
     */
    public static native float min(float a, float b);

    /**
     * Returns the smaller of two int values. That is, the result the argument closer to the value of Integer.MIN_VALUE. If the arguments have the same value, the result is that same value.
     */
    public static native int min(int a, int b);

    /**
     * Returns the smaller of two long values. That is, the result is the argument closer to the value of Long.MIN_VALUE. If the arguments have the same value, the result is that same value.
     */
    public static native long min(long a, long b);

    /**
     * Returns the trigonometric cosine of an angle. Special case: If the argument is NaN or an infinity, then the result is NaN.
     */
    public native static double cos(double a);

    /**
     * Returns the trigonometric sine of an angle. Special cases: If the argument is NaN or an infinity, then the result is NaN. If the argument is positive zero, then the result is positive zero; if the argument is negative zero, then the result is negative zero.
     */
    public native static double sin(double a);

    /**
     * Returns the correctly rounded positive square root of a double value. Special cases: If the argument is NaN or less than zero, then the result is NaN. If the argument is positive infinity, then the result is positive infinity. If the argument is positive zero or negative zero, then the result is the same as the argument.
     */
    public native static double sqrt(double a);

    /**
     * Returns the trigonometric tangent of an angle. Special cases: If the argument is NaN or an infinity, then the result is NaN. If the argument is positive zero, then the result is positive zero; if the argument is negative zero, then the result is negative zero
     */
    public native static double tan(double a);

    public native static double atan(double d);

    /**
     * Returns the arc cosine of a value, in the range 0.0 through pi. Special
     * case: if the argument is NaN or its absolute value is greater than 1,
     * the result is NaN.
     */
    public native static double acos(double a);

    /**
     * Returns the arc sine of a value, in the range -pi/2 through pi/2. Special
     * case: if the argument is NaN or its absolute value is greater than 1, the
     * result is NaN.
     */
    public native static double asin(double a);

    /**
     * Returns the angle theta from the conversion of rectangular coordinates
     * ({@code x}, {@code y}) to polar coordinates -- the angle of the vector,
     * in the range -pi through pi.
     */
    public native static double atan2(double y, double x);

    /**
     * Returns Euler's number e raised to the power of a value.
     */
    public native static double exp(double a);

    /**
     * Returns the natural logarithm (base e) of a value. Special cases: if the
     * argument is NaN or less than zero the result is NaN; if it is positive
     * zero or negative zero the result is negative infinity.
     */
    public native static double log(double a);

    /**
     * Returns the base 10 logarithm of a value, with the same special cases as
     * {@link #log(double)}.
     */
    public native static double log10(double a);


    /**
     * Converts an angle measured in radians to the equivalent angle measured in degrees.
     */
    public static double toDegrees(double angrad){
        return angrad * 57.2957795;
    }

    /**
     * Converts an angle measured in degrees to the equivalent angle measured in radians.
     */
    public static double toRadians(double angdeg){
        return angdeg / 57.2957795;
    }

    /**
     * Returns the result of rounding the argument to an integer. The result is
     * equivalent to {@code (long) Math.floor(d+0.5)}.
     * <p>
     * Special cases:
     * <ul>
     * <li>{@code round(+0.0) = +0.0}</li>
     * <li>{@code round(-0.0) = +0.0}</li>
     * <li>{@code round((anything > Long.MAX_VALUE) = Long.MAX_VALUE}</li>
     * <li>{@code round((anything < Long.MIN_VALUE) = Long.MIN_VALUE}</li>
     * <li>{@code round(+infintiy) = Long.MAX_VALUE}</li>
     * <li>{@code round(-infintiy) = Long.MIN_VALUE}</li>
     * <li>{@code round(NaN) = +0.0}</li>
     * </ul>
     * 
     * @param d
     *            the value to be rounded.
     * @return the closest integer to the argument.
     */
    public static long round(double d) {
        // The JDK's own algorithm, on the bits. floor(d + 0.5) was off by one
        // where the addition rounds: 0.49999999999999994 + 0.5 is exactly 1.0,
        // so it answered 1 for a value below one half.
        long bits = Double.doubleToRawLongBits(d);
        long biasedExp = (bits & 0x7ff0000000000000L) >> 52;
        long shift = (51 + 1023) - biasedExp;
        if ((shift & -64) == 0) {
            long r = (bits & 0x000fffffffffffffL) | 0x0010000000000000L;
            if (bits < 0) {
                r = -r;
            }
            return ((r >> shift) + 1) >> 1;
        }
        // NaN answers 0 and anything out of range saturates, as the cast does.
        return (long) d;
    }

    /**
     * Returns the result of rounding the argument to an integer. The result is
     * equivalent to {@code (int) Math.floor(f+0.5)}.
     * <p>
     * Special cases:
     * <ul>
     * <li>{@code round(+0.0) = +0.0}</li>
     * <li>{@code round(-0.0) = +0.0}</li>
     * <li>{@code round((anything > Integer.MAX_VALUE) = Integer.MAX_VALUE}</li>
     * <li>{@code round((anything < Integer.MIN_VALUE) = Integer.MIN_VALUE}</li>
     * <li>{@code round(+infintiy) = Integer.MAX_VALUE}</li>
     * <li>{@code round(-infintiy) = Integer.MIN_VALUE}</li>
     * <li>{@code round(NaN) = +0.0}</li>
     * </ul>
     * 
     * @param f
     *            the value to be rounded.
     * @return the closest integer to the argument.
     */
    public static int round(float f) {
        // The JDK's own algorithm, on the bits; see round(double).
        int bits = Float.floatToRawIntBits(f);
        int biasedExp = (bits & 0x7f800000) >> 23;
        int shift = (22 + 127) - biasedExp;
        if ((shift & -32) == 0) {
            int r = (bits & 0x007fffff) | 0x00800000;
            if (bits < 0) {
                r = -r;
            }
            return ((r >> shift) + 1) >> 1;
        }
        return (int) f;
    }
    
    public static double nextDown(double d) {
        if (Double.isNaN(d)) {
            return d;
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0.0) {
            // Both zeros; stepping the bits of +0.0 down made a NaN.
            return -Double.MIN_VALUE;
        }
        long bits = Double.doubleToLongBits(d);
        boolean negative = (bits & (1L << 63)) != 0;
        if (negative) {
            bits++;
        } else {
            bits--;
        }
        return Double.longBitsToDouble(bits);
    }
    
    /// The double next to `start` in the direction of `direction`.
    public static double nextAfter(double start, double direction) {
        if (start > direction) {
            if (start != 0.0d) {
                long t = Double.doubleToRawLongBits(start);
                return Double.longBitsToDouble(t + ((t > 0L) ? -1L : 1L));
            }
            return -Double.MIN_VALUE;
        } else if (start < direction) {
            long t = Double.doubleToRawLongBits(start + 0.0d);
            return Double.longBitsToDouble(t + ((t >= 0L) ? 1L : -1L));
        } else if (start == direction) {
            return direction;
        }
        return start + direction;
    }

    /// The float next to `start` in the direction of `direction`.
    public static float nextAfter(float start, double direction) {
        if (start > direction) {
            if (start != 0.0f) {
                int t = Float.floatToRawIntBits(start);
                return Float.intBitsToFloat(t + ((t > 0) ? -1 : 1));
            }
            return -Float.MIN_VALUE;
        } else if (start < direction) {
            int t = Float.floatToRawIntBits(start + 0.0f);
            return Float.intBitsToFloat(t + ((t >= 0) ? 1 : -1));
        } else if (start == direction) {
            return (float) direction;
        }
        return start + (float) direction;
    }

    // ---------------------------------------------------------------------
    // Members of the JDK's class this one did not have, in plain Java. The
    // integer division below checks its divisor itself: ParparVM answers 0
    // for a division by zero instead of throwing.
    // ---------------------------------------------------------------------

    /// Twice pi, the ratio of a circle's circumference to its radius.
    public static final double TAU = 2.0 * PI;

    /// Holds the generator of `random()`, created on first use.
    private static final class RandomHolder {
        static final java.util.Random RANDOM = new java.util.Random();

        private RandomHolder() {
        }
    }

    /// A pseudo-random double in `[0, 1)`.
    public static double random() {
        return RandomHolder.RANDOM.nextDouble();
    }

    /// -1.0, 0.0 or 1.0 by the sign of `d`; a zero or NaN is returned as it is.
    public static double signum(double d) {
        return d > 0 ? 1.0 : d < 0 ? -1.0 : d;
    }

    /// -1.0f, 0.0f or 1.0f by the sign of `f`; a zero or NaN is returned as it is.
    public static float signum(float f) {
        return f > 0 ? 1.0f : f < 0 ? -1.0f : f;
    }

    /// `magnitude` with the sign bit of `sign`.
    public static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) & 0x8000000000000000L)
                | (Double.doubleToRawLongBits(magnitude) & 0x7fffffffffffffffL));
    }

    /// `magnitude` with the sign bit of `sign`.
    public static float copySign(float magnitude, float sign) {
        return Float.intBitsToFloat((Float.floatToRawIntBits(sign) & 0x80000000)
                | (Float.floatToRawIntBits(magnitude) & 0x7fffffff));
    }

    /// The unbiased exponent of `d`: 1024 for NaN and the infinities, -1023
    /// for zero and the subnormals.
    public static int getExponent(double d) {
        return (int) (((Double.doubleToRawLongBits(d) & 0x7ff0000000000000L) >> 52) - 1023);
    }

    /// The unbiased exponent of `f`: 128 for NaN and the infinities, -127 for
    /// zero and the subnormals.
    public static int getExponent(float f) {
        return ((Float.floatToRawIntBits(f) & 0x7f800000) >> 23) - 127;
    }

    private static double powerOfTwoD(int n) {
        return Double.longBitsToDouble(((long) n + 1023L) << 52);
    }

    /// The distance from `d` to the next double of larger magnitude.
    public static double ulp(double d) {
        int exp = getExponent(d);
        if (exp == Double.MAX_EXPONENT + 1) {
            return Math.abs(d);
        }
        if (exp == Double.MIN_EXPONENT - 1) {
            return Double.MIN_VALUE;
        }
        exp -= 52;
        if (exp >= Double.MIN_EXPONENT) {
            return powerOfTwoD(exp);
        }
        return Double.longBitsToDouble(1L << (exp - (Double.MIN_EXPONENT - 52)));
    }

    /// The distance from `f` to the next float of larger magnitude.
    public static float ulp(float f) {
        int exp = getExponent(f);
        if (exp == Float.MAX_EXPONENT + 1) {
            return Math.abs(f);
        }
        if (exp == Float.MIN_EXPONENT - 1) {
            return Float.MIN_VALUE;
        }
        exp -= 23;
        if (exp >= Float.MIN_EXPONENT) {
            return Float.intBitsToFloat((exp + 127) << 23);
        }
        return Float.intBitsToFloat(1 << (exp - (Float.MIN_EXPONENT - 23)));
    }

    /// The double next to `d` towards positive infinity.
    public static double nextUp(double d) {
        if (d != d || d == Double.POSITIVE_INFINITY) {
            return d;
        }
        d += 0.0d;
        return Double.longBitsToDouble(Double.doubleToRawLongBits(d) + ((d >= 0.0d) ? 1L : -1L));
    }

    /// The float next to `f` towards positive infinity.
    public static float nextUp(float f) {
        if (f != f || f == Float.POSITIVE_INFINITY) {
            return f;
        }
        f += 0.0f;
        return Float.intBitsToFloat(Float.floatToRawIntBits(f) + ((f >= 0.0f) ? 1 : -1));
    }

    /// The float next to `f` towards negative infinity.
    public static float nextDown(float f) {
        if (f != f || f == Float.NEGATIVE_INFINITY) {
            return f;
        }
        if (f == 0.0f) {
            return -Float.MIN_VALUE;
        }
        return Float.intBitsToFloat(Float.floatToRawIntBits(f) + ((f > 0.0f) ? -1 : 1));
    }

    /// `d` times 2 to the power `scaleFactor`, rounded once.
    public static double scalb(double d, int scaleFactor) {
        // Past this every finite input under- or overflows; the significand
        // counts all 53 bits, or the largest value stops one step short of 0.
        final int maxScale = 1023 + 1022 + 53 + 1;
        int scaleIncrement;
        double expDelta;
        if (scaleFactor < 0) {
            scaleFactor = Math.max(scaleFactor, -maxScale);
            scaleIncrement = -512;
            expDelta = 0x1.0p-512;
        } else {
            scaleFactor = Math.min(scaleFactor, maxScale);
            scaleIncrement = 512;
            expDelta = 0x1.0p512;
        }
        // The part of the scale that is not a multiple of 512, with the
        // remainder's sign; the rest goes in exact steps of 2 to the 512.
        int t = (scaleFactor >> 9 - 1) >>> 32 - 9;
        int expAdjust = ((scaleFactor + t) & (512 - 1)) - t;
        d *= powerOfTwoD(expAdjust);
        scaleFactor -= expAdjust;
        while (scaleFactor != 0) {
            d *= expDelta;
            scaleFactor -= scaleIncrement;
        }
        return d;
    }

    /// `f` times 2 to the power `scaleFactor`, rounded once.
    public static float scalb(float f, int scaleFactor) {
        final int maxScale = 127 + 126 + 24 + 1;
        scaleFactor = Math.max(Math.min(scaleFactor, maxScale), -maxScale);
        return (float) ((double) f * powerOfTwoD(scaleFactor));
    }

    /// The square root of `x * x + y * y`, without overflow in between.
    public static double hypot(double x, double y) {
        if (Double.isInfinite(x) || Double.isInfinite(y)) {
            return Double.POSITIVE_INFINITY;
        }
        if (x != x || y != y) {
            return Double.NaN;
        }
        double a = Math.abs(x);
        double b = Math.abs(y);
        double big = a > b ? a : b;
        double small = a > b ? b : a;
        if (big == 0) {
            return 0;
        }
        double ratio = small / big;
        return big * Math.sqrt(1 + ratio * ratio);
    }

    /// The cube root of `a`.
    public static double cbrt(double a) {
        if (a == 0 || !Double.isFinite(a)) {
            return a;
        }
        double magnitude = Math.abs(a);
        // A subnormal has too few significant bits for the estimate below:
        // scale it by 2 to the 54 (a cube, so the root scales by 2 to the 18).
        boolean subnormal = magnitude < Double.MIN_NORMAL;
        if (subnormal) {
            magnitude *= 0x1.0p54;
        }
        double root = Math.pow(magnitude, 1.0 / 3.0);
        // One Newton step takes out the error of the inexact exponent.
        root = root - (root * root * root - magnitude) / (3 * root * root);
        if (subnormal) {
            root *= 0x1.0p-18;
        }
        return a < 0 ? -root : root;
    }

    /// The whole number nearest `a`; a tie goes to the even one.
    public static double rint(double a) {
        if (!Double.isFinite(a) || Math.abs(a) >= 4503599627370496.0) {
            return a;
        }
        double floor = Math.floor(a);
        double diff = a - floor;
        double half = floor / 2;
        double out = diff < 0.5 || diff == 0.5 && half == Math.floor(half) ? floor : floor + 1;
        // -0.2 rounds to negative zero, which prints and divides differently.
        return out == 0 && (a < 0 || copySign(1.0, a) < 0) ? -0.0 : out;
    }

    /// The hyperbolic sine of `x`.
    public static double sinh(double x) {
        if (x != x || x == 0) {
            return x;
        }
        if (Math.abs(x) < 1) {
            // Near zero the two exponentials agree in almost every digit and
            // their difference is lost; the series keeps it.
            return sinhSeries(x);
        }
        if (Math.abs(x) > 22) {
            return x < 0 ? -halfExp(-x) : halfExp(x);
        }
        return (Math.exp(x) - Math.exp(-x)) / 2;
    }

    /// `exp(x) / 2` for `x > 22`, taken as two halves past 709 where
    /// `exp(x)` itself overflows though half of it does not.
    private static double halfExp(double x) {
        if (x < 709) {
            return Math.exp(x) / 2;
        }
        double half = Math.exp(x / 2);
        return half * (half / 2);
    }

    private static double sinhSeries(double x) {
        double x2 = x * x;
        double term = x;
        double sum = x;
        for (int n = 3; n < 40; n += 2) {
            term *= x2 / ((n - 1) * n);
            double next = sum + term;
            if (next == sum) {
                break;
            }
            sum = next;
        }
        return sum;
    }

    /// The hyperbolic cosine of `x`.
    public static double cosh(double x) {
        if (x != x) {
            return x;
        }
        if (Math.abs(x) > 22) {
            return halfExp(Math.abs(x));
        }
        return (Math.exp(x) + Math.exp(-x)) / 2;
    }

    /// The hyperbolic tangent of `x`.
    public static double tanh(double x) {
        if (x != x || x == 0) {
            return x;
        }
        if (x > 20) {
            return 1;
        }
        if (x < -20) {
            return -1;
        }
        if (Math.abs(x) < 1) {
            // Lambert's continued fraction, tanh x = x / (1 + t), with the
            // small correction x t / (1 + t) taken off x last so its rounding
            // stays small against the result: within 1.05 ulp of exact over
            // this range, where sinh / sqrt(1 + sinh^2) reached 4.
            double x2 = x * x;
            double d = 29;
            for (int k = 14; k >= 2; k--) {
                d = (2 * k - 1) + x2 / d;
            }
            double t = x2 / d;
            return x - x * t / (1 + t);
        }
        double e = Math.exp(2 * x);
        return (e - 1) / (e + 1);
    }

    /// The largest int not greater than `x / y`.
    public static int floorDiv(int x, int y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (y == -1) {
            return -x;
        }
        int r = x / y;
        return (x ^ y) < 0 && r * y != x ? r - 1 : r;
    }

    /// The largest long not greater than `x / y`.
    public static long floorDiv(long x, int y) {
        return floorDiv(x, (long) y);
    }

    /// The largest long not greater than `x / y`.
    public static long floorDiv(long x, long y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (y == -1) {
            return -x;
        }
        long r = x / y;
        return (x ^ y) < 0 && r * y != x ? r - 1 : r;
    }

    /// `x - floorDiv(x, y) * y`: the remainder with the sign of `y`.
    public static int floorMod(int x, int y) {
        return x - floorDiv(x, y) * y;
    }

    /// `x - floorDiv(x, y) * y`: the remainder with the sign of `y`.
    public static int floorMod(long x, int y) {
        return (int) floorMod(x, (long) y);
    }

    /// `x - floorDiv(x, y) * y`: the remainder with the sign of `y`.
    public static long floorMod(long x, long y) {
        return x - floorDiv(x, y) * y;
    }

    /// The smallest int not less than `x / y`.
    public static int ceilDiv(int x, int y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (y == -1) {
            return -x;
        }
        int r = x / y;
        return (x ^ y) >= 0 && r * y != x ? r + 1 : r;
    }

    /// The smallest long not less than `x / y`.
    public static long ceilDiv(long x, int y) {
        return ceilDiv(x, (long) y);
    }

    /// The smallest long not less than `x / y`.
    public static long ceilDiv(long x, long y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (y == -1) {
            return -x;
        }
        long r = x / y;
        return (x ^ y) >= 0 && r * y != x ? r + 1 : r;
    }

    /// `x - ceilDiv(x, y) * y`.
    public static int ceilMod(int x, int y) {
        return x - ceilDiv(x, y) * y;
    }

    /// `x - ceilDiv(x, y) * y`.
    public static int ceilMod(long x, int y) {
        return (int) ceilMod(x, (long) y);
    }

    /// `x - ceilDiv(x, y) * y`.
    public static long ceilMod(long x, long y) {
        return x - ceilDiv(x, y) * y;
    }

    /// `x / y`, throwing on the one quotient that overflows.
    public static int divideExact(int x, int y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (x == Integer.MIN_VALUE && y == -1) {
            throw new ArithmeticException("integer overflow");
        }
        return x / y;
    }

    /// `x / y`, throwing on the one quotient that overflows.
    public static long divideExact(long x, long y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (x == Long.MIN_VALUE && y == -1) {
            throw new ArithmeticException("long overflow");
        }
        return x / y;
    }

    /// `floorDiv(x, y)`, throwing on the one quotient that overflows.
    public static int floorDivExact(int x, int y) {
        if (x == Integer.MIN_VALUE && y == -1) {
            throw new ArithmeticException("integer overflow");
        }
        return floorDiv(x, y);
    }

    /// `floorDiv(x, y)`, throwing on the one quotient that overflows.
    public static long floorDivExact(long x, long y) {
        if (x == Long.MIN_VALUE && y == -1) {
            throw new ArithmeticException("long overflow");
        }
        return floorDiv(x, y);
    }

    /// `ceilDiv(x, y)`, throwing on the one quotient that overflows.
    public static int ceilDivExact(int x, int y) {
        if (x == Integer.MIN_VALUE && y == -1) {
            throw new ArithmeticException("integer overflow");
        }
        return ceilDiv(x, y);
    }

    /// `ceilDiv(x, y)`, throwing on the one quotient that overflows.
    public static long ceilDivExact(long x, long y) {
        if (x == Long.MIN_VALUE && y == -1) {
            throw new ArithmeticException("long overflow");
        }
        return ceilDiv(x, y);
    }

    /// `x + y`, throwing when it overflows.
    public static int addExact(int x, int y) {
        int r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    /// `x + y`, throwing when it overflows.
    public static long addExact(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /// `x - y`, throwing when it overflows.
    public static int subtractExact(int x, int y) {
        int r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    /// `x - y`, throwing when it overflows.
    public static long subtractExact(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /// `x * y`, throwing when it overflows.
    public static int multiplyExact(int x, int y) {
        long r = (long) x * (long) y;
        if ((int) r != r) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) r;
    }

    /// `x * y`, throwing when it overflows.
    public static long multiplyExact(long x, int y) {
        return multiplyExact(x, (long) y);
    }

    /// `x * y`, throwing when it overflows.
    public static long multiplyExact(long x, long y) {
        if (x == 0 || y == 0) {
            return 0;
        }
        if (x == Long.MIN_VALUE && y == -1 || y == Long.MIN_VALUE && x == -1) {
            throw new ArithmeticException("long overflow");
        }
        long r = x * y;
        if (r / y != x) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /// The exact product of two ints.
    public static long multiplyFull(int x, int y) {
        return (long) x * (long) y;
    }

    /// The high 64 bits of the 128-bit product of two signed longs.
    public static long multiplyHigh(long x, long y) {
        long x1 = x >> 32;
        long x2 = x & 0xffffffffL;
        long y1 = y >> 32;
        long y2 = y & 0xffffffffL;
        long z2 = x2 * y2;
        long t = x1 * y2 + (z2 >>> 32);
        long z1 = t & 0xffffffffL;
        long z0 = t >> 32;
        z1 += x2 * y1;
        return x1 * y1 + z0 + (z1 >> 32);
    }

    /// The high 64 bits of the 128-bit product of two unsigned longs.
    public static long unsignedMultiplyHigh(long x, long y) {
        long result = multiplyHigh(x, y);
        result += y & (x >> 63);
        result += x & (y >> 63);
        return result;
    }

    /// `a + 1`, throwing when it overflows.
    public static int incrementExact(int a) {
        return addExact(a, 1);
    }

    /// `a + 1`, throwing when it overflows.
    public static long incrementExact(long a) {
        return addExact(a, 1L);
    }

    /// `a - 1`, throwing when it overflows.
    public static int decrementExact(int a) {
        return subtractExact(a, 1);
    }

    /// `a - 1`, throwing when it overflows.
    public static long decrementExact(long a) {
        return subtractExact(a, 1L);
    }

    /// `-a`, throwing when it overflows.
    public static int negateExact(int a) {
        return subtractExact(0, a);
    }

    /// `-a`, throwing when it overflows.
    public static long negateExact(long a) {
        return subtractExact(0L, a);
    }

    /// The value as an int, throwing when it does not fit.
    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    /// The absolute value, throwing for `Integer.MIN_VALUE`, which has none.
    public static int absExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Integer.MIN_VALUE");
        }
        return abs(a);
    }

    /// The absolute value, throwing for `Long.MIN_VALUE`, which has none.
    public static long absExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("Overflow to represent absolute value of Long.MIN_VALUE");
        }
        return abs(a);
    }

    /// `value` clamped to `[min, max]`, as an int.
    public static int clamp(long value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return (int) Math.min(max, Math.max(value, min));
    }

    /// `value` clamped to `[min, max]`.
    public static long clamp(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return Math.min(max, Math.max(value, min));
    }

    /// `value` clamped to `[min, max]`.
    public static double clamp(double value, double min, double max) {
        if (!(min < max)) {
            if (min != min) {
                throw new IllegalArgumentException("min is NaN");
            }
            if (max != max) {
                throw new IllegalArgumentException("max is NaN");
            }
            if (Double.compare(min, max) > 0) {
                throw new IllegalArgumentException(min + " > " + max);
            }
        }
        return Math.min(max, Math.max(value, min));
    }

    /// `value` clamped to `[min, max]`.
    public static float clamp(float value, float min, float max) {
        if (!(min < max)) {
            if (min != min) {
                throw new IllegalArgumentException("min is NaN");
            }
            if (max != max) {
                throw new IllegalArgumentException("max is NaN");
            }
            if (Float.compare(min, max) > 0) {
                throw new IllegalArgumentException(min + " > " + max);
            }
        }
        return Math.min(max, Math.max(value, min));
    }
}
