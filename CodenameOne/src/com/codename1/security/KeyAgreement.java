/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.io.Util;

/// Elliptic-curve Diffie-Hellman key agreement over P-256.
///
/// Two parties that each hold a key pair from [KeyGenerator#ec(int)] exchange
/// public keys; each combines its own private key with the other's public key
/// and both arrive at the same secret, which no one watching the exchange can
/// compute. That secret is not a key yet -- run it through [Hkdf] first.
///
/// ```java
/// KeyPair alice = KeyGenerator.ec(256);
/// KeyPair bob = KeyGenerator.ec(256);
/// byte[] a = KeyAgreement.ecdh(alice.getPrivateKey(), bob.getPublicKey());
/// byte[] b = KeyAgreement.ecdh(bob.getPrivateKey(), alice.getPublicKey());
/// // a and b hold the same 32 bytes
/// ```
///
/// The computation runs in the platform's native crypto provider.
public final class KeyAgreement {
    private KeyAgreement() {}

    /// Computes the ECDH shared secret: the 32-byte x coordinate of the
    /// product of `mine` and `theirs`, as SEC 1 and the JCA define it.
    ///
    /// #### Parameters
    ///
    /// - `mine`: this side's EC private key
    ///
    /// - `theirs`: the other side's EC public key
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if either key is not a P-256 key, or the public key
    ///   is not a point on the curve
    public static byte[] ecdh(PrivateKey mine, PublicKey theirs) {
        if (mine == null || theirs == null) {
            throw new CryptoException("ECDH keys must not be null");
        }
        if (!PublicKey.EC.equals(mine.getAlgorithm()) || !PublicKey.EC.equals(theirs.getAlgorithm())) {
            throw new CryptoException("ECDH needs EC keys, got " + mine.getAlgorithm()
                    + " and " + theirs.getAlgorithm());
        }
        try {
            return Util.ecdhAgree(mine.getEncoded(), theirs.getEncoded());
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }
}
