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

/// Convenience entry points for symmetric (AES) and asymmetric (RSA)
/// encryption. The actual algorithms run on the platform's native crypto
/// provider -- this class is just a thin, friendly facade over the
/// [com.codename1.impl.CodenameOneImplementation] crypto bridge.
///
/// #### Recommended transformations
///
/// - **AES**: `AES/GCM/NoPadding` for authenticated encryption (uses a 12-byte
///   nonce and produces ciphertext with a 16-byte tag appended). Falls back
///   to `AES/CBC/PKCS5Padding` if GCM is unavailable on a target platform.
/// - **RSA**: `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` for new code,
///   `RSA/ECB/PKCS1Padding` only for interop with old systems.
///
/// #### Example: AES-GCM round-trip
///
/// ```java
/// SecretKey key = KeyGenerator.aes(256);
/// byte[] nonce  = SecureRandom.bytes(12);
/// byte[] cipher = Cipher.aesEncrypt(Cipher.AES_GCM, key, nonce, null, "secret".getBytes("UTF-8"));
/// byte[] plain  = Cipher.aesDecrypt(Cipher.AES_GCM, key, nonce, null, cipher);
/// ```
///
/// #### Example: RSA-OAEP round-trip
///
/// ```java
/// KeyPair kp = KeyGenerator.rsa(2048);
/// byte[] cipher = Cipher.rsaEncrypt(Cipher.RSA_OAEP_SHA256, kp.getPublicKey(), data);
/// byte[] plain  = Cipher.rsaDecrypt(Cipher.RSA_OAEP_SHA256, kp.getPrivateKey(), cipher);
/// ```
public final class Cipher {

    /// `AES/GCM/NoPadding` -- recommended authenticated mode for AES.
    public static final String AES_GCM = "AES/GCM/NoPadding";

    /// `AES/CBC/PKCS5Padding` -- block-chained AES with PKCS#5 padding.
    public static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";

    /// `AES/CBC/NoPadding` -- raw CBC, caller must pre-pad to a 16-byte
    /// boundary.
    public static final String AES_CBC = "AES/CBC/NoPadding";

    /// `AES/ECB/PKCS5Padding` -- legacy interop only. ECB leaks structure;
    /// avoid for new designs.
    public static final String AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding";

    /// `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` -- recommended RSA encryption
    /// transformation.
    public static final String RSA_OAEP_SHA256 = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /// `RSA/ECB/PKCS1Padding` -- legacy RSA padding, kept for interop.
    public static final String RSA_PKCS1 = "RSA/ECB/PKCS1Padding";

    private Cipher() {}

    /// Encrypts with AES.
    ///
    /// #### Parameters
    ///
    /// - `transformation`: one of [#AES_GCM], [#AES_CBC_PKCS5], [#AES_CBC],
    ///   [#AES_ECB_PKCS5]
    ///
    /// - `key`: AES key (16, 24 or 32 bytes for AES-128/192/256)
    ///
    /// - `iv`: initialisation vector for CBC (16 bytes) / nonce for GCM
    ///   (12 bytes recommended). Pass null for ECB.
    ///
    /// - `aad`: associated authenticated data -- GCM only, may be null
    ///
    /// - `plaintext`: data to encrypt
    public static byte[] aesEncrypt(String transformation, SecretKey key,
                                    byte[] iv, byte[] aad, byte[] plaintext) {
        try {
            return Util.aesEncrypt(transformation, key.getEncoded(), iv, aad, plaintext);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }

    /// Decrypts AES ciphertext produced by [#aesEncrypt]. For GCM mode, the
    /// auth tag is part of the ciphertext (last 16 bytes) -- a tag mismatch
    /// raises [CryptoException].
    public static byte[] aesDecrypt(String transformation, SecretKey key,
                                    byte[] iv, byte[] aad, byte[] ciphertext) {
        try {
            return Util.aesDecrypt(transformation, key.getEncoded(), iv, aad, ciphertext);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }

    /// Encrypts a small amount of data with RSA. The plaintext size is bounded
    /// by the modulus minus padding overhead (e.g. ~190 bytes max for
    /// RSA-2048 + OAEP-SHA-256); use AES with an RSA-wrapped AES key for
    /// larger payloads.
    public static byte[] rsaEncrypt(String transformation, PublicKey key, byte[] plaintext) {
        try {
            return Util.rsaEncrypt(transformation, key.getEncoded(), plaintext);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }

    /// Decrypts an RSA ciphertext.
    public static byte[] rsaDecrypt(String transformation, PrivateKey key, byte[] ciphertext) {
        try {
            return Util.rsaDecrypt(transformation, key.getEncoded(), ciphertext);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }
}
