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

import com.codename1.impl.javase.SEDatabase;
import com.codename1.impl.migration.DatabaseMigrationTarget;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationTargetTest {
    @Test
    void registeredLibraryMigrationsPrecedeAppWithoutBlockingItsHistory() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SEDatabase db = new SEDatabase(connection);
            com.codename1.db.Migrations.register(MigrationSet.builder("library")
                    .sql("1", "library", "CREATE TABLE library_items (id INT PRIMARY KEY)").build());
            com.codename1.db.Migrations.register(MigrationSet.builder("default")
                    .sql("1", "app", "CREATE TABLE app_items (id INT REFERENCES library_items(id))").build());
            try {
                assertEquals(2, com.codename1.db.Migrations.migrate(db).getMigrationsExecuted());
                assertEquals(0, com.codename1.db.Migrations.migrate(db).getMigrationsExecuted());
            } finally {
                com.codename1.impl.migration.MigrationRegistry.unregister("default");
                com.codename1.impl.migration.MigrationRegistry.unregister("library");
            }
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void cleanDropsReferencedTablesAndRestoresForeignKeys(boolean enabled) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SEDatabase db = new SEDatabase(connection);
            DatabaseMigrationTarget target = new DatabaseMigrationTarget(db);
            db.execute("PRAGMA foreign_keys = " + (enabled ? "ON" : "OFF"));
            db.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY)");
            db.execute("CREATE TABLE child (parent_id INTEGER REFERENCES parent(id))");
            db.execute("INSERT INTO parent VALUES (1)");
            db.execute("INSERT INTO child VALUES (1)");
            new Migrator(target, MigrationSet.builder("test").build()).cleanDisabled(false).clean();
            assertEquals(enabled ? "1" : "0", target.query("PRAGMA foreign_keys", null).get(0)[0]);
            assertEquals("0", target.query("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'",
                    null).get(0)[0]);
        }
    }
    @Test
    void failedDropRestoresForeignKeys() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SEDatabase db = new SEDatabase(connection) {
                @Override
                public void execute(String sql) throws IOException {
                    if (sql.startsWith("DROP TABLE")) {
                        throw new IOException("injected drop failure");
                    }
                    super.execute(sql);
                }
            };
            DatabaseMigrationTarget target = new DatabaseMigrationTarget(db);
            db.execute("PRAGMA foreign_keys = ON");
            db.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY)");
            assertThrows(IOException.class, target::dropAllObjects);
            assertEquals("1", target.query("PRAGMA foreign_keys", null).get(0)[0]);
            assertTrue(target.tableExists("parent"));
        }
    }

    @Test
    void cleanRefusesAnActiveTransactionBeforeDroppingTables() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SEDatabase db = new SEDatabase(connection);
            DatabaseMigrationTarget target = new DatabaseMigrationTarget(db);
            db.execute("PRAGMA foreign_keys = ON");
            db.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY)");
            db.beginTransaction();
            try {
                assertThrows(IOException.class, target::dropAllObjects);
                assertTrue(target.tableExists("parent"));
                assertTrue(db.isInTransaction());
                assertEquals("1", target.query("PRAGMA foreign_keys", null).get(0)[0]);
            } finally {
                db.rollbackTransaction();
            }
        }
    }

}
