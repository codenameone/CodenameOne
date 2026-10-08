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
package com.codename1.compat.jdk;

import com.codename1.util.MathUtil;

import java.util.Random;

/// The members of the number wrappers, `Boolean` and `Math` a desktop
/// application names and the device's classes do not have.
///
/// The build's remap step redirects each such call here. Where two wrappers
/// have a method of one name and one parameter list -- `Long.decode` and
/// `Integer.decode` -- the method here carries the type in its name, and the
/// rule names it.
///
/// Integer division by zero does not throw on every device, so `floorDiv`
/// and `floorMod` test their divisor themselves.
public final class JdkNumbers {

    private static final Random RANDOM = new Random();

    private JdkNumbers() {
    }

    // ---- Integer ----

    public static int max(int a, int b) {
        return a >= b ? a : b;
    }

    public static int min(int a, int b) {
        return a <= b ? a : b;
    }

    public static int sum(int a, int b) {
        return a + b;
    }

    public static int bitCount(int i) {
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        i = (i + (i >>> 4)) & 0x0f0f0f0f;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        return i & 0x3f;
    }

    public static int highestOneBit(int i) {
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i - (i >>> 1);
    }

    public static int lowestOneBit(int i) {
        return i & -i;
    }

    public static int numberOfTrailingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        int n = 0;
        while ((i & 1) == 0) {
            i >>>= 1;
            n++;
        }
        return n;
    }

    public static int reverse(int i) {
        int out = 0;
        for (int bit = 0; bit < 32; bit++) {
            out = (out << 1) | (i & 1);
            i >>>= 1;
        }
        return out;
    }

    public static int rotateLeft(int i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    public static int rotateRight(int i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    public static Integer decodeInteger(String nm) {
        long value = decode(nm);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new NumberFormatException("Value out of range: " + nm);
        }
        return Integer.valueOf((int) value);
    }

    /// The number `nm` spells with an optional sign and a `0x`, `#` or `0`
    /// radix prefix, as the wrappers' `decode` reads it.
    private static long decode(String nm) {
        if (nm.length() == 0) {
            throw new NumberFormatException("Zero length string");
        }
        int index = 0;
        boolean negative = false;
        char first = nm.charAt(0);
        if (first == '-') {
            negative = true;
            index++;
        } else if (first == '+') {
            index++;
        }
        int radix = 10;
        if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        } else if (nm.startsWith("#", index)) {
            index++;
            radix = 16;
        } else if (nm.startsWith("0", index) && nm.length() > index + 1) {
            index++;
            radix = 8;
        }
        if (nm.startsWith("-", index) || nm.startsWith("+", index)) {
            throw new NumberFormatException("Sign character in wrong position");
        }
        return Long.parseLong(negative ? "-" + nm.substring(index) : nm.substring(index), radix);
    }

    // ---- Long ----

    public static long max(long a, long b) {
        return a >= b ? a : b;
    }

    public static long min(long a, long b) {
        return a <= b ? a : b;
    }

    public static long sum(long a, long b) {
        return a + b;
    }

    public static int signum(long i) {
        return i > 0 ? 1 : i < 0 ? -1 : 0;
    }

    public static int bitCount(long i) {
        return bitCount((int) i) + bitCount((int) (i >>> 32));
    }

    public static int numberOfLeadingZeros(long i) {
        int high = (int) (i >>> 32);
        return high != 0 ? Integer.numberOfLeadingZeros(high) : 32 + Integer.numberOfLeadingZeros((int) i);
    }

    public static int numberOfTrailingZeros(long i) {
        int low = (int) i;
        return low != 0 ? numberOfTrailingZeros(low) : 32 + numberOfTrailingZeros((int) (i >>> 32));
    }

    public static long highestOneBit(long i) {
        return i == 0 ? 0 : 1L << (63 - numberOfLeadingZeros(i));
    }

    public static long lowestOneBit(long i) {
        return i & -i;
    }

    public static long reverse(long i) {
        return ((long) reverse((int) i) << 32) | (reverse((int) (i >>> 32)) & 0xffffffffL);
    }

    public static long rotateLeft(long i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    public static long rotateRight(long i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    public static Long decodeLong(String nm) {
        return Long.valueOf(decode(nm));
    }

    public static Long longValueOf(String s) {
        return Long.valueOf(Long.parseLong(s));
    }

    public static Long longValueOf(String s, int radix) {
        return Long.valueOf(Long.parseLong(s, radix));
    }

    public static String toHexString(long i) {
        return unsigned(i, 4);
    }

    public static String toOctalString(long i) {
        return unsigned(i, 3);
    }

    public static String toBinaryString(long i) {
        return unsigned(i, 1);
    }

    private static String unsigned(long value, int shift) {
        if (value == 0) {
            return "0";
        }
        char[] buf = new char[64];
        int pos = 64;
        int mask = (1 << shift) - 1;
        while (value != 0) {
            int digit = (int) value & mask;
            buf[--pos] = (char) (digit < 10 ? '0' + digit : 'a' + digit - 10);
            value >>>= shift;
        }
        return new String(buf, pos, 64 - pos);
    }

    public static short shortValue(Long value) {
        return (short) value.longValue();
    }

    // ---- Short and Byte ----

    public static Short shortValueOf(String s) {
        return shortValueOf(s, 10);
    }

    public static Short shortValueOf(String s, int radix) {
        return Short.valueOf(shortRange(Integer.parseInt(s, radix), s));
    }

    public static Short decodeShort(String nm) {
        long value = decode(nm);
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new NumberFormatException("Value out of range: " + nm);
        }
        return Short.valueOf((short) value);
    }

    private static short shortRange(int value, String s) {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new NumberFormatException("Value out of range. Value:\"" + s + "\"");
        }
        return (short) value;
    }

    public static String toString(short s) {
        return Integer.toString(s);
    }

    public static byte byteValue(Short value) {
        return (byte) value.shortValue();
    }

    public static Byte byteValueOf(String s) {
        return byteValueOf(s, 10);
    }

    public static Byte byteValueOf(String s, int radix) {
        return Byte.valueOf(byteRange(Integer.parseInt(s, radix), s));
    }

    public static Byte decodeByte(String nm) {
        return Byte.valueOf(byteRange(decode(nm), nm));
    }

    private static byte byteRange(long value, String s) {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw new NumberFormatException("Value out of range. Value:\"" + s + "\"");
        }
        return (byte) value;
    }

    public static String toString(byte b) {
        return Integer.toString(b);
    }

    public static short shortValue(Byte value) {
        return value.byteValue();
    }

    // ---- Double and Float ----

    public static boolean isFinite(double d) {
        return d == d && d != Double.POSITIVE_INFINITY && d != Double.NEGATIVE_INFINITY;
    }

    public static boolean isFinite(float f) {
        return f == f && f != Float.POSITIVE_INFINITY && f != Float.NEGATIVE_INFINITY;
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static double sum(double a, double b) {
        return a + b;
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static float sum(float a, float b) {
        return a + b;
    }

    // ---- Boolean ----

    public static String toString(boolean b) {
        return b ? "true" : "false";
    }

    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    public static boolean logicalXor(boolean a, boolean b) {
        return a != b;
    }

    /// Whether the system property `name` is "true", in any case.
    public static boolean getBoolean(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        return "true".equalsIgnoreCase(JdkSystem.getProperty(name));
    }

    // ---- Math ----

    public static double random() {
        return RANDOM.nextDouble();
    }

    public static double signum(double d) {
        return d > 0 ? 1.0 : d < 0 ? -1.0 : d;
    }

    public static float signum(float f) {
        return f > 0 ? 1.0f : f < 0 ? -1.0f : f;
    }

    public static float copySign(float magnitude, float sign) {
        return (float) MathUtil.copySign(magnitude, sign);
    }

    public static double hypot(double x, double y) {
        if (!isFinite(x) || !isFinite(y)) {
            return x != x && isFinite(y) || y != y && isFinite(x) || x != x && y != y
                    ? Double.NaN : Double.POSITIVE_INFINITY;
        }
        double a = Math.abs(x);
        double b = Math.abs(y);
        double big = a > b ? a : b;
        double small = a > b ? b : a;
        if (big == 0) {
            return 0;
        }
        // Scaled, so that neither square overflows.
        double ratio = small / big;
        return big * Math.sqrt(1 + ratio * ratio);
    }

    public static double cbrt(double a) {
        if (a == 0 || !isFinite(a)) {
            return a;
        }
        double root = MathUtil.pow(Math.abs(a), 1.0 / 3.0);
        // One Newton step takes out the error of the inexact exponent.
        root = root - (root * root * root - Math.abs(a)) / (3 * root * root);
        return a < 0 ? -root : root;
    }

    /// The whole number nearest `a`; a tie goes to the even one.
    public static double rint(double a) {
        if (!isFinite(a) || Math.abs(a) >= 4503599627370496.0) {
            return a;
        }
        double floor = Math.floor(a);
        double diff = a - floor;
        double half = floor / 2;
        double out = diff < 0.5 || diff == 0.5 && half == Math.floor(half) ? floor : floor + 1;
        // -0.2 rounds to negative zero, which prints and divides differently.
        return out == 0 && a < 0 ? -0.0 : out;
    }

    public static double sinh(double x) {
        return (MathUtil.exp(x) - MathUtil.exp(-x)) / 2;
    }

    public static double cosh(double x) {
        return (MathUtil.exp(x) + MathUtil.exp(-x)) / 2;
    }

    public static double tanh(double x) {
        if (x > 20) {
            return 1;
        }
        if (x < -20) {
            return -1;
        }
        double e = MathUtil.exp(2 * x);
        return (e - 1) / (e + 1);
    }

    public static int floorDiv(int x, int y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        int r = x / y;
        return (x ^ y) < 0 && r * y != x ? r - 1 : r;
    }

    public static int floorMod(int x, int y) {
        return x - floorDiv(x, y) * y;
    }

    public static long floorDiv(long x, long y) {
        if (y == 0) {
            throw new ArithmeticException("/ by zero");
        }
        long r = x / y;
        return (x ^ y) < 0 && r * y != x ? r - 1 : r;
    }

    public static long floorMod(long x, long y) {
        return x - floorDiv(x, y) * y;
    }

    public static int addExact(int x, int y) {
        int r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    public static long addExact(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    public static int subtractExact(int x, int y) {
        int r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    public static long subtractExact(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    public static int multiplyExact(int x, int y) {
        long r = (long) x * (long) y;
        if ((int) r != r) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) r;
    }

    public static long multiplyExact(long x, long y) {
        if (x == 0 || y == 0) {
            return 0;
        }
        long r = x * y;
        if (x == Long.MIN_VALUE && y == -1 || y == Long.MIN_VALUE && x == -1 || r / y != x) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    public static int incrementExact(int a) {
        return addExact(a, 1);
    }

    public static long incrementExact(long a) {
        return addExact(a, 1L);
    }

    public static int decrementExact(int a) {
        return subtractExact(a, 1);
    }

    public static long decrementExact(long a) {
        return subtractExact(a, 1L);
    }

    public static int negateExact(int a) {
        return subtractExact(0, a);
    }

    public static long negateExact(long a) {
        return subtractExact(0L, a);
    }

    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }
}
