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
            if (binding instanceof Parameter && ((Parameter) binding).name.equals(name)) {
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
        List<Object> values = new ArrayList<Object>();
        String statement = plan.sql;
        for (Object binding : plan.bindings) {
            if (binding instanceof Parameter) {
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
                        count++;
                    }
                    statement = statement.replace(
                            parameter.marker, count == 0 ? "SELECT NULL WHERE 1=0" : SessionImpl.placeholders(count));
                } else {
                    if (value instanceof Iterable || value instanceof Object[]) {
                        throw new IllegalArgumentException("Collection parameter requires IN: " + parameter.name);
                    }
                    values.add(parameter.convert(value));
                }
            } else {
                values.add(Values.storage(binding));
            }
        }
        return new Bound(statement, values.toArray());
    }
    private static final class Parameter {
        final String name;
        String marker;
        QueryImpl query;
        String field;
        Parameter(String name) {
            this.name = name;
        }
        Object convert(Object value) {
            return query == null ? Values.storage(value) : query.parameter(field, value);
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
        final String sql;
        final int kind;
        final EntityModel entity;
        QueryImpl query;
        String field;
        Parameter parameter;
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
        String sql;
        int[] kinds = new int[0];
        EntityModel entity;
        boolean mutation;
        final List<Object> bindings;
        final List<String> fetches = new ArrayList<String>();
        Plan(List<Object> bindings) {
            this.bindings = bindings;
        }
    }
    private static final class Parser {
        final SessionImpl session;
        final List<String> tokens;
        final List<Object> bindings;
        final Map<String, Alias> aliases;
        int position;
        QueryImpl root;
        String rootName;
        Parser(SessionImpl session, List<String> tokens, List<Object> bindings, Map<String, Alias> outer) {
            this.session = session;
            this.tokens = tokens;
            this.bindings = bindings;
            this.aliases = new LinkedHashMap<String, Alias>(outer);
        }
        Plan parse() {
            Plan plan = new Plan(bindings);
            if (take("UPDATE")) {
                plan.mutation = true;
                root();
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
                    Attribute attribute = root.model.attributes()[root.model.index(field)];
                    if (attribute.id || root.model.discriminatorIndex() == root.model.index(field)) {
                        throw error("Bulk updates cannot change entity identifiers");
                    }
                    expect("=");
                    Expr value = expression();
                    Expr target = new Expr(session.q(attribute.column), attribute.kind);
                    target.query = root;
                    target.field = field;
                    bindType(target, value);
                    assignments.append(target.sql).append(" = ").append(value.sql);
                } while (take(","));
                String where = take("WHERE") ? " WHERE " + expression().sql : "";
                String filter = session.discriminatorCondition(root.model, root.rootAlias);
                if (filter.length() > 0) {
                    where = where.length() == 0 ? " WHERE " + filter : where + " AND " + filter;
                }
                plan.sql = "UPDATE " + session.q(root.model.table()) + " AS " + root.rootAlias + " SET " + assignments +
                           where;
                refuseImplicitBulkJoins();
                return plan;
            }
            if (take("DELETE")) {
                plan.mutation = true;
                expect("FROM");
                root(true);
                String where = take("WHERE") ? " WHERE " + expression().sql : "";
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
                    root.model.relationIndex(joinPath);
                    plan.fetches.add(joinPath);
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
            String where = take("WHERE") ? " WHERE " + expression().sql : "";
            String group = "";
            String having = "";
            String order = "";
            if (take("GROUP")) {
                expect("BY");
                group = " GROUP BY " + expressions();
            }
            if (take("HAVING")) {
                having = " HAVING " + expression().sql;
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
                    List<Object> termBindings = new ArrayList<Object>(bindings.subList(firstBinding, bindings.size()));
                    boolean ascending = !take("DESC");
                    if (ascending) {
                        take("ASC");
                    }
                    String rendered = session.orderBy(term.sql, ascending, term.kind);
                    // Some dialects repeat a term to normalize NULL placement.
                    // Each repeated expression also needs its own bound values.
                    for (int occurrence = rendered.indexOf(term.sql) + term.sql.length();
                            (occurrence = rendered.indexOf(term.sql, occurrence)) >= 0;
                            occurrence += term.sql.length()) {
                        bindings.addAll(termBindings);
                    }
                    out.append(rendered);
                } while (take(","));
                order = " ORDER BY " + out;
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
                    kinds.add(Integer.valueOf(expr.kind));
                }
            }
            if (!plan.fetches.isEmpty() && !SessionImpl.sameInstance(plan.entity, root.model)) {
                throw error("Fetch joins require the root entity projection");
            }
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
        private void root(boolean delete) {
            String entity = path();
            EntityModel model = session.model(entity);
            root = delete ? session.queryForDelete(model) : session.query(model.type());
            take("AS");
            rootName = identifier(peek()) && !clause(peek()) ? next() : entity.substring(entity.lastIndexOf('.') + 1);
            aliases.put(rootName, new Alias(root, ""));
        }
        private String expressions() {
            StringBuilder out = new StringBuilder();
            do {
                if (out.length() > 0) {
                    out.append(", ");
                }
                out.append(expression().sql);
            } while (take(","));
            return out.toString();
        }
        private Expr expression() {
            return or();
        }
        private Expr or() {
            Expr left = and();
            while (take("OR")) {
                left = binary(left, "OR", and(), Attribute.BOOLEAN);
            }
            return left;
        }
        private Expr and() {
            Expr left = compare();
            while (take("AND")) {
                left = binary(left, "AND", compare(), Attribute.BOOLEAN);
            }
            return left;
        }
        private Expr compare() {
            if (take("NOT")) {
                return new Expr("NOT (" + compare().sql + ")", Attribute.BOOLEAN);
            }
            if (take("EXISTS")) {
                expect("(");
                String sql = subquery();
                expect(")");
                return new Expr("EXISTS (" + sql + ")", Attribute.BOOLEAN);
            }
            Expr left = add();
            if (take("IS")) {
                boolean not = take("NOT");
                expect("NULL");
                return new Expr(left.sql + (not ? " IS NOT NULL" : " IS NULL"), Attribute.BOOLEAN);
            }
            boolean not = take("NOT");
            if (take("IN")) {
                boolean parens = take("(");
                String values;
                if (peek() != null && peek().startsWith(":") &&
                        (!parens || position + 1 < tokens.size() && ")".equals(tokens.get(position + 1)))) {
                    Parameter parameter = new Parameter(next().substring(1));
                    parameter.query = left.query;
                    parameter.field = left.field;
                    parameter.marker = "/*cn1-list-" + bindings.size() + "*/?";
                    bindings.add(parameter);
                    values = parameter.marker;
                } else {
                    if (!parens) {
                        throw error("Expected IN parameter or parenthesized values");
                    }
                    if (peek("SELECT") || peek("FROM")) {
                        values = subquery();
                    } else {
                        StringBuilder list = new StringBuilder();
                        do {
                            if (list.length() > 0) {
                                list.append(", ");
                            }
                            Expr value = expression();
                            bindType(left, value);
                            list.append(value.sql);
                        } while (take(","));
                        values = list.toString();
                    }
                }
                if (parens) {
                    expect(")");
                }
                return new Expr(left.sql + (not ? " NOT IN (" : " IN (") + values + ")", Attribute.BOOLEAN);
            }
            if (take("BETWEEN")) {
                Expr low = add();
                expect("AND");
                Expr high = add();
                bindType(left, low);
                bindType(left, high);
                return new Expr(left.sql + (not ? " NOT BETWEEN " : " BETWEEN ") + low.sql + " AND " + high.sql,
                        Attribute.BOOLEAN);
            }
            if (take("LIKE")) {
                Expr pattern = add();
                bindType(left, pattern);
                String sql = left.sql + (not ? " NOT LIKE " : " LIKE ") + pattern.sql;
                if (take("ESCAPE")) {
                    sql += " ESCAPE " + add().sql;
                }
                return new Expr(sql, Attribute.BOOLEAN);
            }
            if (not) {
                throw error("Expected IN, LIKE or BETWEEN after NOT");
            }
            String op = peek();
            if ("=".equals(op) || "<>".equals(op) || "!=".equals(op) || ">".equals(op) || "<".equals(op) ||
                    ">=".equals(op) || "<=".equals(op)) {
                next();
                Expr right = add();
                bindType(left, right);
                bindType(right, left);
                return binary(left, op, right, Attribute.BOOLEAN);
            }
            return left;
        }
        private Expr add() {
            Expr left = multiply();
            while (peek("+") || peek("-")) {
                String op = next();
                Expr right = multiply();
                left = binary(left, op, right, numeric(left, right));
            }
            return left;
        }
        private Expr multiply() {
            Expr left = primary();
            while (peek("*") || peek("/") || peek("%")) {
                String op = next();
                Expr right = primary();
                left = binary(left, op, right, numeric(left, right));
            }
            return left;
        }
        private Expr primary() {
            if (take("-")) {
                Expr value = primary();
                return new Expr("-" + value.sql, value.kind);
            }
            if (take("+")) {
                return primary();
            }
            if (take("(")) {
                if (peek("SELECT") || peek("FROM")) {
                    String sql = subquery();
                    expect(")");
                    return new Expr("(" + sql + ")", Attribute.BIGINT);
                }
                Expr value = expression();
                expect(")");
                return new Expr("(" + value.sql + ")", value.kind, value.entity);
            }
            String token = next();
            if (token.startsWith(":")) {
                Parameter parameter = new Parameter(token.substring(1));
                bindings.add(parameter);
                Expr result = new Expr("?", Attribute.TEXT);
                result.parameter = parameter;
                return result;
            }
            if (token.startsWith("'")) {
                bindings.add(token.substring(1, token.length() - 1).replace("''", "'"));
                return new Expr("?", Attribute.TEXT);
            }
            if ("NULL".equalsIgnoreCase(token)) {
                return new Expr("NULL", Attribute.TEXT);
            }
            if ("TRUE".equalsIgnoreCase(token) || "FALSE".equalsIgnoreCase(token)) {
                return new Expr("TRUE".equalsIgnoreCase(token) ? "1" : "0", Attribute.BOOLEAN);
            }
            if (Character.isDigit(token.charAt(0))) {
                Object value = token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0
                                       ? (Object) Double.valueOf(token)
                                       : Long.valueOf(Long.parseLong(token));
                bindings.add(value);
                return new Expr("?", value instanceof Double ? Attribute.REAL : Attribute.BIGINT);
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
                StringBuilder sql = new StringBuilder(function).append('(').append(distinct ? "DISTINCT " : "");
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
                        sql.append(root.rootAlias)
                                .append('.')
                                .append(session.q(arg.entity.attributes()[arg.entity.idIndex()].column));
                    } else {
                        sql.append(arg.sql);
                    }
                }
                int kind = "COUNT".equals(function) || "LENGTH".equals(function) ? Attribute.BIGINT
                           : "AVG".equals(function)                                ? Attribute.REAL
                                                                                     : args.get(0).kind;
                return new Expr(sql.append(')').toString(), kind);
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
                return new Expr(alias.query.rootColumns(), Attribute.BIGINT, alias.query.model);
            }
            String field = alias.field(full.substring(dot + 1));
            Expr result = new Expr(alias.query.column(field), alias.query.kind(field));
            result.query = alias.query;
            result.field = field;
            return result;
        }
        private void bindType(Expr attribute, Expr value) {
            if (attribute.query != null && value.parameter != null) {
                value.parameter.query = attribute.query;
                value.parameter.field = attribute.field;
            }
        }
        private String subquery() {
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
            Plan plan = nested.parse();
            nested.end();
            if (plan.mutation || plan.entity != null || plan.kinds.length != 1) {
                throw error("Subquery requires a single scalar projection");
            }
            return plan.sql;
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
        private Expr binary(Expr left, String op, Expr right, int kind) {
            if (left.entity != null || right.entity != null) {
                throw error("Compare entity identifiers explicitly");
            }
            return new Expr("(" + left.sql + " " + op + " " + right.sql + ")", kind);
        }
        private int numeric(Expr a, Expr b) {
            return a.kind == Attribute.REAL || b.kind == Attribute.REAL ? Attribute.REAL : Attribute.BIGINT;
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
