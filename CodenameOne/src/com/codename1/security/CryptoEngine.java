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

/// The seam through which a platform supplies faster implementations of the
/// compute-heavy cryptography in this package. Every method answers `null`
/// (or `false` where noted) when the engine has no faster path for that
/// input, and the caller then runs the portable Java implementation, which
/// produces identical bytes. A missing kernel therefore degrades to slower,
/// never to different.
///
/// Client ports install one when Codename One initializes; the backend
/// installs its own. Application code has no reason to call this class: use
/// [Pbkdf2], [Secp256k1] and [Hash], which consult the installed engine.
///
/// An engine must be thread safe and must not keep or log the key material
/// it is handed.
@com.codename1.impl.SharedWithBackend
public class CryptoEngine {
    private static final CryptoEngine PORTABLE = new CryptoEngine();
    private static CryptoEngine installed = PORTABLE;

    /// Creates an engine that accelerates nothing; subclasses override what
    /// they accelerate.
    protected CryptoEngine() {
    }

    /// The installed engine, never null.
    public static CryptoEngine get() {
        return installed;
    }

    /// Installs an engine, or restores the portable one when given `null`.
    /// Called by a port during initialization.
    public static void install(CryptoEngine engine) {
        installed = engine == null ? PORTABLE : engine;
    }

    /// Fills `out` with bytes from the platform's CSPRNG, answering `false` when this engine
    /// has no source. Every installed engine has one: the client's draws on the port, the
    /// backend's on the server runtime.
    public boolean randomBytes(byte[] out) {
        return false;
    }

    /// Fresh secure random bytes from the installed engine. Shared code, which cannot reach
    /// the client's `SecureRandom`, draws its nonces and entropy here.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: when no engine with a random source is installed
    public static byte[] random(int length) {
        byte[] out = new byte[length];
        if (!installed.randomBytes(out)) {
            throw new CryptoException("no secure random source is installed");
        }
        return out;
    }

    /// PBKDF2 (RFC 8018) with HMAC over `hashAlgorithm` ([Hash#SHA256] or
    /// [Hash#SHA512]), or `null` to use the Java derivation. The arguments
    /// are already validated.
    public byte[] pbkdf2(String hashAlgorithm, byte[] password, byte[] salt, int iterations, int length) {
        return null;
    }

    /// The Ed25519 public key of a 32-byte seed, or `null` to derive it in
    /// Java.
    public byte[] ed25519PublicKey(byte[] privateKey) {
        return null;
    }

    /// An Ed25519 signature ([Ed25519#sign(byte[], byte[], byte[])]), or
    /// `null` to sign in Java. The arguments are already validated.
    public byte[] ed25519Sign(byte[] privateKey, byte[] publicKey, byte[] message) {
        return null;
    }

    /// An Ed25519 verification: 1 for a valid signature, 0 for an invalid
    /// one, or -1 to verify in Java.
    public int ed25519Verify(byte[] message, byte[] signature, byte[] publicKey) {
        return -1;
    }

    /// A secp256k1 public key ([Secp256k1#publicKey]), or `null` to derive it
    /// in Java. The private key has not been range checked: answer `null`
    /// for one that is out of range and Java reports the error.
    public byte[] secpPublicKey(byte[] privateKey, boolean compressed) {
        return null;
    }

    /// An ECDSA signature over a 32-byte hash as `r || s || recoveryId`
    /// (65 bytes, RFC 6979 nonce, low S), or `null` to sign in Java.
    public byte[] secpSign(byte[] hash, byte[] privateKey) {
        return null;
    }

    /// An ECDSA verification of `r || s` (64 bytes): 1 valid, 0 invalid, or
    /// -1 to verify in Java.
    public int secpVerify(byte[] hash, byte[] compactSignature, byte[] publicKey) {
        return -1;
    }

    /// The public key recovered from `r || s` and a recovery id, or `null`
    /// to recover in Java (which also decides that no key exists).
    public byte[] secpRecover(byte[] hash, byte[] compactSignature, int recoveryId, boolean compressed) {
        return null;
    }

    /// `(privateKey + tweak) mod n`, or `null` to compute it in Java.
    public byte[] secpPrivateKeyTweakAdd(byte[] privateKey, byte[] tweak) {
        return null;
    }

    /// `publicKey + tweak * G`, or `null` to compute it in Java.
    public byte[] secpPublicKeyTweakAdd(byte[] publicKey, byte[] tweak, boolean compressed) {
        return null;
    }

    /// A BIP-340 x-only public key, or `null` to derive it in Java.
    public byte[] schnorrPublicKey(byte[] privateKey) {
        return null;
    }

    /// A BIP-340 signature; `auxRand` is 32 bytes or `null` for zeros.
    /// Answers `null` to sign in Java.
    public byte[] schnorrSign(byte[] message, byte[] privateKey, byte[] auxRand) {
        return null;
    }

    /// A BIP-340 verification: 1 valid, 0 invalid, or -1 to verify in Java.
    /// The key is 32 bytes and the signature 64.
    public int schnorrVerify(byte[] message, byte[] signature, byte[] publicKey) {
        return -1;
    }

    /// Keccak-256 of `data`, or `null` to hash in Java.
    public byte[] keccak256(byte[] data) {
        return null;
    }

    /// [#verifyBatch] of ECDSA over secp256k1.
    public static final int BATCH_SECP256K1 = 1;

    /// [#verifyBatch] of BIP-340 Schnorr signatures.
    public static final int BATCH_SCHNORR = 2;

    /// [#verifyBatch] of Ed25519 signatures.
    public static final int BATCH_ED25519 = 3;

    /// Verifies a batch of signatures of one algorithm ([#BATCH_SECP256K1],
    /// [#BATCH_SCHNORR], [#BATCH_ED25519]). Only the entries whose result
    /// byte is 2 are to be verified: the engine writes 1 (valid), 0
    /// (invalid) or `0xff` (verify this one in Java) over them, and leaves
    /// any it does not reach at 2, which the caller also verifies in Java.
    /// Those entries have non-null arrays; signatures are 64 bytes, messages
    /// 32 for ECDSA, keys 32 except for ECDSA's SEC1 keys.
    public void verifyBatch(int algorithm, byte[][] messages, byte[][] signatures, byte[][] publicKeys,
            byte[] results) {
    }

    /// [#verifyBatch] over packed arrays: entry `i` is `messageLength` bytes
    /// of `messages` from `messageLength * i`, 64 bytes of `signatures` from
    /// `64 * i` and `keyLength` bytes of `keys` from `keyLength * i`. The same
    /// result bytes as [#verifyBatch], and every range is already validated.
    /// An engine that can should read the arrays in place.
    public void verifyPacked(int algorithm, byte[] messages, int messageLength, byte[] signatures, byte[] keys,
            int keyLength, byte[] results, int count) {
    }

    /// [#hashBatch] over packed arrays: digest `i` is of `inputLength` bytes of
    /// `inputs` from `inputLength * i`, written at `32 * i` of `digests`.
    /// Answers false to hash in Java. The ranges are already validated.
    public boolean hashPacked(String algorithm, byte[] inputs, int inputLength, int count, byte[] digests) {
        return false;
    }

    /// The digests of `inputs` under [Hash#SHA256] or [Hash#KECCAK256], a
    /// `null` for each `null` input, or `null` to hash them in Java.
    public byte[][] hashBatch(String algorithm, byte[][] inputs) {
        return null;
    }

    /// SHA-256 of `length` bytes of `data` from `offset`, or `null` to hash
    /// in Java. The range is already validated.
    public byte[] sha256(byte[] data, int offset, int length) {
        return null;
    }

    /// SHA-512 of a range of `data`, or `null` to hash in Java.
    public byte[] sha512(byte[] data, int offset, int length) {
        return null;
    }

    /// RIPEMD-160 of a range of `data`, or `null` to hash in Java.
    public byte[] ripemd160(byte[] data, int offset, int length) {
        return null;
    }

    /// HMAC over `hashAlgorithm` ([Hash#SHA256] or [Hash#SHA512]), or `null`
    /// to compute it in Java. Neither array is null.
    public byte[] hmac(String hashAlgorithm, byte[] key, byte[] data) {
        return null;
    }
}
