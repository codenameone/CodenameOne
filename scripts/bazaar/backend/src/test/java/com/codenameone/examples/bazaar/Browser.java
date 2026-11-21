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

import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A browser for the web app's tests: keeps the session cookie, reads the
/// CSRF token out of the last page, and posts forms the way a page does.
final class Browser {
    final MockMvc mvc;
    String session;
    String csrf;
    MockResponse last;

    Browser(MockMvc mvc) {
        this.mvc = mvc;
    }

    MockResponse get(String path) throws Exception {
        return send(com.codename1.backend.test.MockMvcRequestBuilders.get(path));
    }

    MockResponse send(MockRequestBuilder b) throws Exception {
        b.header("Host", SignIn.HOST);
        if (session != null) {
            b.cookie("CN1SESSION", session);
        }
        MockResponse r = mvc.perform(b).andReturn().getResponse();
        String s = r.getCookie("CN1SESSION");
        if (s != null) {
            session = s.length() == 0 ? null : s;
        }
        String type = r.getContentType();
        if (type != null && type.startsWith("text/html")) {
            String token = token(r.getContentAsString());
            if (token != null) {
                csrf = token;
            }
        }
        last = r;
        return r;
    }

    /// A form post with this session's CSRF token.
    MockResponse post(String path, String... pairs) throws Exception {
        String[] all = new String[pairs.length + 2];
        System.arraycopy(pairs, 0, all, 0, pairs.length);
        all[pairs.length] = "_csrf";
        all[pairs.length + 1] = csrf == null ? "" : csrf;
        return send(form(path, all));
    }

    /// A form post with no token at all.
    MockResponse postWithoutToken(String path, String... pairs) throws Exception {
        return send(form(path, pairs));
    }

    static MockRequestBuilder form(String path, String... pairs) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(OAuth2Parameters.encode(pairs[i])).append('=')
                    .append(OAuth2Parameters.encode(pairs[i + 1]));
        }
        return com.codename1.backend.test.MockMvcRequestBuilders.post(path)
                .contentType("application/x-www-form-urlencoded").content(body.toString());
    }

    /// Signs in through the web form; answers where it was sent.
    String signIn(String username, String password) throws Exception {
        get("/auth/signin");
        MockResponse r = post("/auth/signin", "username", username, "password", password);
        assertEquals(303 == r.getStatus() || 302 == r.getStatus(), true,
                "sign-in answered " + r.getStatus());
        return r.getRedirectedUrl();
    }

    static String token(String html) {
        int at = html.indexOf("name=\"_csrf\"");
        if (at < 0) {
            return null;
        }
        int tag = html.lastIndexOf('<', at);
        int end = html.indexOf('>', at);
        String input = html.substring(tag, end);
        int v = input.indexOf("value=\"");
        if (v < 0) {
            return null;
        }
        return input.substring(v + 7, input.indexOf('"', v + 7));
    }

    void assertPage(MockResponse r, String text) throws Exception {
        assertEquals(200, r.getStatus(), r.getContentAsString());
        assertTrue(r.getContentAsString().contains(text), text);
    }
}
