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
package com.codename1.backend.security.webauthn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64Url;
import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.Migrations;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.SecuritySchema;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// The passkey tables and the repositories over them, against real databases:
/// SQLITE always, on a file; POSTGRES and MYSQL when CN1_TX_POSTGRES /
/// CN1_TX_MYSQL name one, as for the other security stores.
///
/// What the ceremonies rely on is decided by the database -- a key that
/// refuses a second credential of one id, a conditional UPDATE's row count,
/// text compared byte for byte -- so every "process" here is a pool of its
/// own, and the in-memory repositories are held to the same answers.
class WebAuthnStoresTest {
    @TempDir
    File dir;

    private static final String[] TABLES = {"cn1_users", "cn1_authorities", "cn1_api_key",
        "cn1_persistent_logins", "cn1_mfa_totp", "cn1_mfa_recovery_code", "cn1_rate_limit",
        "cn1_federated_identity", "cn1_oauth2_registered_client", "cn1_oauth2_authorization",
        "cn1_oauth2_token", "cn1_webauthn_user", "cn1_webauthn_credential",
        "cn1_security_schema_history"};

    private final List<DataSource> opened = new ArrayList<DataSource>();
    private final List<DataSource> shared = new ArrayList<DataSource>();
    private String sqlite;

    @AfterEach
    void closeAll() {
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

    /// A database with the schema applied and nothing in it; "MEMORY" is no
    /// database at all.
    private DataSource open(String engine) throws IOException {
        if ("MEMORY".equals(engine)) {
            return null;
        }
        DataSource pool = another(engine);
        if (!"SQLITE".equals(engine)) {
            for (String table : TABLES) {
                pool.execute("DROP TABLE IF EXISTS " + table, null);
            }
            shared.add(pool);
        }
        Migrations.of(pool, SecuritySchema.migrations()).migrate();
        return pool;
    }

    /// One more pool on the same database: another server.
    private DataSource another(String engine) throws IOException {
        DataSource pool;
        if ("SQLITE".equals(engine)) {
            if (sqlite == null) {
                sqlite = new File(dir, "passkeys.db").getPath();
            }
            pool = DataSource.open(sqlite, 4, 5000, 10000);
        } else {
            String url = System.getenv("CN1_TX_" + engine);
            Assumptions.assumeTrue(url != null && url.length() > 0,
                    "CN1_TX_" + engine + " is unset; this engine is not exercised");
            pool = DataSource.open(url, 4, 5000, 10000);
        }
        opened.add(pool);
        return pool;
    }

    private static UserCredentialRepository credentials(DataSource pool) {
        return pool == null ? new InMemoryUserCredentialRepository()
                : new JdbcUserCredentialRepository(pool);
    }

    private static PublicKeyCredentialUserEntityRepository users(DataSource pool) {
        return pool == null ? new InMemoryPublicKeyCredentialUserEntityRepository()
                : new JdbcPublicKeyCredentialUserEntityRepository(pool);
    }

    private static byte[] bytes(int length, int seed) {
        byte[] out = new byte[length];
        for (int iter = 0 ; iter < length ; iter++) {
            out[iter] = (byte) (seed * 31 + iter * 7);
        }
        return out;
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEMORY", "SQLITE", "POSTGRES", "MYSQL"})
    void aUserHasOneHandleAndAHandleOneUser(String engine) throws Exception {
        PublicKeyCredentialUserEntityRepository users = users(open(engine));
        assertNull(users.findByUsername("Ada"));
        assertNull(users.findById(bytes(32, 1)));
        assertNull(users.findByUsername(null));
        assertNull(users.findById(null));
        PublicKeyCredentialUserEntity ada = users.save(new PublicKeyCredentialUserEntity("Ada",
                bytes(32, 1), "Ada L."));
        assertArrayEquals(bytes(32, 1), ada.getId());
        // Found whatever the case of the name, and as it was stored.
        for (String asked : new String[] {"Ada", "ada", "ADA"}) {
            PublicKeyCredentialUserEntity found = users.findByUsername(asked);
            assertEquals("Ada Ada L.", found.getName() + " " + found.getDisplayName());
            assertArrayEquals(bytes(32, 1), found.getId());
        }
        assertEquals("Ada", users.findById(bytes(32, 1)).getName());
        // A second handle for the same user is not taken: the first stands.
        PublicKeyCredentialUserEntity again = users.save(new PublicKeyCredentialUserEntity("ADA",
                bytes(32, 2), "Somebody else"));
        assertArrayEquals(bytes(32, 1), again.getId());
        assertEquals("Ada L.", again.getDisplayName());
        assertNull(users.findById(bytes(32, 2)));
        // Nor one handle for a second user.
        assertThrows(RuntimeException.class, () -> users.save(
                new PublicKeyCredentialUserEntity("eve", bytes(32, 1), null)));
        assertNull(users.findByUsername("eve"));
        assertEquals("Ada", users.findById(bytes(32, 1)).getName());

        // Handles are compared byte for byte: these two are the same text but
        // for its case once encoded, and are two users.
        byte[] upper = Base64Url.decode("AAAAAAAAAAAA");
        byte[] lower = Base64Url.decode("aaaaaaaaaaaa");
        users.save(new PublicKeyCredentialUserEntity("upper", upper, null));
        users.save(new PublicKeyCredentialUserEntity("lower", lower, null));
        assertEquals("upper", users.findById(upper).getName());
        assertEquals("lower", users.findById(lower).getName());

        users.delete(bytes(32, 1));
        assertNull(users.findByUsername("ada"));
        assertNull(users.findById(bytes(32, 1)));
        users.delete(bytes(32, 1));
        users.delete(null);
        assertEquals("upper", users.findByUsername("UPPER").getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void serversThatMakeAHandleAtOnceAgreeOnOne(String engine) throws Exception {
        open(engine);
        final int servers = 6;
        final List<PublicKeyCredentialUserEntityRepository> repositories =
                new ArrayList<PublicKeyCredentialUserEntityRepository>();
        for (int iter = 0 ; iter < servers ; iter++) {
            repositories.add(users(another(engine)));
        }
        final String[] handles = new String[servers];
        final List<Throwable> failures = new ArrayList<Throwable>();
        final CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[servers];
        for (int iter = 0 ; iter < servers ; iter++) {
            final int index = iter;
            threads[iter] = new Thread(() -> {
                try {
                    start.await();
                    handles[index] = Base64Url.encode(repositories.get(index).save(
                            new PublicKeyCredentialUserEntity("ada", bytes(32, 100 + index),
                                    null)).getId());
                } catch (Throwable err) {
                    synchronized (failures) {
                        failures.add(err);
                    }
                }
            });
            threads[iter].start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join(30000);
        }
        assertTrue(failures.isEmpty(), failures.toString());
        // Every server was answered the same handle: the one that is stored.
        String stored = Base64Url.encode(repositories.get(0).findByUsername("ada").getId());
        for (String handle : handles) {
            assertEquals(stored, handle);
        }
    }

    private static CredentialRecord record(byte[] id, byte[] user, long count) {
        return CredentialRecord.builder().credentialId(id).userEntityUserId(user)
                .algorithm(CoseKey.ES256).publicKey(bytes(91, 3)).signatureCount(count)
                .created(1000).lastUsed(1000).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEMORY", "SQLITE", "POSTGRES", "MYSQL"})
    void credentials(String engine) throws Exception {
        UserCredentialRepository credentials = credentials(open(engine));
        byte[] ada = bytes(32, 1);
        byte[] eve = bytes(32, 2);
        assertNull(credentials.findByCredentialId(bytes(16, 9)));
        assertNull(credentials.findByCredentialId(null));
        assertEquals(0, credentials.findByUserId(ada).size());
        assertEquals(0, credentials.findByUserId(null).size());

        // Everything of a record comes back as it went in: the longest id the
        // specification allows, an RSA key of 4096 bits, every flag.
        byte[] longest = bytes(1023, 5);
        byte[] rsaKey = bytes(550, 6);
        CredentialRecord full = CredentialRecord.builder().credentialId(longest)
                .userEntityUserId(ada).algorithm(CoseKey.RS256).publicKey(rsaKey)
                .signatureCount(4000000000L).uvInitialized(true).backupEligible(true)
                .backupState(false).transports(Arrays.asList("internal", "hybrid", "usb"))
                .label("Ada's \"work\" key, 'main'; -- it").created(1700000000000L)
                .lastUsed(1700000005000L).build();
        assertTrue(credentials.save(full));
        CredentialRecord read = credentials.findByCredentialId(longest);
        assertArrayEquals(longest, read.getCredentialId());
        assertArrayEquals(ada, read.getUserEntityUserId());
        assertArrayEquals(rsaKey, read.getPublicKey());
        assertEquals(CoseKey.RS256, read.getAlgorithm());
        assertEquals(4000000000L, read.getSignatureCount());
        assertTrue(read.isUvInitialized() && read.isBackupEligible() && !read.isBackupState());
        assertEquals("[internal, hybrid, usb]", read.getTransports().toString());
        assertEquals("Ada's \"work\" key, 'main'; -- it", read.getLabel());
        assertEquals("1700000000000 1700000005000", read.getCreated() + " " + read.getLastUsed());

        CredentialRecord plain = record(bytes(16, 7), ada, 0);
        assertTrue(credentials.save(plain));
        read = credentials.findByCredentialId(bytes(16, 7));
        assertFalse(read.isUvInitialized() || read.isBackupEligible() || read.isBackupState());
        assertEquals("[] ''", read.getTransports() + " '" + read.getLabel() + "'");

        // An id is one credential's: a second is refused whoever it is for, and
        // the first is as it was.
        assertFalse(credentials.save(CredentialRecord.builder().credentialId(bytes(16, 7))
                .userEntityUserId(eve).publicKey(bytes(91, 8)).build()));
        assertFalse(credentials.save(plain));
        read = credentials.findByCredentialId(bytes(16, 7));
        assertArrayEquals(ada, read.getUserEntityUserId());
        assertArrayEquals(bytes(91, 3), read.getPublicKey());
        assertEquals(0, credentials.findByUserId(eve).size());
        // A prefix of an id, and an id that differs in one bit, are other ids.
        assertNull(credentials.findByCredentialId(bytes(15, 7)));
        byte[] near = bytes(16, 7);
        near[15] ^= 1;
        assertNull(credentials.findByCredentialId(near));
        assertTrue(credentials.save(record(near, eve, 5)));

        // A user's credentials, oldest first, and nobody else's.
        List<CredentialRecord> adas = credentials.findByUserId(ada);
        assertEquals(2, adas.size());
        assertArrayEquals(bytes(16, 7), adas.get(0).getCredentialId());
        assertArrayEquals(longest, adas.get(1).getCredentialId());
        assertEquals(1, credentials.findByUserId(eve).size());

        assertTrue(credentials.delete(bytes(16, 7)));
        assertFalse(credentials.delete(bytes(16, 7)));
        assertFalse(credentials.delete(null));
        assertNull(credentials.findByCredentialId(bytes(16, 7)));
        assertEquals(1, credentials.findByUserId(ada).size());
        assertNotNull(credentials.findByCredentialId(near));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEMORY", "SQLITE", "POSTGRES", "MYSQL"})
    void theCounterMovesForwardAndNeverBack(String engine) throws Exception {
        UserCredentialRepository credentials = credentials(open(engine));
        byte[] id = bytes(32, 4);
        assertTrue(credentials.save(record(id, bytes(32, 1), 0)));
        // No counter: zero stays zero, every time, and the rest is recorded.
        assertTrue(credentials.advance(id, 0, true, true, 2000));
        assertTrue(credentials.advance(id, 0, true, false, 3000));
        CredentialRecord read = credentials.findByCredentialId(id);
        assertEquals("0 true false 3000", read.getSignatureCount() + " " + read.isUvInitialized()
                + " " + read.isBackupState() + " " + read.getLastUsed());
        // Forward.
        assertTrue(credentials.advance(id, 1, true, false, 4000));
        assertTrue(credentials.advance(id, 10, true, false, 5000));
        // The same, less, and back to zero: refused, and nothing changed.
        assertFalse(credentials.advance(id, 10, false, true, 6000));
        assertFalse(credentials.advance(id, 9, false, true, 6000));
        assertFalse(credentials.advance(id, 0, false, true, 6000));
        read = credentials.findByCredentialId(id);
        assertEquals("10 true false 5000", read.getSignatureCount() + " " + read.isUvInitialized()
                + " " + read.isBackupState() + " " + read.getLastUsed());
        // The full range of a 32-bit unsigned counter.
        assertTrue(credentials.advance(id, 4294967295L, true, false, 7000));
        assertEquals(4294967295L, credentials.findByCredentialId(id).getSignatureCount());
        // A credential that is not there advances nothing.
        assertFalse(credentials.advance(bytes(32, 99), 1, true, false, 8000));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void ofServersThatTakeTheSameCountOrTheSameIdOneWins(String engine) throws Exception {
        open(engine);
        final int servers = 6;
        final List<UserCredentialRepository> repositories =
                new ArrayList<UserCredentialRepository>();
        for (int iter = 0 ; iter < servers ; iter++) {
            repositories.add(credentials(another(engine)));
        }
        final byte[] id = bytes(32, 4);
        // The same credential id, registered by six servers at once for six users.
        final AtomicInteger saved = new AtomicInteger();
        race(servers, index -> {
            if (repositories.get(index).save(record(id, bytes(32, 10 + index), 0))) {
                saved.incrementAndGet();
            }
        });
        assertEquals(1, saved.get());
        // The same assertion -- one count -- taken by six servers at once, round
        // after round.
        for (int round = 1 ; round <= 5 ; round++) {
            final long count = round;
            final AtomicInteger advanced = new AtomicInteger();
            race(servers, index -> {
                if (repositories.get(index).advance(id, count, true, false, 1000 + count)) {
                    advanced.incrementAndGet();
                }
            });
            assertEquals(1, advanced.get(), "round " + round);
        }
        assertEquals(5, repositories.get(0).findByCredentialId(id).getSignatureCount());
    }

    private interface Step {
        void run(int index) throws Exception;
    }

    private static void race(int count, final Step step) throws Exception {
        final List<Throwable> failures = new ArrayList<Throwable>();
        final CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[count];
        for (int iter = 0 ; iter < count ; iter++) {
            final int index = iter;
            threads[iter] = new Thread(() -> {
                try {
                    start.await();
                    step.run(index);
                } catch (Throwable err) {
                    synchronized (failures) {
                        failures.add(err);
                    }
                }
            });
            threads[iter].start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join(30000);
        }
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void theCeremoniesOverTheDatabase(String engine) throws Exception {
        DataSource pool = open(engine);
        WebAuthnRelyingPartyOperations ops = new WebAuthnRelyingPartyOperations(
                new PublicKeyCredentialRpEntity("example.org", "Example"),
                Arrays.asList("https://example.org"), users(pool), credentials(pool));
        // Another server on the same database, with nothing in common but it.
        DataSource second = another(engine);
        WebAuthnRelyingPartyOperations other = new WebAuthnRelyingPartyOperations(
                new PublicKeyCredentialRpEntity("example.org", "Example"),
                Arrays.asList("https://example.org"), users(second), credentials(second));

        SoftAuthenticator ec = new SoftAuthenticator(false, "example.org", "https://example.org");
        SoftAuthenticator rsa = new SoftAuthenticator(true, "example.org", "https://example.org");
        rsa.format = "packed";
        for (SoftAuthenticator authenticator : new SoftAuthenticator[] {ec, rsa}) {
            PublicKeyCredentialCreationOptions creation =
                    ops.createPublicKeyCredentialCreationOptions("Ada");
            other.registerCredential(creation, json(authenticator.create(creation.toMap())), "k");
        }
        // Both keys are ada's, under the one handle she was given.
        assertArrayEquals(ec.userHandle, rsa.userHandle);
        assertEquals(2, other.createPublicKeyCredentialCreationOptions("ada")
                .getExcludeCredentials().size());

        for (SoftAuthenticator authenticator : new SoftAuthenticator[] {ec, rsa}) {
            PublicKeyCredentialRequestOptions request = ops.createCredentialRequestOptions(null);
            Map<String, Object> answer = json(authenticator.get(request.toMap()));
            assertEquals("Ada", other.authenticate(request, answer).getUser().getName());
            // The same answer again, at the other server: the counter it
            // carries is no longer ahead of what is stored.
            assertEquals(WebAuthnException.COUNTER_REGRESSION, assertThrows(
                    WebAuthnException.class, () -> ops.authenticate(request, answer)).getReason());
            assertEquals(1, new JdbcUserCredentialRepository(pool).findByCredentialId(
                    authenticator.credentialId).getSignatureCount());
        }
        // A second registration of the first key's id, for somebody else.
        SoftAuthenticator eves = new SoftAuthenticator(false, "example.org",
                "https://example.org");
        eves.credentialId = ec.credentialId.clone();
        PublicKeyCredentialCreationOptions forEve =
                ops.createPublicKeyCredentialCreationOptions("eve");
        assertEquals(WebAuthnException.CREDENTIAL_EXISTS, assertThrows(WebAuthnException.class,
                () -> other.registerCredential(forEve, json(eves.create(forEve.toMap())), null))
                .getReason());

        // A store that cannot be asked refuses the ceremony; it does not pass it.
        pool.execute("DROP TABLE cn1_webauthn_credential", null);
        PublicKeyCredentialRequestOptions request = ops.createCredentialRequestOptions(null);
        assertThrows(AuthenticationServiceException.class,
                () -> ops.authenticate(request, json(ec.get(request.toMap()))));
    }

    private static Map<String, Object> json(Map<String, Object> value) throws IOException {
        return Json.parseObject(Json.write(value));
    }
}
