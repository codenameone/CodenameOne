/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import java.io.IOException;

/// Declares methods to append characters or character sequences. Any class that
/// implements this interface can receive data formatted by a
/// `java.util.Formatter`. The appended character or character sequence
/// should be valid according to the rules described in
/// `Unicode Character Representation`.
///
/// `Appendable` itself does not guarantee thread safety. This
/// responsibility is up to the implementing class.
///
/// Implementing classes can choose different exception handling mechanism. They
/// can choose to throw exceptions other than `IOException` or they do not
/// throw any exceptions at all and use error codes instead.
public interface Appendable {

    /// Appends the specified character.
    ///
    /// #### Parameters
    ///
    /// - `c`: the character to append.
    ///
    /// #### Returns
    ///
    /// this `Appendable`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if an I/O error occurs.
    Appendable append(char c) throws IOException;

    /// Appends the character sequence `csq`. Implementation classes may
    /// not append the whole sequence, for example if the target is a buffer with
    /// limited size.
    ///
    /// If `csq` is `null`, the characters "null" are appended.
    ///
    /// #### Parameters
    ///
    /// - `csq`: the character sequence to append.
    ///
    /// #### Returns
    ///
    /// this `Appendable`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if an I/O error occurs.
    Appendable append(CharSequence csq) throws IOException;

    /// Appends a subsequence of `csq`.
    ///
    /// If `csq` is not `null` then calling this method is equivalent
    /// to calling `append(csq.subSequence(start, end))`.
    ///
    /// If `csq` is `null`, the characters "null" are appended.
    ///
    /// #### Parameters
    ///
    /// - `csq`: the character sequence to append.
    ///
    /// - `start`: @param start
    /// the first index of the subsequence of `csq` that is
    /// appended.
    ///
    /// - `end`: @param end
    /// the last index of the subsequence of `csq` that is
    /// appended.
    ///
    /// #### Returns
    ///
    /// this `Appendable`.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `start  end`
    /// or `end` is greater than the length of `csq`.
    ///
    /// - `IOException`: if an I/O error occurs.
    Appendable append(CharSequence csq, int start, int end) throws IOException;
}
