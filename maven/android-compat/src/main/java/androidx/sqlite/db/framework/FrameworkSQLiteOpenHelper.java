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

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/// A [SupportSQLiteOpenHelper] over the platform `SQLiteOpenHelper`, so the
/// version check, the transaction around a create or upgrade and the
/// minimum-version handling are the platform's.
final class FrameworkSQLiteOpenHelper implements SupportSQLiteOpenHelper {

    private final Helper mHelper;

    FrameworkSQLiteOpenHelper(SupportSQLiteOpenHelper.Configuration configuration) {
        mHelper = new Helper(configuration);
    }

    @Override
    public String getDatabaseName() {
        return mHelper.getDatabaseName();
    }

    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        mHelper.setWriteAheadLoggingEnabled(enabled);
    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        return mHelper.wrap(mHelper.getWritableDatabase());
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        return mHelper.wrap(mHelper.getReadableDatabase());
    }

    @Override
    public void close() {
        mHelper.close();
    }

    /// Hands every hook the one wrapper of the database it is given.
    private static final class Helper extends SQLiteOpenHelper {
        private final SupportSQLiteOpenHelper.Callback mCallback;
        private FrameworkSQLiteDatabase mWrapped;

        Helper(SupportSQLiteOpenHelper.Configuration configuration) {
            super(configuration.context, configuration.name, null, configuration.callback.version);
            mCallback = configuration.callback;
        }

        FrameworkSQLiteDatabase wrap(SQLiteDatabase db) {
            if (mWrapped == null || mWrapped.getDelegate() != db) {
                mWrapped = new FrameworkSQLiteDatabase(db);
            }
            return mWrapped;
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            mCallback.onConfigure(wrap(db));
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mCallback.onCreate(wrap(db));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            mCallback.onUpgrade(wrap(db), oldVersion, newVersion);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            mCallback.onDowngrade(wrap(db), oldVersion, newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            mCallback.onOpen(wrap(db));
        }

        @Override
        public void close() {
            super.close();
            mWrapped = null;
        }
    }
}
