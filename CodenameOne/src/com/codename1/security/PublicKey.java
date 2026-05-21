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

/// A public key -- paired with a [PrivateKey] to form a key pair. Carries the
/// algorithm name ("RSA" or "EC") and the encoded key bytes.
///
/// For interop with PEM files (`-----BEGIN PUBLIC KEY-----`) feed the
/// X.509-SubjectPublicKeyInfo (SPKI) DER bytes to [#fromX509]; for raw RSA
/// modulus/exponent use [#rsa].
public final class PublicKey {
    /// RSA algorithm identifier ("RSA").
    public static final String RSA = "RSA";
    /// Elliptic-curve algorithm identifier ("EC").
    public static final String EC = "EC";

    private final String algorithm;
    private final byte[] encoded;
    private final String format; // "X.509" or a vendor format identifier

    PublicKey(String algorithm, byte[] encoded, String format) {
        if (algorithm == null) {
            throw new CryptoException("algorithm must not be null");
        }
        if (encoded == null) {
            throw new CryptoException("encoded must not be null");
        }
        this.algorithm = algorithm;
        this.encoded = new byte[encoded.length];
        System.arraycopy(encoded, 0, this.encoded, 0, encoded.length);
        this.format = format == null ? "X.509" : format;
    }

    /// Wraps an X.509 / SubjectPublicKeyInfo (SPKI) DER blob. This is the
    /// format produced by `openssl rsa -pubout` or `openssl ec -pubout`.
    public static PublicKey fromX509(String algorithm, byte[] x509Der) {
        return new PublicKey(algorithm, x509Der, "X.509");
    }

    /// Convenience: build an RSA [PublicKey] from a [#fromX509] X.509 blob.
    public static PublicKey rsa(byte[] x509Der) {
        return fromX509(RSA, x509Der);
    }

    /// Returns a fresh copy of the encoded key bytes.
    public byte[] getEncoded() {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, encoded.length);
        return copy;
    }

    /// Returns the algorithm this key is for (e.g. "RSA").
    public String getAlgorithm() {
        return algorithm;
    }

    /// Returns the encoding format ("X.509" for SPKI).
    public String getFormat() {
        return format;
    }
}
