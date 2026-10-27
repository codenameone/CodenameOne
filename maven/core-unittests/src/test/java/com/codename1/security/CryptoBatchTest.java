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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// A batch must answer every entry exactly as the single call does, whatever
/// the engine leaves undecided, and treat a malformed entry as invalid.
class CryptoBatchTest {
    private static final int N = 24;

    @AfterEach
    void portable() {
        CryptoEngine.install(null);
    }

    private static byte[] bytes(int seed, int length) {
        byte[] out = new byte[length];
        int x = seed * 0x9E3779B1 + 0x7F4A7C15;
        for (int i = 0; i < length; i++) {
            x ^= x << 13;
            x ^= x >>> 17;
            x ^= x << 5;
            out[i] = (byte) x;
        }
        return out;
    }

    private static byte[] flip(byte[] a, int index) {
        byte[] b = new byte[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        b[index] ^= 1;
        return b;
    }

    @Test
    void secp256k1MatchesSingleVerify() {
        byte[][] hashes = new byte[N][];
        Secp256k1.EcdsaSignature[] sigs = new Secp256k1.EcdsaSignature[N];
        byte[][] keys = new byte[N][];
        for (int i = 0; i < N; i++) {
            byte[] priv = bytes(i + 1, 32);
            hashes[i] = bytes(i + 100, 32);
            sigs[i] = Secp256k1.sign(hashes[i], priv);
            keys[i] = Secp256k1.publicKey(priv, i % 2 == 0);
        }
        hashes[3] = flip(hashes[3], 0);
        keys[5] = keys[6];
        hashes[7] = new byte[31];
        sigs[8] = null;
        keys[9] = flip(keys[9], 10);
        keys[10] = null;
        keys[11] = new byte[12];
        boolean[] got = CryptoBatch.verifySecp256k1(hashes, sigs, keys);
        for (int i = 0; i < N; i++) {
            boolean want;
            try {
                want = Secp256k1.verify(hashes[i], sigs[i], keys[i]);
            } catch (CryptoException e) {
                want = false;
            }
            assertEquals(want, got[i], "entry " + i);
        }
        assertTrue(got[0]);
        assertFalse(got[3] || got[5] || got[7] || got[8] || got[10] || got[11]);
    }

    @Test
    void ed25519AndSchnorrMatchSingleVerify() {
        byte[][] msgs = new byte[N][];
        byte[][] edSigs = new byte[N][];
        byte[][] edKeys = new byte[N][];
        byte[][] schSigs = new byte[N][];
        byte[][] schKeys = new byte[N][];
        for (int i = 0; i < N; i++) {
            byte[] seed = bytes(i + 200, 32);
            msgs[i] = bytes(i + 300, i * 7);
            edKeys[i] = Ed25519.publicKey(seed);
            edSigs[i] = Ed25519.sign(seed, edKeys[i], msgs[i]);
            schKeys[i] = Schnorr.publicKey(seed);
            schSigs[i] = Schnorr.sign(msgs[i], seed, null);
        }
        edSigs[2] = flip(edSigs[2], 1);
        schSigs[2] = flip(schSigs[2], 1);
        edSigs[4] = new byte[63];
        schKeys[4] = new byte[33];
        edKeys[6] = null;
        msgs[7] = null;
        // S + L: refused as non-canonical
        edSigs[9][63] |= 0x10;
        boolean[] ed = CryptoBatch.verifyEd25519(msgs, edSigs, edKeys);
        boolean[] sch = CryptoBatch.verifySchnorr(msgs, schSigs, schKeys);
        for (int i = 0; i < N; i++) {
            assertEquals(Ed25519.verify(msgs[i], edSigs[i], edKeys[i]), ed[i], "ed25519 entry " + i);
            boolean want;
            try {
                want = Schnorr.verify(msgs[i], schSigs[i], schKeys[i]);
            } catch (CryptoException e) {
                want = false;
            }
            assertEquals(want, sch[i], "schnorr entry " + i);
        }
        assertTrue(ed[0] && sch[0]);
        assertFalse(ed[2] || sch[2] || ed[4] || sch[4] || ed[6] || ed[7] || sch[7] || ed[9]);
    }

    @Test
    void entriesTheEngineLeavesAreVerifiedInJava() {
        // an engine that hands every entry back, one way or the other
        CryptoEngine.install(new CryptoEngine() {
            @Override
            public void verifyBatch(int algorithm, byte[][] messages, byte[][] signatures, byte[][] publicKeys,
                    byte[] results) {
                for (int i = 0; i < results.length; i += 2) {
                    if (results[i] == 2) {
                        results[i] = (byte) 0xff;
                    }
                }
            }
        });
        byte[] priv = bytes(1, 32);
        byte[] hash = bytes(2, 32);
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash, priv);
        byte[] key = Secp256k1.publicKey(priv, true);
        boolean[] got = CryptoBatch.verifySecp256k1(new byte[][] {hash, hash, flip(hash, 3)},
                new Secp256k1.EcdsaSignature[] {sig, sig, sig}, new byte[][] {key, key, key});
        assertTrue(got[0]);
        assertTrue(got[1]);
        assertFalse(got[2]);
    }

    @Test
    void hashesMatchSingleCalls() {
        byte[][] in = new byte[N][];
        for (int i = 0; i < N; i++) {
            in[i] = i == 5 ? null : bytes(i + 400, i * 31);
        }
        byte[][] sha = CryptoBatch.sha256(in);
        byte[][] kec = CryptoBatch.keccak256(in);
        for (int i = 0; i < N; i++) {
            if (in[i] == null) {
                assertNull(sha[i]);
                assertNull(kec[i]);
            } else {
                assertArrayEquals(Hash.sha256(in[i]), sha[i]);
                assertArrayEquals(Hash.keccak256(in[i]), kec[i]);
            }
        }
    }

    private static byte[] pack(byte[][] parts, int length) {
        byte[] out = new byte[parts.length * length];
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                System.arraycopy(parts[i], 0, out, i * length, Math.min(length, parts[i].length));
            }
        }
        return out;
    }

    @Test
    void packedFormsMatchSingleVerify() {
        byte[][] hashes = new byte[N][];
        byte[][] compact = new byte[N][];
        byte[][] keys = new byte[N][];
        byte[][] msgs = new byte[N][];
        byte[][] edSigs = new byte[N][];
        byte[][] edKeys = new byte[N][];
        byte[][] schSigs = new byte[N][];
        byte[][] schKeys = new byte[N][];
        for (int i = 0; i < N; i++) {
            byte[] priv = bytes(i + 1, 32);
            hashes[i] = bytes(i + 100, 32);
            compact[i] = Secp256k1.sign(hashes[i], priv).toCompact();
            keys[i] = Secp256k1.publicKey(priv, true);
            byte[] seed = bytes(i + 200, 32);
            msgs[i] = bytes(i + 300, 40);
            edKeys[i] = Ed25519.publicKey(seed);
            edSigs[i] = Ed25519.sign(seed, edKeys[i], msgs[i]);
            schKeys[i] = Schnorr.publicKey(seed);
            schSigs[i] = Schnorr.sign(msgs[i], seed, null);
        }
        hashes[3] = flip(hashes[3], 0);
        keys[5] = keys[6];
        keys[9] = flip(keys[9], 10);
        compact[11] = new byte[64];
        edSigs[2] = flip(edSigs[2], 1);
        schSigs[2] = flip(schSigs[2], 1);
        edSigs[9][63] |= 0x10;
        edKeys[12] = new byte[32];
        schKeys[12] = new byte[32];
        byte[] ecdsa = new byte[N];
        CryptoBatch.verifySecp256k1(pack(hashes, 32), pack(compact, 64), pack(keys, 33), 33, ecdsa, N);
        byte[] ed = new byte[N + 3];
        CryptoBatch.verifyEd25519(pack(msgs, 40), 40, pack(edSigs, 64), pack(edKeys, 32), ed, N);
        byte[] sch = new byte[N];
        CryptoBatch.verifySchnorr(pack(msgs, 40), 40, pack(schSigs, 64), pack(schKeys, 32), sch, N);
        for (int i = 0; i < N; i++) {
            boolean want;
            try {
                want = Secp256k1.verify(hashes[i], Secp256k1.EcdsaSignature.fromCompact(compact[i], 0), keys[i]);
            } catch (CryptoException e) {
                want = false;
            }
            assertEquals(want ? 1 : 0, ecdsa[i], "ecdsa entry " + i);
            assertEquals(Ed25519.verify(msgs[i], edSigs[i], edKeys[i]) ? 1 : 0, ed[i], "ed25519 entry " + i);
            try {
                want = Schnorr.verify(msgs[i], schSigs[i], schKeys[i]);
            } catch (CryptoException e) {
                want = false;
            }
            assertEquals(want ? 1 : 0, sch[i], "schnorr entry " + i);
        }
        assertEquals(1, ecdsa[0]);
        assertEquals(1, ed[0]);
        assertEquals(1, sch[0]);
        assertEquals(0, ecdsa[3] | ecdsa[5] | ecdsa[11] | ed[2] | ed[9] | ed[12] | sch[2] | sch[12]);
        // past count nothing is written
        assertEquals(0, ed[N] | ed[N + 1] | ed[N + 2]);
    }

    @Test
    void packedHashesMatchSingleCalls() {
        int n = 33;
        byte[] in = bytes(7, 64 * n);
        byte[] sha = new byte[32 * n];
        byte[] kec = new byte[32 * n];
        CryptoBatch.sha256(in, 64, n, sha);
        CryptoBatch.keccak256(in, 64, n, kec);
        for (int i = 0; i < n; i++) {
            byte[] one = new byte[64];
            System.arraycopy(in, 64 * i, one, 0, 64);
            byte[] a = new byte[32];
            byte[] b = new byte[32];
            System.arraycopy(sha, 32 * i, a, 0, 32);
            System.arraycopy(kec, 32 * i, b, 0, 32);
            assertArrayEquals(Hash.sha256(one), a, "sha256 entry " + i);
            assertArrayEquals(Hash.keccak256(one), b, "keccak entry " + i);
        }
        // a Merkle level in place: the digests of pairs become the next level's input
        byte[] level = new byte[32 * 4];
        CryptoBatch.sha256(sha, 64, 4, level);
        byte[] pair = new byte[64];
        System.arraycopy(sha, 64, pair, 0, 64);
        byte[] second = new byte[32];
        System.arraycopy(level, 32, second, 0, 32);
        assertArrayEquals(Hash.sha256(pair), second);
    }

    @Test
    void packedRangesAreChecked() {
        assertThrows(CryptoException.class,
                () -> CryptoBatch.verifySecp256k1(new byte[32], new byte[64], new byte[33], 34, new byte[1], 1));
        assertThrows(CryptoException.class,
                () -> CryptoBatch.verifySecp256k1(new byte[32], new byte[63], new byte[33], 33, new byte[1], 1));
        assertThrows(CryptoException.class,
                () -> CryptoBatch.verifyEd25519(new byte[10], 5, new byte[128], new byte[64], new byte[1], 2));
        assertThrows(CryptoException.class, () -> CryptoBatch.sha256(new byte[64], 64, 2, new byte[64]));
    }

    @Test
    void mismatchedArraysAreRefused() {
        assertThrows(CryptoException.class, () -> CryptoBatch.verifyEd25519(new byte[2][], new byte[1][], new byte[2][]));
        assertThrows(CryptoException.class, () -> CryptoBatch.sha256(null));
    }
}
