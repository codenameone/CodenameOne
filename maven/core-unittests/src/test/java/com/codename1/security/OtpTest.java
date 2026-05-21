/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * Distributed under the same terms as the rest of Codename One.
 */
package com.codename1.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// HOTP / TOTP test vectors from RFC 4226 appendix D and RFC 6238 appendix B.
class OtpTest {

    @Test
    void rfc4226_hotpAt8Digits() {
        byte[] secret = "12345678901234567890".getBytes();
        // RFC 4226 D — HOTP values at counter 0..9 with digits=6
        String[] expected = {
            "755224", "287082", "359152", "969429", "338314",
            "254676", "287922", "162583", "399871", "520489"
        };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], Otp.hotp(secret, i, 6), "counter " + i);
        }
    }

    @Test
    void rfc6238_totpKnownInstants_sha1() {
        byte[] secret = "12345678901234567890".getBytes();
        // RFC 6238 appendix B — SHA-1, 8 digits
        long[] times = { 59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L };
        String[] expected = { "94287082", "07081804", "14050471", "89005924", "69279037" };
        for (int i = 0; i < times.length; i++) {
            String code = Otp.totp(secret, times[i] * 1000L, 30, 8, Hash.SHA1);
            assertEquals(expected[i], code, "time " + times[i]);
        }
    }

    @Test
    void totpVerifyWithinTolerance() {
        byte[] secret = "JBSWY3DPEHPK3PXP".getBytes();
        // aligned to the start of a 30s window so the drift assertions are
        // unambiguous (1700000010 / 30 == 56666667 exactly)
        long now = 1700000010000L;
        // 8 digits to keep accidental code-collisions astronomically unlikely
        String currentCode = Otp.totp(secret, now, 30, 8, Hash.SHA1);
        assertTrue(Otp.verifyTotp(secret, currentCode, 0, now, 30, 8, Hash.SHA1));
        // 25s of drift — still in the same window since we started at the edge
        assertTrue(Otp.verifyTotp(secret, currentCode, 0, now + 25000L, 30, 8, Hash.SHA1));
        // two windows ahead — only accepted with tolerance >= 2
        String future = Otp.totp(secret, now + 60000L, 30, 8, Hash.SHA1);
        assertFalse(Otp.verifyTotp(secret, future, 0, now, 30, 8, Hash.SHA1));
        assertTrue(Otp.verifyTotp(secret, future, 2, now, 30, 8, Hash.SHA1));
    }

    @Test
    void hotpInvalidDigitsRejected() {
        try {
            Otp.hotp(new byte[20], 0, 0);
            fail("expected CryptoException");
        } catch (CryptoException expected) { /* ok */ }
    }

    @Test
    void base32RoundTrip() {
        byte[] secret = "12345678901234567890".getBytes();
        String encoded = Base32.encode(secret);
        assertArrayEquals(secret, Base32.decode(encoded));
        // canonical Google Authenticator example
        assertArrayEquals(new byte[]{(byte)0x48, (byte)0x65, (byte)0x6c, (byte)0x6c, (byte)0x6f, (byte)0x21, (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef},
                Base32.decode("JBSWY3DPEHPK3PXP"));
    }

    @Test
    void otpauthUriMatchesGoogleAuthenticatorFormat() {
        byte[] secret = "12345678901234567890".getBytes();
        String uri = Otp.otpauthUri("Acme Bank", "alice@example.com", secret);
        // expected structure: otpauth://totp/<issuer>:<account>?secret=...&issuer=...&algorithm=SHA1&digits=6&period=30
        assertTrue(uri.startsWith("otpauth://totp/Acme%20Bank:alice%40example.com?"), uri);
        assertTrue(uri.contains("&issuer=Acme%20Bank"));
        assertTrue(uri.contains("&algorithm=SHA1"));
        assertTrue(uri.contains("&digits=6"));
        assertTrue(uri.contains("&period=30"));
        // secret is the Base32 of `secret` with padding stripped
        String b32 = Base32.encode(secret).replace("=", "");
        assertTrue(uri.contains("secret=" + b32));
    }

    @Test
    void otpauthUriRejectsColonsInIssuerOrAccount() {
        try {
            Otp.otpauthUri("Acme:Bank", "alice", new byte[20]);
            fail("expected CryptoException");
        } catch (CryptoException expected) { /* ok */ }
        try {
            Otp.otpauthUri("Acme", "alice:doe", new byte[20]);
            fail("expected CryptoException");
        } catch (CryptoException expected) { /* ok */ }
    }
}
