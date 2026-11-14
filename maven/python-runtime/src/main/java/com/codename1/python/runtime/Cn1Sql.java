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
import com.codename1.db.SqlFunction;
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
        if (memory && Database.isInMemorySupported()) {
            return new Conn(Database.openInMemory(), null);
        }
        // A platform without in-memory databases gets a file of its own, deleted on close.
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
        public boolean createFunction(String name, int nargs, SqlFunction body) throws IOException {
            if (!db.supportsFunctions()) {
                return false;
            }
            db.createFunction(name, nargs, body);
            return true;
        }

        /// One result per nesting depth, reused: a caller reads its result
        /// before it runs anything else, and Python runs one thread at a time.
        /// A SQL function that executes a statement while one is stepping is
        /// the only nesting, and it gets the next depth's. The depth is the
        /// function wrapper's to count, so a run takes no handler to undo it.
        private static final Result[] RESULTS = new Result[8];

        /// What [Database#executeStatus(String, Object[], long[])] writes the changes and the
        /// last row id into; read straight after, so one serves every depth.
        private static final long[] COUNTS = new long[2];

        @Override
        public Result run(String sql, Object[] params, boolean query) throws IOException {
            return fill(slot(), sql, params, query);
        }

        @Override
        public Result runStatus(String sql, Object[] params, boolean query) {
            if (query) {
                return super.runStatus(sql, params, query);
            }
            Result r = slot();
            if (db.executeStatus(sql, params, COUNTS) != 0) {
                return failStatus(db.getStatusMessage());
            }
            if (isChange(sql)) {
                r.changes = COUNTS[0];
                r.lastRowId = COUNTS[1];
            }
            return r;
        }

        private static Result slot() {
            int depth = nesting;
            if (depth >= RESULTS.length) {
                return new Result();
            }
            Result r = RESULTS[depth];
            if (r == null) {
                r = new Result();
                RESULTS[depth] = r;
            } else {
                r.clear();
            }
            return r;
        }

        private Result fill(Result r, String sql, Object[] params, boolean query) throws IOException {
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
                        if (!Database.getValues(row, values)) {
                            for (int i = 0; i < n; i++) {
                                values[i] = value(row, i);
                            }
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
                r.changes = db.changes();
                r.lastRowId = db.lastInsertRowId();
            }
            return r;
        }

        private static Object value(Row row, int i) throws IOException {
            if (row.getColumnType(i) == Database.COLUMN_UNKNOWN) {
                return inferred(row.getString(i));
            }
            // The type is cached by the cursor, so this reads only the value.
            return Database.getValue(row, i);
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
    /// Whether the statement's first word is INSERT, UPDATE, DELETE or REPLACE, compared in place:
    /// this runs on every statement, and a copy of the word would be garbage each time.
    static boolean isChange(String sql) {
        int s = wordStart(sql);
        int e = s;
        int n = sql.length();
        while (e < n && isAsciiLetter(sql.charAt(e))) {
            e++;
        }
        return isWord(sql, s, e, "INSERT") || isWord(sql, s, e, "UPDATE") || isWord(sql, s, e, "DELETE")
                || isWord(sql, s, e, "REPLACE");
    }

    private static boolean isWord(String sql, int s, int e, String word) {
        return e - s == word.length() && sql.regionMatches(true, s, word, 0, word.length());
    }

    /// Where the first word starts, past spaces, comments and opening parentheses.
    private static int wordStart(String sql) {
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
        return i;
    }

    private static boolean isAsciiLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }
}
