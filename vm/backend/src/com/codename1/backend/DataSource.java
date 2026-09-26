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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.sql.Dialect;

/**
 * A pool of connections to ONE database, whichever engine that database is.
 *
 * <p>{@link DbPool} pools SQLite and only SQLite -- it opens connections from a
 * file path, so there is nowhere to put a host, a user or a password. That left
 * the server engines with the advice to share a single {@link Database} and
 * accept that every handler queues behind whichever exchange is on the socket,
 * because a Database over PostgreSQL or MySQL owns one wire connection and
 * synchronizes every operation on it.
 *
 * <p>Queueing is the right default for a server whose database is a local file.
 * It is the wrong one for a server whose database is a machine across a network
 * that is perfectly happy to run eight statements at once: there the single
 * connection is not a safety property, it is the bottleneck. This is the same
 * pool for all three, so which engine is behind it stops being a question the
 * application's structure has to answer.
 *
 * <pre>
 *   DataSource db = DataSource.open("postgres://user:pw@db.internal/app");
 *   List rows = db.query("SELECT id, title FROM notes WHERE author = ?",
 *           new Object[] {author});
 * </pre>
 *
 * <p>The convenience methods above borrow a connection, run one statement and
 * release it, which is what a handler wants: it is the borrow held ACROSS
 * unrelated work that empties a pool. {@link #inTransaction} is the form for
 * several statements that have to be one, and it holds exactly one connection
 * for exactly the body.
 *
 * <p>Connections are opened lazily except the first, which is opened by
 * {@link #open} so that a wrong URL, an unreachable host or a refused password
 * fails at start-up rather than on the first request that needed the database.
 *
 * <p>An in-memory SQLite database is pooled at size one, and a configuration
 * asking for more is REFUSED rather than quietly reduced. Each connection to
 * ":memory:" gets its OWN private database, so a pool of them hands successive
 * requests different empty databases -- the one case where a bigger pool is not
 * slower but wrong, and a setting worth reporting rather than ignoring.
 */
public final class DataSource {
    /** A unit of work run against one borrowed connection. */
    public interface Work {
        Object run(Database db) throws Exception;
    }

    private final String url;
    /**
     * What this pool is, for a log line. NEVER the URL, which carries the
     * password: it is the first connection's own description, which
     * {@link Database#toString} builds without one.
     */
    private final String describedAs;
    private final int maxSize;
    private final int busyTimeoutMillis;
    private final long borrowTimeoutMillis;
    private final Dialect dialect;
    private final List idle = new ArrayList();
    private final List all = new ArrayList();
    /**
     * Whether the connections are this pool's to close. False for {@link #of},
     * which wraps one somebody else opened and still owns.
     */
    private final boolean owns;
    private boolean closed;

    private DataSource(String url, String describedAs, int maxSize, int busyTimeoutMillis,
                       long borrowTimeoutMillis, Dialect dialect, boolean owns) {
        this.url = url;
        this.describedAs = describedAs;
        this.maxSize = maxSize;
        this.busyTimeoutMillis = busyTimeoutMillis;
        this.borrowTimeoutMillis = borrowTimeoutMillis;
        this.dialect = dialect;
        this.owns = owns;
    }

    /** The one refusal, so both paths to it say the same thing. */
    private static IOException memoryCannotBePooled(int size) {
        return new IOException("An in-memory SQLite database cannot be pooled: each connection "
                + "would get its own, so rows written through one would be missing from the "
                + "next. " + Config.DATASOURCE_POOL_SIZE + " is " + size + "; leave it unset "
                + "for an in-memory database, or point the URL at a file.");
    }

    /** A pool of the default size for whatever engine {@code url} names. */
    public static DataSource open(String url) throws IOException {
        return open(url, 0, 5000, 10000);
    }

    /** A pool of {@code size} connections; 0 takes the default for the engine. */
    public static DataSource open(String url, int size) throws IOException {
        return open(url, size, 5000, 10000);
    }

    /**
     * The general form.
     *
     * @param size how many connections at most; 0 for the engine's default
     * @param busyTimeoutMillis how long SQLite waits on a locked database; ignored
     *                          by the server engines, which have no such setting
     * @param borrowTimeoutMillis how long a borrow waits for a free connection
     *                            before failing; 0 waits forever
     */
    public static DataSource open(String url, int size, int busyTimeoutMillis,
                                  long borrowTimeoutMillis) throws IOException {
        if(url == null || url.length() == 0) {
            throw new IOException("No database URL");
        }
        if(size < 0 || borrowTimeoutMillis < 0 || busyTimeoutMillis < 0) {
            // A NEGATIVE BUSY TIMEOUT reaches SQLite as PRAGMA busy_timeout=-1,
            // which it reads as zero: the busy handler is disabled and a pool of
            // connections to one file fails immediately with SQLITE_BUSY instead
            // of waiting. A mistyped setting should say so rather than quietly
            // becoming the opposite of what it looks like. Zero is still the
            // documented "do not wait".
            throw new IOException("A pool size, a borrow timeout and a busy timeout cannot "
                    + "be negative; use 0 for no wait");
        }
        // The FIRST connection is opened here, and it is what decides the engine:
        // the alternative is parsing the URL a second time in this class and
        // having two answers to the same question.
        if(size > 1 && !namesAServerEngine(url) && isMemory(url)) {
            // BEFORE anything is opened. Refusing after the first connection was
            // made leaked its native handle every time a process caught the
            // configuration error and retried -- and this question is answered by
            // the URL alone, so there is nothing to open to ask it.
            throw memoryCannotBePooled(size);
        }
        Database first = Database.open(url);
        Dialect dialect = first.dialect();
        int limit = size > 0 ? size : defaultSize(url, dialect);
        if(dialect == Dialect.SQLITE && isMemory(url) && limit != 1) {
            // ALWAYS one, whatever was asked for. Each connection to an in-memory
            // SQLite database gets its OWN database, so a second one is not more
            // capacity, it is a second empty database: the table the first
            // connection created is missing on it, and which one a request gets
            // depends on the order borrows happen in. Honouring the setting here
            // would mean honouring it into a data-loss bug, so it is refused
            // rather than clamped in silence.
            first.close();
            throw memoryCannotBePooled(limit);
        }
        DataSource out = new DataSource(url, first.toString(), limit, busyTimeoutMillis,
                borrowTimeoutMillis, dialect, true);
        try {
            out.configure(first);
        } catch (IOException err) {
            first.close();
            throw err;
        }
        out.all.add(first);
        out.idle.add(first);
        return out;
    }

    /**
     * The pool a {@link Config} describes, which is the one a server normally
     * wants: the URL comes from the deployment rather than from the source.
     *
     * <p>With no URL configured at all this answers an in-memory SQLite database
     * on a development profile, and refuses on any other. Defaulting silently in
     * production is how a service comes up healthy, serves requests, writes
     * everything into a database inside its own process and loses all of it at
     * the next deploy.
     */
    public static DataSource fromConfig(Config config) throws IOException {
        String url = config.get(Config.DATASOURCE_URL);
        if(url == null || url.length() == 0) {
            if(!config.isDevelopmentProfile()) {
                throw new IOException("No database is configured. Set " + Config.DATASOURCE_URL
                        + " (or DATABASE_URL) to a SQLite path or a postgres:// or mysql:// "
                        + "URL. An in-memory database is substituted only on a development "
                        + "profile, and this one is '" + config.getProfile() + "'.");
            }
            url = ":memory:";
        }
        return open(url, config.getInt(Config.DATASOURCE_POOL_SIZE, 0),
                config.getInt(Config.DATASOURCE_BUSY_MILLIS, 5000),
                config.getInt(Config.DATASOURCE_BORROW_MILLIS, 10000));
    }

    /**
     * A pool of one around a connection somebody else opened, for code that has a
     * {@link Database} already and wants the pooled API around it.
     *
     * <p>The connection is NOT closed by {@link #close}: it belongs to whoever
     * opened it.
     */
    public static DataSource of(Database db) throws IOException {
        if(db == null) {
            throw new IOException("No database");
        }
        DataSource out = new DataSource(null, db.toString(), 1, 0, 0, db.dialect(), false);
        // In `all` as well as `idle`, or the pool would count itself empty and
        // try to open a second connection from a URL it does not have -- the
        // description this was constructed with is not one.
        out.all.add(db);
        out.idle.add(db);
        return out;
    }

    /**
     * Takes a connection, blocking until one is free. Release it in a finally, or
     * prefer the methods that cannot leak one.
     */
    public Database borrow() throws IOException {
        // THE THREAD'S TRANSACTION FIRST. A thread inside a @Transactional
        // method gets that transaction's connection, so every statement it runs
        // through this pool -- directly, through a dao, through a session --
        // is part of it. Outside the lock: joining may send a BEGIN, and a
        // round trip to the database is not something to do while every other
        // borrower waits on this monitor.
        Database joined = Transactions.joined(this);
        if(joined != null) {
            return joined;
        }
        return borrowFromPool();
    }

    /**
     * Takes a connection from the pool itself, never the thread's transaction's.
     * What a transaction borrows its own connection with.
     */
    synchronized Database borrowFromPool() throws IOException {
        // Checked before the idle list rather than only when it is empty: close()
        // can run while a borrower still holds a connection, and that borrower's
        // finally releases afterwards, so the list can be non-empty after closing.
        if(closed) {
            throw new IOException("This pool is closed");
        }
        long deadline = borrowTimeoutMillis == 0 ? 0
                : System.currentTimeMillis() + borrowTimeoutMillis;
        while(true) {
            while(!idle.isEmpty()) {
                Database candidate = (Database)idle.remove(idle.size() - 1);
                if(candidate.isOpen()) {
                    return candidate;
                }
                // A connection the server hung up on, or one closed by an error
                // it could not resynchronize from. Dropping it here rather than
                // handing it out is the difference between one failed request
                // and every request that borrows it afterwards.
                discard(candidate);
            }
            if(url != null && all.size() < maxSize) {
                // Opened while HOLDING the monitor, which blocks the other
                // borrowers for a TCP connect and an authentication handshake.
                // That is deliberate: it happens at most maxSize times in the
                // life of the pool, and the alternative -- releasing the lock to
                // connect -- lets several threads decide at once that the pool
                // has room and open more connections than it is allowed.
                Database opened = Database.open(url);
                try {
                    configure(opened);
                } catch (IOException err) {
                    // Not in `all` yet, so nothing else can ever close it: a WAL
                    // pragma the filesystem refuses would leak one handle per
                    // borrow, and the pool would go on believing it has room to
                    // open another. The eager path in open() already does this.
                    opened.close();
                    throw err;
                }
                all.add(opened);
                return opened;
            }
            if(closed) {
                throw new IOException("This pool is closed");
            }
            if(url == null && all.isEmpty()) {
                // A pool made by of(): it wraps ONE connection somebody else
                // opened and has no URL to open another from. Once that
                // connection has been discarded -- the server hung up, or an
                // error it could not resynchronize from closed it -- nothing can
                // ever put one back, so waiting is waiting for an event that
                // cannot happen. With the borrow timeout this form is built with
                // (none), that is every later request hanging for good.
                throw new IOException("The connection this data source wraps was closed and "
                        + "there is no URL to open another from. Open the DataSource with a "
                        + "URL if it has to survive its connection being dropped.");
            }
            long wait = 0;
            if(deadline != 0) {
                wait = deadline - System.currentTimeMillis();
                if(wait <= 0) {
                    throw new IOException("No database connection became free within "
                            + borrowTimeoutMillis + "ms; the pool holds " + maxSize
                            + " and they are all in use. Either the work holding them is "
                            + "too slow or " + Config.DATASOURCE_POOL_SIZE + " is too low.");
                }
            }
            try {
                wait(wait);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for a database connection");
            }
        }
    }

    /** Returns a borrowed connection to the pool. */
    public void release(Database db) {
        // The transaction's connection goes back when the transaction ends, not
        // when one of the statements inside it does.
        if(Transactions.isJoined(this, db)) {
            return;
        }
        releaseToPool(db);
    }

    /** {@link #release}, for a connection that is certainly not a transaction's. */
    synchronized void releaseToPool(Database db) {
        if(db == null) {
            return;
        }
        if(closed) {
            // The borrower was still running when the pool shut down. Everything
            // else is closed already, so putting this back would repopulate an
            // idle list nobody may draw from again.
            all.remove(db);
            if(owns) {
                db.close();
            }
            return;
        }
        if(!db.isOpen()) {
            discard(db);
            return;
        }
        idle.add(db);
        notifyAll();
    }

    /** Borrows a connection, runs body, and returns it however body ends. */
    public Object withConnection(Work body) throws Exception {
        Database db = borrow();
        try {
            return body.run(db);
        } finally {
            release(db);
        }
    }

    /**
     * One transaction on one borrowed connection.
     *
     * <p>Everything the body does through the Database it is handed is inside
     * that transaction. Anything it does through THIS pool is not: that borrows a
     * second connection, which the database sees as another session entirely, and
     * on SQLite it will simply block against the write lock the first one holds.
     */
    public Object inTransaction(final Work body) throws Exception {
        Database joined = Transactions.joined(this);
        if(joined != null) {
            // Already inside the thread's transaction, which every engine refuses
            // to nest: run as part of it, the way a joined @Transactional method
            // does.
            return body.run(joined);
        }
        return withConnection(new Work() {
            public Object run(final Database db) throws Exception {
                return db.transaction(new Database.Work() {
                    public Object run(Database inner) throws Exception {
                        return body.run(inner);
                    }
                });
            }
        });
    }

    /** One statement on a borrowed connection. */
    public int execute(String sql, Object[] params) throws IOException {
        Database db = borrow();
        try {
            return db.execute(sql, params);
        } finally {
            release(db);
        }
    }

    /** One query on a borrowed connection. */
    public List query(String sql, Object[] params) throws IOException {
        Database db = borrow();
        try {
            return db.query(sql, params);
        } finally {
            release(db);
        }
    }

    /** One query that returns at most one row, on a borrowed connection. */
    public Map queryOne(String sql, Object[] params) throws IOException {
        Database db = borrow();
        try {
            return db.queryOne(sql, params);
        } finally {
            release(db);
        }
    }

    /**
     * One INSERT on a borrowed connection, answering the key the database
     * generated.
     *
     * <p>The connection is held across the insert AND the question that reads the
     * key back, which is what makes the answer this insert's on the engines where
     * the key is a property of the session -- SQLite's last_insert_rowid and
     * MySQL's LAST_INSERT_ID both answer for the connection they are asked on. A
     * pooled insert that released between the two would read whatever the next
     * borrower had done.
     */
    public long insert(String sql, Object[] params, String idColumn) throws IOException {
        Database db = borrow();
        try {
            return db.insert(sql, params, idColumn);
        } finally {
            release(db);
        }
    }

    /** How this pool's engine spells things. See {@link Dialect}. */
    public Dialect dialect() {
        return dialect;
    }

    /** How many connections this pool may hold. */
    public int getMaxSize() {
        return maxSize;
    }

    /** How many are open right now, borrowed or idle. */
    public synchronized int getOpenCount() {
        return all.size();
    }

    /** How many are open and free right now. */
    public synchronized int getIdleCount() {
        return idle.size();
    }

    /** Closes every connection. Borrowed ones are closed as they come back. */
    public synchronized void close() {
        closed = true;
        for(int iter = 0 ; iter < idle.size() ; iter++) {
            Database db = (Database)idle.get(iter);
            all.remove(db);
            if(owns) {
                db.close();
            }
        }
        idle.clear();
        notifyAll();
    }

    public String toString() {
        return describedAs + " (pool of " + maxSize + ")";
    }

    /** Drops a connection that can no longer be used. Call under the monitor. */
    private void discard(Database db) {
        all.remove(db);
        if(owns) {
            db.close();
        }
        // AND WAKE THE WAITERS. Dropping a connection is the one event that
        // creates capacity without putting anything back in the idle list, so a
        // borrower parked because the pool was full has to be told it can open a
        // replacement. Without this, a release() that discards leaves them
        // waiting: for the borrow timeout, or -- with the documented 0, meaning
        // wait forever -- for good.
        notifyAll();
    }

    /**
     * SQLite tuning, which is a no-op on the server engines.
     *
     * <p>Write-ahead logging is what lets a reader run while a writer is active,
     * and without it a pool over a SQLite file is no better than one connection.
     * It is a property of the FILE rather than the connection, so setting it on
     * each one is redundant and harmless; the busy timeout is per connection and
     * is not.
     */
    private void configure(Database db) throws IOException {
        // ALWAYS, not only when a timeout was asked for. tuneForConcurrency does
        // two things -- it turns on write-ahead logging and it sets the busy
        // timeout -- and skipping it for a zero timeout skipped the WAL as well.
        // A pool of connections to one SQLite file without WAL is the thing this
        // class exists to avoid: readers and writers then contend on the rollback
        // journal and get SQLITE_BUSY, so the extra connections buy nothing.
        // Zero is a legitimate timeout, meaning do not wait, and it stays that.
        db.tuneForConcurrency(busyTimeoutMillis);
    }

    /**
     * How many connections to hold when nobody said.
     *
     * <p>One for an in-memory SQLite database, because a second would be a second
     * database. Four for a SQLite file: WAL gives real read concurrency and the
     * writes serialize behind one lock whatever the number is. Eight for a server
     * engine, which is a machine that can genuinely run that many at once and
     * whose connection limit is measured in hundreds.
     */
    private static int defaultSize(String url, Dialect dialect) {
        if(dialect != Dialect.SQLITE) {
            return 8;
        }
        return isMemory(url) ? 1 : 4;
    }

    /**
     * Whether this URL names an in-memory SQLite database.
     *
     * <p>":memory:" is the plain spelling; a file: URI carrying mode=memory as a
     * QUERY PARAMETER is the other one SQLite accepts, and a pool over it has
     * the same defect.
     *
     * <p>The parameter is matched as a parameter rather than as a substring. A
     * plain search found "mode=memory" in an ordinary path -- /tmp/mode=memory.db
     * is a file somebody may reasonably have -- and in any part of a server URL,
     * including a password. Ask {@link #namesAServerEngine} first: this question
     * is only ever about SQLite.
     */
    private static boolean isMemory(String url) {
        // Db.open accepts a jdbc:sqlite: URL verbatim, so that prefix is a
        // spelling of the same database and has to come off before the question
        // is asked. It is the one the local development loop meets, because
        // sqlite-jdbc is what serves it.
        //
        // The file: URI forms -- "file::memory:", "file:app?mode=memory" -- do
        // NOT appear here: Database.open refuses them outright, because only the
        // Java SE arm parses a URI and a packaged binary would read the whole
        // string as a file name. That refusal is what keeps this question to two
        // spellings of the same private database.
        String path = url;
        if(path.regionMatches(true, 0, "jdbc:sqlite:", 0, 12)) {
            path = path.substring(12);
        }
        // AN EMPTY PATH IS ONE OF THESE TOO. "jdbc:sqlite:" with nothing after it
        // names no file, and SQLite answers that with a private temporary
        // database PER CONNECTION -- measured: a table created on the first
        // connection is "no such table" on the second. Classified as a file, it
        // took the four-connection default and handed successive requests four
        // different empty databases, which is exactly what pooling ":memory:"
        // does and what this refuses it for.
        //
        // Reached only through the jdbc: spelling: DataSource.open rejects an
        // empty URL outright before anything gets here.
        return ":memory:".equals(path) || path.length() == 0;
    }

    /**
     * Whether the URL names one of the engines that is reached over a network,
     * which is the same test {@link Database#open} makes -- by scheme, ignoring
     * case, because RFC 3986 says a scheme is case insensitive. Anything else is
     * a SQLite path.
     */
    private static boolean namesAServerEngine(String url) {
        return hasScheme(url, "postgres://") || hasScheme(url, "postgresql://")
                || hasScheme(url, "mysql://") || hasScheme(url, "mariadb://");
    }

    private static boolean hasScheme(String url, String scheme) {
        return url.length() >= scheme.length()
                && url.regionMatches(true, 0, scheme, 0, scheme.length());
    }
}
