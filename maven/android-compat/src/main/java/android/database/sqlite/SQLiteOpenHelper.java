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

import android.content.Context;
import android.database.DatabaseErrorHandler;

/// Creates a database on first use and upgrades or downgrades its schema
/// to the version the application asks for, inside one transaction.
public abstract class SQLiteOpenHelper implements AutoCloseable {

    private final Context mContext;
    private final String mName;
    private final SQLiteDatabase.CursorFactory mFactory;
    private final int mNewVersion;
    private final int mMinimumSupportedVersion;
    private final DatabaseErrorHandler mErrorHandler;
    private SQLiteDatabase mDatabase;
    private boolean mIsInitializing;

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        this(context, name, factory, version, null);
    }

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version,
                            DatabaseErrorHandler errorHandler) {
        this(context, name, factory, version, 0, errorHandler);
    }

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version,
                            int minimumSupportedVersion, DatabaseErrorHandler errorHandler) {
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1, was " + version);
        }
        mContext = context;
        mName = name;
        mFactory = factory;
        mNewVersion = version;
        mMinimumSupportedVersion = Math.max(0, minimumSupportedVersion);
        mErrorHandler = errorHandler;
    }

    public String getDatabaseName() {
        return mName;
    }

    public void setWriteAheadLoggingEnabled(boolean enabled) {
    }

    public void setLookasideConfig(int slotSize, int slotCount) {
    }

    public void setIdleConnectionTimeout(long idleConnectionTimeoutMs) {
    }

    public SQLiteDatabase getWritableDatabase() {
        return getDatabaseLocked(true);
    }

    public SQLiteDatabase getReadableDatabase() {
        return getDatabaseLocked(false);
    }

    private SQLiteDatabase getDatabaseLocked(boolean writable) {
        if (mDatabase != null) {
            if (!mDatabase.isOpen()) {
                mDatabase = null;
            } else {
                return mDatabase;
            }
        }
        if (mIsInitializing) {
            throw new IllegalStateException("getDatabase called recursively");
        }
        SQLiteDatabase db = null;
        try {
            mIsInitializing = true;
            if (mName == null) {
                db = SQLiteDatabase.create(mFactory);
            } else {
                db = mContext.openOrCreateDatabase(mName, Context.MODE_PRIVATE, mFactory, mErrorHandler);
            }
            onConfigure(db);
            final int version = db.getVersion();
            if (version != mNewVersion) {
                if (version > 0 && version < mMinimumSupportedVersion) {
                    db.close();
                    SQLiteDatabase.deleteDatabase(mName);
                    db = mContext.openOrCreateDatabase(mName, Context.MODE_PRIVATE, mFactory, mErrorHandler);
                    onConfigure(db);
                    onBeforeDelete(db);
                }
                db.beginTransaction();
                try {
                    int current = db.getVersion();
                    if (current == 0) {
                        onCreate(db);
                    } else if (current > mNewVersion) {
                        onDowngrade(db, current, mNewVersion);
                    } else {
                        onUpgrade(db, current, mNewVersion);
                    }
                    db.setVersion(mNewVersion);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            onOpen(db);
            mDatabase = db;
            return db;
        } finally {
            mIsInitializing = false;
            if (db != null && db != mDatabase) {
                db.close();
            }
        }
    }

    @Override
    public void close() {
        if (mIsInitializing) {
            throw new IllegalStateException("Closed during initialization");
        }
        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;
        }
    }

    public void onConfigure(SQLiteDatabase db) {
    }

    /// Called before a database older than the minimum supported version
    /// is recreated from scratch.
    public void onBeforeDelete(SQLiteDatabase db) {
    }

    public abstract void onCreate(SQLiteDatabase db);

    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Can't downgrade database from version " + oldVersion + " to " + newVersion);
    }

    public void onOpen(SQLiteDatabase db) {
    }
}
