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

import java.util.function.Function;

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

    /// Whether one is installed.
    public static boolean installed() {
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

        /// The row lists of this connection's cursors that still have rows to
        /// read, by identity, while [#holdRead(boolean)] is on for them.
        private Object[] pending = new Object[4];
        private int pendingCount;

        /// A cursor was handed `rows`, one or more: while it works
        /// through them the connection may keep its read lock (see
        /// [#holdRead(boolean)]), as CPython's does under a pending statement.
        public final void pending(Object rows) {
            for (int i = 0; i < pendingCount; i++) {
                if (pending[i] == rows) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return;
                }
            }
            if (pendingCount == pending.length) {
                // A cursor left part read never says it is done: the oldest
                // slot is most likely one, and goes, so it is not kept alive.
                System.arraycopy(pending, 1, pending, 0, pendingCount - 1);
                pendingCount--;
            }
            pending[pendingCount++] = rows;
            // Every time, not only on the first: the engine lets go by itself
            // before a statement the lock would be in the way of.
            holdRead(true);
        }

        /// The cursor that was handed `rows` read the last of them, or moved on.
        public final void drained(Object rows) {
            for (int i = 0; i < pendingCount; i++) {
                if (pending[i] == rows) { //NOPMD CompareObjectsWithEquals - identity is meant
                    pending[i] = pending[--pendingCount];
                    pending[pendingCount] = null;
                    if (pendingCount == 0) {
                        holdRead(false);
                    }
                    return;
                }
            }
        }

        /// Whether any cursor's rows are pending: the caller skips the lookup
        /// when none are.
        public final boolean anyPending() {
            return pendingCount > 0;
        }

        /// Keeps the engine's read lock between statements, or lets it go. Only
        /// ever a saving -- the rows are read already -- so an engine with no
        /// such lock, or no way to keep it, leaves this as it is.
        protected void holdRead(boolean on) {
            // Nothing to keep by default.
        }

        public abstract void begin() throws IOException;

        public abstract void commit() throws IOException;

        public abstract void rollback() throws IOException;

        public abstract void close() throws IOException;

        /// Registers the scalar SQL function `name` of `nargs` arguments (-1
        /// for any number); a later registration of the same name and count
        /// replaces it. Answers false where the engine cannot call back into
        /// the application, which is the default.
        public boolean createFunction(String name, int nargs, Function<Object[], Object> body) throws IOException {
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
