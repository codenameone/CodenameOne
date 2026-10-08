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
package android.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/// Static helpers for building SQL and reading single values.
public class DatabaseUtils {

    public static final int STATEMENT_SELECT = 1;
    public static final int STATEMENT_UPDATE = 2;
    public static final int STATEMENT_ATTACH = 3;
    public static final int STATEMENT_BEGIN = 4;
    public static final int STATEMENT_COMMIT = 5;
    public static final int STATEMENT_ABORT = 6;
    public static final int STATEMENT_PRAGMA = 7;
    public static final int STATEMENT_DDL = 8;
    public static final int STATEMENT_UNPREPARED = 9;
    public static final int STATEMENT_OTHER = 99;

    /// Appends `sqlString` as an SQL string literal, doubling its quotes.
    public static void appendEscapedSQLString(StringBuilder sb, String sqlString) {
        sb.append('\'');
        if (sqlString.indexOf('\'') != -1) {
            int length = sqlString.length();
            for (int i = 0; i < length; i++) {
                char c = sqlString.charAt(i);
                if (c == '\'') {
                    sb.append('\'');
                }
                sb.append(c);
            }
        } else {
            sb.append(sqlString);
        }
        sb.append('\'');
    }

    public static String sqlEscapeString(String value) {
        StringBuilder escaper = new StringBuilder();
        appendEscapedSQLString(escaper, value);
        return escaper.toString();
    }

    /// Appends a value as an SQL literal: `NULL`, `1`/`0` for a boolean,
    /// otherwise a quoted string.
    public static final void appendValueToSql(StringBuilder sql, Object value) {
        if (value == null) {
            sql.append("NULL");
        } else if (value instanceof Boolean) {
            sql.append(((Boolean) value).booleanValue() ? '1' : '0');
        } else {
            appendEscapedSQLString(sql, value.toString());
        }
    }

    public static String concatenateWhere(String a, String b) {
        if (a == null || a.length() == 0) {
            return b;
        }
        if (b == null || b.length() == 0) {
            return a;
        }
        return "(" + a + ") AND (" + b + ")";
    }

    public static String[] appendSelectionArgs(String[] originalValues, String[] newValues) {
        if (originalValues == null || originalValues.length == 0) {
            return newValues;
        }
        String[] result = new String[originalValues.length + newValues.length];
        System.arraycopy(originalValues, 0, result, 0, originalValues.length);
        System.arraycopy(newValues, 0, result, originalValues.length, newValues.length);
        return result;
    }

    /// The `Cursor.FIELD_TYPE_*` an object would be stored as.
    public static int getTypeOfObject(Object obj) {
        if (obj == null) {
            return Cursor.FIELD_TYPE_NULL;
        } else if (obj instanceof byte[]) {
            return Cursor.FIELD_TYPE_BLOB;
        } else if (obj instanceof Float || obj instanceof Double) {
            return Cursor.FIELD_TYPE_FLOAT;
        } else if (obj instanceof Long || obj instanceof Integer || obj instanceof Short || obj instanceof Byte) {
            return Cursor.FIELD_TYPE_INTEGER;
        } else {
            return Cursor.FIELD_TYPE_STRING;
        }
    }

    public static long longForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        SQLiteStatement prog = db.compileStatement(query);
        try {
            return longForQuery(prog, selectionArgs);
        } finally {
            prog.close();
        }
    }

    public static long longForQuery(SQLiteStatement prog, String[] selectionArgs) {
        prog.bindAllArgsAsStrings(selectionArgs);
        return prog.simpleQueryForLong();
    }

    public static String stringForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        SQLiteStatement prog = db.compileStatement(query);
        try {
            return stringForQuery(prog, selectionArgs);
        } finally {
            prog.close();
        }
    }

    public static String stringForQuery(SQLiteStatement prog, String[] selectionArgs) {
        prog.bindAllArgsAsStrings(selectionArgs);
        return prog.simpleQueryForString();
    }

    public static long queryNumEntries(SQLiteDatabase db, String table) {
        return queryNumEntries(db, table, null, null);
    }

    public static long queryNumEntries(SQLiteDatabase db, String table, String selection) {
        return queryNumEntries(db, table, selection, null);
    }

    public static long queryNumEntries(SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
        String s = (selection != null && selection.length() > 0) ? " where " + selection : "";
        return longForQuery(db, "select count(*) from " + table + s, selectionArgs);
    }

    public static boolean queryIsEmpty(SQLiteDatabase db, String table) {
        long isEmpty = longForQuery(db, "select exists(select 1 from " + table + ")", null);
        return isEmpty == 0;
    }

    /// The kind of statement `sql` is, from its first keyword.
    public static int getSqlStatementType(String sql) {
        sql = sql.trim();
        if (sql.length() < 3) {
            return STATEMENT_OTHER;
        }
        String prefix = sql.substring(0, 3);
        if (prefix.equalsIgnoreCase("SEL")) {
            return STATEMENT_SELECT;
        } else if (prefix.equalsIgnoreCase("INS") || prefix.equalsIgnoreCase("UPD") || prefix.equalsIgnoreCase("REP")
                || prefix.equalsIgnoreCase("DEL")) {
            return STATEMENT_UPDATE;
        } else if (prefix.equalsIgnoreCase("ATT")) {
            return STATEMENT_ATTACH;
        } else if (prefix.equalsIgnoreCase("COM") || prefix.equalsIgnoreCase("END")) {
            return STATEMENT_COMMIT;
        } else if (prefix.equalsIgnoreCase("ROL")) {
            return STATEMENT_ABORT;
        } else if (prefix.equalsIgnoreCase("BEG")) {
            return STATEMENT_BEGIN;
        } else if (prefix.equalsIgnoreCase("PRA")) {
            return STATEMENT_PRAGMA;
        } else if (prefix.equalsIgnoreCase("CRE") || prefix.equalsIgnoreCase("DRO") || prefix.equalsIgnoreCase("ALT")) {
            return STATEMENT_DDL;
        } else if (prefix.equalsIgnoreCase("ANA") || prefix.equalsIgnoreCase("DET")) {
            return STATEMENT_UNPREPARED;
        }
        return STATEMENT_OTHER;
    }

    /// Copies the current row into `values`, every column as a string
    /// (blobs as blobs), as Android does.
    public static void cursorRowToContentValues(Cursor cursor, ContentValues values) {
        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
            if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                values.put(columns[i], cursor.getBlob(i));
            } else {
                values.put(columns[i], cursor.getString(i));
            }
        }
    }

    public static void cursorStringToContentValues(Cursor cursor, String field, ContentValues values) {
        cursorStringToContentValues(cursor, field, values, field);
    }

    public static void cursorStringToContentValues(Cursor cursor, String field, ContentValues values, String key) {
        values.put(key, cursor.getString(cursor.getColumnIndexOrThrow(field)));
    }

    public static void cursorLongToContentValues(Cursor cursor, String field, ContentValues values) {
        cursorLongToContentValues(cursor, field, values, field);
    }

    public static void cursorLongToContentValues(Cursor cursor, String field, ContentValues values, String key) {
        int colIndex = cursor.getColumnIndex(field);
        if (!cursor.isNull(colIndex)) {
            values.put(key, Long.valueOf(cursor.getLong(colIndex)));
        } else {
            values.put(key, (Long) null);
        }
    }

    public static void dumpCursor(Cursor cursor) {
        System.out.println(dumpCursorToString(cursor));
    }

    public static void dumpCurrentRow(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        dumpCurrentRow(cursor, sb);
        System.out.println(sb.toString());
    }

    public static String dumpCursorToString(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        sb.append(">>>>> Dumping cursor ").append(cursor).append('\n');
        if (cursor != null) {
            int startPos = cursor.getPosition();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                dumpCurrentRow(cursor, sb);
            }
            cursor.moveToPosition(startPos);
        }
        sb.append("<<<<<\n");
        return sb.toString();
    }

    public static void dumpCurrentRow(Cursor cursor, StringBuilder sb) {
        String[] cols = cursor.getColumnNames();
        sb.append(cursor.getPosition()).append(" {\n");
        int length = cols.length;
        for (int i = 0; i < length; i++) {
            String value;
            if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                value = "<unprintable>";
            } else {
                value = cursor.getString(i);
            }
            sb.append("   ").append(cols[i]).append('=').append(value).append('\n');
        }
        sb.append("}\n");
    }
}
