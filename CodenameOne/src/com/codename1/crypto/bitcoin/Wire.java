/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.bitcoin;

/// Bitcoin's little-endian wire format: a growable writer and a bounds-checked reader.
@com.codename1.impl.SharedWithBackend
final class Wire {
    private byte[] buf;
    private int len;
    private int pos;

    Wire() {
        buf = new byte[256];
    }

    Wire(byte[] data) {
        buf = data;
        len = data.length;
    }

    // ---------------------------------------------------------------- writing

    Wire u8(int v) {
        ensure(1);
        buf[len++] = (byte) v;
        return this;
    }

    Wire u32(long v) {
        ensure(4);
        for (int iter = 0; iter < 4; iter++) {
            buf[len++] = (byte) (v >>> (8 * iter));
        }
        return this;
    }

    Wire u64(long v) {
        ensure(8);
        for (int iter = 0; iter < 8; iter++) {
            buf[len++] = (byte) (v >>> (8 * iter));
        }
        return this;
    }

    Wire varInt(long v) {
        if (v < 0) {
            throw new IllegalArgumentException("negative length");
        }
        if (v < 0xfd) {
            return u8((int) v);
        }
        if (v <= 0xffff) {
            u8(0xfd);
            ensure(2);
            buf[len++] = (byte) v;
            buf[len++] = (byte) (v >>> 8);
            return this;
        }
        if (v <= 0xffffffffL) {
            u8(0xfe);
            return u32(v);
        }
        u8(0xff);
        return u64(v);
    }

    Wire bytes(byte[] b) {
        ensure(b.length);
        System.arraycopy(b, 0, buf, len, b.length);
        len += b.length;
        return this;
    }

    Wire varBytes(byte[] b) {
        varInt(b.length);
        return bytes(b);
    }

    byte[] toBytes() {
        byte[] out = new byte[len];
        System.arraycopy(buf, 0, out, 0, len);
        return out;
    }

    private void ensure(int more) {
        if (len + more > buf.length) {
            byte[] bigger = new byte[Math.max(buf.length * 2, len + more)];
            System.arraycopy(buf, 0, bigger, 0, len);
            buf = bigger;
        }
    }

    // ---------------------------------------------------------------- reading

    boolean hasMore() {
        return pos < len;
    }

    int position() {
        return pos;
    }

    int remaining() {
        return len - pos;
    }

    int peek() {
        need(1);
        return buf[pos] & 0xff;
    }

    int readU8() {
        need(1);
        return buf[pos++] & 0xff;
    }

    long readU32() {
        need(4);
        long v = 0;
        for (int iter = 0; iter < 4; iter++) {
            v |= (long) (buf[pos++] & 0xff) << (8 * iter);
        }
        return v;
    }

    long readU64() {
        need(8);
        long v = 0;
        for (int iter = 0; iter < 8; iter++) {
            v |= (long) (buf[pos++] & 0xff) << (8 * iter);
        }
        return v;
    }

    long readVarInt() {
        int first = readU8();
        long v;
        if (first < 0xfd) {
            return first;
        }
        if (first == 0xfd) {
            need(2);
            v = (buf[pos] & 0xff) | ((buf[pos + 1] & 0xff) << 8);
            pos += 2;
            if (v < 0xfd) {
                throw new IllegalArgumentException("non-canonical compact size");
            }
            return v;
        }
        if (first == 0xfe) {
            v = readU32();
            if (v <= 0xffff) {
                throw new IllegalArgumentException("non-canonical compact size");
            }
            return v;
        }
        v = readU64();
        if (v >= 0 && v <= 0xffffffffL) {
            throw new IllegalArgumentException("non-canonical compact size");
        }
        return v;
    }

    byte[] read(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("negative length");
        }
        need(n);
        byte[] out = new byte[n];
        System.arraycopy(buf, pos, out, 0, n);
        pos += n;
        return out;
    }

    byte[] readVarBytes() {
        long n = readVarInt();
        if (n < 0 || n > remaining()) {
            throw new IllegalArgumentException("length past the end of the data");
        }
        return read((int) n);
    }

    private void need(int n) {
        if (pos + n > len) {
            throw new IllegalArgumentException("data truncated");
        }
    }

    static byte[] reverse(byte[] b) {
        byte[] out = new byte[b.length];
        for (int iter = 0; iter < b.length; iter++) {
            out[iter] = b[b.length - 1 - iter];
        }
        return out;
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static byte[] copy(byte[] b) {
        byte[] out = new byte[b.length];
        System.arraycopy(b, 0, out, 0, b.length);
        return out;
    }
}
