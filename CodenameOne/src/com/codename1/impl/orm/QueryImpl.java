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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Parameterized query using Java attribute names rather than SQL identifiers.
///
/// Internal ORM runtime; not an application API.
/// @hidden
public final class QueryImpl<T> implements com.codename1.orm.session.Query<T> {
    final SessionImpl session;
    final EntityModel<T> model;
    final String rootAlias;
    private final List<Object> params = new ArrayList<Object>();
    // Each builder owns its SQL buffers for the lifetime of that query; this
    // retained state is required to append predicates and ordering incrementally.
    @SuppressWarnings("PMD.AvoidStringBufferField") private final StringBuilder predicates = new StringBuilder();
    @SuppressWarnings("PMD.AvoidStringBufferField") private final StringBuilder order = new StringBuilder();
    private int limit = -1;
    private int offset;
    private final Map<String, Join> joins = new LinkedHashMap<String, Join>();
    private boolean pluralJoin;
    private boolean relationOrdering;
    private static final class Join {
        final EntityModel model;
        final String alias;
        final String sql;
        Join(EntityModel model, String alias, String sql) {
            this.model = model;
            this.alias = alias;
            this.sql = sql;
        }
    }
    /// Adds an inner join on a mapped relationship path.
    @Override
    public QueryImpl<T> join(String path) {
        ensureJoin(path, false);
        return this;
    }
    /// Adds a left join on a mapped relationship path before using it in a predicate.
    @Override
    public QueryImpl<T> leftJoin(String path) {
        ensureJoin(path, true);
        return this;
    }
    private final List<String> fetches = new ArrayList<String>();
    /// Overrides mapping laziness for the named direct relationship.
    @Override
    public QueryImpl<T> fetch(String field) {
        model.relationIndex(field);
        fetches.add(field);
        return this;
    }
    QueryImpl(SessionImpl session, EntityModel<T> model) {
        this(session, model, session.nextAlias());
    }
    QueryImpl(SessionImpl session, EntityModel<T> model, String rootAlias) {
        this.session = session;
        this.model = model;
        this.rootAlias = rootAlias;
    }
    /// Tests membership in an owned scalar collection without loading it.
    @Override
    public QueryImpl<T> containsElement(String field, Object value) {
        Relationship relation = model.relationships()[model.relationIndex(field)];
        if (!relation.element) {
            throw new IllegalArgumentException("Not an element collection: " + field);
        }
        String alias = session.nextAlias();
        conjunction();
        predicates.append("EXISTS (SELECT 1 FROM ")
                .append(session.q(relation.joinTable))
                .append(' ')
                .append(alias)
                .append(" WHERE ")
                .append(session.joinEquality(
                        session.joinColumns(relation.joinColumn, model), alias, session.keyColumns(model), rootAlias))
                .append(" AND ")
                .append(alias)
                .append('.')
                .append(session.q(relation.inverseJoinColumn))
                .append(value == null ? " IS NULL)" : " = ?)");
        if (value != null) {
            if (!relation.target.isInstance(value)) {
                throw new IllegalArgumentException("Wrong element type");
            }
            params.add(Values.storage(value));
        }
        return this;
    }
    @Override
    public QueryImpl<T> eq(String field, Object value) {
        return compare(field, value == null ? "IS NULL" : "=", value);
    }
    @Override
    public QueryImpl<T> ne(String field, Object value) {
        return compare(field, value == null ? "IS NOT NULL" : "<>", value);
    }
    @Override
    public QueryImpl<T> gt(String field, Object value) {
        return compare(field, ">", value);
    }
    @Override
    public QueryImpl<T> ge(String field, Object value) {
        return compare(field, ">=", value);
    }
    @Override
    public QueryImpl<T> lt(String field, Object value) {
        return compare(field, "<", value);
    }
    @Override
    public QueryImpl<T> le(String field, Object value) {
        return compare(field, "<=", value);
    }
    @Override
    public QueryImpl<T> like(String field, String pattern) {
        return compare(field, "LIKE", pattern);
    }
    @Override
    public QueryImpl<T> isNull(String field) {
        return compare(field, "IS NULL", null);
    }
    @Override
    public QueryImpl<T> isNotNull(String field) {
        return compare(field, "IS NOT NULL", null);
    }
    @Override
    public QueryImpl<T> in(String field, Object... values) {
        String name = column(field);
        conjunction();
        if (values.length == 0) {
            predicates.append("1 = 0");
            return this;
        }
        predicates.append(name).append(" IN (");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                predicates.append(", ");
            }
            predicates.append('?');
            params.add(parameter(field, values[i]));
        }
        predicates.append(')');
        return this;
    }
    @Override
    public QueryImpl<T> orderBy(String field, boolean ascending) {
        String name = column(field);
        // Embedded paths still resolve to the root alias. A joined column
        // remains non-root even when its join was created by an earlier clause.
        if (!name.startsWith(rootAlias + ".")) {
            relationOrdering = true;
        }
        if (order.length() > 0) {
            order.append(", ");
        }
        order.append(session.orderBy(name, ascending, kind(field)));
        return this;
    }
    @Override
    public QueryImpl<T> limit(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative limit");
        }
        limit = value;
        return this;
    }
    @Override
    public QueryImpl<T> offset(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative offset");
        }
        offset = value;
        return this;
    }
    @Override
    public List<T> list() {
        return list(limit);
    }
    @Override
    public T first() {
        List<T> rows = list(limit == 0 ? 0 : 1);
        return rows.isEmpty() ? null : rows.get(0);
    }
    private List<T> list(int max) {
        session.autoFlush();
        if (max == 0) {
            return new ArrayList<T>();
        }
        if (pluralJoin && relationOrdering) {
            throw new IllegalArgumentException("Collection joins require ordering by a root field");
        }
        StringBuilder selection = new StringBuilder("SELECT ").append(pluralJoin ? "DISTINCT " : "");
        Attribute[] attributes = model.attributes();
        for (int i = 0; i < attributes.length; i++) {
            if (i > 0) {
                selection.append(", ");
            }
            String column = rootAlias + "." + session.q(attributes[i].column);
            selection.append(pluralJoin ? session.orderValue(column, attributes[i].kind) : column);
        }
        String sql = selection + from() + where() + (order.length() == 0 ? "" : " ORDER BY " + order) +
                     session.limit(max, offset);
        return session.hydrateAll(model, session.read(sql, params.toArray(), session.kinds(model)), fetches);
    }
    /// Counts matching roots, ignoring pagination and ordering.
    @Override
    public long count() {
        session.autoFlush();
        String statement = "SELECT COUNT(*)" + from() + where();
        if (pluralJoin) {
            StringBuilder keys = new StringBuilder();
            for (String column : session.keyColumns(model)) {
                if (keys.length() > 0) {
                    keys.append(", ");
                }
                keys.append(rootAlias).append('.').append(session.q(column));
            }
            statement = "SELECT COUNT(*) FROM (SELECT DISTINCT " + keys + from() + where() + ") cn1_count";
        }
        List<Object[]> rows = session.read(statement, params.toArray(), new int[] {Attribute.BIGINT});
        return ((Number) rows.get(0)[0]).longValue();
    }
    private QueryImpl<T> compare(String field, String op, Object value) {
        String name = column(field);
        boolean unary = op.startsWith("IS ");
        if (!unary && value == null) {
            throw new IllegalArgumentException("Null comparison requires isNull/isNotNull");
        }
        conjunction();
        predicates.append(name).append(' ').append(op);
        if (!unary) {
            predicates.append(" ?");
            params.add(parameter(field, value));
        }
        return this;
    }
    Object parameter(String field, Object value) {
        for (Attribute attribute : model.attributes()) {
            if (attribute.field.equals(field)) {
                return model.parameter(model.index(field), value);
            }
        }
        int dot = field.lastIndexOf('.');
        EntityModel target = dot < 0 ? model : ensureJoin(field.substring(0, dot), false).model;
        return target.parameter(target.index(dot < 0 ? field : field.substring(dot + 1)), value);
    }
    String column(String field) {
        for (Attribute attribute : model.attributes()) {
            if (attribute.field.equals(field)) {
                return rootAlias + "." + session.q(attribute.column);
            }
        }
        int dot = field.lastIndexOf('.');
        if (dot < 0) {
            return rootAlias + "." + session.q(model.attributes()[model.index(field)].column);
        }
        Join join = ensureJoin(field.substring(0, dot), false);
        return join.alias + "." + session.q(join.model.attributes()[join.model.index(field.substring(dot + 1))].column);
    }
    private Join ensureJoin(String path, boolean left) {
        Join existing = joins.get(path);
        if (existing != null) {
            return existing;
        }
        int dot = path.lastIndexOf('.');
        Join parent = dot < 0 ? new Join(model, rootAlias, "") : ensureJoin(path.substring(0, dot), left);
        String field = dot < 0 ? path : path.substring(dot + 1);
        Relationship relation = parent.model.relationships()[parent.model.relationIndex(field)];
        if (relation.element) {
            throw new IllegalArgumentException("Use containsElement() for scalar collections");
        }
        EntityModel target = session.model(relation.target);
        String alias = rootAlias + "_j" + (joins.size() + 1);
        String prefix = left ? " LEFT JOIN " : " INNER JOIN ";
        String join;
        if (relation.column >= 0) {
            join = prefix + session.tableSource(target) + " " + alias + " ON " +
                   session.joinEquality(session.keyColumns(target), alias,
                           session.foreignColumns(parent.model, relation), parent.alias);
        } else {
            pluralJoin |= relation.many;
            Relationship owning = relation;
            boolean inverse = relation.mappedBy.length() > 0;
            if (inverse) {
                owning = target.relationships()[target.relationIndex(relation.mappedBy)];
            }
            if (owning.column >= 0) {
                join = prefix + session.tableSource(target) + " " + alias + " ON " +
                       session.joinEquality(session.foreignColumns(target, owning), alias,
                               session.keyColumns(parent.model), parent.alias);
            } else {
                String link = alias + "_link";
                String ownerColumn = inverse ? owning.inverseJoinColumn : owning.joinColumn;
                String targetColumn = inverse ? owning.joinColumn : owning.inverseJoinColumn;
                join = prefix + session.q(owning.joinTable) + " " + link + " ON " +
                       session.joinEquality(session.joinColumns(ownerColumn, parent.model), link,
                               session.keyColumns(parent.model), parent.alias) +
                       prefix + session.tableSource(target) + " " + alias + " ON " +
                       session.joinEquality(
                               session.keyColumns(target), alias, session.joinColumns(targetColumn, target), link);
            }
        }
        Join created = new Join(target, alias, join);
        joins.put(path, created);
        return created;
    }
    String from() {
        StringBuilder result =
                new StringBuilder(" FROM ").append(session.tableSource(model)).append(' ').append(rootAlias);
        for (Join join : joins.values()) {
            result.append(join.sql);
        }
        return result.toString();
    }
    int kind(String field) {
        for (Attribute attribute : model.attributes()) {
            if (attribute.field.equals(field)) {
                return attribute.kind;
            }
        }
        int dot = field.lastIndexOf('.');
        EntityModel target = dot < 0 ? model : ensureJoin(field.substring(0, dot), false).model;
        return target.attributes()[target.index(dot < 0 ? field : field.substring(dot + 1))].kind;
    }
    String rootColumns() {
        return rootColumns(false);
    }
    String rootColumns(boolean distinct) {
        StringBuilder result = new StringBuilder();
        for (Attribute a : model.attributes()) {
            if (result.length() > 0) {
                result.append(", ");
            }
            String column = rootAlias + "." + session.q(a.column);
            result.append(distinct ? session.orderValue(column, a.kind) : column);
        }
        return result.toString();
    }
    private void conjunction() {
        if (predicates.length() > 0) {
            predicates.append(" AND ");
        }
    }
    private String where() {
        return predicates.length() == 0 ? "" : " WHERE " + predicates;
    }
}
