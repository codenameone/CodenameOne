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
/// The Character class wraps a value of the primitive type char in an object. An object of type Character contains a single field whose type is char.
/// In addition, this class provides several methods for determining the type of a character and converting characters from uppercase to lowercase and vice versa.
/// Character information is based on the Unicode Standard, version 3.0. However, in order to reduce footprint, by default the character property and case conversion operations in CLDC are available only for the ISO Latin-1 range of characters. Other Unicode character blocks can be supported as necessary.
/// Since: JDK1.0, CLDC 1.0
public final class Character{
    /// The maximum radix available for conversion to and from Strings.
    /// See Also:Integer.toString(int, int), Integer.valueOf(java.lang.String), Constant Field Values
    public static final int MAX_RADIX=36;

    /// The constant value of this field is the largest value of type char.
    /// Since: JDK1.0.2 See Also:Constant Field Values
    public static final char MAX_VALUE=(char)65535;

    /// The minimum radix available for conversion to and from Strings.
    /// See Also:Integer.toString(int, int), Integer.valueOf(java.lang.String), Constant Field Values
    public static final int MIN_RADIX=2;

    /// The constant value of this field is the smallest value of type char.
    /// Since: JDK1.0.2 See Also:Constant Field Values
    public static final char MIN_VALUE=(char)0;
    

    /// Constructs a Character object and initializes it so that it represents the primitive value argument.
    /// value - value for the new Character object.
    public Character(char value){
         //TODO codavaj!!
    }

    /// Returns the value of this Character object.
    public char charValue(){
        return ' '; //TODO codavaj!!
    }

    /// Returns the numeric value of the character ch in the specified radix.
    public static int digit(char ch, int radix){
        return 0; //TODO codavaj!!
    }

    /// Compares this object against the specified object. The result is true if and only if the argument is not null and is a Character object that represents the same char value as this object.
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /// Returns a hash code for this Character.
    public int hashCode(){
        return 0; //TODO codavaj!!
    }

    /// Determines if the specified character is alphabetic.
    static boolean isLetterCompat(char ch){
        return isLowerCase(ch) || isUpperCase(ch);
    }

    /// Determines if the specified character is numeric.
    static boolean isDigitCompat(char ch){
        return isDigit(ch);
    }

    /// Determines if the specified character is alphabetic or numeric.
    static boolean isLetterOrDigitCompat(char ch){
        return isLetterCompat(ch) || isDigitCompat(ch);
    }

    /// Determines if the specified character is a digit.
    public static boolean isDigit(char ch){
        return false; //TODO codavaj!!
    }

    /// Determines if the specified character is a lowercase character.
    /// Note that by default CLDC only supports the ISO Latin-1 range of characters.
    /// Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF), the following are lowercase:
    /// a b c d e f g h i j k l m n o p q r s t u v w x y z u00DF u00E0 u00E1 u00E2 u00E3 u00E4 u00E5 u00E6 u00E7 u00E8 u00E9 u00EA u00EB u00EC u00ED u00EE u00EF u00F0 u00F1 u00F2 u00F3 u00F4 u00F5 u00F6 u00F8 u00F9 u00FA u00FB u00FC u00FD u00FE u00FF
    public static boolean isLowerCase(char ch){
        return false; //TODO codavaj!!
    }

    /// Determines if the specified character is an uppercase character.
    /// Note that by default CLDC only supports the ISO Latin-1 range of characters.
    /// Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF), the following are uppercase:
    /// A B C D E F G H I J K L M N O P Q R S T U V W X Y Z u00C0 u00C1 u00C2 u00C3 u00C4 u00C5 u00C6 u00C7 u00C8 u00C9 u00CA u00CB u00CC u00CD u00CE u00CF u00D0 u00D1 u00D2 u00D3 u00D4 u00D5 u00D6 u00D8 u00D9 u00DA u00DB u00DC u00DD u00DE
    public static boolean isUpperCase(char ch){
        return false; //TODO codavaj!!
    }

    /// The given character is mapped to its lowercase equivalent; if the character has no lowercase equivalent, the character itself is returned.
    /// Note that by default CLDC only supports the ISO Latin-1 range of characters.
    public static char toLowerCase(char ch){
        return ' '; //TODO codavaj!!
    }

    /// Returns a String object representing this character's value. Converts this Character object to a string. The result is a string whose length is 1. The string's sole component is the primitive char value represented by this object.
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /// Converts the character argument to uppercase; if the character has no uppercase equivalent, the character itself is returned.
    /// Note that by default CLDC only supports the ISO Latin-1 range of characters.
    public static char toUpperCase(char ch){
        return ' '; //TODO codavaj!!
    }


    /// Minimum value of a high surrogate or leading surrogate unit in UTF-16
    /// encoding - `'\uD800'`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final char MIN_HIGH_SURROGATE = '\uD800';

    /// Maximum value of a high surrogate or leading surrogate unit in UTF-16
    /// encoding - `'\uDBFF'`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';

    /// Minimum value of a low surrogate or trailing surrogate unit in UTF-16
    /// encoding - `'\uDC00'`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final char MIN_LOW_SURROGATE = '\uDC00';

    /// Maximum value of a low surrogate or trailing surrogate unit in UTF-16
    /// encoding - `'\uDFFF'`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final char MAX_LOW_SURROGATE = '\uDFFF';

    /// Minimum value of a surrogate unit in UTF-16 encoding - `'\uD800'`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final char MIN_SURROGATE = '\uD800';

    /// Maximum value of a surrogate unit in UTF-16 encoding - `'\uDFFF'`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final char MAX_SURROGATE = '\uDFFF';

    /// Minimum value of a supplementary code point - `U+010000`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;

    /// Minimum code point value - `U+0000`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final int MIN_CODE_POINT = 0x000000;

    /// Maximum code point value - `U+10FFFF`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final int MAX_CODE_POINT = 0x10FFFF;

    /// Constant for the number of bits to represent a `char` in
    /// two's compliment form.
    ///
    /// #### Since
    ///
    /// 1.5
    public static final int SIZE = 16;

    /// A test for determining if the `codePoint` is a valid Unicode
    /// code point.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: The code point to test.
    ///
    /// #### Returns
    ///
    /// A boolean value.
    ///
    /// #### Since
    ///
    /// 1.5
    public static boolean isValidCodePoint(int codePoint) {
        return (MIN_CODE_POINT <= codePoint && MAX_CODE_POINT >= codePoint);
    }

    /// A test for determining if the `codePoint` is within the
    /// supplementary code point range.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: The code point to test.
    ///
    /// #### Returns
    ///
    /// A boolean value.
    ///
    /// #### Since
    ///
    /// 1.5
    public static boolean isSupplementaryCodePoint(int codePoint) {
        return (MIN_SUPPLEMENTARY_CODE_POINT <= codePoint && MAX_CODE_POINT >= codePoint);
    }

    /// A test for determining if the `char` is a high
    /// surrogate/leading surrogate unit that's used for representing
    /// supplementary characters in UTF-16 encoding.
    ///
    /// #### Parameters
    ///
    /// - `ch`: The `char` unit to test.
    ///
    /// #### Returns
    ///
    /// A boolean value.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - #isLowSurrogate(char)
    public static boolean isHighSurrogate(char ch) {
        return (MIN_HIGH_SURROGATE <= ch && MAX_HIGH_SURROGATE >= ch);
    }

    /// A test for determining if the `char` is a high
    /// surrogate/leading surrogate unit that's used for representing
    /// supplementary characters in UTF-16 encoding.
    ///
    /// #### Parameters
    ///
    /// - `ch`: The `char` unit to test.
    ///
    /// #### Returns
    ///
    /// A boolean value.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - #isHighSurrogate(char)
    public static boolean isLowSurrogate(char ch) {
        return (MIN_LOW_SURROGATE <= ch && MAX_LOW_SURROGATE >= ch);
    }

    /// A test for determining if the `char` pair is a valid
    /// surrogate pair.
    ///
    /// #### Parameters
    ///
    /// - `high`: The high surrogate unit to test.
    ///
    /// - `low`: The low surrogate unit to test.
    ///
    /// #### Returns
    ///
    /// A boolean value.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - #isHighSurrogate(char)
    ///
    /// - #isLowSurrogate(char)
    public static boolean isSurrogatePair(char high, char low) {
        return (isHighSurrogate(high) && isLowSurrogate(low));
    }

    /// Calculates the number of `char` values required to represent
    /// the Unicode code point. This method only tests if the
    /// `codePoint` is greater than or equal to `0x10000`,
    /// in which case `2` is returned, otherwise `1`.
    /// To test if the code point is valid, use the
    /// `#isValidCodePoint(int)` method.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: The code point to test.
    ///
    /// #### Returns
    ///
    /// An `int` value of 2 or 1.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - #isValidCodePoint(int)
    ///
    /// - #isSupplementaryCodePoint(int)
    public static int charCount(int codePoint) {
        return (codePoint >= 0x10000 ? 2 : 1);
    }

    /// Converts a surrogate pair into a Unicode code point. This method assume
    /// that the pair are valid surrogates. If the pair are NOT valid surrogates,
    /// then the result is indeterminate. The
    /// `char)` method should be used prior to this
    /// method to validate the pair.
    ///
    /// #### Parameters
    ///
    /// - `high`: The high surrogate unit.
    ///
    /// - `low`: The low surrogate unit.
    ///
    /// #### Returns
    ///
    /// The decoded code point.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - #isSurrogatePair(char, char)
    public static int toCodePoint(char high, char low) {
        // See RFC 2781, Section 2.2
        // http://www.faqs.org/rfcs/rfc2781.html
        int h = (high & 0x3FF) << 10;
        int l = low & 0x3FF;
        return (h | l) + 0x10000;
    }

    /// Returns the code point at the index in the `CharSequence`.
    /// If `char` unit at the index is a high-surrogate unit, the
    /// next index is less than the length of the sequence and the
    /// `char` unit at the next index is a low surrogate unit, then
    /// the code point represented by the pair is returned; otherwise the
    /// `char` unit at the index is returned.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The sequence of `char` units.
    ///
    /// - `index`: @param index The index into the `seq` to retrieve and
    /// convert.
    ///
    /// #### Returns
    ///
    /// The Unicode code point.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if the `index` is negative
    /// or greater than or equal to `seq.length()`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Returns the code point at the index in the `char[]`. If
    /// `char` unit at the index is a high-surrogate unit, the next
    /// index is less than the length of the sequence and the `char`
    /// unit at the next index is a low surrogate unit, then the code point
    /// represented by the pair is returned; otherwise the `char`
    /// unit at the index is returned.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The sequence of `char` units.
    ///
    /// - `index`: @param index The index into the `seq` to retrieve and
    /// convert.
    ///
    /// #### Returns
    ///
    /// The Unicode code point.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if the `index` is negative
    /// or greater than or equal to `seq.length()`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Returns the code point at the index in the `char[]` that's
    /// within the limit. If `char` unit at the index is a
    /// high-surrogate unit, the next index is less than the `limit`
    /// and the `char` unit at the next index is a low surrogate
    /// unit, then the code point represented by the pair is returned; otherwise
    /// the `char` unit at the index is returned.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The sequence of `char` units.
    ///
    /// - `index`: @param index The index into the `seq` to retrieve and
    /// convert.
    ///
    /// - `limit`: @param limit The exclusive index into the `seq` that marks
    /// the end of the units that can be used.
    ///
    /// #### Returns
    ///
    /// The Unicode code point.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if the `index` is
    /// negative, greater than or equal to `limit`,
    /// `limit` is negative or `limit` is
    /// greater than the length of `seq`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Returns the Unicode code point that proceeds the `index` in
    /// the `CharSequence`. If the `char` unit at
    /// `index - 1` is within the low surrogate range, the value
    /// `index - 2` isn't negative and the `char` unit
    /// at `index - 2` is within the high surrogate range, then the
    /// supplementary code point made up of the surrogate pair is returned;
    /// otherwise, the `char` value at `index - 1` is
    /// returned.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `CharSequence` to search.
    ///
    /// - `index`: The index into the `seq`.
    ///
    /// #### Returns
    ///
    /// A Unicode code point.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is less than 1
    /// or greater than `seq.length()`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Returns the Unicode code point that proceeds the `index` in
    /// the `char[]`. If the `char` unit at
    /// `index - 1` is within the low surrogate range, the value
    /// `index - 2` isn't negative and the `char` unit
    /// at `index - 2` is within the high surrogate range, then the
    /// supplementary code point made up of the surrogate pair is returned;
    /// otherwise, the `char` value at `index - 1` is
    /// returned.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `char[]` to search.
    ///
    /// - `index`: The index into the `seq`.
    ///
    /// #### Returns
    ///
    /// A Unicode code point.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is less than 1
    /// or greater than `seq.length`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Returns the Unicode code point that proceeds the `index` in
    /// the `char[]` and isn't less than `start`. If
    /// the `char` unit at `index - 1` is within the
    /// low surrogate range, the value `index - 2` isn't less than
    /// `start` and the `char` unit at
    /// `index - 2` is within the high surrogate range, then the
    /// supplementary code point made up of the surrogate pair is returned;
    /// otherwise, the `char` value at `index - 1` is
    /// returned.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `char[]` to search.
    ///
    /// - `index`: The index into the `seq`.
    ///
    /// #### Returns
    ///
    /// A Unicode code point.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is less than or
    /// equal to `start`, `index` is greater
    /// than `seq.length`, `start` is not
    /// negative and `start` is greater than
    /// `seq.length`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Converts the Unicode code point, `codePoint`, into a UTF-16
    /// encoded sequence and copies the value(s) into the
    /// `char[]` `dst`, starting at the index
    /// `dstIndex`.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: The Unicode code point to encode.
    ///
    /// - `dst`: The `char[]` to copy the encoded value into.
    ///
    /// - `dstIndex`: The index to start copying into `dst`.
    ///
    /// #### Returns
    ///
    /// @return The number of `char` value units copied into
    /// `dst`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException if `codePoint` is not a
    /// valid Unicode code point.
    ///
    /// - `NullPointerException`: if `dst` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `dstIndex` is negative,
    /// greater than or equal to `dst.length` or equals
    /// `dst.length - 1` when `codePoint` is a
    /// `supplementary code point`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Converts the Unicode code point, `codePoint`, into a UTF-16
    /// encoded sequence that is returned as a `char[]`.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: The Unicode code point to encode.
    ///
    /// #### Returns
    ///
    /// @return The UTF-16 encoded `char` sequence; if code point is
    /// a `supplementary code point`,
    /// then a 2 `char` array is returned, otherwise a 1
    /// `char` array is returned.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException if `codePoint` is not a
    /// valid Unicode code point.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Counts the number of Unicode code points in the subsequence of the
    /// `CharSequence`, as delineated by the
    /// `beginIndex` and `endIndex`. Any surrogate
    /// values with missing pair values will be counted as 1 code point.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `CharSequence` to look through.
    ///
    /// - `beginIndex`: The inclusive index to begin counting at.
    ///
    /// - `endIndex`: The exclusive index to stop counting at.
    ///
    /// #### Returns
    ///
    /// The number of Unicode code points.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `beginIndex` is
    /// negative, greater than `seq.length()` or greater
    /// than `endIndex`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Counts the number of Unicode code points in the subsequence of the
    /// `char[]`, as delineated by the `offset` and
    /// `count`. Any surrogate values with missing pair values will
    /// be counted as 1 code point.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `char[]` to look through.
    ///
    /// - `offset`: The inclusive index to begin counting at.
    ///
    /// - `count`: @param count The number of `char` values to look through in
    /// `seq`.
    ///
    /// #### Returns
    ///
    /// The number of Unicode code points.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `offset` or
    /// `count` is negative or if `endIndex` is
    /// greater than `seq.length`.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Determines the index into the `CharSequence` that is offset
    /// (measured in code points and specified by `codePointOffset`),
    /// from the `index` argument.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `CharSequence` to find the index within.
    ///
    /// - `index`: @param index The index to begin from, within the
    /// `CharSequence`.
    ///
    /// - `codePointOffset`: @param codePointOffset The number of code points to look back or
    /// forwards; may be a negative or positive value.
    ///
    /// #### Returns
    ///
    /// @return The calculated index that is `codePointOffset` code
    /// points from `index`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is negative,
    /// greater than `seq.length()`, there aren't enough
    /// values in `seq` after `index` or before
    /// `index` if `codePointOffset` is
    /// negative.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Determines the index into the `char[]` that is offset
    /// (measured in code points and specified by `codePointOffset`),
    /// from the `index` argument and is within the subsequence as
    /// delineated by `start` and `count`.
    ///
    /// #### Parameters
    ///
    /// - `seq`: The `char[]` to find the index within.
    ///
    /// - `index`: The index to begin from, within the `char[]`.
    ///
    /// - `codePointOffset`: @param codePointOffset The number of code points to look back or
    /// forwards; may be a negative or positive value.
    ///
    /// - `start`: @param start The inclusive index that marks the beginning of the
    /// subsequence.
    ///
    /// - `count`: @param count The number of `char` values to include within
    /// the subsequence.
    ///
    /// #### Returns
    ///
    /// @return The calculated index that is `codePointOffset` code
    /// points from `index`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `seq` is `null`.
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `start` or
    /// `count` is negative, `start + count`
    /// greater than `seq.length`, `index` is
    /// less than `start`, `index` is greater
    /// than `start + count` or there aren't enough values
    /// in `seq` after `index` or before
    /// `index` if `codePointOffset` is
    /// negative.
    ///
    /// #### Since
    ///
    /// 1.5
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

    /// Reverse the order of the first and second bytes in character
    ///
    /// #### Parameters
    ///
    /// - `c`: the character
    ///
    /// #### Returns
    ///
    /// the character with reordered bytes.
    public static char reverseBytes(char c) {
        return (char)((c<<8) | (c>>8));
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
    public static Character valueOf(char i) {
        return null;
    }

    /// See `#isWhitespace(int)`.
    public static boolean isWhitespace(char c) {
        return isWhitespace((int) c);
    }

    
    public static boolean isSpace(char ch) {
        switch (ch) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
                return true;
            default:
                return false;
        }
    }

    public static boolean isSpaceChar(char ch) {
        return false;
    }

    
    
    /// Returns true if the given code point is a Unicode whitespace character.
    /// The exact set of characters considered as whitespace varies with Unicode version.
    /// Note that non-breaking spaces are not considered whitespace.
    /// Note also that line separators are considered whitespace; see `#isSpaceChar`
    /// for an alternative.
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

    public static int toTitleCase(int codePoint) {
        return 0;
    }

    public static char toTitleCase(char c) {
        return (char) 0;
    }

}
