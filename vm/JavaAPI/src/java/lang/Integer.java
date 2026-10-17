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
 * The Integer class wraps a value of the primitive type int in an object. An object of type Integer contains a single field whose type is int.
 * In addition, this class provides several methods for converting an int to a String and a String to an int, as well as other constants and methods useful when dealing with an int.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Integer extends Number implements Comparable<Integer> {
    
    public static final Class<Integer> TYPE = int.class;
    
    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    /**
     * The largest value of type int. The constant value of this field is 2147483647.
     * See Also:Constant Field Values
     */
    public static final int MAX_VALUE=2147483647;

    /**
     * The smallest value of type int. The constant value of this field is -2147483648.
     * See Also:Constant Field Values
     */
    public static final int MIN_VALUE=-2147483648;

    private int value;
    
    /**
     * Constructs a newly allocated Integer object that represents the primitive int argument.
     * value - the value to be represented by the Integer.
     */
    public Integer(int value){
         this.value = value;
    }

    /**
     * Returns the value of this Integer as a byte.
     */
    public byte byteValue(){
        return (byte)cn1Value();
    }

    /**
     * Returns the value of this Integer as a double.
     */
    public double doubleValue(){
        return cn1Value();
    }

    /**
     * Compares this object to the specified object. The result is true if and only if the argument is not null and is an Integer object that contains the same int value as this object.
     */
    public boolean equals(java.lang.Object obj){
        // instanceof (not getClass()) so a tagged Integer needs no header; Integer is final.
        return (obj instanceof Integer) && ((Integer)obj).cn1Value() == cn1Value();
    }

    /**
     * Returns the value of this Integer as a float.
     */
    public float floatValue(){
        return cn1Value();
    }

    /**
     * Returns a hashcode for this Integer.
     */
    public int hashCode(){
        return cn1Value();
    }

    /**
     * Returns the value of this Integer as an int.
     */
    public int intValue(){
        return cn1Value();
    }

    /**
     * Returns the value of this Integer as a long.
     */
    public long longValue(){
        return cn1Value();
    }

    /**
     * Returns this Integer's int value, transparently handling both heap-allocated and
     * tagged-immediate (poor-man's-Valhalla) representations. All value reads route here.
     */
    private native int cn1Value();

    /**
     * Parses the string argument as a signed decimal integer. The characters in the string must all be decimal digits, except that the first character may be an ASCII minus sign '-' ('
     * u002d') to indicate a negative value. The resulting integer value is returned, exactly as if the argument and the radix 10 were given as arguments to the
     * method.
     */
    public static int parseInt(java.lang.String s) throws java.lang.NumberFormatException{
        return parseInt(s, 10); 
    }

    /**
     * Parses the string argument as a signed integer in the radix specified by the second argument. The characters in the string must all be digits of the specified radix (as determined by whether
     * returns a nonnegative value), except that the first character may be an ASCII minus sign '-' ('
     * u002d') to indicate a negative value. The resulting integer value is returned.
     * An exception of type NumberFormatException is thrown if any of the following situations occurs: The first argument is null or is a string of length zero. The radix is either smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX. Any character of the string is not a digit of the specified radix, except that the first character may be a minus sign '-' ('u002d') provided that the string is longer than length 1. The integer value represented by the string is not a value of type int.
     * Examples:
     * parseInt("0", 10) returns 0 parseInt("473", 10) returns 473 parseInt("-0", 10) returns 0 parseInt("-FF", 16) returns -255 parseInt("1100110", 2) returns 102 parseInt("2147483647", 10) returns 2147483647 parseInt("-2147483648", 10) returns -2147483648 parseInt("2147483648", 10) throws a NumberFormatException parseInt("99", 8) throws a NumberFormatException parseInt("Kona", 10) throws a NumberFormatException parseInt("Kona", 27) returns 411787
     */
    public static int parseInt(String string, int radix) throws NumberFormatException {
        if (string == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        checkRadix(radix);
        int length = string.length(), i = 0;
        if (length == 0) {
            throw invalidInt(string, radix);
        }
        boolean negative = string.charAt(i) == '-';
        if (negative && ++i == length) {
            throw invalidInt(string, radix);
        }
        boolean positive = !negative && string.charAt(i) == '+';
        if (positive && ++i == length) {
            throw invalidInt(string, radix);
        }

        return parse(string, i, radix, negative);
    }

    private static int parse(String string, int offset, int radix, boolean negative) throws NumberFormatException {
        int max = Integer.MIN_VALUE / radix;
        int result = 0, length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidInt(string, radix);
            }
            if (max > result) {
                throw invalidInt(string, radix);
            }
            int next = result * radix - digit;
            if (next > result) {
                throw invalidInt(string, radix);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidInt(string, radix);
            }
        }
        return result;
    }

    private static NumberFormatException invalidInt(String s, int radix) {
        return new NumberFormatException("For input string: \"" + s + "\""
                + (radix == 10 ? "" : " under radix " + radix));
    }

    // The JDK's messages for a radix outside [MIN_RADIX, MAX_RADIX].
    static void checkRadix(int radix) {
        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix + " less than Character.MIN_RADIX");
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " greater than Character.MAX_RADIX");
        }
    }


    /**
     * Returns the value of this Integer as a short.
     */
    public short shortValue(){
        return (short)cn1Value();
    }

    /**
     * Creates a string representation of the integer argument as an unsigned integer in base
     * 2.
     * The unsigned integer value is the argument plus 232if the argument is negative; otherwise it is equal to the argument. This value is converted to a string of ASCII digits in binary (base2) with no extra leading 0s. If the unsigned magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the unsigned magnitude will not be the zero character. The characters '0' ('u0030') and '1' ('u0031') are used as binary digits.
     */
    public static java.lang.String toBinaryString(int i){
        return intToBinaryString(i);
    }    
    
    private static String intToBinaryString(int i) {
        int bufLen = 32;  // Max number of binary digits in an int
        char[] buf = new char[bufLen];
        int cursor = bufLen;

        do {
            buf[--cursor] = DIGITS[i & 1];
        }  while ((i >>>= 1) != 0);

        //return new String(cursor, bufLen - cursor, buf);
        return new String(buf, cursor, bufLen - cursor);
    }


    /**
     * Creates a string representation of the integer argument as an unsigned integer in base
     * 16.
     * The unsigned integer value is the argument plus 232 if the argument is negative; otherwise, it is equal to the argument. This value is converted to a string of ASCII digits in hexadecimal (base16) with no extra leading 0s. If the unsigned magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the unsigned magnitude will not be the zero character. The following characters are used as hexadecimal digits:
     * 0123456789abcdef These are the characters '
     * u0030' through '
     * u0039' and 'u\0039' through '
     * u0066'.
     */
    public static java.lang.String toHexString(int i){
        return intToHexString(i, 0);
    }

    public static String intToHexString(int i, int minWidth) {
        int bufLen = 8;  // Max number of hex digits in an int
        char[] buf = new char[bufLen];
        int cursor = bufLen;

        char[] digits = DIGITS;
        do {
            buf[--cursor] = digits[i & 0xf];
        } while ((i >>>= 4) != 0 || (bufLen - cursor < minWidth));

        return new String(buf, cursor, bufLen - cursor);
    }
    
    public static String intToOctalString(int i) {
        int bufLen = 11;  // Max number of octal digits in an int
        char[] buf = new char[bufLen];
        int cursor = bufLen;

        do {
            buf[--cursor] = DIGITS[i & 7];
        } while ((i >>>= 3) != 0);

        return new String(buf, cursor, bufLen - cursor);
    }

    /**
     * Creates a string representation of the integer argument as an unsigned integer in base 8.
     * The unsigned integer value is the argument plus 232 if the argument is negative; otherwise, it is equal to the argument. This value is converted to a string of ASCII digits in octal (base8) with no extra leading 0s.
     * If the unsigned magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the unsigned magnitude will not be the zero character. The octal digits are:
     * 01234567 These are the characters '
     * u0030' through '
     * u0037'.
     */
    public static java.lang.String toOctalString(int i){
        return intToOctalString(i);
    }

    /**
     * Returns a String object representing this Integer's value. The value is converted to signed decimal representation and returned as a string, exactly as if the integer value were given as an argument to the
     * method.
     */
    public java.lang.String toString(){
        return toString(cn1Value());
    }

    /**
     * Returns a new String object representing the specified integer. The argument is converted to signed decimal representation and returned as a string, exactly as if the argument and radix 10 were given as arguments to the
     * method.
     */
    public native static java.lang.String toString(int i);

    /**
     * Creates a string representation of the first argument in the radix specified by the second argument.
     * If the radix is smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX, then the radix 10 is used instead.
     * If the first argument is negative, the first element of the result is the ASCII minus character '-' ('u002d'). If the first argument is not negative, no sign character appears in the result.
     * The remaining characters of the result represent the magnitude of the first argument. If the magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the magnitude will not be the zero character. The following ASCII characters are used as digits:
     * 0123456789abcdefghijklmnopqrstuvwxyz These are '
     * u0030' through '
     * u0039' and '
     * u0061' through '
     * u007a'. If the radix is N, then the first N of these characters are used as radix-N digits in the order shown. Thus, the digits for hexadecimal (radix 16) are 0123456789abcdef.
     */
    public native static java.lang.String toString(int i, int radix);

    /**
     * Returns a new Integer object initialized to the value of the specified String. The argument is interpreted as representing a signed decimal integer, exactly as if the argument were given to the
     * method. The result is an Integer object that represents the integer value specified by the string.
     * In other words, this method returns an Integer object equal to the value of:
     * new Integer(Integer.parseInt(s))
     */
    public static java.lang.Integer valueOf(java.lang.String s) throws java.lang.NumberFormatException{
        return new Integer(parseInt(s)); 
    }

    /**
     * Returns a new Integer object initialized to the value of the specified String. The first argument is interpreted as representing a signed integer in the radix specified by the second argument, exactly as if the arguments were given to the
     * method. The result is an Integer object that represents the integer value specified by the string.
     * In other words, this method returns an Integer object equal to the value of:
     * new Integer(Integer.parseInt(s, radix))
     */
    public static java.lang.Integer valueOf(java.lang.String s, int radix) throws java.lang.NumberFormatException{
        return new Integer(parseInt(s, radix)); 
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    // Native so the tagged-int build can return an immediate without allocating. The
    // off path (and 32-bit-pointer targets) calls valueOfHeap, preserving the cache.
    public static native Integer valueOf(int i);

    static Integer valueOfHeap(int i) {
        if (i >= -128 && i <= 127) {
            return IntegerCache.cache[i + 128];
        }
        return new Integer(i);
    }

    /**
     * Cache of boxed values for the range -128..127, mirroring the JDK's
     * {@code Integer.IntegerCache} so autoboxing in tight loops does not
     * allocate. Initialized eagerly on class load.
     */
    private static final class IntegerCache {
        static final Integer[] cache = new Integer[256];
        static {
            for (int j = 0; j < 256; j++) {
                cache[j] = new Integer(j - 128);
            }
        }
        private IntegerCache() {}
    }

    /**
     * Returns the value of the {@code signum} function for the specified
     * integer.
     *
     * @param i
     *            the integer value to check.
     * @return -1 if {@code i} is negative, 1 if {@code i} is positive, 0 if
     *         {@code i} is zero.
     * @since 1.5
     */
    public static int signum(int i) {
        return (i >> 31) | (-i >>> 31); // Hacker's delight 2-7
    }

    /**
     * Rotates the two's-complement binary representation of {@code i} left by
     * {@code distance} bits.
     *
     * The shift distance is used modulo 32 by the JLS shift rules, which is what
     * makes the negation on the right half correct for every distance, including
     * zero and multiples of 32.
     */
    public static int rotateLeft(int i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    public static int compare(int f1, int f2) {
        
        if (f1 > f2) return 1;
        else if (f1 < f2) return -1;
        return 0;    
        
    }

    public int compareTo(Integer another) {
        int value = cn1Value(), av = another.cn1Value();
        return value < av ? -1 : value > av ? 1 : 0;
    }
    
     public static int numberOfLeadingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        int n = 0;
        if (i >>> 16 != 0) {
            i >>>= 16;
            n |= 16;
        }
        if (i >>> 8 != 0) {
            i >>>= 8;
            n |= 8;
        }
        if (i >>> 4 != 0) {
            i >>>= 4;
            n |= 4;
        }
        if (i >>> 2 != 0) {
            i >>>= 2;
            n |= 2;
        }
        if (i >>> 1 != 0) {
            i >>>= 1;
            n |= 1;
        }
        return 32 - n - 1;
    }

    /**
     * {@code a + b}; a method reference target for reductions ({@code Integer::sum}).
     *
     * @param a an operand
     * @param b an operand
     * @return the sum
     */
    public static int sum(int a, int b) {
        return a + b;
    }

    /**
     * The greater of two values (Math.max), as a method reference target.
     *
     * @param a a value
     * @param b a value
     * @return the greater
     */
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /**
     * The smaller of two values (Math.min), as a method reference target.
     *
     * @param a a value
     * @param b a value
     * @return the smaller
     */
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    // ---------------------------------------------------------------------
    // Members of the JDK's class this one did not have, in plain Java.
    // ---------------------------------------------------------------------

    /// The size of an int in bytes.
    public static final int BYTES = 4;

    /// The size of an int in bits.
    public static final int SIZE = 32;

    /// An Integer of `parseInt(s)`.
    @Deprecated
    public Integer(String s) throws NumberFormatException {
        this(parseInt(s));
    }

    /// The value itself.
    public static int hashCode(int value) {
        return value;
    }

    /// The number of one bits.
    public static int bitCount(int i) {
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        i = (i + (i >>> 4)) & 0x0f0f0f0f;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        return i & 0x3f;
    }

    /// The highest one bit alone, or 0.
    public static int highestOneBit(int i) {
        return i == 0 ? 0 : 0x80000000 >>> numberOfLeadingZeros(i);
    }

    /// The lowest one bit alone, or 0.
    public static int lowestOneBit(int i) {
        return i & -i;
    }

    /// The number of zero bits below the lowest one bit; 32 for zero.
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

    /// The bits in reverse order.
    public static int reverse(int i) {
        i = (i & 0x55555555) << 1 | (i >>> 1) & 0x55555555;
        i = (i & 0x33333333) << 2 | (i >>> 2) & 0x33333333;
        i = (i & 0x0f0f0f0f) << 4 | (i >>> 4) & 0x0f0f0f0f;
        return reverseBytes(i);
    }

    /// The bytes in reverse order.
    public static int reverseBytes(int i) {
        return (i << 24) | ((i & 0xff00) << 8) | ((i >>> 8) & 0xff00) | (i >>> 24);
    }

    /// The bits rotated right by `distance`.
    public static int rotateRight(int i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /// Compares the two as unsigned values.
    public static int compareUnsigned(int x, int y) {
        return compare(x + MIN_VALUE, y + MIN_VALUE);
    }

    /// The value as an unsigned long.
    public static long toUnsignedLong(int x) {
        return ((long) x) & 0xffffffffL;
    }

    /// The unsigned quotient.
    public static int divideUnsigned(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return (int) (toUnsignedLong(dividend) / toUnsignedLong(divisor));
    }

    /// The unsigned remainder.
    public static int remainderUnsigned(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return (int) (toUnsignedLong(dividend) % toUnsignedLong(divisor));
    }

    /// The value as an unsigned decimal string.
    public static String toUnsignedString(int i) {
        return Long.toString(toUnsignedLong(i));
    }

    /// The value as an unsigned string in `radix`.
    public static String toUnsignedString(int i, int radix) {
        return Long.toString(toUnsignedLong(i), radix);
    }

    /// Parses an unsigned decimal int.
    public static int parseUnsignedInt(String s) throws NumberFormatException {
        return parseUnsignedInt(s, 10);
    }

    /// Parses an unsigned int in `radix`.
    public static int parseUnsignedInt(String s, int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string: null");
        }
        if (s.length() > 0 && s.charAt(0) == '-') {
            throw new NumberFormatException("Illegal leading minus sign on unsigned string " + s + ".");
        }
        long value = Long.parseLong(s, radix);
        if ((value & 0xffffffff00000000L) != 0) {
            throw new NumberFormatException("String value " + s + " exceeds range of unsigned int.");
        }
        return (int) value;
    }

    /// Parses `s` from `beginIndex` to `endIndex` as a signed int.
    public static int parseInt(CharSequence s, int beginIndex, int endIndex, int radix)
            throws NumberFormatException {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        return parseInt(s.subSequence(beginIndex, endIndex).toString(), radix);
    }

    /// Parses `s` from `beginIndex` to `endIndex` as an unsigned int.
    public static int parseUnsignedInt(CharSequence s, int beginIndex, int endIndex, int radix)
            throws NumberFormatException {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        return parseUnsignedInt(s.subSequence(beginIndex, endIndex).toString(), radix);
    }

    /// Decodes a decimal, `0x`, `0X`, `#` or leading-zero octal int, with an
    /// optional sign.
    public static Integer decode(String nm) throws NumberFormatException {
        int[] radix = new int[1];
        String digits = Long.decodeDigits(nm, radix);
        return valueOf(parseInt(digits, radix[0]));
    }

    /// The system property `nm` as an Integer, or null.
    public static Integer getInteger(String nm) {
        return getInteger(nm, null);
    }

    /// The system property `nm` as an Integer, or `val`.
    public static Integer getInteger(String nm, int val) {
        Integer result = getInteger(nm, null);
        return result == null ? valueOf(val) : result;
    }

    /// The system property `nm` decoded as an Integer, or `val` when it is
    /// unset or not a number.
    public static Integer getInteger(String nm, Integer val) {
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
    public static int compress(int i, int mask) {
        int result = 0;
        int out = 0;
        for (int bit = 0; bit < 32; bit++) {
            if ((mask & (1 << bit)) != 0) {
                if ((i & (1 << bit)) != 0) {
                    result |= 1 << out;
                }
                out++;
            }
        }
        return result;
    }

    /// The low bits of `i` spread to the positions of the one bits of `mask`.
    public static int expand(int i, int mask) {
        int result = 0;
        int in = 0;
        for (int bit = 0; bit < 32; bit++) {
            if ((mask & (1 << bit)) != 0) {
                if ((i & (1 << in)) != 0) {
                    result |= 1 << bit;
                }
                in++;
            }
        }
        return result;
    }
}
