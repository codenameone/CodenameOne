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
 * The Double class wraps a value of the primitive type double in an object. An object of type Double contains a single field whose type is double.
 * In addition, this class provides several methods for converting a double to a String and a String to a double, as well as other constants and methods useful when dealing with a double.
 * Since: JDK1.0, CLDC 1.1
 */
public final class Double extends Number {
    /**
     * The largest positive finite value of type double. It is equal to the value returned by Double.longBitsToDouble(0x7fefffffffffffffL)
     * See Also:Constant Field Values
     */
    public static final double MAX_VALUE=1.7976931348623157E308d;

    /**
     * The smallest positive value of type double. It is equal to the value returned by Double.longBitsToDouble(0x1L).
     */
    public static final double MIN_VALUE=0.0d;

    /**
     * A Not-a-Number (NaN) value of type double. It is equal to the value returned by Double.longBitsToDouble(0x7ff8000000000000L).
     * See Also:Constant Field Values
     */
    public static final double NaN=0d/0d;

    /**
     * The negative infinity of type double. It is equal to the value returned by Double.longBitsToDouble(0xfff0000000000000L).
     * See Also:Constant Field Values
     */
    public static final double NEGATIVE_INFINITY=-1d/0d;

    /**
     * The positive infinity of type double. It is equal to the value returned by Double.longBitsToDouble(0x7ff0000000000000L).
     * See Also:Constant Field Values
     */
    public static final double POSITIVE_INFINITY=1d/0d;

    private double value;
    
    /**
     * Constructs a newly allocated Double object that represents the primitive double argument.
     * value - the value to be represented by the Double.
     */
    public Double(double value){
         this.value = value;
    }

    /**
     * Returns the value of this Double as a byte (by casting to a byte).
     */
    public byte byteValue(){
        return (byte)value; 
    }

    /**
     * Returns a representation of the specified floating-point value according to the IEEE 754 floating-point "double format" bit layout.
     * Bit 63 (the bit that is selected by the mask 0x8000000000000000L) represents the sign of the floating-point number. Bits 62-52 (the bits that are selected by the mask 0x7ff0000000000000L) represent the exponent. Bits 51-0 (the bits that are selected by the mask 0x000fffffffffffffL) represent the significand (sometimes called the mantissa) of the floating-point number.
     * If the argument is positive infinity, the result is 0x7ff0000000000000L.
     * If the argument is negative infinity, the result is 0xfff0000000000000L.
     * If the argument is NaN, the result is 0x7ff8000000000000L.
     * In all cases, the result is a long integer that, when given to the longBitsToDouble(long) method, will produce a floating-point value equal to the argument to doubleToLongBits.
     */
    public native static long doubleToLongBits(double value);

    /**
     * Returns the double value of this Double.
     */
    public double doubleValue(){
        return value;
    }

    /**
     * Compares this object against the specified object. The result is true if and only if the argument is not null and is a Double object that represents a double that has the identical bit pattern to the bit pattern of the double represented by this object. For this purpose, two double values are considered to be the same if and only if the method
     * returns the same long value when applied to each.
     * Note that in most cases, for two instances of class Double, d1 and d2, the value of d1.equals(d2) is true if and only if
     * d1.doubleValue()
     * == d2.doubleValue()
     * also has the value true. However, there are two exceptions: If d1 and d2 both represent Double.NaN, then the equals method returns true, even though Double.NaN==Double.NaN has the value false. If d1 represents +0.0 while d2 represents -0.0, or vice versa, the equals test has the value false, even though +0.0==-0.0 has the value true. This allows hashtables to operate properly.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && ((Double)obj).value == value;
    }

    /**
     * Returns the float value of this Double.
     */
    public float floatValue(){
        return (float)value; 
    }

    /**
     * Returns a hashcode for this Double object. The result is the exclusive OR of the two halves of the long integer bit representation, exactly as produced by the method
     * , of the primitive double value represented by this Double object. That is, the hashcode is the value of the expression: (int)(v^(v>>>32)) where v is defined by: long v = Double.doubleToLongBits(this.doubleValue());
     */
    public int hashCode(){
        long v = doubleToLongBits(value);
        return (int) (v ^ (v >>> 32));
    }

    /**
     * Returns the integer value of this Double (by casting to an int).
     */
    public int intValue(){
        return (int)value;
    }

    /**
     * Returns true if this Double value is infinitely large in magnitude.
     */
    public boolean isInfinite(){
        return isInfinite(value);
    }

    /**
     * Indicates whether the specified double represents an infinite value.
     *
     * @param d
     *            the double to check.
     * @return {@code true} if the value of {@code d} is positive or negative
     *         infinity; {@code false} otherwise.
     */
    public static boolean isInfinite(double d) {
        return (d == POSITIVE_INFINITY) || (d == NEGATIVE_INFINITY);
    }

    /**
     * Returns true if this Double value is the special Not-a-Number (NaN) value.
     */
    public boolean isNaN(){
        return isNaN(value);
    }

    /**
     * Indicates whether the specified double is a <em>Not-a-Number (NaN)</em>
     * value.
     *
     * @param d
     *            the double value to check.
     * @return {@code true} if {@code d} is <em>Not-a-Number</em>;
     *         {@code false} if it is a (potentially infinite) double number.
     */
    public static boolean isNaN(double d) {
        return d != d;
    }

    /**
     * Returns the double-float corresponding to a given bit representation. The argument is considered to be a representation of a floating-point value according to the IEEE 754 floating-point "double precision" bit layout. That floating-point value is returned as the result.
     * If the argument is 0x7ff0000000000000L, the result is positive infinity.
     * If the argument is 0xfff0000000000000L, the result is negative infinity.
     * If the argument is any value in the range 0x7ff0000000000001L through 0x7fffffffffffffffL or in the range 0xfff0000000000001L through 0xffffffffffffffffL, the result is NaN. All IEEE 754 NaN values of type double are, in effect, lumped together by the Java programming language into a single value called NaN.
     * In all other cases, let s, e, and m be three values that can be computed from the argument:
     * int s = ((bits >> 63) == 0) ? 1 : -1; int e = (int)((bits >> 52) & 0x7ffL); long m = (e == 0) ? (bits & 0xfffffffffffffL) << 1 : (bits & 0xfffffffffffffL) | 0x10000000000000L; Then the floating-point result equals the value of the mathematical expression
     * 2e-1075.
     */
    public native static double longBitsToDouble(long bits);

    /**
     * Returns the long value of this Double (by casting to a long).
     */
    public long longValue(){
        return (long)value;
    }

    /**
     * Returns a new double initialized to the value represented by the specified String, as performed by the valueOf method of class Double.
     */
    public static double parseDouble(java.lang.String s) throws java.lang.NumberFormatException{
        return StringToReal.parseDouble(s);
    }

    /**
     * Returns the value of this Double as a short (by casting to a short).
     */
    public short shortValue(){
        return (short)value; 
    }

    /**
     * Returns a String representation of this Double object. The primitive double value represented by this object is converted to a string exactly as if by the method toString of one argument.
     */
    public java.lang.String toString(){
        return toString(value);
    }

    /**
     * Creates a string representation of the double argument. All characters mentioned below are ASCII characters. If the argument is NaN, the result is the string "NaN". Otherwise, the result is a string that represents the sign and magnitude (absolute value) of the argument. If the sign is negative, the first character of the result is '-' ('-'); if the sign is positive, no sign character appears in the result. As for the magnitude
     * : If
     * is infinity, it is represented by the characters "Infinity"; thus, positive infinity produces the result "Infinity" and negative infinity produces the result "-Infinity". If
     * is zero, it is represented by the characters "0.0"; thus, negative zero produces the result "-0.0" and positive zero produces the result "0.0". If
     * is greater than or equal to 10-3 but less than 107, then it is represented as the integer part of
     * , in decimal form with no leading zeroes, followed by '.' (.), followed by one or more decimal digits representing the fractional part of
     * . If
     * is less than 10-3 or not less than 107, then it is represented in so-called "computerized scientific notation." Let
     * be the unique integer such that 10n
     * =
     * 10n+1; then let
     * be the mathematically exact quotient of
     * and 10n so that 1
     * =
     * 10. The magnitude is then represented as the integer part of
     * , as a single decimal digit, followed by '.' (.), followed by decimal digits representing the fractional part of
     * , followed by the letter 'E' (E), followed by a representation of
     * as a decimal integer, as produced by the method
     * .
     * How many digits must be printed for the fractional part of m or a? There must be at least one digit to represent the fractional part, and beyond that as many, but only as many, more digits as are needed to uniquely distinguish the argument value from adjacent values of type double. That is, suppose that x is the exact mathematical value represented by the decimal representation produced by this method for a finite nonzero argument d. Then d must be the double value nearest to x; or if two double values are equally close to x, then d must be one of them and the least significant bit of the significand of d must be 0.
     */
    public static java.lang.String toString(double d){
        double m = Math.abs(d);
        if ( d == POSITIVE_INFINITY ){
            return "Infinity";
        } else if ( d == NEGATIVE_INFINITY ){
            return "-Infinity";
        } else if ( d == 0 ){
            return "0.0";
        } else if ( m >= 1e-3 && m < 1e7 ){
            String str = toStringImpl(d, false);
            char[] chars = str.toCharArray();
            int i = chars.length-1;
            char c;
            while ( i >=0 && (c = chars[i]) == '0' ){
                i--;
            }
            
            if ( i < 0 || str.indexOf('.') == -1 ){
                return str;
            } else if ( chars[i] == '.' || chars[i] == ','){
                i++;
            }
            
            return str.substring(0, i+1);
        } else {//if ( d < 1e-3 || d >= 1e7 ){
            return toStringImpl(d, true);
        }
       
    }
    
    native static java.lang.String toStringImpl(double d, boolean scientificNotation);

    
    
    /**
     * Returns a new Double object initialized to the value represented by the specified string. The string s is interpreted as the representation of a floating-point value and a Double object representing that value is created and returned.
     * If s is null, then a NullPointerException is thrown.
     * Leading and trailing whitespace characters in s are ignored. The rest of s should constitute a FloatValue as described by the lexical rule:
     * where
     * and
     * are as defined in Section 3.10.2 of the
     * . If it does not have the form of a
     * , then a NumberFormatException is thrown. Otherwise, it is regarded as representing an exact decimal value in the usual "computerized scientific notation"; this exact decimal value is then conceptually converted to an "infinitely precise" binary value that is then rounded to type double by the usual round-to-nearest rule of IEEE 754 floating-point arithmetic. Finally, a new object of class Double is created to represent the double value.
     */
    public static java.lang.Double valueOf(java.lang.String s) throws java.lang.NumberFormatException{
        return new Double(parseDouble(s)); 
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    public static Double valueOf(double i) {
        return new Double(i);
    }
}
