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

import java.io.IOException;
import java.util.List;

/// What a [JavaMigration] may do to the database it is migrating.
@com.codename1.impl.SharedWithBackend
public interface MigrationContext {
    /// The engine being migrated.
    /// @return `sqlite`, `postgresql` or `mysql`; MariaDB answers `mysql`
    String dialect();

    /// Runs one statement.
    /// @param sql a single statement, with `?` for each parameter
    /// @param params the parameter values, or null for none
    /// @throws IOException if the database refuses it
    void execute(String sql, Object[] params) throws IOException;

    /// Runs one query and returns every row with every value as text, null where the column is
    /// null. Text is the one representation every engine and both runtimes agree on; a migration
    /// that needs typed access uses [#connection()].
    /// @param sql a single query, with `?` for each parameter
    /// @param params the parameter values, or null for none
    /// @return the rows in order
    /// @throws IOException if the database refuses it
    List<String[]> query(String sql, Object[] params) throws IOException;

    /// The runtime's own database object: `com.codename1.db.Database` in an application,
    /// `com.codename1.backend.Database` on a server. Test it with `instanceof` before casting.
    /// @return the connection the migration runs on
    Object connection();
}
