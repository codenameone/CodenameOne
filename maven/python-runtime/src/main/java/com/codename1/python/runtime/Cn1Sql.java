/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import java.io.IOException;

/// SQLite through Codename One's `Database`: the platform's own engine on
/// every target. Values come back in their storage class where the port
/// reports it ([Row#getColumnType(int)]), and are inferred from
/// their text where it does not.
public final class Cn1Sql extends PySql {
    private static final java.util.concurrent.atomic.AtomicInteger MEMORY_COUNT =
            new java.util.concurrent.atomic.AtomicInteger();

    /// Makes this the engine unless a host installed its own first.
    public static void installDefault() {
        if (!installed()) {
            install(new Cn1Sql());
        }
    }

    @Override
    public Connection open(String name) throws IOException {
        boolean memory = ":memory:".equals(name) || name.length() == 0;
        String dbName = memory ? "cn1py-memory-" + System.currentTimeMillis() + "-" + MEMORY_COUNT.getAndIncrement() + ".db"
                : localName(name);
        if (memory && Database.exists(dbName)) {
            Database.delete(dbName);
        }
        return new Conn(Database.openOrCreate(dbName), memory ? dbName : null);
    }

    /// A path where the platform takes one; the file name alone elsewhere,
    /// which puts the database in the application's database directory.
    private static String localName(String name) {
        if (name.indexOf('/') >= 0 && !Database.isCustomPathSupported()) {
            return name.substring(name.lastIndexOf('/') + 1);
        }
        return name;
    }

    private static final class Conn extends Connection {
        private final Database db;
        private final String deleteOnClose;

        Conn(Database db, String deleteOnClose) {
            this.db = db;
            this.deleteOnClose = deleteOnClose;
        }

        @Override
        public Result run(String sql, Object[] params, boolean query) throws IOException {
            Result r = new Result();
            if (query) {
                Cursor c = params.length == 0 ? db.executeQuery(sql) : db.executeQuery(sql, params);
                try {
                    int n = c.getColumnCount();
                    r.columns = new String[n];
                    for (int i = 0; i < n; i++) {
                        r.columns[i] = c.getColumnName(i);
                    }
                    while (c.next()) {
                        Row row = c.getRow();
                        Object[] values = new Object[n];
                        for (int i = 0; i < n; i++) {
                            values[i] = value(row, i);
                        }
                        r.rows.add(values);
                    }
                } finally {
                    c.close();
                }
                return r;
            }
            if (params.length == 0) {
                db.execute(sql);
            } else {
                db.execute(sql, params);
            }
            if (isChange(sql)) {
                Cursor c = db.executeQuery("SELECT changes(), last_insert_rowid()");
                try {
                    if (c.next()) {
                        Row row = c.getRow();
                        r.changes = row.getLong(0);
                        r.lastRowId = row.getLong(1);
                    }
                } finally {
                    c.close();
                }
            }
            return r;
        }

        private static Object value(Row row, int i) throws IOException {
            switch (row.getColumnType(i)) {
                case Database.COLUMN_NULL:
                    return null;
                case Database.COLUMN_INTEGER:
                    return Long.valueOf(row.getLong(i));
                case Database.COLUMN_FLOAT:
                    return Double.valueOf(row.getDouble(i));
                case Database.COLUMN_TEXT:
                    return row.getString(i);
                case Database.COLUMN_BLOB:
                    return row.getBlob(i);
                default:
                    return inferred(row.getString(i));
            }
        }

        @Override
        public void begin() throws IOException {
            db.beginTransaction();
        }

        @Override
        public void commit() throws IOException {
            db.commitTransaction();
        }

        @Override
        public void rollback() throws IOException {
            db.rollbackTransaction();
        }

        @Override
        public void close() throws IOException {
            db.close();
            if (deleteOnClose != null) {
                Database.delete(deleteOnClose);
            }
        }
    }

    /// Whether a statement is one whose changes and row id are reported.
    static boolean isChange(String sql) {
        String w = firstWord(sql);
        return "INSERT".equalsIgnoreCase(w) || "UPDATE".equalsIgnoreCase(w) || "DELETE".equalsIgnoreCase(w)
                || "REPLACE".equalsIgnoreCase(w);
    }

    static String firstWord(String sql) {
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                while (i < n && sql.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int e = sql.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else if (c <= ' ' || c == '(') {
                i++;
            } else {
                break;
            }
        }
        int s = i;
        while (i < n && isAsciiLetter(sql.charAt(i))) {
            i++;
        }
        return sql.substring(s, i);
    }

    private static boolean isAsciiLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }
}
