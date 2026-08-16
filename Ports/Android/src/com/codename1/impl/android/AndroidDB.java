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
package com.codename1.impl.android;

import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteProgram;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteStatement;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.impl.SQLStatementSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Chen
 */
public class AndroidDB extends Database {

    private SQLiteDatabase db;

    /** Cursors created from this database, invalidated when it closes. */
    private final List<AndroidCursor> openCursors = new ArrayList<AndroidCursor>();

    /** The path this connection is open on, for the shared registry a conversion consults. */
    private final String openPath;

    /**
     * Wraps a connection whose slot in the registry the caller has already taken.
     *
     * <p>Package-private on purpose. It is the one entry point that does not reserve, because
     * {@code AndroidImplementation} reserves before it opens; handing it to code outside this
     * package would let a handle exist that no conversion can see, while {@code close()} still
     * gives back a slot it never took -- releasing somebody else's. Code outside the port uses
     * the one-argument constructor, which reserves.
     *
     * @param db the open connection
     * @param openPath the file it is open on, whose connection slot the caller already took
     */
    AndroidDB(SQLiteDatabase db, String openPath) {
        this.db = db;
        // The slot is taken by the implementation before the engine opens anything, so that a
        // conversion reading the count during the open has to see this connection. Registering
        // here instead would leave a gap the conversion could start inside.
        this.openPath = openPath;
    }

    /**
     * Wraps a connection somebody else opened, taking the file from the connection itself.
     *
     * <p>This is the signature that existed before the two-argument form, and a library compiled
     * against it would fail to load without it. It takes the connection slot rather than skipping
     * the tracking, so a database reached this way is visible to the check that refuses a key
     * change while something else holds the file.
     *
     * @param db the open connection
     */
    public AndroidDB(SQLiteDatabase db) {
        this.openPath = db == null ? null : db.getPath();
        if (openPath != null) {
            try {
                // The reservation, not the bare count: a conversion in progress is rewriting this
                // file and swapping it under a rename, so a handle taken now reads and writes a
                // file that is about to be replaced -- and anything it wrote goes with the copy
                // that is discarded. The count alone does not refuse that.
                AndroidImplementation.reserveDatabaseConnection(openPath);
            } catch (IOException midConversion) {
                // Carried rather than thrown: this signature predates the reservation and code
                // that calls it compiles without a catch. The connection is closed and nothing is
                // registered, so the object exists but every method on it reports the reason --
                // a handle that cannot be used cannot write to the file being converted.
                unusableReason = midConversion.getMessage();
                try {
                    db.close();
                } catch (RuntimeException alsoFailed) {
                    // The refusal is the reason worth reporting.
                }
                return;
            }
        }
        this.db = db;
    }


    /// Why this handle cannot be used, when it was refused rather than closed.
    ///
    /// Only the compatibility constructor sets it, and only when the file was mid-conversion.
    private String unusableReason;

    private void checkOpen() throws IOException {
        if (db == null) {
            throw new IOException(unusableReason != null
                    ? unusableReason
                    : "This database has been closed");
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
        } catch (Exception err) {
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
        } catch (Exception err) {
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
    /// Ends a transaction the wrapper does not know it is in.
    ///
    /// Behind the same comment the rollback recovery needs: the session layer classifies a
    /// statement by its first three characters, so a bare COMMIT or ROLLBACK is turned into its
    /// own endTransaction() and throws rather than reaching SQLite.
    private void endThroughSql(String keyword) {
        db.execSQL("/* not " + keyword + " to the statement classifier */ " + keyword);
    }

    /// Whether an end failed because the engine had already rolled the transaction back.
    ///
    /// The engine says so in words and nothing else reports it: SQLite has no way to ask whether
    /// a transaction is open, and the Android wrapper answers for its own bookkeeping rather than
    /// for the connection.
    private static boolean alreadyRolledBack(Exception err) {
        String message = err.getMessage();
        return message != null
                && message.toLowerCase().indexOf("no transaction is active") >= 0;
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
            // Ending without setTransactionSuccessful is what rolls back.
            db.endTransaction();
        } catch (Exception err) {
            // A statement with ON CONFLICT ROLLBACK has already rolled the engine back, and the
            // wrapper pops its own record before sending the end -- so this throws with neither
            // layer holding a transaction. Reporting it without clearing the flag left every
            // later begin and key change refused until the connection closed.
            if (!db.inTransaction()) {
                markTransactionEnded();
                if (alreadyRolledBack(err)) {
                    // Satisfied, not failed. Rolling back asks for the transaction to end with
                    // its work discarded, and the engine did exactly that as the statement
                    // failed. The caller only called this because isInTransaction() still said
                    // yes -- this port cannot ask the engine directly the way the others can --
                    // so raising here would punish them for a belief that was ours.
                    return;
                }
            }
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
            } catch (Exception ignored) {
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
            // In a finally: the handle is already gone from this wrapper, so a close that throws
            // would otherwise leave the slot taken with nothing left to give it back. A second
            // close() returns immediately, and the count stays high for the life of the process
            // -- refusing every delete of this database and telling every conversion that
            // somebody else still holds it.
            // Anything this connection attached goes with it: SQLite drops attachments when the
            // connection closes, so the registrations taken for them have to go at the same moment.
            noteConnectionClosed();
            AndroidImplementation.databaseConnectionClosed(openPath);
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        try {
            if (isLegacyBehavior()) {
                db.execSQL(sql);
                // Tracked here too. The legacy hint restores what this port used to run, not what
                // it used to know: an execute("BEGIN") opens a real transaction either way, and a
                // key change allowed over it copies uncommitted rows into the replacement.
                noteFirstStatementTransactionControl(sql);
                return;
            }
            // execSQL rejects anything after the first statement, so split and run each: the
            // portable contract is that execute(String) runs a whole script.
            String[] statements = SQLStatementSplitter.split(sql);
            for (int iter = 0; iter < statements.length; iter++) {
                db.execSQL(statements[iter]);
                // Recorded as each one succeeds, not once at the end: a script that throws
                // partway has already run everything before the statement that failed, and a
                // transaction it opened is still open.
                noteScriptTransactionControl(statements[iter]);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
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
                        // bindString rejects null, so this used to fail outright rather than
                        // storing SQL NULL.
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
                // storing SQL NULL. The Object[] overload already binds null correctly, so
                // without this the same query behaves differently depending only on the declared
                // type of the array it was handed.
                return wrap(db.rawQueryWithFactory(new BlobBindingCursorFactory(params), sql,
                        null, null), sql);
            }
            android.database.Cursor c = db.rawQuery(sql, params);
            return wrap(c, sql);
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
            // "SELECT ? = 42" and typeof(?) both answer wrongly. This is the route
            // androidx.sqlite uses, and it is the only one that carries a blob as well.
            android.database.Cursor c = db.rawQueryWithFactory(
                    new BlobBindingCursorFactory(params), sql, null, null);
            return wrap(c, sql);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        return executeQuery(sql, (String[]) null);
    }

    /// Reports malformed SQL from executeQuery rather than from the first next().
    ///
    /// By preparing the statement and throwing it away. Nothing here touches a cursor, and that
    /// is the point: every navigation method on the platform cursor goes through
    /// AbstractCursor.moveToPosition, which asks getCount() before it moves, and SQLiteCursor
    /// answers that by filling its window with countAllRows set -- a walk of every matching row
    /// before executeQuery returns. moveToFirst() is not cheaper than getCount(); it is
    /// getCount() with a move on the end. A prepare compiles the statement against the schema,
    /// which is the whole of what this check is for, and steps nothing: no rows are read, and a
    /// statement that writes does not write.
    ///
    /// compileStatement only compiles here; it is never executed. The platform documents its
    /// execute methods as rejecting statements that return rows, and that restriction is on
    /// executing, not on compiling -- a SELECT prepares perfectly well, which is all this needs.
    ///
    /// Deliberately not left to rawQuery, which in the current platform sources prepares in the
    /// SQLiteProgram constructor and would raise the same error on its own. That is an
    /// implementation detail of one Android version; the contract that malformed SQL arrives from
    /// executeQuery is ours, so it is enforced here rather than inherited. The cost is one extra
    /// prepare per query, against a row scan that this replaces.
    ///
    /// Not reached for transaction control -- requireQueryStatement refuses that first -- which
    /// matters because the platform's statement classifier skips the prepare for BEGIN, COMMIT
    /// and ROLLBACK, so this would validate nothing for exactly those.
    ///
    /// Skipped under the legacy hint, where an error surfacing from next() is the behaviour an
    /// application was written against.
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
        // Nothing is read from the cursor here. It is handed back exactly as the platform made
        // it: unexecuted, before its first row. The query runs when the caller first asks for
        // data, which is where an unwrapped Android application would run it too. That first
        // access does count the whole result set -- the platform fills its first window with
        // countAllRows set -- and this port neither adds to that nor can remove it.
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

    /** Binds by runtime type onto any SQLiteProgram, which covers both statements and queries. */
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
