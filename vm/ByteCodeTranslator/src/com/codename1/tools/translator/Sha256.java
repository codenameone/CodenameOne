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
package com.codename1.tools.translator;

/**
 * SHA-256 (FIPS 180-4), for the Content-Security-Policy hashes the JavaScript
 * bundle carries. ParparVM's JavaAPI has no {@code java.security}, and a
 * self-hosted translator runs on it.
 */
final class Sha256 {
    private static final int[] K = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    };

    private Sha256() {
    }

    /** {@code Integer.rotateRight}, which JavaAPI does not declare. */
    private static int ror(int x, int n) {
        return (x >>> n) | (x << (32 - n));
    }

    static byte[] digest(byte[] message) {
        int[] h = {
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
        };
        // Pad: 0x80, zeros to 56 mod 64, then the bit length as a big-endian long.
        int padded = ((message.length + 8) / 64 + 1) * 64;
        byte[] data = new byte[padded];
        System.arraycopy(message, 0, data, 0, message.length);
        data[message.length] = (byte) 0x80;
        long bits = (long) message.length * 8;
        for (int i = 0; i < 8; i++) {
            data[padded - 1 - i] = (byte) (bits >>> (8 * i));
        }
        int[] w = new int[64];
        for (int block = 0; block < padded; block += 64) {
            for (int t = 0; t < 16; t++) {
                int o = block + 4 * t;
                w[t] = (data[o] & 0xff) << 24 | (data[o + 1] & 0xff) << 16 | (data[o + 2] & 0xff) << 8
                        | (data[o + 3] & 0xff);
            }
            for (int t = 16; t < 64; t++) {
                int s0 = ror(w[t - 15], 7) ^ ror(w[t - 15], 18) ^ (w[t - 15] >>> 3);
                int s1 = ror(w[t - 2], 17) ^ ror(w[t - 2], 19) ^ (w[t - 2] >>> 10);
                w[t] = w[t - 16] + s0 + w[t - 7] + s1;
            }
            int a = h[0];
            int b = h[1];
            int c = h[2];
            int d = h[3];
            int e = h[4];
            int f = h[5];
            int g = h[6];
            int hh = h[7];
            for (int t = 0; t < 64; t++) {
                int s1 = ror(e, 6) ^ ror(e, 11) ^ ror(e, 25);
                int ch = (e & f) ^ (~e & g);
                int t1 = hh + s1 + ch + K[t] + w[t];
                int s0 = ror(a, 2) ^ ror(a, 13) ^ ror(a, 22);
                int maj = (a & b) ^ (a & c) ^ (b & c);
                int t2 = s0 + maj;
                hh = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
            }
            h[0] += a;
            h[1] += b;
            h[2] += c;
            h[3] += d;
            h[4] += e;
            h[5] += f;
            h[6] += g;
            h[7] += hh;
        }
        byte[] out = new byte[32];
        for (int i = 0; i < 8; i++) {
            out[4 * i] = (byte) (h[i] >>> 24);
            out[4 * i + 1] = (byte) (h[i] >>> 16);
            out[4 * i + 2] = (byte) (h[i] >>> 8);
            out[4 * i + 3] = (byte) h[i];
        }
        return out;
    }
}
