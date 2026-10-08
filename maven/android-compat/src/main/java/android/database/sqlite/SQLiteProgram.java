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

import android.os.CancellationSignal;

/// A compiled SQL statement with its bound arguments. Codename One's
/// database API has no prepared statements, so the SQL is kept as text and
/// the arguments are bound when it runs; the number of parameters comes
/// from scanning the SQL for `?` placeholders outside string literals.
public abstract class SQLiteProgram extends SQLiteClosable {

    private static final Object[] EMPTY = new Object[0];

    private final SQLiteDatabase mDatabase;
    private final String mSql;
    private final int mNumParameters;
    private Object[] mBindArgs;

    SQLiteProgram(SQLiteDatabase db, String sql, Object[] bindArgs, CancellationSignal cancellationSignalForPrepare) {
        mDatabase = db;
        mSql = sql.trim();
        mNumParameters = countParameters(mSql);
        if (bindArgs != null && mNumParameters >= 0 && bindArgs.length > mNumParameters) {
            throw new IllegalArgumentException("Too many bind arguments.  " + bindArgs.length
                    + " arguments were provided but the statement needs " + mNumParameters + " arguments.");
        }
        if (mNumParameters > 0) {
            mBindArgs = new Object[mNumParameters];
        } else if (mNumParameters < 0) {
            mBindArgs = new Object[bindArgs == null ? 0 : bindArgs.length];
        } else {
            mBindArgs = null;
        }
        if (bindArgs != null && bindArgs.length > 0) {
            System.arraycopy(bindArgs, 0, mBindArgs, 0, bindArgs.length);
        }
    }

    /// The highest parameter index the SQL uses, or -1 when it uses named
    /// parameters whose count cannot be known without the engine.
    static int countParameters(String sql) {
        int count = 0;
        int max = 0;
        int n = sql.length();
        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"' || c == '`') {
                int j = i + 1;
                while (j < n) {
                    if (sql.charAt(j) == c) {
                        if (j + 1 < n && sql.charAt(j + 1) == c) {
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                i = j + 1;
                continue;
            }
            if (c == '[') {
                int j = sql.indexOf(']', i + 1);
                i = j < 0 ? n : j + 1;
                continue;
            }
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int j = sql.indexOf('\n', i);
                i = j < 0 ? n : j + 1;
                continue;
            }
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int j = sql.indexOf("*/", i + 2);
                i = j < 0 ? n : j + 2;
                continue;
            }
            if (c == '?') {
                int j = i + 1;
                int number = 0;
                while (j < n && sql.charAt(j) >= '0' && sql.charAt(j) <= '9') {
                    number = number * 10 + (sql.charAt(j) - '0');
                    j++;
                }
                if (j > i + 1) {
                    max = Math.max(max, number);
                    count = Math.max(count, number);
                } else {
                    count++;
                    max = Math.max(max, count);
                }
                i = j;
                continue;
            }
            if ((c == ':' || c == '@' || c == '$') && i + 1 < n && isIdentifierStart(sql.charAt(i + 1))) {
                return -1;
            }
            i++;
        }
        return max;
    }

    private static boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    final SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    final String getSql() {
        return mSql;
    }

    final Object[] getBindArgs() {
        return mBindArgs == null ? EMPTY : mBindArgs;
    }

    @Deprecated
    public final int getUniqueId() {
        return -1;
    }

    public void bindNull(int index) {
        bind(index, null);
    }

    public void bindLong(int index, long value) {
        bind(index, Long.valueOf(value));
    }

    public void bindDouble(int index, double value) {
        bind(index, Double.valueOf(value));
    }

    public void bindString(int index, String value) {
        if (value == null) {
            throw new IllegalArgumentException("the bind value at index " + index + " is null");
        }
        bind(index, value);
    }

    public void bindBlob(int index, byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("the bind value at index " + index + " is null");
        }
        bind(index, value);
    }

    public void clearBindings() {
        if (mBindArgs != null) {
            for (int i = 0; i < mBindArgs.length; i++) {
                mBindArgs[i] = null;
            }
        }
    }

    /// Binds every argument as a string; index `i + 1` gets `bindArgs[i]`.
    public void bindAllArgsAsStrings(String[] bindArgs) {
        if (bindArgs != null) {
            for (int i = bindArgs.length; i != 0; i--) {
                bindString(i, bindArgs[i - 1]);
            }
        }
    }

    @Override
    protected void onAllReferencesReleased() {
        clearBindings();
    }

    private void bind(int index, Object value) {
        if (index < 1 || (mNumParameters >= 0 && index > mNumParameters)) {
            throw new IllegalArgumentException("Cannot bind argument at index " + index
                    + " because the index is out of range.  The statement has " + mNumParameters + " parameters.");
        }
        if (mBindArgs == null || index > mBindArgs.length) {
            Object[] grown = new Object[index];
            if (mBindArgs != null) {
                System.arraycopy(mBindArgs, 0, grown, 0, mBindArgs.length);
            }
            mBindArgs = grown;
        }
        mBindArgs[index - 1] = value;
    }
}
