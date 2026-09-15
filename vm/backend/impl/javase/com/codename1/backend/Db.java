/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java SE twin of Db, over JDBC.
 *
 * The native runtime links SQLite directly; the JVM reaches the same file through
 * the sqlite-jdbc driver. Row values are normalised to the SAME Java types the
 * native side produces -- Long, Double, String, byte[], null -- so a handler that
 * reads a column cannot behave differently between the two targets, which is the
 * whole point of having a local dev loop at all.
 *
 * A path that already looks like a JDBC URL is passed through untouched, so the
 * same code can point at MySQL or Postgres locally without a second API.
 */
public final class Db {
    private Connection connection;
    private long lastInsertId;

    private Db(Connection connection) {
        this.connection = connection;
    }

    public static Db open(String path) throws IOException {
        // Db is the SQLite class on both arms: the packaged one hands this string
        // straight to sqlite3_open. Accepting "jdbc:postgresql:..." here because a
        // driver happens to be on the dev classpath let code work through
        // cn1:backend and then fail once translated -- a dev loop that behaves
        // differently from production, which is the one thing it must not do.
        // Database is what speaks to those servers, on both arms.
        // IGNORING CASE, like the translated twin and like DataSource: a URL
        // scheme is case insensitive, and recognising "jdbc:sqlite:" here while
        // the other two recognised "JDBC:SQLITE:" made the same string an
        // in-memory database on one arm and a file named after the URL on the
        // other.
        if(path != null && path.regionMatches(true, 0, "jdbc:", 0, 5)
                && !path.regionMatches(true, 0, "jdbc:sqlite:", 0, 12)) {
            throw new IOException("Db opens SQLite only, and the translated build "
                    + "would hand " + path + " to sqlite3_open. Use Database.open for "
                    + "PostgreSQL or MySQL.");
        }
        // FORWARDED IN THE SPELLING IT ARRIVED IN, which is safe: measured
        // against sqlite-jdbc, DriverManager accepts jdbc:sqlite::memory:,
        // JDBC:SQLITE::memory:, JDBC:SQLite::memory: and Jdbc:Sqlite::memory:
        // alike -- the driver folds the scheme before comparing it, as a JDBC
        // driver is expected to. Rewriting it to lower case would only change
        // the URL the developer sees quoted back in an error.
        String url = path != null && path.regionMatches(true, 0, "jdbc:", 0, 5)
                ? path : "jdbc:sqlite:" + path;
        try {
            Connection connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            return new Db(connection);
        } catch (SQLException err) {
            if(url.regionMatches(true, 0, "jdbc:sqlite:", 0, 12)) {
                // IGNORING CASE like the two tests above it. This one was left
                // exact when they were relaxed, so a mixed-case URL that failed
                // for the usual reason got the generic message instead of the
                // one naming the driver.
                // The usual cause is a dev classpath without the driver, and
                // "No suitable driver" on its own does not say which one.
                throw new IOException("Could not open " + url + " -- is sqlite-jdbc on "
                        + "the classpath? (" + err.getMessage() + ")");
            }
            throw new IOException("Could not open " + url + ": " + err.getMessage());
        }
    }

    /** See the call sites: a statement holding a NUL means two things on two arms. */
    private static void requireNoNulInStatement(String sql) throws IOException {
        if(sql != null && sql.indexOf(0) >= 0) {
            throw new IOException("An SQL statement cannot hold a NUL: sqlite "
                    + "reads it as the end of the statement, and what is left "
                    + "may still be a valid one that means something else");
        }
    }

    public synchronized int execute(String sql, Object[] params) throws IOException {
        // THE SAME REFUSAL AS THE TRANSLATED ARM. There sqlite reads the statement
        // as a C string, so a NUL ends it and what is left may still be a complete
        // statement with its authorization clause gone -- see the note in the
        // translated twin. JDBC is length-aware and would run the whole string
        // here, which is the worse half of a divergence: the development loop
        // accepts a statement the packaged build silently rewrites.
        requireNoNulInStatement(sql);
        Connection c = live();
        try {
            if((params == null || params.length == 0) && sql.indexOf('?') < 0) {
                // PRAGMA and the transaction verbs are not all preparable on every
                // driver, so a parameterless statement goes through Statement.
                //
                // Only one with NO PLACEHOLDER IN IT, though. Routing every
                // paramless call here meant "INSERT ... VALUES (?, ?)" with null
                // params never reached a PreparedStatement at all, so the count
                // check below could not see it and SQLite inserted two NULLs. A
                // PRAGMA or a transaction verb carries no '?', so the reason this
                // branch exists is untouched; a statement that does carry one has
                // a parameter count to answer for and goes the other way.
                Statement statement = c.createStatement();
                try {
                    statement.execute(sql);
                    // Also here: an INSERT with literal values is still an INSERT, and
                    // the native implementation answers lastInsertRowid for it. Without
                    // this, the two backends disagree and the JavaSE one reports the id
                    // of some earlier parameterised insert. captureInsertId only assigns
                    // when the driver actually returns a key, so a PRAGMA or a CREATE
                    // leaves the previous value alone.
                    captureInsertId(statement);
                    int updated = statement.getUpdateCount();
                    return updated < 0 ? 0 : updated;
                } finally {
                    statement.close();
                }
            }
            PreparedStatement statement = c.prepareStatement(sql);
            try {
                bind(statement, params);
                statement.execute();
                captureInsertId(statement);
                int updated = statement.getUpdateCount();
                return updated < 0 ? 0 : updated;
            } finally {
                statement.close();
            }
        } catch (SQLException err) {
            throw new IOException("Statement failed: " + err.getMessage() + " [" + sql + "]");
        }
    }

    public synchronized List query(String sql, Object[] params) throws IOException {
        // THE SAME REFUSAL AS THE TRANSLATED ARM. There sqlite reads the statement
        // as a C string, so a NUL ends it and what is left may still be a complete
        // statement with its authorization clause gone -- see the note in the
        // translated twin. JDBC is length-aware and would run the whole string
        // here, which is the worse half of a divergence: the development loop
        // accepts a statement the packaged build silently rewrites.
        requireNoNulInStatement(sql);
        Connection c = live();
        try {
            PreparedStatement statement = c.prepareStatement(sql);
            try {
                bind(statement, params);
                ResultSet results = statement.executeQuery();
                try {
                    return readRows(results);
                } finally {
                    results.close();
                }
            } finally {
                statement.close();
            }
        } catch (SQLException err) {
            throw new IOException("Query failed: " + err.getMessage() + " [" + sql + "]");
        }
    }

    /**
     * Synchronized because a TRANSACTION is not one call.
     *
     * SQLite serializes each API call on a connection, which is what made "one
     * shared connection is correct, and SQLite serializes it" look true. It
     * serializes the calls, not the BEGIN/body/COMMIT sequence around them: a
     * second handler sharing this Db can execute between another's BEGIN and
     * COMMIT and have its write committed -- or rolled back -- by a request that
     * knows nothing about it, or meet "cannot start a transaction within a
     * transaction" and fail for a reason its own code cannot explain.
     *
     * The monitor is reentrant, which is what makes this work: transaction() holds
     * it for the whole callback and the execute() calls inside it re-enter freely.
     * A pooled connection is used by one thread at a time anyway, so the cost
     * there is an uncontended lock; a shared one is serialized, which is exactly
     * what correctness requires of it.
     */
    public synchronized Object transaction(Work body) throws Exception {
        // BEGIN IMMEDIATE, not setAutoCommit(false), because that is what the
        // PACKAGED arm does and the two must not disagree about concurrency.
        // setAutoCommit(false) leaves the JDBC driver on SQLite's DEFERRED
        // default, where a read-then-write transaction takes its read snapshot
        // first and only asks for the write lock when it writes: two of them
        // interleave, and the second fails SQLITE_BUSY on the upgrade instead
        // of waiting at its start. So the same code that is well behaved here
        // starts failing once it is packaged, which is the worst direction for
        // a difference like this to run. IMMEDIATE takes the write lock up
        // front, so the second transaction waits (bounded by busy_timeout).
        execute("BEGIN IMMEDIATE", null);
        boolean committed = false;
        try {
            Object result = body.run(this);
            execute("COMMIT", null);
            committed = true;
            return result;
        } finally {
            if(!committed) {
                try {
                    execute("ROLLBACK", null);
                } catch (Exception err) {
                    // The original failure is the one worth reporting.
                    System.err.println("rollback failed: " + err);
                }
            }
        }
    }

    /** A unit of work run inside {@link #transaction}. */
    public interface Work {
        Object run(Db db) throws Exception;
    }

    public void enableWriteAheadLog() throws IOException {
        query("PRAGMA journal_mode=WAL", null);
        execute("PRAGMA synchronous=NORMAL", null);
    }

    /**
     * Makes LIKE compare case SENSITIVELY, as the other two engines do.
     *
     * <p>SQLite folds ASCII case in LIKE by default, so like("title", "lo%")
     * matched a stored "LOW" here and matched nothing on PostgreSQL -- the
     * portable query builder answering different rows depending on which
     * database was configured. The pragma is per CONNECTION, so every pooled
     * one is set as it is opened.
     *
     * <p>Only ASCII is affected either way: SQLite's built-in LIKE has never
     * folded anything else, and neither does this.
     */
    public void useCaseSensitiveLike() throws IOException {
        execute("PRAGMA case_sensitive_like=ON", null);
    }

    public void setBusyTimeout(int millis) throws IOException {
        execute("PRAGMA busy_timeout=" + millis, null);
    }

    /** Synchronized with close for the reason the translated arm gives. */
    public synchronized long lastInsertId() {
        return lastInsertId;
    }

    /**
     * SYNCHRONIZED, like execute, query and transaction on this class, so a
     * DbPool.close() arriving while a borrowed connection is mid-query waits for
     * it rather than closing underneath it.
     *
     * <p>The packaged arm keeps its handle when the close does not take, because
     * sqlite3_close answers SQLITE_BUSY and leaves the connection open. There is
     * no equivalent here: JDBC's close releases the connection's resources
     * whatever the driver reports, so nulling the field cannot strand one.
     */
    public synchronized void close() {
        Connection c = connection;
        connection = null;
        if(c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // already gone
            }
        }
    }

    private Connection live() throws IOException {
        Connection c = connection;
        if(c == null) {
            throw new IOException("Database is closed");
        }
        return c;
    }

    private void captureInsertId(Statement statement) {
        try {
            ResultSet keys = statement.getGeneratedKeys();
            if(keys != null) {
                try {
                    if(keys.next()) {
                        lastInsertId = keys.getLong(1);
                    }
                } finally {
                    keys.close();
                }
            }
        } catch (SQLException ignored) {
            // Not every driver reports generated keys; the caller only misses an id.
        }
    }

    private static List readRows(ResultSet results) throws SQLException {
        List rows = new ArrayList();
        ResultSetMetaData meta = results.getMetaData();
        int columns = meta.getColumnCount();
        String[] names = new String[columns];
        for(int iter = 0 ; iter < columns ; iter++) {
            names[iter] = meta.getColumnLabel(iter + 1);
        }
        while(results.next()) {
            Map row = new LinkedHashMap();
            for(int iter = 0 ; iter < columns ; iter++) {
                row.put(names[iter], value(results, iter + 1));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Normalises to the four types the native runtime hands back. Anything the
     * driver gives us as some other class -- a java.sql.Timestamp, a BigDecimal --
     * becomes its string form, which is what SQLite's text affinity would have
     * produced for the same column.
     */
    private static Object value(ResultSet results, int index) throws SQLException {
        Object raw = results.getObject(index);
        if(raw == null || results.wasNull()) {
            return null;
        }
        if(raw instanceof byte[]) {
            return raw;
        }
        if(raw instanceof Number) {
            if(raw instanceof Double || raw instanceof Float) {
                return Double.valueOf(((Number)raw).doubleValue());
            }
            if(raw instanceof Integer || raw instanceof Long
                    || raw instanceof Short || raw instanceof Byte) {
                return Long.valueOf(((Number)raw).longValue());
            }
            return Double.valueOf(((Number)raw).doubleValue());
        }
        if(raw instanceof Boolean) {
            return Long.valueOf(((Boolean)raw).booleanValue() ? 1 : 0);
        }
        return String.valueOf(raw);
    }

    private static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        // THE COUNT HAS TO MATCH. Passing FEWER values than the statement has
        // placeholders left the rest unbound, and sqlite-jdbc executes that
        // happily with every unbound parameter reading as NULL -- so an insert or
        // an update committed a row the caller never wrote and nothing said so.
        //
        // Measured, and not what review assumed: the claim was that this arm
        // already threw where the packaged one did not. It does not. Both arms
        // accepted it, which makes this a shared defect rather than a divergence,
        // and both now refuse it with the same message. Too MANY parameters was
        // already refused here, by the driver's own range check.
        int expected = statement.getParameterMetaData().getParameterCount();
        int supplied = params == null ? 0 : params.length;
        if(supplied != expected) {
            throw new SQLException("the statement has " + expected + " parameter"
                    + (expected == 1 ? "" : "s") + " and " + supplied + " were supplied");
        }
        if(params == null) {
            return;
        }
        for(int iter = 0 ; iter < params.length ; iter++) {
            Object value = params[iter];
            int index = iter + 1;
            if(value == null) {
                statement.setNull(index, Types.NULL);
            } else if(value instanceof String) {
                statement.setString(index, (String)value);
            } else if(value instanceof Integer || value instanceof Long
                    || value instanceof Short || value instanceof Byte) {
                statement.setLong(index, ((Number)value).longValue());
            } else if(value instanceof Double || value instanceof Float) {
                statement.setDouble(index, ((Number)value).doubleValue());
            } else if(value instanceof byte[]) {
                statement.setBytes(index, (byte[])value);
            } else if(value instanceof Boolean) {
                statement.setLong(index, ((Boolean)value).booleanValue() ? 1 : 0);
            } else {
                statement.setString(index, String.valueOf(value));
            }
        }
    }
}
