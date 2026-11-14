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
package com.codename1.impl;

import com.codename1.db.SqlFunction;
import com.codename1.util.StringUtil;

import java.util.ArrayList;

/// What every port whose database is SQLite in C shares to call a
/// [SqlFunction]: the table that turns the id SQLite holds into the function,
/// and the conversion of arguments and the result. A port supplies only the
/// natives that read an argument and set the result.
///
/// The engine calls back on the thread that steps the statement, and a
/// connection is used by one thread at a time, so the table is never touched
/// by two threads at once.
public abstract class SqlFunctionBridge {
    /// `sqlite3_value_type`: SQLITE_INTEGER.
    public static final int SQLITE_INTEGER = 1;
    /// `sqlite3_value_type`: SQLITE_FLOAT.
    public static final int SQLITE_FLOAT = 2;
    /// `sqlite3_value_type`: SQLITE_TEXT.
    public static final int SQLITE_TEXT = 3;
    /// `sqlite3_value_type`: SQLITE_BLOB.
    public static final int SQLITE_BLOB = 4;

    private final ArrayList<SqlFunction> functions = new ArrayList<SqlFunction>();

    /// The id the engine is handed for a function. Ids are never reused, so a
    /// statement still holding an old one cannot reach a later function.
    public int register(SqlFunction function) {
        functions.add(function);
        return functions.size() - 1;
    }

    /// Called from the native trampoline, while the engine waits, to compute
    /// one value of function `id` and hand it back through `ctx`.
    public void call(long ctx, int id, int argc, long argv) {
        // Looked up outside the try: the cast the generic get carries must not
        // sit under a handler (ParparVM does not check it, see check-cast-semantics.sh).
        SqlFunction f = id >= 0 && id < functions.size() ? functions.get(id) : null;
        if (f == null) {
            resultError(ctx, StringUtil.getBytes("no such function"));
            return;
        }
        try {
            Object[] args = new Object[argc];
            for (int i = 0; i < argc; i++) {
                args[i] = argument(argv, i);
            }
            result(ctx, f.call(args));
        } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - an exception must not unwind through SQLite's C frames
            String m = t.getMessage();
            resultError(ctx, StringUtil.getBytes(m == null ? t.toString() : m));
        }
    }

    private Object argument(long argv, int i) {
        switch (valueType(argv, i)) {
            case SQLITE_INTEGER:
                return Long.valueOf(valueLong(argv, i));
            case SQLITE_FLOAT:
                return Double.valueOf(valueDouble(argv, i));
            case SQLITE_TEXT:
                return StringUtil.newString(valueText(argv, i));
            case SQLITE_BLOB:
                byte[] b = valueBlob(argv, i);
                return b == null ? new byte[0] : b;
            default:
                return null;
        }
    }

    private void result(long ctx, Object v) {
        if (v == null) {
            resultNull(ctx);
        } else if (v instanceof Boolean) {
            resultLong(ctx, ((Boolean) v).booleanValue() ? 1 : 0);
        } else if (v instanceof Double || v instanceof Float) {
            resultDouble(ctx, ((Number) v).doubleValue());
        } else if (v instanceof Number) {
            resultLong(ctx, ((Number) v).longValue());
        } else if (v instanceof byte[]) {
            resultBlob(ctx, (byte[]) v);
        } else {
            resultText(ctx, StringUtil.getBytes(v.toString()));
        }
    }

    /// `sqlite3_value_type` of argument `i`.
    protected abstract int valueType(long argv, int i);

    /// `sqlite3_value_int64` of argument `i`.
    protected abstract long valueLong(long argv, int i);

    /// `sqlite3_value_double` of argument `i`.
    protected abstract double valueDouble(long argv, int i);

    /// The UTF-8 text of argument `i`.
    protected abstract byte[] valueText(long argv, int i);

    /// The bytes of argument `i`.
    protected abstract byte[] valueBlob(long argv, int i);

    /// `sqlite3_result_null`.
    protected abstract void resultNull(long ctx);

    /// `sqlite3_result_int64`.
    protected abstract void resultLong(long ctx, long v);

    /// `sqlite3_result_double`.
    protected abstract void resultDouble(long ctx, double v);

    /// `sqlite3_result_text` of UTF-8 bytes.
    protected abstract void resultText(long ctx, byte[] utf8);

    /// `sqlite3_result_blob`.
    protected abstract void resultBlob(long ctx, byte[] v);

    /// `sqlite3_result_error` of a UTF-8 message.
    protected abstract void resultError(long ctx, byte[] utf8);
}
