/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

package com.codename1.util.regex;

import java.io.IOException;
import java.io.Reader;

/**
 * Encapsulates java.io.Reader as CharacterIterator
 *
 * @author <a href="mailto:ales.novak@netbeans.com">Ales Novak</a>
 * @version CVS $Id: ReaderCharacterIterator.java 518156 2007-03-14 14:31:26Z vgritsenko $
 */
public final class ReaderCharacterIterator implements CharacterIterator {
    /**
     * Underlying reader
     */
    private final Reader reader;

    /**
     * Buffer of read chars
     */
    private final StringBuffer buff;

    /**
     * read end?
     */
    private boolean closed;

    /**
     * @param reader a Reader, which is parsed
     */
    public ReaderCharacterIterator(Reader reader) {
        this.reader = reader;
        this.buff = new StringBuffer(512);
        this.closed = false;
    }

    /**
     * @return a substring
     */
    public String substring(int beginIndex, int endIndex) {
        try {
            ensure(endIndex);
            return buff.substring(beginIndex, endIndex);
        } catch (IOException e) {
            throw new StringIndexOutOfBoundsException(e.getMessage());
        }
    }

    /**
     * @return a substring
     */
    public String substring(int beginIndex) {
        try {
            readAll();
            return buff.substring(beginIndex);
        } catch (IOException e) {
            throw new StringIndexOutOfBoundsException(e.getMessage());
        }
    }

    /**
     * @return a character at the specified position.
     */
    public char charAt(int pos) {
        try {
            ensure(pos);
            return buff.charAt(pos);
        } catch (IOException e) {
            throw new StringIndexOutOfBoundsException(e.getMessage());
        }
    }

    /**
     * @return <tt>true</tt> iff if the specified index is after the end of the character stream
     */
    public boolean isEnd(int pos) {
        if (buff.length() > pos) {
            return false;
        } else {
            try {
                ensure(pos);
                return (buff.length() <= pos);
            } catch (IOException e) {
                throw new StringIndexOutOfBoundsException(e.getMessage());
            }
        }
    }

    /**
     * Reads n characters from the stream and appends them to the buffer
     */
    private int read(int n) throws IOException {
        if (closed) {
            return 0;
        }

        char[] c = new char[n];
        int count = 0;
        int read = 0;

        do {
            read = reader.read(c);
            if (read < 0) // EOF
            {
                closed = true;
                break;
            }
            count += read;
            buff.append(c, 0, read);
        }
        while (count < n);

        return count;
    }

    /**
     * Reads rest of the stream.
     */
    private void readAll() throws IOException {
        while (!closed) {
            read(1000);
        }
    }

    /**
     * Reads chars up to the idx
     */
    private void ensure(int idx) throws IOException {
        if (closed) {
            return;
        }

        if (idx < buff.length()) {
            return;
        }
        read(idx + 1 - buff.length());
    }
}
