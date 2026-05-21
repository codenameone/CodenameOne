/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * Distributed under the same terms as the rest of Codename One.
 */
package com.codename1.security;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// End-to-end round trips through the impl-layer crypto bridge. On JavaSE the
/// bridge defaults to java.security via reflection so these tests also serve
/// as a smoke test for that path.
class CipherSignatureTest extends UITestBase {

    @Test
    void aesGcmRoundTrip() {
        SecretKey key = KeyGenerator.aes(256);
        byte[] iv = SecureRandom.bytes(12);
        byte[] plaintext = "the magic words are squeamish ossifrage".getBytes();
        byte[] aad = "v1".getBytes();

        byte[] ciphertext = Cipher.aesEncrypt(Cipher.AES_GCM, key, iv, aad, plaintext);
        assertNotEquals(plaintext.length, ciphertext.length); // includes tag
        byte[] decrypted = Cipher.aesDecrypt(Cipher.AES_GCM, key, iv, aad, ciphertext);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void aesGcmTamperDetected() {
        SecretKey key = KeyGenerator.aes(128);
        byte[] iv = SecureRandom.bytes(12);
        byte[] plaintext = "secret payload".getBytes();
        byte[] ciphertext = Cipher.aesEncrypt(Cipher.AES_GCM, key, iv, null, plaintext);
        ciphertext[0] ^= 0x01;
        try {
            Cipher.aesDecrypt(Cipher.AES_GCM, key, iv, null, ciphertext);
            fail("expected CryptoException on tampered ciphertext");
        } catch (CryptoException expected) {
            // pass
        }
    }

    @Test
    void aesCbcRoundTrip() {
        SecretKey key = KeyGenerator.aes(128);
        byte[] iv = SecureRandom.bytes(16);
        byte[] plaintext = "block cipher with PKCS#5 padding".getBytes();
        byte[] ciphertext = Cipher.aesEncrypt(Cipher.AES_CBC_PKCS5, key, iv, null, plaintext);
        byte[] decrypted = Cipher.aesDecrypt(Cipher.AES_CBC_PKCS5, key, iv, null, ciphertext);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void rsaRoundTrip() {
        // 2048 bits is the slow path — keep this single test in the suite
        KeyPair kp = KeyGenerator.rsa(2048);
        byte[] plaintext = "wrap this AES key".getBytes();
        byte[] ciphertext = Cipher.rsaEncrypt(Cipher.RSA_OAEP_SHA256, kp.getPublicKey(), plaintext);
        byte[] decrypted = Cipher.rsaDecrypt(Cipher.RSA_OAEP_SHA256, kp.getPrivateKey(), ciphertext);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void rsaSignAndVerify() {
        KeyPair kp = KeyGenerator.rsa(2048);
        byte[] data = "important message".getBytes();
        byte[] sig = Signature.sign(Signature.SHA256_WITH_RSA, kp.getPrivateKey(), data);
        assertTrue(Signature.verify(Signature.SHA256_WITH_RSA, kp.getPublicKey(), data, sig));
        // tamper detection
        data[0] ^= 0x01;
        assertFalse(Signature.verify(Signature.SHA256_WITH_RSA, kp.getPublicKey(), data, sig));
    }

    @Test
    void jwtRsRoundTrip() {
        KeyPair kp = KeyGenerator.rsa(2048);
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "bob");
        String token = Jwt.sign(claims, kp.getPrivateKey(), Jwt.RS256);
        Jwt parsed = Jwt.parse(token);
        assertEquals("RS256", parsed.getAlgorithm());
        assertTrue(parsed.verify(kp.getPublicKey()));
        // wrong key fails
        KeyPair other = KeyGenerator.rsa(2048);
        assertFalse(parsed.verify(other.getPublicKey()));
    }

    @Test
    void secureRandomProducesDifferentBytes() {
        byte[] a = SecureRandom.bytes(32);
        byte[] b = SecureRandom.bytes(32);
        // probability of collision is 2^-256 — never happens in practice
        boolean different = false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) { different = true; break; }
        }
        assertTrue(different);
        // intBelow stays within bounds
        for (int i = 0; i < 100; i++) {
            int v = SecureRandom.intBelow(1000);
            assertTrue(v >= 0 && v < 1000);
        }
    }
}
