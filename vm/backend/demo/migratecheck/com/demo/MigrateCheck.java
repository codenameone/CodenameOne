/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.Migrations;
import com.codename1.migration.JavaMigration;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationContext;
import com.codename1.migration.MigrationException;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.Migrator;

/**
 * Schema migrations against a REAL server, one engine per run, on whichever runtime this is
 * built for.
 *
 * The engine is one body of code, but everything under it differs by runtime and by database:
 * the CRC32 behind the checksum, the wire client that carries the history table's DDL, the
 * session lock, and whether a failed schema change can be rolled back. So this prints the
 * history it wrote, one HISTORY line per row, and the test that drives it requires the same
 * lines from the translated binary and the JVM, and from every engine.
 *
 * Point it at a database with CN1_MIGRATECHECK_URL. Without one it runs against an in-memory
 * SQLite database, which needs nothing installed.
 */
public class MigrateCheck {
    private static int passed;
    private static final List failures = new ArrayList();

    private static final String[] TABLES = {"mc_note", "mc_tag", "mc_half", "cn1_mc_lib",
        "cn1_mc_schema_history", "mc_default_history", "cn1_mclib_schema_history"};

    public static void main(String[] args) throws Exception {
        String url = System.getenv("CN1_MIGRATECHECK_URL");
        if(url == null || url.length() == 0) {
            url = ":memory:";
            note("CN1_MIGRATECHECK_URL is unset, running the SQLite arm only");
        }
        boolean memory = ":memory:".equals(url);
        DataSource pool = memory ? DataSource.open(url) : DataSource.open(url, 4, 5000, 10000);
        try {
            System.out.println("connected to " + pool);
            dropAll(pool);
            run(pool, memory);
            if(memory) {
                note("the two-process race needs a database two pools can share; skipped for :memory:");
            } else {
                dropAll(pool);
                race(pool, url);
            }
            dropAll(pool);
        } finally {
            pool.close();
        }
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "MIGRATECHECK OK" : "MIGRATECHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void dropAll(DataSource pool) throws IOException {
        pool.execute("DROP VIEW IF EXISTS mc_summary", null);
        for(int iter = 0 ; iter < TABLES.length ; iter++) {
            pool.execute("DROP TABLE IF EXISTS " + TABLES[iter], null);
        }
    }

    /** Outside ASCII on purpose: the checksum is over UTF-8 bytes, computed by each runtime's own code. */
    private static final String V1 = "-- caf\u00e9 \u4e2d\u6587\r\n"
            + "CREATE TABLE mc_note (id INT PRIMARY KEY, body VARCHAR(100));\r\n"
            + "INSERT INTO mc_note (id, body) VALUES (1, 'semi;colon');\n"
            + "INSERT INTO mc_note (id, body) VALUES (2, 'r\u00e9sum\u00e9');\n";

    /**
     * Under a name of its own for most of the run, so the history is this check's and the
     * tables other checks leave in a shared database are not its concern.
     */
    private static MigrationSet.Builder base() {
        return base("mc");
    }

    private static MigrationSet.Builder base(String name) {
        return MigrationSet.builder(name)
                .sql("1", "create note", V1)
                .sql("1.1", "create tag", "CREATE TABLE mc_tag (id INT PRIMARY KEY, name VARCHAR(40))")
                .java("2", "seed tags", new JavaMigration() {
                    public void migrate(MigrationContext context) throws IOException {
                        context.execute("INSERT INTO mc_tag (id, name) VALUES (?, ?)",
                                new Object[] { Long.valueOf(1), context.dialect() });
                    }
                })
                .repeatable("summary", "DROP VIEW IF EXISTS mc_summary;\n"
                        + "CREATE VIEW mc_summary AS SELECT COUNT(*) AS n FROM mc_note");
    }

    private static void run(DataSource pool, boolean memory) throws Exception {
        boolean mysql = "mysql".equals(pool.dialect().getName());

        MigrateResult first = Migrations.of(pool, base().build()).migrate();
        check("a new database gets every migration", "4", String.valueOf(first.getMigrationsExecuted()));
        check("the schema version after migrating", "2", String.valueOf(first.getTargetVersion()));
        check("every statement of a script ran", "2", count(pool, "mc_note"));
        check("a semicolon inside a literal does not split",
                "semi;colon", String.valueOf(pool.queryOne("SELECT body FROM mc_note WHERE id = 1", null).get("body")));
        check("text outside ASCII survives the script", "r\u00e9sum\u00e9",
                String.valueOf(pool.queryOne("SELECT body FROM mc_note WHERE id = 2", null).get("body")));
        check("the Java migration saw the engine", pool.dialect().getName(),
                String.valueOf(pool.queryOne("SELECT name FROM mc_tag WHERE id = 1", null).get("name")));
        check("the repeatable migration ran last", "2", String.valueOf(number(pool.queryOne(
                "SELECT n FROM mc_summary", null).get("n"))));

        // The lines the two runtimes and the three engines have to agree on.
        MigrationInfo[] info = Migrations.of(pool, base().build()).info();
        for(int iter = 0 ; iter < info.length ; iter++) {
            System.out.println("HISTORY " + info[iter].getInstalledRank() + "|" + info[iter].getVersion()
                    + "|" + info[iter].getDescription() + "|" + info[iter].getType() + "|" + info[iter].getScript()
                    + "|" + info[iter].getChecksum() + "|" + info[iter].getState());
        }
        check("the history has a row per migration", "4", String.valueOf(info.length));
        check("every row has a time", "true", String.valueOf(info[0].getInstalledOn() != null
                && info[0].getInstalledOn().startsWith("20")));

        MigrateResult second = Migrations.of(pool, base().build()).migrate();
        check("a second run applies nothing", "0", String.valueOf(second.getMigrationsExecuted()));
        check("a second run starts from the migrated version", "2", String.valueOf(second.getInitialVersion()));
        check("validate accepts a migrated schema", "ok", codeOf(Migrations.of(pool, base().build()), "validate"));

        // An applied script edited afterwards.
        MigrationSet edited = MigrationSet.builder("mc")
                .sql("1", "create note", V1 + "-- edited\n")
                .sql("1.1", "create tag", "CREATE TABLE mc_tag (id INT PRIMARY KEY, name VARCHAR(40))")
                .build();
        check("an edited applied script is refused",
                String.valueOf(MigrationException.VALIDATE_FAILED), codeOf(Migrations.of(pool, edited), "migrate"));

        // A script that fails part way.
        MigrationSet broken = base().sql("3", "half", "CREATE TABLE mc_half (id INT PRIMARY KEY);\n"
                + "INSERT INTO mc_no_such_table (id) VALUES (1)").build();
        check("a broken script fails the migration",
                String.valueOf(MigrationException.SCRIPT_FAILED), codeOf(Migrations.of(pool, broken), "migrate"));
        if(mysql) {
            // MySQL committed the CREATE TABLE and cannot take it back.
            check("mysql keeps what the failed script created", "true", String.valueOf(exists(pool, "mc_half")));
            check("mysql records the failure", "5", count(pool, "cn1_mc_schema_history"));
            check("a recorded failure blocks the next run", String.valueOf(
                    MigrationException.FAILED_MIGRATION_PRESENT), codeOf(Migrations.of(pool, broken), "migrate"));
            pool.execute("DROP TABLE mc_half", null);
            check("repair clears the failure", "ok", codeOf(Migrations.of(pool, broken), "repair"));
        } else {
            check("a failed script leaves nothing behind", "false", String.valueOf(exists(pool, "mc_half")));
        }
        check("the history holds only what succeeded", "4", count(pool, "cn1_mc_schema_history"));

        MigrationSet fixed = base().sql("3", "half", "CREATE TABLE mc_half (id INT PRIMARY KEY)").build();
        check("the corrected script runs", "1",
                String.valueOf(Migrations.of(pool, fixed).migrate().getMigrationsExecuted()));

        // The older build against the newer schema.
        check("a newer schema is refused when asked to", String.valueOf(MigrationException.FUTURE_SCHEMA),
                codeOf(Migrations.of(pool, base().build()).ignoreFutureMigrations(false), "migrate"));
        check("a newer schema is tolerated by default", "ok",
                codeOf(Migrations.of(pool, base().build()), "migrate"));

        // A library's set, beside the application's.
        MigrationSet library = MigrationSet.builder("mclib")
                .sql("1", "lib table", "CREATE TABLE cn1_mc_lib (id INT PRIMARY KEY)").build();
        check("a library set migrates into its own history", "1",
                String.valueOf(Migrations.of(pool, library).migrate().getMigrationsExecuted()));
        check("the library history is its own table", "1", count(pool, "cn1_mclib_schema_history"));

        // Adopting a schema that has tables and no history: the application's own set, which
        // is the one that asks whether the database is somebody else's.
        String half = "CREATE TABLE mc_half (id INT PRIMARY KEY)";
        check("tables without a history are refused", String.valueOf(MigrationException.NON_EMPTY_SCHEMA),
                codeOf(Migrations.of(pool, base("default").sql("3", "half", half).build())
                        .table("mc_default_history"), "migrate"));
        check("the refusal created no history", "false", String.valueOf(exists(pool, "mc_default_history")));
        MigrateResult adopted = Migrations.of(pool, base("default").sql("3", "half", half).build())
                .table("mc_default_history").baselineOnMigrate(true).baselineVersion("3").migrate();
        check("a baseline adopts it", "1", String.valueOf(adopted.getMigrationsExecuted()));
        check("only the repeatable ran above the baseline", "[R__summary.sql]", String.valueOf(adopted.getApplied()));

        check("clean is refused unless enabled", String.valueOf(MigrationException.CLEAN_DISABLED),
                codeOf(Migrations.of(pool, fixed), "clean"));
        if(memory) {
            // Only where the database is this run's alone: clean drops EVERY table, and a
            // server the other database checks share is not this one's to empty.
            check("clean runs when enabled", "ok", codeOf(Migrations.of(pool, fixed).cleanDisabled(false), "clean"));
            check("clean drops the application tables", "false", String.valueOf(exists(pool, "mc_note")));
            check("clean drops the history", "false", String.valueOf(exists(pool, "cn1_mc_schema_history")));
        }
    }

    /** Two pools are two processes as far as the database can tell. */
    private static void race(final DataSource first, String url) throws Exception {
        final DataSource second = DataSource.open(url, 4, 5000, 10000);
        try {
            final MigrationSet.Builder many = MigrationSet.builder("mc");
            for(int iter = 1 ; iter <= 10 ; iter++) {
                many.sql(String.valueOf(iter), "step " + iter, iter == 1
                        ? "CREATE TABLE mc_note (id INT PRIMARY KEY, body VARCHAR(100))"
                        : "INSERT INTO mc_note (id, body) VALUES (" + iter + ", 'x')");
            }
            final MigrationSet set = many.build();
            final int[] ran = new int[2];
            final String[] errors = new String[2];
            Thread[] threads = new Thread[2];
            for(int t = 0 ; t < 2 ; t++) {
                final int index = t;
                threads[t] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            ran[index] = Migrations.of(index == 0 ? first : second, set).migrate()
                                    .getMigrationsExecuted();
                        } catch (Throwable err) {
                            errors[index] = String.valueOf(err);
                        }
                    }
                });
                threads[t].start();
            }
            for(int t = 0 ; t < 2 ; t++) {
                threads[t].join();
            }
            check("neither racing migrator failed", "null/null", errors[0] + "/" + errors[1]);
            check("the two together applied each migration once", "10", String.valueOf(ran[0] + ran[1]));
            check("the history has one row per migration", "10", count(first, "cn1_mc_schema_history"));
            check("no insert ran twice", "9", count(first, "mc_note"));
        } finally {
            second.close();
        }
    }

    private static String codeOf(Migrator migrator, String command) {
        try {
            if("validate".equals(command)) {
                migrator.validate();
            } else if("repair".equals(command)) {
                migrator.repair();
            } else if("clean".equals(command)) {
                migrator.clean();
            } else {
                migrator.migrate();
            }
            return "ok";
        } catch (MigrationException refused) {
            return String.valueOf(refused.getCode());
        } catch (IOException err) {
            return "error: " + err.getMessage();
        }
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number)value).longValue() : -1;
    }

    private static String count(DataSource pool, String table) throws IOException {
        Map row = pool.queryOne("SELECT COUNT(*) AS n FROM " + table, null);
        return String.valueOf(number(row.get("n")));
    }

    private static boolean exists(DataSource pool, String table) {
        try {
            count(pool, table);
            return true;
        } catch (IOException missing) {
            return false;
        }
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
