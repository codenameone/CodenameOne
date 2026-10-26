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
package com.codenameone.examples.bastion;

import com.codenameone.examples.bastion.account.DemoAccounts;

import com.codename1.backend.Json;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;

/// Signing in the way the app does: the password form, a code when the
/// account asks for one, then the authorization-code flow with PKCE.
final class SignIn {
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    static final String HOST = "bastion.test:8080";

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
        return post(path).contentType("application/x-www-form-urlencoded").content(body.toString())
                .header("Host", HOST);
    }

    static MockResponse password(MockMvc mvc, String username, String password) throws Exception {
        return mvc.perform(formPost("/login", "username", username, "password", password))
                .andReturn().getResponse();
    }

    static String session(MockMvc mvc, String username, String password) throws Exception {
        MockResponse answer = password(mvc, username, password);
        String redirect = answer.getRedirectedUrl();
        if (answer.getStatus() == 302 && redirect != null && redirect.indexOf("/login/mfa") >= 0
                && isDemoStaff(username)) {
            // The demo staff have an authenticator app; give its code the way
            // the staff member would, a step no earlier sign-in used.
            String pending = answer.getCookie("CN1SESSION");
            MockResponse coded = mvc.perform(formPost("/login/mfa", "code", staffCode(username))
                    .cookie("CN1SESSION", pending)).andReturn().getResponse();
            if (coded.getStatus() != 200) {
                return null;
            }
            String session = coded.getCookie("CN1SESSION");
            return session != null ? session : pending;
        }
        return answer.getStatus() == 200 ? answer.getCookie("CN1SESSION") : null;
    }

    private static boolean isDemoStaff(String username) {
        return DemoAccounts.ADMIN.equals(username) || DemoAccounts.SECOND_ADMIN.equals(username)
                || DemoAccounts.AUDITOR.equals(username);
    }

    /// The last step each demo staff member's code was used in: a step's code
    /// is good once, and the server takes the one before and after too.
    private static final java.util.Map<String, Long> USED_STEP =
            new java.util.HashMap<String, Long>();

    private static synchronized String staffCode(String username) throws Exception {
        Long last = USED_STEP.get(username);
        long step = System.currentTimeMillis() / 30000L - 1;
        if (last != null && last.longValue() >= step) {
            step = last.longValue() + 1;
        }
        while (step > System.currentTimeMillis() / 30000L + 1) {
            Thread.sleep(1000);
        }
        USED_STEP.put(username, Long.valueOf(step));
        return com.codename1.security.Otp.totp(
                com.codename1.security.Base32.decode(DemoAccounts.STAFF_TOTP_SECRET),
                step * 30000L, 30, 6, com.codename1.security.Hash.SHA1);
    }

    /// The authorization code a signed-in session gets, or null when the
    /// session is not signed in.
    static String code(MockMvc mvc, String session) throws Exception {
        String location = mvc.perform(get("/oauth2/authorize?response_type=code&client_id="
                        + SecurityConfig.CLIENT_ID + "&redirect_uri="
                        + OAuth2Parameters.encode(SecurityConfig.LOOPBACK_REDIRECT)
                        + "&scope=openid%20profile&state=s-1&nonce=n-1&code_challenge=" + CHALLENGE
                        + "&code_challenge_method=S256")
                        .header("Host", HOST).cookie("CN1SESSION", session))
                .andReturn().getResponse().getRedirectedUrl();
        if (location == null || !location.startsWith(SecurityConfig.LOOPBACK_REDIRECT)
                || location.indexOf("code=") < 0) {
            return null;
        }
        int at = location.indexOf("code=") + 5;
        int end = location.indexOf('&', at);
        return location.substring(at, end < 0 ? location.length() : end);
    }

    static Map exchange(MockMvc mvc, String session) throws Exception {
        String code = code(mvc, session);
        if (code == null) {
            throw new IllegalStateException("no authorization code");
        }
        String body = mvc.perform(formPost("/oauth2/token",
                        "grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", SecurityConfig.LOOPBACK_REDIRECT,
                        "code_verifier", VERIFIER,
                        "client_id", SecurityConfig.CLIENT_ID))
                .andReturn().getResponse().getContentAsString();
        return Json.parseObject(body);
    }

    static Map tokens(MockMvc mvc, String username, String password) throws Exception {
        String session = session(mvc, username, password);
        if (session == null) {
            throw new IllegalStateException("could not sign in as " + username);
        }
        return exchange(mvc, session);
    }

    static String token(MockMvc mvc, String username, String password) throws Exception {
        Object token = tokens(mvc, username, password).get("access_token");
        if (!(token instanceof String)) {
            throw new IllegalStateException("no access token for " + username);
        }
        return (String) token;
    }

    /// Opens an account through the API and answers its username.
    static String register(MockMvc mvc, String username, String password) throws Exception {
        int status = mvc.perform(post("/api/account/register").header("Host", HOST)
                        .contentType("application/json")
                        .content("{\"email\":\"" + username + "\",\"password\":\"" + password
                                + "\",\"displayName\":\"Test Person\"}"))
                .andReturn().getResponse().getStatus();
        if (status != 200) {
            throw new IllegalStateException("register answered " + status);
        }
        return username;
    }
}
