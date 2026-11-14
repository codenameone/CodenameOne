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
import com.codename1.db.SqlFunction;
import com.codename1.impl.AbstractDBCursor;
import com.codename1.impl.SQLStatementSplitter;
import com.codename1.impl.SqlFunctionBridge;

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

    /// The functions every connection of this process has registered; SQLite holds only the id.
    private static final SqlFunctionBridge FUNCTIONS = new SqlFunctionBridge() {
        @Override
        protected int valueType(long argv, int i) {
            return LinuxNative.sqlValueType(argv, i);
        }

        @Override
        protected long valueLong(long argv, int i) {
            return LinuxNative.sqlValueLong(argv, i);
        }

        @Override
        protected double valueDouble(long argv, int i) {
            return LinuxNative.sqlValueDouble(argv, i);
        }

        @Override
        protected byte[] valueText(long argv, int i) {
            return LinuxNative.sqlValueText(argv, i);
        }

        @Override
        protected byte[] valueBlob(long argv, int i) {
            return LinuxNative.sqlValueBlob(argv, i);
        }

        @Override
        protected void resultNull(long ctx) {
            LinuxNative.sqlResultNull(ctx);
        }

        @Override
        protected void resultLong(long ctx, long v) {
            LinuxNative.sqlResultLong(ctx, v);
        }

        @Override
        protected void resultDouble(long ctx, double v) {
            LinuxNative.sqlResultDouble(ctx, v);
        }

        @Override
        protected void resultText(long ctx, byte[] utf8) {
            LinuxNative.sqlResultText(ctx, utf8);
        }

        @Override
        protected void resultBlob(long ctx, byte[] v) {
            LinuxNative.sqlResultBlob(ctx, v);
        }

        @Override
        protected void resultError(long ctx, byte[] utf8) {
            LinuxNative.sqlResultError(ctx, utf8);
        }
    };

    /// Called by the engine (cn1_linux_db.c) while a statement calls a Java function.
    static void sqlFunctionCall(long ctx, int id, int argc, long argv) {
        FUNCTIONS.call(ctx, id, argc, argv);
    }


    /**
     * The name this database was opened under. Retained because a managed key resolves its
     * keystore alias from the database name, and changeKey() would otherwise have nothing to
     * resolve against.
     */
    private final String databaseName;

    private final List<CursorImpl> openCursors = new ArrayList<CursorImpl>();
    private final com.codename1.impl.SQLStatementCache statements = new com.codename1.impl.SQLStatementCache();

    /// SQLite's success code.
    private static final int SQLITE_OK = 0;

    /// SQLite's "this is not a database file", which is what a wrong key looks like.
    private static final int SQLITE_NOTADB = 26;

    /** The file this connection is open on, for the shared registry a key change consults. */
    private final String openKey;

    /// The registry identity of a database file, resolved the way the engine resolves it.
    ///
    /// SQLite reports the file it opened with every link followed: `PRAGMA database_list` on a
    /// database attached as `/tmp/link/../db`, where `/tmp/link` points at `/var/sub`, answers
    /// `/var/db`. Collapsing `..` as text answers `/tmp/db` instead -- a real path, belonging to
    /// a different file or to none -- so a connection filed under it is invisible to a delete or
    /// a key change asked for by the name the engine uses, and the file is unlinked or rewritten
    /// while SQLite still has it open. Resolving links first is what makes the two agree; the
    /// lexical pass afterwards still earns its keep, because it also folds `.` and doubled
    /// separators in the part that could not be resolved.
    ///
    /// #### Parameters
    ///
    /// - `path`: the resolved database file, which need not exist yet
    ///
    /// #### Returns
    ///
    /// the key this file is registered under
    static String registryKeyFor(String path) {
        return normalizeDatabasePathKey(resolveLinks(path));
    }

    /// The path with its links followed, as far as the filesystem can answer.
    ///
    /// A database being created for the first time has no file to resolve, and a path that cannot
    /// be resolved at all must not change its identity later -- so the directory is resolved
    /// instead and the name put back on the end. The directory is what carries the links; the
    /// last component is the one SQLite is about to create.
    private static String resolveLinks(String path) {
        if (path == null || path.length() == 0) {
            return path;
        }
        java.io.File file = new java.io.File(path);
        if (file.exists()) {
            try {
                return file.getCanonicalPath();
            } catch (IOException cannotResolve) {
                return path;
            }
        }
        java.io.File parent = file.getParentFile();
        if (parent == null || !parent.exists()) {
            return path;
        }
        try {
            String directory = parent.getCanonicalPath();
            return directory.endsWith("/") ? directory + file.getName()
                    : directory + "/" + file.getName();
        } catch (IOException cannotResolve) {
            return path;
        }
    }

    LinuxDatabase(String databaseName, String path, String key) throws IOException {
        this.databaseName = databaseName;
        // Resolved and then normalized, so two spellings of one path are one registry entry: the
        // claim a key change takes is worth nothing if the other connection is filed under
        // "/a/./b" -- or under a link the engine sees through.
        this.openKey = registryKeyFor(path);
        // Registration first, because it is also the refusal: a key change in progress is rewriting
        // this file, and opening it before asking would touch it mid-rewrite and leave the handle
        // behind when the refusal arrived.
        registerOpenDatabase(openKey);
        boolean opened = false;
        try {
            peer = LinuxNative.sqlDbOpen(path);
            if (key != null) {
                int status = LinuxNative.sqlDbApplyKeyStatus(peer, key);
                if (status != SQLITE_OK) {
                    LinuxNative.sqlDbClose(peer);
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
            opened = true;
        } finally {
            if (!opened) {
                // Anything this connection attached goes with it: SQLite drops attachments when the
            // connection closes, so the registrations taken for them have to go at the same moment.
            noteConnectionClosed();
            releaseOpenDatabase(openKey);
            }
        }
    }

    private void checkOpen() throws IOException {
        if (peer == 0) {
            throw new IOException("This database has been closed");
        }
    }

    /// Rejects a script where this method takes one statement.
    ///
    /// Honouring the compatibility flag on a port that never shipped looks odd, and is deliberate:
    /// the flag is a property of the application, not of the platform it happens to be running on.
    /// An application built in compatibility mode passes the same SQL to every port it runs on,
    /// and a string this port refused while Android quietly ran its first statement would make
    /// that application fail here alone. There is no old behaviour of this port to restore, so
    /// what it restores is the behaviour of the ports the application already runs on.
    private void requireSingleStatement(String sql) throws IOException {
        if (isLegacyBehavior()) {
            return;
        }
        if (!isPlainStatement(sql) && SQLStatementSplitter.isMultiStatement(sql)) {
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
        try {
            LinuxNative.sqlDbExecScript(peer, "COMMIT");
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
            LinuxNative.sqlDbExecScript(peer, "ROLLBACK");
        } catch (Throwable ignored) {
            // Nothing to recover: the caller is already reporting the commit failure.
        }
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
        // SQLite will not close a connection whose statements are still alive.
        long[] cached = statements.drain();
        for (int iter = 0; iter < cached.length; iter++) {
            LinuxNative.sqlStmtFinalize(cached[iter]);
        }
        long closing = peer;
        peer = 0;
        try {
            LinuxNative.sqlDbClose(closing);
        } finally {
            // Last, not first: see the iOS port. The claim covers the handle, not the intent to
            // close. In a finally because the native close can fail and the handle has already
            // been cleared here -- a claim left behind would refuse every later delete and key
            // change of this database for the life of the process.
            // Anything this connection attached goes with it: SQLite drops attachments when the
            // connection closes, so the registrations taken for them have to go at the same moment.
            noteConnectionClosed();
            releaseOpenDatabase(openKey);
        }
    }

    @Override
    public long changes() throws IOException {
        checkOpen();
        return LinuxNative.sqlDbChanges(peer);
    }

    @Override
    public long lastInsertRowId() throws IOException {
        checkOpen();
        return LinuxNative.sqlDbLastInsertRowid(peer);
    }

    @Override
    public boolean supportsFunctions() {
        return true;
    }

    @Override
    public void createFunction(String name, int argCount, SqlFunction function) throws IOException {
        checkOpen();
        int id = function == null ? -1 : FUNCTIONS.register(function);
        if (LinuxNative.sqlDbCreateFunction(peer, com.codename1.util.StringUtil.getBytes(name), argCount, id) != 0) {
            throw new IOException("Could not create the SQL function " + name);
        }
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
            LinuxNative.sqlDbRekey(peer, key);
        } finally {
            releaseKeyChangeClaim(openKey);
        }
    }

    @Override
    public void execute(String sql) throws IOException {
        checkOpen();
        // Before the engine runs it: an ATTACH of a database that is being deleted has to be
        // refused rather than undone, because undoing it can fail while the delete proceeds.
        reserveAttachments(sql);
        // The engine runs the whole script; see WindowsDatabase.execute(String).
        try {
            LinuxNative.sqlDbExecScript(peer, sql);
        } finally {
            // Read back from the engine rather than inferred from the script. SQLite stops at the
            // statement that failed, so a trailing COMMIT in the text may never have run.
            // The names first: an outermost SAVEPOINT opens a transaction that only its own
            // RELEASE ends, and the engine reports a boolean without saying which savepoint owns
            // it. A later RELEASE arriving through a parameterized overload -- which has no engine
            // read of its own -- would otherwise go unrecognized and leave this believing a
            // transaction was still open forever.
            noteScriptTransactionControl(sql);
            noteEngineTransactionState(LinuxNative.sqlDbInTransaction(peer));
        }
    }

    @Override
    public void execute(String sql, String[] params) throws IOException {
        checkOpen();
        // Before the engine runs it, and with the parameters: an ATTACH names its
        // file in them, and a reservation taken afterwards cannot undo an attach.
        reserveAttachments(sql, params);
        requireSingleStatement(sql);
        try {
            long stmt = LinuxNative.sqlStmtPrepare(peer, sql);
            bindText(stmt, params);
            LinuxNative.sqlStmtExecuteAndFinalize(stmt);
            // The names on success only -- a statement that failed opened no savepoint.
            noteScriptTransactionControl(sql);
        } finally {
            // The engine either way. A constraint with ON CONFLICT ROLLBACK ends the
            // transaction as it fails, so reading only on the success path would hold the
            // flag over a transaction that is gone and refuse every begin and key change.
            noteEngineTransactionState(LinuxNative.sqlDbInTransaction(peer));
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
        // Before the engine runs it, and with the parameters: an ATTACH names its file
        // in them, and a reservation taken afterwards cannot undo an attach.
        reserveAttachments(sql, params);
        requireSingleStatement(sql);
        try {
            long stmt = statements.take(sql);
            if (stmt == 0) {
                stmt = LinuxNative.sqlStmtPrepare(peer, sql);
            }
            // bind finalizes the statement itself when it throws.
            bind(stmt, params);
            try {
                while (LinuxNative.sqlStmtStep(stmt)) {
                    // Rows of a statement run for its effect are discarded, as before.
                }
            } catch (IOException err) {
                LinuxNative.sqlStmtFinalize(stmt);
                throw err;
            }
            // Reset releases what the run holds, so a cached statement keeps no lock.
            LinuxNative.sqlStmtReset(stmt);
            LinuxNative.sqlStmtFinalize(statements.put(sql, stmt));
            // The names on success only -- a statement that failed opened no savepoint.
            noteScriptTransactionControl(sql);
        } finally {
            // The engine either way. A constraint with ON CONFLICT ROLLBACK ends the
            // transaction as it fails, so reading only on the success path would hold the
            // flag over a transaction that is gone and refuse every begin and key change.
            noteEngineTransactionState(LinuxNative.sqlDbInTransaction(peer));
        }
    }

    /// The status form of [#execute(String, Object...)], with no handler on the way for the
    /// statement nearly every call runs: one plain statement on an open connection, its
    /// parameters ones the engine binds directly. Anything else -- and the bookkeeping after a
    /// failure -- takes the inherited path, which wraps the throwing method.
    @Override
    public int executeStatus(String sql, Object[] params, long[] counts) {
        if (peer == 0 || params == null || !isPlainStatement(sql)) {
            return super.executeStatus(sql, params, counts);
        }
        long stmt = statements.take(sql);
        if (stmt == 0) {
            stmt = LinuxNative.sqlStmtPrepareStatus(peer, sql);
            if (stmt == 0) {
                return failStatus(LinuxNative.sqlDbErrorText(peer), 1);
            }
        }
        int rc = LinuxNative.sqlStmtRunStatus(stmt, params, counts);
        if (rc == 0) {
            // A plain statement that ran changed no transaction state.
            LinuxNative.sqlStmtFinalizeStatus(statements.put(sql, stmt));
            return 0;
        }
        return statementFailed(sql, params, counts, stmt, rc);
    }

    /// The cold half of [#executeStatus(String, Object[], long[])], kept apart so that its handler is
    /// set up only when a statement did not run.
    private int statementFailed(String sql, Object[] params, long[] counts, long stmt, int rc) {
        if (rc == -2 || rc == -3) {
            // Nothing ran: a parameter only Java binds, or a count the throwing path words.
            LinuxNative.sqlStmtFinalizeStatus(statements.put(sql, stmt));
            return super.executeStatus(sql, params, counts);
        }
        String message = LinuxNative.sqlDbErrorText(peer);
        LinuxNative.sqlStmtFinalizeStatus(stmt);
        try {
            // As execute does after any run: ON CONFLICT ROLLBACK ends the transaction as it fails.
            noteEngineTransactionState(LinuxNative.sqlDbInTransaction(peer));
        } catch (IOException err) {
            return failStatus(err.getMessage(), rc);
        }
        return failStatus(message, rc);
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
        long stmt = prepareCached(sql);
        // A statement with placeholders and no arguments would otherwise run with every slot left
        // as NULL rather than reporting the missing parameters. The check is a no-op in legacy
        // mode, where running it unbound is the behaviour applications were written against.
        // Finalized here because this path does not go through the bind helpers, which are what
        // own the statement everywhere else.
        try {
            checkParameterCount(stmt, 0);
        } catch (IOException err) {
            LinuxNative.sqlStmtFinalize(stmt);
            throw err;
        }
        return register(new CursorImpl(stmt), sql);
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
        long stmt = prepareCached(sql);
        bindText(stmt, params);
        return register(new CursorImpl(stmt), sql);
    }

    @Override
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            if (params != null) {
                requireSingleStatement(sql);
                requireQueryStatement(sql);
            }
            return executeQuery(sql);
        }
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
        long stmt = prepareCached(sql);
        bind(stmt, params);
        return register(new CursorImpl(stmt), sql);
    }

    private long prepareCached(String sql) throws IOException {
        long stmt = statements.take(sql);
        return stmt != 0 ? stmt : LinuxNative.sqlStmtPrepare(peer, sql);
    }

    /// Takes back a closed cursor's statement, reset, for the next query with the same SQL.
    void recycle(String sql, long stmt) throws IOException {
        if (peer == 0 || sql == null) {
            LinuxNative.sqlStmtFinalize(stmt);
            return;
        }
        LinuxNative.sqlStmtReset(stmt);
        LinuxNative.sqlStmtFinalize(statements.put(sql, stmt));
    }

    private Cursor register(CursorImpl cursor, String sql) {
        cursor.owner = this;
        cursor.cacheKey = sql;
        // A statement that writes must never be re-executed to move backwards: this cursor is
        // stepped to run it, so a getCount() or last() would write again. The cursor refuses
        // rather than doing it twice.
        cursor.statementWrites(com.codename1.impl.SQLStatementSplitter.writesData(sql));
        openCursors.add(cursor);
        return cursor;
    }


    /// Re-reads the transaction state from the engine.
    ///
    /// A cursor calls this when a step fails, because the failure may have ended the transaction
    /// underneath the tracking. Separate from the read the execute paths do inline only because a
    /// cursor is not this class and cannot reach the inherited hook itself.
    void reconcileTransactionState() throws IOException {
        if (peer == 0) {
            return;
        }
        noteEngineTransactionState(LinuxNative.sqlDbInTransaction(peer));
    }

    void unregister(CursorImpl cursor) {
        openCursors.remove(cursor);
    }

    private void checkParameterCount(long stmt, int supplied) throws IOException {
        // Compatibility mode skips this for the reason given on requireSingleStatement.
        if (isLegacyBehavior()) {
            return;
        }
        int declared = LinuxNative.sqlStmtParameterCount(stmt);
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
                    LinuxNative.sqlStmtBindNull(stmt, iter + 1);
                } else {
                    LinuxNative.sqlStmtBindString(stmt, iter + 1, params[iter]);
                }
            }
        } catch (IOException err) {
            LinuxNative.sqlStmtFinalize(stmt);
            throw err;
        } catch (RuntimeException err) {
            LinuxNative.sqlStmtFinalize(stmt);
            throw err;
        }
    }

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
            LinuxNative.sqlStmtFinalize(stmt);
            throw err;
        } catch (RuntimeException err) {
            LinuxNative.sqlStmtFinalize(stmt);
            throw err;
        }
    }

    /** The typed binds themselves, outside any catch region. See bind(long, Object[]). */
    private void bindEach(long stmt, Object[] params) throws IOException {
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
        String cacheKey;
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

        /// Lets the database that created this cursor pass on what the SQL does. The hook it
        /// forwards to is protected, so only a subclass can reach it.
        void statementWrites(boolean writes) {
            noteStatementWrites(writes);
        }

        @Override
        protected void rewind() throws IOException {
            LinuxNative.sqlStmtReset(stmt);
        }

        @Override
        protected boolean stepForward() throws IOException {
            try {
                return LinuxNative.sqlStmtStep(stmt);
            } catch (IOException failed) {
                if (owner != null) {
                    // A statement runs its work here, not at prepare: an INSERT ... RETURNING
                    // reached through executeQuery does its insert on this step. A constraint
                    // with ON CONFLICT ROLLBACK therefore ends the transaction as this fails,
                    // and without reading the engine back the flag would stay set over a
                    // transaction that is gone -- refusing every later begin and key change,
                    // and failing the rollback that would have cleared it.
                    owner.reconcileTransactionState();
                }
                throw failed;
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
                if (owner != null) {
                    owner.recycle(cacheKey, closing);
                } else {
                    LinuxNative.sqlStmtFinalize(closing);
                }
            }
        }

        @Override
        protected int columnCount() throws IOException {
            return LinuxNative.sqlColCount(stmt);
        }

        @Override
        protected String columnLabel(int columnIndex) throws IOException {
            return LinuxNative.sqlColNameString(stmt, columnIndex);
        }

        @Override
        protected boolean capturesColumnTypeBeforeRead() {
            // sqlite3_column_type is undefined once a getter has converted the value.
            return true;
        }

        @Override
        protected int columnType(int index) throws IOException {
            return LinuxNative.sqlColType(stmt, index);
        }

        @Override
        protected boolean readRow(Object[] out) throws IOException {
            return LinuxNative.sqlRowValues(stmt, out);
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
