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

    /** The path this connection is open on, for the shared registry a conversion consults. */
    private final String openPath;

    /** Whether this connection still holds its slot in that registry. */
    private boolean slotHeld = true;

    AndroidCipherDB(SQLiteDatabase db, String databaseName, String key) {
        this.db = db;
        this.databaseName = databaseName;
        this.currentKey = key == null ? "" : key;
        // The slot was taken by AndroidImplementation before this connection was opened, so that
        // a conversion reading the count during the open has to see it. Registering here would
        // leave a gap the conversion could start inside.
        this.openPath = db.getPath();
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
    protected boolean supportsNestedTransactions() {
        // The platform wrapper ref-counts begins and ends, which is what the legacy behaviour on
        // this port relied on. No other engine here does.
        return true;
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
            if (!db.inTransaction()) {
                // A SAVEPOINT opened this one, and the wrapper's transaction stack knows nothing
                // about it, so setTransactionSuccessful() would throw that there is none. SQLite
                // ends a savepoint-started transaction with an ordinary COMMIT.
                endThroughSql("COMMIT");
                markTransactionEnded();
                return;
            }
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
    /// Ends a transaction the wrapper does not know it is in.
    ///
    /// Behind the same comment the rollback recovery needs: the session layer classifies a
    /// statement by its first three characters, so a bare COMMIT or ROLLBACK is turned into its
    /// own endTransaction() and throws rather than reaching SQLite.
    private void endThroughSql(String keyword) {
        db.execSQL("/* not " + keyword + " to the statement classifier */ " + keyword);
    }

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
            if (!db.inTransaction()) {
                // See commitTransaction(): a savepoint opened this, so it ends in SQL.
                endThroughSql("ROLLBACK");
                markTransactionEnded();
                return;
            }
            db.endTransaction();
        } catch (RuntimeException err) {
            // A statement with ON CONFLICT ROLLBACK has already rolled the engine back, and the
            // wrapper pops its own record before sending the end -- so this throws with neither
            // layer holding a transaction. Reporting it without clearing the flag left every
            // later begin and key change refused until the connection closed.
            if (!db.inTransaction()) {
                markTransactionEnded();
                if (alreadyRolledBack(err)) {
                    // Satisfied, not failed -- the same rule as the plaintext port. Recovery must
                    // not depend on whether the database is encrypted.
                    return;
                }
            }
            throw new IOException(err.getMessage(), err);
        }
        markTransactionEnded();
    }

    /// Whether an end failed because the engine had already rolled the transaction back.
    private static boolean alreadyRolledBack(RuntimeException err) {
        String message = err.getMessage();
        return message != null
                && message.toLowerCase().indexOf("no transaction is active") >= 0;
    }

    @Override
    public void close() throws IOException {
        if (db == null) {
            // A conversion that could neither restore nor validate the live file leaves the handle
            // closed on purpose, and the caller still calls close(). Returning here without giving
            // the slot back left a connection in the registry that nothing could ever release, so
            // every later conversion of that database refused until the process restarted.
            releaseConnectionSlot();
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
        try {
            closing.close();
        } finally {
            // In a finally, for the reason given on the plaintext port: the handle has already
            // been cleared, so a close that throws would strand the slot for the life of the
            // process.
            releaseConnectionSlot();
        }
    }

    /** Gives the connection slot back, once. */
    private void releaseConnectionSlot() {
        if (slotHeld) {
            slotHeld = false;
            // Anything this connection attached goes with it: SQLite drops attachments when the
            // connection closes, so the registrations taken for them have to go at the same moment.
            noteConnectionClosed();
            AndroidImplementation.databaseConnectionClosed(openPath);
        }
    }

    /**
     * Changes the key of this database.
     *
     * <p>Converting to or from plaintext cannot be done in place on this engine -- SQLCipher's
     * own instruction is to export into a second database -- so the connection the caller holds
     * is closed and reopened on the converted file. The settings SQLite will report are carried
     * across: foreign key enforcement, recursive triggers and the busy timeout, which between
     * them decide whether statements are refused, whether triggers recurse, and whether a
     * contended write waits or fails.
     *
     * <p>The list is derived from the engine -- every pragma SQLite reports, set and read back
     * from a fresh connection to see which ones a reopen loses -- rather than written from
     * memory, so it covers the whole class rather than the names somebody happened to think of.
     *
     * <p>One setting still cannot be carried: {@code case_sensitive_like}, which SQLite offers
     * no way to read. An application that sets it has to set it again after this returns. Every
     * other port re-keys in place and keeps the whole connection, so this is the one platform
     * where any of this applies.
     */
    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        checkNoTransactionForKeyChange();
        // The file this connection holds, as the open path does: an implicit managed key is
        // stored under what is passed here, so re-keying under the raw name would write a second
        // key that the next open, which resolves the file, would not find -- and report as wrong.
        String targetKey = config == null || !config.isEncrypted()
                ? "" : config.resolveKeyMaterial(
                        AndroidImplementation.canonicalDatabaseKey(openPath));
        if (currentKey.length() == 0 || targetKey.length() == 0) {
            // One side is plaintext, which rekey refuses outright.
            migrateThroughExport(targetKey);
            return;
        }
        // Claimed like the export route, and for the same reason in a different shape. This one
        // does not replace the file, but it does change the key the file is written with, and it
        // changes it only for this connection: another handle on the same database keeps the old
        // key and every read it makes afterwards fails with "file is not a database". There is no
        // way to rotate a key underneath another connection either, so the caller is told.
        String path = db.getPath();
        try {
            AndroidImplementation.beginDatabaseMigration(path);
        } catch (IOException openElsewhere) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    openElsewhere.getMessage(), openElsewhere);
        }
        try {
            rekeyInPlace(targetKey);
        } finally {
            AndroidImplementation.endDatabaseMigration(path);
        }
    }

    /** The key rotation itself, with this database claimed for the duration. */
    private void rekeyInPlace(String targetKey) throws IOException {
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
     * versioning or file identification would otherwise silently come back at zero. auto_vacuum
     * is carried too, and before the export rather than after it: SQLite fixes that one in the
     * header when the first table is created, so a database that vacuumed itself would come back
     * never doing so again, retaining every page it deleted. journal_mode goes back afterwards,
     * because WAL cannot be entered from inside the transaction the export runs in.
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
     * The connection-scoped settings to put back on the connection this conversion replaces.
     *
     * <p>Every other port re-keys in place, so their connection survives the operation with
     * whatever the application had configured on it. This one cannot: SQLCipher converts by
     * exporting into a second database and swapping the files, which means the connection the
     * caller holds is closed and a new one opened underneath it. A new SQLite connection starts
     * at the defaults, so a caller that had switched foreign key enforcement on carried on
     * inserting afterwards with it silently off, writing rows the constraint existed to refuse.
     *
     * <p>Null until a conversion captures them.
     */
    private java.util.List<String> connectionSettings;

    /**
     * Every connection-scoped pragma this can read back and put on the replacement connection.
     *
     * <p>Derived from the engine rather than written from memory. Each name in PRAGMA
     * pragma_list was read, set to a different value, read back, and then read again from a
     * fresh connection; the ones here are those that reported a value, kept it, and were back
     * at their default in the next connection -- which is precisely what a conversion does to
     * them when it swaps the file and reopens. An earlier version of this list was hand
     * written, and hand written is how it came to be missing query_only and locking_mode.
     *
     * <p>Deliberately absent: the pragmas that are questions rather than settings
     * (integrity_check, table_info and the rest); the ones that live in the file and are carried
     * by the migration itself (journal_mode, auto_vacuum, user_version, application_id);
     * defer_foreign_keys, which SQLite clears at every commit and rollback; and the key pragmas,
     * which the conversion is the one deciding.
     *
     * <p>case_sensitive_like is the one setting that genuinely cannot be carried: SQLite offers
     * no way to read it. ignore_check_constraints was in that category in an earlier note here
     * and should not have been -- it reports a value, so it is in the list.
     */
    private static final String[] CONNECTION_PRAGMAS = {
        "analysis_limit",
        "automatic_index",
        "busy_timeout",
        "cache_size",
        "cache_spill",
        "cell_size_check",
        "checkpoint_fullfsync",
        "count_changes",
        "empty_result_callbacks",
        "foreign_keys",
        "full_column_names",
        "fullfsync",
        "ignore_check_constraints",
        "journal_size_limit",
        "legacy_alter_table",
        "locking_mode",
        "max_page_count",
        "mmap_size",
        "query_only",
        "read_uncommitted",
        "recursive_triggers",
        "reverse_unordered_selects",
        "secure_delete",
        "short_column_names",
        "soft_heap_limit",
        "synchronous",
        "temp_store",
        "threads",
        "trusted_schema",
        "wal_autocheckpoint",
    };

    /**
     * Reads back the connection-scoped settings this can restore.
     *
     * <p>Only the ones SQLite will report, and only when it actually reports them: a pragma this
     * build does not implement answers with no row at all, and reading that as zero would have
     * this SET it to zero on the new connection -- turning a pragma that defaults to on, like
     * {@code checkpoint_fullfsync}, off as a side effect of a key change. So a value that is not
     * there is left alone.
     *
     * <p>A pragma with no getter cannot be captured by anything short of the caller telling us.
     * {@code case_sensitive_like} is the only one of those left, and it is named on changeKey
     * rather than pretended about here.
     */
    private java.util.List<String> captureConnectionSettings() {
        java.util.List<String> settings = new java.util.ArrayList<String>();
        for (String pragma : CONNECTION_PRAGMAS) {
            String value = readOptionalPragma(pragma);
            if (value != null) {
                settings.add("PRAGMA " + pragma + " = " + value);
            }
        }
        return settings;
    }

    /**
     * One pragma's value, or null when this build does not answer for it.
     *
     * @param pragma the pragma name
     * @return the value as SQLite reported it, or null
     */
    private String readOptionalPragma(String pragma) {
        try {
            android.database.Cursor c = db.rawQuery("PRAGMA " + pragma, null);
            try {
                if (!c.moveToFirst() || c.getColumnCount() < 1) {
                    return null;
                }
                String value = c.getString(0);
                // Restored as a number where SQLite gave a number, and not quoted either way:
                // every pragma in the list above takes a bare token, and a value that is not one
                // is a value this does not understand well enough to put back.
                return value != null && value.length() > 0 && isBarePragmaValue(value)
                        ? value : null;
            } finally {
                c.close();
            }
        } catch (RuntimeException unsupported) {
            return null;
        }
    }

    /** Whether a reported pragma value is a bare token that can go straight back into SQL. */
    private static boolean isBarePragmaValue(String value) {
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || c == '-' || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Runs a PRAGMA on a connection, through a cursor.
     *
     * <p>Some pragma assignments answer with a row and some do not -- {@code journal_mode} and
     * {@code busy_timeout} report the value they settled on, while {@code auto_vacuum},
     * {@code user_version} and the two boolean ones say nothing -- and the Android API refuses
     * the first kind through execSQL with "Queries can be performed using SQLiteDatabase query
     * or rawQuery methods only". Which pragma falls on which side is not something a reader
     * should have to remember, so they all go through here. rawQuery is lazy, so the statement
     * runs when the cursor is stepped; that step is the point of moveToFirst, not the row.
     *
     * @param target the connection to run it on
     * @param pragma the complete PRAGMA statement
     */
    private void runPragma(SQLiteDatabase target, String pragma) {
        android.database.Cursor c = target.rawQuery(pragma, null);
        try {
            c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /** Puts the captured settings back on a connection that has just replaced the old one. */
    private void applyConnectionSettings(SQLiteDatabase opened) {
        if (connectionSettings == null) {
            return;
        }
        for (String pragma : connectionSettings) {
            try {
                runPragma(opened, pragma);
            } catch (RuntimeException rejected) {
                // One setting that will not go back is not a reason to abandon a conversion that
                // has already succeeded, and it is not silent either: the value is readable by
                // the caller, which is more than it had before.
            }
        }
    }

    /**
     * The journal mode of the database being converted, or null when it is not one this can
     * safely put back.
     *
     * <p>A name, not a number, and it goes into the SQL text of a PRAGMA -- so it is matched
     * against the modes SQLite defines rather than passed through. A value this does not
     * recognize means the mode is left at the target's default, which is worth strictly more
     * than interpolating whatever came back into a statement.
     *
     * <p>"memory" and "off" are deliberately not restored: both trade durability for speed on a
     * database whose contents this method is in the middle of copying, and neither is a state a
     * conversion should quietly install.
     */
    private String readJournalMode() {
        android.database.Cursor c = db.rawQuery("PRAGMA journal_mode", null);
        String mode;
        try {
            mode = c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
        if (mode == null) {
            return null;
        }
        String lower = mode.toLowerCase();
        if ("delete".equals(lower) || "truncate".equals(lower) || "persist".equals(lower)
                || "wal".equals(lower)) {
            return lower;
        }
        return null;
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
        // Claimed rather than merely counted. The swap renames a new file over this one, and
        // Android lets that succeed while another connection still holds the old file open --
        // that connection keeps writing to a file that is no longer the database, is told each
        // write succeeded, and loses the lot when the backup is deleted. Its WAL is the same
        // story. Reading a count and then converting leaves room for a connection to arrive in
        // between, so the count and the claim are taken together under one lock and every open
        // that arrives afterwards is refused until this returns.
        try {
            AndroidImplementation.beginDatabaseMigration(path);
        } catch (IOException openElsewhere) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    openElsewhere.getMessage(), openElsewhere);
        }
        try {
            migrateThroughExportExclusively(targetKey, path);
        } finally {
            AndroidImplementation.endDatabaseMigration(path);
        }
    }

    /** The conversion itself, with this database claimed for the duration. */
    private void migrateThroughExportExclusively(String targetKey, String path) throws IOException {
        // Before anything is exported or swapped, while this is still the connection the caller
        // configured.
        connectionSettings = captureConnectionSettings();
        // Off for the duration, and put back by whichever connection this ends on. The export
        // writes into the attached target, and SQLite refuses a write to any attached database
        // while query_only is set -- so a caller that had made its handle read-only could not
        // convert at all: the export failed with "attempt to write a readonly database" and the
        // key change reported that rather than doing it. The captured value goes back on the
        // replacement connection through openAt, and on this one in the finally below if the
        // conversion never gets that far.
        boolean readOnlyHandle = wasQueryOnly();
        if (readOnlyHandle) {
            runPragma(db, "PRAGMA query_only = 0");
        }
        try {
            migrateThroughExportBody(targetKey, path);
        } finally {
            if (readOnlyHandle) {
                // Only when this is still the connection that was made writable. A conversion
                // that succeeded replaced it, and that one already had every captured setting
                // put back by openAt -- setting it again here would be harmless, but reading
                // "db" as the original when it is not is the kind of thing that stops being
                // harmless later.
                try {
                    runPragma(db, "PRAGMA query_only = 1");
                } catch (RuntimeException alreadyGone) {
                    // The connection this was toggling has been replaced or closed, which is
                    // the successful path: the new one carries the setting already.
                }
            }
        }
    }

    /// Whether the connection this conversion starts on was made read-only by its caller.
    private boolean wasQueryOnly() {
        return connectionSettings != null && connectionSettings.contains("PRAGMA query_only = 1");
    }

    private void migrateThroughExportBody(String targetKey, String path) throws IOException {
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
            int autoVacuum = readHeaderPragma("PRAGMA auto_vacuum");
            String journalMode = readJournalMode();
            db.execSQL("ATTACH DATABASE " + toPragmaLiteral(target.getPath())
                    + " AS cn1migrate KEY " + toPragmaLiteral(targetKey));
            // Before the export, unlike the two pragmas below, because this one cannot be set
            // afterwards: SQLite fixes auto_vacuum in the header when the first table is created
            // and a later PRAGMA is accepted and ignored. The target is created NONE by default,
            // so a database converted without this comes back with incremental vacuum silently
            // stopped, or -- worse for a FULL database -- holding on to every page it deletes
            // from then on. A conversion is not supposed to change how the database behaves.
            if (autoVacuum != 0) {
                runPragma(db, "PRAGMA cn1migrate.auto_vacuum = " + autoVacuum);
            }
            android.database.Cursor exported = db.rawQuery("SELECT sqlcipher_export('cn1migrate')",
                    null);
            try {
                exported.moveToFirst();
            } finally {
                exported.close();
            }
            runPragma(db, "PRAGMA cn1migrate.user_version = " + userVersion);
            runPragma(db, "PRAGMA cn1migrate.application_id = " + applicationId);
            if (journalMode != null) {
                // After the export, unlike auto_vacuum: the journal mode is settable at any point
                // in a database's life, and WAL in particular cannot be entered from inside a
                // transaction, which is where the export runs. Persistent in the header like the
                // others, and not carried by sqlcipher_export -- so a database an application put
                // into WAL once came back in DELETE mode after a key change, losing the reader
                // and writer concurrency it was relying on and taking new lock failures with it.
                runPragma(db, "PRAGMA cn1migrate.journal_mode = " + journalMode);
            }
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
        try {
            closing.close();
        } catch (RuntimeException cannotClose) {
            // The one step between writing the export and installing it that was left unguarded,
            // and the most expensive one to leave: at this point the export is a complete copy of
            // the database -- in plaintext whenever this conversion is a decrypt -- and the marker
            // names it. Failing out of here left both in the migration directory with db already
            // null, so the caller's close() released the connection slot and nothing else, and
            // that copy stayed on disk until something happened to open this database again and
            // run recovery. It could be never.
            //
            // Discarded here instead, through the same path a failed export uses, which unlinks
            // the export and its working files or empties them when it cannot.
            String surviving = discardExport(target);
            File unusedMarker = AndroidImplementation.databaseMigrationMarker(path);
            if (unusedMarker != null) {
                unusedMarker.delete();
            }
            // Deliberately not reopened, unlike the failures below it. Those leave a database that
            // was never touched, so handing the caller a working connection again is right. Here
            // the close itself failed: what became of the connection is exactly what is not known,
            // and opening a second one on the same file would leave the first alive and the
            // registry counting one where there are two. The object reports itself closed, which
            // is the honest answer, and the database on disk is unchanged.
            throw new IOException("The database could not be closed, so the converted copy was not "
                    + "installed and the database was left as it was: " + cannotClose.getMessage()
                    + surviving, cannotClose);
        }
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
        // Recorded before the install, not after it fails to open. A process that dies between
        // the rename below and the reopen after it leaves both files present with nothing thrown
        // and nobody to record anything -- and recovery reads a live file plus a backup as a
        // completed conversion and deletes the backup. If the installed file is unreadable or was
        // never durably flushed, that was the last usable copy. Written first, the state on disk
        // says "installed but not yet proven" for the whole window, and recovery puts the backup
        // back instead.
        try {
            // The export stays named while it is still under its own name: recovery cleans up an
            // export it can find, and a conversion interrupted here leaves a complete copy of the
            // database behind -- after a decryption, a plaintext one.
            AndroidImplementation.markDatabaseMigrationUnvalidated(path, backup, target);
        } catch (IOException cannotRecord) {
            String surviving = discardExport(target);
            if (backup.renameTo(original)) {
                marker.delete();
                db = openAt(path, currentKey);
                throw new IOException("The conversion could not be marked as unproven, so it was "
                        + "not installed and the original was put back: "
                        + cannotRecord.getMessage() + surviving, cannotRecord);
            }
            throw new IOException("The conversion could not be marked as unproven and the "
                    + "original could not be put back either. The original is intact at " + backup
                    + " -- do not delete it: " + cannotRecord.getMessage() + surviving,
                    cannotRecord);
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
            // Nothing to record here: the marker has said the installed file was unproven since
            // before it was installed, which is what makes recovery put the backup back.
            // Do not reopen, for the reason given where the two renames fail.
            throw new IOException("The converted database at " + path + " could not be opened and "
                    + "the original could not be put back either. The original is intact at "
                    + backup + " and will be restored on the next open; the database was left "
                    + "closed rather than opening the one that will not open: "
                    + cannotOpenConverted.getMessage(), cannotOpenConverted);
        }
        currentKey = targetKey;
        // Proven now, and said so before the backup is dropped. Left unproven, a process death
        // between here and the marker's removal would have recovery put the backup back and undo
        // a conversion that worked -- which after an encrypt means quietly restoring the
        // plaintext copy of a database the application was told is encrypted.
        try {
            AndroidImplementation.writeDatabaseMigrationMarker(path, backup, null);
        } catch (IOException cannotRecord) {
            // The conversion is done and the database is open under its new key, but the marker
            // still calls the installed file unproven, so the next open puts the original back.
            // Anything written through this handle before then goes with it, and a caller that
            // catches this and carries on has no way to know that. Closing it is the only honest
            // answer: writes that cannot be kept must not be accepted.
            SQLiteDatabase unproven = db;
            db = null;
            if (unproven != null) {
                try {
                    unproven.close();
                } catch (RuntimeException alsoFailed) {
                    // The recording failure is the one worth reporting.
                }
            }
            throw new IOException("The database was converted but the state could not be "
                    + "recorded, so the next open will restore the original from " + backup
                    + " and undo it. The converted database was closed rather than left open to "
                    + "accept writes that would go with it: " + cannotRecord.getMessage(),
                    cannotRecord);
        }
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
        SQLiteDatabase opened;
        try {
            opened = SQLiteDatabase.openOrCreateDatabase(new File(path), key, null, null);
        } catch (RuntimeException err) {
            throw new IOException("The converted database could not be reopened: "
                    + err.getMessage(), err);
        }
        applyConnectionSettings(opened);
        // Read something before calling this open a success, the way AndroidCipherFactory does.
        // SQLCipher applies the key lazily, so a file it cannot decrypt or cannot read opens
        // without complaint and fails at the first real query. Here that would be worse than
        // late: the caller treats a successful open as proof the conversion worked, records it,
        // and deletes the backup -- so the failure would surface from an ordinary application
        // query with the only readable copy already gone.
        try {
            android.database.Cursor probe =
                    opened.rawQuery("SELECT count(*) FROM sqlite_master", null);
            try {
                probe.moveToFirst();
            } finally {
                probe.close();
            }
        } catch (RuntimeException unreadable) {
            try {
                opened.close();
            } catch (RuntimeException alsoFailed) {
                // The read failure is the one worth reporting.
            }
            throw new IOException("The converted database at " + path + " opened but could not be "
                    + "read: " + unreadable.getMessage(), unreadable);
        }
        return opened;
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        // Before the engine runs it: an ATTACH of a database that is being deleted has to be
        // refused rather than undone, because undoing it can fail while the delete proceeds.
        reserveAttachments(sql);
        try {
            if (isLegacyBehavior()) {
                db.execSQL(sql);
                // Tracked here too. The legacy hint restores what this port used to run, not what
                // it used to know: an execute("BEGIN") opens a real transaction either way, and a
                // key change allowed over it copies uncommitted rows into the replacement.
                noteFirstStatementTransactionControl(sql);
                return;
            }
            String[] statements = SQLStatementSplitter.split(sql);
            for (int iter = 0; iter < statements.length; iter++) {
                db.execSQL(statements[iter]);
                // Recorded as each one succeeds; see AndroidDB.execute(String).
                noteScriptTransactionControl(statements[iter]);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        // Before the engine runs it, and with the parameters: an ATTACH names its
        // file in them, and a reservation taken afterwards cannot undo an attach.
        reserveAttachments(sql, params);
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
        // A parameterized call is a single statement, and "BEGIN" is a legal one: the guard on
        // changeKey depends on knowing that a transaction was opened, whichever entry point
        // opened it.
        noteScriptTransactionControl(sql);
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
        // Before the engine runs it, and with the parameters: an ATTACH names its file
        // in them, and a reservation taken afterwards cannot undo an attach.
        reserveAttachments(sql, params);
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
        // A parameterized call is a single statement, and "BEGIN" is a legal one: the guard on
        // changeKey depends on knowing that a transaction was opened, whichever entry point
        // opened it.
        noteScriptTransactionControl(sql);
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
        checkParameterCount(sql, params == null ? 0 : params.length);
        validateQuery(sql);
        try {
            if (params != null && !isLegacyBehavior() && hasNull(params)) {
                // rawQuery binds through bindString, which rejects null outright rather than
                // storing SQL NULL. See AndroidDB for the full reasoning.
                return wrap(db.rawQueryWithFactory(new BlobBindingCursorFactory(params), sql,
                        null, null), sql);
            }
            return wrap(db.rawQuery(sql, params), sql);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            if (params != null) {
                requireSingleStatement(sql);
                requireQueryStatement(sql);
            }
            return executeQuery(sql);
        }
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
        checkParameterCount(sql, params.length);
        validateQuery(sql);
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
            return wrap(db.rawQueryWithFactory(new BlobBindingCursorFactory(params), sql, null, null), sql);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return executeQuery(sql, (String[]) null);
    }

    /// Reports malformed SQL from executeQuery, without reading anything.
    ///
    /// The same as AndroidDB.validateQuery and for the same reasons, which are written out there:
    /// every move on a platform cursor asks getCount() first, so validating through the cursor
    /// walks the entire result set before executeQuery returns. A prepare compiles the statement
    /// and steps nothing.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the statement about to be handed to rawQuery
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the statement does not compile
    private void validateQuery(String sql) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        SQLiteStatement prepared = null;
        try {
            prepared = db.compileStatement(sql);
        } catch (RuntimeException doesNotCompile) {
            throw new IOException(doesNotCompile.getMessage(), doesNotCompile);
        } finally {
            if (prepared != null) {
                prepared.close();
            }
        }
    }

    private Cursor wrap(android.database.Cursor c, String sql) throws IOException {
        // Nothing is read from the cursor here; see AndroidDB.wrap. It is handed back as the
        // platform made it, unexecuted and before its first row.
        final AndroidCursor cursor = new AndroidCursor(c);
        // The platform cursor refills its window by running the query again, which for a
        // statement that writes repeats the writes. The cursor refuses to leave the window it
        // holds when that is what the statement does.
        //
        // Deliberately not gated by the compatibility flag. What the flag restores is behaviour an
        // application could depend on, and the behaviour here was a backward seek quietly running
        // an INSERT or an UPDATE a second time. Nothing can depend on that: the rows it wrote were
        // never asked for, and the caller had no way to see it happen.
        cursor.statementWrites(SQLStatementSplitter.writesData(sql));
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
