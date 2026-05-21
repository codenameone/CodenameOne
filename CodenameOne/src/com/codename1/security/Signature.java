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

import com.codename1.io.Util;

/// Digital signature creation and verification. Backed by the platform's
/// native crypto provider -- works with [PublicKey] / [PrivateKey] objects
/// from this package.
///
/// #### Example: sign with RSA-SHA-256 and verify
///
/// ```java
/// KeyPair kp = KeyGenerator.rsa(2048);
/// byte[] sig = Signature.sign(Signature.SHA256_WITH_RSA, kp.getPrivateKey(), data);
/// boolean ok = Signature.verify(Signature.SHA256_WITH_RSA, kp.getPublicKey(), data, sig);
/// ```
public final class Signature {

    /// `SHA256withRSA` -- RSA PKCS#1 v1.5 with SHA-256.
    public static final String SHA256_WITH_RSA = "SHA256withRSA";
    /// `SHA384withRSA` -- RSA PKCS#1 v1.5 with SHA-384.
    public static final String SHA384_WITH_RSA = "SHA384withRSA";
    /// `SHA512withRSA` -- RSA PKCS#1 v1.5 with SHA-512.
    public static final String SHA512_WITH_RSA = "SHA512withRSA";

    /// `SHA256withECDSA` -- ECDSA with SHA-256 (P-256 curve).
    public static final String SHA256_WITH_ECDSA = "SHA256withECDSA";
    /// `SHA384withECDSA` -- ECDSA with SHA-384 (P-384 curve).
    public static final String SHA384_WITH_ECDSA = "SHA384withECDSA";
    /// `SHA512withECDSA` -- ECDSA with SHA-512 (P-521 curve).
    public static final String SHA512_WITH_ECDSA = "SHA512withECDSA";

    private Signature() {}

    /// Signs `data` with the given algorithm and private key.
    public static byte[] sign(String algorithm, PrivateKey key, byte[] data) {
        try {
            return Util.cryptoSign(algorithm, key.getAlgorithm(), key.getEncoded(), data);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }

    /// Verifies `signature` against `data` using the given algorithm and
    /// public key.
    public static boolean verify(String algorithm, PublicKey key, byte[] data, byte[] signature) {
        try {
            return Util.cryptoVerify(algorithm, key.getAlgorithm(), key.getEncoded(), data, signature);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }
}
