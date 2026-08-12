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
import com.codename1.impl.SQLText;

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
     * Cursors opened from this database. Closing the database finalizes its statements, so the
     * cursors have to be marked dead rather than left holding freed pointers.
     */
    private final Vector openCursors = new Vector();

    /** The file this connection is open on, for the shared registry a key change consults. */
    private final String openKey;

    public DatabaseImpl(String databaseName, String path) throws IOException {
        // Normalized, so two spellings of one path are one registry entry: the claim a key
        // change takes is worth nothing if the other connection is filed under "/a/./b".
        this.openKey = normalizeDatabasePathKey(path);
        // Registration first, because it is also the refusal: a key change in progress is rewriting
        // this file, and opening it before asking would touch it mid-rewrite and leave the handle
        // behind when the refusal arrived.
        registerOpenDatabase(openKey);
        boolean opened = false;
        try {
            peer = IOSImplementation.nativeInstance.sqlDbCreateAndOpen(path);
            opened = true;
        } finally {
            if (!opened) {
                releaseOpenDatabase(openKey);
            }
        }
    }

    /// SQLite's success code.
    private static final int SQLITE_OK = 0;

    /// SQLite's "this is not a database file", which is what a wrong key looks like.
    private static final int SQLITE_NOTADB = 26;

    public DatabaseImpl(String databaseName, String path, String key) throws IOException {
        // Normalized, so two spellings of one path are one registry entry: the claim a key
        // change takes is worth nothing if the other connection is filed under "/a/./b".
        this.openKey = normalizeDatabasePathKey(path);
        // See the other constructor: the registration is what refuses an open during a key change,
        // so it has to happen before the file is touched.
        registerOpenDatabase(openKey);
        boolean opened = false;
        try {
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
            opened = true;
        } finally {
            if (!opened) {
                releaseOpenDatabase(openKey);
            }
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
        // Last, not first: until the native handle is gone this connection still holds and locks
        // the file, and giving the claim back sooner lets another connection start rewriting it
        // underneath a rollback that has not finished.
        releaseOpenDatabase(openKey);
    }

    @Override
    public void changeKey(DatabaseConfig config) throws IOException {
        checkOpen();
        checkNoTransactionForKeyChange();
        // Rotating a key rewrites the file under the new one for this connection only; another
        // connection keeps the old key and fails at the first rewritten page it reads.
        requireSoleConnectionForKeyChange(openKey);
        try {
            String key = null;
            if (config != null && config.isEncrypted()) {
                // The resolved file, as the open path does: an implicit managed key is stored
                // under what is passed here, and re-keying under the raw name would write a second
                // key that the next open, which resolves the file, would not find.
                key = config.resolveKeyMaterial(openKey);
            }
            IOSImplementation.nativeInstance.sqlDbRekey(peer, key);
        } finally {
            releaseKeyChangeClaim(openKey);
        }
    }

    private void executeScript(String sql) throws IOException {
        IOSImplementation.nativeInstance.sqlDbExecScript(peer, sql);
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        // sqlite3_exec runs a whole script, which is the portable contract -- and which means a
        // failure partway leaves everything before the failing statement done.
        try {
            executeScript(sql);
        } finally {
            // Read back from the engine rather than inferred from the script. sqlite3_exec stops
            // at the statement that failed, so a trailing COMMIT in the text may never have run.
            // The names first: an outermost SAVEPOINT opens a transaction that only its own
            // RELEASE ends, and the engine reports a boolean without saying which savepoint owns
            // it. A later RELEASE arriving through a parameterized overload -- which has no engine
            // read of its own -- would otherwise go unrecognized and leave this believing a
            // transaction was still open forever.
            noteScriptTransactionControl(sql);
            noteEngineTransactionState(IOSImplementation.nativeInstance.sqlDbInTransaction(peer));
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        // Bind through the statement API rather than the older sqlDbExec, which binds only the
        // values it is given and ignores the placeholder count: too few left placeholders bound to
        // NULL and extra values were discarded, both silently.
        try {
            long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
            bindText(stmt, params);
            IOSImplementation.nativeInstance.sqlStmtExecuteAndFinalize(stmt);
            // The names on success only -- a statement that failed opened no savepoint.
            noteScriptTransactionControl(sql);
        } finally {
            // The engine either way. A constraint with ON CONFLICT ROLLBACK ends the transaction
            // as it fails, so reading only on the success path would hold the flag over a
            // transaction that is gone and refuse every begin and key change until close.
            noteEngineTransactionState(IOSImplementation.nativeInstance.sqlDbInTransaction(peer));
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
        if (isLegacyBehavior()) {
            // Everything used to be stringified here, which stored an Integer as TEXT.
            execute(sql, coerceToText(params, "execute"));
            return;
        }
        try {
            long stmt = IOSImplementation.nativeInstance.sqlStmtPrepare(peer, sql);
            bind(stmt, params);
            IOSImplementation.nativeInstance.sqlStmtExecuteAndFinalize(stmt);
            // The names on success only -- a statement that failed opened no savepoint.
            noteScriptTransactionControl(sql);
        } finally {
            // The engine either way. A constraint with ON CONFLICT ROLLBACK ends the transaction
            // as it fails, so reading only on the success path would hold the flag over a
            // transaction that is gone and refuse every begin and key change until close.
            noteEngineTransactionState(IOSImplementation.nativeInstance.sqlDbInTransaction(peer));
        }
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
        // Finalized here because this path does not go through the bind helpers, which are what
        // own the statement everywhere else.
        try {
            checkParameterCount(stmt, 0);
        } catch (IOException err) {
            IOSImplementation.nativeInstance.sqlStmtFinalize(stmt);
            throw err;
        }
        return register(new CursorImpl(stmt));
    }

    private Cursor register(CursorImpl cursor) {
        cursor.owner = this;
        openCursors.addElement(cursor);
        return cursor;
    }


    /// Re-reads the transaction state from the engine.
    ///
    /// A cursor calls this when a step fails, because the failure may have ended the transaction
    /// underneath the tracking. Separate from the read the execute paths do inline only because a
    /// cursor is not this class and cannot reach the inherited hook itself.
    void reconcileTransactionState() {
        if (peer == 0) {
            return;
        }
        noteEngineTransactionState(IOSImplementation.nativeInstance.sqlDbInTransaction(peer));
    }

    void unregister(CursorImpl cursor) {
        openCursors.removeElement(cursor);
    }

    /** Binds every value as text, rejecting a count that does not match the statement. */
    private void bindText(long stmt, String[] params) throws IOException {
        // Finalized if anything in here throws. The statement is not registered as a cursor
        // until after binding, so a rejected parameter would otherwise strand its peer -- and
        // an unfinalized statement keeps the connection alive after close().
        try {
            int len = params == null ? 0 : params.length;
            checkParameterCount(stmt, len);
            for (int iter = 0; iter < len; iter++) {
                if (params[iter] == null) {
                    IOSImplementation.nativeInstance.sqlStmtBindNull(stmt, iter + 1);
                } else {
                    IOSImplementation.nativeInstance.sqlStmtBindText(stmt, iter + 1,
                            SQLText.toUTF8(params[iter]));
                }
            }
        } catch (IOException err) {
            IOSImplementation.nativeInstance.sqlStmtFinalize(stmt);
            throw err;
        } catch (RuntimeException err) {
            IOSImplementation.nativeInstance.sqlStmtFinalize(stmt);
            throw err;
        }
    }

    private void checkParameterCount(long stmt, int supplied) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        int declared = IOSImplementation.nativeInstance.sqlStmtParameterCount(stmt);
        if (declared != supplied) {
            // Deliberately does NOT finalize. The bind helpers that call this own the statement
            // and finalize it when anything in them throws, so doing it here too would finalize
            // the same pointer twice -- undefined behaviour, and a crash rather than the
            // parameter-count error this is supposed to report.
            throw new IOException("The statement has " + declared + " parameters but "
                    + supplied + " were supplied");
        }
    }

    /** Binds by runtime type, so an Integer is stored as INTEGER rather than as its text form. */
    private void bind(long stmt, Object[] params) throws IOException {
        // Finalized if anything in here throws. The statement is not registered as a cursor
        // until after binding, so a rejected parameter would otherwise strand its peer -- and
        // an unfinalized statement keeps the connection alive after close().
        // The binding loop itself is a separate method because a failed cast is not an exception
        // on every runtime this framework targets, so a cast must not sit inside a block that
        // catches RuntimeException -- there would be nothing for that handler to catch.
        try {
            checkParameterCount(stmt, params.length);
            bindEach(stmt, params);
        } catch (IOException err) {
            IOSImplementation.nativeInstance.sqlStmtFinalize(stmt);
            throw err;
        } catch (RuntimeException err) {
            IOSImplementation.nativeInstance.sqlStmtFinalize(stmt);
            throw err;
        }
    }

    /** The typed binds themselves, outside any catch region. See bind(long, Object[]). */
    private void bindEach(long stmt, Object[] params) throws IOException {
        for (int iter = 0; iter < params.length; iter++) {
            Object p = params[iter];
            int index = iter + 1;
            if (p == null) {
                IOSImplementation.nativeInstance.sqlStmtBindNull(stmt, index);
            } else if (p instanceof byte[]) {
                IOSImplementation.nativeInstance.sqlStmtBindBlob(stmt, index, (byte[])p);
            } else if (p instanceof String) {
                IOSImplementation.nativeInstance.sqlStmtBindText(stmt, index,
                        SQLText.toUTF8((String)p));
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
                IOSImplementation.nativeInstance.sqlStmtBindText(stmt, index,
                        SQLText.toUTF8(p.toString()));
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
            boolean stepped = false;
            try {
                boolean row = IOSImplementation.nativeInstance.sqlStmtStep(stmt);
                stepped = true;
                return row;
            } finally {
                if (!stepped && owner != null) {
                    // A statement runs its work here, not at prepare: an INSERT ... RETURNING
                    // reached through executeQuery does its insert on this step. A constraint
                    // with ON CONFLICT ROLLBACK therefore ends the transaction as this fails,
                    // and without reading the engine back the flag would stay set over a
                    // transaction that is gone -- refusing every later begin and key change,
                    // and failing the rollback that would have cleared it.
                    owner.reconcileTransactionState();
                }
            }
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
            return SQLText.fromUTF8(IOSImplementation.nativeInstance.sqlGetColName(stmt, columnIndex));
        }

        @Override
        protected boolean isNullAt(int index) throws IOException {
            return IOSImplementation.nativeInstance.sqlCursorNullValueAtColumn(stmt, index);
        }

        @Override
        protected boolean legacyWasNullBeforeAnyRead() {
            // iOS answered wasNull() by asking the statement about the last column index it was
            // given, which starts at zero, so before any read it reported on column zero of the
            // current row rather than saying nothing had been read.
            return true;
        }

        @Override
        protected String readString(int index) throws IOException {
            return SQLText.fromUTF8(
                    IOSImplementation.nativeInstance.sqlCursorValueAtColumnText(stmt, index));
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
