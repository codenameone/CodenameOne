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
package com.pyhero;

import com.codename1.python.runtime.PySql;
import com.codename1.db.SqlFunction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// SQLite for a native benchmark binary: the runtime's `PySql` over the
/// host's libsqlite3 (cn1_hostsql.c), where a device port answers with its
/// own database. Paths are the host's own.
public final class HostSql extends PySql {
    private static final int ROW = 100;
    private static final int DONE = 101;
    /// Every registered SQL function; cn1_hostsql.c passes the index back.
    private static final List<SqlFunction> FUNCTIONS = new ArrayList<SqlFunction>();

    @Override
    public PySql.Connection open(String name) throws IOException {
        long db = openImpl(name.isEmpty() ? ":memory:" : name);
        if (db == 0) {
            throw new IOException("unable to open database file");
        }
        return new Conn(db);
    }

    /// A prepared statement and the names of its columns, which are the same every time
    /// it runs until SQLite compiles it again.
    private static final class Stmt {
        final long st;
        String[] columns;
        int reprepares;

        Stmt(long st) {
            this.st = st;
        }
    }

    private static final class Conn extends PySql.Connection {
        /// As many prepared statements as CPython's sqlite3 keeps for a
        /// connection.
        private static final int KEPT = 128;
        private long db;
        /// Statements this connection ran and may run again, by their text,
        /// the one used longest ago first. One that is running is not here,
        /// so a function it calls that runs the same text prepares its own.
        private final LinkedHashMap<String, Stmt> kept = new LinkedHashMap<String, Stmt>();

        Conn(long db) {
            this.db = db;
        }

        @Override
        public Result run(String sql, Object[] params, boolean query) throws IOException {
            Stmt held = kept.remove(sql);
            if (held == null) {
                held = new Stmt(prepareImpl(db, sql));
            }
            long st = held.st;
            if (st == 0) {
                throw new IOException(errorImpl(db));
            }
            Result r = new Result();
            boolean done = false;
            try {
                for (int i = 0; i < params.length; i++) {
                    Object p = params[i];
                    int rc;
                    if (p == null) {
                        rc = bindNullImpl(st, i + 1);
                    } else if (p instanceof Long) {
                        rc = bindLongImpl(st, i + 1, ((Long) p).longValue());
                    } else if (p instanceof Double) {
                        rc = bindDoubleImpl(st, i + 1, ((Double) p).doubleValue());
                    } else if (p instanceof byte[]) {
                        rc = bindBlobImpl(st, i + 1, (byte[]) p);
                    } else {
                        rc = bindStringImpl(st, i + 1, p.toString());
                    }
                    if (rc != 0) {
                        throw new IOException(errorImpl(db));
                    }
                }
                int n = 0;
                boolean first = true;
                while (true) {
                    int rc = stepImpl(st);
                    if (rc != DONE && rc != ROW) {
                        throw new IOException(errorImpl(db));
                    }
                    if (first) {
                        // The names are read after the first step, which is where SQLite
                        // compiles a statement again if the schema changed under it, and
                        // are kept with the statement until it does so.
                        first = false;
                        int compiled = repreparesImpl(st);
                        if (held.columns == null || held.reprepares != compiled) {
                            String[] names = new String[columnCountImpl(st)];
                            for (int i = 0; i < names.length; i++) {
                                names[i] = columnNameImpl(st, i);
                            }
                            held.columns = names;
                            held.reprepares = compiled;
                        }
                        r.columns = held.columns;
                        n = r.columns.length;
                    }
                    if (rc == DONE) {
                        break;
                    }
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) {
                        switch (columnTypeImpl(st, i)) {
                            case 1:
                                row[i] = Long.valueOf(columnLongImpl(st, i));
                                break;
                            case 2:
                                row[i] = Double.valueOf(columnDoubleImpl(st, i));
                                break;
                            case 4:
                                row[i] = columnBlobImpl(st, i);
                                break;
                            case 5:
                                row[i] = null;
                                break;
                            default:
                                row[i] = columnStringImpl(st, i);
                                break;
                        }
                    }
                    r.rows.add(row);
                }
                done = true;
            } finally {
                // One that failed is not run again: the next use prepares it
                // and reports what is wrong with it afresh.
                if (done && resetImpl(st) == 0) {
                    kept.put(sql, held);
                    if (kept.size() > KEPT) {
                        Iterator<Map.Entry<String, Stmt>> it = kept.entrySet().iterator();
                        long old = it.next().getValue().st;
                        it.remove();
                        finalizeImpl(old);
                    }
                } else {
                    finalizeImpl(st);
                }
            }
            r.changes = changesImpl(db);
            r.lastRowId = lastInsertRowIdImpl(db);
            return r;
        }

        private void exec(String sql) throws IOException {
            run(sql, new Object[0], false);
        }

        @Override
        public void begin() throws IOException {
            exec("BEGIN");
        }

        @Override
        public void commit() throws IOException {
            exec("COMMIT");
        }

        @Override
        public void rollback() throws IOException {
            exec("ROLLBACK");
        }

        @Override
        public boolean createFunction(String name, int nargs, SqlFunction body) throws IOException {
            FUNCTIONS.add(body);
            if (createFunctionImpl(db, name, nargs, FUNCTIONS.size() - 1) != 0) {
                throw new IOException(errorImpl(db));
            }
            return true;
        }

        @Override
        public void close() throws IOException {
            if (db != 0) {
                for (Stmt held : kept.values()) {
                    finalizeImpl(held.st);
                }
                kept.clear();
                closeImpl(db);
                db = 0;
            }
        }
    }

    /// One call of a registered function, made by cn1_hostsql.c from inside
    /// the engine. Nothing may be thrown back through SQLite's C frames, so a
    /// failure becomes the statement's error, in CPython's words.
    static void callFunction(long ctx, int id, int argc, long argv) {
        Object[] args = new Object[argc];
        for (int i = 0; i < argc; i++) {
            switch (valueTypeImpl(argv, i)) {
                case 1:
                    args[i] = Long.valueOf(valueLongImpl(argv, i));
                    break;
                case 2:
                    args[i] = Double.valueOf(valueDoubleImpl(argv, i));
                    break;
                case 4:
                    args[i] = valueBlobImpl(argv, i);
                    break;
                case 5:
                    args[i] = null;
                    break;
                default:
                    args[i] = valueStringImpl(argv, i);
                    break;
            }
        }
        Object v;
        try {
            v = FUNCTIONS.get(id).call(args);
        } catch (Throwable e) { //NOPMD AvoidCatchingThrowable - a throw would unwind through SQLite's C frames
            resultErrorImpl(ctx, "user-defined function raised exception");
            return;
        }
        if (v == null) {
            resultNullImpl(ctx);
        } else if (v instanceof Long) {
            resultLongImpl(ctx, ((Long) v).longValue());
        } else if (v instanceof Double) {
            resultDoubleImpl(ctx, ((Double) v).doubleValue());
        } else if (v instanceof byte[]) {
            resultBlobImpl(ctx, (byte[]) v);
        } else {
            resultStringImpl(ctx, String.valueOf(v));
        }
    }

    private static native int createFunctionImpl(long db, String name, int nargs, int id);
    private static native int valueTypeImpl(long argv, int index);
    private static native long valueLongImpl(long argv, int index);
    private static native double valueDoubleImpl(long argv, int index);
    private static native String valueStringImpl(long argv, int index);
    private static native byte[] valueBlobImpl(long argv, int index);
    private static native void resultNullImpl(long ctx);
    private static native void resultLongImpl(long ctx, long value);
    private static native void resultDoubleImpl(long ctx, double value);
    private static native void resultStringImpl(long ctx, String value);
    private static native void resultBlobImpl(long ctx, byte[] value);
    private static native void resultErrorImpl(long ctx, String message);
    private static native long openImpl(String path);
    private static native int closeImpl(long db);
    private static native String errorImpl(long db);
    private static native long prepareImpl(long db, String sql);
    private static native int bindStringImpl(long st, int index, String value);
    private static native int bindLongImpl(long st, int index, long value);
    private static native int bindDoubleImpl(long st, int index, double value);
    private static native int bindNullImpl(long st, int index);
    private static native int bindBlobImpl(long st, int index, byte[] value);
    private static native int stepImpl(long st);
    private static native int repreparesImpl(long st);
    private static native int columnCountImpl(long st);
    private static native String columnNameImpl(long st, int index);
    private static native int columnTypeImpl(long st, int index);
    private static native String columnStringImpl(long st, int index);
    private static native long columnLongImpl(long st, int index);
    private static native double columnDoubleImpl(long st, int index);
    private static native byte[] columnBlobImpl(long st, int index);
    private static native void finalizeImpl(long st);
    private static native int resetImpl(long st);
    private static native long changesImpl(long db);
    private static native long lastInsertRowIdImpl(long db);
}
