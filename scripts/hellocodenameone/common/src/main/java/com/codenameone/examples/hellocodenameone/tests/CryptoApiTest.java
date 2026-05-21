package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.security.Base32;
import com.codename1.security.Cipher;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.security.Jwt;
import com.codename1.security.KeyGenerator;
import com.codename1.security.KeyPair;
import com.codename1.security.Otp;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import com.codename1.security.Signature;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * On-device end-to-end coverage for com.codename1.security. The JUnit suite
 * in maven/core-unittests/src/test/java/com/codename1/security/ exercises the
 * same API surface against the JavaSE simulator's java.security bridge;
 * this test runs the same vectors and round-trips on the actual device build
 * so we catch any divergence introduced by:
 *
 * - ParparVM lowering on iOS (the native CN1Crypto bridge in
 *   Ports/iOSPort/nativeSources/CN1Crypto.{h,m})
 * - AndroidImplementation overrides
 * - Build-system gating (CN1_INCLUDE_CRYPTO #define toggled by IPhoneBuilder
 *   when com.codename1.security.* is referenced)
 *
 * Skipped on HTML5/JavaScript -- the JS port does not yet provide a crypto
 * impl, see Cn1ssDeviceRunner.HTML5_SKIP_TESTS.
 */
public class CryptoApiTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            runHashVectors();
            runHmacVectors();
            runOtpVectors();
            runOtpAuthUri();
            runSecureRandom();
            runJwtHs256();
            runAesGcmRoundTrip();
            runRsaRoundTrip();
            runRsaSignVerify();
            runJwtRs256();
        } catch (Throwable t) {
            fail("Crypto API test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    // ---- Hash test vectors (FIPS 180-4 + RFC 1321) ----------------------

    private void runHashVectors() {
        byte[] abc = ascii("abc");
        assertEqual("900150983cd24fb0d6963f7d28e17f72",
                Hash.toHex(Hash.md5(abc)), "MD5 of abc");
        assertEqual("a9993e364706816aba3e25717850c26c9cd0d89d",
                Hash.toHex(Hash.sha1(abc)), "SHA-1 of abc");
        assertEqual("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Hash.toHex(Hash.sha256(abc)), "SHA-256 of abc");
        assertEqual(
                "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
                Hash.toHex(Hash.sha384(abc)), "SHA-384 of abc");
        assertEqual(
                "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
                Hash.toHex(Hash.sha512(abc)), "SHA-512 of abc");

        // Streaming-equivalent test: feed byte by byte
        Hash h = Hash.create(Hash.SHA256);
        byte[] msg = ascii("The quick brown fox jumps over the lazy dog");
        for (int i = 0; i < msg.length; i++) {
            h.update(msg[i]);
        }
        assertEqual("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
                Hash.toHex(h.digest()), "SHA-256 streaming");
    }

    // ---- HMAC test vectors (RFC 4231) -----------------------------------

    private void runHmacVectors() {
        // RFC 4231 Test Case 1: Key = 0x0b * 20, Data = "Hi There"
        byte[] key = repeat((byte) 0x0b, 20);
        byte[] expected = Hash.fromHex("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7");
        assertBytes(expected, Hmac.sha256(key, ascii("Hi There")), "HMAC-SHA-256 RFC 4231 #1");

        // Test Case 2: Key = "Jefe"
        expected = Hash.fromHex("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
        assertBytes(expected, Hmac.sha256(ascii("Jefe"), ascii("what do ya want for nothing?")),
                "HMAC-SHA-256 RFC 4231 #2");

        // Streaming HMAC produces the same tag as the one-shot variant
        byte[] data = ascii("the message split into pieces here");
        Hmac streaming = Hmac.create(Hash.SHA256, ascii("supersecret"));
        streaming.update(data, 0, 10);
        streaming.update(data, 10, data.length - 10);
        assertBytes(Hmac.sha256(ascii("supersecret"), data), streaming.doFinal(),
                "Hmac streaming");

        // Constant-time compare
        assertTrue(Hmac.constantTimeEquals(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}),
                "constantTimeEquals identical");
        assertTrue(!Hmac.constantTimeEquals(new byte[]{1, 2, 3}, new byte[]{1, 2, 4}),
                "constantTimeEquals different");
    }

    // ---- HOTP / TOTP RFC vectors ----------------------------------------

    private void runOtpVectors() {
        byte[] secret = ascii("12345678901234567890");
        // RFC 4226 D
        String[] hotp = {
                "755224", "287082", "359152", "969429", "338314",
                "254676", "287922", "162583", "399871", "520489"
        };
        for (int i = 0; i < hotp.length; i++) {
            assertEqual(hotp[i], Otp.hotp(secret, i, 6), "HOTP at counter " + i);
        }
        // RFC 6238 B (SHA-1, 8 digits)
        long[] times = {59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L};
        String[] totp = {"94287082", "07081804", "14050471", "89005924", "69279037"};
        for (int i = 0; i < times.length; i++) {
            assertEqual(totp[i],
                    Otp.totp(secret, times[i] * 1000L, 30, 8, Hash.SHA1),
                    "TOTP at t=" + times[i]);
        }
        // verifyTotp with tolerance
        long now = 1700000010000L; // aligned to a 30s window
        String code = Otp.totp(secret, now, 30, 8, Hash.SHA1);
        assertTrue(Otp.verifyTotp(secret, code, 0, now, 30, 8, Hash.SHA1),
                "verifyTotp current window");
        assertTrue(Otp.verifyTotp(secret, code, 0, now + 25000L, 30, 8, Hash.SHA1),
                "verifyTotp drift within window");
    }

    private void runOtpAuthUri() {
        byte[] secret = ascii("12345678901234567890");
        String uri = Otp.otpauthUri("Acme Bank", "alice@example.com", secret);
        assertTrue(uri.startsWith("otpauth://totp/Acme%20Bank:alice%40example.com?"),
                "otpauthUri prefix: " + uri);
        assertTrue(uri.indexOf("&algorithm=SHA1") > 0, "otpauthUri carries algorithm: " + uri);
        assertTrue(uri.indexOf("&digits=6") > 0, "otpauthUri carries digits: " + uri);
        String b32 = Base32.encode(secret).replace("=", "");
        assertTrue(uri.indexOf("secret=" + b32) > 0, "otpauthUri carries Base32 secret: " + uri);
    }

    // ---- SecureRandom: only smoke-test, no statistical claim -----------

    private void runSecureRandom() {
        byte[] a = SecureRandom.bytes(32);
        byte[] b = SecureRandom.bytes(32);
        boolean different = false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                different = true;
                break;
            }
        }
        assertTrue(different, "SecureRandom should not produce identical buffers");
        for (int i = 0; i < 50; i++) {
            int v = SecureRandom.intBelow(1000);
            assertTrue(v >= 0 && v < 1000, "intBelow within range");
        }
    }

    // ---- JWT HS256 round-trip + canonical jwt.io vector ----------------

    private void runJwtHs256() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "alice");
        claims.put("name", "Alice On-Device");
        byte[] secret = ascii("supersecret");
        String token = Jwt.signHs256(claims, secret);
        Jwt parsed = Jwt.parse(token);
        assertEqual("HS256", parsed.getAlgorithm(), "JWT alg HS256");
        assertEqual("alice", String.valueOf(parsed.getClaim("sub")), "JWT sub claim");
        assertTrue(parsed.verifyHs256(secret), "JWT HS256 verify good");
        assertTrue(!parsed.verifyHs256(ascii("wrong")), "JWT HS256 reject wrong key");

        // jwt.io canonical example
        String canonical = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ."
                + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertTrue(Jwt.parse(canonical).verifyHs256(ascii("your-256-bit-secret")),
                "JWT HS256 canonical vector");
    }

    // ---- AES-GCM round-trip + tamper detection --------------------------

    private void runAesGcmRoundTrip() {
        SecretKey key = KeyGenerator.aes(256);
        byte[] nonce = SecureRandom.bytes(12);
        byte[] plaintext = ascii("the magic words are squeamish ossifrage");
        byte[] aad = ascii("v1");

        byte[] ciphertext = Cipher.aesEncrypt(Cipher.AES_GCM, key, nonce, aad, plaintext);
        assertBytes(plaintext, Cipher.aesDecrypt(Cipher.AES_GCM, key, nonce, aad, ciphertext),
                "AES-GCM round-trip");

        // Tamper detection
        ciphertext[0] ^= 0x01;
        boolean threw = false;
        try {
            Cipher.aesDecrypt(Cipher.AES_GCM, key, nonce, aad, ciphertext);
        } catch (com.codename1.security.CryptoException expected) {
            threw = true;
        }
        assertTrue(threw, "AES-GCM tamper should throw");
    }

    // ---- RSA encrypt/decrypt round-trip --------------------------------

    private void runRsaRoundTrip() {
        // 2048 is the smallest modern minimum; on-device generation is slow
        // (a few hundred ms) but still acceptable for a one-shot test.
        KeyPair kp = KeyGenerator.rsa(2048);
        byte[] payload = ascii("wrap this AES key");
        byte[] sealed = Cipher.rsaEncrypt(Cipher.RSA_OAEP_SHA256, kp.getPublicKey(), payload);
        assertBytes(payload, Cipher.rsaDecrypt(Cipher.RSA_OAEP_SHA256, kp.getPrivateKey(), sealed),
                "RSA-OAEP round-trip");
    }

    private void runRsaSignVerify() {
        KeyPair kp = KeyGenerator.rsa(2048);
        byte[] data = ascii("important message");
        byte[] sig = Signature.sign(Signature.SHA256_WITH_RSA, kp.getPrivateKey(), data);
        assertTrue(Signature.verify(Signature.SHA256_WITH_RSA, kp.getPublicKey(), data, sig),
                "RSA-SHA256 signature verifies");
        data[0] ^= 0x01;
        assertTrue(!Signature.verify(Signature.SHA256_WITH_RSA, kp.getPublicKey(), data, sig),
                "RSA-SHA256 signature rejects tampered data");
    }

    private void runJwtRs256() {
        KeyPair kp = KeyGenerator.rsa(2048);
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "bob");
        String token = Jwt.sign(claims, kp.getPrivateKey(), Jwt.RS256);
        Jwt parsed = Jwt.parse(token);
        assertEqual("RS256", parsed.getAlgorithm(), "JWT RS256 alg");
        assertTrue(parsed.verify(kp.getPublicKey()), "JWT RS256 verify with right key");
        KeyPair other = KeyGenerator.rsa(2048);
        assertTrue(!parsed.verify(other.getPublicKey()),
                "JWT RS256 rejects signature from wrong key");
    }

    // ---- tiny assertion helpers (assertEqual/assertTrue come from AbstractTest) ----

    /// Byte-array equality assertion -- AbstractTest doesn't ship one out of
    /// the box, and Arrays.equals is convenient but we want a precise failure
    /// message identifying which byte diverged.
    private void assertBytes(byte[] expected, byte[] actual, String msg) {
        if (expected.length != actual.length) {
            throw new RuntimeException(msg + ": length expected " + expected.length + ", got " + actual.length);
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                throw new RuntimeException(msg + ": byte " + i + " expected "
                        + (expected[i] & 0xff) + ", got " + (actual[i] & 0xff));
            }
        }
    }

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] repeat(byte b, int n) {
        byte[] a = new byte[n];
        for (int i = 0; i < n; i++) {
            a[i] = b;
        }
        return a;
    }
}
