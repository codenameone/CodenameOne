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

package com.codename1.teavm.io;


import com.codename1.teavm.jso.io.Blob;
import java.io.IOException;
import java.io.InputStream;
import com.codename1.html5.js.typedarrays.Uint8Array;


/**
 *
 * @author shannah
 */
public class ArrayBufferInputStream extends InputStream {
    private Uint8Array buf;
    private String type;
    int pos = 0;
    int len;
    String src;
    // Worker-local copy of the backing buffer, materialised lazily on the
    // FIRST single-byte read. ``buf.get(pos++)`` is a JSO-bridge virtual
    // dispatch (string-parsed method id + wrapper unwrap per call);
    // ``Resources.load(theme.res)`` issues hundreds of thousands of
    // single-byte reads through DataInputStream, which made the
    // Initializr's boot crawl for minutes. One bulk copy turns every
    // subsequent read into a plain Java array access. Callers that only
    // bulk-read (media) or grab the blob/buffer never pay the copy.
    private byte[] local;

    public ArrayBufferInputStream(Uint8Array buf, String type) {
        this.buf = buf;
        this.type=type;
        this.len = buf.getByteLength();
    }

    private void ensureLocal() {
        if (local == null) {
            local = new byte[len];
            readBulkImpl(buf, 0, local, 0, len);
        }
    }

    @Override
    public int read() throws IOException {
        if ( pos >= len ){
            return -1;
        }
        ensureLocal();
        return local[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int length) throws IOException {
        if (pos >= len) {
            return -1;
        }
        if (length <= 0) {
            return 0;
        }
        int n = length;
        int avail = len - pos;
        if (n > avail) {
            n = avail;
        }
        if (local != null) {
            System.arraycopy(local, pos, b, off, n);
            pos += n;
            return n;
        }
        // Native intrinsic: one JS-side loop copies n bytes from the
        // backing Uint8Array into the Java byte[] without per-byte
        // virtual dispatch through the cooperative scheduler. This
        // collapses thousands of single-byte yields during
        // ``Resources.load(theme.res)`` into a single non-suspending
        // call.
        readBulkImpl(buf, pos, b, off, n);
        pos += n;
        return n;
    }

    @Override
    public void reset() throws IOException {
        pos = 0;
    }

    @Override
    public long skip(long n) throws IOException {

        int oldPos = pos;

        pos += (int)n;

        if ( pos > len ){
            pos = len;
        }
        int out = pos-oldPos;

        return pos-oldPos;
    }

    /**
     * Native intrinsic: bulk-copy {@code length} bytes from
     * {@code src[srcOff..]} into {@code dst[dstOff..]}. Implemented as
     * a single JS loop in port.js so it does not pay the cooperative
     * scheduler's per-byte yield overhead.
     */
    private static native void readBulkImpl(Uint8Array src, int srcOff,
            byte[] dst, int dstOff, int length);

    @Override
    public int available() throws IOException {
        return len-pos;
    }

    @Override
    public void close() throws IOException {
        buf = null;
        local = null;
        len = 0;
    }

    
    
    public Blob getBlob() {
        return BlobUtil.createBlob(buf, type);
    }
    
    public Uint8Array getBuffer() {
        return buf;
    }
    
    
    public String getSrc() {
        return src;
    }
    
    public void setSrc(String src) {
        this.src = src;
    }
      
}
