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
package com.codename1.security;

/// AES (FIPS 197) in CBC mode over whole 16 byte blocks, with no padding: the
/// building block of formats that define their own padding or key handling,
/// such as the PDF security handler. Runs on the client and on the backend.
///
/// The installed [CryptoEngine] does the work where the platform has a native
/// cipher (the client ports and the backend); elsewhere, the JavaScript port
/// among them, a portable Java implementation produces the same bytes. For
/// authenticated encryption of application data on the client use `Cipher` instead.
///
/// ```java
/// byte[] key = new byte[16];
/// byte[] iv = new byte[16];
/// byte[] sealed = Aes.cbcEncrypt(key, iv, new byte[32]);
/// byte[] plain = Aes.cbcDecrypt(key, iv, sealed);
/// ```
@com.codename1.impl.SharedWithBackend
public final class Aes {
    private static final int[] SBOX = new int[256];
    private static final int[] INV = new int[256];

    static {
        int p = 1;
        int q = 1;
        do {
            p = p ^ ((p << 1) & 0xff) ^ ((p & 0x80) != 0 ? 0x1b : 0);
            q ^= q << 1;
            q ^= q << 2;
            q ^= q << 4;
            q &= 0xff;
            if ((q & 0x80) != 0) {
                q ^= 0x09;
            }
            int x = q ^ rotl(q, 1) ^ rotl(q, 2) ^ rotl(q, 3) ^ rotl(q, 4);
            SBOX[p] = (x ^ 0x63) & 0xff;
        } while (p != 1);
        SBOX[0] = 0x63;
        for (int i = 0; i < 256; i++) {
            INV[SBOX[i]] = i;
        }
    }

    private final int[] w;
    private final int rounds;

    private static int rotl(int x, int s) {
        return ((x << s) | (x >> (8 - s))) & 0xff;
    }

    /// Encrypts `data`, a whole number of blocks, with `key` (16, 24 or 32
    /// bytes) chained from the 16 byte `iv`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a key, IV or data of the wrong length
    public static byte[] cbcEncrypt(byte[] key, byte[] iv, byte[] data) {
        check(key, iv, data);
        byte[] fast = CryptoEngine.get().aesCbc(true, key, iv, data);
        return fast != null && fast.length == data.length ? fast : new Aes(key).encryptCbc(iv, data);
    }

    /// Decrypts `data`, a whole number of blocks, with `key` (16, 24 or 32
    /// bytes) chained from the 16 byte `iv`. Padding, if the format has any,
    /// is left for the caller to remove.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a key, IV or data of the wrong length
    public static byte[] cbcDecrypt(byte[] key, byte[] iv, byte[] data) {
        check(key, iv, data);
        byte[] fast = CryptoEngine.get().aesCbc(false, key, iv, data);
        return fast != null && fast.length == data.length ? fast : new Aes(key).decryptCbc(iv, data);
    }

    private static void check(byte[] key, byte[] iv, byte[] data) {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException("AES key of " + key.length + " bytes");
        }
        if (iv.length != 16 || data.length % 16 != 0) {
            throw new IllegalArgumentException("AES-CBC needs a 16 byte IV and whole blocks");
        }
    }

    private Aes(byte[] key) {
        int nk = key.length / 4;
        rounds = nk + 6;
        w = new int[4 * (rounds + 1)];
        for (int i = 0; i < nk; i++) {
            w[i] = ((key[4 * i] & 0xff) << 24) | ((key[4 * i + 1] & 0xff) << 16) | ((key[4 * i + 2] & 0xff) << 8)
                    | (key[4 * i + 3] & 0xff);
        }
        int rcon = 1;
        for (int i = nk; i < w.length; i++) {
            int t = w[i - 1];
            if (i % nk == 0) {
                t = sub((t << 8) | (t >>> 24)) ^ (rcon << 24);
                rcon = xtime(rcon);
            } else if (nk > 6 && i % nk == 4) {
                t = sub(t);
            }
            w[i] = w[i - nk] ^ t;
        }
    }

    private static int sub(int t) {
        return (SBOX[(t >>> 24) & 0xff] << 24) | (SBOX[(t >>> 16) & 0xff] << 16) | (SBOX[(t >>> 8) & 0xff] << 8)
                | SBOX[t & 0xff];
    }

    private static int xtime(int b) {
        return ((b << 1) ^ ((b & 0x80) != 0 ? 0x1b : 0)) & 0xff;
    }

    private static int mul(int a, int b) {
        int r = 0;
        while (b != 0) {
            if ((b & 1) != 0) {
                r ^= a;
            }
            a = xtime(a);
            b >>= 1;
        }
        return r;
    }

    private void addKey(int[] s, int round) {
        for (int c = 0; c < 4; c++) {
            int k = w[round * 4 + c];
            s[c * 4] ^= (k >>> 24) & 0xff;
            s[c * 4 + 1] ^= (k >>> 16) & 0xff;
            s[c * 4 + 2] ^= (k >>> 8) & 0xff;
            s[c * 4 + 3] ^= k & 0xff;
        }
    }

    private void encryptBlock(byte[] in, int inOff, byte[] out, int outOff) {
        int[] s = new int[16];
        for (int i = 0; i < 16; i++) {
            s[i] = in[inOff + i] & 0xff;
        }
        addKey(s, 0);
        for (int r = 1; r <= rounds; r++) {
            for (int i = 0; i < 16; i++) {
                s[i] = SBOX[s[i]];
            }
            int[] t = new int[16];
            for (int c = 0; c < 4; c++) {
                for (int row = 0; row < 4; row++) {
                    t[c * 4 + row] = s[((c + row) % 4) * 4 + row];
                }
            }
            if (r != rounds) {
                for (int c = 0; c < 4; c++) {
                    int a0 = t[c * 4];
                    int a1 = t[c * 4 + 1];
                    int a2 = t[c * 4 + 2];
                    int a3 = t[c * 4 + 3];
                    s[c * 4] = mul(a0, 2) ^ mul(a1, 3) ^ a2 ^ a3;
                    s[c * 4 + 1] = a0 ^ mul(a1, 2) ^ mul(a2, 3) ^ a3;
                    s[c * 4 + 2] = a0 ^ a1 ^ mul(a2, 2) ^ mul(a3, 3);
                    s[c * 4 + 3] = mul(a0, 3) ^ a1 ^ a2 ^ mul(a3, 2);
                }
            } else {
                System.arraycopy(t, 0, s, 0, 16);
            }
            addKey(s, r);
        }
        for (int i = 0; i < 16; i++) {
            out[outOff + i] = (byte) s[i];
        }
    }

    private void decryptBlock(byte[] in, int inOff, byte[] out, int outOff) {
        int[] s = new int[16];
        for (int i = 0; i < 16; i++) {
            s[i] = in[inOff + i] & 0xff;
        }
        addKey(s, rounds);
        for (int r = rounds - 1; r >= 0; r--) {
            int[] t = new int[16];
            for (int c = 0; c < 4; c++) {
                for (int row = 0; row < 4; row++) {
                    t[((c + row) % 4) * 4 + row] = s[c * 4 + row];
                }
            }
            for (int i = 0; i < 16; i++) {
                s[i] = INV[t[i]];
            }
            addKey(s, r);
            if (r != 0) {
                for (int c = 0; c < 4; c++) {
                    int a0 = s[c * 4];
                    int a1 = s[c * 4 + 1];
                    int a2 = s[c * 4 + 2];
                    int a3 = s[c * 4 + 3];
                    s[c * 4] = mul(a0, 14) ^ mul(a1, 11) ^ mul(a2, 13) ^ mul(a3, 9);
                    s[c * 4 + 1] = mul(a0, 9) ^ mul(a1, 14) ^ mul(a2, 11) ^ mul(a3, 13);
                    s[c * 4 + 2] = mul(a0, 13) ^ mul(a1, 9) ^ mul(a2, 14) ^ mul(a3, 11);
                    s[c * 4 + 3] = mul(a0, 11) ^ mul(a1, 13) ^ mul(a2, 9) ^ mul(a3, 14);
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            out[outOff + i] = (byte) s[i];
        }
    }

    private byte[] decryptCbc(byte[] iv, byte[] data) {
        int off = 0;
        int blocks = data.length / 16;
        byte[] out = new byte[blocks * 16];
        byte[] prev = new byte[16];
        System.arraycopy(iv, 0, prev, 0, 16);
        for (int b = 0; b < blocks; b++) {
            decryptBlock(data, off + b * 16, out, b * 16);
            for (int i = 0; i < 16; i++) {
                out[b * 16 + i] ^= prev[i];
            }
            System.arraycopy(data, off + b * 16, prev, 0, 16);
        }
        return out;
    }

    private byte[] encryptCbc(byte[] iv, byte[] data) {
        byte[] out = new byte[data.length / 16 * 16];
        byte[] prev = new byte[16];
        System.arraycopy(iv, 0, prev, 0, 16);
        byte[] block = new byte[16];
        for (int b = 0; b < out.length; b += 16) {
            for (int i = 0; i < 16; i++) {
                block[i] = (byte) (data[b + i] ^ prev[i]);
            }
            encryptBlock(block, 0, out, b);
            System.arraycopy(out, b, prev, 0, 16);
        }
        return out;
    }
}
