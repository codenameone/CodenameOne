/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

import java.io.*;

/** A streaming DEFLATE decoder; closing it closes its source. */
public class InflaterInputStream extends FilterInputStream {
    protected Inflater inf;
    protected byte[] buf;
    protected int len;
    private boolean own, closed, eof;
    public InflaterInputStream(InputStream in) {
        this(in, new Inflater(), 512);
        own = true;
    }
    public InflaterInputStream(InputStream in, Inflater inf) {
        this(in, inf, 512);
    }
    public InflaterInputStream(InputStream in, Inflater inf, int size) {
        super(in);
        if (in == null || inf == null)
            throw new NullPointerException();
        if (size <= 0)
            throw new IllegalArgumentException("Buffer size must be positive");
        this.inf = inf;
        buf = new byte[size];
    }
    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Stream closed");
    }
    public int read() throws IOException {
        byte[] b = new byte[1];
        return read(b, 0, 1) < 0 ? -1 : b[0] & 255;
    }
    public int read(byte[] b, int off, int length) throws IOException {
        ensureOpen();
        Deflater.bounds(b, off, length);
        if (length == 0)
            return 0;
        try {
            while (true) {
                int n = inf.inflate(b, off, length);
                if (n > 0)
                    return n;
                if (inf.finished() || inf.needsDictionary()) {
                    eof = true;
                    return -1;
                }
                if (inf.needsInput())
                    fill();
                else
                    throw new ZipException("Inflater made no progress");
            }
        } catch (DataFormatException ex) {
            throw new ZipException(ex.getMessage());
        }
    }
    protected void fill() throws IOException {
        ensureOpen();
        len = in.read(buf, 0, buf.length);
        if (len < 0)
            throw new EOFException("Truncated compressed stream");
        if (len == 0) {
            int b = in.read();
            if (b < 0)
                throw new EOFException();
            buf[0] = (byte) b;
            len = 1;
        }
        inf.setInput(buf, 0, len);
    }
    public long skip(long n) throws IOException {
        if (n < 0)
            throw new IllegalArgumentException();
        byte[] b = new byte[512];
        long total = 0;
        while (total < n) {
            int count = read(b, 0, (int) Math.min(n - total, b.length));
            if (count < 0)
                break;
            total += count;
        }
        return total;
    }
    public int available() throws IOException {
        ensureOpen();
        return eof ? 0 : 1;
    }
    public boolean markSupported() {
        return false;
    }
    public void mark(int limit) {
    }
    public void reset() throws IOException {
        throw new IOException("Mark/reset unsupported");
    }
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if (own)
                inf.end();
            in.close();
        }
    }
}
