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

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "DBCHECK OK" : "DBCHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
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
        parameterCountsMustMatch(db, postgres);

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
