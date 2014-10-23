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
     * Float.intBitsToFloat(0x7fffffff & Float.floatToIntBits(a))
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
        // check for NaN
        if (d != d) {
            return 0L;
        }
        return (long) floor(d + 0.5d);
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
        // check for NaN
        if (f != f) {
            return 0;
        }
        return (int) floor(f + 0.5f);
    }
    
}
