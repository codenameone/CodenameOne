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
    private final boolean sqlServer;
    private final boolean oracle;
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
        this.sqlServer = "sqlserver".equals(dialect.getName());
        this.oracle = "oracle".equals(dialect.getName());
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
        this.sqlServer = "sqlserver".equals(dialect.getName());
        this.oracle = "oracle".equals(dialect.getName());
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
        } else if (sqlServer) {
            sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = SCHEMA_NAME() "
                    + "AND table_type = '" + (views ? "VIEW" : "BASE TABLE") + "'";
        } else if (oracle) {
            // The names exactly as stored: unquoted DDL stores them upper case.
            sql = views ? "SELECT view_name FROM user_views"
                    : "SELECT table_name FROM user_tables WHERE dropped = 'NO' AND nested = 'NO'";
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
            // SQL Server's catalog compares names under the database's
            // collation, which is case insensitive unless it was made otherwise.
            // Oracle stores an unquoted name upper case, so a script's
            // "CREATE TABLE users" is the table "users" names.
            if (mysql || sqlServer || oracle ? name.equalsIgnoreCase(table) : name.equals(table)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUserObjects() throws IOException {
        for (String name : names(false)) {
            if (!HistoryTable.isFrameworkOwned(oracle ? asciiLower(name) : name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsDdlTransactions() {
        // Oracle, like MySQL, commits around every DDL statement.
        return !mysql && !oracle;
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
        if (sqlServer) {
            // An application lock owned by the session, so a dead process
            // releases it; sp_getapplock waits inside the server, and answers
            // 0 or 1 when the lock is granted. One batch with no terminators:
            // T-SQL needs none between these, and Database refuses a ';' that
            // would make it look like several statements.
            List<String[]> rows = query("DECLARE @granted INT EXEC @granted = sp_getapplock @Resource = N'"
                    + literal(name) + "', @LockMode = 'Exclusive', @LockOwner = 'Session', @LockTimeout = "
                    + (attempts * 1000) + " SELECT CASE WHEN @granted >= 0 THEN 1 ELSE 0 END AS granted", null);
            return answeredOne(rows);
        }
        if (oracle) {
            // DBMS_LOCK, held by the session (release_on_commit FALSE), so a
            // dead process releases it. It needs EXECUTE ON SYS.DBMS_LOCK,
            // which Oracle does not grant by default. 0 is granted, 4 already
            // held by this session.
            // One second per attempt, as on PostgreSQL: a single long wait
            // would outlast the connection's socket timeout.
            for (int i = 0; i < attempts; i++) {
                try {
                    db().execute("BEGIN IF DBMS_LOCK.REQUEST(" + oracleLockId(name) + ", DBMS_LOCK.X_MODE, 1, FALSE)"
                            + " NOT IN (0, 4) THEN RAISE_APPLICATION_ERROR(-20001, 'migration lock busy'); END IF; END;",
                            new Object[0]);
                    return true;
                } catch (IOException busy) {
                    if (busy.getMessage() == null || busy.getMessage().indexOf("ORA-20001") < 0) {
                        throw new IOException("Taking the migration lock needs EXECUTE ON SYS.DBMS_LOCK: "
                                + busy.getMessage(), busy);
                    }
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
        } else if (oracle) {
            db.execute("DECLARE r INTEGER; BEGIN r := DBMS_LOCK.RELEASE(" + oracleLockId(name) + "); END;",
                    new Object[0]);
        } else if (sqlServer) {
            db.execute("EXEC sp_releaseapplock @Resource = N'" + literal(name) + "', @LockOwner = 'Session'",
                    new Object[0]);
        }
    }

    private static String literal(String text) {
        return text.indexOf('\'') < 0 ? text : text.replace("'", "''");
    }

    private static long lockKey(String name) {
        // Two words: a fixed one that marks the key as a migration lock, and the name's
        // checksum. Advisory lock keys are one flat number space per database.
        return (0x434E314DL << 32) | (MigrationChecksum.of(name) & 0xFFFFFFFFL);
    }

    /// A DBMS_LOCK id for the name, inside the range applications may use
    /// (0 to 1073741823).
    private static long oracleLockId(String name) {
        return MigrationChecksum.of(name) & 0x3FFFFFFFL;
    }

    private static String asciiLower(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    private static boolean answeredOne(List<String[]> rows) {
        return !rows.isEmpty() && rows.get(0).length > 0 && HistoryTable.truthy(rows.get(0)[0]);
    }

    @Override
    public String currentUser() throws IOException {
        if (mysql || postgres || sqlServer || oracle) {
            List<String[]> rows = query(mysql ? "SELECT CURRENT_USER()" : sqlServer ? "SELECT SUSER_SNAME()"
                    : oracle ? "SELECT USER FROM DUAL" : "SELECT current_user", null);
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
            if (sqlServer) {
                // No switch turns foreign keys off for a DROP here, so they go
                // first.
                for (String[] row : query("SELECT OBJECT_NAME(parent_object_id) AS table_name, name FROM sys.foreign_keys "
                        + "WHERE OBJECT_SCHEMA_NAME(parent_object_id) = SCHEMA_NAME()", null)) {
                    db().execute("ALTER TABLE " + dialect.quote(row[0]) + " DROP CONSTRAINT "
                            + dialect.quote(row[1]), none);
                }
            }
            if (oracle) {
                // Names exactly as stored, so quoted by hand: Dialect.quote
                // upper-cases. CASCADE CONSTRAINTS takes the foreign keys
                // that point in; PURGE skips the recycle bin.
                for (String name : names(true)) {
                    db().execute("DROP VIEW \"" + name.replace("\"", "\"\"") + "\"", none);
                }
                for (String name : names(false)) {
                    db().execute("DROP TABLE \"" + name.replace("\"", "\"\"") + "\" CASCADE CONSTRAINTS PURGE",
                            none);
                }
                return;
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
