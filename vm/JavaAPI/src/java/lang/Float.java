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
 * The Float class wraps a value of primitive type float in an object. An object of type Float contains a single field whose type is float.
 * In addition, this class provides several methods for converting a float to a String and a String to a float, as well as other constants and methods useful when dealing with a float.
 * Since: JDK1.0, CLDC 1.1
 */
public final class Float extends Number implements Comparable<Float> {

    /**
     * The class object for the primitive type this class wraps.
     */
    public static final Class<Float> TYPE = Class.getPrimitiveClass(Class.CN1_PRIM_FLOAT);

    /**
     * The largest positive value of type float. It is equal to the value returned by Float.intBitsToFloat(0x7f7fffff).
     * See Also:Constant Field Values
     */
    public static final float MAX_VALUE=3.4028235E38f;

    /**
     * The smallest positive value of type float. It is equal to the value returned by Float.intBitsToFloat(0x1).
     * See Also:Constant Field Values
     */
    public static final float MIN_VALUE=1.4E-45f;

    /**
     * The Not-a-Number (NaN) value of type float. It is equal to the value returned by Float.intBitsToFloat(0x7fc00000).
     * See Also:Constant Field Values
     */
    public static final float NaN=0f/0f;

    /**
     * The negative infinity of type float. It is equal to the value returned by Float.intBitsToFloat(0xff800000).
     * See Also:Constant Field Values
     */
    public static final float NEGATIVE_INFINITY=-1f/0f;

    /**
     * The positive infinity of type float. It is equal to the value returned by Float.intBitsToFloat(0x7f800000).
     * See Also:Constant Field Values
     */
    public static final float POSITIVE_INFINITY=1f/0f;

    private float value;
    
    /**
     * Constructs a newly allocated Floatobject that represents the argument converted to type float.
     * value - the value to be represented by the Float.
     */
    public Float(double value){
         this.value = (float)value;
    }

    /**
     * Constructs a newly allocated Float object that represents the primitive float argument.
     * value - the value to be represented by the Float.
     */
    public Float(float value){
         this.value = value;
    }

    /**
     * Returns the value of this Float as a byte (by casting to a byte).
     */
    public byte byteValue(){
        return (byte)cn1Value();
    }

    /**
     * Returns the double value of this Float object.
     */
    public double doubleValue(){
        return cn1Value();
    }

    /**
     * Returns this Float's value, transparently handling both heap-allocated and
     * tagged-immediate representations. All value reads route here -- Float is final, so a
     * plain `return value;` getter would be inlined into a raw field load off a tagged
     * pointer, which has no fields.
     */
    private native float cn1Value();

    /**
     * Compares this object against some other object. The result is true if and only if the argument is not null and is a Float object that represents a float that has the identical bit pattern to the bit pattern of the float represented by this object. For this purpose, two float values are considered to be the same if and only if the method
     * returns the same int value when applied to each.
     * Note that in most cases, for two instances of class Float, f1 and f2, the value of f1.equals(f2) is true if and only if
     * f1.floatValue() == f2.floatValue()
     * also has the value true. However, there are two exceptions: If f1 and f2 both represent Float.NaN, then the equals method returns true, even though Float.NaN==Float.NaN has the value false. If f1 represents +0.0f while f2 represents -0.0f, or vice versa, the equal test has the value false, even though 0.0f==-0.0f has the value true. This definition allows hashtables to operate properly.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && floatToIntBits(((Float)obj).cn1Value()) == floatToIntBits(cn1Value());
    }

    /**
     * Returns the bit representation of a single-float value. The result is a representation of the floating-point argument according to the IEEE 754 floating-point "single precision" bit layout. Bit 31 (the bit that is selected by the mask 0x80000000) represents the sign of the floating-point number. Bits 30-23 (the bits that are selected by the mask 0x7f800000) represent the exponent. Bits 22-0 (the bits that are selected by the mask 0x007fffff) represent the significand (sometimes called the mantissa) of the floating-point number. If the argument is positive infinity, the result is 0x7f800000. If the argument is negative infinity, the result is 0xff800000. If the argument is NaN, the result is 0x7fc00000. In all cases, the result is an integer that, when given to the
     * method, will produce a floating-point value equal to the argument to floatToIntBits.
     */
    /**
     * The raw IEEE 754 bits of {@code value}, without collapsing NaN to the
     * canonical NaN.
     *
     * Delegates rather than declaring a second native. ParparVM's floatToIntBits
     * is a bare union punt that does not collapse NaN to the canonical NaN -- so it
     * is already the raw operation, and the two differ in the spec but not here. A
     * separate native would be one more mangled symbol to get wrong, silently, for
     * no behavioural difference.
     */
    public static int floatToRawIntBits(float value) {
        return floatToIntBits(value);
    }

    public native static int floatToIntBits(float value);

    /**
     * Returns the float value of this Float object.
     */
    public float floatValue(){
        return cn1Value();
    }

    /**
     * Returns a hashcode for this Float object. The result is the integer bit representation, exactly as produced by the method
     * , of the primitive float value represented by this Float object.
     */
    public int hashCode(){
        return floatToIntBits(cn1Value());
    }

    /**
     * Returns the single-float corresponding to a given bit representation. The argument is considered to be a representation of a floating-point value according to the IEEE 754 floating-point "single precision" bit layout.
     * If the argument is 0x7f800000, the result is positive infinity.
     * If the argument is 0xff800000, the result is negative infinity.
     * If the argument is any value in the range 0x7f800001 through 0x7fffffff or in the range 0xff800001 through 0xffffffff, the result is NaN. All IEEE 754 NaN values of type float are, in effect, lumped together by the Java programming language into a single float value called NaN.
     * In all other cases, let s, e, and m be three values that can be computed from the argument:
     * int s = ((bits >> 31) == 0) ? 1 : -1; int e = ((bits >> 23) &amp; 0xff); int m = (e == 0) ? (bits &amp; 0x7fffff) << 1 : (bits &amp; 0x7fffff) | 0x800000; Then the floating-point result equals the value of the mathematical expression
     * .
     */
    public native static float intBitsToFloat(int bits);

    /**
     * Returns the integer value of this Float (by casting to an int).
     */
    public int intValue(){
        return (int)cn1Value();
    }

    /**
     * Returns true if this Float value is infinitely large in magnitude.
     */
    public boolean isInfinite(){
        return isInfinite(cn1Value());
    }

    /**
     * Indicates whether the specified float represents an infinite value.
     *
     * @param f
     *            the float to check.
     * @return {@code true} if the value of {@code f} is positive or negative
     *         infinity; {@code false} otherwise.
     */
    public static boolean isInfinite(float f) {
        return (f == POSITIVE_INFINITY) || (f == NEGATIVE_INFINITY);
    }

    /**
     * Returns true if this Float value is Not-a-Number (NaN).
     */
    public boolean isNaN(){
        float cn1v = cn1Value();
        return cn1v != cn1v;
    }

    /**
     * Returns true if the specified number is the special Not-a-Number (NaN) value.
     */
    public static boolean isNaN(float v){
        return v != v; 
    }

    /**
     * Returns the long value of this Float (by casting to a long).
     */
    public long longValue(){
        return (long)cn1Value();
    }

    /**
     * Returns a new float initialized to the value represented by the specified String.
     */
    public static float parseFloat(java.lang.String s) throws java.lang.NumberFormatException{
        return StringToReal.parseFloat(s);
    }

    /**
     * Returns the value of this Float as a short (by casting to a short).
     */
    public short shortValue(){
        return (short)cn1Value();
    }

    /**
     * Returns a String representation of this Float object. The primitive float value represented by this object is converted to a String exactly as if by the method toString of one argument.
     */
    public java.lang.String toString() {
        return toString(cn1Value());
    }

    /**
     * Returns a String representation for the specified float value. The argument is converted to a readable string format as follows. All characters and characters in strings mentioned below are ASCII characters. If the argument is NaN, the result is the string "NaN". Otherwise, the result is a string that represents the sign and magnitude (absolute value) of the argument. If the sign is negative, the first character of the result is '-' ('-'); if the sign is positive, no sign character appears in the result. As for the magnitude m: If m is infinity, it is represented by the characters "Infinity"; thus, positive infinity produces the result "Infinity" and negative infinity produces the result "-Infinity". If m is zero, it is represented by the characters "0.0"; thus, negative zero produces the result "-0.0" and positive zero produces the result "0.0". If m is greater than or equal to 10-3 but less than 107, then it is represented as the integer part of m, in decimal form with no leading zeroes, followed by '.' (.), followed by one or more decimal digits representing the fractional part of m. If m is less than 10-3 or not less than 107, then it is represented in so-called "computerized scientific notation." Let n be the unique integer such that 10n
     * =m
     * 1; then let a be the mathematically exact quotient of m and 10n so that 1
     * a &lt; 10. The magnitude is then represented as the integer part of a, as a single decimal digit, followed by '.' (.), followed by decimal digits representing the fractional part of a, followed by the letter 'E' (E), followed by a representation of n as a decimal integer, as produced by the method
     * of one argument. How many digits must be printed for the fractional part of m or a? There must be at least one digit to represent the fractional part, and beyond that as many, but only as many, more digits as are needed to uniquely distinguish the argument value from adjacent values of type float. That is, suppose that x is the exact mathematical value represented by the decimal representation produced by this method for a finite nonzero argument f. Then f must be the float value nearest to x; or, if two float values are equally close to xthen f must be one of them and the least significant bit of the significand of f must be 0.
     */
    public static java.lang.String toString(float d){
        float m = Math.abs(d);
        // The NaN and signed-zero branches mirror Double.toString, which has had both for a
        // while; Float was never brought into line. Without the first, NaN fell through to
        // the scientific-notation path (every comparison against a NaN is false); without
        // the second, Float.toString(-0.0f) printed "0.0", losing the sign that
        // Float.compare, Float.equals and Arrays.sort all still honour.
        if (isNaN(d)) {
            return "NaN";
        } else if ( d == POSITIVE_INFINITY ){
            return "Infinity";
        } else if ( d == NEGATIVE_INFINITY ){
            return "-Infinity";
        } else if ( d == 0 ){
            return floatToIntBits(d) == NEGATIVE_ZERO_BITS ? "-0.0" : "0.0";
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
    public native static java.lang.String toStringImpl(float f, boolean scientificNotation);

    /**
     * Returns the floating point value represented by the specified String. The string s is interpreted as the representation of a floating-point value and a Float object representing that value is created and returned.
     * If s is null, then a NullPointerException is thrown.
     * Leading and trailing whitespace characters in s are ignored. The rest of s should constitute a FloatValue as described by the lexical syntax rules:
     * where
     * ,
     * are as defined in Section 3.10.2 of the
     * . If it does not have the form of a
     * , then a NumberFormatException is thrown. Otherwise, it is regarded as representing an exact decimal value in the usual "computerized scientific notation"; this exact decimal value is then conceptually converted to an "infinitely precise" binary value that is then rounded to type float by the usual round-to-nearest rule of IEEE 754 floating-point arithmetic.
     */
    public static java.lang.Float valueOf(java.lang.String s) throws java.lang.NumberFormatException{
        return Float.parseFloat(s); 
    }


    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    // Native so the tagged build can return an immediate without allocating. The off path
    // (and 32-bit-pointer targets) calls valueOfHeap, preserving the previous behaviour.
    public static native Float valueOf(float i);

    static Float valueOfHeap(float i) {
        return new Float(i);
    }
    
    private static final int NEGATIVE_ZERO_BITS=floatToIntBits(-0f);
    
    public static int compare(float f1, float f2) {
        if (isNaN(f1) && isNaN(f2)) {
            return 0;
        } else if (isNaN(f1)) {
            return 1;
        } else if (isNaN(f2)) {
            return -1;
        } else if (f1 == 0f && f2 == 0f) {
            int f1bits = floatToIntBits(f1);
            int f2bits = floatToIntBits(f2);
            return f1bits == f2bits ? 0 :
                    f1bits == NEGATIVE_ZERO_BITS ? -1 : 1;
        } else {
            return f1 < f2 ? -1 : f1 > f2 ? 1 : 0;
        }
    }

    public int compareTo(Float another) {
        return compare(cn1Value(), another.cn1Value());
    }
}
