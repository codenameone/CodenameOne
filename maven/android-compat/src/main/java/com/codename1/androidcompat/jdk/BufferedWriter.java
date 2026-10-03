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
import java.io.Writer;

/// `java.io.BufferedWriter` for the Codename One runtime; see [BufferedReader].
public class BufferedWriter extends Writer {

    private final Writer out;
    private final char[] buf;
    private int count;

    public BufferedWriter(Writer out) {
        this(out, 8192);
    }

    public BufferedWriter(Writer out, int size) {
        this.out = out;
        this.buf = new char[size];
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    @Override
    public void write(int c) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (char) c;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (len >= buf.length) {
            flushBuffer();
            out.write(cbuf, off, len);
            return;
        }
        if (len > buf.length - count) {
            flushBuffer();
        }
        System.arraycopy(cbuf, off, buf, count, len);
        count += len;
    }

    @Override
    public void write(String s, int off, int len) throws IOException {
        char[] c = new char[len];
        s.getChars(off, off + len, c, 0);
        write(c, 0, len);
    }

    public void newLine() throws IOException {
        write('\n');
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        out.close();
    }
}
