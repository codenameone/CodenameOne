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

import com.codename1.util.StringUtil;

/// A public key -- paired with a [PrivateKey] to form a key pair. Carries the
/// algorithm name ("RSA" or "EC") and the encoded key bytes.
///
/// PEM files (`-----BEGIN PUBLIC KEY-----`) go through [#fromPem], which
/// strips the armor and decodes the base64 for you; [#fromX509] is the lower
/// level entry point for callers that already hold the DER bytes.
public final class PublicKey extends Key {
    /// RSA algorithm identifier ("RSA").
    public static final String RSA = "RSA";
    /// Elliptic-curve algorithm identifier ("EC").
    public static final String EC = "EC";

    PublicKey(String algorithm, byte[] encoded, String format) {
        super(algorithm, encoded, format == null ? "X.509" : format);
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

    /// Parses a PEM-encoded public key, determining the algorithm from the key
    /// itself. This is the form `openssl rsa -pubout` and every backend key
    /// store hands out:
    ///
    /// ```java
    /// InputStream is = Display.getInstance().getResourceAsStream(MyApp.class, "/public.pem");
    /// PublicKey key = PublicKey.fromPem(Util.readInputStream(is));
    /// ```
    ///
    /// Accepts a `PUBLIC KEY` (SPKI) block, the older PKCS#1 `RSA PUBLIC KEY`
    /// block, and -- for keys carried in JSON or a build hint rather than a
    /// file -- bare base64 with no `-----BEGIN-----` armor at all. Line
    /// endings, blank lines and text surrounding the block are ignored.
    ///
    /// Throws [CryptoException] if the text is not a public key, if it is
    /// passphrase-encrypted, or if the key is neither RSA nor EC.
    public static PublicKey fromPem(String pem) {
        byte[] der = Pem.toSpki(pem);
        return fromX509(Pem.algorithm(der), der);
    }

    /// [#fromPem] over the raw bytes of a `.pem` file, so a stream read with
    /// `Util.readInputStream` can be passed straight in. The bytes are decoded
    /// as UTF-8.
    public static PublicKey fromPem(byte[] pem) {
        if (pem == null) {
            throw new CryptoException("pem must not be null");
        }
        return fromPem(StringUtil.newString(pem));
    }

    /// [#fromPem] with the algorithm supplied by the caller rather than read
    /// from the key. Use this only for a key whose algorithm OID this class
    /// does not recognize but the platform does.
    public static PublicKey fromPem(String algorithm, String pem) {
        return fromX509(algorithm, Pem.toSpki(pem));
    }

    /// [#fromPem(String,String)] over the raw bytes of a `.pem` file.
    public static PublicKey fromPem(String algorithm, byte[] pem) {
        if (pem == null) {
            throw new CryptoException("pem must not be null");
        }
        return fromPem(algorithm, StringUtil.newString(pem));
    }
}
