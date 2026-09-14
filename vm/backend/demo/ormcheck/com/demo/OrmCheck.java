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
import java.util.Date;
import java.util.List;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.orm.Dao;
import com.codename1.backend.orm.EntityManager;

/**
 * The ORM against a REAL server, one engine per run.
 *
 * <p>Same shape as dbcheck and for the same reason: the claim is that ONE entity
 * class, annotated once, is stored by SQLite, PostgreSQL and MySQL alike, and the
 * only way to hold that claim is to run one body of assertions against all three
 * and require the same answers. The generated dao is the production one -- the
 * maven plugin wrote it from {@link Note} during generate-contract.sh -- so what
 * runs here is what a developer's project runs.
 *
 * <p>Point it at a database with CN1_ORMCHECK_URL, or CN1_DBCHECK_URL, which is
 * what the test harness already sets. Without either it runs the SQLite arm,
 * which needs nothing installed.
 */
public class OrmCheck {
    private static int passed;
    private static final List failures = new ArrayList();

    public static void main(String[] args) throws Exception {
        // What the generated entry point does at start-up, and the only thing
        // that keeps the generated daos in the binary: the translator drops a
        // class nothing references.
        new cn1app.BackendDaoBootstrap();

        String url = System.getenv("CN1_ORMCHECK_URL");
        if(url == null || url.length() == 0) {
            url = System.getenv("CN1_DBCHECK_URL");
        }
        if(url == null || url.length() == 0) {
            url = ":memory:";
            note("no database URL set, running the SQLite arm only");
        }
        System.out.println("checking " + url);
        DataSource pool = DataSource.open(url, 1);
        try {
            System.out.println("connected to " + pool + " as " + pool.dialect());
            EntityManager em = EntityManager.open(pool);
            Dao<Note> notes = em.dao(Note.class);
            notes.dropTable();
            notes.createTable();
            try {
                roundTripsEveryColumn(notes);
                updatesAndDeletes(notes);
                queriesByFieldName(notes);
                transactionsAreAtomic(em, notes);
                createTableIsIdempotent(notes);
                anEntityThatIsOnlyAKey(em);
                aTerminatedStatementStillAnswersItsKey(pool);
                anUpsertThatUpdatesStillAnswers(pool);
                aMultiRowInsertIsRefused(pool);
            } finally {
                notes.dropTable();
            }
        } finally {
            pool.close();
        }
        configurationChoosesTheDatabase();

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "ORMCHECK OK" : "ORMCHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    /**
     * Every column type, out and back.
     *
     * <p>The types are where the engines differ if nobody looks: a boolean is a
     * SMALLINT on PostgreSQL and a TINYINT on MySQL, a date is milliseconds in a
     * BIGINT rather than a native timestamp whose text format follows the
     * server's time zone, and a blob is BYTEA, BLOB or LONGBLOB.
     */
    private static void roundTripsEveryColumn(Dao<Note> notes) throws Exception {
        Note note = new Note();
        note.title = "first";
        // A QUESTION MARK in a stored value. Every statement goes through the
        // placeholder rewrite, and a value is not a statement.
        note.body = "how? like this";
        note.views = 3;
        note.pinned = true;
        note.score = 1.5;
        note.created = new Date(1700000000000L);
        note.payload = new byte[] {1, 2, 3, 0, 4};
        note.revision = null;
        note.cached = "never stored";
        notes.insert(note);
        check("the generated key is written back", "true", String.valueOf(note.id > 0));

        Note back = notes.findById(Long.valueOf(note.id));
        check("the row is there", "true", String.valueOf(back != null));
        if(back == null) {
            return;
        }
        check("String", "first", back.title);
        check("String with a question mark in it", "how? like this", back.body);
        check("int", "3", String.valueOf(back.views));
        check("boolean", "true", String.valueOf(back.pinned));
        check("double", "1.5", String.valueOf(back.score));
        check("Date", "1700000000000", String.valueOf(back.created.getTime()));
        check("byte[] length", "5", String.valueOf(back.payload == null ? -1 : back.payload.length));
        // A NUL INSIDE the blob, which is where a length-unaware path truncates.
        check("byte[] content", "1,2,3,0,4", join(back.payload));
        check("a null boxed field stays null", "null", String.valueOf(back.revision));
        check("@DbTransient is not stored", "null", String.valueOf(back.cached));

        // And a value in the boxed column comes back as that value.
        back.revision = Long.valueOf(7);
        notes.update(back);
        check("a boxed field round trips", "7",
                String.valueOf(notes.findById(Long.valueOf(note.id)).revision));
    }

    private static void updatesAndDeletes(Dao<Note> notes) throws Exception {
        Note note = new Note();
        note.title = "to change";
        notes.insert(note);
        note.title = "changed";
        check("update reports the row", "true", String.valueOf(notes.update(note)));
        check("the update took", "changed",
                notes.findById(Long.valueOf(note.id)).title);
        check("delete reports the row", "true", String.valueOf(notes.delete(note)));
        // The row that is not there is a case a handler turns into a 404, which
        // is why these answer rather than returning void.
        check("a second delete reports nothing", "false", String.valueOf(notes.delete(note)));
        check("an update of a missing row reports nothing", "false",
                String.valueOf(notes.update(note)));
        check("a missing row reads as null", "null",
                String.valueOf(notes.findById(Long.valueOf(note.id))));
    }

    /**
     * The query builder, which names JAVA FIELDS.
     *
     * <p>`created` maps to a column called createdAt, and PostgreSQL folds an
     * unquoted name to lower case -- so these queries pass only because every
     * identifier the ORM writes is quoted.
     */
    private static void queriesByFieldName(Dao<Note> notes) throws Exception {
        // Cleared first, so what these count is what this method inserted rather
        // than what the checks before it left behind. A query with no conditions
        // deletes the table, which is the reading of the SQL it builds.
        notes.query().delete();
        check("the table is empty after a bulk delete", "0", String.valueOf(notes.count()));
        notes.insert(note("low", 1, false, 1000L));
        notes.insert(note("high", 10, true, 2000L));
        notes.insert(note("mid", 5, false, 3000L));
        check("count", "3", String.valueOf(notes.count()));
        check("gt", "1", String.valueOf(notes.query().gt("views", Integer.valueOf(5)).count()));
        check("eq on a boolean", "1",
                String.valueOf(notes.query().eq("pinned", Boolean.TRUE).count()));
        check("gte on a date and a mixed-case column", "2",
                String.valueOf(notes.query().gte("created", new Date(2000L)).count()));
        check("like", "1", String.valueOf(notes.query().like("title", "lo%").count()));
        check("in", "2",
                String.valueOf(notes.query().in("title", new Object[] {"low", "mid"}).count()));
        // An empty set matches nothing: a filter that silently disappeared would
        // return every row in the table.
        check("in with nothing in it", "0",
                String.valueOf(notes.query().in("title", new Object[0]).count()));
        check("isNull", "3", String.valueOf(notes.query().isNull("body").count()));
        // And a condition that matches some of them deletes only those.
        check("a bulk delete answers how many", "1",
                String.valueOf(notes.query().eq("title", "low").delete()));
        notes.insert(note("low", 1, false, 1000L));

        List<Note> ordered = notes.query().orderBy("views", false).limit(2).list();
        check("order and limit", "2", String.valueOf(ordered.size()));
        check("ordered first", "high", ordered.get(0).title);
        check("ordered second", "mid", ordered.get(1).title);
        check("first()", "low", notes.query().orderBy("views", true).first().title);
        List<Note> offset = notes.query().orderBy("views", true).offset(1).limit(1).list();
        check("offset", "mid", offset.get(0).title);

        // The escape hatch, which is SQL: the column name, not the field name.
        check("a hand-written where clause", "1",
                String.valueOf(notes.find("views > ?", new Object[] {Integer.valueOf(5)}).size()));
    }

    private static void transactionsAreAtomic(EntityManager em, Dao<Note> notes)
            throws Exception {
        final long before = notes.count();
        try {
            em.transaction(new EntityManager.Work() {
                public Object run(EntityManager tx) throws Exception {
                    tx.dao(Note.class).insert(note("rolled back", 1, false, 1L));
                    throw new IllegalStateException("deliberate");
                }
            });
            check("a failing transaction throws", "threw", "returned");
        } catch (IllegalStateException expected) {
            check("a failing transaction throws", "threw", "threw");
        }
        check("a failing transaction wrote nothing", String.valueOf(before),
                String.valueOf(notes.count()));

        em.transaction(new EntityManager.Work() {
            public Object run(EntityManager tx) throws Exception {
                tx.dao(Note.class).insert(note("committed", 1, false, 1L));
                return null;
            }
        });
        check("a successful transaction committed", String.valueOf(before + 1),
                String.valueOf(notes.count()));
    }

    /** Called at every start-up on a development profile, so it has to be idempotent. */
    private static void createTableIsIdempotent(Dao<Note> notes) throws Exception {
        long before = notes.count();
        notes.createTable();
        check("creating an existing table keeps its rows", String.valueOf(before),
                String.valueOf(notes.count()));
    }

    /**
     * An entity with nothing but a generated key, on every engine.
     *
     * <p>The insert names no columns, and the three engines disagree about how
     * to write that: SQLite and PostgreSQL want DEFAULT VALUES and refuse the
     * empty lists, MySQL wants the empty lists and has no DEFAULT VALUES.
     */
    private static void anEntityThatIsOnlyAKey(EntityManager em) throws Exception {
        Dao<Ticket> tickets = em.dao(Ticket.class);
        tickets.dropTable();
        tickets.createTable();
        try {
            Ticket first = new Ticket();
            tickets.insert(first);
            check("a key-only entity is created", "true", String.valueOf(first.id > 0));
            Ticket second = new Ticket();
            tickets.insert(second);
            check("a key-only entity gets a new key each time", "true",
                    String.valueOf(second.id > first.id));
            check("both rows are there", "2", String.valueOf(tickets.count()));
        } finally {
            tickets.dropTable();
        }
    }

    /**
     * An INSERT written with the semicolon SQL is usually written with.
     *
     * <p>PostgreSQL is the engine that has to read its generated key back out of
     * the statement, and appending RETURNING after a terminator produces two
     * statements, the second of which is not SQL. The same call works on the
     * other two whatever the terminator, which is what makes it a portability
     * hole rather than an error everywhere.
     */
    private static void aTerminatedStatementStillAnswersItsKey(DataSource pool) throws Exception {
        pool.execute("DROP TABLE IF EXISTS cn1_terminated", null);
        pool.execute("CREATE TABLE cn1_terminated (id "
                + pool.dialect().generatedKeyColumn(com.codename1.backend.sql.Dialect.BIGINT)
                + ", name " + pool.dialect().columnType(com.codename1.backend.sql.Dialect.TEXT)
                + ")", null);
        try {
            long key = pool.insert("INSERT INTO cn1_terminated (name) VALUES (?);",
                    new Object[] {"terminated"}, "id");
            check("a statement ending in a semicolon still answers its key", "true",
                    String.valueOf(key > 0));
            check("and it wrote exactly one row", "1",
                    String.valueOf(pool.query("SELECT name FROM cn1_terminated", null).size()));
        } finally {
            pool.execute("DROP TABLE IF EXISTS cn1_terminated", null);
        }
    }

    /**
     * An upsert that UPDATES an existing row still answers, on every engine.
     *
     * <p>One row, and each engine spells the conflict clause its own way, which
     * is why this is the one check that branches. What it is really about is
     * MySQL: it reports TWO affected rows for a single-tuple ON DUPLICATE KEY
     * UPDATE that changed a row, and reading that as "rows inserted" refused an
     * ordinary upsert AFTER it had committed.
     */
    private static void anUpsertThatUpdatesStillAnswers(DataSource pool) throws Exception {
        String engine = pool.dialect().getName();
        boolean mysql = "mysql".equals(engine);
        pool.execute("DROP TABLE IF EXISTS cn1_upsert", null);
        pool.execute("CREATE TABLE cn1_upsert (id "
                + pool.dialect().generatedKeyColumn(com.codename1.backend.sql.Dialect.BIGINT)
                + ", name " + (mysql ? "VARCHAR(64)" : "TEXT")
                + " NOT NULL UNIQUE, hits INTEGER)", null);
        try {
            long first = pool.insert("INSERT INTO cn1_upsert (name, hits) VALUES (?, ?)",
                    new Object[] {"a", Long.valueOf(1)}, "id");
            check("the upsert fixture inserted", "true", String.valueOf(first > 0));
            String upsert = mysql
                    ? "INSERT INTO cn1_upsert (name, hits) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE hits = hits + 1"
                    : "INSERT INTO cn1_upsert (name, hits) VALUES (?, ?) "
                            + "ON CONFLICT (name) DO UPDATE SET hits = cn1_upsert.hits + 1";
            String answered;
            try {
                pool.insert(upsert, new Object[] {"a", Long.valueOf(1)}, "id");
                answered = "answered";
            } catch (Exception err) {
                answered = "threw: " + err.getMessage();
            }
            check("an upsert that updated a row does not report failure", "answered", answered);
            List rows = pool.query("SELECT hits FROM cn1_upsert WHERE name = ?",
                    new Object[] {"a"});
            check("and the update happened", "2",
                    String.valueOf(((java.util.Map)rows.get(0)).values().iterator().next()));
        } finally {
            pool.execute("DROP TABLE IF EXISTS cn1_upsert", null);
        }
    }

    /**
     * A multi-row insert is refused BEFORE it writes, on every engine.
     *
     * <p>The three key one differently -- SQLite reports the last, MySQL the
     * first, PostgreSQL returns a row per insert -- so there is no key insert()
     * could answer with.
     */
    private static void aMultiRowInsertIsRefused(DataSource pool) throws Exception {
        pool.execute("DROP TABLE IF EXISTS cn1_multi", null);
        pool.execute("CREATE TABLE cn1_multi (id "
                + pool.dialect().generatedKeyColumn(com.codename1.backend.sql.Dialect.BIGINT)
                + ", name " + pool.dialect().columnType(com.codename1.backend.sql.Dialect.TEXT)
                + ")", null);
        try {
            String refused;
            try {
                pool.insert("INSERT INTO cn1_multi (name) VALUES (?), (?)",
                        new Object[] {"one", "two"}, "id");
                refused = "accepted";
            } catch (Exception err) {
                String message = String.valueOf(err.getMessage());
                refused = message.indexOf("ONE generated key") >= 0
                        ? "refused" : "other: " + message;
            }
            check("a multi-row insert is refused", "refused", refused);
            check("and it wrote nothing", "0",
                    String.valueOf(pool.query("SELECT name FROM cn1_multi", null).size()));
        } finally {
            pool.execute("DROP TABLE IF EXISTS cn1_multi", null);
        }
    }

    /**
     * The switch this whole layer exists for: the same binary, a different
     * database, decided by the environment rather than by the source.
     */
    private static void configurationChoosesTheDatabase() throws Exception {
        DataSource dev = DataSource.fromConfig(Config.of(new java.util.Properties(), "dev"));
        try {
            check("a development profile with nothing configured gets SQLite", "sqlite",
                    dev.dialect().getName());
        } finally {
            dev.close();
        }
        String refused;
        try {
            DataSource.fromConfig(Config.of(new java.util.Properties(), "production")).close();
            refused = "opened";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            // The WORDING, not merely that it threw: anything failing would also
            // throw, and would pass a bare "it failed" check.
            refused = message.indexOf("No database is configured") >= 0
                    ? "refused" : "other: " + message;
        }
        check("any other profile refuses to invent one", "refused", refused);
    }

    private static Note note(String title, int views, boolean pinned, long created) {
        Note out = new Note();
        out.title = title;
        out.views = views;
        out.pinned = pinned;
        out.created = new Date(created);
        return out;
    }

    private static String join(byte[] bytes) {
        if(bytes == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < bytes.length ; iter++) {
            out.append(iter > 0 ? "," : "").append(bytes[iter]);
        }
        return out.toString();
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
