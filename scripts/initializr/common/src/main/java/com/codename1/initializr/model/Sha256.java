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
package com.codename1.initializr.model;

import java.io.UnsupportedEncodingException;

/// SHA-256 (FIPS 180-4), written out by hand because the Codename One runtime
/// the Initializr runs on -- the JavaScript port in particular -- has no
/// java.security.MessageDigest.
///
/// It exists for one value: the anonymous project id the generated launchers
/// report, a hash of the lower-cased package name. The website's download
/// beacon (docs/website/assets/js/cn1-initializr-beacon.js) sends the same hash
/// computed with WebCrypto, and BuildCloud hashes the package of every cloud
/// build the same way, so the three join without the package name itself ever
/// leaving the developer's machine. Any divergence from the standard breaks that
/// join silently, hence the test vectors in GeneratorModelMatrixTest.
final class Sha256 {
    private static final int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Sha256() {
    }

    /// Lower-case hex SHA-256 of the UTF-8 bytes of `text`.
    static String hex(String text) {
        try {
            return hex(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is mandatory on every Java runtime; unreachable.
            throw new RuntimeException(e.toString());
        }
    }

    /// Lower-case hex SHA-256 of `data`.
    static String hex(byte[] data) {
        int[] h = {
                0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
                0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };
        // Padding: 0x80, zeros to 56 mod 64, then the bit length as a 64-bit
        // big-endian integer.
        int padded = ((data.length + 8) / 64 + 1) * 64;
        byte[] msg = new byte[padded];
        System.arraycopy(data, 0, msg, 0, data.length);
        msg[data.length] = (byte) 0x80;
        long bits = ((long) data.length) * 8L;
        for (int i = 0; i < 8; i++) {
            msg[padded - 1 - i] = (byte) (bits >>> (8 * i));
        }

        int[] w = new int[64];
        for (int block = 0; block < padded; block += 64) {
            for (int t = 0; t < 16; t++) {
                int o = block + t * 4;
                w[t] = ((msg[o] & 0xff) << 24) | ((msg[o + 1] & 0xff) << 16)
                        | ((msg[o + 2] & 0xff) << 8) | (msg[o + 3] & 0xff);
            }
            for (int t = 16; t < 64; t++) {
                int s0 = rotr(w[t - 15], 7) ^ rotr(w[t - 15], 18) ^ (w[t - 15] >>> 3);
                int s1 = rotr(w[t - 2], 17) ^ rotr(w[t - 2], 19) ^ (w[t - 2] >>> 10);
                w[t] = w[t - 16] + s0 + w[t - 7] + s1;
            }
            int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
            for (int t = 0; t < 64; t++) {
                int s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
                int ch = (e & f) ^ (~e & g);
                int t1 = hh + s1 + ch + K[t] + w[t];
                int s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
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

        char[] out = new char[64];
        for (int i = 0; i < 8; i++) {
            for (int nibble = 0; nibble < 8; nibble++) {
                out[i * 8 + nibble] = HEX[(h[i] >>> (28 - 4 * nibble)) & 0xf];
            }
        }
        return new String(out);
    }

    private static int rotr(int x, int n) {
        return (x >>> n) | (x << (32 - n));
    }
}
