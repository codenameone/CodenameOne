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
package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.os.CancellationSignal;
import android.util.Log;

import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.db.RowExt;

import java.io.IOException;
import java.util.ArrayList;

/// An SQLite database, backed by a Codename One `Database`.
///
/// Transactions nest the way Android's do: only the outermost
/// `beginTransaction`/`endTransaction` pair reaches the engine, and it
/// commits only if every nested level was marked successful.
public final class SQLiteDatabase extends SQLiteClosable {

    private static final String TAG = "SQLiteDatabase";

    public static final int CONFLICT_ROLLBACK = 1;
    public static final int CONFLICT_ABORT = 2;
    public static final int CONFLICT_FAIL = 3;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_REPLACE = 5;
    public static final int CONFLICT_NONE = 0;

    private static final String[] CONFLICT_VALUES = {"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ",
        " OR REPLACE "};

    public static final int SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;
    public static final int OPEN_READWRITE = 0x00000000;
    public static final int OPEN_READONLY = 0x00000001;
    public static final int NO_LOCALIZED_COLLATORS = 0x00000010;
    public static final int CREATE_IF_NECESSARY = 0x10000000;
    public static final int ENABLE_WRITE_AHEAD_LOGGING = 0x20000000;
    public static final int MAX_SQL_CACHE_SIZE = 100;

    /// Creates the cursors a query returns; `null` means `SQLiteCursor`.
    public interface CursorFactory {
        Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query);
    }

    private static int memoryCounter;

    private final String mPath;
    private final int mOpenFlags;
    private final CursorFactory mCursorFactory;
    private final DatabaseErrorHandler mErrorHandler;
    private final boolean mDeleteOnClose;
    private Database mDb;
    private final ArrayList<Transaction> mTransactions = new ArrayList<Transaction>();

    private static final class Transaction {
        boolean markedSuccessful;
        boolean childFailed;
        SQLiteTransactionListener listener;
    }

    private SQLiteDatabase(String path, int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler,
                           boolean deleteOnClose) {
        mPath = path;
        mOpenFlags = openFlags;
        mCursorFactory = cursorFactory;
        mErrorHandler = errorHandler;
        mDeleteOnClose = deleteOnClose;
    }

    // ------------------------------------------------------------ opening

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags) {
        return openDatabase(path, factory, flags, null);
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags,
                                              DatabaseErrorHandler errorHandler) {
        return open(path, factory, flags, errorHandler, false);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory) {
        return openDatabase(path, factory, CREATE_IF_NECESSARY, null);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory,
                                                      DatabaseErrorHandler errorHandler) {
        return openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler);
    }

    /// An in-memory database. Codename One has no in-memory engine on
    /// every platform, so this is a uniquely named database that is
    /// deleted when it is closed.
    public static SQLiteDatabase create(CursorFactory factory) {
        memoryCounter++;
        String name = "cn1_android_memory_" + System.currentTimeMillis() + "_" + memoryCounter + ".db";
        return open(name, factory, CREATE_IF_NECESSARY, null, true);
    }

    private static SQLiteDatabase open(String path, CursorFactory factory, int flags,
                                       DatabaseErrorHandler errorHandler, boolean deleteOnClose) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if ((flags & CREATE_IF_NECESSARY) == 0 && !Database.exists(path)) {
            throw new SQLiteCantOpenDatabaseException("unable to open database file: " + path);
        }
        SQLiteDatabase db = new SQLiteDatabase(path, flags, factory, errorHandler, deleteOnClose);
        try {
            db.mDb = Database.openOrCreate(path);
        } catch (IOException e) {
            throw new SQLiteCantOpenDatabaseException("unable to open database file: " + path, e);
        }
        return db;
    }

    /// Wraps a Codename One database that is already open.
    static SQLiteDatabase wrap(Database database, String path, CursorFactory factory,
                               DatabaseErrorHandler errorHandler) {
        SQLiteDatabase db = new SQLiteDatabase(path, CREATE_IF_NECESSARY, factory, errorHandler, false);
        db.mDb = database;
        return db;
    }

    /// Deletes a database by the name it was opened with.
    public static boolean deleteDatabase(String path) {
        if (!Database.exists(path)) {
            return false;
        }
        try {
            Database.delete(path);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "deleting " + path + ": " + e);
            return false;
        }
    }

    public static int releaseMemory() {
        return 0;
    }

    // ------------------------------------------------------------ state

    public String getPath() {
        return mPath;
    }

    public boolean isOpen() {
        return mDb != null;
    }

    public boolean isReadOnly() {
        return (mOpenFlags & OPEN_READONLY) == OPEN_READONLY;
    }

    public boolean isInMemoryDatabase() {
        return mDeleteOnClose;
    }

    @Override
    protected void onAllReferencesReleased() {
        Database db = mDb;
        mDb = null;
        if (db == null) {
            return;
        }
        try {
            db.close();
        } catch (IOException e) {
            Log.w(TAG, "closing " + mPath + ": " + e);
        }
        if (mDeleteOnClose) {
            deleteDatabase(mPath);
        }
    }

    private Database db() {
        if (mDb == null) {
            throw new IllegalStateException("attempt to re-open an already-closed object: SQLiteDatabase: " + mPath);
        }
        return mDb;
    }

    public int getVersion() {
        return (int) DatabaseUtils.longForQuery(this, "PRAGMA user_version;", null);
    }

    public void setVersion(int version) {
        execSQL("PRAGMA user_version = " + version);
    }

    public boolean needUpgrade(int newVersion) {
        return newVersion > getVersion();
    }

    public long getMaximumSize() {
        long pageCount = DatabaseUtils.longForQuery(this, "PRAGMA max_page_count;", null);
        return pageCount * getPageSize();
    }

    public long setMaximumSize(long numBytes) {
        long pageSize = getPageSize();
        long numPages = numBytes / pageSize;
        if ((numBytes % pageSize) != 0) {
            numPages++;
        }
        long newPageCount = DatabaseUtils.longForQuery(this, "PRAGMA max_page_count = " + numPages, null);
        return newPageCount * pageSize;
    }

    public long getPageSize() {
        return DatabaseUtils.longForQuery(this, "PRAGMA page_size;", null);
    }

    public void setPageSize(long numBytes) {
        execSQL("PRAGMA page_size = " + numBytes);
    }

    public void setForeignKeyConstraintsEnabled(boolean enable) {
        execSQL("PRAGMA foreign_keys = " + (enable ? "ON" : "OFF"));
    }

    public void setMaxSqlCacheSize(int cacheSize) {
        if (cacheSize > MAX_SQL_CACHE_SIZE || cacheSize < 0) {
            throw new IllegalStateException("expected value between 0 and " + MAX_SQL_CACHE_SIZE);
        }
    }

    /// Write-ahead logging is the engine's choice on each platform; this
    /// reports it as not enabled.
    public boolean enableWriteAheadLogging() {
        return false;
    }

    public void disableWriteAheadLogging() {
    }

    public boolean isWriteAheadLoggingEnabled() {
        return false;
    }

    public boolean isDatabaseIntegrityOk() {
        String result = DatabaseUtils.stringForQuery(this, "PRAGMA integrity_check;", null);
        return "ok".equalsIgnoreCase(result);
    }

    public boolean isDbLockedByCurrentThread() {
        return !mTransactions.isEmpty();
    }

    @Deprecated
    public boolean isDbLockedByOtherThreads() {
        return false;
    }

    @Deprecated
    public boolean yieldIfContended() {
        return false;
    }

    public boolean yieldIfContendedSafely() {
        return false;
    }

    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        return false;
    }

    // ------------------------------------------------------------ transactions

    public void beginTransaction() {
        beginTransactionWithListener(null);
    }

    public void beginTransactionNonExclusive() {
        beginTransactionWithListener(null);
    }

    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener transactionListener) {
        beginTransactionWithListener(transactionListener);
    }

    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {
        Database d = db();
        if (mTransactions.isEmpty()) {
            try {
                d.beginTransaction();
            } catch (IOException e) {
                throw toSqlException(e, "BEGIN");
            }
        }
        Transaction t = new Transaction();
        t.listener = transactionListener;
        mTransactions.add(t);
        if (transactionListener != null) {
            try {
                transactionListener.onBegin();
            } catch (RuntimeException ex) {
                mTransactions.remove(mTransactions.size() - 1);
                if (mTransactions.isEmpty()) {
                    rollbackQuietly();
                }
                throw ex;
            }
        }
    }

    public void setTransactionSuccessful() {
        db();
        if (mTransactions.isEmpty()) {
            throw new IllegalStateException("Cannot perform this operation because there is no current transaction.");
        }
        Transaction top = mTransactions.get(mTransactions.size() - 1);
        if (top.markedSuccessful) {
            throw new IllegalStateException("Cannot perform this operation because the transaction has already "
                    + "been marked successful.  The only thing you can do now is call endTransaction().");
        }
        top.markedSuccessful = true;
    }

    public void endTransaction() {
        Database d = db();
        if (mTransactions.isEmpty()) {
            throw new IllegalStateException("Cannot perform this operation because there is no current transaction.");
        }
        Transaction top = mTransactions.remove(mTransactions.size() - 1);
        boolean successful = top.markedSuccessful && !top.childFailed;
        RuntimeException listenerException = null;
        if (top.listener != null) {
            try {
                if (successful) {
                    top.listener.onCommit();
                } else {
                    top.listener.onRollback();
                }
            } catch (RuntimeException ex) {
                listenerException = ex;
                successful = false;
            }
        }
        if (!mTransactions.isEmpty()) {
            if (!successful) {
                mTransactions.get(mTransactions.size() - 1).childFailed = true;
            }
        } else if (successful) {
            try {
                d.commitTransaction();
            } catch (IOException e) {
                throw toSqlException(e, "COMMIT");
            }
        } else {
            rollbackQuietly();
        }
        if (listenerException != null) {
            throw listenerException;
        }
    }

    private void rollbackQuietly() {
        try {
            db().rollbackTransaction();
        } catch (IOException e) {
            Log.w(TAG, "rollback failed: " + e);
        }
    }

    public boolean inTransaction() {
        return !mTransactions.isEmpty();
    }

    // ------------------------------------------------------------ statements

    public SQLiteStatement compileStatement(String sql) {
        db();
        return new SQLiteStatement(this, sql, null);
    }

    public void execSQL(String sql) {
        executeSql(sql, null);
    }

    public void execSQL(String sql, Object[] bindArgs) {
        if (bindArgs == null) {
            throw new IllegalArgumentException("Empty bindArgs");
        }
        executeSql(sql, bindArgs);
    }

    private void executeSql(String sql, Object[] bindArgs) {
        SQLiteStatement statement = new SQLiteStatement(this, sql, bindArgs);
        try {
            statement.execute();
        } finally {
            statement.close();
        }
    }

    // ------------------------------------------------------------ queries

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit) {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy,
                limit, null);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit,
                        CancellationSignal cancellationSignal) {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy,
                limit, cancellationSignal);
    }

    public Cursor queryWithFactory(CursorFactory cursorFactory, boolean distinct, String table, String[] columns,
                                   String selection, String[] selectionArgs, String groupBy, String having,
                                   String orderBy, String limit) {
        return queryWithFactory(cursorFactory, distinct, table, columns, selection, selectionArgs, groupBy, having,
                orderBy, limit, null);
    }

    public Cursor queryWithFactory(CursorFactory cursorFactory, boolean distinct, String table, String[] columns,
                                   String selection, String[] selectionArgs, String groupBy, String having,
                                   String orderBy, String limit, CancellationSignal cancellationSignal) {
        String sql = SQLiteQueryBuilder.buildQueryString(distinct, table, columns, selection, groupBy, having, orderBy,
                limit);
        return rawQueryWithFactory(cursorFactory, sql, selectionArgs, findEditTable(table), cancellationSignal);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy) {
        return query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, null);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) {
        return query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return rawQueryWithFactory(null, sql, selectionArgs, null, null);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs, CancellationSignal cancellationSignal) {
        return rawQueryWithFactory(null, sql, selectionArgs, null, cancellationSignal);
    }

    public Cursor rawQueryWithFactory(CursorFactory cursorFactory, String sql, String[] selectionArgs,
                                      String editTable) {
        return rawQueryWithFactory(cursorFactory, sql, selectionArgs, editTable, null);
    }

    public Cursor rawQueryWithFactory(CursorFactory cursorFactory, String sql, String[] selectionArgs,
                                      String editTable, CancellationSignal cancellationSignal) {
        db();
        SQLiteCursorDriver driver = new SQLiteDirectCursorDriver(this, sql, editTable, cancellationSignal);
        return driver.query(cursorFactory != null ? cursorFactory : mCursorFactory, selectionArgs);
    }

    /// The first table of a FROM list, which is what a cursor edits.
    public static String findEditTable(String tables) {
        if (tables == null || tables.length() == 0) {
            throw new IllegalStateException("Invalid tables");
        }
        int spacepos = tables.indexOf(' ');
        int commapos = tables.indexOf(',');
        if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
            return tables.substring(0, spacepos);
        } else if (commapos > 0 && (commapos < spacepos || spacepos < 0)) {
            return tables.substring(0, commapos);
        }
        return tables;
    }

    // ------------------------------------------------------------ writes

    public long insert(String table, String nullColumnHack, ContentValues values) {
        try {
            return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values, e);
            return -1;
        }
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues, CONFLICT_REPLACE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + initialValues, e);
            return -1;
        }
    }

    public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues) {
        return insertWithOnConflict(table, nullColumnHack, initialValues, CONFLICT_REPLACE);
    }

    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues,
                                     int conflictAlgorithm) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT");
        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');
        Object[] bindArgs = null;
        int size = (initialValues != null && !initialValues.isEmpty()) ? initialValues.size() : 0;
        if (size > 0) {
            bindArgs = new Object[size];
            int i = 0;
            for (String colName : initialValues.keySet()) {
                sql.append((i > 0) ? "," : "");
                sql.append(colName);
                bindArgs[i++] = initialValues.get(colName);
            }
            sql.append(')');
            sql.append(" VALUES (");
            for (i = 0; i < size; i++) {
                sql.append((i > 0) ? ",?" : "?");
            }
        } else {
            sql.append(nullColumnHack).append(") VALUES (NULL");
        }
        sql.append(')');
        SQLiteStatement statement = new SQLiteStatement(this, sql.toString(), bindArgs);
        try {
            return statement.executeInsert();
        } finally {
            statement.close();
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        SQLiteStatement statement = new SQLiteStatement(this, "DELETE FROM " + table
                + (whereClause != null && whereClause.length() > 0 ? " WHERE " + whereClause : ""), whereArgs);
        try {
            return statement.executeUpdateDelete();
        } finally {
            statement.close();
        }
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE);
    }

    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs,
                                    int conflictAlgorithm) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Empty values");
        }
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(table);
        sql.append(" SET ");
        int setValuesSize = values.size();
        int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
        Object[] bindArgs = new Object[bindArgsSize];
        int i = 0;
        for (String colName : values.keySet()) {
            sql.append((i > 0) ? "," : "");
            sql.append(colName);
            bindArgs[i++] = values.get(colName);
            sql.append("=?");
        }
        if (whereArgs != null) {
            for (i = setValuesSize; i < bindArgsSize; i++) {
                bindArgs[i] = whereArgs[i - setValuesSize];
            }
        }
        if (whereClause != null && whereClause.length() > 0) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }
        SQLiteStatement statement = new SQLiteStatement(this, sql.toString(), bindArgs);
        try {
            return statement.executeUpdateDelete();
        } finally {
            statement.close();
        }
    }

    // ------------------------------------------------------------ engine access

    /// Android binds longs, doubles, strings, blobs and null; anything else a
    /// caller passes is reduced to one of those the way Android's binder does.
    private static Object[] normalize(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a == null || a instanceof String || a instanceof byte[] || a instanceof Long || a instanceof Double) {
                out[i] = a;
            } else if (a instanceof Boolean) {
                out[i] = Long.valueOf(((Boolean) a).booleanValue() ? 1 : 0);
            } else if (a instanceof Float) {
                out[i] = Double.valueOf(((Float) a).doubleValue());
            } else if (a instanceof Number) {
                out[i] = Long.valueOf(((Number) a).longValue());
            } else {
                out[i] = a.toString();
            }
        }
        return out;
    }

    void runStatement(String sql, Object[] bindArgs) {
        Database d = db();
        Object[] args = normalize(bindArgs);
        try {
            if (args == null) {
                d.execute(sql);
            } else {
                d.execute(sql, args);
            }
        } catch (IOException e) {
            throw toSqlException(e, sql);
        }
    }

    com.codename1.db.Cursor runQuery(String sql, Object[] bindArgs) {
        Database d = db();
        Object[] args = normalize(bindArgs);
        try {
            return args == null ? d.executeQuery(sql) : d.executeQuery(sql, args);
        } catch (IOException e) {
            throw toSqlException(e, sql);
        }
    }

    /// The first column of the first row as text (`null` inside the array
    /// for SQL NULL), or null when the query returned no rows.
    String[] firstValue(String sql, Object[] bindArgs) {
        com.codename1.db.Cursor c = runQuery(sql, bindArgs);
        try {
            if (!c.next()) {
                return null;
            }
            Row row = c.getRow();
            String s = row.getString(0);
            if (row instanceof RowExt && ((RowExt) row).wasNull()) {
                s = null;
            }
            return new String[] {s};
        } catch (IOException e) {
            throw toSqlException(e, sql);
        } finally {
            try {
                c.close();
            } catch (IOException ignored) {
                // the value has been read; a failure to release it changes nothing for the caller
            }
        }
    }

    /// `changes()` and `last_insert_rowid()` for the statement just run.
    long[] changesAndLastRowId() {
        String sql = "SELECT changes(), last_insert_rowid()";
        com.codename1.db.Cursor c = runQuery(sql, null);
        try {
            if (!c.next()) {
                return new long[] {0, -1};
            }
            Row row = c.getRow();
            return new long[] {row.getLong(0), row.getLong(1)};
        } catch (IOException e) {
            throw toSqlException(e, sql);
        } finally {
            try {
                c.close();
            } catch (IOException ignored) {
                // the values have been read; a failure to release them changes nothing for the caller
            }
        }
    }

    /// Maps an engine error to the SQLiteException subclass Android would
    /// throw for it, from SQLite's own message wording.
    SQLiteException toSqlException(IOException e, String sql) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String full = msg + ", while executing: " + sql;
        if (containsIgnoreCase(msg, "constraint")) {
            return new SQLiteConstraintException(full, e);
        }
        if (containsIgnoreCase(msg, "malformed") || containsIgnoreCase(msg, "not a database")) {
            if (mErrorHandler != null) {
                mErrorHandler.onCorruption(this);
            }
            return new SQLiteDatabaseCorruptException(full, e);
        }
        if (containsIgnoreCase(msg, "readonly") || containsIgnoreCase(msg, "read-only")) {
            return new SQLiteReadOnlyDatabaseException(full, e);
        }
        if (containsIgnoreCase(msg, "database or disk is full")) {
            return new SQLiteFullException(full, e);
        }
        return new SQLiteException(full, e);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        int n = needle.length();
        for (int i = 0; i + n <= haystack.length(); i++) {
            if (haystack.regionMatches(true, i, needle, 0, n)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "SQLiteDatabase: " + mPath;
    }
}
