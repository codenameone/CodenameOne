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

import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;

/// Binds [EthKeys] to the framework's primitives.
final class CoreCrypto {
    private CoreCrypto() {
    }

    static byte[] keccak256(byte[] data) {
        return Hash.keccak256(data);
    }

    /// The 64 bytes of x and y, without the 0x04 prefix an address hashes over.
    static byte[] publicKey64(byte[] key) {
        return strip(Secp256k1.publicKey(key, false));
    }

    static byte[] publicKey33(byte[] key) {
        return Secp256k1.publicKey(key, true);
    }

    /// r, s (low-S) and the recovery id, 65 bytes.
    static byte[] signRecoverable(byte[] key, byte[] hash) {
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash, key).normalize();
        byte[] out = new byte[65];
        System.arraycopy(sig.toCompact(), 0, out, 0, 64);
        out[64] = (byte) sig.recoveryId();
        return out;
    }

    /// The signer's 64-byte key, or null when no key fits the signature.
    static byte[] recover64(byte[] hash, byte[] sig) {
        byte[] compact = new byte[64];
        System.arraycopy(sig, 0, compact, 0, 64);
        byte[] pub = Secp256k1.recoverPublicKey(hash,
                Secp256k1.EcdsaSignature.fromCompact(compact, sig[64] & 0xff), false);
        return pub == null ? null : strip(pub);
    }

    static byte[] tweakAdd(byte[] key, byte[] tweak) {
        return Secp256k1.privateKeyTweakAdd(key, tweak);
    }

    static boolean validPrivate(byte[] key) {
        return Secp256k1.isValidPrivateKey(key);
    }

    private static byte[] strip(byte[] uncompressed) {
        byte[] out = new byte[64];
        System.arraycopy(uncompressed, 1, out, 0, 64);
        return out;
    }
}
