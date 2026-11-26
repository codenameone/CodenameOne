/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.restock;

import com.codename1.backend.Crypto;
import com.codename1.backend.Jwt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The three signatures, checked against values computed here the way the
/// documentation describes them.
class SignaturesTest {
    private static final String SECRET = "bzs_test-client-secret";

    @Test
    void aBodySignatureMatchesOnlyItsOwnBodyAndTimestamp() throws Exception {
        String key = Signatures.signingKey(SECRET);
        assertEquals(64, key.length());
        long now = 1700000000L;
        String ts = Long.toString(now);
        byte[] body = "{\"a\":1}".getBytes("UTF-8");
        String header = "sha256=" + Signatures.hex(Crypto.hmacSha256(key.getBytes("UTF-8"),
                (ts + ".{\"a\":1}").getBytes("UTF-8")));
        assertTrue(Signatures.body(key, body, header, ts, now));
        assertTrue(Signatures.body(key, body, header, ts, now + 300));
        assertFalse(Signatures.body(key, "{\"a\":2}".getBytes("UTF-8"), header, ts, now));
        assertFalse(Signatures.body(key, body, null, ts, now));
        // A replay outside the window, and a timestamp moved to look fresh.
        assertFalse(Signatures.body(key, body, header, ts, now + 301));
        assertFalse(Signatures.body(key, body, header, Long.toString(now + 301), now + 301));
        assertFalse(Signatures.body(key, body, header, null, now));
        // The body alone is no longer what is signed.
        assertFalse(Signatures.body(key, body, "sha256=" + Signatures.hex(Crypto.hmacSha256(
                key.getBytes("UTF-8"), body)), ts, now));
    }

    @Test
    void aProxySignatureCoversEveryParameterAndExpires() throws Exception {
        String key = Signatures.signingKey(SECRET);
        long now = 1700000000L;
        Map<String, String> q = new LinkedHashMap<String, String>();
        q.put("shop", "mugs");
        q.put("path_prefix", "/apps/restock");
        q.put("timestamp", Long.toString(now));
        q.put("product", "a b");
        // Each name and value percent-encoded before they are joined.
        String message = "path_prefix=%2Fapps%2Frestock&product=a%20b&shop=mugs&timestamp=" + now;
        q.put("signature", Signatures.hex(Crypto.hmacSha256(key.getBytes("UTF-8"),
                message.getBytes("UTF-8"))));
        assertTrue(Signatures.proxy(q, key, now));
        assertFalse(Signatures.proxy(q, key, now + 301));
        q.put("product", "other");
        assertFalse(Signatures.proxy(q, key, now));
        // A value holding "&" and "=" does not sign like two parameters.
        Map<String, String> one = new LinkedHashMap<String, String>();
        one.put("timestamp", Long.toString(now));
        one.put("a", "1&b=2");
        Map<String, String> two = new LinkedHashMap<String, String>();
        two.put("timestamp", Long.toString(now));
        two.put("a", "1");
        two.put("b", "2");
        String joined = "a=1&b=2&timestamp=" + now;
        String forged = Signatures.hex(Crypto.hmacSha256(key.getBytes("UTF-8"),
                joined.getBytes("UTF-8")));
        two.put("signature", forged);
        assertTrue(Signatures.proxy(two, key, now));
        one.put("signature", forged);
        assertFalse(Signatures.proxy(one, key, now));
    }

    @Test
    void aSessionTokenIsForThisAppOnly() throws Exception {
        String key = Signatures.signingKey(SECRET);
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("iss", "bazaar");
        claims.put("aud", "bzc_restock");
        claims.put("dest", "store-1");
        final String token = Jwt.issue(claims, key.getBytes("UTF-8"), 300);
        assertEquals("store-1", Signatures.session(token, key, "bzc_restock").get("dest"));
        assertThrows(IOException.class, () -> Signatures.session(token, key, "bzc_other"));
        assertThrows(IOException.class, () -> Signatures.session(token,
                Signatures.signingKey("another secret"), "bzc_restock"));
    }
}
