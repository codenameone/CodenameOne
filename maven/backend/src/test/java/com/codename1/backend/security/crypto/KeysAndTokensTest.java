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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.security.Clock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Keys in and out of the forms they travel in -- PEM, DER, JSON Web Key --
/// against keys the openssl command line made, and the tokens built on HMAC.
class KeysAndTokensTest {
    // Read off the fixture keys by openssl: `rsa -pubin -noout -modulus` and
    // `ec -pubin -noout -text`, then base64url; the key ids are the base64url
    // of `openssl dgst -sha256 -binary` over each public key's DER.
    private static final String RSA_N = "sdSsKFlEhkmBlNA-y85NnS_1-rmDlPafhV8UVFdwHGiYPpKVC_frTgHiTFL"
            + "NbMyLhAPV2MywVTbHJxX9jiCxYvBluD9pyGJkkr5KEsClmpuV2EOSYBaVG8J3IbJwRoLUZ5sqSNySCqlqmVJ"
            + "3Lc5TM5FY_OuuKjcChg7-43oYqLe3BWVWruXIJVqNi05XIFymQUX_Y5kpJjmi-bclbvkmxZLHQO7Kd7hPwYe"
            + "lZBACkWp6grAcOEgkCAh1fhRrDyIc2oQycgn9KcTXDdrfLGM_5CNWWpQXWWhfobtj5LzZ0SamwER-pJjWMBA"
            + "YI6konz9ZA6PwgVgSDcKPmAqmzI8i4w";
    private static final String RSA_KID = "cEpgxcQJChnbSgernTjOoIauH2tk74bh0Bw8QVjNRx8";
    private static final String EC256_X = "9teOALB_mkUnrMHBMAXpTYWQk0KLoYJIcZHMCSPhQ78";
    private static final String EC256_Y = "DYbeVzyTCzxjWKvl-kwyvIQ4hgkduO8dpJR6p4en83Q";
    private static final String EC256_KID = "DubajPIkzbar2uwDIXPKCm7jEs6DbvO3CI3Ryk-ZNgY";
    private static final String EC384_X =
            "hYNsSel9dNE68LGZMpK6T61fi98bAk7w6uhgqgz7_H02K4SQu2Gg8LAW3lD7xdm-";
    private static final String EC384_Y =
            "XaghT48L2hXkcMMQQN5byhlFQzhXJqU2tOLJFs7Ky1m8WhwwVjC4z17_X_fjWbId";
    private static final String EC384_KID = "TbTGa30_WZUuUJa5M-veIYu-IoTnfjF2RV8Ue34fsWo";

    private static final byte[] MESSAGE = KeyFixtures.MESSAGE.getBytes(StandardCharsets.US_ASCII);

    private static byte[] der(String base64) {
        return Base64.decode(base64);
    }

    @Test
    @DisplayName("PEM: every form of private and public key openssl writes")
    void pem() throws Exception {
        assertArrayEquals(der(KeyFixtures.RSA_PKCS8_DER), KeyFiles.privateKey(KeyFixtures.RSA_PKCS8_PEM));
        // PKCS#1 wrapped here is byte for byte the PKCS#8 openssl wrapped.
        assertArrayEquals(der(KeyFixtures.RSA_PKCS8_DER), KeyFiles.privateKey(KeyFixtures.RSA_PKCS1_PEM));
        assertArrayEquals(der(KeyFixtures.RSA_PUBLIC_DER), KeyFiles.publicKey(KeyFixtures.RSA_PUBLIC_PEM));
        assertArrayEquals(der(KeyFixtures.EC256_PKCS8_DER),
                KeyFiles.privateKey(KeyFixtures.EC256_PKCS8_PEM));
        assertArrayEquals(der(KeyFixtures.EC256_PUBLIC_DER),
                KeyFiles.publicKey(KeyFixtures.EC256_PUBLIC_PEM));
        assertArrayEquals(der(KeyFixtures.EC384_PUBLIC_DER),
                KeyFiles.publicKey(KeyFixtures.EC384_PUBLIC_PEM));

        // SEC 1 wrapped here is a different envelope from openssl's, around the
        // same key: it signs what the public key verifies.
        String[][] sec1 = {{KeyFixtures.EC256_SEC1_PEM, KeyFixtures.EC256_PUBLIC_DER, Crypto.ES256},
            {KeyFixtures.EC384_SEC1_PEM, KeyFixtures.EC384_PUBLIC_DER, Crypto.ES384}};
        for (String[] c : sec1) {
            byte[] wrapped = KeyFiles.privateKey(c[0]);
            assertEquals(Der.EC, Der.privateKeyType(wrapped));
            assertArrayEquals(der(c[1]), Der.publicKeyOf(wrapped));
            assertTrue(Crypto.verify(c[2], der(c[1]), MESSAGE, Crypto.sign(c[2], wrapped, MESSAGE)));
        }
        // Windows line endings, and text around the block.
        String crlf = "Bag Attributes\r\n" + KeyFixtures.RSA_PKCS8_PEM.replace("\n", "\r\n") + "trailer";
        assertArrayEquals(der(KeyFixtures.RSA_PKCS8_DER), KeyFiles.privateKey(crlf));
    }

    @Test
    @DisplayName("PEM: what is refused, and what the message says to do about it")
    void pemRefusals() {
        String encrypted = "This private key is encrypted with a passphrase, which is not "
                + "supported. Write it without one -- `openssl pkcs8 -topk8 -nocrypt -in key.pem "
                + "-out key-plain.pem` -- and protect the file instead";
        assertEquals(encrypted, assertThrows(IOException.class,
                () -> KeyFiles.privateKey(KeyFixtures.RSA_ENCRYPTED_PKCS8_PEM)).getMessage());
        assertEquals(encrypted, assertThrows(IOException.class,
                () -> KeyFiles.privateKey(KeyFixtures.RSA_ENCRYPTED_PKCS1_PEM)).getMessage());
        assertEquals("Not a private key: the PEM block is a PUBLIC KEY. A private key is a "
                + "PRIVATE KEY, an RSA PRIVATE KEY or an EC PRIVATE KEY",
                assertThrows(IOException.class,
                        () -> KeyFiles.privateKey(KeyFixtures.RSA_PUBLIC_PEM)).getMessage());
        assertEquals("Not a public key: the PEM block is a PRIVATE KEY. A public key is a PUBLIC "
                + "KEY, as `openssl pkey -pubout` writes one", assertThrows(IOException.class,
                        () -> KeyFiles.publicKey(KeyFixtures.RSA_PKCS8_PEM)).getMessage());
        assertEquals("This is a certificate, not a public key. Extract the key with `openssl x509 "
                + "-pubkey -noout -in cert.pem`", assertThrows(IOException.class,
                        () -> KeyFiles.publicKey("-----BEGIN CERTIFICATE-----\nAAAA\n"
                                + "-----END CERTIFICATE-----\n")).getMessage());
        assertEquals("Not PEM: no -----BEGIN line. A key file starts with a line such as "
                + "-----BEGIN PRIVATE KEY-----", assertThrows(IOException.class,
                        () -> KeyFiles.privateKey("MIIEvgIBADANBg")).getMessage());
        assertEquals("Not PEM: no -----END PRIVATE KEY----- line", assertThrows(IOException.class,
                () -> KeyFiles.privateKey("-----BEGIN PRIVATE KEY-----\nMIIE\n")).getMessage());
        assertEquals("Not PEM: the block's content is not base64", assertThrows(IOException.class,
                () -> KeyFiles.privateKey("-----BEGIN PRIVATE KEY-----\n!!!!\n"
                        + "-----END PRIVATE KEY-----\n")).getMessage());
        // Base64 that is not a key.
        assertThrows(IOException.class, () -> KeyFiles.privateKey(
                "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n"));
        assertThrows(IOException.class, () -> KeyFiles.publicKey(
                "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----\n"));
    }

    @Test
    @DisplayName("A key file is read from disk, with or without file: in front")
    void files() throws Exception {
        Path dir = Files.createTempDirectory("keyfiles");
        Path file = dir.resolve("signing.pem");
        Files.write(file, KeyFixtures.RSA_PKCS1_PEM.getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(der(KeyFixtures.RSA_PKCS8_DER), KeyFiles.readPrivateKey(file.toString()));
        assertArrayEquals(der(KeyFixtures.RSA_PKCS8_DER),
                KeyFiles.readPrivateKey("file:" + file));
        Files.write(file, KeyFixtures.EC256_PUBLIC_PEM.getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(der(KeyFixtures.EC256_PUBLIC_DER), KeyFiles.readPublicKey(file.toString()));
        String missing = dir.resolve("absent.pem").toString();
        assertEquals("Could not open the key file " + missing, assertThrows(IOException.class,
                () -> KeyFiles.readPrivateKey(missing)).getMessage());
        assertEquals("A key is read from a file, not from the classpath: classpath:key.pem. Name "
                + "a path on disk", assertThrows(IOException.class,
                        () -> KeyFiles.readPublicKey("classpath:key.pem")).getMessage());
    }

    @Test
    @DisplayName("DER: an RSA key to and from its numbers, and out of its private key")
    void rsaDer() throws Exception {
        byte[] spki = der(KeyFixtures.RSA_PUBLIC_DER);
        assertEquals(Der.RSA, Der.publicKeyType(spki));
        assertEquals(Der.RSA, Der.privateKeyType(der(KeyFixtures.RSA_PKCS8_DER)));
        assertEquals(2048, Der.rsaModulusBits(spki));
        byte[][] parts = Der.rsaPublicKeyParts(spki);
        assertEquals(RSA_N, Base64Url.encode(parts[0]));
        assertEquals("AQAB", Base64Url.encode(parts[1]));
        // Built from the numbers alone, it is the file openssl wrote.
        assertArrayEquals(spki, Der.rsaPublicKey(Base64Url.decode(RSA_N), Base64Url.decode("AQAB")));
        // Leading zeros on the way in change nothing.
        byte[] padded = new byte[parts[0].length + 2];
        System.arraycopy(parts[0], 0, padded, 2, parts[0].length);
        assertArrayEquals(spki, Der.rsaPublicKey(padded, new byte[] {0, 1, 0, 1}));
        assertArrayEquals(spki, Der.publicKeyOf(der(KeyFixtures.RSA_PKCS8_DER)));

        assertEquals("An RSA public key needs a modulus and an exponent",
                assertThrows(IOException.class, () -> Der.rsaPublicKey(new byte[0], new byte[] {1}))
                        .getMessage());
        assertEquals("Not an RSA public key", assertThrows(IOException.class,
                () -> Der.rsaPublicKeyParts(der(KeyFixtures.EC256_PUBLIC_DER))).getMessage());
        byte[] truncated = new byte[spki.length - 20];
        System.arraycopy(spki, 0, truncated, 0, truncated.length);
        assertEquals("Truncated DER", assertThrows(IOException.class,
                () -> Der.rsaPublicKeyParts(truncated)).getMessage());
        assertThrows(IOException.class, () -> Der.publicKeyType(new byte[] {0x30, (byte) 0x84, 1, 2}));
        assertThrows(IOException.class, () -> Der.publicKeyType(new byte[0]));
        assertThrows(IOException.class, () -> Der.publicKeyType(null));
    }

    @Test
    @DisplayName("DER: EC keys on both curves to and from their coordinates")
    void ecDer() throws Exception {
        String[][] cases = {
            {KeyFixtures.EC256_PUBLIC_DER, KeyFixtures.EC256_PKCS8_DER, Der.P256, EC256_X, EC256_Y, "32"},
            {KeyFixtures.EC384_PUBLIC_DER, KeyFixtures.EC384_PKCS8_DER, Der.P384, EC384_X, EC384_Y, "48"}};
        for (String[] c : cases) {
            byte[] spki = der(c[0]);
            assertEquals(Der.EC, Der.publicKeyType(spki));
            assertEquals(c[2], Der.ecCurve(spki));
            assertEquals(Integer.parseInt(c[5]), Der.ecCoordinateLength(c[2]));
            byte[][] parts = Der.ecPublicKeyParts(spki);
            assertEquals(c[3], Base64Url.encode(parts[0]));
            assertEquals(c[4], Base64Url.encode(parts[1]));
            assertArrayEquals(spki, Der.ecPublicKey(c[2], Base64Url.decode(c[3]), Base64Url.decode(c[4])));
            assertArrayEquals(spki, Der.publicKeyOf(der(c[1])));
        }
        assertEquals("A P-256 public key has two coordinates of 32 bytes each",
                assertThrows(IOException.class, () -> Der.ecPublicKey(Der.P256,
                        Base64Url.decode(EC384_X), Base64Url.decode(EC384_Y))).getMessage());
        assertEquals("Not a supported curve: P-521. The curves are P-256 and P-384",
                assertThrows(IOException.class, () -> Der.ecPublicKey("P-521", new byte[66],
                        new byte[66])).getMessage());
        // A private key written by the JDK leaves its public half out.
        java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("EC");
        generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        java.security.KeyPair pair = generator.generateKeyPair();
        byte[] bare = pair.getPrivate().getEncoded();
        if (bare.length < 100) {
            assertEquals("This EC private key does not carry its public key. Give the public key "
                    + "as well, or write the private key again with `openssl pkcs8 -topk8 "
                    + "-nocrypt`, which includes it", assertThrows(IOException.class,
                            () -> Der.publicKeyOf(bare)).getMessage());
        }
        // And is usable with it given beside.
        Jwk both = Jwk.ofKeyPair(bare, pair.getPublic().getEncoded());
        assertTrue(Crypto.verify(Crypto.ES256, both.getPublicKey(), MESSAGE,
                Crypto.sign(Crypto.ES256, both.getPrivateKey(), MESSAGE)));
    }

    @Test
    @DisplayName("ECDSA signatures between DER and the r||s of a JSON Web Signature")
    void ecdsaForms() throws Exception {
        byte[] theirs = der(KeyFixtures.ES256_SIGNATURE);
        byte[] jose = Der.ecdsaDerToJose(theirs, 32);
        assertEquals(64, jose.length);
        // DER is canonical, so the way back is the bytes openssl wrote.
        assertArrayEquals(theirs, Der.ecdsaJoseToDer(jose));
        byte[] theirs384 = der(KeyFixtures.ES384_SIGNATURE);
        assertEquals(96, Der.ecdsaDerToJose(theirs384, 48).length);
        assertArrayEquals(theirs384, Der.ecdsaJoseToDer(Der.ecdsaDerToJose(theirs384, 48)));

        // A number with its top bit set gets a zero in front in DER and loses it
        // again; a short one is padded on the left.
        byte[] rs = new byte[64];
        rs[0] = (byte) 0x80;
        rs[63] = 0x01;
        byte[] encoded = Der.ecdsaJoseToDer(rs);
        assertEquals("30260221008000000000000000000000000000000000000000000000000000000000000000"
                + "020101", CryptoPrimitivesTest.hex(encoded));
        assertArrayEquals(rs, Der.ecdsaDerToJose(encoded, 32));

        assertEquals("Not an ECDSA signature of that curve", assertThrows(IOException.class,
                () -> Der.ecdsaDerToJose(theirs384, 32)).getMessage());
        assertEquals("Not an ECDSA signature: two numbers of equal length",
                assertThrows(IOException.class, () -> Der.ecdsaJoseToDer(new byte[63])).getMessage());
        assertThrows(IOException.class, () -> Der.ecdsaDerToJose(new byte[] {1, 2, 3}, 32));
        // Trailing bytes after the two numbers are not a signature.
        byte[] trailing = Der.ecdsaJoseToDer(rs);
        trailing[1] += 2;
        byte[] longer = new byte[trailing.length + 2];
        System.arraycopy(trailing, 0, longer, 0, trailing.length);
        longer[trailing.length] = 0x05;
        assertThrows(IOException.class, () -> Der.ecdsaDerToJose(longer, 32));
    }

    @Test
    @DisplayName("JWK: RSA and EC public keys to JSON and back, with the SHA-256 key id")
    @SuppressWarnings("unchecked")
    void jwk() throws Exception {
        Jwk rsa = Jwk.ofPrivateKey(der(KeyFixtures.RSA_PKCS8_DER));
        assertEquals(RSA_KID, rsa.getKeyId());
        assertTrue(rsa.isPrivate());
        Map<String, Object> json = rsa.toPublicJson();
        assertEquals("{\"kty\":\"RSA\",\"kid\":\"" + RSA_KID + "\",\"n\":\"" + RSA_N
                + "\",\"e\":\"AQAB\"}", Json.write(json));
        Jwk parsed = Jwk.parse(json);
        assertFalse(parsed.isPrivate());
        assertArrayEquals(der(KeyFixtures.RSA_PUBLIC_DER), parsed.getPublicKey());
        assertEquals(RSA_KID, parsed.getKeyId());

        Jwk p256 = Jwk.ofPem(KeyFixtures.EC256_PKCS8_PEM).withUse("sig").withAlgorithm("ES256");
        assertEquals("{\"kty\":\"EC\",\"use\":\"sig\",\"alg\":\"ES256\",\"kid\":\"" + EC256_KID
                + "\",\"crv\":\"P-256\",\"x\":\"" + EC256_X + "\",\"y\":\"" + EC256_Y + "\"}",
                Json.write(p256.toPublicJson()));
        Jwk p384 = Jwk.ofPem(KeyFixtures.EC384_PUBLIC_PEM);
        assertEquals(EC384_KID, p384.getKeyId());
        assertEquals(Der.P384, p384.getCurve());
        assertFalse(p384.isPrivate());

        // A set: what a jwks_uri serves, and what comes back from one.
        JwkSet set = JwkSet.of(rsa.withKeyId("2026-10"), p256, p384, Jwk.ofSecret(new byte[32]));
        String published = set.toJson();
        assertFalse(published.contains("\"d\""), published);
        assertFalse(published.contains("oct"), published);
        JwkSet read = JwkSet.parse(published);
        assertEquals(3, read.getKeys().size());
        assertArrayEquals(der(KeyFixtures.RSA_PUBLIC_DER), read.get("2026-10").getPublicKey());
        assertArrayEquals(der(KeyFixtures.EC256_PUBLIC_DER), read.get(EC256_KID).getPublicKey());
        assertEquals("ES256", read.get(EC256_KID).getAlgorithm());
        assertEquals("sig", read.get(EC256_KID).getUse());
        assertNull(read.get("another"));

        // A set from somewhere else: keys this runtime has no use for are left
        // out, and the ones it has are kept.
        JwkSet mixed = JwkSet.parse("{\"keys\":[{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"AAAA\"},"
                + "{\"kty\":\"EC\",\"crv\":\"P-521\",\"x\":\"AAAA\",\"y\":\"AAAA\"},"
                + "{\"kty\":\"RSA\",\"use\":\"enc\",\"n\":\"" + RSA_N + "\",\"e\":\"AQAB\"},"
                + "{\"kty\":\"RSA\",\"n\":\"!!\",\"e\":\"AQAB\"}, 7,"
                + "{\"kty\":\"RSA\",\"kid\":\"good\",\"d\":\"c2VjcmV0\",\"n\":\"" + RSA_N
                + "\",\"e\":\"AQAB\"}]}");
        assertEquals(1, mixed.getKeys().size());
        assertEquals("good", mixed.getKeys().get(0).getKeyId());
        assertFalse(mixed.getKeys().get(0).isPrivate());

        assertEquals("Not a JSON Web Key Set: it has no \"keys\" array",
                assertThrows(IOException.class, () -> JwkSet.parse("{}")).getMessage());
        assertThrows(IOException.class, () -> JwkSet.parse("not json"));
        assertEquals("Not a supported JSON Web Key type: oct. The types are RSA and EC",
                assertThrows(IOException.class, () -> Jwk.parse((Map<String, Object>) (Map)
                        Json.parseObject("{\"kty\":\"oct\",\"k\":\"AAAA\"}"))).getMessage());
        assertEquals("The JSON Web Key has no usable \"e\"", assertThrows(IOException.class,
                () -> Jwk.parse((Map<String, Object>) (Map) Json.parseObject("{\"kty\":\"RSA\",\"n\":\""
                        + RSA_N + "\"}"))).getMessage());
        assertEquals("The private key and the public key are of different kinds",
                assertThrows(IOException.class, () -> Jwk.ofKeyPair(der(KeyFixtures.RSA_PKCS8_DER),
                        der(KeyFixtures.EC256_PUBLIC_DER))).getMessage());
        assertThrows(IllegalStateException.class, () -> Jwk.ofSecret(new byte[32]).toPublicJson());
    }

    /// A clock a test moves.
    static final class Moving implements Clock {
        long now = 1700000000000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    @Test
    @DisplayName("Signed tokens: purpose, expiry, fingerprint, and nothing forged")
    void signedTokens() throws Exception {
        byte[] secret = new byte[32];
        for (int iter = 0 ; iter < secret.length ; iter++) {
            secret[iter] = (byte) (iter * 7 + 1);
        }
        SignedTokens tokens = new SignedTokens(secret);
        Moving clock = new Moving();
        tokens.setClock(clock);

        String token = tokens.create("email-verify", "user:42", 600);
        // Fixed inputs, fixed token: HMAC is deterministic.
        assertEquals(token, tokens.create("email-verify", "user:42", 600));
        assertEquals("user:42", tokens.verify("email-verify", token));
        assertEquals("user:42", SignedTokens.subject(token));
        // Another purpose signed by the same secret is another token.
        assertNull(tokens.verify("password-reset", token));
        // It was made without a fingerprint, so none verifies it with one.
        assertNull(tokens.verify("email-verify", token, "ada@example.com"));
        assertNull(tokens.verify("email-verify", token, ""));

        clock.now += 599000;
        assertEquals("user:42", tokens.verify("email-verify", token));
        clock.now += 1000;
        assertNull(tokens.verify("email-verify", token), "expired at exactly its lifetime");
        clock.now -= 600000;

        // The fingerprint: valid until what it was taken of changes.
        String reset = tokens.create("password-reset", "ada", 3600, "{bcrypt}$2a$10$old");
        assertEquals("ada", tokens.verify("password-reset", reset, "{bcrypt}$2a$10$old"));
        assertNull(tokens.verify("password-reset", reset, "{bcrypt}$2a$10$new"));
        assertNull(tokens.verify("password-reset", reset));
        assertNull(tokens.verify("password-reset", reset, null));

        // Tampering: the subject, the expiry, the signature.
        int dot = token.indexOf('.');
        String forgedPayload = Base64Url.encode("9999999999:user:1".getBytes(StandardCharsets.UTF_8));
        assertNull(tokens.verify("email-verify", forgedPayload + token.substring(dot)));
        assertNull(tokens.verify("email-verify", token.substring(0, dot + 1)
                + Base64Url.encode(new byte[32])));
        assertNull(tokens.verify("email-verify", token + "A"));
        assertNull(tokens.verify("email-verify", token.substring(0, dot)));
        assertNull(tokens.verify("email-verify", token + ".x"));
        assertNull(tokens.verify("email-verify", ""));
        assertNull(tokens.verify("email-verify", null));
        assertNull(tokens.verify(null, token));
        assertNull(SignedTokens.subject("nonsense"));
        // Another secret.
        byte[] other = secret.clone();
        other[0] ^= 1;
        SignedTokens others = new SignedTokens(other);
        others.setClock(clock);
        assertNull(others.verify("email-verify", token));
        assertNotEquals(token, others.create("email-verify", "user:42", 600));
        // Field boundaries are signed: moving a character between the purpose
        // and the subject makes a different token.
        assertNotEquals(tokens.create("ab", "c", 600).substring(tokens.create("ab", "c", 600)
                .indexOf('.')), tokens.create("a", "bc", 600).substring(tokens.create("a", "bc", 600)
                .indexOf('.')));

        assertEquals("The signing secret must be at least 32 bytes", assertThrows(
                IllegalArgumentException.class, () -> new SignedTokens(new byte[31])).getMessage());
        assertEquals("A token needs a positive lifetime", assertThrows(
                IllegalArgumentException.class, () -> tokens.create("p", "s", 0)).getMessage());
    }

    @Test
    @DisplayName("Spring's {pbkdf2} hashes verify: its own documentation's, and ones 6.4 wrote")
    void springPbkdf2() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // The example in the Spring Security reference, for "password".
        String documented = "{pbkdf2}5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4e"
                + "d4d6b99ca763d8dc";
        assertTrue(encoder.matches("password", documented));
        assertFalse(encoder.matches("Password", documented));
        assertTrue(encoder.upgradeEncoding(documented), "an older scheme is re-encoded on sign-in");
        // Written by spring-security-crypto 6.4.4 for a password that is not ASCII.
        String password = "p\u00e4ssword";
        String v55 = "{pbkdf2}8025389de04d342c376027189064f478268cfc8d7f25d423761ae9e069a5ceea4491c3c4"
                + "f7dc621d";
        String v58 = "{pbkdf2@SpringSecurity_v5_8}beccbc5a992abfdbe1204d108a798498144110429e43b1603480"
                + "da558ae0cb919b18ea38944038e582a93d599a6fc86b";
        assertTrue(encoder.matches(password, v55));
        assertTrue(encoder.matches(password, v58));
        assertFalse(encoder.matches("password", v55));
        assertFalse(encoder.matches("password", v58));
        // One scheme's hash under the other's id does not verify.
        assertFalse(encoder.matches(password, "{pbkdf2}" + v58.substring(v58.indexOf('}') + 1)));

        // A secret, SHA-512, and both encodings: new Pbkdf2PasswordEncoder("pepper", 16, 1000,
        // PBKDF2WithHmacSHA512).
        Pbkdf2PasswordEncoder custom = new Pbkdf2PasswordEncoder("pepper", 16, 1000, Crypto.SHA512);
        assertTrue(custom.matches("password", "728f87695a5ce6cabec6fe35b3cfef500991275929396fd986b98e"
                + "bdb2503bd5a59f27e8fea19d29833618a4f55e8b9bad9dbb0a1ea0234a8e83743db3aaf3f533be8814ee"
                + "6febb709244e2971b79a51"));
        assertFalse(new Pbkdf2PasswordEncoder("", 16, 1000, Crypto.SHA512).matches("password",
                "728f87695a5ce6cabec6fe35b3cfef500991275929396fd986b98ebdb2503bd5a59f27e8fea19d2983"
                + "3618a4f55e8b9bad9dbb0a1ea0234a8e83743db3aaf3f533be8814ee6febb709244e2971b79a51"));
        custom.setEncodeHashAsBase64(true);
        assertTrue(custom.matches("password", "2MkqMRCJRqTxpqPVr/tFetcK7mUK9yqeIeh/W5DJqlhGjYeEfNlmeG"
                + "kc4i8/1f2sjl8ciBZW41wLkMddTaQ+52wb4YyYDOv+JgzS5t2WhR8="));
        // What it writes, it reads; and no two encodings of a password are alike.
        String written = custom.encode("s3cret");
        assertNotEquals(written, custom.encode("s3cret"));
        assertTrue(custom.matches("s3cret", written));
        assertFalse(custom.matches("s3cres", written));
        // Not a hash at all.
        assertFalse(custom.matches("password", "not base64 !"));
        assertFalse(custom.matches("password", ""));
        assertFalse(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches("password", "abc"));
        assertFalse(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches("password",
                "zz923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4ed4d6b99ca763d8dc"));
        assertEquals("The digest is SHA-1, SHA-256 or SHA-512, not MD5", assertThrows(
                IllegalArgumentException.class,
                () -> new Pbkdf2PasswordEncoder("", 16, 1000, "MD5")).getMessage());
    }
}
