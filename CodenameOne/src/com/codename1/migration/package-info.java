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
/// Versioned database migrations, shared by applications and the Codename One backend.
///
/// A migration is a script named `V<version>__<description>.sql`, or a [JavaMigration]. Each
/// one runs exactly once per database, in version order, and is recorded with a checksum in a
/// history table, so a database can always be brought from whatever version it is at to the one
/// the running build expects. The file naming, the commands and the `flyway_schema_history`
/// table follow Flyway.
///
/// Start from the runtime's `Migrations` class, which hands out a [Migrator]:
/// `com.codename1.db.Migrations` in an application and `com.codename1.backend.Migrations` on a
/// server. A [MigrationSet] groups the migrations that share one history table.
package com.codename1.migration;
