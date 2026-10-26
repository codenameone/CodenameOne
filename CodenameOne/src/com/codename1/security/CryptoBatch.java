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

/// Many signature verifications or hashes in one call: the deposits of a
/// block an exchange scans, the transactions an indexer checks, the leaves of
/// a Merkle tree.
///
/// Where the platform has native kernels the whole batch crosses into native
/// code once and is spread over every CPU core, so a batch of thousands runs
/// several times faster than the same calls one at a time. Elsewhere it is
/// the same loop over [Secp256k1#verify], [Schnorr#verify],
/// [Ed25519#verify] or [Hash] that a caller would write.
///
/// Every entry gets exactly the answer the single call gives it; nothing here
/// uses a batch equation that could accept a signature a single verify
/// rejects. A malformed entry -- a `null`, a key that does not parse, a
/// wrong length -- is simply invalid, so one bad entry never aborts the rest.
@com.codename1.impl.SharedWithBackend
public final class CryptoBatch {
    private CryptoBatch() {
    }

    /// ECDSA over secp256k1, entry by entry as [Secp256k1#verify]: each
    /// 32-byte hash, signature and SEC1 public key.
    public static boolean[] verifySecp256k1(byte[][] hashes, Secp256k1.EcdsaSignature[] signatures,
            byte[][] publicKeys) {
        int n = count(hashes, signatures, publicKeys);
        byte[][] compact = new byte[n][];
        byte[] results = new byte[n];
        for (int i = 0; i < n; i++) {
            Secp256k1.EcdsaSignature s = signatures[i];
            compact[i] = s == null ? null : s.toCompact();
            results[i] = is(hashes[i], 32) && compact[i] != null && publicKeys[i] != null ? TODO : 0;
        }
        CryptoEngine.get().verifyBatch(CryptoEngine.BATCH_SECP256K1, hashes, compact, publicKeys, results);
        boolean[] out = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (results[i] == TODO || results[i] == JAVA) {
                try {
                    out[i] = Secp256k1.verify(hashes[i], signatures[i], publicKeys[i]);
                } catch (CryptoException e) {
                    out[i] = false;
                }
            } else {
                out[i] = results[i] == 1;
            }
        }
        return out;
    }

    /// BIP-340 Schnorr, entry by entry as [Schnorr#verify]: each message, 64-byte
    /// signature and 32-byte x-only public key.
    public static boolean[] verifySchnorr(byte[][] messages, byte[][] signatures, byte[][] publicKeys) {
        return verify(CryptoEngine.BATCH_SCHNORR, messages, signatures, publicKeys);
    }

    /// Ed25519, entry by entry as [Ed25519#verify]: each message, 64-byte
    /// signature and 32-byte public key.
    public static boolean[] verifyEd25519(byte[][] messages, byte[][] signatures, byte[][] publicKeys) {
        return verify(CryptoEngine.BATCH_ED25519, messages, signatures, publicKeys);
    }

    /// The SHA-256 of each input. A `null` input has a `null` digest.
    public static byte[][] sha256(byte[][] inputs) {
        return hash(Hash.SHA256, inputs);
    }

    /// The Keccak-256 of each input (Ethereum's hash). A `null` input has a
    /// `null` digest.
    public static byte[][] keccak256(byte[][] inputs) {
        return hash(Hash.KECCAK256, inputs);
    }

    /// A result byte the engine has still to decide.
    static final byte TODO = 2;

    /// A result byte the engine leaves to the Java implementation.
    static final byte JAVA = (byte) 0xff;

    private static boolean[] verify(int algorithm, byte[][] messages, byte[][] signatures, byte[][] publicKeys) {
        int n = count(messages, signatures, publicKeys);
        byte[] results = new byte[n];
        for (int i = 0; i < n; i++) {
            results[i] = messages[i] != null && is(signatures[i], 64) && is(publicKeys[i], 32) ? TODO : 0;
        }
        CryptoEngine.get().verifyBatch(algorithm, messages, signatures, publicKeys, results);
        boolean[] out = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (results[i] == TODO || results[i] == JAVA) {
                out[i] = algorithm == CryptoEngine.BATCH_ED25519
                        ? Ed25519.verify(messages[i], signatures[i], publicKeys[i])
                        : schnorr(messages[i], signatures[i], publicKeys[i]);
            } else {
                out[i] = results[i] == 1;
            }
        }
        return out;
    }

    private static boolean schnorr(byte[] message, byte[] signature, byte[] publicKey) {
        try {
            return Schnorr.verify(message, signature, publicKey);
        } catch (CryptoException e) {
            return false;
        }
    }

    private static byte[][] hash(String algorithm, byte[][] inputs) {
        if (inputs == null) {
            throw new CryptoException("inputs must not be null");
        }
        byte[][] fast = CryptoEngine.get().hashBatch(algorithm, inputs);
        if (fast != null && fast.length == inputs.length) {
            return fast;
        }
        byte[][] out = new byte[inputs.length][];
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != null) {
                out[i] = Hash.SHA256.equals(algorithm) ? Hash.sha256(inputs[i]) : Hash.keccak256(inputs[i]);
            }
        }
        return out;
    }

    private static int count(byte[][] a, Object[] b, byte[][] c) {
        if (a == null || b == null || c == null) {
            throw new CryptoException("the batch arrays must not be null");
        }
        if (a.length != b.length || a.length != c.length) {
            throw new CryptoException("the batch arrays differ in length");
        }
        return a.length;
    }

    private static boolean is(byte[] a, int length) {
        return a != null && a.length == length;
    }
}
