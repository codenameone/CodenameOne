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
package com.codename1.backend;

import com.codename1.backend.sql.Dialect;
import com.codename1.impl.migration.MigrationRegistry;
import com.codename1.migration.JavaMigration;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationContext;
import com.codename1.migration.MigrationException;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.MigrationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared migration engine against real databases, through the server's target.
 *
 * SQLITE always runs, on a file in a temporary directory. POSTGRES and MYSQL run when
 * CN1_TX_POSTGRES / CN1_TX_MYSQL name a database, the same variables the transaction tests read:
 * what differs between the engines is exactly what this is about -- the history table's DDL,
 * the session lock, and whether a failed schema change can be rolled back.
 */
class MigrationsTest {
    @TempDir
    File dir;

    private final List<DataSource> opened = new ArrayList<DataSource>();

    @AfterEach
    void closeAll() {
        for (DataSource pool : opened) {
            pool.close();
        }
        MigrationRegistry.unregister("default");
        MigrationRegistry.unregister("library");
    }

    private static final String[] TABLES = {"mig_notes", "mig_tags", "mig_half", "cn1_mig_lib", "mig_existing",
        "flyway_schema_history", "cn1_library_schema_history", "mig_history"};

    private DataSource open(String engine) throws IOException {
        DataSource pool;
        if ("SQLITE".equals(engine)) {
            pool = DataSource.open(new File(dir, "mig-" + opened.size() + ".db").getPath(), 4, 5000, 10000);
        } else {
            String url = System.getenv("CN1_TX_" + engine);
            Assumptions.assumeTrue(url != null && url.length() > 0,
                    "CN1_TX_" + engine + " is unset; this engine is not exercised");
            pool = DataSource.open(url, 4, 5000, 10000);
            for (String table : TABLES) {
                pool.execute("DROP TABLE IF EXISTS " + table, null);
            }
        }
        opened.add(pool);
        return pool;
    }

    private static MigrationSet.Builder notes() {
        return MigrationSet.builder("default")
                .sql("1", "create notes", "-- the first table\n"
                        + "CREATE TABLE mig_notes (id INT PRIMARY KEY, body VARCHAR(100));\n"
                        + "INSERT INTO mig_notes (id, body) VALUES (1, 'semi;colon');\n")
                .sql("2", "create tags", "CREATE TABLE mig_tags (id INT PRIMARY KEY, name VARCHAR(40))");
    }

    private static long count(DataSource pool, String table) throws IOException {
        Map row = pool.queryOne("SELECT COUNT(*) AS n FROM " + table, null);
        return ((Number) row.get("n")).longValue();
    }

    private static boolean exists(DataSource pool, String table) {
        try {
            count(pool, table);
            return true;
        } catch (IOException missing) {
            return false;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void migratesRecordsAndIsThenANoOp(String engine) throws Exception {
        DataSource pool = open(engine);
        MigrateResult result = Migrations.of(pool, notes().build()).migrate();
        assertEquals(2, result.getMigrationsExecuted());
        assertEquals("2", result.getTargetVersion());
        assertEquals(1, count(pool, "mig_notes"));
        assertEquals(0, count(pool, "mig_tags"));
        assertEquals("semi;colon", pool.queryOne("SELECT body FROM mig_notes", null).get("body"));

        MigrationInfo[] info = Migrations.of(pool, notes().build()).info();
        assertEquals(2, info.length);
        assertEquals(MigrationState.SUCCESS, info[0].getState());
        assertEquals("1", info[0].getVersion());
        assertEquals("create notes", info[0].getDescription());
        assertEquals("V1__create_notes.sql", info[0].getScript());
        assertEquals(1, info[0].getInstalledRank());
        assertEquals(2, info[1].getInstalledRank());
        assertNotNull(info[0].getInstalledOn());
        assertTrue(info[0].getInstalledOn().startsWith("20"), info[0].getInstalledOn());
        assertNotNull(info[0].getChecksum());

        // The history is Flyway's table, readable by name.
        Map row = pool.queryOne("SELECT version, type, installed_by FROM flyway_schema_history "
                + "WHERE installed_rank = 2", null);
        assertEquals("2", row.get("version"));
        assertEquals("SQL", row.get("type"));
        assertNotNull(row.get("installed_by"));

        MigrateResult again = Migrations.of(pool, notes().build()).migrate();
        assertEquals(0, again.getMigrationsExecuted());
        assertEquals("2", again.getInitialVersion());
        assertEquals(2, count(pool, "flyway_schema_history"));
        Migrations.of(pool, notes().build()).validate();

        // Nothing is left borrowed: the pool still lends every connection it has.
        List<Database> all = new ArrayList<Database>();
        for (int i = 0; i < 4; i++) {
            all.add(pool.borrow());
        }
        for (Database db : all) {
            pool.release(db);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES"})
    void aFailedScriptIsUndoneWhereSchemaChangesRollBack(String engine) throws Exception {
        DataSource pool = open(engine);
        MigrationSet set = notes().sql("3", "half", "CREATE TABLE mig_half (id INT PRIMARY KEY);\n"
                + "INSERT INTO mig_no_such_table (id) VALUES (1)").build();
        MigrationException failure = assertThrows(MigrationException.class, Migrations.of(pool, set)::migrate);
        assertEquals(MigrationException.SCRIPT_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("V3__half.sql"), failure.getMessage());
        assertTrue(failure.getMessage().contains("rolled back"), failure.getMessage());
        assertFalse(exists(pool, "mig_half"));
        assertEquals(2, count(pool, "flyway_schema_history"));
        // The connection went back usable, and the corrected script runs.
        MigrationSet fixed = notes().sql("3", "half", "CREATE TABLE mig_half (id INT PRIMARY KEY)").build();
        assertEquals(1, Migrations.of(pool, fixed).migrate().getMigrationsExecuted());
        assertTrue(exists(pool, "mig_half"));
    }

    @Test
    void aFailedScriptIsRecordedWhereSchemaChangesCommit() throws Exception {
        DataSource pool = open("MYSQL");
        MigrationSet set = notes().sql("3", "half", "CREATE TABLE mig_half (id INT PRIMARY KEY);\n"
                + "INSERT INTO mig_no_such_table (id) VALUES (1)").build();
        MigrationException failure = assertThrows(MigrationException.class, Migrations.of(pool, set)::migrate);
        assertTrue(failure.getMessage().contains("recorded as failed"), failure.getMessage());
        // MySQL committed the CREATE TABLE; the history says the migration did not finish.
        assertTrue(exists(pool, "mig_half"));
        assertEquals(MigrationState.FAILED, Migrations.of(pool, set).info()[2].getState());
        MigrationException refused = assertThrows(MigrationException.class, Migrations.of(pool, set)::migrate);
        assertEquals(MigrationException.FAILED_MIGRATION_PRESENT, refused.getCode());
        pool.execute("DROP TABLE mig_half", null);
        Migrations.of(pool, set).repair();
        MigrationSet fixed = notes().sql("3", "half", "CREATE TABLE mig_half (id INT PRIMARY KEY)").build();
        assertEquals(1, Migrations.of(pool, fixed).migrate().getMigrationsExecuted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void processesStartingTogetherApplyEachMigrationOnce(String engine) throws Exception {
        final DataSource first = open(engine);
        // A second pool is a second process as far as the database can tell.
        final DataSource second;
        if ("SQLITE".equals(engine)) {
            second = DataSource.open(new File(dir, "mig-0.db").getPath(), 4, 5000, 10000);
        } else {
            second = DataSource.open(System.getenv("CN1_TX_" + engine), 4, 5000, 10000);
        }
        opened.add(second);
        final MigrationSet.Builder many = MigrationSet.builder("default");
        for (int i = 1; i <= 12; i++) {
            many.sql(String.valueOf(i), "step " + i, i == 1
                    ? "CREATE TABLE mig_notes (id INT PRIMARY KEY, body VARCHAR(100))"
                    : "INSERT INTO mig_notes (id, body) VALUES (" + i + ", 'x')");
        }
        // A repeatable migration has no version to be found by: the second process to get
        // the write lock has to find that the first ran it, or this row is inserted twice.
        many.repeatable("seed", "INSERT INTO mig_notes (id, body) VALUES (100, 'seed')");
        final MigrationSet set = many.build();
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());
        final int[] ran = new int[2];
        final CountDownLatch go = new CountDownLatch(1);
        Thread[] threads = new Thread[2];
        for (int t = 0; t < 2; t++) {
            final int index = t;
            threads[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        go.await();
                        ran[index] = Migrations.of(index == 0 ? first : second, set).migrate()
                                .getMigrationsExecuted();
                    } catch (Throwable failure) {
                        failures.add(failure);
                    }
                }
            });
            threads[t].start();
        }
        go.countDown();
        for (Thread thread : threads) {
            thread.join(60000);
        }
        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(13, ran[0] + ran[1]);
        assertEquals(13, count(first, "flyway_schema_history"));
        assertEquals(12, count(first, "mig_notes"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void anExistingSchemaIsRefusedThenAdoptedAtTheBaseline(String engine) throws Exception {
        DataSource pool = open(engine);
        // Framework tables do not make a database "somebody's schema".
        pool.execute("CREATE TABLE cn1_library_schema_history (x INT)", null);
        assertEquals(2, Migrations.of(pool, notes().build()).migrate().getMigrationsExecuted());
        pool.execute("DROP TABLE flyway_schema_history", null);

        MigrationSet set = notes().sql("3", "half", "CREATE TABLE mig_half (id INT PRIMARY KEY)").build();
        MigrationException refused = assertThrows(MigrationException.class, Migrations.of(pool, set)::migrate);
        assertEquals(MigrationException.NON_EMPTY_SCHEMA, refused.getCode());
        assertFalse(exists(pool, "flyway_schema_history"));

        MigrateResult adopted = Migrations.of(pool, set).baselineOnMigrate(true).baselineVersion("2").migrate();
        assertEquals("[V3__half.sql]", adopted.getApplied().toString());
        MigrationInfo[] info = Migrations.of(pool, set).info();
        assertEquals(MigrationState.BASELINE, info[0].getState());
        assertNull(info[0].getChecksum());
        assertEquals(MigrationState.SUCCESS, info[1].getState());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void librarySetsKeepTheirOwnHistoryAndJavaMigrationsRun(String engine) throws Exception {
        DataSource pool = open(engine);
        final String[] seen = new String[1];
        MigrationSet library = MigrationSet.builder("library")
                .sql("1", "lib table", "CREATE TABLE cn1_mig_lib (id INT PRIMARY KEY, v VARCHAR(20))")
                .java("2", "seed", new JavaMigration() {
                    @Override
                    public void migrate(MigrationContext context) throws IOException {
                        seen[0] = context.dialect();
                        assertTrue(context.connection() instanceof Database);
                        context.execute("INSERT INTO cn1_mig_lib (id, v) VALUES (?, ?)",
                                new Object[] {Long.valueOf(7), "seven"});
                        List<String[]> rows = context.query("SELECT v FROM cn1_mig_lib WHERE id = ?",
                                new Object[] {Long.valueOf(7)});
                        assertEquals("seven", rows.get(0)[0]);
                    }
                })
                .build();
        assertEquals(2, Migrations.of(pool, library).migrate().getMigrationsExecuted());
        assertEquals(pool.dialect().getName(), seen[0]);
        assertEquals(2, count(pool, "cn1_library_schema_history"));
        // The application's own set starts from nothing beside it: a library's tables are
        // named cn1_..., and those do not make the schema somebody else's.
        assertEquals(2, Migrations.of(pool, notes().build()).migrate().getMigrationsExecuted());
        assertEquals(2, count(pool, "flyway_schema_history"));
        assertEquals("JAVA", pool.queryOne("SELECT type FROM cn1_library_schema_history "
                + "WHERE installed_rank = 2", null).get("type"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SQLITE", "POSTGRES", "MYSQL"})
    void cleanDropsEverythingOnlyWhenEnabled(String engine) throws Exception {
        DataSource pool = open(engine);
        Migrations.of(pool, notes().build()).migrate();
        MigrationException refused = assertThrows(MigrationException.class,
                Migrations.of(pool, notes().build())::clean);
        assertEquals(MigrationException.CLEAN_DISABLED, refused.getCode());
        assertTrue(exists(pool, "mig_notes"));
        Migrations.of(pool, notes().build()).cleanDisabled(false).clean();
        assertFalse(exists(pool, "mig_notes"));
        assertFalse(exists(pool, "flyway_schema_history"));
        assertEquals(2, Migrations.of(pool, notes().build()).migrate().getMigrationsExecuted());
    }

    @Test
    void aSqliteRebuildScriptManagesItsOwnTransaction() throws Exception {
        DataSource pool = open("SQLITE");
        MigrationSet set = notes()
                .sql("3", "rebuild", "PRAGMA foreign_keys = OFF;\n"
                        + "BEGIN;\n"
                        + "CREATE TABLE mig_half (id INT PRIMARY KEY, body TEXT);\n"
                        + "INSERT INTO mig_half SELECT id, body FROM mig_notes;\n"
                        + "DROP TABLE mig_notes;\n"
                        + "ALTER TABLE mig_half RENAME TO mig_notes;\n"
                        + "COMMIT;\n"
                        + "PRAGMA foreign_keys = ON;")
                .outsideTransaction()
                .build();
        assertEquals(3, Migrations.of(pool, set).migrate().getMigrationsExecuted());
        assertEquals(1, count(pool, "mig_notes"));
        assertFalse(exists(pool, "mig_half"));

        // The same script without the marker is refused before it runs anything.
        DataSource other = open("SQLITE");
        MigrationSet unmarked = MigrationSet.builder("default").sql("1", "x", "BEGIN; CREATE TABLE mig_half "
                + "(id INT); COMMIT").build();
        MigrationException refused = assertThrows(MigrationException.class, Migrations.of(other, unmarked)::migrate);
        assertTrue(refused.getMessage().contains("opens or ends a transaction itself"), refused.getMessage());
        assertFalse(exists(other, "mig_half"));
    }

    @Test
    void scriptsAreCutByEachEnginesOwnLexicalRules() throws Exception {
        assertEquals(Arrays.asList("CREATE TABLE a (x INT)", "INSERT INTO a VALUES ('x;y')"),
                Arrays.asList(Dialect.SQLITE.splitStatements(
                        "CREATE TABLE a (x INT); -- trailing; comment\n/* block; */ INSERT INTO a VALUES ('x;y');;\n")));
        // A trigger body is one statement, a CASE inside it included.
        String trigger = "CREATE TRIGGER t AFTER INSERT ON a BEGIN UPDATE a SET x = CASE WHEN x > 0 THEN 1 "
                + "ELSE 0 END; DELETE FROM a WHERE x = 9; END";
        assertEquals(Arrays.asList(trigger, "SELECT 1"),
                Arrays.asList(Dialect.SQLITE.splitStatements(trigger + ";\nSELECT 1")));
        // PostgreSQL: a dollar-quoted body keeps its semicolons.
        String function = "CREATE FUNCTION f() RETURNS int AS $body$ BEGIN RETURN 1; END; $body$ LANGUAGE plpgsql";
        assertEquals(Arrays.asList(function, "SELECT f()"),
                Arrays.asList(Dialect.POSTGRES.splitStatements(function + ";\nSELECT f();")));
        // MySQL: a backslash escapes the quote, and # starts a comment.
        assertEquals(Arrays.asList("INSERT INTO a VALUES ('it\\'s; here')", "SELECT 2"),
                Arrays.asList(Dialect.MYSQL.splitStatements(
                        "INSERT INTO a VALUES ('it\\'s; here'); # note; more\nSELECT 2")));
        assertEquals(0, Dialect.SQLITE.splitStatements("  -- nothing here\n ; ;").length);
        assertThrows(IOException.class, () -> Dialect.SQLITE.splitStatements("SELECT 'never closed"));
    }

    private static final HttpServer.Handler NOTHING = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return null;
        }
    };

    private static Config config(String... keysAndValues) {
        Properties settings = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            settings.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return Config.of(settings, "prod");
    }

    @Test
    void startUpMigratesBeforeAnythingElseAndAFailureFailsTheStart() throws Exception {
        DataSource pool = open("SQLITE");
        Migrations.register(notes().build());
        Backend backend = Backend.builder(config()).quiet().port(0).handler(NOTHING).dataSource(pool).start();
        try {
            assertEquals(1, count(pool, "mig_notes"));
            assertEquals(2, count(pool, "flyway_schema_history"));
        } finally {
            backend.stop();
        }

        // Switched off, by the builder and by configuration. Asked while the server is up:
        // stopping it closes the pool, and a closed pool would answer "no such table" too.
        DataSource skipped = open("SQLITE");
        backend = Backend.builder(config()).quiet().port(0).handler(NOTHING).dataSource(skipped).migrations(false)
                .start();
        try {
            assertEquals(0, count(skipped, "sqlite_master"));
        } finally {
            backend.stop();
        }
        DataSource disabled = open("SQLITE");
        backend = Backend.builder(config(Config.FLYWAY_ENABLED, "false")).quiet().port(0).handler(NOTHING)
                .dataSource(disabled).start();
        try {
            assertEquals(0, count(disabled, "sqlite_master"));
        } finally {
            backend.stop();
        }

        // The configured table name and target apply to the application's set.
        DataSource renamed = open("SQLITE");
        backend = Backend.builder(config(Config.FLYWAY_TABLE, "mig_history", Config.FLYWAY_TARGET, "1"))
                .quiet().port(0).handler(NOTHING).dataSource(renamed).start();
        try {
            assertEquals(1, count(renamed, "mig_history"));
            assertEquals(1, count(renamed, "mig_notes"));
            assertEquals(0, count(renamed, "sqlite_master WHERE name = 'mig_tags'"));
        } finally {
            backend.stop();
        }

        // A migration that cannot run stops the server from coming up at all.
        DataSource broken = open("SQLITE");
        Migrations.register(notes().sql("3", "bad", "INSERT INTO mig_no_such_table VALUES (1)").build());
        MigrationException failure = assertThrows(MigrationException.class,
                () -> Backend.builder(config()).quiet().port(0).handler(NOTHING).dataSource(broken).start());
        assertEquals(MigrationException.SCRIPT_FAILED, failure.getCode());
    }

    @Test
    void aServerWithOnlyMigrationsStillOpensItsConfiguredDatabase() throws Exception {
        Migrations.register(notes().build());
        String path = new File(dir, "configured.db").getPath();
        Backend backend = Backend.builder(config(Config.DATASOURCE_URL, path)).quiet().port(0).handler(NOTHING).start();
        backend.stop();
        DataSource check = DataSource.open(path);
        opened.add(check);
        assertEquals(1, count(check, "mig_notes"));
    }
}
