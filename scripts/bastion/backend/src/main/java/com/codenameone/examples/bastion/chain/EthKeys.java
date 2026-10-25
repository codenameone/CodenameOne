/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

/// The two primitives every chain class here needs, behind one door:
/// Keccak-256 and recoverable secp256k1 signatures. They come from the
/// framework's `com.codename1.security` (constant-time where a secret is
/// involved, with published test vectors in its unit tests); nothing in this
/// package does curve arithmetic of its own.
public final class EthKeys {
    private EthKeys() {
    }

    public static byte[] keccak256(byte[] data) {
        return CoreCrypto.keccak256(data);
    }

    /// The 64-byte public key (x || y, no 0x04 prefix) of `privateKey`.
    public static byte[] publicKey(byte[] privateKey) {
        return CoreCrypto.publicKey64(privateKey);
    }

    /// r (32) || s (32, low-S) || recovery id (one byte, 0 or 1).
    public static byte[] sign(byte[] privateKey, byte[] hash32) {
        return CoreCrypto.signRecoverable(privateKey, hash32);
    }

    /// The 64-byte public key that made `signature` over `hash32`.
    public static byte[] recover(byte[] hash32, byte[] signature) {
        return CoreCrypto.recover64(hash32, signature);
    }

    /// `(key + tweak) mod n`, or null when the result is not a valid key
    /// (BIP-32 then skips to the next index).
    public static byte[] addPrivate(byte[] key, byte[] tweak) {
        return CoreCrypto.tweakAdd(key, tweak);
    }

    /// The 33-byte compressed public key of `privateKey`.
    public static byte[] compressedPublicKey(byte[] privateKey) {
        return CoreCrypto.publicKey33(privateKey);
    }

    /// True when `key` is a valid private key: 32 bytes, 0 < key < n.
    public static boolean validPrivate(byte[] key) {
        return CoreCrypto.validPrivate(key);
    }
}
