/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

import java.io.*;

/** A streaming DEFLATE encoder. finish() leaves the destination open. */
public class DeflaterOutputStream extends FilterOutputStream {
    protected Deflater def;
    protected byte[] buf;
    private boolean own, closed, syncFlush;
    public DeflaterOutputStream(OutputStream out) {
        this(out, new Deflater(), 512, false);
        own = true;
    }
    public DeflaterOutputStream(OutputStream out, boolean syncFlush) {
        this(out, new Deflater(), 512, syncFlush);
        own = true;
    }
    public DeflaterOutputStream(OutputStream out, Deflater def) {
        this(out, def, 512, false);
    }
    public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
        this(out, def, size, false);
    }
    public DeflaterOutputStream(OutputStream out, Deflater def, boolean syncFlush) {
        this(out, def, 512, syncFlush);
    }
    public DeflaterOutputStream(OutputStream out, Deflater def, int size, boolean syncFlush) {
        super(out);
        if (out == null || def == null)
            throw new NullPointerException();
        if (size <= 0)
            throw new IllegalArgumentException("Buffer size must be positive");
        this.def = def;
        buf = new byte[size];
        this.syncFlush = syncFlush;
    }
    public void write(int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
    }
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed)
            throw new IOException("Stream closed");
        Deflater.bounds(b, off, len);
        if (def.finished())
            throw new IOException("Stream finished");
        if (len == 0)
            return;
        def.setInput(b, off, len);
        while (!def.needsInput()) deflate();
    }
    protected void deflate() throws IOException {
        int n = def.deflate(buf, 0, buf.length);
        if (n > 0)
            out.write(buf, 0, n);
    }
    public void finish() throws IOException {
        if (closed)
            throw new IOException("Stream closed");
        if (!def.finished()) {
            def.finish();
            while (!def.finished()) deflate();
        }
    }
    public void flush() throws IOException {
        if (closed)
            throw new IOException("Stream closed");
        if (syncFlush && !def.finished()) {
            int n;
            do {
                n = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH);
                if (n > 0)
                    out.write(buf, 0, n);
            } while (n == buf.length);
        }
        out.flush();
    }
    public void close() throws IOException {
        if (closed)
            return;
        try {
            finish();
        } finally {
            closed = true;
            if (own)
                def.end();
            out.close();
        }
    }
}
