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

import com.codename1.orm.session.PersistenceException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A parameterized query over generated entity metadata. Supports selects,
/// relationship joins, scalar projections, aggregates, predicate subqueries,
/// grouping, ordering and transactional bulk update/delete. Unsupported syntax
/// is rejected before any SQL is executed.
///
/// Internal ORM runtime; not an application API.
/// @hidden
public final class JpqlQueryImpl<T> implements com.codename1.orm.session.JpqlQuery<T> {
    private final SessionImpl session;
    private final Class<T> resultType;
    private final Plan plan;
    private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    private int limit = -1;
    private int offset;
    JpqlQueryImpl(SessionImpl session, String statement, Class<T> resultType) {
        if (statement == null || resultType == null) {
            throw new IllegalArgumentException("statement/resultType is null");
        }
        this.session = session;
        this.resultType = resultType;
        Parser parser =
                new Parser(session, lex(statement), new ArrayList<Object>(), new LinkedHashMap<String, Alias>());
        plan = parser.parse();
        parser.end();
    }
    @Override
    public JpqlQueryImpl<T> setParameter(String name, Object value) {
        boolean found = false;
        for (Object binding : plan.bindings) {
            if (binding instanceof Parameter && ((Parameter) binding).name.equals(name)
                    || binding instanceof LikeBinding && ((LikeBinding) binding).uses(name)) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown named parameter: " + name);
        }
        parameters.put(name, value);
        return this;
    }
    @Override
    public JpqlQueryImpl<T> limit(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative limit");
        }
        limit = value;
        return this;
    }
    @Override
    public JpqlQueryImpl<T> offset(int value) {
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
        List<T> values = list(limit == 0 ? 0 : 1);
        return values.isEmpty() ? null : values.get(0);
    }
    private List<T> list(int max) {
        if (plan.mutation) {
            throw new IllegalStateException("Use executeUpdate for bulk mutations");
        }
        Bound bound = arguments();
        session.autoFlush();
        List<T> result = new ArrayList<T>();
        if (max == 0) {
            return result;
        }
        List<Object[]> rows = session.read(bound.sql + session.limit(max, offset), bound.args, plan.kinds);
        if (plan.entity != null) {
            List entities = session.hydrateAll(plan.entity, rows, plan.fetches);
            for (Object entity : entities) {
                result.add(checked(entity));
            }
        } else {
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    Expr projection = plan.projections.get(i);
                    if (projection.projectionQuery != null) {
                        row[i] = projection.projectionQuery.project(projection.projectionField, row[i]);
                    } else if (plan.kinds[i] == Attribute.BOOLEAN) {
                        try {
                            row[i] = Values.asBooleanObject(row[i]);
                        } catch (java.io.IOException error) {
                            throw new PersistenceException("Invalid boolean projection", error);
                        }
                    }
                }
                result.add(checked(row.length == 1 ? row[0] : row));
            }
        }
        return result;
    }
    private T checked(Object value) {
        if (value != null && resultType != Object.class && !resultType.isInstance(value)) {
            throw new PersistenceException(
                    "Query result is " + value.getClass().getName() + ", expected " + resultType.getName());
        }
        return (T) value;
    }
    /// Bulk DML bypasses cascades/callbacks and clears the context after execution.
    @Override
    public int executeUpdate() {
        if (!plan.mutation) {
            throw new IllegalStateException("Not a bulk mutation");
        }
        if (limit >= 0 || offset > 0) {
            throw new IllegalArgumentException("Bulk mutation pagination is not supported");
        }
        Bound bound = arguments();
        session.requireTransaction();
        session.flush();
        int changed = session.write(bound.sql, bound.args);
        session.clear();
        return changed;
    }
    private static final class Bound {
        final String sql;
        final Object[] args;
        Bound(String sql, Object[] args) {
            this.sql = sql;
            this.args = args;
        }
    }
    private Bound arguments() {
        String template = plan.sql;
        if (template == null) {
            throw new IllegalStateException("Query has no SQL plan");
        }
        List<Object> values = new ArrayList<Object>();
        // Render each occurrence separately: dialect ordering and checked
        // arithmetic can repeat expressions containing the same bound value.
        StringBuilder statement = new StringBuilder();
        int position = 0;
        int start;
        while ((start = template.indexOf("/*cn1-bind-", position)) >= 0) {
            statement.append(template.substring(position, start));
            int end = template.indexOf("*/?", start);
            int index = Integer.parseInt(template.substring(start + 11, end));
            Object binding = plan.bindings.get(index);
            position = end + 3;
            statement.append("?");
            if (binding instanceof LiteralBinding && ((LiteralBinding) binding).value == null
                    && ((LiteralBinding) binding).query == null) {
                // Keep untyped, unmapped NULL valid in COUNT(NULL) and similar SQL.
                statement.setLength(statement.length() - 1);
                statement.append("NULL");
            } else if (binding instanceof LikeBinding) {
                LikeBinding like = (LikeBinding) binding;
                Object pattern = boundValue(like.pattern);
                Object escape = like.escaped ? boundValue(like.escape) : null;
                if (pattern != null && !(pattern instanceof String)
                        || like.escaped && (!(escape instanceof String) || ((String) escape).length() != 1)) {
                    throw new IllegalArgumentException("LIKE requires a string pattern and a one-character ESCAPE");
                }
                values.add(session.likePattern((String) pattern, (String) escape));
            } else if (binding instanceof Parameter) {
                Parameter parameter = (Parameter) binding;
                if (!parameters.containsKey(parameter.name)) {
                    throw new IllegalArgumentException("Unbound named parameter: " + parameter.name);
                }
                Object value = parameters.get(parameter.name);
                if (parameter.marker != null) {
                    Iterable collection;
                    if (value instanceof Object[]) {
                        collection = java.util.Arrays.asList((Object[]) value);
                    } else if (value instanceof Iterable) {
                        collection = (Iterable) value;
                    } else {
                        throw new IllegalArgumentException(
                                "IN parameter requires an iterable or Object[]: " + parameter.name);
                    }
                    int count = 0;
                    for (Object item : collection) {
                        values.add(parameter.convert(item));
                        SessionImpl.checkParameterCount(values.size());
                        count++;
                    }
                    statement.setLength(statement.length() - 1);
                    statement.append(count == 0 ? "SELECT NULL WHERE 1=0" : SessionImpl.placeholders(count));
                } else {
                    if (value instanceof Iterable || value instanceof Object[]) {
                        throw new IllegalArgumentException("Collection parameter requires IN: " + parameter.name);
                    }
                    values.add(parameter.convert(value));
                }
            } else {
                values.add(boundValue(binding));
            }
        }
        SessionImpl.checkParameterCount(values.size());
        statement.append(template.substring(position));
        return new Bound(statement.toString(), values.toArray());
    }
    private Object boundValue(Object binding) {
        if (binding instanceof LiteralBinding) {
            return ((LiteralBinding) binding).convert();
        }
        if (!(binding instanceof Parameter)) {
            return binding;
        }
        Parameter parameter = (Parameter) binding;
        if (!parameters.containsKey(parameter.name)) {
            throw new IllegalArgumentException("Unbound named parameter: " + parameter.name);
        }
        return parameter.convert(parameters.get(parameter.name));
    }
    private static final class LiteralBinding {
        final Object value;
        QueryImpl query;
        String field;
        LiteralBinding(Object value) {
            this.value = value;
        }
        Object convert() {
            return query == null ? Values.storage(value) : query.parameter(field, value);
        }
    }
    private static final class LikeBinding {
        final Object pattern;
        final Object escape;
        final boolean escaped;
        LikeBinding(Object pattern, Object escape, boolean escaped) {
            this.pattern = pattern;
            this.escape = escape;
            this.escaped = escaped;
        }
        boolean uses(String name) {
            return pattern instanceof Parameter && ((Parameter) pattern).name.equals(name)
                    || escape instanceof Parameter && ((Parameter) escape).name.equals(name);
        }
    }
    private static final class Parameter {
        final String name;
        String marker;
        QueryImpl query;
        String field;
        boolean nonNull;
        boolean numeric;
        boolean integral;
        boolean integralAssignment;
        boolean logical;
        int expectedKind = -1;
        Parameter(String name) {
            this.name = name;
        }
        Object convert(Object value) {
            Object converted = query == null ? Values.storage(value) : query.parameter(field, value);
            boolean mappedBoolean = query != null && query.mapping(field).startsWith("converter:")
                    && (Long.valueOf(0).equals(converted) || Long.valueOf(1).equals(converted));
            if (converted != null && (logical || expectedKind == Attribute.BOOLEAN)
                    && !(value instanceof Boolean) && !mappedBoolean) {
                throw new IllegalArgumentException("Logical parameter requires a Boolean: " + name);
            }
            Values.requireStorageKind(converted, expectedKind);
            if (query != null && !numeric && !integralAssignment) {
                query.requireIntegralRange(field, converted);
            }
            if (converted != null && integralAssignment && !(converted instanceof Byte
                    || converted instanceof Short || converted instanceof Integer || converted instanceof Long)) {
                throw new IllegalArgumentException("Integral assignment requires an integral stored value: " + name);
            }
            if (converted != null && numeric) {
                if (!(value instanceof Number) || !(converted instanceof Number) || integral && !(converted instanceof Byte
                        || converted instanceof Short || converted instanceof Integer || converted instanceof Long)) {
                    throw new IllegalArgumentException("Arithmetic parameter requires "
                            + (integral ? "an integral" : "a numeric") + " value: " + name);
                }
            }
            if (nonNull && converted == null) {
                throw new IllegalArgumentException("Null parameter for a required subtype or primitive attribute: " + name);
            }
            return converted;
        }
    }
    private static final class Alias {
        final QueryImpl query;
        final String path;
        Alias(QueryImpl query, String path) {
            this.query = query;
            this.path = path;
        }
        String field(String field) {
            return path.length() == 0 ? field : path + "." + field;
        }
    }
    private static final class Expr {
        String sql;
        int kind;
        final EntityModel entity;
        QueryImpl query;
        String field;
        Parameter parameter;
        final List<Expr> children = new ArrayList<Expr>();
        boolean literal;
        boolean aggregate;
        boolean numericOperands;
        boolean association;
        boolean predicate;
        boolean nonNull;
        boolean nullPreserving;
        boolean coalesce;
        Expr scalarProjection;
        QueryImpl projectionQuery;
        String projectionField;
        Object literalValue;
        LiteralBinding literalBinding;
        Expr(String sql, int kind) {
            this(sql, kind, null);
        }
        Expr(String sql, int kind, EntityModel entity) {
            this.sql = sql;
            this.kind = kind;
            this.entity = entity;
        }
    }
    private static final class Plan {
        List<Expr> projections = new ArrayList<Expr>();
        final List<Expr> correlations;
        String sql;
        int[] kinds = new int[0];
        EntityModel entity;
        boolean mutation;
        boolean singleRow;
        boolean guaranteedRow;
        final List<Object> bindings;
        final List<String> fetches = new ArrayList<String>();
        Plan(List<Object> bindings, List<Expr> correlations) {
            this.bindings = bindings;
            this.correlations = correlations;
        }
    }
    private static final class Parser {
        final SessionImpl session;
        final List<String> tokens;
        final List<Object> bindings;
        final Map<String, Alias> aliases;
        int position;
        QueryImpl root;
        final List<Expr> correlations = new ArrayList<Expr>();
        String rootName;
        String mutationTable;
        Parser(SessionImpl session, List<String> tokens, List<Object> bindings, Map<String, Alias> outer) {
            this.session = session;
            this.tokens = tokens;
            this.bindings = bindings;
            this.aliases = new LinkedHashMap<String, Alias>(outer);
        }
        Plan parse() {
            Plan plan = new Plan(bindings, correlations);
            if (take("UPDATE")) {
                plan.mutation = true;
                root(true);
                mutationTable = root.model.table();
                expect("SET");
                StringBuilder assignments = new StringBuilder();
                do {
                    if (assignments.length() > 0) {
                        assignments.append(", ");
                    }
                    String path = path();
                    String field = local(path);
                    if (field.indexOf('.') >= 0) {
                        throw error("Bulk assignment must target a root field");
                    }
                    Attribute attribute = root.model.attributes()[root.model.queryIndex(field)];
                    if (attribute.id || root.model.discriminatorIndex() == root.model.queryIndex(field)) {
                        throw error("Bulk updates cannot change entity identifiers");
                    }
                    expect("=");
                    Expr value = rowExpression();
                    Expr target = new Expr(session.q(attribute.column), attribute.kind);
                    target.query = root;
                    target.field = field;
                    target.projectionQuery = root;
                    target.projectionField = field;
                    compatible(target, value);
                    if (attribute.kind == Attribute.INTEGER || attribute.kind == Attribute.BIGINT) {
                        requireIntegralAssignment(value);
                    }
                    validateAssignment(root.mapping(field), value);
                    bindType(target, value);
                    if (root.model.primitive(root.model.queryIndex(field)) && !nonNullExpression(value, true, true)) {
                        throw error("Nullable bulk assignment to a primitive attribute requires a non-null fallback");
                    }
                    if (attribute.nullable && root.model.required(root.model.queryIndex(field))) {
                        requireNonNull(value);
                    }
                    if (attribute.kind == Attribute.INTEGER) {
                        int index = root.model.queryIndex(field);
                        value.sql = session.checkedIntegralAssignment(value.sql,
                                root.model.minimumIntegralValue(index), root.model.maximumIntegralValue(index));
                    }
                    if (root.model.singlePrecision(root.model.queryIndex(field))) {
                        value.sql = session.checkedFloatAssignment(value.sql);
                    }
                    assignments.append(target.sql).append(" = ").append(value.sql);
                } while (take(","));
                String where = take("WHERE") ? " WHERE " + condition(rowExpression()) : "";
                String filter = session.discriminatorCondition(root.model, root.rootAlias);
                if (filter.length() > 0) {
                    where = where.length() == 0 ? " WHERE " + filter : where + " AND " + filter;
                }
                plan.sql = session.updateTable(root.model.table(), root.rootAlias) + " SET " + assignments +
                           where;
                refuseImplicitBulkJoins();
                return plan;
            }
            if (take("DELETE")) {
                plan.mutation = true;
                expect("FROM");
                root(true);
                mutationTable = root.model.table();
                String where = take("WHERE") ? " WHERE " + condition(rowExpression()) : "";
                String filter = session.discriminatorCondition(root.model, root.rootAlias);
                if (filter.length() > 0) {
                    where = where.length() == 0 ? " WHERE " + filter : where + " AND " + filter;
                }
                plan.sql = session.deleteFrom(root.model.table(), root.rootAlias) + where;
                refuseImplicitBulkJoins();
                return plan;
            }
            int start = position;
            int from = -1;
            int depth = 0;
            for (int i = position; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if ("(".equals(token)) {
                    depth++;
                } else if (")".equals(token)) {
                    depth--;
                }
                if (depth == 0 && "FROM".equalsIgnoreCase(token)) {
                    from = i;
                    break;
                }
            }
            if (from < 0) {
                throw error("SELECT requires FROM");
            }
            position = from + 1;
            root();
            while (peek("JOIN") || peek("LEFT") || peek("INNER")) {
                boolean left = take("LEFT");
                if (left) {
                    take("OUTER");
                } else {
                    take("INNER");
                }
                expect("JOIN");
                boolean fetch = take("FETCH");
                String joinPath = local(path());
                if (fetch) {
                    if (joinPath.indexOf('.') >= 0) {
                        throw error("Nested fetch paths are not supported");
                    }
                    root.model.queryRelationIndex(joinPath);
                    plan.fetches.add(joinPath);
                }
                if (fetch) {
                    root.fetchJoin(joinPath, left);
                } else if (left) {
                    root.leftJoin(joinPath);
                } else {
                    root.join(joinPath);
                }
                take("AS");
                if (identifier(peek()) && !clause(peek())) {
                    aliases.put(next(), new Alias(root, joinPath));
                }
            }
            int afterFrom = position;
            position = start;
            boolean distinct = false;
            List<Expr> selections = new ArrayList<Expr>();
            if (take("SELECT")) {
                distinct = take("DISTINCT");
                do {
                    selections.add(expression());
                } while (take(","));
                if (position != from) {
                    throw error("Unexpected SELECT expression");
                }
            } else if (position == from) {
                selections.add(new Expr(root.rootColumns(), Attribute.BIGINT, root.model));
            } else {
                throw error("Expected SELECT or FROM");
            }
            position = afterFrom;
            Expr predicate = take("WHERE") ? expression() : null;
            if (predicate != null && hasAggregate(predicate)) {
                throw error("Aggregate functions are not allowed in WHERE");
            }
            String where = predicate == null ? "" : " WHERE " + condition(predicate);
            List<Expr> groups = new ArrayList<Expr>();
            List<Expr> groupedExpressions = new ArrayList<Expr>(selections);
            String group = "";
            String having = "";
            String order = "";
            if (take("GROUP")) {
                expect("BY");
                StringBuilder terms = new StringBuilder();
                do {
                    Expr term = expression();
                    if (hasAggregate(term) || term.entity != null) {
                        throw error("GROUP BY requires scalar non-aggregate expressions");
                    }
                    groups.add(term);
                    if (terms.length() > 0) {
                        terms.append(", ");
                    }
                    // A SELECT position reuses that expression's binding positions
                    // on PostgreSQL, where separate equal-valued parameters differ.
                    StringBuilder selectedPositions = new StringBuilder();
                    for (int i = 0; i < selections.size(); i++) {
                        if (term.sql.indexOf("/*cn1-bind-") >= 0 && sameExpression(term, selections.get(i))) {
                            if (selectedPositions.length() > 0) {
                                selectedPositions.append(", ");
                            }
                            selectedPositions.append(i + 1);
                        }
                    }
                    terms.append(selectedPositions.length() == 0 ? term.sql : selectedPositions.toString());
                } while (take(","));
                group = " GROUP BY " + terms;
            }
            if (take("HAVING")) {
                Expr term = expression();
                groupedExpressions.add(term);
                having = " HAVING " + condition(term);
            }
            if (take("ORDER")) {
                expect("BY");
                StringBuilder out = new StringBuilder();
                do {
                    if (out.length() > 0) {
                        out.append(", ");
                    }
                    int firstBinding = bindings.size();
                    Expr term = expression();
                    groupedExpressions.add(term);
                    List<Object> termBindings = new ArrayList<Object>(bindings.subList(firstBinding, bindings.size()));
                    if (distinct && (!termBindings.isEmpty() || !selected(term, selections))) {
                        throw error("DISTINCT ordering must use a selected expression");
                    }
                    boolean ascending = !take("DESC");
                    if (ascending) {
                        take("ASC");
                    }
                    String rendered = session.orderBy(term.sql, ascending, term.kind);
                    out.append(rendered);
                } while (take(","));
                order = " ORDER BY " + out;
            }
            boolean grouped = !groups.isEmpty() || having.length() > 0;
            for (Expr expr : groupedExpressions) {
                grouped |= hasAggregate(expr);
            }
            if (grouped) {
                for (Expr expr : groupedExpressions) {
                    validateGrouped(expr, groups);
                }
            }
            StringBuilder projection = new StringBuilder();
            List<Integer> kinds = new ArrayList<Integer>();
            for (Expr expr : selections) {
                if (projection.length() > 0) {
                    projection.append(", ");
                }
                // PostgreSQL requires the collated ORDER BY expression in a
                // DISTINCT projection, even when the underlying column is selected.
                projection.append(!distinct ? expr.sql : expr.entity != null ? root.rootColumns(true)
                        : session.orderValue(expr.sql, expr.kind));
                if (expr.entity != null) {
                    if (selections.size() != 1) {
                        throw error("Entity/scalar mixed projections are not supported");
                    }
                    plan.entity = expr.entity;
                    for (Attribute a : expr.entity.attributes()) {
                        kinds.add(Integer.valueOf(a.kind));
                    }
                } else {
                    projection.append(" AS cn1_scalar_").append(kinds.size());
                    validateProjection(expr);
                    kinds.add(Integer.valueOf(expr.kind));
                }
            }
            if (!plan.fetches.isEmpty() && !SessionImpl.sameInstance(plan.entity, root.model)) {
                throw error("Fetch joins require the root entity projection");
            }
            if (groups.isEmpty()) {
                for (Expr expression : groupedExpressions) {
                    plan.singleRow |= hasRowAggregate(expression);
                }
            }
            plan.guaranteedRow = plan.singleRow && having.length() == 0;
            if (mutationTable != null && "mysql".equals(session.sqlDialect()) && root.readsTable(mutationTable)) {
                throw error("MySQL bulk subqueries cannot read the mutation target table");
            }
            plan.projections = selections;
            SessionImpl.checkParameterCount(bindings.size());
            plan.kinds = new int[kinds.size()];
            for (int i = 0; i < plan.kinds.length; i++) {
                plan.kinds[i] = kinds.get(i).intValue();
            }
            plan.sql = "SELECT " + (distinct ? "DISTINCT " : "") + projection + root.from() +
                       where + group + having + order;
            return plan;
        }
        private void refuseImplicitBulkJoins() {
            if (root.from().indexOf(" JOIN ") >= 0) {
                throw error("Use a subquery for relationship predicates in bulk mutations");
            }
        }
        private void root() {
            root(false);
        }
        private void root(boolean mutation) {
            String entity = path();
            EntityModel model = session.model(entity);
            root = mutation ? session.queryForMutation(model) : session.query(model.type());
            take("AS");
            rootName = identifier(peek()) && !clause(peek()) ? next() : entity.substring(entity.lastIndexOf('.') + 1);
            aliases.put(rootName, new Alias(root, ""));
        }
        private Expr expression() {
            return or();
        }
        private Expr or() {
            Expr left = and();
            while (take("OR")) {
                left = logical(left, "OR", and());
            }
            return left;
        }
        private Expr and() {
            Expr left = compare();
            while (take("AND")) {
                left = logical(left, "AND", compare());
            }
            return left;
        }
        private Expr compare() {
            if (take("NOT")) {
                Expr value = compare();
                return node("NOT (" + condition(value) + ")", Attribute.BOOLEAN, value);
            }
            if (take("EXISTS")) {
                expect("(");
                Plan nested = subquery();
                expect(")");
                return node("EXISTS (" + nested.sql + ")", Attribute.BOOLEAN,
                        nested.correlations.toArray(new Expr[nested.correlations.size()]));
            }
            Expr left = add();
            if (take("IS")) {
                if (left.kind < 0) {
                    throw error("IS NULL requires a known operand storage kind");
                }
                boolean not = take("NOT");
                expect("NULL");
                if (left.projectionQuery != null
                        && left.projectionQuery.parameter(left.projectionField, null) != null) {
                    Expr mappedNull = literal(left.kind, null);
                    mappedNull.literalBinding.query = left.projectionQuery;
                    mappedNull.literalBinding.field = left.projectionField;
                    return node(left.sql + (not ? " <> " : " = ") + mappedNull.sql,
                            Attribute.BOOLEAN, left, mappedNull);
                }
                return node(left.sql + (not ? " IS NOT NULL" : " IS NULL"), Attribute.BOOLEAN, left);
            }
            boolean not = take("NOT");
            if (take("IN")) {
                boolean parens = take("(");
                String values;
                List<Expr> operands = new ArrayList<Expr>();
                operands.add(left);
                if (peek() != null && peek().startsWith(":") &&
                        (!parens || position + 1 < tokens.size() && ")".equals(tokens.get(position + 1)))) {
                    if (left.kind < 0) {
                        throw error("IN parameter requires a known left operand storage kind");
                    }
                    Parameter parameter = new Parameter(next().substring(1));
                    parameter.query = left.query;
                    parameter.field = left.field;
                    parameter.expectedKind = left.kind;
                    parameter.marker = bind(parameter);
                    values = parameter.marker;
                } else {
                    if (!parens) {
                        throw error("Expected IN parameter or parenthesized values");
                    }
                    if (peek("SELECT") || peek("FROM")) {
                        Plan nested = subquery();
                        compatible(left, nested.projections.get(0));
                        values = nested.sql;
                        operands.addAll(nested.correlations);
                    } else {
                        StringBuilder list = new StringBuilder();
                        do {
                            if (list.length() > 0) {
                                list.append(", ");
                            }
                            Expr value = expression();
                            compatible(left, value);
                            bindType(left, value);
                            operands.add(value);
                            list.append(value.sql);
                        } while (take(","));
                        values = list.toString();
                    }
                }
                if (parens) {
                    expect(")");
                }
                return node(left.sql + (not ? " NOT IN (" : " IN (") + values + ")", Attribute.BOOLEAN,
                        operands.toArray(new Expr[operands.size()]));
            }
            if (take("BETWEEN")) {
                Expr low = add();
                expect("AND");
                Expr high = add();
                compatible(left, low);
                compatible(left, high);
                bindType(left, low);
                bindType(left, high);
                return node(session.orderValue(left.sql, left.kind) + (not ? " NOT BETWEEN " : " BETWEEN ")
                        + "(" + session.orderValue(low.sql, low.kind) + ") AND (" + session.orderValue(high.sql, high.kind) + ")",
                        Attribute.BOOLEAN, left, low, high);
            }
            if (take("LIKE")) {
                int firstBinding = bindings.size();
                Expr pattern = add();
                requireKind(left, Attribute.TEXT);
                requireKind(pattern, Attribute.TEXT);
                compatibleMappings(left, pattern);
                bindType(left, pattern);
                boolean escaped = take("ESCAPE");
                Expr escape = escaped ? add() : null;
                String sql;
                if ((pattern.parameter != null || pattern.literal)
                        && (!escaped || escape.parameter != null || escape.literal)) {
                    while (bindings.size() > firstBinding) {
                        bindings.remove(bindings.size() - 1);
                    }
                    String marker = bind(new LikeBinding(pattern.parameter == null ? pattern.literalBinding : pattern.parameter,
                            !escaped ? null : escape.parameter == null ? escape.literalBinding : escape.parameter, escaped));
                    sql = left.sql + session.likeOperator(escaped).replace("?", marker);
                } else {
                    if (escaped) {
                        throw error("LIKE with ESCAPE requires literal or parameter patterns and escapes");
                    }
                    sql = left.sql + session.likeOperator(false).replace("?", session.likeExpression(pattern.sql));
                }
                return escaped ? node(not ? "NOT (" + sql + ")" : sql, Attribute.BOOLEAN, left, pattern, escape)
                        : node(not ? "NOT (" + sql + ")" : sql, Attribute.BOOLEAN, left, pattern);
            }
            if (not) {
                throw error("Expected IN, LIKE or BETWEEN after NOT");
            }
            String op = peek();
            if ("=".equals(op) || "<>".equals(op) || "!=".equals(op) || ">".equals(op) || "<".equals(op) ||
                    ">=".equals(op) || "<=".equals(op)) {
                next();
                Expr right = add();
                compatible(left, right);
                bindType(left, right);
                bindType(right, left);
                if (left.kind == Attribute.BOOLEAN || right.kind == Attribute.BOOLEAN) {
                    return node("(" + condition(left) + " " + op + " " + condition(right) + ")", Attribute.BOOLEAN, left, right);
                }
                if (numericKind(left.kind) && numericKind(right.kind)
                        && (left.kind == Attribute.REAL || right.kind == Attribute.REAL)) {
                    return node("(" + session.numericOperand(left.sql, Attribute.REAL) + " " + op + " "
                            + session.numericOperand(right.sql, Attribute.REAL) + ")", Attribute.BOOLEAN, left, right);
                }
                if (">".equals(op) || "<".equals(op) || ">=".equals(op) || "<=".equals(op)) {
                    return node("(" + session.orderValue(left.sql, left.kind) + " " + op + " "
                            + session.orderValue(right.sql, right.kind) + ")", Attribute.BOOLEAN, left, right);
                }
                return binary(left, op, right, Attribute.BOOLEAN);
            }
            return left;
        }
        private Expr add() {
            Expr left = multiply();
            while (peek("+") || peek("-")) {
                String op = next();
                Expr right = multiply();
                left = arithmetic(left, op, right);
            }
            return left;
        }
        private Expr multiply() {
            Expr left = primary();
            while (peek("*") || peek("/") || peek("%")) {
                String op = next();
                Expr right = primary();
                left = arithmetic(left, op, right);
            }
            return left;
        }
        private Expr primary() {
            if (take("-")) {
                String magnitude = peek();
                if (magnitude.length() > 0 && Character.isDigit(magnitude.charAt(0))
                        && magnitude.indexOf('.') < 0 && magnitude.indexOf('e') < 0 && magnitude.indexOf('E') < 0) {
                    // Parse the sign with the magnitude so Long.MIN_VALUE remains representable.
                    return literal(Attribute.BIGINT, Long.valueOf(Long.parseLong("-" + next())));
                }
                Expr value = primary();
                requireNumeric(value, value.kind != Attribute.REAL);
                Expr result = wrap(session.checkedArithmetic("-" + session.numericOperand(value.sql,
                        value.kind < 0 ? Attribute.BIGINT : value.kind), value.kind < 0 ? Attribute.BIGINT : value.kind), value);
                result.kind = value.kind < 0 ? Attribute.BIGINT : value.kind;
                result.projectionQuery = null;
                return result;
            }
            if (take("+")) {
                Expr value = primary();
                requireNumeric(value, value.kind != Attribute.REAL);
                Expr result = wrap(session.numericOperand(value.sql, value.kind < 0 ? Attribute.BIGINT : value.kind), value);
                result.kind = value.kind < 0 ? Attribute.BIGINT : value.kind;
                result.projectionQuery = null;
                return result;
            }
            if (take("(")) {
                if (peek("SELECT") || peek("FROM")) {
                    Plan nested = subquery();
                    expect(")");
                    if (!nested.singleRow) {
                        throw error("Scalar subqueries require an aggregate without GROUP BY");
                    }
                    Expr result = new Expr("(" + nested.sql + ")", nested.kinds[0]);
                    result.children.addAll(nested.correlations);
                    if (nested.guaranteedRow) {
                        result.scalarProjection = nested.projections.get(0);
                    }
                    result.projectionQuery = nested.projections.get(0).projectionQuery;
                    result.projectionField = nested.projections.get(0).projectionField;
                    return result;
                }
                Expr value = expression();
                expect(")");
                return wrap("(" + value.sql + ")", value);
            }
            String token = next();
            if (token.startsWith(":")) {
                Parameter parameter = new Parameter(token.substring(1));
                Expr result = new Expr(bind(parameter), -1);
                result.parameter = parameter;
                return result;
            }
            if (token.startsWith("'")) {
                String value = token.substring(1, token.length() - 1).replace("''", "'");
                return literal(Attribute.TEXT, value);
            }
            if ("NULL".equalsIgnoreCase(token)) {
                return literal(-1, null);
            }
            if ("TRUE".equalsIgnoreCase(token) || "FALSE".equalsIgnoreCase(token)) {
                return literal(Attribute.BOOLEAN, Boolean.valueOf("TRUE".equalsIgnoreCase(token)));
            }
            if (Character.isDigit(token.charAt(0))) {
                Object value = token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0
                                       ? (Object) Double.valueOf(token)
                                       : Long.valueOf(Long.parseLong(token));
                if (value instanceof Double) {
                    double number = ((Double) value).doubleValue();
                    if (Double.isInfinite(number)) {
                        throw error("Floating-point literal overflow");
                    }
                    if (number == 0.0) {
                        for (int i = 0; i < token.length() && token.charAt(i) != 'e' && token.charAt(i) != 'E'; i++) {
                            if (token.charAt(i) >= '1' && token.charAt(i) <= '9') {
                                throw error("Floating-point literal underflow");
                            }
                        }
                    }
                }
                int kind = value instanceof Double ? Attribute.REAL : Attribute.BIGINT;
                return literal(kind, value);
            }
            if (!identifier(token)) {
                throw error("Expected expression");
            }
            if (take("(")) {
                String function = upper(token);
                if (!(" COUNT SUM AVG MIN MAX LOWER UPPER LENGTH ABS COALESCE NULLIF TRIM ".contains(
                            " " + function + " "))) {
                    throw error("Unsupported function: " + token);
                }
                boolean distinct = take("DISTINCT");
                List<Expr> args = new ArrayList<Expr>();
                if (take("*")) {
                    args.add(new Expr("*", Attribute.BIGINT));
                } else {
                    do {
                        args.add(expression());
                    } while (take(","));
                }
                expect(")");
                validateFunction(function, args, distinct);
                StringBuilder sql = new StringBuilder(session.functionName(function)).append('(').append(distinct ? "DISTINCT " : "");
                for (Expr arg : args) {
                    if (sql.charAt(sql.length() - 1) != '(' && !(distinct && sql.toString().endsWith("DISTINCT "))) {
                        sql.append(", ");
                    }
                    if (arg.entity != null) {
                        if (!"COUNT".equals(function)) {
                            throw error("Entity argument requires COUNT");
                        }
                        if (distinct && arg.entity.idIndexes().length > 1) {
                            throw error("Use the query builder count() for distinct composite identities");
                        }
                        sql.append(arg.query.rootAlias)
                                .append('.')
                                .append(session.q(arg.entity.attributes()[arg.entity.idIndex()].column));
                    } else {
                        sql.append(arg.sql);
                    }
                }
                if ("NULLIF".equals(function)) {
                    compatibleMappings(args.get(0), args.get(1));
                }
                int kind = "COUNT".equals(function) || "LENGTH".equals(function) ? Attribute.BIGINT
                           : "LOWER".equals(function) || "UPPER".equals(function) || "TRIM".equals(function) ? Attribute.TEXT
                           : "SUM".equals(function) && args.get(0).kind != Attribute.REAL ? Attribute.BIGINT
                           : "AVG".equals(function) ? Attribute.REAL
                           : "COALESCE".equals(function) || "NULLIF".equals(function) && args.get(0).kind < 0
                                   ? commonKind(args) : args.get(0).kind;
                Expr result = new Expr(sql.append(')').toString(), kind);
                if ("SUM".equals(function) && kind == Attribute.BIGINT) {
                    result.sql = session.integralSum(result.sql);
                } else if ("AVG".equals(function)) {
                    result.sql = session.numericOperand(result.sql, Attribute.REAL);
                }
                result.nonNull = "COUNT".equals(function);
                result.coalesce = "COALESCE".equals(function);
                result.nullPreserving = " LOWER UPPER LENGTH ABS TRIM ".contains(" " + function + " ");
                result.aggregate = " COUNT SUM AVG MIN MAX ".contains(" " + function + " ");
                result.numericOperands = " SUM AVG MIN MAX ABS COALESCE NULLIF ".contains(" " + function + " ");
                result.children.addAll(args);
                if (" MIN MAX COALESCE NULLIF ".contains(" " + function + " ")) {
                    for (Expr arg : args) {
                        result.association |= arg.association;
                    }
                }
                // These functions preserve their operands' domain representation.
                if ("COALESCE".equals(function)) {
                    Expr mapping = coalesceMapping(args);
                    if (mapping != null && mapping.projectionQuery != null) {
                        result.query = mapping.projectionQuery;
                        result.field = mapping.projectionField;
                        for (Expr arg : args) {
                            bindType(result, arg);
                        }
                    }
                } else if (" MIN MAX NULLIF ".contains(" " + function + " ")) {
                    for (Expr arg : args) {
                        if (arg.query != null) {
                            for (Expr other : args) {
                                bindType(arg, other);
                            }
                            result.query = arg.query;
                            result.field = arg.field;
                            break;
                        }
                    }
                }
                if (result.query != null && kind == result.query.kind(result.field)
                        && " MIN MAX COALESCE NULLIF ".contains(" " + function + " ")) {
                    result.projectionQuery = result.query;
                    result.projectionField = result.field;
                }
                return result;
            }
            StringBuilder path = new StringBuilder(token);
            while (take(".")) {
                path.append('.').append(word());
            }
            String full = path.toString();
            int dot = full.indexOf('.');
            Alias alias = aliases.get(dot < 0 ? full : full.substring(0, dot));
            if (alias == null) {
                throw error("Unknown alias: " + full);
            }
            if (dot < 0) {
                if (alias.path.length() > 0) {
                    throw error("Select a root entity or scalar fields");
                }
                Expr result = new Expr(alias.query.rootColumns(), Attribute.BIGINT, alias.query.model);
                result.query = alias.query;
                if (!SessionImpl.sameInstance(alias.query, root)) {
                    correlations.add(result);
                }
                return result;
            }
            String field = alias.field(full.substring(dot + 1));
            Expr result = new Expr(alias.query.column(field), alias.query.kind(field));
            result.query = alias.query;
            result.field = field;
            result.association = alias.query.association(field);
            result.nonNull = alias.query.nonNull(field);
            result.projectionQuery = result.query;
            result.projectionField = field;
            if (!SessionImpl.sameInstance(alias.query, root)) {
                correlations.add(result);
            }
            return result;
        }
        private String bind(Object value) {
            String marker = "/*cn1-bind-" + bindings.size() + "*/?";
            bindings.add(value);
            return marker;
        }
        private void validateProjection(Expr value) {
            if (value.kind < 0) {
                throw error("Scalar projection requires a known storage kind");
            }
            if (value.association) {
                throw error("Association-valued scalar projections are not supported; select an explicit target field");
            }
        }
        private void validateFunction(String function, List<Expr> args, boolean distinct) {
            boolean aggregate = " COUNT SUM AVG MIN MAX ".contains(" " + function + " ");
            int size = args.size();
            if ("COALESCE".equals(function) ? size < 2 : size != ("NULLIF".equals(function) ? 2 : 1)) {
                throw error("Invalid argument count for " + function);
            }
            if (distinct && !aggregate) {
                throw error("DISTINCT requires an aggregate function");
            }
            boolean text = " LOWER UPPER LENGTH TRIM ".contains(" " + function + " ");
            boolean numeric = " SUM AVG ABS ".contains(" " + function + " ");
            for (Expr arg : args) {
                if (aggregate && hasAggregate(arg)) {
                    throw error("Nested aggregate functions are not supported");
                }
                if (text) {
                    requireKind(arg, Attribute.TEXT);
                }
                if (numeric) {
                    requireNumeric(arg, false);
                }
                if ("*".equals(arg.sql)) {
                    if (!"COUNT".equals(function) || distinct) {
                        throw error("Only COUNT(*) accepts a wildcard");
                    }
                } else if (arg.entity != null) {
                    if (!"COUNT".equals(function)) {
                        throw error("Entity argument requires COUNT");
                    }
                } else if (arg.kind >= 0) {
                    boolean number = arg.kind == Attribute.INTEGER || arg.kind == Attribute.BIGINT
                            || arg.kind == Attribute.REAL;
                    if (text && arg.kind != Attribute.TEXT || numeric && !number) {
                        throw error("Invalid operand type for " + function);
                    }
                    if (("MIN".equals(function) || "MAX".equals(function))
                            && !number && arg.kind != Attribute.TEXT && arg.kind != Attribute.TIMESTAMP) {
                        throw error("Invalid ordered operand for " + function);
                    }
                }
            }
            if ("COALESCE".equals(function) || "NULLIF".equals(function)) {
                int kind = commonKind(args);
                if (kind >= 0) {
                    for (Expr arg : args) {
                        if (arg.kind < 0) {
                            requireKind(arg, kind);
                        }
                    }
                }
            }
        }
        private Expr literal(int kind, Object value) {
            LiteralBinding binding = new LiteralBinding(value);
            String sql = bind(binding);
            if (numericKind(kind) || kind == Attribute.BOOLEAN) {
                sql = session.numericOperand(sql, kind == Attribute.BOOLEAN ? Attribute.BIGINT : kind);
            }
            Expr result = new Expr(sql, kind);
            result.literal = true;
            result.literalValue = value;
            result.literalBinding = binding;
            return result;
        }
        private Expr wrap(String sql, Expr value) {
            Expr result = new Expr(sql, value.kind, value.entity);
            result.children.add(value);
            result.nullPreserving = true;
            result.numericOperands = true;
            result.predicate = value.predicate;
            result.association = value.association;
            result.projectionQuery = value.projectionQuery;
            result.projectionField = value.projectionField;
            result.query = value.query;
            result.field = value.field;
            result.parameter = value.parameter;
            result.literal = value.literal;
            result.literalValue = value.literalValue;
            result.literalBinding = value.literalBinding;
            return result;
        }
        private int commonKind(List<Expr> args) {
            int kind = -1;
            for (Expr arg : args) {
                if (arg.kind < 0) {
                    continue;
                }
                if (kind < 0) {
                    kind = arg.kind;
                } else if (kind != arg.kind) {
                    boolean numeric = (kind == Attribute.INTEGER || kind == Attribute.BIGINT || kind == Attribute.REAL)
                            && (arg.kind == Attribute.INTEGER || arg.kind == Attribute.BIGINT || arg.kind == Attribute.REAL);
                    if (!numeric) {
                        throw error("Function operands need compatible storage types");
                    }
                    kind = kind == Attribute.REAL || arg.kind == Attribute.REAL ? Attribute.REAL : Attribute.BIGINT;
                }
            }
            return kind;
        }
        private boolean nonNullExpression(Expr value, boolean allowParameters, boolean enforce) {
            if (value.scalarProjection != null) {
                return nonNullExpression(value.scalarProjection, allowParameters, enforce);
            }
            if (value.parameter != null) {
                if (allowParameters && enforce) {
                    value.parameter.nonNull = true;
                }
                return allowParameters || value.parameter.nonNull;
            }
            if (value.literal) {
                return value.literalBinding.convert() != null;
            }
            if (value.nonNull) {
                return true;
            }
            if (value.coalesce) {
                // Prefer an unconditional fallback so earlier nullable parameters
                // remain usable when a later operand already guarantees a value.
                for (Expr child : value.children) {
                    if (nonNullExpression(child, false, false)) {
                        return true;
                    }
                }
                for (Expr child : value.children) {
                    if (nonNullExpression(child, allowParameters, false)) {
                        return nonNullExpression(child, allowParameters, enforce);
                    }
                }
            } else if (value.nullPreserving) {
                for (Expr child : value.children) {
                    if (!nonNullExpression(child, allowParameters, enforce)) {
                        return false;
                    }
                }
                return !value.children.isEmpty();
            }
            return false;
        }
        private void requireNonNull(Expr value) {
            if (value.parameter != null) {
                value.parameter.nonNull = true;
            } else if (!value.literal || value.literalBinding.convert() == null) {
                throw error("Bulk assignment to a required subtype attribute needs a non-null literal or parameter");
            }
        }
        private boolean selected(Expr term, List<Expr> selections) {
            for (Expr selection : selections) {
                if (selection.entity == null && selection.sql.equals(term.sql)) {
                    return true;
                }
                if (selection.entity != null && SessionImpl.sameInstance(term.query, root) && term.field != null
                        && term.sql.equals(root.column(term.field))) {
                    for (Attribute attribute : root.model.attributes()) {
                        if (attribute.field.equals(term.field)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        private Expr coalesceMapping(List<Expr> args) {
            Expr selected = null;
            String mapping = null;
            for (Expr arg : args) {
                if (arg.literal || arg.parameter != null) {
                    continue;
                }
                String current = arg.projectionQuery == null ? ""
                        : arg.projectionQuery.mapping(arg.projectionField);
                if (mapping != null && !mapping.equals(current)) {
                    throw error("COALESCE operands require matching domain and converter mappings");
                }
                if (selected == null) {
                    selected = arg;
                    mapping = current;
                }
            }
            return selected;
        }
        private void requireIntegralAssignment(Expr value) {
            if (value.kind == Attribute.REAL) {
                throw error("REAL expressions cannot be assigned to integral attributes");
            }
            if (value.parameter != null) {
                value.parameter.integralAssignment = true;
            }
            if (value.kind < 0 || value.numericOperands) {
                for (Expr child : value.children) {
                    requireIntegralAssignment(child);
                }
            }
        }
        private void validateAssignment(String mapping, Expr value) {
            if (value.query != null && value.field != null && !mapping.equals(value.query.mapping(value.field))) {
                throw error("Bulk assignment requires matching domain and converter mappings");
            }
            if (value.projectionQuery != null
                    && !mapping.equals(value.projectionQuery.mapping(value.projectionField))) {
                throw error("Bulk assignment requires matching domain and converter mappings");
            }
            for (Expr child : value.children) {
                validateAssignment(mapping, child);
            }
        }
        private void bindType(Expr attribute, Expr value) {
            if (attribute.query == null) {
                return;
            }
            if (value.literalBinding != null && value.literalBinding.query == null) {
                value.literalBinding.query = attribute.query;
                value.literalBinding.field = attribute.field;
            }
            if (value.parameter != null && value.parameter.query == null) {
                value.parameter.query = attribute.query;
                value.parameter.field = attribute.field;
            }
            for (Expr child : value.children) {
                bindType(attribute, child);
            }
        }
        private Plan subquery() {
            int start = position;
            int depth = 0;
            while (position < tokens.size()) {
                if (peek(")") && depth == 0) {
                    break;
                }
                if (peek("(")) {
                    depth++;
                } else if (peek(")")) {
                    depth--;
                }
                position++;
            }
            Parser nested =
                    new Parser(session, new ArrayList<String>(tokens.subList(start, position)), bindings, aliases);
            nested.mutationTable = mutationTable;
            Plan plan = nested.parse();
            for (Expr dependency : plan.correlations) {
                if (!SessionImpl.sameInstance(dependency.query, root)) {
                    correlations.add(dependency);
                }
            }
            nested.end();
            if (plan.mutation || plan.entity != null || plan.kinds.length != 1) {
                throw error("Subquery requires a single scalar projection");
            }
            return plan;
        }
        private String local(String path) {
            int dot = path.indexOf('.');
            Alias alias = aliases.get(dot < 0 ? path : path.substring(0, dot));
            if (alias == null || !SessionImpl.sameInstance(alias.query, root) || dot < 0) {
                throw error("Expected a root relationship or attribute path");
            }
            return alias.field(path.substring(dot + 1));
        }
        private String path() {
            StringBuilder value = new StringBuilder(word());
            while (take(".")) {
                value.append('.').append(word());
            }
            return value.toString();
        }
        private String word() {
            String word = next();
            if (!identifier(word)) {
                throw error("Expected identifier");
            }
            return word;
        }
        private Expr node(String sql, int kind, Expr... children) {
            Expr result = new Expr(sql, kind);
            result.predicate = kind == Attribute.BOOLEAN;
            for (Expr child : children) {
                result.children.add(child);
            }
            return result;
        }
        private Expr binary(Expr left, String op, Expr right, int kind) {
            if (left.entity != null || right.entity != null) {
                throw error("Compare entity identifiers explicitly");
            }
            Expr result = new Expr("(" + left.sql + " " + op + " " + right.sql + ")", kind);
            result.predicate = kind == Attribute.BOOLEAN;
            result.children.add(left);
            result.children.add(right);
            return result;
        }
        private boolean numericKind(int kind) {
            return kind == Attribute.INTEGER || kind == Attribute.BIGINT || kind == Attribute.REAL;
        }
        private void requireKind(Expr value, int kind) {
            if (value.entity != null || value.kind >= 0 && value.kind != kind
                    && !(numericKind(value.kind) && numericKind(kind))) {
                throw error("Incompatible expression storage types");
            }
            if (value.parameter != null) {
                value.parameter.expectedKind = kind;
            } else if (value.kind < 0) {
                for (Expr child : value.children) {
                    requireKind(child, kind);
                }
            }
        }
        private Expr rowExpression() {
            Expr value = expression();
            if (hasAggregate(value)) {
                throw error("Aggregate expressions are not allowed directly in bulk clauses");
            }
            return value;
        }
        private boolean flexibleMapping(Expr value) {
            if (value.literal || value.parameter != null) {
                return true;
            }
            if (value.projectionQuery != null || value.query != null || !value.numericOperands
                    || value.children.isEmpty()) {
                return false;
            }
            for (Expr child : value.children) {
                if (!flexibleMapping(child)) {
                    return false;
                }
            }
            return true;
        }
        private void compatibleMappings(Expr left, Expr right) {
            if (flexibleMapping(left) || flexibleMapping(right)) {
                return;
            }
            String first = left.projectionQuery == null ? "" : left.projectionQuery.mapping(left.projectionField);
            String second = right.projectionQuery == null ? "" : right.projectionQuery.mapping(right.projectionField);
            if (!first.equals(second)) {
                throw error("Comparison requires matching domain and converter mappings");
            }
        }
        private void compatible(Expr left, Expr right) {
            compatibleMappings(left, right);
            if (left.kind < 0 && right.kind < 0) {
                throw error("Comparison requires at least one known storage kind");
            }
            if (left.kind >= 0) {
                requireKind(right, left.kind);
            }
            if (right.kind >= 0) {
                requireKind(left, right.kind);
            }
        }
        private String condition(Expr value) {
            requireKind(value, Attribute.BOOLEAN);
            if (value.parameter != null) {
                value.parameter.logical = true;
            }
            return value.predicate ? value.sql : "(" + value.sql + " <> 0)";
        }
        private Expr logical(Expr left, String op, Expr right) {
            return node("(" + condition(left) + " " + op + " " + condition(right) + ")", Attribute.BOOLEAN, left, right);
        }
        private Expr arithmetic(Expr left, String op, Expr right) {
            int kind = left.kind == Attribute.REAL || right.kind == Attribute.REAL ? Attribute.REAL : Attribute.BIGINT;
            if ("%".equals(op) && kind == Attribute.REAL) {
                throw error("Remainder requires integral operands");
            }
            requireNumeric(left, kind != Attribute.REAL);
            requireNumeric(right, kind != Attribute.REAL);
            Expr result = binary(left, op, right, kind);
            result.numericOperands = true;
            result.nullPreserving = !("/".equals(op) || "%".equals(op))
                    || right.literal && right.literalValue instanceof Number
                            && ((Number) right.literalValue).doubleValue() != 0;
            result.sql = session.arithmetic(left.sql, op, right.sql, kind);
            return result;
        }
        private void requireNumeric(Expr value, boolean integral) {
            if (value.entity != null || value.kind >= 0 && value.kind != Attribute.INTEGER
                    && value.kind != Attribute.BIGINT && value.kind != Attribute.REAL) {
                throw error("Arithmetic requires numeric operands");
            }
            if (value.parameter != null) {
                value.parameter.numeric = true;
                value.parameter.integral |= integral;
            }
            if (value.kind < 0 || value.numericOperands) {
                for (Expr child : value.children) {
                    requireNumeric(child, integral);
                }
            }
        }
        private boolean hasAggregate(Expr value) {
            if (value.aggregate) {
                return true;
            }
            for (Expr child : value.children) {
                if (hasAggregate(child)) {
                    return true;
                }
            }
            return false;
        }
        private String bindingShape(String sql) {
            StringBuilder result = new StringBuilder();
            int position = 0;
            int start;
            while ((start = sql.indexOf("/*cn1-bind-", position)) >= 0) {
                result.append(sql.substring(position, start)).append('?');
                position = sql.indexOf("*/?", start) + 3;
            }
            return result.append(sql.substring(position)).toString();
        }
        private boolean sameExpression(Expr left, Expr right) {
            if (!bindingShape(left.sql).equals(bindingShape(right.sql)) || left.literal != right.literal
                    || left.children.size() != right.children.size()) {
                return false;
            }
            if (left.literal && !(left.literalValue == null ? right.literalValue == null : left.literalValue.equals(right.literalValue))) {
                return false;
            }
            if (left.parameter != null) {
                if (right.parameter == null || !left.parameter.name.equals(right.parameter.name)) {
                    return false;
                }
            } else if (right.parameter != null) {
                return false;
            }
            for (int i = 0; i < left.children.size(); i++) {
                if (!sameExpression(left.children.get(i), right.children.get(i))) {
                    return false;
                }
            }
            return true;
        }
        private boolean hasRowAggregate(Expr value) {
            // An aggregate referencing only an outer query belongs to that outer
            // scope; it does not reduce the rows produced by this nested query.
            if (value.aggregate) {
                return aggregateScope(value) != 2;
            }
            for (Expr child : value.children) {
                if (hasRowAggregate(child)) {
                    return true;
                }
            }
            return false;
        }
        private int aggregateScope(Expr value) {
            int scope = value.query == null ? 0 : SessionImpl.sameInstance(value.query, root) ? 1 : 2;
            for (Expr child : value.children) {
                scope |= aggregateScope(child);
            }
            return scope;
        }
        private void validateGrouped(Expr value, List<Expr> groups) {
            if (value.aggregate || value.query != null && !SessionImpl.sameInstance(value.query, root)) {
                return;
            }
            for (Expr group : groups) {
                if (sameExpression(value, group)) {
                    return;
                }
            }
            if (!value.children.isEmpty()) {
                for (Expr child : value.children) {
                    validateGrouped(child, groups);
                }
            } else if (value.entity != null || value.field != null) {
                throw error("Non-aggregate fields must be covered by GROUP BY");
            }
        }
        private boolean peek(String value) {
            return value.equalsIgnoreCase(peek());
        }
        private String peek() {
            return position < tokens.size() ? tokens.get(position) : "";
        }
        private String next() {
            if (position >= tokens.size()) {
                throw error("Unexpected end of query");
            }
            return tokens.get(position++);
        }
        private boolean take(String value) {
            if (peek(value)) {
                position++;
                return true;
            }
            return false;
        }
        private void expect(String value) {
            if (!take(value)) {
                throw error("Expected " + value);
            }
        }
        void end() {
            if (position != tokens.size()) {
                throw error("Unsupported syntax: " + peek());
            }
        }
        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at token " + position);
        }
    }
    private static String upper(String token) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            result.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return result.toString();
    }
    private static boolean clause(String token) {
        return "WHERE".equalsIgnoreCase(token) || "JOIN".equalsIgnoreCase(token) || "LEFT".equalsIgnoreCase(token) ||
                "INNER".equalsIgnoreCase(token) || "GROUP".equalsIgnoreCase(token) ||
                "HAVING".equalsIgnoreCase(token) || "ORDER".equalsIgnoreCase(token) || "SET".equalsIgnoreCase(token);
    }
    private static boolean identifier(String token) {
        if (token.length() == 0 || !Character.isJavaIdentifierStart(token.charAt(0))) {
            return false;
        }
        for (int i = 1; i < token.length(); i++) {
            if (!Character.isJavaIdentifierPart(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    private static List<String> lex(String text) {
        List<String> tokens = new ArrayList<String>();
        for (int i = 0; i < text.length();) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i++;
            if (c == '\'') {
                boolean closed = false;
                while (i < text.length()) {
                    char current = text.charAt(i);
                    i++;
                    if (current == '\'') {
                        if (i < text.length() && text.charAt(i) == '\'') {
                            i++;
                        } else {
                            closed = true;
                            break;
                        }
                    }
                }
                if (!closed) {
                    throw new IllegalArgumentException("Unterminated string literal");
                }
            } else if (c == ':') {
                if (i >= text.length() || !Character.isJavaIdentifierStart(text.charAt(i))) {
                    throw new IllegalArgumentException("Invalid named parameter");
                }
                while (i < text.length() && Character.isJavaIdentifierPart(text.charAt(i))) {
                    i++;
                }
            } else if (Character.isJavaIdentifierStart(c)) {
                while (i < text.length() && Character.isJavaIdentifierPart(text.charAt(i))) {
                    i++;
                }
            } else if (Character.isDigit(c)) {
                while (i < text.length() && Character.isDigit(text.charAt(i))) {
                    i++;
                }
                if (i < text.length() && text.charAt(i) == '.') {
                    i++;
                    while (i < text.length() && Character.isDigit(text.charAt(i))) {
                        i++;
                    }
                }
                if (i < text.length() && (text.charAt(i) == 'e' || text.charAt(i) == 'E')) {
                    i++;
                    if (i < text.length() && (text.charAt(i) == '+' || text.charAt(i) == '-')) {
                        i++;
                    }
                    int exponent = i;
                    while (i < text.length() && Character.isDigit(text.charAt(i))) {
                        i++;
                    }
                    if (i == exponent) {
                        throw new IllegalArgumentException("Missing numeric exponent");
                    }
                }
            } else if ("=<>!".indexOf(c) >= 0) {
                if (i < text.length() && (text.charAt(i) == '=' || c == '<' && text.charAt(i) == '>')) {
                    i++;
                }
            } else if ("(),.+-*/%".indexOf(c) < 0) {
                throw new IllegalArgumentException("Unsupported query character: " + c);
            }
            tokens.add(text.substring(start, i));
        }
        return tokens;
    }
}
