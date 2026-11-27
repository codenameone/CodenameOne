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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import com.codename1.db.sql.QueryRows;
import java.util.Map;

/// SQLite persistence for server-side binaries, on the engine the translator
/// already bundles. Not com.codename1.db.Database, which needs a
/// CodenameOneImplementation for every call.
///
/// Parameters are always bound, never interpolated: string concatenation into SQL
/// is how injection happens, and a server parses input it did not write.
public final class Db {
    /// Column type codes, mirroring SQLITE_*.
    private static final int TYPE_INTEGER = 1;
    private static final int TYPE_FLOAT = 2;
    private static final int TYPE_TEXT = 3;
    private static final int TYPE_BLOB = 4;
    private static final int TYPE_NULL = 5;

    private long handle;

    /// Prepared statements kept for reuse, by their SQL, oldest first.
    ///
    /// Preparing is parsing and planning the statement, and an application
    /// sends the same few statements over and over: measured on llm's log
    /// writer, preparing again on every call cost as much as a third of what
    /// SQLite spent executing them. CPython's sqlite3 keeps 128 for the same
    /// reason. A statement is marked BUSY while it runs and cleared afterwards,
    /// reset, so a call that re-enters with the same SQL -- a SQL function's
    /// body querying the database mid-step -- prepares a fresh one instead of
    /// resetting a statement that is still stepping.
    ///
    /// Entries stay in the map while they run, and the oldest is found by a
    /// use counter only when an insertion overflows the cache: taking one out
    /// and putting it back, as a LinkedHashMap of boxed handles did, allocated
    /// a map node and a Long on every statement.
    private final HashMap<String, Stmt> statements = new HashMap<String, Stmt>();

    /// Counts statement uses, for picking the least recently used to evict.
    private long useClock;

    /// One prepared statement. `cached` is whether `statements` holds it.
    private static final class Stmt {
        final String sql;
        final long handle;
        boolean busy;
        boolean cached;
        long used;
        /// The column names last read, and the statement's re-prepare count
        /// then (sqlite3_stmt_status SQLITE_STMTSTATUS_REPREPARE).
        String[] names;
        int namesAt;

        Stmt(String sql, long handle) {
            this.sql = sql;
            this.handle = handle;
        }
    }

    private static final int STATEMENT_CACHE_SIZE = 64;

    /// Every registered SQL function body, indexed by the id SQLite hands back
    /// as the function's user data. A C callback cannot hold a Java reference,
    /// so the id is the only thing that crosses; a slot is freed when the
    /// connection that registered it closes, and reused.
    private static Object[] functions = new Object[8];
    private static final Object FUNCTIONS_LOCK = new Object();

    /// The slots this connection registered, released by close().
    private int[] functionIds = new int[0];

    /// A statement stepped to its first row and left there while [#holdRead] is
    /// on, so the connection keeps the shared lock it took; 0 until first used.
    private long readHold;
    private boolean holding;

    /// The file this connection opened, or null for one that is never held (an
    /// in-memory database takes no file locks to save).
    private final String file;

    /// The connections open on each file, for [#holdRead]: a lock one of them
    /// keeps would stand in the way of another's commit.
    private static final HashMap<String, java.util.ArrayList<Db>> OPEN =
            new HashMap<String, java.util.ArrayList<Db>>();

    private Db(long handle, String file) {
        this.handle = handle;
        this.file = file;
    }

    /// Keeps the connection's read transaction open between statements, or lets
    /// it go.
    ///
    /// Every statement SQLite runs outside a transaction takes the file's shared
    /// lock and reads the header to see whether its page cache is still good, and
    /// gives the lock up when it finishes: four fcntl calls and a read. While a
    /// statement is still pending -- a Python cursor whose rows are not all read --
    /// CPython's sqlite3 keeps it un-reset, so the lock stays taken and every
    /// statement run meanwhile skips all of that. A loop that reads a row and
    /// queries for its details, which is how llm builds its log rows, measured
    /// 40% slower on CPython once that was taken away. Our cursors have read
    /// their rows already, so a held statement of our own does the same, and
    /// letting it go at any moment changes nothing a caller can see.
    ///
    /// Held only while this is the one connection open on its file, and let go
    /// before any statement but a query, a SAVEPOINT or a RELEASE: a pending read
    /// makes DROP TABLE, VACUUM and a journal mode change fail, and would hold up
    /// another connection's commit. Another PROCESS writing the file waits for it
    /// as it waits for CPython's pending cursor.
    public void holdRead(boolean on) {
        enter();
        try {
            holdReadLocked(on);
        } finally {
            exit();
        }
    }

    private void holdReadLocked(boolean on) {
        if (!on) {
            dropHold();
            return;
        }
        if (holding || handle == 0 || file == null || !alone()) {
            return;
        }
        if (readHold == 0) {
            readHold = prepareImpl(handle, "SELECT count(*) FROM sqlite_schema");
            if (readHold == 0) {
                return;
            }
        }
        if (step(readHold) == 1) {
            holding = true;
        } else {
            resetImpl(readHold);
        }
    }

    private void dropHold() {
        if (holding) {
            holding = false;
            resetImpl(readHold);
        }
    }

    private boolean alone() {
        synchronized (OPEN) {
            java.util.ArrayList<Db> list = OPEN.get(file);
            return list != null && list.size() == 1;
        }
    }

    /// Whether `sql` begins, after blanks and comments, with `word`.
    static boolean startsWith(String sql, String word) {
        int n = sql.length();
        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            if (c <= ' ') {
                i++;
            } else if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int end = sql.indexOf('\n', i);
                i = end < 0 ? n : end + 1;
            } else if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else {
                break;
            }
        }
        return sql.regionMatches(true, i, word, 0, word.length());
    }

    /// Opens (or creates) the database at the given path. ":memory:" gives a
    /// process-lifetime database, which is what a stateless function usually wants
    /// for a cache.
    public static Db open(String path) throws IOException {
        // A NUL ENDS THE PATH where it becomes a C string, and sqlite3_open then
        // names a different database: "allowed.db" followed by a NUL and
        // "/ignored" opens allowed.db, while a caller that checked the directory
        // or the suffix it was given approved something else. Reported as "could
        // not open", which is what this method already says for a path that names
        // nothing -- and what the Java SE arm answers, since the JDBC URL it
        // builds fails on the same byte.
        if (path != null && path.indexOf(0) >= 0) {
            throw new IOException("Could not open database at " + describeForMessage(path));
        }
        // THE SAME URL HAS TO MEAN THE SAME DATABASE ON BOTH ARMS. The Java SE
        // twin hands a "jdbc:sqlite:" URL to the driver, which reads what follows
        // the prefix -- so jdbc:sqlite::memory: is an in-memory database there.
        // sqlite3_open has never heard of the prefix and would take the whole
        // string as a FILE NAME, creating "jdbc:sqlite::memory:" in the working
        // directory: the development loop gets an ephemeral database and the
        // packaged binary gets a file, which is the divergence this class refuses
        // jdbc:postgresql: to avoid.
        String opened = path;
        if (opened != null && opened.regionMatches(true, 0, "jdbc:sqlite:", 0, 12)) {
            opened = opened.substring(12);
        }
        long h = openImpl(opened);
        if (h == 0) {
            throw new IOException("Could not open database at " + path);
        }
        String file = opened == null || opened.length() == 0 || ":memory:".equals(opened)
                || opened.regionMatches(true, 0, "file::memory:", 0, 13) ? null : opened;
        Db db = new Db(h, file);
        if (file != null) {
            java.util.ArrayList<Db> others;
            synchronized (OPEN) {
                java.util.ArrayList<Db> list = OPEN.get(file);
                if (list == null) {
                    list = new java.util.ArrayList<Db>(1);
                    OPEN.put(file, list);
                }
                others = new java.util.ArrayList<Db>(list);
                list.add(db);
            }
            // Outside the registry's lock, and holding no connection's monitor:
            // a connection mid-statement finishes it first.
            for (int i = 0; i < others.size(); i++) {
                others.get(i).holdRead(false);
            }
        }
        return db;
    }

    /// A path for a message, cut at the NUL so the part that would have been
    /// dropped silently is visible rather than invisible in whatever reads the log.
    private static String describeForMessage(String path) {
        int nul = path.indexOf(0);
        return nul < 0 ? path : path.substring(0, nul) + "<NUL>...";
    }

    /// Runs a statement that returns no rows. Returns the number of rows changed.
    /// With `lastInsertId`, also stores [#lastInsertId] into `lastInsertId[0]`
    /// under the same hold of the gate.
    private int executeLocked(String sql, Object[] params, long[] lastInsertId) throws IOException {
        if (holding && !startsWith(sql, "SAVEPOINT") && !startsWith(sql, "RELEASE")) {
            dropHold();
        }
        Stmt st = prepare(sql, params);
        long stmt = st.handle;
        try {
            int rc = step(stmt);
            if (rc < 0) {
                throw new IOException("Statement failed: " + errorImpl(handle) + " [" + sql + "]");
            }
            // Drain: a statement may return rows even when the caller ignores them.
            while (rc == 1) {
                rc = step(stmt);
                if (rc < 0) {
                    throw new IOException("Statement failed: " + errorImpl(handle) + " [" + sql + "]");
                }
            }
            if (lastInsertId != null) {
                lastInsertId[0] = lastInsertRowIdImpl(handle);
            }
            return changesImpl(handle);
        } finally {
            release(st);
        }
    }

    /// Runs a query and returns every row as a column-name to value map. Values are
    /// String, Long, Double or null, which is exactly what the JSON writer accepts.
    private List queryLocked(String sql, Object[] params, boolean positional) throws IOException {
        QueryRows rows = new QueryRows(0);
        String[] names = run(sql, params, positional, rows);
        rows.setColumnCount(names.length);
        // The statement's cached names: never changed, and getColumnNames copies.
        rows.adoptColumnNames(names);
        return rows;
    }

    private String[] run(String sql, Object[] params, boolean positional, List rows) throws IOException {
        if (holding && !startsWith(sql, "SELECT") && !startsWith(sql, "WITH") && !startsWith(sql, "VALUES")) {
            dropHold();
        }
        Stmt st = prepare(sql, params);
        long stmt = st.handle;
        try {
            // AFTER THE FIRST STEP: a cached statement is re-prepared for a
            // schema change only inside sqlite3_step, so before it the count
            // and names are the old program's -- an ALTER TABLE ADD COLUMN
            // between two runs of `SELECT *` would drop the new column.
            int columns = -1;
            String[] names = null;
            while (true) {
                int rc = step(stmt);
                if (columns < 0 && rc >= 0) {
                    columns = columnCountImpl(stmt);
                    names = columnNames(st, columns);
                }
                if (rc < 0) {
                    throw new IOException("Query failed: " + errorImpl(handle) + " [" + sql + "]");
                }
                if (rc == 0) {
                    return names;
                }
                if (positional) {
                    Object[] row = new Object[columns];
                    for (int iter = 0 ; iter < columns ; iter++) {
                        row[iter] = columnValue(stmt, iter);
                    }
                    rows.add(row);
                    continue;
                }
                Map row = new LinkedHashMap();
                for (int iter = 0 ; iter < columns ; iter++) {
                    row.put(names[iter], columnValue(stmt, iter));
                }
                rows.add(row);
            }
        } finally {
            release(st);
        }
    }

    // Runs body inside a transaction, committing when it returns and rolling back
    // if it throws. A half-applied multi-statement change is the failure mode this
    // exists to prevent, and getting the rollback right by hand at every call site
    // is how it gets missed.
    /// Gated because a TRANSACTION is not one call.
    ///
    /// SQLite serializes each API call on a connection, which is what made "one
    /// shared connection is correct, and SQLite serializes it" look true. It
    /// serializes the calls, not the BEGIN/body/COMMIT sequence around them: a
    /// second handler sharing this Db can execute between another's BEGIN and
    /// COMMIT and have its write committed -- or rolled back -- by a request that
    /// knows nothing about it, or meet "cannot start a transaction within a
    /// transaction" and fail for a reason its own code cannot explain.
    ///
    /// The gate is reentrant, which is what makes this work: transaction() holds
    /// it for the whole callback and the execute() calls inside it re-enter freely.
    /// A pooled connection is used by one thread at a time anyway, so the cost
    /// there is an uncontended lock; a shared one is serialized, which is exactly
    /// what correctness requires of it.
    private Object transactionLocked(Work body) throws Exception {
        execute("BEGIN IMMEDIATE", null);
        boolean committed = false;
        try {
            Object result = body.run(this);
            execute("COMMIT", null);
            committed = true;
            return result;
        } finally {
            if (!committed) {
                try {
                    execute("ROLLBACK", null);
                } catch (Exception err) {
                    // The original failure is the one worth reporting; a rollback
                    // that also fails must not replace it.
                    System.err.println("rollback failed: " + err);
                }
            }
        }
    }


    /// THE CONNECTION GATE, held by every operation instead of this object's
    /// monitor -- the same flag Database holds, for the same reason.
    ///
    /// sqlite3_step and friends run on the native I/O pool while a virtual
    /// thread waits for them PARKED, so the host serves other requests meanwhile
    /// (cn1_backend_offload.c). A virtual thread holding a monitor cannot park:
    /// monitor ownership is the host's, so the runtime pins it instead, and a
    /// `synchronized` method here held the host for the whole query -- the
    /// offload would have run and changed nothing. The exclusion the connection
    /// needs (one call at a time, a transaction reserved to its caller, close not
    /// freeing the handle under a reader) is this flag, taken and given back under
    /// a monitor held only for that instant. Reentrant, so transaction()'s
    /// callback re-enters execute() freely. A virtual thread waiting for it naps;
    /// anything else waits on the monitor.
    private final Object gate = new Object();
    private Object gateOwner;
    private int gateHolds;
    /// A close asked for while another caller held the gate; that caller runs it
    /// when its outermost hold ends. See [#close].
    private boolean closePending;

    /// Who is asking: the virtual thread, or the platform thread. Not
    /// Thread.currentThread() alone, which every virtual thread on a host shares,
    /// and not the virtual thread's address, which a server stop can free while
    /// it holds this gate and a later virtual thread can then reuse -- that one
    /// would walk into a gate it never took, on a connection mid-transaction.
    private static Object caller() {
        long virtual = VirtualThread.currentSerial();
        return virtual != 0 ? (Object) Long.valueOf(virtual) : Thread.currentThread();
    }

    /// Takes the gate, reentrantly; an interrupt is kept for the caller rather
    /// than ending the wait, as a monitor would.
    private void enter() {
        Object me = caller();
        boolean virtual = VirtualThread.isVirtual();
        boolean interrupted = false;
        long nap = 1;
        while (true) {
            synchronized (gate) {
                if (gateOwner == null || gateOwner.equals(me)) {
                    gateOwner = me;
                    gateHolds++;
                    break;
                }
                if (!virtual) {
                    try {
                        gate.wait();
                    } catch (InterruptedException error) {
                        interrupted = true;
                    }
                    continue;
                }
            }
            HttpServer.napUntil(System.currentTimeMillis() + nap);
            nap = Math.min(20, nap * 2);
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void exit() {
        boolean closeNow = false;
        synchronized (gate) {
            gateHolds--;
            if (gateHolds <= 0) {
                gateHolds = 0;
                if (closePending) {
                    // Still the owner: the deferred close runs below, outside the
                    // monitor, so its offloaded sqlite3_close can park.
                    closePending = false;
                    closeNow = true;
                } else {
                    gateOwner = null;
                    gate.notifyAll();
                }
            }
        }
        if (closeNow) {
            try {
                closeLocked();
            } finally {
                synchronized (gate) {
                    gateOwner = null;
                    gate.notifyAll();
                }
            }
        }
    }

    /// Holds the connection gate (see [#enter]) for [#executeLocked].
    public int execute(String sql, Object[] params) throws IOException {
        return execute(sql, params, null);
    }

    /// [#execute], also storing [#lastInsertId] into `lastInsertId[0]` when it
    /// is not null -- under the one hold of the gate, rather than taking it again.
    public int execute(String sql, Object[] params, long[] lastInsertId) throws IOException {
        enter();
        try {
            return executeLocked(sql, params, lastInsertId);
        } finally {
            exit();
        }
    }

    /// Holds the connection gate (see [#enter]) for [#queryLocked].
    public List query(String sql, Object[] params) throws IOException {
        enter();
        try {
            return queryLocked(sql, params, false);
        } finally {
            exit();
        }
    }

    /// Runs a query and returns every row as an `Object[]` of its values in
    /// the order the statement projects them; the column names are on the
    /// result ([Database#columnNames]). See [Database#queryPositional].
    public List queryPositional(String sql, Object[] params) throws IOException {
        enter();
        try {
            return queryLocked(sql, params, true);
        } finally {
            exit();
        }
    }

    /// [#queryPositional] into a list the caller owns and reuses, answering the
    /// column names -- the statement's own array, which must not be changed. A
    /// new result list per statement cost an object and its first storage block,
    /// and the DB-API layer then copied every row into a list of its own.
    public String[] queryPositionalInto(String sql, Object[] params, List out) throws IOException {
        enter();
        try {
            return run(sql, params, true, out);
        } finally {
            exit();
        }
    }

    /// Holds the connection gate (see [#enter]) for [#transactionLocked].
    public Object transaction(Work body) throws Exception {
        enter();
        try {
            return transactionLocked(body);
        } finally {
            exit();
        }
    }

    /// Holds the connection gate (see [#enter]) for [#lastInsertIdLocked].
    public long lastInsertId() {
        enter();
        try {
            return lastInsertIdLocked();
        } finally {
            exit();
        }
    }

    /// Holds the connection gate (see [#enter]) for [#closeLocked].
    ///
    /// NEVER WAITS FOR ANOTHER CALLER. When the connection is in use, the close is
    /// recorded and the user runs it as its outermost operation ends -- after a
    /// transaction, not in the middle of one. Waiting was a hang at shutdown:
    /// HttpServer.stop() lets go of a virtual thread whose query outlived the
    /// drain window while its sqlite3_step is still on the I/O pool, and that
    /// thread holds the gate and never runs again, so a pool.close() that waited
    /// for it waited for ever. Such a connection is left to the process exit.
    public void close() {
        Object me = caller();
        synchronized (gate) {
            if (gateOwner != null && !gateOwner.equals(me)) {
                closePending = true;
                return;
            }
            // Taken in the same section as the check: a caller entering
            // between the two would make this close wait instead of deferring.
            gateOwner = me;
            gateHolds++;
        }
        try {
            closeLocked();
        } finally {
            exit();
        }
    }

    /// A unit of work run inside [#transaction].
    public interface Work {
        Object run(Db db) throws Exception;
    }

    /// Switches the database to write-ahead logging, which is what lets readers run
    /// while a writer is active. Worth doing once after open for anything that
    /// serves concurrent requests; pointless for :memory:.
    public void enableWriteAheadLog() throws IOException {
        query("PRAGMA journal_mode=WAL", null);
        execute("PRAGMA synchronous=NORMAL", null);
    }

    /// How long a blocked writer waits for a competing one before giving up. Without
    /// this, two connections writing at once produce SQLITE_BUSY immediately rather
    /// than queueing.
    public void setBusyTimeout(int millis) throws IOException {
        execute("PRAGMA busy_timeout=" + millis, null);
    }

    /// The rowid the most recent insert produced.
    ///
    /// GATED, like execute, query, transaction and close -- it was the
    /// one operation here that was not, and the only one that hands the native
    /// handle over without holding the lock that guards it. close() frees the
    /// sqlite3 connection and then zeroes the field, so a reader that had already
    /// loaded the handle called sqlite3_last_insert_rowid on freed memory. The
    /// native's own null check cannot help with that: the pointer it is given is
    /// not null, it is dangling.
    private long lastInsertIdLocked() {
        return lastInsertRowIdImpl(handle);
    }

    /// GATED, like execute, query and transaction, and it keeps the handle
    /// when the close does not take.
    ///
    /// sqlite3_close answers SQLITE_BUSY when a prepared statement is still
    /// alive on the connection, and does NOT destroy it. Zeroing the handle
    /// regardless threw away the only reference to a connection that was still
    /// open, so nothing could ever close it again -- the native connection and
    /// everything it owns leaked, once per shutdown that raced a borrower.
    ///
    /// The gate is what makes that rare rather than merely recoverable: a
    /// DbPool.close() arriving while a borrowed connection is mid-query never
    /// closes underneath it -- [#close] defers to the borrower, which closes the
    /// connection as its query ends.
    private void closeLocked() {
        if (handle != 0) {
            dropHold();
            if (readHold != 0) {
                finalizeImpl(readHold);
                readHold = 0;
            }
            // sqlite3_close answers SQLITE_BUSY while any statement is alive,
            // so the cached ones go first.
            // A busy one is mid-step under a re-entrant call; release() finalizes it
            // when that call returns, since the handle is gone by then.
            for (Stmt cached : statements.values()) {
                cached.cached = false;
                if (!cached.busy) {
                    finalizeImpl(cached.handle);
                }
            }
            statements.clear();
            if (closeImpl(handle) == 0) {
                handle = 0;
                releaseFunctions();
                // Only once closed: a connection that stays open is still
                // one of the file's.
                if (file != null) {
                    synchronized (OPEN) {
                        java.util.ArrayList<Db> list = OPEN.get(file);
                        if (list != null) {
                            list.remove(this);
                            if (list.isEmpty()) {
                                OPEN.remove(file);
                            }
                        }
                    }
                }
            } else {
                // Not silent. The handle is deliberately kept so a later close can
                // retry, but a connection that will not close is worth a line:
                // without one, "the pool closed" and "the pool leaked a connection
                // per shutdown" look identical from outside.
                System.err.println("the database did not close (" + errorImpl(handle)
                        + "); it stays open and can be closed again");
            }
        }
    }

    private Object columnValue(long stmt, int index) {
        switch (columnTypeImpl(stmt, index)) {
            case TYPE_INTEGER:
                return Long.valueOf(columnLongImpl(stmt, index));
            case TYPE_FLOAT:
                return Double.valueOf(columnDoubleImpl(stmt, index));
            case TYPE_NULL:
                return null;
            case TYPE_BLOB:
                return columnBlobImpl(stmt, index);
            case TYPE_TEXT:
            default:
                return columnStringImpl(stmt, index);
        }
    }

    /// The statement's column names. Decoding them is a string per column per
    /// run, and they change only when SQLite re-prepares the statement after a
    /// schema change -- which moves where it keeps them, so the addresses tell.
    private static String[] columnNames(Stmt st, int columns) {
        int at = reprepareCountImpl(st.handle);
        String[] names = st.names;
        if (names != null && names.length == columns && st.namesAt == at) {
            return names;
        }
        names = new String[columns];
        for (int iter = 0 ; iter < columns ; iter++) {
            names[iter] = columnNameImpl(st.handle, iter);
        }
        st.names = names;
        st.namesAt = at;
        return names;
    }

    /// Finalizes a statement the caller will never see, dropping it from the
    /// cache when it was the cached copy.
    private void discard(Stmt st) {
        if (st.cached) {
            statements.remove(st.sql);
            st.cached = false;
        }
        finalizeImpl(st.handle);
    }

    /// Hands a statement back to the cache, reset, or finalizes it when the
    /// connection closed meanwhile or another copy of it is already cached.
    private void release(Stmt st) {
        resetImpl(st.handle);
        st.busy = false;
        if (st.cached) {
            return;
        }
        if (handle == 0 || statements.containsKey(st.sql)) {
            finalizeImpl(st.handle);
            return;
        }
        if (statements.size() >= STATEMENT_CACHE_SIZE) {
            Stmt oldest = null;
            for (Stmt e : statements.values()) {
                if (!e.busy && (oldest == null || e.used < oldest.used)) {
                    oldest = e;
                }
            }
            if (oldest != null) {
                statements.remove(oldest.sql);
                oldest.cached = false;
                finalizeImpl(oldest.handle);
            }
        }
        st.cached = true;
        statements.put(st.sql, st);
    }

    private Stmt prepare(String sql, Object[] params) throws IOException {
        // THE STATEMENT ITSELF cannot hold a NUL. sqlite3_prepare_v2 is called
        // with -1, so it reads to the first zero byte -- and the native's note
        // says such a truncation leaves an incomplete statement sqlite rejects.
        // That is true of most cuts and not of all of them: "... WHERE id='1'"
        // followed by a NUL and " AND owner='bob'" truncates to a statement that
        // is perfectly complete and missing its authorization clause. Refused
        // here so the claim holds by construction rather than by luck; no SQL
        // this runtime can execute contains one, since sqlite reads C strings.
        if (sql != null && sql.indexOf(0) >= 0) {
            throw new IOException("An SQL statement cannot hold a NUL: sqlite "
                    + "reads it as the end of the statement, and what is left "
                    + "may still be a valid one that means something else");
        }
        if (handle == 0) {
            throw new IOException("Database is closed");
        }
        Stmt st = statements.get(sql);
        if (st == null || st.busy) {
            long fresh = prepareImpl(handle, sql);
            if (fresh == 0) {
                throw new IOException("Could not prepare: " + errorImpl(handle) + " [" + sql + "]");
            }
            st = new Stmt(sql, fresh);
        }
        st.busy = true;
        st.used = ++useClock;
        long stmt = st.handle;
        // THE COUNT HAS TO MATCH, both ways. Binding more than the statement has
        // is SQLITE_RANGE and bind() refuses it -- but binding FEWER said nothing
        // at all: SQLite leaves an unbound parameter as NULL, so an insert or an
        // update committed a row the caller never wrote, and the Java SE JDBC arm
        // throws for the same call. One API, two answers, and the one that stays
        // quiet is the one that reaches production.
        int expected = parameterCountImpl(stmt);
        int supplied = params == null ? 0 : params.length;
        if (supplied != expected) {
            discard(st);
            throw new IOException("the statement has " + expected + " parameter"
                    + (expected == 1 ? "" : "s") + " and " + supplied
                    + " were supplied [" + sql + "]");
        }
        if (params != null) {
            try {
                for (int iter = 0 ; iter < params.length ; iter++) {
                    bind(stmt, iter + 1, params[iter]);
                }
            } catch (IOException refused) {
                // FINALIZED HERE, because the caller never sees this handle: it
                // gets one only if this returns, so the finally in execute() and
                // query() has nothing to close. SQLITE_TOOBIG and SQLITE_NOMEM
                // both reach here, and the statement they leave behind outlives
                // the call AND the Database -- sqlite3_close answers SQLITE_BUSY
                // while any statement is unfinalized, and nothing holds a handle
                // to finalize. The parameter-count branch above already does this.
                discard(st);
                throw refused;
            }
        }
        return st;
    }

    /// Binds one parameter, or fails.
    ///
    /// The status was discarded, so binding more parameters than the statement has
    /// placeholders -- SQLITE_RANGE -- was ignored and the statement executed anyway,
    /// with the unbound parameter reading as NULL. A mutation committed with the
    /// wrong values and nothing said so, where the JavaSE JDBC path throws.
    /// Registers a scalar SQL function; see Database.createFunction.
    public boolean createFunction(String name, int argc,
            java.util.function.Function<Object[], Object> body) throws IOException {
        enter();
        try {
            return createFunctionLocked(name, argc, body);
        } finally {
            exit();
        }
    }

    private boolean createFunctionLocked(String name, int argc,
            java.util.function.Function<Object[], Object> body) throws IOException {
        if (handle == 0) {
            throw new IOException("the database is closed");
        }
        // A NUL would end the name where it becomes a C string, registering a
        // different function from the one asked for; see open().
        if (name.indexOf('\0') >= 0) {
            throw new IOException("a function name cannot contain NUL");
        }
        byte[] utf8 = name.getBytes("UTF-8");
        int id;
        synchronized (FUNCTIONS_LOCK) {
            id = -1;
            for (int i = 0; i < functions.length; i++) {
                if (functions[i] == null) {
                    id = i;
                    break;
                }
            }
            if (id < 0) {
                id = functions.length;
                Object[] grown = new Object[functions.length * 2];
                System.arraycopy(functions, 0, grown, 0, functions.length);
                functions = grown;
            }
            functions[id] = body;
        }
        if (createFunctionImpl(handle, utf8, argc, id) != 0) {
            synchronized (FUNCTIONS_LOCK) {
                functions[id] = null;
            }
            throw new IOException("could not register " + name + ": " + errorImpl(handle));
        }
        int[] ids = new int[functionIds.length + 1];
        System.arraycopy(functionIds, 0, ids, 0, functionIds.length);
        ids[functionIds.length] = id;
        functionIds = ids;
        return true;
    }

    private void releaseFunctions() {
        synchronized (FUNCTIONS_LOCK) {
            for (int id : functionIds) {
                functions[id] = null;
            }
        }
        functionIds = new int[0];
    }

    /// Called from the native trampoline while a statement runs: reads the
    /// arguments, runs the body and hands its answer (or its failure) back.
    /// Nothing may escape, since the caller is C.
    @SuppressWarnings("unchecked")
    static void callFunction(long ctx, int id, int argc, long argv) {
        Object body;
        synchronized (FUNCTIONS_LOCK) {
            body = id >= 0 && id < functions.length ? functions[id] : null;
        }
        if (!(body instanceof java.util.function.Function)) {
            resultError(ctx, "the function is no longer registered");
            return;
        }
        try {
            Object[] args = new Object[argc];
            for (int i = 0; i < argc; i++) {
                switch (valueTypeImpl(argv, i)) {
                    case TYPE_INTEGER:
                        args[i] = Long.valueOf(valueLongImpl(argv, i));
                        break;
                    case TYPE_FLOAT:
                        args[i] = Double.valueOf(valueDoubleImpl(argv, i));
                        break;
                    case TYPE_TEXT:
                        args[i] = valueTextImpl(argv, i);
                        break;
                    case TYPE_BLOB:
                        args[i] = valueBlobImpl(argv, i);
                        break;
                    default:
                        args[i] = null;
                        break;
                }
            }
            Object r = ((java.util.function.Function<Object[], Object>) body).apply(args);
            if (r == null) {
                resultNullImpl(ctx);
            } else if (r instanceof Double || r instanceof Float) {
                resultDoubleImpl(ctx, ((Number) r).doubleValue());
            } else if (r instanceof Number) {
                resultLongImpl(ctx, ((Number) r).longValue());
            } else if (r instanceof Boolean) {
                resultLongImpl(ctx, ((Boolean) r).booleanValue() ? 1 : 0);
            } else if (r instanceof byte[]) {
                resultBlobImpl(ctx, (byte[]) r);
            } else {
                resultTextImpl(ctx, r.toString().getBytes("UTF-8"));
            }
        } catch (Throwable t) { //NOPMD AvoidCatchingThrowable - the caller is C; anything escaping would unwind through SQLite
            String message = t.getMessage();
            resultError(ctx, message == null ? t.toString() : message);
        }
    }

    private static void resultError(long ctx, String message) {
        try {
            resultErrorImpl(ctx, message.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException err) {
            resultNullImpl(ctx);
        }
    }

    private static void bind(long stmt, int index, Object value) throws IOException {
        int status;
        if (value == null) {
            status = bindNullImpl(stmt, index);
        } else if (value instanceof Long) {
            // The three types nearly every parameter is, tested first and read
            // without a virtual call.
            status = bindLongImpl(stmt, index, ((Long) value).longValue());
        } else if (value instanceof String) {
            status = bindStringImpl(stmt, index, (String) value);
        } else if (value instanceof Double) {
            status = bindDoubleImpl(stmt, index, ((Double) value).doubleValue());
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            status = bindLongImpl(stmt, index, ((Number) value).longValue());
        } else if (value instanceof Float) {
            status = bindDoubleImpl(stmt, index, ((Number) value).doubleValue());
        } else if (value instanceof byte[]) {
            status = bindBlobImpl(stmt, index, (byte[]) value);
        } else if (value instanceof Boolean) {
            status = bindLongImpl(stmt, index, ((Boolean) value).booleanValue() ? 1 : 0);
        } else {
            status = bindStringImpl(stmt, index, String.valueOf(value));
        }
        if (status != 0) {                       // anything but SQLITE_OK
            throw new IOException("Could not bind parameter " + index
                    + " (sqlite status " + status + "); check the parameter count");
        }
    }

    private static native long openImpl(String path);
    private static native int createFunctionImpl(long handle, byte[] name, int argc, int id);
    private static native int valueTypeImpl(long argv, int index);
    private static native long valueLongImpl(long argv, int index);
    private static native double valueDoubleImpl(long argv, int index);
    private static native String valueTextImpl(long argv, int index);
    private static native byte[] valueBlobImpl(long argv, int index);
    private static native void resultNullImpl(long ctx);
    private static native void resultLongImpl(long ctx, long value);
    private static native void resultDoubleImpl(long ctx, double value);
    private static native void resultTextImpl(long ctx, byte[] value);
    private static native void resultBlobImpl(long ctx, byte[] value);
    private static native void resultErrorImpl(long ctx, byte[] message);
    private static native int closeImpl(long handle);
    private static native String errorImpl(long handle);
    private static native long prepareImpl(long handle, String sql);
    private static native int bindStringImpl(long stmt, int index, String value);
    private static native int bindLongImpl(long stmt, int index, long value);
    private static native int bindDoubleImpl(long stmt, int index, double value);
    private static native int bindNullImpl(long stmt, int index);
    private static native int bindBlobImpl(long stmt, int index, byte[] value);
    /// One sqlite3_step: offloaded ([#stepImpl]), or on this thread
    /// ([#stepInlineImpl]) once the connection has a function written in Java,
    /// which sqlite3_step calls and which has to run on a VM thread.
    private int step(long stmt) {
        return functionIds.length > 0 ? stepInlineImpl(stmt) : stepImpl(stmt);
    }

    private static native int stepImpl(long stmt);

    private static native int stepInlineImpl(long stmt);

    private static native int parameterCountImpl(long stmt);
    private static native int columnCountImpl(long stmt);
    private static native String columnNameImpl(long stmt, int index);
    private static native int reprepareCountImpl(long stmt);
    private static native int columnTypeImpl(long stmt, int index);
    private static native String columnStringImpl(long stmt, int index);
    private static native long columnLongImpl(long stmt, int index);
    private static native double columnDoubleImpl(long stmt, int index);
    private static native byte[] columnBlobImpl(long stmt, int index);
    private static native void finalizeImpl(long stmt);
    private static native void resetImpl(long stmt);
    private static native int changesImpl(long handle);
    private static native long lastInsertRowIdImpl(long handle);
}
