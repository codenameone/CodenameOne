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


/// This interface represents an ordered set of characters and defines the
/// methods to probe them.
public interface CharSequence {

    /// Returns the number of characters in this sequence.
    ///
    /// #### Returns
    ///
    /// the number of characters.
    public int length();

    /// Returns the character at the specified index, with the first character
    /// having index zero.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the character to return.
    ///
    /// #### Returns
    ///
    /// the requested character.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `index < 0` or `index` is greater than the
    /// length of this sequence.
    public char charAt(int index);

    /// Returns a `CharSequence` from the `start` index (inclusive)
    /// to the `end` index (exclusive) of this sequence.
    ///
    /// #### Parameters
    ///
    /// - `start`: @param start
    /// the start offset of the sub-sequence. It is inclusive, that
    /// is, the index of the first character that is included in the
    /// sub-sequence.
    ///
    /// - `end`: @param end
    /// the end offset of the sub-sequence. It is exclusive, that is,
    /// the index of the first character after those that are included
    /// in the sub-sequence
    ///
    /// #### Returns
    ///
    /// the requested sub-sequence.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `start  end`,
    /// or if `start` or `end` are greater than the
    /// length of this sequence.
    public CharSequence subSequence(int start, int end);

    /// Returns a string with the same characters in the same order as in this
    /// sequence.
    ///
    /// #### Returns
    ///
    /// a string based on this sequence.
    public String toString();
}
