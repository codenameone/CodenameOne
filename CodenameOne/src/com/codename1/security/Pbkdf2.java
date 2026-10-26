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
package com.codename1.security;

/// PBKDF2 (RFC 8018) with HMAC-SHA-256 or HMAC-SHA-512: the password hash of
/// key stores and vaults, and with SHA-512 and 2048 iterations the seed of a
/// BIP-39 mnemonic.
///
/// A [CryptoEngine] that derives natively is used when one is installed
/// (every client port installs one). Otherwise the derivation runs in Java,
/// and still costs two hash compressions per iteration: the HMAC key's inner
/// and outer padded blocks are compressed once, up front, and each iteration
/// resumes from those two states with the previous output as its only block.
/// A straightforward HMAC loop re-hashes both padded keys every round, twice
/// the work.
///
/// ```java
/// byte[] seed = Pbkdf2.hmacSha512(mnemonicUtf8, saltUtf8, 2048, 64);
/// ```
@com.codename1.impl.SharedWithBackend
public final class Pbkdf2 {
    private static final long[] IV512 = {
        0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL, 0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
        0x510e527fade682d1L, 0x9b05688c2b3e6c1fL, 0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
    };
    private static final int[] IV256 = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };

    private Pbkdf2() {
    }

    /// PBKDF2 with HMAC-SHA-512.
    ///
    /// #### Parameters
    ///
    /// - `password`: the password bytes, used as they are (encode text as
    ///   UTF-8, normalized as the protocol requires, before calling)
    ///
    /// - `salt`: the salt
    ///
    /// - `iterations`: at least 1
    ///
    /// - `length`: how many bytes to derive, at least 1
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if an argument is null or out of range
    public static byte[] hmacSha512(byte[] password, byte[] salt, int iterations, int length) {
        return derive(Hash.SHA512, password, salt, iterations, length);
    }

    /// PBKDF2 with HMAC-SHA-256; see [#hmacSha512] for the parameters.
    public static byte[] hmacSha256(byte[] password, byte[] salt, int iterations, int length) {
        return derive(Hash.SHA256, password, salt, iterations, length);
    }

    private static byte[] derive(String hash, byte[] password, byte[] salt, int iterations, int length) {
        if (password == null || salt == null) {
            throw new CryptoException("password and salt must not be null");
        }
        if (iterations < 1 || length < 1) {
            throw new CryptoException("iterations and length must be positive");
        }
        byte[] fast = CryptoEngine.get().pbkdf2(hash, password, salt, iterations, length);
        if (fast != null && fast.length == length) {
            return fast;
        }
        return Hash.SHA512.equals(hash)
                ? portable512(password, salt, iterations, length)
                : portable256(password, salt, iterations, length);
    }

    /// The Java derivation with HMAC-SHA-512, whatever engine is installed:
    /// what an engine falls back to, and what the tests hold a native one to.
    static byte[] portable512(byte[] password, byte[] salt, int iterations, int length) {
        byte[] key = password.length > 128 ? Hash.sha512(password) : password;
        long[] w = new long[80];
        long[] inner = padState512(key, (byte) 0x36, w);
        long[] outer = padState512(key, (byte) 0x5c, w);
        int blocks = (length + 63) / 64;
        byte[] out = new byte[blocks * 64];
        byte[] msg = new byte[salt.length + 4];
        System.arraycopy(salt, 0, msg, 0, salt.length);
        long[] u = new long[8];
        long[] t = new long[8];
        long[] st = new long[8];
        for (int index = 1; index <= blocks; index++) {
            putInt(msg, salt.length, index);
            byte[] u1 = Hmac.sha512(password, msg);
            for (int i = 0; i < 8; i++) {
                u[i] = readLong(u1, i * 8);
                t[i] = u[i];
            }
            for (int round = 1; round < iterations; round++) {
                hmacBlock512(inner, outer, u, st, w);
                for (int i = 0; i < 8; i++) {
                    t[i] ^= u[i];
                }
            }
            for (int i = 0; i < 8; i++) {
                writeLong(out, (index - 1) * 64 + i * 8, t[i]);
            }
        }
        byte[] exact = new byte[length];
        System.arraycopy(out, 0, exact, 0, length);
        zero(out);
        zero(u);
        zero(t);
        zero(st);
        zero(inner);
        zero(outer);
        return exact;
    }

    // u = HMAC(u) from the two precomputed pad states: the message is one
    // 64-byte block, so each half is a single compression with the padding
    // and the length (128 + 64 bytes) written in directly
    private static void hmacBlock512(long[] inner, long[] outer, long[] u, long[] st, long[] w) {
        System.arraycopy(inner, 0, st, 0, 8);
        System.arraycopy(u, 0, w, 0, 8);
        w[8] = 0x8000000000000000L;
        for (int i = 9; i < 15; i++) {
            w[i] = 0;
        }
        w[15] = (128 + 64) * 8;
        MessageDigestImpl.Sha512Family.compress(st, w);
        System.arraycopy(outer, 0, u, 0, 8);
        System.arraycopy(st, 0, w, 0, 8);
        w[8] = 0x8000000000000000L;
        for (int i = 9; i < 15; i++) {
            w[i] = 0;
        }
        w[15] = (128 + 64) * 8;
        MessageDigestImpl.Sha512Family.compress(u, w);
    }

    private static long[] padState512(byte[] key, byte pad, long[] w) {
        byte[] block = new byte[128];
        for (int i = 0; i < 128; i++) {
            block[i] = (byte) ((i < key.length ? key[i] : 0) ^ pad);
        }
        for (int i = 0; i < 16; i++) {
            w[i] = readLong(block, i * 8);
        }
        long[] st = new long[8];
        System.arraycopy(IV512, 0, st, 0, 8);
        MessageDigestImpl.Sha512Family.compress(st, w);
        zero(block);
        return st;
    }

    /// The Java derivation with HMAC-SHA-256; see [#portable512].
    static byte[] portable256(byte[] password, byte[] salt, int iterations, int length) {
        byte[] key = password.length > 64 ? Hash.sha256(password) : password;
        int[] w = new int[64];
        int[] inner = padState256(key, (byte) 0x36, w);
        int[] outer = padState256(key, (byte) 0x5c, w);
        int blocks = (length + 31) / 32;
        byte[] out = new byte[blocks * 32];
        byte[] msg = new byte[salt.length + 4];
        System.arraycopy(salt, 0, msg, 0, salt.length);
        int[] u = new int[8];
        int[] t = new int[8];
        int[] st = new int[8];
        for (int index = 1; index <= blocks; index++) {
            putInt(msg, salt.length, index);
            byte[] u1 = Hmac.sha256(password, msg);
            for (int i = 0; i < 8; i++) {
                u[i] = readInt(u1, i * 4);
                t[i] = u[i];
            }
            for (int round = 1; round < iterations; round++) {
                System.arraycopy(inner, 0, st, 0, 8);
                System.arraycopy(u, 0, w, 0, 8);
                w[8] = 0x80000000;
                for (int i = 9; i < 15; i++) {
                    w[i] = 0;
                }
                w[15] = (64 + 32) * 8;
                MessageDigestImpl.Sha256Family.compress(st, w);
                System.arraycopy(outer, 0, u, 0, 8);
                System.arraycopy(st, 0, w, 0, 8);
                w[8] = 0x80000000;
                for (int i = 9; i < 15; i++) {
                    w[i] = 0;
                }
                w[15] = (64 + 32) * 8;
                MessageDigestImpl.Sha256Family.compress(u, w);
                for (int i = 0; i < 8; i++) {
                    t[i] ^= u[i];
                }
            }
            for (int i = 0; i < 8; i++) {
                putInt(out, (index - 1) * 32 + i * 4, t[i]);
            }
        }
        byte[] exact = new byte[length];
        System.arraycopy(out, 0, exact, 0, length);
        zero(out);
        for (int i = 0; i < 8; i++) {
            u[i] = 0;
            t[i] = 0;
            st[i] = 0;
            inner[i] = 0;
            outer[i] = 0;
        }
        return exact;
    }

    private static int[] padState256(byte[] key, byte pad, int[] w) {
        byte[] block = new byte[64];
        for (int i = 0; i < 64; i++) {
            block[i] = (byte) ((i < key.length ? key[i] : 0) ^ pad);
        }
        for (int i = 0; i < 16; i++) {
            w[i] = readInt(block, i * 4);
        }
        int[] st = new int[8];
        System.arraycopy(IV256, 0, st, 0, 8);
        MessageDigestImpl.Sha256Family.compress(st, w);
        zero(block);
        return st;
    }

    private static long readLong(byte[] b, int o) {
        return ((long) readInt(b, o) << 32) | (readInt(b, o + 4) & 0xffffffffL);
    }

    private static int readInt(byte[] b, int o) {
        return (b[o] & 0xff) << 24 | (b[o + 1] & 0xff) << 16 | (b[o + 2] & 0xff) << 8 | (b[o + 3] & 0xff);
    }

    private static void writeLong(byte[] b, int o, long v) {
        putInt(b, o, (int) (v >>> 32));
        putInt(b, o + 4, (int) v);
    }

    private static void putInt(byte[] b, int o, int v) {
        b[o] = (byte) (v >>> 24);
        b[o + 1] = (byte) (v >>> 16);
        b[o + 2] = (byte) (v >>> 8);
        b[o + 3] = (byte) v;
    }

    private static void zero(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }

    private static void zero(long[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }
}
