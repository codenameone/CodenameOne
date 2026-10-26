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

/// Ed25519 signatures (RFC 8032): the signatures of Solana, Cardano,
/// Stellar, Tezos, SSH keys and many protocols beside.
///
/// A private key is a 32-byte seed; the public key is 32 bytes and a
/// signature 64. Signing is deterministic and constant time; verification is
/// the cofactorless RFC 8032 check, rejecting a non-canonical `S` and a
/// public key or `R` that does not decode, as libsodium does.
///
/// ```java
/// byte[] seed = SecureRandom.bytes(32);
/// byte[] pub = Ed25519.publicKey(seed);
/// byte[] sig = Ed25519.sign(seed, pub, message);
/// boolean ok = Ed25519.verify(message, sig, pub);
/// ```
@com.codename1.impl.SharedWithBackend
public final class Ed25519 {
    /// Length of a private key (the seed).
    public static final int PRIVATE_KEY_LENGTH = 32;

    /// Length of a public key.
    public static final int PUBLIC_KEY_LENGTH = 32;

    /// Length of a signature.
    public static final int SIGNATURE_LENGTH = 64;

    private Ed25519() {
    }

    /// Whether 32 bytes are the encoding of a point on the curve. Solana's program-derived
    /// addresses are exactly the hashes for which this answers `false`.
    public static boolean isOnCurve(byte[] point) {
        if (point == null || point.length != PUBLIC_KEY_LENGTH) {
            return false;
        }
        return Ed25519Math.decode(new Ed25519Math.P3(), point, 0);
    }

    /// The public key of a 32-byte seed.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the seed is not 32 bytes
    public static byte[] publicKey(byte[] privateKey) {
        byte[] fast = privateKey != null && privateKey.length == 32
                ? CryptoEngine.get().ed25519PublicKey(privateKey) : null;
        if (fast != null) {
            return fast;
        }
        byte[] h = expand(privateKey);
        byte[] a = new byte[32];
        System.arraycopy(h, 0, a, 0, 32);
        byte[] out = new byte[32];
        Ed25519Math.P3 p = new Ed25519Math.P3();
        Ed25519Math.mulBase(p, a);
        Ed25519Math.encode(out, 0, p);
        wipe(a);
        wipe(h);
        return out;
    }

    /// Signs a message with a seed, deriving the public key first. When the
    /// public key is at hand, [#sign(byte[], byte[], byte[])] saves that
    /// work, which is most of the cost of a signature.
    public static byte[] sign(byte[] privateKey, byte[] message) {
        return sign(privateKey, publicKey(privateKey), message);
    }

    /// Signs a message.
    ///
    /// #### Parameters
    ///
    /// - `privateKey`: the 32-byte seed
    ///
    /// - `publicKey`: the seed's public key, from [#publicKey]; a wrong one
    ///   produces a signature nobody can verify
    ///
    /// - `message`: the message, of any length
    ///
    /// #### Returns
    ///
    /// the 64-byte signature
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if an argument is null or the wrong length
    public static byte[] sign(byte[] privateKey, byte[] publicKey, byte[] message) {
        if (message == null) {
            throw new CryptoException("message must not be null");
        }
        if (publicKey == null || publicKey.length != PUBLIC_KEY_LENGTH) {
            throw new CryptoException("public key must be 32 bytes");
        }
        byte[] fast = CryptoEngine.get().ed25519Sign(privateKey, publicKey, message);
        if (fast != null) {
            return fast;
        }
        byte[] h = expand(privateKey);
        byte[] a = new byte[32];
        System.arraycopy(h, 0, a, 0, 32);
        Hash d = Hash.create(Hash.SHA512);
        d.update(h, 32, 32);
        d.update(message);
        byte[] r = Ed25519Math.reduce64(d.digest());
        byte[] sig = new byte[64];
        Ed25519Math.P3 p = new Ed25519Math.P3();
        Ed25519Math.mulBase(p, r);
        Ed25519Math.encode(sig, 0, p);
        d.update(sig, 0, 32);
        d.update(publicKey);
        d.update(message);
        byte[] k = Ed25519Math.reduce64(d.digest());
        byte[] s = Ed25519Math.mulAdd(k, a, r);
        System.arraycopy(s, 0, sig, 32, 32);
        wipe(h);
        wipe(a);
        wipe(r);
        wipe(s);
        return sig;
    }

    /// Verifies a signature.
    ///
    /// #### Returns
    ///
    /// `true` if `signature` was made over `message` by the key behind
    /// `publicKey`; `false` for any malformed or wrong signature or key
    public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
        if (message == null || signature == null || publicKey == null
                || signature.length != SIGNATURE_LENGTH || publicKey.length != PUBLIC_KEY_LENGTH) {
            return false;
        }
        int fast = CryptoEngine.get().ed25519Verify(message, signature, publicKey);
        if (fast >= 0) {
            return fast == 1;
        }
        if (!Ed25519Math.isCanonicalScalar(signature, 32)) {
            return false;
        }
        Ed25519Math.P3 a = new Ed25519Math.P3();
        if (!Ed25519Math.decode(a, publicKey, 0)) {
            return false;
        }
        Hash d = Hash.create(Hash.SHA512);
        d.update(signature, 0, 32);
        d.update(publicKey);
        d.update(message);
        byte[] k = Ed25519Math.reduce64(d.digest());
        byte[] s = new byte[32];
        System.arraycopy(signature, 32, s, 0, 32);
        Ed25519Math.P3 r = new Ed25519Math.P3();
        Ed25519Math.doubleMulVar(r, k, a, s);
        byte[] enc = new byte[32];
        Ed25519Math.encode(enc, 0, r);
        int diff = 0;
        for (int i = 0; i < 32; i++) {
            diff |= enc[i] ^ signature[i];
        }
        return diff == 0;
    }

    // SHA-512 of the seed, the scalar half clamped
    private static byte[] expand(byte[] privateKey) {
        if (privateKey == null || privateKey.length != PRIVATE_KEY_LENGTH) {
            throw new CryptoException("private key must be 32 bytes");
        }
        byte[] h = Hash.sha512(privateKey);
        h[0] &= (byte) 248;
        h[31] &= 127;
        h[31] |= 64;
        return h;
    }

    private static void wipe(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }
}
