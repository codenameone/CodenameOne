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
package com.codename1.impl.migration;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.impl.SQLStatementSplitter;
import com.codename1.ui.Display;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The application database as a migration target.
///
/// Every script is split here and run one statement per call. That is deliberate: with the
/// legacy database behaviour switched on, `Database.execute(String)` runs only the first
/// statement of a script on some ports, and a migration that silently stopped after its first
/// statement would still be recorded as applied.
///
/// Internal migration runtime; not an application API.
/// @hidden
public final class DatabaseMigrationTarget implements MigrationTarget {
    private final Database db;

    /// Wraps a database the caller owns and uses from the calling thread.
    public DatabaseMigrationTarget(Database db) {
        if (db == null) {
            throw new IllegalArgumentException("database is null");
        }
        this.db = db;
    }

    @Override
    public String dialect() {
        return "sqlite";
    }

    @Override
    public String[] split(String script) {
        return SQLStatementSplitter.split(script);
    }

    @Override
    public void execute(String sql, Object[] params) throws IOException {
        if (params == null || params.length == 0) {
            db.execute(sql);
        } else {
            db.execute(sql, params);
        }
    }

    @Override
    public List<String[]> query(String sql, Object[] params) throws IOException {
        Cursor cursor = params == null || params.length == 0 ? db.executeQuery(sql) : db.executeQuery(sql, params);
        try {
            List<String[]> rows = new ArrayList<String[]>();
            int columns = cursor.getColumnCount();
            while (cursor.next()) {
                Row row = cursor.getRow();
                String[] values = new String[columns];
                for (int i = 0; i < columns; i++) {
                    values[i] = row.getString(i);
                }
                rows.add(values);
            }
            return rows;
        } finally {
            cursor.close();
        }
    }

    @Override
    public boolean tableExists(String table) throws IOException {
        for (String name : names("table")) {
            if (name.equals(table)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUserObjects() throws IOException {
        for (String name : names("table")) {
            if (!HistoryTable.isFrameworkOwned(name)) {
                return true;
            }
        }
        return false;
    }

    private List<String> names(String type) throws IOException {
        List<String> names = new ArrayList<String>();
        for (String[] row : query("SELECT name FROM sqlite_master WHERE type = '" + type + "'", null)) {
            if (row[0] != null) {
                names.add(row[0]);
            }
        }
        return names;
    }

    @Override
    public boolean supportsDdlTransactions() {
        return true;
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
    public boolean isTransactionActive() {
        return db.isInTransaction();
    }

    @Override
    public boolean lock(String name, int waitSeconds) {
        // One process, and the calling thread owns the database.
        return true;
    }

    @Override
    public void unlock(String name) {
    }

    @Override
    public String currentUser() {
        if (Display.isInitialized()) {
            String version = Display.getInstance().getProperty("AppVersion", null);
            if (version != null && version.length() > 0) {
                return "app " + version;
            }
        }
        return "app";
    }

    @Override
    public void dropAllObjects() throws IOException {
        for (String name : names("view")) {
            db.execute("DROP VIEW IF EXISTS " + quote(name));
        }
        for (String name : names("table")) {
            // SQLite's own bookkeeping tables cannot be dropped and are emptied with the rest.
            if (!name.startsWith("sqlite_")) {
                db.execute("DROP TABLE IF EXISTS " + quote(name));
            }
        }
    }

    private static String quote(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    @Override
    public Object connection() {
        return db;
    }

    @Override
    public void done() {
        // The caller owns the database.
    }
}
