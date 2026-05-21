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

class JwtTest extends UITestBase {

    @Test
    void hs256_roundTrip() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "alice");
        claims.put("exp", Long.valueOf(1700000000L));
        byte[] secret = "supersecret".getBytes("UTF-8");

        String token = Jwt.signHs256(claims, secret);
        // standard JWT shape: three URL-safe-base64 segments
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        Jwt parsed = Jwt.parse(token);
        assertEquals("HS256", parsed.getAlgorithm());
        assertTrue(parsed.verifyHs256(secret));
        assertFalse(parsed.verifyHs256("wrong".getBytes("UTF-8")));

        assertEquals("alice", parsed.getClaim("sub"));
        // exp comes back as Long via the JSON parser
        Object exp = parsed.getClaim("exp");
        assertNotNull(exp);
    }

    @Test
    void hs256_referenceVector() throws Exception {
        // jwt.io canonical example:
        //   header  {"alg":"HS256","typ":"JWT"}
        //   payload {"sub":"1234567890","name":"John Doe","iat":1516239022}
        //   secret  "your-256-bit-secret"
        //   token   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                     + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ."
                     + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        Jwt parsed = Jwt.parse(token);
        assertTrue(parsed.verifyHs256("your-256-bit-secret".getBytes("UTF-8")));
        assertEquals("John Doe", parsed.getClaim("name"));
    }

    @Test
    void hs512_roundTrip() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("k", "v");
        byte[] secret = "s".getBytes("UTF-8");
        String token = Jwt.signHs512(claims, secret);
        Jwt parsed = Jwt.parse(token);
        assertTrue(parsed.verifyHs512(secret));
    }

    @Test
    void parseMalformedRejected() {
        try {
            Jwt.parse("not-a-jwt");
            fail("expected CryptoException");
        } catch (CryptoException expected) { /* ok */ }
        try {
            Jwt.parse("header.payload"); // missing signature segment
            fail("expected CryptoException");
        } catch (CryptoException expected) { /* ok */ }
    }

    @Test
    void noneAlgorithmRejectedByDefault() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "x");
        String token = Jwt.signNone(claims);
        Jwt parsed = Jwt.parse(token);
        assertEquals("none", parsed.getAlgorithm());
        // verify(PublicKey) with no opt-in must refuse
        assertFalse(parsed.verify(null));
        parsed.setVerifyAllowNoneAlgorithm(true);
        // null key is fine for none — only the boolean opt-in matters
        assertTrue(parsed.verify(null));
    }

    @Test
    void base64UrlRoundTrip() throws Exception {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) data[i] = (byte) i;
        assertArrayEquals(data,
                com.codename1.util.Base64.decodeUrlSafe(com.codename1.util.Base64.encodeUrlSafe(data)));
    }
}
