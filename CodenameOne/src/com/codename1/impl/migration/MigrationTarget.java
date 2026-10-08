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
package com.codename1.impl.migration;

import java.io.IOException;
import java.util.List;

/// The database a migration run is applied to, as the shared engine sees it. An application
/// database and a server connection each implement this once; everything else is one engine.
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public interface MigrationTarget {
    /// The engine: `sqlite`, `postgresql` or `mysql`.
    String dialect();

    /// Splits a script into the statements this engine would run, using the engine's own
    /// lexical rules. Never returns an empty element.
    String[] split(String script) throws IOException;

    /// Runs one statement; params may be null.
    void execute(String sql, Object[] params) throws IOException;

    /// Runs one query; every value comes back as text, null for a null column.
    List<String[]> query(String sql, Object[] params) throws IOException;

    /// Whether a table of exactly this name exists.
    boolean tableExists(String table) throws IOException;

    /// Whether the database holds tables an application created. Engine objects and tables the
    /// framework owns (named `cn1_...`, and every history table) do not count.
    boolean hasUserObjects() throws IOException;

    /// Whether a schema change rolls back with its transaction. False on MySQL and MariaDB,
    /// where every DDL statement commits.
    boolean supportsDdlTransactions();

    /// Begins a transaction on the migration connection.
    void begin() throws IOException;

    /// Commits it.
    void commit() throws IOException;

    /// Rolls it back.
    void rollback() throws IOException;

    /// Whether a transaction is open, including one a script opened for itself.
    boolean isTransactionActive();

    /// Takes the cross-process migration lock, waiting up to roughly the given number of
    /// seconds. Answers false on a timeout. An engine that needs no lock answers true.
    boolean lock(String name, int waitSeconds) throws IOException;

    /// Releases the lock [#lock(String, int)] took.
    void unlock(String name) throws IOException;

    /// The name recorded as having applied a migration.
    String currentUser() throws IOException;

    /// Drops every table and view, framework-owned ones included.
    void dropAllObjects() throws IOException;

    /// The runtime's own database object, handed to Java migrations.
    Object connection();

    /// Called once when a command is over, however it ended, so a target that borrowed a
    /// connection for the run can hand it back.
    void done();
}
