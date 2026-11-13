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
package com.codename1.backend.security.pki;

/// Triple DES and RC2 in CBC mode, for the PKCS#12 files Apple's keychain
/// imports and exports: a 3DES key bag and an RC2-40 certificate bag. Nothing
/// else in the backend uses either, and neither is offered as a general
/// cipher: both are broken by current standards, and they are here only so
/// those files can be read and written.
///
/// Plain Java on purpose, so the two runtimes share one implementation and no
/// native library has to keep a legacy provider loaded for it.
final class LegacyCiphers {
    private LegacyCiphers() {
    }

    // ------------------------------------------------------------------ DES

    private static final byte[] PC1 = {57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18,
        10, 2, 59, 51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36, 63, 55, 47, 39, 31, 23, 15, 7, 62,
        54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 28, 20, 12, 4};
    private static final byte[] PC2 = {14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12,
        4, 26, 8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40, 51, 45, 33, 48, 44, 49,
        39, 56, 34, 53, 46, 42, 50, 36, 29, 32};
    private static final byte[] SHIFTS = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};
    private static final byte[] IP = {58, 50, 42, 34, 26, 18, 10, 2, 60, 52, 44, 36, 28, 20, 12,
        4, 62, 54, 46, 38, 30, 22, 14, 6, 64, 56, 48, 40, 32, 24, 16, 8, 57, 49, 41, 33, 25, 17,
        9, 1, 59, 51, 43, 35, 27, 19, 11, 3, 61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31,
        23, 15, 7};
    private static final byte[] FP = {40, 8, 48, 16, 56, 24, 64, 32, 39, 7, 47, 15, 55, 23, 63,
        31, 38, 6, 46, 14, 54, 22, 62, 30, 37, 5, 45, 13, 53, 21, 61, 29, 36, 4, 44, 12, 52, 20,
        60, 28, 35, 3, 43, 11, 51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58, 26, 33, 1, 41, 9, 49,
        17, 57, 25};
    private static final byte[] E = {32, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9, 8, 9, 10, 11, 12, 13,
        12, 13, 14, 15, 16, 17, 16, 17, 18, 19, 20, 21, 20, 21, 22, 23, 24, 25, 24, 25, 26, 27,
        28, 29, 28, 29, 30, 31, 32, 1};
    private static final byte[] P = {16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23, 26, 5, 18, 31,
        10, 2, 8, 24, 14, 32, 27, 3, 9, 19, 13, 30, 6, 22, 11, 4, 25};
    private static final byte[][] S = {
        {14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7, 0, 15, 7, 4, 14, 2, 13, 1, 10, 6,
            12, 11, 9, 5, 3, 8, 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0, 15, 12, 8,
            2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13},
        {15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10, 3, 13, 4, 7, 15, 2, 8, 14, 12, 0,
            1, 10, 6, 9, 11, 5, 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15, 13, 8, 10,
            1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9},
        {10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8, 13, 7, 0, 9, 3, 4, 6, 10, 2, 8,
            5, 14, 12, 11, 15, 1, 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7, 1, 10,
            13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12},
        {7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15, 13, 8, 11, 5, 6, 15, 0, 3, 4, 7,
            2, 12, 1, 10, 14, 9, 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4, 3, 15, 0,
            6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14},
        {2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9, 14, 11, 2, 12, 4, 7, 13, 1, 5, 0,
            15, 10, 3, 9, 8, 6, 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14, 11, 8, 12,
            7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3},
        {12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11, 10, 15, 4, 2, 7, 12, 9, 5, 6, 1,
            13, 14, 0, 11, 3, 8, 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6, 4, 3, 2,
            12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13},
        {4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1, 13, 0, 11, 7, 4, 9, 1, 10, 14, 3,
            5, 12, 2, 15, 8, 6, 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2, 6, 11, 13,
            8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12},
        {13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7, 1, 15, 13, 8, 10, 3, 7, 4, 12, 5,
            6, 11, 0, 14, 9, 2, 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8, 2, 1, 14,
            7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11}};

    /// Bit `n` (1 = most significant) of a `width`-bit value.
    private static long permute(long in, int width, byte[] table) {
        long out = 0;
        for (byte bit : table) {
            out = (out << 1) | ((in >>> (width - bit)) & 1L);
        }
        return out;
    }

    /// The sixteen 48-bit round keys of an 8-byte DES key.
    private static long[] desSchedule(byte[] key, int offset) {
        long k = 0;
        for (int iter = 0 ; iter < 8 ; iter++) {
            k = (k << 8) | (key[offset + iter] & 0xffL);
        }
        long cd = permute(k, 64, PC1);
        int c = (int) (cd >>> 28) & 0xfffffff;
        int d = (int) cd & 0xfffffff;
        long[] out = new long[16];
        for (int round = 0 ; round < 16 ; round++) {
            for (int s = 0 ; s < SHIFTS[round] ; s++) {
                c = ((c << 1) | (c >>> 27)) & 0xfffffff;
                d = ((d << 1) | (d >>> 27)) & 0xfffffff;
            }
            out[round] = permute(((long) c << 28) | d, 56, PC2);
        }
        return out;
    }

    private static long des(long block, long[] keys, boolean decrypt) {
        long b = permute(block, 64, IP);
        int l = (int) (b >>> 32);
        int r = (int) b;
        for (int round = 0 ; round < 16 ; round++) {
            long x = permute(r & 0xffffffffL, 32, E) ^ keys[decrypt ? 15 - round : round];
            int f = 0;
            for (int box = 0 ; box < 8 ; box++) {
                int six = (int) (x >>> (42 - 6 * box)) & 0x3f;
                int row = ((six & 0x20) >>> 4) | (six & 1);
                int col = (six >>> 1) & 0xf;
                f = (f << 4) | S[box][row * 16 + col];
            }
            int next = l ^ (int) permute(f & 0xffffffffL, 32, P);
            l = r;
            r = next;
        }
        return permute(((long) r << 32) | (l & 0xffffffffL), 64, FP);
    }

    /// DES-EDE3 (or EDE2 for a 16-byte key) in CBC mode over whole 8-byte blocks.
    static byte[] tripleDes(boolean encrypt, byte[] key, byte[] iv, byte[] data) {
        long[] k1 = desSchedule(key, 0);
        long[] k2 = desSchedule(key, 8);
        long[] k3 = key.length >= 24 ? desSchedule(key, 16) : k1;
        long chain = toLong(iv, 0);
        byte[] out = new byte[data.length];
        for (int at = 0 ; at < data.length ; at += 8) {
            long in = toLong(data, at);
            long value;
            if (encrypt) {
                value = des(des(des(in ^ chain, k1, false), k2, true), k3, false);
                chain = value;
            } else {
                value = des(des(des(in, k3, true), k2, false), k1, true) ^ chain;
                chain = in;
            }
            fromLong(value, out, at);
        }
        return out;
    }

    private static long toLong(byte[] data, int at) {
        long v = 0;
        for (int iter = 0 ; iter < 8 ; iter++) {
            v = (v << 8) | (data[at + iter] & 0xffL);
        }
        return v;
    }

    private static void fromLong(long v, byte[] out, int at) {
        for (int iter = 7 ; iter >= 0 ; iter--) {
            out[at + iter] = (byte) v;
            v >>>= 8;
        }
    }

    // ------------------------------------------------------------------ RC2

    private static final int[] PITABLE = {
        0xd9, 0x78, 0xf9, 0xc4, 0x19, 0xdd, 0xb5, 0xed, 0x28, 0xe9, 0xfd, 0x79, 0x4a, 0xa0, 0xd8, 0x9d,
        0xc6, 0x7e, 0x37, 0x83, 0x2b, 0x76, 0x53, 0x8e, 0x62, 0x4c, 0x64, 0x88, 0x44, 0x8b, 0xfb, 0xa2,
        0x17, 0x9a, 0x59, 0xf5, 0x87, 0xb3, 0x4f, 0x13, 0x61, 0x45, 0x6d, 0x8d, 0x09, 0x81, 0x7d, 0x32,
        0xbd, 0x8f, 0x40, 0xeb, 0x86, 0xb7, 0x7b, 0x0b, 0xf0, 0x95, 0x21, 0x22, 0x5c, 0x6b, 0x4e, 0x82,
        0x54, 0xd6, 0x65, 0x93, 0xce, 0x60, 0xb2, 0x1c, 0x73, 0x56, 0xc0, 0x14, 0xa7, 0x8c, 0xf1, 0xdc,
        0x12, 0x75, 0xca, 0x1f, 0x3b, 0xbe, 0xe4, 0xd1, 0x42, 0x3d, 0xd4, 0x30, 0xa3, 0x3c, 0xb6, 0x26,
        0x6f, 0xbf, 0x0e, 0xda, 0x46, 0x69, 0x07, 0x57, 0x27, 0xf2, 0x1d, 0x9b, 0xbc, 0x94, 0x43, 0x03,
        0xf8, 0x11, 0xc7, 0xf6, 0x90, 0xef, 0x3e, 0xe7, 0x06, 0xc3, 0xd5, 0x2f, 0xc8, 0x66, 0x1e, 0xd7,
        0x08, 0xe8, 0xea, 0xde, 0x80, 0x52, 0xee, 0xf7, 0x84, 0xaa, 0x72, 0xac, 0x35, 0x4d, 0x6a, 0x2a,
        0x96, 0x1a, 0xd2, 0x71, 0x5a, 0x15, 0x49, 0x74, 0x4b, 0x9f, 0xd0, 0x5e, 0x04, 0x18, 0xa4, 0xec,
        0xc2, 0xe0, 0x41, 0x6e, 0x0f, 0x51, 0xcb, 0xcc, 0x24, 0x91, 0xaf, 0x50, 0xa1, 0xf4, 0x70, 0x39,
        0x99, 0x7c, 0x3a, 0x85, 0x23, 0xb8, 0xb4, 0x7a, 0xfc, 0x02, 0x36, 0x5b, 0x25, 0x55, 0x97, 0x31,
        0x2d, 0x5d, 0xfa, 0x98, 0xe3, 0x8a, 0x92, 0xae, 0x05, 0xdf, 0x29, 0x10, 0x67, 0x6c, 0xba, 0xc9,
        0xd3, 0x00, 0xe6, 0xcf, 0xe1, 0x9e, 0xa8, 0x2c, 0x63, 0x16, 0x01, 0x3f, 0x58, 0xe2, 0x89, 0xa9,
        0x0d, 0x38, 0x34, 0x1b, 0xab, 0x33, 0xff, 0xb0, 0xbb, 0x48, 0x0c, 0x5f, 0xb9, 0xb1, 0xcd, 0x2e,
        0xc5, 0xf3, 0xdb, 0x47, 0xe5, 0xa5, 0x9c, 0x77, 0x0a, 0xa6, 0x20, 0x68, 0xfe, 0x7f, 0xc1, 0xad};

    /// RFC 2268 key expansion: 64 16-bit words.
    private static int[] rc2Schedule(byte[] key, int effectiveBits) {
        int[] l = new int[128];
        int t = key.length;
        for (int iter = 0 ; iter < t ; iter++) {
            l[iter] = key[iter] & 0xff;
        }
        for (int iter = t ; iter < 128 ; iter++) {
            l[iter] = PITABLE[(l[iter - 1] + l[iter - t]) & 0xff];
        }
        int t8 = (effectiveBits + 7) / 8;
        int tm = 0xff % (1 << (8 + effectiveBits - 8 * t8));
        l[128 - t8] = PITABLE[l[128 - t8] & tm];
        for (int iter = 127 - t8 ; iter >= 0 ; iter--) {
            l[iter] = PITABLE[l[iter + 1] ^ l[iter + t8]];
        }
        int[] k = new int[64];
        for (int iter = 0 ; iter < 64 ; iter++) {
            k[iter] = l[2 * iter] | (l[2 * iter + 1] << 8);
        }
        return k;
    }

    private static int rol16(int x, int n) {
        return ((x << n) | (x >>> (16 - n))) & 0xffff;
    }

    private static int ror16(int x, int n) {
        return ((x >>> n) | (x << (16 - n))) & 0xffff;
    }

    private static final int[] ROT = {1, 2, 3, 5};

    private static void rc2Encrypt(int[] r, int[] k) {
        int j = 0;
        for (int round = 0 ; round < 16 ; round++) {
            for (int i = 0 ; i < 4 ; i++) {
                r[i] = (r[i] + k[j++] + (r[(i + 3) & 3] & r[(i + 2) & 3])
                        + (~r[(i + 3) & 3] & r[(i + 1) & 3])) & 0xffff;
                r[i] = rol16(r[i], ROT[i]);
            }
            if (round == 4 || round == 10) {
                for (int i = 0 ; i < 4 ; i++) {
                    r[i] = (r[i] + k[r[(i + 3) & 3] & 63]) & 0xffff;
                }
            }
        }
    }

    private static void rc2Decrypt(int[] r, int[] k) {
        int j = 63;
        for (int round = 15 ; round >= 0 ; round--) {
            for (int i = 3 ; i >= 0 ; i--) {
                r[i] = ror16(r[i], ROT[i]);
                r[i] = (r[i] - k[j--] - (r[(i + 3) & 3] & r[(i + 2) & 3])
                        - (~r[(i + 3) & 3] & r[(i + 1) & 3])) & 0xffff;
            }
            if (round == 5 || round == 11) {
                for (int i = 3 ; i >= 0 ; i--) {
                    r[i] = (r[i] - k[r[(i + 3) & 3] & 63]) & 0xffff;
                }
            }
        }
    }

    /// RC2 in CBC mode over whole 8-byte blocks.
    static byte[] rc2(boolean encrypt, byte[] key, int effectiveBits, byte[] iv, byte[] data) {
        int[] k = rc2Schedule(key, effectiveBits);
        byte[] chain = new byte[8];
        System.arraycopy(iv, 0, chain, 0, 8);
        byte[] out = new byte[data.length];
        int[] r = new int[4];
        for (int at = 0 ; at < data.length ; at += 8) {
            byte[] in = new byte[8];
            System.arraycopy(data, at, in, 0, 8);
            for (int i = 0 ; i < 8 ; i++) {
                if (encrypt) {
                    in[i] ^= chain[i];
                }
            }
            for (int i = 0 ; i < 4 ; i++) {
                r[i] = (in[2 * i] & 0xff) | ((in[2 * i + 1] & 0xff) << 8);
            }
            if (encrypt) {
                rc2Encrypt(r, k);
            } else {
                rc2Decrypt(r, k);
            }
            for (int i = 0 ; i < 4 ; i++) {
                out[at + 2 * i] = (byte) r[i];
                out[at + 2 * i + 1] = (byte) (r[i] >>> 8);
            }
            if (encrypt) {
                System.arraycopy(out, at, chain, 0, 8);
            } else {
                for (int i = 0 ; i < 8 ; i++) {
                    out[at + i] ^= chain[i];
                }
                System.arraycopy(data, at, chain, 0, 8);
            }
        }
        return out;
    }
}
