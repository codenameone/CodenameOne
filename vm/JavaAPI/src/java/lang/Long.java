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
 * The Long class wraps a value of the primitive type long in an object. An object of type Long contains a single field whose type is long.
 * In addition, this class provides several methods for converting a long to a String and a String to a long, as well as other constants and methods useful when dealing with a long.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Long extends Number implements Comparable<Long> {
    
    public static Class<Long> TYPE = long.class;
    
    /**
     * The largest value of type long.
     * See Also:Constant Field Values
     */
    public static final long MAX_VALUE=9223372036854775807l;

    /**
     * The smallest value of type long.
     * See Also:Constant Field Values
     */
    public static final long MIN_VALUE=-9223372036854775808l;

    private long value;
    
    /**
     * Constructs a newly allocated Long object that represents the primitive long argument.
     * value - the value to be represented by the Long object.
     */
    public Long(long value){
         this.value = value;
    }

    /**
     * Returns the value of this Long as a double.
     */
    public double doubleValue(){
        return (double)cn1Value();
    }

    /**
     * Compares this object against the specified object. The result is true if and only if the argument is not null and is a Long object that contains the same long value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && ((Long)obj).cn1Value() == cn1Value();
    }

    /**
     * Returns the value of this Long as a float.
     */
    public float floatValue(){
        return (float)cn1Value();
    }

    /**
     * Computes a hashcode for this Long. The result is the exclusive OR of the two halves of the primitive long value represented by this Long object. That is, the hashcode is the value of the expression: (int)(this.longValue()^(this.longValue()>>>32))
     */
    public int hashCode(){
        return hashCode(cn1Value());
    }

    /**
     * Returns a hash code for a {@code long} value, without boxing it
     * (Java 8's {@code Long.hashCode(long)}). The result is the exclusive OR of
     * the two halves of the value: {@code (int)(value^(value>>>32))}.
     */
    public static int hashCode(long value) {
        return (int)(value^(value>>>32));
    }

    /**
     * Returns the value of this Long as a long value.
     */
    public long longValue(){
        return cn1Value();
    }

    /**
     * Returns this Long's value, transparently handling both heap-allocated and
     * tagged-immediate representations. All value reads route here -- Long is final, so a
     * plain `return value;` getter would be inlined into a raw field load off a tagged
     * pointer, which has no fields.
     */
    private native long cn1Value();

    /**
     * Returns the value of this Long as an int value.
     */
    public int intValue() {
        return (int)cn1Value();
    }

    /**
     * Returns the value of this Long as a byte value.
     */
    public byte byteValue() {
        return (byte)cn1Value();
    }
    
    /**
     * Parses the string argument as a signed decimal long. The characters in the string must all be decimal digits, except that the first character may be an ASCII minus sign '-' (
     * u002d') to indicate a negative value. The resulting long value is returned, exactly as if the argument and the radix 10 were given as arguments to the
     * method that takes two arguments.
     * Note that neither L nor l is permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code.
     */
    public static long parseLong(java.lang.String s) throws java.lang.NumberFormatException{
        return parseLong(s, 10);
    }

    /**
     * Parses the string argument as a signed long in the radix specified by the second argument. The characters in the string must all be digits of the specified radix (as determined by whether Character.digit returns a nonnegative value), except that the first character may be an ASCII minus sign '-' ('
     * u002d' to indicate a negative value. The resulting long value is returned.
     * Note that neither L nor l is permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code - except that either L or l may appear as a digit for a radix greater than 22.
     * An exception of type NumberFormatException is thrown if any of the following situations occurs: The first argument is null or is a string of length zero. The radix is either smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX. The first character of the string is not a digit of the specified radix and is not a minus sign '-' ('u002d'). The first character of the string is a minus sign and the string is of length 1. Any character of the string after the first is not a digit of the specified radix. The integer value represented by the string cannot be represented as a value of type long.
     * Examples:
     * parseLong("0", 10) returns 0L parseLong("473", 10) returns 473L parseLong("-0", 10) returns 0L parseLong("-FF", 16) returns -255L parseLong("1100110", 2) returns 102L parseLong("99", 8) throws a NumberFormatException parseLong("Hazelnut", 10) throws a NumberFormatException parseLong("Hazelnut", 36) returns 1356099454469L
     */
    public static long parseLong(String string, int radix) throws NumberFormatException {
        if (string == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        Integer.checkRadix(radix);
        int length = string.length(), i = 0;
        if (length == 0) {
            throw invalidLong(string, radix);
        }
        boolean negative = string.charAt(i) == '-';
        if (negative && ++i == length) {
            throw invalidLong(string, radix);
        }
        boolean positive = !negative && string.charAt(i) == '+';
        if (positive && ++i == length) {
            throw invalidLong(string, radix);
        }

        return parse(string, i, radix, negative);
    }

    private static long parse(String string, int offset, int radix, boolean negative) {
        long max = Long.MIN_VALUE / radix;
        long result = 0, length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidLong(string, radix);
            }
            if (max > result) {
                throw invalidLong(string, radix);
            }
            long next = result * radix - digit;
            if (next > result) {
                throw invalidLong(string, radix);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidLong(string, radix);
            }
        }
        return result;
    }

    private static NumberFormatException invalidLong(String s, int radix) {
        return new NumberFormatException("For input string: \"" + s + "\""
                + (radix == 10 ? "" : " under radix " + radix));
    }

    /**
     * Returns a String object representing this Long's value. The long integer value represented by this Long object is converted to signed decimal representation and returned as a string, exactly as if the long value were given as an argument to the
     * method that takes one argument.
     */
    public java.lang.String toString(){
        return toString(cn1Value());
    }

    /**
     * Returns a new String object representing the specified integer. The argument is converted to signed decimal representation and returned as a string, exactly as if the argument and the radix 10 were given as arguments to the
     * method that takes two arguments.
     */
    public static java.lang.String toString(long i) {
        return toString(i, 10);
    }
    
    /**
     * Creates a string representation of the first argument in the radix specified by the second argument.
     * If the radix is smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX, then the radix 10 is used instead.
     * If the first argument is negative, the first element of the result is the ASCII minus sign '-' ('u002d'. If the first argument is not negative, no sign character appears in the result.
     * The remaining characters of the result represent the magnitude of the first argument. If the magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the magnitude will not be the zero character. The following ASCII characters are used as digits:
     * 0123456789abcdefghijklmnopqrstuvwxyz These are '
     * u0030' through '
     * u0039' and '
     * u0061' through '
     * u007a'. If the radix is N, then the first N of these characters are used as radix-N digits in the order shown. Thus, the digits for hexadecimal (radix 16) are 0123456789abcdef.
     */
    public native static java.lang.String toString(long i, int radix);

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    // Native so the tagged build can return an immediate without allocating. The off path
    // (and 32-bit-pointer targets) calls valueOfHeap, preserving the previous behaviour.
    public static native Long valueOf(long i);

    static Long valueOfHeap(long i) {
        if (i >= -128 && i <= 127) {
            return LongCache.cache[(int) i + 128];
        }
        return new Long(i);
    }

    /** Cache of boxed values for -128..127, mirroring the JDK's LongCache. */
    private static final class LongCache {
        static final Long[] cache = new Long[256];
        static {
            for (int j = 0; j < 256; j++) {
                cache[j] = new Long(j - 128);
            }
        }
        private LongCache() {}
    }

    public static int compare(long f1, long f2) {
       if (f1 > f2) return 1;
       else if (f1 < f2) return -1;
       return 0;
    }

    public int compareTo(Long another) {
        long cn1a = cn1Value();
        long cn1b = another.cn1Value();
        return cn1a < cn1b ? -1 : cn1a > cn1b ? 1 : 0;
    }

    /**
     * {@code a + b}; a method reference target for reductions ({@code Long::sum}).
     *
     * @param a an operand
     * @param b an operand
     * @return the sum
     */
    public static long sum(long a, long b) {
        return a + b;
    }

    /**
     * The greater of two values (Math.max), as a method reference target.
     *
     * @param a a value
     * @param b a value
     * @return the greater
     */
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    /**
     * The smaller of two values (Math.min), as a method reference target.
     *
     * @param a a value
     * @param b a value
     * @return the smaller
     */
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    // ---------------------------------------------------------------------
    // Members of the JDK's class this one did not have, in plain Java.
    // ---------------------------------------------------------------------

    /// The size of a long in bytes.
    public static final int BYTES = 8;

    /// The size of a long in bits.
    public static final int SIZE = 64;

    /// A Long of `parseLong(s)`.
    @Deprecated
    public Long(String s) throws NumberFormatException {
        this(parseLong(s));
    }

    /// A Long of `parseLong(s)`.
    public static Long valueOf(String s) throws NumberFormatException {
        return valueOf(parseLong(s, 10));
    }

    /// A Long of `parseLong(s, radix)`.
    public static Long valueOf(String s, int radix) throws NumberFormatException {
        return valueOf(parseLong(s, radix));
    }

    /// The number of one bits.
    public static int bitCount(long i) {
        return Integer.bitCount((int) i) + Integer.bitCount((int) (i >>> 32));
    }

    /// The highest one bit alone, or 0.
    public static long highestOneBit(long i) {
        return i == 0 ? 0 : 0x8000000000000000L >>> numberOfLeadingZeros(i);
    }

    /// The lowest one bit alone, or 0.
    public static long lowestOneBit(long i) {
        return i & -i;
    }

    /// The number of zero bits above the highest one bit; 64 for zero.
    public static int numberOfLeadingZeros(long i) {
        int high = (int) (i >>> 32);
        return high != 0 ? Integer.numberOfLeadingZeros(high) : 32 + Integer.numberOfLeadingZeros((int) i);
    }

    /// The number of zero bits below the lowest one bit; 64 for zero.
    public static int numberOfTrailingZeros(long i) {
        int low = (int) i;
        return low != 0 ? Integer.numberOfTrailingZeros(low) : 32 + Integer.numberOfTrailingZeros((int) (i >>> 32));
    }

    /// The bits in reverse order.
    public static long reverse(long i) {
        return ((long) Integer.reverse((int) i) << 32) | (Integer.reverse((int) (i >>> 32)) & 0xffffffffL);
    }

    /// The bytes in reverse order.
    public static long reverseBytes(long i) {
        return ((long) Integer.reverseBytes((int) i) << 32) | (Integer.reverseBytes((int) (i >>> 32)) & 0xffffffffL);
    }

    /// The bits rotated left by `distance`.
    public static long rotateLeft(long i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    /// The bits rotated right by `distance`.
    public static long rotateRight(long i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /// -1, 0 or 1 by the sign of `i`.
    public static int signum(long i) {
        return (int) ((i >> 63) | (-i >>> 63));
    }

    /// The value as unsigned hexadecimal digits.
    public static String toHexString(long i) {
        return toPow2String(i, 4);
    }

    /// The value as unsigned octal digits.
    public static String toOctalString(long i) {
        return toPow2String(i, 3);
    }

    /// The value as unsigned binary digits.
    public static String toBinaryString(long i) {
        return toPow2String(i, 1);
    }

    private static String toPow2String(long i, int shift) {
        char[] buf = new char[64];
        int at = 64;
        long mask = (1L << shift) - 1;
        do {
            buf[--at] = Character.forDigit((int) (i & mask), 1 << shift);
            i >>>= shift;
        } while (i != 0);
        return new String(buf, at, 64 - at);
    }

    /// Compares the two as unsigned values.
    public static int compareUnsigned(long x, long y) {
        return compare(x + MIN_VALUE, y + MIN_VALUE);
    }

    /// The unsigned quotient.
    public static long divideUnsigned(long dividend, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("/ by zero");
        }
        if (divisor < 0) {
            return compareUnsigned(dividend, divisor) < 0 ? 0 : 1;
        }
        if (dividend >= 0) {
            return dividend / divisor;
        }
        long quotient = ((dividend >>> 1) / divisor) << 1;
        long rem = dividend - quotient * divisor;
        return quotient + (compareUnsigned(rem, divisor) >= 0 ? 1 : 0);
    }

    /// The unsigned remainder.
    public static long remainderUnsigned(long dividend, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return dividend - divideUnsigned(dividend, divisor) * divisor;
    }

    /// The value as an unsigned decimal string.
    public static String toUnsignedString(long i) {
        return toUnsignedString(i, 10);
    }

    /// The value as an unsigned string in `radix`.
    public static String toUnsignedString(long i, int radix) {
        if (i >= 0) {
            return toString(i, radix);
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            radix = 10;
        }
        long quotient = divideUnsigned(i, radix);
        long rem = i - quotient * radix;
        return toString(quotient, radix) + Character.forDigit((int) rem, radix);
    }

    /// Parses an unsigned decimal long.
    public static long parseUnsignedLong(String s) throws NumberFormatException {
        return parseUnsignedLong(s, 10);
    }

    /// Parses an unsigned long in `radix`.
    public static long parseUnsignedLong(String s, int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string: null");
        }
        int len = s.length();
        if (len == 0) {
            throw new NumberFormatException("For input string: \"\"" + (radix == 10 ? "" : " under radix " + radix));
        }
        if (s.charAt(0) == '-') {
            throw new NumberFormatException("Illegal leading minus sign on unsigned string " + s + ".");
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " out of range");
        }
        int start = s.charAt(0) == '+' ? 1 : 0;
        if (start == len) {
            throw new NumberFormatException("For input string: \"" + s + "\"" + (radix == 10 ? "" : " under radix " + radix));
        }
        long limit = divideUnsigned(-1L, radix);
        long result = 0;
        for (int i = start; i < len; i++) {
            int d = Character.digit(s.charAt(i), radix);
            if (d < 0) {
                throw new NumberFormatException("For input string: \"" + s + "\"" + (radix == 10 ? "" : " under radix " + radix));
            }
            if (compareUnsigned(result, limit) > 0) {
                throw new NumberFormatException("String value " + s + " exceeds range of unsigned long.");
            }
            long shifted = result * radix;
            long next = shifted + d;
            if (compareUnsigned(next, shifted) < 0) {
                throw new NumberFormatException("String value " + s + " exceeds range of unsigned long.");
            }
            result = next;
        }
        return result;
    }

    /// Parses `s` from `beginIndex` to `endIndex` as a signed long.
    public static long parseLong(CharSequence s, int beginIndex, int endIndex, int radix)
            throws NumberFormatException {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        return parseLong(s.subSequence(beginIndex, endIndex).toString(), radix);
    }

    /// Parses `s` from `beginIndex` to `endIndex` as an unsigned long.
    public static long parseUnsignedLong(CharSequence s, int beginIndex, int endIndex, int radix)
            throws NumberFormatException {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        return parseUnsignedLong(s.subSequence(beginIndex, endIndex).toString(), radix);
    }

    /// Decodes a decimal, `0x`, `0X`, `#` or leading-zero octal long, with an
    /// optional sign.
    public static Long decode(String nm) throws NumberFormatException {
        int[] radix = new int[1];
        String digits = decodeDigits(nm, radix);
        return valueOf(parseLong(digits, radix[0]));
    }

    /// The number `decode` reads from `nm`, in the JDK's grammar: an
    /// optional sign, then `0x`, `0X` or `#` for hexadecimal or a leading zero
    /// for octal. Answers the signed digits to parse; the radix goes in
    /// `radixOut[0]`.
    static String decodeDigits(String nm, int[] radixOut) throws NumberFormatException {
        if (nm.length() == 0) {
            throw new NumberFormatException("Zero length string");
        }
        int radix = 10;
        int index = 0;
        boolean negative = false;
        char first = nm.charAt(0);
        if (first == '-') {
            negative = true;
            index++;
        } else if (first == '+') {
            index++;
        }
        if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        } else if (nm.startsWith("#", index)) {
            index++;
            radix = 16;
        } else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
            index++;
            radix = 8;
        }
        if (nm.startsWith("-", index) || nm.startsWith("+", index)) {
            throw new NumberFormatException("Sign character in wrong position");
        }
        radixOut[0] = radix;
        return negative ? "-" + nm.substring(index) : nm.substring(index);
    }

    /// The system property `nm` as a Long, or null.
    public static Long getLong(String nm) {
        return getLong(nm, null);
    }

    /// The system property `nm` as a Long, or `val`.
    public static Long getLong(String nm, long val) {
        Long result = getLong(nm, null);
        return result == null ? valueOf(val) : result;
    }

    /// The system property `nm` decoded as a Long, or `val` when it is unset
    /// or not a number.
    public static Long getLong(String nm, Long val) {
        String v = null;
        if (nm != null && nm.length() > 0) {
            v = System.getProperty(nm);
        }
        if (v != null) {
            try {
                return decode(v);
            } catch (NumberFormatException e) {
                // fall through to the default, as the JDK does
            }
        }
        return val;
    }

    /// The bits of `i` selected by `mask`, packed to the low end.
    public static long compress(long i, long mask) {
        long result = 0;
        int out = 0;
        for (int bit = 0; bit < 64; bit++) {
            if ((mask & (1L << bit)) != 0) {
                if ((i & (1L << bit)) != 0) {
                    result |= 1L << out;
                }
                out++;
            }
        }
        return result;
    }

    /// The low bits of `i` spread to the positions of the one bits of `mask`.
    public static long expand(long i, long mask) {
        long result = 0;
        int in = 0;
        for (int bit = 0; bit < 64; bit++) {
            if ((mask & (1L << bit)) != 0) {
                if ((i & (1L << in)) != 0) {
                    result |= 1L << bit;
                }
                in++;
            }
        }
        return result;
    }
}
