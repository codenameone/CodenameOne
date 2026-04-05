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

package java.util;

/// The `StringTokenizer` class allows an application to break a string
/// into tokens by performing code point comparison. The `StringTokenizer`
/// methods do not distinguish among identifiers, numbers, and quoted strings,
/// nor do they recognize and skip comments.
///
/// The set of delimiters (the codepoints that separate tokens) may be specified
/// either at creation time or on a per-token basis.
///
/// An instance of `StringTokenizer` behaves in one of three ways,
/// depending on whether it was created with the `returnDelimiters` flag
/// having the value `true` or `false`:
///
/// - If returnDelims is `false`, delimiter code points serve to separate
/// tokens. A token is a maximal sequence of consecutive code points that are not
/// delimiters.
///
/// - If returnDelims is `true`, delimiter code points are themselves
/// considered to be tokens. In this case a token will be received for each
/// delimiter code point.
///
/// A token is thus either one delimiter code point, or a maximal sequence of
/// consecutive code points that are not delimiters.
///
/// A `StringTokenizer` object internally maintains a current position
/// within the string to be tokenized. Some operations advance this current
/// position past the code point processed.
///
/// A token is returned by taking a substring of the string that was used to
/// create the `StringTokenizer` object.
///
/// Here's an example of the use of the default delimiter `StringTokenizer`
/// :
///
/// ```java
/// StringTokenizer st = new StringTokenizer("this is a test");
/// while (st.hasMoreTokens()) {
///     println(st.nextToken());
/// }
/// ```
///
/// This prints the following output:
///
/// ```java
///     this
///     is
///     a
///     test
/// ```
///
/// Here's an example of how to use a `StringTokenizer` with a user
/// specified delimiter:
///
/// ```java
/// StringTokenizer st = new StringTokenizer(
///         "this is a test with supplementary characters \ud800\ud800\udc00\udc00",
///         " \ud800\udc00");
/// while (st.hasMoreTokens()) {
///     println(st.nextToken());
/// }
/// ```
///
/// This prints the following output:
///
/// ```java
///     this
///     is
///     a
///     test
///     with
///     supplementary
///     characters
///     \ud800
///     \udc00
/// ```
public class StringTokenizer implements java.util.Enumeration<Object> {

    private String string;

    private String delimiters;

    private boolean returnDelimiters;

    private int position;

    /// Constructs a new `StringTokenizer` for the parameter string using
    /// whitespace as the delimiter. The `returnDelimiters` flag is set to
    /// `false`.
    ///
    /// #### Parameters
    ///
    /// - `string`: the string to be tokenized.
    public StringTokenizer(String string) {
        this(string, " \t\n\r\f", false); //$NON-NLS-1$
    }

    /// Constructs a new `StringTokenizer` for the parameter string using
    /// the specified delimiters. The `returnDelimiters` flag is set to
    /// `false`. If `delimiters` is `null`, this constructor
    /// doesn't throw an `Exception`, but later calls to some methods might
    /// throw a `NullPointerException`.
    ///
    /// #### Parameters
    ///
    /// - `string`: the string to be tokenized.
    ///
    /// - `delimiters`: the delimiters to use.
    public StringTokenizer(String string, String delimiters) {
        this(string, delimiters, false);
    }

    /// Constructs a new `StringTokenizer` for the parameter string using
    /// the specified delimiters, returning the delimiters as tokens if the
    /// parameter `returnDelimiters` is `true`. If `delimiters`
    /// is null this constructor doesn't throw an `Exception`, but later
    /// calls to some methods might throw a `NullPointerException`.
    ///
    /// #### Parameters
    ///
    /// - `string`: the string to be tokenized.
    ///
    /// - `delimiters`: the delimiters to use.
    ///
    /// - `returnDelimiters`: `true` to return each delimiter as a token.
    public StringTokenizer(String string, String delimiters,
            boolean returnDelimiters) {
        if (string != null) {
            this.string = string;
            this.delimiters = delimiters;
            this.returnDelimiters = returnDelimiters;
            this.position = 0;
        } else
            throw new NullPointerException();
    }

    /// Returns the number of unprocessed tokens remaining in the string.
    ///
    /// #### Returns
    ///
    /// number of tokens that can be retreived before an `Exception` will result from a call to `nextToken()`.
    public int countTokens() {
        int count = 0;
        boolean inToken = false;
        for (int i = position, length = string.length(); i < length; i++) {
            if (delimiters.indexOf(string.charAt(i), 0) >= 0) {
                if (returnDelimiters)
                    count++;
                if (inToken) {
                    count++;
                    inToken = false;
                }
            } else {
                inToken = true;
            }
        }
        if (inToken)
            count++;
        return count;
    }

    /// Returns `true` if unprocessed tokens remain. This method is
    /// implemented in order to satisfy the `Enumeration` interface.
    ///
    /// #### Returns
    ///
    /// `true` if unprocessed tokens remain.
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    /// Returns `true` if unprocessed tokens remain.
    ///
    /// #### Returns
    ///
    /// `true` if unprocessed tokens remain.
    public boolean hasMoreTokens() {
        if (delimiters == null) {
            throw new NullPointerException();
        }
        int length = string.length();
        if (position < length) {
            if (returnDelimiters)
                return true; // there is at least one character and even if
            // it is a delimiter it is a token

            // otherwise find a character which is not a delimiter
            for (int i = position; i < length; i++)
                if (delimiters.indexOf(string.charAt(i), 0) == -1)
                    return true;
        }
        return false;
    }

    /// Returns the next token in the string as an `Object`. This method is
    /// implemented in order to satisfy the `Enumeration` interface.
    ///
    /// #### Returns
    ///
    /// next token in the string as an `Object`
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if no tokens remain.
    public Object nextElement() {
        return nextToken();
    }

    /// Returns the next token in the string as a `String`.
    ///
    /// #### Returns
    ///
    /// next token in the string as a `String`.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if no tokens remain.
    public String nextToken() {
        if (delimiters == null) {
            throw new NullPointerException();
        }
        int i = position;
        int length = string.length();

        if (i < length) {
            if (returnDelimiters) {
                if (delimiters.indexOf(string.charAt(position), 0) >= 0)
                    return String.valueOf(string.charAt(position++));
                for (position++; position < length; position++)
                    if (delimiters.indexOf(string.charAt(position), 0) >= 0)
                        return string.substring(i, position);
                return string.substring(i);
            }

            while (i < length && delimiters.indexOf(string.charAt(i), 0) >= 0)
                i++;
            position = i;
            if (i < length) {
                for (position++; position < length; position++)
                    if (delimiters.indexOf(string.charAt(position), 0) >= 0)
                        return string.substring(i, position);
                return string.substring(i);
            }
        }
        throw new NoSuchElementException();
    }

    /// Returns the next token in the string as a `String`. The delimiters
    /// used are changed to the specified delimiters.
    ///
    /// #### Parameters
    ///
    /// - `delims`: the new delimiters to use.
    ///
    /// #### Returns
    ///
    /// next token in the string as a `String`.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if no tokens remain.
    public String nextToken(String delims) {
        this.delimiters = delims;
        return nextToken();
    }
}
