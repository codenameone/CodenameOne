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
package com.codename1.impl.android.cipher;

import android.database.sqlite.SQLiteException;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.db.DatabaseEncryptionException;
import com.codename1.impl.android.AndroidCursor;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.SQLStatementSplitter;

import net.zetetic.database.sqlcipher.SQLiteCursor;
import net.zetetic.database.sqlcipher.SQLiteCursorDriver;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteProgram;
import net.zetetic.database.sqlcipher.SQLiteQuery;
import net.zetetic.database.sqlcipher.SQLiteStatement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The SQLCipher-backed database.
 *
 * Structurally this mirrors AndroidDB, because SQLCipher deliberately mirrors
 * android.database.sqlite. The two cannot share code: the types are source compatible but not
 * assignment compatible, and any shared supertype referencing net.zetetic would have to live in
 * the always-present part of the port, which is exactly what has to stay deletable.
 */
class AndroidCipherDB extends Database {

    private SQLiteDatabase db;
    private final List<AndroidCursor> openCursors = new ArrayList<AndroidCursor>();

    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name.
     */
    private final String databaseName;

    /**
     * The key this database is currently open under, empty for a plaintext file.
     *
     * Retained because changeKey has to know which of two very different migrations it is
     * performing, and because the export route below has to reopen the database afterwards.
     * SQLCipher already holds the key for the lifetime of the connection, so this adds no
     * exposure beyond what having the database open already implies.
     */
    private String currentKey;

    /**
     * Every connection this port has open, by the file it is open on.
     *
     * Only a conversion needs this. A conversion is not a statement: it renames a new file over
     * the database while the process is running, and on Android that rename succeeds even while
     * another connection holds the old file open. That connection keeps writing to the file that
     * has just been replaced -- its writes are accepted, and then the backup they landed in is
     * deleted -- so the caller is told its data was saved and it is not there. Counting the
     * connections is what lets a conversion refuse rather than do that.
     */
    private static final java.util.Map<String, Integer> OPEN_CONNECTIONS =
            new java.util.HashMap<String, Integer>();

    private static synchronized void connectionOpened(String path) {
        Integer count = OPEN_CONNECTIONS.get(path);
        OPEN_CONNECTIONS.put(path, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
    }

    private static synchronized void connectionClosed(String path) {
        Integer count = OPEN_CONNECTIONS.get(path);
        if (count == null) {
            return;
        }
        if (count.intValue() <= 1) {
            OPEN_CONNECTIONS.remove(path);
        } else {
            OPEN_CONNECTIONS.put(path, Integer.valueOf(count.intValue() - 1));
        }
    }

    private static synchronized int openConnections(String path) {
        Integer count = OPEN_CONNECTIONS.get(path);
        return count == null ? 0 : count.intValue();
    }

    /** The path this connection is open on, for the registry above. */
    private final String openPath;

    AndroidCipherDB(SQLiteDatabase db, String databaseName, String key) {
        this.db = db;
        this.databaseName = databaseName;
        this.currentKey = key == null ? "" : key;
        this.openPath = db.getPath();
        connectionOpened(this.openPath);
    }

    private void checkOpen() throws IOException {
        if (db == null) {
            throw new IOException("This database has been closed");
        }
    }

    /// Rejects a call that supplies the wrong number of bind arguments.
    ///
    /// The Android engine binds what it is given and leaves any remaining placeholder as NULL, so
    /// a short argument list silently executes a different statement than the caller wrote. Every
    /// other port gets this check from its engine; here it comes from the text.
    private void checkParameterCount(String sql, int supplied) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        int declared = SQLStatementSplitter.countParameters(sql);
        if (declared != SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN && declared != supplied) {
            throw new IOException("The statement has " + declared + " parameters but "
                    + supplied + " were supplied");
        }
    }

    private void requireSingleStatement(String sql) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        if (SQLStatementSplitter.isMultiStatement(sql)) {
            throw new IOException("This method takes a single SQL statement, but the string "
                    + "contains " + SQLStatementSplitter.countStatements(sql)
                    + ". Use execute(String) to run a script.");
        }
    }

    @Override
    public void beginTransaction() throws IOException {
        checkOpen();
        checkBeginTransaction();
        try {
            db.beginTransaction();
        } catch (RuntimeException err) {
            markTransactionEnded();
            throw new IOException(err.getMessage(), err);
        }
    }

    @Override
    public void commitTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        try {
            db.setTransactionSuccessful();
            // A deferred constraint is checked here, so this is where a commit fails. The engine
            // reports it as an unchecked SQLiteException, while the contract for this API is that
            // every failure is an IOException.
            db.endTransaction();
        } catch (RuntimeException err) {
            rollbackQuietly();
            throw abandonFailedCommit(err);
        }
        markTransactionEnded();
    }

    /// Ends a transaction that a failed commit left behind.
    ///
    /// endTransaction() pops its own transaction record before it sends the COMMIT, so when the
    /// COMMIT fails the engine is still holding the transaction while the wrapper believes there
    /// is none. Measured on API 34, after a deferred foreign key violation: inTransaction()
    /// reports false, the uncommitted row is still visible to reads, and the next
    /// beginTransaction() fails with "cannot start a transaction within a transaction". Leaving
    /// it is not an option -- the database is unusable for transactions from then on, and every
    /// later read sees writes that were never committed.
    ///
    /// The wrapper cannot roll it back for us. A plain execSQL("ROLLBACK") never reaches SQLite:
    /// the session layer classifies a statement by its first three characters and turns "ROL"
    /// into its own endTransaction(), which throws because it thinks no transaction is open.
    /// Same for a second endTransaction(), and for compileStatement and rawQuery, which route
    /// through the same classifier. A statement that does not start with those three characters
    /// is passed through to the engine unexamined, so a leading comment is what gets the rollback
    /// to the connection that actually needs it. Verified against all of the above on API 34:
    /// this is the only one that recovers.
    ///
    /// That measurement was taken against the stock engine. SQLCipher's SQLiteDatabase is a fork
    /// of the same AOSP sources and classifies statements the same way, so the same applies here.
    private void rollbackQuietly() {
        try {
            db.execSQL("/* not ROLLBACK to the statement classifier */ ROLLBACK");
        } catch (Throwable ignored) {
            // Nothing to end, or a platform that classifies differently. Either way the caller is
            // already reporting the commit failure, and a conformance run reports the rest.
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        try {
            db.endTransaction();
        } catch (RuntimeException err) {
            throw new IOException(err.getMessage(), err);
        }
        markTransactionEnded();
    }

    @Override
    public void close() throws IOException {
        if (db == null) {
            return;
        }
        SQLiteDatabase closing = db;
        db = null;
        if (inTransaction) {
            inTransaction = false;
            try {
                closing.endTransaction();
            } catch (RuntimeException ignored) {
                // Best effort; the close below is what matters.
            }
        }
        AndroidCursor[] cursors = openCursors.toArray(new AndroidCursor[openCursors.size()]);
        openCursors.clear();
        for (int iter = 0; iter < cursors.length; iter++) {
            cursors[iter].invalidate();
        }
        closing.close();
        connectionClosed(openPath);
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        checkNoTransactionForKeyChange();
        String targetKey = config == null || !config.isEncrypted()
                ? "" : config.resolveKeyMaterial(databaseName);
        if (currentKey.length() == 0 || targetKey.length() == 0) {
            // One side is plaintext, which rekey refuses outright.
            migrateThroughExport(targetKey);
            return;
        }
        // Quote through the shared helper: a passphrase may contain quotes, and interpolating it
        // directly would let one change the statement.
        String statement = "PRAGMA rekey = " + toPragmaLiteral(targetKey);
        // rawQuery, not execSQL. This PRAGMA answers with a row, and execSQL rejects anything
        // that returns one outright -- "Queries can be performed using SQLiteDatabase query or
        // rawQuery methods only" -- so every re-key failed before it began. The row itself is of
        // no interest; the cursor has to be stepped for the statement to run at all.
        android.database.Cursor c = null;
        try {
            c = db.rawQuery(statement, null);
            c.moveToFirst();
            currentKey = targetKey;
        } catch (SQLiteException err) {
            throw new IOException(err.getMessage(), err);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Converts between plaintext and encrypted, which PRAGMA rekey cannot do.
     *
     * SQLCipher accepts rekey only between two encrypted states: on a plaintext database, and on
     * a rekey to the empty key, it refuses with "PRAGMA rekey can only be run on an existing
     * encrypted database. Use sqlcipher_export() and ATTACH to convert encrypted/plaintext
     * databases." Both of those are exactly Database.encrypt and Database.decrypt, so this is
     * the route both take here. The engines differ on this: the SQLite3MC build the other ports
     * carry does rekey all three directions in place, which is why only Android needs it.
     *
     * sqlcipher_export copies schema and rows but not the header pragmas, so user_version and
     * application_id are carried across explicitly; an application using either for schema
     * versioning or file identification would otherwise silently come back at zero.
     *
     * The new database is built beside the old one and swapped in only once it is complete. The
     * original is renamed aside rather than deleted, so a complete database exists under one of
     * the two names at every instant; AndroidCipherFactory recovers from the backup if the
     * process dies in the gap.
     */
    /** Reads one integer header pragma, which sqlcipher_export does not carry across. */
    private int readHeaderPragma(String pragma) {
        android.database.Cursor c = db.rawQuery(pragma, null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    /**
     * Disposes of an export that is not going to be used.
     *
     * The export is a complete second copy of the data, and when the conversion was a decryption
     * it is a plaintext copy, so leaving it behind is a disclosure rather than litter. The rule
     * lives in AndroidImplementation because recovery has to apply the same one, and this package
     * is deleted from applications that never encrypt.
     *
     * @param target the export to remove
     * @return a sentence to append to the failure message, empty when nothing survived
     */
    private static String discardExport(File target) {
        return AndroidImplementation.discardDatabaseMigrationExport(target);
    }

    /**
     * Builds a createTempFile prefix from a database name.
     *
     * Two things the name itself cannot be trusted about. createTempFile rejects a prefix under
     * three characters with an IllegalArgumentException, and "a" is a perfectly good database
     * name, so converting one would have died before it exported anything -- with an unchecked
     * exception, out of a method that promises IOException. And a name may legally contain a line
     * break, which the generated file name would inherit and the marker, which is a line per
     * entry, would then record as two: recovery would look for a file whose name is the first
     * line and never find the export or the backup. Control characters are replaced rather than
     * escaped, because the name here is a hint for whoever reads these files when something has
     * gone wrong and not something anything parses back out.
     *
     * @param name the database's file name
     * @return a prefix createTempFile will accept and the marker can record on one line
     */
    private static String tempPrefix(String name) {
        StringBuilder prefix = new StringBuilder();
        for (int iter = 0; iter < name.length(); iter++) {
            char c = name.charAt(iter);
            prefix.append(c < ' ' || c == 127 ? '_' : c);
        }
        while (prefix.length() < 3) {
            prefix.append('_');
        }
        return prefix.append('.').toString();
    }

    private void migrateThroughExport(String targetKey) throws IOException {
        String path = db.getPath();
        if (openConnections(path) > 1) {
            // Refused rather than raced. The swap renames a new file over this one, and Android
            // lets that succeed while another connection still holds the old file open -- that
            // connection keeps writing to a file that is no longer the database, is told each
            // write succeeded, and then loses the lot when the backup is deleted. Its WAL is the
            // same story. There is no way to migrate underneath it, so the caller is told.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database " + path + " is open more than once, and converting it replaces "
                    + "the file underneath every connection to it. Close the other connections "
                    + "first; writes made through them during the conversion would be accepted "
                    + "and then lost.");
        }
        String name = new File(path).getName();
        File dir = AndroidImplementation.databaseMigrationDir(path);
        if (dir == null) {
            throw new IOException("The database " + path + " has no directory to convert it in");
        }
        if (!dir.isDirectory() && !dir.mkdirs()) {
            // A file already sitting at that name is the one collision left, and refusing is the
            // only safe answer: it may be an application database and this must not touch it.
            throw new IOException("The conversion needs a working directory at " + dir
                    + " and could not create one. If a file exists there, move it aside.");
        }
        // Created rather than named. A path chosen up front can already be occupied - custom
        // database paths mean an application can put a database anywhere, including in here -
        // and deleting whatever is sitting there is how the previous three attempts at this went
        // wrong. A file this call creates is ours by construction and collides with nothing.
        File target = File.createTempFile(tempPrefix(name), ".target", dir);
        // Recorded while it is still empty, and before a single row goes into it. Between creating
        // this file and finishing the conversion the process can be killed at any point, and from
        // then on the file is a complete second copy of the data -- in the clear when this is a
        // decryption. Writing the record afterwards would leave that copy on disk under a random
        // name with nothing that knows to look for it; writing it first costs a file that recovery
        // finds and removes.
        AndroidImplementation.writeDatabaseMigrationMarker(path, null, target);
        int userVersion = 0;
        int applicationId = 0;
        try {
            userVersion = readHeaderPragma("PRAGMA user_version");
            applicationId = readHeaderPragma("PRAGMA application_id");
            db.execSQL("ATTACH DATABASE " + toPragmaLiteral(target.getPath())
                    + " AS cn1migrate KEY " + toPragmaLiteral(targetKey));
            android.database.Cursor exported = db.rawQuery("SELECT sqlcipher_export('cn1migrate')",
                    null);
            try {
                exported.moveToFirst();
            } finally {
                exported.close();
            }
            db.execSQL("PRAGMA cn1migrate.user_version = " + userVersion);
            db.execSQL("PRAGMA cn1migrate.application_id = " + applicationId);
            db.execSQL("DETACH DATABASE cn1migrate");
        } catch (RuntimeException err) {
            // Detach before giving up. A caller that catches this and carries on with the same
            // database would otherwise be left with cn1migrate still attached, and the next
            // conversion would fail because that name is already in use.
            try {
                db.execSQL("DETACH DATABASE cn1migrate");
            } catch (RuntimeException notAttached) {
                // It may never have been attached; the conversion failure is what matters.
            }
            String surviving = discardExport(target);
            File unusedMarker = AndroidImplementation.databaseMigrationMarker(path);
            if (unusedMarker != null) {
                unusedMarker.delete();
            }
            throw new IOException("The database could not be converted: " + err.getMessage()
                    + surviving, err);
        }
        SQLiteDatabase closing = db;
        db = null;
        // Invalidate the cursors before letting go of the connection. Each holds a SQLiteQuery
        // that keeps the old connection alive, and its wrapper still reports itself open, so
        // after the swap it would either read the file that is about to be replaced or fail with
        // an unchecked error from a closed pool. close() does this; this path bypassed it.
        AndroidCursor[] migrating = openCursors.toArray(new AndroidCursor[openCursors.size()]);
        openCursors.clear();
        for (int iter = 0; iter < migrating.length; iter++) {
            migrating[iter].invalidate();
        }
        closing.close();
        File original = new File(path);
        // Same reasoning as the target: created, not named, so nothing pre-existing is disturbed.
        File backup;
        try {
            backup = File.createTempFile(tempPrefix(name), ".backup", dir);
            // Rewritten, now naming both files: the original is about to move aside, and the
            // export still has to be cleaned up if the swap does not complete.
            AndroidImplementation.writeDatabaseMigrationMarker(path, backup, target);
        } catch (IOException err) {
            String surviving = discardExport(target);
            File unusedMarker = AndroidImplementation.databaseMigrationMarker(path);
            if (unusedMarker != null) {
                unusedMarker.delete();
            }
            db = openAt(path, currentKey);
            throw new IOException("The conversion could not be marked as in progress, so it was "
                    + "not started: " + err.getMessage() + surviving, err);
        }
        File marker = AndroidImplementation.databaseMigrationMarker(path);
        // Move the original aside rather than deleting it. Deleting first leaves a window where
        // the only copy of the data is the converted file under a name nothing looks for: a
        // process kill there strands it, the next open creates an empty database in its place,
        // and the migration after that removes the stranded copy as stale leftovers. Renaming
        // means there is a complete database under one of the two names at every instant.
        if (!original.renameTo(backup)) {
            String surviving = discardExport(target);
            marker.delete();
            db = openAt(path, currentKey);
            throw new IOException("The database " + path + " could not be moved aside, so it was "
                    + "left as it was and not converted." + surviving);
        }
        if (!target.renameTo(original)) {
            if (!backup.renameTo(original)) {
                // The marker stays: the backup still holds the data and recovery has to find it.
                // Do not reopen. openAt creates what it cannot find, so it would put an empty
                // database at the live name, and the next recovery would then see both files,
                // read that as a completed conversion, and delete the backup holding the data.
                throw new IOException("The converted database could not replace " + path
                        + " and the original could not be put back either. The original is intact "
                        + "at " + backup + " and the database was left closed rather than opening "
                        + "an empty one over it.");
            }
            String surviving = discardExport(target);
            marker.delete();
            db = openAt(path, currentKey);
            throw new IOException("The converted database could not replace " + path
                    + ", so the original was restored and nothing was converted." + surviving);
        }
        // Opened before the backup is dropped, and the backup put back if it will not open. The
        // converted file is installed at the live path by now, so a failure here would otherwise
        // leave both files in place with the marker still naming the backup -- and the next open
        // reads that state as a completed conversion, deletes the backup, and retries the live
        // database that just refused to open. The last readable copy of the data would go with it.
        try {
            db = openAt(path, targetKey);
        } catch (IOException cannotOpenConverted) {
            File converted = new File(path);
            if (converted.delete() && backup.renameTo(converted)) {
                marker.delete();
                try {
                    db = openAt(path, currentKey);
                } catch (IOException cannotReopenOriginal) {
                    throw new IOException("The converted database could not be opened, and "
                            + "neither could the original after it was put back: "
                            + cannotReopenOriginal.getMessage(), cannotReopenOriginal);
                }
                throw new IOException("The converted database could not be opened, so the "
                        + "original was put back and nothing was converted: "
                        + cannotOpenConverted.getMessage(), cannotOpenConverted);
            }
            // The marker stays: the backup still holds the data and recovery has to find it. Do
            // not reopen, for the reason given where the two renames fail.
            throw new IOException("The converted database at " + path + " could not be opened and "
                    + "the original could not be put back either. The original is intact at "
                    + backup + " and the database was left closed rather than opening the one "
                    + "that will not open: " + cannotOpenConverted.getMessage(),
                    cannotOpenConverted);
        }
        currentKey = targetKey;
        // The backup is the database in its previous form. After an encrypt that means a
        // plaintext copy of what is now an encrypted database, sitting at a predictable name --
        // which defeats the encryption entirely, so its removal is checked rather than assumed.
        if (!backup.delete() && backup.exists()) {
            // Marker left in place on purpose: recovery has to come back and finish this.
            throw new IOException("The database was converted, but the copy of its previous form "
                    + "at " + backup + " could not be removed. Delete it before relying on this "
                    + "database being encrypted.");
        }
        marker.delete();
    }

    private SQLiteDatabase openAt(String path, String key) throws IOException {
        try {
            return SQLiteDatabase.openOrCreateDatabase(new File(path), key, null, null);
        } catch (RuntimeException err) {
            throw new IOException("The converted database could not be reopened: "
                    + err.getMessage(), err);
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        try {
            if (isLegacyBehavior()) {
                db.execSQL(sql);
                return;
            }
            String[] statements = SQLStatementSplitter.split(sql);
            for (int iter = 0; iter < statements.length; iter++) {
                db.execSQL(statements[iter]);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
        noteScriptTransactionControl(sql);
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        checkParameterCount(sql, params == null ? 0 : params.length);
        SQLiteStatement s = null;
        try {
            s = db.compileStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] == null) {
                        s.bindNull(i + 1);
                    } else {
                        s.bindString(i + 1, params[i]);
                    }
                }
            }
            s.execute();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    @Override
    public void execute(String sql, Object... params) throws IOException {
        if (params == null) {
            // Only a null array means "no parameters at all". An explicitly empty one is still a
            // parameterized call, so it goes down the path below and is held to both the
            // single-statement rule and the parameter count -- otherwise
            // execute("INSERT ... VALUES (?)", new Object[0]) would run with the slot unbound.
            execute(sql);
            return;
        }
        checkOpen();
        requireSingleStatement(sql);
        checkParameterCount(sql, params.length);
        SQLiteStatement s = null;
        try {
            s = db.compileStatement(sql);
            bind(s, params);
            s.execute();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        checkParameterCount(sql, params == null ? 0 : params.length);
        try {
            if (params != null && !isLegacyBehavior() && hasNull(params)) {
                // rawQuery binds through bindString, which rejects null outright rather than
                // storing SQL NULL. See AndroidDB for the full reasoning.
                return wrap(db.rawQueryWithFactory(new BlobBindingCursorFactory(params), sql,
                        null, null));
            }
            return wrap(db.rawQuery(sql, params));
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            if (params != null) {
                requireSingleStatement(sql);
            }
            return executeQuery(sql);
        }
        checkOpen();
        requireSingleStatement(sql);
        checkParameterCount(sql, params.length);
        if (isLegacyBehavior() && !hasBlob(params)) {
            // rawQuery can only carry text, which is what this port used to do to every query
            // argument whatever its type. A blob still goes through the factory even here: blob
            // query parameters used to throw on every port, so nothing can depend on that and
            // the compatibility switch deliberately does not cover it.
            return executeQuery(sql, coerceToText(params, "executeQuery"));
        }
        try {
            // Bind through a cursor factory rather than stringifying: the contract is that a
            // parameter binds by its runtime type, so a Long has to reach SQLite as INTEGER or
            // "SELECT ? = 42" and typeof(?) both answer wrongly.
            return wrap(db.rawQueryWithFactory(new BlobBindingCursorFactory(params), sql, null, null));
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return executeQuery(sql, (String[]) null);
    }

    private Cursor wrap(android.database.Cursor c) throws IOException {
        if (!isLegacyBehavior()) {
            // Eager, so malformed SQL is reported from executeQuery rather than from next().
            try {
                c.getCount();
            } catch (RuntimeException err) {
                // Never handed out and never registered, so this is the only chance to release it.
                try {
                    c.close();
                } catch (RuntimeException ignored) {
                    // The compile failure is the one worth reporting.
                }
                throw err;
            }
        }
        final AndroidCursor cursor = new AndroidCursor(c);
        cursor.setCloseListener(new AndroidCursor.CloseListener() {
            @Override
            public void cursorClosed(AndroidCursor closing) {
                openCursors.remove(closing);
            }
        });
        openCursors.add(cursor);
        return cursor;
    }

    private static boolean hasNull(Object[] params) {
        for (int iter = 0; iter < params.length; iter++) {
            if (params[iter] == null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBlob(Object[] params) {
        for (int iter = 0; iter < params.length; iter++) {
            if (params[iter] instanceof byte[]) {
                return true;
            }
        }
        return false;
    }

    private static void bind(SQLiteProgram s, Object[] params) {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int index = i + 1;
            if (p == null) {
                s.bindNull(index);
            } else if (p instanceof String) {
                s.bindString(index, (String) p);
            } else if (p instanceof byte[]) {
                s.bindBlob(index, (byte[]) p);
            } else if (p instanceof Double || p instanceof Float) {
                s.bindDouble(index, ((Number) p).doubleValue());
            } else if (p instanceof Long || p instanceof Integer || p instanceof Short
                    || p instanceof Byte) {
                s.bindLong(index, ((Number) p).longValue());
            } else if (p instanceof Boolean) {
                s.bindLong(index, ((Boolean) p).booleanValue() ? 1 : 0);
            } else {
                s.bindString(index, p.toString());
            }
        }
    }

    /** Binds typed parameters, including blobs, onto the query before the cursor reads it. */
    private static final class BlobBindingCursorFactory implements SQLiteDatabase.CursorFactory {
        private final Object[] params;

        BlobBindingCursorFactory(Object[] params) {
            this.params = params;
        }

        @Override
        public android.database.Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery,
                String editTable, SQLiteQuery query) {
            bind(query, params);
            return new SQLiteCursor(masterQuery, editTable, query);
        }
    }
}
