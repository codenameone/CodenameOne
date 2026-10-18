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

package java.lang;


/**
 * This interface represents an ordered set of characters and defines the
 * methods to probe them.
 */
public interface CharSequence {

    /**
     * Returns the number of characters in this sequence.
     * 
     * @return the number of characters.
     */
    public int length();

    /**
     * Returns the character at the specified index, with the first character
     * having index zero.
     * 
     * @param index
     *            the index of the character to return.
     * @return the requested character.
     * @throws IndexOutOfBoundsException
     *             if {@code index < 0} or {@code index} is greater than the
     *             length of this sequence.
     */
    public char charAt(int index);

    /**
     * Returns a {@code CharSequence} from the {@code start} index (inclusive)
     * to the {@code end} index (exclusive) of this sequence.
     * 
     * @param start
     *            the start offset of the sub-sequence. It is inclusive, that
     *            is, the index of the first character that is included in the
     *            sub-sequence.
     * @param end
     *            the end offset of the sub-sequence. It is exclusive, that is,
     *            the index of the first character after those that are included
     *            in the sub-sequence
     * @return the requested sub-sequence.
     * @throws IndexOutOfBoundsException
     *             if {@code start < 0}, {@code end < 0}, {@code start > end},
     *             or if {@code start} or {@code end} are greater than the
     *             length of this sequence.
     */
    public CharSequence subSequence(int start, int end);

    /**
     * Returns a string with the same characters in the same order as in this
     * sequence.
     * 
     * @return a string based on this sequence.
     */
    public String toString();

    /// Whether the sequence has no characters.
    default boolean isEmpty() {
        return length() == 0;
    }

    /// The sequence's `char` values, zero-extended to `int`. The
    /// characters are read when this is called, not when the stream runs,
    /// which is the same answer as the JDK's for any sequence that is not
    /// modified between the two. Changing a builder after asking for its
    /// stream and before running it is not supported: a lazy spliterator
    /// would cost every caller for a pattern that reads as a bug.
    default java.util.stream.IntStream chars() {
        int n = length();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = charAt(i);
        }
        return java.util.stream.IntStream.of(out);
    }

    /// The sequence's code points: a valid surrogate pair becomes one
    /// supplementary code point, and an unpaired surrogate is passed
    /// through as its own value, as the JDK does.
    default java.util.stream.IntStream codePoints() {
        int n = length();
        int[] out = new int[n];
        int count = 0;
        int i = 0;
        while (i < n) {
            char c = charAt(i++);
            if (Character.isHighSurrogate(c) && i < n) {
                char d = charAt(i);
                if (Character.isLowSurrogate(d)) {
                    i++;
                    out[count++] = Character.toCodePoint(c, d);
                    continue;
                }
            }
            out[count++] = c;
        }
        if (count == n) {
            return java.util.stream.IntStream.of(out);
        }
        int[] trimmed = new int[count];
        System.arraycopy(out, 0, trimmed, 0, count);
        return java.util.stream.IntStream.of(trimmed);
    }

    /// Compares two sequences lexicographically, char by char, the shorter
    /// first when one is a prefix of the other.
    public static int compare(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2) {
            return 0;
        }
        int len1 = cs1.length();
        int len2 = cs2.length();
        int n = len1 < len2 ? len1 : len2;
        for (int i = 0; i < n; i++) {
            char a = cs1.charAt(i);
            char b = cs2.charAt(i);
            if (a != b) {
                return a - b;
            }
        }
        return len1 - len2;
    }
}
