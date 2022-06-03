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

import java.util.Arrays;

/**
 * The Character class wraps a value of the primitive type char in an object. An object of type Character contains a single field whose type is char.
 * In addition, this class provides several methods for determining the type of a character and converting characters from uppercase to lowercase and vice versa.
 * Character information is based on the Unicode Standard, version 3.0. However, in order to reduce footprint, by default the character property and case conversion operations in CLDC are available only for the ISO Latin-1 range of characters. Other Unicode character blocks can be supported as necessary.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Character implements Comparable<Character>{
    /**
     * The maximum radix available for conversion to and from Strings.
     * See Also:Integer.toString(int, int), Integer.valueOf(java.lang.String), Constant Field Values
     */
    public static final int MAX_RADIX=36;

    /**
     * The constant value of this field is the largest value of type char.
     * Since: JDK1.0.2 See Also:Constant Field Values
     */
    public static final char MAX_VALUE=(char)65535;

    /**
     * The minimum radix available for conversion to and from Strings.
     * See Also:Integer.toString(int, int), Integer.valueOf(java.lang.String), Constant Field Values
     */
    public static final int MIN_RADIX=2;

    /**
     * The constant value of this field is the smallest value of type char.
     * Since: JDK1.0.2 See Also:Constant Field Values
     */
    public static final char MIN_VALUE=(char)0;
    
    
    /**
     * <p>
     * Minimum value of a high surrogate or leading surrogate unit in UTF-16
     * encoding - <code>'\uD800'</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final char MIN_HIGH_SURROGATE = '\uD800';

    /**
     * <p>
     * Maximum value of a high surrogate or leading surrogate unit in UTF-16
     * encoding - <code>'\uDBFF'</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';

    /**
     * <p>
     * Minimum value of a low surrogate or trailing surrogate unit in UTF-16
     * encoding - <code>'\uDC00'</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final char MIN_LOW_SURROGATE = '\uDC00';

    /**
     * Maximum value of a low surrogate or trailing surrogate unit in UTF-16
     * encoding - <code>'\uDFFF'</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final char MAX_LOW_SURROGATE = '\uDFFF';

    /**
     * <p>
     * Minimum value of a surrogate unit in UTF-16 encoding - <code>'\uD800'</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final char MIN_SURROGATE = '\uD800';

    /**
     * <p>
     * Maximum value of a surrogate unit in UTF-16 encoding - <code>'\uDFFF'</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final char MAX_SURROGATE = '\uDFFF';

    /**
     * <p>
     * Minimum value of a supplementary code point - <code>U+010000</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;

    /**
     * <p>
     * Minimum code point value - <code>U+0000</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final int MIN_CODE_POINT = 0x000000;

    /**
     * <p>
     * Maximum code point value - <code>U+10FFFF</code>.
     * </p>
     * 
     * @since 1.5
     */
    public static final int MAX_CODE_POINT = 0x10FFFF;

    /**
     * <p>
     * Constant for the number of bits to represent a <code>char</code> in
     * two's compliment form.
     * </p>
     * 
     * @since 1.5
     */
    public static final int SIZE = 16;
    
    //public static final int MIN_RADIX = 2;
    //public static final int MAX_RADIX = 36;
    //public static final char MIN_VALUE = '\0';
    //public static final char MAX_VALUE = '\uFFFF';
    public static final Class<Character> TYPE = char.class;
    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte OTHER_LETTER = 5;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte LETTER_NUMBER = 10;
    public static final byte OTHER_NUMBER = 11;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte CONTROL = 15;
    public static final byte FORMAT = 16;
    public static final byte PRIVATE_USE = 17;
    public static final byte SURROGATE = 19;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte START_PUNCTUATION = 21;
    public static final byte END_PUNCTUATION = 22;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte MATH_SYMBOL = 25;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte OTHER_SYMBOL = 28;
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;
    public static final byte DIRECTIONALITY_UNDEFINED = -1;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
    public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
    public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
    public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
    public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
    public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
    public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
    public static final byte DIRECTIONALITY_WHITESPACE = 12;
    public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;

    private static final int SURROGATE_NEUTRAL_BIT_MASK = 0xF800;
    private static final int SURROGATE_BITS = 0xD800;
    private static final int SURROGATE_BIT_MASK = 0xFC00;
    private static final int SURROGATE_BIT_INV_MASK = 0x03FF;
    private static final int HIGH_SURROGATE_BITS = 0xD800;
    private static final int LOW_SURROGATE_BITS = 0xDC00;
    private static final int MEANINGFUL_SURROGATE_BITS = 10;
    

    private char value;
    
    /**
     * Constructs a Character object and initializes it so that it represents the primitive value argument.
     * value - value for the new Character object.
     */
    public Character(char value){
         this.value = value;
    }

    /**
     * Returns the value of this Character object.
     */
    public char charValue(){
        return value; 
    }

    /**
     * Returns the numeric value of the character ch in the specified radix.
     */
    public static int digit(char ch, int radix){
        return digit((int) ch, radix);
    }
    
    /**
     * Convenience method to determine the value of the character
     * {@code codePoint} in the supplied radix. The value of {@code radix} must
     * be between MIN_RADIX and MAX_RADIX.
     *
     * @param codePoint
     *            the character, including supplementary characters.
     * @param radix
     *            the radix.
     * @return if {@code radix} lies between {@link #MIN_RADIX} and
     *         {@link #MAX_RADIX} then the value of the character in the radix;
     *         -1 otherwise.
     */
    public static int digit(int codePoint, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            return -1;
        }
        if (codePoint < 128) {
            // Optimized for ASCII
            int result = -1;
            if ('0' <= codePoint && codePoint <= '9') {
                result = codePoint - '0';
            } else if ('a' <= codePoint && codePoint <= 'z') {
                result = 10 + (codePoint - 'a');
            } else if ('A' <= codePoint && codePoint <= 'Z') {
                result = 10 + (codePoint - 'A');
            }
            return result < radix ? result : -1;
        }
        //return digitImpl(codePoint, radix);
        return codePoint;
    }


    /**
     * Compares this object against the specified object. The result is true if and only if the argument is not null and is a Character object that represents the same char value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && ((Character)obj).value == value;
    }

    /**
     * Returns a hash code for this Character.
     */
    public int hashCode(){
        return value; 
    }
    
    public static boolean isLetterOrDigit(char ch) {
        return isLetterOrDigit((int) ch);
    }

    public static boolean isLetterOrDigit(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case DECIMAL_DIGIT_NUMBER:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean isLetter(char ch) {
        return isLetter((int) ch);
    }

    public static boolean isLetter(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean isJavaIdentifierStart(char ch) {
        return isJavaIdentifierStart((int) ch);
    }

    public static boolean isJavaIdentifierStart(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
            case CONNECTOR_PUNCTUATION:
            case CURRENCY_SYMBOL:
                return true;
            default:
                return isIdentifierIgnorable(codePoint);
        }
    }
    
    public static boolean isJavaIdentifierPart(char ch) {
        return isJavaIdentifierPart((int) ch);
    }

    public static boolean isJavaIdentifierPart(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
            case DECIMAL_DIGIT_NUMBER:
            case COMBINING_SPACING_MARK:
            case NON_SPACING_MARK:
            case CONNECTOR_PUNCTUATION:
            case CURRENCY_SYMBOL:
                return true;
            default:
                return isIdentifierIgnorable(codePoint);
        }
    }

    
    public static boolean isIdentifierIgnorable(char ch) {
        return isIdentifierIgnorable((int) ch);
    }

    public static boolean isIdentifierIgnorable(int codePoint) {
        if (codePoint >= 0x00 && codePoint <= 0x08 || codePoint >= 0x0E && codePoint <= 0x1B
                || codePoint >= 0x7F && codePoint <= 0x9F) {
            return true;
        }
        return getType(codePoint) == FORMAT;
    }
    

    /**
     * Determines if the specified character is a digit.
     */
    public static boolean isDigit(char ch){
        return isDigit((int) ch);
    }

    /**
     * Indicates whether the specified code point is a digit.
     *
     * @param codePoint
     *            the code point to check.
     * @return {@code true} if {@code codePoint} is a digit; {@code false}
     *         otherwise.
     */
    public static boolean isDigit(int codePoint) {
        // Optimized case for ASCII
        if ('0' <= codePoint && codePoint <= '9') {
            return true;
        }
        if (codePoint < 1632) {
            return false;
        }
        //return isDigitImpl(codePoint);
        return false;
    }

    /**
     * Determines if the specified character is a lowercase character.
     * Note that by default CLDC only supports the ISO Latin-1 range of characters.
     * Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF), the following are lowercase:
     * a b c d e f g h i j k l m n o p q r s t u v w x y z u00DF u00E0 u00E1 u00E2 u00E3 u00E4 u00E5 u00E6 u00E7 u00E8 u00E9 u00EA u00EB u00EC u00ED u00EE u00EF u00F0 u00F1 u00F2 u00F3 u00F4 u00F5 u00F6 u00F8 u00F9 u00FA u00FB u00FC u00FD u00FE u00FF
     */
    public static boolean isLowerCase(char ch){
        return isLowerCase((int) ch);
    }

    /**
     * Indicates whether the specified code point is a lower case letter.
     *
     * @param codePoint
     *            the code point to check.
     * @return {@code true} if {@code codePoint} is a lower case letter;
     *         {@code false} otherwise.
     */
    public static boolean isLowerCase(int codePoint) {
        // Optimized case for ASCII
        if ('a' <= codePoint && codePoint <= 'z') {
            return true;
        }
        if (codePoint < 128) {
            return false;
        }
        //return isLowerCaseImpl(codePoint);
        return false;
    }

    /**
     * Determines if the specified character is an uppercase character.
     * Note that by default CLDC only supports the ISO Latin-1 range of characters.
     * Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF), the following are uppercase:
     * A B C D E F G H I J K L M N O P Q R S T U V W X Y Z u00C0 u00C1 u00C2 u00C3 u00C4 u00C5 u00C6 u00C7 u00C8 u00C9 u00CA u00CB u00CC u00CD u00CE u00CF u00D0 u00D1 u00D2 u00D3 u00D4 u00D5 u00D6 u00D8 u00D9 u00DA u00DB u00DC u00DD u00DE
     */
    public static boolean isUpperCase(char ch){
        return isUpperCase((int) ch);
    }

    /**
     * Indicates whether the specified code point is an upper case letter.
     *
     * @param codePoint
     *            the code point to check.
     * @return {@code true} if {@code codePoint} is a upper case letter;
     *         {@code false} otherwise.
     */
    public static boolean isUpperCase(int codePoint) {
        // Optimized case for ASCII
        if ('A' <= codePoint && codePoint <= 'Z') {
            return true;
        }
        /*if (codePoint < 128) {
            return false;
        }
        return isUpperCaseImpl(codePoint);*/
        return false;
    }

    /**
     * The given character is mapped to its lowercase equivalent; if the character has no lowercase equivalent, the character itself is returned.
     * Note that by default CLDC only supports the ISO Latin-1 range of characters.
     */
    public static native char toLowerCase(char ch);

    /**
     * Returns the lower case equivalent for the specified code point if it is
     * an upper case letter. Otherwise, the specified code point is returned
     * unchanged.
     *
     * @param codePoint
     *            the code point to check.
     * @return if {@code codePoint} is an upper case character then its lower
     *         case counterpart, otherwise just {@code codePoint}.
     */
    public static native int toLowerCase(int codePoint);
    
    /**
     * Returns a String object representing this character's value. Converts this Character object to a string. The result is a string whose length is 1. The string's sole component is the primitive char value represented by this object.
     */
    public java.lang.String toString(){
        return new String(new char[] {value});
    }

    /**
     * Converts the character argument to uppercase; if the character has no uppercase equivalent, the character itself is returned.
     * Note that by default CLDC only supports the ISO Latin-1 range of characters.
     */
    public static char toUpperCase(char ch){
        return (char) toUpperCase((int) ch);
    }

    /**
     * Returns the upper case equivalent for the specified code point if the
     * code point is a lower case letter. Otherwise, the specified code point is
     * returned unchanged.
     *
     * @param codePoint
     *            the code point to convert.
     * @return if {@code codePoint} is a lower case character then its upper
     *         case counterpart, otherwise just {@code codePoint}.
     */
    public static int toUpperCase(int codePoint) {
        // Optimized case for ASCII
        if ('a' <= codePoint && codePoint <= 'z') {
            return (char) (codePoint - ('a' - 'A'));
        }
        /*if (codePoint < 181) {
            return codePoint;
        }
        return toUpperCaseImpl(codePoint);*/
        return codePoint;
    }



    /**
     * <p>
     * A test for determining if the <code>codePoint</code> is a valid Unicode
     * code point.
     * </p>
     * 
     * @param codePoint The code point to test.
     * @return A boolean value.
     * @since 1.5
     */
    public static boolean isValidCodePoint(int codePoint) {
        return (MIN_CODE_POINT <= codePoint && MAX_CODE_POINT >= codePoint);
    }



    /**
     * <p>
     * A test for determining if the <code>codePoint</code> is within the
     * supplementary code point range.
     * </p>
     * 
     * @param codePoint The code point to test.
     * @return A boolean value.
     * @since 1.5
     */
    public static boolean isSupplementaryCodePoint(int codePoint) {
        return (MIN_SUPPLEMENTARY_CODE_POINT <= codePoint && MAX_CODE_POINT >= codePoint);
    }

    /**
     * <p>
     * A test for determining if the <code>char</code> is a high
     * surrogate/leading surrogate unit that's used for representing
     * supplementary characters in UTF-16 encoding.
     * </p>
     * 
     * @param ch The <code>char</code> unit to test.
     * @return A boolean value.
     * @since 1.5
     * @see #isLowSurrogate(char)
     */
    public static boolean isHighSurrogate(char ch) {
        return (MIN_HIGH_SURROGATE <= ch && MAX_HIGH_SURROGATE >= ch);
    }

    public static char highSurrogate(int codePoint) {
        codePoint -= MIN_SUPPLEMENTARY_CODE_POINT;
        return (char) (HIGH_SURROGATE_BITS | (codePoint >> MEANINGFUL_SURROGATE_BITS) & SURROGATE_BIT_INV_MASK);
    }

    public static char lowSurrogate(int codePoint) {
        return (char) (LOW_SURROGATE_BITS | codePoint & SURROGATE_BIT_INV_MASK);
    }


    /**
     * <p>
     * A test for determining if the <code>char</code> is a high
     * surrogate/leading surrogate unit that's used for representing
     * supplementary characters in UTF-16 encoding.
     * </p>
     * 
     * @param ch The <code>char</code> unit to test.
     * @return A boolean value.
     * @since 1.5
     * @see #isHighSurrogate(char)
     */
    public static boolean isLowSurrogate(char ch) {
        return (MIN_LOW_SURROGATE <= ch && MAX_LOW_SURROGATE >= ch);
    }

    /**
     * <p>
     * A test for determining if the <code>char</code> pair is a valid
     * surrogate pair.
     * </p>
     * 
     * @param high The high surrogate unit to test.
     * @param low The low surrogate unit to test.
     * @return A boolean value.
     * @since 1.5
     * @see #isHighSurrogate(char)
     * @see #isLowSurrogate(char)
     */
    public static boolean isSurrogatePair(char high, char low) {
        return (isHighSurrogate(high) && isLowSurrogate(low));
    }

    /**
     * <p>
     * Calculates the number of <code>char</code> values required to represent
     * the Unicode code point. This method only tests if the
     * <code>codePoint</code> is greater than or equal to <code>0x10000</code>,
     * in which case <code>2</code> is returned, otherwise <code>1</code>.
     * To test if the code point is valid, use the
     * {@link #isValidCodePoint(int)} method.
     * </p>
     * 
     * @param codePoint The code point to test.
     * @return An <code>int</code> value of 2 or 1.
     * @since 1.5
     * @see #isValidCodePoint(int)
     * @see #isSupplementaryCodePoint(int)
     */
    public static int charCount(int codePoint) {
        return (codePoint >= 0x10000 ? 2 : 1);
    }

    /**
     * <p>
     * Converts a surrogate pair into a Unicode code point. This method assume
     * that the pair are valid surrogates. If the pair are NOT valid surrogates,
     * then the result is indeterminate. The
     * {@link #isSurrogatePair(char, char)} method should be used prior to this
     * method to validate the pair.
     * </p>
     * 
     * @param high The high surrogate unit.
     * @param low The low surrogate unit.
     * @return The decoded code point.
     * @since 1.5
     * @see #isSurrogatePair(char, char)
     */
    public static int toCodePoint(char high, char low) {
        // See RFC 2781, Section 2.2
        // http://www.faqs.org/rfcs/rfc2781.html
        int h = (high & 0x3FF) << 10;
        int l = low & 0x3FF;
        return (h | l) + 0x10000;
    }

    /**
     * <p>
     * Returns the code point at the index in the <code>CharSequence</code>.
     * If <code>char</code> unit at the index is a high-surrogate unit, the
     * next index is less than the length of the sequence and the
     * <code>char</code> unit at the next index is a low surrogate unit, then
     * the code point represented by the pair is returned; otherwise the
     * <code>char</code> unit at the index is returned.
     * </p>
     * 
     * @param seq The sequence of <code>char</code> units.
     * @param index The index into the <code>seq</code> to retrieve and
     *        convert.
     * @return The Unicode code point.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if the <code>index</code> is negative
     *         or greater than or equal to <code>seq.length()</code>.
     * @since 1.5
     */
    public static int codePointAt(CharSequence seq, int index) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length();
        if (index < 0 || index >= len) {
            throw new IndexOutOfBoundsException();
        }

        char high = seq.charAt(index++);
        if (index >= len) {
            return high;
        }
        char low = seq.charAt(index);
        if (isSurrogatePair(high, low)) {
            return toCodePoint(high, low);
        }
        return high;
    }

    /**
     * <p>
     * Returns the code point at the index in the <code>char[]</code>. If
     * <code>char</code> unit at the index is a high-surrogate unit, the next
     * index is less than the length of the sequence and the <code>char</code>
     * unit at the next index is a low surrogate unit, then the code point
     * represented by the pair is returned; otherwise the <code>char</code>
     * unit at the index is returned.
     * </p>
     * 
     * @param seq The sequence of <code>char</code> units.
     * @param index The index into the <code>seq</code> to retrieve and
     *        convert.
     * @return The Unicode code point.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if the <code>index</code> is negative
     *         or greater than or equal to <code>seq.length()</code>.
     * @since 1.5
     */
    public static int codePointAt(char[] seq, int index) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length;
        if (index < 0 || index >= len) {
            throw new IndexOutOfBoundsException();
        }

        char high = seq[index++];
        if (index >= len) {
            return high;
        }
        char low = seq[index];
        if (isSurrogatePair(high, low)) {
            return toCodePoint(high, low);
        }
        return high;
    }

    /**
     * <p>
     * Returns the code point at the index in the <code>char[]</code> that's
     * within the limit. If <code>char</code> unit at the index is a
     * high-surrogate unit, the next index is less than the <code>limit</code>
     * and the <code>char</code> unit at the next index is a low surrogate
     * unit, then the code point represented by the pair is returned; otherwise
     * the <code>char</code> unit at the index is returned.
     * </p>
     * 
     * @param seq The sequence of <code>char</code> units.
     * @param index The index into the <code>seq</code> to retrieve and
     *        convert.
     * @param limit The exclusive index into the <code>seq</code> that marks
     *        the end of the units that can be used.
     * @return The Unicode code point.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if the <code>index</code> is
     *         negative, greater than or equal to <code>limit</code>,
     *         <code>limit</code> is negative or <code>limit</code> is
     *         greater than the length of <code>seq</code>.
     * @since 1.5
     */
    public static int codePointAt(char[] seq, int index, int limit) {
        if (index < 0 || index >= limit || limit < 0 || limit > seq.length) {
            throw new IndexOutOfBoundsException();
        }       

        char high = seq[index++];
        if (index >= limit) {
            return high;
        }
        char low = seq[index];
        if (isSurrogatePair(high, low)) {
            return toCodePoint(high, low);
        }
        return high;
    }

    /**
     * <p>
     * Returns the Unicode code point that proceeds the <code>index</code> in
     * the <code>CharSequence</code>. If the <code>char</code> unit at
     * <code>index - 1</code> is within the low surrogate range, the value
     * <code>index - 2</code> isn't negative and the <code>char</code> unit
     * at <code>index - 2</code> is within the high surrogate range, then the
     * supplementary code point made up of the surrogate pair is returned;
     * otherwise, the <code>char</code> value at <code>index - 1</code> is
     * returned.
     * </p>
     * 
     * @param seq The <code>CharSequence</code> to search.
     * @param index The index into the <code>seq</code>.
     * @return A Unicode code point.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>index</code> is less than 1
     *         or greater than <code>seq.length()</code>.
     * @since 1.5
     */
    public static int codePointBefore(CharSequence seq, int index) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length();
        if (index < 1 || index > len) {
            throw new IndexOutOfBoundsException();
        }

        char low = seq.charAt(--index);
        if (--index < 0) {
            return low;
        }
        char high = seq.charAt(index);
        if (isSurrogatePair(high, low)) {
            return toCodePoint(high, low);
        }
        return low;
    }

    /**
     * <p>
     * Returns the Unicode code point that proceeds the <code>index</code> in
     * the <code>char[]</code>. If the <code>char</code> unit at
     * <code>index - 1</code> is within the low surrogate range, the value
     * <code>index - 2</code> isn't negative and the <code>char</code> unit
     * at <code>index - 2</code> is within the high surrogate range, then the
     * supplementary code point made up of the surrogate pair is returned;
     * otherwise, the <code>char</code> value at <code>index - 1</code> is
     * returned.
     * </p>
     * 
     * @param seq The <code>char[]</code> to search.
     * @param index The index into the <code>seq</code>.
     * @return A Unicode code point.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>index</code> is less than 1
     *         or greater than <code>seq.length</code>.
     * @since 1.5
     */
    public static int codePointBefore(char[] seq, int index) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length;
        if (index < 1 || index > len) {
            throw new IndexOutOfBoundsException();
        }

        char low = seq[--index];
        if (--index < 0) {
            return low;
        }
        char high = seq[index];
        if (isSurrogatePair(high, low)) {
            return toCodePoint(high, low);
        }
        return low;
    }

    /**
     * <p>
     * Returns the Unicode code point that proceeds the <code>index</code> in
     * the <code>char[]</code> and isn't less than <code>start</code>. If
     * the <code>char</code> unit at <code>index - 1</code> is within the
     * low surrogate range, the value <code>index - 2</code> isn't less than
     * <code>start</code> and the <code>char</code> unit at
     * <code>index - 2</code> is within the high surrogate range, then the
     * supplementary code point made up of the surrogate pair is returned;
     * otherwise, the <code>char</code> value at <code>index - 1</code> is
     * returned.
     * </p>
     * 
     * @param seq The <code>char[]</code> to search.
     * @param index The index into the <code>seq</code>.
     * @return A Unicode code point.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>index</code> is less than or
     *         equal to <code>start</code>, <code>index</code> is greater
     *         than <code>seq.length</code>, <code>start</code> is not
     *         negative and <code>start</code> is greater than
     *         <code>seq.length</code>.
     * @since 1.5
     */
    public static int codePointBefore(char[] seq, int index, int start) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length;
        if (index <= start || index > len || start < 0 || start >= len) {
            throw new IndexOutOfBoundsException();
        }

        char low = seq[--index];
        if (--index < start) {
            return low;
        }
        char high = seq[index];
        if (isSurrogatePair(high, low)) {
            return toCodePoint(high, low);
        }
        return low;
    }

    /**
     * <p>
     * Converts the Unicode code point, <code>codePoint</code>, into a UTF-16
     * encoded sequence and copies the value(s) into the
     * <code>char[]</code> <code>dst</code>, starting at the index
     * <code>dstIndex</code>.
     * </p>
     * 
     * @param codePoint The Unicode code point to encode.
     * @param dst The <code>char[]</code> to copy the encoded value into.
     * @param dstIndex The index to start copying into <code>dst</code>.
     * @return The number of <code>char</code> value units copied into
     *         <code>dst</code>.
     * @throws IllegalArgumentException if <code>codePoint</code> is not a
     *         valid Unicode code point.
     * @throws NullPointerException if <code>dst</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>dstIndex</code> is negative,
     *         greater than or equal to <code>dst.length</code> or equals
     *         <code>dst.length - 1</code> when <code>codePoint</code> is a
     *         {@link #isSupplementaryCodePoint(int) supplementary code point}.
     * @since 1.5
     */
    public static int toChars(int codePoint, char[] dst, int dstIndex) {
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException();
        }
        if (dst == null) {
            throw new NullPointerException();
        }
        if (dstIndex < 0 || dstIndex >= dst.length) {
            throw new IndexOutOfBoundsException();
        }

        if (isSupplementaryCodePoint(codePoint)) {
            if (dstIndex == dst.length - 1) {
                throw new IndexOutOfBoundsException();
            }
            // See RFC 2781, Section 2.1
            // http://www.faqs.org/rfcs/rfc2781.html
            int cpPrime = codePoint - 0x10000;
            int high = 0xD800 | ((cpPrime >> 10) & 0x3FF);
            int low = 0xDC00 | (cpPrime & 0x3FF);
            dst[dstIndex] = (char) high;
            dst[dstIndex + 1] = (char) low;
            return 2;
        }

        dst[dstIndex] = (char) codePoint;
        return 1;
    }

    /**
     * <p>
     * Converts the Unicode code point, <code>codePoint</code>, into a UTF-16
     * encoded sequence that is returned as a <code>char[]</code>.
     * </p>
     * 
     * @param codePoint The Unicode code point to encode.
     * @return The UTF-16 encoded <code>char</code> sequence; if code point is
     *         a {@link #isSupplementaryCodePoint(int) supplementary code point},
     *         then a 2 <code>char</code> array is returned, otherwise a 1
     *         <code>char</code> array is returned.
     * @throws IllegalArgumentException if <code>codePoint</code> is not a
     *         valid Unicode code point.
     * @since 1.5
     */
    public static char[] toChars(int codePoint) {
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException();
        }

        if (isSupplementaryCodePoint(codePoint)) {
            int cpPrime = codePoint - 0x10000;
            int high = 0xD800 | ((cpPrime >> 10) & 0x3FF);
            int low = 0xDC00 | (cpPrime & 0x3FF);
            return new char[] { (char) high, (char) low };
        }
        return new char[] { (char) codePoint };
    }

    /**
     * <p>
     * Counts the number of Unicode code points in the subsequence of the
     * <code>CharSequence</code>, as delineated by the
     * <code>beginIndex</code> and <code>endIndex</code>. Any surrogate
     * values with missing pair values will be counted as 1 code point.
     * </p>
     * 
     * @param seq The <code>CharSequence</code> to look through.
     * @param beginIndex The inclusive index to begin counting at.
     * @param endIndex The exclusive index to stop counting at.
     * @return The number of Unicode code points.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>beginIndex</code> is
     *         negative, greater than <code>seq.length()</code> or greater
     *         than <code>endIndex</code>.
     * @since 1.5
     */
    public static int codePointCount(CharSequence seq, int beginIndex,
            int endIndex) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length();
        if (beginIndex < 0 || endIndex > len || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }

        int result = 0;
        for (int i = beginIndex; i < endIndex; i++) {
            char c = seq.charAt(i);
            if (isHighSurrogate(c)) {
                if (++i < endIndex) {
                    c = seq.charAt(i);
                    if (!isLowSurrogate(c)) {
                        result++;
                    }
                }
            }
            result++;
        }
        return result;
    }

    /**
     * <p>
     * Counts the number of Unicode code points in the subsequence of the
     * <code>char[]</code>, as delineated by the <code>offset</code> and
     * <code>count</code>. Any surrogate values with missing pair values will
     * be counted as 1 code point.
     * </p>
     * 
     * @param seq The <code>char[]</code> to look through.
     * @param offset The inclusive index to begin counting at.
     * @param count The number of <code>char</code> values to look through in
     *        <code>seq</code>.
     * @return The number of Unicode code points.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>offset</code> or
     *         <code>count</code> is negative or if <code>endIndex</code> is
     *         greater than <code>seq.length</code>.
     * @since 1.5
     */
    public static int codePointCount(char[] seq, int offset, int count) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length;
        int endIndex = offset + count;
        if (offset < 0 || count < 0 || endIndex > len) {
            throw new IndexOutOfBoundsException();
        }

        int result = 0;
        for (int i = offset; i < endIndex; i++) {
            char c = seq[i];
            if (isHighSurrogate(c)) {
                if (++i < endIndex) {
                    c = seq[i];
                    if (!isLowSurrogate(c)) {
                        result++;
                    }
                }
            }
            result++;
        }
        return result;
    }

    /**
     * <p>
     * Determines the index into the <code>CharSequence</code> that is offset
     * (measured in code points and specified by <code>codePointOffset</code>),
     * from the <code>index</code> argument.
     * </p>
     * 
     * @param seq The <code>CharSequence</code> to find the index within.
     * @param index The index to begin from, within the
     *        <code>CharSequence</code>.
     * @param codePointOffset The number of code points to look back or
     *        forwards; may be a negative or positive value.
     * @return The calculated index that is <code>codePointOffset</code> code
     *         points from <code>index</code>.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>index</code> is negative,
     *         greater than <code>seq.length()</code>, there aren't enough
     *         values in <code>seq</code> after <code>index</code> or before
     *         <code>index</code> if <code>codePointOffset</code> is
     *         negative.
     * @since 1.5
     */
    public static int offsetByCodePoints(CharSequence seq, int index,
            int codePointOffset) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int len = seq.length();
        if (index < 0 || index > len) {
            throw new IndexOutOfBoundsException();
        }

        if (codePointOffset == 0) {
            return index;
        }

        if (codePointOffset > 0) {
            int codePoints = codePointOffset;
            int i = index;
            while (codePoints > 0) {
                codePoints--;
                if (i >= len) {
                    throw new IndexOutOfBoundsException();
                }
                if (isHighSurrogate(seq.charAt(i))) {
                    int next = i + 1;
                    if (next < len && isLowSurrogate(seq.charAt(next))) {
                        i++;
                    }
                }
                i++;
            }
            return i;
        }

        int codePoints = -codePointOffset;
        int i = index;
        while (codePoints > 0) {
            codePoints--;
            i--;
            if (i < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (isLowSurrogate(seq.charAt(i))) {
                int prev = i - 1;
                if (prev >= 0 && isHighSurrogate(seq.charAt(prev))) {
                    i--;
                }
            }
        }
        return i;
    }

    /**
     * <p>
     * Determines the index into the <code>char[]</code> that is offset
     * (measured in code points and specified by <code>codePointOffset</code>),
     * from the <code>index</code> argument and is within the subsequence as
     * delineated by <code>start</code> and <code>count</code>.
     * </p>
     * 
     * @param seq The <code>char[]</code> to find the index within.
     * 
     * @param index The index to begin from, within the <code>char[]</code>.
     * @param codePointOffset The number of code points to look back or
     *        forwards; may be a negative or positive value.
     * @param start The inclusive index that marks the beginning of the
     *        subsequence.
     * @param count The number of <code>char</code> values to include within
     *        the subsequence.
     * @return The calculated index that is <code>codePointOffset</code> code
     *         points from <code>index</code>.
     * @throws NullPointerException if <code>seq</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>start</code> or
     *         <code>count</code> is negative, <code>start + count</code>
     *         greater than <code>seq.length</code>, <code>index</code> is
     *         less than <code>start</code>, <code>index</code> is greater
     *         than <code>start + count</code> or there aren't enough values
     *         in <code>seq</code> after <code>index</code> or before
     *         <code>index</code> if <code>codePointOffset</code> is
     *         negative.
     * @since 1.5
     */
    public static int offsetByCodePoints(char[] seq, int start, int count,
            int index, int codePointOffset) {
        if (seq == null) {
            throw new NullPointerException();
        }
        int end = start + count;
        if (start < 0 || count < 0 || end > seq.length || index < start
                || index > end) {
            throw new IndexOutOfBoundsException();
        }

        if (codePointOffset == 0) {
            return index;
        }

        if (codePointOffset > 0) {
            int codePoints = codePointOffset;
            int i = index;
            while (codePoints > 0) {
                codePoints--;
                if (i >= end) {
                    throw new IndexOutOfBoundsException();
                }
                if (isHighSurrogate(seq[i])) {
                    int next = i + 1;
                    if (next < end && isLowSurrogate(seq[next])) {
                        i++;
                    }
                }
                i++;
            }
            return i;
        }

        int codePoints = -codePointOffset;
        int i = index;
        while (codePoints > 0) {
            codePoints--;
            i--;
            if (i < start) {
                throw new IndexOutOfBoundsException();
            }
            if (isLowSurrogate(seq[i])) {
                int prev = i - 1;
                if (prev >= start && isHighSurrogate(seq[prev])) {
                    i--;
                }
            }
        }
        return i;
    }

    /**
     * Reverse the order of the first and second bytes in character
     * @param c
     *            the character
     * @return    the character with reordered bytes.
     */
    public static char reverseBytes(char c) {
        return (char)((c<<8) | (c>>8));
    }


    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    public static Character valueOf(char i) {
        return new Character(i);
    }

    /**
     * See {@link #isWhitespace(int)}.
     */
    public static boolean isWhitespace(char c) {
        return isWhitespace((int) c);
    }

    public static boolean isSpaceChar(char c) {
        return c == ' ';
    }

    public static byte getDirectionality(char ch) {
        throw new UnsupportedOperationException("Character.getDirectionality() not supported on this platform");
    }
    
    public static int getType(char c) {
        return getType((int) c);
    }

    public static boolean isBmpCodePoint(int codePoint) {
        return codePoint > 0 && codePoint <= MAX_VALUE;
    }
    
    public static boolean isSurrogate(char ch) {
        return isHighSurrogate(ch) || isLowSurrogate(ch);
    }
    
    private static UnicodeHelper.Range[] classMapping;
    private static UnicodeHelper.Range[] getClasses() {
       throw new UnsupportedOperationException("UnicodeHelper.getClasses() not supported");
    }
    
    public static int getType(int codePoint) {
        if (isBmpCodePoint(codePoint) && isSurrogate((char) codePoint)) {
            return SURROGATE;
        }
        UnicodeHelper.Range[] classes = getClasses();
        int l = 0;
        int u = classes.length - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            UnicodeHelper.Range range = classes[i];
            if (codePoint >= range.end) {
                l = i + 1;
            } else if (codePoint < range.start) {
                u = i - 1;
            } else {
                return range.data[codePoint - range.start];
            }
        }
        return 0;
    }
    
    /**
     * Returns true if the given code point is a Unicode whitespace character.
     * The exact set of characters considered as whitespace varies with Unicode version.
     * Note that non-breaking spaces are not considered whitespace.
     * Note also that line separators are considered whitespace; see {@link #isSpaceChar}
     * for an alternative.
     */
    public static boolean isWhitespace(int codePoint) {
        // We don't just call into icu4c because of the JNI overhead. Ideally we'd fix that.
        // Any ASCII whitespace character?
        if ((codePoint >= 0x1c && codePoint <= 0x20) || (codePoint >= 0x09 && codePoint <= 0x0d)) {
            return true;
        }
        if (codePoint < 0x1000) {
            return false;
        }
        // OGHAM SPACE MARK or MONGOLIAN VOWEL SEPARATOR?
        if (codePoint == 0x1680 || codePoint == 0x180e) {
            return true;
        }
        if (codePoint < 0x2000) {
            return false;
        }
        // Exclude General Punctuation's non-breaking spaces (which includes FIGURE SPACE).
        if (codePoint == 0x2007 || codePoint == 0x202f) {
            return false;
        }
        if (codePoint <= 0xffff) {
            // Other whitespace from General Punctuation...
            return codePoint <= 0x200a || codePoint == 0x2028 || codePoint == 0x2029 || codePoint == 0x205f ||
                codePoint == 0x3000; // ...or CJK Symbols and Punctuation?
        }
        // Let icu4c worry about non-BMP code points.
        return false;
    }

    public int compareTo(Character another) {
        return toString().compareTo(String.valueOf(another.value));
    }

    private static String _codepointToString(int cp) {
        StringBuilder sb = new StringBuilder();
        if (isBmpCodePoint(cp)) {
            sb.append((char) cp);
        } else if (isValidCodePoint(cp)) {
            sb.append(highSurrogate(cp));
            sb.append(lowSurrogate(cp));
        } else {
            sb.append('?');
        }
        return sb.toString();
    }

    public static int toTitleCase(int codePoint) {
        return _codepointToString(codePoint).charAt(0);
    }

    public static char toTitleCase(char c) {
        return String.valueOf(c).toUpperCase().charAt(0);
    }
    
}


class UnicodeHelper {
    private UnicodeHelper() {
    }

    static char hexDigit(int value) {
        return value < 10 ? (char) ('0' + value) : (char) ('A' + value);
    }

    static int valueOfHexDigit(char digit) {
        return digit <= '9' ? digit - '0' : digit - 'A' + 10;
    }

    public static class Range {
        public final int start;
        public final int end;
        public final byte[] data;

        public Range(int start, int end, byte[] data) {
            this.start = start;
            this.end = end;
            this.data = data;
        }
    }

    public static String encodeIntPairsDiff(int[] data) {
        StringBuilder sb = new StringBuilder();
        Base46.encodeUnsigned(sb, data.length / 2);
        int lastKey = 0;
        int lastValue = 0;
        for (int i = 0; i < data.length; i += 2) {
            int key = data[i];
            int value = data[i + 1];
            Base46.encode(sb, key - lastKey);
            Base46.encode(sb, value - lastValue);
            lastKey = key;
            lastValue = value;
        }
        return sb.toString();
    }

    public static int[] decodeIntPairsDiff(String text) {
        CharFlow flow = new CharFlow(text.toCharArray());
        int sz = Base46.decodeUnsigned(flow);
        int[] data = new int[sz * 2];
        int j = 0;
        int lastKey = 0;
        int lastValue = 0;
        for (int i = 0; i < sz; i++) {
            lastKey += Base46.decode(flow);
            lastValue += Base46.decode(flow);
            data[j++] = lastKey;
            data[j++] = lastValue;
        }
        return data;
    }

    public static String encodeIntDiff(int[] data) {
        StringBuilder sb = new StringBuilder();
        Base46.encodeUnsigned(sb, data.length);
        int last = 0;
        for (int i = 0; i < data.length; i++) {
            int v = data[i];
            Base46.encode(sb, v - last);
            last = v;
        }
        return sb.toString();
    }

    public static int[] decodeIntDiff(String text) {
        CharFlow flow = new CharFlow(text.toCharArray());
        int sz = Base46.decodeUnsigned(flow);
        int[] data = new int[sz];
        int last = 0;
        for (int i = 0; i < sz; i++) {
            last += Base46.decode(flow);
            data[i] = last;
        }
        return data;
    }

    public static char encodeByte(byte b) {
        if (b < '\"' - ' ') {
            return (char) (b + ' ');
        } else if (b < '\\' - ' ' - 1) {
            return (char) (b + ' ' + 1);
        } else {
            return (char) (b + ' ' + 2);
        }
    }

    public static byte decodeByte(char c) {
        if (c > '\\') {
            return (byte) (c - ' ' - 2);
        } else if (c > '"') {
            return (byte) (c - ' ' - 1);
        } else {
            return (byte) (c - ' ');
        }
    }

    public static String compressRle(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length;) {
            byte b = bytes[i];
            if (i < bytes.length - 1 && b == bytes[i + 1]) {
                int count = 0;
                while (count < 16384 && bytes[i + count] == b) {
                    ++count;
                }
                i += count;
                if (count < 80) {
                    sb.append(UnicodeHelper.encodeByte((byte) (b + 32)));
                    sb.append(UnicodeHelper.encodeByte((byte) count));
                } else {
                    sb.append(UnicodeHelper.encodeByte((byte) 64));
                    sb.append(UnicodeHelper.encodeByte(b));
                    for (int j = 0; j < 3; ++j) {
                        sb.append(UnicodeHelper.encodeByte((byte) (count & 0x3F)));
                        count /= 0x40;
                    }
                }
            } else {
                sb.append(UnicodeHelper.encodeByte(bytes[i++]));
            }
        }
        return sb.toString();
    }

    public static Range[] extractRle(String encoded) {
        Range[] ranges = new Range[16384];
        byte[] buffer = new byte[16384];
        int index = 0;
        int rangeIndex = 0;
        int codePoint = 0;
        for (int i = 0; i < encoded.length(); ++i) {
            byte b = decodeByte(encoded.charAt(i));
            int count;
            if (b == 64) {
                b = decodeByte(encoded.charAt(++i));
                count = 0;
                int pos = 1;
                for (int j = 0; j < 3; ++j) {
                    byte digit = decodeByte(encoded.charAt(++i));
                    count |= pos * digit;
                    pos *= 0x40;
                }
            } else if (b >= 32) {
                b -= 32;
                count = decodeByte(encoded.charAt(++i));
            } else {
                count = 1;
            }
            if (b != 0 || count < 128) {
                if (index + count >= buffer.length) {
                    ranges[rangeIndex++] = new Range(codePoint, codePoint + index, Arrays.copyOf(buffer, index));
                    codePoint += index + count;
                    index = 0;
                }
                while (count-- > 0) {
                    buffer[index++] = b;
                }
            } else {
                if (index > 0) {
                    ranges[rangeIndex++] = new Range(codePoint, codePoint + index, Arrays.copyOf(buffer, index));
                }
                codePoint += index + count;
                index = 0;
            }
        }
        return Arrays.copyOf(ranges, rangeIndex);
    }
    
    
}

/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
class CharFlow {
    public final char[] characters;
    public int pointer;

    public CharFlow(char[] characters) {
        this.characters = characters;
    }
}

/**
 * <p>Base47 encoding is best fit for encoding varible length numbers in JavaScript strings.</p>
 *
 * <p>47 = (int)(93 / 2), where 94 is the number of ASCII characters representable in JavaScript string
 * without escaping. These characters are encoded by one byte in UTF-8 charset. All other character require
 * either escaping or two or more bytes in UTF-8.</p>
 *
 * <p>We divide 93 by 2 for the following trick. Representing integers takes 5 bytes in Base93. However,
 * we often need smaller integers that might be represented by one or two bytes. By each Base93 digit we
 * can encode both part of the number and a flag indicating whether the number contains one more digit.</p>
 *
 * @author Alexey Andreev
 */
final class Base46 {
    private Base46() {
    }

    public static void encodeUnsigned(StringBuilder sb, int number) {
        boolean hasMore;
        do {
            int digit = number % 46;
            number /= 46;
            hasMore = number > 0;
            digit = digit * 2 + (hasMore ? 1 : 0);
            sb.append(encodeDigit(digit));
        } while (hasMore);
    }

    public static void encode(StringBuilder sb, int number) {
        encodeUnsigned(sb, Math.abs(number) * 2 + (number >= 0 ? 0 : 1));
    }

    public static void encodeUnsigned(StringBuilder sb, long number) {
        boolean hasMore;
        do {
            int digit = (int) (number % 46);
            number /= 46;
            hasMore = number > 0;
            digit = digit * 2 + (hasMore ? 1 : 0);
            sb.append(encodeDigit(digit));
        } while (hasMore);
    }

    public static void encode(StringBuilder sb, long number) {
        encodeUnsigned(sb, Math.abs(number) * 2 + (number >= 0 ? 0 : 1));
    }

    public static int decodeUnsigned(CharFlow seq) {
        int number = 0;
        int pos = 1;
        boolean hasMore;
        do {
            int digit = decodeDigit(seq.characters[seq.pointer++]);
            hasMore = digit % 2 == 1;
            number += pos * (digit / 2);
            pos *= 46;
        } while (hasMore);
        return number;
    }

    public static int decode(CharFlow seq) {
        int number = decodeUnsigned(seq);
        int result = number / 2;
        if (number % 2 != 0) {
            result = -result;
        }
        return result;
    }

    public static long decodeUnsignedLong(CharFlow seq) {
        long number = 0;
        long pos = 1;
        boolean hasMore;
        do {
            int digit = decodeDigit(seq.characters[seq.pointer++]);
            hasMore = digit % 2 == 1;
            number += pos * (digit / 2);
            pos *= 46;
        } while (hasMore);
        return number;
    }

    public static long decodeLong(CharFlow seq) {
        long number = decodeUnsigned(seq);
        long result = number / 2;
        if (number % 2 != 0) {
            result = -result;
        }
        return result;
    }

    public static char encodeDigit(int digit) {
        if (digit < 2) {
            return (char) (digit + ' ');
        } else if (digit < 59) {
            return (char) (digit + 1 + ' ');
        } else {
            return (char) (digit + 2 + ' ');
        }
    }

    public static int decodeDigit(char c) {
        if (c < '"') {
            return c - ' ';
        } else if (c < '\\') {
            return c - ' ' - 1;
        } else {
            return c - ' ' - 2;
        }
    }



}
