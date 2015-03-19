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
public final class Long{
    /**
     * The largest value of type long.
     * See Also:Constant Field Values
     */
    public static final long MAX_VALUE=9223372036854775807l;

    /**
     * The smallest value of type long.
     * See Also:Constant Field Values
     */
    public static final long MIN_VALUE=-9223372036854775807l;

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
        return (double)value;
    }

    /**
     * Compares this object against the specified object. The result is true if and only if the argument is not null and is a Long object that contains the same long value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && ((Long)obj).value == value;
    }

    /**
     * Returns the value of this Long as a float.
     */
    public float floatValue(){
        return (float)value;
    }

    /**
     * Computes a hashcode for this Long. The result is the exclusive OR of the two halves of the primitive long value represented by this Long object. That is, the hashcode is the value of the expression: (int)(this.longValue()^(this.longValue()>>>32))
     */
    public int hashCode(){
        return (int)value; 
    }

    /**
     * Returns the value of this Long as a long value.
     */
    public long longValue(){
        return value;
    }

    /**
     * Returns the value of this Long as an int value.
     */
    public int intValue() {
        return (int)value;
    }

    /**
     * Returns the value of this Long as a byte value.
     */
    public int byteValue() {
        return (byte)value;
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
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null) {
            throw invalidLong(string);
        }
        int length = string.length(), i = 0;
        if (length == 0) {
            throw invalidLong(string);
        }
        boolean negative = string.charAt(i) == '-';
        if (negative && ++i == length) {
            throw invalidLong(string);
        }

        return parse(string, i, radix, negative);
    }

    private static long parse(String string, int offset, int radix, boolean negative) {
        long max = Long.MIN_VALUE / radix;
        long result = 0, length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidLong(string);
            }
            if (max > result) {
                throw invalidLong(string);
            }
            long next = result * radix - digit;
            if (next > result) {
                throw invalidLong(string);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidLong(string);
            }
        }
        return result;
    }

    private static NumberFormatException invalidLong(String s) {
        throw new NumberFormatException("Invalid long: \"" + s + "\"");
    }

    /**
     * Returns a String object representing this Long's value. The long integer value represented by this Long object is converted to signed decimal representation and returned as a string, exactly as if the long value were given as an argument to the
     * method that takes one argument.
     */
    public java.lang.String toString(){
        return toString(value);
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
    public static Long valueOf(long i) {
        return new Long(i);
    }
}
