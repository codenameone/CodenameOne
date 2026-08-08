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
     * @param db the open connection
     * @param openPath the file it is open on, whose connection slot the caller already took
     */
    public AndroidDB(SQLiteDatabase db, String openPath) {
        this.db = db;
        // The slot is taken by the implementation before the engine opens anything, so that a
        // conversion reading the count during the open has to see this connection. Registering
        // here instead would leave a gap the conversion could start inside.
        this.openPath = openPath;
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
        closing.close();
        AndroidImplementation.databaseConnectionClosed(openPath);
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
        checkParameterCount(sql, params == null ? 0 : params.length);
        try {
            if (params != null && !isLegacyBehavior() && hasNull(params)) {
                // rawQuery binds through bindString, which rejects null outright rather than
                // storing SQL NULL. The Object[] overload already binds null correctly, so
                // without this the same query behaves differently depending only on the declared
                // type of the array it was handed.
                return wrap(db.rawQueryWithFactory(new BlobBindingCursorFactory(params), sql,
                        null, null));
            }
            android.database.Cursor c = db.rawQuery(sql, params);
            return wrap(c);
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
            // "SELECT ? = 42" and typeof(?) both answer wrongly. This is the route
            // androidx.sqlite uses, and it is the only one that carries a blob as well.
            android.database.Cursor c = db.rawQueryWithFactory(
                    new BlobBindingCursorFactory(params), sql, null, null);
            return wrap(c);
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
            // rawQuery is lazy: without forcing the window fill here, malformed SQL surfaces from
            // the first next() instead of from executeQuery.
            try {
                c.getCount();
            } catch (RuntimeException err) {
                // Compiling or filling failed, so this cursor is never handed out and never
                // registered - closing it here is the only chance to release the query and the
                // database reference it holds.
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
