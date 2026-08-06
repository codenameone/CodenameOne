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
import com.codename1.impl.android.AndroidCursor;
import com.codename1.impl.SQLStatementSplitter;

import net.zetetic.database.sqlcipher.SQLiteCursor;
import net.zetetic.database.sqlcipher.SQLiteCursorDriver;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteProgram;
import net.zetetic.database.sqlcipher.SQLiteQuery;
import net.zetetic.database.sqlcipher.SQLiteStatement;

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

    AndroidCipherDB(SQLiteDatabase db, String databaseName) {
        this.db = db;
        this.databaseName = databaseName;
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
        int declared = SQLStatementSplitter.countPositionalParameters(sql);
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
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        try {
            if (config == null || !config.isEncrypted()) {
                db.execSQL("PRAGMA rekey = ''");
            } else {
                // Quote through the shared helper: a passphrase may contain quotes, and
                // interpolating it directly would let one change the statement.
                db.execSQL("PRAGMA rekey = "
                        + toPragmaLiteral(config.resolveKeyMaterial(databaseName)));
            }
        } catch (SQLiteException err) {
            throw new IOException(err.getMessage(), err);
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
        if (params == null || params.length == 0) {
            // An explicitly empty array is still a parameterized call, so it is held to the
            // single-statement rule; only a null array means "no parameters at all".
            if (params != null) {
                requireSingleStatement(sql);
            }
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
        if (isLegacyBehavior()) {
            // rawQuery can only carry text, which is what this port used to do to every query
            // argument whatever its type.
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
            c.getCount();
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
