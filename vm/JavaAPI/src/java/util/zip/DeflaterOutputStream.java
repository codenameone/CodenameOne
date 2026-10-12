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
package java.util.zip;

import java.io.*;

/** A streaming DEFLATE encoder. finish() leaves the destination open. */
public class DeflaterOutputStream extends FilterOutputStream {
    protected Deflater def;
    protected byte[] buf;
    private boolean own, closed, syncFlush;
    private final byte[] singleByte = new byte[1];
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
        singleByte[0] = (byte) b;
        write(singleByte, 0, 1);
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
            // A flush marker needs room to complete in one call. Tiny buffers
            // can otherwise cause another marker on every continuation call.
            byte[] flushBuffer = buf.length < 512 ? new byte[512] : buf;
            int n;
            do {
                n = def.deflate(flushBuffer, 0, flushBuffer.length, Deflater.SYNC_FLUSH);
                if (n > 0)
                    out.write(flushBuffer, 0, n);
            } while (n == flushBuffer.length);
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
