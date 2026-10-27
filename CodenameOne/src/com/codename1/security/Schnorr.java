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
