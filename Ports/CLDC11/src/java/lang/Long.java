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
/// The Long class wraps a value of the primitive type long in an object. An object of type Long contains a single field whose type is long.
/// In addition, this class provides several methods for converting a long to a String and a String to a long, as well as other constants and methods useful when dealing with a long.
/// Since: JDK1.0, CLDC 1.0
public final class Long extends Number implements Comparable<Long>{
    public static Class<Long> TYPE = long.class;
    /// The largest value of type long.
    /// See Also:Constant Field Values
    public static final long MAX_VALUE=9223372036854775807l;

    /// The smallest value of type long.
    /// See Also:Constant Field Values
    public static final long MIN_VALUE=-9223372036854775808l;

    /// Constructs a newly allocated Long object that represents the primitive long argument.
    /// value - the value to be represented by the Long object.
    public Long(long value){
         //TODO codavaj!!
    }

    /// Returns the value of this Long as a double.
    public double doubleValue(){
        return 0.0d; //TODO codavaj!!
    }

    /// Compares this object against the specified object. The result is true if and only if the argument is not null and is a Long object that contains the same long value as this object.
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /// Returns the value of this Long as a float.
    public float floatValue(){
        return 0.0f; //TODO codavaj!!
    }

    /// Computes a hashcode for this Long. The result is the exclusive OR of the two halves of the primitive long value represented by this Long object. That is, the hashcode is the value of the expression: (int)(this.longValue()^(this.longValue()>>>32))
    public int hashCode(){
        return 0; //TODO codavaj!!
    }

    /// Returns the value of this Long as a long value.
    public long longValue(){
        return 0l; //TODO codavaj!!
    }

    /// Returns the value of this Long as an int value.
    public int intValue() {
        return 0;
    }

    /// Returns the value of this Long as a byte value.
    public byte byteValue() {
        return (byte)0;
    }
    
    /// Parses the string argument as a signed decimal long. The characters in the string must all be decimal digits, except that the first character may be an ASCII minus sign '-' (
    /// u002d') to indicate a negative value. The resulting long value is returned, exactly as if the argument and the radix 10 were given as arguments to the
    /// method that takes two arguments.
    /// Note that neither L nor l is permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code.
    public static long parseLong(java.lang.String s) throws java.lang.NumberFormatException{
        return 0l; //TODO codavaj!!
    }

    /// Parses the string argument as a signed long in the radix specified by the second argument. The characters in the string must all be digits of the specified radix (as determined by whether Character.digit returns a nonnegative value), except that the first character may be an ASCII minus sign '-' ('
    /// u002d' to indicate a negative value. The resulting long value is returned.
    /// Note that neither L nor l is permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code - except that either L or l may appear as a digit for a radix greater than 22.
    /// An exception of type NumberFormatException is thrown if any of the following situations occurs: The first argument is null or is a string of length zero. The radix is either smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX. The first character of the string is not a digit of the specified radix and is not a minus sign '-' ('u002d'). The first character of the string is a minus sign and the string is of length 1. Any character of the string after the first is not a digit of the specified radix. The integer value represented by the string cannot be represented as a value of type long.
    /// Examples:
    /// parseLong("0", 10) returns 0L parseLong("473", 10) returns 473L parseLong("-0", 10) returns 0L parseLong("-FF", 16) returns -255L parseLong("1100110", 2) returns 102L parseLong("99", 8) throws a NumberFormatException parseLong("Hazelnut", 10) throws a NumberFormatException parseLong("Hazelnut", 36) returns 1356099454469L
    public static long parseLong(java.lang.String s, int radix) throws java.lang.NumberFormatException{
        return 0l; //TODO codavaj!!
    }

    /// Returns a String object representing this Long's value. The long integer value represented by this Long object is converted to signed decimal representation and returned as a string, exactly as if the long value were given as an argument to the
    /// method that takes one argument.
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /// Returns a new String object representing the specified integer. The argument is converted to signed decimal representation and returned as a string, exactly as if the argument and the radix 10 were given as arguments to the
    /// method that takes two arguments.
    public static java.lang.String toString(long i){
        return null; //TODO codavaj!!
    }

    /// Creates a string representation of the first argument in the radix specified by the second argument.
    /// If the radix is smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX, then the radix 10 is used instead.
    /// If the first argument is negative, the first element of the result is the ASCII minus sign '-' ('u002d'. If the first argument is not negative, no sign character appears in the result.
    /// The remaining characters of the result represent the magnitude of the first argument. If the magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the magnitude will not be the zero character. The following ASCII characters are used as digits:
    /// 0123456789abcdefghijklmnopqrstuvwxyz These are '
    /// u0030' through '
    /// u0039' and '
    /// u0061' through '
    /// u007a'. If the radix is N, then the first N of these characters are used as radix-N digits in the order shown. Thus, the digits for hexadecimal (radix 16) are 0123456789abcdef.
    public static java.lang.String toString(long i, int radix){
        return null; //TODO codavaj!!
    }

    /// Returns the object instance of i
    ///
    /// #### Parameters
    ///
    /// - `i`: the primitive
    ///
    /// #### Returns
    ///
    /// object instance
    public static Long valueOf(long i) {
        return null;
    }
    
    public static int compare(long l1, long l2) {
        return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
    }
    
    public int compareTo(Long l) {
        return compare(this, l);
    }

    public static int hashCode(long value) {
        return 0;
    }

    public static long sum(long a, long b) {
        return 0;
    }

    public static long max(long a, long b) {
        return 0;
    }

    public static long min(long a, long b) {
        return 0;
    }

    /// The size of a long in bytes.
    public static final int BYTES = 8;

    /// The size of a long in bits.
    public static final int SIZE = 64;

    /// A Long of `parseLong(s)`.
    @Deprecated
    public Long(String s) throws NumberFormatException {
    }

    /// A Long of `parseLong(s)`.
    public static Long valueOf(String s) throws NumberFormatException {
        return null;
    }

    /// A Long of `parseLong(s, radix)`.
    public static Long valueOf(String s, int radix) throws NumberFormatException {
        return null;
    }

    /// The number of one bits.
    public static int bitCount(long i) {
        return 0;
    }

    /// The highest one bit alone, or 0.
    public static long highestOneBit(long i) {
        return 0;
    }

    /// The lowest one bit alone, or 0.
    public static long lowestOneBit(long i) {
        return 0;
    }

    /// The number of zero bits above the highest one bit; 64 for zero.
    public static int numberOfLeadingZeros(long i) {
        return 0;
    }

    /// The number of zero bits below the lowest one bit; 64 for zero.
    public static int numberOfTrailingZeros(long i) {
        return 0;
    }

    /// The bits in reverse order.
    public static long reverse(long i) {
        return 0;
    }

    /// The bytes in reverse order.
    public static long reverseBytes(long i) {
        return 0;
    }

    /// The bits rotated left by `distance`.
    public static long rotateLeft(long i, int distance) {
        return 0;
    }

    /// The bits rotated right by `distance`.
    public static long rotateRight(long i, int distance) {
        return 0;
    }

    /// -1, 0 or 1 by the sign of `i`.
    public static int signum(long i) {
        return 0;
    }

    /// The value as unsigned hexadecimal digits.
    public static String toHexString(long i) {
        return null;
    }

    /// The value as unsigned octal digits.
    public static String toOctalString(long i) {
        return null;
    }

    /// The value as unsigned binary digits.
    public static String toBinaryString(long i) {
        return null;
    }

    /// Compares the two as unsigned values.
    public static int compareUnsigned(long x, long y) {
        return 0;
    }

    /// The unsigned quotient.
    public static long divideUnsigned(long dividend, long divisor) {
        return 0;
    }

    /// The unsigned remainder.
    public static long remainderUnsigned(long dividend, long divisor) {
        return 0;
    }

    /// The value as an unsigned decimal string.
    public static String toUnsignedString(long i) {
        return null;
    }

    /// The value as an unsigned string in `radix`.
    public static String toUnsignedString(long i, int radix) {
        return null;
    }

    /// Parses an unsigned decimal long.
    public static long parseUnsignedLong(String s) throws NumberFormatException {
        return 0;
    }

    /// Parses an unsigned long in `radix`.
    public static long parseUnsignedLong(String s, int radix) throws NumberFormatException {
        return 0;
    }

    /// Parses `s` from `beginIndex` to `endIndex` as a signed long.
    public static long parseLong(CharSequence s, int beginIndex, int endIndex, int radix)
            throws NumberFormatException {
        return 0;
    }

    /// Parses `s` from `beginIndex` to `endIndex` as an unsigned long.
    public static long parseUnsignedLong(CharSequence s, int beginIndex, int endIndex, int radix)
            throws NumberFormatException {
        return 0;
    }

    /// Decodes a decimal, `0x`, `0X`, `#` or leading-zero octal long, with an
    /// optional sign.
    public static Long decode(String nm) throws NumberFormatException {
        return null;
    }

    /// The system property `nm` as a Long, or null.
    public static Long getLong(String nm) {
        return null;
    }

    /// The system property `nm` as a Long, or `val`.
    public static Long getLong(String nm, long val) {
        return null;
    }

    /// The system property `nm` decoded as a Long, or `val` when it is unset
    /// or not a number.
    public static Long getLong(String nm, Long val) {
        return null;
    }

    /// The bits of `i` selected by `mask`, packed to the low end.
    public static long compress(long i, long mask) {
        return 0;
    }

    /// The low bits of `i` spread to the positions of the one bits of `mask`.
    public static long expand(long i, long mask) {
        return 0;
    }
}
