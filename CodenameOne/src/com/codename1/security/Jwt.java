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

import com.codename1.io.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

/// JSON Web Token (RFC 7519) signing and verification.
///
/// Supported algorithms:
///
/// - `HS256`, `HS384`, `HS512` -- HMAC with SHA-2. Pure Java, available on
///   every platform.
/// - `RS256`, `RS384`, `RS512` -- RSA-PKCS1-v1_5 with SHA-2. Backed by the
///   platform's native crypto via [Signature].
/// - `ES256`, `ES384`, `ES512` -- ECDSA with SHA-2. Backed by the platform's
///   native crypto via [Signature].
/// - `none` -- unsigned tokens. Accepted on the signing side only when caller
///   explicitly passes it; rejected on verification unless caller opts in via
///   [#verifyAllowNoneAlgorithm].
///
/// #### Sign a token
///
/// ```java
/// Map<String, Object> claims = new HashMap<String, Object>();
/// claims.put("sub", "user-123");
/// claims.put("exp", System.currentTimeMillis() / 1000 + 3600);
///
/// String token = Jwt.signHs256(claims, "secret".getBytes("UTF-8"));
/// ```
///
/// #### Verify and read claims
///
/// ```java
/// Jwt parsed = Jwt.parse(token);
/// if (!parsed.verifyHs256("secret".getBytes("UTF-8"))) {
///     throw new SecurityException("bad signature");
/// }
/// String sub = (String) parsed.getClaim("sub");
/// ```
public final class Jwt {

    /// HMAC-SHA-256 ("HS256")
    public static final String HS256 = "HS256";
    /// HMAC-SHA-384 ("HS384")
    public static final String HS384 = "HS384";
    /// HMAC-SHA-512 ("HS512")
    public static final String HS512 = "HS512";
    /// RSA PKCS#1 v1.5 with SHA-256 ("RS256")
    public static final String RS256 = "RS256";
    /// RSA PKCS#1 v1.5 with SHA-384 ("RS384")
    public static final String RS384 = "RS384";
    /// RSA PKCS#1 v1.5 with SHA-512 ("RS512")
    public static final String RS512 = "RS512";
    /// ECDSA with SHA-256 ("ES256")
    public static final String ES256 = "ES256";
    /// ECDSA with SHA-384 ("ES384")
    public static final String ES384 = "ES384";
    /// ECDSA with SHA-512 ("ES512")
    public static final String ES512 = "ES512";
    /// Unsigned token marker -- verification rejects this unless the caller
    /// explicitly opts in.
    public static final String NONE = "none";

    private final Map<String, Object> header;
    private final Map<String, Object> claims;
    private final byte[] signature;
    private final String signingInput; // header.payload (no signature)
    private boolean verifyAllowNoneAlgorithm;

    private Jwt(Map<String, Object> header, Map<String, Object> claims,
                byte[] signature, String signingInput) {
        this.header = header;
        this.claims = claims;
        this.signature = signature;
        this.signingInput = signingInput;
    }

    // ================================================================
    // signing

    /// Signs `claims` with HS256 and returns the encoded token.
    public static String signHs256(Map<String, Object> claims, byte[] secret) {
        return sign(claims, secret, HS256);
    }

    /// Signs `claims` with HS384 and returns the encoded token.
    public static String signHs384(Map<String, Object> claims, byte[] secret) {
        return sign(claims, secret, HS384);
    }

    /// Signs `claims` with HS512 and returns the encoded token.
    public static String signHs512(Map<String, Object> claims, byte[] secret) {
        return sign(claims, secret, HS512);
    }

    /// Signs `claims` with the given HMAC algorithm. Use this when you want
    /// to pass the algorithm dynamically.
    public static String sign(Map<String, Object> claims, byte[] secret, String algorithm) {
        if (claims == null) {
            throw new CryptoException("claims must not be null");
        }
        if (algorithm == null) {
            throw new CryptoException("algorithm must not be null");
        }
        String signingInput = signingInput(algorithm, claims);
        byte[] sig = computeHmac(algorithm, secret, signingInput);
        return signingInput + "." + com.codename1.util.Base64.encodeUrlSafe(sig);
    }

    /// Signs `claims` with the given RSA or ECDSA algorithm.
    ///
    /// #### Parameters
    ///
    /// - `claims`: token body
    ///
    /// - `privateKey`: signing key -- must match the algorithm family
    ///
    /// - `algorithm`: one of [#RS256], [#RS384], [#RS512], [#ES256], [#ES384],
    ///   [#ES512]
    public static String sign(Map<String, Object> claims, PrivateKey privateKey, String algorithm) {
        if (claims == null) {
            throw new CryptoException("claims must not be null");
        }
        if (privateKey == null) {
            throw new CryptoException("privateKey must not be null");
        }
        String sigAlg = signatureAlgorithmFor(algorithm);
        String signingInput = signingInput(algorithm, claims);
        byte[] data;
        try {
            data = signingInput.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("UTF-8 not supported", e);
        }
        byte[] sig = Signature.sign(sigAlg, privateKey, data);
        // For ES* the JWT spec mandates a raw `r||s` concatenation rather than
        // the platform's default DER-encoded SEQUENCE. We convert here.
        if (algorithm.startsWith("ES")) {
            sig = derToJoseEcdsa(sig, ecdsaCoordinateLength(algorithm));
        }
        return signingInput + "." + com.codename1.util.Base64.encodeUrlSafe(sig);
    }

    /// Builds an unsigned token (header `{"alg":"none"}`). Accepting these on
    /// the verify side is dangerous -- see [#verifyAllowNoneAlgorithm].
    public static String signNone(Map<String, Object> claims) {
        return signingInput(NONE, claims) + ".";
    }

    // ================================================================
    // parsing

    /// Parses an encoded JWT into a [Jwt] object. The signature is NOT
    /// verified -- you must call one of the `verify*` methods afterwards.
    public static Jwt parse(String token) {
        if (token == null) {
            throw new CryptoException("token must not be null");
        }
        int firstDot = token.indexOf('.');
        if (firstDot < 0) {
            throw new CryptoException("malformed JWT: no '.'");
        }
        int secondDot = token.indexOf('.', firstDot + 1);
        if (secondDot < 0) {
            throw new CryptoException("malformed JWT: only one '.'");
        }
        String headerB64 = token.substring(0, firstDot);
        String payloadB64 = token.substring(firstDot + 1, secondDot);
        String sigB64 = token.substring(secondDot + 1);
        Map<String, Object> header = readJson(com.codename1.util.Base64.decodeUrlSafe(headerB64));
        Map<String, Object> claims = readJson(com.codename1.util.Base64.decodeUrlSafe(payloadB64));
        byte[] sig = sigB64.length() == 0 ? new byte[0] : com.codename1.util.Base64.decodeUrlSafe(sigB64);
        return new Jwt(header, claims, sig, headerB64 + "." + payloadB64);
    }

    // ================================================================
    // verification

    /// When set to true, [#verify] will accept tokens whose `alg` header is
    /// `none` (i.e. unsigned). The default is false because in most JWT
    /// deployments accepting unsigned tokens is a critical security bug.
    /// Only enable this if you have very deliberately decided that you trust
    /// the transport.
    public void setVerifyAllowNoneAlgorithm(boolean allow) {
        this.verifyAllowNoneAlgorithm = allow;
    }

    /// Verifies with a shared HMAC secret. The token's `alg` header is read
    /// and must be one of the HS family.
    public boolean verifyHs256(byte[] secret) {
        return verifyHmac(HS256, secret);
    }

    /// HMAC verification with HS384.
    public boolean verifyHs384(byte[] secret) {
        return verifyHmac(HS384, secret);
    }

    /// HMAC verification with HS512.
    public boolean verifyHs512(byte[] secret) {
        return verifyHmac(HS512, secret);
    }

    private boolean verifyHmac(String expectedAlg, byte[] secret) {
        if (!expectedAlg.equals(getAlgorithm())) {
            return false;
        }
        byte[] expected = computeHmac(expectedAlg, secret, signingInput);
        return Hmac.constantTimeEquals(expected, signature);
    }

    /// Verifies an RSA or ECDSA signature using the given public key. The
    /// algorithm must match the token's `alg` header (RS256/384/512 or
    /// ES256/384/512).
    public boolean verify(PublicKey publicKey) {
        String alg = getAlgorithm();
        if (NONE.equals(alg)) {
            return verifyAllowNoneAlgorithm && (signature == null || signature.length == 0);
        }
        String sigAlg = signatureAlgorithmFor(alg);
        byte[] data;
        try {
            data = signingInput.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("UTF-8 not supported", e);
        }
        byte[] sig = signature;
        if (alg.startsWith("ES")) {
            sig = joseToDerEcdsa(sig, ecdsaCoordinateLength(alg));
        }
        return Signature.verify(sigAlg, publicKey, data, sig);
    }

    // ================================================================
    // accessors

    /// Returns the `alg` field from the JWT header (e.g. "HS256").
    public String getAlgorithm() {
        Object v = header.get("alg");
        return v == null ? null : v.toString();
    }

    /// Returns the parsed header as an unmodifiable view into the original
    /// map. Mutating it has undefined behaviour.
    public Map<String, Object> getHeader() {
        return header;
    }

    /// Returns the parsed claims (token payload) as an unmodifiable view into
    /// the original map. Mutating it has undefined behaviour.
    public Map<String, Object> getClaims() {
        return claims;
    }

    /// Returns the value of a single claim, or null if the claim is absent.
    public Object getClaim(String name) {
        return claims == null ? null : claims.get(name);
    }

    /// Returns the raw bytes of the signature segment as decoded from
    /// URL-safe base64. May be empty for unsigned tokens.
    public byte[] getSignature() {
        return signature;
    }

    // ================================================================
    // internals

    private static String signingInput(String algorithm, Map<String, Object> claims) {
        Map<String, Object> hdr = new LinkedHashMap<String, Object>();
        hdr.put("alg", algorithm);
        hdr.put("typ", "JWT");
        String headerJson = JSONParser.mapToJson(hdr);
        String claimsJson = JSONParser.mapToJson(claims);
        try {
            return com.codename1.util.Base64.encodeUrlSafe(headerJson.getBytes("UTF-8")) + "."
                 + com.codename1.util.Base64.encodeUrlSafe(claimsJson.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("UTF-8 not supported", e);
        }
    }

    private static byte[] computeHmac(String algorithm, byte[] secret, String signingInput) {
        String hashAlg;
        if (HS256.equals(algorithm)) {
            hashAlg = Hash.SHA256;
        } else if (HS384.equals(algorithm)) {
            hashAlg = Hash.SHA384;
        } else if (HS512.equals(algorithm)) {
            hashAlg = Hash.SHA512;
        } else {
            throw new CryptoException("unsupported HMAC algorithm: " + algorithm);
        }
        try {
            return Hmac.create(hashAlg, secret).doFinal(signingInput.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("UTF-8 not supported", e);
        }
    }

    private static String signatureAlgorithmFor(String jwtAlg) {
        if (RS256.equals(jwtAlg)) {
            return Signature.SHA256_WITH_RSA;
        }
        if (RS384.equals(jwtAlg)) {
            return Signature.SHA384_WITH_RSA;
        }
        if (RS512.equals(jwtAlg)) {
            return Signature.SHA512_WITH_RSA;
        }
        if (ES256.equals(jwtAlg)) {
            return Signature.SHA256_WITH_ECDSA;
        }
        if (ES384.equals(jwtAlg)) {
            return Signature.SHA384_WITH_ECDSA;
        }
        if (ES512.equals(jwtAlg)) {
            return Signature.SHA512_WITH_ECDSA;
        }
        throw new CryptoException("unsupported JWT algorithm: " + jwtAlg);
    }

    private static int ecdsaCoordinateLength(String jwtAlg) {
        if (ES256.equals(jwtAlg)) {
            return 32;   // P-256
        }
        if (ES384.equals(jwtAlg)) {
            return 48;   // P-384
        }
        if (ES512.equals(jwtAlg)) {
            return 66;   // P-521
        }
        throw new CryptoException("not an ECDSA algorithm: " + jwtAlg);
    }

    private static Map<String, Object> readJson(byte[] data) {
        try {
            JSONParser parser = new JSONParser();
            return parser.parseJSON(new InputStreamReader(new ByteArrayInputStream(data), "UTF-8"));
        } catch (Exception e) {
            throw new CryptoException("invalid JSON in JWT", e);
        }
    }

    // ----------------------------------------------------------------
    // ECDSA signatures: platform sign() returns ASN.1 DER (SEQUENCE { r, s }),
    // JWT requires raw r||s. Tiny converters here keep ES* support compact.
    private static byte[] derToJoseEcdsa(byte[] der, int coordLen) {
        // SEQUENCE (0x30) length ...
        // INTEGER  (0x02) lenR rBytes
        // INTEGER  (0x02) lenS sBytes
        if (der.length < 8 || (der[0] & 0xff) != 0x30) {
            throw new CryptoException("bad ECDSA DER signature");
        }
        int p = 2;
        if ((der[1] & 0x80) != 0) {
            int n = der[1] & 0x7f;
            p = 2 + n;
        }
        if ((der[p] & 0xff) != 0x02) {
            throw new CryptoException("bad ECDSA DER signature");
        }
        int rLen = der[p + 1] & 0xff;
        int rOff = p + 2;
        int sStart = rOff + rLen;
        if ((der[sStart] & 0xff) != 0x02) {
            throw new CryptoException("bad ECDSA DER signature");
        }
        int sLen = der[sStart + 1] & 0xff;
        int sOff = sStart + 2;
        byte[] out = new byte[coordLen * 2];
        copyAndPad(der, rOff, rLen, out, 0, coordLen);
        copyAndPad(der, sOff, sLen, out, coordLen, coordLen);
        return out;
    }

    private static void copyAndPad(byte[] src, int srcOff, int srcLen,
                                   byte[] dst, int dstOff, int dstLen) {
        // Strip leading zero pad if any
        while (srcLen > 0 && src[srcOff] == 0) {
            srcOff++;
            srcLen--;
        }
        if (srcLen > dstLen) {
            throw new CryptoException("ECDSA coordinate too large");
        }
        int pad = dstLen - srcLen;
        for (int i = 0; i < pad; i++) {
            dst[dstOff + i] = 0;
        }
        System.arraycopy(src, srcOff, dst, dstOff + pad, srcLen);
    }

    private static byte[] joseToDerEcdsa(byte[] jose, int coordLen) {
        if (jose.length != coordLen * 2) {
            throw new CryptoException("bad ECDSA JOSE signature length");
        }
        byte[] r = trimAndAddSignByte(jose, 0, coordLen);
        byte[] s = trimAndAddSignByte(jose, coordLen, coordLen);
        int seqLen = 2 + r.length + 2 + s.length;
        byte[] out;
        if (seqLen < 128) {
            out = new byte[2 + seqLen];
            out[0] = 0x30;
            out[1] = (byte) seqLen;
            int p = 2;
            out[p++] = 0x02; out[p++] = (byte) r.length;
            System.arraycopy(r, 0, out, p, r.length); p += r.length;
            out[p++] = 0x02; out[p++] = (byte) s.length;
            System.arraycopy(s, 0, out, p, s.length);
        } else {
            // 0x81 length form
            out = new byte[3 + seqLen];
            out[0] = 0x30;
            out[1] = (byte) 0x81;
            out[2] = (byte) seqLen;
            int p = 3;
            out[p++] = 0x02; out[p++] = (byte) r.length;
            System.arraycopy(r, 0, out, p, r.length); p += r.length;
            out[p++] = 0x02; out[p++] = (byte) s.length;
            System.arraycopy(s, 0, out, p, s.length);
        }
        return out;
    }

    private static byte[] trimAndAddSignByte(byte[] src, int off, int len) {
        int start = off;
        int end = off + len;
        while (start < end - 1 && src[start] == 0) {
            start++;
        }
        boolean needPad = (src[start] & 0x80) != 0;
        int outLen = (end - start) + (needPad ? 1 : 0);
        byte[] out = new byte[outLen];
        int p = 0;
        if (needPad) {
            out[p++] = 0;
        }
        System.arraycopy(src, start, out, p, end - start);
        return out;
    }
}
