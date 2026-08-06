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
            // reports it as an unchecked SQLiteException; the contract for this API is that every
            // failure is an IOException, and the transaction stays open so a rollback can recover.
            db.endTransaction();
        } catch (RuntimeException err) {
            throw new IOException(err.getMessage(), err);
        }
        markTransactionEnded();
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
        if (!hasBlob(params)) {
            return executeQuery(sql, coerceToText(params, "executeQuery"));
        }
        try {
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
