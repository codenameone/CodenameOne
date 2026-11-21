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
package com.codename1.db.sql;

import java.io.IOException;
import java.util.List;

/// An open session with a database server: [Postgres], [MySql], [SqlServer] or
/// [Oracle].
///
/// This is a base class, not a driver interface. Nothing looks an engine up by
/// name: code that wants one names its class -- `Postgres.connect(...)`, or its
/// [#CONNECTOR] field -- and an engine nothing names is stripped from the
/// application.
@com.codename1.impl.SharedWithBackend
public abstract class ServerSession {
    /// Opens sessions for one engine. Each engine class exposes its own as a
    /// `CONNECTOR` constant; referring to that constant is what puts the engine
    /// in an application.
    public abstract static class Connector {
        /// The engine's canonical name, as [Engines] spells it.
        public abstract String engine();

        /// Connects and signs in. `sslMode` is `disable`, `prefer`, `require`,
        /// `verify-ca`, `verify-full` or (SQL Server) `strict`; `caFile` the trust
        /// root, or null for the platform's.
        public abstract ServerSession connect(String host, int port, String database,
                                              String user, String password, String sslMode,
                                              String caFile, int timeoutMillis,
                                              int socketTimeoutMillis) throws IOException;
    }

    /// The SQL this session's server speaks.
    public abstract Dialect dialect();

    /// Runs a statement that returns no rows.
    ///
    /// #### Returns
    ///
    /// the number of rows it changed
    public abstract int execute(String sql, Object[] params) throws IOException;

    /// Runs a query: a list of `Map` rows keyed by column label, in column order.
    public abstract List query(String sql, Object[] params) throws IOException;

    /// True when beginning, committing and rolling back are protocol requests or
    /// session state of this client's own, rather than statements a caller could
    /// equally run.
    public abstract boolean ownsTransactionControl();

    /// Begins a read-write transaction at the server's default isolation.
    public abstract void begin() throws IOException;

    /// Begins a read-only transaction at the server's default isolation.
    public abstract void beginReadOnly() throws IOException;

    /// Begins a transaction.
    public abstract void begin(boolean readOnly, TransactionIsolation isolation)
            throws IOException;

    /// Commits the transaction.
    public abstract void commit() throws IOException;

    /// Rolls the transaction back.
    public abstract void rollback() throws IOException;

    /// Runs a savepoint statement: `verb` is the dialect's word for set, roll
    /// back to or release, and `name` the savepoint.
    public abstract void savepoint(String verb, String name) throws IOException;

    /// The key the last INSERT generated, for an engine that reports one that
    /// way; 0 otherwise.
    public abstract long lastInsertId();

    /// Closes the session; closing twice does nothing.
    public abstract void close();

    /// True once the session is closed or its connection was lost.
    public abstract boolean isClosed();
}
