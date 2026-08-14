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
package com.codename1.impl.javase;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.testing.DatabaseConformanceSuite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sqlite.SQLiteConfig;
import org.sqlite.mc.SQLiteMCSqlCipherConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs the portable database contract against the real simulator implementation.
 *
 * SEDatabase takes a plain JDBC Connection, so this exercises the shipping code headlessly in a
 * couple of seconds on every pull request, rather than waiting for a device job.
 */
public class SEDatabaseConformanceTest {

    /** Collects results so one run reports every violation instead of stopping at the first. */
    private static final class CollectingReporter implements DatabaseConformanceSuite.Reporter {
        private final List<String> failures = new ArrayList<String>();
        private final List<String> skips = new ArrayList<String>();
        private int passed;

        public void check(boolean condition, String message) {
            if (condition) {
                passed++;
            } else {
                failures.add(message);
            }
        }

        public void skip(String reason) {
            skips.add(reason);
        }

        public void info(String message) {
            // Diagnostics only; nothing to assert.
        }

        void assertClean(String label) {
            if (!failures.isEmpty()) {
                StringBuilder b = new StringBuilder();
                b.append(label).append(": ").append(failures.size())
                        .append(" conformance failures (").append(passed).append(" passed)\n");
                for (int iter = 0; iter < failures.size(); iter++) {
                    b.append("  - ").append(failures.get(iter)).append('\n');
                }
                fail(b.toString());
            }
            assertTrue(passed > 0, label + " ran no checks at all");
        }
    }

    private File dbFile;
    private SEDatabase db;

    @BeforeEach
    public void setUp() throws Exception {
        Database.setLegacyBehavior(false);
        // No Display is up here, so the suite cannot autodetect which port it is running against
        // and the legacy expectations would fall back to the portable contract.
        DatabaseConformanceSuite.setPortKind(DatabaseConformanceSuite.PORT_SIMULATOR);
        Class.forName("org.sqlite.JDBC");
        dbFile = File.createTempFile("cn1-conformance", ".db");
        assertTrue(dbFile.delete());
        db = identified(openPlain(dbFile), dbFile);
    }

    /**
     * A database that knows which file it holds, the way JavaSEPort opens one.
     *
     * The connection-taking constructor deliberately cannot say, and a key change through it is
     * refused for that reason -- so a test that re-keys has to identify its file like the port
     * does, taking the claim first and passing the same key on.
     */
    private static SEDatabase identified(java.sql.Connection conn, File file) throws Exception {
        String openKey = file.getCanonicalPath();
        SEDatabase.reserveConnection(openKey);
        boolean kept = false;
        try {
            SEDatabase opened = new SEDatabase(conn, file.getName(), openKey);
            kept = true;
            return opened;
        } finally {
            if (!kept) {
                SEDatabase.releaseConnection(openKey);
            }
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (db != null) {
            try {
                db.close();
            } catch (Exception ignored) {
                // The test's own assertions matter more than a cleanup failure.
            }
        }
        if (dbFile != null) {
            dbFile.delete();
        }
        Database.setLegacyBehavior(false);
        DatabaseConformanceSuite.setPortKind(DatabaseConformanceSuite.PORT_AUTODETECT);
    }

    @Test
    public void rewindingAfterAnotherConnectionWroteSeesTheNewRows() throws Exception {
        // A cursor caches its row count once it has walked to the end. Rewinding re-executes the
        // statement, and outside a transaction the new pass can see rows another connection wrote
        // in between -- so a count carried over from the first pass answers for a result set that
        // no longer exists, and last() stops on what used to be the final row.
        db.execute("CREATE TABLE watched (id INTEGER PRIMARY KEY)");
        db.execute("INSERT INTO watched (id) VALUES (1)");
        db.execute("INSERT INTO watched (id) VALUES (2)");

        Cursor cur = db.executeQuery("SELECT id FROM watched ORDER BY id");
        try {
            assertTrue(cur.last(), "walked to the end of the first pass");
            assertEquals(2, com.codename1.db.Database.count(cur), "two rows to begin with");

            com.codename1.db.Database.beforeFirst(cur);
            // Written through this connection rather than a second one: an open cursor holds a
            // read lock that a separate connection cannot write past, and the cache does not care
            // who wrote. What it must not do is answer for the pass that has been rewound away.
            db.execute("INSERT INTO watched (id) VALUES (3)");

            assertEquals(3, com.codename1.db.Database.count(cur),
                    "the rewound pass counts the row written since");
            assertTrue(cur.last(), "and last() reaches it");
            assertEquals(3, cur.getRow().getInteger(0), "which is the new final row");
        } finally {
            cur.close();
        }
    }

    @Test
    public void aSavepointTransactionEndsThroughARawCommit() throws Exception {
        // A SAVEPOINT opens a transaction JDBC never hears about, so it stays in autocommit and
        // its commit() rejects the call. The script form has to work here exactly as it does on
        // the native ports.
        db.execute("CREATE TABLE sp (id INTEGER PRIMARY KEY)");
        db.execute("SAVEPOINT s");
        db.execute("INSERT INTO sp (id) VALUES (1)");
        db.execute("COMMIT");
        assertFalse(db.isInTransaction(), "the COMMIT ended it");

        Cursor cur = db.executeQuery("SELECT count(*) FROM sp");
        try {
            assertTrue(cur.next());
            assertEquals(1, cur.getRow().getInteger(0), "and the row committed");
        } finally {
            cur.close();
        }

        db.execute("SAVEPOINT s2");
        db.execute("INSERT INTO sp (id) VALUES (2)");
        db.execute("ROLLBACK");
        assertFalse(db.isInTransaction(), "the ROLLBACK ended it too");
        cur = db.executeQuery("SELECT count(*) FROM sp");
        try {
            assertTrue(cur.next());
            assertEquals(1, cur.getRow().getInteger(0), "and discarded its row");
        } finally {
            cur.close();
        }
    }

    @Test
    public void aWrappedConnectionIsVisibleToAnotherHandlesKeyChange() throws Exception {
        // The connection-taking constructor takes the file from the connection's own JDBC URL. An
        // unregistered handle is invisible to the sole-connection check, so a database opened the
        // ordinary way could be re-keyed while this one held it open and read on through the
        // rewrite.
        SEDatabase wrapped = new SEDatabase(openPlain(dbFile));
        try {
            boolean refused = false;
            try {
                db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException expected) {
                refused = true;
            }
            assertTrue(refused, "the other open handle was seen");
        } finally {
            wrapped.close();
        }

        // And once it closes, the key change goes through.
        db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
    }

    @Test
    public void aWrappedUriConnectionRegistersTheSameFile() throws Exception {
        // SQLite's URI form, which this driver accepts. Handing "file:/tmp/app.db" to File reads
        // it as a relative path under the working directory, so the wrapped handle registered a
        // key nothing else could match and the check it exists for passed while it held the file.
        Connection uriConn = java.sql.DriverManager.getConnection(
                "jdbc:sqlite:file:" + dbFile.getAbsolutePath() + "?mode=rwc");
        SEDatabase wrapped = new SEDatabase(uriConn);
        try {
            boolean refused = false;
            try {
                db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException expected) {
                refused = true;
            }
            assertTrue(refused, "the URI-form handle was seen");
        } finally {
            wrapped.close();
        }
    }

    @Test
    public void anOpaqueUriConnectionResolvesToItsFile() throws Exception {
        // "file:name.db" is opaque to java.net.URI -- its scheme-specific part does not start with
        // a slash -- so getPath() is null and the handle registered nothing. SQLite reads it as a
        // name under the working directory, and so does this.
        File local = new File("cn1-opaque-" + System.nanoTime() + ".db");
        try {
            SEDatabase owner = identified(openPlain(local), local);
            try {
                Connection uriConn = java.sql.DriverManager.getConnection(
                        "jdbc:sqlite:file:" + local.getName() + "?mode=rwc");
                SEDatabase wrapped = new SEDatabase(uriConn);
                try {
                    String message = null;
                    try {
                        owner.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
                    } catch (java.io.IOException expected) {
                        message = expected.getMessage();
                    }
                    assertNotNull(message, "an opaque-URI handle is still seen");
                    // The identity refusal, not the "could not say which file" one: this asserts
                    // the URI resolved, rather than that it fell back to blocking everything.
                    assertTrue(message.indexOf("open more than once") >= 0,
                            "refused because it is the same file, not because it is unknown: "
                            + message);
                } finally {
                    wrapped.close();
                }
            } finally {
                owner.close();
            }
        } finally {
            local.delete();
        }
    }

    @Test
    public void aConnectionWhoseFileCannotBeIdentifiedBlocksEveryRekey() throws Exception {
        // A connection that will not say what it is open on could be open on this file, and a key
        // change rewrites the file underneath whoever holds it. Not knowing has to read as yes.
        Connection unreadable = unreadableConnection();
        SEDatabase wrapped = new SEDatabase(unreadable);
        try {
            boolean refused = false;
            try {
                db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException expected) {
                refused = true;
            }
            assertTrue(refused, "a connection with no identifiable file blocks the key change");
        } finally {
            wrapped.close();
        }

        // Once it closes, the key change goes through again.
        db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
    }

    @Test
    public void anInMemoryConnectionBlocksNothing() throws Exception {
        // ":memory:" names no file, which is not the same as a file that could not be worked out.
        // There is nothing for another connection to hold and nothing for a key change to rewrite,
        // so counting it as unidentified stopped every unrelated database being deleted or
        // re-keyed for as long as it stayed open.
        Connection memory = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
        SEDatabase wrapped = new SEDatabase(memory);
        try {
            db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
        } finally {
            wrapped.close();
        }
    }

    @Test
    public void inMemoryUrlsInEverySpellingBlockNothing() throws Exception {
        // The URI spellings read as a filename produce a path, so deriving the registry key first
        // and asking whether the connection names a file afterwards registered them against a
        // file they have nothing to do with -- and then refused a delete or a key change on the
        // real database that happened to be there.
        // The URI name is deliberately the file of the database under test: that is the shape
        // that bites, because the derived key then matches the very database whose key change is
        // about to be refused. A name that collided with nothing would pass whether or not the
        // no-file answer is authoritative, which is no test at all.
        String[] urls = {
            "jdbc:sqlite::memory:",
            "jdbc:sqlite:file::memory:",
            "jdbc:sqlite:file:" + dbFile.getCanonicalPath() + "?mode=memory&cache=shared",
        };
        for (String url : urls) {
            Connection memory = java.sql.DriverManager.getConnection(url);
            SEDatabase wrapped = new SEDatabase(memory);
            try {
                db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException refused) {
                fail(url + " should not block anything, but the key change was refused: "
                        + refused.getMessage());
            } finally {
                wrapped.close();
            }
        }
    }

    @Test
    public void aFileWhoseNameStartsWithAColonIsStillAFile() throws Exception {
        // SQLite reserves the leading colon and advises against it, but ":name.db" is a file
        // called ":name.db" -- only ":memory:" itself is the in-memory database.
        //
        // Both halves are asserted, because a connection registered under no file at all refuses
        // the key change below too, and an assertion that only watched for a refusal would pass
        // either way. What separates them is the unrelated database: a connection of unknown file
        // blocks that one as well, and a connection registered against its own file does not.
        File odd = new File(":cn1-conformance-colon.db").getAbsoluteFile();
        Connection relative = java.sql.DriverManager.getConnection(
                "jdbc:sqlite::cn1-conformance-colon.db");
        SEDatabase wrapped = new SEDatabase(relative);
        SEDatabase second = null;
        try {
            // Half one: it is visible on its own file.
            second = identified(java.sql.DriverManager.getConnection(
                    "jdbc:sqlite:" + odd.getCanonicalPath()), odd);
            boolean refused = false;
            try {
                second.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException expected) {
                refused = true;
            }
            assertTrue(refused, "the connection on the colon-named file has to be visible to a "
                    + "key change on that same file");

            // Half two: and it is visible as that file rather than as a file nobody could work
            // out, so it holds nothing else up.
            try {
                db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException refusedUnrelated) {
                fail("a connection on a colon-named file must not block an unrelated database, "
                        + "but the key change was refused: " + refusedUnrelated.getMessage());
            }
        } finally {
            if (second != null) {
                second.close();
            }
            wrapped.close();
            odd.delete();
        }
    }

    @Test
    public void aUriFormConnectionRegistersUnderTheFileOnDisk() throws Exception {
        // The URI form names a real file, and resolving it is what makes the key match the one
        // the ordinary open path derives. Read as a plain filename, "file:/tmp/app.db" is a
        // relative path under the working directory, so the two spellings of one database would
        // register apart and neither would see the other. A percent escape is in the name because
        // a URI may carry them and the file on disk does not.
        File spaced = new File(dbFile.getParentFile(), "cn1 uri form.db").getAbsoluteFile();
        spaced.delete();
        Connection uri = java.sql.DriverManager.getConnection("jdbc:sqlite:file:"
                + spaced.getAbsolutePath().replace(" ", "%20"));
        SEDatabase wrapped = new SEDatabase(uri);
        SEDatabase second = null;
        try {
            second = identified(java.sql.DriverManager.getConnection(
                    "jdbc:sqlite:" + spaced.getCanonicalPath()), spaced);
            boolean refused = false;
            try {
                second.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException expected) {
                refused = true;
            }
            assertTrue(refused, "the URI-form connection has to register under the file on disk, "
                    + "so a key change on that same file sees it");
            try {
                db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
            } catch (java.io.IOException refusedUnrelated) {
                fail("a URI-form connection must not block an unrelated database, but the key "
                        + "change was refused: " + refusedUnrelated.getMessage());
            }
        } finally {
            if (second != null) {
                second.close();
            }
            wrapped.close();
            spaced.delete();
        }
    }

    /// A connection that refuses to say which URL it is open on, which is the case the registry
    /// has to treat as "could be anything".
    private static Connection unreadableConnection() {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[] {Connection.class},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method,
                            Object[] args) throws Throwable {
                        String name = method.getName();
                        if ("getMetaData".equals(name)) {
                            throw new java.sql.SQLException("this connection will not say");
                        }
                        if ("getAutoCommit".equals(name)) {
                            return Boolean.TRUE;
                        }
                        if ("isClosed".equals(name)) {
                            return Boolean.FALSE;
                        }
                        Class<?> returns = method.getReturnType();
                        if (returns == boolean.class) {
                            return Boolean.FALSE;
                        }
                        if (returns == int.class) {
                            return Integer.valueOf(0);
                        }
                        return null;
                    }
                });
    }

    private static Connection openPlain(File f) throws Exception {
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        return DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath(),
                config.toProperties());
    }

    private static Connection openEncrypted(File f, String key) throws Exception {
        Properties props = SQLiteMCSqlCipherConfig.getV4Defaults().withKey(key)
                .build().toProperties();
        return DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath(), props);
    }

    @Test
    public void statementsConformToTheContract() throws Exception {
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runStatements(db, DatabaseConformanceSuite.MODE_STRICT, r);
        r.assertClean("statements");
    }

    @Test
    public void cursorsConformToTheContract() throws Exception {
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runCursor(db, DatabaseConformanceSuite.MODE_STRICT, r);
        r.assertClean("cursor");
    }

    @Test
    public void transactionsConformToTheContract() throws Exception {
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runTransactions(db, DatabaseConformanceSuite.MODE_STRICT, r);
        r.assertClean("transactions");
    }

    /**
     * The compatibility promise is that {@code db.legacy} restores this port's own previous
     * behaviour, so the legacy expectations have to be exercised on every PR rather than only by
     * the device suites. Running the groups themselves, not a sampled subset, is what catches a
     * new strict-mode rule that forgot to check the flag: those fire in legacy mode too and this
     * goes red.
     */
    @Test
    public void statementsConformToTheLegacyContract() throws Exception {
        Database.setLegacyBehavior(true);
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runStatements(db, DatabaseConformanceSuite.MODE_LEGACY, r);
        r.assertClean("statements (legacy)");
    }

    /**
     * A port that shipped after this contract has no old behaviour, so the hint must not hold it
     * to another port's.
     *
     * Windows, Linux and the browser are new here. The legacy group ran against them anyway, and
     * one expectation was written as "every port except iOS", which quietly included them and
     * asserted the simulator's silent truncation of a multi-statement script against ports that
     * correctly run the whole thing. Both device jobs went red for behaving properly.
     *
     * What this pins is the suite's expectations, not the engine. On those ports the hint is
     * readable but nothing reads it, so their behaviour is the portable contract whether it is set
     * or not -- and the legacy group must therefore expect exactly what the strict group expects.
     * Leaving the flag off reproduces that precisely: every expectation the legacy run makes here
     * has to be the strict one, and any that is not is a legacy branch that forgot to name the
     * port it belongs to. The device suites are the authority, but they cost forty minutes; this
     * costs a second and fails on the same mistake.
     */
    @Test
    public void aPortWithNoPriorBehaviourIsNotHeldToAnothersLegacy() throws Exception {
        Database.setLegacyBehavior(false);
        DatabaseConformanceSuite.setPortKind(DatabaseConformanceSuite.PORT_OTHER);
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runStatements(db, DatabaseConformanceSuite.MODE_LEGACY, r);
        DatabaseConformanceSuite.runCursor(db, DatabaseConformanceSuite.MODE_LEGACY, r);
        DatabaseConformanceSuite.runTransactions(db, DatabaseConformanceSuite.MODE_LEGACY, r);
        r.assertClean("a port with no prior behaviour (legacy)");
    }

    @Test
    public void cursorsConformToTheLegacyContract() throws Exception {
        Database.setLegacyBehavior(true);
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runCursor(db, DatabaseConformanceSuite.MODE_LEGACY, r);
        r.assertClean("cursor (legacy)");
    }

    @Test
    public void transactionsConformToTheLegacyContract() throws Exception {
        Database.setLegacyBehavior(true);
        CollectingReporter r = new CollectingReporter();
        DatabaseConformanceSuite.runTransactions(db, DatabaseConformanceSuite.MODE_LEGACY, r);
        r.assertClean("transactions (legacy)");
    }

    /**
     * The simulator could not seek at all before: the JDBC driver only produces
     * TYPE_FORWARD_ONLY result sets, so first(), last(), prev() and position() every one threw.
     */
    @Test
    public void cursorSupportsRandomAccess() throws Exception {
        db.execute("CREATE TABLE t (id INTEGER PRIMARY KEY)");
        for (int iter = 0; iter < 4; iter++) {
            db.execute("INSERT INTO t (id) VALUES (?)", new Object[] {Integer.valueOf(iter)});
        }
        Cursor cur = db.executeQuery("SELECT id FROM t ORDER BY id");
        try {
            assertEquals(-1, cur.getPosition(), "a new cursor is before the first row");
            assertTrue(cur.last());
            assertEquals(3, cur.getPosition());
            assertEquals(3, cur.getRow().getInteger(0));
            assertTrue(cur.first());
            assertEquals(0, cur.getPosition());
            assertTrue(cur.position(2));
            assertEquals(2, cur.getRow().getInteger(0));
            assertTrue(cur.prev());
            assertEquals(1, cur.getRow().getInteger(0));
            assertFalse(cur.position(99), "seeking past the end reports failure");
            assertEquals(4, Database.count(cur), "getCount walks the result set");
        } finally {
            cur.close();
        }
    }

    /** execute(String) must run a whole script; a PreparedStatement would drop all but the first. */
    @Test
    public void executeRunsEveryStatementOfAScript() throws Exception {
        db.execute("CREATE TABLE a (x INTEGER); CREATE TABLE b (x INTEGER)");
        Cursor cur = db.executeQuery(
                "SELECT count(*) FROM sqlite_master WHERE name IN ('a','b')");
        try {
            assertTrue(cur.next());
            assertEquals(2, cur.getRow().getInteger(0));
        } finally {
            cur.close();
        }
    }

    /** Previously the tail was silently discarded, which loses data without any signal. */
    @Test
    public void parameterizedFormsRejectMultipleStatements() throws Exception {
        db.execute("CREATE TABLE t (x TEXT)");
        try {
            db.execute("INSERT INTO t VALUES (?); INSERT INTO t VALUES ('second')",
                    new String[] {"first"});
            fail("a multi-statement script must be rejected, not silently truncated");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("single SQL statement"));
        }
    }

    /** A rollback used to leave the connection outside autocommit. */
    @Test
    public void rollbackRestoresAutocommit() throws Exception {
        db.execute("CREATE TABLE t (id INTEGER PRIMARY KEY)");
        db.beginTransaction();
        db.execute("INSERT INTO t (id) VALUES (1)");
        db.rollbackTransaction();
        db.execute("INSERT INTO t (id) VALUES (2)");

        // Read through a second connection: anything still inside an open transaction on the
        // first one would be invisible here.
        Connection other = openPlain(dbFile);
        try {
            java.sql.Statement s = other.createStatement();
            java.sql.ResultSet rs = s.executeQuery("SELECT count(*) FROM t");
            rs.next();
            assertEquals(1, rs.getInt(1), "the statement after the rollback was committed");
            rs.close();
            s.close();
        } finally {
            other.close();
        }
    }

    @Test
    public void encryptedDatabaseRoundTripsAndIsNotPlaintextOnDisk() throws Exception {
        db.close();
        db = null;
        assertTrue(dbFile.delete());

        String key = "correct horse battery staple";
        SEDatabase enc = identified(openEncrypted(dbFile, key), dbFile);
        try {
            enc.execute("CREATE TABLE secret (v TEXT)");
            enc.execute("INSERT INTO secret (v) VALUES (?)", new String[] {"classified"});
        } finally {
            enc.close();
        }

        assertFalse(startsWithPlaintextHeader(dbFile),
                "the file must not begin with a plaintext SQLite header");

        SEDatabase reopened = identified(openEncrypted(dbFile, key), dbFile);
        try {
            Cursor cur = reopened.executeQuery("SELECT v FROM secret");
            assertTrue(cur.next());
            assertEquals("classified", cur.getRow().getString(0));
            cur.close();
        } finally {
            reopened.close();
        }

        try {
            openEncrypted(dbFile, "the wrong key").close();
            fail("the wrong key must not open the database");
        } catch (java.sql.SQLException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    /** encrypt() and decrypt() are built on changeKey, which re-keys the file in place. */
    @Test
    public void changeKeyEncryptsAndDecryptsInPlace() throws Exception {
        db.execute("CREATE TABLE t (v TEXT)");
        db.execute("INSERT INTO t (v) VALUES ('was plaintext')");
        db.changeKey(com.codename1.db.DatabaseConfig.passphrase("a secret"));
        db.close();
        db = null;

        assertFalse(startsWithPlaintextHeader(dbFile));

        SEDatabase enc = identified(openEncrypted(dbFile, "a secret"), dbFile);
        try {
            Cursor cur = enc.executeQuery("SELECT v FROM t");
            assertTrue(cur.next());
            assertEquals("was plaintext", cur.getRow().getString(0),
                    "existing rows survive encryption");
            cur.close();
            enc.changeKey(com.codename1.db.DatabaseConfig.plain());
        } finally {
            enc.close();
        }

        assertTrue(startsWithPlaintextHeader(dbFile),
                "decrypting restores a plaintext SQLite file");
    }

    /** The compatibility promise is only real if it is tested. */
    @Test
    public void legacyModeRestoresTheOldPositionBaseAndTruncation() throws Exception {
        Database.setLegacyBehavior(true);
        db.execute("CREATE TABLE t (id INTEGER PRIMARY KEY)");
        db.execute("INSERT INTO t (id) VALUES (1)");

        Cursor cur = db.executeQuery("SELECT id FROM t");
        try {
            assertEquals(0, cur.getPosition(), "legacy positions count from one");
            assertTrue(cur.next());
            assertEquals(1, cur.getPosition(), "legacy first row is position 1");
        } finally {
            cur.close();
        }

        // Legacy accepted a script here and quietly ran only the first statement.
        db.execute("CREATE TABLE l1 (x INTEGER); CREATE TABLE l2 (x INTEGER)");
        Cursor chk = db.executeQuery(
                "SELECT count(*) FROM sqlite_master WHERE name IN ('l1','l2')");
        try {
            chk.next();
            assertEquals(1, chk.getRow().getInteger(0),
                    "legacy execute(String) runs only the first statement");
        } finally {
            chk.close();
        }
    }

    private static boolean startsWithPlaintextHeader(File f) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            byte[] header = new byte[15];
            int read = in.read(header);
            return read == 15 && "SQLite format 3".equals(new String(header, 0, 15, "UTF-8"));
        } finally {
            in.close();
        }
    }
}
