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
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.OutputStream;
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
public final class DatabaseConformanceSuite {

    /// Assert the portable contract.
    public static final int MODE_STRICT = 0;

    /// Assert that legacy compatibility mode restores the previous per-platform behaviour.
    public static final int MODE_LEGACY = 1;

    /// A port with no legacy behaviour of its own to restore.
    public static final int PORT_OTHER = 0;

    /// The simulator, whose `getPosition()` used to be 1-based.
    public static final int PORT_SIMULATOR = 1;

    /// The iOS port, which has the most legacy behaviour to restore.
    public static final int PORT_IOS = 2;

    /// The Android port.
    public static final int PORT_ANDROID = 3;

    /// Not overridden; the port is read from the running `Display`.
    public static final int PORT_AUTODETECT = -1;

    /// Set by `#setPortKind(int)`.
    ///
    /// The legacy expectations are per-port, because the behaviour being restored genuinely was.
    /// Normally the port is read from `Display`, but a harness driving a `Database` directly --
    /// the fast simulator gate on every PR, for instance -- has no `Display` up, so autodetection
    /// reports `#PORT_OTHER` and the legacy groups would assert the portable contract instead of
    /// that port's own previous behaviour. Such a harness declares what it is testing.
    private static int portKindOverride = PORT_AUTODETECT;

    /// Declares which port the suite is being run against, for a harness with no `Display`.
    ///
    /// Pass `#PORT_AUTODETECT` to go back to reading it from `Display`, which is what an
    /// on-device harness wants. This is global state, so a harness that sets it should reset it
    /// afterwards.
    ///
    /// #### Parameters
    ///
    /// - `portKind`: one of the `PORT_` constants
    public static void setPortKind(int portKind) {
        portKindOverride = portKind;
    }

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
        noteDatabase(db);
        db.execute("CREATE TABLE IF NOT EXISTS lifecycle (id INTEGER PRIMARY KEY)");
        db.close();

        r.check(Database.exists(databaseName), "exists() is true after the database is created");

        String path = Database.getDatabasePath(databaseName);
        r.check(path != null, "getDatabasePath returns a path for an existing database");
        if (path != null && Database.isCustomPathSupported()) {
            // Only meaningful where databases live on a filesystem. A platform that reports no
            // custom-path support is saying its databases are keyed by name inside a storage
            // pool, and there the returned value is an opaque handle rather than something
            // FileSystemStorage could open - the JavaScript port is the case in point.
            r.check(FileSystemStorage.getInstance().exists(path),
                    "the path from getDatabasePath resolves through FileSystemStorage");
        } else if (path != null) {
            r.info("databases are not filesystem backed here, so the path is an opaque handle");
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

        // ---- deleting a database somebody still holds is refused
        // Every platform here unlinks an open file quite happily, and the handle keeps working on
        // a file with no name: reopening the name makes a different database, and whatever the
        // old handle writes goes away with it. Refusing is the only answer that does not lose
        // writes silently.
        Database stillOpen = Database.openOrCreate(databaseName);
        boolean refusedWhileOpen = false;
        try {
            Database.delete(databaseName);
        } catch (IOException expected) {
            refusedWhileOpen = true;
        }
        r.check(refusedWhileOpen, "delete() is refused while a connection to the database is open");
        stillOpen.close();
        r.check(Database.exists(databaseName), "and the refused delete left the database alone");

        // ---- delete takes the working files with it
        // A database is not one file. Rows committed in WAL mode sit in -wal until a checkpoint
        // moves them, and a crash or a kill leaves that file behind holding them; deleting the
        // database file alone reports a deletion that did not happen, and reopening the same name
        // reads them back through the leftover. For an encrypted database they are as readable as
        // the pages they came from.
        //
        // The leftover is written here rather than produced by crashing, which a test cannot do:
        // a clean close checkpoints and removes the real one, so waiting for SQLite to leave one
        // would assert nothing. What is under test is that delete() looks for it.
        String sidecarOwner = Database.getDatabasePath(databaseName);
        if (sidecarOwner != null && Database.isCustomPathSupported()) {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String leftover = sidecarOwner + "-wal";
            OutputStream out = null;
            try {
                out = fs.openOutputStream(leftover);
                out.write(new byte[] {'r', 'o', 'w', 's'});
            } catch (IOException cannotWriteIt) {
                r.info("could not stage a leftover working file here: "
                        + cannotWriteIt.getMessage());
            } finally {
                closeQuietly(out);
            }
            if (fs.exists(leftover)) {
                Database.delete(databaseName);
                r.check(!fs.exists(leftover),
                        "delete() removes the working files beside the database, not just the "
                        + "database");
            }
        }

        // ---- an attached database counts as open
        // ATTACH makes a second database part of this connection, and SQLite holds it until the
        // connection closes or it is detached. Only the connection's own file was registered, so
        // a delete of the attached one saw nothing holding it and unlinked it underneath SQLite:
        // writes through that schema then go to a file with no name and are lost at close, while
        // reopening the name makes a fresh empty database.
        if (Database.isCustomPathSupported()) {
            Database db2 = Database.openOrCreate(databaseName);
            String attachedName = null;
            try {
                String base = Database.getDatabasePath(databaseName);
                if (base != null) {
                    attachedName = "file://" + base + ".attached";
                    Database attached = Database.openOrCreate(attachedName);
                    attached.close();
                    db2.execute("ATTACH DATABASE '" + base + ".attached' AS cn1attached");
                    boolean refused = false;
                    try {
                        Database.delete(attachedName);
                    } catch (IOException expected) {
                        refused = true;
                    }
                    r.check(refused, "deleting a database another connection has attached is "
                            + "refused");
                    db2.execute("DETACH DATABASE cn1attached");
                    // And given back on detach, rather than held for the life of the process.
                    boolean deleted = true;
                    try {
                        Database.delete(attachedName);
                    } catch (IOException stillRefused) {
                        deleted = false;
                    }
                    r.check(deleted, "and deletable again once it has been detached");
                }
            } catch (IOException attachUnsupported) {
                r.info("this engine would not attach a second database: "
                        + attachUnsupported.getMessage());
            } finally {
                closeQuietly(db2);
                if (attachedName != null && Database.exists(attachedName)) {
                    try {
                        Database.delete(attachedName);
                    } catch (IOException leftBehind) {
                        r.info("the attached database could not be cleaned up: "
                                + leftBehind.getMessage());
                    }
                }
            }
        }

        if (Database.exists(databaseName)) {
            Database.delete(databaseName);
        }
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
        noteDatabase(db);
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
        // The compatibility hint restores what a port used to do, so it has something to restore
        // only where the port shipped before this contract. Naming those ports rather than
        // excluding iOS is the difference: Windows, Linux and the browser are new here, never ran
        // a script any other way, and asserting the simulator's old truncation against them fails
        // a port that is behaving correctly.
        if (mode == MODE_LEGACY && portKind() == PORT_SIMULATOR) {
            // The simulator ran the first statement through a PreparedStatement and dropped the
            // rest, without saying so.
            r.check(!multiThrew && created == 1,
                    "legacy: execute(String) runs only the first statement of a script, got "
                    + created + " of 2");
        } else if (mode == MODE_LEGACY && portKind() == PORT_ANDROID) {
            // Android handed the whole script to execSQL, which takes a single statement. Whether
            // it refuses the tail by throwing or by ignoring it is the engine's business and not
            // something the hint can promise, so what is asserted is the promise itself: the
            // script does not run whole.
            r.check(multiThrew || created < 2,
                    "legacy: execute(String) does not run a whole script on Android, threw="
                    + multiThrew + " created=" + created + " of 2");
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

        // ---- a long past the range a double can hold
        //
        // SQLite stores 64-bit integers and the API hands back a Java long, so the whole range has
        // to survive the round trip. A port that carries the value through a JavaScript number, or
        // through any other 53-bit-mantissa float, loses the low bits and returns a value that is
        // close enough to look right in a test that only checks a small number. Two neighbours are
        // used because rounding maps them to the same double: if that is what happens, one of the
        // two comes back as the other.
        db.execute("DELETE FROM conf_stmt");
        long big = 9007199254740993L;
        long biggerStill = 9223372036854775807L;
        db.execute("INSERT INTO conf_stmt (id, i) VALUES (?, ?)",
                new Object[] {Integer.valueOf(90), Long.valueOf(big)});
        db.execute("INSERT INTO conf_stmt (id, i) VALUES (?, ?)",
                new Object[] {Integer.valueOf(91), Long.valueOf(biggerStill)});
        Cursor wide = null;
        try {
            wide = db.executeQuery("SELECT i FROM conf_stmt WHERE id = 90");
            r.check(wide.next(), "the wide-integer row is there");
            long readBack = wide.getRow().getLong(0);
            r.check(readBack == big, "a long past 2^53 round trips exactly, got " + readBack);
        } finally {
            closeQuietly(wide);
        }
        wide = null;
        try {
            wide = db.executeQuery("SELECT i FROM conf_stmt WHERE id = 91");
            r.check(wide.next(), "the Long.MAX_VALUE row is there");
            long readBack = wide.getRow().getLong(0);
            r.check(readBack == biggerStill,
                    "Long.MAX_VALUE round trips exactly, got " + readBack);
        } finally {
            closeQuietly(wide);
        }

        // ---- typed binding
        db.execute("DELETE FROM conf_stmt");
        // Under the legacy hint iOS stringifies every parameter, and a byte[] has no text form, so
        // the blob column is left out of the insert there. That is the behaviour the hint restores,
        // not a shortcoming: the check below still confirms the stringification itself.
        boolean stringifiesParameters = mode == MODE_LEGACY && portKind() == PORT_IOS;
        boolean blobWriteSupported = !stringifiesParameters;
        boolean typedRowInserted = true;
        try {
            if (stringifiesParameters) {
                db.execute("INSERT INTO conf_stmt (id, t, i, d) VALUES (?, ?, ?, ?)",
                        new Object[] {Integer.valueOf(1), "text", Long.valueOf(42),
                            Double.valueOf(1.5)});
            } else {
                db.execute("INSERT INTO conf_stmt (id, t, i, d, b) VALUES (?, ?, ?, ?, ?)",
                        new Object[] {Integer.valueOf(1), "text", Long.valueOf(42),
                            Double.valueOf(1.5), new byte[] {1, 2, 3}});
            }
        } catch (IOException err) {
            blobWriteSupported = false;
            typedRowInserted = false;
            r.check(false, "execute() binds a parameter of each type: " + err.getMessage());
        }

        if (typedRowInserted) {
            // typeof() over the parameter itself rather than over a stored column. A column
            // carries an affinity, and SQLite converts an inserted value to it -- text "42" lands
            // in an INTEGER column as an integer -- so reading the type back from the table says
            // what the column is, not what was bound. This is the only form that distinguishes
            // typed binding from stringified binding at all.
            Cursor cur = stringifiesParameters
                    ? db.executeQuery("SELECT typeof(?), typeof(?), typeof(?)",
                            new Object[] {"text", Long.valueOf(42), Double.valueOf(1.5)})
                    : db.executeQuery("SELECT typeof(?), typeof(?), typeof(?), typeof(?)",
                            new Object[] {"text", Long.valueOf(42), Double.valueOf(1.5),
                                new byte[] {1, 2, 3}});
            try {
                r.check(cur.next(), "typeof() over the bound parameters returns a row");
                Row row = cur.getRow();
                String tType = row.getString(0);
                String iType = row.getString(1);
                String dType = row.getString(2);
                if (stringifiesParameters) {
                    r.check("text".equals(iType),
                            "legacy: parameters are bound as text, typeof of a Long was " + iType);
                    r.check("text".equals(dType),
                            "legacy: parameters are bound as text, typeof of a Double was "
                            + dType);
                } else {
                    String bType = row.getString(3);
                    r.check("text".equals(tType), "a String binds as TEXT, got " + tType);
                    r.check("integer".equals(iType), "a Long binds as INTEGER, got " + iType);
                    r.check("real".equals(dType), "a Double binds as REAL, got " + dType);
                    r.check("blob".equals(bType), "a byte[] binds as BLOB, got " + bType);
                }
            } finally {
                closeQuietly(cur);
            }

            // ---- blob round trip, where one was written
            if (blobWriteSupported) {
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
            } else {
                r.info("legacy: no blob was written, since every parameter is stringified here");
            }

            // ---- an empty value is empty, not null
            // The ports that bind through the C API hand SQLite a pointer and a length, and an
            // empty Java array has no storage behind it: the pointer is null, which is exactly
            // how SQL NULL is spelled to sqlite3_bind_text and sqlite3_bind_blob. So "" arrives
            // as NULL, reads back as null, and is refused outright by a NOT NULL column -- with
            // nothing to say it happened.
            db.execute("DELETE FROM conf_stmt");
            db.execute("INSERT INTO conf_stmt (id, t) VALUES (7, ?)", new Object[] {""});
            Cursor emptyCur = db.executeQuery("SELECT t, typeof(t) FROM conf_stmt WHERE id = 7");
            try {
                emptyCur.next();
                Row emptyRow = emptyCur.getRow();
                String stored = emptyRow.getString(0);
                r.check("".equals(stored), "an empty string is stored as an empty string, got "
                        + (stored == null ? "null" : "\"" + stored + "\""));
                r.check("text".equals(emptyRow.getString(1)),
                        "and it is TEXT rather than NULL, got " + emptyRow.getString(1));
            } finally {
                closeQuietly(emptyCur);
            }

            if (blobWriteSupported) {
                db.execute("DELETE FROM conf_stmt");
                db.execute("INSERT INTO conf_stmt (id, b) VALUES (8, ?)",
                        new Object[] {new byte[0]});
                Cursor emptyBlobCur =
                        db.executeQuery("SELECT b, typeof(b) FROM conf_stmt WHERE id = 8");
                try {
                    emptyBlobCur.next();
                    Row emptyBlobRow = emptyBlobCur.getRow();
                    byte[] readBack = emptyBlobRow.getBlob(0);
                    r.check(readBack != null && readBack.length == 0,
                            "an empty blob is stored as an empty blob, got "
                            + (readBack == null ? "null" : readBack.length + " bytes"));
                    r.check("blob".equals(emptyBlobRow.getString(1)),
                            "and it is BLOB rather than NULL, got " + emptyBlobRow.getString(1));
                } finally {
                    closeQuietly(emptyBlobCur);
                }
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
            db.execute("DELETE FROM conf_stmt", noParameterArray());
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

        // ---- a named or numbered placeholder is still a parameter
        // The API binds positionally, but SQLite lets a statement name its slots, and a port that
        // counts only "?" markers sees no parameters at all in ":a, :b" and lets a call through
        // with one argument -- leaving the second slot as SQL NULL, silently. The count SQLite
        // reports is the largest index it assigned, so a repeated name counts once and "?3" alone
        // counts three.
        if (mode == MODE_STRICT) {
            db.execute("DELETE FROM conf_stmt");
            boolean tooFewRejected = false;
            try {
                db.execute("INSERT INTO conf_stmt (id, t) VALUES (:id, :t)",
                        new Object[] {Integer.valueOf(7)});
            } catch (IOException expected) {
                tooFewRejected = true;
            }
            r.check(tooFewRejected, "a named placeholder counts as a parameter, so one argument "
                    + "for two of them is rejected rather than left as NULL");

            boolean bothBound = false;
            try {
                db.execute("INSERT INTO conf_stmt (id, t) VALUES (:id, :t)",
                        new Object[] {Integer.valueOf(7), "named"});
                bothBound = true;
            } catch (IOException err) {
                r.check(false, "named placeholders bind positionally: " + err.getMessage());
            }
            if (bothBound) {
                Cursor named = null;
                try {
                    named = db.executeQuery("SELECT t FROM conf_stmt WHERE id = 7");
                    r.check(named.next() && "named".equals(named.getRow().getString(0)),
                            "both named placeholders received their argument");
                } finally {
                    closeQuietly(named);
                }
            }
            db.execute("DELETE FROM conf_stmt");
        }

        // ---- text survives a round trip byte for byte
        // SQLite stores TEXT as UTF-8 and measures it in bytes, so a port that hands its engine a
        // C string agrees with it on neither: anything outside ASCII comes back one character per
        // byte, and a string holding a zero character is cut there. Both are invisible until
        // someone stores a name or a pasted document, and both differ per port, which is exactly
        // what this API is supposed to stop happening.
        db.execute("DELETE FROM conf_stmt");
        String[] texts = new String[] {
            "plain ascii",
            "caf\u00e9 na\u00efve",          // Latin-1 range, two UTF-8 bytes each
            "\u4e2d\u6587\u30c6\u30b9\u30c8",     // three UTF-8 bytes each
            "\ud83d\ude00 emoji",           // a surrogate pair, four UTF-8 bytes
            "before\u0000after",            // a zero character, which SQLite stores
        };
        for (int iter = 0; iter < texts.length; iter++) {
            db.execute("INSERT INTO conf_stmt (id, t) VALUES (?, ?)",
                    new Object[] {Integer.valueOf(100 + iter), texts[iter]});
        }
        for (int iter = 0; iter < texts.length; iter++) {
            Cursor cur = null;
            String back = null;
            try {
                cur = db.executeQuery("SELECT t FROM conf_stmt WHERE id = ?",
                        new Object[] {Integer.valueOf(100 + iter)});
                if (cur.next()) {
                    back = cur.getRow().getString(0);
                }
            } finally {
                closeQuietly(cur);
            }
            r.check(texts[iter].equals(back),
                    "text round trips unchanged: " + describeText(texts[iter])
                    + " read back as " + describeText(back));
        }
        db.execute("DELETE FROM conf_stmt");

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
                    new Object[] {Integer.valueOf(3), new byte[] {9, 8, 7}});
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
            int advertised = blobQueryParametersSupported();
            if (advertised == CAPABILITY_YES) {
                r.check(queried && !threw,
                        "isBlobQueryParameterSupported() is true, so a blob query parameter works");
            } else if (advertised == CAPABILITY_NO) {
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

        if (mode == MODE_STRICT) {
            // ---- the argument list has to match the placeholders
            //
            // Supplying too few is the dangerous direction: an engine that binds what it is given
            // and leaves the rest NULL runs a different statement than the caller wrote, silently
            // and with no error. Too many is checked as well, since it means the same confusion.
            db.execute("DROP TABLE IF EXISTS conf_args");
            db.execute("CREATE TABLE conf_args (a TEXT, b TEXT)");

            boolean tooFewThrew = false;
            try {
                db.execute("INSERT INTO conf_args (a, b) VALUES (?, ?)", new String[] {"only one"});
            } catch (IOException expected) {
                tooFewThrew = true;
            }
            r.check(tooFewThrew, "execute() with fewer arguments than placeholders throws rather "
                    + "than binding the missing ones as NULL");

            boolean tooManyThrew = false;
            try {
                db.execute("INSERT INTO conf_args (a, b) VALUES (?, ?)",
                        new String[] {"one", "two", "three"});
            } catch (IOException expected) {
                tooManyThrew = true;
            }
            r.check(tooManyThrew, "execute() with more arguments than placeholders throws");

            boolean queryTooFewThrew = false;
            Cursor argCursor = null;
            try {
                argCursor = db.executeQuery("SELECT a FROM conf_args WHERE a = ? AND b = ?",
                        new String[] {"only one"});
            } catch (IOException expected) {
                queryTooFewThrew = true;
            } finally {
                closeQuietly(argCursor);
            }
            r.check(queryTooFewThrew,
                    "executeQuery() with fewer arguments than placeholders throws");

            // A literal question mark inside a string is not a placeholder, so a port that counts
            // them by scanning the text has to skip quoted content.
            boolean literalOk = true;
            try {
                db.execute("INSERT INTO conf_args (a, b) VALUES ('is it? yes', ?)",
                        new String[] {"bound"});
            } catch (IOException err) {
                literalOk = false;
            }
            r.check(literalOk, "a question mark inside a string literal is not counted as a "
                    + "placeholder");

            db.execute("DROP TABLE IF EXISTS conf_args");
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
        noteDatabase(db);
        db.execute("DROP TABLE IF EXISTS conf_cur");
        db.execute("CREATE TABLE conf_cur (id INTEGER PRIMARY KEY, name TEXT)");
        for (int iter = 0; iter < 5; iter++) {
            db.execute("INSERT INTO conf_cur (id, name) VALUES (?, ?)",
                    new Object[] {Integer.valueOf(iter), "row" + iter});
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

        // ---- a statement that counts as writing can still be read
        // A port that protects a writing statement from being run twice has to tell "run again"
        // apart from "already here". Android reads rows through a window and refuses one outside
        // it, and a fresh cursor has no window at all, so the very first row was refused for a
        // statement that had already run -- reporting a failure for work that had happened, and
        // inviting a retry that would repeat it.
        //
        // A pragma is the shape that reaches this without needing RETURNING, which not every
        // platform's SQLite is new enough to have: most pragmas change something, so a port that
        // classifies statements at all classifies this one as writing, and journal_mode answers
        // with a row.
        Cursor pragma = null;
        try {
            pragma = db.executeQuery("PRAGMA journal_mode");
        } catch (IOException notThroughAQuery) {
            r.info("this port does not take a pragma through executeQuery: "
                    + notThroughAQuery.getMessage());
        }
        if (pragma != null) {
            try {
                r.check(pragma.next(), "the first row of a statement that counts as writing "
                        + "is readable");
                r.check(pragma.getRow().getString(0) != null,
                        "and its value can be read");
            } catch (IOException refused) {
                r.check(false, "reading the first row of a statement that counts as writing was "
                        + "refused: " + refused.getMessage());
            } finally {
                closeQuietly(pragma);
            }
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

        // ---- stepping past the end repeatedly must not move the row count
        cur = db.executeQuery("SELECT id FROM conf_cur ORDER BY id");
        try {
            int walked = 0;
            while (cur.next()) {
                walked++;
            }
            r.check(walked == 5, "walking the whole result set sees every row, saw " + walked);
            int countAfterFirstExhaustion = Database.count(cur);
            // Every one of these must stay false. SQLite resets and re-executes a statement when
            // stepped after it is done, so a port that steps again hands back the first row while
            // reporting a position past the end - the same query silently answering differently
            // depending on how many times it was walked.
            boolean steppedPastEnd = cur.next() || cur.next() || cur.next();
            r.check(!steppedPastEnd, "next() past the end keeps returning false rather than "
                    + "re-running the query");
            boolean offRowAfterEnd = true;
            try {
                cur.getRow().getString(0);
                offRowAfterEnd = false;
            } catch (IOException expected) {
                // Off a row is what this should be.
            }
            r.check(offRowAfterEnd, "the cursor stays off a row after being stepped past the end");
            if (countAfterFirstExhaustion >= 0) {
                r.check(Database.count(cur) == countAfterFirstExhaustion,
                        "calling next() past the end repeatedly does not inflate the row count, was "
                        + countAfterFirstExhaustion + " now " + Database.count(cur));
            }
            r.check(cur.last(), "last() still finds a row after the cursor was over-stepped");
            int lastBase = (mode == MODE_LEGACY && portKind() == PORT_SIMULATOR) ? 1 : 0;
            r.check(cur.getPosition() == 4 + lastBase,
                    "last() lands on the final row after over-stepping, position "
                    + cur.getPosition());
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

        // ---- a column index the result set does not have is a programming error, not a null
        cur = db.executeQuery("SELECT id, name FROM conf_cur ORDER BY id");
        try {
            cur.next();
            Row row = cur.getRow();
            r.check(readRejectsColumn(row, 2),
                    "reading past the last column raises IOException rather than answering null");
            r.check(readRejectsColumn(row, -1),
                    "reading a negative column raises IOException rather than answering null");
            boolean nameRejected = false;
            try {
                cur.getColumnName(99);
            } catch (IOException expected) {
                nameRejected = true;
            } catch (RuntimeException unchecked) {
                r.check(false, "getColumnName out of range raises IOException, not "
                        + unchecked.getClass().getName());
            }
            r.check(nameRejected, "getColumnName rejects an index the result set does not have");
        } finally {
            closeQuietly(cur);
        }

        db.execute("DROP TABLE IF EXISTS conf_null");
        db.execute("DROP TABLE IF EXISTS conf_cur");
    }

    /// The null array `execute(String, Object[])` has to accept as "no parameters at all".
    ///
    /// A method rather than `(Object[]) null` at the call site, which sits inside a block that
    /// catches Throwable: a failed cast is not an exception on every runtime this framework
    /// targets, so a cast there is one whose failure nothing could catch.
    private static Object[] noParameterArray() {
        return null;
    }

    /// Renders a string as code points, so a failure names what actually came back rather than
    /// printing characters a device log may not be able to show.
    private static String describeText(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        out.append('[');
        for (int iter = 0; iter < value.length(); iter++) {
            if (iter > 0) {
                out.append(' ');
            }
            out.append(Integer.toHexString(value.charAt(iter)));
        }
        out.append(']');
        return out.toString();
    }

    /// Reports whether reading `index` off `row` fails the way the contract says it should.
    ///
    /// A port whose engine substitutes SQL NULL for an out-of-range column - the SQLite C API does
    /// exactly that - would otherwise return null or zero here, which the caller cannot tell from
    /// stored data.
    private static boolean readRejectsColumn(Row row, int index) {
        try {
            row.getString(index);
            return false;
        } catch (IOException expected) {
            return true;
        } catch (RuntimeException unchecked) {
            return false;
        }
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
        noteDatabase(db);
        db.execute("DROP TABLE IF EXISTS conf_tx");
        db.execute("CREATE TABLE conf_tx (id INTEGER PRIMARY KEY)");

        // ---- a transaction opened by SQL is the same transaction
        // The API offers two ways to open one and only one of them goes through
        // beginTransaction(). If they disagree, changeKey() can replace the database underneath a
        // transaction it cannot see, and a commit can address one that has already ended.
        if (mode == MODE_STRICT) {
            db.execute("BEGIN");
            r.check(db.isInTransaction(), "execute(\"BEGIN\") opens a transaction the API sees");
            db.execute("ROLLBACK");
            r.check(!db.isInTransaction(), "execute(\"ROLLBACK\") ends it");

            // Mixed, in both directions: whichever way a transaction is opened, the other way of
            // ending it has to work. On an engine reached through a driver that keeps its own
            // transaction state -- the simulator's -- these are the calls that catch the two
            // states drifting apart, because one of them throws.
            boolean mixedCommitWorked = false;
            try {
                db.execute("BEGIN");
                db.execute("INSERT INTO conf_tx (id) VALUES (91)");
                db.commitTransaction();
                mixedCommitWorked = true;
            } catch (IOException err) {
                r.check(false, "commitTransaction() ends a transaction execute(\"BEGIN\") opened: "
                        + err.getMessage());
            }
            if (mixedCommitWorked) {
                r.check(rowCount(db, "conf_tx") == 1, "and the row it committed is there");
                r.check(!db.isInTransaction(), "and nothing is left open");
                db.execute("DELETE FROM conf_tx");
            }

            // BEGIN IMMEDIATE takes its write lock up front, which is the whole reason to write
            // it. It has to be a transaction like any other here; a port that reduced it to a
            // deferred BEGIN would still pass this, but one that rejected it or lost track of it
            // would not.
            boolean immediateWorked = false;
            try {
                db.execute("BEGIN IMMEDIATE");
                immediateWorked = db.isInTransaction();
                db.execute("INSERT INTO conf_tx (id) VALUES (93)");
                db.commitTransaction();
            } catch (IOException err) {
                r.check(false, "BEGIN IMMEDIATE opens a transaction: " + err.getMessage());
            }
            r.check(immediateWorked, "execute(\"BEGIN IMMEDIATE\") opens a transaction");
            if (immediateWorked) {
                r.check(rowCount(db, "conf_tx") == 1, "and its insert committed");
                db.execute("DELETE FROM conf_tx");
            }

            db.beginTransaction();
            db.execute("COMMIT");
            r.check(!db.isInTransaction(),
                    "a transaction the API opened is ended by execute(\"COMMIT\")");
            // And the connection is usable afterwards rather than left mid-transaction.
            boolean usableAfter = false;
            try {
                db.beginTransaction();
                db.execute("INSERT INTO conf_tx (id) VALUES (92)");
                db.rollbackTransaction();
                usableAfter = true;
            } catch (IOException err) {
                r.check(false, "a transaction still works after execute(\"COMMIT\") ended one: "
                        + err.getMessage());
            }
            if (usableAfter) {
                r.check(rowCount(db, "conf_tx") == 0, "and that rollback discarded its insert");
            }
        }

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
            // ---- a failed commit ends the transaction rather than wedging the database
            db.execute("DROP TABLE IF EXISTS conf_fk_child");
            db.execute("DROP TABLE IF EXISTS conf_fk_parent");
            db.execute("CREATE TABLE conf_fk_parent (id INTEGER PRIMARY KEY)");
            db.execute("CREATE TABLE conf_fk_child (id INTEGER PRIMARY KEY, parent INTEGER "
                    + "REFERENCES conf_fk_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            boolean deferredConstraintsWork = true;
            try {
                db.execute("PRAGMA foreign_keys = ON");
                db.beginTransaction();
                db.execute("INSERT INTO conf_fk_child (id, parent) VALUES (1, 999)");
                db.commitTransaction();
                // The engine did not enforce the constraint, so there is nothing to recover from.
                deferredConstraintsWork = false;
            } catch (IOException expected) {
                // The commit failed, which is the interesting case. The engines disagree about
                // what they leave behind - Android has already ended the transaction, the SQLite
                // C API and JDBC have not - so the contract is that the port reconciles that and
                // the database comes back with no transaction open either way.
                r.check(!db.isInTransaction(),
                        "a failed commit leaves no transaction open rather than wedging the "
                        + "database with one that can never be committed");
                boolean reusable = true;
                try {
                    db.beginTransaction();
                    db.rollbackTransaction();
                } catch (IOException err) {
                    reusable = false;
                }
                r.check(reusable, "a new transaction can be started after a failed commit");
            } catch (RuntimeException unchecked) {
                // Caught deliberately: the contract is that every failure is an IOException, so an
                // engine exception escaping unwrapped is itself the finding, and reporting it beats
                // letting it abort the whole group.
                deferredConstraintsWork = false;
                r.check(false, "a failed commit raises IOException rather than "
                        + unchecked.getClass().getName());
                try {
                    if (db.isInTransaction()) {
                        db.rollbackTransaction();
                    }
                } catch (Throwable ignored) {
                    // Already reported.
                }
            }
            if (!deferredConstraintsWork) {
                r.info("this engine did not enforce the deferred foreign key, so the "
                        + "failed-commit recovery path was not exercised");
                if (db.isInTransaction()) {
                    db.rollbackTransaction();
                }
            }
            db.execute("PRAGMA foreign_keys = OFF");
            db.execute("DROP TABLE IF EXISTS conf_fk_child");
            db.execute("DROP TABLE IF EXISTS conf_fk_parent");

            // ---- a failure that surfaces from a cursor step is reconciled too
            // executeQuery runs its statement lazily on the ports that prepare and step: an
            // INSERT reached this way does its insert on the first step, not at prepare. A
            // constraint with ON CONFLICT ROLLBACK ends the transaction as it fails, so a port
            // that reads the engine back only in execute() keeps a flag over a transaction that
            // is gone -- and then the rollback that would have cleared it fails too, with "no
            // transaction", leaving every later begin and key change refused until close.
            // What is asserted is recovery, not which engine ends what: after the failure the
            // database has to be usable again.
            db.execute("DROP TABLE IF EXISTS conf_tx_unique");
            db.execute("CREATE TABLE conf_tx_unique (id INTEGER PRIMARY KEY)");
            db.execute("INSERT INTO conf_tx_unique (id) VALUES (1)");
            db.beginTransaction();
            boolean conflicted = false;
            // Deliberately flat: the close happens after the catch rather than in a finally,
            // because a finally runs while the failure is still unwinding and the ports reach
            // back into the engine there.
            Cursor conflicting = null;
            try {
                conflicting = db.executeQuery(
                        "INSERT OR ROLLBACK INTO conf_tx_unique (id) VALUES (1)");
                conflicting.next();
            } catch (IOException expected) {
                conflicted = true;
            } catch (RuntimeException unchecked) {
                conflicted = true;
                r.check(false, "a constraint failure reached through executeQuery raises "
                        + "IOException rather than " + unchecked.getClass().getName());
            }
            if (conflicting != null) {
                try {
                    conflicting.close();
                } catch (IOException alsoFailed) {
                    // The constraint failure is the one under test.
                } catch (RuntimeException alsoFailed) {
                    // Likewise.
                }
            }
            if (conflicted) {
                try {
                    if (db.isInTransaction()) {
                        db.rollbackTransaction();
                    }
                    boolean reusable = true;
                    try {
                        db.beginTransaction();
                        db.rollbackTransaction();
                    } catch (IOException err) {
                        reusable = false;
                    }
                    r.check(reusable, "a new transaction can be started after a constraint "
                            + "failure reached through executeQuery");
                } catch (IOException stuck) {
                    r.check(false, "after a constraint failure reached through executeQuery the "
                            + "transaction the port still believes is open cannot be ended: "
                            + stuck.getMessage());
                } catch (RuntimeException stuck) {
                    r.check(false, "after a constraint failure reached through executeQuery the "
                            + "transaction cannot be ended: " + stuck.getClass().getName());
                }
            } else {
                r.info("this engine accepted the conflicting insert, so the cursor-step "
                        + "reconciliation path was not exercised");
                if (db.isInTransaction()) {
                    db.rollbackTransaction();
                }
            }
            db.execute("DROP TABLE IF EXISTS conf_tx_unique");

            // ---- transaction control is not something executeQuery will run
            // A cursor runs its statement when it is stepped, so a BEGIN reached this way opens a
            // transaction nothing recorded: isInTransaction() then answers false over an open one
            // and a key change is allowed across live work. Refused, and the check is that the
            // refusal happened AND no transaction was left behind by it.
            boolean queryControlRefused = false;
            Cursor control = null;
            try {
                control = db.executeQuery("BEGIN");
            } catch (IOException expected) {
                queryControlRefused = true;
            }
            if (control != null) {
                try {
                    control.close();
                } catch (IOException ignored) {
                    // The refusal above is what is under test.
                }
            }
            // Note for whoever reads a failure here: the simulator routes transaction control
            // through the driver from executeQuery as well, so it satisfies this whether or not
            // the refusal is in place. The ports that prepare and step are the ones this is for,
            // and they only run the statement when the cursor is stepped.
            r.check(queryControlRefused, "executeQuery refuses a transaction control statement");
            r.check(!db.isInTransaction(), "and it did not leave a transaction open");
            if (db.isInTransaction()) {
                db.rollbackTransaction();
            }

            // ---- a cursor over a writing statement never runs it twice
            // executeQuery prepares and steps, so an INSERT ... RETURNING does its insert while
            // the cursor is walked. Anything that rewinds -- getCount(), last(), going backwards
            // -- re-executes the statement on the ports that step, which would write the rows a
            // second time without saying so. The count afterwards is what proves it.
            db.execute("DROP TABLE IF EXISTS conf_returning");
            // No explicit key: a second execution has to be able to succeed, or a duplicate write
            // would collide with the first and the check would pass without proving anything.
            db.execute("CREATE TABLE conf_returning (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "v TEXT)");
            Cursor returning = null;
            boolean returningSupported = true;
            try {
                returning = db.executeQuery(
                        "INSERT INTO conf_returning (v) VALUES ('a') RETURNING id");
                returning.next();
            } catch (IOException unsupported) {
                // RETURNING arrived in SQLite 3.35. An engine without it has nothing to test.
                returningSupported = false;
            }
            if (returningSupported && returning != null) {
                try {
                    Database.count(returning);
                    r.info("this port answers getCount() on a writing statement without "
                            + "re-running it");
                } catch (IOException refused) {
                    r.info("this port refuses to rewind a writing statement, which is how it "
                            + "avoids running the write twice");
                }
                try {
                    returning.close();
                } catch (IOException ignored) {
                    // The row count below is the assertion.
                }
                r.check(rowCount(db, "conf_returning") == 1,
                        "an INSERT ... RETURNING walked through a cursor inserts its row once, "
                        + "however the cursor was navigated");
            } else {
                if (returning != null) {
                    try {
                        returning.close();
                    } catch (IOException ignored) {
                        // Nothing was established either way.
                    }
                }
                r.info("this engine does not support RETURNING, so a writing cursor could not "
                        + "be exercised");
            }
            // ---- and first() on one still works, because it needs no re-execution
            // The cursor has not been stepped, so the first row is reached by stepping forward
            // like any other. A guard that refused every backward move without noticing that
            // would break ordinary navigation over rows next() hands back quite happily. Asked
            // of a cursor nothing has touched, which is the only state where it is a question.
            if (returningSupported) {
                Cursor fresh = null;
                boolean firstWorks = false;
                try {
                    fresh = db.executeQuery(
                            "INSERT INTO conf_returning (v) VALUES ('b') RETURNING id");
                    firstWorks = fresh.first();
                } catch (IOException refused) {
                    firstWorks = false;
                }
                if (fresh != null) {
                    try {
                        fresh.close();
                    } catch (IOException ignored) {
                        // The answer above is what is under test.
                    }
                }
                r.check(firstWorks, "first() reaches the row of a statement that writes, which "
                        + "asks for no re-execution");
                // The other side of the same coin, and the one prev() and position(-1) reach:
                // a cursor that has not been stepped is already before its first row, so asking
                // to go back there is a no-op rather than a rewind.
                boolean beforeFirstWorks = false;
                Cursor untouched = null;
                try {
                    untouched = db.executeQuery(
                            "INSERT INTO conf_returning (v) VALUES ('c') RETURNING id");
                    Database.beforeFirst(untouched);
                    beforeFirstWorks = true;
                } catch (IOException refused) {
                    beforeFirstWorks = false;
                }
                if (untouched != null) {
                    try {
                        untouched.close();
                    } catch (IOException ignored) {
                        // The answer above is what is under test.
                    }
                }
                r.check(beforeFirstWorks, "beforeFirst() on a cursor that has not been stepped is "
                        + "a no-op, including over a statement that writes");
                // How many rows that left is a port difference rather than a contract: the ports
                // that prepare and step have not run the statement at all, because nothing
                // stepped the cursor, while the simulator's driver runs it when the query is
                // executed. Both are three at most, and neither is two inserts from one
                // statement, which is what this section is really about.
                int afterUntouched = rowCount(db, "conf_returning");
                r.info("a writing statement whose cursor was never stepped left "
                        + afterUntouched + " row(s): this engine runs it "
                        + (afterUntouched == 3 ? "when the query is executed" : "only when the "
                            + "cursor is stepped"));
                r.check(afterUntouched <= 3,
                        "and no statement here inserted more than the one row it describes");

            }
            db.execute("DROP TABLE IF EXISTS conf_returning");

            // ---- the engine's own compile-time defaults are the same everywhere
            // Neither of these can be changed on the ports that use somebody else's engine: the
            // Android platform build and the simulator's JDBC driver are compiled as they are,
            // and DQS has no pragma at all. So the engine Codename One bundles has to keep
            // SQLite's defaults, or the same SQL would mean different things per port -- and on
            // iOS it would change meaning when encryption was switched on, since that is what
            // swaps Apple's engine for the bundled one.
            boolean doubleQuotedStringWorks = false;
            Cursor quoted = null;
            try {
                quoted = db.executeQuery("SELECT \"conf_dqs\" AS v");
                doubleQuotedStringWorks = quoted.next()
                        && "conf_dqs".equals(quoted.getRow().getString(0));
            } catch (IOException rejected) {
                doubleQuotedStringWorks = false;
            }
            if (quoted != null) {
                try {
                    quoted.close();
                } catch (IOException ignored) {
                    // The answer above is what is under test.
                }
            }
            // Reported, not asserted. SQLITE_DQS is a compile-time setting with no pragma, so it
            // cannot be made uniform: the Android and Apple engines accept a double quoted string
            // literal and the simulator's driver rejects it, and neither is ours to rebuild. The
            // engine Codename One does build keeps SQLite's default so that turning encryption on
            // never changes what a statement means. Recorded on every port so the difference is
            // visible in a run rather than discovered in an application.
            r.info(doubleQuotedStringWorks
                    ? "double quoted string literals are accepted by this engine; they are "
                        + "rejected in the simulator, so single quotes are the portable form"
                    : "double quoted string literals are rejected by this engine, as SQL requires "
                        + "-- use single quotes for strings and this is portable everywhere");

            db.execute("DROP TABLE IF EXISTS conf_fk_off_child");
            db.execute("DROP TABLE IF EXISTS conf_fk_off_parent");
            db.execute("CREATE TABLE conf_fk_off_parent (id INTEGER PRIMARY KEY)");
            db.execute("CREATE TABLE conf_fk_off_child (id INTEGER PRIMARY KEY, parent INTEGER "
                    + "REFERENCES conf_fk_off_parent(id))");
            boolean foreignKeysIgnoredUntilAskedFor = true;
            try {
                db.execute("INSERT INTO conf_fk_off_child (id, parent) VALUES (1, 404)");
            } catch (IOException enforced) {
                foreignKeysIgnoredUntilAskedFor = false;
            }
            r.check(foreignKeysIgnoredUntilAskedFor,
                    "foreign keys are enforced only once PRAGMA foreign_keys = ON asks for it, "
                    + "which is SQLite's default and the only setting every port here shares");
            db.execute("DROP TABLE IF EXISTS conf_fk_off_child");
            db.execute("DROP TABLE IF EXISTS conf_fk_off_parent");

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

        // Whether the bytes behind a database can be read at all, established against a database
        // known to be plaintext rather than assumed from the platform. A port can hand back a path
        // that opens and still is not the database file: the browser keeps its databases in a
        // storage pool whose slots begin with the pool's own metadata, so a read there succeeds and
        // never looks like SQLite -- which silently satisfies every "this is not plaintext" check
        // and fails only the one that wants the opposite.
        boolean storedBytesVisible = storedBytesAreVisible(r, databaseName);

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
        checkStoredBytes(r, storedBytesVisible, databaseName, false,
                "the encrypted database does not begin with a plaintext SQLite header");
        r.check(Database.isEncrypted(databaseName), "isEncrypted reports true for it");

        // ---- no key at all is rejected
        //
        // The proof of encryption that needs no access to the bytes. Where a port keeps its
        // databases somewhere the checks above cannot read, this is what is left to say the
        // contents are not simply sitting there for anyone who opens the database.
        //
        // On its own database, because the answer is allowed to cost the database. Android hands
        // an unkeyed open to the platform SQLite, which reports the ciphertext as corruption; the
        // port keeps the file rather than letting the platform delete it, but a port that did not
        // would leave every assertion after this one running against whatever was left.
        String unkeyedName = databaseName + "-nokey";
        deleteQuietly(unkeyedName);
        Database unkeyed = Database.openOrCreate(unkeyedName, DatabaseConfig.passphrase(passphrase));
        try {
            unkeyed.execute("CREATE TABLE secret (id INTEGER PRIMARY KEY, v TEXT)");
            unkeyed.execute("INSERT INTO secret (id, v) VALUES (1, 'classified')");
        } finally {
            closeQuietly(unkeyed);
        }
        boolean noKeyRejected = false;
        unkeyed = null;
        try {
            unkeyed = Database.openOrCreate(unkeyedName);
            Cursor probe = unkeyed.executeQuery("SELECT v FROM secret");
            noKeyRejected = !probe.next() || !"classified".equals(probe.getRow().getString(0));
            probe.close();
        } catch (IOException err) {
            noKeyRejected = true;
        } finally {
            closeQuietly(unkeyed);
        }
        r.check(noKeyRejected, "an encrypted database does not give up its rows without a key");
        // The refused open must not have destroyed it: the data is intact and one correct key away,
        // and a port that answered a wrong key by deleting the file would have thrown that away.
        Database survived = null;
        try {
            survived = Database.openOrCreate(unkeyedName, DatabaseConfig.passphrase(passphrase));
            Cursor rows = survived.executeQuery("SELECT v FROM secret WHERE id = 1");
            r.check(rows.next() && "classified".equals(rows.getRow().getString(0)),
                    "a refused open leaves the database readable with the right key");
            rows.close();
        } catch (IOException err) {
            r.check(false, "a refused open destroyed the database: " + err.getMessage());
        } finally {
            closeQuietly(survived);
        }
        deleteQuietly(unkeyedName);

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
                // A note rather than a skip. Managed keys need a non-prompting secure store, which
                // not every port has, but everything else in this group -- passphrases, raw keys,
                // re-keying, conversion, ciphertext on disk -- has already run and been asserted
                // by the time we get here. Reporting the group as skipped would hide all of that
                // behind one unavailable sub-feature and read as "encryption is untested on this
                // port" when the opposite is true. The missing capability is SecureStorage's, and
                // it is reported under SecureStorage.
                r.info("managed keys are unavailable here, so only the generated-key case is "
                        + "untested: " + err.getMessage());
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
        checkStoredBytes(r, storedBytesVisible, migrateName, false,
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
        checkStoredBytes(r, storedBytesVisible, migrateName, true,
                "decrypt() restores a plaintext SQLite file");
        // Asked of the port as well as of the bytes. Where the bytes cannot be read this is the
        // only thing left that says the conversion happened, and it was the missing half: the
        // encrypted side has been asserted since the beginning and the decrypted side never was.
        r.check(!Database.isEncrypted(migrateName), "isEncrypted is false again after decrypt()");
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
        if (portKindOverride != PORT_AUTODETECT) {
            return portKindOverride;
        }
        if (portKindFromDatabase != PORT_AUTODETECT) {
            return portKindFromDatabase;
        }
        try {
            String platform = Display.getInstance().getPlatformName();
            if ("ios".equals(platform)) {
                return PORT_IOS;
            }
            if ("and".equals(platform)) {
                return PORT_ANDROID;
            }
            if (Display.getInstance().isSimulator()) {
                return PORT_SIMULATOR;
            }
        } catch (Throwable displayNotUp) {
            return PORT_OTHER;
        }
        return PORT_OTHER;
    }

    /// Autodetected from a database implementation, or `#PORT_AUTODETECT` before one is seen.
    private static int portKindFromDatabase = PORT_AUTODETECT;

    /// Records which port a database came from, which is the only unambiguous signal available.
    ///
    /// `Display` cannot answer this. The desktop simulator reports the platform name of the skin it
    /// is wearing and answers `isSimulator()` with true -- and so does a real iOS build running on
    /// the iOS Simulator, which is where this suite runs in CI. Asking `isSimulator()` first read
    /// every iOS Simulator run as a desktop one and applied the wrong port's legacy expectations to
    /// it; asking the platform name first would still confuse the desktop simulator wearing an iOS
    /// skin for iOS. The implementation class behind the `Database` is neither.
    private static void noteDatabase(Database db) {
        if (db == null || portKindFromDatabase != PORT_AUTODETECT) {
            return;
        }
        String impl = db.getClass().getName();
        if (impl.startsWith("com.codename1.impl.javase.")) {
            portKindFromDatabase = PORT_SIMULATOR;
        } else if (impl.startsWith("com.codename1.impl.ios.")) {
            portKindFromDatabase = PORT_IOS;
        } else if (impl.startsWith("com.codename1.impl.android.")) {
            portKindFromDatabase = PORT_ANDROID;
        } else {
            portKindFromDatabase = PORT_OTHER;
        }
    }

    /// The blob-query capability could not be determined.
    private static final int CAPABILITY_UNKNOWN = -1;

    /// The platform reports that it does not support blob query parameters.
    private static final int CAPABILITY_NO = 0;

    /// The platform reports that it supports blob query parameters.
    private static final int CAPABILITY_YES = 1;

    /// Reads the blob-query capability.
    ///
    /// Reports `#CAPABILITY_UNKNOWN` rather than guessing when the display is not up, so that a
    /// harness driving a `Database` directly does not assert the opposite of what the port
    /// actually supports.
    private static int blobQueryParametersSupported() {
        try {
            return Database.isBlobQueryParameterSupported() ? CAPABILITY_YES : CAPABILITY_NO;
        } catch (Throwable displayNotUp) {
            return CAPABILITY_UNKNOWN;
        }
    }

    /// The stored bytes could not be read, so neither answer below is available.
    private static final int HEADER_UNREADABLE = -1;

    /// The stored bytes do not begin with SQLite's plaintext header.
    private static final int HEADER_ENCRYPTED = 0;

    /// The stored bytes begin with SQLite's plaintext header.
    private static final int HEADER_PLAINTEXT = 1;

    /// Whether the file behind a database begins with SQLite's plaintext header.
    ///
    /// Three answers rather than two, because a platform that keeps its databases somewhere other
    /// than the filesystem -- the browser's storage pool has no path to open -- can give neither.
    /// Folding that into "not plaintext" is what made the ciphertext checks pass on a port where
    /// nothing was read at all, which is the failure this suite exists to catch.
    private static int plaintextHeaderState(String databaseName) {
        String path = Database.getDatabasePath(databaseName);
        if (path == null) {
            return HEADER_UNREADABLE;
        }
        try {
            InputStream in = null;
            try {
                in = FileSystemStorage.getInstance().openInputStream(path);
                if (in == null) {
                    return HEADER_UNREADABLE;
                }
                byte[] header = new byte[15];
                int offset = 0;
                while (offset < header.length) {
                    int read = in.read(header, offset, header.length - offset);
                    if (read < 0) {
                        return HEADER_UNREADABLE;
                    }
                    offset += read;
                }
                // Compare bytes rather than decoding: the header is fixed ASCII, and decoding it
                // would depend on the platform default charset.
                byte[] expected = {'S', 'Q', 'L', 'i', 't', 'e', ' ', 'f', 'o', 'r', 'm',
                    'a', 't', ' ', '3'};
                for (int iter = 0; iter < expected.length; iter++) {
                    if (header[iter] != expected[iter]) {
                        return HEADER_ENCRYPTED;
                    }
                }
                return HEADER_PLAINTEXT;
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException err) {
            return HEADER_UNREADABLE;
        }
    }

    /// Asserts what the stored bytes are, where the platform lets them be read.
    ///
    /// #### Parameters
    ///
    /// - `r`: the reporter
    /// - `databaseName`: the database whose file to look at
    /// - `expectPlaintext`: whether the file should begin with the plaintext header
    /// - `message`: what is being asserted
    private static void checkStoredBytes(Reporter r, boolean storedBytesVisible,
            String databaseName, boolean expectPlaintext, String message) {
        if (!storedBytesVisible) {
            r.info("the stored bytes are not reachable on this platform, so this was not"
                    + " checked: " + message);
            return;
        }
        r.check((plaintextHeaderState(databaseName) == HEADER_PLAINTEXT) == expectPlaintext,
                message);
    }

    /// Whether reading a database's stored bytes gives back the database.
    ///
    /// Calibrated against a plaintext database rather than assumed, because the failure it guards
    /// against looks exactly like success: a port whose path is not the database file still reads
    /// something, and that something is never an SQLite header, so every "this is not plaintext"
    /// assertion passes without a database having been read at all.
    ///
    /// #### Parameters
    ///
    /// - `r`: the reporter, told once when the checks cannot run
    /// - `databaseName`: the database under test, whose name the probe borrows
    private static boolean storedBytesAreVisible(Reporter r, String databaseName) {
        String probeName = databaseName + "-bytes-probe";
        deleteQuietly(probeName);
        try {
            Database probe = Database.openOrCreate(probeName);
            try {
                probe.execute("CREATE TABLE probe (id INTEGER PRIMARY KEY)");
            } finally {
                closeQuietly(probe);
            }
            boolean visible = plaintextHeaderState(probeName) == HEADER_PLAINTEXT;
            if (!visible) {
                r.info("a plaintext database does not read back as one here, so this platform does "
                        + "not expose its databases as files and the ciphertext-on-disk checks "
                        + "cannot run");
            }
            return visible;
        } catch (IOException err) {
            r.info("a probe database could not be created, so the ciphertext-on-disk checks cannot "
                    + "run: " + err.getMessage());
            return false;
        } finally {
            deleteQuietly(probeName);
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

    /// Closes a stream, if one was opened, without letting the close hide what came before it.
    private static void closeQuietly(OutputStream out) {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException alreadyReported) {
            // The write is what mattered, and it has been reported already.
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
