/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codename1.security.Base32;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.redirectedUrl;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.httpBasic;

/// The sign-in half end to end, in process: the flows the app's tests drive from
/// a device, here with the server's own client so that a device failure points at
/// the device. Every token below is one this server signed, and the secured
/// routes verify it with the key it was signed with.
@BackendTest
class SignInFlowTest {
    /// RFC 7636's own example pair: the challenge is the S256 of the verifier.
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String HOST = "10.0.2.2:8765";
    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    @Autowired
    private MockMvc mvc;

    private static String authorize(String scope, String redirect) {
        return "/oauth2/authorize?response_type=code&client_id=" + SecurityConfig.CLIENT_ID
                + "&redirect_uri=" + redirect + "&scope=" + scope + "&state=xyz&nonce=n-1"
                + "&code_challenge=" + CHALLENGE + "&code_challenge_method=S256";
    }

    /// The value of `name` in the query of `url`, as it is written there.
    private static String query(String url, String name) {
        int at = url.indexOf(name + "=", url.indexOf('?'));
        assertTrue(at > 0, name + " in " + url);
        int end = url.indexOf('&', at);
        return url.substring(at + name.length() + 1, end < 0 ? url.length() : end);
    }

    private Map tokens(String code, String redirect) throws Exception {
        String body = mvc.perform(post("/oauth2/token").header("Host", HOST)
                        .param("grant_type", "authorization_code").param("code", code)
                        .param("redirect_uri", redirect).param("code_verifier", VERIFIER)
                        .param("client_id", SecurityConfig.CLIENT_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.parseObject(body);
    }

    @Test
    void theIssuerIsTheAddressTheClientUsed() throws Exception {
        // The Android emulator's name for the host, and a LAN address: each client
        // is told endpoints under the address it asked at.
        mvc.perform(get("/.well-known/openid-configuration").header("Host", HOST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://10.0.2.2:8765"))
                .andExpect(jsonPath("$.token_endpoint").value("http://10.0.2.2:8765/oauth2/token"));
        mvc.perform(get("/.well-known/openid-configuration").header("Host", "192.168.1.20:9000"))
                .andExpect(jsonPath("$.issuer").value("http://192.168.1.20:9000"))
                .andExpect(jsonPath("$.device_authorization_endpoint")
                        .value("http://192.168.1.20:9000/oauth2/device_authorization"));
        mvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"));
    }

    @Test
    void anAuthorizationCodeBecomesTokensTheSecuredRoutesAccept() throws Exception {
        // A program, with nobody signed in: told so, not sent to a login page.
        mvc.perform(get(authorize("openid%20notes:read", "http://127.0.0.1/callback")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("login_required"));

        // The loopback address with a port the registration never named.
        String redirect = "http://127.0.0.1:53124/callback";
        String location = mvc.perform(get(authorize("openid%20profile%20notes:read", redirect))
                        .header("Host", HOST)
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD)))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();
        assertTrue(location.startsWith(redirect + "?code="), location);
        assertEquals("xyz", query(location, "state"));
        assertEquals("http%3A%2F%2F10.0.2.2%3A8765", query(location, "iss"));
        String code = query(location, "code");

        Map issued = tokens(code, redirect);
        assertEquals("Bearer", issued.get("token_type"));
        assertEquals("openid profile notes:read", issued.get("scope"));
        assertEquals(Long.valueOf(300), Long.valueOf(((Number) issued.get("expires_in")).longValue()));
        assertNotNull(issued.get("id_token"));
        String access = (String) issued.get("access_token");
        String refresh = (String) issued.get("refresh_token");
        assertNotNull(refresh);

        // A code is good once.
        mvc.perform(post("/oauth2/token").param("grant_type", "authorization_code")
                        .param("code", code).param("redirect_uri", redirect)
                        .param("code_verifier", VERIFIER)
                        .param("client_id", SecurityConfig.CLIENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));

        // Issued as 10.0.2.2 and used as 127.0.0.1: the token is verified by its
        // signature, whichever name the request came in under.
        mvc.perform(get("/api/secure/notes").header("Host", "127.0.0.1:8765")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("first"))
                .andExpect(jsonPath("$.length()").value(Long.valueOf(3)));
        mvc.perform(get("/api/secure/whoami").header("Authorization", "Bearer " + access))
                .andExpect(jsonPath("$.name").value("ada"))
                .andExpect(jsonPath("$.issuer").value("http://10.0.2.2:8765"));
        mvc.perform(get("/api/secure/summary/ada").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("ada"));
        // It was not asked for notes:write, so it does not have it.
        String denied = mvc.perform(get("/api/secure/admin")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getHeader("WWW-Authenticate");
        assertTrue(denied.startsWith("Bearer error=\"insufficient_scope\""), denied);
        // One character of the signature changed.
        char last = access.charAt(access.length() - 1);
        String forged = access.substring(0, access.length() - 1) + (last == 'A' ? 'B' : 'A');
        String refused = mvc.perform(get("/api/secure/notes")
                        .header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getHeader("WWW-Authenticate");
        assertTrue(refused.startsWith("Bearer error=\"invalid_token\""), refused);

        // The refresh token is replaced when it is used.
        String refreshed = mvc.perform(post("/oauth2/token").param("grant_type", "refresh_token")
                        .param("refresh_token", refresh)
                        .param("client_id", SecurityConfig.CLIENT_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map second = Json.parseObject(refreshed);
        assertNotNull(second.get("refresh_token"));
        assertFalse(refresh.equals(second.get("refresh_token")), "the refresh token rotates");
        mvc.perform(get("/api/secure/notes")
                        .header("Authorization", "Bearer " + second.get("access_token")))
                .andExpect(status().isOk());
    }

    @Test
    void theCustomSchemeRedirectAndTheScopeTheAppNeverAsksFor() throws Exception {
        String redirect = SecurityConfig.APP_REDIRECT;
        String location = mvc.perform(get(authorize("openid%20notes:write",
                        "com.codenameone.examples.hellocodenameone%3A%2Foauth2redirect"))
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD)))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();
        assertTrue(location.startsWith(redirect + "?code="), location);
        Map issued = tokens(query(location, "code"), redirect);
        mvc.perform(get("/api/secure/admin")
                        .header("Authorization", "Bearer " + issued.get("access_token")))
                .andExpect(status().isOk());
        // An address the client never registered is refused outright, not redirected to.
        mvc.perform(get(authorize("openid", "http://example.com/callback"))
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aFormSignInKeepsASessionAndAWrongPasswordDoesNot() throws Exception {
        mvc.perform(post("/login").param("username", SecurityConfig.USER)
                        .param("password", "not-the-password"))
                .andExpect(redirectedUrl("/login?error"));
        MockResponse signedIn = mvc.perform(post("/login").param("username", SecurityConfig.USER)
                        .param("password", SecurityConfig.USER_PASSWORD))
                .andExpect(redirectedUrl("/account/me"))
                .andReturn().getResponse();
        String session = signedIn.getCookie("CN1SESSION");
        assertNotNull(session);
        mvc.perform(get("/account/me").cookie("CN1SESSION", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ada"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"));
        // The session is what the authorization endpoint asks: no credentials now.
        mvc.perform(get(authorize("openid", "http://127.0.0.1/callback"))
                        .cookie("CN1SESSION", session))
                .andExpect(status().isFound());
        mvc.perform(get("/account/me")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void aUserWithASecondFactorIsNotSignedInUntilTheCode() throws Exception {
        MockResponse first = mvc.perform(post("/login").param("username", SecurityConfig.MFA_USER)
                        .param("password", SecurityConfig.MFA_USER_PASSWORD))
                .andExpect(redirectedUrl("/login/mfa"))
                .andReturn().getResponse();
        String pending = first.getCookie("CN1SESSION");
        assertNotNull(pending);
        mvc.perform(get("/account/me").cookie("CN1SESSION", pending))
                .andExpect(redirectedUrl("/login"));
        byte[] secret = Base32.decode(SecurityConfig.MFA_SECRET);
        // This step's code. Should the clock tick into the next step before the
        // server reads it, it is still accepted: one step either way is allowed.
        long now = System.currentTimeMillis();
        String code = Otp.totp(secret, now, 30, 6, Hash.SHA1);
        String wrong = "000000".equals(code) ? "000001" : "000000";
        mvc.perform(post("/login/mfa").cookie("CN1SESSION", pending).param("code", wrong))
                .andExpect(redirectedUrl("/login/mfa?error"));
        MockResponse done = mvc.perform(post("/login/mfa").cookie("CN1SESSION", pending)
                        .param("code", code))
                .andExpect(redirectedUrl("/account/me"))
                .andReturn().getResponse();
        // The id changed at sign-in; the one that waited is nobody's now.
        String session = done.getCookie("CN1SESSION");
        assertNotNull(session);
        assertFalse(pending.equals(session));
        mvc.perform(get("/account/me").cookie("CN1SESSION", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("grace"));

        // The same code in a new sign-in is refused: it was used.
        String again = mvc.perform(post("/login").param("username", SecurityConfig.MFA_USER)
                        .param("password", SecurityConfig.MFA_USER_PASSWORD))
                .andExpect(redirectedUrl("/login/mfa"))
                .andReturn().getResponse().getCookie("CN1SESSION");
        mvc.perform(post("/login/mfa").cookie("CN1SESSION", again).param("code", code))
                .andExpect(redirectedUrl("/login/mfa?error"));
        mvc.perform(get("/account/me").cookie("CN1SESSION", again))
                .andExpect(redirectedUrl("/login"));
        // The next step's code is good already, and is later than the one used:
        // a code of a step no later than one already accepted is spent with it.
        mvc.perform(post("/login/mfa").cookie("CN1SESSION", again)
                        .param("code", Otp.totp(secret, now + 30000L, 30, 6, Hash.SHA1)))
                .andExpect(redirectedUrl("/account/me"));
    }

    @Test
    void aDeviceIsApprovedByTheSignedInUserAndThenHoldsTokens() throws Exception {
        String started = mvc.perform(post("/oauth2/device_authorization").header("Host", HOST)
                        .param("client_id", SecurityConfig.CLIENT_ID)
                        .param("scope", "openid notes:read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value(Long.valueOf(5)))
                .andExpect(jsonPath("$.expires_in").value(Long.valueOf(300)))
                .andExpect(jsonPath("$.verification_uri")
                        .value("http://10.0.2.2:8765/oauth2/device_verification"))
                .andReturn().getResponse().getContentAsString();
        Map device = Json.parseObject(started);
        String deviceCode = (String) device.get("device_code");
        String userCode = (String) device.get("user_code");
        assertEquals(9, userCode.length(), userCode);

        // The user, signed in with Basic on each request; the session is only
        // where the question's one-use ticket is kept.
        MockResponse asked = mvc.perform(post("/oauth2/device_verification")
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD))
                        .param("user_code", userCode))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        String page = asked.getContentAsString();
        assertTrue(page.indexOf("Hello Codename One") > 0, page);
        String marker = "name=\"ticket\" value=\"";
        int at = page.indexOf(marker);
        assertTrue(at > 0, page);
        String ticket = page.substring(at + marker.length(),
                page.indexOf('"', at + marker.length()));
        String session = asked.getCookie("CN1SESSION");
        assertNotNull(session);
        String approved = mvc.perform(post("/oauth2/device_verification")
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD))
                        .cookie("CN1SESSION", session).param("user_code", userCode)
                        .param("ticket", ticket).param("decision", "approve"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(approved.indexOf("Device approved") > 0, approved);

        // The device's first poll: nothing to slow down yet.
        String polled = mvc.perform(post("/oauth2/token").param("grant_type", DEVICE_GRANT)
                        .param("device_code", deviceCode)
                        .param("client_id", SecurityConfig.CLIENT_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map issued = Json.parseObject(polled);
        assertEquals("openid notes:read", issued.get("scope"));
        mvc.perform(get("/api/secure/whoami")
                        .header("Authorization", "Bearer " + issued.get("access_token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ada"));
    }

    @Test
    void aDeviceIsToldToWaitThenToSlowDownAndThenThatItWasRefused() throws Exception {
        Map device = Json.parseObject(mvc.perform(post("/oauth2/device_authorization")
                        .param("client_id", SecurityConfig.CLIENT_ID).param("scope", "openid"))
                .andReturn().getResponse().getContentAsString());
        String deviceCode = (String) device.get("device_code");
        String userCode = (String) device.get("user_code");
        mvc.perform(post("/oauth2/token").param("grant_type", DEVICE_GRANT)
                        .param("device_code", deviceCode)
                        .param("client_id", SecurityConfig.CLIENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("authorization_pending"));
        // Again at once, well inside the five seconds it was told to leave.
        mvc.perform(post("/oauth2/token").param("grant_type", DEVICE_GRANT)
                        .param("device_code", deviceCode)
                        .param("client_id", SecurityConfig.CLIENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("slow_down"));

        // An answer that does not carry the ticket of a question is not an answer.
        MockResponse asked = mvc.perform(post("/oauth2/device_verification")
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD))
                        .param("user_code", userCode).param("decision", "approve"))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        String page = asked.getContentAsString();
        String marker = "name=\"ticket\" value=\"";
        int at = page.indexOf(marker);
        assertTrue(at > 0, page);
        String ticket = page.substring(at + marker.length(),
                page.indexOf('"', at + marker.length()));
        String refused = mvc.perform(post("/oauth2/device_verification")
                        .with(httpBasic(SecurityConfig.USER, SecurityConfig.USER_PASSWORD))
                        .cookie("CN1SESSION", asked.getCookie("CN1SESSION"))
                        .param("user_code", userCode).param("ticket", ticket)
                        .param("decision", "deny"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(refused.indexOf("Device refused") > 0, refused);
        // Nobody signed in at all is sent to sign in, and decides nothing.
        mvc.perform(post("/oauth2/device_verification").param("user_code", userCode))
                .andExpect(redirectedUrl("/login"));
    }
}
