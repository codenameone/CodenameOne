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

package java.io;

/// A specialized `Reader` that reads characters from a `String` in
/// a sequential manner.
///
/// #### See also
///
/// - StringWriter
public class StringReader extends Reader {
    private String str;

    private int markpos = -1;

    private int pos;

    private int count;

    /// Construct a new `StringReader` with `str` as source. The size
    /// of the reader is set to the `length()` of the string and the Object
    /// to synchronize access through is set to `str`.
    ///
    /// #### Parameters
    ///
    /// - `str`: the source string for this reader.
    public StringReader(String str) {
        super();
        this.str = str;
        this.count = str.length();
    }

    /// Closes this reader. Once it is closed, read operations on this reader
    /// will throw an `IOException`. Only the first invocation of this
    /// method has any effect.
    @Override
    public void close() {
        str = null;
    }

    /// Returns a boolean indicating whether this reader is closed.
    ///
    /// #### Returns
    ///
    /// `true` if closed, otherwise `false`.
    private boolean isClosed() {
        return str == null;
    }

    /// Sets a mark position in this reader. The parameter `readLimit` is
    /// ignored for this class. Calling `reset()` will reposition the
    /// reader back to the marked position.
    ///
    /// #### Parameters
    ///
    /// - `readLimit`: ignored for `StringReader` instances.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `readLimit < 0`.
    ///
    /// - `IOException`: if this reader is closed.
    ///
    /// #### See also
    ///
    /// - #markSupported()
    ///
    /// - #reset()
    @Override
    public void mark(int readLimit) throws IOException {
        if (readLimit < 0) {
            throw new IllegalArgumentException();
        }

        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("String reader already closed"); //$NON-NLS-1$
            }
            markpos = pos;
        }
    }

    /// Indicates whether this reader supports the `mark()` and `reset()` methods. This implementation returns `true`.
    ///
    /// #### Returns
    ///
    /// always `true`.
    @Override
    public boolean markSupported() {
        return true;
    }

    /// Reads a single character from the source string and returns it as an
    /// integer with the two higher-order bytes set to 0. Returns -1 if the end
    /// of the source string has been reached.
    ///
    /// #### Returns
    ///
    /// @return the character read or -1 if the end of the source string has been
    /// reached.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this reader is closed.
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("String reader already closed"); //$NON-NLS-1$
            }
            if (pos != count) {
                return str.charAt(pos++);
            }
            return -1;
        }
    }

    /// Reads at most `len` characters from the source string and stores
    /// them at `offset` in the character array `buf`. Returns the
    /// number of characters actually read or -1 if the end of the source string
    /// has been reached.
    ///
    /// #### Parameters
    ///
    /// - `buf`: the character array to store the characters read.
    ///
    /// - `offset`: @param offset
    /// the initial position in `buffer` to store the characters
    /// read from this reader.
    ///
    /// - `len`: the maximum number of characters to read.
    ///
    /// #### Returns
    ///
    /// @return the number of characters read or -1 if the end of the reader has
    /// been reached.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `offset < 0` or `len < 0`, or if
    /// `offset + len` is greater than the size of `buf`.
    ///
    /// - `IOException`: if this reader is closed.
    @Override
    public int read(char buf[], int offset, int len) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                // luni.D6=StringReader is closed.
                throw new IOException("StringReader already closed"); //$NON-NLS-1$
            }
            if (offset < 0 || offset > buf.length) {
                // luni.12=Offset out of bounds \: {0}
                throw new ArrayIndexOutOfBoundsException(); //$NON-NLS-1$
            }
            if (len < 0 || len > buf.length - offset) {
                // luni.18=Length out of bounds \: {0}
                throw new ArrayIndexOutOfBoundsException(); //$NON-NLS-1$
            }
            if (len == 0) {
                return 0;
            }
            if (pos == this.count) {
                return -1;
            }
            int end = pos + len > this.count ? this.count : pos + len;
            str.getChars(pos, end, buf, offset);
            int read = end - pos;
            pos = end;
            return read;
        }
    }

    /// Indicates whether this reader is ready to be read without blocking. This
    /// implementation always returns `true`.
    ///
    /// #### Returns
    ///
    /// always `true`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this reader is closed.
    ///
    /// #### See also
    ///
    /// - #read()
    ///
    /// - #read(char[], int, int)
    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("String reader already closed"); //$NON-NLS-1$
            }
            return true;
        }
    }

    /// Resets this reader's position to the last `mark()` location.
    /// Invocations of `read()` and `skip()` will occur from this new
    /// location. If this reader has not been marked, it is reset to the
    /// beginning of the source string.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this reader is closed.
    ///
    /// #### See also
    ///
    /// - #mark(int)
    ///
    /// - #markSupported()
    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("StringReader already closed"); //$NON-NLS-1$
            }
            pos = markpos != -1 ? markpos : 0;
        }
    }

    /// Moves `ns` characters in the source string. Unlike the `overridden method`, this method may skip negative skip
    /// distances: this rewinds the input so that characters may be read again.
    /// When the end of the source string has been reached, the input cannot be
    /// rewound.
    ///
    /// #### Parameters
    ///
    /// - `ns`: @param ns
    /// the maximum number of characters to skip. Positive values skip
    /// forward; negative values skip backward.
    ///
    /// #### Returns
    ///
    /// @return the number of characters actually skipped. This is bounded below
    /// by the number of characters already read and above by the
    /// number of characters remaining:
    ///  `-(num chars already
    /// read) <= distance skipped <= num chars remaining`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this reader is closed.
    ///
    /// #### See also
    ///
    /// - #mark(int)
    ///
    /// - #markSupported()
    ///
    /// - #reset()
    @Override
    public long skip(long ns) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("StringReader already closed"); //$NON-NLS-1$
            }

            int minSkip = -pos;
            int maxSkip = count - pos;

            if (maxSkip == 0 || ns > maxSkip) {
                ns = maxSkip; // no rewinding if we're at the end
            } else if (ns < minSkip) {
                ns = minSkip;
            }

            pos += ns;
            return ns;
        }
    }
}