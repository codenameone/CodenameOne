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
package com.codenameone.developerguide.backend;

import com.codename1.backend.DataSource;
import com.codename1.backend.Migrations;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationSet;
import java.io.IOException;

/** The Backend chapter's migration examples, compiled so they cannot drift. */
public final class MigrationSnippets {

    private MigrationSnippets() {
    }

    public static void inspect(DataSource pool) throws IOException {
// tag::backend-migrations-info[]
for (MigrationInfo migration : Migrations.of(pool).info()) {
    System.out.println(migration.getVersion() + " " + migration.getDescription()
            + " " + migration.getState());
}
// end::backend-migrations-info[]
    }

    public static void repair(DataSource pool) throws IOException {
// tag::backend-migrations-repair[]
Migrations.of(pool).repair();
Migrations.of(pool).migrate();
// end::backend-migrations-repair[]
    }

    public static void library() {
// tag::backend-migrations-library[]
Migrations.register(MigrationSet.builder("audit")
        .sql("1", "create audit log",
             "CREATE TABLE cn1_audit_log (id BIGINT PRIMARY KEY, entry VARCHAR(2000))")
        .sql("2", "index audit log", "postgresql",
             "CREATE INDEX cn1_audit_entry ON cn1_audit_log USING hash (entry)")
        .sql("2", "index audit log", "mysql",
             "CREATE INDEX cn1_audit_entry ON cn1_audit_log (entry(191))")
        .sql("2", "index audit log", "sqlite",
             "CREATE INDEX cn1_audit_entry ON cn1_audit_log (entry)")
        .build());
// end::backend-migrations-library[]
    }
}
