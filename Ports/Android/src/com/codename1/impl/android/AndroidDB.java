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

    public AndroidDB(SQLiteDatabase db) {
        this.db = db;
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
        db.beginTransaction();
    }

    @Override
    public void commitTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        db.setTransactionSuccessful();
        db.endTransaction();
        markTransactionEnded();
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        // Ending without setTransactionSuccessful is what rolls back.
        db.endTransaction();
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
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        try {
            if (isLegacyBehavior()) {
                db.execSQL(sql);
                return;
            }
            // execSQL rejects anything after the first statement, so split and run each: the
            // portable contract is that execute(String) runs a whole script.
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
        if (!hasBlob(params)) {
            return executeQuery(sql, coerceToText(params, "executeQuery"));
        }
        try {
            // rawQuery can only carry text arguments. Binding through a cursor factory is the
            // supported way to get a blob into a query, and is what androidx.sqlite does.
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
