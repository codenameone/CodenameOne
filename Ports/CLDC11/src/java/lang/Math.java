/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.lang;
/// The class Math contains methods for performing basic numeric operations.
/// Since: JDK1.0, CLDC 1.0
public final class Math{
    /// The double value that is closer than any other to e, the base of the natural logarithms.
    /// Since: CLDC 1.1 See Also:Constant Field Values
    public static final double E=2.718281828459045d;

    /// The double value that is closer than any other to
    /// , the ratio of the circumference of a circle to its diameter.
    /// Since: CLDC 1.1 See Also:Constant Field Values
    public static final double PI=3.141592653589793d;

    /// Returns the absolute value of a double value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned. Special cases: If the argument is positive zero or negative zero, the result is positive zero. If the argument is infinite, the result is positive infinity. If the argument is NaN, the result is NaN. In other words, the result is equal to the value of the expression:
    /// Double.longBitsToDouble((Double.doubleToLongBits(a)>>1)
    public static double abs(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the absolute value of a float value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned. Special cases: If the argument is positive zero or negative zero, the result is positive zero. If the argument is infinite, the result is positive infinity. If the argument is NaN, the result is NaN. In other words, the result is equal to the value of the expression:
    /// Float.intBitsToFloat(0x7fffffff & Float.floatToIntBits(a))
    public static float abs(float a){
        return 0.0f; //TODO codavaj!!
    }

    /// Returns the absolute value of an int value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.
    /// Note that if the argument is equal to the value of Integer.MIN_VALUE, the most negative representable int value, the result is that same value, which is negative.
    public static int abs(int a){
        return 0; //TODO codavaj!!
    }

    /// Returns the absolute value of a long value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.
    /// Note that if the argument is equal to the value of Long.MIN_VALUE, the most negative representable long value, the result is that same value, which is negative.
    public static long abs(long a){
        return 0l; //TODO codavaj!!
    }

    /// Returns the smallest (closest to negative infinity) double value that is not less than the argument and is equal to a mathematical integer. Special cases: If the argument value is already equal to a mathematical integer, then the result is the same as the argument. If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument. If the argument value is less than zero but greater than -1.0, then the result is negative zero. Note that the value of Math.ceil(x) is exactly the value of -Math.floor(-x).
    public static double ceil(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the trigonometric cosine of an angle. Special case: If the argument is NaN or an infinity, then the result is NaN.
    public static double cos(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the largest (closest to positive infinity) double value that is not greater than the argument and is equal to a mathematical integer. Special cases: If the argument value is already equal to a mathematical integer, then the result is the same as the argument. If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument.
    public static double floor(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the greater of two double values. That is, the result is the argument closer to positive infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other negative zero, the result is positive zero.
    public static double max(double a, double b){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the greater of two float values. That is, the result is the argument closer to positive infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other negative zero, the result is positive zero.
    public static float max(float a, float b){
        return 0.0f; //TODO codavaj!!
    }

    /// Returns the greater of two int values. That is, the result is the argument closer to the value of Integer.MAX_VALUE. If the arguments have the same value, the result is that same value.
    public static int max(int a, int b){
        return 0; //TODO codavaj!!
    }

    /// Returns the greater of two long values. That is, the result is the argument closer to the value of Long.MAX_VALUE. If the arguments have the same value, the result is that same value.
    public static long max(long a, long b){
        return 0l; //TODO codavaj!!
    }

    /// Returns the smaller of two double values. That is, the result is the value closer to negative infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other is negative zero, the result is negative zero.
    public static double min(double a, double b){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the smaller of two float values. That is, the result is the value closer to negative infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other is negative zero, the result is negative zero.
    public static float min(float a, float b){
        return 0.0f; //TODO codavaj!!
    }

    /// Returns the smaller of two int values. That is, the result the argument closer to the value of Integer.MIN_VALUE. If the arguments have the same value, the result is that same value.
    public static int min(int a, int b){
        return 0; //TODO codavaj!!
    }

    /// Returns the smaller of two long values. That is, the result is the argument closer to the value of Long.MIN_VALUE. If the arguments have the same value, the result is that same value.
    public static long min(long a, long b){
        return 0l; //TODO codavaj!!
    }

    /// Returns the trigonometric sine of an angle. Special cases: If the argument is NaN or an infinity, then the result is NaN. If the argument is positive zero, then the result is positive zero; if the argument is negative zero, then the result is negative zero.
    public static double sin(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the correctly rounded positive square root of a double value. Special cases: If the argument is NaN or less than zero, then the result is NaN. If the argument is positive infinity, then the result is positive infinity. If the argument is positive zero or negative zero, then the result is the same as the argument.
    public static double sqrt(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the trigonometric tangent of an angle. Special cases: If the argument is NaN or an infinity, then the result is NaN. If the argument is positive zero, then the result is positive zero; if the argument is negative zero, then the result is negative zero
    public static double tan(double a){
        return 0.0d; //TODO codavaj!!
    }

    /// Converts an angle measured in radians to the equivalent angle measured in degrees.
    public static double toDegrees(double angrad){
        return 0.0d; //TODO codavaj!!
    }

    /// Converts an angle measured in degrees to the equivalent angle measured in radians.
    public static double toRadians(double angdeg){
        return 0.0d; //TODO codavaj!!
    }

    /// Returns the result of rounding the argument to an integer. The result is
    /// equivalent to `(long) Math.floor(d+0.5)`.
    ///
    /// Special cases:
    ///
    /// - `round(+0.0) = (long) +0.0`
    ///
    /// - `round(-0.0) = (long) +0.0`
    ///
    /// - `round((anything > Long.MAX_VALUE) = Long.MAX_VALUE`
    ///
    /// - `round((anything < Long.MIN_VALUE) = Long.MIN_VALUE`
    ///
    /// - `round(+infinity) = Long.MAX_VALUE`
    ///
    /// - `round(-infinity) = Long.MIN_VALUE`
    ///
    /// - `round(NaN) = (long) +0.0`
    ///
    /// #### Parameters
    ///
    /// - `d`: the value to be rounded.
    ///
    /// #### Returns
    ///
    /// the closest integer to the argument.
    public static long round(double d) {
        // check for NaN
        if (d != d) {
            return 0L;
        }
        return (long) floor(d + 0.5d);
    }

    /// Returns the result of rounding the argument to an integer. The result is
    /// equivalent to `(int) Math.floor(f+0.5)`.
    ///
    /// Special cases:
    ///
    /// - `round(+0.0) = (int) +0.0`
    ///
    /// - `round(-0.0) = (int) +0.0`
    ///
    /// - `round((anything > Integer.MAX_VALUE) = Integer.MAX_VALUE`
    ///
    /// - `round((anything < Integer.MIN_VALUE) = Integer.MIN_VALUE`
    ///
    /// - `round(+infinity) = Integer.MAX_VALUE`
    ///
    /// - `round(-infinity) = Integer.MIN_VALUE`
    ///
    /// - `round(NaN) = (int) +0.0`
    ///
    /// #### Parameters
    ///
    /// - `f`: the value to be rounded.
    ///
    /// #### Returns
    ///
    /// the closest integer to the argument.
    public static int round(float f) {
        // check for NaN
        if (f != f) {
            return 0;
        }
        return (int) floor(f + 0.5f);
    }
    
    public static double nextDown(double d) {
        if (Double.isNaN(d)) {
            return d;
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return d;
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
    
    
    public static double nextAfter(double start, double direction) {
        return 0;
    }

    public static float nextAfter(float start, double direction) {
        return 0;
    }

    /// Twice pi, the ratio of a circle's circumference to its radius.
    public static final double TAU = 2.0 * PI;

    /// A pseudo-random double in `[0, 1)`.
    public static double random() {
        return 0;
    }

    /// -1.0, 0.0 or 1.0 by the sign of `d`; a zero or NaN is returned as it is.
    public static double signum(double d) {
        return 0;
    }

    /// -1.0f, 0.0f or 1.0f by the sign of `f`; a zero or NaN is returned as it is.
    public static float signum(float f) {
        return 0;
    }

    /// `magnitude` with the sign bit of `sign`.
    public static double copySign(double magnitude, double sign) {
        return 0;
    }

    /// `magnitude` with the sign bit of `sign`.
    public static float copySign(float magnitude, float sign) {
        return 0;
    }

    /// The unbiased exponent of `d`: 1024 for NaN and the infinities, -1023
    /// for zero and the subnormals.
    public static int getExponent(double d) {
        return 0;
    }

    /// The unbiased exponent of `f`: 128 for NaN and the infinities, -127 for
    /// zero and the subnormals.
    public static int getExponent(float f) {
        return 0;
    }

    /// The distance from `d` to the next double of larger magnitude.
    public static double ulp(double d) {
        return 0;
    }

    /// The distance from `f` to the next float of larger magnitude.
    public static float ulp(float f) {
        return 0;
    }

    /// The double next to `d` towards positive infinity.
    public static double nextUp(double d) {
        return 0;
    }

    /// The float next to `f` towards positive infinity.
    public static float nextUp(float f) {
        return 0;
    }

    /// The float next to `f` towards negative infinity.
    public static float nextDown(float f) {
        return 0;
    }

    /// `d` times 2 to the power `scaleFactor`, rounded once.
    public static double scalb(double d, int scaleFactor) {
        return 0;
    }

    /// `f` times 2 to the power `scaleFactor`, rounded once.
    public static float scalb(float f, int scaleFactor) {
        return 0;
    }

    /// The square root of `x * x + y * y`, without overflow in between.
    public static double hypot(double x, double y) {
        return 0;
    }

    /// The cube root of `a`.
    public static double cbrt(double a) {
        return 0;
    }

    /// The whole number nearest `a`; a tie goes to the even one.
    public static double rint(double a) {
        return 0;
    }

    /// The hyperbolic sine of `x`.
    public static double sinh(double x) {
        return 0;
    }

    /// The hyperbolic cosine of `x`.
    public static double cosh(double x) {
        return 0;
    }

    /// The hyperbolic tangent of `x`.
    public static double tanh(double x) {
        return 0;
    }

    /// The largest int not greater than `x / y`.
    public static int floorDiv(int x, int y) {
        return 0;
    }

    /// The largest long not greater than `x / y`.
    public static long floorDiv(long x, int y) {
        return 0;
    }

    /// The largest long not greater than `x / y`.
    public static long floorDiv(long x, long y) {
        return 0;
    }

    /// `x - floorDiv(x, y) * y`: the remainder with the sign of `y`.
    public static int floorMod(int x, int y) {
        return 0;
    }

    /// `x - floorDiv(x, y) * y`: the remainder with the sign of `y`.
    public static int floorMod(long x, int y) {
        return 0;
    }

    /// `x - floorDiv(x, y) * y`: the remainder with the sign of `y`.
    public static long floorMod(long x, long y) {
        return 0;
    }

    /// The smallest int not less than `x / y`.
    public static int ceilDiv(int x, int y) {
        return 0;
    }

    /// The smallest long not less than `x / y`.
    public static long ceilDiv(long x, int y) {
        return 0;
    }

    /// The smallest long not less than `x / y`.
    public static long ceilDiv(long x, long y) {
        return 0;
    }

    /// `x - ceilDiv(x, y) * y`.
    public static int ceilMod(int x, int y) {
        return 0;
    }

    /// `x - ceilDiv(x, y) * y`.
    public static int ceilMod(long x, int y) {
        return 0;
    }

    /// `x - ceilDiv(x, y) * y`.
    public static long ceilMod(long x, long y) {
        return 0;
    }

    /// `x / y`, throwing on the one quotient that overflows.
    public static int divideExact(int x, int y) {
        return 0;
    }

    /// `x / y`, throwing on the one quotient that overflows.
    public static long divideExact(long x, long y) {
        return 0;
    }

    /// `floorDiv(x, y)`, throwing on the one quotient that overflows.
    public static int floorDivExact(int x, int y) {
        return 0;
    }

    /// `floorDiv(x, y)`, throwing on the one quotient that overflows.
    public static long floorDivExact(long x, long y) {
        return 0;
    }

    /// `ceilDiv(x, y)`, throwing on the one quotient that overflows.
    public static int ceilDivExact(int x, int y) {
        return 0;
    }

    /// `ceilDiv(x, y)`, throwing on the one quotient that overflows.
    public static long ceilDivExact(long x, long y) {
        return 0;
    }

    /// `x + y`, throwing when it overflows.
    public static int addExact(int x, int y) {
        return 0;
    }

    /// `x + y`, throwing when it overflows.
    public static long addExact(long x, long y) {
        return 0;
    }

    /// `x - y`, throwing when it overflows.
    public static int subtractExact(int x, int y) {
        return 0;
    }

    /// `x - y`, throwing when it overflows.
    public static long subtractExact(long x, long y) {
        return 0;
    }

    /// `x * y`, throwing when it overflows.
    public static int multiplyExact(int x, int y) {
        return 0;
    }

    /// `x * y`, throwing when it overflows.
    public static long multiplyExact(long x, int y) {
        return 0;
    }

    /// `x * y`, throwing when it overflows.
    public static long multiplyExact(long x, long y) {
        return 0;
    }

    /// The exact product of two ints.
    public static long multiplyFull(int x, int y) {
        return 0;
    }

    /// The high 64 bits of the 128-bit product of two signed longs.
    public static long multiplyHigh(long x, long y) {
        return 0;
    }

    /// The high 64 bits of the 128-bit product of two unsigned longs.
    public static long unsignedMultiplyHigh(long x, long y) {
        return 0;
    }

    /// `a + 1`, throwing when it overflows.
    public static int incrementExact(int a) {
        return 0;
    }

    /// `a + 1`, throwing when it overflows.
    public static long incrementExact(long a) {
        return 0;
    }

    /// `a - 1`, throwing when it overflows.
    public static int decrementExact(int a) {
        return 0;
    }

    /// `a - 1`, throwing when it overflows.
    public static long decrementExact(long a) {
        return 0;
    }

    /// `-a`, throwing when it overflows.
    public static int negateExact(int a) {
        return 0;
    }

    /// `-a`, throwing when it overflows.
    public static long negateExact(long a) {
        return 0;
    }

    /// The value as an int, throwing when it does not fit.
    public static int toIntExact(long value) {
        return 0;
    }

    /// The absolute value, throwing for `Integer.MIN_VALUE`, which has none.
    public static int absExact(int a) {
        return 0;
    }

    /// The absolute value, throwing for `Long.MIN_VALUE`, which has none.
    public static long absExact(long a) {
        return 0;
    }

    /// `value` clamped to `[min, max]`, as an int.
    public static int clamp(long value, int min, int max) {
        return 0;
    }

    /// `value` clamped to `[min, max]`.
    public static long clamp(long value, long min, long max) {
        return 0;
    }

    /// `value` clamped to `[min, max]`.
    public static double clamp(double value, double min, double max) {
        return 0;
    }

    /// `value` clamped to `[min, max]`.
    public static float clamp(float value, float min, float max) {
        return 0;
    }
}
