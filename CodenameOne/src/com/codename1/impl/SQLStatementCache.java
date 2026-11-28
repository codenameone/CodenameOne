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
package com.codename1.impl;

/// Prepared statements a C-backed SQLite port keeps between executions of the same SQL.
///
/// Preparing is parsing: for a short INSERT it costs more than running it, and code that writes
/// a row at a time sends the same text again and again. The port takes a statement out with
/// `take`, runs and resets it, and hands it back with `put`; a statement is never in the cache
/// while it runs, so a failure leaves nothing half-stepped behind -- the port finalizes it
/// instead of returning it. Statement handles are opaque here, the port finalizes whatever `put`
/// evicts and everything `drain` returns, and must drain before closing the connection, because
/// SQLite refuses to close one with a statement still alive.
public final class SQLStatementCache {
    private static final int SIZE = 16;
    private final String[] sql = new String[SIZE];
    private final long[] stmt = new long[SIZE];
    private int next;

    /// The cached statement for `text`, removed from the cache, or 0 when there is none.
    public long take(String text) {
        for (int iter = 0; iter < SIZE; iter++) {
            String s = sql[iter];
            // Identity first: callers usually pass the same string object each time.
            if (s != null && (s == text || s.equals(text))) { //NOPMD CompareObjectsWithEquals - identity is the fast path, equals follows
                long out = stmt[iter];
                sql[iter] = null;
                stmt[iter] = 0;
                return out;
            }
        }
        return 0;
    }

    /// Caches a reset statement for `text`, and answers the statement it displaced, which the
    /// caller must finalize, or 0.
    public long put(String text, long statement) {
        for (int iter = 0; iter < SIZE; iter++) {
            if (sql[iter] == null) {
                sql[iter] = text;
                stmt[iter] = statement;
                return 0;
            }
        }
        long evicted = stmt[next];
        sql[next] = text;
        stmt[next] = statement;
        next = (next + 1) % SIZE;
        return evicted;
    }

    /// Empties the cache and answers every statement it held, for the caller to finalize.
    public long[] drain() {
        int count = 0;
        for (int iter = 0; iter < SIZE; iter++) {
            if (sql[iter] != null) {
                count++;
            }
        }
        long[] out = new long[count];
        int pos = 0;
        for (int iter = 0; iter < SIZE; iter++) {
            if (sql[iter] != null) {
                out[pos++] = stmt[iter];
                sql[iter] = null;
                stmt[iter] = 0;
            }
        }
        return out;
    }
}
