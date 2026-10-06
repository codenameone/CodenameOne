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
package com.codename1.maven.processors;

import com.codename1.backend.DataSource;
import com.codename1.build.SystemStreamLog;
import com.codename1.impl.migration.MigrationChecksum;
import com.codename1.impl.migration.MigrationEntry;
import com.codename1.impl.migration.MigrationRegistry;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationSet;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// The scripts a project keeps as files reach the runtime as a compiled class. This holds the
/// three things that have to survive the trip: the text exactly, the checksum the runtime
/// would compute for it, and the registration the bootstrap performs.
public class MigrationGeneratorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @After
    public void forget() {
        MigrationRegistry.unregister("default");
    }

    private File module;
    private File classes;

    private void module(boolean backend) throws Exception {
        module = tmp.newFolder();
        classes = new File(module, "target/classes");
        assertTrue(classes.mkdirs());
        assertTrue(new File(module, backend ? MigrationGenerator.BACKEND_LOCATION
                : MigrationGenerator.CLIENT_LOCATION).mkdirs());
    }

    private void script(boolean backend, String name, String text) throws Exception {
        File file = new File(new File(module, backend ? MigrationGenerator.BACKEND_LOCATION
                : MigrationGenerator.CLIENT_LOCATION), name);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), text.getBytes("UTF-8"));
    }

    private static String location(Class<?> type) throws Exception {
        return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
    }

    private ProcessorContext run(boolean backend) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        OrmAnnotationProcessor proc = new OrmAnnotationProcessor();
        // The flavour is read off the classpath: whichever runtime's Database is on it.
        List<String> classpath = Collections.singletonList(location(backend
                ? com.codename1.backend.Database.class : com.codename1.db.Database.class));
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index, new SystemStreamLog(),
                module, null, null, Collections.<String>emptyList(), "UTF-8", classpath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        proc.finish(ctx);
        return ctx;
    }

    private static String errors(ProcessorContext ctx) {
        return ctx.getErrors().toString();
    }

    private Class<?> load(String name) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                MigrationGeneratorTest.class.getClassLoader());
        return loader.loadClass(name);
    }

    private static MigrationEntry entry(MigrationSet set, String scriptName, String dialect) {
        for (MigrationEntry e : set.entries()) {
            if (e.scriptName().equals(scriptName) && (dialect == null ? e.dialect() == null
                    : dialect.equals(e.dialect()))) {
                return e;
            }
        }
        throw new AssertionError("no entry " + dialect + "/" + scriptName);
    }

    @Test
    public void serverScriptsAreCompiledInExactlyAndRun() throws Exception {
        module(true);
        // Outside ASCII, with CRLF, and longer than one literal can hold.
        StringBuilder big = new StringBuilder("-- caf\u00e9 \u4e2d\u6587 \ud83d\ude00 \"quoted\" back\\slash\r\n");
        for (int i = 0; i < 400; i++) {
            big.append("INSERT INTO notes (id, body) VALUES (").append(i).append(", 'r\u00e9sum\u00e9 ")
                    .append(i).append(" \u4e2d');\r\n");
        }
        script(true, "V1__create_notes.sql", "CREATE TABLE notes (id INT PRIMARY KEY, body VARCHAR(100));\n");
        script(true, "V1_1__fill_notes.sql", big.toString());
        script(true, "sqlite/V2__engine.sql", "CREATE TABLE engine (name TEXT);\nINSERT INTO engine VALUES ('sqlite');");
        script(true, "postgresql/V2__engine.sql", "CREATE TABLE engine (name TEXT);\nINSERT INTO engine VALUES ('pg');");
        script(true, "mysql/V2__engine.sql", "CREATE TABLE engine (name TEXT);\nINSERT INTO engine VALUES ('my');");
        script(true, "V3__own_transaction.sql", "BEGIN;\nCREATE TABLE own (id INT);\nCOMMIT;\n");
        script(true, "V3__own_transaction.sql.conf", "executeInTransaction=false\n");
        script(true, "R__views.sql", "DROP VIEW IF EXISTS note_count;\nCREATE VIEW note_count AS SELECT COUNT(*) AS n FROM notes;");
        script(true, "README.md", "not a migration");

        ProcessorContext ctx = run(true);
        assertFalse(errors(ctx), ctx.hasErrors());
        assertTrue(new File(classes, "cn1app/BackendMigrations.class").isFile());
        assertTrue(new File(classes, "cn1app/BackendDaoBootstrap.class").isFile());
        assertFalse(new File(classes, "cn1app/ClientMigrations.class").exists());

        MigrationSet set = (MigrationSet) load("cn1app.BackendMigrations").getMethod("create").invoke(null);
        assertEquals("default", set.getName());
        assertEquals(7, set.entries().length);

        MigrationEntry fill = entry(set, "V1_1__fill_notes.sql", null);
        assertEquals("1.1", fill.version());
        assertEquals("fill notes", fill.description());
        assertEquals(big.toString(), fill.script());
        // The literal the build computed is the number the runtime computes for the same text.
        assertEquals(MigrationChecksum.of(big.toString()), fill.checksum().intValue());
        assertEquals(MigrationChecksum.of(big.toString().replace("\r\n", "\n")), fill.checksum().intValue());
        assertTrue(fill.transactional());
        assertFalse(entry(set, "V3__own_transaction.sql", null).transactional());
        assertNull(entry(set, "R__views.sql", null).version());
        assertTrue(entry(set, "V2__engine.sql", "postgresql").script().contains("'pg'"));

        DataSource pool = DataSource.open(":memory:");
        try {
            MigrateResult result = com.codename1.backend.Migrations.of(pool, set).migrate();
            assertEquals("[V1__create_notes.sql, V1_1__fill_notes.sql, V2__engine.sql, "
                    + "V3__own_transaction.sql, R__views.sql]", result.getApplied().toString());
            assertEquals(Long.valueOf(400), pool.queryOne("SELECT n FROM note_count", null).get("n"));
            assertEquals("sqlite", pool.queryOne("SELECT name FROM engine", null).get("name"));
            assertEquals("r\u00e9sum\u00e9 7 \u4e2d", pool.queryOne("SELECT body FROM notes WHERE id = 7", null)
                    .get("body"));
        } finally {
            pool.close();
        }

        // The bootstrap is what registers the set at start-up.
        assertNull(MigrationRegistry.find("default"));
        load("cn1app.BackendDaoBootstrap").getConstructor().newInstance();
        assertNotNull(MigrationRegistry.find("default"));
        assertTrue(com.codename1.backend.Migrations.isRegistered());
    }

    /// What `cn1:migrate` and its siblings launch: an entry point generated beside the scripts,
    /// so the goals apply exactly the scripts the server carries. It is a class of its own, and
    /// the class that holds the scripts has no `main`: that one is translated into the packaged
    /// server, whose translation takes exactly one entry point and stops at a second.
    @Test
    public void theGeneratedEntryPointRunsMigrationCommands() throws Exception {
        module(true);
        script(true, "V1__create_notes.sql", "CREATE TABLE notes (id INT PRIMARY KEY, body VARCHAR(100));\n"
                + "INSERT INTO notes (id, body) VALUES (1, 'one');");
        ProcessorContext ctx = run(true);
        assertFalse(errors(ctx), ctx.hasErrors());
        File database = new File(tmp.newFolder(), "cli.db");
        String[] keys = {"cn1.datasource.url", "cn1.config.location"};
        String[] before = {System.getProperty(keys[0]), System.getProperty(keys[1])};
        System.setProperty(keys[0], database.getPath());
        System.setProperty(keys[1], tmp.newFolder().getPath());
        try {
            for (java.lang.reflect.Method declared : load("cn1app.BackendMigrations").getDeclaredMethods()) {
                assertFalse("the scripts' class must not be an entry point", "main".equals(declared.getName()));
            }
            java.lang.reflect.Method main = load("cn1app.BackendMigrationsCli").getMethod("main", String[].class);
            main.invoke(null, (Object) new String[] {"info"});
            main.invoke(null, (Object) new String[] {"migrate"});
            main.invoke(null, (Object) new String[] {"validate"});
            main.invoke(null, (Object) new String[] {"repair"});
            try {
                main.invoke(null, (Object) new String[] {"upgrade"});
                org.junit.Assert.fail("an unknown command should be refused");
            } catch (java.lang.reflect.InvocationTargetException refused) {
                assertTrue(String.valueOf(refused.getCause()),
                        refused.getCause().getMessage().contains("Unknown migration command 'upgrade'"));
            }
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (before[i] == null) {
                    System.clearProperty(keys[i]);
                } else {
                    System.setProperty(keys[i], before[i]);
                }
            }
        }
        DataSource pool = DataSource.open(database.getPath());
        try {
            assertEquals("one", pool.queryOne("SELECT body FROM notes", null).get("body"));
            assertEquals(Long.valueOf(1), pool.queryOne("SELECT COUNT(*) AS n FROM flyway_schema_history", null)
                    .get("n"));
        } finally {
            pool.close();
        }
    }

    @Test
    public void applicationScriptsAreCompiledInAndRegisteredByTheDaoBootstrap() throws Exception {
        module(false);
        script(false, "V1__create_notes.sql", "CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT);");
        script(false, "V2__add_created.sql", "ALTER TABLE notes ADD COLUMN created INTEGER;");
        ProcessorContext ctx = run(false);
        assertFalse(errors(ctx), ctx.hasErrors());
        assertTrue(new File(classes, "cn1app/ClientMigrations.class").isFile());
        assertTrue(new File(classes, "cn1app/DaoBootstrap.class").isFile());
        assertFalse(new File(classes, "cn1app/BackendMigrations.class").exists());

        load("cn1app.DaoBootstrap").getConstructor().newInstance();
        MigrationSet set = MigrationRegistry.find("default");
        assertNotNull(set);
        assertTrue(com.codename1.db.Migrations.isRegistered());
        assertEquals(2, set.entries().length);
        assertEquals("ALTER TABLE notes ADD COLUMN created INTEGER;",
                entry(set, "V2__add_created.sql", null).script());
    }

    @Test
    public void aJavaMigrationIsRegisteredBesideTheScripts() throws Exception {
        module(true);
        script(true, "V1__create_notes.sql", "CREATE TABLE notes (id INT PRIMARY KEY, body VARCHAR(100));");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("mig.Seed", "package mig; import com.codename1.annotations.db.Migration; "
                + "import com.codename1.migration.*; "
                + "@Migration(version = \"2\", description = \"seed notes\") "
                + "public class Seed implements JavaMigration { "
                + "public void migrate(MigrationContext c) throws java.io.IOException { "
                + "c.execute(\"INSERT INTO notes (id, body) VALUES (1, 'seeded')\", null); } }");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(new File(location(
                com.codename1.backend.Database.class))));
        ProcessorContext ctx = run(true);
        assertFalse(errors(ctx), ctx.hasErrors());
        MigrationSet set = (MigrationSet) load("cn1app.BackendMigrations").getMethod("create").invoke(null);
        DataSource pool = DataSource.open(":memory:");
        try {
            assertEquals(2, com.codename1.backend.Migrations.of(pool, set).migrate().getMigrationsExecuted());
            assertEquals("seeded", pool.queryOne("SELECT body FROM notes", null).get("body"));
            assertEquals("JAVA", pool.queryOne("SELECT type FROM flyway_schema_history WHERE version = '2'", null)
                    .get("type"));
        } finally {
            pool.close();
        }

        // A script claiming the same version is a build error, not a run-time surprise.
        script(true, "V2__also_two.sql", "SELECT 1;");
        ProcessorContext clash = run(true);
        assertTrue(errors(clash), errors(clash).contains("Two migrations share version 2"));
        assertTrue(errors(clash), errors(clash).contains("mig.Seed"));
    }

    @Test
    public void aModuleWithNeitherEntitiesNorScriptsGetsNoBootstrap() throws Exception {
        module(true);
        ProcessorContext ctx = run(true);
        assertFalse(errors(ctx), ctx.hasErrors());
        assertFalse(new File(classes, "cn1app").exists());
    }

    private String errorsFor(boolean backend, String name, String text) throws Exception {
        module(backend);
        script(backend, "V1__ok.sql", "SELECT 1;");
        script(backend, name, text);
        ProcessorContext ctx = run(backend);
        assertTrue("expected an error for " + name, ctx.hasErrors());
        assertFalse(new File(classes, "cn1app").exists());
        return errors(ctx);
    }

    @Test
    public void whatCannotRunIsRefusedByTheBuild() throws Exception {
        String bad = errorsFor(true, "create_notes.sql", "SELECT 1;");
        assertTrue(bad, bad.contains("Not a migration file name"));
        assertTrue(bad, bad.contains("V<version>__<description>.sql"));

        String letters = errorsFor(true, "V1a__x.sql", "SELECT 1;");
        assertTrue(letters, letters.contains("must be V followed by a version"));

        String undo = errorsFor(true, "U1__drop_notes.sql", "DROP TABLE notes;");
        assertTrue(undo, undo.contains("Undo migrations are not supported"));

        // 1, 1.0 and 01 are one version at run time, so they are one version here.
        String twice = errorsFor(true, "V1_0__again.sql", "SELECT 2;");
        assertTrue(twice, twice.contains("Two migrations share version"));
        assertTrue(twice, twice.contains("V1__ok.sql"));
        assertTrue(twice, twice.contains("V1_0__again.sql"));

        String both = errorsFor(true, "postgresql/V1__ok.sql", "SELECT 3;");
        assertTrue(both, both.contains("has a script for every engine"));
        assertTrue(both, both.contains("postgresql"));

        String empty = errorsFor(true, "V2__empty.sql", "  \n\t\n");
        assertTrue(empty, empty.contains("is empty"));

        String control = errorsFor(true, "V2__nul.sql", "SELECT 'a\u0001b';");
        assertTrue(control, control.contains("control character (code 1)"));

        String oracle = errorsFor(true, "oracle/V2__x.sql", "SELECT 1 FROM dual;");
        assertTrue(oracle, oracle.contains("Only the directories sqlite, postgresql and mysql"));

        module(true);
        script(true, "V1__ok.sql", "SELECT 1;");
        script(true, "V1__ok.sql.conf", "placeholderReplacement=false\n");
        ProcessorContext conf = run(true);
        assertTrue(errors(conf), errors(conf).contains("Unsupported setting"));
        assertTrue(errors(conf), errors(conf).contains("placeholderReplacement"));
    }

    @Test
    public void anApplicationsScriptsDoNotGoUnderResources() throws Exception {
        module(false);
        File resources = new File(module, MigrationGenerator.BACKEND_LOCATION);
        assertTrue(resources.mkdirs());
        Files.write(new File(resources, "V1__x.sql").toPath(), "SELECT 1;".getBytes("UTF-8"));
        ProcessorContext ctx = run(false);
        assertTrue(errors(ctx), errors(ctx).contains("An application keeps them in src/main/db/migration"));

        String server = errorsFor(false, "postgresql/V2__x.sql", "SELECT 1;");
        assertTrue(server, server.contains("The application database is SQLite"));
    }

    @Test
    public void versionsAreOneKeyHoweverTheyAreSpelled() {
        assertEquals("1", MigrationGenerator.canonical("1"));
        assertEquals("1", MigrationGenerator.canonical("1.0"));
        assertEquals("1", MigrationGenerator.canonical("01_0"));
        assertEquals("1.0.3", MigrationGenerator.canonical("1.0.3"));
        assertEquals("2026.5.21", MigrationGenerator.canonical("2026_05_21"));
        assertTrue(MigrationGenerator.isVersion("2026.05.21.1"));
        assertFalse(MigrationGenerator.isVersion("1..2"));
        assertFalse(MigrationGenerator.isVersion("1."));
        assertFalse(MigrationGenerator.isVersion(""));
        assertFalse(MigrationGenerator.isVersion("1234567890123456789"));
        List<String> same = new ArrayList<String>();
        for (String text : new String[] {"a\nb\n", "a\r\nb\r\n", "\ufeffa\nb\n"}) {
            same.add(String.valueOf(MigrationGenerator.checksum(text)));
            assertEquals(MigrationChecksum.of(text), MigrationGenerator.checksum(text));
        }
        assertEquals(1, new java.util.HashSet<String>(same).size());
    }
}
