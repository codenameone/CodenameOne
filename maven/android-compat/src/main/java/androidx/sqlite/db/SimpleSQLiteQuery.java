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

/// A query over an array of arguments, each bound by its own type.
public final class SimpleSQLiteQuery implements SupportSQLiteQuery {

    private final String mQuery;
    private final Object[] mBindArgs;

    public SimpleSQLiteQuery(String query, Object[] bindArgs) {
        mQuery = query;
        mBindArgs = bindArgs;
    }

    public SimpleSQLiteQuery(String query) {
        this(query, null);
    }

    @Override
    public String getSql() {
        return mQuery;
    }

    @Override
    public void bindTo(SupportSQLiteProgram statement) {
        bind(statement, mBindArgs);
    }

    @Override
    public int getArgCount() {
        return mBindArgs == null ? 0 : mBindArgs.length;
    }

    /// Binds `bindArgs[i]` at index `i + 1`, by the argument's type. A
    /// boolean binds as 1 or 0, as Room stores it.
    public static void bind(SupportSQLiteProgram statement, Object[] bindArgs) {
        if (bindArgs == null) {
            return;
        }
        for (int i = 0; i < bindArgs.length; i++) {
            bind(statement, i + 1, bindArgs[i]);
        }
    }

    private static void bind(SupportSQLiteProgram statement, int index, Object arg) {
        if (arg == null) {
            statement.bindNull(index);
        } else if (arg instanceof byte[]) {
            statement.bindBlob(index, (byte[]) arg);
        } else if (arg instanceof Float || arg instanceof Double) {
            statement.bindDouble(index, ((Number) arg).doubleValue());
        } else if (arg instanceof Number) {
            statement.bindLong(index, ((Number) arg).longValue());
        } else if (arg instanceof Boolean) {
            statement.bindLong(index, ((Boolean) arg).booleanValue() ? 1 : 0);
        } else if (arg instanceof Character) {
            statement.bindLong(index, ((Character) arg).charValue());
        } else if (arg instanceof String) {
            statement.bindString(index, (String) arg);
        } else {
            throw new IllegalArgumentException("Cannot bind " + arg + " at index " + index
                    + " Supported types: Null, ByteArray, Float, Double, Long, Int, Short, Byte, String");
        }
    }
}
