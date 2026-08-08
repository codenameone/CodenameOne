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

    /** The resolved file this connection holds, as the open-database registry knows it. */
    private String openKey;

    /**
     * Cursors created from this connection. Closing the database has to invalidate them, because
     * their statements belong to the connection and are gone once it closes.
     */
    private final List<SECursor> openCursors = new ArrayList<SECursor>();

    public SEDatabase(java.sql.Connection conn) throws IOException {
        this(conn, null, null);
    }

    public SEDatabase(java.sql.Connection conn, String databaseName) throws IOException {
        reserveConnection(databaseName);
        boolean kept = false;
        try {
            init(conn, databaseName, databaseName);
            kept = true;
        } finally {
            if (!kept) {
                releaseConnection(databaseName);
            }
        }
    }

    /// Claims a database file before anything opens it.
    ///
    /// The claim is also the refusal: a key change in progress is rewriting the file, and a
    /// connection that opened it first would read pages from both sides of the rewrite before the
    /// refusal arrived. So the caller takes this before `DriverManager.getConnection`, and passes
    /// the same key to the constructor, which does not take it again.
    ///
    /// @param openKey the resolved file, as `#normalizeDatabasePathKey(String)` or a canonical path
    static void reserveConnection(String openKey) throws IOException {
        registerOpenDatabase(openKey);
    }

    /// Gives back a claim `#reserveConnection(String)` took, for an open that did not happen.
    static void releaseConnection(String openKey) {
        releaseOpenDatabase(openKey);
    }

    /**
     * @param openKey identifies the file on disk rather than the name it was asked for. The two
     * differ here: a name is resolved against the storage directory unless it looks like a path,
     * so "app.db", the absolute path it resolves to, and "/tmp/./app.db" can all name one file.
     * Registering the name would let two of those spellings rekey the file under each other.
     *
     * <p>The caller is expected to hold the claim for that key already, from
     * {@link #reserveConnection(String)}, taken before the connection was opened. This constructor
     * does not take it, and closing the database gives it back.
     */
    public SEDatabase(java.sql.Connection conn, String databaseName, String openKey)
            throws IOException {
        init(conn, databaseName, openKey);
    }

    /// Takes ownership of a connection whose claim the caller already holds.
    private void init(java.sql.Connection conn, String databaseName, String openKey) {
        this.databaseName = databaseName;
        this.openKey = openKey;
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

    /// Carries out a transaction-control statement through JDBC, if that is what it is.
    ///
    /// The parameterized entry points reach this before preparing anything: `execute("BEGIN", ...)`
    /// means what `beginTransaction()` means, and preparing it as ordinary SQL would open a
    /// transaction the connection's autocommit flag knows nothing about.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the single statement the caller supplied
    ///
    /// #### Returns
    ///
    /// true if it was transaction control and has been carried out
    private boolean runAsTransactionControl(String sql) throws IOException {
        if (transactionControlKeyword(sql) == null) {
            return false;
        }
        try {
            executeTransactionControl(sql);
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
        noteScriptTransactionControl(sql);
        return true;
    }

    /// Puts the connection into the locking mode a BEGIN asked for.
    ///
    /// Restored to the driver's default once the transaction ends, so a later plain `BEGIN` is
    /// deferred again rather than inheriting a mode from the statement before it.
    private void applyBeginMode(String statement) {
        String named = beginTransactionMode(statement);
        org.sqlite.SQLiteConfig.TransactionMode mode =
                org.sqlite.SQLiteConfig.TransactionMode.DEFERRED;
        if ("IMMEDIATE".equals(named)) {
            mode = org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;
        } else if ("EXCLUSIVE".equals(named)) {
            mode = org.sqlite.SQLiteConfig.TransactionMode.EXCLUSIVE;
        }
        setTransactionMode(mode);
    }

    /// Ends a transaction the driver does not know it is in.
    private void endTransactionThroughSql(String statement) throws SQLException {
        Statement s = null;
        try {
            s = conn.createStatement();
            s.execute(statement);
        } finally {
            cleanup(s);
        }
    }

    /// Sets the driver's transaction mode, where the driver in use has one.
    private void setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode mode) {
        if (conn instanceof org.sqlite.SQLiteConnection) {
            ((org.sqlite.SQLiteConnection) conn).setCurrentTransactionMode(mode);
        }
    }

    /// Runs a transaction-control statement through JDBC, or reports that it is not one.
    ///
    /// `execute("BEGIN")` and `beginTransaction()` mean the same thing to the caller and have to
    /// mean the same thing to the connection. Everything else is passed to the driver untouched.
    ///
    /// #### Parameters
    ///
    /// - `statement`: a single statement from the script
    ///
    /// #### Returns
    ///
    /// true if it was transaction control and has been carried out
    private boolean executeTransactionControl(String statement) throws SQLException {
        String keyword = transactionControlKeyword(statement);
        if ("BEGIN".equals(keyword)) {
            // BEGIN IMMEDIATE and BEGIN EXCLUSIVE take their write locks up front, which is the
            // whole reason to write them; reducing every variant to setAutoCommit(false) would
            // start a deferred transaction and let another writer take the lock first. The driver
            // issues the BEGIN itself, so the mode is set on the connection rather than sent as
            // SQL -- sending it would open a transaction JDBC's autocommit flag knows nothing of.
            applyBeginMode(statement);
            conn.setAutoCommit(false);
            return true;
        }
        if ("COMMIT".equals(keyword) || "END".equals(keyword)) {
            conn.commit();
            conn.setAutoCommit(true);
            setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.DEFERRED);
            return true;
        }
        if ("ROLLBACK".equals(keyword)) {
            conn.rollback();
            conn.setAutoCommit(true);
            setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.DEFERRED);
            return true;
        }
        return false;
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
            if (conn.getAutoCommit()) {
                // Opened by SAVEPOINT rather than by this API, so JDBC never left autocommit and
                // conn.commit() would throw. SQLite ends a savepoint-started transaction with an
                // ordinary COMMIT, releasing every savepoint inside it.
                endTransactionThroughSql("COMMIT");
                markTransactionEnded();
                return;
            }
            conn.commit();
            conn.setAutoCommit(true);
            // The mode belongs to the transaction that just ended, not to the connection. Leaving
            // an IMMEDIATE from execute("BEGIN IMMEDIATE") set would make the next plain
            // beginTransaction() take a write lock nobody asked for.
            setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.DEFERRED);
            markTransactionEnded();
        } catch (SQLException ex) {
            // JDBC leaves the transaction open when the commit fails, so discard it here rather
            // than leaving the connection wedged with no way back to autocommit.
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // The commit failure is the one worth reporting.
            }
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Same.
            }
            setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.DEFERRED);
            throw abandonFailedCommit(ex);
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        try {
            if (conn.getAutoCommit()) {
                // See commitTransaction(): a savepoint opened this, so it ends in SQL.
                endTransactionThroughSql("ROLLBACK");
                markTransactionEnded();
                return;
            }
            conn.rollback();
            if (!isLegacyBehavior()) {
                // Without this the connection stays outside autocommit, so every subsequent
                // statement silently joins a new implicit transaction.
                conn.setAutoCommit(true);
            }
            setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.DEFERRED);
            markTransactionEnded();
        } catch (SQLException ex) {
            setTransactionMode(org.sqlite.SQLiteConfig.TransactionMode.DEFERRED);
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
        } finally {
            // Last, not first: until the driver has let the file go this connection still holds
            // and locks it, and giving the claim back sooner lets another connection start
            // rewriting it underneath a rollback that has not finished.
            releaseOpenDatabase(openKey);
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
                // Transaction control goes through JDBC rather than to the driver as SQL. This
                // connection's transaction is JDBC's autocommit flag, and handing it a raw BEGIN
                // would leave the two disagreeing: a following commitTransaction() calls
                // conn.commit() on a connection JDBC still believes is autocommitting, which
                // throws, and a raw COMMIT after beginTransaction() would leave autocommit off
                // over a transaction that has ended.
                if (!executeTransactionControl(statements[iter])) {
                    s.execute(statements[iter]);
                }
                // Recorded as each one succeeds, so a script that throws partway leaves the
                // state describing what actually ran.
                noteScriptTransactionControl(statements[iter]);
            }
        } catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        } finally {
            cleanup(s);
        }
    }

    /// Rejects a call that supplies the wrong number of bind arguments.
    ///
    /// The driver does not check this itself: too few leaves the trailing placeholders unbound,
    /// and too many walks off the end of its parameter array and raises an
    /// ArrayIndexOutOfBoundsException, which is not the IOException this API promises.
    private void checkParameterCount(PreparedStatement s, int supplied) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        int declared;
        try {
            declared = s.getParameterMetaData().getParameterCount();
        } catch (SQLException ex) {
            // The driver could not say. Binding below will report anything it does detect.
            return;
        }
        if (declared != supplied) {
            // Closed here rather than by the caller: the query paths only clean up from their
            // SQLException handler, which this does not go through.
            cleanup(s);
            throw new IOException("The statement has " + declared + " parameters but "
                    + supplied + " were supplied");
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        if (runAsTransactionControl(sql)) {
            return;
        }
        PreparedStatement s = null;
        try {
            s = conn.prepareStatement(sql);
            checkParameterCount(s, params == null ? 0 : params.length);
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
        if (params == null) {
            // Only a null array means "no parameters at all". An explicitly empty one is still a
            // parameterized call, so it goes down the path below and is held to both the
            // single-statement rule and the parameter count -- otherwise
            // execute("INSERT ... VALUES (?)", new Object[0]) would run with the slot unbound.
            execute(sql);
            return;
        }
        checkOpen();
        requireSingleStatement(sql);
        PreparedStatement s = null;
        try {
            // Prepared before the transaction-control branch, not after it, because preparing is
            // what rejects "BEGIN ?" as the syntax error it is on every other port, and because a
            // parameterized call is held to its parameter count whatever the statement says.
            s = conn.prepareStatement(sql);
            checkParameterCount(s, params.length);
            if (transactionControlKeyword(sql) != null) {
                cleanup(s);
                s = null;
                runAsTransactionControl(sql);
                return;
            }
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
            checkParameterCount(s, params == null ? 0 : params.length);
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
            checkParameterCount(s, params.length);
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
            // The driver would report unbound parameters itself, but with its own wording. Going
            // through the same check keeps the message identical to the other overloads and to
            // the other ports.
            checkParameterCount(s, 0);
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
        checkNoTransactionForKeyChange();
        // Rotating a key rewrites the file under the new one for this connection only; another
        // connection keeps the old key and fails at the first rewritten page it reads.
        requireSoleConnectionForKeyChange(openKey);
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
            releaseKeyChangeClaim(openKey);
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
