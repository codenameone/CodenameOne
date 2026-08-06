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
package com.codename1.testing;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.db.DatabaseEncryptionException;
import com.codename1.db.Row;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;

/// The portable acceptance test for `com.codename1.db`.
///
/// One set of assertions, run unchanged on every platform. A port is conformant when this suite
/// passes against it; there is no separate, per-platform notion of correct behaviour. It lives in
/// the framework rather than in a test project so that the device suite, the simulator unit tests
/// and anyone writing a new port can all run the same checks.
///
/// Failures are reported through a `Reporter` rather than thrown, so a single run reports every
/// violation instead of stopping at the first. That matters most when bringing up a new port,
/// where the useful output is the whole list.
///
/// ```java
/// DatabaseConformanceSuite.runAll("conformance.db",
///         DatabaseConformanceSuite.MODE_STRICT, myReporter);
/// ```
///
/// #### Modes
///
/// `#MODE_STRICT` asserts the contract documented in the `com.codename1.db` package. `#MODE_LEGACY`
/// asserts that `Database#setLegacyBehavior(boolean)` really does restore what each platform used
/// to do, which is what makes the compatibility promise testable rather than aspirational. Run the
/// legacy mode only with the flag actually set.
public class DatabaseConformanceSuite {

    /// Assert the portable contract.
    public static final int MODE_STRICT = 0;

    /// Assert that legacy compatibility mode restores the previous per-platform behaviour.
    public static final int MODE_LEGACY = 1;

    private static final int PORT_OTHER = 0;
    private static final int PORT_SIMULATOR = 1;
    private static final int PORT_IOS = 2;
    private static final int PORT_ANDROID = 3;

    /// Receives the outcome of each individual check.
    public interface Reporter {

        /// Records one assertion.
        ///
        /// #### Parameters
        ///
        /// - `condition`: true when the check passed
        ///
        /// - `message`: describes what was being checked, and is shown when it fails
        void check(boolean condition, String message);

        /// Records that a group of checks was not applicable here.
        ///
        /// #### Parameters
        ///
        /// - `reason`: why the checks were skipped
        void skip(String reason);

        /// Records a diagnostic note that is not itself a pass or a failure.
        ///
        /// #### Parameters
        ///
        /// - `message`: the observation
        void info(String message);
    }

    private DatabaseConformanceSuite() {
    }

    /// Runs every group against a scratch database.
    ///
    /// The database is created and deleted by this method, so pass a name the application does not
    /// otherwise use.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: a scratch database name
    ///
    /// - `mode`: `#MODE_STRICT` or `#MODE_LEGACY`
    ///
    /// - `r`: receives the results
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the scratch database cannot be created at all
    public static void runAll(String databaseName, int mode, Reporter r) throws IOException {
        if (!isDatabaseAvailable(r)) {
            return;
        }
        runLifecycle(databaseName, mode, r);

        Database db = Database.openOrCreate(databaseName);
        try {
            runStatements(db, mode, r);
            runCursor(db, mode, r);
            runTransactions(db, mode, r);
        } finally {
            closeQuietly(db);
        }
        runEncryption(databaseName + "-enc", mode, r);
        deleteQuietly(databaseName);
    }

    /// Reports whether this platform provides a database at all.
    ///
    /// Ports without an implementation return null from `Database#openOrCreate(java.lang.String)`.
    /// Callers use this to skip cleanly rather than fail.
    ///
    /// #### Parameters
    ///
    /// - `r`: receives a skip message when there is no database
    ///
    /// #### Returns
    ///
    /// true when a database can be opened
    public static boolean isDatabaseAvailable(Reporter r) {
        Database probe = null;
        try {
            probe = Database.openOrCreate("cn1-conformance-probe.db");
            if (probe == null) {
                r.skip("no-database-implementation-on-" + Display.getInstance().getPlatformName());
                return false;
            }
            return true;
        } catch (Throwable err) {
            r.skip("database-unavailable-on-" + Display.getInstance().getPlatformName()
                    + ": " + err.getMessage());
            return false;
        } finally {
            closeQuietly(probe);
            deleteQuietly("cn1-conformance-probe.db");
        }
    }

    // ------------------------------------------------------------------ lifecycle

    /// Checks opening, existence, deletion, paths and use-after-close.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: a scratch database name
    ///
    /// - `mode`: `#MODE_STRICT` or `#MODE_LEGACY`
    ///
    /// - `r`: receives the results
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the scratch database cannot be created
    public static void runLifecycle(String databaseName, int mode, Reporter r) throws IOException {
        deleteQuietly(databaseName);

        Database db = Database.openOrCreate(databaseName);
        r.check(db != null, "openOrCreate returns a database");
        if (db == null) {
            return;
        }
        db.execute("CREATE TABLE IF NOT EXISTS lifecycle (id INTEGER PRIMARY KEY)");
        db.close();

        r.check(Database.exists(databaseName), "exists() is true after the database is created");

        String path = Database.getDatabasePath(databaseName);
        r.check(path != null, "getDatabasePath returns a path for an existing database");
        if (path != null) {
            r.check(FileSystemStorage.getInstance().exists(path),
                    "the path from getDatabasePath resolves through FileSystemStorage");
        }

        r.check(!Database.isEncrypted(databaseName),
                "isEncrypted is false for a plaintext database");

        // close() is idempotent
        Database again = Database.openOrCreate(databaseName);
        again.close();
        boolean secondCloseThrew = false;
        try {
            again.close();
        } catch (IOException err) {
            secondCloseThrew = true;
        }
        r.check(!secondCloseThrew, "close() a second time is a no-op rather than an error");

        // use-after-close
        boolean useAfterCloseThrew = false;
        try {
            again.execute("SELECT 1");
        } catch (IOException err) {
            useAfterCloseThrew = true;
        } catch (RuntimeException err) {
            useAfterCloseThrew = true;
            r.check(false, "use-after-close throws IOException, not " + err.getClass().getName());
        }
        r.check(useAfterCloseThrew, "using a closed database throws");

        // custom paths agree with the advertised capability
        if (!Database.isCustomPathSupported()) {
            boolean rejected = false;
            try {
                Database.openOrCreate("some/path/db.sqlite");
            } catch (IllegalArgumentException err) {
                rejected = true;
            } catch (IOException err) {
                rejected = false;
            }
            r.check(rejected,
                    "a path separator is rejected when custom paths are unsupported");
        } else {
            r.info("custom database paths are supported here");
        }

        Database.delete(databaseName);
        r.check(!Database.exists(databaseName), "exists() is false after delete()");
    }

    // ------------------------------------------------------------------ statements

    /// Checks statement execution, parameter binding and error reporting.
    ///
    /// #### Parameters
    ///
    /// - `db`: an open database, left usable
    ///
    /// - `mode`: `#MODE_STRICT` or `#MODE_LEGACY`
    ///
    /// - `r`: receives the results
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the scratch table cannot be created
    public static void runStatements(Database db, int mode, Reporter r) throws IOException {
        db.execute("DROP TABLE IF EXISTS conf_stmt");
        db.execute("CREATE TABLE conf_stmt (id INTEGER PRIMARY KEY, t TEXT, i INTEGER, "
                + "d REAL, b BLOB)");

        // ---- multi-statement execute
        db.execute("DROP TABLE IF EXISTS conf_m1");
        db.execute("DROP TABLE IF EXISTS conf_m2");
        boolean multiThrew = false;
        try {
            db.execute("CREATE TABLE conf_m1 (a INTEGER); CREATE TABLE conf_m2 (a INTEGER)");
        } catch (IOException err) {
            multiThrew = true;
        }
        int created = countTables(db, "conf_m1", "conf_m2");
        if (mode == MODE_LEGACY && portKind() == PORT_SIMULATOR) {
            r.check(!multiThrew && created == 1,
                    "legacy: execute(String) runs only the first statement of a script, got "
                    + created + " of 2");
        } else {
            r.check(!multiThrew && created == 2,
                    "execute(String) runs every statement of a script, got " + created + " of 2");
        }

        // ---- the parameterized forms take exactly one statement
        if (mode == MODE_STRICT) {
            boolean rejected = false;
            try {
                db.execute("INSERT INTO conf_stmt (t) VALUES (?); INSERT INTO conf_stmt (t) VALUES ('x')",
                        new String[] {"a"});
            } catch (IOException err) {
                rejected = true;
            }
            r.check(rejected,
                    "a parameterized call rejects a multi-statement script instead of dropping the tail");
        }

        // ---- typed binding
        db.execute("DELETE FROM conf_stmt");
        boolean blobWriteSupported = true;
        try {
            db.execute("INSERT INTO conf_stmt (id, t, i, d, b) VALUES (?, ?, ?, ?, ?)",
                    new Object[] {new Integer(1), "text", new Long(42), new Double(1.5),
                        new byte[] {1, 2, 3}});
        } catch (IOException err) {
            blobWriteSupported = false;
            r.check(false, "execute() binds a byte[] parameter: " + err.getMessage());
        }

        if (blobWriteSupported) {
            Cursor cur = db.executeQuery("SELECT typeof(t), typeof(i), typeof(d), typeof(b) "
                    + "FROM conf_stmt WHERE id = 1");
            try {
                r.check(cur.next(), "the typed row was inserted");
                Row row = cur.getRow();
                String tType = row.getString(0);
                String iType = row.getString(1);
                String dType = row.getString(2);
                String bType = row.getString(3);
                if (mode == MODE_LEGACY && portKind() == PORT_IOS) {
                    r.check("text".equals(iType),
                            "legacy: parameters are bound as text, typeof(i) was " + iType);
                } else {
                    r.check("text".equals(tType), "a String binds as TEXT, got " + tType);
                    r.check("integer".equals(iType), "a Long binds as INTEGER, got " + iType);
                    r.check("real".equals(dType), "a Double binds as REAL, got " + dType);
                    r.check("blob".equals(bType), "a byte[] binds as BLOB, got " + bType);
                }
            } finally {
                closeQuietly(cur);
            }

            // ---- blob round trip
            Cursor blobCur = db.executeQuery("SELECT b FROM conf_stmt WHERE id = 1");
            try {
                blobCur.next();
                byte[] read = blobCur.getRow().getBlob(0);
                boolean same = read != null && read.length == 3
                        && read[0] == 1 && read[1] == 2 && read[2] == 3;
                r.check(same, "a blob round trips through getBlob");
            } finally {
                closeQuietly(blobCur);
            }
        }

        // ---- null handling
        db.execute("DELETE FROM conf_stmt");
        boolean nullStringArrayOk = true;
        try {
            db.execute("INSERT INTO conf_stmt (id, t) VALUES (2, ?)", new String[] {null});
        } catch (IOException err) {
            nullStringArrayOk = false;
        }
        r.check(nullStringArrayOk, "a null element in a String[] binds SQL NULL rather than failing");
        if (nullStringArrayOk) {
            Cursor cur = db.executeQuery("SELECT t FROM conf_stmt WHERE id = 2");
            try {
                cur.next();
                r.check(cur.getRow().getString(0) == null, "a bound null reads back as null");
            } finally {
                closeQuietly(cur);
            }
        }

        // ---- a null parameter array is the same as no parameters
        boolean nullArrayOk = true;
        try {
            db.execute("DELETE FROM conf_stmt", (Object[]) null);
        } catch (Throwable err) {
            nullArrayOk = false;
        }
        r.check(nullArrayOk, "execute(sql, (Object[]) null) behaves like the no-parameter form");

        // ---- malformed SQL is reported from executeQuery, not from the first next()
        if (mode == MODE_STRICT) {
            boolean threwAtQuery = false;
            Cursor bad = null;
            try {
                bad = db.executeQuery("SELECT nonexistent_column_xyz FROM conf_stmt");
            } catch (IOException err) {
                threwAtQuery = true;
            } finally {
                closeQuietly(bad);
            }
            r.check(threwAtQuery, "executeQuery reports malformed SQL rather than deferring to next()");
        }

        // ---- errors are IOException with a message
        boolean properError = false;
        try {
            db.execute("THIS IS NOT SQL");
        } catch (IOException err) {
            properError = err.getMessage() != null && err.getMessage().length() > 0;
        } catch (RuntimeException err) {
            r.check(false, "a SQL error raises IOException, not " + err.getClass().getName());
        }
        r.check(properError, "a SQL error raises IOException carrying a message");

        // ---- blob query parameters, both directions of the advertised capability
        if (blobWriteSupported) {
            db.execute("DELETE FROM conf_stmt");
            db.execute("INSERT INTO conf_stmt (id, b) VALUES (?, ?)",
                    new Object[] {new Integer(3), new byte[] {9, 8, 7}});
            boolean queried = false;
            boolean threw = false;
            Cursor cur = null;
            try {
                cur = db.executeQuery("SELECT id FROM conf_stmt WHERE b = ?",
                        new Object[] {new byte[] {9, 8, 7}});
                queried = cur.next();
            } catch (IOException err) {
                threw = true;
            } finally {
                closeQuietly(cur);
            }
            Boolean advertised = blobQueryParametersSupported();
            if (Boolean.TRUE.equals(advertised)) {
                r.check(queried && !threw,
                        "isBlobQueryParameterSupported() is true, so a blob query parameter works");
            } else if (Boolean.FALSE.equals(advertised)) {
                r.check(threw,
                        "isBlobQueryParameterSupported() is false, so a blob query parameter "
                        + "raises a clean IOException rather than silently misbehaving");
            } else {
                // The capability could not be read. The point of the check is to catch silent
                // wrongness, so either a working query or a clean failure is acceptable; what is
                // not acceptable is returning no row while claiming success.
                r.check(threw || queried,
                        "a blob query parameter either works or fails cleanly, never silently "
                        + "matching nothing");
            }
        }

        db.execute("DROP TABLE IF EXISTS conf_m1");
        db.execute("DROP TABLE IF EXISTS conf_m2");
    }

    // ------------------------------------------------------------------ cursor

    /// Checks cursor navigation, positions, metadata and null reads.
    ///
    /// #### Parameters
    ///
    /// - `db`: an open database, left usable
    ///
    /// - `mode`: `#MODE_STRICT` or `#MODE_LEGACY`
    ///
    /// - `r`: receives the results
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the scratch table cannot be created
    public static void runCursor(Database db, int mode, Reporter r) throws IOException {
        db.execute("DROP TABLE IF EXISTS conf_cur");
        db.execute("CREATE TABLE conf_cur (id INTEGER PRIMARY KEY, name TEXT)");
        for (int iter = 0; iter < 5; iter++) {
            db.execute("INSERT INTO conf_cur (id, name) VALUES (?, ?)",
                    new Object[] {new Integer(iter), "row" + iter});
        }

        // ---- metadata is available before the first next()
        Cursor cur = db.executeQuery("SELECT id, name AS alias_name FROM conf_cur ORDER BY id");
        try {
            r.check(cur.getColumnCount() == 2, "getColumnCount before the first next()");
            r.check(cur.getColumnIndex("alias_name") == 1,
                    "getColumnIndex resolves the result set label of an aliased column");
            r.check(cur.getColumnIndex("ALIAS_NAME") == 1, "getColumnIndex is case-insensitive");
            r.check(cur.getColumnIndex("no_such_column") == -1,
                    "getColumnIndex returns -1 for an unknown column");
            r.check("alias_name".equalsIgnoreCase(cur.getColumnName(1)),
                    "getColumnName reports the result set label, got " + cur.getColumnName(1));
        } finally {
            closeQuietly(cur);
        }

        // ---- positions
        cur = db.executeQuery("SELECT id, name FROM conf_cur ORDER BY id");
        try {
            int base = (mode == MODE_LEGACY && portKind() == PORT_SIMULATOR) ? 1 : 0;
            if (base == 1) {
                r.info("legacy: the simulator reports positions counted from one");
            }
            r.check(cur.getPosition() == -1 + base,
                    "a new cursor sits before the first row, position " + cur.getPosition());
            r.check(cur.next(), "next() reaches the first row");
            r.check(cur.getPosition() == base, "the first row is at position " + base);

            if (mode == MODE_LEGACY && portKind() == PORT_IOS) {
                r.check(cur.first(), "legacy: first() reports success");
                r.info("legacy: first() rewinds without landing on a row on iOS");
            } else {
                r.check(cur.first(), "first() lands on the first row");
                r.check(cur.getPosition() == base, "first() leaves position at " + base);
                r.check(cur.next() && cur.getPosition() == 1 + base, "next() advances to row 1");
                r.check(cur.prev() && cur.getPosition() == base, "prev() returns to row 0");
                r.check(cur.last(), "last() lands on the last row");
                r.check(cur.getPosition() == 4 + base, "last() leaves position at " + (4 + base));
                r.check(cur.position(2) && cur.getPosition() == 2 + base, "position(2) seeks to row 2");
                r.check("row2".equals(cur.getRow().getString(1)),
                        "position(2) really is the third row");
                r.check(!cur.position(99), "position() beyond the end returns false");
                r.check(!cur.position(-1), "position(-1) returns false");
                r.check(cur.getPosition() == -1 + base, "position(-1) rewinds to before the first row");
            }
        } finally {
            closeQuietly(cur);
        }

        // ---- an empty result set
        cur = db.executeQuery("SELECT id FROM conf_cur WHERE id = 12345");
        try {
            if (mode == MODE_LEGACY && portKind() == PORT_IOS) {
                r.info("legacy: first() reports success on an empty result set on iOS");
            } else {
                r.check(!cur.first(), "first() is false for an empty result set");
                r.check(!cur.next(), "next() is false for an empty result set");
                r.check(!cur.last(), "last() is false for an empty result set");
            }
        } finally {
            closeQuietly(cur);
        }

        // ---- getRow off a row
        if (mode == MODE_STRICT) {
            cur = db.executeQuery("SELECT id FROM conf_cur ORDER BY id");
            try {
                boolean threw = false;
                try {
                    cur.getRow();
                } catch (IOException err) {
                    threw = true;
                }
                r.check(threw, "getRow() before the first row throws");
            } finally {
                closeQuietly(cur);
            }
        }

        // ---- nulls and wasNull
        db.execute("DROP TABLE IF EXISTS conf_null");
        db.execute("CREATE TABLE conf_null (a TEXT, b INTEGER)");
        db.execute("INSERT INTO conf_null (a, b) VALUES (NULL, 0)");
        cur = db.executeQuery("SELECT a, b FROM conf_null");
        try {
            Row row;
            if (mode == MODE_LEGACY && (portKind() == PORT_IOS || portKind() == PORT_ANDROID)) {
                cur.next();
                row = cur.getRow();
                r.check(Database.wasNull(row),
                        "legacy: wasNull() reports true before any value has been read");
            } else {
                cur.next();
                row = cur.getRow();
                if (Database.supportsWasNull(row)) {
                    r.check(!Database.wasNull(row), "wasNull() is false before any value is read");
                }
            }
            r.check(row.getString(0) == null, "a SQL NULL reads back as null from getString");
            if (Database.supportsWasNull(row)) {
                r.check(Database.wasNull(row), "wasNull() is true after reading a NULL");
                r.check(row.getInteger(1) == 0, "a stored zero reads back as 0");
                r.check(!Database.wasNull(row), "wasNull() is false after reading a stored zero");
            } else {
                r.check(false, "every port implements RowExt");
            }
        } finally {
            closeQuietly(cur);
        }

        // ---- cursor close is idempotent, and a closed cursor rejects use
        cur = db.executeQuery("SELECT id FROM conf_cur");
        cur.close();
        boolean secondCloseThrew = false;
        try {
            cur.close();
        } catch (IOException err) {
            secondCloseThrew = true;
        }
        r.check(!secondCloseThrew, "closing a cursor twice is a no-op");
        boolean useThrew = false;
        try {
            cur.next();
        } catch (IOException err) {
            useThrew = true;
        }
        r.check(useThrew, "using a closed cursor throws");

        db.execute("DROP TABLE IF EXISTS conf_null");
        db.execute("DROP TABLE IF EXISTS conf_cur");
    }

    // ------------------------------------------------------------------ transactions

    /// Checks commit, rollback and the flat transaction rules.
    ///
    /// #### Parameters
    ///
    /// - `db`: an open database, left usable
    ///
    /// - `mode`: `#MODE_STRICT` or `#MODE_LEGACY`
    ///
    /// - `r`: receives the results
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the scratch table cannot be created
    public static void runTransactions(Database db, int mode, Reporter r) throws IOException {
        db.execute("DROP TABLE IF EXISTS conf_tx");
        db.execute("CREATE TABLE conf_tx (id INTEGER PRIMARY KEY)");

        // ---- commit persists
        db.beginTransaction();
        db.execute("INSERT INTO conf_tx (id) VALUES (1)");
        db.commitTransaction();
        r.check(rowCount(db, "conf_tx") == 1, "a committed insert persists");

        // ---- rollback discards
        db.beginTransaction();
        db.execute("INSERT INTO conf_tx (id) VALUES (2)");
        db.rollbackTransaction();
        r.check(rowCount(db, "conf_tx") == 1, "a rolled back insert is discarded");

        // ---- autocommit is restored after a rollback
        db.execute("INSERT INTO conf_tx (id) VALUES (3)");
        r.check(rowCount(db, "conf_tx") == 2,
                "a statement after a rollback autocommits rather than joining an open transaction");

        if (mode == MODE_STRICT) {
            // ---- nesting is rejected
            db.beginTransaction();
            boolean nestedThrew = false;
            try {
                db.beginTransaction();
            } catch (IOException err) {
                nestedThrew = true;
            }
            r.check(nestedThrew, "a nested beginTransaction() throws");
            db.rollbackTransaction();

            // ---- an orphan commit or rollback is rejected
            boolean orphanCommitThrew = false;
            try {
                db.commitTransaction();
            } catch (IOException err) {
                orphanCommitThrew = true;
            }
            r.check(orphanCommitThrew, "commitTransaction() with no transaction open throws");

            boolean orphanRollbackThrew = false;
            try {
                db.rollbackTransaction();
            } catch (IOException err) {
                orphanRollbackThrew = true;
            }
            r.check(orphanRollbackThrew, "rollbackTransaction() with no transaction open throws");
        } else if (portKind() == PORT_ANDROID) {
            db.beginTransaction();
            boolean nestedThrew = false;
            try {
                db.beginTransaction();
            } catch (IOException err) {
                nestedThrew = true;
            }
            r.check(!nestedThrew, "legacy: a nested beginTransaction() is accepted on Android");
            db.rollbackTransaction();
            db.rollbackTransaction();
        }

        db.execute("DROP TABLE IF EXISTS conf_tx");
    }

    // ------------------------------------------------------------------ encryption

    /// Checks encrypted opens, the wrong-key path and the on-disk result.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: a scratch database name, created and deleted here
    ///
    /// - `mode`: `#MODE_STRICT` or `#MODE_LEGACY`
    ///
    /// - `r`: receives the results
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the scratch database cannot be created
    public static void runEncryption(String databaseName, int mode, Reporter r) throws IOException {
        if (!Database.isEncryptionSupported()) {
            // Asking for encryption where it is unavailable must fail loudly rather than
            // quietly handing back a plaintext database, so check that even here.
            boolean refused = false;
            Database db = null;
            try {
                db = Database.openOrCreate(databaseName, DatabaseConfig.passphrase("secret"));
            } catch (DatabaseEncryptionException err) {
                refused = err.getErrorCode() == DatabaseEncryptionException.NOT_SUPPORTED;
            } catch (IOException err) {
                refused = false;
            } finally {
                closeQuietly(db);
            }
            r.check(refused, "requesting encryption where it is unsupported raises NOT_SUPPORTED "
                    + "rather than returning a plaintext database");
            r.skip("encryption-unsupported-on-" + Display.getInstance().getPlatformName());
            return;
        }

        deleteQuietly(databaseName);
        String passphrase = "correct horse battery staple";

        // ---- round trip
        Database db = Database.openOrCreate(databaseName, DatabaseConfig.passphrase(passphrase));
        try {
            db.execute("CREATE TABLE secret (id INTEGER PRIMARY KEY, v TEXT)");
            db.execute("INSERT INTO secret (id, v) VALUES (1, 'classified')");
        } finally {
            closeQuietly(db);
        }

        db = Database.openOrCreate(databaseName, DatabaseConfig.passphrase(passphrase));
        Cursor cur = null;
        try {
            cur = db.executeQuery("SELECT v FROM secret WHERE id = 1");
            r.check(cur.next() && "classified".equals(cur.getRow().getString(0)),
                    "an encrypted database round trips with the correct passphrase");
        } finally {
            closeQuietly(cur);
            closeQuietly(db);
        }

        // ---- the single most valuable assertion here: the bytes on disk are not plaintext
        r.check(!startsWithPlaintextHeader(databaseName),
                "the encrypted database does not begin with a plaintext SQLite header");
        r.check(Database.isEncrypted(databaseName), "isEncrypted reports true for it");

        // ---- the wrong passphrase is rejected
        boolean wrongKeyRejected = false;
        Database bad = null;
        try {
            bad = Database.openOrCreate(databaseName, DatabaseConfig.passphrase("wrong"));
            Cursor probe = bad.executeQuery("SELECT v FROM secret");
            probe.next();
            probe.close();
        } catch (DatabaseEncryptionException err) {
            wrongKeyRejected = err.getErrorCode() == DatabaseEncryptionException.WRONG_KEY;
            if (!wrongKeyRejected) {
                r.info("a wrong passphrase reported code " + err.getErrorCode()
                        + " rather than WRONG_KEY");
            }
        } catch (IOException err) {
            r.info("a wrong passphrase raised a plain IOException: " + err.getMessage());
        } finally {
            closeQuietly(bad);
        }
        r.check(wrongKeyRejected, "the wrong passphrase raises WRONG_KEY");

        // ---- re-keying
        db = Database.openOrCreate(databaseName, DatabaseConfig.passphrase(passphrase));
        try {
            db.changeKey(DatabaseConfig.passphrase("a different secret"));
        } finally {
            closeQuietly(db);
        }
        db = Database.openOrCreate(databaseName, DatabaseConfig.passphrase("a different secret"));
        cur = null;
        try {
            cur = db.executeQuery("SELECT v FROM secret WHERE id = 1");
            r.check(cur.next() && "classified".equals(cur.getRow().getString(0)),
                    "the database opens with the new passphrase after changeKey");
        } finally {
            closeQuietly(cur);
            closeQuietly(db);
        }
        boolean oldKeyRejected = false;
        Database old = null;
        try {
            old = Database.openOrCreate(databaseName, DatabaseConfig.passphrase(passphrase));
            Cursor probe = old.executeQuery("SELECT v FROM secret");
            probe.next();
            probe.close();
        } catch (IOException err) {
            oldKeyRejected = true;
        } finally {
            closeQuietly(old);
        }
        r.check(oldKeyRejected, "the previous passphrase no longer opens the database");
        deleteQuietly(databaseName);

        // ---- managed keys
        String managedName = databaseName + "-managed";
        deleteQuietly(managedName);
        Database managed = null;
        try {
            managed = Database.openOrCreate(managedName, DatabaseConfig.managed());
            managed.execute("CREATE TABLE m (id INTEGER PRIMARY KEY)");
            managed.execute("INSERT INTO m (id) VALUES (1)");
            managed.close();
            managed = Database.openOrCreate(managedName, DatabaseConfig.managed());
            r.check(rowCount(managed, "m") == 1,
                    "a managed-key database reopens with the same generated key");
            r.info("managed keys are hardware backed here: "
                    + DatabaseConfig.managed().isKeyHardwareBacked());
        } catch (DatabaseEncryptionException err) {
            if (err.getErrorCode() == DatabaseEncryptionException.KEY_UNAVAILABLE) {
                r.skip("managed-keys-unavailable: " + err.getMessage());
            } else {
                r.check(false, "managed key open failed: " + err.getMessage());
            }
        } finally {
            closeQuietly(managed);
        }
        Database.forgetManagedKey(managedName);
        deleteQuietly(managedName);

        // ---- converting an existing plaintext database
        String migrateName = databaseName + "-migrate";
        deleteQuietly(migrateName);
        Database plain = Database.openOrCreate(migrateName);
        try {
            plain.execute("CREATE TABLE p (id INTEGER PRIMARY KEY, v TEXT)");
            plain.execute("INSERT INTO p (id, v) VALUES (1, 'was plaintext')");
        } finally {
            closeQuietly(plain);
        }
        Database.encrypt(migrateName, DatabaseConfig.passphrase(passphrase));
        r.check(!startsWithPlaintextHeader(migrateName),
                "encrypt() leaves ciphertext on disk");
        Database migrated = Database.openOrCreate(migrateName,
                DatabaseConfig.passphrase(passphrase));
        cur = null;
        try {
            cur = migrated.executeQuery("SELECT v FROM p WHERE id = 1");
            r.check(cur.next() && "was plaintext".equals(cur.getRow().getString(0)),
                    "encrypt() preserves the existing rows");
        } finally {
            closeQuietly(cur);
            closeQuietly(migrated);
        }
        Database.decrypt(migrateName, DatabaseConfig.passphrase(passphrase));
        r.check(startsWithPlaintextHeader(migrateName),
                "decrypt() restores a plaintext SQLite file");
        deleteQuietly(migrateName);
    }

    // ------------------------------------------------------------------ helpers

    /// Identifies the running port, so the legacy checks can assert what that port used to do.
    ///
    /// The simulator is detected first and separately: under a device skin it reports the
    /// *simulated* platform name, so the name alone cannot tell a simulator run from a device.
    ///
    /// Tolerates a display that is not up, which is how the groups taking a `Database` directly
    /// can be driven from a plain unit test.
    private static int portKind() {
        try {
            if (Display.getInstance().isSimulator()) {
                return PORT_SIMULATOR;
            }
            String platform = Display.getInstance().getPlatformName();
            if ("ios".equals(platform)) {
                return PORT_IOS;
            }
            if ("and".equals(platform)) {
                return PORT_ANDROID;
            }
        } catch (Throwable displayNotUp) {
            return PORT_OTHER;
        }
        return PORT_OTHER;
    }

    /// Reads the blob-query capability, or null when it cannot be determined.
    ///
    /// Returns null rather than a guess when the display is not up, so that a harness driving a
    /// `Database` directly does not assert the opposite of what the port actually supports.
    private static Boolean blobQueryParametersSupported() {
        try {
            return Database.isBlobQueryParameterSupported() ? Boolean.TRUE : Boolean.FALSE;
        } catch (Throwable displayNotUp) {
            return null;
        }
    }

    private static boolean startsWithPlaintextHeader(String databaseName) {
        String path = Database.getDatabasePath(databaseName);
        if (path == null) {
            return false;
        }
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(path);
            byte[] header = new byte[15];
            int offset = 0;
            while (offset < header.length) {
                int read = in.read(header, offset, header.length - offset);
                if (read < 0) {
                    return false;
                }
                offset += read;
            }
            return "SQLite format 3".equals(new String(header, 0, 15));
        } catch (IOException err) {
            return false;
        } finally {
            Util.cleanup(in);
        }
    }

    private static int countTables(Database db, String first, String second) throws IOException {
        Cursor cur = db.executeQuery("SELECT count(*) FROM sqlite_master WHERE name IN (?, ?)",
                new String[] {first, second});
        try {
            cur.next();
            return cur.getRow().getInteger(0);
        } finally {
            closeQuietly(cur);
        }
    }

    private static int rowCount(Database db, String table) throws IOException {
        Cursor cur = db.executeQuery("SELECT count(*) FROM " + table);
        try {
            cur.next();
            return cur.getRow().getInteger(0);
        } finally {
            closeQuietly(cur);
        }
    }

    private static void closeQuietly(Database db) {
        if (db != null) {
            try {
                db.close();
            } catch (Throwable ignored) {
                // A close failure must not mask the assertion that was actually being made.
            }
        }
    }

    private static void closeQuietly(Cursor cur) {
        if (cur != null) {
            try {
                cur.close();
            } catch (Throwable ignored) {
                // As above.
            }
        }
    }

    private static void deleteQuietly(String databaseName) {
        try {
            if (Database.exists(databaseName)) {
                Database.delete(databaseName);
            }
        } catch (Throwable ignored) {
            // Best effort cleanup between groups.
        }
    }
}
