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
package com.codename1.impl.ios;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.db.DatabaseEncryptionException;
import com.codename1.impl.AbstractDBCursor;
import com.codename1.impl.SQLStatementSplitter;

import java.io.IOException;
import java.util.Vector;

/**
 * Implementation of the database SQL API
 *
 * @author Shai Almog
 */
class DatabaseImpl extends Database {
    private long peer;

    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name, and changeKey() would otherwise have nothing to
     * resolve against.
     */
    private final String databaseName;

    /**
     * Cursors opened from this database. Closing the database finalizes its statements, so the
     * cursors have to be marked dead rather than left holding freed pointers.
     */
    private final Vector openCursors = new Vector();

    public DatabaseImpl(String databaseName, String path) {
        this.databaseName = databaseName;
        peer = IOSImplementation.nativeInstance.sqlDbCreateAndOpen(path);
    }

    /// SQLite's success code.
    private static final int SQLITE_OK = 0;

    /// SQLite's "this is not a database file", which is what a wrong key looks like.
    private static final int SQLITE_NOTADB = 26;

    public DatabaseImpl(String databaseName, String path, String key) throws IOException {
        this.databaseName = databaseName;
        peer = IOSImplementation.nativeInstance.sqlDbCreateAndOpen(path);
        int status = IOSImplementation.nativeInstance.sqlDbApplyKeyStatus(peer, key);
        if (status != SQLITE_OK) {
            IOSImplementation.nativeInstance.sqlDbClose(peer);
            peer = 0;
            if (status == SQLITE_NOTADB) {
                throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                        "The supplied key does not decrypt this database");
            }
            // A corrupt image or a read error. Reporting it as a wrong key would send an
            // application that follows the error codes into prompting for a passphrase forever.
            throw new IOException("The database " + databaseName + " could not be read, SQLite "
                    + "result " + status);
        }
    }

    public static long getPeer(Object db) {
        if (db instanceof DatabaseImpl) {
            return ((DatabaseImpl)db).peer;
        }
        return 0l;
    }

    private void checkOpen() throws IOException {
        if (peer == 0) {
            throw new IOException("This database has been closed");
        }
    }

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
    public void beginTransaction() throws IOException {
        checkOpen();
        checkBeginTransaction();
        try {
            executeScript("BEGIN");
        } catch (IOException err) {
            inTransaction = false;
            throw err;
        }
    }

    @Override
    public void commitTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        try {
            executeScript("COMMIT");
        } catch (IOException err) {
            // SQLite leaves the transaction open when COMMIT fails, so discard it here.
            rollbackQuietly();
            throw abandonFailedCommit(err);
        }
        markTransactionEnded();
    }

    /// Rolls back without reporting a failure, for the path where a commit has already failed and
    /// the engine may or may not have left the transaction open.
    private void rollbackQuietly() {
        try {
            executeScript("ROLLBACK");
        } catch (Throwable ignored) {
            // Nothing to recover: the caller is already reporting the commit failure.
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        executeScript("ROLLBACK");
        markTransactionEnded();
    }

    @Override
    public void close() throws IOException {
        if (peer == 0) {
            return;
        }
        if (inTransaction) {
            inTransaction = false;
            try {
                executeScript("ROLLBACK");
            } catch (IOException ignored) {
                // Best effort; the close below is what matters.
            }
        }
        // Invalidate before closing: the statements belong to the connection and go with it.
        Object[] cursors = new Object[openCursors.size()];
        openCursors.copyInto(cursors);
        openCursors.removeAllElements();
        for (int iter = 0; iter < cursors.length; iter++) {
            ((CursorImpl)cursors[iter]).databaseClosed();
        }
        long closing = peer;
        peer = 0;
        IOSImplementation.nativeInstance.sqlDbClose(closing);
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        String key = null;
        if (config != null && config.isEncrypted()) {
            key = config.resolveKeyMaterial(databaseName);
        }
        IOSImplementation.nativeInstance.sqlDbRekey(peer, key);
    }

    private void executeScript(String sql) throws IOException {
        IOSImplementation.nativeInstance.sqlDbExecScript(peer, sql);
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        // sqlite3_exec runs a whole script, which is the portable contract.
        executeScript(sql);
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        // Bind through the statement API rather than the older sqlDbExec, which binds only the
        // values it is given and ignores the placeholder count: too few left placeholders bound to
        // NULL and extra values were discarded, both silently.
        long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
        bindText(stmt, params);
        IOSImplementation.nativeInstance.sqlStmtExecuteAndFinalize(stmt);
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
        if (isLegacyBehavior()) {
            // Everything used to be stringified here, which stored an Integer as TEXT.
            execute(sql, coerceToText(params, "execute"));
            return;
        }
        long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
        bind(stmt, params);
        IOSImplementation.nativeInstance.sqlStmtExecuteAndFinalize(stmt);
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
        bindText(stmt, params);
        return register(new CursorImpl(stmt));
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
        if (isLegacyBehavior()) {
            return executeQuery(sql, coerceToText(params, "executeQuery"));
        }
        long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
        bind(stmt, params);
        return register(new CursorImpl(stmt));
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        if (isLegacyBehavior()) {
            return register(new CursorImpl(
                    IOSImplementation.nativeInstance.sqlDbExecQuery(peer, sql, null)));
        }
        // Prepared rather than handed to sqlDbExecQuery, which binds nothing when given a null
        // argument array: a statement with placeholders would run with every slot left as NULL
        // instead of reporting the parameters the caller did not supply.
        long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
        checkParameterCount(stmt, 0);
        return register(new CursorImpl(stmt));
    }

    private Cursor register(CursorImpl cursor) {
        cursor.owner = this;
        openCursors.addElement(cursor);
        return cursor;
    }

    void unregister(CursorImpl cursor) {
        openCursors.removeElement(cursor);
    }

    /** Binds every value as text, rejecting a count that does not match the statement. */
    private void bindText(long stmt, String[] params) throws IOException {
        int len = params == null ? 0 : params.length;
        checkParameterCount(stmt, len);
        for (int iter = 0; iter < len; iter++) {
            if (params[iter] == null) {
                IOSImplementation.nativeInstance.sqlStmtBindNull(stmt, iter + 1);
            } else {
                IOSImplementation.nativeInstance.sqlStmtBindString(stmt, iter + 1, params[iter]);
            }
        }
    }

    private void checkParameterCount(long stmt, int supplied) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        int declared = IOSImplementation.nativeInstance.sqlStmtParameterCount(stmt);
        if (declared != supplied) {
            IOSImplementation.nativeInstance.sqlStmtFinalize(stmt);
            throw new IOException("The statement has " + declared + " parameters but "
                    + supplied + " were supplied");
        }
    }

    /** Binds by runtime type, so an Integer is stored as INTEGER rather than as its text form. */
    private void bind(long stmt, Object[] params) throws IOException {
        checkParameterCount(stmt, params.length);
        for (int iter = 0; iter < params.length; iter++) {
            Object p = params[iter];
            int index = iter + 1;
            if (p == null) {
                IOSImplementation.nativeInstance.sqlStmtBindNull(stmt, index);
            } else if (p instanceof byte[]) {
                IOSImplementation.nativeInstance.sqlStmtBindBlob(stmt, index, (byte[])p);
            } else if (p instanceof String) {
                IOSImplementation.nativeInstance.sqlStmtBindString(stmt, index, (String)p);
            } else if (p instanceof Double || p instanceof Float) {
                IOSImplementation.nativeInstance.sqlStmtBindDouble(stmt, index,
                        ((Number)p).doubleValue());
            } else if (p instanceof Long || p instanceof Integer || p instanceof Short
                    || p instanceof Byte) {
                IOSImplementation.nativeInstance.sqlStmtBindLong(stmt, index,
                        ((Number)p).longValue());
            } else if (p instanceof Boolean) {
                IOSImplementation.nativeInstance.sqlStmtBindLong(stmt, index,
                        ((Boolean)p).booleanValue() ? 1 : 0);
            } else {
                IOSImplementation.nativeInstance.sqlStmtBindString(stmt, index, p.toString());
            }
        }
    }

    /**
     * Cursor over a compiled statement.
     *
     * A sqlite3_stmt only steps forward, so seeking backwards resets it and steps again. That is
     * what AbstractDBCursor is built around, and it is the same strategy Android's own cursor uses
     * when a requested row falls outside its window. Buffering rows instead would mean copying
     * every column of every row stepped past, blobs included.
     */
    static class CursorImpl extends AbstractDBCursor {
        private long stmt;
        DatabaseImpl owner;

        CursorImpl(long stmt) {
            this.stmt = stmt;
        }

        /** Marks the cursor dead because the connection that owned its statement has gone. */
        void databaseClosed() {
            // Finalize first. sqlite3_close_v2 with an outstanding statement leaves a zombie
            // connection alive until that statement is finalized, and dropping the only handle to
            // it here would mean that could never happen.
            if (stmt != 0) {
                long closing = stmt;
                stmt = 0;
                try {
                    IOSImplementation.nativeInstance.sqlStmtFinalize(closing);
                } catch (Throwable alreadyGone) {
                    // The connection is going away regardless; nothing useful to report.
                }
            }
            invalidate();
        }

        @Override
        protected void rewind() throws IOException {
            IOSImplementation.nativeInstance.sqlStmtReset(stmt);
        }

        @Override
        protected boolean stepForward() throws IOException {
            return IOSImplementation.nativeInstance.sqlStmtStep(stmt);
        }

        @Override
        protected void closeImpl() throws IOException {
            if (owner != null) {
                owner.unregister(this);
            }
            if (stmt != 0) {
                long closing = stmt;
                stmt = 0;
                IOSImplementation.nativeInstance.sqlStmtFinalize(closing);
            }
        }

        @Override
        protected int columnCount() throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorGetColumnCount(stmt);
        }

        @Override
        protected String columnLabel(int columnIndex) throws IOException {
            return IOSImplementation.nativeInstance.sqlGetColName(stmt, columnIndex);
        }

        @Override
        protected boolean isNullAt(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorNullValueAtColumn(stmt, index);
        }

        @Override
        protected String readString(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnString(stmt, index);
        }

        @Override
        protected byte[] readBlob(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnBlob(stmt, index);
        }

        @Override
        protected double readDouble(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnDouble(stmt, index);
        }

        @Override
        protected long readLong(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorValueAtColumnLong(stmt, index);
        }

        @Override
        protected boolean isLegacyFirstRewind() {
            // first() used to be a bare sqlite3_reset that reported success even for an empty
            // result set, leaving the statement off a row.
            return Database.isLegacyBehavior();
        }
    }
}
