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
package com.codenameone.examples.bazaar;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The web app's foundation: security headers and the nonce CSP, CSRF,
/// web sign-in with session rotation, the account gate, page ETags and the
/// immutable fingerprinted assets.
@BackendTest
class WebFoundationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void signInPageCarriesStrictHeadersAndNonce() throws Exception {
        Browser b = new Browser(mvc);
        MockResponse r = b.get("/auth/signin");
        assertEquals(200, r.getStatus(), r.getContentAsString());
        String csp = r.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("frame-ancestors 'none'"), csp);
        assertTrue(csp.contains("object-src 'none'"), csp);
        assertFalse(csp.contains("unsafe-inline"), csp);
        int at = csp.indexOf("'nonce-");
        assertTrue(at > 0, csp);
        String nonce = csp.substring(at + 7, csp.indexOf('\'', at + 7));
        assertTrue(r.getContentAsString().contains("nonce=\"" + nonce + "\""));
        assertEquals("nosniff", r.getHeader("X-Content-Type-Options"));
        assertNotNull(r.getHeader("Referrer-Policy"));
        assertNotNull(r.getHeader("Permissions-Policy"));
        assertNotNull(r.getHeader("Strict-Transport-Security"));
        assertNotNull(b.csrf, "the sign-in form carries a CSRF token");
        String again = b.get("/auth/signin").getHeader("Content-Security-Policy");
        assertNotEquals(csp, again, "a fresh nonce per response");
    }

    @Test
    void postWithoutCsrfTokenIsRefused() throws Exception {
        Browser b = new Browser(mvc);
        b.get("/auth/signin");
        MockResponse r = b.postWithoutToken("/auth/signin", "username", DemoAccounts.CUSTOMER,
                "password", DemoAccounts.PASSWORD);
        assertEquals(403, r.getStatus());
    }

    @Test
    void accountNeedsSignInAndSignInRotatesSession() throws Exception {
        Browser b = new Browser(mvc);
        MockResponse gate = b.get("/account");
        assertTrue(gate.getStatus() == 302 || gate.getStatus() == 303, "" + gate.getStatus());
        assertTrue(gate.getRedirectedUrl().contains("/auth/signin"), gate.getRedirectedUrl());
        b.get("/auth/signin");
        String before = b.session;
        String to = b.signIn(DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        assertNotNull(to);
        assertNotEquals(before, b.session, "the session id changes at sign-in");
        b.assertPage(b.get("/account"), "Hello");
        MockResponse out = b.post("/auth/signout");
        assertTrue(out.getStatus() == 302 || out.getStatus() == 303, "" + out.getStatus());
        MockResponse after = b.get("/account");
        assertNotEquals(200, after.getStatus());
    }

    @Test
    void wrongPasswordGoesBackToTheForm() throws Exception {
        Browser b = new Browser(mvc);
        b.get("/auth/signin");
        MockResponse r = b.post("/auth/signin", "username", DemoAccounts.SECOND_CUSTOMER,
                "password", "not-it");
        assertTrue(r.getRedirectedUrl() != null && r.getRedirectedUrl().contains("error"),
                "" + r.getRedirectedUrl());
    }

    @Test
    void pagesAnswer304ForTheirEtag() throws Exception {
        Browser b = new Browser(mvc);
        b.get("/auth/signin");
        // The page holds this session's CSRF token, so the tag is per session;
        // within one, the nonce differs per response and the tag does not.
        MockResponse r = b.get("/auth/signin");
        String etag = r.getHeader("ETag");
        assertNotNull(etag);
        assertTrue(r.getHeader("Cache-Control").contains("no-cache"));
        assertEquals(etag, b.get("/auth/signin").getHeader("ETag"));
        MockResponse cached = b.send(com.codename1.backend.test.MockMvcRequestBuilders
                .get("/auth/signin").header("If-None-Match", etag));
        assertEquals(304, cached.getStatus());
    }

    @Test
    void fingerprintedAssetsAreImmutable() throws Exception {
        Browser b = new Browser(mvc);
        String html = b.get("/auth/signin").getContentAsString();
        int at = html.indexOf("/static/app.");
        assertTrue(at > 0);
        String path = html.substring(at, html.indexOf('"', at));
        MockResponse css = b.get(path);
        assertEquals(200, css.getStatus());
        assertTrue(css.getHeader("Cache-Control").contains("immutable"),
                css.getHeader("Cache-Control"));
    }
}
