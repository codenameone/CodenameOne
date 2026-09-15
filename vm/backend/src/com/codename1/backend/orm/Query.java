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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A query named in the entity's own terms.
 *
 * <pre>
 *   List&lt;Note&gt; recent = notes.query()
 *           .eq("author", author)
 *           .gt("created", cutoff)
 *           .orderBy("created", false)
 *           .limit(20)
 *           .list();
 * </pre>
 *
 * <p>Every name here is a JAVA FIELD of the entity, which is what makes the query
 * portable: the builder maps it to the column the entity declared and quotes it
 * for the engine, so a field called createdAt works on PostgreSQL, which folds an
 * unquoted name to lower case, exactly as it works on the other two. A name that
 * is not a field of this entity is refused immediately, naming the ones that are,
 * rather than reaching the server as a column it does not have.
 *
 * <p>Values are bound, never interpolated. A Date becomes epoch milliseconds and a
 * boolean becomes 0 or 1, which is how an entity stores them, so what the query
 * compares is what the table holds.
 *
 * <p>Conditions are joined with AND, in the order they were added. OR and grouping
 * are deliberately absent: the moment a query needs them it is past what naming
 * fields expresses clearly, and {@link Dao#find} takes the SQL that says it.
 */
public final class Query<T> {
    private final Dao<T> dao;
    private final Table table;
    private final StringBuilder where = new StringBuilder();
    private final List params = new ArrayList();
    private String order = "";
    private int limit = -1;
    private int offset;

    Query(Dao<T> dao, Table table) {
        this.dao = dao;
        this.table = table;
    }

    /** field = value, or field IS NULL when the value is null. */
    public Query<T> eq(String field, Object value) {
        return value == null ? isNull(field) : condition(field, " = ?", value);
    }

    /** field &lt;&gt; value, or field IS NOT NULL when the value is null. */
    public Query<T> ne(String field, Object value) {
        return value == null ? isNotNull(field) : condition(field, " <> ?", value);
    }

    /** field &gt; value. */
    public Query<T> gt(String field, Object value) {
        return condition(field, " > ?", required(field, value));
    }

    /** field &gt;= value. */
    public Query<T> gte(String field, Object value) {
        return condition(field, " >= ?", required(field, value));
    }

    /** field &lt; value. */
    public Query<T> lt(String field, Object value) {
        return condition(field, " < ?", required(field, value));
    }

    /** field &lt;= value. */
    public Query<T> lte(String field, Object value) {
        return condition(field, " <= ?", required(field, value));
    }

    /**
     * field LIKE pattern, with the engine's own wildcards: % for any run of
     * characters and _ for one. The pattern is BOUND rather than pasted, so a
     * value holding a quote is a value and not a statement.
     */
    public Query<T> like(String field, String pattern) {
        return condition(field, " LIKE ?", required(field, pattern));
    }

    /** field IS NULL. */
    public Query<T> isNull(String field) {
        return raw(column(field) + " IS NULL");
    }

    /** field IS NOT NULL. */
    public Query<T> isNotNull(String field) {
        return raw(column(field) + " IS NOT NULL");
    }

    /**
     * field IN (...). An empty or absent set matches nothing, which it says as a
     * condition that is false rather than by dropping the clause -- a filter that
     * silently disappears returns every row in the table.
     */
    public Query<T> in(String field, Object[] values) {
        String quoted = column(field);
        if(values == null || values.length == 0) {
            return raw("1 = 0");
        }
        StringBuilder clause = new StringBuilder(quoted);
        clause.append(" IN (");
        for(int iter = 0 ; iter < values.length ; iter++) {
            clause.append(iter > 0 ? ", ?" : "?");
            params.add(bound(values[iter]));
        }
        clause.append(")");
        return raw(clause.toString());
    }

    /** Orders by a field, ascending or descending. Several calls order by each in turn. */
    public Query<T> orderBy(String field, boolean ascending) {
        String quoted = column(field);
        order = order.length() == 0 ? " ORDER BY " : order + ", ";
        // The DIALECT renders the term, because where a null sorts is not the
        // same on the three by default and MySQL cannot even spell the standard
        // way of saying it. See Dialect.orderBy.
        order = order + table.dialect.orderBy(quoted, ascending);
        return this;
    }

    /** At most this many rows. */
    public Query<T> limit(int count) {
        limit = count;
        return this;
    }

    /** Skips this many rows. Pair it with an order, or the rows skipped are arbitrary. */
    public Query<T> offset(int count) {
        offset = count;
        return this;
    }

    /** The matching rows, as entities. */
    public List<T> list() throws IOException {
        return dao.list(table.select(tail(true)), params());
    }

    /**
     * The first matching row, or null.
     *
     * <p>Asks the database for one row rather than reading them all and taking
     * the first: the limit is part of the statement.
     */
    public T first() throws IOException {
        int wanted = limit;
        limit = 1;
        try {
            List<T> found = list();
            return found.isEmpty() ? (T)null : found.get(0);
        } finally {
            limit = wanted;
        }
    }

    /**
     * How many rows match.
     *
     * <p>The ordering and the limit are left off, because neither changes an
     * answer that is one number -- and because MySQL refuses ORDER BY in some
     * places where a count is legal.
     */
    public long count() throws IOException {
        return dao.scalar(table.count(tail(false)), params());
    }

    /**
     * Deletes every matching row and answers how many.
     *
     * <p>ONE statement rather than a read followed by deletes, which is both
     * faster and atomic -- and which is why it is here rather than left to the
     * caller: written by hand it is a loop that can be interrupted halfway.
     *
     * <p>A query with NO conditions deletes the whole table, exactly as the SQL
     * it builds would. That is the reading with no surprises in it, and it is
     * what makes this the idiomatic way to empty a table in a test.
     *
     * <p>The ordering and the limit are not part of it: MySQL alone accepts a
     * LIMIT on a DELETE, so honouring one here would mean a query that behaves
     * differently on the engine it was not developed against.
     */
    public int delete() throws IOException {
        return dao.execute(table.delete(tail(false)), params());
    }

    /** The conditions as SQL, for a message or for a statement built around them. */
    public String toString() {
        return table.select(tail(true));
    }

    /** WHERE, ORDER BY and LIMIT, or as much of them as this query has. */
    private String tail(boolean ordered) {
        StringBuilder out = new StringBuilder();
        if(where.length() > 0) {
            out.append(" WHERE ").append(where);
        }
        if(ordered) {
            out.append(order);
            out.append(table.dialect.limit(limit, offset));
        }
        return out.toString();
    }

    private Object[] params() {
        return params.toArray();
    }

    private Query<T> condition(String field, String operator, Object value) {
        String quoted = column(field);
        params.add(bound(value));
        return raw(quoted + operator);
    }

    private Query<T> raw(String clause) {
        if(where.length() > 0) {
            where.append(" AND ");
        }
        where.append(clause);
        return this;
    }

    /**
     * The quoted column a field maps to.
     *
     * <p>An unknown name is a programming error rather than a condition to
     * handle, so it is unchecked -- and it names the fields that do exist,
     * because the usual cause is the column name being used where the field name
     * belongs.
     */
    private String column(String field) {
        int index = table.definition.indexOfField(field);
        if(index < 0) {
            StringBuilder known = new StringBuilder();
            ColumnDefinition[] columns = table.columns;
            for(int iter = 0 ; iter < columns.length ; iter++) {
                known.append(iter > 0 ? ", " : "").append(columns[iter].getField());
            }
            throw new IllegalArgumentException(table.definition.type().getName()
                    + " has no persisted field '" + field + "'. It has: " + known);
        }
        return table.quoted(index);
    }

    private static Object required(String field, Object value) {
        if(value == null) {
            throw new IllegalArgumentException("A comparison against " + field
                    + " needs a value; use isNull to look for rows that have none");
        }
        return value;
    }

    /**
     * A Java value as the entity stores it.
     *
     * <p>The same conversions the generated entity access performs, so a query
     * compares like with like: a Date is milliseconds in an integer column and a
     * boolean is 0 or 1, on every engine.
     */
    static Object bound(Object value) {
        if(value instanceof Date) {
            return Long.valueOf(((Date)value).getTime());
        }
        if(value instanceof Boolean) {
            return Long.valueOf(((Boolean)value).booleanValue() ? 1L : 0L);
        }
        if(value instanceof Character) {
            // THE CODE UNIT, because that is what the column holds: the
            // server-side mapping stores a char as a number rather than as
            // one-character text. Binding the text compared 120 with "x" and
            // matched nothing -- silently, since a query that finds no row is
            // an ordinary answer. findById and deleteById come through here too,
            // so an assigned character key had the same hole.
            return Long.valueOf(((Character)value).charValue());
        }
        return value;
    }
}
