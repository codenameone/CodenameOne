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
 * What differs between engines, and cannot be papered over: {@link #lastInsertId}
 * is meaningful for SQLite and MySQL and always 0 for PostgreSQL, which has no
 * such concept -- use `INSERT ... RETURNING id` there and read it as a row.
 */
public final class Database {
    private final Db sqlite;
    /** Db has no isClosed, so closure is recorded where it happens. */
    private boolean sqliteClosed;
    private final Postgres postgres;
    private final MySql mysql;
    private final String describedAs;

    private Database(Db sqlite, Postgres postgres, MySql mysql, String describedAs) {
        this.sqlite = sqlite;
        this.postgres = postgres;
        this.mysql = mysql;
        this.describedAs = describedAs;
    }

    /** A unit of work run inside {@link #transaction}. */
    public interface Work {
        Object run(Database db) throws Exception;
    }

    /**
     * Opens the database the URL names. Anything that is not a recognised scheme
     * is taken as a SQLite path, so an ordinary file name keeps working.
     */
    public static Database open(String url) throws IOException {
        if(url == null) {
            throw new IOException("No database URL");
        }
        if(url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            Url parsed = Url.parse(url, 5432);
            return new Database(null, Postgres.connect(parsed.host, parsed.port,
                    parsed.path, parsed.user, parsed.password, parsed.sslMode,
                    parsed.caFile, parsed.timeoutMillis), null, parsed.describe("postgres"));
        }
        if(url.startsWith("mysql://") || url.startsWith("mariadb://")) {
            Url parsed = Url.parse(url, 3306);
            return new Database(null, null, MySql.connect(parsed.host, parsed.port,
                    parsed.path, parsed.user, parsed.password, parsed.sslMode,
                    parsed.caFile, parsed.timeoutMillis), parsed.describe("mysql"));
        }
        return new Database(Db.open(url), null, null, "sqlite:" + url);
    }

    /** Wraps an already-open SQLite handle, for code that opened one directly. */
    public static Database of(Db db) {
        return new Database(db, null, null, "sqlite");
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
        if(sqlite != null) {
            return sqlite.execute(sql, params);
        }
        if(postgres != null) {
            return postgres.execute(sql, params);
        }
        return mysql.execute(sql, params);
    }

    /** Runs a query and returns every row as a column-name to value map. */
    public synchronized List query(String sql, Object[] params) throws IOException {
        if(sqlite != null) {
            return sqlite.query(sql, params);
        }
        if(postgres != null) {
            return postgres.query(sql, params);
        }
        return mysql.query(sql, params);
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
     */
    public long lastInsertId() {
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

    public void close() {
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

        private static String decode(String value) {
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
                    if(c == '%' && iter + 2 < value.length()) {
                        int high = digit(value.charAt(iter + 1));
                        int low = digit(value.charAt(iter + 2));
                        if(high >= 0 && low >= 0) {
                            flushLiteral(literal, out);
                            out.write((high << 4) | low);
                            iter += 2;
                            continue;
                        }
                    }
                    literal.append(c);
                }
                flushLiteral(literal, out);
                byte[] bytes = out.toByteArray();
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
