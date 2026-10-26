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
package com.codename1.crypto.solana;

/// Solana's wire helpers: the compact-u16 length prefix ("short vec") and a growable buffer.
final class ShortVec {
    private byte[] buf = new byte[128];
    private int len;

    ShortVec u8(int v) {
        ensure(1);
        buf[len++] = (byte) v;
        return this;
    }

    ShortVec u32(long v) {
        for (int i = 0; i < 4; i++) {
            u8((int) (v >>> (8 * i)));
        }
        return this;
    }

    ShortVec u64(long v) {
        for (int i = 0; i < 8; i++) {
            u8((int) (v >>> (8 * i)));
        }
        return this;
    }

    ShortVec length(int n) {
        if (n < 0 || n > 0xffff) {
            throw new IllegalArgumentException("a compact-u16 holds 0 to 65535");
        }
        int rem = n;
        while (true) {
            int b = rem & 0x7f;
            rem >>>= 7;
            if (rem == 0) {
                u8(b);
                return this;
            }
            u8(b | 0x80);
        }
    }

    ShortVec bytes(byte[] b) {
        ensure(b.length);
        System.arraycopy(b, 0, buf, len, b.length);
        len += b.length;
        return this;
    }

    byte[] toBytes() {
        byte[] out = new byte[len];
        System.arraycopy(buf, 0, out, 0, len);
        return out;
    }

    private void ensure(int n) {
        if (len + n > buf.length) {
            byte[] b = new byte[Math.max(buf.length * 2, len + n)];
            System.arraycopy(buf, 0, b, 0, len);
            buf = b;
        }
    }

    static byte[] copy(byte[] b) {
        byte[] out = new byte[b.length];
        System.arraycopy(b, 0, out, 0, b.length);
        return out;
    }

    /// Reads the wire form.
    static final class Reader {
        private final byte[] data;
        private int pos;

        Reader(byte[] data) {
            this.data = data;
        }

        int u8() {
            if (pos >= data.length) {
                throw new IllegalArgumentException("truncated Solana data");
            }
            return data[pos++] & 0xff;
        }

        int peek() {
            if (pos >= data.length) {
                throw new IllegalArgumentException("truncated Solana data");
            }
            return data[pos] & 0xff;
        }

        // compact-u16, canonical: no redundant continuation byte, at most three bytes
        int length() {
            int value = 0;
            for (int i = 0; i < 3; i++) {
                int b = u8();
                value |= (b & 0x7f) << (7 * i);
                if ((b & 0x80) == 0) {
                    if (i > 0 && b == 0) {
                        throw new IllegalArgumentException("non-canonical compact-u16");
                    }
                    if (value > 0xffff) {
                        throw new IllegalArgumentException("compact-u16 overflow");
                    }
                    return value;
                }
            }
            throw new IllegalArgumentException("compact-u16 overflow");
        }

        byte[] read(int n) {
            if (n < 0 || n > data.length - pos) {
                throw new IllegalArgumentException("truncated Solana data");
            }
            byte[] out = new byte[n];
            System.arraycopy(data, pos, out, 0, n);
            pos += n;
            return out;
        }

        byte[] rest() {
            return read(data.length - pos);
        }

        boolean hasMore() {
            return pos < data.length;
        }
    }
}
