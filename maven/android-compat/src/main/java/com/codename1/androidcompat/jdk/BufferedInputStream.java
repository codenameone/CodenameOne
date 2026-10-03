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
import java.io.InputStream;

/// `java.io.BufferedInputStream` for the Codename One runtime, with mark and
/// reset support.
public class BufferedInputStream extends FilterInputStream {

    protected byte[] buf;
    protected int count;
    protected int pos;
    protected int markpos = -1;
    protected int marklimit;

    public BufferedInputStream(InputStream in) {
        this(in, 8192);
    }

    public BufferedInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    private void fill() throws IOException {
        if (markpos < 0) {
            pos = 0;
        } else if (pos >= buf.length) {
            if (markpos > 0) {
                int keep = pos - markpos;
                System.arraycopy(buf, markpos, buf, 0, keep);
                pos = keep;
                markpos = 0;
            } else if (buf.length >= marklimit) {
                markpos = -1;
                pos = 0;
            } else {
                byte[] grown = new byte[Math.min(buf.length * 2, Math.max(marklimit, buf.length + 1))];
                System.arraycopy(buf, 0, grown, 0, pos);
                buf = grown;
            }
        }
        count = pos;
        int n = in.read(buf, pos, buf.length - pos);
        if (n > 0) {
            count = n + pos;
        }
    }

    @Override
    public int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        return buf[pos++] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int avail = count - pos;
        if (avail <= 0) {
            if (len >= buf.length && markpos < 0) {
                return in.read(b, off, len);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return -1;
            }
        }
        int n = Math.min(avail, len);
        System.arraycopy(buf, pos, b, off, n);
        pos += n;
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;
        if (avail <= 0) {
            if (markpos < 0) {
                return in.skip(n);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return 0;
            }
        }
        long skipped = Math.min(avail, n);
        pos += (int) skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (count - pos) + in.available();
    }

    @Override
    public void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    @Override
    public void reset() throws IOException {
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        pos = markpos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
