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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Migrations;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationException;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.MigrationState;
import com.codename1.testing.DatabaseConformanceSuite;

import java.io.IOException;

/**
 * Schema migrations on the device's own SQLite.
 *
 * Two things only a device can answer. That the scripts in src/main/db/migration, which the
 * build compiles into the app, were registered by the generated bootstrap on this port -- the
 * bootstrap is installed differently by every build path. And that the engine behaves the same
 * on this port's SQLite as on the simulator's: a script runs whole and once, a failed one leaves
 * nothing behind, and a database newer than the app is refused untouched.
 */
public class DatabaseMigrationTest extends DatabaseConformanceTest {

    @Override
    protected String testName() {
        return "DatabaseMigrationTest";
    }

    @Override
    protected void runGroup(int mode, final DatabaseConformanceSuite.Reporter reporter) throws Exception {
        withScratchDatabase(new DatabaseBody() {
            public void run(Database db) throws Exception {
                compiledIn(db, reporter);
                programmatic(db, reporter);
                legacyMode(db, reporter);
            }
        });
    }

    private static long number(Database db, String sql) throws IOException {
        Cursor cursor = db.executeQuery(sql);
        try {
            return cursor.next() ? cursor.getRow().getLong(0) : -1;
        } finally {
            cursor.close();
        }
    }

    private static boolean tableExists(Database db, String table) throws IOException {
        return number(db, "SELECT COUNT(*) FROM sqlite_master WHERE name = '" + table + "'") > 0;
    }

    /** The scripts the build compiled in, through the registration the bootstrap performed. */
    private void compiledIn(Database db, DatabaseConformanceSuite.Reporter r) throws IOException {
        r.check(Migrations.isRegistered(), "the generated bootstrap did not register the app's migrations");
        if (!Migrations.isRegistered()) {
            return;
        }
        MigrateResult first = Migrations.migrate(db);
        r.check(first.getMigrationsExecuted() == 3, "expected 3 migrations on a new database, ran "
                + first.getApplied());
        r.check("2".equals(first.getTargetVersion()), "schema version after migrating: "
                + first.getTargetVersion());
        r.check(number(db, "SELECT COUNT(*) FROM cn1ss_mig_note") == 2, "V1 did not run every statement");
        r.check(number(db, "SELECT COUNT(*) FROM cn1ss_mig_note WHERE body = 'semi;colon'") == 1,
                "a semicolon inside a literal split the statement");
        r.check(number(db, "SELECT total FROM cn1ss_mig_summary") == 30, "V2 or the repeatable view did not run");

        MigrateResult second = Migrations.migrate(db);
        r.check(second.getMigrationsExecuted() == 0, "a second migrate ran " + second.getApplied());
        r.check("2".equals(second.getInitialVersion()), "initial version on the second run: "
                + second.getInitialVersion());

        MigrationInfo[] info = Migrations.of(db).info();
        r.check(info.length == 3, "info rows: " + info.length);
        for (int i = 0; i < info.length; i++) {
            r.check(info[i].getState() == MigrationState.SUCCESS, info[i] + " should be applied");
            r.check(info[i].getInstalledOn() != null && info[i].getInstalledOn().length() > 0,
                    info[i] + " has no installed-on time");
        }
        r.check(number(db, "SELECT COUNT(*) FROM flyway_schema_history") == 3, "history rows");
        Migrations.of(db).validate();
    }

    private static MigrationSet upTo(int version, String lastScript) {
        MigrationSet.Builder set = MigrationSet.builder("cn1ss");
        set.sql("1", "create", "CREATE TABLE cn1ss_prog (id INTEGER PRIMARY KEY, v TEXT)");
        if (version >= 2) {
            set.sql("2", "seed", "INSERT INTO cn1ss_prog (id, v) VALUES (1, 'a'); "
                    + "INSERT INTO cn1ss_prog (id, v) VALUES (2, 'b')");
        }
        if (version >= 3) {
            set.sql("3", "third", lastScript);
        }
        return set.build();
    }

    private static int codeOf(Database db, MigrationSet set) throws IOException {
        try {
            Migrations.of(db, set).migrate();
            return 0;
        } catch (MigrationException refused) {
            return refused.getCode();
        }
    }

    /** What an upgrade, a downgrade and a broken script each do, with sets built in code. */
    private void programmatic(Database db, DatabaseConformanceSuite.Reporter r) throws IOException {
        String add = "ALTER TABLE cn1ss_prog ADD COLUMN extra INTEGER";
        r.check(Migrations.of(db, upTo(2, null)).migrate().getMigrationsExecuted() == 2, "first install");
        r.check(number(db, "SELECT COUNT(*) FROM cn1ss_prog") == 2, "seed rows after the first install");

        // A script that fails part way leaves nothing behind and records nothing.
        String broken = "CREATE TABLE cn1ss_half (id INTEGER); INSERT INTO cn1ss_no_such_table VALUES (1)";
        r.check(codeOf(db, upTo(3, broken)) == MigrationException.SCRIPT_FAILED, "a broken script should fail");
        r.check(!tableExists(db, "cn1ss_half"), "a failed migration left its table behind");
        r.check(number(db, "SELECT COUNT(*) FROM cn1_cn1ss_schema_history") == 2,
                "a failed migration was recorded");
        r.check(!db.isInTransaction(), "a failed migration left a transaction open");

        // The upgrade: only the new version runs.
        MigrateResult upgrade = Migrations.of(db, upTo(3, add)).migrate();
        r.check(upgrade.getMigrationsExecuted() == 1 && "3".equals(upgrade.getTargetVersion()),
                "upgrade ran " + upgrade.getApplied());

        // An older build over newer data is refused before it changes anything.
        r.check(codeOf(db, upTo(2, null)) == MigrationException.FUTURE_SCHEMA,
                "a database newer than the app should be refused");
        r.check(number(db, "SELECT COUNT(*) FROM cn1_cn1ss_schema_history") == 3, "the refusal changed the history");

        // An applied script that was edited afterwards is caught by its checksum.
        r.check(codeOf(db, upTo(3, add + " DEFAULT 0")) == MigrationException.VALIDATE_FAILED,
                "an edited applied script should fail validation");

        // clean() is refused unless asked for, and then leaves an empty database.
        try {
            Migrations.of(db, upTo(3, add)).clean();
            r.check(false, "clean() ran while disabled");
        } catch (MigrationException refused) {
            r.check(refused.getCode() == MigrationException.CLEAN_DISABLED, "clean refusal code");
        }
        Migrations.of(db, upTo(3, add)).cleanDisabled(false).clean();
        r.check(!tableExists(db, "cn1ss_prog") && !tableExists(db, "flyway_schema_history"),
                "clean() left tables behind");
    }

    /**
     * With the legacy database behaviour on, some ports run only the first statement of a
     * script. A migration recorded as applied after running a third of itself is the failure
     * the engine's own statement splitting exists to prevent.
     */
    private void legacyMode(Database db, DatabaseConformanceSuite.Reporter r) throws IOException {
        boolean before = Database.isLegacyBehavior();
        Database.setLegacyBehavior(true);
        try {
            r.check(Migrations.of(db, upTo(2, null)).migrate().getMigrationsExecuted() == 2,
                    "migrating in legacy mode");
            r.check(number(db, "SELECT COUNT(*) FROM cn1ss_prog") == 2,
                    "legacy mode ran only part of a migration script");
        } finally {
            Database.setLegacyBehavior(before);
        }
    }
}
