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
package com.codename1.impl.javase;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.impl.SQLStatementSplitter;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulator database, backed by the SQLite JDBC driver.
 *
 * @author Chen
 */
public class SEDatabase extends Database {

    private java.sql.Connection conn;

    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name, and changeKey() would otherwise have nothing to
     * resolve against.
     */
    private String databaseName;

    /**
     * Cursors created from this connection. Closing the database has to invalidate them, because
     * their statements belong to the connection and are gone once it closes.
     */
    private final List<SECursor> openCursors = new ArrayList<SECursor>();

    public SEDatabase(java.sql.Connection conn) {
        this(conn, null);
    }

    public SEDatabase(java.sql.Connection conn, String databaseName) {
        this.databaseName = databaseName;
        this.conn = conn;
        try {
            conn.setAutoCommit(true);
        } catch (SQLException err) {
            // A connection that cannot be put into autocommit is unusable, but the constructor
            // cannot report it; the first statement will fail with a real message.
        }
    }

    private void checkOpen() throws IOException {
        if (conn == null) {
            throw new IOException("This database has been closed");
        }
    }

    void registerCursor(SECursor cursor) {
        openCursors.add(cursor);
    }

    void unregisterCursor(SECursor cursor) {
        openCursors.remove(cursor);
    }

    @Override
    public void beginTransaction() throws IOException {
        checkOpen();
        checkBeginTransaction();
        try {
            conn.setAutoCommit(false);
        } catch (SQLException ex) {
            markTransactionEnded();
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    public void commitTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        try {
            conn.commit();
            conn.setAutoCommit(true);
            markTransactionEnded();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        try {
            conn.rollback();
            if (!isLegacyBehavior()) {
                // Without this the connection stays outside autocommit, so every subsequent
                // statement silently joins a new implicit transaction.
                conn.setAutoCommit(true);
            }
            markTransactionEnded();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (conn != null) {
            System.out.println("**** WARNING! Database object was released by the GC without being closed first! *****");
        }
        super.finalize();
    }

    @Override
    public void close() throws IOException {
        if (conn == null) {
            return;
        }
        java.sql.Connection closing = conn;
        conn = null;
        if (inTransaction) {
            inTransaction = false;
            try {
                closing.rollback();
            } catch (SQLException ignored) {
                // Rolling back on close is best effort; the close below is what matters.
            }
        }
        SECursor[] cursors = openCursors.toArray(new SECursor[openCursors.size()]);
        openCursors.clear();
        for (int iter = 0; iter < cursors.length; iter++) {
            cursors[iter].invalidate();
        }
        try {
            closing.close();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private static void cleanup(Statement s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (SQLException ignored) {
            // Nothing useful to do; the caller is already reporting the real failure.
        }
    }

    /**
     * Rejects a script where the caller promised a single statement. The JDBC driver would
     * otherwise prepare the first statement and silently discard the rest.
     */
    private void requireSingleStatement(String sql) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        if (SQLStatementSplitter.isMultiStatement(sql)) {
            throw new IOException("This method takes a single SQL statement, but the string "
                    + "contains " + SQLStatementSplitter.countStatements(sql)
                    + ". Use execute(String) to run a script.");
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        if (isLegacyBehavior()) {
            // A PreparedStatement prepares only the first statement of a script and silently
            // discards the rest, which is exactly what this port used to do.
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(sql);
                ps.execute();
            } catch (SQLException ex) {
                throw new IOException(ex.getMessage(), ex);
            } finally {
                cleanup(ps);
            }
            return;
        }
        // Split explicitly rather than handing the whole script to the driver: JDBC drivers
        // differ on whether Statement.execute runs a script or just its first statement, and
        // silently running less than the caller asked for is the failure we are removing.
        String[] statements = SQLStatementSplitter.split(sql);
        Statement s = null;
        try {
            s = conn.createStatement();
            for (int iter = 0; iter < statements.length; iter++) {
                s.execute(statements[iter]);
            }
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            cleanup(s);
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] == null) {
                        s.setNull(i + 1, java.sql.Types.NULL);
                    } else {
                        s.setString(i + 1, params[i]);
                    }
                }
            }
            s.execute();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            cleanup(s);
        }
    }

    @Override
    public void execute(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            // An explicitly empty array is still a parameterized call, so it is held to the
            // single-statement rule; only a null array means "no parameters at all".
            if (params != null) {
                requireSingleStatement(sql);
            }
            execute(sql);
            return;
        }
        checkOpen();
        requireSingleStatement(sql);
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(sql);
            bind(s, params);
            s.execute();
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            cleanup(s);
        }
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] == null) {
                        s.setNull(i + 1, java.sql.Types.NULL);
                    } else {
                        s.setString(i + 1, params[i]);
                    }
                }
            }
            ResultSet resultSet = s.executeQuery();
            SECursor cursor = new SECursor(this, s, resultSet);
            registerCursor(cursor);
            return cursor;
        } catch (SQLException ex) {
            cleanup(s);
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            if (params != null) {
                requireSingleStatement(sql);
            }
            return executeQuery(sql);
        }
        checkOpen();
        requireSingleStatement(sql);
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(sql);
            bind(s, params);
            ResultSet resultSet = s.executeQuery();
            SECursor cursor = new SECursor(this, s, resultSet);
            registerCursor(cursor);
            return cursor;
        } catch (SQLException ex) {
            cleanup(s);
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(sql);
            ResultSet resultSet = s.executeQuery();
            SECursor cursor = new SECursor(this, s, resultSet);
            registerCursor(cursor);
            return cursor;
        } catch (SQLException ex) {
            cleanup(s);
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        Statement s = null;
        try {
            s = conn.createStatement();
            // The cipher has to be selected before the key is applied, otherwise the driver's own
            // default scheme is used and the result is unreadable by every other platform.
            s.execute("PRAGMA cipher = 'sqlcipher'");
            s.execute("PRAGMA legacy = 4");
            if (config == null || !config.isEncrypted()) {
                s.execute("PRAGMA rekey = ''");
            } else {
                s.execute("PRAGMA rekey = "
                        + toPragmaLiteral(JavaSEPort.databaseKeyMaterial(config, databaseName)));
            }
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            cleanup(s);
        }
    }

    private static void bind(PreparedStatement s, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) {
                s.setNull(i + 1, java.sql.Types.NULL);
            } else if (p instanceof String) {
                s.setString(i + 1, (String) p);
            } else if (p instanceof byte[]) {
                s.setBytes(i + 1, (byte[]) p);
            } else if (p instanceof Double) {
                s.setDouble(i + 1, ((Double) p).doubleValue());
            } else if (p instanceof Float) {
                s.setDouble(i + 1, ((Float) p).doubleValue());
            } else if (p instanceof Long) {
                s.setLong(i + 1, ((Long) p).longValue());
            } else if (p instanceof Integer) {
                s.setLong(i + 1, ((Integer) p).intValue());
            } else if (p instanceof Short) {
                s.setLong(i + 1, ((Short) p).shortValue());
            } else if (p instanceof Byte) {
                s.setLong(i + 1, ((Byte) p).byteValue());
            } else if (p instanceof Boolean) {
                s.setLong(i + 1, ((Boolean) p).booleanValue() ? 1 : 0);
            } else {
                s.setString(i + 1, p.toString());
            }
        }
    }
}
