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

import java.io.File;

/// The class the migration commands of both build plugins launch: the `main` the
/// annotation pass generates beside a backend module's compiled scripts.
///
/// It is named here, once, because each plugin used to spell it for itself and the two
/// disagreed. The Gradle tasks launched `cn1app.BackendMigrations` -- the class that
/// holds the scripts, which has no `main` -- so every one of them stopped at start-up,
/// while the Maven goals launched the class beside it and worked. The generator, the
/// packager that keeps this class out of a translation, the Maven goals and the Gradle
/// tasks now all read the name from this class.
public final class BackendMigrateEntryPoint {
    /// The fully qualified name of the generated class with the `main`.
    public static final String CLASS_NAME = "cn1app.BackendMigrationsCli";

    private BackendMigrateEntryPoint() {
    }

    /// The compiled entry point inside a classes directory, whether or not it is there.
    ///
    /// @param classesDirectory a directory of compiled classes
    /// @return where the class file is or would be
    public static File classFile(File classesDirectory) {
        return new File(classesDirectory, CLASS_NAME.replace('.', File.separatorChar) + ".class");
    }

    /// What a migration command says for a module the class was not generated for,
    /// which is a module with nothing to migrate.
    ///
    /// @return the message, without the place that was looked in
    public static String missingMessage() {
        return "This module has no migrations to run: add V<version>__<description>.sql files "
                + "to src/main/resources/db/migration, or ask for the security tables with "
                + "cn1.security.schema.enabled=true in application.properties";
    }
}
