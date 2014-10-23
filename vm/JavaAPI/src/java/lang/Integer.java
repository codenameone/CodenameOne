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
public final class Integer{
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
        return (byte)value; 
    }

    /**
     * Returns the value of this Integer as a double.
     */
    public double doubleValue(){
        return value;
    }

    /**
     * Compares this object to the specified object. The result is true if and only if the argument is not null and is an Integer object that contains the same int value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && ((Integer)obj).value == value;
    }

    /**
     * Returns the value of this Integer as a float.
     */
    public float floatValue(){
        return value;
    }

    /**
     * Returns a hashcode for this Integer.
     */
    public int hashCode(){
        return value; 
    }

    /**
     * Returns the value of this Integer as an int.
     */
    public int intValue(){
        return value;
    }

    /**
     * Returns the value of this Integer as a long.
     */
    public long longValue(){
        return value; 
    }

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
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null) {
            throw invalidInt(string);
        }
        int length = string.length(), i = 0;
        if (length == 0) {
            throw invalidInt(string);
        }
        boolean negative = string.charAt(i) == '-';
        if (negative && ++i == length) {
            throw invalidInt(string);
        }

        return parse(string, i, radix, negative);
    }

    private static int parse(String string, int offset, int radix, boolean negative) throws NumberFormatException {
        int max = Integer.MIN_VALUE / radix;
        int result = 0, length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidInt(string);
            }
            if (max > result) {
                throw invalidInt(string);
            }
            int next = result * radix - digit;
            if (next > result) {
                throw invalidInt(string);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidInt(string);
            }
        }
        return result;
    }

    private static NumberFormatException invalidInt(String s) {
        throw new NumberFormatException("Invalid int: \"" + s + "\"");
    }


    /**
     * Returns the value of this Integer as a short.
     */
    public short shortValue(){
        return (short)value;
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
        return new String(cursor, bufLen - cursor, buf);
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

        return new String(cursor, bufLen - cursor, buf);
    }
    
    public static String intToOctalString(int i) {
        int bufLen = 11;  // Max number of octal digits in an int
        char[] buf = new char[bufLen];
        int cursor = bufLen;

        do {
            buf[--cursor] = DIGITS[i & 7];
        } while ((i >>>= 3) != 0);

        return new String(cursor, bufLen - cursor, buf);
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
        return toString(value);
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
    public static Integer valueOf(int i) {
        return new Integer(i);
    }
}
