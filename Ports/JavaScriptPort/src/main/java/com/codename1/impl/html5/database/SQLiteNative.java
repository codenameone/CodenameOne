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
package com.codename1.impl.html5.database;

import java.io.IOException;

/**
 * Bindings to the SQLite build compiled to WebAssembly, implemented in port.js.
 *
 * The engine runs inside the same worker as the application code, so every call here is an
 * ordinary synchronous function call once {@link #init()} has completed. Only that first call
 * suspends, while the module is fetched and its storage is opened; the runtime's yield-on-promise
 * support makes even that look synchronous to Java.
 *
 * This replaces WebSQL, which Chrome removed and which Firefox never implemented.
 */
public class SQLiteNative {

    private SQLiteNative() {
    }

    /**
     * Loads the engine and opens its storage. Safe to call repeatedly; only the first call does
     * any work.
     *
     * @return true when the engine is usable
     */
    public static native boolean init();

    /** True when databases are stored durably rather than only in memory. */
    public static native boolean isPersistent();

    /** True when the engine supports encrypted databases. */
    public static native boolean isCipherAvailable();

    public static native boolean exists(String name);

    public static native void delete(String name);

    /**
     * Opens or creates a database.
     *
     * @param name the database name
     * @param key  the key literal, or null for a plaintext database
     * @return the database peer, or 0 on failure
     */
    public static native long open(String name, String key) throws IOException;

    public static native void close(long dbPeer);

    public static native void rekey(long dbPeer, String key) throws IOException;

    /** Runs a whole script, which may contain several statements. */
    public static native void execScript(long dbPeer, String sql) throws IOException;

    public static native long prepare(long dbPeer, String sql) throws IOException;

    public static native int parameterCount(long stmtPeer);

    public static native void bindNull(long stmtPeer, int index);

    public static native void bindString(long stmtPeer, int index, String value);

    public static native void bindBlob(long stmtPeer, int index, byte[] value);

    public static native void bindLong(long stmtPeer, int index, long value);

    public static native void bindDouble(long stmtPeer, int index, double value);

    /** Steps a statement, returning true when it landed on a row. */
    public static native boolean step(long stmtPeer) throws IOException;

    /** Resets a statement to before its first row, keeping its bindings. */
    public static native void reset(long stmtPeer);

    public static native void finish(long stmtPeer);

    /** Steps to completion and finalizes, for statements that return no rows. */
    public static native void executeAndFinish(long stmtPeer) throws IOException;

    public static native int columnCount(long stmtPeer);

    public static native String columnName(long stmtPeer, int col);

    public static native boolean columnIsNull(long stmtPeer, int col);

    public static native String columnString(long stmtPeer, int col);

    public static native byte[] columnBlob(long stmtPeer, int col);

    public static native double columnDouble(long stmtPeer, int col);

    public static native long columnLong(long stmtPeer, int col);
}
