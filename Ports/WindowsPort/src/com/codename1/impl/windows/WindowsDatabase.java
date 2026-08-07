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
package com.codename1.impl.windows;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.db.DatabaseEncryptionException;
import com.codename1.impl.AbstractDBCursor;
import com.codename1.impl.SQLStatementSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database for the native Windows port, backed by the bundled SQLite engine.
 *
 * Before this the port had no database at all: Database.openOrCreate returned null and calling
 * code failed with a NullPointerException.
 */
class WindowsDatabase extends Database {

    private long peer;

    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name, and changeKey() would otherwise have nothing to
     * resolve against.
     */
    private final String databaseName;

    private final List<CursorImpl> openCursors = new ArrayList<CursorImpl>();

    /// SQLite's success code.
    private static final int SQLITE_OK = 0;

    /// SQLite's "this is not a database file", which is what a wrong key looks like.
    private static final int SQLITE_NOTADB = 26;

    WindowsDatabase(String databaseName, String path, String key) throws IOException {
        this.databaseName = databaseName;
        peer = WindowsNative.sqlDbOpen(path);
        if (key != null) {
            int status = WindowsNative.sqlDbApplyKeyStatus(peer, key);
            if (status != SQLITE_OK) {
                WindowsNative.sqlDbClose(peer);
                peer = 0;
                if (status == SQLITE_NOTADB) {
                    throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                            "The supplied key does not decrypt this database");
                }
                // A corrupt image or a read error, which no key repairs. Reporting it as a wrong
                // key would have an application prompting for a passphrase that cannot help.
                throw new IOException("The database " + databaseName + " could not be read, "
                        + "SQLite result " + status);
            }
        }
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
            WindowsNative.sqlDbExecScript(peer, "BEGIN");
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
            WindowsNative.sqlDbExecScript(peer, "COMMIT");
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
            WindowsNative.sqlDbExecScript(peer, "ROLLBACK");
        } catch (Throwable ignored) {
            // Nothing to recover: the caller is already reporting the commit failure.
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        WindowsNative.sqlDbExecScript(peer, "ROLLBACK");
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
                WindowsNative.sqlDbExecScript(peer, "ROLLBACK");
            } catch (IOException ignored) {
                // Best effort; the close below is what matters.
            }
        }
        CursorImpl[] cursors = openCursors.toArray(new CursorImpl[openCursors.size()]);
        openCursors.clear();
        for (int iter = 0; iter < cursors.length; iter++) {
            cursors[iter].databaseClosed();
        }
        long closing = peer;
        peer = 0;
        WindowsNative.sqlDbClose(closing);
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        String key = null;
        if (config != null && config.isEncrypted()) {
            key = config.resolveKeyMaterial(databaseName);
        }
        WindowsNative.sqlDbRekey(peer, key);
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        WindowsNative.sqlDbExecScript(peer, sql);
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = WindowsNative.sqlStmtPrepare(peer, sql);
        bindText(stmt, params);
        WindowsNative.sqlStmtExecuteAndFinalize(stmt);
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
        long stmt = WindowsNative.sqlStmtPrepare(peer, sql);
        bind(stmt, params);
        WindowsNative.sqlStmtExecuteAndFinalize(stmt);
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = WindowsNative.sqlStmtPrepare(peer, sql);
        // A statement with placeholders and no arguments would otherwise run with every slot left
        // as NULL rather than reporting the missing parameters. The check is a no-op in legacy
        // mode, where running it unbound is the behaviour applications were written against.
        checkParameterCount(stmt, 0);
        return register(new CursorImpl(stmt));
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = WindowsNative.sqlStmtPrepare(peer, sql);
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
        long stmt = WindowsNative.sqlStmtPrepare(peer, sql);
        bind(stmt, params);
        return register(new CursorImpl(stmt));
    }

    private Cursor register(CursorImpl cursor) {
        cursor.owner = this;
        openCursors.add(cursor);
        return cursor;
    }

    void unregister(CursorImpl cursor) {
        openCursors.remove(cursor);
    }

    private void checkParameterCount(long stmt, int supplied) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        int declared = WindowsNative.sqlStmtParameterCount(stmt);
        if (declared != supplied) {
            WindowsNative.sqlStmtFinalize(stmt);
            throw new IOException("The statement has " + declared + " parameters but "
                    + supplied + " were supplied");
        }
    }

    private void bindText(long stmt, String[] params) throws IOException {
        // Finalized if anything in here throws. The statement is not registered as a cursor
        // until after binding, so a rejected parameter would otherwise strand its peer -- and
        // an unfinalized statement keeps the connection alive after close().
        try {
            int len = params == null ? 0 : params.length;
            checkParameterCount(stmt, len);
            for (int iter = 0; iter < len; iter++) {
                if (params[iter] == null) {
                    WindowsNative.sqlStmtBindNull(stmt, iter + 1);
                } else {
                    WindowsNative.sqlStmtBindString(stmt, iter + 1, params[iter]);
                }
            }
        } catch (IOException err) {
            WindowsNative.sqlStmtFinalize(stmt);
            throw err;
        } catch (RuntimeException err) {
            WindowsNative.sqlStmtFinalize(stmt);
            throw err;
        }
    }

    private void bind(long stmt, Object[] params) throws IOException {
        // Finalized if anything in here throws. The statement is not registered as a cursor
        // until after binding, so a rejected parameter would otherwise strand its peer -- and
        // an unfinalized statement keeps the connection alive after close().
        try {
            checkParameterCount(stmt, params.length);
            for (int iter = 0; iter < params.length; iter++) {
                Object p = params[iter];
                int index = iter + 1;
                if (p == null) {
                    WindowsNative.sqlStmtBindNull(stmt, index);
                } else if (p instanceof byte[]) {
                    WindowsNative.sqlStmtBindBlob(stmt, index, (byte[]) p);
                } else if (p instanceof String) {
                    WindowsNative.sqlStmtBindString(stmt, index, (String) p);
                } else if (p instanceof Double || p instanceof Float) {
                    WindowsNative.sqlStmtBindDouble(stmt, index, ((Number) p).doubleValue());
                } else if (p instanceof Long || p instanceof Integer || p instanceof Short
                        || p instanceof Byte) {
                    WindowsNative.sqlStmtBindLong(stmt, index, ((Number) p).longValue());
                } else if (p instanceof Boolean) {
                    WindowsNative.sqlStmtBindLong(stmt, index, ((Boolean) p).booleanValue() ? 1 : 0);
                } else {
                    WindowsNative.sqlStmtBindString(stmt, index, p.toString());
                }
            }
        } catch (IOException err) {
            WindowsNative.sqlStmtFinalize(stmt);
            throw err;
        } catch (RuntimeException err) {
            WindowsNative.sqlStmtFinalize(stmt);
            throw err;
        }
    }

    /** Cursor over a compiled statement; seeks by resetting and stepping again. */
    static class CursorImpl extends AbstractDBCursor {
        private long stmt;
        WindowsDatabase owner;

        CursorImpl(long stmt) {
            this.stmt = stmt;
        }

        void databaseClosed() {
            // Finalize first. sqlite3_close_v2 with an outstanding statement leaves a zombie
            // connection alive until that statement is finalized, and dropping the only handle to
            // it here would mean that could never happen.
            if (stmt != 0) {
                long closing = stmt;
                stmt = 0;
                try {
                    WindowsNative.sqlStmtFinalize(closing);
                } catch (Throwable alreadyGone) {
                    // The connection is going away regardless; nothing useful to report.
                }
            }
            invalidate();
        }

        @Override
        protected void rewind() throws IOException {
            WindowsNative.sqlStmtReset(stmt);
        }

        @Override
        protected boolean stepForward() throws IOException {
            return WindowsNative.sqlStmtStep(stmt);
        }

        @Override
        protected void closeImpl() throws IOException {
            if (owner != null) {
                owner.unregister(this);
            }
            if (stmt != 0) {
                long closing = stmt;
                stmt = 0;
                WindowsNative.sqlStmtFinalize(closing);
            }
        }

        @Override
        protected int columnCount() throws IOException {
            return WindowsNative.sqlColCount(stmt);
        }

        @Override
        protected String columnLabel(int columnIndex) throws IOException {
            return WindowsNative.sqlColName(stmt, columnIndex);
        }

        @Override
        protected boolean isNullAt(int index) throws IOException {
            return WindowsNative.sqlColIsNull(stmt, index);
        }

        @Override
        protected String readString(int index) throws IOException {
            return WindowsNative.sqlColString(stmt, index);
        }

        @Override
        protected byte[] readBlob(int index) throws IOException {
            return WindowsNative.sqlColBlob(stmt, index);
        }

        @Override
        protected double readDouble(int index) throws IOException {
            return WindowsNative.sqlColDouble(stmt, index);
        }

        @Override
        protected long readLong(int index) throws IOException {
            return WindowsNative.sqlColLong(stmt, index);
        }
    }
}
