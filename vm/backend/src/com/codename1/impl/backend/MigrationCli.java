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
package com.codename1.impl.backend;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.Migrations;
import com.codename1.impl.migration.MigrationRegistry;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.Migrator;
import java.io.IOException;
import java.util.List;

/// The migration commands behind the build's `migrate` goals: the same engine start-up
/// uses, run by hand against the configured database.
///
/// Reached from the `main` the build generates into `cn1app.BackendMigrations`, which
/// registers the project's scripts first. Nothing here is looked up by name, so a server that
/// never runs a command does not carry this class.
///
/// Internal; not an application API.
/// @hidden
public final class MigrationCli {
    private MigrationCli() {
    }

    /// Runs one command -- `migrate`, `info`, `validate`, `repair` or `baseline` -- for every
    /// registered migration set, reading the database and the `cn1.flyway` settings from the
    /// process configuration.
    public static void run(String[] args) throws IOException {
        String command = args == null || args.length == 0 ? "migrate" : args[0];
        boolean known = "migrate".equals(command) || "info".equals(command) || "validate".equals(command)
                || "repair".equals(command) || "baseline".equals(command);
        if (!known) {
            throw new IOException("Unknown migration command '" + command
                    + "'; expected migrate, info, validate, repair or baseline");
        }
        MigrationSet[] sets = MigrationRegistry.sets();
        if (sets.length == 0) {
            throw new IOException("No migrations are registered");
        }
        Config config = Config.load();
        DataSource pool = DataSource.fromConfig(config);
        try {
            System.out.println("cn1: " + command + " on " + pool);
            for (MigrationSet set : sets) {
                run(command, set, Migrations.configure(Migrations.of(pool, set), config));
            }
        } finally {
            pool.close();
        }
    }

    private static void run(String command, MigrationSet set, Migrator migrator) throws IOException {
        String label = "cn1: " + set.getName() + ": ";
        if ("migrate".equals(command)) {
            MigrateResult result = migrator.migrate();
            for (String warning : result.getWarnings()) {
                System.out.println(label + warning);
            }
            List<String> applied = result.getApplied();
            for (String script : applied) {
                System.out.println(label + "applied " + script);
            }
            System.out.println(label + (applied.isEmpty() ? "nothing to apply" : applied.size() + " applied")
                    + ", schema at version " + (result.getTargetVersion() == null ? "none"
                    : result.getTargetVersion()));
        } else if ("info".equals(command)) {
            MigrationInfo[] info = migrator.info();
            System.out.println(label + info.length + " migration(s) in " + set.getTable());
            System.out.println(row("Version", "Description", "Type", "State", "Installed on"));
            for (MigrationInfo migration : info) {
                System.out.println(row(migration.getVersion() == null ? "" : migration.getVersion(),
                        migration.getDescription(), migration.getType(), migration.getState().toString(),
                        migration.getInstalledOn() == null ? "" : migration.getInstalledOn()));
            }
        } else if ("validate".equals(command)) {
            migrator.validate();
            System.out.println(label + "the schema history matches this build's migrations");
        } else if ("repair".equals(command)) {
            migrator.repair();
            System.out.println(label + "failed rows removed and checksums realigned");
        } else {
            migrator.baseline();
            System.out.println(label + "baseline recorded");
        }
    }

    private static String row(String version, String description, String type, String state, String on) {
        StringBuilder line = new StringBuilder(96);
        pad(line, version, 16);
        pad(line, description, 36);
        pad(line, type, 9);
        pad(line, state, 15);
        line.append(on);
        return line.toString();
    }

    private static void pad(StringBuilder line, String text, int width) {
        line.append(text);
        for (int i = text.length(); i < width - 1; i++) {
            line.append(' ');
        }
        line.append(' ');
    }
}
