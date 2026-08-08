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
package com.codename1.impl.html5.database;

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
 * Database for the JavaScript port, backed by SQLite compiled to WebAssembly.
 *
 * The previous implementation sat on WebSQL, which Chrome removed and Firefox never shipped, and
 * which could not do transactions, blobs or seeking. The engine now runs inside the application's
 * own worker, so it behaves like every other port.
 */
public class DatabaseImpl extends Database {

    private long peer;

    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name, and changeKey() would otherwise have nothing to
     * resolve against.
     */
    private final String databaseName;

    private final List<CursorImpl> openCursors = new ArrayList<CursorImpl>();

    public DatabaseImpl(String name, String key) throws IOException {
        this.databaseName = name;
        peer = SQLiteNative.open(name, key);
        if (peer == 0) {
            if (SQLiteNative.lastOpenWasWrongKey()) {
                throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                        "The supplied key does not decrypt this database");
            }
            // Storage or corruption, which no key can resolve. Reporting it as a key failure
            // would have an application prompting for a passphrase that cannot help, and for an
            // open with no key at all the diagnosis is impossible on its face.
            throw new IOException("The database " + name + " could not be opened: "
                    + SQLiteNative.lastError());
        }
    }

    /**
     * Turns a failed native call into the IOException this API promises.
     *
     * The bindings report failure with a sentinel rather than by throwing: an exception raised
     * inside one does not reach here as a Java throwable, it unwinds the worker and hangs every
     * thread waiting on the call.
     */
    private static void checkNative(boolean ok) throws IOException {
        if (!ok) {
            throw new IOException(SQLiteNative.lastError());
        }
    }

    /** Raises the prepare failure the binding reported through its zero sentinel. */
    private static void checkPrepared(long stmt) throws IOException {
        if (stmt == 0) {
            throw new IOException(SQLiteNative.lastError());
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
            checkNative(SQLiteNative.execScript(peer, "BEGIN"));
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
            checkNative(SQLiteNative.execScript(peer, "COMMIT"));
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
            checkNative(SQLiteNative.execScript(peer, "ROLLBACK"));
        } catch (Throwable ignored) {
            // Nothing to recover: the caller is already reporting the commit failure.
        }
    }

    @Override
    public void rollbackTransaction() throws IOException {
        checkOpen();
        checkEndTransaction();
        checkNative(SQLiteNative.execScript(peer, "ROLLBACK"));
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
                checkNative(SQLiteNative.execScript(peer, "ROLLBACK"));
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
        // Reported, not swallowed. The peer is already cleared so there is no retry, which makes
        // this the only chance to say the data may not have reached storage.
        checkNative(SQLiteNative.close(closing));
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        checkNoTransactionForKeyChange();
        String key = null;
        if (config != null && config.isEncrypted()) {
            key = config.resolveKeyMaterial(databaseName);
        }
        checkNative(SQLiteNative.rekey(peer, key));
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        // The engine runs the whole script; see the iOS port for why the failure path matters.
        boolean completed = false;
        try {
            checkNative(SQLiteNative.execScript(peer, sql));
            completed = true;
        } finally {
            noteScriptTransactionControl(sql, completed);
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
        bindText(stmt, params);
        checkNative(SQLiteNative.executeAndFinish(stmt));
        // A parameterized call is a single statement, and "BEGIN" is a legal one: the guard on
        // changeKey depends on knowing that a transaction was opened, whichever entry point
        // opened it.
        noteScriptTransactionControl(sql);
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
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
        bind(stmt, params);
        checkNative(SQLiteNative.executeAndFinish(stmt));
        // A parameterized call is a single statement, and "BEGIN" is a legal one: the guard on
        // changeKey depends on knowing that a transaction was opened, whichever entry point
        // opened it.
        noteScriptTransactionControl(sql);
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
        // A statement with placeholders and no arguments would otherwise run with every slot left
        // as NULL rather than reporting the missing parameters. The check is a no-op in legacy
        // mode, where running it unbound is the behaviour applications were written against.
        // Finalized here because this path does not go through the bind helpers, which are what
        // own the statement everywhere else.
        try {
            checkParameterCount(stmt, 0);
        } catch (IOException err) {
            SQLiteNative.finish(stmt);
            throw err;
        }
        return register(new CursorImpl(stmt));
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
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
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
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
        int declared = SQLiteNative.parameterCount(stmt);
        if (declared != supplied) {
            // Deliberately does NOT finalize. The bind helpers that call this own the statement
            // and finalize it when anything in them throws, so doing it here too would finalize
            // the same pointer twice -- undefined behaviour, and a crash rather than the
            // parameter-count error this is supposed to report.
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
                    checkNative(SQLiteNative.bindNull(stmt, iter + 1));
                } else {
                    checkNative(SQLiteNative.bindString(stmt, iter + 1, params[iter]));
                }
            }
        } catch (IOException err) {
            SQLiteNative.finish(stmt);
            throw err;
        } catch (RuntimeException err) {
            SQLiteNative.finish(stmt);
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
                    checkNative(SQLiteNative.bindNull(stmt, index));
                } else if (p instanceof byte[]) {
                    checkNative(SQLiteNative.bindBlob(stmt, index, (byte[]) p));
                } else if (p instanceof String) {
                    checkNative(SQLiteNative.bindString(stmt, index, (String) p));
                } else if (p instanceof Double || p instanceof Float) {
                    checkNative(SQLiteNative.bindDouble(stmt, index, ((Number) p).doubleValue()));
                } else if (p instanceof Long || p instanceof Integer || p instanceof Short
                        || p instanceof Byte) {
                    checkNative(SQLiteNative.bindLong(stmt, index, ((Number) p).longValue()));
                } else if (p instanceof Boolean) {
                    checkNative(SQLiteNative.bindLong(stmt, index, ((Boolean) p).booleanValue() ? 1 : 0));
                } else {
                    checkNative(SQLiteNative.bindString(stmt, index, p.toString()));
                }
            }
        } catch (IOException err) {
            SQLiteNative.finish(stmt);
            throw err;
        } catch (RuntimeException err) {
            SQLiteNative.finish(stmt);
            throw err;
        }
    }

    /** Cursor over a compiled statement; seeks by resetting and stepping again. */
    static class CursorImpl extends AbstractDBCursor {
        private long stmt;
        DatabaseImpl owner;

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
                    SQLiteNative.finish(closing);
                } catch (Throwable alreadyGone) {
                    // The connection is going away regardless; nothing useful to report.
                }
            }
            invalidate();
        }

        @Override
        protected void rewind() throws IOException {
            checkNative(SQLiteNative.reset(stmt));
        }

        @Override
        protected boolean stepForward() throws IOException {
            int stepped = SQLiteNative.step(stmt);
            if (stepped < 0) {
                throw new IOException(SQLiteNative.lastError());
            }
            return stepped == 1;
        }

        @Override
        protected void closeImpl() throws IOException {
            if (owner != null) {
                owner.unregister(this);
            }
            if (stmt != 0) {
                long closing = stmt;
                stmt = 0;
                SQLiteNative.finish(closing);
            }
        }

        @Override
        protected int columnCount() throws IOException {
            return SQLiteNative.columnCount(stmt);
        }

        @Override
        protected String columnLabel(int columnIndex) throws IOException {
            return SQLiteNative.columnName(stmt, columnIndex);
        }

        @Override
        protected boolean isNullAt(int index) throws IOException {
            int nullness = SQLiteNative.columnIsNull(stmt, index);
            if (nullness < 0) {
                // Reported rather than answered. A bad column index would otherwise come back as
                // a null value, and the getter would hand out null or zero - data the caller
                // cannot tell from a real NULL.
                throw new IOException(SQLiteNative.lastError());
            }
            return nullness == 1;
        }

        @Override
        protected String readString(int index) throws IOException {
            return SQLiteNative.columnString(stmt, index);
        }

        @Override
        protected byte[] readBlob(int index) throws IOException {
            return SQLiteNative.columnBlob(stmt, index);
        }

        @Override
        protected double readDouble(int index) throws IOException {
            return SQLiteNative.columnDouble(stmt, index);
        }

        @Override
        protected long readLong(int index) throws IOException {
            return SQLiteNative.columnLong(stmt, index);
        }
    }
}
