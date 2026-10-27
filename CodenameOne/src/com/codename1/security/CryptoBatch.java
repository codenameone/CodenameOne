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

    /// The packed form of
    /// [#verifySecp256k1(byte[][], Secp256k1.EcdsaSignature[], byte[][])], for
    /// batches large enough that an object per entry matters. Entry `i` is the
    /// 32-byte hash at `32 * i` of `hashes`, the 64-byte compact signature
    /// (`r || s`) at `64 * i` of `signatures`, and the SEC1 public key of
    /// `publicKeyLength` bytes (33 or 65) at `publicKeyLength * i` of
    /// `publicKeys`. Writes 1 (valid) or 0 to each of the first `count` bytes
    /// of `results`; a malformed entry is 0.
    ///
    /// On a native target the kernels read and write these arrays where they
    /// are, without copying them, so a pipeline that fills them in place pays
    /// nothing per entry outside the verification itself. Allocating them with
    /// [com.codename1.util.Simd#allocByte(int)] aligns them for the vector
    /// units and the GPU.
    public static void verifySecp256k1(byte[] hashes, byte[] signatures, byte[] publicKeys, int publicKeyLength,
            byte[] results, int count) {
        if (publicKeyLength != 33 && publicKeyLength != 65) {
            throw new CryptoException("publicKeyLength must be 33 or 65");
        }
        packed(hashes, 32, signatures, publicKeys, publicKeyLength, results, count);
        prime(results, count);
        CryptoEngine.get().verifyPacked(CryptoEngine.BATCH_SECP256K1, hashes, 32, signatures, publicKeys,
                publicKeyLength, results, count);
        for (int i = 0; i < count; i++) {
            if (results[i] == TODO || results[i] == JAVA) {
                try {
                    results[i] = Secp256k1.verify(slice(hashes, 32, i),
                            Secp256k1.EcdsaSignature.fromCompact(slice(signatures, 64, i), 0),
                            slice(publicKeys, publicKeyLength, i)) ? (byte) 1 : 0;
                } catch (CryptoException e) {
                    results[i] = 0;
                }
            }
        }
    }

    /// The packed form of [#verifySchnorr(byte[][], byte[][], byte[][])]:
    /// entry `i` is the message of `messageLength` bytes at
    /// `messageLength * i` of `messages`, the 64-byte signature at `64 * i` of
    /// `signatures` and the 32-byte x-only key at `32 * i` of `publicKeys`.
    /// Writes 1 or 0 to each of the first `count` bytes of `results`. See
    /// [#verifySecp256k1(byte[], byte[], byte[], int, byte[], int)] on
    /// allocating the arrays.
    public static void verifySchnorr(byte[] messages, int messageLength, byte[] signatures, byte[] publicKeys,
            byte[] results, int count) {
        verifyPacked(CryptoEngine.BATCH_SCHNORR, messages, messageLength, signatures, publicKeys, results, count);
    }

    /// The packed form of [#verifyEd25519(byte[][], byte[][], byte[][])], laid
    /// out as [#verifySchnorr(byte[], int, byte[], byte[], byte[], int)].
    public static void verifyEd25519(byte[] messages, int messageLength, byte[] signatures, byte[] publicKeys,
            byte[] results, int count) {
        verifyPacked(CryptoEngine.BATCH_ED25519, messages, messageLength, signatures, publicKeys, results, count);
    }

    /// The SHA-256 of `count` inputs of `inputLength` bytes each, input `i` at
    /// `inputLength * i` of `inputs` and its digest written at `32 * i` of
    /// `digests`. A level of a Merkle tree is `inputLength` 64 over the level
    /// below; `digests` may then be the next level's `inputs`.
    public static void sha256(byte[] inputs, int inputLength, int count, byte[] digests) {
        hashPacked(Hash.SHA256, inputs, inputLength, count, digests);
    }

    /// The Keccak-256 of `count` packed inputs, laid out as
    /// [#sha256(byte[], int, int, byte[])].
    public static void keccak256(byte[] inputs, int inputLength, int count, byte[] digests) {
        hashPacked(Hash.KECCAK256, inputs, inputLength, count, digests);
    }

    private static void verifyPacked(int algorithm, byte[] messages, int messageLength, byte[] signatures,
            byte[] publicKeys, byte[] results, int count) {
        packed(messages, messageLength, signatures, publicKeys, 32, results, count);
        prime(results, count);
        CryptoEngine.get().verifyPacked(algorithm, messages, messageLength, signatures, publicKeys, 32,
                results, count);
        for (int i = 0; i < count; i++) {
            if (results[i] == TODO || results[i] == JAVA) {
                byte[] m = slice(messages, messageLength, i);
                byte[] s = slice(signatures, 64, i);
                byte[] k = slice(publicKeys, 32, i);
                boolean ok = algorithm == CryptoEngine.BATCH_ED25519 ? Ed25519.verify(m, s, k) : schnorr(m, s, k);
                results[i] = ok ? (byte) 1 : 0;
            }
        }
    }

    private static void hashPacked(String algorithm, byte[] inputs, int inputLength, int count, byte[] digests) {
        if (inputs == null || digests == null) {
            throw new CryptoException("the batch arrays must not be null");
        }
        if (count < 0 || inputLength < 0 || (long) inputLength * count > inputs.length
                || 32L * count > digests.length) {
            throw new CryptoException("the batch arrays are too short for count entries");
        }
        if (count == 0 || CryptoEngine.get().hashPacked(algorithm, inputs, inputLength, count, digests)) {
            return;
        }
        boolean sha = Hash.SHA256.equals(algorithm);
        for (int i = 0; i < count; i++) {
            byte[] in = slice(inputs, inputLength, i);
            System.arraycopy(sha ? Hash.sha256(in) : Hash.keccak256(in), 0, digests, 32 * i, 32);
        }
    }

    private static void packed(byte[] messages, int messageLength, byte[] signatures, byte[] publicKeys,
            int keyLength, byte[] results, int count) {
        if (messages == null || signatures == null || publicKeys == null || results == null) {
            throw new CryptoException("the batch arrays must not be null");
        }
        if (count < 0 || messageLength < 0 || (long) messageLength * count > messages.length
                || 64L * count > signatures.length || (long) keyLength * count > publicKeys.length
                || count > results.length) {
            throw new CryptoException("the batch arrays are too short for count entries");
        }
    }

    private static void prime(byte[] results, int count) {
        for (int i = 0; i < count; i++) {
            results[i] = TODO;
        }
    }

    private static byte[] slice(byte[] packed, int length, int i) {
        byte[] out = new byte[length];
        System.arraycopy(packed, length * i, out, 0, length);
        return out;
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
