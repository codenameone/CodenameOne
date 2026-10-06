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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import java.io.IOException;
import java.util.Map;

/// A credential's public key as an authenticator sends it -- a COSE_Key, RFC
/// 9052 -- turned into what [Crypto#verify] takes: an algorithm name and a
/// SubjectPublicKeyInfo.
///
/// Two kinds of key are read, the two every platform authenticator offers:
///
/// | COSE algorithm | Key | Verified as |
/// |---|---|---|
/// | -7, ES256 | EC2 on P-256, uncompressed | [Crypto#ES256] |
/// | -257, RS256 | RSA of 2048 bits or more | [Crypto#RS256] |
///
/// Any other algorithm is refused with
/// [WebAuthnException#UNSUPPORTED_ALGORITHM] -- EdDSA (-8) among them, which
/// the server's cryptography does not verify -- and a key that names one of
/// the two and is not such a key with [WebAuthnException#INVALID_KEY]. The
/// registration options offer only these two, so a conforming authenticator
/// answers with nothing else.
public final class CoseKey {
    /// The COSE identifier of ES256: ECDSA on P-256 with SHA-256.
    public static final long ES256 = -7;
    /// The COSE identifier of RS256: RSASSA-PKCS1-v1_5 with SHA-256.
    public static final long RS256 = -257;

    private static final Long KTY = Long.valueOf(1);
    private static final Long ALG = Long.valueOf(3);
    private static final Long P1 = Long.valueOf(-1);
    private static final Long P2 = Long.valueOf(-2);
    private static final Long P3 = Long.valueOf(-3);

    private final long algorithm;
    private final byte[] publicKey;

    private CoseKey(long algorithm, byte[] publicKey) {
        this.algorithm = algorithm;
        this.publicKey = publicKey;
    }

    /// The key a CBOR map decodes to.
    ///
    /// - [WebAuthnException]: when it is not a key of a supported algorithm
    static CoseKey of(Object decoded) {
        if (!(decoded instanceof Map)) {
            throw invalid("The credential's public key is not a COSE key");
        }
        Map key = (Map) decoded;
        Object kty = key.get(KTY);
        Object alg = key.get(ALG);
        if (!(kty instanceof Long) || !(alg instanceof Long)) {
            throw invalid("The credential's public key names no key type or no algorithm");
        }
        long algorithm = ((Long) alg).longValue();
        long type = ((Long) kty).longValue();
        try {
            if (algorithm == ES256) {
                Object curve = key.get(P1);
                Object x = key.get(P2);
                Object y = key.get(P3);
                // Key type 2 is EC2 and curve 1 is P-256; a y that is a truth
                // value is the compressed form, which is not read.
                if (type != 2 || !(curve instanceof Long) || ((Long) curve).longValue() != 1
                        || !(x instanceof byte[]) || !(y instanceof byte[])) {
                    throw invalid("The credential's key says ES256 and is not an "
                            + "uncompressed EC2 key on P-256");
                }
                return new CoseKey(algorithm, Der.ecPublicKey(Der.P256, (byte[]) x, (byte[]) y));
            }
            if (algorithm == RS256) {
                Object n = key.get(P1);
                Object e = key.get(P2);
                if (type != 3 || !(n instanceof byte[]) || !(e instanceof byte[])) {
                    throw invalid("The credential's key says RS256 and is not an RSA key");
                }
                byte[] spki = Der.rsaPublicKey((byte[]) n, (byte[]) e);
                int bits = Der.rsaModulusBits(spki);
                if (bits < 2048 || bits > 8192) {
                    throw invalid("The credential's RSA key is " + bits + " bits; 2048 to 8192 "
                            + "are accepted");
                }
                return new CoseKey(algorithm, spki);
            }
        } catch (IOException err) {
            throw new WebAuthnException(WebAuthnException.INVALID_KEY, "The credential's public "
                    + "key is not a key of the algorithm it names: " + err.getMessage(), err);
        }
        throw new WebAuthnException(WebAuthnException.UNSUPPORTED_ALGORITHM, "The credential's "
                + "key is of COSE algorithm " + algorithm + ". This server verifies ES256 (-7) "
                + "and RS256 (-257), and offers no other.");
    }

    private static WebAuthnException invalid(String message) {
        return new WebAuthnException(WebAuthnException.INVALID_KEY, message);
    }

    /// The key read from stored bytes: what [#getPublicKey] returned.
    ///
    /// - [WebAuthnException]: when `algorithm` is not one this server verifies
    public static CoseKey of(long algorithm, byte[] publicKey) {
        if (algorithm != ES256 && algorithm != RS256) {
            throw new WebAuthnException(WebAuthnException.UNSUPPORTED_ALGORITHM, "COSE "
                    + "algorithm " + algorithm + " is not one this server verifies");
        }
        if (publicKey == null || publicKey.length == 0) {
            throw invalid("A credential needs a public key");
        }
        return new CoseKey(algorithm, publicKey.clone());
    }

    /// The COSE identifier of the key's algorithm: [#ES256] or [#RS256].
    public long getAlgorithm() {
        return algorithm;
    }

    /// The key as a SubjectPublicKeyInfo, in DER.
    public byte[] getPublicKey() {
        return publicKey.clone();
    }

    /// Whether `signature` is this key's signature of `data`. An ES256
    /// signature is ASN.1 DER, as an authenticator sends it.
    ///
    /// - [WebAuthnException]: [WebAuthnException#INVALID_KEY] when the key
    /// cannot be used at all, which is not the same as a signature that is
    /// wrong
    public boolean verify(byte[] data, byte[] signature) {
        try {
            return Crypto.verify(algorithm == ES256 ? Crypto.ES256 : Crypto.RS256, publicKey,
                    data, signature);
        } catch (IOException err) {
            throw new WebAuthnException(WebAuthnException.INVALID_KEY, "The credential's public "
                    + "key cannot be used: " + err.getMessage(), err);
        }
    }
}
