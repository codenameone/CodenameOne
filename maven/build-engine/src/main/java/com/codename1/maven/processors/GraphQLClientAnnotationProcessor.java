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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.Type;

/// Build-time `@GraphQLClient` processor. Scans the project's compiled
/// classes for `@GraphQLClient`-annotated interfaces, validates them,
/// and emits:
///
/// 1. One `<SimpleName>Impl` per `@GraphQLClient` interface in the same
///    package. For each `@Query` / `@Mutation` method the impl builds a
///    variables map from the method's `@Var` parameters and chains
///    `com.codename1.io.graphql.GraphQL.execute(endpoint, bearer,
///    operationName, document, vars, ResponseData.class, callback)`. For
///    each `@Subscription` method it returns
///    `GraphQL.subscribe(...)`.
/// 2. A single `cn1app.GraphQLClientBootstrap` that registers every
///    accepted interface with `com.codename1.io.graphql.GraphQLClients`.
///
/// Mirrors [GrpcClientAnnotationProcessor] in structure.
public final class GraphQLClientAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String GRAPHQL_CLIENT_DESC = "Lcom/codename1/annotations/graphql/GraphQLClient;";
    public static final String QUERY_DESC = "Lcom/codename1/annotations/graphql/Query;";
    public static final String MUTATION_DESC = "Lcom/codename1/annotations/graphql/Mutation;";
    public static final String SUBSCRIPTION_DESC = "Lcom/codename1/annotations/graphql/Subscription;";
    public static final String VAR_DESC = "Lcom/codename1/annotations/graphql/Var;";
    public static final String HEADER_DESC = "Lcom/codename1/annotations/rest/Header;";

    static final String ONCOMPLETE_DESC = "Lcom/codename1/util/OnComplete;";
    static final String HANDLER_DESC = "Lcom/codename1/io/graphql/GraphQLSubscription$Handler;";

    static final String BOOTSTRAP_BINARY = "cn1app.GraphQLClientBootstrap";
    static final String BOOTSTRAP_SIMPLE = "GraphQLClientBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(GRAPHQL_CLIENT_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, GraphQLApi> accepted = new TreeMap<String, GraphQLApi>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) return;
        AnnotationValues clientAnno = cls.getClassAnnotation(GRAPHQL_CLIENT_DESC);
        if (clientAnno == null) return;
        if (!cls.isInterface()) {
            ctx.error(cls, "@GraphQLClient requires an interface; "
                    + cls.getBinaryName() + " is not an interface");
            return;
        }
        if (!cls.isPublic()) {
            ctx.error(cls, "@GraphQLClient interface " + cls.getBinaryName()
                    + " must be public");
            return;
        }

        GraphQLApi api = new GraphQLApi();
        api.binaryName = cls.getBinaryName();
        api.simpleName = simpleName(api.binaryName);
        api.packageName = packageOf(api.binaryName);
        api.implSimpleName = api.simpleName + "Impl";
        api.implBinaryName = api.packageName.length() == 0
                ? api.implSimpleName
                : api.packageName + "." + api.implSimpleName;

        boolean anyError = false;
        for (MethodInfo m : cls.getMethods()) {
            if (m.isStatic()) continue;
            if (m.isSynthetic()) continue;
            if (m.isConstructor()) continue;
            if ((m.getAccess() & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0) continue;
            if (!m.isAbstract()) continue;

            GraphQLMethod gm = parseMethod(cls, m, ctx);
            if (gm == null) {
                anyError = true;
                continue;
            }
            api.methods.add(gm);
        }

        if (!anyError) {
            accepted.put(api.binaryName, api);
        }
    }

    /// Parses and validates one interface method. Returns null (and
    /// reports the failure via `ctx.error`) when the method is not a
    /// valid GraphQL operation.
    private GraphQLMethod parseMethod(AnnotatedClass cls, MethodInfo m, ProcessorContext ctx) {
        String label = cls.getBinaryName() + "." + m.getName();

        AnnotationValues query = m.getAnnotation(QUERY_DESC);
        AnnotationValues mutation = m.getAnnotation(MUTATION_DESC);
        AnnotationValues subscription = m.getAnnotation(SUBSCRIPTION_DESC);
        int opCount = (query != null ? 1 : 0) + (mutation != null ? 1 : 0) + (subscription != null ? 1 : 0);
        if (opCount == 0) {
            ctx.error(cls, "@GraphQLClient method " + label
                    + " must carry one of @Query, @Mutation or @Subscription");
            return null;
        }
        if (opCount > 1) {
            ctx.error(cls, "@GraphQLClient method " + label
                    + " carries more than one of @Query / @Mutation / @Subscription");
            return null;
        }

        GraphQLMethod gm = new GraphQLMethod();
        gm.name = m.getName();
        gm.descriptor = m.getDescriptor();
        gm.signature = m.getSignature();
        if (query != null) {
            gm.kind = Kind.QUERY;
            gm.document = query.getString("value");
            gm.operationName = query.getStringOrDefault("operationName", "");
        } else if (mutation != null) {
            gm.kind = Kind.MUTATION;
            gm.document = mutation.getString("value");
            gm.operationName = mutation.getStringOrDefault("operationName", "");
        } else {
            gm.kind = Kind.SUBSCRIPTION;
            gm.document = subscription.getString("value");
            gm.operationName = subscription.getStringOrDefault("operationName", "");
        }
        if (gm.document == null || gm.document.length() == 0) {
            ctx.error(cls, "@GraphQLClient method " + label
                    + " has an empty operation document");
            return null;
        }

        Type[] paramTypes = Type.getArgumentTypes(gm.descriptor);
        List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
        String[] genericSigs = RestClientAnnotationProcessor.parseGenericParameterSignatures(
                gm.signature, paramTypes.length);
        gm.paramTypes = paramTypes;

        for (int i = 0; i < paramTypes.length; i++) {
            String descriptor = paramTypes[i].getDescriptor();
            Map<String, AnnotationValues> pa = (paramAnnotations != null && i < paramAnnotations.size())
                    ? paramAnnotations.get(i) : null;
            AnnotationValues varAnno = pa == null ? null : pa.get(VAR_DESC);
            AnnotationValues hdr = pa == null ? null : pa.get(HEADER_DESC);

            if (ONCOMPLETE_DESC.equals(descriptor)) {
                if (gm.callbackIndex >= 0 || gm.handlerIndex >= 0) {
                    ctx.error(cls, "@GraphQLClient method " + label
                            + " declares more than one callback");
                    return null;
                }
                gm.callbackIndex = i;
                String payloadSig = genericSigs == null ? null : genericSigs[i];
                gm.responseBinaryName = RestClientAnnotationProcessor.extractResponsePayload(payloadSig);
                continue;
            }
            if (HANDLER_DESC.equals(descriptor)) {
                if (gm.callbackIndex >= 0 || gm.handlerIndex >= 0) {
                    ctx.error(cls, "@GraphQLClient method " + label
                            + " declares more than one callback");
                    return null;
                }
                gm.handlerIndex = i;
                String payloadSig = genericSigs == null ? null : genericSigs[i];
                gm.responseBinaryName = extractHandlerPayload(payloadSig);
                continue;
            }
            if (hdr != null) {
                String hv = hdr.getStringOrDefault("value", "");
                if ("Authorization".equalsIgnoreCase(hv)) {
                    gm.bearerIndex = i;
                    continue;
                }
                ctx.error(cls, "@GraphQLClient method " + label
                        + " carries @Header(\"" + hv + "\") -- only @Header(\"Authorization\") "
                        + "is honoured for GraphQL clients in this release");
                return null;
            }
            if (varAnno != null) {
                GraphQLVar v = new GraphQLVar();
                v.paramIndex = i;
                v.name = varAnno.getStringOrDefault("value", "");
                v.primitive = paramTypes[i].getSort() != Type.OBJECT && paramTypes[i].getSort() != Type.ARRAY;
                if (v.name.length() == 0) {
                    ctx.error(cls, "@GraphQLClient method " + label
                            + " has an @Var with an empty variable name at position " + i);
                    return null;
                }
                gm.vars.add(v);
                continue;
            }
            ctx.error(cls, "@GraphQLClient method " + label
                    + " has an unrecognised parameter at position " + i
                    + " (descriptor " + descriptor + "); expected @Var-annotated variables, "
                    + "an optional @Header(\"Authorization\") String, and a trailing callback");
            return null;
        }

        if (gm.kind == Kind.SUBSCRIPTION) {
            if (gm.handlerIndex < 0) {
                ctx.error(cls, "@Subscription method " + label
                        + " must end with a GraphQLSubscription.Handler<T> parameter");
                return null;
            }
            Type ret = Type.getReturnType(gm.descriptor);
            if (!"com.codename1.io.graphql.GraphQLSubscription".equals(
                    ret.getClassName())) {
                ctx.error(cls, "@Subscription method " + label
                        + " must return com.codename1.io.graphql.GraphQLSubscription");
                return null;
            }
        } else {
            if (gm.callbackIndex < 0) {
                ctx.error(cls, "@" + (gm.kind == Kind.QUERY ? "Query" : "Mutation") + " method " + label
                        + " must end with an OnComplete<GraphQLResponse<T>> callback");
                return null;
            }
        }
        return gm;
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) return;
        if (accepted.isEmpty()) return;

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (GraphQLApi api : accepted.values()) {
            sources.put(api.implBinaryName, generateImplSource(api));
        }
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(accepted.values()));

        try {
            List<java.io.File> cp = new ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated @GraphQLClient sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @GraphQLClient impl(s) + " + BOOTSTRAP_BINARY);
    }

    // ----------------------------------------------------------------
    // Source generation
    // ----------------------------------------------------------------

    private static String generateImplSource(GraphQLApi api) {
        StringBuilder sb = new StringBuilder(2048);
        if (api.packageName.length() > 0) {
            sb.append("package ").append(api.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(api.implSimpleName)
                .append(" implements ").append(api.binaryName).append(" {\n\n");
        sb.append("    private final String endpoint;\n\n");
        sb.append("    public ").append(api.implSimpleName).append("(String endpoint) {\n");
        sb.append("        this.endpoint = endpoint;\n");
        sb.append("    }\n\n");
        for (GraphQLMethod gm : api.methods) {
            emitMethod(sb, gm);
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitMethod(StringBuilder sb, GraphQLMethod gm) {
        String returnType = gm.kind == Kind.SUBSCRIPTION
                ? "com.codename1.io.graphql.GraphQLSubscription" : "void";
        sb.append("    public ").append(returnType).append(' ').append(gm.name).append("(");
        for (int i = 0; i < gm.paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramJavaType(gm, i)).append(" p").append(i);
        }
        sb.append(") {\n");

        sb.append("        java.util.Map<String, Object> _vars = new java.util.LinkedHashMap<String, Object>();\n");
        for (GraphQLVar v : gm.vars) {
            String arg = "p" + v.paramIndex;
            if (v.primitive) {
                sb.append("        _vars.put(\"").append(escape(v.name)).append("\", ")
                        .append(arg).append(");\n");
            } else {
                sb.append("        if (").append(arg).append(" != null) _vars.put(\"")
                        .append(escape(v.name)).append("\", ").append(arg).append(");\n");
            }
        }

        String bearerExpr = gm.bearerIndex < 0 ? "null" : "p" + gm.bearerIndex;
        String opNameExpr = (gm.operationName == null || gm.operationName.length() == 0)
                ? "null" : "\"" + escape(gm.operationName) + "\"";
        String docExpr = "\"" + escape(gm.document) + "\"";
        String dataClass = gm.responseBinaryName + ".class";

        if (gm.kind == Kind.SUBSCRIPTION) {
            String handlerExpr = "p" + gm.handlerIndex;
            sb.append("        return com.codename1.io.graphql.GraphQL.subscribe(\n")
                    .append("            endpoint, ").append(bearerExpr).append(", ")
                    .append(opNameExpr).append(",\n")
                    .append("            ").append(docExpr).append(",\n")
                    .append("            _vars, ").append(dataClass).append(", ")
                    .append(handlerExpr).append(");\n");
        } else {
            String callbackExpr = "p" + gm.callbackIndex;
            sb.append("        com.codename1.io.graphql.GraphQL.execute(\n")
                    .append("            endpoint, ").append(bearerExpr).append(", ")
                    .append(opNameExpr).append(",\n")
                    .append("            ").append(docExpr).append(",\n")
                    .append("            _vars, ").append(dataClass).append(", ")
                    .append(callbackExpr).append(");\n");
        }
        sb.append("    }\n\n");
    }

    /// Returns the Java type literal for the impl-method parameter at
    /// position `i`. The callback / handler re-uses the response payload
    /// type so the impl method's signature stays parameterized the way
    /// the user declared it; everything else uses the erased descriptor
    /// type.
    private static String paramJavaType(GraphQLMethod gm, int i) {
        if (i == gm.callbackIndex) {
            return "com.codename1.util.OnComplete<com.codename1.io.graphql.GraphQLResponse<"
                    + gm.responseBinaryName + ">>";
        }
        if (i == gm.handlerIndex) {
            return "com.codename1.io.graphql.GraphQLSubscription.Handler<"
                    + gm.responseBinaryName + ">";
        }
        String descriptor = gm.paramTypes[i].getDescriptor();
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
        }
        if (descriptor.startsWith("[")) {
            return gm.paramTypes[i].getClassName(); // e.g. java.lang.String[]
        }
        return primitiveJava(descriptor);
    }

    private static String primitiveJava(String descriptor) {
        if ("I".equals(descriptor)) return "int";
        if ("J".equals(descriptor)) return "long";
        if ("F".equals(descriptor)) return "float";
        if ("D".equals(descriptor)) return "double";
        if ("Z".equals(descriptor)) return "boolean";
        if ("B".equals(descriptor)) return "byte";
        if ("S".equals(descriptor)) return "short";
        if ("C".equals(descriptor)) return "char";
        return "java.lang.Object";
    }

    private static String generateBootstrapSource(Iterable<GraphQLApi> apis) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// GraphQL-client bootstrap. The iOS / Android per-build application\n");
        sb.append("/// stub instantiates this class before Display.init (the build\n");
        sb.append("/// server probes the project zip for it and emits the install\n");
        sb.append("/// line conditionally); JavaSEPort picks it up via Class.forName\n");
        sb.append("/// for the simulator and desktop runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        for (GraphQLApi api : apis) {
            sb.append("        com.codename1.io.graphql.GraphQLClients.register(")
                    .append(api.binaryName)
                    .append(".class, new com.codename1.io.graphql.GraphQLClients.Factory<")
                    .append(api.binaryName).append(">() {\n");
            sb.append("            public ").append(api.binaryName).append(" create(String endpoint) {\n");
            sb.append("                return new ").append(api.implBinaryName).append("(endpoint);\n");
            sb.append("            }\n");
            sb.append("        });\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Signature helpers
    // ----------------------------------------------------------------

    /// Extracts the payload type `T` from a
    /// `GraphQLSubscription.Handler<T>` parameter's generic signature
    /// `Lcom/codename1/io/graphql/GraphQLSubscription$Handler<Lpkg/Data;>;`.
    /// Returns `java.lang.Object` when the signature is missing.
    static String extractHandlerPayload(String paramSignature) {
        if (paramSignature == null) return "java.lang.Object";
        int lt = paramSignature.indexOf('<');
        int gt = paramSignature.lastIndexOf('>');
        if (lt < 0 || gt < 0 || lt > gt) return "java.lang.Object";
        String inner = paramSignature.substring(lt + 1, gt);
        if (inner.startsWith("L") && inner.endsWith(";")) {
            // Strip any nested generics on the payload itself.
            int payloadGt = inner.indexOf('<');
            if (payloadGt >= 0) {
                return inner.substring(1, payloadGt).replace('/', '.');
            }
            return inner.substring(1, inner.length() - 1).replace('/', '.');
        }
        return "java.lang.Object";
    }

    // ----------------------------------------------------------------
    // Misc
    // ----------------------------------------------------------------

    private static String packageOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? "" : binary.substring(0, dot);
    }

    private static String simpleName(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    // ----------------------------------------------------------------
    // Accumulators
    // ----------------------------------------------------------------

    enum Kind { QUERY, MUTATION, SUBSCRIPTION }

    static final class GraphQLApi {
        String binaryName;
        String packageName;
        String simpleName;
        String implBinaryName;
        String implSimpleName;
        final List<GraphQLMethod> methods = new ArrayList<GraphQLMethod>();
    }

    static final class GraphQLMethod {
        String name;
        String descriptor;
        String signature;
        Kind kind;
        String document;
        String operationName;
        int bearerIndex = -1;
        int callbackIndex = -1;
        int handlerIndex = -1;
        String responseBinaryName = "java.lang.Object";
        final List<GraphQLVar> vars = new ArrayList<GraphQLVar>();
        Type[] paramTypes;
    }

    static final class GraphQLVar {
        int paramIndex;
        String name;
        boolean primitive;
    }
}
