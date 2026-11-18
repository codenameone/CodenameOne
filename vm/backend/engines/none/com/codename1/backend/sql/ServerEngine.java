/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.sql;

import com.codename1.backend.annotations.Isolation;

import java.io.IOException;
import java.util.List;

/// The one server engine this application was built with.
///
/// There is a class of this name per engine, each in a source tree of its own
/// under `vm/backend/engines`, and a build compiles exactly one of them --
/// the one `cn1.backend.database` names. [com.codename1.backend.Database]
/// holds this class and no other, so the wire client of an engine the
/// application does not use is referenced by nothing and is not in the binary.
///
/// It is a class and not an interface on purpose. Every call below is a direct
/// call on a final class into another final class; there is no dispatch to pay
/// for on the statement path, and nothing to implement twice.
///
/// This is the one for an application with no server engine: SQLite alone.
public final class ServerEngine {
    private ServerEngine() {
    }

    /// The engine compiled in, as [Engines] names it.
    public static String built() {
        return Engines.SQLITE;
    }

    /// Connects. `engine` is the name the URL gave, already checked against
    /// [#built].
    public static ServerEngine connect(String engine, String host, int port, String database,
                                       String user, String password, String sslMode,
                                       String caFile, int timeoutMillis,
                                       int socketTimeoutMillis) throws IOException {
        throw new IOException("This application was built for SQLite alone");
    }

    public Dialect dialect() {
        return Dialect.SQLITE;
    }

    public int execute(String sql, Object[] params) throws IOException {
        throw new IOException("No server engine");
    }

    public List query(String sql, Object[] params) throws IOException {
        throw new IOException("No server engine");
    }

    public void close() {
        // nothing was ever opened
    }

    public boolean isClosed() {
        return true;
    }

    /// Whether transaction control goes through the methods below rather than
    /// as ordinary statements.
    public boolean ownsTransactionControl() {
        return false;
    }

    public void begin() throws IOException {
        throw new IOException("No server engine");
    }

    public void beginReadOnly() throws IOException {
        throw new IOException("No server engine");
    }

    public void begin(boolean readOnly, Isolation isolation) throws IOException {
        throw new IOException("No server engine");
    }

    public void commit() throws IOException {
        throw new IOException("No server engine");
    }

    public void rollback() throws IOException {
        throw new IOException("No server engine");
    }

    public void savepoint(String verb, String name) throws IOException {
        throw new IOException("No server engine");
    }

    public long lastInsertId() {
        return 0;
    }
}
