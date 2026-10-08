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
package android.database.sqlite;

/// A statement compiled once and run any number of times with different
/// bindings.
public final class SQLiteStatement extends SQLiteProgram {

    SQLiteStatement(SQLiteDatabase db, String sql, Object[] bindArgs) {
        super(db, sql, bindArgs, null);
    }

    public void execute() {
        acquireReference();
        try {
            getDatabase().runStatement(getSql(), getBindArgs());
        } finally {
            releaseReference();
        }
    }

    /// Runs an UPDATE or DELETE and returns the number of rows it changed.
    public int executeUpdateDelete() {
        acquireReference();
        try {
            getDatabase().runStatement(getSql(), getBindArgs());
            return (int) getDatabase().changesAndLastRowId()[0];
        } finally {
            releaseReference();
        }
    }

    /// Runs an INSERT and returns the new row's id, or -1 when no row was
    /// inserted (an `OR IGNORE` conflict, for instance).
    ///
    /// The statement and the `changes()`/`last_insert_rowid()` read are two
    /// engine calls, deliberately without a lock around them: Codename One
    /// code runs on one thread and the runtime adds no locks. Used from one
    /// thread the pair is exact. Two threads inserting through one database
    /// at once can interleave here, as they can anywhere else in this class;
    /// serializing statements on the connection belongs to the Codename One
    /// `Database` implementation, not to a lock in this runtime.
    public long executeInsert() {
        acquireReference();
        try {
            getDatabase().runStatement(getSql(), getBindArgs());
            long[] r = getDatabase().changesAndLastRowId();
            return r[0] > 0 ? r[1] : -1;
        } finally {
            releaseReference();
        }
    }

    /// The first column of the first row as a long.
    public long simpleQueryForLong() {
        String v = simpleQuery();
        return v == null ? 0 : SQLiteCursor.toLong(v);
    }

    /// The first column of the first row as a string.
    public String simpleQueryForString() {
        return simpleQuery();
    }

    private String simpleQuery() {
        acquireReference();
        try {
            String[] first = getDatabase().firstValue(getSql(), getBindArgs());
            if (first == null) {
                throw new SQLiteDoneException();
            }
            return first[0];
        } finally {
            releaseReference();
        }
    }

    @Override
    public String toString() {
        return "SQLiteProgram: " + getSql();
    }
}
