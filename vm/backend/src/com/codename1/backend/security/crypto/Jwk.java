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
package com.codename1.backend.security.crypto;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// One key, as a JSON Web Key describes it (RFC 7517): an RSA key, an EC key on
/// P-256 or P-384, or a shared secret.
///
/// ```java
/// Jwk signing = Jwk.ofPrivateKey(KeyFiles.readPrivateKey("/etc/app/signing.pem"));
/// Map<String, Object> published = signing.toPublicJson();   // n and e, never d
/// Jwk theirs = Jwk.parse(Json.parseObject(text));
/// ```
///
/// Inside, a key is held in the form the runtime computes with -- PKCS#8 and
/// SubjectPublicKeyInfo -- and the JSON form is made and read at the edges.
///
/// A key has an id. Unless one is given it is the base64url SHA-256 of the
/// SubjectPublicKeyInfo, so the same key has the same id wherever it is loaded
/// and a rotated key has a new one without anybody naming it.
public final class Jwk {
    /// The key type of a shared secret.
    public static final String OCT = "oct";

    private final String keyType;
    private final String curve;
    private final byte[] publicKey;
    private final byte[] privateKey;
    private final String keyId;
    private final String algorithm;
    private final String use;

    private Jwk(String keyType, String curve, byte[] publicKey, byte[] privateKey, String keyId,
                String algorithm, String use) {
        this.keyType = keyType;
        this.curve = curve;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.keyId = keyId;
        this.algorithm = algorithm;
        this.use = use;
    }

    /// A key that verifies, from a SubjectPublicKeyInfo.
    public static Jwk ofPublicKey(byte[] publicKey) throws IOException {
        String type = Der.publicKeyType(publicKey);
        String curve = null;
        if (Der.EC.equals(type)) {
            curve = Der.ecCurve(publicKey);
            Der.ecPublicKeyParts(publicKey);
        } else {
            Der.rsaPublicKeyParts(publicKey);
        }
        return new Jwk(type, curve, publicKey.clone(), null, null, null, null);
    }

    /// A key that signs and verifies, from a PKCS#8 private key that holds its
    /// public half; see [Der#publicKeyOf].
    public static Jwk ofPrivateKey(byte[] privateKey) throws IOException {
        return ofKeyPair(privateKey, Der.publicKeyOf(privateKey));
    }

    /// A key that signs and verifies, from a PKCS#8 private key and its
    /// SubjectPublicKeyInfo.
    public static Jwk ofKeyPair(byte[] privateKey, byte[] publicKey) throws IOException {
        Jwk verifying = ofPublicKey(publicKey);
        if (!verifying.keyType.equals(Der.privateKeyType(privateKey))) {
            throw new IOException("The private key and the public key are of different kinds");
        }
        String algorithm = Der.RSA.equals(verifying.keyType) ? Crypto.RS256
                : (Der.P384.equals(verifying.curve) ? Crypto.ES384 : Crypto.ES256);
        byte[] challenge = Crypto.randomBytes(32);
        if (!Crypto.verify(algorithm, verifying.publicKey, challenge,
                Crypto.sign(algorithm, privateKey, challenge))) {
            throw new IOException("The private key and the public key do not form a key pair");
        }
        return new Jwk(verifying.keyType, verifying.curve, verifying.publicKey,
                privateKey.clone(), null, null, null);
    }

    /// A shared secret, for the HMAC algorithms. It has no id unless given one,
    /// and is never written to JSON.
    public static Jwk ofSecret(byte[] secret) {
        if (secret == null || secret.length == 0) {
            throw new IllegalArgumentException("A secret is required");
        }
        return new Jwk(OCT, null, null, secret.clone(), null, null, null);
    }

    /// The key in a PEM text: a private key of any of the forms [KeyFiles]
    /// reads, or a public key.
    public static Jwk ofPem(String pem) throws IOException {
        if (pem != null && pem.indexOf("PRIVATE KEY-----") >= 0) {
            return ofPrivateKey(KeyFiles.privateKey(pem));
        }
        return ofPublicKey(KeyFiles.publicKey(pem));
    }

    /// The public key a JSON Web Key describes. Only what verifies is read: a
    /// `d` in the JSON is ignored, and a shared secret is refused.
    ///
    /// @throws IOException when the JSON is not an RSA key or an EC key on
    /// P-256 or P-384, with what is wrong
    public static Jwk parse(Map<String, Object> json) throws IOException {
        if (json == null) {
            throw new IOException("No JSON Web Key was given");
        }
        String type = text(json, "kty");
        byte[] publicKey;
        String curve = null;
        if (Der.RSA.equals(type)) {
            publicKey = Der.rsaPublicKey(number(json, "n"), number(json, "e"));
        } else if (Der.EC.equals(type)) {
            curve = text(json, "crv");
            publicKey = Der.ecPublicKey(curve, number(json, "x"), number(json, "y"));
        } else {
            throw new IOException("Not a supported JSON Web Key type: " + type
                    + ". The types are RSA and EC");
        }
        return new Jwk(type, curve, publicKey, null, text(json, "kid"), text(json, "alg"),
                text(json, "use"));
    }

    private static String text(Map<String, Object> json, String name) {
        Object value = json.get(name);
        return value instanceof String ? (String) value : null;
    }

    private static byte[] number(Map<String, Object> json, String name) throws IOException {
        String value = text(json, name);
        byte[] decoded = value == null ? null : Base64Url.decode(value);
        if (decoded == null || decoded.length == 0) {
            throw new IOException("The JSON Web Key has no usable \"" + name + "\"");
        }
        return decoded;
    }

    /// This key under another id.
    public Jwk withKeyId(String keyId) {
        return new Jwk(keyType, curve, publicKey, privateKey, keyId, algorithm, use);
    }

    /// This key for one algorithm alone: `RS256`. A token signed under any other
    /// is not verified with it.
    public Jwk withAlgorithm(String algorithm) {
        return new Jwk(keyType, curve, publicKey, privateKey, keyId, algorithm, use);
    }

    /// This key with a declared use: `sig`.
    public Jwk withUse(String use) {
        return new Jwk(keyType, curve, publicKey, privateKey, keyId, algorithm, use);
    }

    /// `RSA`, `EC` or `oct`.
    public String getKeyType() {
        return keyType;
    }

    /// `P-256` or `P-384` for an EC key; null otherwise.
    public String getCurve() {
        return curve;
    }

    /// The id given, or else the base64url SHA-256 of the SubjectPublicKeyInfo;
    /// null for a shared secret that was given none.
    public String getKeyId() {
        if (keyId != null || publicKey == null) {
            return keyId;
        }
        return Base64Url.encode(Crypto.sha256(publicKey));
    }

    /// The one algorithm the key is for, or null when it is for any its type
    /// allows.
    public String getAlgorithm() {
        return algorithm;
    }

    /// The declared use, or null.
    public String getUse() {
        return use;
    }

    /// The SubjectPublicKeyInfo; null for a shared secret.
    public byte[] getPublicKey() {
        return publicKey == null ? null : publicKey.clone();
    }

    /// The PKCS#8 private key, or the bytes of a shared secret; null for a key
    /// that only verifies.
    public byte[] getPrivateKey() {
        return privateKey == null ? null : privateKey.clone();
    }

    /// Whether the key can sign.
    public boolean isPrivate() {
        return privateKey != null;
    }

    /// The key as the JSON a key set publishes: its type, id and public
    /// numbers, with `alg` and `use` when it has them. Nothing private is ever
    /// in it.
    ///
    /// @throws IllegalStateException for a shared secret, which has no public
    /// form
    public Map<String, Object> toPublicJson() {
        if (publicKey == null) {
            throw new IllegalStateException("A shared secret has no public form");
        }
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("kty", keyType);
        if (use != null) {
            json.put("use", use);
        }
        if (algorithm != null) {
            json.put("alg", algorithm);
        }
        json.put("kid", getKeyId());
        try {
            if (Der.RSA.equals(keyType)) {
                byte[][] parts = Der.rsaPublicKeyParts(publicKey);
                json.put("n", Base64Url.encode(parts[0]));
                json.put("e", Base64Url.encode(parts[1]));
            } else {
                byte[][] parts = Der.ecPublicKeyParts(publicKey);
                json.put("crv", curve);
                json.put("x", Base64Url.encode(parts[0]));
                json.put("y", Base64Url.encode(parts[1]));
            }
        } catch (IOException err) {
            // Checked when the key was made.
            throw new IllegalStateException(err.getMessage(), err);
        }
        return json;
    }

    @Override
    public String toString() {
        return "Jwk [" + keyType + (curve == null ? "" : " " + curve) + ", kid=" + getKeyId()
                + (privateKey == null ? "" : ", private") + "]";
    }
}
