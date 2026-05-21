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

/// Common base for every key type in the security package -- [SecretKey] for
/// symmetric algorithms, [PublicKey] and [PrivateKey] for asymmetric ones.
///
/// Holds the three pieces of metadata every key carries:
///
/// - an algorithm name (e.g. "AES", "RSA", "EC")
/// - a defensive copy of the encoded key bytes
/// - a format identifier (e.g. "RAW" for symmetric keys, "X.509" for SPKI
///   public keys, "PKCS#8" for private keys)
///
/// Application code should not extend this class directly; create the
/// concrete subtype that matches the algorithm you are using.
public abstract class Key {
    private final String algorithm;
    private final byte[] encoded;
    private final String format;

    /// #### Parameters
    ///
    /// - `algorithm`: human-readable algorithm name (e.g. "AES")
    ///
    /// - `encoded`: raw key material -- defensively copied
    ///
    /// - `format`: encoding format identifier ("RAW", "X.509", "PKCS#8")
    protected Key(String algorithm, byte[] encoded, String format) {
        if (algorithm == null) {
            throw new CryptoException("algorithm must not be null");
        }
        if (encoded == null) {
            throw new CryptoException("encoded must not be null");
        }
        this.algorithm = algorithm;
        this.encoded = new byte[encoded.length];
        System.arraycopy(encoded, 0, this.encoded, 0, encoded.length);
        this.format = format;
    }

    /// Returns a fresh copy of the encoded key bytes. Treat returns from
    /// private keys as sensitive material -- do not log or store
    /// unencrypted.
    public final byte[] getEncoded() {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, encoded.length);
        return copy;
    }

    /// Returns the algorithm this key is intended for (e.g. "AES", "RSA").
    public final String getAlgorithm() {
        return algorithm;
    }

    /// Returns the encoding format identifier. Standard values:
    ///
    /// - "RAW" -- symmetric keys ([SecretKey])
    /// - "X.509" -- SubjectPublicKeyInfo DER ([PublicKey])
    /// - "PKCS#8" -- PrivateKeyInfo DER ([PrivateKey])
    public final String getFormat() {
        return format;
    }

    /// Subclasses can expose the raw byte array internally without going
    /// through `getEncoded()` so they don't pay the defensive-copy cost on
    /// every crypto-bridge call. Not exposed publicly; package-private only.
    final byte[] rawEncoded() {
        return encoded;
    }
}
