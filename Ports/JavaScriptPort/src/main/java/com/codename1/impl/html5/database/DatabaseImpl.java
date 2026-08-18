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
import com.codename1.ui.Display;

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

    /** The pool file this connection holds, as the open-database registry knows it. */
    private final String openKey;

    private final List<CursorImpl> openCursors = new ArrayList<CursorImpl>();

    /**
     * The pool file a name maps to, which is what the registry and a managed key alias use.
     * "foo" and "/foo" are the same file to the storage pool, and so the same database here.
     */
    public static String poolKeyFor(String name) {
        return name != null && name.startsWith("/") ? name : "/" + name;
    }

    public DatabaseImpl(String name, String key) throws IOException {
        this.databaseName = name;
        // The storage pool puts "foo" and "/foo" in the same file, so the registry has to see them
        // as one database: two entries for one file would let either connection pass the
        // sole-connection check and rekey the file underneath the other.
        this.openKey = poolKeyFor(name);
        // Registration first, because it is also the refusal: a key change in progress is rewriting
        // this database, and opening it before asking would leave the handle behind when the
        // refusal arrived.
        registerOpenDatabase(openKey);
        boolean opened = false;
        try {
            refuseIfDataIsOnlyInTheLegacyStore(name);
            peer = SQLiteNative.open(name, key);
            opened = openOrFail(name);
        } finally {
            if (!opened) {
                // Anything this connection attached goes with it: SQLite drops attachments when the
            // connection closes, so the registrations taken for them have to go at the same moment.
            noteConnectionClosed();
            releaseOpenDatabase(openKey);
            }
        }
    }

    /**
     * Refuses to create a database whose data is sitting in the store this engine replaced.
     *
     * The previous implementation kept its databases in WebSQL, which this engine cannot read.
     * WebSQL went out of Chrome in 119 and Firefox never had it, so on nearly every browser there
     * is nothing there and this costs one call that answers false. Where there is something there,
     * an application that upgraded would open a database of the same name, find it empty, and
     * carry on as though the user's rows had never existed -- which for an application that writes
     * as it goes means overwriting them with an empty state rather than merely failing to read
     * them.
     *
     * Reported rather than migrated, and reported rather than ignored. A copy would have to walk
     * a schema through an API no browser this runs on still implements, which is untestable code
     * on a path nobody can exercise; an application that is told instead can export through its
     * own code, which is the only code that knows what its data means. Setting
     * cn1.db.ignoreLegacyWebSql to true proceeds with the new empty database, which an application
     * that has finished with the old store needs, since nothing removes it.
     *
     * @param name the database about to be created
     * @throws IOException if a legacy database of this name holds tables
     */
    private void refuseIfDataIsOnlyInTheLegacyStore(String name) throws IOException {
        if ("true".equals(Display.getInstance().getProperty("cn1.db.ignoreLegacyWebSql", "false"))) {
            return;
        }
        // Only when this engine has nothing of its own. A database it already carries has been
        // through this once, and asking again on every open would charge every application a
        // storage round trip for an answer that cannot change.
        if (SQLiteNative.exists(name) || !SQLiteNative.legacyStoreHasData(name)) {
            return;
        }
        throw new IOException("The database " + name + " exists in this browser's WebSQL store, "
                + "which this build no longer uses and cannot read, and creating it here would "
                + "hand the application an empty database in its place. Export what the old store "
                + "holds through a build that still reads it, or set the cn1.db.ignoreLegacyWebSql "
                + "property to true to continue with a new empty database.");
    }

    /** Reports a failed open, or true when the peer is live. */
    private boolean openOrFail(String name) throws IOException {
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
        return true;
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
        try {
            // Reported, not swallowed. The peer is already cleared so there is no retry, which
            // makes this the only chance to say the data may not have reached storage.
            checkNative(SQLiteNative.close(closing));
        } finally {
            // Last, not first: until the engine has let the database go this connection still
            // holds it, and giving the claim back sooner lets another connection start rewriting
            // it underneath a rollback that has not finished.
            // Anything this connection attached goes with it: SQLite drops attachments when the
            // connection closes, so the registrations taken for them have to go at the same moment.
            noteConnectionClosed();
            releaseOpenDatabase(openKey);
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
            checkNative(SQLiteNative.rekey(peer, key));
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
        // The engine runs the whole script; see the iOS port for why the failure path matters.
        try {
            checkNative(SQLiteNative.execScript(peer, sql));
        } finally {
            // Read back from the engine rather than inferred from the script: it stops at the
            // statement that failed, so a trailing COMMIT in the text may never have run.
            // The names first: an outermost SAVEPOINT opens a transaction that only its own
            // RELEASE ends, and the engine reports a boolean without saying which savepoint owns
            // it. A later RELEASE arriving through a parameterized overload -- which has no engine
            // read of its own -- would otherwise go unrecognized and leave this believing a
            // transaction was still open forever.
            noteScriptTransactionControl(sql);
            noteEngineTransactionState(SQLiteNative.inTransaction(peer));
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
            long stmt = SQLiteNative.prepare(peer, sql);
            checkPrepared(stmt);
            bindText(stmt, params);
            checkNative(SQLiteNative.executeAndFinish(stmt));
            // The names on success only -- a statement that failed opened no savepoint.
            noteScriptTransactionControl(sql);
        } finally {
            // The engine either way. A constraint with ON CONFLICT ROLLBACK ends the transaction
            // as it fails, so reading only on the success path would hold the flag over a
            // transaction that is gone and refuse every begin and key change until close.
            noteEngineTransactionState(SQLiteNative.inTransaction(peer));
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
            long stmt = SQLiteNative.prepare(peer, sql);
            checkPrepared(stmt);
            bind(stmt, params);
            checkNative(SQLiteNative.executeAndFinish(stmt));
            // The names on success only -- a statement that failed opened no savepoint.
            noteScriptTransactionControl(sql);
        } finally {
            // The engine either way. A constraint with ON CONFLICT ROLLBACK ends the transaction
            // as it fails, so reading only on the success path would hold the flag over a
            // transaction that is gone and refuse every begin and key change until close.
            noteEngineTransactionState(SQLiteNative.inTransaction(peer));
        }
    }

    @Override
    public Cursor executeQuery(String sql) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
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
        return register(new CursorImpl(stmt), sql);
    }

    @Override
    public Cursor executeQuery(String sql, String[] params) throws IOException {
        checkOpen();
        requireSingleStatement(sql);
        requireQueryStatement(sql);
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
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
        long stmt = SQLiteNative.prepare(peer, sql);
        checkPrepared(stmt);
        bind(stmt, params);
        return register(new CursorImpl(stmt), sql);
    }

    private Cursor register(CursorImpl cursor, String sql) {
        cursor.owner = this;
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
    void reconcileTransactionState() {
        if (peer == 0) {
            return;
        }
        noteEngineTransactionState(SQLiteNative.inTransaction(peer));
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

        /// Lets the database that created this cursor pass on what the SQL does. The hook it
        /// forwards to is protected, so only a subclass can reach it.
        void statementWrites(boolean writes) {
            noteStatementWrites(writes);
        }

        @Override
        protected void rewind() throws IOException {
            checkNative(SQLiteNative.reset(stmt));
        }

        @Override
        protected boolean stepForward() throws IOException {
            int stepped = SQLiteNative.step(stmt);
            if (stepped < 0) {
                if (owner != null) {
                    // A statement runs its work here, not at prepare: an INSERT ... RETURNING
                    // reached through executeQuery does its insert on this step. A constraint
                    // with ON CONFLICT ROLLBACK therefore ends the transaction as this fails,
                    // and without reading the engine back the flag would stay set over a
                    // transaction that is gone -- refusing every later begin and key change,
                    // and failing the rollback that would have cleared it.
                    owner.reconcileTransactionState();
                }
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
            return requireRead(SQLiteNative.columnString(stmt, index));
        }

        @Override
        protected byte[] readBlob(int index) throws IOException {
            return requireRead(SQLiteNative.columnBlob(stmt, index));
        }

        /// Tells a value that is null from a read that failed and reported null.
        ///
        /// The bindings cannot throw across into Java -- an exception raised in one unwinds the
        /// worker instead -- so a failure comes back as the same null a NULL column gives, and
        /// returning it would report SQL NULL for a blob the engine could not read or an array
        /// the runtime could not allocate. Those are the two readers where that can happen: both
        /// allocate the whole value, and a large one is exactly what fails.
        ///
        /// The error is the discriminator, and it is cleared as each binding is entered, so a
        /// message here belongs to this read and not to something earlier.
        ///
        /// Not applied to the number readers, where the failure value is 0 and 0 is a perfectly
        /// good column value. Checking there would mean asking the engine for an error string
        /// after every zero read, on a path that allocates nothing and whose only realistic
        /// failure is a handle that is already invalid -- which fails at the step before this.
        ///
        /// #### Parameters
        ///
        /// - `value`: what the binding returned
        ///
        /// #### Returns
        ///
        /// the value, when it really was null
        ///
        /// #### Throws
        ///
        /// - `IOException`: if the binding recorded a failure instead
        private <T> T requireRead(T value) throws IOException {
            if (value != null) {
                return value;
            }
            String failure = SQLiteNative.lastError();
            if (failure != null && failure.length() > 0) {
                throw new IOException(failure);
            }
            return value;
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
