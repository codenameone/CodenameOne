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
package com.codename1.unitycompat.system;

/// What translated code calls for the things .NET does and the JVM, or one
/// of the VMs Codename One runs on, does not: unsigned arithmetic, checked
/// unboxing, the mapping from VM exceptions to .NET ones.
///
/// Everything here is written against the smallest `java.lang` the targets
/// share, which is why there is no `Integer.divideUnsigned` or
/// `Long.toUnsignedString`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Interop {
    private Interop() {
    }

    // ------------------------------------------------------------ exceptions

    /// Turns what the VM threw into what C# catches. A translated `catch`
    /// sees the result; a clause that does not match rethrows the original.
    public static Throwable wrap(Throwable t) {
        if (t instanceof Exception) {
            return t;
        }
        if (t instanceof NullPointerException) {
            return new NullReferenceException();
        }
        if (t instanceof java.lang.ArithmeticException) {
            return new DivideByZeroException();
        }
        if (t instanceof IndexOutOfBoundsException) {
            return new IndexOutOfRangeException();
        }
        if (t instanceof ClassCastException) {
            return new InvalidCastException();
        }
        if (t instanceof NegativeArraySizeException) {
            return new OverflowException();
        }
        return t;
    }

    /// Reached when a `castclass` fails. Explicit because ParparVM does not
    /// check casts.
    public static void invalidCast() {
        throw new InvalidCastException();
    }

    // Translated code reaches the four methods below only when a signed
    // divisor is 0 or -1 -- one test in line decides that, see the
    // translator's `MethodTranslator.divide` -- so every other division is
    // the bare instruction. They never run the instruction on the smallest
    // value and -1: that pair has no answer the type holds, a JVM quietly
    // answers the smallest value again, and in the C ParparVM generates it is
    // undefined behaviour, a SIGFPE on x86. CIL's `div` throws there, and
    // .NET's `rem` does too -- C# promises `x % y` throws wherever `x / y`
    // would -- which is what `dotnet run` of the console sample prints.

    /// `a / b` for a `b` that is 0 or -1.
    public static int divideEdge(int a, int b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        if (a == Integer.MIN_VALUE) {
            throw new OverflowException("Arithmetic operation resulted in an overflow.");
        }
        return -a;
    }

    /// `a % b` for a `b` that is 0 or -1.
    public static int remainderEdge(int a, int b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        if (a == Integer.MIN_VALUE) {
            throw new OverflowException("Arithmetic operation resulted in an overflow.");
        }
        return 0;
    }

    /// `a / b` for a `b` that is 0 or -1.
    public static long divideEdge(long a, long b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        if (a == Long.MIN_VALUE) {
            throw new OverflowException("Arithmetic operation resulted in an overflow.");
        }
        return -a;
    }

    /// `a % b` for a `b` that is 0 or -1.
    public static long remainderEdge(long a, long b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        if (a == Long.MIN_VALUE) {
            throw new OverflowException("Arithmetic operation resulted in an overflow.");
        }
        return 0;
    }

    // -------------------------------------------------------------- unboxing

    private static Object boxed(Object o) {
        if (o == null) {
            throw new NullReferenceException();
        }
        return o;
    }

    public static boolean unboxBoolean(Object o) {
        if (boxed(o) instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        throw new InvalidCastException();
    }

    public static char unboxChar(Object o) {
        if (boxed(o) instanceof Character) {
            return ((Character) o).charValue();
        }
        throw new InvalidCastException();
    }

    public static byte unboxByte(Object o) {
        if (boxed(o) instanceof Byte) {
            return ((Byte) o).byteValue();
        }
        if (o instanceof EnumBox) {
            return (byte) ((EnumBox) o).value;
        }
        throw new InvalidCastException();
    }

    public static short unboxShort(Object o) {
        if (boxed(o) instanceof Short) {
            return ((Short) o).shortValue();
        }
        if (o instanceof EnumBox) {
            return (short) ((EnumBox) o).value;
        }
        throw new InvalidCastException();
    }

    public static int unboxInt(Object o) {
        if (boxed(o) instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof EnumBox) {
            return (int) ((EnumBox) o).value;
        }
        throw new InvalidCastException();
    }

    public static long unboxLong(Object o) {
        if (boxed(o) instanceof Long) {
            return ((Long) o).longValue();
        }
        if (o instanceof EnumBox) {
            return (long) ((EnumBox) o).value;
        }
        throw new InvalidCastException();
    }

    public static float unboxFloat(Object o) {
        if (boxed(o) instanceof Float) {
            return ((Float) o).floatValue();
        }
        throw new InvalidCastException();
    }

    public static double unboxDouble(Object o) {
        if (boxed(o) instanceof Double) {
            return ((Double) o).doubleValue();
        }
        throw new InvalidCastException();
    }

    // -------------------------------------------------------------- unsigned

    public static int compareUnsigned(int a, int b) {
        int x = a ^ 0x80000000;
        int y = b ^ 0x80000000;
        return x < y ? -1 : x > y ? 1 : 0;
    }

    public static int compareUnsigned(long a, long b) {
        long x = a ^ 0x8000000000000000L;
        long y = b ^ 0x8000000000000000L;
        return x < y ? -1 : x > y ? 1 : 0;
    }

    public static int divideUnsigned(int a, int b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        return (int) ((a & 0xFFFFFFFFL) / (b & 0xFFFFFFFFL));
    }

    public static int remainderUnsigned(int a, int b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        return (int) ((a & 0xFFFFFFFFL) % (b & 0xFFFFFFFFL));
    }

    public static long divideUnsigned(long a, long b) {
        if (b == 0) {
            throw new DivideByZeroException();
        }
        if (b < 0) {
            // The divisor has its top bit set: the quotient is 0 or 1.
            return compareUnsigned(a, b) < 0 ? 0 : 1;
        }
        if (a >= 0) {
            return a / b;
        }
        // Halve the dividend to make it signed-positive, then correct.
        long q = ((a >>> 1) / b) << 1;
        long r = a - q * b;
        return compareUnsigned(r, b) >= 0 ? q + 1 : q;
    }

    public static long remainderUnsigned(long a, long b) {
        return a - divideUnsigned(a, b) * b;
    }

    public static double unsignedToDouble(long v) {
        if (v >= 0) {
            return (double) v;
        }
        // Halve, keeping the lost bit so the rounding still sees it.
        return ((double) ((v >>> 1) | (v & 1))) * 2.0;
    }

    public static String unsignedToString(long v) {
        if (v >= 0) {
            return Long.toString(v);
        }
        long q = divideUnsigned(v, 10);
        return Long.toString(q) + (v - q * 10);
    }

    // ------------------------------------------------------------ formatting

    public static String format(float v) {
        return shortest(Float.toString(v), 7);
    }

    public static String format(double v) {
        return shortest(Double.toString(v), 15);
    }

    /// Rewrites Java's shortest round-trip form as .NET's. The digits are
    /// the same; what differs is where each switches to an exponent, how the
    /// exponent is written, and that .NET drops a zero fraction.
    private static String shortest(String java, int maxExponent) {
        if ("NaN".equals(java)) {
            return "NaN";
        }
        if ("Infinity".equals(java)) {
            return "Infinity";
        }
        if ("-Infinity".equals(java)) {
            return "-Infinity";
        }
        boolean negative = java.charAt(0) == '-';
        String s = negative ? java.substring(1) : java;
        int exponent = 0;
        int e = s.indexOf('E');
        if (e >= 0) {
            exponent = Integer.parseInt(s.substring(e + 1));
            s = s.substring(0, e);
        }
        int dot = s.indexOf('.');
        String digits = s.substring(0, dot) + s.substring(dot + 1);
        // Position of the decimal point within `digits`.
        int point = dot + exponent;
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
            point--;
        }
        int end = digits.length();
        while (end > start + 1 && digits.charAt(end - 1) == '0') {
            end--;
        }
        digits = digits.substring(start, end);
        if ("0".equals(digits)) {
            return negative ? "-0" : "0";
        }
        StringBuilder out = new StringBuilder();
        if (negative) {
            out.append('-');
        }
        int scientific = point - 1;
        if (scientific >= maxExponent || scientific < -5) {
            out.append(digits.charAt(0));
            if (digits.length() > 1) {
                out.append('.').append(digits.substring(1));
            }
            out.append('E').append(scientific < 0 ? '-' : '+');
            int magnitude = scientific < 0 ? -scientific : scientific;
            if (magnitude < 10) {
                out.append('0');
            }
            out.append(magnitude);
        } else if (point <= 0) {
            out.append("0.");
            for (int i = point; i < 0; i++) {
                out.append('0');
            }
            out.append(digits);
        } else if (point >= digits.length()) {
            out.append(digits);
            for (int i = digits.length(); i < point; i++) {
                out.append('0');
            }
        } else {
            out.append(digits.substring(0, point)).append('.').append(digits.substring(point));
        }
        return out.toString();
    }

    // ------------------------------------------------- comparison, equality

    /// `IComparable.CompareTo` and `IComparable<T>.CompareTo`. A string or a
    /// boxed primitive implements both on .NET and is a JDK class here.
    public static int CompareTo(Object a, Object b) {
        if (a instanceof IComparable_1) {
            return ((IComparable_1) a).CompareTo(b);
        }
        if (a instanceof IComparable) {
            return ((IComparable) a).CompareTo(b);
        }
        if (b == null) {
            return 1;
        }
        if (a instanceof Integer) {
            return Int32_.CompareTo(((Integer) a).intValue(), ((Integer) b).intValue());
        }
        if (a instanceof String) {
            int c = ((String) a).compareTo((String) b);
            return c < 0 ? -1 : c > 0 ? 1 : 0;
        }
        if (a instanceof Float) {
            return Single_.CompareTo(((Float) a).floatValue(), ((Float) b).floatValue());
        }
        if (a instanceof Double) {
            return Double_.CompareTo(((Double) a).doubleValue(), ((Double) b).doubleValue());
        }
        if (a instanceof Long) {
            return Int64_.CompareTo(((Long) a).longValue(), ((Long) b).longValue());
        }
        if (a instanceof Character) {
            return ((Character) a).charValue() - ((Character) b).charValue();
        }
        if (a instanceof Short) {
            return ((Short) a).shortValue() - ((Short) b).shortValue();
        }
        if (a instanceof Byte) {
            return ((Byte) a).byteValue() - ((Byte) b).byteValue();
        }
        if (a instanceof Boolean) {
            return Boolean_.CompareTo(((Boolean) a).booleanValue(), ((Boolean) b).booleanValue());
        }
        if (a == null) {
            throw new NullReferenceException();
        }
        throw new InvalidCastException();
    }

    /// `IEquatable<T>.Equals`.
    public static boolean Equals(Object a, Object b) {
        if (a instanceof IEquatable_1) {
            return ((IEquatable_1) a).Equals(b);
        }
        if (a == null) {
            throw new NullReferenceException();
        }
        return a.equals(b);
    }

    public static boolean areEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b)); // NOPMD CompareObjectsWithEquals
    }

    public static int hash(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    // ------------------------------------------------------ erased arrays

    /// Moves the elements of the `Object[]` a generic member returned into
    /// the array of the element type the caller was promised. See the
    /// translator's `arrayFromErased`.
    public static Object[] copyArray(Object[] from, Object[] to) {
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }

    public static int[] toIntArray(Object[] from) {
        int[] to = new int[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxInt(from[i]);
        }
        return to;
    }

    public static float[] toFloatArray(Object[] from) {
        float[] to = new float[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxFloat(from[i]);
        }
        return to;
    }

    public static double[] toDoubleArray(Object[] from) {
        double[] to = new double[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxDouble(from[i]);
        }
        return to;
    }

    public static long[] toLongArray(Object[] from) {
        long[] to = new long[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxLong(from[i]);
        }
        return to;
    }

    public static boolean[] toBooleanArray(Object[] from) {
        boolean[] to = new boolean[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxBoolean(from[i]);
        }
        return to;
    }

    public static char[] toCharArray(Object[] from) {
        char[] to = new char[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxChar(from[i]);
        }
        return to;
    }

    public static byte[] toByteArray(Object[] from) {
        byte[] to = new byte[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxByte(from[i]);
        }
        return to;
    }

    public static short[] toShortArray(Object[] from) {
        short[] to = new short[from.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = unboxShort(from[i]);
        }
        return to;
    }
}
