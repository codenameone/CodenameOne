/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package androidx.sqlite.db.framework;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteProgram;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;

/// A [SupportSQLiteDatabase] over the platform database. Queries bind their
/// arguments by type, through a cursor factory, as AndroidX does: the
/// platform's `rawQuery` takes strings only.
public final class FrameworkSQLiteDatabase implements SupportSQLiteDatabase {

    private static final String[] CONFLICT_VALUES = {"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ",
        " OR REPLACE "};
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final SQLiteDatabase mDelegate;

    public FrameworkSQLiteDatabase(SQLiteDatabase delegate) {
        mDelegate = delegate;
    }

    /// The platform database this one wraps.
    public SQLiteDatabase getDelegate() {
        return mDelegate;
    }

    @Override
    public SupportSQLiteStatement compileStatement(String sql) {
        return new FrameworkSQLiteStatement(mDelegate.compileStatement(sql));
    }

    @Override
    public void beginTransaction() {
        mDelegate.beginTransaction();
    }

    @Override
    public void beginTransactionNonExclusive() {
        mDelegate.beginTransactionNonExclusive();
    }

    @Override
    public void endTransaction() {
        mDelegate.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        mDelegate.setTransactionSuccessful();
    }

    @Override
    public boolean inTransaction() {
        return mDelegate.inTransaction();
    }

    @Override
    public boolean isDbLockedByCurrentThread() {
        return mDelegate.isDbLockedByCurrentThread();
    }

    @Override
    public boolean yieldIfContendedSafely() {
        return mDelegate.yieldIfContendedSafely();
    }

    @Override
    public int getVersion() {
        return mDelegate.getVersion();
    }

    @Override
    public void setVersion(int version) {
        mDelegate.setVersion(version);
    }

    @Override
    public long getMaximumSize() {
        return mDelegate.getMaximumSize();
    }

    @Override
    public long setMaximumSize(long numBytes) {
        return mDelegate.setMaximumSize(numBytes);
    }

    @Override
    public long getPageSize() {
        return mDelegate.getPageSize();
    }

    @Override
    public void setPageSize(long numBytes) {
        mDelegate.setPageSize(numBytes);
    }

    @Override
    public Cursor query(String query) {
        return query(new SimpleSQLiteQuery(query));
    }

    @Override
    public Cursor query(String query, Object[] bindArgs) {
        return query(new SimpleSQLiteQuery(query, bindArgs));
    }

    @Override
    public Cursor query(SupportSQLiteQuery supportQuery) {
        return mDelegate.rawQueryWithFactory(new TypedCursorFactory(supportQuery), supportQuery.getSql(),
                EMPTY_STRING_ARRAY, null);
    }

    @Override
    public long insert(String table, int conflictAlgorithm, ContentValues values) {
        return mDelegate.insertWithOnConflict(table, null, values, conflictAlgorithm);
    }

    @Override
    public int delete(String table, String whereClause, Object[] whereArgs) {
        String sql = "DELETE FROM " + table + (isEmpty(whereClause) ? "" : " WHERE " + whereClause);
        SupportSQLiteStatement statement = compileStatement(sql);
        try {
            SimpleSQLiteQuery.bind(statement, whereArgs);
            return statement.executeUpdateDelete();
        } finally {
            statement.close();
        }
    }

    @Override
    public int update(String table, int conflictAlgorithm, ContentValues values, String whereClause,
                      Object[] whereArgs) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("Empty values");
        }
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ").append(CONFLICT_VALUES[conflictAlgorithm]).append(table).append(" SET ");
        int setValuesSize = values.size();
        int bindArgsSize = whereArgs == null ? setValuesSize : setValuesSize + whereArgs.length;
        Object[] bindArgs = new Object[bindArgsSize];
        int i = 0;
        for (String colName : values.keySet()) {
            sql.append(i > 0 ? "," : "");
            sql.append(colName);
            bindArgs[i++] = values.get(colName);
            sql.append("=?");
        }
        if (whereArgs != null) {
            for (i = setValuesSize; i < bindArgsSize; i++) {
                bindArgs[i] = whereArgs[i - setValuesSize];
            }
        }
        if (!isEmpty(whereClause)) {
            sql.append(" WHERE ").append(whereClause);
        }
        SupportSQLiteStatement statement = compileStatement(sql.toString());
        try {
            SimpleSQLiteQuery.bind(statement, bindArgs);
            return statement.executeUpdateDelete();
        } finally {
            statement.close();
        }
    }

    @Override
    public void execSQL(String sql) {
        mDelegate.execSQL(sql);
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) {
        mDelegate.execSQL(sql, bindArgs);
    }

    @Override
    public boolean isReadOnly() {
        return mDelegate.isReadOnly();
    }

    @Override
    public boolean isOpen() {
        return mDelegate.isOpen();
    }

    @Override
    public boolean needUpgrade(int newVersion) {
        return mDelegate.needUpgrade(newVersion);
    }

    @Override
    public String getPath() {
        return mDelegate.getPath();
    }

    @Override
    public void setForeignKeyConstraintsEnabled(boolean enabled) {
        mDelegate.setForeignKeyConstraintsEnabled(enabled);
    }

    @Override
    public boolean enableWriteAheadLogging() {
        return mDelegate.enableWriteAheadLogging();
    }

    @Override
    public void disableWriteAheadLogging() {
        mDelegate.disableWriteAheadLogging();
    }

    @Override
    public boolean isWriteAheadLoggingEnabled() {
        return mDelegate.isWriteAheadLoggingEnabled();
    }

    @Override
    public boolean isDatabaseIntegrityOk() {
        return mDelegate.isDatabaseIntegrityOk();
    }

    @Override
    public void close() {
        mDelegate.close();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    /// Binds a support query's arguments, by type, before the cursor runs it.
    private static final class TypedCursorFactory implements SQLiteDatabase.CursorFactory {
        private final SupportSQLiteQuery mSupportQuery;

        TypedCursorFactory(SupportSQLiteQuery supportQuery) {
            mSupportQuery = supportQuery;
        }

        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable,
                                SQLiteQuery query) {
            mSupportQuery.bindTo(new QueryProgram(query));
            return new SQLiteCursor(masterQuery, editTable, query);
        }
    }

    /// Binds a support query's arguments into the platform query.
    private static final class QueryProgram implements SupportSQLiteProgram {
        private final SQLiteQuery mQuery;

        QueryProgram(SQLiteQuery query) {
            mQuery = query;
        }

        @Override
        public void bindNull(int index) {
            mQuery.bindNull(index);
        }

        @Override
        public void bindLong(int index, long value) {
            mQuery.bindLong(index, value);
        }

        @Override
        public void bindDouble(int index, double value) {
            mQuery.bindDouble(index, value);
        }

        @Override
        public void bindString(int index, String value) {
            mQuery.bindString(index, value);
        }

        @Override
        public void bindBlob(int index, byte[] value) {
            mQuery.bindBlob(index, value);
        }

        @Override
        public void clearBindings() {
            mQuery.clearBindings();
        }

        @Override
        public void close() {
            // The cursor owns the query.
        }
    }
}
