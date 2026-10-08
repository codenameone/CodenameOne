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

/// A migration written in Java, for a change SQL cannot express: recomputing a column, moving
/// data between tables with logic in between.
///
/// It runs once, in version order with the SQL migrations, inside the same transaction a script
/// would get. It has no checksum, so editing the class after it ran is not detected.
@com.codename1.impl.SharedWithBackend
public interface JavaMigration {
    /// Applies the change.
    /// @param context the connection the migration runs on
    /// @throws IOException if the database refuses a statement; the migration is then rolled back
    ///     where the engine can roll schema changes back, and recorded as failed where it cannot
    void migrate(MigrationContext context) throws IOException;
}
