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


/// The server engines a database URL can name: their schemes, canonical names
/// and default ports.
///
/// Nothing here opens a connection or decides which engines an application
/// carries -- that is whichever engine classes it names (see
/// [ServerSession.Connector]). This class knows every engine's URL scheme all
/// the same, so that a URL naming an engine is recognised as one rather than
/// opened as a SQLite file called "postgres:".
@com.codename1.impl.SharedWithBackend
public final class Engines {
    /// No server engine: SQLite alone.
    public static final String SQLITE = "sqlite";
    public static final String POSTGRES = "postgres";
    /// MySQL and MariaDB, which share a protocol and one client.
    public static final String MYSQL = "mysql";
    public static final String SQLSERVER = "sqlserver";
    public static final String ORACLE = "oracle";

    private Engines() {
    }

    /// The engine whose scheme the URL opens with, or null when it has none of
    /// them and is therefore a SQLite path.
    ///
    /// RFC 3986 makes a scheme case insensitive. regionMatches rather than
    /// toLowerCase: String.toLowerCase is locale sensitive and this runtime has
    /// no Locale to ask for the root one, so on a Turkish host the I of a
    /// scheme folds to a dotless i and stops matching.
    public static String named(String url) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        if (hasScheme(url, "postgres://") || hasScheme(url, "postgresql://")) {
            return POSTGRES;
        }
        if (hasScheme(url, "mysql://") || hasScheme(url, "mariadb://")) {
            return MYSQL;
        }
        if (hasScheme(url, "sqlserver://") || hasScheme(url, "mssql://")) {
            return SQLSERVER;
        }
        if (hasScheme(url, "oracle://")) {
            return ORACLE;
        }
        return null;
    }

    /// The port an engine listens on when the URL gives none.
    public static int defaultPort(String engine) {
        if (POSTGRES.equals(engine)) {
            return 5432;
        }
        if (MYSQL.equals(engine)) {
            return 3306;
        }
        if (SQLSERVER.equals(engine)) {
            return 1433;
        }
        return 1521;
    }

    /// The canonical name of an engine as the build property spells it, or null
    /// for a name that is none of them. "postgresql", "mariadb" and "mssql" are
    /// accepted for the engines they mean.
    public static String canonical(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (SQLITE.equalsIgnoreCase(trimmed) || "none".equalsIgnoreCase(trimmed)) {
            return SQLITE;
        }
        if (POSTGRES.equalsIgnoreCase(trimmed) || "postgresql".equalsIgnoreCase(trimmed)) {
            return POSTGRES;
        }
        if (MYSQL.equalsIgnoreCase(trimmed) || "mariadb".equalsIgnoreCase(trimmed)) {
            return MYSQL;
        }
        if (SQLSERVER.equalsIgnoreCase(trimmed) || "mssql".equalsIgnoreCase(trimmed)) {
            return SQLSERVER;
        }
        if (ORACLE.equalsIgnoreCase(trimmed)) {
            return ORACLE;
        }
        return null;
    }

    private static boolean hasScheme(String url, String scheme) {
        return url.length() >= scheme.length()
                && url.regionMatches(true, 0, scheme, 0, scheme.length());
    }
}
