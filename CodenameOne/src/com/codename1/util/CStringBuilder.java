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

/// This is generally the Apache String builder class refactored here so we can
/// use it regardless of vm limitations/differences for increased performance.
/// A modifiable `sequence of characters` for use in creating
/// and modifying Strings. This class is intended as a direct replacement of
/// `StringBuffer` for non-concurrent use; unlike `StringBuffer` this
/// class is not synchronized for thread safety.
///
/// The majority of the modification methods on this class return `StringBuilder`, so that, like `StringBuffer`s, they can be used in
/// chaining method calls together. For example, `new StringBuilder("One
/// should ").append("always strive ").append("to achieve Harmony")`.
///
/// #### Since
///
/// 1.5
///
/// #### Deprecated
///
/// we will be moving to the proper string builder very soon
///
/// #### See also
///
/// - CharSequence
///
/// - Appendable
///
/// - StringBuffer
///
/// - String
public final class CStringBuilder extends AbstractStringBuilder {
    /// Constructs an instance with an initial capacity of `16`.
    ///
    /// #### See also
    ///
    /// - #capacity()
    public CStringBuilder() {
        super();
    }

    /// Constructs an instance with the specified capacity.
    ///
    /// #### Parameters
    ///
    /// - `capacity`: the initial capacity to use.
    ///
    /// #### Throws
    ///
    /// - `NegativeArraySizeException`: if the specified `capacity` is negative.
    ///
    /// #### See also
    ///
    /// - #capacity()
    public CStringBuilder(int capacity) {
        super(capacity);
    }

    /// Constructs an instance that's initialized with the contents of the
    /// specified `String`. The capacity of the new builder will be the
    /// length of the `String` plus 16.
    ///
    /// #### Parameters
    ///
    /// - `str`: the `String` to copy into the builder.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `str` is `null`.
    public CStringBuilder(String str) {
        super(str);
    }

    /// Appends the string representation of the specified `boolean` value.
    /// The `boolean` value is converted to a String according to the rule
    /// defined by `String#valueOf(boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `b`: the `boolean` value to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(boolean)
    public CStringBuilder append(boolean b) {
        append0(b ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
        return this;
    }

    /// Appends the string representation of the specified `char` value.
    /// The `char` value is converted to a string according to the rule
    /// defined by `String#valueOf(char)`.
    ///
    /// #### Parameters
    ///
    /// - `c`: the `char` value to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(char)
    public CStringBuilder append(char c) {
        append0(c);
        return this;
    }

    /// Appends the string representation of the specified `int` value. The
    /// `int` value is converted to a string according to the rule defined
    /// by `String#valueOf(int)`.
    ///
    /// #### Parameters
    ///
    /// - `i`: the `int` value to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(int)
    public CStringBuilder append(int i) {
        append0(Integer.toString(i));
        return this;
    }

    /// Appends the string representation of the specified `long` value.
    /// The `long` value is converted to a string according to the rule
    /// defined by `String#valueOf(long)`.
    ///
    /// #### Parameters
    ///
    /// - `lng`: the `long` value.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(long)
    public CStringBuilder append(long lng) {
        append0(Long.toString(lng));
        return this;
    }

    /// Appends the string representation of the specified `float` value.
    /// The `float` value is converted to a string according to the rule
    /// defined by `String#valueOf(float)`.
    ///
    /// #### Parameters
    ///
    /// - `f`: the `float` value to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(float)
    public CStringBuilder append(float f) {
        append0(Float.toString(f));
        return this;
    }

    /// Appends the string representation of the specified `double` value.
    /// The `double` value is converted to a string according to the rule
    /// defined by `String#valueOf(double)`.
    ///
    /// #### Parameters
    ///
    /// - `d`: the `double` value to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(double)
    public CStringBuilder append(double d) {
        append0(Double.toString(d));
        return this;
    }

    /// Appends the string representation of the specified `Object`.
    /// The `Object` value is converted to a string according to the rule
    /// defined by `String#valueOf(Object)`.
    ///
    /// #### Parameters
    ///
    /// - `obj`: the `Object` to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(Object)
    public CStringBuilder append(Object obj) {
        if (obj == null) {
            appendNull();
        } else {
            append0(obj.toString());
        }
        return this;
    }

    /// Appends the contents of the specified string. If the string is `null`, then the string `"null"` is appended.
    ///
    /// #### Parameters
    ///
    /// - `str`: the string to append.
    ///
    /// #### Returns
    ///
    /// this builder.
    public CStringBuilder append(String str) {
        append0(str);
        return this;
    }

    /// Appends the string representation of the specified `char[]`.
    /// The `char[]` is converted to a string according to the rule
    /// defined by `String#valueOf(char[])`.
    ///
    /// #### Parameters
    ///
    /// - `ch`: the `char[]` to append..
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - String#valueOf(char[])
    public CStringBuilder append(char[] ch) {
        append0(ch);
        return this;
    }

    /// Appends the string representation of the specified subset of the `char[]`. The `char[]` value is converted to a String according to
    /// the rule defined by `int, int)`.
    ///
    /// #### Parameters
    ///
    /// - `str`: the `char[]` to append.
    ///
    /// - `offset`: the inclusive offset index.
    ///
    /// - `len`: the number of characters.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException if `offset` and `len` do not specify a valid
    /// subsequence.
    ///
    /// #### See also
    ///
    /// - String#valueOf(char[], int, int)
    public CStringBuilder append(char[] str, int offset, int len) {
        append0(str, offset, len);
        return this;
    }


    /// Appends the encoded Unicode code point. The code point is converted to a
    /// `char[]` as defined by `Character#toChars(int)`.
    ///
    /// #### Parameters
    ///
    /// - `codePoint`: the Unicode code point to encode and append.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### See also
    ///
    /// - Character#toChars(int)
    public CStringBuilder appendCodePoint(int codePoint) {
        append0(Character.toChars(codePoint));
        return this;
    }

    /// Deletes a sequence of characters specified by `start` and `end`. Shifts any remaining characters to the left.
    ///
    /// #### Parameters
    ///
    /// - `start`: the inclusive start index.
    ///
    /// - `end`: the exclusive end index.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `start` is less than zero, greater than the current
    /// length or greater than `end`.
    public CStringBuilder delete(int start, int end) {
        delete0(start, end);
        return this;
    }

    /// Deletes the character at the specified index. shifts any remaining
    /// characters to the left.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the character to delete.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `index` is less than zero or is greater than or
    /// equal to the current length.
    public CStringBuilder deleteCharAt(int index) {
        deleteCharAt0(index);
        return this;
    }

    /// Inserts the string representation of the specified `boolean` value
    /// at the specified `offset`. The `boolean` value is converted
    /// to a string according to the rule defined by
    /// `String#valueOf(boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `b`: the `boolean` value to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(boolean)
    public CStringBuilder insert(int offset, boolean b) {
        insert0(offset, b ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
        return this;
    }

    /// Inserts the string representation of the specified `char` value at
    /// the specified `offset`. The `char` value is converted to a
    /// string according to the rule defined by `String#valueOf(char)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `c`: the `char` value to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(char)
    public CStringBuilder insert(int offset, char c) {
        insert0(offset, c);
        return this;
    }

    /// Inserts the string representation of the specified `int` value at
    /// the specified `offset`. The `int` value is converted to a
    /// String according to the rule defined by `String#valueOf(int)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `i`: the `int` value to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(int)
    public CStringBuilder insert(int offset, int i) {
        insert0(offset, Integer.toString(i));
        return this;
    }

    /// Inserts the string representation of the specified `long` value at
    /// the specified `offset`. The `long` value is converted to a
    /// String according to the rule defined by `String#valueOf(long)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `l`: the `long` value to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// {code length()}.
    ///
    /// #### See also
    ///
    /// - String#valueOf(long)
    public CStringBuilder insert(int offset, long l) {
        insert0(offset, Long.toString(l));
        return this;
    }

    /// Inserts the string representation of the specified `float` value at
    /// the specified `offset`. The `float` value is converted to a
    /// string according to the rule defined by `String#valueOf(float)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `f`: the `float` value to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(float)
    public CStringBuilder insert(int offset, float f) {
        insert0(offset, Float.toString(f));
        return this;
    }

    /// Inserts the string representation of the specified `double` value
    /// at the specified `offset`. The `double` value is converted
    /// to a String according to the rule defined by
    /// `String#valueOf(double)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `d`: the `double` value to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(double)
    public CStringBuilder insert(int offset, double d) {
        insert0(offset, Double.toString(d));
        return this;
    }

    /// Inserts the string representation of the specified `Object` at the
    /// specified `offset`. The `Object` value is converted to a
    /// String according to the rule defined by `String#valueOf(Object)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `obj`: the `Object` to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(Object)
    public CStringBuilder insert(int offset, Object obj) {
        insert0(offset, obj == null ? "null" : obj.toString()); //$NON-NLS-1$
        return this;
    }

    /// Inserts the specified string at the specified `offset`. If the
    /// specified string is null, then the String `"null"` is inserted.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `str`: the `String` to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    public CStringBuilder insert(int offset, String str) {
        insert0(offset, str);
        return this;
    }

    /// Inserts the string representation of the specified `char[]` at the
    /// specified `offset`. The `char[]` value is converted to a
    /// String according to the rule defined by `String#valueOf(char[])`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `ch`: the `char[]` to insert.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`.
    ///
    /// #### See also
    ///
    /// - String#valueOf(char[])
    public CStringBuilder insert(int offset, char[] ch) {
        insert0(offset, ch);
        return this;
    }

    /// Inserts the string representation of the specified subsequence of the
    /// `char[]` at the specified `offset`. The `char[]` value
    /// is converted to a String according to the rule defined by
    /// `int, int)`.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the index to insert at.
    ///
    /// - `str`: the `char[]` to insert.
    ///
    /// - `strOffset`: the inclusive index.
    ///
    /// - `strLen`: the number of characters.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `offset` is negative or greater than the current
    /// `length()`, or `strOffset` and `strLen` do
    /// not specify a valid subsequence.
    ///
    /// #### See also
    ///
    /// - String#valueOf(char[], int, int)
    public CStringBuilder insert(int offset, char[] str, int strOffset,
                                 int strLen) {
        insert0(offset, str, strOffset, strLen);
        return this;
    }

    /// Replaces the specified subsequence in this builder with the specified
    /// string.
    ///
    /// #### Parameters
    ///
    /// - `start`: the inclusive begin index.
    ///
    /// - `end`: the exclusive end index.
    ///
    /// - `str`: the replacement string.
    ///
    /// #### Returns
    ///
    /// this builder.
    ///
    /// #### Throws
    ///
    /// - `StringIndexOutOfBoundsException`: @throws StringIndexOutOfBoundsException if `start` is negative, greater than the current
    /// `length()` or greater than `end`.
    ///
    /// - `NullPointerException`: if `str` is `null`.
    public CStringBuilder replace(int start, int end, String str) {
        replace0(start, end, str);
        return this;
    }

    /// Reverses the order of characters in this builder.
    ///
    /// #### Returns
    ///
    /// this buffer.
    public CStringBuilder reverse() {
        reverse0();
        return this;
    }

    /// Returns the contents of this builder.
    ///
    /// #### Returns
    ///
    /// the string representation of the data in this builder.
    @Override
    @SuppressWarnings("PMD.UselessOverridingMethod")
    public String toString() {
        /* Note: This method is required to workaround a compiler bug
         * in the RI javac (at least in 1.5.0_06) that will generate a
         * reference to the non-public AbstractStringBuilder if we don't
         * override it here.
         */
        return super.toString();
    }
}
