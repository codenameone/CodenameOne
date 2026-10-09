/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.IOException;

/// The crypto a server needs to authenticate a request. Every primitive comes from
/// OpenSSL, which the backend already links for outbound TLS - none of it is
/// implemented here, because hand-rolled HMAC and hand-rolled password hashing are
/// the two most reliable ways to ship an authentication system that looks correct
/// and is not.
public final class Crypto {
    /// PBKDF2 iterations for a stored password. Deliberately expensive: the cost is
    /// paid once per login and multiplied by every guess an attacker makes against a
    /// stolen table.
    public static final int PASSWORD_ITERATIONS = 210000;
    /// The most iterations verifyPassword will compute for a stored value.
    ///
    /// That count is READ OUT of the stored string rather than chosen here, so a
    /// row an attacker can influence -- or an application handing this a value from
    /// somewhere it does not control -- names the work directly. PBKDF2 measured at
    /// roughly 0.08s per million rounds, so an unbounded count is minutes of CPU per
    /// login attempt, and a login endpoint invites the repeat.
    ///
    /// Well clear of PASSWORD_ITERATIONS so raising that stays a local decision:
    /// a hash this server wrote verifies under a second either way.
    private static final int MAX_VERIFY_ITERATIONS = 10000000;

    /// See verifyPassword: the other half of bounding the work a row can ask for.
    private static final int MAX_VERIFY_HASH_BYTES = 64;

    private static final int PASSWORD_SALT_BYTES = 16;
    private static final int PASSWORD_HASH_BYTES = 32;

    private Crypto() {
    }

    public static byte[] sha256(byte[] data) {
        return sha256Impl(data);
    }

    /// SHA-1, for the wire protocols that specify it by name: MySQL's
    /// mysql_native_password, and the RFC 6455 4.2.2 websocket handshake, where the
    /// digest of the client key and a fixed GUID becomes Sec-WebSocket-Accept.
    ///
    /// Never for anything this code CHOOSES: passwords go through
    /// [#hashPassword] and tokens through [#hmacSha256]. Both callers
    /// here are standards quoting the algorithm, and in neither is the result
    /// standing in for a signature -- the handshake value is a replay guard against
    /// caches and proxies, not an authenticator.
    public static byte[] sha1(byte[] data) {
        return sha1Impl(data);
    }

    /// MD5, for PostgreSQL's md5 authentication method. See [#sha1].
    public static byte[] md5(byte[] data) {
        return md5Impl(data);
    }

    /// PBKDF2-HMAC-SHA-256. Exposed because SCRAM-SHA-256 -- how PostgreSQL
    /// authenticates by default -- is defined in terms of it with the server's
    /// iteration count, which [#hashPassword] does not let a caller choose.
    public static byte[] pbkdf2Sha256(byte[] password, byte[] salt, int iterations, int length)
            throws IOException {
        return pbkdf2(password, salt, iterations, length);
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        return hmacSha256Impl(key, data);
    }

    /// Cryptographically secure bytes. Throws rather than returning weak ones.
    public static byte[] randomBytes(int length) throws IOException {
        byte[] out = randomBytesImpl(length);
        if (out == null) {
            throw new IOException("No secure randomness available");
        }
        return out;
    }

    /// Compares without leaking where two values first differ. An early exit on the
    /// first differing byte lets a MAC be forged one byte at a time.
    public static boolean equalsConstantTime(byte[] a, byte[] b) {
        return equalsConstantTimeImpl(a, b);
    }

    /// Hashes a password for storage. Returns "pbkdf2$iterations$salt$hash" with
    /// both binary parts base64url-encoded, so the iteration count travels with the
    /// hash and can be raised later without invalidating existing rows.
    public static String hashPassword(String password) throws IOException {
        // A null password is a MISSING one, not an empty one. utf8(null) answers an
        // empty array, so a handler that passed a DTO field the client never sent
        // got a perfectly valid verifier -- and verifyPassword("", thatHash) then
        // succeeds, which turns an omitted credential into an empty-password
        // account. verifyPassword already refuses null; this is the other half.
        if (password == null) {
            throw new IllegalArgumentException("a password is required");
        }
        byte[] salt = randomBytes(PASSWORD_SALT_BYTES);
        byte[] hash = pbkdf2(utf8(password), salt, PASSWORD_ITERATIONS, PASSWORD_HASH_BYTES);
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$" + Base64Url.encode(salt) + "$" + Base64Url.encode(hash);
    }

    /// False for any malformed stored value rather than throwing.
    public static boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = split(stored, '$');
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException err) {
            return false;
        }
        byte[] salt = Base64Url.decode(parts[2]);
        byte[] expected = Base64Url.decode(parts[3]);
        if (salt == null || expected == null || iterations <= 0
                || iterations > MAX_VERIFY_ITERATIONS) {
            return false;
        }
        // Non-EMPTY, not merely non-null. "pbkdf2$1$$" decodes to two empty arrays,
        // pbkdf2 then derives zero bytes, and comparing an empty expectation with
        // an empty derivation is TRUE -- so a stored row of that shape accepted
        // every password. Base64Url.decode answers an empty array for an empty
        // field, so the null check above never saw it. The floors are the standard
        // minimums (RFC 8018 wants at least eight bytes of salt); anything this
        // server writes is 16 and 32.
        // AND AN UPPER BOUND ON THE HASH, because its length is the OTHER factor
        // in the work this does. PBKDF2 runs the iteration count once per output
        // block, so capping the count alone -- which is what MAX_VERIFY_ITERATIONS
        // did -- still leaves a few kilobytes of stored hash multiplying it by a
        // hundred or more. 64 covers anything a sane writer produces, including a
        // SHA-512-sized digest; this server writes PASSWORD_HASH_BYTES.
        if (salt.length < 8 || expected.length < 16
                || expected.length > MAX_VERIFY_HASH_BYTES) {
            return false;
        }
        byte[] actual = pbkdf2Impl(utf8(password), salt, iterations, expected.length);
        return actual != null && equalsConstantTime(expected, actual);
    }

    /// The digest names [#hmac] and [#pbkdf2] take.
    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";
    public static final String SHA384 = "SHA-384";
    public static final String SHA512 = "SHA-512";

    /// RSASSA-PKCS1-v1_5 over SHA-256, SHA-384 and SHA-512: the same bytes for
    /// the same key and message, every time.
    public static final String RS256 = "RS256";
    public static final String RS384 = "RS384";
    public static final String RS512 = "RS512";
    /// RSASSA-PSS over SHA-256 with MGF1-SHA-256 and a 32 byte salt.
    public static final String PS256 = "PS256";
    /// ECDSA over P-256 with SHA-256, and over P-384 with SHA-384.
    public static final String ES256 = "ES256";
    public static final String ES384 = "ES384";

    /// SHA-384; null for null.
    public static byte[] sha384(byte[] data) {
        return digestImpl(3, data);
    }

    /// SHA-512; null for null.
    public static byte[] sha512(byte[] data) {
        return digestImpl(4, data);
    }

    /// HMAC over one of [#SHA1], [#SHA256], [#SHA384] and [#SHA512]. SHA-1 is
    /// here for what specifies it by name -- a TOTP secret an authenticator
    /// application already holds, Spring's oldest password format -- and not for
    /// anything new.
    ///
    /// @return the tag, or null when `key` or `data` is null
    /// @throws IllegalArgumentException for a digest that is not one of the four
    public static byte[] hmac(String digest, byte[] key, byte[] data) {
        return hmacImpl(digestCode(digest), key, data);
    }

    /// PBKDF2 over HMAC with one of [#SHA1], [#SHA256], [#SHA384] and [#SHA512],
    /// on the password's bytes as they are given.
    ///
    /// @throws IOException when a count is not positive or derivation fails
    public static byte[] pbkdf2(String digest, byte[] password, byte[] salt, int iterations,
                                int length) throws IOException {
        int code = digestCode(digest);
        if (password == null || salt == null) {
            throw new IOException("A password and a salt are required");
        }
        if (iterations <= 0 || length <= 0) {
            throw new IOException("iterations and length must both be positive");
        }
        byte[] out = pbkdf2DigestImpl(code, password, salt, iterations, length);
        if (out == null) {
            throw new IOException("Key derivation failed");
        }
        return out;
    }

    /// Signs `data` with a private key in PKCS#8 DER, under one of [#RS256],
    /// [#RS384], [#RS512], [#PS256], [#ES256] and [#ES384].
    ///
    /// The key has to be of the kind the algorithm is defined over: an RSA key
    /// for the first four, a P-256 key for ES256, a P-384 key for ES384. Any
    /// other pairing is refused rather than adapted to.
    ///
    /// An ECDSA signature is returned as ASN.1 DER, the SEQUENCE of r and s both
    /// OpenSSL and the JDK produce. JOSE wants the two numbers side by side
    /// instead; `com.codename1.backend.security.crypto.Der` converts.
    ///
    /// @throws IOException when the key cannot be read, does not fit the
    /// algorithm, or signing fails
    public static byte[] sign(String algorithm, byte[] privateKey, byte[] data) throws IOException {
        int code = signatureCode(algorithm);
        if (privateKey == null || data == null) {
            throw new IOException("A key and data are required");
        }
        byte[] out = signImpl(code, privateKey, data);
        if (out == null) {
            throw new IOException("Could not sign with " + algorithm
                    + ": the key is not a PKCS#8 key of the kind that algorithm uses");
        }
        return out;
    }

    /// Checks a signature against a public key in SubjectPublicKeyInfo DER. The
    /// algorithms and the pairing of key and algorithm are those of [#sign], and
    /// an ECDSA signature is given as ASN.1 DER.
    ///
    /// @return true when `signature` is that key's signature of `data`; false
    /// when it is not, whatever is wrong with it
    /// @throws IOException when the question could not be asked: the key cannot
    /// be read or does not fit the algorithm. A key that is broken is never
    /// reported as a signature that is forged.
    public static boolean verify(String algorithm, byte[] publicKey, byte[] data, byte[] signature)
            throws IOException {
        int code = signatureCode(algorithm);
        if (publicKey == null || data == null) {
            throw new IOException("A key and data are required");
        }
        if (signature == null) {
            return false;
        }
        int answer = verifyImpl(code, publicKey, data, signature);
        if (answer < 0) {
            throw new IOException("Could not verify with " + algorithm
                    + ": the key is not a SubjectPublicKeyInfo of the kind that algorithm uses");
        }
        return answer == 1;
    }

    /// A new RSA private key as PKCS#8 DER, with the public exponent 65537.
    ///
    /// For a development profile, so a server that signs tokens starts without
    /// a key file. A key made at start-up is gone at the next one, and every
    /// token signed with it stops verifying: a deployed server loads its key.
    ///
    /// @param bits 2048 to 8192
    public static byte[] generateRsaKey(int bits) throws IOException {
        if (bits < 2048 || bits > 8192) {
            throw new IOException("An RSA key is 2048 to 8192 bits, not " + bits);
        }
        byte[] out = generateRsaKeyImpl(bits);
        if (out == null) {
            throw new IOException("Could not generate an RSA key");
        }
        return out;
    }

    /// AES-GCM. The result is the ciphertext followed by the 16 byte tag.
    ///
    /// The nonce must never repeat under one key: 12 bytes from [#randomBytes]
    /// for each call is the ordinary way, stored beside the result.
    ///
    /// @param key 16, 24 or 32 bytes
    /// @param iv the nonce; 12 bytes unless a protocol says otherwise
    /// @param aad data that is authenticated and not encrypted; null for none
    public static byte[] aesGcmEncrypt(byte[] key, byte[] iv, byte[] aad, byte[] plaintext)
            throws IOException {
        checkGcm(key, iv, plaintext, 0);
        byte[] out = aesGcmImpl(true, key, iv, aad, plaintext);
        if (out == null) {
            throw new IOException("AES-GCM encryption failed");
        }
        return out;
    }

    /// Opens what [#aesGcmEncrypt] sealed.
    ///
    /// @return the plaintext, or null when the tag does not match: the key, the
    /// nonce, the associated data or the sealed bytes are not the ones it was
    /// made with
    /// @throws IOException when the sizes are not ones AES-GCM has
    public static byte[] aesGcmDecrypt(byte[] key, byte[] iv, byte[] aad, byte[] sealed)
            throws IOException {
        checkGcm(key, iv, sealed, 16);
        return aesGcmImpl(false, key, iv, aad, sealed);
    }

    private static int digestCode(String digest) {
        if (SHA256.equals(digest)) {
            return 2;
        }
        if (SHA1.equals(digest)) {
            return 1;
        }
        if (SHA384.equals(digest)) {
            return 3;
        }
        if (SHA512.equals(digest)) {
            return 4;
        }
        throw new IllegalArgumentException("Not a supported digest: " + digest);
    }

    private static int signatureCode(String algorithm) throws IOException {
        if (RS256.equals(algorithm)) {
            return 1;
        }
        if (RS384.equals(algorithm)) {
            return 2;
        }
        if (RS512.equals(algorithm)) {
            return 3;
        }
        if (PS256.equals(algorithm)) {
            return 4;
        }
        if (ES256.equals(algorithm)) {
            return 5;
        }
        if (ES384.equals(algorithm)) {
            return 6;
        }
        throw new IOException("Not a supported signature algorithm: " + algorithm);
    }

    private static void checkGcm(byte[] key, byte[] iv, byte[] input, int minimum) throws IOException {
        if (key == null || iv == null || input == null) {
            throw new IOException("AES-GCM needs a key, a nonce and data");
        }
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IOException("An AES key is 16, 24 or 32 bytes, not " + key.length);
        }
        if (iv.length == 0) {
            throw new IOException("AES-GCM needs a nonce");
        }
        if (input.length < minimum) {
            throw new IOException("Too short to carry an AES-GCM tag");
        }
    }

    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length) throws IOException {
        byte[] out = pbkdf2Impl(password, salt, iterations, length);
        if (out == null) {
            throw new IOException("Key derivation failed");
        }
        return out;
    }

    static byte[] utf8(String value) {
        try {
            return value == null ? new byte[0] : value.getBytes("UTF-8");
        } catch (IOException err) {
            return new byte[0];
        }
    }

    private static String[] split(String value, char sep) {
        java.util.List parts = new java.util.ArrayList();
        int pos = 0;
        while (true) {
            int next = value.indexOf(sep, pos);
            if (next < 0) {
                parts.add(value.substring(pos));
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 1;
        }
        String[] out = new String[parts.size()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (String) parts.get(iter);
        }
        return out;
    }

    private static native byte[] sha256Impl(byte[] data);
    private static native byte[] sha1Impl(byte[] data);
    private static native byte[] md5Impl(byte[] data);
    private static native byte[] hmacSha256Impl(byte[] key, byte[] data);
    private static native byte[] pbkdf2Impl(byte[] password, byte[] salt, int iterations, int length);
    private static native byte[] randomBytesImpl(int length);
    private static native boolean equalsConstantTimeImpl(byte[] a, byte[] b);
    private static native byte[] digestImpl(int algorithm, byte[] data);
    private static native byte[] hmacImpl(int algorithm, byte[] key, byte[] data);
    private static native byte[] pbkdf2DigestImpl(int algorithm, byte[] password, byte[] salt,
                                                  int iterations, int length);
    private static native byte[] signImpl(int algorithm, byte[] pkcs8, byte[] data);
    private static native int verifyImpl(int algorithm, byte[] spki, byte[] data, byte[] signature);
    private static native byte[] generateRsaKeyImpl(int bits);
    private static native byte[] aesGcmImpl(boolean encrypt, byte[] key, byte[] iv, byte[] aad,
                                            byte[] input);
}
