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

import android.database.sqlite.SQLiteStatement;
import androidx.sqlite.db.SupportSQLiteStatement;

/// A [SupportSQLiteStatement] over the platform statement.
final class FrameworkSQLiteStatement implements SupportSQLiteStatement {

    private final SQLiteStatement mDelegate;

    FrameworkSQLiteStatement(SQLiteStatement delegate) {
        mDelegate = delegate;
    }

    @Override
    public void bindNull(int index) {
        mDelegate.bindNull(index);
    }

    @Override
    public void bindLong(int index, long value) {
        mDelegate.bindLong(index, value);
    }

    @Override
    public void bindDouble(int index, double value) {
        mDelegate.bindDouble(index, value);
    }

    @Override
    public void bindString(int index, String value) {
        mDelegate.bindString(index, value);
    }

    @Override
    public void bindBlob(int index, byte[] value) {
        mDelegate.bindBlob(index, value);
    }

    @Override
    public void clearBindings() {
        mDelegate.clearBindings();
    }

    @Override
    public void close() {
        mDelegate.close();
    }

    @Override
    public void execute() {
        mDelegate.execute();
    }

    @Override
    public int executeUpdateDelete() {
        return mDelegate.executeUpdateDelete();
    }

    @Override
    public long executeInsert() {
        return mDelegate.executeInsert();
    }

    @Override
    public long simpleQueryForLong() {
        return mDelegate.simpleQueryForLong();
    }

    @Override
    public String simpleQueryForString() {
        return mDelegate.simpleQueryForString();
    }
}
