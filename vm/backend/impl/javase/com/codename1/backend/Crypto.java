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
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/// Java SE twin of Crypto, on the JDK's own providers.
///
/// Same rule as the translated one: nothing is implemented by hand. The
/// constant-time compare is MessageDigest.isEqual, which the JDK documents as not
/// short-circuiting; a loop written here would let the optimizer decide, and an
/// early exit on the first differing byte lets a MAC be forged a byte at a time.
public final class Crypto {
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
    private static final SecureRandom RANDOM = new SecureRandom();

    private Crypto() {
    }

    public static byte[] sha256(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception err) {
            return null;
        }
    }

    /// PBKDF2-HMAC-SHA-256. Exposed because SCRAM-SHA-256 -- how PostgreSQL
    /// authenticates by default -- is defined in terms of it with the server's
    /// iteration count, which [#hashPassword] does not let a caller choose.
    public static byte[] pbkdf2Sha256(byte[] password, byte[] salt, int iterations, int length)
            throws IOException {
        return pbkdf2(password, salt, iterations, length);
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
        return digest("SHA-1", data);
    }

    /// MD5, for PostgreSQL's md5 authentication method. See [#sha1].
    public static byte[] md5(byte[] data) {
        return digest("MD5", data);
    }

    private static byte[] digest(String algorithm, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return MessageDigest.getInstance(algorithm).digest(data);
        } catch (java.security.NoSuchAlgorithmException err) {
            throw new IllegalStateException(algorithm + " is not available", err);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        if (key == null || data == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException | RuntimeException err) {
            // The provider's checked failures and SecretKeySpec's refusal of an
            // empty key alike: the native arm answers null for both.
            return null;
        }
    }

    public static byte[] randomBytes(int length) throws IOException {
        if (length <= 0) {
            throw new IOException("No secure randomness available");
        }
        byte[] out = new byte[length];
        RANDOM.nextBytes(out);
        return out;
    }

    public static boolean equalsConstantTime(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }

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
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$" + Base64Url.encode(salt)
                + "$" + Base64Url.encode(hash);
    }

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
        try {
            return equalsConstantTime(expected, pbkdf2(utf8(password), salt, iterations, expected.length));
        } catch (IOException err) {
            return false;
        }
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
        return digest("SHA-384", data);
    }

    /// SHA-512; null for null.
    public static byte[] sha512(byte[] data) {
        return digest("SHA-512", data);
    }

    /// HMAC over one of [#SHA1], [#SHA256], [#SHA384] and [#SHA512]. SHA-1 is
    /// here for what specifies it by name -- a TOTP secret an authenticator
    /// application already holds, Spring's oldest password format -- and not for
    /// anything new.
    ///
    /// @return the tag, or null when `key` or `data` is null
    /// @throws IllegalArgumentException for a digest that is not one of the four
    public static byte[] hmac(String digest, byte[] key, byte[] data) {
        String algorithm = MAC_NAMES[digestCode(digest)];
        if (key == null || data == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(algorithm);
            // An empty key and a single zero byte are the same HMAC key -- it is
            // padded with zeros to the block size -- and SecretKeySpec refuses
            // the first, which OpenSSL takes.
            mac.init(new SecretKeySpec(key.length == 0 ? new byte[1] : key, algorithm));
            return mac.doFinal(data);
        } catch (GeneralSecurityException err) {
            throw new IllegalStateException(algorithm + " is not available", err);
        }
    }

    /// PBKDF2 over HMAC with one of [#SHA1], [#SHA256], [#SHA384] and [#SHA512],
    /// on the password's bytes as they are given.
    ///
    /// @throws IOException when a count is not positive or derivation fails
    public static byte[] pbkdf2(String digest, byte[] password, byte[] salt, int iterations,
                                int length) throws IOException {
        String algorithm = MAC_NAMES[digestCode(digest)];
        if (password == null || salt == null) {
            throw new IOException("A password and a salt are required");
        }
        return pbkdf2Mac(algorithm, password, salt, iterations, length);
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
        try {
            PrivateKey key = KeyFactory.getInstance(code >= 5 ? "EC" : "RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            requireCurve(key, code);
            Signature signer = signature(code);
            signer.initSign(key);
            signer.update(data);
            return signer.sign();
        } catch (GeneralSecurityException | RuntimeException err) {
            throw new IOException("Could not sign with " + algorithm
                    + ": the key is not a PKCS#8 key of the kind that algorithm uses", err);
        }
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
        Signature verifier;
        try {
            PublicKey key = KeyFactory.getInstance(code >= 5 ? "EC" : "RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKey));
            requireCurve(key, code);
            verifier = signature(code);
            verifier.initVerify(key);
            verifier.update(data);
        } catch (GeneralSecurityException | RuntimeException err) {
            throw new IOException("Could not verify with " + algorithm
                    + ": the key is not a SubjectPublicKeyInfo of the kind that algorithm uses",
                    err);
        }
        try {
            return verifier.verify(signature);
        } catch (SignatureException | RuntimeException malformed) {
            // Bytes that are not a signature at all -- the wrong length, DER that
            // does not parse -- are a signature that does not verify.
            return false;
        }
    }

    private static Signature signature(int code) throws GeneralSecurityException {
        switch (code) {
            case 1: return Signature.getInstance("SHA256withRSA");
            case 2: return Signature.getInstance("SHA384withRSA");
            case 3: return Signature.getInstance("SHA512withRSA");
            case 4:
                // Stock Java 8 has RSASSA-PSS since 8u251 (JDK-8146293).
                // CryptoPrimitivesTest verifies OpenSSL PS256 vectors on our Java 8
                // toolchain; no external provider or newer Java API is required.
                Signature pss = Signature.getInstance("RSASSA-PSS");
                pss.setParameter(new PSSParameterSpec("SHA-256", "MGF1",
                        MGF1ParameterSpec.SHA256, 32, 1));
                return pss;
            case 5: return Signature.getInstance("SHA256withECDSA");
            default: return Signature.getInstance("SHA384withECDSA");
        }
    }

    /// The JDK signs with whatever curve an EC key is on; ES256 is P-256 and
    /// ES384 is P-384 and nothing else.
    private static void requireCurve(Key key, int code) throws GeneralSecurityException {
        if (code < 5) {
            return;
        }
        if (!(key instanceof ECKey)) {
            throw new InvalidKeyException("Not an EC key");
        }
        AlgorithmParameters named = AlgorithmParameters.getInstance("EC");
        named.init(new ECGenParameterSpec(code == 5 ? "secp256r1" : "secp384r1"));
        ECParameterSpec want = named.getParameterSpec(ECParameterSpec.class);
        ECParameterSpec have = ((ECKey) key).getParams();
        if (!have.getCurve().equals(want.getCurve()) || !have.getGenerator().equals(want.getGenerator())
                || !have.getOrder().equals(want.getOrder())
                || have.getCofactor() != want.getCofactor()) {
            throw new InvalidKeyException("The key is not on the curve of the algorithm");
        }
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
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bits, RANDOM);
            return generator.generateKeyPair().getPrivate().getEncoded();
        } catch (GeneralSecurityException | RuntimeException err) {
            throw new IOException("Could not generate an RSA key", err);
        }
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
        try {
            return gcm(Cipher.ENCRYPT_MODE, key, iv, aad).doFinal(plaintext);
        } catch (GeneralSecurityException | RuntimeException err) {
            throw new IOException("AES-GCM encryption failed", err);
        }
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
        Cipher cipher;
        try {
            cipher = gcm(Cipher.DECRYPT_MODE, key, iv, aad);
        } catch (GeneralSecurityException | RuntimeException err) {
            throw new IOException("AES-GCM decryption failed", err);
        }
        try {
            return cipher.doFinal(sealed);
        } catch (GeneralSecurityException | RuntimeException mismatch) {
            // AEADBadTagException: with the sizes checked it is the one failure left.
            return null;
        }
    }

    private static Cipher gcm(int mode, byte[] key, byte[] iv, byte[] aad)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }
        return cipher;
    }

    /// The JDK's name for HMAC over each digest, by [#digestCode].
    private static final String[] MAC_NAMES = {null, "HmacSHA1", "HmacSHA256", "HmacSHA384",
        "HmacSHA512"};

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

    /// PBKDF2-HMAC-SHA256 over the password BYTES, per RFC 8018.
    ///
    /// Computed here rather than through PBEKeySpec, which takes chars and leaves the
    /// encoding to the provider: for PBKDF2WithHmacSHA256 that encoding is UTF-8, so
    /// a byte of 0xc3 handed over as a char came back out as TWO bytes. Mapping the
    /// UTF-8 bytes to chars first therefore did not preserve them -- it re-encoded
    /// them -- and the derived key stopped matching the native side, which passes the
    /// original octets to OpenSSL. The effect was confined to non-ASCII passwords: a
    /// hash written by one runtime that no longer verifies on the other, and a
    /// PostgreSQL SCRAM proof that simply does not authenticate.
    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length)
            throws IOException {
        return pbkdf2Mac("HmacSHA256", password, salt, iterations, length);
    }

    private static byte[] pbkdf2Mac(String macName, byte[] password, byte[] salt, int iterations,
                                    int length) throws IOException {
        // The native arm refuses these outright (cn1_backend_crypto.c), and this one
        // did not: a non-positive iteration count ran the loop zero extra times and
        // returned the ONE-ROUND result, so a misconfigured SCRAM or key derivation
        // produced a weak key that looked like it worked -- locally only, and with a
        // different key from the one the packaged server would derive.
        if (iterations <= 0 || length <= 0) {
            throw new IOException("iterations and length must both be positive");
        }
        try {
            Mac mac = Mac.getInstance(macName);
            // SecretKeySpec rejects a zero-length key. HMAC pads the key to the block
            // size with zeros, so a single zero byte and an empty key are the same
            // key -- the substitution is exact rather than a workaround.
            mac.init(new SecretKeySpec(password.length == 0 ? new byte[1] : password,
                    macName));
            int hLen = mac.getMacLength();
            byte[] out = new byte[length];
            byte[] counted = new byte[salt.length + 4];
            System.arraycopy(salt, 0, counted, 0, salt.length);
            int done = 0;
            for (int block = 1 ; done < length ; block++) {
                counted[salt.length] = (byte) (block >>> 24);
                counted[salt.length + 1] = (byte) (block >>> 16);
                counted[salt.length + 2] = (byte) (block >>> 8);
                counted[salt.length + 3] = (byte) block;
                byte[] u = mac.doFinal(counted);
                byte[] t = new byte[hLen];
                System.arraycopy(u, 0, t, 0, hLen);
                for (int round = 1 ; round < iterations ; round++) {
                    u = mac.doFinal(u);
                    for (int iter = 0 ; iter < hLen ; iter++) {
                        t[iter] ^= u[iter];
                    }
                }
                int take = length - done < hLen ? length - done : hLen;
                System.arraycopy(t, 0, out, done, take);
                done += take;
            }
            return out;
        } catch (GeneralSecurityException | RuntimeException err) {
            throw new IOException("Key derivation failed", err);
        }
    }

    static byte[] utf8(String value) {
        try {
            return value == null ? new byte[0] : value.getBytes("UTF-8");
        } catch (IOException err) {
            return new byte[0];
        }
    }

    private static String[] split(String value, char sep) {
        List<String> parts = new ArrayList<String>();
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
        return parts.toArray(new String[parts.size()]);
    }
}
