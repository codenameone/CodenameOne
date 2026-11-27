/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

/// `_cn1binascii`: the loops of the bundled `binascii`, which CPython runs in
/// C. The Python module still checks its arguments and raises every error, so
/// these see only bytes and answer `None` where the Python code must report.
final class PyBinascii {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final byte[] B64 = new byte[64];
    private static final int[] REV = new int[256];
    private static final int[] CRC = new int[256];

    static {
        java.util.Arrays.fill(REV, -1);
        for (int k = 0; k < 64; k++) {
            B64[k] = (byte) ALPHABET.charAt(k);
            REV[B64[k]] = k;
        }
        for (int k = 0; k < 256; k++) {
            int c = k;
            for (int j = 0; j < 8; j++) {
                c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
            }
            CRC[k] = c;
        }
    }

    private PyBinascii() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1binascii", new String[0]);
        m.setAttr("b2a_base64", PyBuiltin.fixed2("b2a_base64", (a0, a1) -> new PyBytes(encode(((PyBytes) a0).value, Py.isTrue(a1)))));
        m.setAttr("a2b_base64", PyBuiltin.fixed1("a2b_base64", a0 -> decode(((PyBytes) a0).value)));
        m.setAttr("crc32", PyBuiltin.fixed2("crc32", (a0, a1) -> Long.valueOf(crc32(((PyBytes) a0).value, Py.asLong(a1)))));
        m.setAttr("crc_hqx", PyBuiltin.fixed2("crc_hqx", (a0, a1) -> Long.valueOf(crcHqx(((PyBytes) a0).value, (int) Py.asLong(a1)))));
        return m;
    }

    static byte[] encode(byte[] d, boolean newline) {
        int n = d.length;
        byte[] out = new byte[(n + 2) / 3 * 4 + (newline ? 1 : 0)];
        int i = 0;
        int o = 0;
        while (i + 2 < n) {
            int v = (d[i] & 0xff) << 16 | (d[i + 1] & 0xff) << 8 | d[i + 2] & 0xff;
            out[o] = B64[v >> 18];
            out[o + 1] = B64[v >> 12 & 63];
            out[o + 2] = B64[v >> 6 & 63];
            out[o + 3] = B64[v & 63];
            o += 4;
            i += 3;
        }
        int rest = n - i;
        if (rest > 0) {
            int v = (d[i] & 0xff) << 16 | (rest == 2 ? (d[i + 1] & 0xff) << 8 : 0);
            out[o] = B64[v >> 18];
            out[o + 1] = B64[v >> 12 & 63];
            out[o + 2] = rest == 2 ? B64[v >> 6 & 63] : (byte) '=';
            out[o + 3] = (byte) '=';
            o += 4;
        }
        if (newline) {
            out[o] = (byte) '\n';
        }
        return out;
    }

    /// The non-strict decode of the Python module, or `None` when the input
    /// ends mid-quantum: the Python code then runs and raises its error.
    static Object decode(byte[] d) {
        byte[] out = new byte[d.length / 4 * 3 + 3];
        int o = 0;
        int acc = 0;
        int bits = 0;
        int quad = 0;
        int pads = 0;
        for (byte b : d) {
            int c = b & 0xff;
            if (c == '=') {
                if (quad >= 2) {
                    pads++;
                    if (quad + pads >= 4) {
                        acc = 0;
                        bits = 0;
                        quad = 0;
                        pads = 0;
                    }
                }
                continue;
            }
            int v = REV[c];
            if (v < 0) {
                continue;
            }
            pads = 0;
            acc = acc << 6 | v;
            bits += 6;
            quad = (quad + 1) & 3;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) (acc >> bits);
                acc &= (1 << bits) - 1;
            }
        }
        if (quad != 0) {
            return Py.NONE;
        }
        return new PyBytes(java.util.Arrays.copyOf(out, o));
    }

    static long crc32(byte[] d, long start) {
        int c = ~(int) start;
        for (byte b : d) {
            c = CRC[(c ^ b) & 0xff] ^ (c >>> 8);
        }
        return ~c & 0xFFFFFFFFL;
    }

    static int crcHqx(byte[] d, int value) {
        int c = value & 0xFFFF;
        for (byte b : d) {
            c ^= (b & 0xff) << 8;
            for (int j = 0; j < 8; j++) {
                c = (c & 0x8000) != 0 ? (c << 1 ^ 0x1021) & 0xFFFF : c << 1 & 0xFFFF;
            }
        }
        return c;
    }
}
