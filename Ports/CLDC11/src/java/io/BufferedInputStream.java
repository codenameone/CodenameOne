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
package java.io;

/// Reads from another stream through a buffer, so that many small reads
/// become few large ones, and supports `mark` and `reset`.
///
/// Like most streams, it is for one thread at a time.
public class BufferedInputStream extends FilterInputStream {

    /// The buffer; null once the stream is closed, which every read checks.
    protected byte[] buf;
    /// How many bytes of [#buf] are valid.
    protected int count;
    /// The index in [#buf] of the next byte to read.
    protected int pos;
    /// The index in [#buf] of the mark, or -1 when there is none.
    protected int markpos = -1;
    /// How far past the mark a read may go before the mark is dropped.
    protected int marklimit;

    /// Buffers `in` with an 8192 byte buffer.
    public BufferedInputStream(InputStream in) {
        this(in, 8192);
    }

    /// Buffers `in` with a buffer of `size` bytes.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `size` is not positive
    public BufferedInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    private void ensureOpen() throws IOException {
        if (buf == null) {
            throw new IOException("Stream closed");
        }
    }

    /// Refills the buffer once it is consumed, keeping the bytes from a
    /// valid mark on and growing the buffer up to the mark limit.
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
                int grownSize = buf.length * 2;
                if (grownSize > marklimit) {
                    grownSize = marklimit;
                }
                if (grownSize <= buf.length) {
                    grownSize = buf.length + 1;
                }
                byte[] grown = new byte[grownSize];
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
        ensureOpen();
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        return buf[pos++] & 0xff;
    }

    /// Reads as the JDK does: on past the buffer while the stream underneath
    /// has bytes available without blocking, so a small buffer does not make
    /// every read a short one.
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int total = 0;
        while (true) {
            int n = readOnce(b, off + total, len - total);
            if (n <= 0) {
                return total == 0 ? n : total;
            }
            total += n;
            if (total >= len) {
                return total;
            }
            InputStream input = in;
            if (input != null && input.available() <= 0) {
                return total;
            }
        }
    }

    private int readOnce(byte[] b, int off, int len) throws IOException {
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
        int n = avail < len ? avail : len;
        System.arraycopy(buf, pos, b, off, n);
        pos += n;
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
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
        long skipped = avail < n ? avail : n;
        pos += (int) skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return (count - pos) + in.available();
    }

    @Override
    public void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    @Override
    public void reset() throws IOException {
        ensureOpen();
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        pos = markpos;
    }

    @Override
    public void close() throws IOException {
        if (buf == null) {
            return;
        }
        buf = null;
        in.close();
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
