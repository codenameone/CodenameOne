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
package com.codenameone.developerguide.snippets;

import com.codename1.db.Database;
import com.codename1.db.Migrations;
import com.codename1.migration.MigrationException;
import com.codename1.migration.MigrationSet;
import com.codename1.orm.EntityManager;
import com.codename1.ui.Display;
import java.io.IOException;

public class MigrationSnippets {

    public Database open() throws IOException {
        // tag::migrations-client-migrate[]
        Database db = Display.getInstance().openOrCreate("notes.db");
        Migrations.migrate(db);
        // end::migrations-client-migrate[]
        return db;
    }

    public EntityManager openManager() throws IOException {
        // tag::migrations-client-orm[]
        EntityManager manager = EntityManager.open("notes.db");
        // end::migrations-client-orm[]
        return manager;
    }

    public Database openOrExplain() throws IOException {
        // tag::migrations-client-future[]
        Database db = Display.getInstance().openOrCreate("notes.db");
        try {
            Migrations.migrate(db);
        } catch (MigrationException refused) {
            if (refused.getCode() == MigrationException.FUTURE_SCHEMA) {
                db.close();
                // the data was written by a newer version of the app: ask the user to update
                return null;
            }
            throw refused;
        }
        // end::migrations-client-future[]
        return db;
    }

    public void adopt(Database db) throws IOException {
        // tag::migrations-client-baseline[]
        Migrations.of(db).baselineOnMigrate(true).baselineVersion("3").migrate();
        // end::migrations-client-baseline[]
    }

    public void programmatic(Database db) throws IOException {
        // tag::migrations-client-programmatic[]
        MigrationSet set = MigrationSet.builder("default")
                .sql("1", "create note", "CREATE TABLE note (id INTEGER PRIMARY KEY, body TEXT)")
                .sql("2", "add note created", "ALTER TABLE note ADD COLUMN created INTEGER")
                .build();
        Migrations.of(db, set).migrate();
        // end::migrations-client-programmatic[]
    }
}
