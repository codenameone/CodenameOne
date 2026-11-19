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

import com.codename1.build.BuildExecutionException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// The one database engine a backend application is built with.
///
/// The backend runtime reaches a database server through a single class,
/// `com.codename1.backend.sql.ServerEngine`, and its sources jar carries one
/// class of that name per engine under `cn1-engines/<engine>/`. A build
/// compiles exactly one, named by the `cn1.backend.database` property, so the
/// wire clients of the engines an application does not use are referenced by
/// nothing and are not in its binary.
///
/// Nothing is assumed: a backend build that declares no engine fails, here,
/// naming the property. Every path that builds a backend -- the Maven goals,
/// the Gradle tasks, the test packager -- goes through this class so that they
/// cannot disagree about what a name means.
public final class BackendEngines {
    /// The property, in a pom's `<properties>` or `gradle.properties`.
    public static final String PROPERTY = "cn1.backend.database";

    /// The resource a JVM run reads the declared engine from; it is written
    /// beside the application's classes, where the runtime looks for it.
    public static final String DECLARATION = "cn1-backend.properties";

    /// The directory of the runtime's sources jar that holds the engines.
    static final String ENGINES_DIR = "cn1-engines";

    private static final String SQLITE = "sqlite";

    /// Every engine a build can name. Keep in step with the runtime's
    /// `com.codename1.backend.sql.Engines`.
    private static final List<String> ENGINES = Collections.unmodifiableList(
            Arrays.asList(SQLITE, "postgres", "mysql", "sqlserver", "oracle"));

    private BackendEngines() {
    }

    /// The engine a build declared, in its canonical spelling.
    ///
    /// @param declared the value of [#PROPERTY], or null
    /// @param sqlite whether SQLite is linked at all (`cn1.backend.sqlite`)
    /// @throws BuildExecutionException when none is declared, when the name is
    ///         not an engine, or when the build asks for SQLite alone with
    ///         SQLite switched off
    public static String require(String declared, boolean sqlite) throws BuildExecutionException {
        if (declared == null || declared.trim().length() == 0) {
            throw new BuildExecutionException("This backend declares no database engine. "
                    + "An application is built for one, so that the clients of the others "
                    + "are not in its binary: set the property " + PROPERTY + " to one of "
                    + ENGINES + " (" + SQLITE + " means no database server at all). "
                    + "mysql covers MariaDB.");
        }
        String name = canonical(declared.trim());
        if (name == null) {
            throw new BuildExecutionException(PROPERTY + "=" + declared.trim()
                    + " names no database engine. It is one of " + ENGINES + ".");
        }
        if (SQLITE.equals(name) && !sqlite) {
            throw new BuildExecutionException(PROPERTY + "=" + SQLITE + " with "
                    + "cn1.backend.sqlite=false leaves this backend with no database "
                    + "at all. Name the server engine it talks to, or link SQLite.");
        }
        return name;
    }

    private static String canonical(String name) {
        if (name.equalsIgnoreCase(SQLITE) || name.equalsIgnoreCase("none")) {
            return SQLITE;
        }
        if (name.equalsIgnoreCase("postgres") || name.equalsIgnoreCase("postgresql")) {
            return "postgres";
        }
        if (name.equalsIgnoreCase("mysql") || name.equalsIgnoreCase("mariadb")) {
            return "mysql";
        }
        if (name.equalsIgnoreCase("sqlserver") || name.equalsIgnoreCase("mssql")) {
            return "sqlserver";
        }
        if (name.equalsIgnoreCase("oracle")) {
            return "oracle";
        }
        return null;
    }

    /// Leaves exactly one `ServerEngine` in an unpacked runtime source tree:
    /// the declared engine's moves to the root, where javac finds it under its
    /// package, and the rest are deleted. Left where they are they would be
    /// several classes of one name in one compilation.
    ///
    /// @param runtimeSources the unpacked `parparvm-sources` jar
    /// @param engine a name from [#require]
    public static void select(File runtimeSources, String engine) throws BuildExecutionException {
        File all = new File(runtimeSources, ENGINES_DIR);
        File chosen = new File(all, SQLITE.equals(engine) ? "none" : engine);
        if (!chosen.isDirectory()) {
            throw new BuildExecutionException("The backend runtime's sources carry no "
                    + engine + " engine (" + chosen + " is missing). The build plugin and "
                    + "the runtime are different versions.");
        }
        try {
            move(chosen, runtimeSources);
        } catch (IOException ex) {
            throw new BuildExecutionException("Could not select the " + engine
                    + " engine: " + ex.getMessage(), ex);
        }
        BackendPackager.emptyDirs(all);
        if (all.exists() && !all.delete()) {
            throw new BuildExecutionException("Could not delete " + all);
        }
    }

    private static void move(File from, File to) throws IOException {
        File[] children = from.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) {
                if (!target.isDirectory() && !target.mkdirs()) {
                    throw new IOException("could not create " + target);
                }
                move(child, target);
            } else {
                Files.move(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /// Writes the declared engine beside the application's classes, where the
    /// JVM runtime reads it, so a run in the IDE, `cn1:backend-run` and the
    /// module's own tests answer for the same single engine the packaged
    /// binary has compiled in.
    public static void declare(File classesDir, String engine) throws BuildExecutionException {
        File file = new File(classesDir, DECLARATION);
        try {
            if (!classesDir.isDirectory() && !classesDir.mkdirs()) {
                throw new IOException("could not create " + classesDir);
            }
            byte[] content = (PROPERTY + "=" + engine + "\n").getBytes(StandardCharsets.ISO_8859_1);
            // Unchanged content is left alone: the file is an input of every
            // incremental build after this one.
            if (file.isFile() && Arrays.equals(Files.readAllBytes(file.toPath()), content)) {
                return;
            }
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(content);
            }
        } catch (IOException ex) {
            throw new BuildExecutionException("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }
}
