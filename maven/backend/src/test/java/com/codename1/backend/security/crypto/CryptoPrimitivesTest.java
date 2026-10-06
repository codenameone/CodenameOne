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
import com.codename1.backend.Crypto;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The runtime's primitives against the vectors their standards publish, and
/// against signatures the openssl command line made.
class CryptoPrimitivesTest {
    static byte[] hex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) Integer.parseInt(hex.substring(iter * 2, iter * 2 + 2), 16);
        }
        return out;
    }

    static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    static byte[] ascii(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static byte[] repeat(int value, int count) {
        byte[] out = new byte[count];
        java.util.Arrays.fill(out, (byte) value);
        return out;
    }

    private static final byte[] MESSAGE = ascii(KeyFixtures.MESSAGE);

    @Test
    @DisplayName("SHA-384 and SHA-512 of the FIPS 180 messages")
    void digests() {
        assertEquals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed"
                + "8086072ba1e7cc2358baeca134c825a7", hex(Crypto.sha384(ascii("abc"))));
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
                hex(Crypto.sha512(ascii("abc"))));
        assertEquals("38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da"
                + "274edebfe76f65fbd51ad2f14898b95b", hex(Crypto.sha384(new byte[0])));
        assertNull(Crypto.sha384(null));
        assertNull(Crypto.sha512(null));
    }

    @Test
    @DisplayName("HMAC: RFC 2202 for SHA-1, RFC 4231 cases 1, 2 and 6 for the SHA-2 family")
    void hmac() {
        // RFC 2202 3, cases 1 and 2.
        assertEquals("b617318655057264e28bc0b6fb378c8ef146be00",
                hex(Crypto.hmac(Crypto.SHA1, repeat(0x0b, 20), ascii("Hi There"))));
        assertEquals("effcdf6ae5eb2fa2d27416d5f184df9c259a7c79",
                hex(Crypto.hmac(Crypto.SHA1, ascii("Jefe"), ascii("what do ya want for nothing?"))));
        // RFC 4231 4.2.
        byte[] key = repeat(0x0b, 20);
        byte[] data = ascii("Hi There");
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
                hex(Crypto.hmac(Crypto.SHA256, key, data)));
        assertEquals("afd03944d84895626b0825f4ab46907f15f9dadbe4101ec682aa034c7cebc59c"
                + "faea9ea9076ede7f4af152e8b2fa9cb6", hex(Crypto.hmac(Crypto.SHA384, key, data)));
        assertEquals("87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cde"
                + "daa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854",
                hex(Crypto.hmac(Crypto.SHA512, key, data)));
        // RFC 4231 4.3.
        key = ascii("Jefe");
        data = ascii("what do ya want for nothing?");
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                hex(Crypto.hmac(Crypto.SHA256, key, data)));
        assertEquals("af45d2e376484031617f78d2b58a6b1b9c7ef464f5a01b47e42ec3736322445e"
                + "8e2240ca5e69e2c78b3239ecfab21649", hex(Crypto.hmac(Crypto.SHA384, key, data)));
        assertEquals("164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554"
                + "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737",
                hex(Crypto.hmac(Crypto.SHA512, key, data)));
        // RFC 4231 4.7: a key longer than the block.
        key = repeat(0xaa, 131);
        data = ascii("Test Using Larger Than Block-Size Key - Hash Key First");
        assertEquals("60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
                hex(Crypto.hmac(Crypto.SHA256, key, data)));
        assertEquals("80b24263c7c1a3ebb71493c1dd7be8b49b46d1f41b4aeec1121b013783f8f352"
                + "6b56d037e05f2598bd0fd2215d6a1e5295e64f73f63f0aec8b915a985d786598",
                hex(Crypto.hmac(Crypto.SHA512, key, data)));
        // The generic entry and the one that predates it are the same function.
        assertArrayEquals(Crypto.hmacSha256(key, data), Crypto.hmac(Crypto.SHA256, key, data));
        assertNull(Crypto.hmac(Crypto.SHA256, null, data));
        assertNull(Crypto.hmac(Crypto.SHA256, key, null));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> Crypto.hmac("MD5", repeat(1, 16), ascii("x")));
        assertEquals("Not a supported digest: MD5", refused.getMessage());
    }

    @Test
    @DisplayName("PBKDF2: RFC 6070 for SHA-1, RFC 7914 11 for SHA-256, and SHA-384/512")
    void pbkdf2() throws Exception {
        byte[] password = ascii("password");
        byte[] salt = ascii("salt");
        // RFC 6070 2.
        assertEquals("0c60c80f961f0e71f3a9b524af6012062fe037a6",
                hex(Crypto.pbkdf2(Crypto.SHA1, password, salt, 1, 20)));
        assertEquals("ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957",
                hex(Crypto.pbkdf2(Crypto.SHA1, password, salt, 2, 20)));
        assertEquals("4b007901b765489abead49d926f721d065a429c1",
                hex(Crypto.pbkdf2(Crypto.SHA1, password, salt, 4096, 20)));
        assertEquals("3d2eec4fe41c849b80c8d83662c0e44a8b291a964cf2f07038",
                hex(Crypto.pbkdf2(Crypto.SHA1, ascii("passwordPASSWORDpassword"),
                        ascii("saltSALTsaltSALTsaltSALTsaltSALTsalt"), 4096, 25)));
        // RFC 7914 11: more output than one block of the digest.
        assertEquals("55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
                + "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
                hex(Crypto.pbkdf2(Crypto.SHA256, ascii("passwd"), salt, 1, 64)));
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
                hex(Crypto.pbkdf2(Crypto.SHA256, password, salt, 4096, 32)));
        assertArrayEquals(Crypto.pbkdf2Sha256(password, salt, 4096, 32),
                Crypto.pbkdf2(Crypto.SHA256, password, salt, 4096, 32));
        assertEquals("54f775c6d790f21930459162fc535dbf04a939185127016a04176a0730c6f1f4"
                + "fb48832ad1261baadd2cedd50814b1c8",
                hex(Crypto.pbkdf2(Crypto.SHA384, password, salt, 2, 48)));
        assertEquals("867f70cf1ade02cff3752599a3a53dc4af34c7a669815ae5d513554e1c8cf252"
                + "c02d470a285a0501bad999bfe943c08f050235d7d68b1da55e63f73b60a57fce",
                hex(Crypto.pbkdf2(Crypto.SHA512, password, salt, 1, 64)));
        IOException refused = assertThrows(IOException.class,
                () -> Crypto.pbkdf2(Crypto.SHA256, password, salt, 0, 32));
        assertEquals("iterations and length must both be positive", refused.getMessage());
    }

    @Test
    @DisplayName("AES-GCM: the McGrew-Viega test cases NIST adopted, 2, 4 and 16")
    void aesGcm() throws Exception {
        // Test case 2: a zero key, a zero nonce and one zero block.
        byte[] sealed = Crypto.aesGcmEncrypt(new byte[16], new byte[12], null, new byte[16]);
        assertEquals("0388dace60b6a392f328c2b971b2fe78" + "ab6e47d42cec13bdf53a67b21257bddf",
                hex(sealed));
        assertArrayEquals(new byte[16], Crypto.aesGcmDecrypt(new byte[16], new byte[12], null, sealed));
        // Test case 4: associated data, and a plaintext that is not whole blocks.
        byte[] key = hex("feffe9928665731c6d6a8f9467308308");
        byte[] iv = hex("cafebabefacedbaddecaf888");
        byte[] plain = hex("d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72"
                + "1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39");
        byte[] aad = hex("feedfacedeadbeeffeedfacedeadbeefabaddad2");
        sealed = Crypto.aesGcmEncrypt(key, iv, aad, plain);
        assertEquals("42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e"
                + "21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091"
                + "5bc94fbc3221a5db94fae95ae7121a47", hex(sealed));
        assertArrayEquals(plain, Crypto.aesGcmDecrypt(key, iv, aad, sealed));
        // Test case 16: the same with a 256 bit key.
        byte[] key256 = hex("feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308");
        byte[] sealed256 = Crypto.aesGcmEncrypt(key256, iv, aad, plain);
        assertEquals("522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa"
                + "8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f662"
                + "76fc6ece0f4e1768cddf8853bb2d551b", hex(sealed256));
        assertArrayEquals(plain, Crypto.aesGcmDecrypt(key256, iv, aad, sealed256));

        // Anything that is not what was sealed opens as null, not as garbage.
        byte[] flipped = sealed.clone();
        flipped[3] ^= 1;
        assertNull(Crypto.aesGcmDecrypt(key, iv, aad, flipped));
        byte[] badTag = sealed.clone();
        badTag[badTag.length - 1] ^= 1;
        assertNull(Crypto.aesGcmDecrypt(key, iv, aad, badTag));
        assertNull(Crypto.aesGcmDecrypt(key, iv, null, sealed));
        assertNull(Crypto.aesGcmDecrypt(key, hex("cafebabefacedbaddecaf889"), aad, sealed));
        assertNull(Crypto.aesGcmDecrypt(hex("feffe9928665731c6d6a8f9467308309"), iv, aad, sealed));
        // An empty message is still authenticated.
        byte[] empty = Crypto.aesGcmEncrypt(key, iv, aad, new byte[0]);
        assertEquals(16, empty.length);
        assertEquals(0, Crypto.aesGcmDecrypt(key, iv, aad, empty).length);

        final byte[] sealedCopy = sealed;
        assertEquals("An AES key is 16, 24 or 32 bytes, not 15", assertThrows(IOException.class,
                () -> Crypto.aesGcmEncrypt(new byte[15], iv, null, plain)).getMessage());
        assertEquals("Too short to carry an AES-GCM tag", assertThrows(IOException.class,
                () -> Crypto.aesGcmDecrypt(key, iv, null, new byte[15])).getMessage());
        assertEquals("AES-GCM needs a nonce", assertThrows(IOException.class,
                () -> Crypto.aesGcmDecrypt(key, new byte[0], null, sealedCopy)).getMessage());
    }

    @Test
    @DisplayName("RSASSA-PKCS1-v1_5 is deterministic: the bytes openssl made, to the byte")
    void rsaPkcs1() throws Exception {
        byte[] privateKey = Base64.decode(KeyFixtures.RSA_PKCS8_DER);
        byte[] publicKey = Base64.decode(KeyFixtures.RSA_PUBLIC_DER);
        String[][] cases = {{Crypto.RS256, KeyFixtures.RS256_SIGNATURE},
            {Crypto.RS384, KeyFixtures.RS384_SIGNATURE}, {Crypto.RS512, KeyFixtures.RS512_SIGNATURE}};
        for (String[] c : cases) {
            byte[] expected = Base64.decode(c[1]);
            assertArrayEquals(expected, Crypto.sign(c[0], privateKey, MESSAGE), c[0]);
            assertTrue(Crypto.verify(c[0], publicKey, MESSAGE, expected), c[0]);
            assertFalse(Crypto.verify(c[0], publicKey, ascii("another message"), expected), c[0]);
        }
        // One algorithm's signature is not another's.
        assertFalse(Crypto.verify(Crypto.RS384, publicKey, MESSAGE,
                Base64.decode(KeyFixtures.RS256_SIGNATURE)));
        assertFalse(Crypto.verify(Crypto.PS256, publicKey, MESSAGE,
                Base64.decode(KeyFixtures.RS256_SIGNATURE)));
    }

    @Test
    @DisplayName("PS256, ES256 and ES384: openssl's signatures verify, and so do our own")
    void randomizedSchemes() throws Exception {
        Object[][] cases = {
            {Crypto.PS256, KeyFixtures.RSA_PKCS8_DER, KeyFixtures.RSA_PUBLIC_DER, KeyFixtures.PS256_SIGNATURE},
            {Crypto.ES256, KeyFixtures.EC256_PKCS8_DER, KeyFixtures.EC256_PUBLIC_DER, KeyFixtures.ES256_SIGNATURE},
            {Crypto.ES384, KeyFixtures.EC384_PKCS8_DER, KeyFixtures.EC384_PUBLIC_DER, KeyFixtures.ES384_SIGNATURE}};
        for (Object[] c : cases) {
            String algorithm = (String) c[0];
            byte[] privateKey = Base64.decode((String) c[1]);
            byte[] publicKey = Base64.decode((String) c[2]);
            byte[] theirs = Base64.decode((String) c[3]);
            assertTrue(Crypto.verify(algorithm, publicKey, MESSAGE, theirs), algorithm);
            byte[] ours = Crypto.sign(algorithm, privateKey, MESSAGE);
            byte[] again = Crypto.sign(algorithm, privateKey, MESSAGE);
            assertFalse(java.util.Arrays.equals(ours, again), algorithm + " is randomized");
            assertTrue(Crypto.verify(algorithm, publicKey, MESSAGE, ours), algorithm);
            assertTrue(Crypto.verify(algorithm, publicKey, MESSAGE, again), algorithm);
            assertFalse(Crypto.verify(algorithm, publicKey, ascii("another message"), ours), algorithm);
            byte[] tampered = ours.clone();
            tampered[tampered.length - 1] ^= 1;
            assertFalse(Crypto.verify(algorithm, publicKey, MESSAGE, tampered), algorithm);
        }
    }

    @Test
    @DisplayName("A signature that is not one is 'no'; a key that is not one is an error")
    void badSignatureIsNotAnError() throws Exception {
        byte[] rsaPublic = Base64.decode(KeyFixtures.RSA_PUBLIC_DER);
        byte[] ecPublic = Base64.decode(KeyFixtures.EC256_PUBLIC_DER);
        // Not valid, in every shape a signature can be wrong.
        assertFalse(Crypto.verify(Crypto.RS256, rsaPublic, MESSAGE, new byte[256]));
        assertFalse(Crypto.verify(Crypto.RS256, rsaPublic, MESSAGE, new byte[3]));
        assertFalse(Crypto.verify(Crypto.RS256, rsaPublic, MESSAGE, new byte[0]));
        assertFalse(Crypto.verify(Crypto.RS256, rsaPublic, MESSAGE, null));
        assertFalse(Crypto.verify(Crypto.ES256, ecPublic, MESSAGE, new byte[] {1, 2, 3}));
        assertFalse(Crypto.verify(Crypto.ES256, ecPublic, MESSAGE, new byte[0]));
        assertFalse(Crypto.verify(Crypto.PS256, rsaPublic, MESSAGE, new byte[256]));

        // Could not be asked.
        String kind = ": the key is not a SubjectPublicKeyInfo of the kind that algorithm uses";
        assertEquals("Could not verify with RS256" + kind, assertThrows(IOException.class,
                () -> Crypto.verify(Crypto.RS256, new byte[] {1, 2, 3}, MESSAGE, new byte[256]))
                .getMessage());
        // The key decides nothing: an EC key under an RSA name, and the reverse.
        assertEquals("Could not verify with RS256" + kind, assertThrows(IOException.class,
                () -> Crypto.verify(Crypto.RS256, ecPublic, MESSAGE, new byte[256])).getMessage());
        assertEquals("Could not verify with ES256" + kind, assertThrows(IOException.class,
                () -> Crypto.verify(Crypto.ES256, rsaPublic, MESSAGE, new byte[64])).getMessage());
        // And the curve is part of the algorithm.
        byte[] p384 = Base64.decode(KeyFixtures.EC384_PUBLIC_DER);
        assertEquals("Could not verify with ES256" + kind, assertThrows(IOException.class,
                () -> Crypto.verify(Crypto.ES256, p384, MESSAGE,
                        Base64.decode(KeyFixtures.ES384_SIGNATURE))).getMessage());
        assertEquals("Could not verify with ES384" + kind, assertThrows(IOException.class,
                () -> Crypto.verify(Crypto.ES384, ecPublic, MESSAGE,
                        Base64.decode(KeyFixtures.ES256_SIGNATURE))).getMessage());
        assertEquals("Not a supported signature algorithm: none", assertThrows(IOException.class,
                () -> Crypto.verify("none", rsaPublic, MESSAGE, new byte[0])).getMessage());
        assertEquals("Not a supported signature algorithm: HS256", assertThrows(IOException.class,
                () -> Crypto.verify("HS256", rsaPublic, MESSAGE, new byte[32])).getMessage());

        String signKind = ": the key is not a PKCS#8 key of the kind that algorithm uses";
        byte[] ecPrivate = Base64.decode(KeyFixtures.EC256_PKCS8_DER);
        assertEquals("Could not sign with RS256" + signKind, assertThrows(IOException.class,
                () -> Crypto.sign(Crypto.RS256, ecPrivate, MESSAGE)).getMessage());
        assertEquals("Could not sign with ES384" + signKind, assertThrows(IOException.class,
                () -> Crypto.sign(Crypto.ES384, ecPrivate, MESSAGE)).getMessage());
        assertEquals("Could not sign with ES256" + signKind, assertThrows(IOException.class,
                () -> Crypto.sign(Crypto.ES256, Base64.decode(KeyFixtures.RSA_PKCS8_DER), MESSAGE))
                .getMessage());
    }

    @Test
    @DisplayName("A generated RSA key signs, and is a different key every time")
    void generatedKey() throws Exception {
        byte[] first = Crypto.generateRsaKey(2048);
        byte[] second = Crypto.generateRsaKey(2048);
        assertNotEquals(hex(first), hex(second));
        byte[] publicKey = Der.publicKeyOf(first);
        assertEquals(2048, Der.rsaModulusBits(publicKey));
        byte[] signature = Crypto.sign(Crypto.RS256, first, MESSAGE);
        assertTrue(Crypto.verify(Crypto.RS256, publicKey, MESSAGE, signature));
        assertFalse(Crypto.verify(Crypto.RS256, Der.publicKeyOf(second), MESSAGE, signature));
        assertEquals("An RSA key is 2048 to 8192 bits, not 1024", assertThrows(IOException.class,
                () -> Crypto.generateRsaKey(1024)).getMessage());
    }
}
