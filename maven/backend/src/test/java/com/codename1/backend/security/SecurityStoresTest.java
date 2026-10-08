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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Migrations;
import com.codename1.backend.security.apikey.ApiKey;
import com.codename1.backend.security.apikey.JdbcApiKeyRepository;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository;
import com.codename1.backend.security.mfa.JdbcTotpRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpCredential;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.ratelimit.JdbcRateLimiter;
import com.codename1.backend.security.rememberme.JdbcTokenRepository;
import com.codename1.backend.security.rememberme.PersistentRememberMeToken;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationState;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// The security layer's schema and its database-backed stores, against real
/// databases.
///
/// SQLITE always runs, on a file in a temporary directory. POSTGRES and MYSQL
/// run when CN1_TX_POSTGRES / CN1_TX_MYSQL name a database, as for the
/// migration and transaction tests: what these stores rely on -- a key that is
/// compared byte for byte, a conditional UPDATE's row count -- is exactly what
/// differs between engines.
class SecurityStoresTest {
    @TempDir
    File dir;

    private final List<DataSource> opened = new ArrayList<DataSource>();

    private static final String[] TABLES = {"cn1_users", "cn1_authorities", "cn1_api_key",
        "cn1_persistent_logins", "cn1_mfa_totp", "cn1_mfa_recovery_code", "cn1_rate_limit",
        "cn1_federated_identity", "cn1_oauth2_registered_client", "cn1_oauth2_authorization",
        "cn1_oauth2_token", "cn1_webauthn_user", "cn1_webauthn_credential",
        "cn1_security_schema_history"};

    private final List<DataSource> shared = new ArrayList<DataSource>();

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
        SecurityContextHolder.clearContext();
    }

    /// A database with the schema applied, and nothing in it.
    private DataSource open(String engine) throws IOException {
        DataSource pool;
        if ("SQLITE".equals(engine)) {
            pool = DataSource.open(new File(dir, "sec-" + opened.size() + ".db").getPath(), 4,
                    5000, 10000);
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

    private static final class Ticking implements Clock {
        long now = 1700000000000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void theSchemaIsAppliedOnceUnderItsOwnHistory(String engine) throws Exception {
        DataSource pool = open(engine);
        // open() migrated: every table is there and empty.
        for (String table : TABLES) {
            if (!table.endsWith("_history")) {
                assertEquals(0L, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM " + table,
                        null).get("n")).longValue(), table);
            }
        }
        MigrationInfo[] info = Migrations.of(pool, SecuritySchema.migrations()).info();
        assertEquals(11, info.length);
        for (int i = 0; i < info.length; i++) {
            assertEquals(String.valueOf(i + 1), info[i].getVersion());
            assertEquals(MigrationState.SUCCESS, info[i].getState());
        }
        assertEquals("users and authorities", info[0].getDescription());
        assertEquals("rate limits", info[4].getDescription());
        assertEquals("federated identities", info[5].getDescription());
        assertEquals("oauth2 registered clients", info[6].getDescription());
        assertEquals("oauth2 authorizations", info[7].getDescription());
        assertEquals("passkeys", info[8].getDescription());
        assertEquals("rate limit expiry", info[9].getDescription());
        // Its own history, not the application's.
        assertEquals(11L, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM "
                + "cn1_security_schema_history", null).get("n")).longValue());
        // And a second run finds nothing to do.
        MigrateResult again = Migrations.of(pool, SecuritySchema.migrations()).migrate();
        assertEquals(0, again.getMigrationsExecuted());
        assertEquals("11", again.getTargetVersion());
        assertEquals("security", SecuritySchema.migrations().getName());
        assertEquals("cn1_security_schema_history", SecuritySchema.migrations().getTable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void users(String engine) throws Exception {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(open(engine));
        assertFalse(users.userExists("Ada"));
        assertEquals("ada", assertThrows(UsernameNotFoundException.class,
                () -> users.loadUserByUsername("ada")).getMessage());
        users.createUser(User.withUsername("Ada").password("{noop}one")
                .authorities("ROLE_USER", "notes:read").build());
        users.createUser(User.withUsername("locked").password("{noop}x").roles("USER")
                .accountLocked(true).disabled(true).build());

        // Found whatever the case, and returned as it was created.
        for (String asked : new String[] {"Ada", "ada", "ADA"}) {
            UserDetails ada = users.loadUserByUsername(asked);
            assertEquals("Ada", ada.getUsername());
            assertEquals("{noop}one", ada.getPassword());
            assertEquals("[ROLE_USER, notes:read]", ada.getAuthorities().toString());
            assertTrue(ada.isEnabled() && ada.isAccountNonLocked() && ada.isAccountNonExpired()
                    && ada.isCredentialsNonExpired());
        }
        assertTrue(users.userExists("ADA"));
        UserDetails locked = users.loadUserByUsername("locked");
        assertFalse(locked.isEnabled());
        assertFalse(locked.isAccountNonLocked());
        assertTrue(locked.isAccountNonExpired());

        // One user per name, whatever its case; and nothing of the refused one
        // is left behind.
        assertEquals("user should not exist", assertThrows(IllegalArgumentException.class,
                () -> users.createUser(User.withUsername("ADA").password("{noop}two")
                        .authorities("ROLE_ADMIN").build())).getMessage());
        assertEquals("[ROLE_USER, notes:read]",
                users.loadUserByUsername("ada").getAuthorities().toString());

        users.updateUser(User.withUsername("ada").password("{noop}two")
                .authorities("ROLE_ADMIN").credentialsExpired(true).build());
        UserDetails updated = users.loadUserByUsername("Ada");
        assertEquals("{noop}two", updated.getPassword());
        assertEquals("[ROLE_ADMIN]", updated.getAuthorities().toString());
        assertFalse(updated.isCredentialsNonExpired());
        assertEquals("user should exist", assertThrows(IllegalArgumentException.class,
                () -> users.updateUser(User.withUsername("nobody").password("x").roles("A")
                        .build())).getMessage());

        // Upgrade-on-login, and a user changing their own password.
        assertEquals("{noop}three", users.updatePassword(updated, "{noop}three").getPassword());
        assertEquals("{noop}three", users.loadUserByUsername("ada").getPassword());
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken
                .authenticated("ADA", null, updated.getAuthorities()));
        users.changePassword("{noop}three", "{noop}four");
        assertEquals("{noop}four", users.loadUserByUsername("ada").getPassword());
        SecurityContextHolder.clearContext();
        assertTrue(assertThrows(AccessDeniedException.class,
                () -> users.changePassword("a", "b")).getMessage().contains("no Authentication"));

        users.deleteUser("ADA");
        assertFalse(users.userExists("ada"));
        assertTrue(users.userExists("locked"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void deletingAUserRemovesDurableCredentialsBeforeNameReuse(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(pool);
        for (String name : new String[] {"Ada", "Eve"}) {
            users.createUser(User.withUsername(name).password("old").roles("USER").build());
            seedCredentials(pool, name);
        }
        users.deleteUser("ADA");
        users.createUser(User.withUsername("ada").password("new").roles("USER").build());
        assertEquals("new", users.loadUserByUsername("Ada").getPassword());
        for (String table : CREDENTIAL_TABLES) {
            List rows = pool.query("SELECT * FROM " + table, null);
            assertEquals(1, rows.size(), table + " retained the deleted account's credentials");
            assertTrue(rows.get(0).toString().contains("Eve") || rows.get(0).toString().contains("eve"),
                    table + " deleted the wrong account: " + rows);
        }
        assertNull(new JdbcTokenRepository(pool).getTokenForSeries("Ada"));
        assertNull(new JdbcApiKeyRepository(pool).findByHash("Ada"));
    }

    private static final String[] CREDENTIAL_TABLES = {"cn1_persistent_logins", "cn1_mfa_totp",
        "cn1_mfa_recovery_code", "cn1_federated_identity", "cn1_webauthn_user",
        "cn1_webauthn_credential", "cn1_api_key", "cn1_oauth2_authorization", "cn1_oauth2_token"};

    private void seedCredentials(DataSource pool, String name) throws Exception {
        String key = SecuritySchema.usernameKey(name);
        pool.execute("INSERT INTO cn1_persistent_logins (series, username_key, username, token_hash, last_used) "
                + "VALUES (?, ?, ?, 'hash', 1)",
                new Object[] {name, key, name});
        pool.execute("INSERT INTO cn1_mfa_totp VALUES (?, 'secret', 'nonce', 1, 1, 1)",
                new Object[] {key});
        pool.execute("INSERT INTO cn1_mfa_recovery_code VALUES (?, ?)", new Object[] {key, name});
        pool.execute("INSERT INTO cn1_federated_identity VALUES ('provider', ?, ?, ?, 1)",
                new Object[] {name, key, name});
        pool.execute("INSERT INTO cn1_webauthn_user VALUES (?, ?, ?, ?, 1)",
                new Object[] {key, name, name, name});
        pool.execute("INSERT INTO cn1_webauthn_credential VALUES (?, ?, ?, -7, 'key', 1, 1, 0, 0, "
                + "'[]', 'phone', 1, 1)", new Object[] {name, name, name});
        pool.execute("INSERT INTO cn1_api_key VALUES (?, ?, ?, '[]', 'cn1_', 'last', 0, 1)",
                new Object[] {name, name, name});
        pool.execute("INSERT INTO cn1_oauth2_authorization VALUES (?, 'client', ?, 'authorization_code', "
                + "'openid', 'active', '{}', 1, 9999999999999)", new Object[] {name, name});
        pool.execute("INSERT INTO cn1_oauth2_token VALUES (?, ?, 'refresh', 0, 9999999999999, 0)",
                new Object[] {name, name});
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void deletingCredentialsAndUserIsOneTransaction(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(pool);
        users.createUser(User.withUsername("Ada").password("old").roles("USER").build());
        seedCredentials(pool, "Ada");
        pool.execute("DROP TABLE cn1_federated_identity", null);
        assertThrows(RuntimeException.class, () -> users.deleteUser("Ada"));
        assertTrue(users.userExists("Ada"));
        assertEquals(1, pool.query("SELECT * FROM cn1_persistent_logins", null).size());
        assertEquals(1, pool.query("SELECT * FROM cn1_mfa_totp", null).size());
        assertEquals(1, pool.query("SELECT * FROM cn1_authorities", null).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void apiKeys(String engine) throws Exception {
        JdbcApiKeyRepository keys = new JdbcApiKeyRepository(open(engine));
        Ticking clock = new Ticking();
        keys.setClock(clock);
        keys.save(new ApiKey("k1", "ada", Arrays.asList("orders:read", "orders:write"), "hash-1",
                "cn1_", "abcd", false));
        clock.now += 1000;
        keys.save(new ApiKey("k2", "ada", new ArrayList<String>(), "hash-2", "cn1_", "wxyz",
                false));
        keys.save(new ApiKey("k3", "ray", Arrays.asList("x"), "HASH-1", "cn1_", "0000", false));

        ApiKey found = keys.findByHash("hash-1");
        assertEquals("k1 ada [orders:read, orders:write] cn1_...abcd false", found.getId() + " "
                + found.getOwner() + " " + found.getScopes() + " " + found.getDisplayName() + " "
                + found.isRevoked());
        // A hash is compared byte for byte on every engine.
        assertEquals("k3", keys.findByHash("HASH-1").getId());
        assertNull(keys.findByHash("Hash-1"));
        assertNull(keys.findByHash(null));
        assertEquals("[]", keys.findByHash("hash-2").getScopes().toString());

        assertTrue(keys.revoke("k1"));
        // Revoked once: the second call changed nothing and says so.
        assertFalse(keys.revoke("k1"));
        assertFalse(keys.revoke("nope"));
        assertTrue(keys.findByHash("hash-1").isRevoked());
        assertFalse(keys.findByHash("hash-2").isRevoked());

        List<ApiKey> adas = keys.findByOwner("ada");
        assertEquals("k1,k2", adas.get(0).getId() + "," + adas.get(1).getId());
        assertEquals(2, adas.size());
        assertTrue(keys.delete("k2"));
        assertFalse(keys.delete("k2"));
        assertNull(keys.findByHash("hash-2"));
        // Two keys cannot share an id or a hash.
        assertThrows(IOException.class, () -> keys.save(new ApiKey("k1", "eve",
                new ArrayList<String>(), "hash-9", "cn1_", "eeee", false)));
        assertThrows(IllegalArgumentException.class, () -> keys.save(new ApiKey("k9", "eve",
                Arrays.asList("two words"), "hash-8", "cn1_", "eeee", false)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void persistentLogins(String engine) throws Exception {
        JdbcTokenRepository tokens = new JdbcTokenRepository(open(engine));
        tokens.createNewToken(new PersistentRememberMeToken("Ada", "series-1", "hash-a", 1000L));
        tokens.createNewToken(new PersistentRememberMeToken("Ada", "series-2", "hash-b", 2000L));
        tokens.createNewToken(new PersistentRememberMeToken("ray", "series-3", "hash-c", 500L));
        PersistentRememberMeToken one = tokens.getTokenForSeries("series-1");
        assertEquals("Ada series-1 hash-a 1000", one.getUsername() + " " + one.getSeries() + " "
                + one.getTokenHash() + " " + one.getLastUsed());
        assertNull(tokens.getTokenForSeries("SERIES-1"));
        assertNull(tokens.getTokenForSeries(null));

        // Replaced only by whoever names the token that is there.
        assertFalse(tokens.updateToken("series-1", "hash-wrong", "hash-z", 5000L));
        assertEquals("hash-a", tokens.getTokenForSeries("series-1").getTokenHash());
        assertTrue(tokens.updateToken("series-1", "hash-a", "hash-a2", 5000L));
        assertFalse(tokens.updateToken("series-1", "hash-a", "hash-a3", 6000L));
        one = tokens.getTokenForSeries("series-1");
        assertEquals("hash-a2 5000", one.getTokenHash() + " " + one.getLastUsed());
        assertEquals("hash-a", one.getPreviousTokenHash());
        assertNull(tokens.getTokenForSeries("series-2").getPreviousTokenHash());

        assertEquals(1, tokens.deleteExpired(10000L, 9));
        assertNull(tokens.getTokenForSeries("series-3"));
        // Every browser of the user, whatever case the name is given in.
        tokens.removeUserTokens("ADA");
        assertNull(tokens.getTokenForSeries("series-1"));
        assertNull(tokens.getTokenForSeries("series-2"));
        tokens.createNewToken(new PersistentRememberMeToken("ada", "series-4", "h", 1L));
        tokens.removeToken("series-4");
        assertNull(tokens.getTokenForSeries("series-4"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void replacedTotpEnrollmentRejectsPreviouslyVerifiedCodes(String engine) throws Exception {
        DataSource pool = open(engine);
        JdbcTotpRepository repo = new JdbcTotpRepository(pool, Crypto.sha256(new byte[] {1, 2, 3}));
        MfaTest.checkReplacement(repo, true);
        MfaTest.checkReplacement(repo, false);
        byte[] secret = new byte[] {1, 2, 3, 4};
        repo.save("ada", secret);
        java.util.concurrent.ExecutorService callers = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            for (boolean confirming : new boolean[] {true, false}) {
                CountDownLatch start = new CountDownLatch(1);
                List<java.util.concurrent.Future<Boolean>> answers = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    // Independent repository instances exercise the shared database boundary.
                    JdbcTotpRepository other = new JdbcTotpRepository(pool,
                            Crypto.sha256(new byte[] {1, 2, 3}));
                    answers.add(callers.submit(() -> {
                        start.await();
                        return confirming ? other.confirm("ADA", secret, 100)
                                : other.advance("ADA", secret, 101);
                    }));
                }
                start.countDown();
                int accepted = 0;
                for (java.util.concurrent.Future<Boolean> answer : answers) {
                    if (answer.get(30, java.util.concurrent.TimeUnit.SECONDS)) {
                        accepted++;
                    }
                }
                assertEquals(1, accepted, "one code must be consumed exactly once");
            }
        } finally {
            callers.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void secondFactors(String engine) throws Exception {
        DataSource pool = open(engine);
        byte[] key = Crypto.sha256("a key".getBytes("UTF-8"));
        JdbcTotpRepository totp = new JdbcTotpRepository(pool, key);
        byte[] secret = "12345678901234567890".getBytes("UTF-8");
        totp.save("Ada", secret);
        TotpCredential stored = totp.find("ADA");
        assertTrue(Arrays.equals(secret, stored.getSecret()));
        assertFalse(stored.isConfirmed());
        assertEquals(-1L, stored.getLastUsedStep());
        assertNull(totp.find("ray"));

        // What the database holds is not the secret, and not its base64.
        Map row = pool.queryOne("SELECT secret, nonce FROM cn1_mfa_totp", null);
        String sealed = String.valueOf(row.get("secret"));
        assertFalse(sealed.contains("1234567890"), sealed);
        assertFalse(sealed.contains(com.codename1.backend.Base64.encode(secret)), sealed);
        assertEquals(12, com.codename1.backend.Base64.decode(String.valueOf(row.get("nonce")))
                .length);

        assertTrue(totp.confirm("ada"));
        assertFalse(totp.confirm("ada"), "confirmed twice");
        assertTrue(totp.find("ada").isConfirmed());

        // The replay guard: a step is recorded once, and never an older one.
        assertTrue(totp.advance("ada", 100));
        assertFalse(totp.advance("ada", 100));
        assertFalse(totp.advance("ada", 99));
        assertTrue(totp.advance("ADA", 101));
        assertEquals(101L, totp.find("ada").getLastUsedStep());
        assertFalse(totp.advance("nobody", 500));

        // Another key does not open it, and says which setting to look at.
        JdbcTotpRepository otherKey = new JdbcTotpRepository(pool,
                Crypto.sha256("another key".getBytes("UTF-8")));
        assertTrue(assertThrows(AuthenticationServiceException.class,
                () -> otherKey.find("ada")).getMessage().contains("does not open with "
                        + "cn1.security.mfa.encryptionKey"));
        // Nor does a row copied onto another user: the name is bound in.
        pool.execute("INSERT INTO cn1_mfa_totp (username_key, secret, nonce, confirmed, "
                + "last_used_step, created_at) SELECT 'eve', secret, nonce, confirmed, "
                + "last_used_step, created_at FROM cn1_mfa_totp WHERE username_key = 'ada'", null);
        assertTrue(assertThrows(AuthenticationServiceException.class,
                () -> totp.find("eve")).getMessage().contains("does not open"));

        // Enrolling again replaces the secret and starts unconfirmed.
        totp.save("ada", "ABCDEFGHIJKLMNOPQRST".getBytes("UTF-8"));
        assertFalse(totp.find("ada").isConfirmed());
        assertEquals(-1L, totp.find("ada").getLastUsedStep());
        assertTrue(totp.delete("ada"));
        assertFalse(totp.delete("ada"));
        assertNull(totp.find("ada"));

        // The whole service over the database: one code, once.
        Ticking clock = new Ticking();
        TotpService service = new TotpService(totp, "Acme");
        service.setClock(clock);
        assertTrue(service.beginEnrollment("ray").getOtpauthUri().startsWith("otpauth://totp/"));
        assertTrue(service.confirmEnrollment("ray", service.currentCode("ray")));
        clock.now += 30000;
        String code = service.currentCode("ray");
        assertTrue(service.verify("ray", code));
        assertFalse(service.verify("ray", code), "a code was accepted twice");

        RecoveryCodeService recovery = new RecoveryCodeService(new JdbcRecoveryCodeRepository(pool));
        List<String> codes = recovery.generate("Ada");
        assertEquals(10, codes.size());
        assertEquals(10, recovery.remaining("ada"));
        assertFalse(String.valueOf(pool.query("SELECT code_hash FROM cn1_mfa_recovery_code", null))
                .contains(codes.get(0)), "a recovery code is stored as it is");
        assertFalse(recovery.consume("ray", codes.get(0)), "another user's code");
        assertTrue(recovery.consume("ADA", codes.get(0)));
        assertFalse(recovery.consume("ada", codes.get(0)), "a recovery code worked twice");
        assertEquals(9, recovery.remaining("ada"));
        // Generating again replaces what was left.
        List<String> fresh = recovery.generate("ada");
        assertFalse(recovery.consume("ada", codes.get(1)));
        assertTrue(recovery.consume("ada", fresh.get(9)));
        assertEquals(9, recovery.remaining("ada"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void rateLimits(String engine) throws Exception {
        DataSource pool = open(engine);
        Ticking clock = new Ticking();
        JdbcRateLimiter login = new JdbcRateLimiter(pool, "login", 3, 60);
        JdbcRateLimiter api = new JdbcRateLimiter(pool, "api", 1, 60);
        login.setClock(clock);
        api.setClock(clock);
        assertTrue(login.tryAcquire("ip:1"));
        clock.now += 10000;
        assertTrue(login.tryAcquire("ip:1"));
        assertTrue(login.tryAcquire("ip:1"));
        assertFalse(login.tryAcquire("ip:1"), "a fourth request in the window");
        // The window is counted from its first request: 50 seconds are left.
        assertEquals(50L, login.retryAfterSeconds("ip:1"));
        // Another key, and another limiter, count apart.
        assertTrue(login.tryAcquire("ip:2"));
        assertTrue(api.tryAcquire("ip:1"));
        assertFalse(api.tryAcquire("ip:1"));
        assertEquals(1L, login.retryAfterSeconds("ip:unknown"));

        clock.now += 49000;
        assertFalse(login.tryAcquire("ip:1"));
        clock.now += 1000;
        // The window has passed: a new one starts with this request.
        assertTrue(login.tryAcquire("ip:1"));
        assertEquals(60L, login.retryAfterSeconds("ip:1"));
        assertEquals(2L, ((Number) pool.queryOne("SELECT hits FROM cn1_rate_limit WHERE "
                + "limit_key = 'login|ip:1'", null).get("hits")).longValue() + 1);

        clock.now += 3600000;
        assertTrue(login.deleteExpired(60) >= 3);
        assertEquals(0L, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM cn1_rate_limit", null)
                .get("n")).longValue());

        // Forgetting a key gives it a full window again, and touches no other
        // key and no other limiter.
        for (int hit = 0; hit < 3; hit++) {
            assertTrue(login.tryAcquire("ip:9"));
        }
        assertTrue(api.tryAcquire("ip:9"));
        assertFalse(login.tryAcquire("ip:9"));
        login.reset("ip:9");
        login.reset("ip:never-seen");
        for (int hit = 0; hit < 3; hit++) {
            assertTrue(login.tryAcquire("ip:9"), "after reset, hit " + hit);
        }
        assertFalse(login.tryAcquire("ip:9"));
        assertFalse(api.tryAcquire("ip:9"), "another limiter's count went with it");

        // Keys longer than the column: told apart by all of their text, not by as much of
        // it as fits. Cut off, two users with a long name in common were one row, and so
        // was every caller of a limiter whose own name was long.
        StringBuilder common = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            common.append('u');
        }
        JdbcRateLimiter byUser = new JdbcRateLimiter(pool, "byUser", 1, 60);
        byUser.setClock(clock);
        assertTrue(byUser.tryAcquire(common + "-ada"));
        assertTrue(byUser.tryAcquire(common + "-grace"), "another user's permit was taken");
        assertFalse(byUser.tryAcquire(common + "-ada"));
        byUser.reset(common + "-ada");
        assertTrue(byUser.tryAcquire(common + "-ada"));
        assertFalse(byUser.tryAcquire(common + "-grace"), "reset reached another user's row");
        JdbcRateLimiter longName = new JdbcRateLimiter(pool, common.toString(), 1, 60);
        longName.setClock(clock);
        assertTrue(longName.tryAcquire("ip:1"));
        assertTrue(longName.tryAcquire("ip:2"), "every caller of a long-named limiter was one row");
        assertFalse(longName.tryAcquire("ip:1"));
        assertEquals(0L, ((Number) pool.queryOne("SELECT COUNT(*) AS n FROM cn1_rate_limit WHERE "
                + "LENGTH(limit_key) > 200", null).get("n")).longValue());

        // Atomic: many callers at one moment, as several processes would be,
        // and exactly the permits are taken -- for a key that has no row yet,
        // where the callers race to insert it.
        final JdbcRateLimiter burst = new JdbcRateLimiter(pool, "burst", 5, 60);
        burst.setClock(clock);
        final AtomicInteger allowed = new AtomicInteger();
        final CountDownLatch start = new CountDownLatch(1);
        Thread[] callers = new Thread[4];
        for (int i = 0; i < callers.length; i++) {
            callers[i] = new Thread(() -> {
                try {
                    start.await();
                    for (int n = 0; n < 6; n++) {
                        if (burst.tryAcquire("shared")) {
                            allowed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            });
            callers[i].start();
        }
        start.countDown();
        for (Thread caller : callers) {
            caller.join(30000);
        }
        assertEquals(5, allowed.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void rateLimitCleanupPreservesActiveWindows(String engine) throws Exception {
        DataSource pool = open(engine);
        Ticking clock = new Ticking();
        JdbcRateLimiter daily = new JdbcRateLimiter(pool, "daily", 1, 86400);
        JdbcRateLimiter minute = new JdbcRateLimiter(pool, "minute", 1, 60);
        daily.setClock(clock);
        minute.setClock(clock);
        assertTrue(daily.tryAcquire("client"));
        assertTrue(minute.tryAcquire("client"));
        clock.now += 3601000;
        assertEquals(0, daily.deleteExpired(3600));
        assertEquals(0, minute.deleteExpired(3600));
        assertFalse(daily.tryAcquire("client"));
        clock.now += 60000;
        assertEquals(1, minute.deleteExpired(3600));
        assertFalse(daily.tryAcquire("client"));
        clock.now += 86400000;
        assertEquals(1, daily.deleteExpired(3600));
        assertThrows(IllegalArgumentException.class, () -> daily.deleteExpired(-1));
    }

    @org.junit.jupiter.api.Test
    void theEncryptionKeyIsRequiredOutsideDevelopment() throws Exception {
        DataSource pool = open("SQLITE");
        Properties none = new Properties();
        assertTrue(assertThrows(IllegalStateException.class,
                () -> JdbcTotpRepository.fromConfig(pool, Config.of(none, "prod"))).getMessage()
                .startsWith("cn1.security.mfa.encryptionKey is not set"));
        // A development profile gets a fixed key rather than a refusal.
        assertNotNull(JdbcTotpRepository.fromConfig(pool, Config.of(none, "dev")));
        Properties bad = new Properties();
        bad.setProperty("cn1.security.mfa.encryptionKey", "c2hvcnQ=");
        assertEquals("cn1.security.mfa.encryptionKey must be 32 bytes in base64, and is 5 bytes",
                assertThrows(IllegalStateException.class,
                        () -> JdbcTotpRepository.fromConfig(pool, Config.of(bad, "prod")))
                        .getMessage());
        Properties good = new Properties();
        good.setProperty("cn1.security.mfa.encryptionKey", com.codename1.backend.Base64.encode(
                Crypto.sha256("k".getBytes("UTF-8"))));
        JdbcTotpRepository repository = JdbcTotpRepository.fromConfig(pool, Config.of(good, "prod"));
        repository.save("ada", "secret-secret-secret".getBytes("UTF-8"));
        assertNotEquals(null, repository.find("ada"));
    }
}
