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
        String url = path != null && path.startsWith("jdbc:") ? path : "jdbc:sqlite:" + path;
        try {
            Connection connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            return new Db(connection);
        } catch (SQLException err) {
            if(url.startsWith("jdbc:sqlite:")) {
                // The usual cause is a dev classpath without the driver, and
                // "No suitable driver" on its own does not say which one.
                throw new IOException("Could not open " + url + " -- is sqlite-jdbc on "
                        + "the classpath? (" + err.getMessage() + ")");
            }
            throw new IOException("Could not open " + url + ": " + err.getMessage());
        }
    }

    public int execute(String sql, Object[] params) throws IOException {
        Connection c = live();
        try {
            if(params == null || params.length == 0) {
                // PRAGMA and the transaction verbs are not all preparable on every
                // driver, so a parameterless statement goes through Statement.
                Statement statement = c.createStatement();
                try {
                    statement.execute(sql);
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

    public List query(String sql, Object[] params) throws IOException {
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

    public Object transaction(Work body) throws Exception {
        Connection c = live();
        c.setAutoCommit(false);
        boolean committed = false;
        try {
            Object result = body.run(this);
            c.commit();
            committed = true;
            return result;
        } finally {
            if(!committed) {
                try {
                    c.rollback();
                } catch (SQLException err) {
                    // The original failure is the one worth reporting.
                    System.err.println("rollback failed: " + err);
                }
            }
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignored) {
                // The connection is going away anyway.
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

    public void setBusyTimeout(int millis) throws IOException {
        execute("PRAGMA busy_timeout=" + millis, null);
    }

    public long lastInsertId() {
        return lastInsertId;
    }

    public void close() {
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
