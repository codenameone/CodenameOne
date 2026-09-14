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
package com.codename1.backend.orm;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.codename1.backend.sql.Dialect;

/**
 * One entity's SQL, built once for one engine.
 *
 * <p>Every statement an entity needs is assembled here at start-up rather than
 * per request: the strings depend on the entity and the dialect, and neither
 * changes while the process runs. What a request pays is the parameter array.
 *
 * <p>Every identifier is QUOTED. That is not caution, it is the only way one
 * schema works on three engines: PostgreSQL folds an unquoted name to lower case
 * while SQLite and MySQL preserve it, so a column called createdAt is created as
 * createdat on one engine, comes back under that name in a row, and is looked for
 * under the other. Quoting it once at the point it is generated makes the name
 * the same everywhere.
 */
final class Table {
    final EntityDefinition definition;
    final Dialect dialect;
    final ColumnDefinition[] columns;
    final int idIndex;

    private final String quotedTable;
    private final String[] quotedColumns;
    private final String selectPrefix;
    private final String insertSql;
    private final int[] insertColumns;
    private final String updateSql;
    private final int[] updateColumns;
    private final String deleteSql;
    private final String createTableSql;
    private final String dropTableSql;
    private final String idCondition;

    Table(EntityDefinition definition, Dialect dialect) {
        this.definition = definition;
        this.dialect = dialect;
        this.columns = definition.columns();
        this.idIndex = definition.idIndex();
        this.quotedTable = dialect.quote(definition.table());
        this.quotedColumns = new String[columns.length];
        for(int iter = 0 ; iter < columns.length ; iter++) {
            quotedColumns[iter] = dialect.quote(columns[iter].getColumn());
            for(int earlier = 0 ; earlier < iter ; earlier++) {
                if(columns[earlier].getColumn().equals(columns[iter].getColumn())) {
                    // Two fields on one column. Every statement would name it
                    // twice: the CREATE TABLE is refused outright, and against a
                    // schema the ORM did not create, a read puts the same value
                    // in both fields and a write stores whichever came last. The
                    // generator refuses this at build time; a hand-written
                    // definition reaches here instead.
                    throw new IllegalStateException(definition.type().getName()
                            + " maps both " + columns[earlier].getField() + " and "
                            + columns[iter].getField() + " to the column '"
                            + columns[iter].getColumn() + "'");
                }
            }
        }
        this.idCondition = quotedColumns[idIndex] + " = ?";

        StringBuilder select = new StringBuilder("SELECT ");
        for(int iter = 0 ; iter < columns.length ; iter++) {
            if(iter > 0) {
                select.append(", ");
            }
            select.append(quotedColumns[iter]);
        }
        // An explicit column list rather than SELECT *: the rows then hold
        // exactly the columns this entity knows about, in a table that may have
        // grown others, and a migration that adds one cannot change what a query
        // costs to carry back.
        select.append(" FROM ").append(quotedTable);
        this.selectPrefix = select.toString();

        this.insertColumns = indicesExcept(columns[idIndex].isGenerated() ? idIndex : -1);
        if(insertColumns.length == 0) {
            // An entity whose only persisted field is a generated key. The empty
            // column list builds "INSERT INTO t () VALUES ()", which SQLite and
            // PostgreSQL both refuse, so the row could not be created at all --
            // and each engine spells the no-columns insert its own way.
            this.insertSql = dialect.insertDefaults(quotedTable);
        } else {
            StringBuilder insert = new StringBuilder("INSERT INTO ");
            insert.append(quotedTable).append(" (");
            for(int iter = 0 ; iter < insertColumns.length ; iter++) {
                if(iter > 0) {
                    insert.append(", ");
                }
                insert.append(quotedColumns[insertColumns[iter]]);
            }
            insert.append(") VALUES (");
            for(int iter = 0 ; iter < insertColumns.length ; iter++) {
                insert.append(iter > 0 ? ", ?" : "?");
            }
            insert.append(")");
            this.insertSql = insert.toString();
        }

        this.updateColumns = indicesExcept(idIndex);
        StringBuilder update = new StringBuilder("UPDATE ");
        update.append(quotedTable).append(" SET ");
        for(int iter = 0 ; iter < updateColumns.length ; iter++) {
            if(iter > 0) {
                update.append(", ");
            }
            update.append(quotedColumns[updateColumns[iter]]).append(" = ?");
        }
        update.append(" WHERE ").append(idCondition);
        this.updateSql = updateColumns.length == 0 ? null : update.toString();

        this.deleteSql = "DELETE FROM " + quotedTable + " WHERE " + idCondition;
        this.dropTableSql = "DROP TABLE IF EXISTS " + quotedTable;

        StringBuilder create = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        create.append(quotedTable).append(" (");
        for(int iter = 0 ; iter < columns.length ; iter++) {
            if(iter > 0) {
                create.append(", ");
            }
            create.append(quotedColumns[iter]).append(' ').append(declaration(columns[iter]));
        }
        create.append(")");
        this.createTableSql = create.toString();
    }

    String getCreateTableSql() {
        return createTableSql;
    }

    String getDropTableSql() {
        return dropTableSql;
    }

    String getInsertSql() {
        return insertSql;
    }

    String getDeleteSql() {
        return deleteSql;
    }

    String getIdColumn() {
        return columns[idIndex].getColumn();
    }

    String getIdCondition() {
        return idCondition;
    }

    String quoted(int index) {
        return quotedColumns[index];
    }

    /** SELECT of every column, with {@code tail} -- WHERE, ORDER BY, LIMIT -- after it. */
    String select(String tail) {
        return tail == null || tail.length() == 0 ? selectPrefix : selectPrefix + tail;
    }

    /** DELETE of every matching row, with {@code tail} -- a WHERE -- after it. */
    String delete(String tail) {
        String head = "DELETE FROM " + quotedTable;
        return tail == null || tail.length() == 0 ? head : head + tail;
    }

    /** SELECT COUNT(*), with {@code tail} after it. */
    String count(String tail) {
        String head = "SELECT COUNT(*) AS " + dialect.quote("cn1_count") + " FROM " + quotedTable;
        return tail == null || tail.length() == 0 ? head : head + tail;
    }

    /** The values an insert binds, in the order {@link #getInsertSql} names them. */
    Object[] insertParams(Object entity) {
        Object[] out = new Object[insertColumns.length];
        for(int iter = 0 ; iter < insertColumns.length ; iter++) {
            out[iter] = definition.get(entity, insertColumns[iter]);
        }
        return out;
    }

    /** The update statement, or null when the entity has nothing but a key. */
    String getUpdateSql() {
        return updateSql;
    }

    /** The values an update binds: every non-key column, then the key. */
    Object[] updateParams(Object entity) {
        Object[] out = new Object[updateColumns.length + 1];
        for(int iter = 0 ; iter < updateColumns.length ; iter++) {
            out[iter] = definition.get(entity, updateColumns[iter]);
        }
        out[updateColumns.length] = definition.get(entity, idIndex);
        return out;
    }

    /** Writes a generated key back into the entity. */
    void setId(Object entity, long key) throws IOException {
        definition.set(entity, idIndex, Long.valueOf(key));
    }

    /** The key of {@code entity}, as a bound parameter. */
    Object idOf(Object entity) {
        return definition.get(entity, idIndex);
    }

    /** One row as an entity. */
    Object read(Map row) throws IOException {
        Object entity = definition.newInstance();
        for(int iter = 0 ; iter < columns.length ; iter++) {
            definition.set(entity, iter, lookup(row, columns[iter].getColumn()));
        }
        return entity;
    }

    /** Every row as an entity, in order. */
    List readAll(List rows, List out) throws IOException {
        for(int iter = 0 ; iter < rows.size() ; iter++) {
            Object row = rows.get(iter);
            if(row instanceof Map) {
                out.add(read((Map)row));
            }
        }
        return out;
    }

    /**
     * A column's value out of a row.
     *
     * <p>By exact name first, because that is what quoting the identifiers buys.
     * The case-insensitive sweep behind it is for the table this entity did not
     * create: a column an older migration made unquoted is createdat on
     * PostgreSQL however the entity spells it, and an entity that could not read
     * its own existing table would be a poor way to adopt this.
     */
    private static Object lookup(Map row, String column) {
        if(row.containsKey(column)) {
            return row.get(column);
        }
        Iterator keys = row.keySet().iterator();
        while(keys.hasNext()) {
            Object key = keys.next();
            if(key instanceof String && ((String)key).equalsIgnoreCase(column)) {
                return row.get(key);
            }
        }
        return null;
    }

    /** Everything after the column name in CREATE TABLE. */
    private String declaration(ColumnDefinition column) {
        if(column.isId()) {
            if(column.isGenerated()) {
                return dialect.generatedKeyColumn(column.getKind());
            }
            if(column.getDeclaredType() != null) {
                // NOT NULL for the reason Dialect#assignedKeyColumn gives: SQLite
                // admits a null primary key that the other two refuse.
                return column.getDeclaredType() + " NOT NULL PRIMARY KEY";
            }
            return dialect.assignedKeyColumn(column.getKind());
        }
        String type = column.getDeclaredType() != null
                ? column.getDeclaredType() : dialect.columnType(column.getKind());
        return column.isNullable() ? type : type + " NOT NULL";
    }

    private int[] indicesExcept(int skip) {
        int count = skip < 0 ? columns.length : columns.length - 1;
        int[] out = new int[count];
        int at = 0;
        for(int iter = 0 ; iter < columns.length ; iter++) {
            if(iter != skip) {
                out[at++] = iter;
            }
        }
        return out;
    }
}
