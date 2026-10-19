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

/// Writes text to another writer through a buffer.
public class BufferedWriter extends Writer {

    private final Writer out;
    private final char[] buf;
    private int count;
    /// Set by [#close()]. The buffer and the underlying writer are kept, so
    /// without it a later write would land in the buffer and a second close
    /// could hand that data to a writer whose own close is a no-op.
    private boolean closed;

    /// Buffers `out` with an 8192 character buffer.
    public BufferedWriter(Writer out) {
        this(out, 8192);
    }

    /// Buffers `out` with a buffer of `size` characters.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `size` is not positive
    public BufferedWriter(Writer out, int size) {
        super(out);
        if (out == null) {
            throw new NullPointerException();
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.out = out;
        this.buf = new char[size];
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    @Override
    public void write(int c) throws IOException {
        ensureOpen();
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (char) c;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || len > cbuf.length - off) {
            throw new IndexOutOfBoundsException();
        }
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
        ensureOpen();
        if (off < 0 || len < 0 || off > s.length() - len) {
            throw new IndexOutOfBoundsException();
        }
        // Through the buffer a chunk at a time: a copy of the whole range
        // would double the memory a large write needs.
        int end = off + len;
        while (off < end) {
            if (count == buf.length) {
                flushBuffer();
            }
            int n = Math.min(buf.length - count, end - off);
            s.getChars(off, off + n, buf, count);
            count += n;
            off += n;
        }
    }

    /// Writes a line separator, `\n`.
    public void newLine() throws IOException {
        write('\n');
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        flushBuffer();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        // try-with-resources by hand: the failure to write the buffer is the
        // one reported, with the delegate's close failure attached.
        try {
            flushBuffer();
        } catch (IOException e) {
            count = 0;
            try {
                out.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        } catch (RuntimeException e) {
            count = 0;
            try {
                out.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;        } catch (Error e) {
            count = 0;
            try {
                out.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        count = 0;
        out.close();
    }
}
