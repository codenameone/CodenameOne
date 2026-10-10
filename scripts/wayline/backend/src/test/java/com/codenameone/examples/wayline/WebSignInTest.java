/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.wayline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Signing in from the app running in a browser, which sees only how each
/// request ended: no redirect it can stop at, and no cookie it can read.
@BackendTest
class WebSignInTest {
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String HERE = "http://" + SignIn.HOST + SecurityConfig.WEB_REDIRECT_PATH;

    @Autowired
    private MockMvc mvc;

    private String authorize(String session, String redirect) throws Exception {
        return mvc.perform(get("/oauth2/authorize?response_type=code&client_id="
                        + SecurityConfig.CLIENT_ID + "&redirect_uri=" + OAuth2Parameters.encode(redirect)
                        + "&scope=openid%20profile&state=s-web&code_challenge=" + CHALLENGE
                        + "&code_challenge_method=S256")
                        .header("Host", SignIn.HOST).cookie("CN1SESSION", session))
                .andReturn().getResponse().getRedirectedUrl();
    }

    @Test
    void thePasswordIsAnsweredWithAStatus() throws Exception {
        MockResponse accepted = mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST))
                .andExpect(status().isOk()).andReturn().getResponse();
        // Not a redirect: a browser would follow one and report the page at its end.
        assertNull(accepted.getRedirectedUrl());
        assertNotNull(accepted.getCookie("CN1SESSION"));

        MockResponse refused = mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", "not-the-password")
                        .header("Host", SignIn.HOST))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertNull(refused.getRedirectedUrl());
        assertNull(refused.getCookie("CN1SESSION"));
    }

    @Test
    void theCodeComesBackThroughThisServer() throws Exception {
        String session = SignIn.session(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        String location = authorize(session, HERE);
        assertNotNull(location);
        assertTrue(location.startsWith(HERE + "?"), location);

        // What the browser does next, and what the app reads.
        String answer = mvc.perform(get(location.substring(("http://" + SignIn.HOST).length()))
                        .header("Host", SignIn.HOST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("s-web"))
                .andReturn().getResponse().getContentAsString();
        String code = (String) Json.parseObject(answer).get("code");
        assertNotNull(code);

        Map tokens = Json.parseObject(mvc.perform(SignIn.formPost("/oauth2/token",
                        "grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", HERE,
                        "code_verifier", VERIFIER,
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String token = (String) tokens.get("access_token");
        assertNotNull(token);
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void aCodeIsNeverSentToAnotherServer() throws Exception {
        String session = SignIn.session(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        // Same path, somebody else's server: the address has to be this one.
        String elsewhere = authorize(session, "http://attacker.example" + SecurityConfig.WEB_REDIRECT_PATH);
        assertTrue(elsewhere == null || elsewhere.indexOf("attacker.example") < 0, String.valueOf(elsewhere));
        String otherPath = authorize(session, "http://" + SignIn.HOST + "/somewhere/else");
        assertTrue(otherPath == null || otherPath.indexOf("code=") < 0, String.valueOf(otherPath));
    }

    @Test
    void theInstalledAppStillSignsInItsOwnWay() throws Exception {
        assertNotNull(SignIn.token(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD));
    }

    @Test
    void theCodePageSaysWhenThereIsNoCode() throws Exception {
        mvc.perform(get(SecurityConfig.WEB_REDIRECT_PATH + "?error=access_denied&state=s-web")
                        .header("Host", SignIn.HOST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("access_denied"));
    }

    @Test
    void aSignInAnotherSiteSubmittedIsRefused() throws Exception {
        MockResponse forged = mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST).header("Origin", "http://attacker.example"))
                .andExpect(status().isForbidden()).andReturn().getResponse();
        assertNull(forged.getCookie("CN1SESSION"));
        // A page in a sandboxed frame, or opened from a file.
        mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST).header("Origin", "null"))
                .andExpect(status().isForbidden());
        // The same name on another port or scheme is another site.
        mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST).header("Origin", "https://" + SignIn.HOST))
                .andExpect(status().isForbidden());
        mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST).header("Origin", "http://wayline.test:9090"))
                .andExpect(status().isForbidden());
        // The rest of what changes something on this chain is held to it too.
        mvc.perform(SignIn.formPost("/oauth2/token", "grant_type", "authorization_code")
                        .header("Host", SignIn.HOST).header("Origin", "http://attacker.example"))
                .andExpect(status().isForbidden());
        mvc.perform(SignIn.formPost("/logout")
                        .header("Host", SignIn.HOST).header("Origin", "http://attacker.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aSignInFromThisServersOwnPageIsAccepted() throws Exception {
        // A browser spells the host in lower case; the comparison does not care.
        MockResponse own = mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST).header("Origin", "HTTP://WAYLINE.TEST:8080"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertNotNull(own.getCookie("CN1SESSION"));
        // And the installed app, which is not a browser and names no origin.
        assertNotNull(SignIn.session(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD));
    }

    @Test
    void theOriginIsHeldToTheIssuerWhenOneIsConfigured() {
        assertTrue(SecurityConfig.sameOrigin("https://wayline.example", "https://wayline.example"));
        assertTrue(SecurityConfig.sameOrigin("https://WAYLINE.example", "https://wayline.example/auth"));
        assertTrue(!SecurityConfig.sameOrigin("https://wayline.example.attacker.example",
                "https://wayline.example"));
        assertTrue(!SecurityConfig.sameOrigin("http://wayline.example", "https://wayline.example"));
        assertTrue(!SecurityConfig.sameOrigin("https://wayline.example:8443", "https://wayline.example"));
        // With an issuer the address a request arrived at is never consulted.
        assertEquals("https://wayline.example", SecurityConfig.here("https://wayline.example"));
        // With none, and no request in hand, there is no address to accept.
        assertNull(SecurityConfig.here(""));
    }

    @Test
    void theSessionCookieIsNotSentFromOtherSitesOrReadByScripts() throws Exception {
        String header = mvc.perform(SignIn.formPost("/login",
                        "username", DemoAccounts.RIDER, "password", DemoAccounts.PASSWORD)
                        .header("Host", SignIn.HOST))
                .andReturn().getResponse().getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.indexOf("HttpOnly") > 0, header);
        assertTrue(header.indexOf("SameSite=Lax") > 0 || header.indexOf("SameSite=Strict") > 0, header);
    }

    @Test
    void theSessionEndsWhenTheAppHasItsTokens() throws Exception {
        String session = SignIn.session(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        String location = authorize(session, HERE);
        assertTrue(location.indexOf("code=") > 0, location);
        String answer = mvc.perform(get(location.substring(("http://" + SignIn.HOST).length()))
                        .header("Host", SignIn.HOST))
                .andReturn().getResponse().getContentAsString();
        Map tokens = Json.parseObject(mvc.perform(SignIn.formPost("/oauth2/token",
                        "grant_type", "authorization_code",
                        "code", (String) Json.parseObject(answer).get("code"),
                        "redirect_uri", HERE,
                        "code_verifier", VERIFIER,
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andReturn().getResponse().getContentAsString());

        // Asking nicely does not end it: only a POST does.
        mvc.perform(get("/logout").header("Host", SignIn.HOST).cookie("CN1SESSION", session));
        assertTrue(authorize(session, HERE).indexOf("code=") > 0);

        MockResponse ended = mvc.perform(SignIn.formPost("/logout")
                        .header("Host", SignIn.HOST).header("Origin", "http://" + SignIn.HOST)
                        .cookie("CN1SESSION", session))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertNull(ended.getRedirectedUrl());
        // The cookie earns no more codes...
        String after = authorize(session, HERE);
        assertTrue(after == null || after.indexOf("code=") < 0, String.valueOf(after));
        // ...and the tokens it earned are untouched.
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + tokens.get("access_token")))
                .andExpect(status().isOk());
        mvc.perform(SignIn.formPost("/oauth2/token",
                        "grant_type", "refresh_token",
                        "refresh_token", (String) tokens.get("refresh_token"),
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andExpect(status().isOk());
    }

    @Test
    void theCodePageIsDataAndNothingElse() throws Exception {
        MockResponse page = mvc.perform(get(SecurityConfig.WEB_REDIRECT_PATH
                        + "?code=%3Cscript%3Ealert(1)%3C/script%3E&state=s").header("Host", SignIn.HOST))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(String.valueOf(page.getContentType()).startsWith("application/json"),
                String.valueOf(page.getContentType()));
        assertEquals("nosniff", page.getHeader("X-Content-Type-Options"));
        assertEquals("no-store", page.getHeader("Cache-Control"));
    }

    @Test
    void anIssuerIsTheOnlyWebAddressWhenOneIsConfigured() {
        com.codename1.backend.security.oauth2.server.authorization.RegisteredClient app =
                com.codename1.backend.security.oauth2.server.authorization.RegisteredClient
                        .withId("wayline").clientId(SecurityConfig.CLIENT_ID)
                        .clientAuthenticationMethod(com.codename1.backend.security.oauth2.core
                                .ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(com.codename1.backend.security.oauth2.core
                                .AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri(SecurityConfig.LOOPBACK_REDIRECT).build();
        java.util.Set<String> uris = SecurityConfig.withWebRedirect(app, "https://wayline.example")
                .getRedirectUris();
        assertTrue(uris.contains("https://wayline.example/signin/code"), uris.toString());
        assertTrue(uris.contains(SecurityConfig.LOOPBACK_REDIRECT), uris.toString());
        assertEquals(2, uris.size(), uris.toString());
        // With none configured and no request in hand, nothing is added.
        assertEquals(1, SecurityConfig.withWebRedirect(app, "").getRedirectUris().size());
    }
}
