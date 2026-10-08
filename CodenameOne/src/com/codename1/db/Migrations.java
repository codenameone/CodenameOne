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
package com.codename1.db;

import com.codename1.impl.migration.DatabaseMigrationTarget;
import com.codename1.impl.migration.MigrationRegistry;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.Migrator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Versioned schema migrations for the application's SQLite database.
///
/// Put scripts named `V<version>__<description>.sql` in the project's `src/main/db/migration`
/// directory. The build compiles them into the application, and one call brings whatever
/// database the device holds up to the version this build expects:
///
/// ```java
/// Database db = Display.getInstance().openOrCreate("notes.db");
/// Migrations.migrate(db);
/// ```
///
/// Each script runs once, in version order, inside a transaction, and is recorded in the
/// `flyway_schema_history` table with a checksum. A script that has run is never edited: a
/// change to the schema is a new script with a higher version.
///
/// Call this on the thread that owns the database, before anything else uses it, and not on
/// the event dispatch thread if the scripts are large. A database that a newer build of the
/// application already migrated is refused with
/// [com.codename1.migration.MigrationException#FUTURE_SCHEMA] before anything is changed, so an
/// older build installed over newer data fails loudly instead of misreading its tables.
///
/// The same engine, scripts and history table are used by the Codename One backend.
public final class Migrations {
    private Migrations() {
    }

    /// Registers a migration set. The build registers the application's own; a library that
    /// keeps tables of its own registers one under its own name.
    /// @param set the set to register, replacing one of the same name
    public static void register(MigrationSet set) {
        if (set == null) {
            throw new IllegalArgumentException("set is null");
        }
        MigrationRegistry.register(set);
    }

    /// Whether any migration set is registered.
    /// @return true when [#migrate(Database)] has something to run
    public static boolean isRegistered() {
        return !MigrationRegistry.isEmpty();
    }

    /// A migrator for the application's own set.
    /// @param db the database, owned by the caller and used from the calling thread
    /// @return a migrator to configure and run
    /// @throws IllegalStateException if the build found no migrations and none was registered
    public static Migrator of(Database db) {
        MigrationSet set = MigrationRegistry.find(MigrationSet.DEFAULT_NAME);
        if (set == null) {
            throw new IllegalStateException("No migrations are registered. Add V<version>__<description>.sql "
                    + "files to src/main/db/migration, or register a MigrationSet.");
        }
        return of(db, set);
    }

    /// A migrator for a specific set.
    /// @param db the database, owned by the caller and used from the calling thread
    /// @param set the migrations to run
    /// @return a migrator to configure and run
    public static Migrator of(Database db, MigrationSet set) {
        return new Migrator(new DatabaseMigrationTarget(db), set).ignoreFutureMigrations(false);
    }

    /// Applies every pending migration of every registered set: library sets first, the
    /// application's own last. Does nothing when no set is registered.
    /// @param db the database, owned by the caller and used from the calling thread
    /// @return what ran; the versions are those of the application's own set
    /// @throws com.codename1.migration.MigrationException if a migration fails or the database
    ///     does not match this build
    /// @throws IOException if the database fails
    public static MigrateResult migrate(Database db) throws IOException {
        MigrationSet application = MigrationRegistry.find(MigrationSet.DEFAULT_NAME);
        if (application != null) {
            of(db, application).prepareHistory();
        }
        List<String> applied = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        String initial = null;
        String target = null;
        for (MigrationSet set : MigrationRegistry.sets()) {
            MigrateResult result = of(db, set).migrate();
            applied.addAll(result.getApplied());
            warnings.addAll(result.getWarnings());
            if (set.isDefault()) {
                initial = result.getInitialVersion();
                target = result.getTargetVersion();
            }
        }
        return new MigrateResult(initial, target, applied, warnings);
    }
}
