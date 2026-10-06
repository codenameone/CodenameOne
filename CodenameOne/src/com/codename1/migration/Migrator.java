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
package com.codename1.migration;

import com.codename1.impl.migration.HistoryTable;
import com.codename1.impl.migration.MigrationEngine;
import com.codename1.impl.migration.MigrationTarget;
import java.io.IOException;

/// Runs one [MigrationSet] against one database.
///
/// Obtain one from the runtime's `Migrations` class: `com.codename1.db.Migrations.of(database)`
/// in an application, `com.codename1.backend.Migrations.of(dataSource)` on a server. The
/// commands and settings are Flyway's, and the history table is Flyway's, so the same scripts
/// and the same history work under either.
///
/// ```java
/// MigrateResult result = Migrations.of(database).baselineOnMigrate(true).migrate();
/// ```
///
/// A migrator holds no connection state between calls and is not thread safe.
@com.codename1.impl.SharedWithBackend
public final class Migrator {
    private final MigrationTarget target;
    private final MigrationSet set;
    private final MigrationEngine.Options options = new MigrationEngine.Options();

    /// Binds a set to a database. Called by the runtime's `Migrations` class.
    /// @param target the database
    /// @param set the migrations
    /// @hidden
    public Migrator(MigrationTarget target, MigrationSet set) {
        if (target == null || set == null) {
            throw new IllegalArgumentException("A migrator needs a database and a migration set");
        }
        this.target = target;
        this.set = set;
    }

    /// The set this migrator runs.
    /// @return the migration set
    public MigrationSet getSet() {
        return set;
    }

    /// Uses a history table other than the set's own.
    /// @param table a plain identifier
    /// @return this migrator
    public Migrator table(String table) {
        HistoryTable.checkName(table);
        options.table = table;
        return this;
    }

    /// Adopts a database that already has tables and no history, by recording a baseline before
    /// migrating instead of refusing. Off by default: silently adopting a schema of unknown
    /// shape is how a migration runs against the wrong database.
    /// @param value true to baseline on the first migrate
    /// @return this migrator
    public Migrator baselineOnMigrate(boolean value) {
        options.baselineOnMigrate = value;
        return this;
    }

    /// The version an adopted database is taken to be at; migrations at or below it never run.
    /// @param version the baseline version, `1` by default
    /// @return this migrator
    public Migrator baselineVersion(String version) {
        options.baselineVersion = version;
        return this;
    }

    /// The description written on the baseline row.
    /// @param description the description
    /// @return this migrator
    public Migrator baselineDescription(String description) {
        options.baselineDescription = description;
        return this;
    }

    /// Whether [#migrate()] first checks that every applied migration is still present and
    /// unchanged. On by default.
    /// @param value false to skip the check
    /// @return this migrator
    public Migrator validateOnMigrate(boolean value) {
        options.validateOnMigrate = value;
        return this;
    }

    /// Whether a migration older than the newest applied one is applied instead of refused.
    /// @param value true to allow it
    /// @return this migrator
    public Migrator outOfOrder(boolean value) {
        options.outOfOrder = value;
        return this;
    }

    /// Whether [#clean()] is refused. On by default.
    /// @param value false to allow clean
    /// @return this migrator
    public Migrator cleanDisabled(boolean value) {
        options.cleanDisabled = value;
        return this;
    }

    /// The highest version to apply.
    /// @param version the version, or null for the newest
    /// @return this migrator
    public Migrator target(String version) {
        options.target = version;
        return this;
    }

    /// Whether a database migrated by a newer build is tolerated. A server tolerates it, because
    /// a rolling deployment runs old and new builds against one database. An application
    /// refuses it by default, because the build on the device cannot know what a newer one did
    /// to its tables.
    /// @param value true to continue with a warning, false to throw
    /// @return this migrator
    public Migrator ignoreFutureMigrations(boolean value) {
        options.ignoreFutureMigrations = value;
        return this;
    }

    /// The name recorded in the history as having applied each migration.
    /// @param name the name, or null for the runtime's default
    /// @return this migrator
    public Migrator installedBy(String name) {
        options.installedBy = name;
        return this;
    }

    /// How long to wait for another process that is migrating the same database, in roughly
    /// one-second attempts.
    /// @param attempts the number of attempts, 50 by default
    /// @return this migrator
    public Migrator lockRetryCount(int attempts) {
        options.lockRetryCount = attempts;
        return this;
    }

    private MigrationEngine engine() {
        return new MigrationEngine(target, set, options);
    }

    /// Applies every migration that has not run yet, in version order, then every repeatable
    /// migration whose script changed.
    /// @return what ran
    /// @throws MigrationException if the history does not match, a script fails, or the
    ///     database is not in a state to migrate; the code says which
    /// @throws IOException if the database fails outside a migration
    public MigrateResult migrate() throws IOException {
        return engine().migrate();
    }

    /// Reports every migration, applied or pending, without changing anything.
    /// @return applied migrations in the order they ran, then the ones that have not
    /// @throws IOException if the history cannot be read
    public MigrationInfo[] info() throws IOException {
        return engine().info();
    }

    /// Throws unless the database is exactly at this build's migrations: nothing pending,
    /// nothing edited, nothing missing.
    /// @throws MigrationException on any difference
    /// @throws IOException if the history cannot be read
    public void validate() throws IOException {
        engine().validate();
    }

    /// Marks an existing database as already being at the baseline version.
    /// @throws IOException if the history already records migrations or cannot be written
    public void baseline() throws IOException {
        engine().baseline();
    }

    /// Deletes the rows of failed migrations and rewrites recorded checksums to match the
    /// scripts in this build. Run it after cleaning up what a failed migration left behind, or
    /// after deliberately editing an applied script.
    /// @throws IOException if the history cannot be written
    public void repair() throws IOException {
        engine().repair();
    }

    /// Drops every table and view in the database, the history included.
    /// @throws MigrationException unless clean was enabled with [#cleanDisabled(boolean)]
    /// @throws IOException if the database refuses
    public void clean() throws IOException {
        engine().clean();
    }
}
