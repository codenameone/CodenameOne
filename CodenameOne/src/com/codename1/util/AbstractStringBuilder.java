/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.codename1.util;

/// A modifiable `sequence of characters` for use in creating
/// and modifying Strings. This class is intended as a base class for
/// `StringBuffer` and `StringBuilder`.
///
/// #### Since
///
/// 1.5
///
/// #### See also
///
/// - StringBuffer
///
/// - StringBuilder
abstract class AbstractStringBuilder {

    static final int INITIAL_CAPACITY = 16;

    private char[] value;

    private int count;

    AbstractStringBuilder() {
        value = new char[INITIAL_CAPACITY];
    }

    AbstractStringBuilder(int capacity) {
        if (capacity < 0) {
            throw new NegativeArraySizeException();
        }
        value = new char[capacity];
    }

    AbstractStringBuilder(String string) {
        count = string.length();
        value = new char[count + INITIAL_CAPACITY];
        string.getChars(0, count, value, 0);
    }

    /*
     * Returns the character array.
     */
    final char[] getValue() {
        return value;
    }

    /*
     * Restores internal state after deserialization.
     */
    final void set(char[] val, int len) {
        if (val == null) {
            val = new char[0];
        }
        if (val.length < len) {
            throw new RuntimeException();
        }

        value = val;
        count = len;
    }

    private void enlargeBuffer(int min) {
        int newSize = ((value.length >> 1) + value.length) + 2;
        char[] newData = new char[min > newSize ? min : newSize];
        System.arraycopy(value, 0, newData, 0, count);
        value = newData;
    }

    final void appendNull() {
        int newSize = count + 4;
        if (newSize > value.length) {
            enlargeBuffer(newSize);
        }
        value[count++] = 'n';
        value[count++] = 'u';
        value[count++] = 'l';
        value[count++] = 'l';
    }

    final void append0(char[] chars) {
        int newSize = count + chars.length;
        if (newSize > value.length) {
            enlargeBuffer(newSize);
        }
        System.arraycopy(chars, 0, value, count, chars.length);
        count = newSize;
    }

    final void append0(char[] chars, int offset, int length) {
        // Force null check of chars first!
        if (offset > chars.length || offset < 0) {
            // luni.12=Offset out of bounds \: {0}
            throw new ArrayIndexOutOfBoundsException();
        }
        if (length < 0 || chars.length - offset < length) {
            // luni.18=Length out of bounds \: {0}
            throw new ArrayIndexOutOfBoundsException();
        }

        int newSize = count + length;
        if (newSize > value.length) {
            enlargeBuffer(newSize);
        }
        System.arraycopy(chars, offset, value, count, length);
        count = newSize;
    }

    final void append0(char ch) {
        if (count == value.length) {
            enlargeBuffer(count + 1);
        }
        value[count++] = ch;
    }

    final void append0(String string) {
        if (string == null) {
            appendNull();
            return;
        }
        int adding = string.length();
        int newSize = count + adding;
        if (newSize > value.length) {
            enlargeBuffer(newSize);
        }
        string.getChars(0, adding, value, count);
        count = newSize;
    }

    /// Returns the number of characters that can be held without growing.
    ///
    /// #### Returns
    ///
    /// the capacity
    ///
    /// #### See also
    ///
    /// - #ensureCapacity
    ///
    /// - #length
    public int capacity() {
        return value.length;
    }

    /// Retrieves the character at the `index`.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the character to retrieve.
    ///
    /// #### Returns
    ///
    /// the char value.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is negative or greater than or equal to the
    ///                                   current `#length()`.
    public char charAt(int index) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    final void delete0(int start, int end) {
        if (start >= 0) {
            if (end > count) {
                end = count;
            }
            if (end == start) {
                return;
            }
            if (end > start) {
                int length = count - end;
                if (length >= 0) {
                    System.arraycopy(value, end, value, start, length);
                }
                count -= end - start;
                return;
            }
        }
        throw new StringIndexOutOfBoundsException();
    }

    final void deleteCharAt0(int location) {
        if (0 > location || location >= count) {
            throw new StringIndexOutOfBoundsException(location);
        }
        int length = count - location - 1;
        if (length > 0) {
            System.arraycopy(value, location + 1, value, location, length);
        }
        count--;
    }

    /// Ensures that this object has a minimum capacity available before
    /// requiring the internal buffer to be enlarged. The general policy of this
    /// method is that if the `minimumCapacity` is larger than the current
    /// `#capacity()`, then the capacity will be increased to the largest
    /// value of either the `minimumCapacity` or the current capacity
    /// multiplied by two plus two. Although this is the general policy, there is
    /// no guarantee that the capacity will change.
    ///
    /// #### Parameters
    ///
    /// - `min`: the new minimum capacity to set.
    public void ensureCapacity(int min) {
        if (min > value.length) {
            int twice = (value.length << 1) + 2;
            enlargeBuffer(twice > min ? twice : min);
        }
    }

    /// Copies the requested sequence of characters to the `char[]` passed
    /// starting at `destStart`.
    ///
    /// #### Parameters
    ///
    /// - `start`: the inclusive start index of the characters to copy.
    ///
    /// - `end`: the exclusive end index of the characters to copy.
    ///
    /// - `dest`: the `char[]` to copy the characters to.
    ///
    /// - `destStart`: the inclusive start index of `dest` to begin copying to.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if the `start` is negative, the `destStart` is
    ///                                   negative, the `start` is greater than `end`, the
    ///                                   `end` is greater than the current `#length()` or
    ///                                   `destStart + end - begin` is greater than
    ///                                   `dest.length`.
    public void getChars(int start, int end, char[] dest, int destStart) {
        if (start > count || end > count || start > end) {
            throw new StringIndexOutOfBoundsException();
        }
        System.arraycopy(value, start, dest, destStart, end - start);
    }

    final void insert0(int index, char[] chars) {
        if (0 > index || index > count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        if (chars.length != 0) {
            move(chars.length, index);
            System.arraycopy(chars, 0, value, index, chars.length);
            count += chars.length;
        }
    }

    final void insert0(int index, char[] chars, int start, int length) {
        if (0 <= index && index <= count) {
            // start + length could overflow, start/length maybe MaxInt
            if (start >= 0 && 0 <= length && length <= chars.length - start) {
                if (length != 0) {
                    move(length, index);
                    System.arraycopy(chars, start, value, index, length);
                    count += length;
                }
                return;
            }
            throw new StringIndexOutOfBoundsException("offset " + start //$NON-NLS-1$
                    + ", length " + length //$NON-NLS-1$
                    + ", char[].length " + chars.length); //$NON-NLS-1$
        }
        throw new StringIndexOutOfBoundsException(index);
    }

    final void insert0(int index, char ch) {
        if (0 > index || index > count) {
            // RI compatible exception type
            throw new ArrayIndexOutOfBoundsException(index);
        }
        move(1, index);
        value[index] = ch;
        count++;
    }

    final void insert0(int index, String string) {
        if (0 <= index && index <= count) {
            if (string == null) {
                string = "null"; //$NON-NLS-1$
            }
            int min = string.length();
            if (min != 0) {
                move(min, index);
                string.getChars(0, min, value, index);
                count += min;
            }
        } else {
            throw new StringIndexOutOfBoundsException(index);
        }
    }

    /// The current length.
    ///
    /// #### Returns
    ///
    /// the number of characters contained in this instance.
    public int length() {
        return count;
    }

    private void move(int size, int index) {
        int newSize;
        if (value.length - count >= size) {
            System.arraycopy(value, index, value, index + size, count
                    - index); // index == count case is no-op
            return;
        } else {
            int a = count + size;
            int b = (value.length << 1) + 2;
            newSize = a > b ? a : b;
        }

        char[] newData = new char[newSize];
        System.arraycopy(value, 0, newData, 0, index);
        // index == count case is no-op
        System.arraycopy(value, index, newData, index + size, count - index);
        value = newData;
    }

    final void replace0(int start, int end, String string) {
        if (start >= 0) {
            if (end > count) {
                end = count;
            }
            if (end > start) {
                int stringLength = string.length();
                int diff = end - start - stringLength;
                if (diff > 0) { // replacing with fewer characters
                    // index == count case is no-op
                    System.arraycopy(value, end, value, start
                            + stringLength, count - end);
                } else if (diff < 0) {
                    // replacing with more characters...need some room
                    move(-diff, end);
                }
                string.getChars(0, stringLength, value, start);
                count -= diff;
                return;
            }
            if (start == end) {
                if (string == null) {
                    throw new NullPointerException();
                }
                insert0(start, string);
                return;
            }
        }
        throw new StringIndexOutOfBoundsException();
    }

    final void reverse0() {
        if (count < 2) {
            return;
        }
        int end = count - 1;
        char frontHigh = value[0];
        char endLow = value[end];
        boolean allowFrontSur = true;
        boolean allowEndSur = true;
        for (int i = 0, mid = count / 2; i < mid; i++, --end) {
            char frontLow = value[i + 1];
            char endHigh = value[end - 1];
            boolean surAtFront = allowFrontSur && frontLow >= 0xdc00
                    && frontLow <= 0xdfff && frontHigh >= 0xd800
                    && frontHigh <= 0xdbff;
            if (surAtFront && (count < 3)) {
                return;
            }
            boolean surAtEnd = allowEndSur && endHigh >= 0xd800
                    && endHigh <= 0xdbff && endLow >= 0xdc00
                    && endLow <= 0xdfff;
            allowFrontSur = allowEndSur = true;
            if (surAtFront == surAtEnd) {
                if (surAtFront) {
                    // both surrogates
                    value[end] = frontLow;
                    value[end - 1] = frontHigh;
                    value[i] = endHigh;
                    value[i + 1] = endLow;
                    frontHigh = value[i + 2];
                    endLow = value[end - 2];
                    i++;
                    end--;
                } else {
                    // neither surrogates
                    value[end] = frontHigh;
                    value[i] = endLow;
                    frontHigh = frontLow;
                    endLow = endHigh;
                }
            } else {
                if (surAtFront) {
                    // surrogate only at the front
                    value[end] = frontLow;
                    value[i] = endLow;
                    endLow = endHigh;
                    allowFrontSur = false;
                } else {
                    // surrogate only at the end
                    value[end] = frontHigh;
                    value[i] = endHigh;
                    frontHigh = frontLow;
                    allowEndSur = false;
                }
            }
        }
        if ((count & 1) == 1 && (!allowFrontSur || !allowEndSur)) {
            value[end] = allowFrontSur ? endLow : frontHigh;
        }
    }

    /// Sets the character at the `index`.
    ///
    /// #### Parameters
    ///
    /// - `index`: the zero-based index of the character to replace.
    ///
    /// - `ch`: the character to set.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is negative or greater than or equal to the
    ///                                   current `#length()`.
    public void setCharAt(int index, char ch) {
        if (0 > index || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        value[index] = ch;
    }

    /// Sets the current length to a new value. If the new length is larger than
    /// the current length, then the new characters at the end of this object
    /// will contain the `char` value of `\u0000`.
    ///
    /// #### Parameters
    ///
    /// - `length`: the new length of this StringBuffer.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `length < 0`.
    ///
    /// #### See also
    ///
    /// - #length
    public void setLength(int length) {
        if (length < 0) {
            throw new StringIndexOutOfBoundsException(length);
        }
        if (length > value.length) {
            enlargeBuffer(length);
        } else {
            if (count < length) {
                for (int iter = count; iter < count + length; iter++) {
                    value[iter] = (char) 0;
                }
            }
        }
        count = length;
    }

    /// Returns the String value of the subsequence from the `start` index
    /// to the current end.
    ///
    /// #### Parameters
    ///
    /// - `start`: the inclusive start index to begin the subsequence.
    ///
    /// #### Returns
    ///
    /// a String containing the subsequence.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `start` is negative or greater than the current
    ///                                         `#length()`.
    public String substring(int start) {
        if (0 <= start && start <= count) {
            if (start == count) {
                return ""; //$NON-NLS-1$
            }

            // Remove String sharing for more performance
            return new String(value, start, count - start);
        }
        throw new StringIndexOutOfBoundsException(start);
    }

    /// Returns the String value of the subsequence from the `start` index
    /// to the `end` index.
    ///
    /// #### Parameters
    ///
    /// - `start`: the inclusive start index to begin the subsequence.
    ///
    /// - `end`: the exclusive end index to end the subsequence.
    ///
    /// #### Returns
    ///
    /// a String containing the subsequence.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `start` is negative, greater than `end` or if
    ///                                         `end` is greater than the current `#length()`.
    public String substring(int start, int end) {
        if (0 <= start && start <= end && end <= count) {
            if (start == end) {
                return ""; //$NON-NLS-1$
            }

            // Remove String sharing for more performance
            return new String(value, start, end - start);
        }
        throw new StringIndexOutOfBoundsException();
    }

    /// Returns the current String representation.
    ///
    /// #### Returns
    ///
    /// a String containing the characters in this instance.
    @Override
    public String toString() {
        if (count == 0) {
            return "";
        }
        return new String(value, 0, count);
    }

    /// Searches for the first index of the specified character. The search for
    /// the character starts at the beginning and moves towards the end.
    ///
    /// #### Parameters
    ///
    /// - `string`: the string to find.
    ///
    /// #### Returns
    ///
    /// @return the index of the specified character, -1 if the character isn't
    /// found.
    ///
    /// #### Since
    ///
    /// 1.4
    ///
    /// #### See also
    ///
    /// - #lastIndexOf(String)
    public int indexOf(String string) {
        return indexOf(string, 0);
    }

    /// Searches for the index of the specified character. The search for the
    /// character starts at the specified offset and moves towards the end.
    ///
    /// #### Parameters
    ///
    /// - `subString`: the string to find.
    ///
    /// - `start`: the starting offset.
    ///
    /// #### Returns
    ///
    /// @return the index of the specified character, -1 if the character isn't
    /// found
    ///
    /// #### Since
    ///
    /// 1.4
    ///
    /// #### See also
    ///
    /// - #lastIndexOf(String, int)
    public int indexOf(String subString, int start) {
        if (start < 0) {
            start = 0;
        }
        int subCount = subString.length();
        if (subCount > 0) {
            if (subCount + start > count) {
                return -1;
            }
            // TODO optimize charAt to direct array access
            char firstChar = subString.charAt(0);
            while (true) {
                int i = start;
                boolean found = false;
                for (; i < count; i++) {
                    if (value[i] == firstChar) {
                        found = true;
                        break;
                    }
                }
                if (!found || subCount + i > count) {
                    return -1; // handles subCount > count || start >= count
                }
                int o1 = i;
                int o2 = 0;
                while (++o2 < subCount && value[++o1] == subString.charAt(o2)) { // NOPMD EmptyControlStatement
                }
                if (o2 == subCount) {
                    return i;
                }
                start = i + 1;
            }
        }
        return (start < count || start == 0) ? start : count;
    }

    /// Searches for the last index of the specified character. The search for
    /// the character starts at the end and moves towards the beginning.
    ///
    /// #### Parameters
    ///
    /// - `string`: the string to find.
    ///
    /// #### Returns
    ///
    /// @return the index of the specified character, -1 if the character isn't
    /// found.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `string` is `null`.
    ///
    /// #### Since
    ///
    /// 1.4
    ///
    /// #### See also
    ///
    /// - String#lastIndexOf(java.lang.String)
    public int lastIndexOf(String string) {
        return lastIndexOf(string, count);
    }

    /// Searches for the index of the specified character. The search for the
    /// character starts at the specified offset and moves towards the beginning.
    ///
    /// #### Parameters
    ///
    /// - `subString`: the string to find.
    ///
    /// - `start`: the starting offset.
    ///
    /// #### Returns
    ///
    /// @return the index of the specified character, -1 if the character isn't
    /// found.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `subString` is `null`.
    ///
    /// #### Since
    ///
    /// 1.4
    ///
    /// #### See also
    ///
    /// - String#lastIndexOf(String, int)
    @SuppressWarnings("PMD.EmptyControlStatement")
    public int lastIndexOf(String subString, int start) {
        int subCount = subString.length();
        if (subCount <= count && start >= 0) {
            if (subCount > 0) {
                if (start > count - subCount) {
                    start = count - subCount; // count and subCount are both
                }
                // >= 1
                // TODO optimize charAt to direct array access
                char firstChar = subString.charAt(0);
                while (true) {
                    int i = start;
                    boolean found = false;
                    for (; i >= 0; --i) {
                        if (value[i] == firstChar) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return -1;
                    }
                    int o1 = i;
                    int o2 = 0;
                    // NOPMD EmptyControlStatement
                    while (++o2 < subCount
                            && value[++o1] == subString.charAt(o2)) {
                    }
                    if (o2 == subCount) {
                        return i;
                    }
                    start = i - 1;
                }
            }
            return start < count ? start : count;
        }
        return -1;
    }

    /// Trims off any extra capacity beyond the current length. Note, this method
    /// is NOT guaranteed to change the capacity of this object.
    ///
    /// #### Since
    ///
    /// 1.5
    public void trimToSize() {
        if (count < value.length) {
            char[] newValue = new char[count];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
    }

    /// Retrieves the Unicode code point value at the `index`.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index to the `char` code unit.
    ///
    /// #### Returns
    ///
    /// the Unicode code point value.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is negative or greater than or equal to
    ///                                   `#length()`.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - Character
    ///
    /// - Character#codePointAt(char[], int, int)
    public int codePointAt(int index) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointAt(value, index, count);
    }

    /// Retrieves the Unicode code point value that precedes the `index`.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index to the `char` code unit within this object.
    ///
    /// #### Returns
    ///
    /// the Unicode code point value.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is less than 1 or greater than
    ///                                   `#length()`.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - Character
    ///
    /// - Character#codePointBefore(char[], int, int)
    public int codePointBefore(int index) {
        if (index < 1 || index > count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointBefore(value, index);
    }

    /// Calculates the number of Unicode code points between `beginIndex`
    /// and `endIndex`.
    ///
    /// #### Parameters
    ///
    /// - `beginIndex`: the inclusive beginning index of the subsequence.
    ///
    /// - `endIndex`: the exclusive end index of the subsequence.
    ///
    /// #### Returns
    ///
    /// the number of Unicode code points in the subsequence.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `beginIndex` is negative or greater than
    ///                                   `endIndex` or `endIndex` is greater than
    ///                                   `#length()`.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - Character
    ///
    /// - Character#codePointCount(char[], int, int)
    public int codePointCount(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > count || beginIndex > endIndex) {
            throw new StringIndexOutOfBoundsException();
        }
        return Character.codePointCount(value, beginIndex, endIndex
                - beginIndex);
    }

    /// Returns the index that is offset `codePointOffset` code points from
    /// `index`.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index to calculate the offset from.
    ///
    /// - `codePointOffset`: the number of code points to count.
    ///
    /// #### Returns
    ///
    /// @return the index that is `codePointOffset` code points away from
    /// index.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `index` is negative or greater than
    ///                                   `#length()` or if there aren't enough code points
    ///                                   before or after `index` to match
    ///                                   `codePointOffset`.
    ///
    /// #### Since
    ///
    /// 1.5
    ///
    /// #### See also
    ///
    /// - Character
    ///
    /// - Character#offsetByCodePoints(char[], int, int, int, int)
    public int offsetByCodePoints(int index, int codePointOffset) {
        return Character.offsetByCodePoints(value, 0, count, index,
                codePointOffset);
    }
}
