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

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.sql.Dialect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A server database as a migration target.
///
/// One connection is borrowed for the whole run and handed back when the engine says the run
/// is over. It has to be one connection: the migration lock on PostgreSQL and MySQL belongs to
/// the session that took it, and a statement sent down a second connection would run outside
/// it.
///
/// Internal migration runtime; not an application API.
/// @hidden
public final class BackendMigrationTarget implements MigrationTarget {
    private final DataSource pool;
    private final Dialect dialect;
    private final boolean mysql;
    private final boolean postgres;
    private Database db;
    private final boolean supplied;
    /// A transaction a script opened for itself, which the connection does not track.
    private boolean scriptTransaction;

    /// Migrates through a connection borrowed from a pool.
    public BackendMigrationTarget(DataSource pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null");
        }
        this.pool = pool;
        this.supplied = false;
        this.dialect = pool.dialect();
        this.mysql = "mysql".equals(dialect.getName());
        this.postgres = "postgresql".equals(dialect.getName());
    }

    /// Migrates through a connection the caller owns.
    public BackendMigrationTarget(Database db) {
        if (db == null) {
            throw new IllegalArgumentException("database is null");
        }
        this.pool = null;
        this.db = db;
        this.supplied = true;
        this.dialect = db.dialect();
        this.mysql = "mysql".equals(dialect.getName());
        this.postgres = "postgresql".equals(dialect.getName());
    }

    private Database db() throws IOException {
        if (db == null) {
            db = pool.borrow();
        }
        return db;
    }

    @Override
    public String dialect() {
        return dialect.getName();
    }

    @Override
    public String[] split(String script) throws IOException {
        return dialect.splitStatements(script);
    }

    @Override
    public void execute(String sql, Object[] params) throws IOException {
        db().execute(sql, params == null ? new Object[0] : params);
        int control = MigrationEngine.transactionControl(sql);
        if (control != 0 && !db.isInTransaction()) {
            scriptTransaction = control > 0;
        }
    }

    @Override
    public List<String[]> query(String sql, Object[] params) throws IOException {
        List rows = db().query(sql, params == null ? new Object[0] : params);
        List<String[]> out = new ArrayList<String[]>(rows.size());
        for (Object row : rows) {
            if (!(row instanceof Map)) {
                continue;
            }
            Map map = (Map) row;
            String[] values = new String[map.size()];
            int at = 0;
            for (Object value : map.values()) {
                values[at] = text(value);
                at++;
            }
            out.add(values);
        }
        return out;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append("0123456789abcdef".charAt((b >> 4) & 0xF));
                hex.append("0123456789abcdef".charAt(b & 0xF));
            }
            return hex.toString();
        }
        return value.toString();
    }

    private List<String> names(boolean views) throws IOException {
        String sql;
        if (mysql) {
            sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() "
                    + "AND table_type = '" + (views ? "VIEW" : "BASE TABLE") + "'";
        } else if (postgres) {
            sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema() "
                    + "AND table_type = '" + (views ? "VIEW" : "BASE TABLE") + "'";
        } else {
            sql = "SELECT name FROM sqlite_master WHERE type = '" + (views ? "view" : "table") + "'";
        }
        List<String> names = new ArrayList<String>();
        for (String[] row : query(sql, null)) {
            if (row.length > 0 && row[0] != null) {
                names.add(row[0]);
            }
        }
        return names;
    }

    @Override
    public boolean tableExists(String table) throws IOException {
        for (String name : names(false)) {
            // MySQL folds table names by the file system it runs on, so the same name can come
            // back in another case than it was created with.
            if (mysql ? name.equalsIgnoreCase(table) : name.equals(table)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUserObjects() throws IOException {
        for (String name : names(false)) {
            if (!HistoryTable.isFrameworkOwned(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsDdlTransactions() {
        return !mysql;
    }

    @Override
    public void begin() throws IOException {
        db().beginTransaction();
    }

    @Override
    public void commit() throws IOException {
        db().commitTransaction();
    }

    @Override
    public void rollback() throws IOException {
        if (db == null) {
            return;
        }
        if (db.isInTransaction()) {
            db.rollbackTransaction();
        } else if (scriptTransaction) {
            scriptTransaction = false;
            db.execute("ROLLBACK", new Object[0]);
        }
    }

    @Override
    public boolean isTransactionActive() {
        return db != null && (scriptTransaction || db.isInTransaction());
    }

    /// The lock is the engine's own session lock, so a process that dies holding it releases
    /// it by dying. Waiting happens inside the database -- `GET_LOCK` with a timeout,
    /// `pg_sleep` between attempts -- which parks a virtual thread on its socket instead of
    /// sleeping a host thread.
    ///
    /// SQLite takes none here: a migration there runs in a transaction that begins by
    /// taking the write lock, and the engine re-reads the history under it. The exception is
    /// a migration marked as running outside a transaction, which opens its own: SQLite has
    /// no lock that outlasts a transaction, so nothing holds a second process off such a
    /// script. See the note where MigrationEngine re-reads the history.
    @Override
    public boolean lock(String name, int waitSeconds) throws IOException {
        int attempts = waitSeconds < 1 ? 1 : waitSeconds;
        if (mysql) {
            for (int i = 0; i < attempts; i++) {
                if (answeredOne(query("SELECT GET_LOCK('" + name + "', 1)", null))) {
                    return true;
                }
            }
            return false;
        }
        if (postgres) {
            String key = String.valueOf(lockKey(name));
            for (int i = 0; i < attempts; i++) {
                if (answeredOne(query("SELECT pg_try_advisory_lock(" + key + ")", null))) {
                    return true;
                }
                if (i + 1 < attempts) {
                    query("SELECT pg_sleep(1)", null);
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public void unlock(String name) throws IOException {
        if (db == null) {
            return;
        }
        if (mysql) {
            query("SELECT RELEASE_LOCK('" + name + "')", null);
        } else if (postgres) {
            query("SELECT pg_advisory_unlock(" + lockKey(name) + ")", null);
        }
    }

    private static long lockKey(String name) {
        // Two words: a fixed one that marks the key as a migration lock, and the name's
        // checksum. Advisory lock keys are one flat number space per database.
        return (0x434E314DL << 32) | (MigrationChecksum.of(name) & 0xFFFFFFFFL);
    }

    private static boolean answeredOne(List<String[]> rows) {
        return !rows.isEmpty() && rows.get(0).length > 0 && HistoryTable.truthy(rows.get(0)[0]);
    }

    @Override
    public String currentUser() throws IOException {
        if (mysql || postgres) {
            List<String[]> rows = query(mysql ? "SELECT CURRENT_USER()" : "SELECT current_user", null);
            if (!rows.isEmpty() && rows.get(0).length > 0 && rows.get(0)[0] != null) {
                return rows.get(0)[0];
            }
        }
        return "backend";
    }

    @Override
    public void dropAllObjects() throws IOException {
        Object[] none = new Object[0];
        boolean sqliteForeignKeys = "sqlite".equals(dialect.getName())
                && answeredOne(query("PRAGMA foreign_keys", null));
        if (mysql) {
            db().execute("SET FOREIGN_KEY_CHECKS = 0", none);
        }
        try {
            if (sqliteForeignKeys) {
                db().execute("PRAGMA foreign_keys = OFF", none);
                if (answeredOne(query("PRAGMA foreign_keys", null))) {
                    throw new IOException("SQLite clean requires a connection outside a transaction");
                }
            }
            for (String name : names(true)) {
                db().execute("DROP VIEW IF EXISTS " + dialect.quote(name) + (postgres ? " CASCADE" : ""), none);
            }
            for (String name : names(false)) {
                if (!name.startsWith("sqlite_")) {
                    db().execute("DROP TABLE IF EXISTS " + dialect.quote(name) + (postgres ? " CASCADE" : ""),
                            none);
                }
            }
        } finally {
            if (mysql) {
                db().execute("SET FOREIGN_KEY_CHECKS = 1", none);
            } else if (sqliteForeignKeys) {
                db().execute("PRAGMA foreign_keys = ON", none);
            }
        }
    }

    @Override
    public Object connection() {
        try {
            return db();
        } catch (IOException failure) {
            throw new IllegalStateException("No connection: " + failure.getMessage(), failure);
        }
    }

    @Override
    public void done() {
        scriptTransaction = false;
        if (!supplied && db != null) {
            Database borrowed = db;
            db = null;
            pool.release(borrowed);
        }
    }
}
