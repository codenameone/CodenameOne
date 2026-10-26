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
package androidx.sqlite.db;

import android.content.ContentValues;
import android.database.Cursor;

/// The database a Room migration, callback or open helper works on.
public interface SupportSQLiteDatabase {

    SupportSQLiteStatement compileStatement(String sql);

    void beginTransaction();

    void beginTransactionNonExclusive();

    void endTransaction();

    void setTransactionSuccessful();

    boolean inTransaction();

    boolean isDbLockedByCurrentThread();

    boolean yieldIfContendedSafely();

    int getVersion();

    void setVersion(int version);

    long getMaximumSize();

    long setMaximumSize(long numBytes);

    long getPageSize();

    void setPageSize(long numBytes);

    Cursor query(String query);

    Cursor query(String query, Object[] bindArgs);

    Cursor query(SupportSQLiteQuery query);

    long insert(String table, int conflictAlgorithm, ContentValues values);

    int delete(String table, String whereClause, Object[] whereArgs);

    int update(String table, int conflictAlgorithm, ContentValues values, String whereClause, Object[] whereArgs);

    void execSQL(String sql);

    void execSQL(String sql, Object[] bindArgs);

    boolean isReadOnly();

    boolean isOpen();

    boolean needUpgrade(int newVersion);

    String getPath();

    void setForeignKeyConstraintsEnabled(boolean enabled);

    boolean enableWriteAheadLogging();

    void disableWriteAheadLogging();

    boolean isWriteAheadLoggingEnabled();

    boolean isDatabaseIntegrityOk();

    void close();
}
