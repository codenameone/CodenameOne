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

/// A private key -- paired with a [PublicKey] to form a key pair. Carries the
/// algorithm name ("RSA" or "EC") and the encoded key bytes.
///
/// For interop with PEM files (`-----BEGIN PRIVATE KEY-----`) feed the
/// PKCS#8 DER bytes to [#fromPkcs8].
public final class PrivateKey {
    private final String algorithm;
    private final byte[] encoded;
    private final String format;

    PrivateKey(String algorithm, byte[] encoded, String format) {
        if (algorithm == null) {
            throw new CryptoException("algorithm must not be null");
        }
        if (encoded == null) {
            throw new CryptoException("encoded must not be null");
        }
        this.algorithm = algorithm;
        this.encoded = new byte[encoded.length];
        System.arraycopy(encoded, 0, this.encoded, 0, encoded.length);
        this.format = format == null ? "PKCS#8" : format;
    }

    /// Wraps a PKCS#8 DER blob. This is the format produced by `openssl
    /// pkcs8 -topk8 -nocrypt`.
    public static PrivateKey fromPkcs8(String algorithm, byte[] pkcs8Der) {
        return new PrivateKey(algorithm, pkcs8Der, "PKCS#8");
    }

    /// Convenience: build an RSA [PrivateKey] from a [#fromPkcs8] PKCS#8 blob.
    public static PrivateKey rsa(byte[] pkcs8Der) {
        return fromPkcs8(PublicKey.RSA, pkcs8Der);
    }

    /// Returns a fresh copy of the encoded key bytes. Treat as sensitive
    /// material -- do not log or store unencrypted.
    public byte[] getEncoded() {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, encoded.length);
        return copy;
    }

    /// Returns the algorithm this key is for (e.g. "RSA").
    public String getAlgorithm() {
        return algorithm;
    }

    /// Returns the encoding format ("PKCS#8").
    public String getFormat() {
        return format;
    }
}
