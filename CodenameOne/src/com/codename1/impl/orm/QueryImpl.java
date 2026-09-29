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
@com.codename1.impl.SharedWithBackend
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
    private boolean resolvingOrder;
    private static final class Join {
        final EntityModel model;
        final String alias;
        String sql;
        boolean left;
        boolean requiredForCount = true;
        boolean plural;
        Join(EntityModel model, String alias, String sql) {
            this.model = model;
            this.alias = alias;
            this.sql = sql;
        }
    }
    /// Adds an inner join on a mapped relationship path.
    @Override
    public QueryImpl<T> join(String path) {
        validatePath(path, false);
        ensureJoin(path, false);
        return this;
    }
    /// Adds a left join on a mapped relationship path before using it in a predicate.
    @Override
    public QueryImpl<T> leftJoin(String path) {
        validatePath(path, false);
        ensureJoin(path, true);
        return this;
    }
    void fetchJoin(String field, boolean left) {
        Relationship relation = model.relationships()[model.queryRelationIndex(field)];
        if (!relation.element) {
            ensureJoin(field, left);
        } else if (!joins.containsKey(field)) {
            String alias = rootAlias + "_j" + (joins.size() + 1);
            String sql = (left ? " LEFT JOIN " : " INNER JOIN ") + session.q(relation.joinTable) + " " + alias
                    + " ON " + session.joinEquality(session.joinColumns(relation.joinColumn, model), alias,
                            session.keyColumns(model), rootAlias);
            Join join = new Join(model, alias, sql);
            join.plural = true;
            joins.put(field, join);
            pluralJoin = true;
        }
    }
    private final List<String> fetches = new ArrayList<String>();
    /// Overrides mapping laziness for the named direct relationship.
    @Override
    public QueryImpl<T> fetch(String field) {
        model.queryRelationIndex(field);
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
        Relationship relation = model.relationships()[model.queryRelationIndex(field)];
        if (!relation.element) {
            throw new IllegalArgumentException("Not an element collection: " + field);
        }
        if (value != null && !relation.target.isInstance(value)) {
            throw new IllegalArgumentException("Wrong element type");
        }
        Object converted = Values.storage(value);
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
                .append(converted == null ? " IS NULL)" : " = ?)");
        if (converted != null) {
            params.add(converted);
        }
        return this;
    }
    @Override
    public QueryImpl<T> eq(String field, Object value) {
        return compare(field, "=", value);
    }
    @Override
    public QueryImpl<T> ne(String field, Object value) {
        return compare(field, "<>", value);
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
        if (kind(field) != Attribute.TEXT) {
            throw new IllegalArgumentException("LIKE requires text storage: " + field);
        }
        Object converted = builderParameter(field, pattern);
        if (converted != null && !(converted instanceof String)) {
            throw new IllegalArgumentException("LIKE requires a text pattern after conversion");
        }
        String normalized = session.likePattern((String) converted, null);
        String name = column(field);
        conjunction();
        predicates.append(name).append(session.likeOperator(false));
        params.add(normalized);
        return this;
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
        SessionImpl.checkParameterCount(params.size() + values.length);

        Object[] converted = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            converted[i] = builderParameter(field, values[i]);
        }
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
            params.add(converted[i]);
        }
        predicates.append(')');
        return this;
    }
    @Override
    public QueryImpl<T> orderBy(String field, boolean ascending) {
        resolvingOrder = true;
        try {
            return appendOrder(field, ascending);
        } finally {
            resolvingOrder = false;
        }
    }
    private QueryImpl<T> appendOrder(String field, boolean ascending) {
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
        if (pluralJoin && relationOrdering) {
            throw new IllegalArgumentException("Collection joins require ordering by a root field");
        }
        session.autoFlush();
        if (max == 0) {
            return new ArrayList<T>();
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
        String statement = "SELECT COUNT(*)" + from(true) + where();
        boolean distinct = false;
        for (Join join : joins.values()) {
            distinct |= join.requiredForCount && join.plural;
        }
        if (distinct) {
            StringBuilder keys = new StringBuilder();
            for (String column : session.keyColumns(model)) {
                if (keys.length() > 0) {
                    keys.append(", ");
                }
                keys.append(rootAlias).append('.').append(session.q(column));
            }
            statement = "SELECT COUNT(*) FROM (SELECT DISTINCT " + keys + from(true) + where() + ") cn1_count";
        }
        List<Object[]> rows = session.read(statement, params.toArray(), new int[] {Attribute.BIGINT});
        return ((Number) rows.get(0)[0]).longValue();
    }
    private QueryImpl<T> compare(String field, String op, Object value) {
        boolean unary = op.startsWith("IS ");
        Object converted = builderParameter(field, value);
        if (unary && converted != null) {
            op = "IS NULL".equals(op) ? "=" : "<>";
            unary = false;
        }
        if (!unary && converted == null) {
            if ("=".equals(op) || "<>".equals(op)) {
                op = "=".equals(op) ? "IS NULL" : "IS NOT NULL";
                unary = true;
            } else {
                throw new IllegalArgumentException("Null comparison requires isNull/isNotNull");
            }
        }
        String name = column(field);
        if (">".equals(op) || "<".equals(op) || ">=".equals(op) || "<=".equals(op)) {
            name = session.orderValue(name, kind(field));
        }
        conjunction();
        predicates.append(name).append(' ').append(op);
        if (!unary) {
            predicates.append(" ?");
            params.add(converted);
        }
        return this;
    }
    private static final class Field {
        final Join join;
        final int index;
        Field(Join join, int index) {
            this.join = join;
            this.index = index;
        }
    }
    private Field validatePath(String path, boolean attribute) {
        EntityModel current = model;
        String remaining = path;
        while (true) {
            if (attribute) {
                for (Attribute candidate : current.attributes()) {
                    if (candidate.field.equals(remaining)) {
                        return new Field(new Join(current, "", ""), current.queryIndex(remaining));
                    }
                }
            }
            int dot = remaining.indexOf('.');
            if (attribute && dot < 0) {
                throw new IllegalArgumentException("Unknown attribute: " + path);
            }
            String field = dot < 0 ? remaining : remaining.substring(0, dot);
            Relationship relation = current.relationships()[current.queryRelationIndex(field)];
            if (relation.element) {
                throw new IllegalArgumentException("Use containsElement() for scalar collections");
            }
            current = session.model(relation.target);
            if (dot < 0) {
                return null;
            }
            remaining = remaining.substring(dot + 1);
        }
    }
    private Field resolveField(String path) {
        // Validate the complete path before creating joins or promoting existing
        // ordering-only joins into predicates/counts.
        validatePath(path, true);
        Join join = new Join(model, rootAlias, "");
        String remaining = path;
        int offset = 0;
        while (true) {
            Attribute[] attributes = join.model.attributes();
            for (Attribute attribute : attributes) {
                if (attribute.field.equals(remaining)) {
                    return new Field(join, join.model.queryIndex(remaining));
                }
            }
            int dot = remaining.indexOf('.');
            if (dot < 0) {
                throw new IllegalArgumentException("Unknown attribute: " + path);
            }
            offset += dot;
            join = ensureJoin(path.substring(0, offset), false);
            offset++;
            remaining = path.substring(offset);
        }
    }
    boolean association(String field) {
        Field resolved = resolveField(field);
        String name = resolved.join.model.attributes()[resolved.index].field;
        for (Relationship relation : resolved.join.model.relationships()) {
            if (relation.field.equals(name)) {
                return true;
            }
        }
        return false;
    }
    private Object builderParameter(String field, Object value) {
        Object converted = parameter(field, value);
        boolean mappedBoolean = mapping(field).startsWith("converter:")
                && (Long.valueOf(0).equals(converted) || Long.valueOf(1).equals(converted));
        if (converted != null && kind(field) == Attribute.BOOLEAN && !(value instanceof Boolean)
                && !mappedBoolean) {
            throw new IllegalArgumentException("Boolean predicate requires a Boolean: " + field);
        }
        Values.requireStorageKind(converted, kind(field));
        requireIntegralRange(field, converted);
        return converted;
    }
    Object parameter(String field, Object value) {
        Field resolved = validatePath(field, true);
        return resolved.join.model.parameter(resolved.index, value);
    }
    void requireIntegralRange(String field, Object value) {
        Field resolved = validatePath(field, true);
        EntityModel target = resolved.join.model;
        if (target.attributes()[resolved.index].kind == Attribute.INTEGER && value instanceof Number) {
            long minimum = target.minimumIntegralValue(resolved.index);
            long maximum = target.maximumIntegralValue(resolved.index);
            double number = ((Number) value).doubleValue();
            if (!(number >= minimum && number <= maximum)) {
                throw new IllegalArgumentException("Parameter is outside the mapped integral range: " + field);
            }
        }
    }
    boolean nonNull(String field) {
        Field resolved = resolveField(field);
        return !resolved.join.left && resolved.join.model.nonNullQueryValue(resolved.index);
    }
    String mapping(String field) {
        Field resolved = validatePath(field, true);
        return resolved.join.model.mapping(resolved.index);
    }
    Object project(String field, Object value) {
        Field resolved = validatePath(field, true);
        return resolved.join.model.project(resolved.index, value);
    }
    String column(String field) {
        Field resolved = resolveField(field);
        return resolved.join.alias + "." + session.q(resolved.join.model.attributes()[resolved.index].column);
    }
    private Join ensureJoin(String path, boolean left) {
        Join existing = joins.get(path);
        if (existing != null) {
            if (!resolvingOrder) {
                existing.requiredForCount = true;
                int parent = path.lastIndexOf('.');
                if (parent >= 0) {
                    ensureJoin(path.substring(0, parent), left);
                }
            }
            if (!left || existing.left) {
                return existing;
            }
        }
        int dot = path.lastIndexOf('.');
        Join parent = dot < 0 ? new Join(model, rootAlias, "") : ensureJoin(path.substring(0, dot), left);
        String field = dot < 0 ? path : path.substring(dot + 1);
        Relationship relation = parent.model.relationships()[parent.model.queryRelationIndex(field)];
        if (relation.element) {
            throw new IllegalArgumentException("Use containsElement() for scalar collections");
        }
        EntityModel target = session.model(relation.target);
        String alias = existing == null ? rootAlias + "_j" + (joins.size() + 1) : existing.alias;
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
        if (existing != null) {
            existing.sql = join;
            existing.left = left;
            return existing;
        }
        Join created = new Join(target, alias, join);
        created.left = left;
        created.requiredForCount = !resolvingOrder;
        created.plural = relation.many;
        joins.put(path, created);
        return created;
    }
    boolean readsTable(String table) {
        if (model.table().equalsIgnoreCase(table)) {
            return true;
        }
        for (Join join : joins.values()) {
            if (join.model.table().equalsIgnoreCase(table)) {
                return true;
            }
        }
        return false;
    }
    String from() {
        return from(false);
    }
    private String from(boolean counting) {
        StringBuilder result =
                new StringBuilder(" FROM ").append(session.tableSource(model)).append(' ').append(rootAlias);
        for (Join join : joins.values()) {
            if (!counting || join.requiredForCount) {
                result.append(join.sql);
            }
        }
        return result.toString();
    }
    int kind(String field) {
        Field resolved = validatePath(field, true);
        return resolved.join.model.attributes()[resolved.index].kind;
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
