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
package com.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Database;

/**
 * Exercises the database layer against a REAL server, one engine per run.
 *
 * The same assertions run against SQLite, PostgreSQL and MySQL, which is the
 * point: the Database facade claims a handler cannot tell which engine answered
 * it, and the only way to hold that claim is to run one body of checks against
 * all three and require the same answers. Value TYPES are asserted as well as
 * values, because that is where the engines differ if nobody looks.
 *
 * Point it at a database with CN1_DBCHECK_URL. Without one it runs the SQLite
 * arm only, which needs nothing installed.
 */
public class DbCheck {
    private static int passed;
    private static final List failures = new ArrayList();

    public static void main(String[] args) throws Exception {
        String url = System.getenv("CN1_DBCHECK_URL");
        if(url == null || url.length() == 0) {
            url = ":memory:";
            note("CN1_DBCHECK_URL is unset, running the SQLite arm only");
        }
        System.out.println("checking " + url);
        Database db = Database.open(url);
        try {
            System.out.println("connected to " + db);
            run(db, url);
        } finally {
            db.close();
        }
        rejectsAnUntrustedCertificate(url);
        refusesCleartextPasswordWithoutTls();

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "DBCHECK OK" : "DBCHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    /**
     * A cleartext password is not handed to an unencrypted connection.
     *
     * <p>PostgreSQL's AuthenticationCleartextPassword sends the password AS
     * ITSELF. With the default sslmode=prefer the socket is clear whenever the
     * PEER answers 'N' to the SSLRequest -- and an attacker in the path can answer
     * for it, then ask for method 3 and read the credential. Verified by hand
     * against a server that asks for it: refused with sslmode absent, and still
     * connecting with sslmode=disable, which is the caller choosing a clear
     * channel rather than an attacker choosing it for them.
     *
     * <p>NEEDS A SERVER THAT ASKS FOR CLEARTEXT, which an ordinary PostgreSQL does
     * not -- they default to scram-sha-256. Point CN1_DBCHECK_CLEARTEXT_URL at one
     * to run this; it skips loudly otherwise, and does not run in CI today:
     *
     * <pre>
     * podman run -d --rm --name pg-cleartext -e POSTGRES_PASSWORD=secret \
     *     -e POSTGRES_USER=cn1 -e POSTGRES_DB=cn1 \
     *     -e POSTGRES_HOST_AUTH_METHOD=password -p 55433:5432 postgres:16
     * CN1_DBCHECK_CLEARTEXT_URL=postgres://cn1:secret@127.0.0.1:55433/cn1
     * </pre>
     */
    private static void refusesCleartextPasswordWithoutTls() throws Exception {
        String url = System.getenv("CN1_DBCHECK_CLEARTEXT_URL");
        if(url == null || url.length() == 0) {
            note("cleartext-auth check skipped: set CN1_DBCHECK_CLEARTEXT_URL to a "
                    + "server with POSTGRES_HOST_AUTH_METHOD=password");
            return;
        }
        String refused;
        try {
            Database.open(url).close();
            refused = "connected";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            // The wording, not merely that it threw: the server being down would
            // also throw, and would pass a bare "it failed" check.
            refused = message.indexOf("cleartext password") >= 0
                    ? "refused" : "other: " + message;
        }
        check("a cleartext password is refused on an unencrypted connection",
                "refused", refused);

        // And sslmode=disable still connects, because that is the caller saying the
        // password may cross in the clear. Losing that would make the fix a
        // regression for anyone who meant it.
        String opted;
        try {
            Database db = Database.open(url + (url.indexOf('?') < 0 ? "?" : "&")
                    + "sslmode=disable");
            try {
                opted = String.valueOf(db.query("SELECT 1 AS one", null).size());
            } finally {
                db.close();
            }
        } catch (Exception err) {
            opted = "threw: " + err.getMessage();
        }
        check("sslmode=disable still connects to it", "1", opted);
    }

    private static void run(Database db, String url) throws Exception {
        boolean postgres = url.startsWith("postgres");
        boolean mysql = url.startsWith("mysql") || url.startsWith("mariadb");
        // Each engine spells "auto-incrementing primary key" and "binary blob"
        // differently. Everything BELOW this line is identical for all three,
        // which is the part being tested.
        String key = postgres ? "id SERIAL PRIMARY KEY"
                : (mysql ? "id BIGINT AUTO_INCREMENT PRIMARY KEY"
                         : "id INTEGER PRIMARY KEY AUTOINCREMENT");
        String blob = postgres ? "BYTEA" : (mysql ? "BLOB" : "BLOB");
        String real = postgres ? "DOUBLE PRECISION" : "DOUBLE";

        db.execute("DROP TABLE IF EXISTS cn1_check", null);
        db.execute("CREATE TABLE cn1_check (" + key + ", name VARCHAR(64), size " + real
                + ", payload " + blob + ")", null);

        check("an insert reports one row", "1", String.valueOf(db.execute(
                "INSERT INTO cn1_check (name, size, payload) VALUES ("
                + placeholders(postgres, 3) + ")",
                new Object[]{"first", Double.valueOf(1.5), bytes("hello")})));

        db.execute("INSERT INTO cn1_check (name, size, payload) VALUES ("
                + placeholders(postgres, 3) + ")",
                new Object[]{"second", Double.valueOf(2.5), null});

        List rows = db.query("SELECT id, name, size, payload FROM cn1_check ORDER BY id",
                null);
        check("both rows come back", "2", String.valueOf(rows.size()));

        Map first = (Map)rows.get(0);
        // The TYPES are the contract, not just the values: a handler that gets a
        // String where it got a Long on the other engine is broken by the switch.
        check("an integer column is a Long", "java.lang.Long", typeOf(first.get("id")));
        check("a text column is a String", "java.lang.String", typeOf(first.get("name")));
        check("a real column is a Double", "java.lang.Double", typeOf(first.get("size")));
        check("a blob column is a byte[]", "byte[]", typeOf(first.get("payload")));
        check("the text value survives", "first", String.valueOf(first.get("name")));
        check("the real value survives", "1.5", String.valueOf(first.get("size")));
        check("the blob value survives", "hello",
                new String((byte[])first.get("payload"), "UTF-8"));

        Map second = (Map)rows.get(1);
        check("a NULL column is null", "null", String.valueOf(second.get("payload")));

        // DECIMAL is the one type the three engines cannot be asked to agree
        // on, because SQLite does not have it: a column DECLARED DECIMAL there
        // has NUMERIC affinity and stores an INTEGER or a REAL, so there is no
        // exact-decimal value to compare against. On the two servers that do
        // have it, the column exists precisely because a double would not hold
        // the value -- so it has to come back exact and it has to come back as
        // a number the caller can read, not as a BLOB that JSON base64s.
        if(postgres || mysql) {
            String exact = "123456789012345678901234567890.12345";
            db.execute("DROP TABLE IF EXISTS cn1_check_decimal", null);
            db.execute("CREATE TABLE cn1_check_decimal (amount DECIMAL(65,5))", null);
            db.execute("INSERT INTO cn1_check_decimal (amount) VALUES (" + exact + ")", null);
            List decimals = db.query("SELECT amount FROM cn1_check_decimal", null);
            Object amount = ((Map)decimals.get(0)).get("amount");
            check("a decimal column is a String", "java.lang.String", typeOf(amount));
            check("the decimal value is exact", exact, String.valueOf(amount));
        }

        // Binding, not interpolation. A value containing a quote would end the
        // statement early if this were concatenated.
        db.execute("INSERT INTO cn1_check (name, size) VALUES (" + placeholders(postgres, 2) + ")",
                new Object[]{"O'Brien; DROP TABLE cn1_check; --", Double.valueOf(0)});
        List quoted = db.query("SELECT name FROM cn1_check WHERE name = "
                + placeholder(postgres, 1), new Object[]{"O'Brien; DROP TABLE cn1_check; --"});
        check("a quote in a bound value is data, not syntax", "1",
                String.valueOf(quoted.size()));

        List counted = db.query("SELECT COUNT(*) AS total FROM cn1_check", null);
        check("the table survived the injection attempt", "3",
                String.valueOf(((Map)counted.get(0)).get("total")));

        int updated = db.execute("UPDATE cn1_check SET size = " + placeholder(postgres, 1)
                + " WHERE name = " + placeholder(postgres, 2),
                new Object[]{Double.valueOf(9.5), "second"});
        check("an update reports the rows it changed", "1", String.valueOf(updated));

        // A transaction that throws must leave nothing behind.
        try {
            db.transaction(new Database.Work() {
                public Object run(Database inner) throws Exception {
                    inner.execute("INSERT INTO cn1_check (name, size) VALUES ("
                            + placeholders(inner.toString().startsWith("postgres"), 2) + ")",
                            new Object[]{"rolled-back", Double.valueOf(1)});
                    throw new IllegalStateException("deliberate");
                }
            });
            failures.add("a failing transaction must propagate its exception");
        } catch (IllegalStateException expected) {
            passed++;
        }
        List afterRollback = db.query("SELECT id FROM cn1_check WHERE name = "
                + placeholder(postgres, 1), new Object[]{"rolled-back"});
        check("a rolled-back insert left nothing", "0", String.valueOf(afterRollback.size()));

        Object committed = db.transaction(new Database.Work() {
            public Object run(Database inner) throws Exception {
                inner.execute("INSERT INTO cn1_check (name, size) VALUES ("
                        + placeholders(inner.toString().startsWith("postgres"), 2) + ")",
                        new Object[]{"committed", Double.valueOf(1)});
                return "done";
            }
        });
        check("a transaction returns its body's value", "done", String.valueOf(committed));
        List afterCommit = db.query("SELECT id FROM cn1_check WHERE name = "
                + placeholder(postgres, 1), new Object[]{"committed"});
        check("a committed insert is there", "1", String.valueOf(afterCommit.size()));

        // A statement error must be an exception, not a silent zero.
        try {
            db.query("SELECT no_such_column FROM cn1_check", null);
            failures.add("a bad statement must throw");
        } catch (Exception expected) {
            passed++;
        }
        // ...and the connection must still work afterwards, which is what a
        // desynchronised protocol implementation gets wrong.
        List afterError = db.query("SELECT COUNT(*) AS total FROM cn1_check", null);
        check("the connection survives a statement error", "4",
                String.valueOf(((Map)afterError.get(0)).get("total")));

        if(!postgres) {
            // PostgreSQL has no last-insert-id; the facade documents that it
            // returns 0 there rather than pretending.
            db.execute("INSERT INTO cn1_check (name, size) VALUES ("
                    + placeholders(postgres, 2) + ")",
                    new Object[]{"with-id", Double.valueOf(1)});
            check("the new row's id is reported", "true",
                    String.valueOf(db.lastInsertId() > 0));
        } else {
            List returning = db.query("INSERT INTO cn1_check (name, size) VALUES ($1, $2) "
                    + "RETURNING id", new Object[]{"with-id", Double.valueOf(1)});
            check("RETURNING gives the new id", "true",
                    String.valueOf(((Map)returning.get(0)).get("id") != null));
        }

        db.execute("DROP TABLE cn1_check", null);
        parameterCountsMustMatch(db, postgres);
        oversizeMessagesAreRefused(url, postgres, mysql);
        wholePacketsPastTheBoundAreRefused(url, mysql);
        concurrentTransactionsOwnTheSession(db, postgres);
    }

    /**
     * Four handlers, one shared Database, transactions that must not interleave.
     *
     * This is the engine-agnostic half of a hazard SQLite only half has. Postgres
     * and MySql each own a Wire, and a Wire owns ONE buffer with a position and a
     * limit plus one output stream every message is built in; MySql also carries
     * the packet sequence number. Two calls at once therefore write into the same
     * message buffer and move each other's parse position -- protocol corruption,
     * not merely one request's rows committed by another's COMMIT. Which of the
     * two failures shows up first is timing, so this asserts on both: no thread
     * may fail, and the row count must be exact.
     */
    private static void concurrentTransactionsOwnTheSession(final Database db,
            boolean postgres) throws Exception {
        // The engine's own placeholder spelling, like every other statement here:
        // PostgreSQL wants $1 and answers "syntax error at or near )" for a ?.
        final String slot = placeholders(postgres, 1);
        // Dropped first, because the table outlives a run that dies. The finally
        // below cannot fire if the process is killed, and the next run then meets
        // "relation already exists" -- against a SHARED server, which is what CI
        // uses, that turns one interrupted run into a failure for every run after
        // it. Cost me exactly that here.

        db.execute("DROP TABLE IF EXISTS cn1_lock", null);
        db.execute("CREATE TABLE cn1_lock (tag TEXT)", null);
        try {
            final List failures = new ArrayList();
            Thread[] threads = new Thread[4];
            for(int t = 0 ; t < threads.length ; t++) {
                final String tag = "t" + t;
                threads[t] = new Thread(new Runnable() {
                    public void run() {
                        for(int round = 0 ; round < 10 ; round++) {
                            try {
                                db.transaction(new Database.Work() {
                                    public Object run(Database inner) throws Exception {
                                        inner.execute("INSERT INTO cn1_lock (tag) VALUES (" + slot + ")",
                                                new Object[]{tag});
                                        inner.execute("INSERT INTO cn1_lock (tag) VALUES (" + slot + ")",
                                                new Object[]{tag});
                                        return null;
                                    }
                                });
                            } catch (Exception err) {
                                synchronized(failures) {
                                    failures.add(String.valueOf(err));
                                }
                                return;
                            }
                        }
                    }
                });
                threads[t].start();
            }
            for(int t = 0 ; t < threads.length ; t++) {
                threads[t].join(60000);
            }
            check("concurrent transactions on one session all succeed", "0",
                    String.valueOf(failures.size())
                            + (failures.isEmpty() ? "" : " -> " + failures.get(0)));
            List counted = db.query("SELECT COUNT(*) AS n FROM cn1_lock", null);
            check("every transaction wrote both of its rows", "80",
                    String.valueOf(((Map)counted.get(0)).get("n")));
        } finally {
            db.execute("DROP TABLE cn1_lock", null);
        }
    }

    /**
     * The same URL with sslmode=require and no CA named must FAIL against a server
     * whose certificate this host does not trust.
     *
     * Verification is the half of TLS that fails open: a client that encrypts and
     * does not verify looks exactly like one that does, right up to the moment
     * someone is in the middle. This check runs only when the URL under test named
     * a CA, because that is precisely the case where the system store must not be
     * enough.
     */
    private static void rejectsAnUntrustedCertificate(String url) {
        int at = url.indexOf("sslrootcert=");
        if(at < 0) {
            note("untrusted-certificate check skipped: this URL names no CA");
            return;
        }
        int end = url.indexOf('&', at);
        String withoutCa = url.substring(0, at) + (end < 0 ? "" : url.substring(end + 1));
        try {
            Database db = Database.open(withoutCa);
            db.close();
            failures.add("a certificate signed by an untrusted CA was accepted");
        } catch (Exception expected) {
            passed++;
        }
    }

    /** PostgreSQL numbers its placeholders; the other two use a question mark. */
    private static String placeholder(boolean postgres, int index) {
        return postgres ? "$" + index : "?";
    }

    /**
     * A server message larger than the configured bound is refused.
     *
     * <p>Both clients read a LENGTH the peer chose and allocate to match, before
     * the connection is authenticated and, with sslmode=prefer or before MySQL's
     * TLS, before it is encrypted. PostgreSQL's 32-bit length can name 2GB in one
     * message; MySQL's logical packet is built from 16MB continuations with no
     * limit on the count. An OutOfMemoryError from either is not an IOException --
     * it unwinds past every catch here and takes the process down.
     *
     * <p>Driven from a REAL server rather than a fake one: asking for a value
     * bigger than the bound makes the server send exactly the oversized message
     * the guard exists to refuse. Needs CN1_DB_MAX_MESSAGE_MB set small, because
     * proving the 64MB default would mean moving 64MB to prove it.
     */
    private static void oversizeMessagesAreRefused(String url,
            boolean postgres, boolean mysql) throws Exception {
        int limitMb = configuredLimitMb();
        if(limitMb < 1 || limitMb > 4) {
            note("oversize-message check skipped: set CN1_DB_MAX_MESSAGE_MB to 1..4");
            return;
        }
        if(!postgres && !mysql) {
            note("oversize-message check skipped: it needs a network engine");
            return;
        }
        // MySQL's continuation path only engages past ONE packet, which is 16MB by
        // protocol, so the oversize must clear that before the accumulator can see
        // it. PostgreSQL has no such floor and one message is enough.
        int bytes = mysql ? 20 * 1024 * 1024 : (limitMb * 1024 * 1024) + (1024 * 1024);
        String sql = postgres
                ? "SELECT repeat('x', " + bytes + ") AS big"
                : "SELECT REPEAT('x', " + bytes + ") AS big";
        // ITS OWN CONNECTION, because refusing the oversized message CLOSES the
        // one it arrives on -- that is the point of the last assertion here -- and
        // a check that used the shared Database would leave every check after it
        // talking to a dead session. Measured the hard way: it did.
        Database db = Database.open(url);
        try {
        // THE ORDINARY MESSAGE FIRST. Refusing an oversized one leaves the rest of
        // it unread on the wire, so the client closes the connection rather than
        // hand a desynchronised one back to the pool -- which means nothing can be
        // asked of it afterwards, and a check that tried would be testing the
        // close instead of the bound.
        List small = db.query(postgres ? "SELECT repeat('y', 1024) AS small"
                                       : "SELECT REPEAT('y', 1024) AS small", null);
        check("a message inside the bound still arrives", "1024",
                small.isEmpty() ? "<none>"
                        : String.valueOf(String.valueOf(
                                ((Map)small.get(0)).get("small")).length()));

        String outcome;
        try {
            List rows = db.query(sql, null);
            Object value = rows.isEmpty() ? null : ((Map)rows.get(0)).get("big");
            outcome = "returned " + (value == null ? "null"
                    : String.valueOf(String.valueOf(value).length()));
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            // The bound's own wording. The server refusing to build the value, or
            // the connection dropping, would also throw and would pass a bare
            // "it failed" check while proving nothing.
            outcome = message.indexOf("CN1_DB_MAX_MESSAGE_MB") >= 0
                    ? "refused" : "other: " + message;
        }
        check("a message past the bound is refused", "refused", outcome);

        // And the connection is CLOSED by it, not left desynchronised for the next
        // borrower to read this message's tail as its own answer.
        String afterwards;
        try {
            db.query(postgres ? "SELECT 1 AS one" : "SELECT 1 AS one", null);
            afterwards = "still usable";
        } catch (Exception expected) {
            afterwards = "closed";
        }
        check("and the connection is taken out of service", "closed", afterwards);
        } finally {
            db.close();
        }
    }

    /** The ceiling the two bound checks run under, or 0 when it is unusable. */
    private static int configuredLimitMb() {
        String configured = System.getenv("CN1_DB_MAX_MESSAGE_MB");
        if(configured == null) {
            return 0;
        }
        try {
            return Integer.parseInt(configured.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * The bound holds on a packet that arrives WHOLE, which on MySQL is the case
     * the check above cannot reach.
     *
     * <p>MySQL splits a long value into 16MB packets, and the bound used to live
     * only in the branch that reassembles them. A ceiling set under 16MB was
     * therefore enforced nowhere at all: one packet just short of the maximum is
     * not full-length, so the loop that checks never ran, and the read ahead of it
     * had already allocated what the peer asked for. The check above asks for 20MB
     * precisely to exercise the split, which is why it never noticed.
     *
     * <p>A few megabytes against a one-megabyte ceiling fits in a single packet and
     * so proves the bound rather than the reassembly. Before the guard moved ahead
     * of the read this returned the value in full.
     *
     * <p>PostgreSQL has no 16MB floor -- one message is one message -- so its side
     * is already covered above and this check is MySQL's alone.
     */
    private static void wholePacketsPastTheBoundAreRefused(String url, boolean mysql)
            throws Exception {
        int limitMb = configuredLimitMb();
        if(limitMb < 1 || limitMb > 4) {
            note("whole-packet bound check skipped: set CN1_DB_MAX_MESSAGE_MB to 1..4");
            return;
        }
        if(!mysql) {
            note("whole-packet bound check skipped: it is MySQL's 16MB packet floor");
            return;
        }
        // Over the ceiling and under MAX_PACKET_BODY, so the server sends it as one.
        int bytes = (limitMb * 1024 * 1024) + (1024 * 1024);
        // ITS OWN CONNECTION, for the reason the check above documents: the refusal
        // closes the connection it arrives on.
        Database db = Database.open(url);
        try {
            List small = db.query("SELECT REPEAT('y', 1024) AS small", null);
            check("a whole packet inside the bound still arrives", "1024",
                    small.isEmpty() ? "<none>"
                            : String.valueOf(String.valueOf(
                                    ((Map)small.get(0)).get("small")).length()));

            String outcome;
            try {
                List rows = db.query("SELECT REPEAT('x', " + bytes + ") AS big", null);
                Object value = rows.isEmpty() ? null : ((Map)rows.get(0)).get("big");
                outcome = "returned " + (value == null ? "null"
                        : String.valueOf(String.valueOf(value).length()));
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                outcome = message.indexOf("CN1_DB_MAX_MESSAGE_MB") >= 0
                        ? "refused" : "other: " + message;
            }
            check("a whole packet past the bound is refused", "refused", outcome);
        } finally {
            db.close();
        }
    }

    /**
     * A statement runs with exactly as many values as it has placeholders, on
     * every engine, and a mismatch is refused before anything is written.
     *
     * <p>Here rather than in SelfTest because the three engines get this wrong in
     * three different ways and only a shared body of checks holds them to one
     * answer. SQLite leaves an unbound parameter NULL and commits a row the caller
     * never wrote. MySQL's COM_STMT_EXECUTE carries a null bitmap and a type table
     * sized by the CLIENT while the server decodes using the statement's own
     * count, so extra descriptors shift the bytes read as the first value -- the
     * wrong rows updated rather than an error. PostgreSQL's Bind is checked by the
     * server, which is the only one of the three that was already safe.
     */
    private static void parameterCountsMustMatch(Database db, boolean postgres)
            throws Exception {
        db.execute("DROP TABLE IF EXISTS cn1_params", null);
        db.execute("CREATE TABLE cn1_params (a VARCHAR(32), b VARCHAR(32))", null);
        try {
            String two = placeholders(postgres, 2);
            // The matched call first, so nothing below can pass because the table
            // or the statement was broken all along.
            db.execute("INSERT INTO cn1_params (a, b) VALUES (" + two + ")",
                    new Object[]{"one", "two"});
            check("a matched statement inserts", "1",
                    String.valueOf(db.query("SELECT a FROM cn1_params", null).size()));

            check("too few values are refused", "refused",
                    refusal(db, "INSERT INTO cn1_params (a, b) VALUES (" + two + ")",
                            new Object[]{"only"}));
            check("no values at all are refused", "refused",
                    refusal(db, "INSERT INTO cn1_params (a, b) VALUES (" + two + ")", null));
            check("too many values are refused", "refused",
                    refusal(db, "INSERT INTO cn1_params (a, b) VALUES (" + two + ")",
                            new Object[]{"a", "b", "c"}));
            // A statement with NO placeholders and values supplied is the MySQL
            // case the wire format punishes hardest: the server decodes nothing
            // and the extra descriptors are read as something else entirely.
            check("values for a statement with no placeholders are refused", "refused",
                    refusal(db, "INSERT INTO cn1_params (a, b) VALUES ('x', 'y')",
                            new Object[]{"surplus"}));

            // And every refusal refused: only the one matched insert is there.
            check("the refused statements wrote nothing", "1",
                    String.valueOf(db.query("SELECT a FROM cn1_params", null).size()));
        } finally {
            db.execute("DROP TABLE IF EXISTS cn1_params", null);
        }
    }

    /** "refused" if the statement threw, "accepted" if it ran. */
    private static String refusal(Database db, String sql, Object[] params) {
        try {
            db.execute(sql, params);
            return "accepted";
        } catch (Exception refused) {
            return "refused";
        }
    }

    private static String placeholders(boolean postgres, int count) {
        StringBuilder out = new StringBuilder();
        for(int iter = 1 ; iter <= count ; iter++) {
            if(iter > 1) {
                out.append(", ");
            }
            out.append(placeholder(postgres, iter));
        }
        return out.toString();
    }

    private static String typeOf(Object value) {
        if(value == null) {
            return "null";
        }
        if(value instanceof byte[]) {
            return "byte[]";
        }
        return value.getClass().getName();
    }

    private static byte[] bytes(String value) throws Exception {
        return value.getBytes("UTF-8");
    }

    private static void check(String name, String expected, String actual) {
        if(expected.equals(actual)) {
            passed++;
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void note(String message) {
        System.out.println("NOTE " + message);
    }
}
