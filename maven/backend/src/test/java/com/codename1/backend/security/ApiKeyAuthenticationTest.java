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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.apikey.ApiKey;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.ApiKeyRepository;
import com.codename1.backend.security.apikey.GeneratedApiKey;
import com.codename1.backend.security.apikey.InMemoryApiKeyRepository;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.impl.backend.security.SecuritySupport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// API keys made, stored and presented to a running server.
class ApiKeyAuthenticationTest {
    private static final String INVALID = "Bearer error=\"invalid_token\", error_description=\"The "
            + "API key is not valid\", error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"";

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, who == null ? "nobody"
                    : who.getName() + " " + who.getAuthorities() + " "
                    + who.getPrincipal().getClass().getSimpleName());
        }
    };

    private static SecuredServer.Chain chain(final Customizer<ApiKeyConfigurer> keys,
                                             final boolean tokensToo) {
        return new SecuredServer.Chain() {
            @Override
            public SecurityFilterChain build(HttpSecurity http) {
                http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/deploy/**").hasAuthority("SCOPE_deploy")
                        .anyRequest().authenticated())
                    .apiKey(keys);
                if (tokensToo) {
                    http.oauth2ResourceServer(o -> o.jwt(jwt -> jwt.decoder(DefaultJwtDecoder
                            .withPublicKey(Base64.decode(KeyFixtures.RSA_PUBLIC_DER)).build())));
                }
                return http.build();
            }
        };
    }

    @Test
    @DisplayName("A generated key: a prefix and 32 random bytes, stored as its SHA-256 only")
    void generator() throws Exception {
        ApiKeyGenerator generator = new ApiKeyGenerator();
        GeneratedApiKey made = generator.generate("ci-bot", "deploy", "read");
        String plaintext = made.getPlaintext();
        assertTrue(plaintext.startsWith("cn1_"), plaintext);
        assertEquals(4 + 43, plaintext.length(), "32 bytes as base64url without padding");
        ApiKey stored = made.getApiKey();
        assertEquals("ci-bot", stored.getOwner());
        assertEquals(Arrays.asList("deploy", "read"), stored.getScopes());
        assertFalse(stored.isRevoked());
        assertEquals("cn1_", stored.getPrefix());
        assertEquals(plaintext.substring(plaintext.length() - 4), stored.getLastFour());
        assertEquals("cn1_..." + stored.getLastFour(), stored.getDisplayName());
        // The hash is the plain SHA-256 another implementation would compute.
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                plaintext.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        assertEquals(hex.toString(), stored.getHash());
        assertEquals(stored.getHash(), ApiKeyGenerator.hash(plaintext));
        // Nothing that is kept or printed holds the key.
        assertFalse(stored.toString().contains(plaintext.substring(4, 40)));
        assertFalse(made.toString().contains(plaintext.substring(4, 40)));
        assertFalse(stored.getHash().contains(plaintext.substring(4, 40)));

        GeneratedApiKey another = generator.generate("ci-bot");
        assertNotEquals(plaintext, another.getPlaintext());
        assertNotEquals(stored.getId(), another.getApiKey().getId());
        assertTrue(another.getApiKey().getScopes().isEmpty());
        assertTrue(new ApiKeyGenerator("acme_live_").generate("x").getPlaintext()
                .startsWith("acme_live_"));
        assertEquals("An API key prefix is letters, digits and underscores: a-b",
                assertThrows(IllegalArgumentException.class, () -> new ApiKeyGenerator("a-b"))
                        .getMessage());
        assertEquals("An API key prefix is 1 to 32 characters", assertThrows(
                IllegalArgumentException.class, () -> new ApiKeyGenerator("")).getMessage());
        assertEquals("An API key needs an owner", assertThrows(IllegalArgumentException.class,
                () -> generator.generate("")).getMessage());
    }

    @Test
    @DisplayName("A key in either header is who the request is from; a bad one is 401")
    void acceptedAndRefused() throws Exception {
        ApiKeyGenerator generator = new ApiKeyGenerator();
        GeneratedApiKey deployer = generator.generate("ci-bot", "deploy", "read");
        GeneratedApiKey reader = generator.generate("dashboard", "read");
        GeneratedApiKey retired = generator.generate("old-bot", "deploy");
        final InMemoryApiKeyRepository repository = new InMemoryApiKeyRepository(
                deployer.getApiKey(), reader.getApiKey(), retired.getApiKey());
        assertTrue(repository.revoke(retired.getApiKey().getId()));
        assertFalse(repository.revoke("no-such-id"));
        assertEquals(3, repository.findAll().size());

        try (SecuredServer server = SecuredServer.start(APP,
                chain(keys -> keys.repository(repository), false))) {
            // Both headers.
            Reply byHeader = server.get("/api/me", "X-API-Key", deployer.getPlaintext());
            assertEquals(200, byHeader.status, byHeader.toString());
            assertEquals("ci-bot [SCOPE_deploy, SCOPE_read] ApiKey", byHeader.body);
            Reply byBearer = server.get("/api/me", "Authorization",
                    "Bearer " + deployer.getPlaintext());
            assertEquals("ci-bot [SCOPE_deploy, SCOPE_read] ApiKey", byBearer.body);
            assertEquals(200, server.get("/api/me", "Authorization",
                    "bearer " + deployer.getPlaintext()).status);
            assertEquals(200, server.get("/api/deploy/prod", "X-API-Key",
                    deployer.getPlaintext()).status);
            // Nothing is kept.
            assertNull(byHeader.header("Set-Cookie"));
            assertTrue(server.cookies.isEmpty());

            // A key that does not grant the scope.
            Reply denied = server.get("/api/deploy/prod", "X-API-Key", reader.getPlaintext());
            assertEquals(403, denied.status, denied.toString());
            assertTrue(denied.header("WWW-Authenticate").startsWith(
                    "Bearer error=\"insufficient_scope\""), denied.toString());
            assertEquals("dashboard [SCOPE_read] ApiKey",
                    server.get("/api/me", "X-API-Key", reader.getPlaintext()).body);

            int reached = server.reached().size();
            // Revoked, in both headers.
            Reply revoked = server.get("/api/me", "X-API-Key", retired.getPlaintext());
            assertEquals(401, revoked.status);
            assertEquals(INVALID, revoked.header("WWW-Authenticate"));
            assertEquals(INVALID, server.get("/api/me", "Authorization",
                    "Bearer " + retired.getPlaintext()).header("WWW-Authenticate"));
            // Unknown: the right shape, never issued. The same answer as revoked.
            String unknown = generator.generate("nobody").getPlaintext();
            assertEquals(INVALID, server.get("/api/me", "X-API-Key", unknown)
                    .header("WWW-Authenticate"));
            // One character off.
            String good = deployer.getPlaintext();
            String off = good.substring(0, good.length() - 1)
                    + (good.endsWith("A") ? "B" : "A");
            assertEquals(401, server.get("/api/me", "X-API-Key", off).status);
            // The wrong prefix, in the key header: a key, and not one of ours.
            assertEquals(INVALID, server.get("/api/me", "X-API-Key",
                    "sk_" + good.substring(4)).header("WWW-Authenticate"));
            assertEquals(INVALID, server.get("/api/me", "X-API-Key", "cn1_")
                    .header("WWW-Authenticate"));
            assertEquals(INVALID, server.get("/api/me", "X-API-Key", "")
                    .header("WWW-Authenticate"));
            char[] huge = new char[5000];
            Arrays.fill(huge, 'a');
            assertEquals(401, server.get("/api/me", "X-API-Key", "cn1_" + new String(huge)).status);
            // The wrong prefix as a bearer value is not a key at all: nobody
            // signed in, and the request is asked for credentials.
            Reply foreign = server.get("/api/me", "Authorization",
                    "Bearer sk_" + good.substring(4));
            assertEquals(401, foreign.status);
            assertEquals("Bearer", foreign.header("WWW-Authenticate"));
            assertEquals("Bearer", server.get("/api/me").header("WWW-Authenticate"));
            assertEquals(reached, server.reached().size(), "none of those reached the app");

            // Revoking takes effect on the next request.
            assertTrue(repository.revoke(reader.getApiKey().getId()));
            assertEquals(401, server.get("/api/me", "X-API-Key", reader.getPlaintext()).status);

            // A key is exempt from CSRF; a request without one is not.
            assertEquals(200, server.call("POST", "/api/deploy/prod", "{}", "application/json",
                    "X-API-Key", deployer.getPlaintext()).status);
            assertEquals(403, server.call("POST", "/api/deploy/prod", "{}", "application/json")
                    .status);
        }
    }

    @Test
    @DisplayName("Beside tokens: the prefix decides which a bearer value is")
    void besideTokens() throws Exception {
        GeneratedApiKey key = new ApiKeyGenerator().generate("ci-bot", "deploy");
        final InMemoryApiKeyRepository repository = new InMemoryApiKeyRepository(key.getApiKey());
        long now = System.currentTimeMillis() / 1000L;
        String token = new DefaultJwtEncoder(JwkSet.of(Jwk.ofPrivateKey(
                Base64.decode(KeyFixtures.RSA_PKCS8_DER)))).encode(JwtEncoderParameters.from(
                        JwtClaimsSet.builder().subject("ada").expiresAt(now + 300)
                                .claim("scope", "deploy").build())).getTokenValue();
        try (SecuredServer server = SecuredServer.start(APP,
                chain(keys -> keys.repository(repository), true))) {
            assertEquals("ci-bot [SCOPE_deploy] ApiKey", server.get("/api/me", "Authorization",
                    "Bearer " + key.getPlaintext()).body);
            assertEquals("ada [SCOPE_deploy] Jwt", server.get("/api/me", "Authorization",
                    "Bearer " + token).body);
            // One set of rules covers both.
            assertEquals(200, server.get("/api/deploy/x", "Authorization",
                    "Bearer " + key.getPlaintext()).status);
            assertEquals(200, server.get("/api/deploy/x", "Authorization", "Bearer " + token).status);
            // A bad key is answered as a key, not handed on to be misread as a token.
            Reply badKey = server.get("/api/me", "Authorization", "Bearer cn1_notakey");
            assertEquals(401, badKey.status);
            assertEquals(INVALID, badKey.header("WWW-Authenticate"));
            // And a bad token as a token.
            Reply badToken = server.get("/api/me", "Authorization", "Bearer abc.def.ghi");
            assertEquals(401, badToken.status);
            assertTrue(badToken.header("WWW-Authenticate").contains("Malformed token"),
                    badToken.toString());
            assertEquals("ci-bot [SCOPE_deploy] ApiKey", server.get("/api/me", "X-API-Key",
                    key.getPlaintext()).body);
        }
    }

    @Test
    @DisplayName("Another prefix and another header, and the repository found as a bean")
    void configured() throws Exception {
        GeneratedApiKey key = new ApiKeyGenerator("acme_").generate("partner", "read");
        ApiKeyRepository repository = new InMemoryApiKeyRepository(key.getApiKey());
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "test",
                new Object[] {repository}, APP,
                chain(keys -> keys.prefix("acme_").headerName("X-Acme-Key"), false))) {
            assertEquals("partner [SCOPE_read] ApiKey",
                    server.get("/api/me", "X-Acme-Key", key.getPlaintext()).body);
            assertEquals(200, server.get("/api/me", "Authorization",
                    "Bearer " + key.getPlaintext()).status);
            // The default header is no longer where a key is read from.
            assertEquals(401, server.get("/api/me", "X-API-Key", key.getPlaintext()).status);
        }
        // A key of one prefix presented to a chain configured for another.
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "test",
                new Object[] {repository}, APP, chain(Customizer.<ApiKeyConfigurer>withDefaults(),
                        false))) {
            assertEquals(401, server.get("/api/me", "X-API-Key", key.getPlaintext()).status);
            assertEquals(401, server.get("/api/me", "Authorization",
                    "Bearer " + key.getPlaintext()).status);
        }
        assertEquals("apiKey() needs somewhere to look keys up, and this application has none. "
                + "Declare an ApiKeyRepository bean, or call repository(...) on the apiKey() "
                + "configurer.", assertThrows(IllegalStateException.class,
                        () -> SecuredServer.start(APP, chain(
                                Customizer.<ApiKeyConfigurer>withDefaults(), false))).getMessage());
    }

    @Test
    @DisplayName("A repository that answers with another key's row is not believed")
    void repositoryIsChecked() throws Exception {
        final GeneratedApiKey real = new ApiKeyGenerator().generate("admin", "deploy");
        // A lookup that ignores what it was asked: a LIKE where an = was meant.
        final ApiKeyRepository sloppy = new ApiKeyRepository() {
            @Override
            public ApiKey findByHash(String hash) {
                return real.getApiKey();
            }
        };
        try (SecuredServer server = SecuredServer.start(APP,
                chain(keys -> keys.repository(sloppy), false))) {
            assertEquals(200, server.get("/api/me", "X-API-Key", real.getPlaintext()).status);
            assertEquals(401, server.get("/api/me", "X-API-Key",
                    new ApiKeyGenerator().generate("x").getPlaintext()).status);
        }
    }
}
