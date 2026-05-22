/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * Distributed under the same terms as the rest of Codename One.
 */
package com.codename1.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// HMAC test vectors from RFC 4231 (HMAC-SHA-2).
class HmacTest {

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] repeat(byte b, int n) {
        byte[] a = new byte[n];
        for (int i = 0; i < n; i++) a[i] = b;
        return a;
    }

    @Test
    void rfc4231_testCase1_sha256() {
        // Key = 0x0b * 20, data = "Hi There"
        byte[] expected = Hash.fromHex(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7");
        byte[] actual = Hmac.sha256(repeat((byte)0x0b, 20), ascii("Hi There"));
        assertArrayEquals(expected, actual);
    }

    @Test
    void rfc4231_testCase2_keyShorterThanBlock_sha256() {
        // Key = "Jefe", data = "what do ya want for nothing?"
        byte[] expected = Hash.fromHex(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
        byte[] actual = Hmac.sha256(ascii("Jefe"), ascii("what do ya want for nothing?"));
        assertArrayEquals(expected, actual);
    }

    @Test
    void rfc4231_testCase2_sha512() {
        // Key = "Jefe", data = "what do ya want for nothing?"
        byte[] expected = Hash.fromHex(
            "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737");
        byte[] actual = Hmac.sha512(ascii("Jefe"), ascii("what do ya want for nothing?"));
        assertArrayEquals(expected, actual);
    }

    @Test
    void rfc4231_testCase6_longerThanBlockKey_sha256() {
        // Key = 0xaa * 131, Data = "Test Using Larger Than Block-Size Key - Hash Key First"
        byte[] expected = Hash.fromHex(
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54");
        byte[] actual = Hmac.sha256(
                repeat((byte)0xaa, 131),
                ascii("Test Using Larger Than Block-Size Key - Hash Key First"));
        assertArrayEquals(expected, actual);
    }

    @Test
    void streamingProducesSameAsOneShot() {
        byte[] key = ascii("supersecret");
        byte[] data = ascii("the message split into pieces here");
        byte[] oneShot = Hmac.sha256(key, data);

        Hmac h = Hmac.create(Hash.SHA256, key);
        h.update(data, 0, 10);
        h.update(data, 10, data.length - 10);
        assertArrayEquals(oneShot, h.doFinal());
    }

    @Test
    void constantTimeEqualsHandlesNullsAndLengths() {
        assertFalse(Hmac.constantTimeEquals(null, new byte[1]));
        assertFalse(Hmac.constantTimeEquals(new byte[1], null));
        assertFalse(Hmac.constantTimeEquals(new byte[2], new byte[3]));
        assertTrue(Hmac.constantTimeEquals(new byte[]{1,2,3}, new byte[]{1,2,3}));
        assertFalse(Hmac.constantTimeEquals(new byte[]{1,2,3}, new byte[]{1,2,4}));
    }
}
