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
package com.codename1.impl.orm;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// SQLite adapter used by managed sessions; the entity manager owns the database.
/// Internal connection adapter; not an application API.
/// @hidden
public final class SessionSqlAccess implements SqlAccess {
    private final Database db;
    private boolean ownsTransaction;
    private void checkOwner() throws IOException {
        if (db.isInTransaction() && !ownsTransaction) {
            throw new IOException("Database is in another transaction");
        }
        if (!db.isInTransaction()) {
            db.execute("PRAGMA foreign_keys = ON");
        }
    }
    public SessionSqlAccess(Database db) {
        this.db = db;
    }
    @Override
    public String dialect() {
        return "sqlite";
    }
    @Override
    public List<Object[]> describe(String table) throws IOException {
        List<Object[]> raw = query("PRAGMA table_info(" + quote(table) + ")", new Object[0],
                new int[] {Attribute.INTEGER, Attribute.TEXT, Attribute.TEXT, Attribute.INTEGER, Attribute.TEXT,
                        Attribute.INTEGER});
        List<Object[]> result = new ArrayList<Object[]>();
        for (Object[] row : raw) {
            result.add(new Object[] {row[1], row[2], row[3], row[5]});
        }
        return result;
    }
    @Override
    public String quote(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
    @Override
    public String columnType(int kind) {
        switch (kind) {
            case Attribute.TEXT:
                return "TEXT";
            case Attribute.REAL:
                return "REAL";
            case Attribute.BLOB:
                return "BLOB";
            default:
                return "INTEGER";
        }
    }
    @Override
    public String generatedKeyColumn(int kind, String name) {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }
    @Override
    public String assignedKeyColumn(int kind) {
        return columnType(kind) + " PRIMARY KEY NOT NULL";
    }
    @Override
    public String insertDefaults(String table) {
        return "INSERT INTO " + table + " DEFAULT VALUES";
    }
    @Override
    public String orderValue(String expression, int kind) {
        return expression;
    }
    @Override
    public String orderBy(String expression, boolean ascending, int kind) {
        // SQLite already sorts nulls low and text in binary order. Avoid the
        // NULLS FIRST/LAST syntax unavailable on older supported devices.
        return expression + (ascending ? " ASC" : " DESC");
    }
    @Override
    public String likeOperator(boolean escaped) {
        return " GLOB ?";
    }
    @Override
    public String likePattern(String pattern, String escape) {
        return SqlPatterns.normalize(pattern, escape, true);
    }
    @Override
    public String likeExpression(String expression) {
        return SqlPatterns.globExpression(expression);
    }
    @Override
    public String lockClause(com.codename1.orm.session.LockMode mode) {
        if (mode == com.codename1.orm.session.LockMode.NONE) {
            return "";
        }
        throw new UnsupportedOperationException("SQLite does not support pessimistic row locks");
    }
    @Override
    public String limit(int limit, int offset) {
        return limit < 0 && offset == 0 ? "" : " LIMIT " + limit + " OFFSET " + offset;
    }
    private void validateParameters(Object[] args) throws IOException {
        for (Object value : args) {
            if (value instanceof Double && (Double.isNaN(((Double) value).doubleValue())
                    || Double.isInfinite(((Double) value).doubleValue()))
                    || value instanceof Float && (Float.isNaN(((Float) value).floatValue())
                    || Float.isInfinite(((Float) value).floatValue()))) {
                throw new IOException("Non-finite numeric parameters are not portable");
            }
            if (value instanceof String && ((String) value).indexOf(0) >= 0) {
                throw new IOException("NUL in text parameters is not portable; use a byte array");
            }
        }
    }
    @Override
    public List<Object[]> query(String sql, Object[] args, int[] kinds) throws IOException {
        validateParameters(args);
        synchronized (db) {
            checkOwner();
            Cursor cursor = db.executeQuery(sql, args);
            try {
                List<Object[]> result = new ArrayList<Object[]>();
                while (cursor.next()) {
                    Row row = cursor.getRow();
                    Object[] values = new Object[kinds.length];
                    for (int i = 0; i < values.length; i++) {
                        switch (kinds[i]) {
                            case Attribute.TEXT:
                                values[i] = row.getString(i);
                                break;
                            case Attribute.BLOB:
                                values[i] = row.getBlob(i);
                                break;
                            case Attribute.REAL:
                                values[i] = Double.valueOf(row.getDouble(i));
                                break;
                            default:
                                values[i] = Long.valueOf(row.getLong(i));
                                break;
                        }
                        if (Database.supportsWasNull(row)) {
                            if (Database.wasNull(row)) {
                                values[i] = null;
                            }
                        } else if (kinds[i] != Attribute.TEXT && kinds[i] != Attribute.BLOB &&
                                   row.getString(i) == null) {
                            values[i] = null;
                        }
                    }
                    result.add(values);
                }
                return result;
            } finally {
                cursor.close();
            }
        }
    }
    @Override
    public int execute(String sql, Object[] args) throws IOException {
        validateParameters(args);
        synchronized (db) {
            checkOwner();
            db.execute(sql, args);
            Cursor cursor = db.executeQuery("SELECT changes()");
            try {
                if (!cursor.next()) {
                    throw new IOException("SQLite did not return changes()");
                }
                return cursor.getRow().getInteger(0);
            } finally {
                cursor.close();
            }
        }
    }
    @Override
    public long insert(String sql, Object[] args, String keyColumn) throws IOException {
        validateParameters(args);
        synchronized (db) {
            checkOwner();
            db.execute(sql, args);
            Cursor cursor = db.executeQuery("SELECT last_insert_rowid()");
            try {
                if (!cursor.next()) {
                    throw new IOException("SQLite did not return generated id");
                }
                return cursor.getRow().getLong(0);
            } finally {
                cursor.close();
            }
        }
    }
    @Override
    public void prepareGenerator(int strategy, String name) throws IOException {
        if (strategy == 1) {
            return;
        }
        execute("CREATE TABLE IF NOT EXISTS cn1_orm_sequences (sequence_name TEXT PRIMARY KEY NOT NULL, next_value "
                        + "INTEGER NOT NULL)",
                new Object[0]);
        execute("INSERT OR IGNORE INTO cn1_orm_sequences (sequence_name,next_value) VALUES (?,0)", new Object[] {name});
    }
    @Override
    public Object nextIdentifier(int strategy, String name, int kind) throws IOException {
        if (strategy == 1) {
            String hex = (String) query("SELECT lower(hex(randomblob(16)))", new Object[0], new int[] {Attribute.TEXT})
                                 .get(0)[0];
            int variant = (Integer.parseInt(hex.substring(16, 17), 16) & 3) | 8;
            return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-4" + hex.substring(13, 16) + "-" +
                    Integer.toString(variant, 16) + hex.substring(17, 20) + "-" + hex.substring(20);
        }
        synchronized (db) {
            long max = kind == Attribute.INTEGER ? Integer.MAX_VALUE : Long.MAX_VALUE;
            if (execute("UPDATE cn1_orm_sequences SET next_value = next_value + 1 WHERE sequence_name = ? AND "
                                + "next_value < ?",
                        new Object[] {name, Long.valueOf(max)}) != 1) {
                throw new IOException("Identifier generator is missing or exhausted: " + name);
            }
            return query("SELECT next_value FROM cn1_orm_sequences WHERE sequence_name = ?", new Object[] {name},
                    new int[] {Attribute.BIGINT})
                    .get(0)[0];
        }
    }
    @Override
    public void begin() throws IOException {
        synchronized (db) {
            if (db.isInTransaction()) {
                throw new IOException("Database already has an active transaction");
            }
            db.execute("PRAGMA foreign_keys = ON");
            db.beginTransaction();
            ownsTransaction = true;
        }
    }
    @Override
    public void commit() throws IOException {
        synchronized (db) {
            if (!ownsTransaction) {
                throw new IOException("Session does not own the transaction");
            }
            db.commitTransaction();
            ownsTransaction = false;
        }
    }
    @Override
    public void rollback() throws IOException {
        synchronized (db) {
            if (!ownsTransaction) {
                throw new IOException("Session does not own the transaction");
            }
            db.rollbackTransaction();
            ownsTransaction = false;
        }
    }
    @Override
    public void close() {
    }
}
