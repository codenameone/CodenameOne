/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import com.codename1.security.CryptoException;
import com.codename1.security.Hash;
import com.codename1.security.Jwt;
import com.codename1.security.PublicKey;
import com.codename1.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// What [OidcClient] asks of an ID token before it hands one to the application: that its
/// claims are for this client from this issuer, now, and that the provider signed it.
///
/// The claims are checked as OpenID Connect Core 3.1.3.7 lists them. The signature is
/// checked against a key of the provider's JWK Set, built here from the JWK's own numbers
/// into the X.509 form [PublicKey] takes: `n` and `e` for RSA, `x` and `y` for a P-256 or
/// P-384 curve.
final class IdTokenVerifier {
    /// `1.2.840.113549.1.1.1`, rsaEncryption, with its NULL parameters.
    private static final byte[] RSA_ALGORITHM = {0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
        (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00};
    /// id-ecPublicKey with the curve prime256v1.
    private static final byte[] P256_ALGORITHM = {0x30, 0x13, 0x06, 0x07, 0x2a, (byte) 0x86, 0x48,
        (byte) 0xce, 0x3d, 0x02, 0x01, 0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d,
        0x03, 0x01, 0x07};
    /// id-ecPublicKey with the curve secp384r1.
    private static final byte[] P384_ALGORITHM = {0x30, 0x10, 0x06, 0x07, 0x2a, (byte) 0x86, 0x48,
        (byte) 0xce, 0x3d, 0x02, 0x01, 0x06, 0x05, 0x2b, (byte) 0x81, 0x04, 0x00, 0x22};

    private IdTokenVerifier() {
    }

    private static OidcException invalid(String why) {
        return new OidcException(OidcException.INVALID_ID_TOKEN, why);
    }

    /// Checks the claims of an ID token.
    ///
    /// #### Parameters
    ///
    /// - `jwt`: the parsed ID token
    ///
    /// - `issuer`: the provider's expected issuer; required to accept an ID token
    ///
    /// - `clientId`: this client
    ///
    /// - `nonce`: the nonce the authorization request carried, or null when the token did not
    ///   come from one -- a refresh, a device grant
    ///
    /// - `accessToken`: the access token that arrived with it, for `at_hash`
    ///
    /// - `skewSeconds`: how far the device's clock may be from the provider's
    ///
    /// - `nowMillis`: the time
    ///
    /// #### Returns
    ///
    /// null when the claims are good; otherwise why they are not
    static OidcException checkClaims(Jwt jwt, String issuer, String clientId, String nonce,
            String accessToken, int skewSeconds, long nowMillis) {
        Map<String, Object> claims = jwt.getClaims();
        Object iss = claims.get("iss");
        if (issuer == null || issuer.length() == 0) {
            return invalid("An expected issuer is required to accept an ID token");
        }
        if (!sameIssuer(issuer, iss, claims.get("tid"))) {
            return invalid("The ID token was issued by " + iss + ", not by " + issuer);
        }
        Object aud = claims.get("aud");
        boolean ours = false;
        int audiences = 0;
        if (aud instanceof String) {
            audiences = 1;
            ours = aud.equals(clientId);
        } else if (aud instanceof List) {
            for (Object one : (List) aud) {
                audiences++;
                ours |= one != null && one.equals(clientId);
            }
        }
        if (!ours) {
            return invalid("The ID token is not for this client: its audience is " + aud);
        }
        Object azp = claims.get("azp");
        if ((audiences > 1 || azp != null) && (azp == null || !azp.equals(clientId))) {
            return invalid("The ID token was issued to another party: azp is " + azp);
        }
        // Who the token is about is the one thing every ID token must say (OpenID Connect
        // Core section 2), and it is what OidcTokens.getSubject() hands the application.
        // A token without it used to pass, and a sign-in completed for nobody in particular.
        Object sub = claims.get("sub");
        if (!(sub instanceof String) || ((String) sub).length() == 0) {
            return invalid("The ID token names no subject");
        }
        Object exp = claims.get("exp");
        if (!(exp instanceof Number)) {
            return invalid("The ID token has no expiry");
        }
        long skew = skewSeconds * 1000L;
        if (((Number) exp).longValue() * 1000L + skew < nowMillis) {
            return invalid("The ID token has expired");
        }
        if (!(claims.get("iat") instanceof Number)) {
            return invalid("The ID token has no numeric issuance time");
        }
        Object nbf = claims.get("nbf");
        if (nbf instanceof Number && ((Number) nbf).longValue() * 1000L - skew > nowMillis) {
            return invalid("The ID token is not valid yet");
        }
        if (nonce != null) {
            Object sent = claims.get("nonce");
            if (sent == null || !nonce.equals(sent.toString())) {
                return new OidcException(OidcException.NONCE_MISMATCH,
                        sent == null ? "The ID token carries no nonce"
                                : "ID token nonce did not match");
            }
        }
        Object atHash = claims.get("at_hash");
        if (atHash != null && accessToken != null) {
            String expected = leftHalfHash(jwt.getAlgorithm(), accessToken);
            if (expected != null && !expected.equals(atHash.toString())) {
                return invalid("The ID token does not belong to the access token it came "
                        + "with: at_hash does not match");
            }
        }
        return null;
    }

    /// Whether a token's `iss` is the configured issuer. Two providers write it in a way
    /// that is not the text of their discovery document: Microsoft's multi-tenant document
    /// has `{tenantid}` where a token has the tenant, and Google's tokens may leave the
    /// `https://` out.
    private static boolean sameIssuer(String issuer, Object iss, Object tenant) {
        if (!(iss instanceof String)) {
            return false;
        }
        if (issuer.equals(iss) || ("https://accounts.google.com".equals(issuer)
                && "accounts.google.com".equals(iss))) {
            return true;
        }
        int at = issuer.indexOf("{tenantid}");
        return at >= 0 && tenant instanceof String && ((String) tenant).length() > 0
                && (issuer.substring(0, at) + tenant + issuer.substring(at + 10)).equals(iss);
    }

    /// The `at_hash` of an access token under a signature algorithm: the left half of the
    /// algorithm's own hash, in base64url. Null for an algorithm whose hash is not known.
    static String leftHalfHash(String algorithm, String accessToken) {
        if (algorithm == null || algorithm.length() != 5) {
            return null;
        }
        byte[] ascii = new byte[accessToken.length()];
        for (int i = 0; i < ascii.length; i++) {
            ascii[i] = (byte) accessToken.charAt(i);
        }
        byte[] digest;
        if (algorithm.endsWith("256")) {
            digest = Hash.sha256(ascii);
        } else if (algorithm.endsWith("384")) {
            digest = Hash.sha384(ascii);
        } else if (algorithm.endsWith("512")) {
            digest = Hash.sha512(ascii);
        } else {
            return null;
        }
        byte[] half = new byte[digest.length / 2];
        System.arraycopy(digest, 0, half, 0, half.length);
        return unpadded(Base64.encodeUrlSafe(half));
    }

    private static String unpadded(String base64) {
        int end = base64.length();
        while (end > 0 && base64.charAt(end - 1) == '=') {
            end--;
        }
        return base64.substring(0, end);
    }

    /// The kind of key that signs under `algorithm`: `RSA` or `EC`. Null for an algorithm an
    /// ID token is not accepted under -- `none`, and the HMAC ones, whose key would be the
    /// client secret.
    static String keyType(String algorithm) {
        if ("RS256".equals(algorithm) || "RS384".equals(algorithm) || "RS512".equals(algorithm)) {
            return "RSA";
        }
        if ("ES256".equals(algorithm) || "ES384".equals(algorithm)) {
            return "EC";
        }
        return null;
    }

    /// The keys of a JWK Set that could have signed `jwt`: of the algorithm's kind, for
    /// signing, and with the token's key id when it names one.
    static List<Map<String, Object>> candidates(Jwt jwt, List<Map<String, Object>> keys) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        String type = keyType(jwt.getAlgorithm());
        Object kid = jwt.getHeader().get("kid");
        if (type == null || keys == null) {
            return out;
        }
        for (Map<String, Object> key : keys) {
            if (!type.equals(key.get("kty"))) {
                continue;
            }
            Object use = key.get("use");
            if (use != null && !"sig".equals(use)) {
                continue;
            }
            Object alg = key.get("alg");
            if (alg != null && !alg.equals(jwt.getAlgorithm())) {
                continue;
            }
            if (kid != null && !kid.equals(key.get("kid"))) {
                continue;
            }
            out.add(key);
        }
        return out;
    }

    /// Verifies the signature of `jwt` against the candidate keys.
    ///
    /// #### Returns
    ///
    /// null when one of them verifies it; otherwise why none did. A platform that cannot
    /// check a signature of this kind is such a reason: the token is refused, not waved
    /// through
    static OidcException verifySignature(Jwt jwt, List<Map<String, Object>> candidates) {
        String algorithm = jwt.getAlgorithm();
        if (keyType(algorithm) == null) {
            return invalid("The ID token is signed with " + algorithm + ", which is not "
                    + "accepted: an ID token is signed with RS256, RS384, RS512, ES256 or "
                    + "ES384");
        }
        if (candidates.isEmpty()) {
            return invalid("The provider's keys hold none for this ID token (alg " + algorithm
                    + ", kid " + jwt.getHeader().get("kid") + ")");
        }
        for (Map<String, Object> jwk : candidates) {
            PublicKey key;
            try {
                key = publicKey(jwk);
            } catch (RuntimeException malformed) {
                continue;
            }
            try {
                if (jwt.verify(key)) {
                    return null;
                }
            } catch (CryptoException unsupported) {
                return new OidcException(OidcException.INVALID_ID_TOKEN, "This platform could "
                        + "not verify an " + algorithm + " signature (" + unsupported.getMessage()
                        + "), so the ID token was not accepted. If this provider's ID tokens "
                        + "cannot be verified on this platform, call "
                        + "OidcClient.setVerifyIdTokenSignature(false) and verify the token on "
                        + "your server before trusting what it says.", unsupported);
            }
        }
        return invalid("The ID token's signature does not verify against the provider's keys");
    }

    /// A [PublicKey] from a JWK.
    ///
    /// - `CryptoException`: when the JWK is not an RSA key or a P-256 or P-384 one, or lacks
    ///   its numbers
    static PublicKey publicKey(Map<String, Object> jwk) {
        Object kty = jwk.get("kty");
        if ("RSA".equals(kty)) {
            return PublicKey.fromX509(PublicKey.RSA, rsaSpki(number(jwk, "n"), number(jwk, "e")));
        }
        if ("EC".equals(kty)) {
            Object crv = jwk.get("crv");
            byte[] x = number(jwk, "x");
            byte[] y = number(jwk, "y");
            if ("P-256".equals(crv)) {
                return PublicKey.fromX509(PublicKey.EC, ecSpki(P256_ALGORITHM, 32, x, y));
            }
            if ("P-384".equals(crv)) {
                return PublicKey.fromX509(PublicKey.EC, ecSpki(P384_ALGORITHM, 48, x, y));
            }
            throw new CryptoException("Unsupported curve: " + crv);
        }
        throw new CryptoException("Unsupported key type: " + kty);
    }

    private static byte[] number(Map<String, Object> jwk, String name) {
        Object value = jwk.get(name);
        byte[] decoded = value instanceof String ? Base64.decodeUrlSafe((String) value) : null;
        if (decoded == null || decoded.length == 0) {
            throw new CryptoException("The JWK has no " + name);
        }
        return decoded;
    }

    /// SubjectPublicKeyInfo for an RSA key: the algorithm, then a BIT STRING holding the
    /// SEQUENCE of the modulus and the exponent.
    static byte[] rsaSpki(byte[] modulus, byte[] exponent) {
        byte[] key = sequence(concat(integer(modulus), integer(exponent)));
        byte[] bits = new byte[key.length + 1];
        System.arraycopy(key, 0, bits, 1, key.length);
        return sequence(concat(RSA_ALGORITHM, tagged(0x03, bits)));
    }

    /// SubjectPublicKeyInfo for a point on a curve: the algorithm with its curve, then a BIT
    /// STRING holding the uncompressed point, each coordinate at the curve's width.
    static byte[] ecSpki(byte[] algorithm, int width, byte[] x, byte[] y) {
        byte[] bits = new byte[2 + 2 * width];
        bits[1] = 0x04;
        fixed(x, bits, 2, width);
        fixed(y, bits, 2 + width, width);
        return sequence(concat(algorithm, tagged(0x03, bits)));
    }

    /// Copies an unsigned number into `width` bytes at `at`, dropping leading zeros it has
    /// too many of and adding the ones it lacks.
    private static void fixed(byte[] number, byte[] out, int at, int width) {
        int from = 0;
        while (number.length - from > width && number[from] == 0) {
            from++;
        }
        int length = number.length - from;
        if (length > width) {
            throw new CryptoException("A coordinate is wider than its curve");
        }
        System.arraycopy(number, from, out, at + width - length, length);
    }

    /// A DER INTEGER for an unsigned number: no leading zeros but the one that keeps it
    /// positive.
    private static byte[] integer(byte[] unsigned) {
        int from = 0;
        while (from < unsigned.length - 1 && unsigned[from] == 0) {
            from++;
        }
        boolean pad = (unsigned[from] & 0x80) != 0;
        byte[] value = new byte[unsigned.length - from + (pad ? 1 : 0)];
        System.arraycopy(unsigned, from, value, pad ? 1 : 0, unsigned.length - from);
        return tagged(0x02, value);
    }

    private static byte[] sequence(byte[] content) {
        return tagged(0x30, content);
    }

    private static byte[] tagged(int tag, byte[] content) {
        int length = content.length;
        byte[] header;
        if (length < 0x80) {
            header = new byte[] {(byte) tag, (byte) length};
        } else if (length < 0x100) {
            header = new byte[] {(byte) tag, (byte) 0x81, (byte) length};
        } else {
            header = new byte[] {(byte) tag, (byte) 0x82, (byte) (length >> 8), (byte) length};
        }
        return concat(header, content);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
