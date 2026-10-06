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
package com.codename1.backend.security;

import static com.codename1.backend.security.OAuth2Testing.form;
import static com.codename1.backend.security.OAuth2Testing.json;
import static com.codename1.backend.security.OAuth2Testing.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2TokenContext;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.TokenSettings;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// The authorization server, through a real chain: every grant, and every
/// refusal with the error it is refused by.
class AuthorizationServerTest {
    private static final String APP_REDIRECT = "com.acme.app:/oauth2redirect";
    private static final String WEB_REDIRECT = "https://web.example.com/login/oauth2/code/own";
    private static final String FORM = "application/x-www-form-urlencoded";

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, request.pathFrom(0) + " "
                    + (who == null ? "nobody" : who.getName()));
        }
    };

    @TempDir
    File dir;

    private final OAuth2Testing.Ticking clock = new OAuth2Testing.Ticking();
    private final InMemoryOAuth2AuthorizationService grants = new InMemoryOAuth2AuthorizationService();
    private String issuer;

    static RegisteredClientRepository clients() {
        return new InMemoryRegisteredClientRepository(
                RegisteredClient.withId("1").clientId("app").clientName("Acme App")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                        .redirectUri(APP_REDIRECT).redirectUri("http://127.0.0.1/cb")
                        .scope("openid").scope("profile").scope("email").scope("orders:read")
                        .build(),
                RegisteredClient.withId("2").clientId("web")
                        .clientSecret(OAuth2Testing.PLAIN.encode("web-secret"))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .redirectUri(WEB_REDIRECT)
                        .scope("openid").scope("profile").scope("api")
                        .tokenSettings(TokenSettings.builder().reuseRefreshTokens(true).build())
                        .build(),
                RegisteredClient.withId("3").clientId("service")
                        .clientSecret(OAuth2Testing.PLAIN.encode("service-secret"))
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("api").build());
    }

    private static Object[] users() {
        return new Object[] {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())};
    }

    private SecuredServer start() throws Exception {
        return start(as -> { });
    }

    private SecuredServer start(Customizer<AuthorizationServerConfigurer> more) throws Exception {
        int port = OAuth2Testing.freePort();
        issuer = "http://127.0.0.1:" + port;
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, issuer);
        final RegisteredClientRepository clients = clients();
        return SecuredServer.start(settings, "test", users(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .authorizationServer(as -> {
                            as.registeredClientRepository(clients).authorizationService(grants)
                                    .clientSecretEncoder(OAuth2Testing.PLAIN).clock(clock);
                            more.customize(as);
                        }).build());
    }

    private static String authorizeUrl(String client, String redirect, String scope,
                                       String verifier, String state, String nonce) {
        return "/oauth2/authorize?" + form("response_type", "code", "client_id", client,
                "redirect_uri", redirect, "scope", scope, "state", state, "nonce", nonce,
                "code_challenge", verifier == null ? null : OAuth2Parameters.sha256(verifier),
                "code_challenge_method", verifier == null ? null : "S256");
    }

    /// A browser at the authorization endpoint, signing in as ada when it is
    /// asked to; answers the redirect back to the client.
    private Reply authorize(SecuredServer server, String url) throws Exception {
        Reply reply = server.get(url, "Accept", "text/html");
        if (reply.status == 302 && "/login".equals(reply.header("Location"))) {
            Reply signedIn = OAuth2Testing.signIn(server, "ada", "ada-pw");
            assertEquals(302, signedIn.status, signedIn.toString());
            // Back to the very request that was interrupted, query and all.
            assertEquals(url, signedIn.header("Location"));
            reply = server.get(signedIn.header("Location"), "Accept", "text/html");
        }
        return reply;
    }

    private String code(SecuredServer server, String client, String redirect, String scope,
                        String verifier) throws Exception {
        Reply back = authorize(server, authorizeUrl(client, redirect, scope, verifier, "st-1",
                "n-1"));
        assertEquals(302, back.status, back.toString());
        Map<String, String> answer = query(back.header("Location"));
        assertNotNull(answer.get("code"), back.toString());
        return answer.get("code");
    }

    private static Reply token(SecuredServer server, String... pairs) throws Exception {
        return server.call("POST", "/oauth2/token", form(pairs), FORM);
    }

    private static void refused(Reply reply, int status, String error) throws Exception {
        assertEquals(status, reply.status, reply.toString());
        assertEquals(error, json(reply).get("error"), reply.toString());
        assertEquals("no-store", reply.header("Cache-Control"), reply.toString());
    }

    // ------------------------------------------------------------- metadata

    @Test
    @DisplayName("the metadata names every endpoint a client looks for, under the issuer")
    void metadata() throws Exception {
        try (SecuredServer server = start()) {
            for (String path : new String[] {"/.well-known/openid-configuration",
                "/.well-known/oauth-authorization-server"}) {
                Reply reply = server.get(path);
                assertEquals(200, reply.status, reply.toString());
                Map m = json(reply);
                // What com.codename1.io.oidc.OidcConfiguration reads, field for field.
                assertEquals(issuer, m.get("issuer"));
                assertEquals(issuer + "/oauth2/authorize", m.get("authorization_endpoint"));
                assertEquals(issuer + "/oauth2/token", m.get("token_endpoint"));
                assertEquals(issuer + "/userinfo", m.get("userinfo_endpoint"));
                assertEquals(issuer + "/oauth2/revoke", m.get("revocation_endpoint"));
                assertEquals(issuer + "/oauth2/jwks", m.get("jwks_uri"));
                // No RP-initiated logout, and none advertised.
                assertNull(m.get("end_session_endpoint"));
                assertEquals(issuer + "/oauth2/device_authorization",
                        m.get("device_authorization_endpoint"));
                assertEquals(Arrays.asList("S256"), m.get("code_challenge_methods_supported"));
                assertEquals(Arrays.asList("code"), m.get("response_types_supported"));
                assertEquals(Arrays.asList("RS256"), m.get("id_token_signing_alg_values_supported"));
                assertTrue(((List) m.get("grant_types_supported")).contains(
                        "urn:ietf:params:oauth:grant-type:device_code"));
                assertTrue(((List) m.get("token_endpoint_auth_methods_supported"))
                        .containsAll(Arrays.asList("client_secret_basic", "client_secret_post",
                                "none")));
            }
            // A post there is a browser's forged one before it is anything else.
            assertEquals(403, server.post("/.well-known/openid-configuration", "").status);
        }
    }

    @Test
    @DisplayName("the paths can be moved, and the metadata follows")
    void movedEndpoints() throws Exception {
        try (SecuredServer server = start(as -> as.settings(AuthorizationServerSettings.builder()
                .tokenEndpoint("/connect/token").jwkSetEndpoint("/keys").build()))) {
            Map m = json(server.get("/.well-known/openid-configuration"));
            assertEquals(issuer + "/connect/token", m.get("token_endpoint"));
            assertEquals(issuer + "/keys", m.get("jwks_uri"));
            assertEquals(200, server.get("/keys").status);
            refused(server.call("POST", "/connect/token", form("grant_type", "client_credentials",
                    "client_id", "nobody", "client_secret", "x"), FORM), 401, "invalid_client");
            // The old path is the application's again: the chain guards it.
            assertEquals(302, server.get("/oauth2/jwks", "Accept", "text/html").status);
        }
    }

    // ------------------------------------------------ authorization code + PKCE

    @Test
    @DisplayName("authorization code with PKCE, end to end, for a public client")
    void authorizationCode() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            String url = authorizeUrl("app", APP_REDIRECT, "openid profile orders:read", verifier,
                    "st-7", "nonce-7");
            // Signed out: a browser is sent to sign in.
            Reply first = server.get(url, "Accept", "text/html");
            assertEquals(302, first.status);
            assertEquals("/login", first.header("Location"));
            Reply back = authorize(server, url);
            assertEquals(302, back.status, back.toString());
            String location = back.header("Location");
            assertTrue(location.startsWith(APP_REDIRECT + "?code="), location);
            assertEquals("no-store", back.header("Cache-Control"));
            Map<String, String> answer = query(location);
            assertEquals("st-7", answer.get("state"));
            assertEquals(issuer, answer.get("iss"));
            // 256 bits, in the URL-safe alphabet.
            assertEquals(43, answer.get("code").length());

            long before = clock.now / 1000L;
            Reply reply = token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", answer.get("code"), "redirect_uri", APP_REDIRECT, "code_verifier",
                    verifier);
            assertEquals(200, reply.status, reply.toString());
            assertEquals("no-store", reply.header("Cache-Control"));
            assertEquals("no-cache", reply.header("Pragma"));
            Map tokens = json(reply);
            // What com.codename1.io.oidc.OidcTokens reads.
            assertEquals("Bearer", tokens.get("token_type"));
            assertEquals(300L, ((Number) tokens.get("expires_in")).longValue());
            assertEquals("openid profile orders:read", tokens.get("scope"));
            assertEquals(43, ((String) tokens.get("refresh_token")).length());

            String jwks = server.get("/oauth2/jwks").body;
            assertFalse(jwks.contains("\"d\""), "the JWK Set publishes a private key");
            Jwt access = OAuth2Testing.verify((String) tokens.get("access_token"), jwks);
            assertEquals("at+jwt", access.getHeaders().get("typ"));
            assertEquals("RS256", access.getHeaders().get("alg"));
            assertEquals(issuer, access.getIssuer());
            assertEquals("ada", access.getSubject());
            assertEquals(Arrays.asList("app"), access.getAudience());
            assertEquals("app", access.getClaimAsString("client_id"));
            assertEquals("openid profile orders:read", access.getClaimAsString("scope"));
            assertEquals(before + 300, access.getExpiresAt().longValue());

            Jwt id = OAuth2Testing.verify((String) tokens.get("id_token"), jwks);
            assertEquals(issuer, id.getIssuer());
            assertEquals("ada", id.getSubject());
            assertEquals(Arrays.asList("app"), id.getAudience());
            assertEquals("app", id.getClaimAsString("azp"));
            assertEquals("nonce-7", id.getClaimAsString("nonce"));
            assertEquals(before, id.getClaimAsLong("auth_time").longValue());
            assertEquals("ada", id.getClaimAsString("preferred_username"));
            // at_hash: the left half of the access token's SHA-256.
            byte[] digest = com.codename1.backend.Crypto.sha256(
                    ((String) tokens.get("access_token")).getBytes("US-ASCII"));
            assertEquals(com.codename1.backend.Base64Url.encode(Arrays.copyOf(digest, 16)),
                    id.getClaimAsString("at_hash"));

            // Signed in now: the next request is answered at once.
            Reply again = server.get(authorizeUrl("app", APP_REDIRECT, "openid",
                    OAuth2Parameters.random(32), "st-8", null), "Accept", "text/html");
            assertEquals(302, again.status);
            assertEquals("st-8", query(again.header("Location")).get("state"));
        }
    }

    @Test
    @DisplayName("a request whose client or redirect address is wrong is never redirected")
    void authorizeRefusedWithoutRedirect() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            Reply unknown = server.get(authorizeUrl("nobody", APP_REDIRECT, "openid", verifier,
                    "s", null), "Accept", "text/html");
            refused(unknown, 400, "invalid_request");
            assertNull(unknown.header("Location"));
            assertEquals("Unknown client_id", json(unknown).get("error_description"));

            for (String wrong : new String[] {"https://evil.example/cb", APP_REDIRECT + "/more",
                "com.acme.app:/oauth2redirect#frag", "http://127.0.0.1.evil.example/cb",
                "http://127.0.0.1:99/other", "https://127.0.0.1/cb", "http://127.0.0.1:1x/cb"}) {
                Reply reply = server.get(authorizeUrl("app", wrong, "openid", verifier, "s", null),
                        "Accept", "text/html");
                refused(reply, 400, "invalid_request");
                assertNull(reply.header("Location"), wrong);
                assertEquals("The redirect_uri is not one the client registered",
                        json(reply).get("error_description"), wrong);
            }
            // Two addresses registered, none named: there is no telling which.
            assertEquals(400, server.get("/oauth2/authorize?response_type=code&client_id=app",
                    "Accept", "text/html").status);
            assertEquals(403, server.post("/oauth2/authorize", "client_id=app").status,
                    "the authorization endpoint is not excused from CSRF protection");
        }
    }

    @Test
    @DisplayName("a loopback redirect address may name any port (RFC 8252)")
    void loopbackPort() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            String redirect = "http://127.0.0.1:53712/cb";
            Reply back = authorize(server, authorizeUrl("app", redirect, "openid", verifier, "s",
                    null));
            assertEquals(302, back.status, back.toString());
            assertTrue(back.header("Location").startsWith(redirect + "?code="));
            // The exchange repeats the address it asked with, port and all.
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    query(back.header("Location")).get("code"), "redirect_uri",
                    "http://127.0.0.1:1/cb", "code_verifier", verifier), 400, "invalid_grant");
        }
    }

    @Test
    @DisplayName("once the address is the client's, a refusal goes back to it with the state")
    void authorizeRefusedByRedirect() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            String[][] cases = {
                {authorizeUrl("app", APP_REDIRECT, "openid admin", verifier, "s1", null),
                    "invalid_scope"},
                // A public client without PKCE.
                {authorizeUrl("app", APP_REDIRECT, "openid", null, "s1", null), "invalid_request"},
                {"/oauth2/authorize?" + form("response_type", "code", "client_id", "app",
                        "redirect_uri", APP_REDIRECT, "state", "s1", "code_challenge",
                        OAuth2Parameters.sha256(verifier), "code_challenge_method", "plain"),
                    "invalid_request"},
                // No method at all reads as plain, and is refused the same.
                {"/oauth2/authorize?" + form("response_type", "code", "client_id", "app",
                        "redirect_uri", APP_REDIRECT, "state", "s1", "code_challenge",
                        OAuth2Parameters.sha256(verifier)), "invalid_request"},
                {"/oauth2/authorize?" + form("response_type", "token", "client_id", "app",
                        "redirect_uri", APP_REDIRECT, "state", "s1"), "unsupported_response_type"},
                // Nobody signed in, and the client asked not to be shown a page.
                {authorizeUrl("app", APP_REDIRECT, "openid", verifier, "s1", null)
                        + "&prompt=none", "login_required"}};
            for (String[] one : cases) {
                Reply reply = server.get(one[0], "Accept", "text/html");
                assertEquals(302, reply.status, one[0] + " " + reply);
                assertTrue(reply.header("Location").startsWith(APP_REDIRECT + "?error="), one[0]);
                Map<String, String> answer = query(reply.header("Location"));
                assertEquals(one[1], answer.get("error"), one[0]);
                assertEquals("s1", answer.get("state"));
                assertEquals(issuer, answer.get("iss"));
                assertNull(answer.get("code"));
            }
            // A client registered without the grant.
            Reply noGrant = server.get("/oauth2/authorize?" + form("response_type", "code",
                    "client_id", "service", "state", "s1"), "Accept", "text/html");
            assertEquals(400, noGrant.status, "service has no redirect address at all");
        }
    }

    @Test
    @DisplayName("a client that is not a browser is told to sign in, not sent to a page")
    void headless() throws Exception {
        try (SecuredServer server = start()) {
            String url = authorizeUrl("app", APP_REDIRECT, "openid", OAuth2Parameters.random(32),
                    "s", null);
            for (String accept : new String[] {null, "application/json", "*/*"}) {
                Reply reply = accept == null ? server.get(url) : server.get(url, "Accept", accept);
                refused(reply, 401, "login_required");
                assertNull(reply.header("Location"));
            }
            // And signed in, it is answered like any other.
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            Reply reply = server.get(url, "Accept", "application/json");
            assertEquals(302, reply.status, reply.toString());
            assertNotNull(query(reply.header("Location")).get("code"));
        }
    }

    @Test
    @DisplayName("the exchange is refused for a wrong verifier, address or client, each as invalid_grant")
    void exchangeRefused() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            // A wrong verifier, and the code is spent by the attempt.
            String code = code(server, "app", APP_REDIRECT, "openid", verifier);
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", APP_REDIRECT, "code_verifier",
                    OAuth2Parameters.random(32)), 400, "invalid_grant");
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", APP_REDIRECT, "code_verifier", verifier), 400,
                    "invalid_grant");
            // No verifier.
            code = code(server, "app", APP_REDIRECT, "openid", verifier);
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", APP_REDIRECT), 400, "invalid_grant");
            // Another address than the one asked with; and none.
            code = code(server, "app", APP_REDIRECT, "openid", verifier);
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", "http://127.0.0.1/cb", "code_verifier", verifier), 400,
                    "invalid_grant");
            code = code(server, "app", APP_REDIRECT, "openid", verifier);
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "code_verifier", verifier), 400, "invalid_grant");
            // Another client's code.
            code = code(server, "app", APP_REDIRECT, "openid", verifier);
            refused(server.call("POST", "/oauth2/token", form("grant_type", "authorization_code",
                    "code", code, "redirect_uri", APP_REDIRECT, "code_verifier", verifier), FORM,
                    "Authorization", OAuth2Testing.basic("web", "web-secret")), 400,
                    "invalid_grant");
            // A code nobody issued, and none.
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    OAuth2Parameters.random(32), "redirect_uri", APP_REDIRECT, "code_verifier",
                    verifier), 400, "invalid_grant");
            refused(token(server, "grant_type", "authorization_code", "client_id", "app"), 400,
                    "invalid_request");
            // An expired one: five minutes and a second later.
            code = code(server, "app", APP_REDIRECT, "openid", verifier);
            clock.now += 301000;
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", APP_REDIRECT, "code_verifier", verifier), 400,
                    "invalid_grant");
            // A verifier for a request that had no challenge: the challenge was
            // stripped on the way.
            Reply back = authorize(server, authorizeUrl("web", WEB_REDIRECT, "openid", null, "s",
                    null));
            refused(server.call("POST", "/oauth2/token", form("grant_type", "authorization_code",
                    "code", query(back.header("Location")).get("code"), "redirect_uri",
                    WEB_REDIRECT, "code_verifier", verifier), FORM, "Authorization",
                    OAuth2Testing.basic("web", "web-secret")), 400, "invalid_grant");
        }
    }

    @Test
    @DisplayName("a code presented twice revokes what its first use was issued")
    void codeReuse() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            String code = code(server, "app", APP_REDIRECT, "openid profile", verifier);
            Map tokens = json(token(server, "grant_type", "authorization_code", "client_id",
                    "app", "code", code, "redirect_uri", APP_REDIRECT, "code_verifier", verifier));
            String access = (String) tokens.get("access_token");
            assertEquals(200, server.get("/userinfo", "Authorization", "Bearer " + access).status);

            // At once -- the same request arriving twice: refused, and no more.
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", APP_REDIRECT, "code_verifier", verifier), 400,
                    "invalid_grant");
            assertEquals(200, server.get("/userinfo", "Authorization", "Bearer " + access).status);
            // Later, it is somebody else presenting it.
            clock.now += 10000;
            refused(token(server, "grant_type", "authorization_code", "client_id", "app", "code",
                    code, "redirect_uri", APP_REDIRECT, "code_verifier", verifier), 400,
                    "invalid_grant");
            // The refresh token of the first use is gone, and so is the access
            // token's standing here.
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", (String) tokens.get("refresh_token")), 400, "invalid_grant");
            Reply info = server.get("/userinfo", "Authorization", "Bearer " + access);
            assertEquals(401, info.status);
            assertTrue(info.header("WWW-Authenticate").contains("error=\"invalid_token\""));
        }
    }

    // --------------------------------------------------------- the client

    @Test
    @DisplayName("an unknown client and a wrong secret are both invalid_client, with a 401")
    void clientAuthentication() throws Exception {
        try (SecuredServer server = start()) {
            refused(token(server, "grant_type", "client_credentials", "client_id", "nobody",
                    "client_secret", "x"), 401, "invalid_client");
            refused(token(server, "grant_type", "client_credentials"), 401, "invalid_client");
            Reply wrong = server.call("POST", "/oauth2/token", form("grant_type",
                    "client_credentials"), FORM, "Authorization",
                    OAuth2Testing.basic("service", "nope"));
            refused(wrong, 401, "invalid_client");
            assertEquals("Basic realm=\"oauth2\"", wrong.header("WWW-Authenticate"));
            // The answer says nothing of which of the two was wrong, nor the secret.
            assertEquals("Client authentication failed", json(wrong).get("error_description"));
            refused(token(server, "grant_type", "client_credentials", "client_id", "service",
                    "client_secret", "nope"), 401, "invalid_client");
            // "service" was registered for Basic only: the same secret, posted,
            // is not how it authenticates.
            refused(token(server, "grant_type", "client_credentials", "client_id", "service",
                    "client_secret", "service-secret"), 401, "invalid_client");
            // A confidential client cannot pass itself off as a public one.
            refused(token(server, "grant_type", "refresh_token", "client_id", "web",
                    "refresh_token", "x"), 401, "invalid_client");
            refused(server.call("POST", "/oauth2/token", form("grant_type", "client_credentials"),
                    FORM, "Authorization", "Basic !!!"), 401, "invalid_client");
            assertEquals(405, server.get("/oauth2/token").status);
            refused(token(server, "grant_type", "password", "client_id", "app", "username", "ada",
                    "password", "ada-pw"), 400, "unsupported_grant_type");
            refused(token(server, "client_id", "app"), 400, "invalid_request");
        }
    }

    @Test
    @DisplayName("client credentials: a token for the client itself, with no refresh token")
    void clientCredentials() throws Exception {
        try (SecuredServer server = start()) {
            Reply reply = server.call("POST", "/oauth2/token", form("grant_type",
                    "client_credentials", "scope", "api"), FORM, "Authorization",
                    OAuth2Testing.basic("service", "service-secret"));
            assertEquals(200, reply.status, reply.toString());
            Map tokens = json(reply);
            assertNull(tokens.get("refresh_token"));
            assertNull(tokens.get("id_token"));
            assertEquals("api", tokens.get("scope"));
            Jwt access = OAuth2Testing.verify((String) tokens.get("access_token"),
                    server.get("/oauth2/jwks").body);
            assertEquals("service", access.getSubject());
            assertEquals("api", access.getClaimAsString("scope"));
            // A client's own token is nobody's at the user info endpoint.
            assertEquals(401, server.get("/userinfo", "Authorization", "Bearer "
                    + tokens.get("access_token")).status);

            // Posted credentials, for the client registered for them; no scope
            // asked, none granted.
            Map posted = json(token(server, "grant_type", "client_credentials", "client_id", "web",
                    "client_secret", "web-secret"));
            assertNull(posted.get("scope"));
            assertNotNull(posted.get("access_token"));

            refused(server.call("POST", "/oauth2/token", form("grant_type", "client_credentials",
                    "scope", "api admin"), FORM, "Authorization",
                    OAuth2Testing.basic("service", "service-secret")), 400, "invalid_scope");
            // A grant the client was not registered for.
            refused(token(server, "grant_type", "client_credentials", "client_id", "app"), 400,
                    "unauthorized_client");
            refused(server.call("POST", "/oauth2/token", form("grant_type", "refresh_token",
                    "refresh_token", "x"), FORM, "Authorization",
                    OAuth2Testing.basic("service", "service-secret")), 400, "unauthorized_client");
        }
    }

    // ------------------------------------------------------ refresh tokens

    @Test
    @DisplayName("a refresh token is replaced on every use, and using a replaced one revokes the grant")
    void refreshRotation() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            Map first = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "openid profile orders:read",
                            verifier), "redirect_uri", APP_REDIRECT, "code_verifier", verifier));
            String r1 = (String) first.get("refresh_token");
            // Only its hash is kept.
            assertNull(grants.findToken(OAuth2AuthorizationService.REFRESH_TOKEN, r1));
            assertNotNull(grants.findToken(OAuth2AuthorizationService.REFRESH_TOKEN,
                    OAuth2Parameters.sha256(r1)));

            clock.now += 60000;
            Reply reply = token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", r1, "scope", "openid orders:read");
            assertEquals(200, reply.status, reply.toString());
            Map second = json(reply);
            String r2 = (String) second.get("refresh_token");
            assertNotEquals(r1, r2);
            assertEquals("openid orders:read", second.get("scope"));
            assertNotEquals(first.get("access_token"), second.get("access_token"));
            Jwt id = OAuth2Testing.verify((String) second.get("id_token"),
                    server.get("/oauth2/jwks").body);
            // A refreshed ID token still says when the user signed in.
            assertEquals((clock.now - 60000) / 1000L, id.getClaimAsLong("auth_time").longValue());

            // More than the grant has.
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", r2, "scope", "openid email"), 400, "invalid_scope");
            // That refusal used nothing up.
            Map third = json(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", r2));
            String r3 = (String) third.get("refresh_token");
            assertEquals("openid profile orders:read", third.get("scope"));

            // The one just replaced, at once: a client refreshing twice at the
            // same moment. Refused, and the grant stands.
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", r2), 400, "invalid_grant");
            assertEquals(200, server.get("/userinfo", "Authorization", "Bearer "
                    + third.get("access_token")).status);
            // The first one again, later: somebody has a copy. Everything goes.
            clock.now += 10000;
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", r1), 400, "invalid_grant");
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", r3), 400, "invalid_grant");
            assertEquals(401, server.get("/userinfo", "Authorization", "Bearer "
                    + third.get("access_token")).status);

            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", OAuth2Parameters.random(32)), 400, "invalid_grant");
            refused(token(server, "grant_type", "refresh_token", "client_id", "app"), 400,
                    "invalid_request");
        }
    }

    @Test
    @DisplayName("a refresh token expires an hour after its last use, and a client may keep its own")
    void refreshExpiryAndReuse() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            Map first = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "openid", verifier), "redirect_uri",
                    APP_REDIRECT, "code_verifier", verifier));
            clock.now += 3599000;
            Map second = json(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", (String) first.get("refresh_token")));
            clock.now += 3601000;
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", (String) second.get("refresh_token")), 400, "invalid_grant");

            // "web" was registered to keep its refresh token.
            Reply back = authorize(server, authorizeUrl("web", WEB_REDIRECT, "openid api", null,
                    "s", null));
            String basic = OAuth2Testing.basic("web", "web-secret");
            Map web = json(server.call("POST", "/oauth2/token", form("grant_type",
                    "authorization_code", "code", query(back.header("Location")).get("code"),
                    "redirect_uri", WEB_REDIRECT), FORM, "Authorization", basic));
            String kept = (String) web.get("refresh_token");
            for (int round = 0 ; round < 3 ; round++) {
                Map again = json(server.call("POST", "/oauth2/token", form("grant_type",
                        "refresh_token", "refresh_token", kept), FORM, "Authorization", basic));
                assertEquals(kept, again.get("refresh_token"));
            }
            // And it is the client's alone.
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", kept), 400, "invalid_grant");
        }
    }

    // ---------------------------------------------------------- revocation

    @Test
    @DisplayName("revocation ends a grant, for the client it was made to and nobody else")
    void revocation() throws Exception {
        try (SecuredServer server = start()) {
            String verifier = OAuth2Parameters.random(32);
            Map tokens = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "openid", verifier), "redirect_uri",
                    APP_REDIRECT, "code_verifier", verifier));
            String refresh = (String) tokens.get("refresh_token");
            String access = (String) tokens.get("access_token");

            // Another client asking is answered 200 and changes nothing.
            Reply other = server.call("POST", "/oauth2/revoke", form("token", refresh), FORM,
                    "Authorization", OAuth2Testing.basic("web", "web-secret"));
            assertEquals(200, other.status, other.toString());
            assertEquals(200, server.get("/userinfo", "Authorization", "Bearer " + access).status);
            // So is a token nobody issued (RFC 7009).
            assertEquals(200, server.call("POST", "/oauth2/revoke", form("token", "nonsense",
                    "client_id", "app"), FORM).status);
            assertEquals(200, server.call("POST", "/oauth2/revoke", form("token", "a.b.c",
                    "client_id", "app"), FORM).status);
            refused(server.call("POST", "/oauth2/revoke", form("token", refresh), FORM), 401,
                    "invalid_client");
            refused(server.call("POST", "/oauth2/revoke", form("client_id", "app"), FORM), 400,
                    "invalid_request");

            // The way com.codename1.io.oidc.OidcClient asks: token and client_id.
            Reply revoked = server.call("POST", "/oauth2/revoke", form("token", refresh,
                    "client_id", "app"), FORM);
            assertEquals(200, revoked.status, revoked.toString());
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", refresh), 400, "invalid_grant");
            assertEquals(401, server.get("/userinfo", "Authorization", "Bearer " + access).status);

            // By the access token, too.
            tokens = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "openid", verifier), "redirect_uri",
                    APP_REDIRECT, "code_verifier", verifier));
            assertEquals(200, server.call("POST", "/oauth2/revoke", form("token",
                    (String) tokens.get("access_token"), "client_id", "app"), FORM).status);
            refused(token(server, "grant_type", "refresh_token", "client_id", "app",
                    "refresh_token", (String) tokens.get("refresh_token")), 400, "invalid_grant");
        }
    }

    // ----------------------------------------------------------- user info

    @Test
    @DisplayName("user info answers an access token granted openid, with what its scopes entitle it to")
    void userInfo() throws Exception {
        try (SecuredServer server = start(as -> as.userInfoMapper((username, scopes) -> {
            Map<String, Object> claims = new LinkedHashMap<String, Object>();
            if (scopes.contains("email")) {
                claims.put("email", username + "@example.com");
                claims.put("email_verified", Boolean.TRUE);
            }
            if (scopes.contains("profile")) {
                claims.put("name", "Ada Lovelace");
            }
            // Nothing a mapper says replaces who the token is about.
            claims.put("sub", "somebody-else");
            return claims;
        }))) {
            String verifier = OAuth2Parameters.random(32);
            Map tokens = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "openid email", verifier),
                    "redirect_uri", APP_REDIRECT, "code_verifier", verifier));
            Reply reply = server.get("/userinfo", "Authorization", "Bearer "
                    + tokens.get("access_token"));
            assertEquals(200, reply.status, reply.toString());
            Map claims = json(reply);
            assertEquals("ada", claims.get("sub"));
            assertEquals("ada@example.com", claims.get("email"));
            assertEquals(Boolean.TRUE, claims.get("email_verified"));
            assertNull(claims.get("name"), "profile was not granted");
            // The same in the ID token, which is what a client reads first.
            Jwt id = OAuth2Testing.verify((String) tokens.get("id_token"),
                    server.get("/oauth2/jwks").body);
            assertEquals("ada@example.com", id.getClaimAsString("email"));
            assertEquals("ada", id.getSubject());

            // Without openid: a good token, and not one for this.
            tokens = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "orders:read", verifier),
                    "redirect_uri", APP_REDIRECT, "code_verifier", verifier));
            assertNull(tokens.get("id_token"));
            Reply denied = server.get("/userinfo", "Authorization", "Bearer "
                    + tokens.get("access_token"));
            assertEquals(403, denied.status);
            assertEquals("insufficient_scope", json(denied).get("error"));
            assertTrue(denied.header("WWW-Authenticate").contains("insufficient_scope"));

            Reply none = server.get("/userinfo");
            assertEquals(401, none.status);
            assertEquals("Bearer", none.header("WWW-Authenticate"));
            assertEquals(401, server.get("/userinfo", "Authorization", "Bearer nonsense").status);
            // A token that is not this server's: same shape, another key.
            Jwk stranger = Jwk.ofPrivateKey(KeyFiles.privateKey(KeyFixtures.EC256_PKCS8_PEM));
            String forged = new com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder(
                    AuthorizationServerKeys.of(Arrays.asList(stranger))).encode(
                    com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters.from(
                            com.codename1.backend.security.oauth2.jwt.JwtClaimsSet.builder()
                                    .issuer(issuer).subject("ada").claim("scope", "openid")
                                    .expiresAt(System.currentTimeMillis() / 1000 + 600).build()))
                    .getTokenValue();
            assertEquals(401, server.get("/userinfo", "Authorization", "Bearer " + forged).status);
        }
    }

    // ---------------------------------------------------------------- keys

    @Test
    @DisplayName("two keys are published, the first signs, and a token of either verifies")
    void rotation() throws Exception {
        final Jwk rsa = AuthorizationServerKeys.usable(Jwk.ofPrivateKey(KeyFiles.privateKey(
                KeyFixtures.RSA_PKCS8_PEM)), "rsa");
        final Jwk ec = AuthorizationServerKeys.usable(Jwk.ofPrivateKey(KeyFiles.privateKey(
                KeyFixtures.EC256_PKCS8_PEM)), "ec");
        final List<Jwk> order = new ArrayList<Jwk>(Arrays.asList(rsa, ec));
        JwkSource rotating = () -> new ArrayList<Jwk>(order);
        try (SecuredServer server = start(as -> as.jwkSource(rotating))) {
            String basic = OAuth2Testing.basic("service", "service-secret");
            String first = (String) json(server.call("POST", "/oauth2/token", form("grant_type",
                    "client_credentials"), FORM, "Authorization", basic)).get("access_token");
            // The new key goes first; the old one stays published.
            order.clear();
            order.add(ec);
            order.add(rsa);
            String second = (String) json(server.call("POST", "/oauth2/token", form("grant_type",
                    "client_credentials"), FORM, "Authorization", basic)).get("access_token");

            String jwks = server.get("/oauth2/jwks").body;
            List keys = (List) com.codename1.backend.Json.parseObject(jwks).get("keys");
            assertEquals(2, keys.size());
            assertEquals(ec.getKeyId(), ((Map) keys.get(0)).get("kid"));
            assertEquals("sig", ((Map) keys.get(0)).get("use"));
            assertEquals("ES256", ((Map) keys.get(0)).get("alg"));
            assertEquals("RS256", ((Map) keys.get(1)).get("alg"));
            for (Object key : keys) {
                assertNull(((Map) key).get("d"), "a private key was published");
                assertNull(((Map) key).get("p"));
            }
            Jwt one = OAuth2Testing.verify(first, jwks);
            Jwt two = OAuth2Testing.verify(second, jwks);
            assertEquals("RS256", one.getHeaders().get("alg"));
            assertEquals(rsa.getKeyId(), one.getHeaders().get("kid"));
            assertEquals("ES256", two.getHeaders().get("alg"));
            assertEquals(ec.getKeyId(), two.getHeaders().get("kid"));
            assertEquals(Arrays.asList("ES256", "RS256"), json(server.get(
                    "/.well-known/openid-configuration")).get(
                    "id_token_signing_alg_values_supported"));
        }
    }

    @Test
    @DisplayName("keys come from the files the configuration lists; none, outside development, stops the start")
    void keysFromConfiguration() throws Exception {
        File rsa = new File(dir, "rsa.pem");
        File ec = new File(dir, "ec.pem");
        Files.write(rsa.toPath(), KeyFixtures.RSA_PKCS8_PEM.getBytes("US-ASCII"));
        Files.write(ec.toPath(), KeyFixtures.EC256_PKCS8_PEM.getBytes("US-ASCII"));
        Properties p = new Properties();
        p.setProperty(AuthorizationServerKeys.KEYS, ec.getPath() + " , " + rsa.getPath());
        List<Jwk> keys = AuthorizationServerKeys.load(Config.of(p, "prod")).getKeys();
        assertEquals(2, keys.size());
        assertEquals("ES256", keys.get(0).getAlgorithm());
        assertEquals("RS256", keys.get(1).getAlgorithm());
        assertTrue(keys.get(0).isPrivate());

        IllegalStateException none = assertThrows(IllegalStateException.class,
                () -> AuthorizationServerKeys.load(Config.of(new Properties(), "prod")));
        assertTrue(none.getMessage().contains("openssl genpkey") && none.getMessage().contains(
                AuthorizationServerKeys.KEYS), none.getMessage());
        // On a development profile one is made, and it signs.
        List<Jwk> made = AuthorizationServerKeys.load(Config.of(new Properties(), "dev")).getKeys();
        assertEquals(1, made.size());
        assertEquals("RS256", made.get(0).getAlgorithm());
        // A P-384 key is not one this server signs with.
        File p384 = new File(dir, "p384.pem");
        Files.write(p384.toPath(), KeyFixtures.EC384_PKCS8_PEM.getBytes("US-ASCII"));
        p.setProperty(AuthorizationServerKeys.KEYS, p384.getPath());
        assertThrows(IllegalStateException.class,
                () -> AuthorizationServerKeys.load(Config.of(p, "prod")));
    }

    @Test
    @DisplayName("outside development the issuer must be given; in development it is the request's")
    void issuer() throws Exception {
        Properties bare = SecuredServer.settings();
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(bare, "prod", users(), APP,
                        http -> http.authorizationServer(as -> as.registeredClientRepository(
                                clients())).build()));
        assertTrue(missing.getMessage().contains(AuthorizationServerSettings.ISSUER),
                missing.getMessage());
        IllegalStateException noClients = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(bare, "dev", users(), APP,
                        http -> http.authorizationServer(Customizer.withDefaults()).build()));
        assertTrue(noClients.getMessage().contains("RegisteredClientRepository"),
                noClients.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> AuthorizationServerSettings.builder().issuer("https://id.example.com/"));
        assertThrows(IllegalArgumentException.class,
                () -> AuthorizationServerSettings.builder().issuer("id.example.com"));

        try (SecuredServer server = SecuredServer.start(bare, "dev", users(), APP,
                http -> http.authorizationServer(as -> as.registeredClientRepository(clients())
                        .clientSecretEncoder(OAuth2Testing.PLAIN)).build())) {
            Map m = json(server.get("/.well-known/openid-configuration", "Host",
                    "dev.example.test:8080"));
            assertEquals("http://dev.example.test:8080", m.get("issuer"));
            assertEquals(400, server.get("/.well-known/openid-configuration", "Host",
                    "evil.example/path?").status);
        }
    }

    @Test
    @DisplayName("a customizer adds to the claims of both tokens")
    void customizer() throws Exception {
        try (SecuredServer server = start(as -> as.tokenCustomizer(context -> {
            if (OAuth2TokenContext.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claim("roles", context.getAuthorization() == null ? null
                        : context.getAuthorization().getAttribute("authorities"));
                context.getClaims().claim("grant", context.getAuthorizationGrantType());
            } else {
                context.getClaims().claim("tenant", "acme");
            }
        }))) {
            String verifier = OAuth2Parameters.random(32);
            Map tokens = json(token(server, "grant_type", "authorization_code", "client_id", "app",
                    "code", code(server, "app", APP_REDIRECT, "openid", verifier), "redirect_uri",
                    APP_REDIRECT, "code_verifier", verifier));
            String jwks = server.get("/oauth2/jwks").body;
            Jwt access = OAuth2Testing.verify((String) tokens.get("access_token"), jwks);
            assertEquals(Arrays.asList("ROLE_USER"), access.getClaimAsStringList("roles"));
            assertEquals("authorization_code", access.getClaimAsString("grant"));
            assertEquals("acme", OAuth2Testing.verify((String) tokens.get("id_token"), jwks)
                    .getClaimAsString("tenant"));
        }
    }
}
