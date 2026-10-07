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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Migrations;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.client.JdbcFederatedIdentityRepository;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.ClientSettings;
import com.codename1.backend.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.JdbcRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2Authorization;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.TokenSettings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// The database-backed stores of OAuth2 login and of the authorization server,
/// against real databases: SQLITE always, on a file; POSTGRES and MYSQL when
/// CN1_TX_POSTGRES / CN1_TX_MYSQL name one, as for [SecurityStoresTest].
///
/// What these stores promise is about several processes on one database, so
/// every "process" here is a pool of its own.
class OAuth2StoresTest {
    @TempDir
    File dir;

    private static final String[] TABLES = {"cn1_users", "cn1_authorities", "cn1_api_key",
        "cn1_persistent_logins", "cn1_mfa_totp", "cn1_mfa_recovery_code", "cn1_rate_limit",
        "cn1_federated_identity", "cn1_oauth2_registered_client", "cn1_oauth2_authorization",
        "cn1_oauth2_token", "cn1_webauthn_user", "cn1_webauthn_credential",
        "cn1_security_schema_history"};

    private final List<DataSource> opened = new ArrayList<DataSource>();
    private final List<DataSource> shared = new ArrayList<DataSource>();
    private String sqliteFile;

    @AfterEach
    void closeAll() {
        // A server's database is shared with other tests: leave nothing in it.
        for (DataSource pool : shared) {
            for (String table : TABLES) {
                try {
                    pool.execute("DROP TABLE IF EXISTS " + table, null);
                } catch (IOException ignored) {
                    // Closing down; the next run drops them before it starts.
                }
            }
        }
        for (DataSource pool : opened) {
            pool.close();
        }
    }

    /// A database with the schema applied, and nothing in it.
    private DataSource open(String engine) throws IOException {
        DataSource pool;
        if ("SQLITE".equals(engine)) {
            sqliteFile = new File(dir, "oauth2.db").getPath();
            pool = DataSource.open(sqliteFile, 4, 5000, 10000);
        } else {
            String url = System.getenv("CN1_TX_" + engine);
            Assumptions.assumeTrue(url != null && url.length() > 0,
                    "CN1_TX_" + engine + " is unset; this engine is not exercised");
            pool = DataSource.open(url, 4, 5000, 10000);
            for (String table : TABLES) {
                pool.execute("DROP TABLE IF EXISTS " + table, null);
            }
            shared.add(pool);
        }
        opened.add(pool);
        Migrations.of(pool, SecuritySchema.migrations()).migrate();
        return pool;
    }

    /// Another pool on the same database: another process.
    private DataSource again(String engine) throws IOException {
        DataSource pool = DataSource.open("SQLITE".equals(engine) ? sqliteFile
                : System.getenv("CN1_TX_" + engine), 4, 5000, 10000);
        opened.add(pool);
        return pool;
    }

    private static long count(DataSource pool, String table) throws IOException {
        return ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM " + table, null).get("n"))
                .longValue();
    }

    // ------------------------------------------------- federated identities

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void federatedIdentities(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcFederatedIdentityRepository identities = new JdbcFederatedIdentityRepository(pool);
        assertNull(identities.findUsername("google", "sub-1"));
        assertEquals("Ada@Example.com", identities.link("google", "sub-1", "Ada@Example.com"));
        // Tied once: a second tie answers the first, and changes nothing.
        assertEquals("Ada@Example.com", identities.link("google", "sub-1", "eve@example.com"));
        assertEquals("Ada@Example.com", identities.findUsername("google", "sub-1"));
        // A subject is compared byte for byte, on every engine.
        assertNull(identities.findUsername("google", "SUB-1"));
        assertNull(identities.findUsername("Google", "sub-1"));
        assertEquals("ada@example.com", identities.link("github", "583231", "ada@example.com"));
        // A user is found whatever the case of the name.
        List<String[]> mine = identities.findByUsername("ADA@EXAMPLE.COM");
        assertEquals(2, mine.size());
        assertEquals(new HashSet<String>(Arrays.asList("google/sub-1", "github/583231")),
                new HashSet<String>(Arrays.asList(mine.get(0)[0] + "/" + mine.get(0)[1],
                        mine.get(1)[0] + "/" + mine.get(1)[1])));
        assertTrue(identities.unlink("github", "583231"));
        assertFalse(identities.unlink("github", "583231"));
        assertEquals(1, identities.findByUsername("ada@example.com").size());

        // Two processes tying one identity at the same moment, each to a
        // different user: both are answered the same one, and there is one row.
        final DataSource other = again(engine);
        for (int round = 0 ; round < 5 ; round++) {
            final String subject = "raced-" + round;
            final List<String> answers = Collections.synchronizedList(new ArrayList<String>());
            final CountDownLatch go = new CountDownLatch(1);
            List<Thread> threads = new ArrayList<Thread>();
            for (int t = 0 ; t < 6 ; t++) {
                final int who = t;
                Thread thread = new Thread(() -> {
                    try {
                        go.await();
                        answers.add(new JdbcFederatedIdentityRepository(who % 2 == 0 ? pool
                                : other).link("google", subject, "user-" + who));
                    } catch (Exception err) {
                        answers.add("failed: " + err);
                    }
                });
                threads.add(thread);
                thread.start();
            }
            go.countDown();
            for (Thread thread : threads) {
                thread.join(30000);
            }
            assertEquals(6, answers.size());
            assertEquals(1, new HashSet<String>(answers).size(), answers.toString());
            assertTrue(answers.get(0).startsWith("user-"), answers.toString());
            assertEquals(answers.get(0), identities.findUsername("google", subject));
        }
        assertEquals(6L, count(pool, "cn1_federated_identity"));
    }

    // --------------------------------------------------- registered clients

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void registeredClients(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcRegisteredClientRepository clients = new JdbcRegisteredClientRepository(pool);
        assertNull(clients.findByClientId("app"));
        RegisteredClient app = RegisteredClient.withId("id-1").clientId("app")
                .clientName("Acme \"App\" & co")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                .redirectUri("com.acme.app:/oauth2redirect")
                .redirectUri("https://app.example/cb?tenant=a%20b&x=1,2")
                .scope("openid").scope("orders:read")
                .resource("https://orders.example/api").resource("urn:acme:billing")
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(120)
                        .refreshTokenTimeToLive(86400).reuseRefreshTokens(true).build())
                .build();
        clients.save(app);
        // Saved again unchanged: an update that changes nothing is not a new client.
        clients.save(app);
        assertEquals(1L, count(pool, "cn1_oauth2_registered_client"));
        for (RegisteredClient read : new RegisteredClient[] {clients.findByClientId("app"),
            clients.findById("id-1")}) {
            assertEquals("id-1", read.getId());
            assertEquals("app", read.getClientId());
            assertEquals("Acme \"App\" & co", read.getClientName());
            assertNull(read.getClientSecret());
            assertTrue(read.isPublic());
            assertEquals(app.getAuthorizationGrantTypes(), read.getAuthorizationGrantTypes());
            assertEquals(app.getClientAuthenticationMethods(),
                    read.getClientAuthenticationMethods());
            assertEquals(new ArrayList<String>(app.getRedirectUris()),
                    new ArrayList<String>(read.getRedirectUris()));
            assertEquals("[openid, orders:read]", read.getScopes().toString());
            assertEquals("[https://orders.example/api, urn:acme:billing]",
                    read.getResources().toString());
            assertTrue(read.getClientSettings().isRequireProofKey());
            assertEquals(120, read.getTokenSettings().getAccessTokenTimeToLive());
            assertEquals(86400, read.getTokenSettings().getRefreshTokenTimeToLive());
            assertEquals(300, read.getTokenSettings().getAuthorizationCodeTimeToLive());
            assertTrue(read.getTokenSettings().isReuseRefreshTokens());
        }
        // A client id is compared byte for byte.
        assertNull(clients.findByClientId("APP"));

        // Changed in place, by its id.
        clients.save(RegisteredClient.withId("id-1").clientId("app-renamed")
                .clientSecret("{enc}secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).scope("api")
                .build());
        assertNull(clients.findByClientId("app"));
        RegisteredClient renamed = clients.findByClientId("app-renamed");
        assertEquals("{enc}secret", renamed.getClientSecret());
        assertEquals("[client_secret_basic]", renamed.getClientAuthenticationMethods().toString());
        assertFalse(renamed.getTokenSettings().isReuseRefreshTokens());
        assertEquals(1L, count(pool, "cn1_oauth2_registered_client"));
        // Two clients cannot call themselves the same.
        assertThrows(IllegalStateException.class, () -> clients.save(RegisteredClient
                .withId("id-2").clientId("app-renamed").clientSecret("{enc}x")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build()));
    }

    // ------------------------------------------------------- authorizations

    private static OAuth2Authorization grant(String id, String status, long expiresAt) {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("redirect_uri", "com.acme.app:/cb");
        attributes.put("auth_time", Long.valueOf(1700000000L));
        attributes.put("authorities", Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
        return new OAuth2Authorization(id, "client-1", "ada", "authorization_code",
                Arrays.asList("openid", "orders:read"), status, attributes, 1000L, expiresAt);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void tokenIssuanceCannotResurrectRevokedGrants(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcOAuth2AuthorizationService issuer = new JdbcOAuth2AuthorizationService(pool);
        JdbcOAuth2AuthorizationService revoker = new JdbcOAuth2AuthorizationService(again(engine));
        issuer.save(grant("gone", OAuth2Authorization.ACTIVE, 5000L));
        issuer.addToken("gone", OAuth2AuthorizationService.REFRESH_TOKEN, "old", 5000L);
        assertTrue(issuer.consumeToken(OAuth2AuthorizationService.REFRESH_TOKEN, "old", 2000L));
        revoker.remove("gone");
        assertFalse(issuer.issueTokens("gone", 2000L, 9000L, "replacement", false));
        assertNull(issuer.findById("gone"));
        assertEquals(0L, count(pool, "cn1_oauth2_token"));

        for (int round = 0; round < 12; round++) {
            final String id = "race-" + round;
            issuer.save(grant(id, OAuth2Authorization.ACTIVE, 5000L));
            final CountDownLatch go = new CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<Throwable> error =
                    new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread issuing = new Thread(() -> {
                try {
                    go.await();
                    issuer.issueTokens(id, 2000L, 9000L, "token-" + id, false);
                } catch (Throwable failure) {
                    error.set(failure);
                }
            });
            Thread revoking = new Thread(() -> {
                try {
                    go.await();
                    revoker.remove(id);
                } catch (Throwable failure) {
                    error.set(failure);
                }
            });
            issuing.start();
            revoking.start();
            go.countDown();
            issuing.join(10000);
            revoking.join(10000);
            assertFalse(issuing.isAlive() || revoking.isAlive(), "issuance/revocation deadlocked");
            assertNull(error.get(), String.valueOf(error.get()));
            assertNull(issuer.findById(id));
            assertEquals(0L, count(pool, "cn1_oauth2_token"), "revocation left a replacement token");
        }
        issuer.save(grant("reuse", OAuth2Authorization.ACTIVE, 5000L));
        assertTrue(issuer.issueTokens("reuse", 2000L, 9000L, "kept", false));
        assertTrue(issuer.issueTokens("reuse", 2000L, 9000L, "kept", true));
        assertTrue(issuer.issueTokens("reuse", 2000L, 12000L, "kept", true));
        assertEquals(12000L, issuer.findById("reuse").getExpiresAt());
        assertEquals(12000L, issuer.findToken(OAuth2AuthorizationService.REFRESH_TOKEN,
                "kept").getExpiresAt());
        revoker.remove("reuse");
        assertFalse(issuer.issueTokens("reuse", 2000L, 15000L, "kept", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void authorizations(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcOAuth2AuthorizationService service = new JdbcOAuth2AuthorizationService(pool);
        assertNull(service.findById("g1"));
        service.save(grant("g1", OAuth2Authorization.ACTIVE, 5000L));
        service.save(grant("g1", OAuth2Authorization.ACTIVE, 5000L));
        OAuth2Authorization read = service.findById("g1");
        assertEquals("client-1", read.getRegisteredClientId());
        assertEquals("ada", read.getPrincipalName());
        assertEquals("[openid, orders:read]", read.getScopes().toString());
        assertEquals(Arrays.asList("ROLE_USER", "ROLE_ADMIN"), read.getAttribute("authorities"));
        assertEquals(1700000000L, ((Number) read.getAttribute("auth_time")).longValue());
        assertEquals(1000L, read.getCreatedAt());
        service.save(read.withExpiresAt(9000L));
        assertEquals(9000L, service.findById("g1").getExpiresAt());
        assertEquals(1000L, service.findById("g1").getCreatedAt());
        assertEquals(1L, count(pool, "cn1_oauth2_authorization"));

        // A secret is found by its hash alone, and by its kind.
        String code = OAuth2Parameters.random(32);
        String hash = OAuth2Parameters.sha256(code);
        service.addToken("g1", OAuth2AuthorizationService.CODE, hash, 2000L);
        assertNull(service.findToken(OAuth2AuthorizationService.CODE, code));
        assertNull(service.findToken(OAuth2AuthorizationService.REFRESH_TOKEN, hash));
        OAuth2AuthorizationService.StoredToken stored = service.findToken(
                OAuth2AuthorizationService.CODE, hash);
        assertEquals("g1", stored.getAuthorizationId());
        assertFalse(stored.isUsed());
        assertEquals(2000L, stored.getExpiresAt());
        assertEquals(0L, stored.getPolledAt());
        assertEquals(0L, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM cn1_oauth2_token "
                + "WHERE token_hash = ?", new Object[] {code}).get("n")).longValue(),
                "the secret itself was stored");

        // Used up once: the wrong kind does not, the second call does not.
        assertFalse(service.extendToken(OAuth2AuthorizationService.REFRESH_TOKEN, hash, 1500L, 4000L));
        assertTrue(service.extendToken(OAuth2AuthorizationService.CODE, hash, 1500L, 4000L));
        assertTrue(service.extendToken(OAuth2AuthorizationService.CODE, hash, 1500L, 3000L));
        assertEquals(4000L, service.findToken(OAuth2AuthorizationService.CODE, hash).getExpiresAt());
        assertFalse(service.consumeToken(OAuth2AuthorizationService.REFRESH_TOKEN, hash, 1500L));
        assertTrue(service.consumeToken(OAuth2AuthorizationService.CODE, hash, 1500L));
        assertFalse(service.consumeToken(OAuth2AuthorizationService.CODE, hash, 1500L));
        // It stays, marked: that is how a second presentation is told apart.
        assertTrue(service.findToken(OAuth2AuthorizationService.CODE, hash).isUsed());
        assertFalse(service.extendToken(OAuth2AuthorizationService.CODE, hash, 1500L, 9000L));
        // An expired one is not used up, at its last millisecond or after.
        String late = OAuth2Parameters.sha256("late");
        service.addToken("g1", OAuth2AuthorizationService.CODE, late, 2000L);
        assertFalse(service.extendToken(OAuth2AuthorizationService.CODE, late, 2000L, 9000L));
        assertFalse(service.consumeToken(OAuth2AuthorizationService.CODE, late, 2000L));
        assertFalse(service.consumeToken(OAuth2AuthorizationService.CODE, late, 2001L));
        assertFalse(service.findToken(OAuth2AuthorizationService.CODE, late).isUsed());
        assertTrue(service.consumeToken(OAuth2AuthorizationService.CODE, late, 1999L));
        assertFalse(service.consumeToken(OAuth2AuthorizationService.CODE,
                OAuth2Parameters.sha256("never issued"), 0L));

        service.touchToken(OAuth2AuthorizationService.CODE, hash, 1234L);
        assertEquals(1234L, service.findToken(OAuth2AuthorizationService.CODE, hash)
                .getPolledAt());

        // A device grant is answered once, by whoever answers first.
        service.save(new OAuth2Authorization("d1", "client-1", "", "device", null,
                OAuth2Authorization.PENDING, null, 1000L, 5000L));
        JdbcOAuth2AuthorizationService elsewhere = new JdbcOAuth2AuthorizationService(
                again(engine));
        Map<String, Object> who = new LinkedHashMap<String, Object>();
        who.put("auth_time", Long.valueOf(42));
        assertTrue(elsewhere.decide("d1", true, "ada", who));
        assertFalse(service.decide("d1", false, "eve", null));
        OAuth2Authorization decided = service.findById("d1");
        assertEquals(OAuth2Authorization.ACTIVE, decided.getStatus());
        assertEquals("ada", decided.getPrincipalName());
        assertEquals(42L, ((Number) decided.getAttribute("auth_time")).longValue());
        service.save(new OAuth2Authorization("d2", "client-1", "", "device", null,
                OAuth2Authorization.PENDING, null, 1000L, 5000L));
        assertTrue(service.decide("d2", false, "ada", null));
        assertEquals(OAuth2Authorization.DENIED, service.findById("d2").getStatus());
        assertEquals("", service.findById("d2").getPrincipalName());
        assertFalse(service.decide("nobody", true, "ada", null));

        // Removing a grant takes every secret issued under it, and no other's.
        service.save(grant("g2", OAuth2Authorization.ACTIVE, 5000L));
        service.addToken("g2", OAuth2AuthorizationService.REFRESH_TOKEN, "h2", 5000L);
        service.remove("g1");
        assertNull(service.findById("g1"));
        assertNull(service.findToken(OAuth2AuthorizationService.CODE, hash));
        assertNull(service.findToken(OAuth2AuthorizationService.CODE, late));
        assertNotNull(service.findToken(OAuth2AuthorizationService.REFRESH_TOKEN, "h2"));
        service.remove("g1");

        // Purging forgets what expired, a bounded number at a time.
        for (int iter = 0 ; iter < 7 ; iter++) {
            service.save(grant("old-" + iter, OAuth2Authorization.ACTIVE, 100L + iter));
            service.addToken("old-" + iter, OAuth2AuthorizationService.CODE, "old-code-" + iter,
                    100L + iter);
        }
        long tokens = count(pool, "cn1_oauth2_token");
        long grants = count(pool, "cn1_oauth2_authorization");
        assertEquals(6, service.purgeExpired(1000L, 3));
        // Three secrets, and three grants whose secrets those were.
        assertEquals(tokens - 3, count(pool, "cn1_oauth2_token"));
        assertEquals(grants - 3, count(pool, "cn1_oauth2_authorization"));
        assertTrue(service.purgeExpired(1000L, 100) >= 4);
        assertEquals(0, service.purgeExpired(1000L, 100));
        // What has not expired is untouched.
        assertNotNull(service.findById("g2"));
        assertNotNull(service.findToken(OAuth2AuthorizationService.REFRESH_TOKEN, "h2"));
        assertNotNull(service.findById("d1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void oneCodeIsUsedUpByExactlyOneOfManyProcesses(String engine) throws Exception {
        final DataSource one = open(engine);
        final DataSource two = again(engine);
        final JdbcOAuth2AuthorizationService setup = new JdbcOAuth2AuthorizationService(one);
        setup.save(grant("g", OAuth2Authorization.ACTIVE, Long.MAX_VALUE / 2));
        for (int round = 0 ; round < 12 ; round++) {
            final String hash = OAuth2Parameters.sha256("code-" + round);
            setup.addToken("g", OAuth2AuthorizationService.CODE, hash, Long.MAX_VALUE / 2);
            final AtomicInteger won = new AtomicInteger();
            final AtomicInteger failed = new AtomicInteger();
            final CountDownLatch go = new CountDownLatch(1);
            List<Thread> threads = new ArrayList<Thread>();
            for (int t = 0 ; t < 8 ; t++) {
                final JdbcOAuth2AuthorizationService process = new JdbcOAuth2AuthorizationService(
                        t % 2 == 0 ? one : two);
                Thread thread = new Thread(() -> {
                    try {
                        go.await();
                        if (process.consumeToken(OAuth2AuthorizationService.CODE, hash, 1L)) {
                            won.incrementAndGet();
                        }
                    } catch (Exception err) {
                        failed.incrementAndGet();
                    }
                });
                threads.add(thread);
                thread.start();
            }
            go.countDown();
            for (Thread thread : threads) {
                thread.join(30000);
            }
            assertEquals(0, failed.get());
            assertEquals(1, won.get(), "round " + round + ": a code was used up " + won.get()
                    + " times");
        }
    }

    // ------------------------------ two authorization servers, one database

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "app");
        }
    };

    /// One authorization server under `prefix`, keeping everything in `pool`:
    /// what one process of several would be.
    private static SecuredServer.Chain process(final String prefix, final DataSource pool,
                                               final JwkSource keys, final Clock clock) {
        return http -> http.securityMatcher(prefix + "/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.loginPage(prefix + "/login"))
                .authorizationServer(as -> as
                        .settings(AuthorizationServerSettings.builder()
                                .authorizationEndpoint(prefix + "/oauth2/authorize")
                                .tokenEndpoint(prefix + "/oauth2/token")
                                .jwkSetEndpoint(prefix + "/oauth2/jwks")
                                .tokenRevocationEndpoint(prefix + "/oauth2/revoke")
                                .deviceAuthorizationEndpoint(prefix + "/oauth2/device_authorization")
                                .deviceVerificationEndpoint(prefix + "/oauth2/device_verification")
                                .oidcUserInfoEndpoint(prefix + "/userinfo").build())
                        .registeredClientRepository(new JdbcRegisteredClientRepository(pool))
                        .authorizationService(new JdbcOAuth2AuthorizationService(pool))
                        .jwkSource(keys).clock(clock)).build();
    }

    /// A form posted over a real connection; answers `status body`.
    private static String post(int port, String path, String form) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.getOutputStream().write(form.getBytes("UTF-8"));
        int status = connection.getResponseCode();
        InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        for (int n = in == null ? -1 : in.read(buffer) ; n >= 0 ; n = in.read(buffer)) {
            body.write(buffer, 0, n);
        }
        return status + " " + new String(body.toByteArray(), "UTF-8");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void twoProcessesRedeemOneCode(String engine) throws Exception {
        DataSource one = open(engine);
        DataSource two = again(engine);
        new JdbcRegisteredClientRepository(one).save(RegisteredClient.withId("1").clientId("app")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("com.acme.app:/cb").scope("openid").build());
        JwkSource keys = AuthorizationServerKeys.of(Arrays.asList(AuthorizationServerKeys.usable(
                Jwk.ofPrivateKey(KeyFiles.privateKey(KeyFixtures.RSA_PKCS8_PEM)), "the key")));
        final int port = OAuth2Testing.freePort();
        final OAuth2Testing.Ticking clock = new OAuth2Testing.Ticking();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, "http://127.0.0.1:" + port);
        Object[] beans = {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())};
        try (SecuredServer server = SecuredServer.start(settings, "test", beans, APP,
                process("/a", one, keys, clock), process("/b", two, keys, clock))) {
            assertEquals(302, server.post("/a/login", "username=ada&password=ada-pw").status);
            Set<String> refreshTokens = new HashSet<String>();
            for (int round = 0 ; round < 6 ; round++) {
                final String verifier = OAuth2Parameters.random(32);
                // Issued by one process...
                Reply back = server.get((round % 2 == 0 ? "/a" : "/b") + "/oauth2/authorize?"
                        + form("response_type", "code", "client_id", "app", "redirect_uri",
                                "com.acme.app:/cb", "scope", "openid", "state", "s",
                                "code_challenge", OAuth2Parameters.sha256(verifier),
                                "code_challenge_method", "S256"));
                assertEquals(302, back.status, back.toString());
                final String code = OAuth2Testing.query(back.header("Location")).get("code");
                assertNotNull(code, back.toString());
                // ...and presented to both at the same moment.
                final List<String> answers = Collections.synchronizedList(new ArrayList<String>());
                final CountDownLatch go = new CountDownLatch(1);
                List<Thread> threads = new ArrayList<Thread>();
                for (final String prefix : new String[] {"/a", "/b", "/a", "/b"}) {
                    Thread thread = new Thread(() -> {
                        try {
                            go.await();
                            answers.add(post(port, prefix + "/oauth2/token", form("grant_type",
                                    "authorization_code", "client_id", "app", "code", code,
                                    "redirect_uri", "com.acme.app:/cb", "code_verifier",
                                    verifier)));
                        } catch (Exception err) {
                            answers.add("failed: " + err);
                        }
                    });
                    threads.add(thread);
                    thread.start();
                }
                go.countDown();
                for (Thread thread : threads) {
                    thread.join(60000);
                }
                assertEquals(4, answers.size());
                String winner = null;
                int refused = 0;
                for (String answer : answers) {
                    if (answer.startsWith("200 ")) {
                        assertNull(winner, "two requests were issued tokens for one code: "
                                + answers);
                        winner = answer;
                    } else {
                        assertTrue(answer.startsWith("400 ") && answer.contains(
                                "\"error\":\"invalid_grant\""), answer);
                        refused++;
                    }
                }
                assertNotNull(winner, "nobody was issued tokens: " + answers);
                assertEquals(3, refused);
                String refresh = (String) Json.parseObject(winner.substring(4)).get(
                        "refresh_token");
                assertTrue(refreshTokens.add(refresh));
                // The one that won keeps what it won: its refresh token works,
                // at the other process.
                String refreshed = post(port, (round % 2 == 0 ? "/b" : "/a") + "/oauth2/token",
                        form("grant_type", "refresh_token", "client_id", "app", "refresh_token",
                                refresh));
                assertTrue(refreshed.startsWith("200 "), refreshed);
                String rotated = (String) Json.parseObject(refreshed.substring(4)).get(
                        "refresh_token");
                // The code again, once all that is over: now it is a replay, and
                // it ends the grant in both processes.
                clock.now += 10000;
                assertTrue(post(port, "/a/oauth2/token", form("grant_type", "authorization_code",
                        "client_id", "app", "code", code, "redirect_uri", "com.acme.app:/cb",
                        "code_verifier", verifier)).startsWith("400 "));
                String after = post(port, "/b/oauth2/token", form("grant_type", "refresh_token",
                        "client_id", "app", "refresh_token", rotated));
                assertTrue(after.startsWith("400 ") && after.contains("invalid_grant"), after);
            }
            // Nothing of all that is left but the grants a replay did not reach.
            assertEquals(0L, count(one, "cn1_oauth2_authorization"));
            assertEquals(0L, count(one, "cn1_oauth2_token"));
        }
    }
}
