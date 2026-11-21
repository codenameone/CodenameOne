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
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/// The one server engine this application was built with, on a JVM.
///
/// A packaged application compiles in a single engine's class of this name and
/// nothing of the others. The JVM runtime is one prebuilt jar and cannot be
/// narrowed that way, so this one carries every wire client -- and still
/// answers for ONE engine, the one the build declared, so that a program which
/// runs here is a program that runs packaged. A URL naming any other engine is
/// refused exactly as the packaged binary refuses it.
///
/// The declared engine is read once: from the system property
/// `cn1.backend.database`, and otherwise from the `cn1-backend.properties`
/// resource the build writes beside the application's classes.
public final class ServerEngine {
    private static final String BUILT = declared();

    private final Postgres postgres;
    private final MySql mysql;
    private final SqlServer sqlServer;
    private Oracle oracle;

    private ServerEngine(Postgres postgres, MySql mysql, SqlServer sqlServer) {
        this.postgres = postgres;
        this.mysql = mysql;
        this.sqlServer = sqlServer;
    }

    /// The engine the build declared, as [Engines] names it, or null when it
    /// declared none.
    public static String built() {
        return BUILT;
    }

    private static String declared() {
        String name = System.getProperty("cn1.backend.database");
        if (name == null || name.trim().length() == 0) {
            name = null;
            try (InputStream in = ServerEngine.class.getResourceAsStream("/cn1-backend.properties")) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    name = props.getProperty("cn1.backend.database");
                }
            } catch (IOException ignored) {
                // An unreadable resource is an undeclared engine, which a server
                // URL then reports by name.
                name = null;
            }
        }
        return Engines.canonical(name);
    }

    /// Connects. `engine` is the name the URL gave, already checked against
    /// [#built].
    public static ServerEngine connect(String engine, String host, int port, String database,
                                       String user, String password, String sslMode,
                                       String caFile, int timeoutMillis,
                                       int socketTimeoutMillis) throws IOException {
        if (Engines.POSTGRES.equals(engine)) {
            return new ServerEngine(Postgres.connect(host, port, database, user, password,
                    sslMode, caFile, timeoutMillis, socketTimeoutMillis), null, null);
        }
        if (Engines.MYSQL.equals(engine)) {
            return new ServerEngine(null, MySql.connect(host, port, database, user, password,
                    sslMode, caFile, timeoutMillis, socketTimeoutMillis), null);
        }
        if (Engines.SQLSERVER.equals(engine)) {
            return new ServerEngine(null, null, SqlServer.connect(host, port, database, user,
                    password, sslMode, caFile, timeoutMillis, socketTimeoutMillis));
        }
        if (Engines.ORACLE.equals(engine)) {
            ServerEngine session = new ServerEngine(null, null, null);
            session.oracle = Oracle.connect(host, port, database, user, password, sslMode,
                    caFile, timeoutMillis, socketTimeoutMillis);
            return session;
        }
        throw new IOException("No client for " + engine);
    }

    public Dialect dialect() {
        if (oracle != null) {
            return Dialect.ORACLE;
        }
        if (postgres != null) {
            return Dialect.POSTGRES;
        }
        if (sqlServer != null) {
            return Dialect.SQLSERVER;
        }
        return mysql.isMariaDb() ? Dialect.MARIADB : Dialect.MYSQL;
    }

    public int execute(String sql, Object[] params) throws IOException {
        if (oracle != null) {
            return oracle.execute(sql, params);
        }
        if (sqlServer != null) {
            return sqlServer.execute(sql, params);
        }
        return postgres != null ? postgres.execute(sql, params) : mysql.execute(sql, params);
    }

    public List query(String sql, Object[] params) throws IOException {
        if (oracle != null) {
            return oracle.query(sql, params);
        }
        if (sqlServer != null) {
            return sqlServer.query(sql, params);
        }
        return postgres != null ? postgres.query(sql, params) : mysql.query(sql, params);
    }

    public void close() {
        if (oracle != null) {
            oracle.close();
            return;
        }
        if (sqlServer != null) {
            sqlServer.close();
        } else if (postgres != null) {
            postgres.close();
        } else {
            mysql.close();
        }
    }

    public boolean isClosed() {
        if (oracle != null) {
            return oracle.isClosed();
        }
        if (sqlServer != null) {
            return sqlServer.isClosed();
        }
        return postgres != null ? postgres.isClosed() : mysql.isClosed();
    }

    /// Whether transaction control goes through the methods below rather than
    /// as ordinary statements; see the packaged engines' classes.
    public boolean ownsTransactionControl() {
        return postgres == null;
    }

    public void begin() throws IOException {
        if (oracle != null) {
            oracle.begin();
            return;
        }
        if (sqlServer != null) {
            sqlServer.begin();
        } else if (postgres != null) {
            postgres.begin();
        } else {
            mysql.begin();
        }
    }

    public void beginReadOnly() throws IOException {
        if (oracle != null) {
            oracle.beginReadOnly();
            return;
        }
        if (sqlServer != null) {
            sqlServer.beginReadOnly();
        } else if (postgres != null) {
            postgres.beginReadOnly();
        } else {
            mysql.beginReadOnly();
        }
    }

    public void begin(boolean readOnly, Isolation isolation) throws IOException {
        if (oracle != null) {
            oracle.begin(readOnly, isolation);
            return;
        }
        if (sqlServer != null) {
            sqlServer.begin(readOnly, isolation);
        } else if (postgres != null) {
            postgres.begin(readOnly, isolation);
        } else {
            mysql.begin(readOnly, isolation);
        }
    }

    public void commit() throws IOException {
        if (oracle != null) {
            oracle.commit();
            return;
        }
        if (sqlServer != null) {
            sqlServer.commit();
        } else if (postgres != null) {
            postgres.commit();
        } else {
            mysql.commit();
        }
    }

    public void rollback() throws IOException {
        if (oracle != null) {
            oracle.rollback();
            return;
        }
        if (sqlServer != null) {
            sqlServer.rollback();
        } else if (postgres != null) {
            postgres.rollback();
        } else {
            mysql.rollback();
        }
    }

    public void savepoint(String verb, String name) throws IOException {
        if (oracle != null) {
            oracle.savepoint(verb, name);
            return;
        }
        if (sqlServer != null) {
            sqlServer.savepoint(verb, name);
        } else if (postgres != null) {
            postgres.savepoint(verb, name);
        } else {
            mysql.savepoint(verb, name);
        }
    }

    public long lastInsertId() {
        if (oracle != null) {
            return oracle.lastInsertId();
        }
        if (sqlServer != null) {
            return sqlServer.lastInsertId();
        }
        return postgres != null ? 0 : mysql.lastInsertId();
    }
}
