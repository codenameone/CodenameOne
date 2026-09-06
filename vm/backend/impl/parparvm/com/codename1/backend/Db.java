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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite persistence for server-side binaries, on the engine the translator
 * already bundles. Not com.codename1.db.Database, which needs a
 * CodenameOneImplementation for every call.
 *
 * Parameters are always bound, never interpolated: string concatenation into SQL
 * is how injection happens, and a server parses input it did not write.
 */
public final class Db {
    /** Column type codes, mirroring SQLITE_*. */
    private static final int TYPE_INTEGER = 1;
    private static final int TYPE_FLOAT = 2;
    private static final int TYPE_TEXT = 3;
    private static final int TYPE_BLOB = 4;
    private static final int TYPE_NULL = 5;

    private long handle;

    private Db(long handle) {
        this.handle = handle;
    }

    /**
     * Opens (or creates) the database at the given path. ":memory:" gives a
     * process-lifetime database, which is what a stateless function usually wants
     * for a cache.
     */
    public static Db open(String path) throws IOException {
        long h = openImpl(path);
        if(h == 0) {
            throw new IOException("Could not open database at " + path);
        }
        return new Db(h);
    }

    /**
     * Runs a statement that returns no rows. Returns the number of rows changed.
     */
    public int execute(String sql, Object[] params) throws IOException {
        long stmt = prepare(sql, params);
        try {
            int rc = stepImpl(stmt);
            if(rc < 0) {
                throw new IOException("Statement failed: " + errorImpl(handle) + " [" + sql + "]");
            }
            // Drain: a statement may return rows even when the caller ignores them.
            while(rc == 1) {
                rc = stepImpl(stmt);
                if(rc < 0) {
                    throw new IOException("Statement failed: " + errorImpl(handle) + " [" + sql + "]");
                }
            }
            return changesImpl(handle);
        } finally {
            finalizeImpl(stmt);
        }
    }

    /**
     * Runs a query and returns every row as a column-name to value map. Values are
     * String, Long, Double or null, which is exactly what the JSON writer accepts.
     */
    public List query(String sql, Object[] params) throws IOException {
        long stmt = prepare(sql, params);
        try {
            List rows = new ArrayList();
            int columns = columnCountImpl(stmt);
            String[] names = new String[columns];
            for(int iter = 0 ; iter < columns ; iter++) {
                names[iter] = columnNameImpl(stmt, iter);
            }
            while(true) {
                int rc = stepImpl(stmt);
                if(rc < 0) {
                    throw new IOException("Query failed: " + errorImpl(handle) + " [" + sql + "]");
                }
                if(rc == 0) {
                    return rows;
                }
                Map row = new LinkedHashMap();
                for(int iter = 0 ; iter < columns ; iter++) {
                    row.put(names[iter], columnValue(stmt, iter));
                }
                rows.add(row);
            }
        } finally {
            finalizeImpl(stmt);
        }
    }

    /**
     * Runs body inside a transaction, committing when it returns and rolling back
     * if it throws. A half-applied multi-statement change is the failure mode this
     * exists to prevent, and getting the rollback right by hand at every call site
     * is how it gets missed.
     */
    public Object transaction(Work body) throws Exception {
        execute("BEGIN IMMEDIATE", null);
        boolean committed = false;
        try {
            Object result = body.run(this);
            execute("COMMIT", null);
            committed = true;
            return result;
        } finally {
            if(!committed) {
                try {
                    execute("ROLLBACK", null);
                } catch (Exception err) {
                    // The original failure is the one worth reporting; a rollback
                    // that also fails must not replace it.
                    System.err.println("rollback failed: " + err);
                }
            }
        }
    }

    /** A unit of work run inside {@link #transaction}. */
    public interface Work {
        Object run(Db db) throws Exception;
    }

    /**
     * Switches the database to write-ahead logging, which is what lets readers run
     * while a writer is active. Worth doing once after open for anything that
     * serves concurrent requests; pointless for :memory:.
     */
    public void enableWriteAheadLog() throws IOException {
        query("PRAGMA journal_mode=WAL", null);
        execute("PRAGMA synchronous=NORMAL", null);
    }

    /**
     * How long a blocked writer waits for a competing one before giving up. Without
     * this, two connections writing at once produce SQLITE_BUSY immediately rather
     * than queueing.
     */
    public void setBusyTimeout(int millis) throws IOException {
        execute("PRAGMA busy_timeout=" + millis, null);
    }

    /** The rowid the most recent insert produced. */
    public long lastInsertId() {
        return lastInsertRowIdImpl(handle);
    }

    public void close() {
        if(handle != 0) {
            long h = handle;
            handle = 0;
            closeImpl(h);
        }
    }

    private Object columnValue(long stmt, int index) {
        switch(columnTypeImpl(stmt, index)) {
            case TYPE_INTEGER:
                return Long.valueOf(columnLongImpl(stmt, index));
            case TYPE_FLOAT:
                return Double.valueOf(columnDoubleImpl(stmt, index));
            case TYPE_NULL:
                return null;
            case TYPE_BLOB:
                return columnBlobImpl(stmt, index);
            case TYPE_TEXT:
            default:
                return columnStringImpl(stmt, index);
        }
    }

    private long prepare(String sql, Object[] params) throws IOException {
        if(handle == 0) {
            throw new IOException("Database is closed");
        }
        long stmt = prepareImpl(handle, sql);
        if(stmt == 0) {
            throw new IOException("Could not prepare: " + errorImpl(handle) + " [" + sql + "]");
        }
        if(params != null) {
            for(int iter = 0 ; iter < params.length ; iter++) {
                bind(stmt, iter + 1, params[iter]);
            }
        }
        return stmt;
    }

    private static void bind(long stmt, int index, Object value) {
        if(value == null) {
            bindNullImpl(stmt, index);
        } else if(value instanceof String) {
            bindStringImpl(stmt, index, (String)value);
        } else if(value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            bindLongImpl(stmt, index, ((Number)value).longValue());
        } else if(value instanceof Double || value instanceof Float) {
            bindDoubleImpl(stmt, index, ((Number)value).doubleValue());
        } else if(value instanceof byte[]) {
            bindBlobImpl(stmt, index, (byte[])value);
        } else if(value instanceof Boolean) {
            bindLongImpl(stmt, index, ((Boolean)value).booleanValue() ? 1 : 0);
        } else {
            bindStringImpl(stmt, index, String.valueOf(value));
        }
    }

    private static native long openImpl(String path);
    private static native int closeImpl(long handle);
    private static native String errorImpl(long handle);
    private static native long prepareImpl(long handle, String sql);
    private static native void bindStringImpl(long stmt, int index, String value);
    private static native void bindLongImpl(long stmt, int index, long value);
    private static native void bindDoubleImpl(long stmt, int index, double value);
    private static native void bindNullImpl(long stmt, int index);
    private static native void bindBlobImpl(long stmt, int index, byte[] value);
    private static native int stepImpl(long stmt);
    private static native int columnCountImpl(long stmt);
    private static native String columnNameImpl(long stmt, int index);
    private static native int columnTypeImpl(long stmt, int index);
    private static native String columnStringImpl(long stmt, int index);
    private static native long columnLongImpl(long stmt, int index);
    private static native double columnDoubleImpl(long stmt, int index);
    private static native byte[] columnBlobImpl(long stmt, int index);
    private static native void finalizeImpl(long stmt);
    private static native int changesImpl(long handle);
    private static native long lastInsertRowIdImpl(long handle);
}
