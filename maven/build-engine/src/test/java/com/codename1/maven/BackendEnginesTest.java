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
package com.codename1.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.build.BuildExecutionException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// The engine a backend build declares: what a name means, which sources are
/// left to compile, and what the JVM runtime is told.
class BackendEnginesTest {
    private static final String ENGINE_FILE = "com/codename1/backend/sql/ServerEngine.java";

    @Test void anUndeclaredEngineFailsNamingTheProperty() {
        for (String missing : new String[] {null, "", "   "}) {
            BuildExecutionException error = assertThrows(BuildExecutionException.class,
                    () -> BackendEngines.require(missing, true));
            assertTrue(error.getMessage().contains(BackendEngines.PROPERTY), error.getMessage());
            assertTrue(error.getMessage().contains("postgres"), error.getMessage());
        }
    }

    @Test void anUnknownEngineFailsNamingWhatWasWritten() {
        BuildExecutionException error = assertThrows(BuildExecutionException.class,
                () -> BackendEngines.require(" oracle ", true));
        assertTrue(error.getMessage().contains(BackendEngines.PROPERTY + "=oracle"), error.getMessage());
    }

    @Test void namesAndAliasesAnswerTheCanonicalSpelling() throws Exception {
        assertEquals("sqlite", BackendEngines.require("sqlite", true));
        assertEquals("sqlite", BackendEngines.require("none", true));
        assertEquals("postgres", BackendEngines.require("postgres", true));
        assertEquals("postgres", BackendEngines.require(" PostgreSQL ", true));
        assertEquals("mysql", BackendEngines.require("mysql", true));
        assertEquals("mysql", BackendEngines.require("MariaDB", true));
    }

    @Test void sqliteAloneWithSqliteOffIsNoDatabaseAtAll() throws Exception {
        for (String name : new String[] {"sqlite", "none"}) {
            BuildExecutionException error = assertThrows(BuildExecutionException.class,
                    () -> BackendEngines.require(name, false));
            assertTrue(error.getMessage().contains("cn1.backend.sqlite=false"), error.getMessage());
        }
        // A server engine does not need SQLite linked.
        assertEquals("postgres", BackendEngines.require("postgres", false));
        assertEquals("mysql", BackendEngines.require("mysql", false));
    }

    @Test void selectLeavesTheChosenEngineAlone(@TempDir Path dir) throws Exception {
        assertSelected(dir.resolve("a"), "postgres", "postgres");
        assertSelected(dir.resolve("b"), "mysql", "mysql");
        // SQLite alone is the engine that reaches no server.
        assertSelected(dir.resolve("c"), "sqlite", "none");
    }

    private static void assertSelected(Path sources, String engine, String directory) throws Exception {
        for (String each : new String[] {"none", "postgres", "mysql"}) {
            write(sources.resolve(BackendEngines.ENGINES_DIR).resolve(each).resolve(ENGINE_FILE), each);
        }
        write(sources.resolve(BackendEngines.ENGINES_DIR).resolve("postgres")
                .resolve("com/codename1/backend/sql/pg/Wire.java"), "wire");
        write(sources.resolve("com/codename1/backend/sql/Engines.java"), "shared");

        BackendEngines.select(sources.toFile(), engine);

        assertEquals(directory, read(sources.resolve(ENGINE_FILE)));
        assertEquals("shared", read(sources.resolve("com/codename1/backend/sql/Engines.java")));
        assertFalse(Files.exists(sources.resolve(BackendEngines.ENGINES_DIR)),
                "the other engines are several classes of one name if they stay");
        assertEquals("postgres".equals(directory),
                Files.exists(sources.resolve("com/codename1/backend/sql/pg/Wire.java")));
    }

    @Test void selectingAnEngineTheRuntimeLacksFails(@TempDir Path dir) throws Exception {
        write(dir.resolve(BackendEngines.ENGINES_DIR).resolve("none").resolve(ENGINE_FILE), "none");
        BuildExecutionException error = assertThrows(BuildExecutionException.class,
                () -> BackendEngines.select(dir.toFile(), "mysql"));
        assertTrue(error.getMessage().contains("mysql"), error.getMessage());
        // And a tree with no engines at all, as an older runtime's is.
        File empty = Files.createDirectories(dir.resolve("old")).toFile();
        assertThrows(BuildExecutionException.class, () -> BackendEngines.select(empty, "sqlite"));
    }

    @Test void declareWritesWhatTheRuntimeReads(@TempDir Path dir) throws Exception {
        // The classes directory need not exist yet.
        File classes = dir.resolve("target/classes").toFile();
        BackendEngines.declare(classes, "postgres");
        Path file = classes.toPath().resolve(BackendEngines.DECLARATION);
        assertEquals("cn1-backend.properties", BackendEngines.DECLARATION);
        assertEquals("cn1.backend.database=postgres\n", read(file));
        java.util.Properties loaded = new java.util.Properties();
        try (java.io.InputStream in = Files.newInputStream(file)) {
            loaded.load(in);
        }
        assertEquals("postgres", loaded.getProperty(BackendEngines.PROPERTY));
    }

    @Test void declareLeavesAnUnchangedFileAlone(@TempDir Path dir) throws Exception {
        BackendEngines.declare(dir.toFile(), "mysql");
        Path file = dir.resolve(BackendEngines.DECLARATION);
        // An old stamp, so that a rewrite of the same bytes would show.
        FileTime old = FileTime.fromMillis(1000000000000L);
        Files.setLastModifiedTime(file, old);
        BackendEngines.declare(dir.toFile(), "mysql");
        assertEquals(old, Files.getLastModifiedTime(file));
        assertEquals("cn1.backend.database=mysql\n", read(file));

        BackendEngines.declare(dir.toFile(), "sqlite");
        assertEquals("cn1.backend.database=sqlite\n", read(file));
    }

    private static void write(Path file, String text) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path file) throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
