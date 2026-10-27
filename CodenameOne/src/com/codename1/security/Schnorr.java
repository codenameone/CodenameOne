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

/// BIP-340 Schnorr signatures over secp256k1: the signatures of Bitcoin's
/// Taproot (key-path spends, P2TR) and of Nostr events.
///
/// Keys are the 32-byte x coordinate alone ("x-only"); the point with the
/// even y is meant. A private key is the same 32 bytes as for [Secp256k1].
///
/// ```java
/// byte[] privateKey = SecureRandom.bytes(32);
/// byte[] messageHash = Hash.sha256("hello".getBytes());
/// byte[] pub = Schnorr.publicKey(privateKey);
/// byte[] sig = Schnorr.sign(messageHash, privateKey, SecureRandom.bytes(32));
/// boolean ok = Schnorr.verify(messageHash, sig, pub);
/// ```
///
/// Signing is deterministic given the auxiliary randomness; passing fresh
/// random bytes there, as BIP-340 recommends, protects against fault and
/// side-channel attacks that a purely deterministic nonce does not.
///
/// [#sign(byte[], byte[], byte[])] derives the public key on every call,
/// which BIP-340 signing needs, and that is half the work of a signature. A
/// [KeyPair] derives it once:
///
/// ```java
/// byte[] privateKey = SecureRandom.bytes(32);
/// Schnorr.KeyPair keys = new Schnorr.KeyPair(privateKey);
/// byte[] first = keys.sign(Hash.sha256("first".getBytes()), SecureRandom.bytes(32));
/// byte[] second = keys.sign(Hash.sha256("second".getBytes()), SecureRandom.bytes(32));
/// keys.destroy();
/// ```
@com.codename1.impl.SharedWithBackend
public final class Schnorr {
    /// Length of an x-only public key.
    public static final int PUBLIC_KEY_LENGTH = 32;

    /// Length of a signature.
    public static final int SIGNATURE_LENGTH = 64;

    private Schnorr() {
    }

    /// The x-only public key of a private key.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the private key is not valid
    public static byte[] publicKey(byte[] privateKey) {
        return Secp256k1.xOnlyPublicKey(privateKey);
    }

    /// Signs a message, normally a 32-byte hash.
    ///
    /// #### Parameters
    ///
    /// - `message`: the message; BIP-340 signs it as given
    ///
    /// - `privateKey`: 32 bytes in `[1, n-1]`
    ///
    /// - `auxRand`: 32 random bytes, or `null` for the all-zero value the
    ///   standard's test vectors use
    ///
    /// #### Returns
    ///
    /// the 64-byte signature
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the key or the randomness is malformed
    public static byte[] sign(byte[] message, byte[] privateKey, byte[] auxRand) {
        return Secp256k1.schnorrSign(message, privateKey, auxRand);
    }

    /// A private key with its x-only public key, derived once, for signing
    /// many messages; the counterpart of libsecp256k1's `secp256k1_keypair`.
    /// The two cannot be supplied separately: signing with a public key that
    /// is not the private key's own reveals the private key.
    ///
    /// The object holds the private key until [#destroy()] wipes it. Use it
    /// from one thread at a time: a `destroy` racing a `sign` signs with a
    /// half-wiped key. Where a native engine signs, it derives the public key
    /// itself and the saving is the engine's to make.
    public static final class KeyPair {
        private final byte[] privateKey;
        private final byte[] publicKey = new byte[PUBLIC_KEY_LENGTH];
        private final int[] scalar;
        private boolean destroyed;

        /// Derives the public key of `privateKey`, which is copied.
        ///
        /// #### Throws
        ///
        /// - `CryptoException`: if the private key is not valid
        public KeyPair(byte[] privateKey) {
            scalar = Secp256k1.schnorrScalar(privateKey, publicKey);
            this.privateKey = new byte[privateKey.length];
            System.arraycopy(privateKey, 0, this.privateKey, 0, privateKey.length);
        }

        /// The x-only public key, as [Schnorr#publicKey(byte[])] returns it.
        public byte[] getPublicKey() {
            byte[] out = new byte[PUBLIC_KEY_LENGTH];
            System.arraycopy(publicKey, 0, out, 0, PUBLIC_KEY_LENGTH);
            return out;
        }

        /// Signs a message, giving the signature
        /// [Schnorr#sign(byte[], byte[], byte[])] gives.
        ///
        /// #### Throws
        ///
        /// - `CryptoException`: if the randomness is malformed, or the key
        ///   has been destroyed
        public byte[] sign(byte[] message, byte[] auxRand) {
            if (destroyed) {
                throw new CryptoException("the key pair has been destroyed");
            }
            if (message != null && (auxRand == null || auxRand.length == 32)) {
                byte[] fast = CryptoEngine.get().schnorrSign(message, privateKey, auxRand);
                if (fast != null) {
                    return fast;
                }
            }
            return Secp256k1.schnorrSignPrepared(message, scalar, publicKey, auxRand);
        }

        /// Wipes the private key; later calls to [#sign(byte[], byte[])] throw.
        public void destroy() {
            destroyed = true;
            for (int i = 0; i < privateKey.length; i++) {
                privateKey[i] = 0;
            }
            for (int i = 0; i < scalar.length; i++) {
                scalar[i] = 0;
            }
        }
    }

    /// Verifies a signature.
    ///
    /// #### Returns
    ///
    /// `true` if `signature` was made over `message` by the key behind
    /// `publicKey`; `false` for any malformed or wrong signature
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the message is null or the public key is not
    ///   32 bytes
    public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
        return Secp256k1.schnorrVerify(message, signature, publicKey);
    }
}
