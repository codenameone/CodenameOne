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

import com.codename1.impl.migration.BackendMigrationTarget;
import com.codename1.impl.migration.MigrationRegistry;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.Migrator;
import java.io.IOException;

/// Versioned schema migrations for the server's database.
///
/// Put scripts named `V<version>__<description>.sql` in `src/main/resources/db/migration`.
/// The build compiles them into the server, and the server applies whatever has not run yet
/// before it opens the entity manager or accepts a request. A script for one engine only goes
/// in a `sqlite`, `postgresql` or `mysql` subdirectory.
///
/// Each script runs once, in version order, and is recorded with a checksum in the
/// `flyway_schema_history` table -- the table, the file naming and the commands are Flyway's,
/// so a schema already managed by Flyway carries straight over. Several server processes
/// starting at once against one database take a lock, so each migration still runs exactly
/// once.
///
/// The settings mirror Spring Boot's `spring.flyway.*` under `cn1.flyway.*`; see [Config].
/// This class is for the cases start-up does not cover: running a migration by hand, reading
/// the state of the schema, or migrating a database the server was not configured with.
///
/// ```java
/// MigrationInfo[] state = Migrations.of(pool).info();
/// Migrations.of(pool).repair();
/// ```
///
/// On MySQL and MariaDB a schema change commits as it runs and cannot be rolled back. A
/// script that fails there is recorded as failed and every later start refuses to migrate
/// until [Migrator#repair()] has been called.
public final class Migrations {
    private Migrations() {
    }

    /// Registers a migration set. The build registers the application's own; a library that
    /// keeps tables of its own registers one under its own name, and it then runs before the
    /// application's.
    /// @param set the set to register, replacing one of the same name
    public static void register(MigrationSet set) {
        if (set == null) {
            throw new IllegalArgumentException("set is null");
        }
        MigrationRegistry.register(set);
    }

    /// Whether any migration set is registered.
    /// @return true when the server has migrations to run at start-up
    public static boolean isRegistered() {
        return !MigrationRegistry.isEmpty();
    }

    /// A migrator for the application's own set.
    /// @param pool the database
    /// @return a migrator to configure and run
    /// @throws IllegalStateException if the build found no migrations and none was registered
    public static Migrator of(DataSource pool) {
        return of(pool, defaultSet());
    }

    /// A migrator for a specific set.
    /// @param pool the database
    /// @param set the migrations to run
    /// @return a migrator to configure and run
    public static Migrator of(DataSource pool, MigrationSet set) {
        return new Migrator(new BackendMigrationTarget(pool), set);
    }

    /// A migrator for the application's own set over one connection the caller owns.
    /// @param db the connection
    /// @return a migrator to configure and run
    /// @throws IllegalStateException if the build found no migrations and none was registered
    public static Migrator of(Database db) {
        return of(db, defaultSet());
    }

    /// A migrator for a specific set over one connection the caller owns.
    /// @param db the connection
    /// @param set the migrations to run
    /// @return a migrator to configure and run
    public static Migrator of(Database db, MigrationSet set) {
        return new Migrator(new BackendMigrationTarget(db), set);
    }

    private static MigrationSet defaultSet() {
        MigrationSet set = MigrationRegistry.find(MigrationSet.DEFAULT_NAME);
        if (set == null) {
            throw new IllegalStateException("No migrations are registered. Add V<version>__<description>.sql "
                    + "files to src/main/resources/db/migration, or register a MigrationSet.");
        }
        return set;
    }

    /// Applies the settings a deployment configured to a migrator. `cn1.flyway.table` names
    /// the application's own history table only: a library's set keeps its own.
    /// @param migrator the migrator to configure
    /// @param config the configuration to read `cn1.flyway.*` from
    /// @return the same migrator
    /// @throws IOException if a setting cannot be read
    public static Migrator configure(Migrator migrator, Config config) throws IOException {
        String table = config.get(Config.FLYWAY_TABLE, null);
        if (table != null && table.length() > 0 && migrator.getSet().isDefault()) {
            migrator.table(table);
        }
        migrator.baselineOnMigrate(config.getBoolean(Config.FLYWAY_BASELINE_ON_MIGRATE, false));
        String baseline = config.get(Config.FLYWAY_BASELINE_VERSION, null);
        if (baseline != null && baseline.length() > 0) {
            migrator.baselineVersion(baseline);
        }
        String description = config.get(Config.FLYWAY_BASELINE_DESCRIPTION, null);
        if (description != null && description.length() > 0) {
            migrator.baselineDescription(description);
        }
        migrator.validateOnMigrate(config.getBoolean(Config.FLYWAY_VALIDATE_ON_MIGRATE, true));
        migrator.outOfOrder(config.getBoolean(Config.FLYWAY_OUT_OF_ORDER, false));
        migrator.cleanDisabled(config.getBoolean(Config.FLYWAY_CLEAN_DISABLED, true));
        String target = config.get(Config.FLYWAY_TARGET, null);
        if (target != null && target.length() > 0 && !"latest".equals(target)) {
            migrator.target(target);
        }
        migrator.ignoreFutureMigrations(config.getBoolean(Config.FLYWAY_IGNORE_FUTURE_MIGRATIONS, true));
        String installedBy = config.get(Config.FLYWAY_INSTALLED_BY, null);
        if (installedBy != null && installedBy.length() > 0) {
            migrator.installedBy(installedBy);
        }
        migrator.lockRetryCount(config.getInt(Config.FLYWAY_LOCK_RETRY_COUNT, 50));
        return migrator;
    }

    /// Applies every pending migration of every registered set, as start-up does: library
    /// sets first, the application's own last, each configured from `cn1.flyway.*`.
    /// @param pool the database
    /// @param config the configuration
    /// @return the number of migrations that ran
    /// @throws com.codename1.migration.MigrationException if a migration fails or the schema
    ///     does not match this build
    /// @throws IOException if the database fails
    public static int migrate(DataSource pool, Config config) throws IOException {
        int ran = 0;
        for (MigrationSet set : MigrationRegistry.sets()) {
            MigrateResult result = configure(of(pool, set), config).migrate();
            ran += result.getMigrationsExecuted();
            for (String warning : result.getWarnings()) {
                System.out.println("cn1: migrations (" + set.getName() + "): " + warning);
            }
            if (result.getMigrationsExecuted() > 0) {
                System.out.println("cn1: migrations (" + set.getName() + "): applied "
                        + result.getMigrationsExecuted() + ", schema now at version "
                        + (result.getTargetVersion() == null ? "none" : result.getTargetVersion()));
            }
        }
        return ran;
    }
}
