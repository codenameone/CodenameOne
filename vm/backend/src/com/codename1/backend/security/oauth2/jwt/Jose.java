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
package com.codename1.backend.security.oauth2.jwt;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.MacAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import java.io.IOException;

/// The one place that says which key an algorithm goes with, and computes a
/// signature under it. The encoder and the decoder both ask here, so they
/// cannot come to disagree.
final class Jose {
    /// The smallest RSA key a token is signed or verified with (RFC 7518 3.3).
    static final int MIN_RSA_BITS = 2048;

    private Jose() {
    }

    /// The algorithm of this name, or null when it is none this runtime has --
    /// `none` among them.
    static JwsAlgorithm algorithm(String name) {
        JwsAlgorithm found = SignatureAlgorithm.from(name);
        return found != null ? found : MacAlgorithm.from(name);
    }

    /// Whether `key` is of the kind `algorithm` is defined over: an RSA key for
    /// RS256/384/512 and PS256, a P-256 key for ES256, a P-384 key for ES384, a
    /// shared secret for HS256/384/512 -- and, when the key names one algorithm
    /// of its own, that one.
    static boolean fits(Jwk key, JwsAlgorithm algorithm) {
        if (key.getAlgorithm() != null && !key.getAlgorithm().equals(algorithm.getName())) {
            return false;
        }
        if ("enc".equals(key.getUse())) {
            return false;
        }
        String type = key.getKeyType();
        if (algorithm instanceof MacAlgorithm) {
            return Jwk.OCT.equals(type);
        }
        if (algorithm == SignatureAlgorithm.ES256) {
            return Der.EC.equals(type) && Der.P256.equals(key.getCurve());
        }
        if (algorithm == SignatureAlgorithm.ES384) {
            return Der.EC.equals(type) && Der.P384.equals(key.getCurve());
        }
        return Der.RSA.equals(type);
    }

    /// The algorithm a key signs with when nobody says: RS256 for an RSA key,
    /// the curve's own for an EC key, HS256 for a secret.
    static JwsAlgorithm defaultFor(Jwk key) {
        if (key.getAlgorithm() != null) {
            JwsAlgorithm named = algorithm(key.getAlgorithm());
            if (named != null) {
                return named;
            }
        }
        if (Jwk.OCT.equals(key.getKeyType())) {
            return MacAlgorithm.HS256;
        }
        if (Der.EC.equals(key.getKeyType())) {
            return Der.P384.equals(key.getCurve()) ? SignatureAlgorithm.ES384
                    : SignatureAlgorithm.ES256;
        }
        return SignatureAlgorithm.RS256;
    }

    private static String digest(JwsAlgorithm algorithm) {
        if (algorithm == MacAlgorithm.HS384) {
            return Crypto.SHA384;
        }
        return algorithm == MacAlgorithm.HS512 ? Crypto.SHA512 : Crypto.SHA256;
    }

    /// The bytes of the secret an HMAC algorithm needs at least: its digest's.
    static int minSecretBytes(JwsAlgorithm algorithm) {
        if (algorithm == MacAlgorithm.HS384) {
            return 48;
        }
        return algorithm == MacAlgorithm.HS512 ? 64 : 32;
    }

    private static int ecPartLength(JwsAlgorithm algorithm) {
        return algorithm == SignatureAlgorithm.ES384 ? 48 : 32;
    }

    /// Refuses a key too weak to be worth a signature.
    static void requireStrength(Jwk key, JwsAlgorithm algorithm) throws IOException {
        if (Der.RSA.equals(key.getKeyType())
                && Der.rsaModulusBits(key.getPublicKey()) < MIN_RSA_BITS) {
            throw new IOException("An RSA key under " + MIN_RSA_BITS + " bits is not accepted");
        }
        if (Jwk.OCT.equals(key.getKeyType())
                && key.getPrivateKey().length < minSecretBytes(algorithm)) {
            throw new IOException("A secret for " + algorithm.getName() + " is at least "
                    + minSecretBytes(algorithm) + " bytes");
        }
    }

    /// The signature of `input` as a token carries it.
    static byte[] sign(JwsAlgorithm algorithm, Jwk key, byte[] input) throws IOException {
        requireStrength(key, algorithm);
        if (algorithm instanceof MacAlgorithm) {
            return Crypto.hmac(digest(algorithm), key.getPrivateKey(), input);
        }
        byte[] signature = Crypto.sign(algorithm.getName(), key.getPrivateKey(), input);
        if (algorithm == SignatureAlgorithm.ES256 || algorithm == SignatureAlgorithm.ES384) {
            return Der.ecdsaDerToJose(signature, ecPartLength(algorithm));
        }
        return signature;
    }

    /// Whether `signature` is `key`'s signature of `input` under `algorithm`.
    ///
    /// @throws IOException when the key cannot be used; never for a signature
    /// that is merely wrong
    static boolean verify(JwsAlgorithm algorithm, Jwk key, byte[] input, byte[] signature)
            throws IOException {
        requireStrength(key, algorithm);
        if (algorithm instanceof MacAlgorithm) {
            if (key.getPrivateKey() == null) {
                throw new IOException("A shared secret is needed to verify "
                        + algorithm.getName());
            }
            return Crypto.equalsConstantTime(
                    Crypto.hmac(digest(algorithm), key.getPrivateKey(), input), signature);
        }
        byte[] wire = signature;
        if (algorithm == SignatureAlgorithm.ES256 || algorithm == SignatureAlgorithm.ES384) {
            // Two numbers of exactly the curve's size, or it is not a signature
            // of this algorithm -- and nothing is handed to the verifier.
            if (signature.length != 2 * ecPartLength(algorithm)) {
                return false;
            }
            wire = Der.ecdsaJoseToDer(signature);
        }
        return Crypto.verify(algorithm.getName(), key.getPublicKey(), input, wire);
    }

    static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }

    static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    static String string(byte[] value) {
        try {
            return new String(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
