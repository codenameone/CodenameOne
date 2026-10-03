/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.androidcompat.jdk;

import java.io.IOException;
import java.io.Reader;

/// `java.io.BufferedReader` for the Codename One runtime, which has no
/// buffered reader. Android code is compiled against the JDK class and the
/// build's remap step points it here, together with the compatibility
/// runtime's own uses.
public class BufferedReader extends Reader {

    private final Reader in;
    private final char[] buf;
    private int pos;
    private int count;
    private boolean skipLf;

    public BufferedReader(Reader in) {
        this(in, 8192);
    }

    public BufferedReader(Reader in, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.in = in;
        this.buf = new char[size];
    }

    private boolean fill() throws IOException {
        int n = in.read(buf, 0, buf.length);
        if (n <= 0) {
            return false;
        }
        pos = 0;
        count = n;
        return true;
    }

    @Override
    public int read() throws IOException {
        while (true) {
            if (pos >= count && !fill()) {
                return -1;
            }
            char c = buf[pos++];
            if (skipLf) {
                skipLf = false;
                if (c == '\n') {
                    continue;
                }
            }
            return c;
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int n = 0;
        while (n < len) {
            int c = read();
            if (c < 0) {
                return n == 0 ? -1 : n;
            }
            cbuf[off + n++] = (char) c;
            if (pos >= count && !in.ready()) {
                break;
            }
        }
        return n;
    }

    public String readLine() throws IOException {
        StringBuilder sb = null;
        while (true) {
            if (pos >= count && !fill()) {
                return sb == null ? null : sb.toString();
            }
            if (skipLf) {
                skipLf = false;
                if (buf[pos] == '\n') {
                    pos++;
                    continue;
                }
            }
            int start = pos;
            while (pos < count) {
                char c = buf[pos];
                if (c == '\n' || c == '\r') {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(buf, start, pos - start);
                    pos++;
                    if (c == '\r') {
                        skipLf = true;
                    }
                    return sb.toString();
                }
                pos++;
            }
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append(buf, start, pos - start);
        }
    }

    @Override
    public boolean ready() throws IOException {
        return pos < count || in.ready();
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = 0;
        while (skipped < n && read() >= 0) {
            skipped++;
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
