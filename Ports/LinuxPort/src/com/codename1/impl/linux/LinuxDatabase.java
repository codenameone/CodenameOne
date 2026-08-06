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
package com.codename1.impl.linux;

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
 * Database for the native Linux port, backed by the bundled SQLite engine.
 *
 * Before this the port had no database at all: Database.openOrCreate returned null and calling
 * code failed with a NullPointerException.
 */
class LinuxDatabase extends Database {

    private long peer;

    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name, and changeKey() would otherwise have nothing to
     * resolve against.
     */
    private final String databaseName;

    private final List<CursorImpl> openCursors = new ArrayList<CursorImpl>();

    LinuxDatabase(String databaseName, String path, String key) throws IOException {
        this.databaseName = databaseName;
        peer = LinuxNative.sqlDbOpen(path);
        if (key != null) {
            if (!LinuxNative.sqlDbApplyKey(peer, key)) {
                LinuxNative.sqlDbClose(peer);
                peer = 0;
                throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                        "The supplied key does not decrypt this database");
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
            LinuxNative.sqlDbExecScript(peer, "BEGIN");
        } catch (IOException err) {
            inTransaction = false;
            throw err;
        }
    }

    @Override
    public void commitTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        LinuxNative.sqlDbExecScript(peer, "COMMIT");
        markTransactionEnded();
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        LinuxNative.sqlDbExecScript(peer, "ROLLBACK");
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
                LinuxNative.sqlDbExecScript(peer, "ROLLBACK");
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
        LinuxNative.sqlDbClose(closing);
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        String key = null;
        if (config != null && config.isEncrypted()) {
            key = config.resolveKeyMaterial(databaseName);
        }
        LinuxNative.sqlDbRekey(peer, key);
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        LinuxNative.sqlDbExecScript(peer, sql);
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = LinuxNative.sqlStmtPrepare(peer, sql);
        bindText(stmt, params);
        LinuxNative.sqlStmtExecuteAndFinalize(stmt);
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
        long stmt = LinuxNative.sqlStmtPrepare(peer, sql);
        bind(stmt, params);
        LinuxNative.sqlStmtExecuteAndFinalize(stmt);
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        return register(new CursorImpl(LinuxNative.sqlStmtPrepare(peer, sql)));
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = LinuxNative.sqlStmtPrepare(peer, sql);
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
        long stmt = LinuxNative.sqlStmtPrepare(peer, sql);
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
        int declared = LinuxNative.sqlStmtParameterCount(stmt);
        if (declared != supplied) {
            LinuxNative.sqlStmtFinalize(stmt);
            throw new IOException("The statement has " + declared + " parameters but "
                    + supplied + " were supplied");
        }
    }

    private void bindText(long stmt, String[] params) throws IOException {
        int len = params == null ? 0 : params.length;
        checkParameterCount(stmt, len);
        for (int iter = 0; iter < len; iter++) {
            if (params[iter] == null) {
                LinuxNative.sqlStmtBindNull(stmt, iter + 1);
            } else {
                LinuxNative.sqlStmtBindString(stmt, iter + 1, params[iter]);
            }
        }
    }

    private void bind(long stmt, Object[] params) throws IOException {
        checkParameterCount(stmt, params.length);
        for (int iter = 0; iter < params.length; iter++) {
            Object p = params[iter];
            int index = iter + 1;
            if (p == null) {
                LinuxNative.sqlStmtBindNull(stmt, index);
            } else if (p instanceof byte[]) {
                LinuxNative.sqlStmtBindBlob(stmt, index, (byte[]) p);
            } else if (p instanceof String) {
                LinuxNative.sqlStmtBindString(stmt, index, (String) p);
            } else if (p instanceof Double || p instanceof Float) {
                LinuxNative.sqlStmtBindDouble(stmt, index, ((Number) p).doubleValue());
            } else if (p instanceof Long || p instanceof Integer || p instanceof Short
                    || p instanceof Byte) {
                LinuxNative.sqlStmtBindLong(stmt, index, ((Number) p).longValue());
            } else if (p instanceof Boolean) {
                LinuxNative.sqlStmtBindLong(stmt, index, ((Boolean) p).booleanValue() ? 1 : 0);
            } else {
                LinuxNative.sqlStmtBindString(stmt, index, p.toString());
            }
        }
    }

    /** Cursor over a compiled statement; seeks by resetting and stepping again. */
    static class CursorImpl extends AbstractDBCursor {
        private long stmt;
        LinuxDatabase owner;

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
                    LinuxNative.sqlStmtFinalize(closing);
                } catch (Throwable alreadyGone) {
                    // The connection is going away regardless; nothing useful to report.
                }
            }
            invalidate();
        }

        @Override
        protected void rewind() throws IOException {
            LinuxNative.sqlStmtReset(stmt);
        }

        @Override
        protected boolean stepForward() throws IOException {
            return LinuxNative.sqlStmtStep(stmt);
        }

        @Override
        protected void closeImpl() throws IOException {
            if (owner != null) {
                owner.unregister(this);
            }
            if (stmt != 0) {
                long closing = stmt;
                stmt = 0;
                LinuxNative.sqlStmtFinalize(closing);
            }
        }

        @Override
        protected int columnCount() throws IOException {
            return LinuxNative.sqlColCount(stmt);
        }

        @Override
        protected String columnLabel(int columnIndex) throws IOException {
            return LinuxNative.sqlColName(stmt, columnIndex);
        }

        @Override
        protected boolean isNullAt(int index) throws IOException {
            return LinuxNative.sqlColIsNull(stmt, index);
        }

        @Override
        protected String readString(int index) throws IOException {
            return LinuxNative.sqlColString(stmt, index);
        }

        @Override
        protected byte[] readBlob(int index) throws IOException {
            return LinuxNative.sqlColBlob(stmt, index);
        }

        @Override
        protected double readDouble(int index) throws IOException {
            return LinuxNative.sqlColDouble(stmt, index);
        }

        @Override
        protected long readLong(int index) throws IOException {
            return LinuxNative.sqlColLong(stmt, index);
        }
    }
}
