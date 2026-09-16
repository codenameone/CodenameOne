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
                // IGNORING CASE, because the strictest engine decides: SQLite
                // and MySQL treat "name" and "NAME" as one column even when both
                // are quoted, while PostgreSQL keeps them apart. A mapping only
                // PostgreSQL accepts is not a portable mapping.
                //
                // equalsIgnoreCase, never toLowerCase: this runtime has no
                // Locale to ask for the root one, and on a Turkish device an I
                // folds to a dotless i.
                if(columns[earlier].getColumn().equalsIgnoreCase(columns[iter].getColumn())) {
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
            create.append(quotedColumns[iter]).append(' ')
                    .append(declaration(columns[iter], quotedColumns[iter]));
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

    /**
     * A key as the column stores it, refusing one the column cannot hold.
     *
     * <p>findById and deleteById take an Object, and until this they bound it
     * without consulting the key's kind -- so findById("abc") on an integer key
     * made PostgreSQL reject the text while MySQL coerced it to 0 and answered
     * the row whose id is 0. deleteById is the same call and DELETES that row.
     * The query predicates refuse this now, and the two lookups that take a bare
     * key are where it matters most.
     */
    Object checkedKey(Object id) {
        Object canonical = Query.bound(id);
        int kind = columns[idIndex].getKind();
        if(canonical == null || Query.fits(kind, canonical)) {
            return canonical;
        }
        throw new IllegalArgumentException(definition.type().getName() + " has a "
                + dialect.columnType(kind) + " key and cannot be looked up with "
                + id.getClass().getName() + ". PostgreSQL refuses the comparison and "
                + "MySQL coerces it, so the same call throws on one engine and answers "
                + "or deletes the wrong row on another.");
    }

    /** The quoted table name, for the few statements built outside this class. */
    String quotedTable() {
        return quotedTable;
    }

    /** The quoted key column, for the same reason. */
    String quotedId() {
        return quoted(idIndex);
    }

    /** The values an insert binds, in the order {@link #getInsertSql} names them. */
    Object[] insertParams(Object entity) throws IOException {
        Object[] out = new Object[insertColumns.length];
        for(int iter = 0 ; iter < insertColumns.length ; iter++) {
            Object raw = definition.get(entity, insertColumns[iter]);
            // The key is among these only when the application assigns it,
            // which is exactly the case key() has something to say about; every
            // other column still goes through the shared value check.
            out[iter] = insertColumns[iter] == idIndex
                    ? key(raw)
                    : value(raw, columns[insertColumns[iter]].getField());
        }
        return out;
    }

    /** The update statement, or null when the entity has nothing but a key. */
    String getUpdateSql() {
        return updateSql;
    }

    /** The values an update binds: every non-key column, then the key. */
    Object[] updateParams(Object entity) throws IOException {
        Object[] out = new Object[updateColumns.length + 1];
        for(int iter = 0 ; iter < updateColumns.length ; iter++) {
            out[iter] = value(definition.get(entity, updateColumns[iter]),
                    columns[updateColumns[iter]].getField());
        }
        out[updateColumns.length] = key(definition.get(entity, idIndex));
        return out;
    }

    /** Writes a generated key back into the entity. */
    void setId(Object entity, long key) throws IOException {
        definition.set(entity, idIndex, Long.valueOf(key));
    }

    /**
     * The longest an ASSIGNED text key may be, on every engine.
     *
     * <p>MySQL cannot index an unbounded TEXT column -- it needs a prefix length
     * -- so a string key has to be declared VARCHAR(n) there, while SQLite and
     * PostgreSQL leave it unbounded. Without a shared bound the same entity
     * stores a 256-character key on two engines and fails on the third with
     * "Data too long", which is the one thing this layer exists to prevent. So
     * the tightest engine's limit is the limit everywhere, and it is enforced
     * here rather than left to whichever server the deployment happens to use.
     *
     * <p>A column with an explicit {@code @Column(type)} is exempt: the
     * developer named the type, it is written through unchanged on all three,
     * and the bound is theirs to know.
     */
    static final int MAX_ASSIGNED_TEXT_KEY = 255;

    /**
     * {@code value} as a bound column value, or a refusal when the three engines
     * would not agree about it.
     *
     * <p>A NUL inside a string is the case: SQLite and MySQL store it happily --
     * both keep text with a length rather than a terminator -- while PostgreSQL
     * refuses the row outright with "invalid byte sequence for encoding UTF8:
     * 0x00", because its text type cannot hold a zero byte at all. So an entity
     * that inserted through a whole SQLite development cycle failed the first
     * time it met the production database. Refused on every engine instead, at
     * the point the value is bound, so the answer does not depend on which one
     * is behind it.
     *
     * <p>A blob is untouched: a byte[] is exactly where a NUL belongs, and all
     * three store one. It is TEXT that cannot carry it.
     */
    Object value(Object value, String field) throws IOException {
        if(value instanceof String && ((String)value).indexOf(0) >= 0) {
            throw new IOException("The value for " + field + " holds a NUL, which "
                    + "PostgreSQL cannot store in a text column at all -- SQLite and MySQL "
                    + "would take it, so this row would insert in development and fail in "
                    + "production. Strip it, or declare the field as byte[], where a NUL is "
                    + "an ordinary byte on every engine.");
        }
        return value;
    }

    /**
     * {@code value} as a bound key, or a refusal when no engine pair could agree
     * about it. See {@link #MAX_ASSIGNED_TEXT_KEY}.
     */
    Object key(Object value) throws IOException {
        value = value(value, columns[idIndex].getField());
        if(value instanceof String
                && !columns[idIndex].isGenerated()
                && columns[idIndex].getDeclaredType() == null
                && ((String)value).length() > MAX_ASSIGNED_TEXT_KEY) {
            throw new IOException("A key of " + ((String)value).length() + " characters is "
                    + "longer than the " + MAX_ASSIGNED_TEXT_KEY + " this ORM stores on every "
                    + "engine: MySQL cannot index an unbounded text column, so the key column "
                    + "is VARCHAR(" + MAX_ASSIGNED_TEXT_KEY + ") there and the row would be "
                    + "refused after working in development. Shorten the key, or declare the "
                    + "column with @Column(type = \"...\") and size it yourself.");
        }
        return value;
    }

    /** The key of {@code entity}, as a bound parameter. */
    Object idOf(Object entity) throws IOException {
        return key(definition.get(entity, idIndex));
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
    private String declaration(ColumnDefinition column, String quotedColumn) {
        if(column.isId()) {
            if(column.isGenerated()) {
                // The DECLARED TYPE IS NOT CONSULTED, and that is not an
                // oversight: a generated key's declaration is one indivisible
                // form per engine -- SQLite's AUTOINCREMENT is legal only after
                // the exact words INTEGER PRIMARY KEY -- so there is nowhere to
                // put an arbitrary type. @Column(type) on an autoIncrement @Id
                // is refused by the annotation processor rather than discarded
                // here, so this branch is only ever reached without one.
                // The QUOTED NAME travels with it so SQLite can bound an int key
                // to the Java int range; see Dialect#generatedKeyColumn(int, String).
                return dialect.generatedKeyColumn(column.getKind(), quotedColumn);
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
