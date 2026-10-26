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
package com.codename1.androidcompat.runtime;

import android.database.Cursor;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/// What the DAO implementations the Room build step generates call. Not an
/// application API.
public final class RoomSupport {

    private RoomSupport() {
    }

    public static Cursor query(RoomDatabase db, String sql, List<Object> args) {
        return db.query(sql, args.toArray());
    }

    /// Runs an INSERT and answers the row id, or -1 when a conflict
    /// strategy skipped the row.
    public static long insert(RoomDatabase db, String sql, Object[] args) {
        SupportSQLiteStatement st = db.compileStatement(sql);
        try {
            SimpleSQLiteQuery.bind(st, args);
            return st.executeInsert();
        } finally {
            st.close();
        }
    }

    /// Runs an UPDATE or DELETE and answers how many rows it changed.
    public static int updateDelete(RoomDatabase db, String sql, Object[] args) {
        SupportSQLiteStatement st = db.compileStatement(sql);
        try {
            SimpleSQLiteQuery.bind(st, args);
            return st.executeUpdateDelete();
        } finally {
            st.close();
        }
    }

    public static int updateDelete(RoomDatabase db, String sql, List<Object> args) {
        return updateDelete(db, sql, args.toArray());
    }

    public static void execute(RoomDatabase db, String sql, List<Object> args) {
        SupportSQLiteStatement st = db.compileStatement(sql);
        try {
            SimpleSQLiteQuery.bind(st, args.toArray());
            st.execute();
        } finally {
            st.close();
        }
    }

    /// The column's index, looking past the backticks a result column may
    /// keep, or -1.
    public static int columnIndex(Cursor c, String name) {
        int i = c.getColumnIndex(name);
        if (i >= 0) {
            return i;
        }
        i = c.getColumnIndex("`" + name + "`");
        if (i >= 0) {
            return i;
        }
        // A view or a join can report a column as table.column.
        String[] names = c.getColumnNames();
        String suffix = "." + name;
        for (int k = 0; k < names.length; k++) {
            String n = names[k];
            if (n.endsWith(suffix) || n.endsWith(suffix + "`")) {
                return k;
            }
        }
        return -1;
    }

    public static int columnIndexOrThrow(Cursor c, String name) {
        int i = columnIndex(c, name);
        if (i < 0) {
            throw new IllegalArgumentException("column '" + name + "' does not exist. Available columns: "
                    + java.util.Arrays.asList(c.getColumnNames()));
        }
        return i;
    }

    /// Appends one placeholder per element of an `IN (:list)` parameter
    /// and records the values.
    public static void appendIn(StringBuilder sql, List<Object> args, Collection<?> values) {
        int n = 0;
        if (values != null) {
            Iterator<?> it = values.iterator();
            while (it.hasNext()) {
                sql.append(n++ == 0 ? "?" : ",?");
                args.add(it.next());
            }
        }
    }

    public static void appendIn(StringBuilder sql, List<Object> args, Object[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sql.append(i == 0 ? "?" : ",?");
                args.add(values[i]);
            }
        }
    }

    public static void appendIn(StringBuilder sql, List<Object> args, long[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sql.append(i == 0 ? "?" : ",?");
                args.add(Long.valueOf(values[i]));
            }
        }
    }

    public static void appendIn(StringBuilder sql, List<Object> args, int[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sql.append(i == 0 ? "?" : ",?");
                args.add(Integer.valueOf(values[i]));
            }
        }
    }

    public static void appendIn(StringBuilder sql, List<Object> args, short[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sql.append(i == 0 ? "?" : ",?");
                args.add(Short.valueOf(values[i]));
            }
        }
    }

    public static void appendIn(StringBuilder sql, List<Object> args, double[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sql.append(i == 0 ? "?" : ",?");
                args.add(Double.valueOf(values[i]));
            }
        }
    }

    public static void appendIn(StringBuilder sql, List<Object> args, float[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sql.append(i == 0 ? "?" : ",?");
                args.add(Float.valueOf(values[i]));
            }
        }
    }

    /// An enum constant by name, found without `Enum.valueOf` (which looks
    /// the class up reflectively).
    public static <E extends Enum<E>> E enumByName(E[] values, String name) {
        if (name == null) {
            return null;
        }
        for (E e : values) {
            if (e.name().equals(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + name);
    }

    public static String enumName(Enum<?> e) {
        return e == null ? null : e.name();
    }

    /// Whether an insert failed on a primary key or unique constraint, the
    /// case in which an upsert updates instead.
    public static boolean isUniqueViolation(RuntimeException e) {
        String m = e.getMessage();
        if (m == null) {
            return false;
        }
        return containsIgnoreCase(m, "unique") || containsIgnoreCase(m, "primary key")
                || m.indexOf("2067") >= 0 || m.indexOf("1555") >= 0;
    }

    private static boolean containsIgnoreCase(String s, String part) {
        int n = part.length();
        for (int i = 0; i + n <= s.length(); i++) {
            if (s.regionMatches(true, i, part, 0, n)) {
                return true;
            }
        }
        return false;
    }

    public static Object[] concat(Object[] a, Object[] b) {
        Object[] out = new Object[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /// The boolean a column holds, Room's way: nonzero is true.
    public static boolean toBoolean(Cursor c, int i) {
        return c.getInt(i) != 0;
    }

    public static Object bindBoolean(boolean b) {
        return Integer.valueOf(b ? 1 : 0);
    }

    public static Object bindChar(Character c) {
        return c == null ? null : Integer.valueOf(c.charValue());
    }

    public static Object bindBoolean(Boolean b) {
        return b == null ? null : Integer.valueOf(b.booleanValue() ? 1 : 0);
    }
}
