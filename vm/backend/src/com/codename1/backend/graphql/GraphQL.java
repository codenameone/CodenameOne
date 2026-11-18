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
package com.codename1.backend.graphql;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.log.Logger;
import com.codename1.backend.security.AccessDeniedException;
import com.codename1.backend.security.AuthenticationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A GraphQL engine: a [GraphQLSchema] with the resolvers of its fields,
/// executing queries, mutations and subscriptions.
///
/// The build makes one for a backend with `.graphqls` files under
/// `src/main/resources/graphql`, binding each `@QueryMapping`,
/// `@MutationMapping`, `@SubscriptionMapping` and `@SchemaMapping` method as a
/// [DataFetcher], and serves it at `/graphql`. A program can also build one by
/// hand:
///
/// ```java
/// GraphQL graphQL = GraphQL.newGraphQL(GraphQLSchema.parse(sdl))
///     .fetcher("Query", "book", env -> books.find((String) env.getArgument("id")))
///     .build();
/// Map result = graphQL.execute("{ book(id: \"1\") { title } }");
/// ```
///
/// Results are maps in the specification's response shape: `data`, and
/// `errors` when there were any, each with `message`, `locations`, `path` and
/// `extensions.classification`. A resolver failure nulls its field and is
/// reported without stopping the rest; a null in a non-null field nulls the
/// nearest nullable parent. Mutation fields run in order, one after another.
///
/// An exception from a resolver is reported by classification, as Spring for
/// GraphQL does: [GraphQLException] with its own message, an
/// `AccessDeniedException` as `FORBIDDEN`, an `AuthenticationException` as
/// `UNAUTHORIZED`, a `ResponseStatusException` by its status (`NOT_FOUND`,
/// `BAD_REQUEST`, ...), and anything else as `INTERNAL_ERROR` with no detail,
/// logged on the server.
public final class GraphQL {
    private static final Logger LOG = Logger.getLogger(GraphQL.class);

    private final GraphQLSchema schema;
    private final Map<String, DataFetcher> fetchers;
    private final Map<String, BatchLoader> batchLoaders;
    private final Map<String, TypeResolver> typeResolvers;
    private final Values values;
    private final boolean introspection;
    private final int maxDepth;
    private final int maxFields;
    private final Object rootValue;
    /// Built once, here: the schema never changes after the engine is built.
    private final Map<String, Object> introspectionValue;

    private GraphQL(Builder b) {
        this.schema = b.schema;
        this.fetchers = new HashMap<String, DataFetcher>(b.fetchers);
        this.batchLoaders = new HashMap<String, BatchLoader>(b.batchLoaders);
        this.typeResolvers = new HashMap<String, TypeResolver>(b.typeResolvers);
        this.values = new Values(schema, new HashMap<String, GraphQLScalar>(b.scalars));
        this.introspection = b.introspection;
        this.maxDepth = b.maxDepth;
        this.maxFields = b.maxFields;
        this.rootValue = b.rootValue;
        this.introspectionValue = b.introspection ? Introspection.of(schema) : null;
    }

    /// Starts building an engine for `schema`.
    public static Builder newGraphQL(GraphQLSchema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("No schema");
        }
        return new Builder(schema);
    }

    /// The schema this engine executes.
    public GraphQLSchema getSchema() {
        return schema;
    }

    /// Whether `__schema` and `__type` can be queried.
    public boolean isIntrospectionEnabled() {
        return introspection;
    }

    /// Executes a document with one operation and no variables.
    public Map<String, Object> execute(String query) {
        return execute(query, null, null, null);
    }

    /// Executes one operation of `query`.
    ///
    /// #### Parameters
    ///
    /// - `operationName`: the operation to run; may be null when the document has one
    ///
    /// - `variables`: the variables as parsed from JSON; may be null
    ///
    /// - `context`: what resolvers see as [DataFetchingEnvironment#getContext()]
    ///
    /// #### Returns
    ///
    /// the response, a map with `data` and, when there were errors, `errors`.
    /// A subscription answers an error here; see [#subscribe].
    public Map<String, Object> execute(String query, String operationName, Map variables,
                                       Object context) {
        Prepared p = prepare(query, operationName, variables);
        if (p.errors != null) {
            return p.errors;
        }
        if ("subscription".equals(p.op.type)) {
            return errorsOnly(new GraphQLException("Subscriptions are served over WebSocket, "
                    + "with the graphql-transport-ws protocol.", p.op.line, p.op.column), null);
        }
        Execution ex = new Execution(p, context);
        SchemaType root = schema.rootType(p.op.type);
        Object data;
        try {
            data = ex.executeSelectionSet(root, rootValue, p.op.selections,
                    new ArrayList<Object>(), "mutation".equals(p.op.type));
        } catch (NullPropagation np) {
            data = null;
        }
        return ex.result(data);
    }

    /// Receives what a subscription produces.
    public interface SubscriptionListener {
        /// One response, a map like [#execute] returns, for one event.
        void next(Map<String, Object> result);

        /// The subscription failed before or while streaming; `result` has
        /// `errors` only. Nothing follows.
        void error(Map<String, Object> result);

        /// The stream ended. Nothing follows.
        void complete();
    }

    /// Starts a subscription: validates the document, calls the subscription
    /// field's resolver for its [GraphQLStream], and resolves each event
    /// against the selection set into a response for `listener`.
    ///
    /// #### Returns
    ///
    /// what cancels the subscription; null when it failed at once, in which
    /// case `listener` has had `error`
    public Runnable subscribe(String query, String operationName, Map variables,
                              final Object context, final SubscriptionListener listener) {
        final Prepared p = prepare(query, operationName, variables);
        if (p.errors != null) {
            listener.error(p.errors);
            return null;
        }
        if (!"subscription".equals(p.op.type)) {
            // Over the subscription transport a query or mutation is one response.
            Map<String, Object> r = execute(query, operationName, variables, context);
            listener.next(r);
            listener.complete();
            return NOTHING_TO_CANCEL;
        }
        final SchemaType root = schema.rootType("subscription");
        Execution probe = new Execution(p, context);
        Map<String, List<Parser.Field>> fields = probe.collectFields(root, p.op.selections);
        Map.Entry<String, List<Parser.Field>> only = fields.entrySet().iterator().next();
        final String key = only.getKey();
        final List<Parser.Field> fieldNodes = only.getValue();
        Parser.Field first = fieldNodes.get(0);
        FieldDef def = root.fields.get(first.name);
        final List<Object> path = new ArrayList<Object>();
        path.add(key);
        Object source;
        try {
            Map<String, Object> args = probe.arguments(def, first);
            DataFetcher f = fetchers.get(root.name + "." + first.name);
            if (f == null) {
                throw new GraphQLException("No resolver for subscription field "
                        + root.name + "." + first.name + ".");
            }
            source = f.get(new DataFetchingEnvironment(rootValue, args, context, first.name,
                    root.name, path, p.variables));
        } catch (Throwable t) {
            probe.fieldError(t, first, path);
            listener.error(probe.result(null));
            return null;
        }
        if (!(source instanceof GraphQLStream)) {
            probe.fieldError(new GraphQLException("Subscription field " + root.name + "."
                    + first.name + " must return a GraphQLStream."), first, path);
            listener.error(probe.result(null));
            return null;
        }
        final boolean[] done = new boolean[1];
        GraphQLStream.Emitter emitter = new GraphQLStream.Emitter() {
            @Override
            public void next(Object event) {
                synchronized (done) {
                    if (done[0]) {
                        return;
                    }
                }
                Execution ex = new Execution(p, context);
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                try {
                    data.put(key, ex.completeField(root, fieldNodes, event, path));
                } catch (NullPropagation np) {
                    data = null;
                }
                listener.next(ex.result(data));
            }

            @Override
            public void error(Throwable error) {
                synchronized (done) {
                    if (done[0]) {
                        return;
                    }
                    done[0] = true;
                }
                Execution ex = new Execution(p, context);
                ex.fieldError(error, fieldNodes.get(0), path);
                listener.error(ex.result(null));
            }

            @Override
            public void complete() {
                synchronized (done) {
                    if (done[0]) {
                        return;
                    }
                    done[0] = true;
                }
                listener.complete();
            }
        };
        final Runnable stop;
        try {
            stop = ((GraphQLStream) source).open(emitter);
        } catch (Throwable t) {
            emitter.error(t);
            return null;
        }
        return new Cancel(done, stop);
    }

    private static final Runnable NOTHING_TO_CANCEL = new Cancel(new boolean[1], null);

    /// Stops a subscription: no event is delivered after it, and the stream's
    /// own cancellation runs.
    private static final class Cancel implements Runnable {
        private final boolean[] done;
        private final Runnable stop;

        Cancel(boolean[] done, Runnable stop) {
            this.done = done;
            this.stop = stop;
        }

        @Override
        public void run() {
            synchronized (done) {
                done[0] = true;
            }
            if (stop != null) {
                stop.run();
            }
        }
    }

    /// The type of the operation `query` would run -- `query`, `mutation` or
    /// `subscription` -- or null when it does not parse or names no operation.
    String operationType(String query, String operationName) {
        try {
            Parser.Document doc = new Parser(query).parseDocument();
            for (Parser.Operation o : doc.operations) {
                if (operationName == null || operationName.length() == 0
                        ? doc.operations.size() == 1 : operationName.equals(o.name)) {
                    return o.type;
                }
            }
        } catch (GraphQLException e) {
            return null;
        }
        return null;
    }

    // ---------------------------------------------------------------- prepare

    /// A parsed, validated operation with its coerced variables, or the errors
    /// that stopped it.
    private static final class Prepared {
        Parser.Document doc;
        Parser.Operation op;
        Map<String, Object> variables;
        Map<String, Object> errors;
    }

    private Prepared prepare(String query, String operationName, Map variables) {
        Prepared p = new Prepared();
        if (query == null || query.length() == 0) {
            p.errors = errorsOnly(new GraphQLException("No query was given."), null);
            return p;
        }
        try {
            p.doc = new Parser(query).parseDocument();
        } catch (GraphQLException e) {
            p.errors = errorsOnly(e, null);
            return p;
        }
        List<GraphQLException> invalid = new Validator(schema, p.doc, values, introspection,
                maxDepth, maxFields).validate();
        if (!invalid.isEmpty()) {
            p.errors = errorsOnly(null, invalid);
            return p;
        }
        if (operationName == null || operationName.length() == 0) {
            if (p.doc.operations.size() != 1) {
                p.errors = errorsOnly(new GraphQLException(p.doc.operations.isEmpty()
                        ? "The document has no operation." : "Must provide operation name if "
                        + "query contains multiple operations."), null);
                return p;
            }
            p.op = p.doc.operations.get(0);
        } else {
            for (Parser.Operation o : p.doc.operations) {
                if (operationName.equals(o.name)) {
                    p.op = o;
                }
            }
            if (p.op == null) {
                p.errors = errorsOnly(new GraphQLException("Unknown operation named \""
                        + operationName + "\"."), null);
                return p;
            }
        }
        Map<String, Object> coerced = new LinkedHashMap<String, Object>();
        Map in = variables == null ? Collections.EMPTY_MAP : variables;
        List<GraphQLException> varErrors = new ArrayList<GraphQLException>();
        for (Parser.VariableDef v : p.op.variables) {
            String where = "Variable \"$" + v.name + "\" got invalid value";
            try {
                if (in.containsKey(v.name)) {
                    coerced.put(v.name, values.coerceInput(in.get(v.name), v.type, where));
                } else if (v.hasDefault) {
                    coerced.put(v.name, values.coerceLiteral(v.defaultValue, v.type, null, where));
                } else if (v.type.isNonNull()) {
                    throw new GraphQLException("Variable \"$" + v.name + "\" of required type \""
                            + v.type + "\" was not provided.");
                }
            } catch (GraphQLException e) {
                varErrors.add(new GraphQLException(e.getMessage(), v.line, v.column));
            }
        }
        if (!varErrors.isEmpty()) {
            p.errors = errorsOnly(null, varErrors);
            return p;
        }
        p.variables = coerced;
        return p;
    }

    private static Map<String, Object> errorsOnly(GraphQLException one,
                                                  List<GraphQLException> many) {
        List<Object> errors = new ArrayList<Object>();
        if (one != null) {
            errors.add(errorMap(one.getMessage(), one.getLine(), one.getColumn(), null,
                    one.getExtensions() != null ? one.getExtensions() : classification("ValidationError")));
        }
        if (many != null) {
            for (GraphQLException e : many) {
                errors.add(errorMap(e.getMessage(), e.getLine(), e.getColumn(), null,
                        classification("ValidationError")));
            }
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("errors", errors);
        return out;
    }

    private static Map<String, Object> classification(String c) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("classification", c);
        return m;
    }

    private static Map<String, Object> errorMap(String message, int line, int column,
                                                List<Object> path, Map extensions) {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("message", message);
        if (line > 0) {
            List<Object> locations = new ArrayList<Object>();
            Map<String, Object> l = new LinkedHashMap<String, Object>();
            l.put("line", Integer.valueOf(line));
            l.put("column", Integer.valueOf(column));
            locations.add(l);
            e.put("locations", locations);
        }
        if (path != null) {
            e.put("path", new ArrayList<Object>(path));
        }
        if (extensions != null && !extensions.isEmpty()) {
            e.put("extensions", extensions);
        }
        return e;
    }

    /// Thrown up the tree when a non-null field resolved to null; the error is
    /// already recorded, and the nearest nullable parent becomes null.
    private static final class NullPropagation extends RuntimeException {
        NullPropagation() {
            super("null propagation");
        }
    }

    // -------------------------------------------------------------- execution

    /// One execution of an operation: its errors and its per-request caches.
    private final class Execution {
        private final Prepared p;
        private final Object context;
        private final List<Object> errors = new ArrayList<Object>();
        /// Objects the default resolver has read, as their JSON trees.
        private final Map<Object, Object> trees = new IdentityHashMap<Object, Object>();

        Execution(Prepared p, Object context) {
            this.p = p;
            this.context = context;
        }

        Map<String, Object> result(Object data) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            if (!errors.isEmpty()) {
                out.put("errors", errors);
            }
            out.put("data", data);
            return out;
        }

        Map<String, List<Parser.Field>> collectFields(SchemaType type,
                                                      List<Parser.Selection> sels) {
            Map<String, List<Parser.Field>> out = new LinkedHashMap<String, List<Parser.Field>>();
            collect(type, sels, out, new HashSet<String>());
            return out;
        }

        private void collect(SchemaType type, List<Parser.Selection> sels,
                             Map<String, List<Parser.Field>> out, Set<String> visited) {
            for (Parser.Selection s : sels) {
                if (!included(s.directives)) {
                    continue;
                }
                if (s instanceof Parser.Field) {
                    Parser.Field f = (Parser.Field) s;
                    List<Parser.Field> l = out.get(f.responseKey());
                    if (l == null) {
                        l = new ArrayList<Parser.Field>();
                        out.put(f.responseKey(), l);
                    }
                    l.add(f);
                } else if (s instanceof Parser.InlineFragment) {
                    Parser.InlineFragment f = (Parser.InlineFragment) s;
                    if (f.typeCondition == null || applies(type, f.typeCondition)) {
                        collect(type, f.selections, out, visited);
                    }
                } else {
                    String name = ((Parser.FragmentSpread) s).name;
                    if (!visited.add(name)) {
                        continue;
                    }
                    Parser.Fragment f = p.doc.fragments.get(name);
                    if (f != null && applies(type, f.typeCondition)) {
                        collect(type, f.selections, out, visited);
                    }
                }
            }
        }

        private boolean applies(SchemaType object, String condition) {
            SchemaType c = schema.type(condition);
            return c != null && schema.isPossibleType(c, object);
        }

        private boolean included(List<Parser.Directive> ds) {
            if (ds == null) {
                return true;
            }
            for (Parser.Directive d : ds) {
                if ("skip".equals(d.name) || "include".equals(d.name)) {
                    Object v = values.coerceLiteral(d.arguments.get("if"),
                            TypeRef.nonNull(TypeRef.named("Boolean")), p.variables, "@" + d.name);
                    boolean flag = Boolean.TRUE.equals(v);
                    if ("skip".equals(d.name) == flag) {
                        return false;
                    }
                }
            }
            return true;
        }

        Map<String, Object> executeSelectionSet(SchemaType type, Object source,
                                                List<Parser.Selection> sels, List<Object> path,
                                                boolean serial) {
            // Serial or not, fields run in order on this thread; "serial" is
            // what the specification requires of mutations, and it holds.
            Map<String, List<Parser.Field>> fields = collectFields(type, sels);
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, List<Parser.Field>> e : fields.entrySet()) {
                path.add(e.getKey());
                try {
                    out.put(e.getKey(), executeField(type, source, e.getValue(), path));
                } finally {
                    path.remove(path.size() - 1);
                }
            }
            return out;
        }

        private Object executeField(SchemaType type, Object source, List<Parser.Field> nodes,
                                    List<Object> path) {
            Parser.Field f = nodes.get(0);
            if ("__typename".equals(f.name)) {
                return type.name;
            }
            FieldDef def = type.fields.get(f.name);
            Object value;
            if (def == null) {
                // Validation admitted it, so it is a meta field of the query root.
                if ("__schema".equals(f.name)) {
                    def = Validator.META_SCHEMA;
                    value = introspection();
                } else {
                    def = Validator.META_TYPE;
                    Object n = arguments(def, f).get("name");
                    value = Introspection.type(introspection(), String.valueOf(n));
                }
                return complete(def.type, nodes, value, path);
            }
            try {
                Map<String, Object> args = arguments(def, f);
                DataFetcher fetcher = fetchers.get(type.name + "." + f.name);
                BatchLoader loader = fetcher == null ? batchLoaders.get(type.name + "." + f.name)
                        : null;
                if (loader != null) {
                    value = batched(type, f, loader, source, path);
                } else if (fetcher != null) {
                    value = fetcher.get(new DataFetchingEnvironment(source, args, context,
                            f.name, type.name, new ArrayList<Object>(path), p.variables));
                } else if (type.name.startsWith("__")) {
                    value = introspectionField(source, f.name, args);
                } else {
                    value = property(source, f.name);
                }
            } catch (Throwable t) {
                fieldError(t, f, path);
                if (def.type.isNonNull()) {
                    throw new NullPropagation(); // NOPMD PreserveStackTrace - a signal; the error was recorded
                }
                return null;
            }
            return complete(def.type, nodes, value, path);
        }

        /// Completes the value of a subscription event, which stands in for
        /// the root field's resolved value.
        Object completeField(SchemaType root, List<Parser.Field> nodes, Object value,
                             List<Object> path) {
            FieldDef def = root.fields.get(nodes.get(0).name);
            return complete(def.type, nodes, value, path);
        }

        Map<String, Object> arguments(FieldDef def, Parser.Field f) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (InputValueDef a : def.args.values()) {
                String where = "Argument \"" + a.name + "\"";
                boolean given = f.arguments.containsKey(a.name);
                Object literal = f.arguments.get(a.name);
                if (given && literal instanceof Parser.Variable
                        && !p.variables.containsKey(((Parser.Variable) literal).name)) {
                    given = false;
                }
                if (given) {
                    out.put(a.name, values.coerceLiteral(literal, a.type, p.variables, where));
                } else if (a.hasDefault) {
                    out.put(a.name, values.coerceLiteral(a.defaultValue, a.type, null, where));
                } else if (a.type.isNonNull()) {
                    throw new GraphQLException(where + " of required type \"" + a.type
                            + "\" was not provided.");
                }
            }
            return out;
        }

        private Object complete(TypeRef type, List<Parser.Field> nodes, Object value,
                                List<Object> path) {
            Parser.Field f = nodes.get(0);
            if (type.kind == TypeRef.NON_NULL) {
                Object v = complete(type.ofType, nodes, value, path);
                if (v == null) {
                    if (!(value == null && errorRecordedAt(path))) {
                        addError("Cannot return null for non-nullable field \""
                                + pathString(path) + "\".", f, path, classification("INTERNAL_ERROR"));
                    }
                    throw new NullPropagation();
                }
                return v;
            }
            if (value == null) {
                return null;
            }
            if (type.kind == TypeRef.LIST) {
                Iterator it = iterate(value);
                if (it == null) {
                    addError("Expected an iterable for list field \"" + pathString(path)
                            + "\", but got " + value.getClass().getName() + ".", f, path,
                            classification("INTERNAL_ERROR"));
                    return null;
                }
                List<Object> items = new ArrayList<Object>();
                while (it.hasNext()) {
                    items.add(it.next());
                }
                prefetchBatches(type.ofType, nodes, items, path);
                List<Object> out = new ArrayList<Object>();
                int i = 0;
                for (Object item : items) {
                    path.add(Integer.valueOf(i));
                    try {
                        out.add(complete(type.ofType, nodes, item, path));
                    } catch (NullPropagation np) {
                        if (type.ofType.isNonNull()) {
                            throw np;
                        }
                        out.add(null);
                    } finally {
                        path.remove(path.size() - 1);
                    }
                    i++;
                }
                return out;
            }
            SchemaType t = schema.type(type.name);
            if (t.isLeaf()) {
                try {
                    return values.serialize(t, value);
                } catch (GraphQLException e) {
                    addError(e.getMessage(), f, path, classification("INTERNAL_ERROR"));
                    return null;
                }
            }
            SchemaType object = t;
            if (t.isAbstract()) {
                object = resolveType(t, value);
                if (object == null) {
                    addError("Could not resolve the object type of a \"" + t.name
                            + "\" value for field \"" + pathString(path) + "\": return a map "
                            + "with __typename, a class named after the type, or register a "
                            + "TypeResolver.", f, path, classification("INTERNAL_ERROR"));
                    return null;
                }
            }
            List<Parser.Selection> merged = new ArrayList<Parser.Selection>();
            for (Parser.Field n : nodes) {
                if (n.selections != null) {
                    merged.addAll(n.selections);
                }
            }
            return executeSelectionSet(object, value, merged, path, false);
        }

        private boolean errorRecordedAt(List<Object> path) {
            if (errors.isEmpty()) {
                return false;
            }
            Object last = ((Map) errors.get(errors.size() - 1)).get("path");
            return path.equals(last);
        }

        private SchemaType resolveType(SchemaType abstractType, Object value) {
            String name = null;
            TypeResolver r = typeResolvers.get(abstractType.name);
            if (r != null) {
                name = r.resolveType(value);
            }
            if (name == null && value instanceof Map) {
                Object n = ((Map) value).get("__typename");
                name = n == null ? null : n.toString();
            }
            if (name == null) {
                String cls = value.getClass().getName();
                name = cls.substring(Math.max(cls.lastIndexOf('.'), cls.lastIndexOf('$')) + 1);
            }
            SchemaType t = schema.type(name);
            if (t == null || !schema.isPossibleType(abstractType, t)) {
                return null;
            }
            return t;
        }

        /// The default resolver: the field's entry in the parent map, or in
        /// the JSON tree the codecs write for the parent object.
        private Object property(Object source, String name) {
            if (source == null) {
                return null;
            }
            if (source instanceof Map) {
                return ((Map) source).get(name);
            }
            Object tree = trees.get(source);
            if (tree == null) {
                try {
                    tree = Json.parse(Json.write(source));
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e.toString(), e);
                }
                trees.put(source, tree);
            }
            return tree instanceof Map ? ((Map) tree).get(name) : null;
        }

        private Object introspectionField(Object source, String name, Map<String, Object> args) {
            Object v = property(source, name);
            if (args.containsKey("includeDeprecated")) {
                return Introspection.filterDeprecated(v, args.get("includeDeprecated"));
            }
            return v;
        }

        /// Values batch loaders produced ahead of time, by field and then by
        /// source object; a Throwable stands for the loader's failure.
        private final Map<String, Map<Object, Object>> prefetched =
                new HashMap<String, Map<Object, Object>>();

        /// Before the items of a list are completed: one call per batched
        /// field for every item at once, instead of one per item.
        private void prefetchBatches(TypeRef itemType, List<Parser.Field> nodes,
                                     List<Object> items, List<Object> path) {
            if (batchLoaders.isEmpty() || items.isEmpty()) {
                return;
            }
            SchemaType t = schema.type(itemType.namedType());
            if (t == null || !SchemaType.OBJECT.equals(t.kind)) {
                return;
            }
            List<Parser.Selection> merged = new ArrayList<Parser.Selection>();
            for (Parser.Field n : nodes) {
                if (n.selections != null) {
                    merged.addAll(n.selections);
                }
            }
            Map<String, List<Parser.Field>> fields = collectFields(t, merged);
            for (List<Parser.Field> fs : fields.values()) {
                Parser.Field f = fs.get(0);
                String key = t.name + "." + f.name;
                BatchLoader loader = batchLoaders.get(key);
                if (loader == null || fetchers.containsKey(key)) {
                    continue;
                }
                List<Object> sources = new ArrayList<Object>();
                for (Object o : items) {
                    if (o != null) {
                        sources.add(o);
                    }
                }
                load(key, loader, sources, f, t, path);
            }
        }

        private void load(String key, BatchLoader loader, List<Object> sources, Parser.Field f,
                          SchemaType t, List<Object> path) {
            Map<Object, Object> into = prefetched.get(key);
            if (into == null) {
                into = new IdentityHashMap<Object, Object>();
                prefetched.put(key, into);
            }
            Object result;
            try {
                result = loader.load(Collections.unmodifiableList(sources),
                        new DataFetchingEnvironment(null, new LinkedHashMap<String, Object>(),
                                context, f.name, t.name, new ArrayList<Object>(path),
                                p.variables));
                if (result instanceof Map) {
                    Map m = (Map) result;
                    for (Object s : sources) {
                        into.put(s, m.get(s));
                    }
                } else if (result instanceof List && ((List) result).size() == sources.size()) {
                    List l = (List) result;
                    for (int i = 0; i < sources.size(); i++) {
                        into.put(sources.get(i), l.get(i));
                    }
                } else {
                    throw new IllegalStateException("The batch loader for " + key + " must "
                            + "return a Map keyed by source, or a List with one value per source "
                            + "in order");
                }
            } catch (Throwable e) {
                for (Object s : sources) {
                    into.put(s, new Failure(e));
                }
            }
        }

        private Object batched(SchemaType type, Parser.Field f, BatchLoader loader,
                               Object source, List<Object> path)
                throws Throwable {
            String key = type.name + "." + f.name;
            Map<Object, Object> done = prefetched.get(key);
            if (done == null || !done.containsKey(source)) {
                // Not part of a list: a batch of one.
                List<Object> one = new ArrayList<Object>();
                one.add(source);
                load(key, loader, one, f, type, path);
                done = prefetched.get(key);
            }
            Object v = done.get(source);
            if (v instanceof Failure) {
                throw ((Failure) v).cause;
            }
            return v;
        }

        void fieldError(Throwable t, Parser.Field f, List<Object> path) {
            String message;
            Map ext;
            if (t instanceof GraphQLException) {
                message = t.getMessage();
                ext = ((GraphQLException) t).getExtensions();
                if (ext == null) {
                    ext = classification("DataFetchingException");
                }
            } else if (t instanceof AccessDeniedException) {
                message = "Forbidden";
                ext = classification("FORBIDDEN");
            } else if (t instanceof AuthenticationException) {
                message = "Unauthorized";
                ext = classification("UNAUTHORIZED");
            } else if (t instanceof ResponseStatusException) {
                ResponseStatusException r = (ResponseStatusException) t;
                int s = r.getStatus();
                String c = s == 404 ? "NOT_FOUND" : s == 400 ? "BAD_REQUEST"
                        : s == 401 ? "UNAUTHORIZED" : s == 403 ? "FORBIDDEN"
                        : "INTERNAL_ERROR";
                message = "INTERNAL_ERROR".equals(c) || r.getReason() == null
                        ? c : r.getReason();
                ext = classification(c);
            } else {
                message = "INTERNAL_ERROR for " + pathString(path);
                ext = classification("INTERNAL_ERROR");
                LOG.error("GraphQL resolver for {} failed", pathString(path), t);
            }
            addError(message, f, path, ext);
        }

        private void addError(String message, Parser.Field f, List<Object> path, Map ext) {
            errors.add(errorMap(message, f.line, f.column, path, ext));
        }
    }

    private Map<String, Object> introspection() {
        return introspectionValue;
    }

    private static Iterator iterate(Object value) {
        if (value instanceof Iterable) {
            return ((Iterable) value).iterator();
        }
        if (value instanceof Object[]) {
            final Object[] a = (Object[]) value;
            List<Object> l = new ArrayList<Object>(a.length);
            for (Object o : a) {
                l.add(o);
            }
            return l.iterator();
        }
        if (value instanceof int[]) {
            int[] a = (int[]) value;
            List<Object> l = new ArrayList<Object>(a.length);
            for (int o : a) {
                l.add(Integer.valueOf(o));
            }
            return l.iterator();
        }
        if (value instanceof long[]) {
            long[] a = (long[]) value;
            List<Object> l = new ArrayList<Object>(a.length);
            for (long o : a) {
                l.add(Long.valueOf(o));
            }
            return l.iterator();
        }
        if (value instanceof double[]) {
            double[] a = (double[]) value;
            List<Object> l = new ArrayList<Object>(a.length);
            for (double o : a) {
                l.add(Double.valueOf(o));
            }
            return l.iterator();
        }
        return null;
    }

    /// A batch loader's failure, kept for each source it was asked for.
    private static final class Failure {
        final Throwable cause;

        Failure(Throwable cause) {
            this.cause = cause;
        }
    }

    /// Resolves one field for many sources in one call -- what
    /// `@BatchMapping` binds, and Spring's answer to the N+1 problem: the
    /// field of every object in a list is loaded together, before any of them
    /// is completed.
    public interface BatchLoader {
        /// The values for `sources`: a `Map` keyed by source, or a `List`
        /// with one value per source in the same order.
        Object load(List<Object> sources, DataFetchingEnvironment environment)
                throws Exception;
    }

    private static String pathString(List<Object> path) {
        StringBuilder sb = new StringBuilder();
        for (Object o : path) {
            if (o instanceof Integer) {
                sb.append('[').append(o).append(']');
            } else {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(o);
            }
        }
        return sb.toString();
    }

    /// Builds a [GraphQL].
    public static final class Builder {
        private final GraphQLSchema schema;
        private final Map<String, DataFetcher> fetchers = new LinkedHashMap<String, DataFetcher>();
        private final Map<String, BatchLoader> batchLoaders = new HashMap<String, BatchLoader>();
        private final Map<String, TypeResolver> typeResolvers = new HashMap<String, TypeResolver>();
        private final Map<String, GraphQLScalar> scalars = new HashMap<String, GraphQLScalar>();
        private boolean introspection = true;
        private int maxDepth = 15;
        private int maxFields = 2000;
        private Object rootValue;

        private Builder(GraphQLSchema schema) {
            this.schema = schema;
        }

        /// Resolves `typeName.fieldName` with `fetcher`.
        ///
        /// #### Throws
        ///
        /// - `IllegalArgumentException`: when the schema has no such field, or
        /// the field already has a resolver -- the build reports the same as
        /// errors, as Spring for GraphQL's schema inspection does
        public Builder fetcher(String typeName, String fieldName, DataFetcher fetcher) {
            fetchers.put(claim(typeName, fieldName), fetcher);
            return this;
        }

        /// Resolves `typeName.fieldName` for many sources at once with `loader`.
        ///
        /// #### Throws
        ///
        /// - `IllegalArgumentException`: as [#fetcher]
        public Builder batchLoader(String typeName, String fieldName, BatchLoader loader) {
            batchLoaders.put(claim(typeName, fieldName), loader);
            return this;
        }

        private String claim(String typeName, String fieldName) {
            if (!schema.hasField(typeName, fieldName)) {
                throw new IllegalArgumentException("The schema has no field " + typeName + "."
                        + fieldName + " to resolve.");
            }
            String key = typeName + "." + fieldName;
            if (fetchers.containsKey(key) || batchLoaders.containsKey(key)) {
                throw new IllegalArgumentException("Two resolvers for " + key + ".");
            }
            return key;
        }

        /// Names the object type of values of the interface or union `typeName`.
        public Builder typeResolver(String typeName, TypeResolver resolver) {
            SchemaType t = schema.type(typeName);
            if (t == null || !t.isAbstract()) {
                throw new IllegalArgumentException(typeName + " is not an interface or union "
                        + "of the schema.");
            }
            typeResolvers.put(typeName, resolver);
            return this;
        }

        /// Coerces the custom scalar `typeName` with `scalar`.
        public Builder scalar(String typeName, GraphQLScalar scalar) {
            SchemaType t = schema.type(typeName);
            if (t == null || !SchemaType.SCALAR.equals(t.kind)) {
                throw new IllegalArgumentException(typeName + " is not a scalar of the schema.");
            }
            scalars.put(typeName, scalar);
            return this;
        }

        /// Whether `__schema` and `__type` answer; on by default, as in Spring
        /// for GraphQL (`spring.graphql.schema.introspection.enabled`).
        public Builder introspection(boolean enabled) {
            this.introspection = enabled;
            return this;
        }

        /// The deepest nesting of selection sets a document may have; 15 by
        /// default. A deeper document is refused before it runs, which bounds
        /// what one request can make the server do.
        /// The most fields one document may select, counting every alias and
        /// every place a fragment is spread; 2000 unless set. A document over
        /// it is refused before anything runs.
        public Builder maxFields(int fields) {
            if (fields < 1) {
                throw new IllegalArgumentException("maxFields must be at least 1");
            }
            this.maxFields = fields;
            return this;
        }

        public Builder maxDepth(int depth) {
            if (depth < 1) {
                throw new IllegalArgumentException("maxDepth must be at least 1");
            }
            this.maxDepth = depth;
            return this;
        }

        /// The source the root fields' resolvers are given; null by default.
        public Builder rootValue(Object value) {
            this.rootValue = value;
            return this;
        }

        /// The engine.
        public GraphQL build() {
            return new GraphQL(this);
        }
    }
}
