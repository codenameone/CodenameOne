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
package com.codename1.backend.security.pki;

import java.io.IOException;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;

/// The X.509 signature algorithms this package accepts, mapped to the
/// [Crypto] algorithm that checks them.
///
/// The profile: RSA PKCS#1 v1.5 with SHA-256, SHA-384 or SHA-512; RSASSA-PSS
/// with SHA-256, MGF1-SHA-256 and a 32 byte salt; ECDSA with SHA-256 over P-256
/// and SHA-384 over P-384. SHA-1 and MD5 signatures are refused, and so is
/// ECDSA with a digest that does not match its curve.
final class SignatureAlgorithms {
    static final String SHA256_RSA = "1.2.840.113549.1.1.11";
    static final String SHA384_RSA = "1.2.840.113549.1.1.12";
    static final String SHA512_RSA = "1.2.840.113549.1.1.13";
    static final String RSA_PSS = "1.2.840.113549.1.1.10";
    static final String ECDSA_SHA256 = "1.2.840.10045.4.3.2";
    static final String ECDSA_SHA384 = "1.2.840.10045.4.3.3";
    private static final String SHA256 = "2.16.840.1.101.3.4.2.1";
    private static final String MGF1 = "1.2.840.113549.1.1.8";

    private SignatureAlgorithms() {
    }

    /// The [Crypto] algorithm for an AlgorithmIdentifier, or an exception
    /// naming what is outside the profile.
    static String forIdentifier(byte[] algorithmIdentifier) throws IOException {
        Asn1.Reader id = new Asn1.Reader(algorithmIdentifier).open(Asn1.SEQUENCE);
        String oid = id.oid();
        if (SHA256_RSA.equals(oid)) {
            return Crypto.RS256;
        }
        if (SHA384_RSA.equals(oid)) {
            return Crypto.RS384;
        }
        if (SHA512_RSA.equals(oid)) {
            return Crypto.RS512;
        }
        if (ECDSA_SHA256.equals(oid)) {
            return Crypto.ES256;
        }
        if (ECDSA_SHA384.equals(oid)) {
            return Crypto.ES384;
        }
        if (RSA_PSS.equals(oid) && isPs256(id)) {
            return Crypto.PS256;
        }
        throw new CertificateException("Not a supported signature algorithm: " + name(oid)
                + ". Supported are RSA with SHA-256/384/512, RSASSA-PSS with SHA-256, "
                + "and ECDSA P-256/SHA-256 and P-384/SHA-384");
    }

    /// The algorithm a key signs a certificate request with: RS256 for RSA,
    /// ES256 for P-256, ES384 for P-384.
    static String forKey(byte[] publicKey) throws IOException {
        if (Der.RSA.equals(Der.publicKeyType(publicKey))) {
            return Crypto.RS256;
        }
        String curve = Der.ecCurve(publicKey);
        return Der.P256.equals(curve) ? Crypto.ES256 : Crypto.ES384;
    }

    /// The AlgorithmIdentifier for a [Crypto] algorithm this package signs with.
    static byte[] identifier(String algorithm) {
        if (Crypto.RS256.equals(algorithm)) {
            return Asn1.sequence(Asn1.oid(SHA256_RSA), Asn1.nullValue());
        }
        if (Crypto.ES256.equals(algorithm)) {
            return Asn1.sequence(Asn1.oid(ECDSA_SHA256));
        }
        if (Crypto.ES384.equals(algorithm)) {
            return Asn1.sequence(Asn1.oid(ECDSA_SHA384));
        }
        throw new IllegalArgumentException(algorithm);
    }

    /// RSASSA-PSS parameters that are exactly SHA-256, MGF1 over SHA-256, and a
    /// 32 byte salt, with the trailer field left at its default.
    private static boolean isPs256(Asn1.Reader id) throws IOException {
        Asn1.Reader params = id.openOptional(Asn1.SEQUENCE);
        if (params == null) {
            return false;
        }
        Asn1.Reader hash = params.openOptional(Asn1.explicit(0));
        if (hash == null || !SHA256.equals(hash.open(Asn1.SEQUENCE).oid())) {
            return false;
        }
        Asn1.Reader mgf = params.openOptional(Asn1.explicit(1));
        if (mgf == null) {
            return false;
        }
        Asn1.Reader mgfId = mgf.open(Asn1.SEQUENCE);
        if (!MGF1.equals(mgfId.oid()) || !SHA256.equals(mgfId.open(Asn1.SEQUENCE).oid())) {
            return false;
        }
        Asn1.Reader salt = params.openOptional(Asn1.explicit(2));
        if (salt == null || salt.smallInteger() != 32) {
            return false;
        }
        Asn1.Reader trailer = params.openOptional(Asn1.explicit(3));
        return (trailer == null || trailer.smallInteger() == 1) && !params.hasMore();
    }

    private static String name(String oid) {
        if ("1.2.840.113549.1.1.5".equals(oid)) {
            return "sha1WithRSAEncryption";
        }
        if ("1.2.840.113549.1.1.4".equals(oid)) {
            return "md5WithRSAEncryption";
        }
        if ("1.2.840.10045.4.1".equals(oid)) {
            return "ecdsa-with-SHA1";
        }
        if (RSA_PSS.equals(oid)) {
            return "RSASSA-PSS with parameters other than SHA-256/MGF1-SHA-256/32";
        }
        return oid;
    }
}
