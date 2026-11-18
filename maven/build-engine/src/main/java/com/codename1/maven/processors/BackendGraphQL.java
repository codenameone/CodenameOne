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
package com.codename1.maven.processors;

import com.codename1.maven.GraphQLSchemaIndex;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.MethodInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

/// The GraphQL half of the backend build, in the shape of Spring for GraphQL:
/// `@QueryMapping`, `@MutationMapping`, `@SubscriptionMapping`,
/// `@SchemaMapping` and `@BatchMapping` methods of beans, checked against the
/// `.graphqls` files under `src/main/resources/graphql`, each bean's resolvers
/// registered by one generated adapter that calls the methods directly.
///
/// Spring reports a resolver for a field the schema lacks in a schema
/// inspection log at startup; here it is a build error, and so is an
/// `@Argument` naming no argument of the field.
final class BackendGraphQL {
    static final String DFE = "com/codename1/backend/graphql/DataFetchingEnvironment";
    static final String REQUEST = "com/codename1/backend/HttpServer$Request";
    static final String SESSION = "com/codename1/backend/WebSocketSession";
    static final String STREAM = "com/codename1/backend/graphql/GraphQLStream";
    static final String SCHEMA_DIR = "graphql";

    enum Kind { QUERY, MUTATION, SUBSCRIPTION, SCHEMA, BATCH }

    /// One parameter of a resolver, and how the adapter fills it.
    static final class Param {
        /// argument, arguments, env, request, session, source or sources.
        String kind;
        String name;
        Type type;
        /// The declared Java type, for a parameter read by a codec or the
        /// element type of a batch's list.
        String declared;
        /// The codec statements reading `j` into `target`, or null.
        String read;
        /// For a source: whether a map is converted to the class by its codec.
        boolean convert;
    }

    /// One resolver method.
    static final class Mapping {
        final MethodInfo method;
        final Kind kind;
        final AnnotatedClass cls;
        final String where;
        String typeName;
        String field;
        final List<Param> params = new ArrayList<Param>();

        Mapping(MethodInfo method, Kind kind, AnnotatedClass cls) {
            this.method = method;
            this.kind = kind;
            this.cls = cls;
            this.where = cls.getSourceName() + "." + method.getName();
        }

        String annotation() {
            switch (kind) {
                case QUERY: return "@QueryMapping";
                case MUTATION: return "@MutationMapping";
                case SUBSCRIPTION: return "@SubscriptionMapping";
                case BATCH: return "@BatchMapping";
                default: return "@SchemaMapping";
            }
        }
    }

    private BackendGraphQL() {
    }

    static boolean isMapping(MethodInfo m) {
        return m.getAnnotation(BackendBeans.QUERY_MAPPING) != null
                || m.getAnnotation(BackendBeans.MUTATION_MAPPING) != null
                || m.getAnnotation(BackendBeans.SUBSCRIPTION_MAPPING) != null
                || m.getAnnotation(BackendBeans.SCHEMA_MAPPING) != null
                || m.getAnnotation(BackendBeans.BATCH_MAPPING) != null;
    }

    private static String text(AnnotationValues a, String... keys) {
        for (String k : keys) {
            String v = a.getStringOrDefault(k, "").trim();
            if (v.length() > 0) {
                return v;
            }
        }
        return "";
    }

    // ---------------------------------------------------------------- collect

    static void collect(BackendBeans model, BackendBeans.Bean bean) {
        AnnotatedClass cls = bean.cls;
        if (cls == null) {
            return;
        }
        AnnotationValues onClass = cls.getClassAnnotation(BackendBeans.SCHEMA_MAPPING);
        String classType = onClass == null ? "" : text(onClass, "typeName");
        for (MethodInfo m : model.inheritedMembers(cls)) {
            Kind kind;
            AnnotationValues a;
            if ((a = m.getAnnotation(BackendBeans.QUERY_MAPPING)) != null) {
                kind = Kind.QUERY;
            } else if ((a = m.getAnnotation(BackendBeans.MUTATION_MAPPING)) != null) {
                kind = Kind.MUTATION;
            } else if ((a = m.getAnnotation(BackendBeans.SUBSCRIPTION_MAPPING)) != null) {
                kind = Kind.SUBSCRIPTION;
            } else if ((a = m.getAnnotation(BackendBeans.SCHEMA_MAPPING)) != null) {
                kind = Kind.SCHEMA;
            } else if ((a = m.getAnnotation(BackendBeans.BATCH_MAPPING)) != null) {
                kind = Kind.BATCH;
            } else {
                continue;
            }
            Mapping mp = new Mapping(m, kind, cls);
            if (collect(model, mp, a, classType)) {
                bean.graphql.add(mp);
            }
        }
    }

    private static boolean collect(BackendBeans model, Mapping mp, AnnotationValues a,
                                   String classType) {
        MethodInfo m = mp.method;
        AnnotatedClass cls = mp.cls;
        String head = mp.annotation() + " method " + mp.where;
        if (m.isStatic()) {
            model.ctx.error(cls, head + " must be an instance method.");
            return false;
        }
        if (model.wovenAsync(cls, m)) {
            model.ctx.error(cls, head + " is also @Async. A resolver answers with what it "
                    + "returns, so it must run to the end; drop @Async.");
            return false;
        }
        if (model.returnsFuture(m)) {
            model.ctx.error(cls, head + " returns a Future. Return the value itself: the "
                    + "engine would otherwise resolve the field to the pending task.");
            return false;
        }
        mp.field = text(a, "value", "name", "field");
        if (mp.field.length() == 0) {
            mp.field = m.getName();
        }
        if (mp.kind == Kind.SCHEMA || mp.kind == Kind.BATCH) {
            String t = text(a, "typeName");
            mp.typeName = t.length() > 0 ? t : classType.length() > 0 ? classType : null;
        }
        Type[] args = Type.getArgumentTypes(m.getDescriptor());
        String[] generic = RestClientAnnotationProcessor.parseGenericParameterSignatures(
                m.getSignature(), args.length);
        BackendJsonCodecs codecs = BackendJsonCodecs.of(model.ctx);
        boolean ok = true;
        boolean source = false;
        for (int i = 0; i < args.length; i++) {
            Map<String, AnnotationValues> pa = BackendBeans.parameterAnnotations(m, i);
            String internal = args[i].getSort() == Type.OBJECT ? args[i].getInternalName() : null;
            String sig = generic == null ? null : generic[i];
            String at = "Parameter " + (i + 1) + " of " + head;
            Param p = new Param();
            p.type = args[i];
            AnnotationValues arg = pa.get(BackendBeans.GQL_ARGUMENT);
            if (mp.kind == Kind.BATCH && (arg != null || pa.get(BackendBeans.GQL_ARGUMENTS) != null)) {
                // One call answers the field for many sources, which may each
                // have asked with different arguments; Spring refuses it too.
                model.ctx.error(cls, at + " binds a field argument, which a @BatchMapping "
                        + "cannot: use @SchemaMapping for a field that takes arguments.");
                ok = false;
                continue;
            }
            if (arg != null) {
                p.kind = "argument";
                p.name = text(arg, "value", "name");
                if (p.name.length() == 0) {
                    model.ctx.error(cls, at + " has an @Argument with no name. A Java "
                            + "parameter name does not survive compilation, so write the "
                            + "argument's name: @Argument(\"id\").");
                    ok = false;
                    continue;
                }
                if (!model.bindable(args[i])) {
                    p.declared = RestClientAnnotationProcessor.javaTypeFor(args[i], sig);
                    String why = codecs.checkRead(p.declared);
                    if (why != null) {
                        model.ctx.error(cls, at + " is " + p.declared + ", which a GraphQL "
                                + "argument cannot be read into: it holds " + why + ".");
                        ok = false;
                        continue;
                    }
                    p.read = codecs.readStatements(p.declared, "j",
                            "com.codename1.impl.backend.JsonCodec.Path.ROOT",
                            BackendJsonCodecs.javaString(p.name), "-1", "0", "target", "");
                }
            } else if (pa.get(BackendBeans.GQL_ARGUMENTS) != null) {
                p.kind = "arguments";
                if (!"java/util/Map".equals(internal)) {
                    p.declared = RestClientAnnotationProcessor.javaTypeFor(args[i], sig);
                    String why = codecs.checkRead(p.declared);
                    if (why != null) {
                        model.ctx.error(cls, at + " is " + p.declared + ", which a field's "
                                + "arguments cannot be read into: it holds " + why + ".");
                        ok = false;
                        continue;
                    }
                    p.read = codecs.readStatements(p.declared, "j",
                            "com.codename1.impl.backend.JsonCodec.Path.ROOT",
                            BackendJsonCodecs.javaString("arguments"), "-1", "0", "target", "");
                }
            } else if (DFE.equals(internal)) {
                p.kind = "env";
            } else if (REQUEST.equals(internal)) {
                p.kind = "request";
            } else if (SESSION.equals(internal)) {
                p.kind = "session";
            } else if (mp.kind == Kind.BATCH && i == 0 && ("java/util/List".equals(internal)
                    || "java/util/Collection".equals(internal))) {
                p.kind = "sources";
                String element = typeArgument(sig, 0);
                if (element == null) {
                    element = "Ljava/lang/Object;";
                }
                Type et = Type.getType(erase(element));
                if (et.getSort() != Type.OBJECT) {
                    model.ctx.error(cls, at + " must be a List of the source type.");
                    ok = false;
                    continue;
                }
                p.declared = RestClientAnnotationProcessor.javaTypeFor(et, element);
                p.convert = sourceConverts(model, codecs, et);
                if (mp.typeName == null) {
                    mp.typeName = simpleName(et.getInternalName());
                }
                source = true;
            } else if (mp.kind == Kind.SCHEMA && !source && internal != null) {
                p.kind = "source";
                p.declared = RestClientAnnotationProcessor.javaTypeFor(args[i], sig);
                p.convert = sourceConverts(model, codecs, args[i]);
                if (mp.typeName == null) {
                    // As Spring does: the source's class names the parent type.
                    mp.typeName = simpleName(internal);
                }
                source = true;
            } else {
                model.ctx.error(cls, at + " is neither an @Argument, @Arguments, a "
                        + "DataFetchingEnvironment, the request or session, nor -- on a "
                        + "@SchemaMapping of a non-root type -- the source object, so "
                        + "nothing can fill it.");
                ok = false;
                continue;
            }
            mp.params.add(p);
        }
        if (mp.kind == Kind.BATCH && !source) {
            model.ctx.error(cls, head + " must take the List of source objects as its first "
                    + "parameter.");
            ok = false;
        }
        if ((mp.kind == Kind.SCHEMA || mp.kind == Kind.BATCH) && mp.typeName == null && ok) {
            model.ctx.error(cls, head + " names no type: set typeName, on the method or on a "
                    + "@SchemaMapping of the class, or take the source object as a parameter.");
            ok = false;
        }
        Type returned = Type.getReturnType(m.getDescriptor());
        String rsig = m.getSignature() == null ? null
                : m.getSignature().substring(m.getSignature().lastIndexOf(')') + 1);
        if (returned.getSort() == Type.VOID) {
            model.ctx.error(cls, head + " returns nothing; a resolver's result is the "
                    + "field's value.");
            return false;
        }
        String rinternal = returned.getSort() == Type.OBJECT ? returned.getInternalName() : "";
        if (mp.kind == Kind.SUBSCRIPTION) {
            if (!STREAM.equals(rinternal)) {
                model.ctx.error(cls, head + " must return a GraphQLStream, whose events are "
                        + "the subscription's results.");
                return false;
            }
            return ok;
        }
        if (mp.kind == Kind.BATCH) {
            boolean map = "java/util/Map".equals(rinternal);
            if (!map && !"java/util/List".equals(rinternal)) {
                model.ctx.error(cls, head + " must return a Map from each source to its value, "
                        + "or a List with one value per source in order.");
                return false;
            }
            String value = typeArgument(rsig, map ? 1 : 0);
            if (value != null && ok) {
                ok = writable(model, codecs, mp, RestClientAnnotationProcessor.javaTypeFor(
                        Type.getType(erase(value)), value));
            }
            return ok;
        }
        if (ok && !"java/lang/Object".equals(rinternal)) {
            ok = writable(model, codecs, mp, RestClientAnnotationProcessor.javaTypeFor(returned,
                    rsig));
        }
        return ok;
    }

    /// The default resolver reads a result's fields through its JSON codec, so
    /// a class of the application's own needs one.
    private static boolean writable(BackendBeans model, BackendJsonCodecs codecs, Mapping mp,
                                    String declared) {
        String why = codecs.checkWrite(declared);
        if (why == null) {
            // The engine reads a result's fields with Json.write, which finds
            // a class's codec in the registry: every class named is a root.
            for (String token : declared.split("[<>,\\[\\] ]+")) {
                if (token.length() > 0) {
                    codecs.typedUse(token, false);
                }
            }
        }
        if (why != null) {
            model.ctx.error(mp.cls, mp.annotation() + " " + mp.where + " returns " + declared
                    + ", whose fields cannot be read: it holds " + why + ".");
            return false;
        }
        return true;
    }

    /// Whether a source that arrives as a map -- a parent resolved to one -- is
    /// read into the parameter's class by its codec.
    private static boolean sourceConverts(BackendBeans model, BackendJsonCodecs codecs, Type t) {
        String internal = t.getInternalName();
        if (internal.startsWith("java/") || model.bindable(t)) {
            return false;
        }
        AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(model.ctx, internal);
        if (c == null || c.isInterface()) {
            return false;
        }
        return codecs.typedUse(t.getClassName(), true) == null;
    }

    private static String simpleName(String internal) {
        String s = internal.substring(internal.lastIndexOf('/') + 1);
        return s.substring(s.lastIndexOf('$') + 1);
    }

    /// The `index`th type argument of a generic signature such as
    /// `Ljava/util/Map<LA;LB;>;`, or null.
    static String typeArgument(String sig, int index) {
        if (sig == null) {
            return null;
        }
        int open = sig.indexOf('<');
        if (open < 0) {
            return null;
        }
        int depth = 0;
        int start = open + 1;
        int n = 0;
        for (int i = open + 1; i < sig.length(); i++) {
            char c = sig.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                if (depth == 0) {
                    return null;
                }
                depth--;
            } else if (c == ';' && depth == 0) {
                if (n == index) {
                    String a = sig.substring(start, i + 1);
                    return a.startsWith("+") || a.startsWith("-") ? a.substring(1) : a;
                }
                n++;
                start = i + 1;
            } else if (c == '*' && depth == 0 && i == start) {
                if (n == index) {
                    return "Ljava/lang/Object;";
                }
                n++;
                start = i + 1;
            }
        }
        return null;
    }

    /// A generic signature with its type arguments removed: a descriptor.
    static String erase(String sig) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < sig.length(); i++) {
            char c = sig.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (depth == 0) {
                sb.append(c);
            }
        }
        String d = sb.toString();
        return d.startsWith("T") ? "Ljava/lang/Object;" : d;
    }

    // ------------------------------------------------------------------- emit

    /// Reads the schema, checks every resolver against it, and writes the
    /// adapters into `sources`.
    static void emit(BackendBeans model, Map<String, String> sources) {
        boolean any = false;
        BackendBeans.Bean first = null;
        for (BackendBeans.Bean b : model.beans) {
            if (!b.graphql.isEmpty()) {
                any = true;
                if (first == null) {
                    first = b;
                }
            }
        }
        String sdl;
        try {
            sdl = readSchema(model);
        } catch (IOException e) {
            model.ctx.error("Could not read the GraphQL schema: " + e.getMessage());
            return;
        }
        if (sdl == null) {
            if (any) {
                model.ctx.error(first.cls, first.cls.getSourceName() + " resolves GraphQL "
                        + "fields, but the module has no schema: put .graphqls files under "
                        + "src/main/resources/" + SCHEMA_DIR + ".");
            }
            return;
        }
        GraphQLSchemaIndex index;
        try {
            index = GraphQLSchemaIndex.parse(sdl, "src/main/resources/" + SCHEMA_DIR);
        } catch (IllegalArgumentException e) {
            model.ctx.error("The GraphQL schema does not parse: " + e.getMessage());
            return;
        }
        model.graphQLSchema = sdl;
        Map<String, String> claimed = new HashMap<String, String>();
        for (BackendBeans.Bean b : model.beans) {
            for (Mapping mp : b.graphql) {
                check(model, index, mp, claimed);
            }
        }
        if (model.ctx.hasErrors()) {
            return;
        }
        for (BackendBeans.Bean b : model.beans) {
            if (b.graphql.isEmpty()) {
                continue;
            }
            b.graphqlAdapter = adapterName(b);
            sources.put(b.graphqlAdapter, adapter(model, b));
        }
    }

    /// The adapter names and the schema, as the main build settled them, for a
    /// pass that writes the wiring without emitting the adapters again.
    static void name(BackendBeans model) {
        for (BackendBeans.Bean b : model.beans) {
            if (!b.graphql.isEmpty()) {
                b.graphqlAdapter = adapterName(b);
            }
        }
        try {
            model.graphQLSchema = readSchema(model);
        } catch (IOException e) {
            model.graphQLSchema = null;
        }
    }

    private static String adapterName(BackendBeans.Bean b) {
        String pkg = RestClientAnnotationProcessor.packageOf(b.type.replace('/', '.'));
        return BackendBeans.qualify(pkg, BackendBeans.baseName(b.type) + "Cn1GraphQL");
    }

    private static void check(BackendBeans model, GraphQLSchemaIndex index, Mapping mp,
                              Map<String, String> claimed) {
        String head = mp.annotation() + " " + mp.where;
        if (mp.typeName == null) {
            String op = mp.kind == Kind.MUTATION ? "mutation"
                    : mp.kind == Kind.SUBSCRIPTION ? "subscription" : "query";
            mp.typeName = index.rootType(op);
            if (!index.hasType(mp.typeName)) {
                model.ctx.error(mp.cls, head + " resolves a field of the " + op + " type, but "
                        + "the schema has no type " + mp.typeName + ".");
                return;
            }
        } else if (!index.hasType(mp.typeName)) {
            model.ctx.error(mp.cls, head + " resolves a field of " + mp.typeName + ", which "
                    + "is not an object type of the schema.");
            return;
        } else if (index.isInterface(mp.typeName)) {
            // Fields resolve on the concrete object type, so a resolver
            // registered on the interface would never be called.
            model.ctx.error(mp.cls, head + " resolves a field of " + mp.typeName + ", which "
                    + "is an interface: map it on each object type that implements it.");
            return;
        }
        Map<String, Boolean> args = index.arguments(mp.typeName, mp.field);
        if (args == null) {
            model.ctx.error(mp.cls, head + " resolves " + mp.typeName + "." + mp.field
                    + ", which the schema does not have. Its fields are "
                    + index.fields(mp.typeName) + ".");
            return;
        }
        String key = mp.typeName + "." + mp.field;
        String other = claimed.get(key);
        if (other != null) {
            model.ctx.error(mp.cls, head + " and " + other + " both resolve " + key + ".");
            return;
        }
        claimed.put(key, mp.where);
        boolean root = mp.typeName.equals(index.rootType("query"))
                || mp.typeName.equals(index.rootType("mutation"))
                || mp.typeName.equals(index.rootType("subscription"));
        for (Param p : mp.params) {
            if ("argument".equals(p.kind)) {
                Boolean nonNull = args.get(p.name);
                if (nonNull == null) {
                    model.ctx.error(mp.cls, head + " binds @Argument(\"" + p.name + "\"), but "
                            + key + " has no such argument; it has " + args.keySet() + ".");
                } else if (!nonNull.booleanValue() && isPrimitive(p.type)) {
                    model.ctx.error(mp.cls, head + " binds the nullable argument \"" + p.name
                            + "\" to a primitive, which cannot hold its absence; use "
                            + p.type.getClassName() + "'s wrapper type, or make the argument "
                            + "non-null in the schema.");
                }
            } else if ("source".equals(p.kind) && root) {
                model.ctx.error(mp.cls, head + " resolves a field of the root type "
                        + mp.typeName + ", which has no source object to pass.");
            }
        }
    }

    private static boolean isPrimitive(Type t) {
        return t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY;
    }

    /// Every `.graphqls` file of the module, in a stable order, joined; null
    /// when there are none.
    static String readSchema(BackendBeans model) throws IOException {
        File dir = null;
        File project = model.ctx.getProjectDir();
        if (project != null) {
            File d = new File(project, "src/main/resources/" + SCHEMA_DIR);
            if (d.isDirectory()) {
                dir = d;
            }
        }
        if (dir == null && model.ctx.getOutputClassDir() != null) {
            File d = new File(model.ctx.getOutputClassDir(), SCHEMA_DIR);
            if (d.isDirectory()) {
                dir = d;
            }
        }
        if (dir == null) {
            return null;
        }
        List<File> files = new ArrayList<File>();
        list(dir, files);
        if (files.isEmpty()) {
            return null;
        }
        Collections.sort(files);
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            sb.append(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8))
              .append('\n');
        }
        return sb.toString();
    }

    private static void list(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                list(f, out);
            } else if (f.getName().endsWith(".graphqls") || f.getName().endsWith(".gqls")) {
                out.add(f);
            }
        }
    }

    // ---------------------------------------------------------------- sources

    private static final String GQL = "com.codename1.backend.graphql.";

    /// The adapter registering one bean's resolvers on a `GraphQL.Builder`.
    static String adapter(BackendBeans model, BackendBeans.Bean b) {
        String binary = b.graphqlAdapter;
        String name = binary.substring(binary.lastIndexOf('.') + 1);
        String type = b.cls.getSourceName();
        StringBuilder sb = new StringBuilder();
        String pkg = RestClientAnnotationProcessor.packageOf(binary);
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated from the GraphQL resolvers of ").append(b.cls.getBinaryName())
          .append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(name).append(" {\n");
        sb.append("    private final ").append(type).append(" target;\n\n");
        sb.append("    public ").append(name).append('(').append(type)
          .append(" target) {\n        this.target = target;\n    }\n\n");
        sb.append("    public void register(").append(GQL).append("GraphQL.Builder b) {\n");
        BackendSources helpers = new BackendSources(model);
        for (Mapping mp : b.graphql) {
            String call = "target." + (BackendBeans.bridged(mp.method)
                    ? BackendWeaver.bridge(mp.method.getName()) : mp.method.getName());
            if (mp.kind == Kind.BATCH) {
                batch(sb, mp, call);
            } else {
                fetcher(sb, mp, call, helpers);
            }
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private static void fetcher(StringBuilder sb, Mapping mp, String call,
                                BackendSources helpers) {
        sb.append("        b.fetcher(").append(BackendSources.quote(mp.typeName)).append(", ")
          .append(BackendSources.quote(mp.field)).append(", new ").append(GQL)
          .append("DataFetcher() {\n");
        sb.append("            public Object get(").append(GQL)
          .append("DataFetchingEnvironment env) throws Exception {\n");
        StringBuilder args = new StringBuilder();
        String in = "                ";
        for (int i = 0; i < mp.params.size(); i++) {
            Param p = mp.params.get(i);
            if (i > 0) {
                args.append(", ");
            }
            if ("env".equals(p.kind)) {
                args.append("env");
            } else if ("request".equals(p.kind)) {
                args.append("env.getContext() instanceof com.codename1.backend.HttpServer.Request"
                        + " ? (com.codename1.backend.HttpServer.Request) env.getContext() : null");
            } else if ("session".equals(p.kind)) {
                args.append("env.getContext() instanceof com.codename1.backend.WebSocketSession"
                        + " ? (com.codename1.backend.WebSocketSession) env.getContext() : null");
            } else if ("source".equals(p.kind)) {
                sb.append(in).append("Object s").append(i).append(" = env.getSource();\n");
                args.append(sourceExpression(p, "s" + i));
            } else if ("arguments".equals(p.kind) && p.read == null) {
                args.append("new java.util.LinkedHashMap(env.getArguments())");
            } else if (p.read == null) {
                args.append(helpers.convert(p.type, "env.getArguments()", p.name, false));
            } else {
                String t = BackendJsonCodecs.source(p.declared);
                sb.append(in).append(t).append(" arg").append(i).append(" = null;\n");
                sb.append(in).append("{\n");
                sb.append(in).append("    Object j = ").append("arguments".equals(p.kind)
                        ? "env.getArgumentsAsJson()" : "env.getArgumentAsJson("
                        + BackendSources.quote(p.name) + ")").append(";\n");
                sb.append(in).append("    if (j != null) {\n");
                sb.append(in).append("        ").append(t).append(" target = null;\n");
                for (String line : p.read.split("\n")) {
                    sb.append(in).append("        ").append(line).append('\n');
                }
                sb.append(in).append("        arg").append(i).append(" = target;\n");
                sb.append(in).append("    }\n").append(in).append("}\n");
                args.append("arg").append(i);
            }
        }
        sb.append(in).append("return ").append(call).append('(').append(args).append(");\n");
        sb.append("            }\n        });\n");
    }

    private static void batch(StringBuilder sb, Mapping mp, String call) {
        Param p = mp.params.get(0);
        String t = BackendJsonCodecs.source(p.declared);
        String in = "                ";
        sb.append("        b.batchLoader(").append(BackendSources.quote(mp.typeName)).append(", ")
          .append(BackendSources.quote(mp.field)).append(", new ").append(GQL)
          .append("GraphQL.BatchLoader() {\n");
        sb.append("            public Object load(java.util.List<Object> sources, ").append(GQL)
          .append("DataFetchingEnvironment env) throws Exception {\n");
        sb.append(in).append("java.util.List<").append(t).append("> in = new java.util.ArrayList<")
          .append(t).append(">();\n");
        sb.append(in).append("for (int i = 0; i < sources.size(); i++) {\n");
        sb.append(in).append("    Object s = sources.get(i);\n");
        sb.append(in).append("    in.add(").append(sourceExpression(p, "s")).append(");\n");
        sb.append(in).append("}\n");
        StringBuilder args = new StringBuilder("in");
        for (int i = 1; i < mp.params.size(); i++) {
            Param q = mp.params.get(i);
            args.append(", ").append("env".equals(q.kind) ? "env"
                    : "request".equals(q.kind) ? "env.getContext() instanceof "
                    + "com.codename1.backend.HttpServer.Request ? "
                    + "(com.codename1.backend.HttpServer.Request) env.getContext() : null"
                    : "session".equals(q.kind) ? "env.getContext() instanceof "
                    + "com.codename1.backend.WebSocketSession ? "
                    + "(com.codename1.backend.WebSocketSession) env.getContext() : null"
                    : "null");
        }
        sb.append(in).append("Object r = ").append(call).append('(').append(args).append(");\n");
        // Keyed by what the engine passed, which a map converted to the
        // application's class is not.
        sb.append(in).append("if (r instanceof java.util.Map) {\n");
        sb.append(in).append("    java.util.Map m = (java.util.Map) r;\n");
        sb.append(in).append("    java.util.Map<Object, Object> out = "
                + "new java.util.IdentityHashMap<Object, Object>();\n");
        sb.append(in).append("    for (int i = 0; i < sources.size(); i++) {\n");
        sb.append(in).append("        out.put(sources.get(i), m.get(in.get(i)));\n");
        sb.append(in).append("    }\n");
        sb.append(in).append("    return out;\n");
        sb.append(in).append("}\n");
        sb.append(in).append("return r;\n");
        sb.append("            }\n        });\n");
    }

    /// The source `var`, as the parameter's class: itself when it is one, read
    /// by the codec when it is a map and the class has one, otherwise null.
    private static String sourceExpression(Param p, String var) {
        String raw = erasedSource(p.declared);
        if ("java.lang.Object".equals(raw)) {
            return var;
        }
        String typed = BackendJsonCodecs.source(p.declared);
        String cast = "(" + typed + ") " + var;
        if (p.convert) {
            return "(" + var + " instanceof " + raw + " ? " + cast + " : " + var + " == null ? null : "
                    + "com.codename1.backend.Json.convert(" + var + ", " + raw + ".class))";
        }
        return "(" + var + " instanceof " + raw + " ? " + cast + " : null)";
    }

    private static String erasedSource(String javaType) {
        int lt = javaType.indexOf('<');
        String s = lt < 0 ? javaType : javaType.substring(0, lt);
        return s.replace('$', '.');
    }
}
