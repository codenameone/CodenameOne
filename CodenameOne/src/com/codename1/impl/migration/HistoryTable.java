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
import java.util.ArrayList;
import java.util.List;

/// The schema history table: its DDL and the few statements that read and write it.
///
/// The columns are Flyway's, so a history either tool wrote can be read by the other. The
/// statements are literal per engine instead of going through a type mapper because the two
/// columns that matter differ in ways a mapper hides: a timestamp with a server default, and a
/// boolean that older SQLite builds have no keyword for.
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class HistoryTable {
    /// One history row.
    public static final class Row {
        /// The order the row was written in.
        public int rank;
        /// The version, or null for a repeatable migration.
        public String version;
        /// The description; never null.
        public String description = "";
        /// `SQL`, `JDBC` (Java), legacy `JAVA`, or `BASELINE`.
        public String type;
        /// The script or class name.
        public String script;
        /// The checksum, or null.
        public Integer checksum;
        /// When it was applied, as the database prints it.
        public String installedOn;
        /// Milliseconds it took.
        public int executionTime;
        /// False for a migration that failed where it could not be rolled back.
        public boolean success;
    }

    private final MigrationTarget target;
    private final String table;
    private final String quoted;
    private final boolean mysql;
    private final boolean postgres;

    /// Binds a table name to a target.
    public HistoryTable(MigrationTarget target, String table) {
        checkName(table);
        this.target = target;
        this.table = table;
        String dialect = target.dialect();
        mysql = "mysql".equals(dialect);
        postgres = "postgresql".equals(dialect);
        quoted = quote(table);
    }

    /// Refuses a table name that is not a plain identifier. The name is spliced into DDL, and a
    /// plain identifier needs no escaping under any engine's quoting rules. At most 53
    /// characters leaves room for MySQL's `cn1_flyway_` lock prefix (64 characters)
    /// and the `_s_idx` suffix within PostgreSQL's 63-character identifier limit.
    public static void checkName(String table) {
        if (table == null || table.length() == 0 || table.length() > 53) {
            throw new IllegalArgumentException("Invalid schema history table name: " + table);
        }
        for (int i = 0; i < table.length(); i++) {
            char c = table.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || i > 0 && c >= '0' && c <= '9';
            if (!ok) {
                throw new IllegalArgumentException("Invalid schema history table name: " + table);
            }
        }
    }

    /// Whether a table belongs to the framework or to a migration history, not to the
    /// application: such a table does not make a database "non-empty".
    public static boolean isFrameworkOwned(String name) {
        return name.startsWith("cn1_") || name.startsWith("sqlite_") || name.startsWith("pg_")
                || name.endsWith("_schema_history") || "android_metadata".equals(name);
    }

    private String quote(String name) {
        return mysql ? "`" + name + "`" : "\"" + name + "\"";
    }

    /// The table name.
    public String name() {
        return table;
    }

    /// Whether the table exists.
    public boolean exists() throws IOException {
        return target.tableExists(table);
    }

    /// Creates the table and its index.
    public void create() throws IOException {
        String timestamp;
        String bool;
        if (mysql) {
            timestamp = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP";
            bool = "BOOL";
        } else if (postgres) {
            timestamp = "TIMESTAMP NOT NULL DEFAULT now()";
            bool = "BOOLEAN";
        } else {
            timestamp = "TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now'))";
            bool = "BOOLEAN";
        }
        target.execute("CREATE TABLE " + quoted + " ("
                + quote("installed_rank") + " INT NOT NULL PRIMARY KEY, "
                + quote("version") + " VARCHAR(50), "
                + quote("description") + " VARCHAR(200) NOT NULL, "
                + quote("type") + " VARCHAR(20) NOT NULL, "
                + quote("script") + " VARCHAR(1000) NOT NULL, "
                + quote("checksum") + " INT, "
                + quote("installed_by") + " VARCHAR(100) NOT NULL, "
                + quote("installed_on") + " " + timestamp + ", "
                + quote("execution_time") + " INT NOT NULL, "
                + quote("success") + " " + bool + " NOT NULL)", null);
        target.execute("CREATE INDEX " + quote(table + "_s_idx") + " ON " + quoted + " (" + quote("success") + ")",
                null);
    }

    /// Reads every row in the order it was written.
    public List<Row> read() throws IOException {
        String text = mysql ? "CHAR" : "TEXT";
        List<String[]> raw = target.query("SELECT " + quote("installed_rank") + ", " + quote("version") + ", "
                + quote("description") + ", " + quote("type") + ", " + quote("script") + ", " + quote("checksum")
                + ", CAST(" + quote("installed_on") + " AS " + text + "), " + quote("execution_time")
                + ", CASE WHEN " + quote("success") + " THEN 1 ELSE 0 END FROM " + quoted + " ORDER BY "
                + quote("installed_rank"), null);
        List<Row> rows = new ArrayList<Row>();
        for (String[] values : raw) {
            Row row = new Row();
            row.rank = (int) number(values[0]);
            row.version = values[1];
            row.description = values[2] == null ? "" : values[2];
            row.type = values[3];
            row.script = values[4];
            row.checksum = values[5] == null ? null : Integer.valueOf((int) number(values[5]));
            row.installedOn = values[6];
            row.executionTime = (int) number(values[7]);
            row.success = number(values[8]) != 0;
            rows.add(row);
        }
        return rows;
    }

    /// Whether a successful row for this version is already recorded. Asked again inside the
    /// migration's own transaction, which is what stops two processes on an engine without a
    /// migration lock from both applying it.
    public boolean applied(String version) throws IOException {
        List<String[]> rows = target.query("SELECT COUNT(*) FROM " + quoted + " WHERE " + quote("version")
                + " = ?", new Object[] {version});
        return !rows.isEmpty() && number(rows.get(0)[0]) > 0;
    }

    /// Appends a row and returns its rank.
    public int insert(String version, String description, String type, String script, Integer checksum,
            String installedBy, int executionTime, boolean success) throws IOException {
        List<String[]> max = target.query("SELECT MAX(" + quote("installed_rank") + ") FROM " + quoted, null);
        int rank = 1;
        if (!max.isEmpty() && max.get(0)[0] != null) {
            rank = (int) number(max.get(0)[0]) + 1;
        }
        String flag;
        if (postgres) {
            flag = success ? "TRUE" : "FALSE";
        } else {
            flag = success ? "1" : "0";
        }
        target.execute("INSERT INTO " + quoted + " (" + quote("installed_rank") + ", " + quote("version") + ", "
                + quote("description") + ", " + quote("type") + ", " + quote("script") + ", " + quote("checksum")
                + ", " + quote("installed_by") + ", " + quote("execution_time") + ", " + quote("success")
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, " + flag + ")",
                new Object[] {Long.valueOf(rank), version, clip(description, 200), type, clip(script, 1000),
                        checksum == null ? null : Long.valueOf(checksum.intValue()), clip(installedBy, 100),
                        Long.valueOf(executionTime)});
        return rank;
    }

    /// Deletes every row marked failed.
    public void deleteFailed() throws IOException {
        target.execute("DELETE FROM " + quoted + " WHERE NOT " + quote("success"), null);
    }

    /// Rewrites the checksum and description of one row.
    public void realign(int rank, Integer checksum, String description) throws IOException {
        target.execute("UPDATE " + quoted + " SET " + quote("checksum") + " = ?, " + quote("description")
                + " = ? WHERE " + quote("installed_rank") + " = ?",
                new Object[] {checksum == null ? null : Long.valueOf(checksum.intValue()), clip(description, 200),
                        Long.valueOf(rank)});
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    /// Whether a value an engine printed for a boolean or a 0/1 answer means true.
    public static boolean truthy(String text) {
        return number(text) != 0;
    }

    /// Parses an integral column an engine printed. Some drivers print an integer through a
    /// floating type, so a fraction of zeros is accepted and dropped.
    static long number(String text) {
        if (text == null) {
            return 0;
        }
        String value = text.trim();
        int dot = value.indexOf('.');
        if (dot >= 0) {
            value = value.substring(0, dot);
        }
        if ("t".equals(value) || "true".equals(value)) {
            return 1;
        }
        if (value.length() == 0 || "f".equals(value) || "false".equals(value)) {
            return 0;
        }
        return Long.parseLong(value);
    }
}
