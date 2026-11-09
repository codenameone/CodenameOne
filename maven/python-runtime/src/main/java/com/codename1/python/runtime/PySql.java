/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

import com.codename1.db.SqlFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The SQLite engine under the `sqlite3` module.
///
/// A Codename One application uses [Cn1Sql], over `com.codename1.db.Database`
/// and so over each platform's own SQLite. A host that is not a Codename One
/// application (a test, a server) installs its own.
public abstract class PySql {
    private static PySql current;

    /// The engine in use.
    public static PySql get() {
        if (current == null) {
            throw Py.error(Py.OS_ERROR, "no SQLite engine is installed");
        }
        return current;
    }

    static boolean installed() {
        return current != null;
    }

    /// Replaces the engine.
    public static void install(PySql sql) {
        current = sql;
    }

    /// Opens, creating it if needed, the database `name`: a file name, a
    /// path, or `:memory:` for a private database that is gone once closed.
    public abstract Connection open(String name) throws IOException;

    /// One open database.
    public abstract static class Connection {
        /// Runs one statement with its parameters bound in order. Each
        /// parameter is null, a `Long`, a `Double`, a `String` or a `byte[]`.
        /// `query` says whether the statement returns rows; when it does not,
        /// the result has no columns, and the changes and the last row id are
        /// filled for an `INSERT`, `UPDATE`, `DELETE` or `REPLACE`.
        public abstract Result run(String sql, Object[] params, boolean query) throws IOException;

        /// How many SQL functions are running a statement of their own,
        /// one inside another: what a statement run at that depth may reuse
        /// is its own. Python runs one thread at a time, so this is static.
        static int nesting;

        private String statusMessage;

        /// [#run(String, Object[], boolean)] for a caller on a hot path: the
        /// result, or null when the statement failed, the reason then being
        /// [#statusMessage()]. A port whose engine reports a status overrides
        /// it to run with no exception handler on the way; this one wraps
        /// the throwing method.
        public Result runStatus(String sql, Object[] params, boolean query) {
            try {
                return run(sql, params, query);
            } catch (IOException e) {
                return failStatus(e.getMessage());
            }
        }

        /// Why the last [#runStatus(String, Object[], boolean)] answered null.
        public String statusMessage() {
            return statusMessage;
        }

        /// Records why a statement failed, for [#statusMessage()]; answers null.
        protected final Result failStatus(String message) {
            statusMessage = String.valueOf(message);
            return null;
        }

        public abstract void begin() throws IOException;

        public abstract void commit() throws IOException;

        public abstract void rollback() throws IOException;

        public abstract void close() throws IOException;

        /// Registers the scalar SQL function `name` of `nargs` arguments (-1
        /// for any number); a later registration of the same name and count
        /// replaces it. Answers false where the engine cannot call back into
        /// the application, which is the default.
        public boolean createFunction(String name, int nargs, SqlFunction body) throws IOException {
            return false;
        }
    }

    /// What a statement produced. Values are null, `Long`, `Double`,
    /// `String` or `byte[]`, as SQLite stored them.
    public static final class Result {
        public String[] columns = NO_COLUMNS;
        public final List<Object[]> rows = new ArrayList<Object[]>();
        public long changes = -1;
        public long lastRowId = -1;
        /// Per column, the kind of value a server database sent as text that
        /// the DB-API layer turns into a Python type -- "decimal", "date",
        /// "datetime", "datetimetz" or "time" -- or null; null altogether when
        /// the engine reports none (SQLite).
        public String[] types;

        /// Empties it for another statement, keeping the row list's storage.
        public void clear() {
            columns = NO_COLUMNS;
            rows.clear();
            changes = -1;
            lastRowId = -1;
            types = null;
        }

        private static final String[] NO_COLUMNS = new String[0];
    }

    /// The value a SQLite engine that cannot report storage classes most
    /// likely stored, from its text: an integer or a real when the text is
    /// exactly the canonical form of one, text otherwise.
    public static Object inferred(String text) {
        if (text == null) {
            return null;
        }
        int n = text.length();
        if (n == 0 || n > 24) {
            return text;
        }
        int i = text.charAt(0) == '-' ? 1 : 0;
        boolean digits = i < n;
        boolean dot = false;
        for (int k = i; k < n; k++) {
            char c = text.charAt(k);
            if (c == '.' && !dot) {
                dot = true;
            } else if (c < '0' || c > '9') {
                digits = false;
                break;
            }
        }
        if (!digits) {
            return text;
        }
        try {
            if (!dot) {
                long v = Long.parseLong(text);
                return Long.toString(v).equals(text) ? (Object) Long.valueOf(v) : text;
            }
            return Double.valueOf(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
