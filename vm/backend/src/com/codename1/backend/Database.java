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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import com.codename1.backend.sql.Dialect;
import com.codename1.backend.sql.MySql;
import com.codename1.backend.sql.Postgres;

/**
 * One database API over SQLite, PostgreSQL and MySQL, chosen by URL.
 *
 * <pre>
 *   Database.open("/var/lib/app/app.db")
 *   Database.open(":memory:")
 *   Database.open("postgres://user:secret@db.internal:5432/app?sslmode=require")
 *   Database.open("mysql://user:secret@db.internal/app?sslmode=require"
 *                 + "&amp;sslrootcert=/etc/ssl/rds-ca.pem")
 * </pre>
 *
 * The point of the single type is that a handler cannot tell which engine
 * answered it. Rows come back as column-name to value maps whose values are
 * always Long, Double, String, byte[] or null, whichever engine produced them --
 * so the same code developed against a local SQLite file runs against a managed
 * PostgreSQL without a branch. Parameters are always bound, never interpolated,
 * on all three.
 *
 * TLS has three settings, and none of them is "encrypted but unverified".
 * `sslmode=require` demands TLS whose certificate chains to a trusted root AND
 * carries the host's name; `sslmode=disable` is plaintext, deliberately;
 * `sslmode=prefer` uses TLS when the server offers it and fails loudly rather
 * than silently downgrading when verification does not hold. A managed instance
 * or a development container that presents a private CA is reached by naming it:
 * `sslrootcert=/path/to/ca.pem`.
 *
 * SQLite goes through {@link Db}, which is per-target: the engine is linked into
 * the binary on the translated side and reached through a JDBC driver on the
 * local Java SE side. PostgreSQL and MySQL are the SAME code on both targets --
 * they speak the wire protocol over {@link Tcp}, so there is no driver to install
 * and nothing that can behave differently between the two.
 *
 * Statements are written ONCE, in the portable form: ? for every parameter and
 * plain unquoted names. PostgreSQL binds $1 rather than ?, and that difference
 * stops inside {@link #execute} and {@link #query} -- see {@link Dialect#bind} --
 * rather than at every call site. SQL already written for one engine keeps
 * working: a statement carrying no ? at all is passed through untouched, so
 * hand-written $1 is left alone.
 *
 * What differs between engines and used to be papered over by the caller:
 * {@link #lastInsertId} is meaningful for SQLite and MySQL and always 0 for
 * PostgreSQL, which has no such concept. {@link #insert} is the portable form of
 * that question -- it asks whichever way this engine answers -- and
 * {@link #dialect} exposes the rest of the differences for code that generates
 * schema.
 */
public final class Database {
    private final Db sqlite;
    /**
     * Db has no isClosed, so closure is recorded where it happens -- and VOLATILE,
     * because isOpen() reads it without the monitor.
     *
     * <p>close() is synchronized like every other operation on this class, so a
     * closing thread publishes this under the lock -- but the pool or health check
     * asking isOpen() never takes that lock, and without a happens-before edge the
     * memory model lets it keep seeing false indefinitely. The connection is then
     * handed out again and fails on its next use.
     *
     * <p>Volatile rather than synchronizing isOpen(): the caller asking whether a
     * connection is usable is exactly the caller that must not block behind a
     * statement already running on it. HttpServer publishes its own `running` flag
     * the same way.
     */
    private volatile boolean sqliteClosed;
    private final Postgres postgres;
    private final MySql mysql;
    private final String describedAs;
    /**
     * WHICH ENGINE THIS IS, as the things that differ between them rather than as
     * a name to branch on. Chosen from the URL before anything is connected, and
     * immutable afterwards, so asking is free on the request path.
     */
    private final Dialect dialect;

    private Database(Db sqlite, Postgres postgres, MySql mysql, String describedAs,
                     Dialect dialect) {
        this.sqlite = sqlite;
        this.postgres = postgres;
        this.mysql = mysql;
        this.describedAs = describedAs;
        this.dialect = dialect;
    }

    /** A unit of work run inside {@link #transaction}. */
    public interface Work {
        Object run(Database db) throws Exception;
    }

    /**
     * Opens the database the URL names. Anything that is not a recognised scheme
     * is taken as a SQLite path, so an ordinary file name keeps working.
     */
    /**
     * Whether the URL opens with this scheme, IGNORING CASE.
     *
     * <p>RFC 3986 makes a scheme case-insensitive, and anything unrecognised here
     * is taken as a SQLite path -- so "PostgreSQL://user@host/db" was not a
     * mismatch that got reported, it was a file name. The caller ended up
     * creating or opening a local database, or failing on a pathname containing
     * "//", with nothing saying the server had never been contacted.
     *
     * <p>regionMatches rather than toLowerCase: String.toLowerCase is locale
     * sensitive and this runtime has no Locale to ask for the root one, so on a
     * Turkish device the I of a scheme folds to a dotless i and stops matching.
     */
    private static boolean hasScheme(String url, String scheme) {
        return url.length() >= scheme.length()
                && url.regionMatches(true, 0, scheme, 0, scheme.length());
    }

    public static Database open(String url) throws IOException {
        if(url == null) {
            throw new IOException("No database URL");
        }
        if(hasScheme(url, "postgres://") || hasScheme(url, "postgresql://")) {
            Url parsed = Url.parse(url, 5432);
            return new Database(null, Postgres.connect(parsed.host, parsed.port,
                    parsed.path, parsed.user, parsed.password, parsed.sslMode,
                    parsed.caFile, parsed.timeoutMillis, parsed.socketTimeoutMillis),
                    null, parsed.describe("postgres"), Dialect.POSTGRES);
        }
        if(hasScheme(url, "mysql://") || hasScheme(url, "mariadb://")) {
            Url parsed = Url.parse(url, 3306);
            return new Database(null, null, MySql.connect(parsed.host, parsed.port,
                    parsed.path, parsed.user, parsed.password, parsed.sslMode,
                    parsed.caFile, parsed.timeoutMillis, parsed.socketTimeoutMillis),
                    parsed.describe("mysql"), Dialect.MYSQL);
        }
        return new Database(Db.open(url), null, null, "sqlite:" + url, Dialect.SQLITE);
    }

    /** Wraps an already-open SQLite handle, for code that opened one directly. */
    public static Database of(Db db) {
        return new Database(db, null, null, "sqlite", Dialect.SQLITE);
    }

    /**
     * Synchronized, like the two below, because a shared session is not safe to
     * interleave -- and for the network engines it is worse than interleaved
     * transactions.
     *
     * Postgres and MySql each own a Wire, and a Wire owns ONE 16KB buffer with a
     * position and a limit, plus one output stream it builds every message in.
     * MySql also carries the packet sequence number. Two handlers calling at once
     * therefore write into the same message buffer, move each other's parse
     * position and desynchronize the sequence: that is protocol corruption, not
     * merely one request's work committed by another's COMMIT.
     *
     * The SQLite path delegates to Db, which is synchronized on its own monitor.
     * Holding this one first is safe -- the order is always Database then Db,
     * never the reverse -- and Db's monitor is reentrant for the callbacks.
     */
    public synchronized int execute(String sql, Object[] params) throws IOException {
        String rendered = bind(sql, params);
        if(sqlite != null) {
            return sqlite.execute(rendered, params);
        }
        if(postgres != null) {
            return postgres.execute(rendered, params);
        }
        return mysql.execute(rendered, params);
    }

    /** Runs a query and returns every row as a column-name to value map. */
    public synchronized List query(String sql, Object[] params) throws IOException {
        String rendered = bind(sql, params);
        if(sqlite != null) {
            return sqlite.query(rendered, params);
        }
        if(postgres != null) {
            return postgres.query(rendered, params);
        }
        return mysql.query(rendered, params);
    }

    /**
     * The one row a query is expected to return, or null when it returns none.
     *
     * <p>Its own method because the alternative is written at every call site and
     * is wrong in the same way each time: reading get(0) off a list without
     * looking at its size, which is an IndexOutOfBoundsException on the day the
     * row is missing rather than the null the code above it is already written to
     * handle. More than one row is a bug in the statement, and it is reported as
     * one rather than silently discarded.
     */
    public synchronized Map queryOne(String sql, Object[] params) throws IOException {
        List rows = query(sql, params);
        if(rows.isEmpty()) {
            return null;
        }
        if(rows.size() > 1) {
            throw new IOException("Expected at most one row and the query returned "
                    + rows.size() + ": [" + sql + "]");
        }
        return (Map)rows.get(0);
    }

    /**
     * Runs an INSERT and answers the key the database generated for it.
     *
     * <p>This is the operation the engines disagree about most and the one an
     * application needs most often. SQLite and MySQL assign the key and hold it
     * until asked -- {@link #lastInsertId} -- while PostgreSQL has no such
     * concept at all, and the only way to learn the key there is to ask the
     * INSERT itself for it with a RETURNING clause. Written by hand that is a
     * branch on the engine at every insert; here the dialect knows which it is.
     *
     * <p>{@code idColumn} names the generated column, which is what RETURNING
     * needs. It is quoted for the engine, so a column named "order" or one whose
     * case matters is spelled correctly rather than folded.
     *
     * @param sql an INSERT in the portable form, with no RETURNING of its own
     * @return the generated key, or 0 where the statement generated none
     */
    public synchronized long insert(String sql, Object[] params, String idColumn)
            throws IOException {
        if(idColumn == null || idColumn.length() == 0) {
            throw new IOException("insert needs the name of the generated key column");
        }
        if(!dialect.generatedKeysThroughReturning()) {
            execute(sql, params);
            return lastInsertId();
        }
        // RETURNING makes this a statement that answers with rows, so it goes
        // through query rather than execute. Appended AFTER the portable form is
        // rendered would mean rendering twice; appending before costs nothing
        // because the clause holds no placeholder.
        Map row = queryOne(withReturning(sql, idColumn), params);
        if(row == null) {
            return 0;
        }
        Object value = row.values().iterator().next();
        if(value instanceof Number) {
            // instanceof rather than a cast whose failure is caught: a failed cast
            // does not throw under ParparVM, it hands the wrong object on and the
            // next instruction reads a native crash out of it.
            return ((Number)value).longValue();
        }
        throw new IOException("The generated key came back as something other than a "
                + "number, so the column named is not the generated one: " + idColumn);
    }

    /**
     * {@code sql} with a RETURNING clause, placed INSIDE the statement.
     *
     * <p>A trailing semicolon is where this goes wrong if nobody looks for it:
     * appending to "INSERT INTO t (a) VALUES (?);" gives
     * "INSERT INTO t (a) VALUES (?); RETURNING id", which is two statements, the
     * second of which is not SQL. A trailing comment is the same defect wearing
     * a different hat -- "INSERT ... VALUES (?) -- why" swallows the clause and
     * the insert then reports a key of zero for a row it wrote. The same call
     * works on SQLite and MySQL, which never append anything, so either one is a
     * portability hole in exactly the engine this method exists for.
     *
     * <p>Whatever followed the statement is kept and follows the clause, so the
     * terminator still terminates and the comment still explains.
     */
    private String withReturning(String sql, String idColumn) throws IOException {
        int end = dialect.endOfStatement(sql);
        return sql.substring(0, end) + " RETURNING " + dialect.quote(idColumn)
                + sql.substring(end);
    }

    /**
     * How this connection's engine spells what the three of them spell
     * differently: parameter placeholders, identifier quoting, column types, the
     * declaration of a generated key.
     *
     * <p>Statements passed to {@link #execute} and {@link #query} are already
     * rendered through it, so ordinary code never needs this. Schema generation
     * does -- it has to ask what this engine calls a 64-bit integer.
     */
    public Dialect dialect() {
        return dialect;
    }

    /**
     * The statement as this engine wants it. See {@link Dialect#bind}: a portable
     * statement binds ? and PostgreSQL is handed $1, $2; a statement that carries
     * no placeholder at all is passed through untouched, which is what keeps SQL
     * written for one engine working.
     */
    private String bind(String sql, Object[] params) throws IOException {
        return dialect.bind(sql, params == null ? 0 : params.length);
    }

    /**
     * Runs body inside a transaction, committing when it returns and rolling back
     * if it throws.
     *
     * SQLite gets BEGIN IMMEDIATE, which takes the write lock up front rather than
     * discovering the conflict at the first write; the other two get a plain
     * BEGIN, which is what they support.
     */
    public synchronized Object transaction(Work body) throws Exception {
        if(sqlite != null) {
            final Work outer = body;
            final Database self = this;
            return sqlite.transaction(new Db.Work() {
                public Object run(Db ignored) throws Exception {
                    return outer.run(self);
                }
            });
        }
        control("BEGIN");
        boolean committed = false;
        try {
            Object result = body.run(this);
            control("COMMIT");
            committed = true;
            return result;
        } finally {
            if(!committed) {
                try {
                    control("ROLLBACK");
                } catch (Exception err) {
                    // The original failure is the one worth reporting; a rollback
                    // that also fails must not replace it.
                    System.err.println("rollback failed: " + err);
                }
            }
        }
    }

    /**
     * Transaction control. MySQL will not PREPARE these statements, so they go
     * through its text protocol; PostgreSQL prepares them like anything else. The
     * strings are constants in this file, never a caller's, which is what keeps
     * the text path from being an injection route.
     */
    private void control(String sql) throws IOException {
        if(mysql != null) {
            if("BEGIN".equals(sql)) {
                mysql.begin();
            } else if("COMMIT".equals(sql)) {
                mysql.commit();
            } else {
                mysql.rollback();
            }
            return;
        }
        execute(sql, null);
    }

    /**
     * The id the most recent insert produced, or 0 where the engine has no such
     * concept. PostgreSQL is the case that has none: use INSERT ... RETURNING.
     *
     * <p>SYNCHRONIZED like every other operation on this class, which is the
     * point: it was the one that was not. A close on another thread -- a pool
     * shutting down, a reconnect -- could therefore run between this reading the
     * engine and the engine reading its own state.
     */
    public synchronized long lastInsertId() {
        if(sqlite != null) {
            return sqlite.lastInsertId();
        }
        if(mysql != null) {
            return mysql.lastInsertId();
        }
        return 0;
    }

    /**
     * SQLite-only tuning, ignored elsewhere. Write-ahead logging is what lets
     * readers run while a writer is active, and it has no counterpart on a server
     * engine that already does.
     */
    public void tuneForConcurrency(int busyTimeoutMillis) throws IOException {
        if(sqlite != null) {
            sqlite.enableWriteAheadLog();
            sqlite.setBusyTimeout(busyTimeoutMillis);
        }
    }

    /** The underlying SQLite handle, or null when this is a server engine. */
    public Db asSqlite() {
        return sqlite;
    }

    /**
     * SYNCHRONIZED, like execute, query and transaction on this class.
     *
     * <p>Without it a shutdown or a reconnect could close the engine while another
     * handler was inside an operation, so Postgres.close or MySql.close wrote its
     * termination packet into a Wire another thread was mid-exchange on -- and on
     * the packaged TLS path freed the native session while that thread was reading
     * through it. The engines' own close() methods were given this lock already;
     * the facade that fronts them was not, which left the same race one level up.
     */
    public synchronized void close() {
        if(sqlite != null) {
            sqliteClosed = true;
            sqlite.close();
        } else if(postgres != null) {
            postgres.close();
        } else {
            mysql.close();
        }
    }

    /** Whether this connection is still usable, which a pool has to know. */
    public boolean isOpen() {
        if(postgres != null) {
            return !postgres.isClosed();
        }
        if(mysql != null) {
            return !mysql.isClosed();
        }
        // The SQLite arm used to answer "yes" whatever had happened to it, so a
        // pool asking the documented usability question put a CLOSED connection
        // back and the next caller got "Database is closed" instead. Db has no
        // isClosed of its own, so closure is recorded here, where it happens.
        return sqlite != null && !sqliteClosed;
    }

    public String toString() {
        return describedAs;
    }

    /**
     * The bit of URL parsing these two schemes need, written here rather than
     * pulled from java.net: URI is not on the server-safe surface, and the
     * password in the userinfo has to be percent-decoded, which a hand-rolled
     * split usually forgets.
     */
    private static final class Url {
        String host = "localhost";
        int port;
        String path = "";
        String user = "";
        String password = "";
        String sslMode = "prefer";
        String caFile;
        int timeoutMillis = 10000;
        int socketTimeoutMillis;

        static Url parse(String url, int defaultPort) throws IOException {
            Url out = new Url();
            out.port = defaultPort;
            int schemeEnd = url.indexOf("://");
            String rest = url.substring(schemeEnd + 3);
            String query = "";
            int queryAt = rest.indexOf('?');
            if(queryAt >= 0) {
                query = rest.substring(queryAt + 1);
                rest = rest.substring(0, queryAt);
            }
            int slash = rest.indexOf('/');
            if(slash >= 0) {
                out.path = decode(rest.substring(slash + 1));
                rest = rest.substring(0, slash);
            }
            int at = rest.lastIndexOf('@');
            if(at >= 0) {
                String credentials = rest.substring(0, at);
                rest = rest.substring(at + 1);
                int colon = credentials.indexOf(':');
                if(colon >= 0) {
                    out.user = decode(credentials.substring(0, colon));
                    out.password = decode(credentials.substring(colon + 1));
                } else {
                    out.user = decode(credentials);
                }
            }
            if(rest.length() > 0) {
                int colon = rest.lastIndexOf(':');
                // A bare IPv6 literal has colons of its own; only a colon after the
                // closing bracket is a port.
                int bracket = rest.lastIndexOf(']');
                if(colon > bracket) {
                    out.host = strip(rest.substring(0, colon));
                    // An EXPLICIT port has to be a real one. Integer.parseInt accepts
                    // a sign, so "host:-1" parsed cleanly and handed -1 downstream,
                    // where Postgres.connect and MySql.connect both read any
                    // non-positive port as "unset" and substituted 5432 / 3306 --
                    // a mistyped deployment setting silently connecting to the
                    // default port of a database nobody meant to reach, rather than
                    // failing at startup where it can be seen. An ABSENT port still
                    // takes the default; out.port was seeded with it above, and this
                    // branch runs only when the URL spelled one out.
                    String portText = rest.substring(colon + 1).trim();
                    out.port = portNumber(portText);
                    if(out.port < 1) {
                        // The URL carries the PASSWORD, and this string goes to a
                        // log. describe() exists because of that and omits it; an
                        // error path that pastes the whole URL undoes the care
                        // taken everywhere else.
                        throw new IOException("Not a port number in the database URL for "
                                + strip(rest.substring(0, colon)));
                    }
                } else {
                    out.host = strip(rest);
                }
            }
            applyQuery(out, query);
            return out;
        }

        /**
         * {@code text} as a TCP port, or -1 unless it is 1 to 65535 written in
         * plain ASCII digits. Every rejected spelling -- a sign, a decimal point,
         * an empty string, a number past the port space -- answers the same way,
         * so the caller has one test rather than a parse plus a range check that
         * could disagree.
         */
        private static int portNumber(String text) {
            if(text.length() < 1 || text.length() > 5) {
                return -1;
            }
            int out = 0;
            for(int iter = 0 ; iter < text.length() ; iter++) {
                char c = text.charAt(iter);
                if(c < '0' || c > '9') {
                    return -1;
                }
                out = out * 10 + (c - '0');
            }
            return (out < 1 || out > 65535) ? -1 : out;
        }

        private static void applyQuery(Url out, String query) throws IOException {
            int at = 0;
            while(at < query.length()) {
                int end = query.indexOf('&', at);
                if(end < 0) {
                    end = query.length();
                }
                String pair = query.substring(at, end);
                at = end + 1;
                int equals = pair.indexOf('=');
                if(equals < 0) {
                    continue;
                }
                String key = pair.substring(0, equals);
                String value = decode(pair.substring(equals + 1));
                if("sslmode".equals(key) || "ssl".equals(key)) {
                    if(!"require".equals(value) && !"prefer".equals(value)
                            && !"disable".equals(value)) {
                        throw new IOException("sslmode must be require, prefer or "
                                + "disable, not '" + value + "'");
                    }
                    out.sslMode = value;
                } else if("user".equals(key)) {
                    out.user = value;
                } else if("password".equals(key)) {
                    out.password = value;
                } else if("sslrootcert".equals(key) || "sslca".equals(key)) {
                    out.caFile = value;
                } else if("connectTimeout".equals(key)) {
                    try {
                        out.timeoutMillis = Integer.parseInt(value.trim());
                    } catch (NumberFormatException err) {
                        throw new IOException("connectTimeout must be a number of "
                                + "milliseconds, not '" + value + "'");
                    }
                    // A negative one is refused here so the two arms cannot fail
                    // differently: Java SE throws IllegalArgumentException out of
                    // Socket.connect, while the packaged client reads any
                    // non-positive value as "block forever" and waits out the
                    // OS TCP timeout. Same URL, one an error and the other a
                    // hang, which is the worst kind of difference to debug.
                    if(out.timeoutMillis < 0) {
                        throw new IOException("connectTimeout must not be negative: '"
                                + value + "'. Use 0 for the platform default.");
                    }
                } else if("socketTimeout".equals(key)) {
                    // A DEADLINE ON THE CONVERSATION, not on reaching the host.
                    // connectTimeout is spent by the time a query goes out, and a
                    // peer that answers the handshake and then stops replying
                    // holds the calling thread for as long as it likes -- a
                    // request worker, or a virtual thread's carrier, so a few
                    // stalled connections are the whole server.
                    //
                    // Default 0, meaning none, because that is what pgjdbc and
                    // MySQL's own client default to, for a reason worth keeping:
                    // a legitimate query can take longer than any number picked
                    // here, and aborting one is a worse failure than the hang it
                    // prevents. A deployment that knows its queries sets it.
                    try {
                        out.socketTimeoutMillis = Integer.parseInt(value.trim());
                    } catch (NumberFormatException err) {
                        throw new IOException("socketTimeout must be a number of "
                                + "milliseconds, not '" + value + "'");
                    }
                    if(out.socketTimeoutMillis < 0) {
                        throw new IOException("socketTimeout must not be negative: '"
                                + value + "'. Use 0 for no deadline.");
                    }
                }
            }
        }

        /** Never includes the password: this ends up in logs. */
        String describe(String scheme) {
            return scheme + "://" + user + "@" + host + ":" + port + "/" + path
                    + " (sslmode=" + sslMode
                    + (caFile == null ? "" : ", sslrootcert=" + caFile) + ")";
        }

        private static String strip(String host) {
            if(host.length() > 1 && host.charAt(0) == '['
                    && host.charAt(host.length() - 1) == ']') {
                return host.substring(1, host.length() - 1);
            }
            return host;
        }

        private static String decode(String value) throws IOException {
            if(value.indexOf('%') < 0) {
                return value;
            }
            // A LITERAL CHARACTER IS ENCODED, not narrowed. Writing (byte)c put
            // the low eight bits of a UTF-16 char into a buffer that is then read
            // back as UTF-8, so a password of "p\u00e4ss%40word" became byte 0xE4
            // -- a lone continuation-less lead byte -- and decoded to U+FFFD. The
            // credential, database name or CA path silently became a DIFFERENT
            // string, and only when something else in it needed escaping: with no
            // escape anywhere this method is never reached.
            //
            // Literals are gathered and handed to the platform encoder rather than
            // encoded by hand, which is what makes a surrogate pair come out as
            // the one four-byte sequence it is instead of two malformed halves.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StringBuilder literal = new StringBuilder();
            try {
                for(int iter = 0 ; iter < value.length() ; iter++) {
                    char c = value.charAt(iter);
                    if(c == '%') {
                        int high = iter + 2 < value.length()
                                ? digit(value.charAt(iter + 1)) : -1;
                        int low = iter + 2 < value.length()
                                ? digit(value.charAt(iter + 2)) : -1;
                        if(high < 0 || low < 0) {
                            // A '%' INTRODUCES TWO HEX DIGITS OR THE URL IS
                            // MALFORMED. RFC 3986 2.1 leaves no other reading, and
                            // this used to append the '%' as a literal: "p%ZZ" and
                            // "p%2" became passwords containing a percent sign, so
                            // the client authenticated with a value the URL does
                            // not contain and the operator read a remote
                            // authentication failure rather than a typo in their
                            // own configuration -- the same silent substitution
                            // the UTF-8 check below exists for, arriving earlier.
                            //
                            // The request target takes the OPPOSITE decision, and
                            // on purpose: browsers do send a bare '%', and a
                            // server that refused one would reject real traffic. A
                            // database URL is configuration somebody typed, so
                            // there is no such traffic to keep working, and the
                            // index is all this says about it -- the component may
                            // be a password.
                            throw new IOException("The URL holds a '%' at index "
                                    + iter + " that is not followed by two hex "
                                    + "digits; percent-encode it as %25");
                        }
                        flushLiteral(literal, out);
                        out.write((high << 4) | low);
                        iter += 2;
                        continue;
                    }
                    literal.append(c);
                }
                flushLiteral(literal, out);
                byte[] bytes = out.toByteArray();
                // AND THE RESULT HAS TO BE TEXT. Each escape can be a valid
                // triplet while the run they form is not valid UTF-8 -- "%C3%28"
                // is a lead byte followed by something that cannot continue it --
                // and new String does not refuse that, it SUBSTITUTES U+FFFD. The
                // same silent change of credential, database name or CA path the
                // comment above describes, arriving by the other road: the client
                // then authenticates with a password the URL does not contain and
                // the operator reads a remote authentication failure instead of a
                // malformed setting.
                if(!Utf8.isValid(bytes, 0, bytes.length)) {
                    throw new IOException("A percent-escape in the database URL is "
                            + "not valid UTF-8, so the value it names cannot be "
                            + "read; check the escaping of the user, password, "
                            + "database name or file path");
                }
                return new String(bytes, 0, bytes.length, "UTF-8");
            } catch (UnsupportedEncodingException err) {
                return value;
            }
        }

        /** Writes the pending literal run as UTF-8 and empties it. */
        private static void flushLiteral(StringBuilder literal, ByteArrayOutputStream out)
                throws UnsupportedEncodingException {
            if(literal.length() == 0) {
                return;
            }
            byte[] encoded = literal.toString().getBytes("UTF-8");
            out.write(encoded, 0, encoded.length);
            literal.setLength(0);
        }

        private static int digit(char c) {
            if(c >= '0' && c <= '9') {
                return c - '0';
            }
            if(c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            }
            if(c >= 'A' && c <= 'F') {
                return c - 'A' + 10;
            }
            return -1;
        }
    }
}
