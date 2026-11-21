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
package com.codename1.db;

import com.codename1.db.sql.SqlServer;

import java.io.IOException;

/// A SQL Server server, over its wire protocol with no driver. See [ServerDatabase].
public class SqlServerDatabase extends ServerDatabase {
    /// The port SQL Server listens on unless configured otherwise.
    public static final int DEFAULT_PORT = 1433;

    /// Connects without TLS where the server allows it (`sslMode=prefer`), with a
    /// 30 second limit on connecting and signing in.
    public SqlServerDatabase(String host, int port, String database, String user, String password)
            throws IOException {
        this(host, port, database, user, password, null, 30000);
    }

    /// Connects and signs in; see [ServerDatabase] for `sslMode`.
    public SqlServerDatabase(String host, int port, String database, String user, String password,
                        String sslMode, int timeoutMillis) throws IOException {
        super(SqlServer.CONNECTOR, host, port, database, user, password, sslMode, timeoutMillis);
    }
}
