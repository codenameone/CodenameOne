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
package com.codenameone.examples.parley;

import com.codename1.backend.Json;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;

/// Signs a user in the way the app does, for the tests that need a real token:
/// the password for a session, the session for an authorization code, the code
/// for tokens.
final class SignIn {
    /// The PKCE pair of RFC 7636's appendix B.
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    static final String HOST = "parley.test:8080";

    private SignIn() {
    }

    static MockRequestBuilder formPost(String path, String... pairs) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(OAuth2Parameters.encode(pairs[i])).append('=')
                    .append(OAuth2Parameters.encode(pairs[i + 1]));
        }
        return post(path).contentType("application/x-www-form-urlencoded").content(body.toString());
    }

    /// The session cookie a correct password earns, or null for a wrong one.
    static String session(MockMvc mvc, String username, String password) throws Exception {
        return mvc.perform(formPost("/login", "username", username, "password", password)
                        .header("Host", HOST))
                .andReturn().getResponse().getCookie("CN1SESSION");
    }

    /// The token response for a user: `access_token`, `refresh_token` and the
    /// rest.
    static Map tokens(MockMvc mvc, String username, String password) throws Exception {
        String session = session(mvc, username, password);
        if (session == null) {
            throw new IllegalStateException("could not sign in as " + username);
        }
        String location = mvc.perform(get("/oauth2/authorize?response_type=code&client_id="
                        + SecurityConfig.CLIENT_ID + "&redirect_uri="
                        + OAuth2Parameters.encode(SecurityConfig.LOOPBACK_REDIRECT)
                        + "&scope=openid%20profile&state=s-1&nonce=n-1&code_challenge=" + CHALLENGE
                        + "&code_challenge_method=S256")
                        .header("Host", HOST).cookie("CN1SESSION", session))
                .andReturn().getResponse().getRedirectedUrl();
        if (location == null || location.indexOf("code=") < 0) {
            throw new IllegalStateException("no authorization code: " + location);
        }
        int at = location.indexOf("code=") + 5;
        int end = location.indexOf('&', at);
        String code = location.substring(at, end < 0 ? location.length() : end);
        String body = mvc.perform(formPost("/oauth2/token",
                        "grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", SecurityConfig.LOOPBACK_REDIRECT,
                        "code_verifier", VERIFIER,
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", HOST))
                .andReturn().getResponse().getContentAsString();
        return Json.parseObject(body);
    }

    /// A bearer token for a user.
    static String token(MockMvc mvc, String username, String password) throws Exception {
        Object token = tokens(mvc, username, password).get("access_token");
        if (!(token instanceof String)) {
            throw new IllegalStateException("no access token for " + username);
        }
        return (String) token;
    }
}
