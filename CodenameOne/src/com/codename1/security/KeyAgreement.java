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
        byte[] privateDer = mine.getEncoded();
        byte[] publicDer = theirs.getEncoded();
        // The EC label says nothing about the curve. Some providers would
        // agree over an imported P-384 key and others refuse it, so a key that
        // does not name P-256 is refused here, the same on every port.
        if (!namesP256(privateDer) || !namesP256(publicDer)) {
            throw new CryptoException("ECDH supports P-256 keys only");
        }
        try {
            return Util.ecdhAgree(privateDer, publicDer);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }

    /// The DER encoding of the P-256 curve's object identifier,
    /// 1.2.840.10045.3.1.7, which a PKCS#8 or X.509 EC key carries in its
    /// algorithm parameters.
    private static final byte[] P256_OID = {
        0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01, 0x07
    };

    private static boolean namesP256(byte[] der) {
        if (der == null) {
            return false;
        }
        int last = der.length - P256_OID.length;
        for (int i = 0; i <= last; i++) {
            int j = 0;
            while (j < P256_OID.length && der[i + j] == P256_OID[j]) {
                j++;
            }
            if (j == P256_OID.length) {
                return true;
            }
        }
        return false;
    }
}
