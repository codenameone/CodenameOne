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

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
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

/// Server-side half of the `@RestClient` contract.
///
/// The SAME annotated interface that
/// [RestClientAnnotationProcessor] turns into a typed client is turned here into
/// the two things a server needs. One declaration, both ends, so a change to the
/// contract is a compile error on whichever side did not follow it -- which is the
/// entire point of sharing the interface rather than hand-writing a client against
/// a REST endpoint.
///
/// For `@RestClient interface GreeterApi` it emits, in the interface's package:
///
/// 1. `GreeterApiServer` -- a SYNCHRONOUS interface the backend implements. The
///    client's methods are asynchronous (they take an
///    `OnComplete<Response<T>>` and return void); a server handler has nothing to
///    do with a callback, so its method returns `T` directly and the callback
///    parameter is dropped. This is gRPC's shape: one definition, an async client
///    stub and a sync server base, rather than forcing one signature to serve both.
///    It is also what keeps `Response` off the server's classpath entirely -- its
///    constructor is package-private, so server code could not build one anyway.
/// 2. `GreeterApiDispatcher` -- routes `(method, path, body)` to the right handler
///    method, binding `@Path` segments and `@Query` parameters out of the request.
///
/// Unsupported bindings are rejected with an error rather than silently bound to
/// null: a parameter that quietly arrives empty at runtime is far more expensive
/// than a build that refuses to produce one.
public final class RestServerAnnotationProcessor extends AbstractAnnotationProcessor {

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(RestClientAnnotationProcessor.REST_CLIENT_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    /// Set -Dcn1.restServer=true (or the cn1.restServer property) to emit the
    /// server half. Off by default: every existing project carries @RestClient
    /// interfaces for its client, and generating server classes into those builds
    /// would grow every app for nothing.
    static boolean isEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("cn1.restServer", "false"));
    }

    private final TreeMap<String, Api> accepted = new TreeMap<String, Api>();

    /// DTO types reachable from an accepted contract, keyed by binary name. A
    /// TreeMap so codec emission order is stable across builds.
    private final TreeMap<String, AnnotatedClass> dtos = new TreeMap<String, AnnotatedClass>();

    static final class Api {
        String binaryName, simpleName, packageName, serverSimpleName, dispatcherSimpleName;
        final List<Op> ops = new ArrayList<Op>();
    }

    static final class Op {
        String name, verb, pathTemplate, returnType;
        final List<Param> params = new ArrayList<Param>();
    }

    static final class Param {
        String javaType, name, bindKind, bindName;
    }

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
        dtos.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (!isEnabled()) return;
        if (cls.isSynthetic()) return;
        if (cls.getClassAnnotation(RestClientAnnotationProcessor.REST_CLIENT_DESC) == null) return;
        // Shape errors (not an interface, not public) are already reported by the
        // client processor over the same class; repeating them would double every
        // message in the build log.
        if (!cls.isInterface() || !cls.isPublic()) return;

        Api api = new Api();
        api.binaryName = cls.getBinaryName();
        api.simpleName = RestClientAnnotationProcessor.simpleName(api.binaryName);
        api.packageName = RestClientAnnotationProcessor.packageOf(api.binaryName);
        api.serverSimpleName = api.simpleName + "Server";
        api.dispatcherSimpleName = api.simpleName + "Dispatcher";

        boolean anyError = false;
        for (MethodInfo m : cls.getMethods()) {
            if (m.isStatic() || m.isSynthetic() || m.isConstructor() || !m.isAbstract()) continue;
            if ((m.getAccess() & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0) continue;

            Op op = new Op();
            op.name = m.getName();

            AnnotationValues va;
            int verbCount = 0;
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.GET_DESC)) != null)    { op.verb = "GET";    op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.POST_DESC)) != null)   { op.verb = "POST";   op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.PUT_DESC)) != null)    { op.verb = "PUT";    op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.DELETE_DESC)) != null) { op.verb = "DELETE"; op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.PATCH_DESC)) != null)  { op.verb = "PATCH";  op.pathTemplate = va.getString("value"); verbCount++; }
            if (verbCount != 1) continue; // the client processor reports this
            if (op.pathTemplate == null) op.pathTemplate = "";

            Type[] paramTypes = Type.getArgumentTypes(m.getDescriptor());
            List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
            String[] genericSigs = RestClientAnnotationProcessor
                    .parseGenericParameterSignatures(m.getSignature(), paramTypes.length);

            op.returnType = "void";
            int bodyCount = 0;
            for (int i = 0; i < paramTypes.length; i++) {
                String descriptor = paramTypes[i].getDescriptor();
                String genericSig = genericSigs == null ? null : genericSigs[i];

                if (RestClientAnnotationProcessor.isCallbackType(descriptor)) {
                    // The callback is the client's result channel. On the server it
                    // becomes the return type and disappears from the signature.
                    String payload = RestClientAnnotationProcessor.extractResponsePayload(genericSig);
                    op.returnType = (payload == null || payload.length() == 0)
                            ? "java.lang.Object" : payload;
                    collectDtos(op.returnType, ctx);
                    continue;
                }

                Map<String, AnnotationValues> pa = i < paramAnnotations.size() ? paramAnnotations.get(i) : null;
                Param p = new Param();
                p.javaType = RestClientAnnotationProcessor.javaTypeFor(paramTypes[i], genericSig);
                AnnotationValues bind;
                if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.PATH_DESC)) != null) {
                    p.bindKind = "path";
                    p.bindName = bind.getString("value");
                } else if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.QUERY_DESC)) != null) {
                    p.bindKind = "query";
                    p.bindName = bind.getString("value");
                } else if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.HEADER_DESC)) != null) {
                    p.bindKind = "header";
                    p.bindName = bind.getString("value");
                } else if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.COOKIE_DESC)) != null) {
                    p.bindKind = "cookie";
                    p.bindName = bind.getString("value");
                } else if (pa != null && pa.get(RestClientAnnotationProcessor.BODY_DESC) != null) {
                    p.bindKind = "body";
                    p.bindName = "body";
                    bodyCount++;
                } else {
                    ctx.error(cls, "Parameter " + i + " of " + api.binaryName + "." + op.name
                            + " carries no REST binding annotation, so the dispatcher cannot "
                            + "supply a value for it");
                    anyError = true;
                    continue;
                }
                if (p.bindName == null || p.bindName.length() == 0) p.bindName = "p" + i;
                p.name = RestClientAnnotationProcessor.sanitizeIdentifier(
                        "body".equals(p.bindKind) ? "body" : p.bindName);
                if ("body".equals(p.bindKind)) {
                    collectDtos(p.javaType, ctx);
                } else if (!isBindableScalar(p.javaType)) {
                    // Path/query/header/cookie values arrive as text. Anything that
                    // is not convertible from a String has no defined binding, and
                    // guessing one would silently hand the handler a null.
                    ctx.error(cls, "Parameter " + i + " of " + api.binaryName + "." + op.name
                            + " is bound from the request as text but has type " + p.javaType
                            + ", which cannot be parsed from a string; use @Body for structured input");
                    anyError = true;
                    continue;
                }
                op.params.add(p);
            }
            if (bodyCount > 1) {
                ctx.error(cls, api.binaryName + "." + op.name
                        + " declares more than one @Body parameter; a request has one body");
                anyError = true;
            }
            api.ops.add(op);
        }
        if (!anyError && !api.ops.isEmpty()) {
            accepted.put(api.binaryName, api);
        }
    }

    /// Records any application class reachable as a body or a result so a codec is
    /// emitted for it. `java.util.List<Foo>` contributes Foo, not List.
    private void collectDtos(String javaType, ProcessorContext ctx) {
        if (javaType == null) return;
        String t = javaType.trim();
        int lt = t.indexOf('<');
        if (lt >= 0) {
            String outer = t.substring(0, lt);
            String inner = t.substring(lt + 1, t.length() - 1);
            if ("java.util.List".equals(outer) || "java.util.Set".equals(outer)) {
                collectDtos(inner, ctx);
            }
            return;
        }
        if (t.startsWith("java.") || t.indexOf('.') < 0) return;   // JDK type or a primitive
        AnnotatedClass cls = ctx.lookup(t.replace('.', '/'));
        if (cls == null || cls.isInterface() || cls.isEnum()) return;
        if (dtos.containsKey(t)) return;
        dtos.put(t, cls);
        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic() || !f.isPublic()) continue;
            collectDtos(fieldJavaType(f), ctx);
        }
    }

    private static String fieldJavaType(FieldInfo f) {
        String sig = f.getSignature();
        if (sig != null && sig.length() > 0) {
            return RestClientAnnotationProcessor.jvmSignatureToJavaType(sig);
        }
        return RestClientAnnotationProcessor.jvmSignatureToJavaType(f.getDescriptor());
    }

    private static boolean isBindableScalar(String javaType) {
        return "java.lang.String".equals(javaType) || "int".equals(javaType) || "long".equals(javaType)
                || "boolean".equals(javaType) || "double".equals(javaType) || "float".equals(javaType)
                || "short".equals(javaType) || "byte".equals(javaType)
                || "java.lang.Integer".equals(javaType) || "java.lang.Long".equals(javaType)
                || "java.lang.Boolean".equals(javaType) || "java.lang.Double".equals(javaType)
                || "java.lang.Float".equals(javaType) || "java.lang.Short".equals(javaType)
                || "java.lang.Byte".equals(javaType);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (!isEnabled() || ctx.hasErrors() || accepted.isEmpty()) return;
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (Api api : accepted.values()) {
            sources.put(qualify(api.packageName, api.serverSimpleName), generateServerInterface(api));
            sources.put(qualify(api.packageName, api.dispatcherSimpleName), generateDispatcher(api));
        }
        for (Map.Entry<String, AnnotatedClass> e : dtos.entrySet()) {
            String pkg = RestClientAnnotationProcessor.packageOf(e.getKey());
            String simple = RestClientAnnotationProcessor.simpleName(e.getKey()) + "Json";
            sources.put(qualify(pkg, simple), generateDtoCodec(e.getKey(), e.getValue()));
        }
        try {
            List<java.io.File> cp = new ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated @RestClient server sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size() + " @RestClient server dispatcher(s) and "
                + dtos.size() + " DTO codec(s)");
    }

    private static String qualify(String pkg, String simple) {
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    private static String codecFor(String dtoBinaryName) {
        return qualify(RestClientAnnotationProcessor.packageOf(dtoBinaryName),
                RestClientAnnotationProcessor.simpleName(dtoBinaryName) + "Json");
    }

    private static String generateServerInterface(Api api) {
        StringBuilder sb = new StringBuilder(1024);
        if (api.packageName.length() > 0) sb.append("package ").append(api.packageName).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations from ").append(api.binaryName).append(". Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public interface ").append(api.serverSimpleName).append(" {\n");
        for (Op op : api.ops) {
            sb.append("    ").append(op.returnType).append(' ').append(op.name).append('(');
            for (int i = 0; i < op.params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(op.params.get(i).javaType).append(' ').append(op.params.get(i).name);
            }
            sb.append(") throws Exception;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Dispatcher
    // ----------------------------------------------------------------

    private static String generateDispatcher(Api api) {
        StringBuilder sb = new StringBuilder(8192);
        if (api.packageName.length() > 0) sb.append("package ").append(api.packageName).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations from ").append(api.binaryName).append(". Do not edit.\n");
        sb.append("//\n");
        sb.append("// References nothing outside java.*, on purpose: generated server code has to\n");
        sb.append("// link into a binary that has no Codename One implementation. JSON text is\n");
        sb.append("// parsed and written by the caller, so `body` arrives as an already-decoded\n");
        sb.append("// Map/List/String and the result goes back the same way.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(api.dispatcherSimpleName).append(" {\n");
        sb.append("    private final ").append(api.serverSimpleName).append(" impl;\n\n");
        sb.append("    public ").append(api.dispatcherSimpleName).append("(")
          .append(api.serverSimpleName).append(" impl) {\n        this.impl = impl;\n    }\n\n");

        sb.append("    /** True when this API has a route for the verb and path. */\n");
        sb.append("    public boolean hasRoute(String method, String rawPath) {\n");
        sb.append("        String path = stripQuery(rawPath);\n");
        sb.append("        String[] seg = split(path);\n");
        for (Op op : api.ops) {
            sb.append("        if(").append(routeCondition(op)).append(") return true;\n");
        }
        sb.append("        return false;\n");
        sb.append("    }\n\n");

        sb.append("    /**\n");
        sb.append("     * Invokes the handler for this request.\n");
        sb.append("     *\n");
        sb.append("     * headers may be null. body is the decoded JSON value (Map/List/String) or\n");
        sb.append("     * null. Returns a JSON-ready value; check hasRoute first, because a handler\n");
        sb.append("     * returning null and no route at all both come back as null.\n");
        sb.append("     */\n");
        sb.append("    public Object dispatch(String method, String rawPath, java.util.Map headers, Object body) throws Exception {\n");
        sb.append("        String path = stripQuery(rawPath);\n");
        sb.append("        String query = queryOf(rawPath);\n");
        sb.append("        String[] seg = split(path);\n");
        for (Op op : api.ops) {
            emitRoute(sb, op);
        }
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        emitHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    private static String routeCondition(Op op) {
        String[] template = splitTemplate(op.pathTemplate);
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(op.verb).append("\".equals(method) && seg.length == ").append(template.length);
        for (int i = 0; i < template.length; i++) {
            if (!isPlaceholder(template[i])) {
                sb.append(" && \"").append(RestClientAnnotationProcessor.escape(template[i]))
                  .append("\".equals(seg[").append(i).append("])");
            }
        }
        return sb.toString();
    }

    private static void emitRoute(StringBuilder sb, Op op) {
        String[] template = splitTemplate(op.pathTemplate);
        sb.append("        if(").append(routeCondition(op)).append(") {\n");
        // Locals are positional (_a0, _a1, ...) rather than the parameter's own name:
        // a @Body parameter called "body" would otherwise shadow dispatch()'s own
        // body argument and fail to compile. Generated identifiers must not be able
        // to collide with the generator's.
        for (int pi = 0; pi < op.params.size(); pi++) {
            Param p = op.params.get(pi);
            sb.append("            ").append(p.javaType).append(" _a").append(pi).append(" = ");
            if ("path".equals(p.bindKind)) {
                int idx = placeholderIndex(template, p.bindName);
                sb.append(idx < 0 ? fromText(p.javaType, "null")
                        : fromText(p.javaType, "decode(seg[" + idx + "])"));
            } else if ("query".equals(p.bindKind)) {
                sb.append(fromText(p.javaType, "queryParam(query, \""
                        + RestClientAnnotationProcessor.escape(p.bindName) + "\")"));
            } else if ("header".equals(p.bindKind)) {
                sb.append(fromText(p.javaType, "header(headers, \""
                        + RestClientAnnotationProcessor.escape(p.bindName) + "\")"));
            } else if ("cookie".equals(p.bindKind)) {
                sb.append(fromText(p.javaType, "cookie(headers, \""
                        + RestClientAnnotationProcessor.escape(p.bindName) + "\")"));
            } else {
                sb.append(fromBody(p.javaType));
            }
            sb.append(";\n");
        }
        sb.append("            ");
        if (!"void".equals(op.returnType)) {
            sb.append(op.returnType).append(" _result = ");
        }
        sb.append("impl.").append(op.name).append('(');
        for (int i = 0; i < op.params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("_a").append(i);
        }
        sb.append(");\n");
        if ("void".equals(op.returnType)) {
            sb.append("            return \"\";\n");
        } else {
            sb.append("            return ").append(toJsonValue(op.returnType, "_result")).append(";\n");
        }
        sb.append("        }\n");
    }

    /// Wraps a String-valued expression in the conversion its target type needs.
    /// A null stays null for the boxed types rather than throwing, so an absent
    /// optional query parameter is not a 500.
    private static String fromText(String javaType, String expr) {
        if ("java.lang.String".equals(javaType)) return expr;
        if ("int".equals(javaType))     return "parseInt(" + expr + ")";
        if ("long".equals(javaType))    return "parseLong(" + expr + ")";
        if ("boolean".equals(javaType)) return "java.lang.Boolean.parseBoolean(" + expr + ")";
        if ("double".equals(javaType))  return "parseDouble(" + expr + ")";
        if ("float".equals(javaType))   return "(float)parseDouble(" + expr + ")";
        if ("short".equals(javaType))   return "(short)parseInt(" + expr + ")";
        if ("byte".equals(javaType))    return "(byte)parseInt(" + expr + ")";
        if ("java.lang.Integer".equals(javaType)) return "boxInt(" + expr + ")";
        if ("java.lang.Long".equals(javaType))    return "boxLong(" + expr + ")";
        if ("java.lang.Double".equals(javaType))  return "boxDouble(" + expr + ")";
        if ("java.lang.Float".equals(javaType))   return "boxFloat(" + expr + ")";
        if ("java.lang.Short".equals(javaType))   return "boxShort(" + expr + ")";
        if ("java.lang.Byte".equals(javaType))    return "boxByte(" + expr + ")";
        if ("java.lang.Boolean".equals(javaType)) return "boxBoolean(" + expr + ")";
        return expr;
    }

    /// The request body, converted to the handler's parameter type.
    ///
    /// The body is whatever the client sent, so its SHAPE is attacker controlled:
    /// a route declaring a DTO can be handed a string, a number or an array. None
    /// of these conversions may therefore rest on a cast. ParparVM's CHECKCAST is
    /// unchecked by default (see CLAUDE.md), so `(Map)body` over a String does not
    /// throw -- it reads a String's header as a Map's and the process dies, which
    /// on a server takes every in-flight connection with it. Every path below
    /// either tests with instanceof or converts through text.
    private static String fromBody(String javaType) {
        if ("java.lang.String".equals(javaType)) return "bodyAsString(body)";
        if (javaType.startsWith("java.util.List<") || javaType.startsWith("java.util.Set<")) {
            String element = javaType.substring(javaType.indexOf('<') + 1, javaType.length() - 1);
            // A Set parameter has to receive a Set. bodyAsList hands back an
            // ArrayList, and casting that to Set is exactly the cast the comment
            // above warns about: the JVM throws ClassCastException before the
            // handler runs, and the translated target does not check at all, so it
            // carries an ArrayList in a Set-typed field until something reads it as
            // one. setFromList converts instead of asserting.
            boolean isSet = javaType.startsWith("java.util.Set<");
            String decoded;
            if (element.startsWith("java.")) {
                decoded = "bodyAsList(body)";
            } else {
                decoded = "listFromMaps(bodyAsList(body), new FromMap() {\n"
                        + "                public Object convert(java.util.Map m) { return "
                        + codecFor(element) + ".fromMap(m); }\n"
                        + "            })";
            }
            if (isSet) {
                decoded = "setFromList(" + decoded + ")";
            }
            return "(" + javaType + ")(Object)" + decoded;
        }
        // A primitive or boxed scalar goes through the same text conversion the
        // query and path parameters use, so a JSON number reaching an `int` body
        // behaves the same as one reaching an `int` query parameter.
        if (javaType.indexOf('.') < 0 || isBoxedScalar(javaType)) {
            return fromText(javaType, "bodyAsString(body)");
        }
        if (javaType.startsWith("java.")) {
            return guardedCast(javaType, "body");
        }
        return codecFor(javaType) + ".fromMap(bodyAsMap(body))";
    }

    private static boolean isBoxedScalar(String javaType) {
        return "java.lang.Integer".equals(javaType) || "java.lang.Long".equals(javaType)
                || "java.lang.Double".equals(javaType) || "java.lang.Float".equals(javaType)
                || "java.lang.Short".equals(javaType) || "java.lang.Byte".equals(javaType)
                || "java.lang.Boolean".equals(javaType);
    }

    /// `expr` narrowed to `javaType` when it already is one, and null when it is
    /// not -- an instanceof rather than a cast, for the reason in {@link #fromBody}.
    /// `expr` is evaluated twice, so it must stay side-effect free (it is always a
    /// local or a Map read).
    private static String guardedCast(String javaType, String expr) {
        String raw = javaType;
        int generic = raw.indexOf('<');
        if (generic > 0) raw = raw.substring(0, generic);
        return "(" + javaType + ")(Object)(" + expr + " instanceof " + raw
                + " ? " + expr + " : null)";
    }

    /// The handler's return value, converted to something the JSON writer accepts.
    private static String toJsonValue(String javaType, String expr) {
        if (javaType.startsWith("java.util.List<") || javaType.startsWith("java.util.Set<")) {
            String element = javaType.substring(javaType.indexOf('<') + 1, javaType.length() - 1);
            if (element.startsWith("java.")) {
                return expr;
            }
            return "listToMaps(" + expr + ", new ToMap() {\n"
                    + "                public java.util.Map convert(Object o) { return "
                    + codecFor(element) + ".toMap((" + element + ")o); }\n"
                    + "            })";
        }
        if (javaType.startsWith("java.") || javaType.indexOf('.') < 0) {
            return expr;
        }
        return codecFor(javaType) + ".toMap(" + expr + ")";
    }

    private static void emitHelpers(StringBuilder sb) {
        sb.append("    /** Converts one element of a decoded JSON array into a DTO. */\n");
        sb.append("    private interface FromMap { Object convert(java.util.Map m); }\n");
        sb.append("    private interface ToMap { java.util.Map convert(Object o); }\n\n");
        sb.append("    private static java.util.List listFromMaps(java.util.List raw, FromMap f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        for(int i = 0 ; i < raw.size() ; i++) {\n");
        sb.append("            Object e = raw.get(i);\n");
        sb.append("            out.add(e instanceof java.util.Map ? f.convert((java.util.Map)e) : null);\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    private static java.util.List listToMaps(java.util.Collection raw, ToMap f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        java.util.Iterator it = raw.iterator();\n");
        sb.append("        while(it.hasNext()) {\n");
        sb.append("            Object e = it.next();\n");
        sb.append("            out.add(e == null ? null : f.convert(e));\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    /** The body as a JSON object, or a 400 -- never a cast. */\n");
        sb.append("    private static java.util.Map bodyAsMap(Object body) {\n");
        sb.append("        if(body == null || body instanceof java.util.Map) return (java.util.Map)body;\n");
        sb.append("        throw new IllegalArgumentException(\"a JSON object is required in the request body\");\n");
        sb.append("    }\n\n");
        sb.append("    /** The body as a JSON array, or a 400 -- never a cast. */\n");
        sb.append("    private static java.util.List bodyAsList(Object body) {\n");
        sb.append("        if(body == null || body instanceof java.util.List) return (java.util.List)body;\n");
        sb.append("        throw new IllegalArgumentException(\"a JSON array is required in the request body\");\n");
        sb.append("    }\n\n");
        sb.append("    private static String bodyAsString(Object body) {\n");
        sb.append("        return body == null ? null : (body instanceof String ? (String)body : String.valueOf(body));\n");
        sb.append("    }\n\n");
        sb.append("    private static String stripQuery(String rawPath) {\n");
        sb.append("        if(rawPath == null) return \"\";\n");
        sb.append("        int q = rawPath.indexOf('?');\n");
        sb.append("        return q < 0 ? rawPath : rawPath.substring(0, q);\n");
        sb.append("    }\n\n");
        sb.append("    private static String queryOf(String rawPath) {\n");
        sb.append("        if(rawPath == null) return \"\";\n");
        sb.append("        int q = rawPath.indexOf('?');\n");
        sb.append("        return q < 0 ? \"\" : rawPath.substring(q + 1);\n");
        sb.append("    }\n\n");
        sb.append("    private static String[] split(String path) {\n");
        sb.append("        return splitOn(path, '/');\n");
        sb.append("    }\n\n");
        sb.append("    private static String queryParam(String query, String name) {\n");
        sb.append("        if(query == null || query.length() == 0) return null;\n");
        sb.append("        String[] pairs = splitOn(query, '&');\n");
        sb.append("        for(int i = 0 ; i < pairs.length ; i++) {\n");
        sb.append("            int eq = pairs[i].indexOf('=');\n");
        sb.append("            if(eq > 0 && pairs[i].substring(0, eq).equals(name)) return decode(pairs[i].substring(eq + 1));\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    /** Header lookup is case-insensitive: HTTP does not guarantee header case. */\n");
        sb.append("    private static String header(java.util.Map headers, String name) {\n");
        sb.append("        if(headers == null) return null;\n");
        sb.append("        Object direct = headers.get(name);\n");
        sb.append("        if(direct != null) return String.valueOf(direct);\n");
        sb.append("        java.util.Iterator it = headers.keySet().iterator();\n");
        sb.append("        while(it.hasNext()) {\n");
        sb.append("            Object k = it.next();\n");
        sb.append("            if(k != null && String.valueOf(k).equalsIgnoreCase(name)) {\n");
        sb.append("                Object v = headers.get(k);\n");
        sb.append("                return v == null ? null : String.valueOf(v);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    /** Cookies are not a header of their own; they are pairs inside Cookie. */\n");
        sb.append("    private static String cookie(java.util.Map headers, String name) {\n");
        sb.append("        String raw = header(headers, \"Cookie\");\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        String[] pairs = splitOn(raw, ';');\n");
        sb.append("        for(int i = 0 ; i < pairs.length ; i++) {\n");
        sb.append("            String pair = pairs[i].trim();\n");
        sb.append("            int eq = pair.indexOf('=');\n");
        sb.append("            if(eq > 0 && pair.substring(0, eq).trim().equals(name)) return decode(pair.substring(eq + 1));\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    private static String[] splitOn(String value, char sep) {\n");
        sb.append("        java.util.List parts = new java.util.ArrayList();\n");
        sb.append("        int pos = 0;\n");
        sb.append("        while(true) {\n");
        sb.append("            int next = value.indexOf(sep, pos);\n");
        sb.append("            if(next < 0) { parts.add(value.substring(pos)); break; }\n");
        sb.append("            parts.add(value.substring(pos, next));\n");
        sb.append("            pos = next + 1;\n");
        sb.append("        }\n");
        sb.append("        String[] out = new String[parts.size()];\n");
        sb.append("        for(int i = 0 ; i < out.length ; i++) out[i] = (String)parts.get(i);\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    /** Percent-decoding, plus '+' as space in query values. */\n");
        sb.append("    private static String decode(String value) {\n");
        sb.append("        if(value == null) return null;\n");
        sb.append("        if(value.indexOf('%') < 0 && value.indexOf('+') < 0) return value;\n");
        // A run of escapes is one UTF-8 sequence, not one character each. Appending
        // %C3%A9 as two chars produced "\u00c3\u00a9" where the client sent one
        // accented letter, so consecutive escapes are gathered as bytes and decoded
        // together.
        sb.append("        StringBuilder out = new StringBuilder();\n");
        sb.append("        byte[] pending = new byte[value.length()];\n");
        sb.append("        int pendingLen = 0;\n");
        sb.append("        for(int i = 0 ; i < value.length() ; i++) {\n");
        sb.append("            char c = value.charAt(i);\n");
        sb.append("            if(c == '%' && i + 2 < value.length()) {\n");
        sb.append("                try {\n");
        sb.append("                    pending[pendingLen++] = (byte)Integer.parseInt(value.substring(i + 1, i + 3), 16);\n");
        sb.append("                    i += 2;\n");
        sb.append("                    continue;\n");
        sb.append("                } catch (NumberFormatException err) { }\n");
        sb.append("            }\n");
        sb.append("            if(pendingLen > 0) {\n");
        sb.append("                out.append(decodeUtf8(pending, pendingLen));\n");
        sb.append("                pendingLen = 0;\n");
        sb.append("            }\n");
        sb.append("            if(c == '+') { out.append(' '); continue; }\n");
        sb.append("            out.append(c);\n");
        sb.append("        }\n");
        sb.append("        if(pendingLen > 0) out.append(decodeUtf8(pending, pendingLen));\n");
        sb.append("        return out.toString();\n");
        sb.append("    }\n\n");
        sb.append("    /** The gathered escape bytes as text. Malformed input keeps its bytes rather than throwing. */\n");
        sb.append("    private static String decodeUtf8(byte[] bytes, int length) {\n");
        sb.append("        try {\n");
        sb.append("            return new String(bytes, 0, length, \"UTF-8\");\n");
        sb.append("        } catch (java.io.UnsupportedEncodingException err) {\n");
        sb.append("            return new String(bytes, 0, length);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    // A missing text value binds to 0 / null rather than throwing: an absent\n");
        sb.append("    // optional query parameter is not a server error.\n");
        sb.append("    private static int parseInt(String v) { return v == null || v.length() == 0 ? 0 : Integer.parseInt(v.trim()); }\n");
        sb.append("    private static long parseLong(String v) { return v == null || v.length() == 0 ? 0L : Long.parseLong(v.trim()); }\n");
        sb.append("    private static double parseDouble(String v) { return v == null || v.length() == 0 ? 0d : Double.parseDouble(v.trim()); }\n");
        sb.append("    private static Integer boxInt(String v) { return v == null || v.length() == 0 ? null : Integer.valueOf(v.trim()); }\n");
        sb.append("    private static Long boxLong(String v) { return v == null || v.length() == 0 ? null : Long.valueOf(v.trim()); }\n");
        sb.append("    private static Double boxDouble(String v) { return v == null || v.length() == 0 ? null : Double.valueOf(v.trim()); }\n");
        sb.append("    private static Float boxFloat(String v) { return v == null || v.length() == 0 ? null : Float.valueOf(v.trim()); }\n");
        sb.append("    private static Short boxShort(String v) { return v == null || v.length() == 0 ? null : Short.valueOf(v.trim()); }\n");
        sb.append("    private static Byte boxByte(String v) { return v == null || v.length() == 0 ? null : Byte.valueOf(v.trim()); }\n");
        sb.append("    private static Boolean boxBoolean(String v) { return v == null ? null : Boolean.valueOf(v.trim()); }\n");
    }

    // ----------------------------------------------------------------
    // DTO codecs
    // ----------------------------------------------------------------

    /// Emits a Map<->DTO codec from the type's public instance fields. Field-based
    /// rather than reflective on purpose: ParparVM has no usable reflection and
    /// Codename One obfuscates, so a name lookup at runtime would fail in exactly
    /// the builds that matter.
    private String generateDtoCodec(String binaryName, AnnotatedClass cls) {
        String pkg = RestClientAnnotationProcessor.packageOf(binaryName);
        String simple = RestClientAnnotationProcessor.simpleName(binaryName);
        StringBuilder sb = new StringBuilder(4096);
        if (pkg.length() > 0) sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations for ").append(binaryName).append(". Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(simple).append("Json {\n");
        sb.append("    private ").append(simple).append("Json() { }\n\n");

        sb.append("    public static java.util.Map toMap(").append(binaryName).append(" o) {\n");
        sb.append("        if(o == null) return null;\n");
        sb.append("        java.util.Map m = new java.util.LinkedHashMap();\n");
        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic() || !f.isPublic()) continue;
            if ((f.getAccess() & org.objectweb.asm.Opcodes.ACC_SYNTHETIC) != 0) continue;
            String type = fieldJavaType(f);
            sb.append("        m.put(\"").append(RestClientAnnotationProcessor.escape(f.getName()))
              .append("\", ").append(fieldToJson(type, "o." + f.getName())).append(");\n");
        }
        sb.append("        return m;\n");
        sb.append("    }\n\n");

        sb.append("    public static ").append(binaryName).append(" fromMap(java.util.Map m) {\n");
        sb.append("        if(m == null) return null;\n");
        sb.append("        ").append(binaryName).append(" o = new ").append(binaryName).append("();\n");
        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic() || !f.isPublic()) continue;
            if ((f.getAccess() & org.objectweb.asm.Opcodes.ACC_SYNTHETIC) != 0) continue;
            if (f.isFinal()) continue; // cannot be assigned after construction
            String type = fieldJavaType(f);
            sb.append("        o.").append(f.getName()).append(" = ")
              .append(fieldFromJson(type, "m.get(\"" + RestClientAnnotationProcessor.escape(f.getName()) + "\")"))
              .append(";\n");
        }
        sb.append("        return o;\n");
        sb.append("    }\n\n");
        emitCodecHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    /// NOTE: a direct-to-bytes writer was tried here and REVERTED. Emitting
    /// `toJson(T, com.codename1.backend.ByteSink)` beside `toMap` is worth a
    /// measured +29% on a JSON route (74% -> 94% of Go net/http), because it drops
    /// the per-request LinkedHashMap, the key hashing and the instanceof dispatch
    /// that walking a map costs.
    ///
    /// It cannot go here as things stand: generated sources are compiled against
    /// `ctx.getOutputClassDir()` and NOTHING ELSE (see the compile call above), so
    /// they cannot name a backend type. It happened to work for a backend app,
    /// whose own build puts com.codename1.backend in that same directory, and
    /// failed for every other project -- including this processor's own tests.
    ///
    /// To take the 29%, the generated code first needs a type it is allowed to
    /// name: either the project's compile classpath reaches the codec compile, or
    /// the sink interface lives somewhere generated code may always depend on.
    /// That is a deliberate decision about this processor's dependency contract,
    /// not a detail to slip in behind a performance patch.
    private static String fieldToJson(String type, String expr) {
        if (type.startsWith("java.util.List<") || type.startsWith("java.util.Set<")) {
            String element = type.substring(type.indexOf('<') + 1, type.length() - 1);
            if (element.startsWith("java.")) return "toValueList(" + expr + ")";
            // A nested DTO list has to become a list of MAPS; handing the writer
            // the DTOs themselves serialises them as toString().
            return "toMapList(" + expr + ", new ToMapFn() {\n"
                    + "            public java.util.Map convert(Object o) { return "
                    + codecFor(element) + ".toMap((" + element + ")o); }\n"
                    + "        })";
        }
        if (type.startsWith("java.") || type.indexOf('.') < 0) return expr;
        return codecFor(type) + ".toMap(" + expr + ")";
    }

    private static String fieldFromJson(String type, String expr) {
        if (type.startsWith("java.util.List<") || type.startsWith("java.util.Set<")) {
            String element = type.substring(type.indexOf('<') + 1, type.length() - 1);
            // Both branches below produce a List, so a Set-typed field has to be
            // converted rather than cast -- the same fix the request-body path
            // needed. Review named the java.* branch; the DTO branch had it too,
            // which is why this converts in one place at the end instead.
            boolean isSet = type.startsWith("java.util.Set<");
            if (element.startsWith("java.")) {
                String decoded = "asList(" + expr + ")";
                if (isSet) {
                    decoded = "setFromList(" + decoded + ")";
                }
                return "(" + type + ")(Object)" + decoded;
            }
            // Each element is converted through the element codec. Returning the
            // decoded Maps as-is -- which this used to do -- gives the handler a
            // List whose elements are Maps typed as DTOs: a lie the JVM catches at
            // the first field read and ParparVM does not catch at all.
            String decodedDtos = "fromMapList(" + expr + ", new FromMapFn() {\n"
                    + "            public Object convert(java.util.Map m) { return "
                    + codecFor(element) + ".fromMap(m); }\n"
                    + "        })";
            if (isSet) {
                decodedDtos = "setFromList(" + decodedDtos + ")";
            }
            return "(" + type + ")(Object)" + decodedDtos;
        }
        if ("java.lang.String".equals(type)) return "asString(" + expr + ")";
        if ("int".equals(type))     return "asInt(" + expr + ")";
        if ("long".equals(type))    return "asLong(" + expr + ")";
        if ("double".equals(type))  return "asDouble(" + expr + ")";
        if ("float".equals(type))   return "(float)asDouble(" + expr + ")";
        if ("short".equals(type))   return "(short)asInt(" + expr + ")";
        if ("byte".equals(type))    return "(byte)asInt(" + expr + ")";
        if ("boolean".equals(type)) return "asBoolean(" + expr + ")";
        if ("java.lang.Integer".equals(type)) return "asBoxedInt(" + expr + ")";
        if ("java.lang.Long".equals(type))    return "asBoxedLong(" + expr + ")";
        if ("java.lang.Double".equals(type))  return "asBoxedDouble(" + expr + ")";
        if ("java.lang.Boolean".equals(type)) return "asBoxedBoolean(" + expr + ")";
        // Float, Short and Byte need the same treatment as the three above. The JSON
        // reader only ever produces Long or Double, so leaving them to guardedCast
        // meant an instanceof against the declared wrapper that never matched, and
        // the field silently arrived null with the client's value discarded.
        if ("java.lang.Float".equals(type))   return "asBoxedFloat(" + expr + ")";
        if ("java.lang.Short".equals(type))   return "asBoxedShort(" + expr + ")";
        if ("java.lang.Byte".equals(type))    return "asBoxedByte(" + expr + ")";
        // Anything else out of java.* is narrowed with instanceof rather than cast:
        // the value came from the wire, so its type is the client's choice.
        if (type.startsWith("java.")) return guardedCast(type, expr);
        return codecFor(type) + ".fromMap(asMap(" + expr + "))";
    }

    private static void emitCodecHelpers(StringBuilder sb) {
        sb.append("    // The JSON reader produces Long for integers and Double for reals, so every\n");
        sb.append("    // numeric read goes through Number rather than casting to the field's type.\n");
        sb.append("    private static String asString(Object v) { return v == null ? null : String.valueOf(v); }\n");
        sb.append("    private static int asInt(Object v) { return v instanceof Number ? ((Number)v).intValue() : (v == null ? 0 : Integer.parseInt(String.valueOf(v).trim())); }\n");
        sb.append("    private static long asLong(Object v) { return v instanceof Number ? ((Number)v).longValue() : (v == null ? 0L : Long.parseLong(String.valueOf(v).trim())); }\n");
        sb.append("    private static double asDouble(Object v) { return v instanceof Number ? ((Number)v).doubleValue() : (v == null ? 0d : Double.parseDouble(String.valueOf(v).trim())); }\n");
        sb.append("    private static boolean asBoolean(Object v) { return v instanceof Boolean ? ((Boolean)v).booleanValue() : (v != null && Boolean.parseBoolean(String.valueOf(v).trim())); }\n");
        sb.append("    private static Integer asBoxedInt(Object v) { return v == null ? null : Integer.valueOf(asInt(v)); }\n");
        sb.append("    private static Long asBoxedLong(Object v) { return v == null ? null : Long.valueOf(asLong(v)); }\n");
        sb.append("    private static Double asBoxedDouble(Object v) { return v == null ? null : Double.valueOf(asDouble(v)); }\n");
        sb.append("    private static Boolean asBoxedBoolean(Object v) { return v == null ? null : Boolean.valueOf(asBoolean(v)); }\n");
        sb.append("    private static Float asBoxedFloat(Object v) { return v == null ? null : Float.valueOf((float)asDouble(v)); }\n");
        sb.append("    private static Short asBoxedShort(Object v) { return v == null ? null : Short.valueOf((short)asInt(v)); }\n");
        sb.append("    private static Byte asBoxedByte(Object v) { return v == null ? null : Byte.valueOf((byte)asInt(v)); }\n");
        sb.append("    /** A decoded value narrowed to a JSON object, or null -- never a cast. */\n");
        sb.append("    private static java.util.Map asMap(Object v) { return v instanceof java.util.Map ? (java.util.Map)v : null; }\n");
        sb.append("    private static java.util.List asList(Object v) { return v instanceof java.util.List ? (java.util.List)v : null; }\n");
        sb.append("    /** A decoded array as a Set, preserving the order it arrived in. */\n");
        sb.append("    private static java.util.Set setFromList(java.util.List v) {\n");
        sb.append("        return v == null ? null : new java.util.LinkedHashSet(v);\n");
        sb.append("    }\n");
        sb.append("    private interface ToMapFn { java.util.Map convert(Object o); }\n");
        sb.append("    private interface FromMapFn { Object convert(java.util.Map m); }\n");
        sb.append("    private static java.util.List toValueList(java.util.Collection raw) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        java.util.Iterator it = raw.iterator();\n");
        sb.append("        while(it.hasNext()) out.add(it.next());\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
        sb.append("    private static java.util.List toMapList(java.util.Collection raw, ToMapFn f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        java.util.Iterator it = raw.iterator();\n");
        sb.append("        while(it.hasNext()) { Object e = it.next(); out.add(e == null ? null : f.convert(e)); }\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
        sb.append("    private static java.util.List fromMapList(Object raw, FromMapFn f) {\n");
        sb.append("        if(!(raw instanceof java.util.List)) return null;\n");
        sb.append("        java.util.List src = (java.util.List)raw;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        for(int i = 0 ; i < src.size() ; i++) {\n");
        sb.append("            Object e = src.get(i);\n");
        sb.append("            out.add(e instanceof java.util.Map ? f.convert((java.util.Map)e) : null);\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
    }

    private static String[] splitTemplate(String template) {
        String t = template == null ? "" : template;
        List<String> parts = new ArrayList<String>();
        int pos = 0;
        while (pos <= t.length()) {
            int next = t.indexOf('/', pos);
            if (next < 0) { parts.add(t.substring(pos)); break; }
            parts.add(t.substring(pos, next));
            pos = next + 1;
        }
        return parts.toArray(new String[parts.size()]);
    }

    private static boolean isPlaceholder(String segment) {
        return segment.length() > 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}';
    }

    private static int placeholderIndex(String[] template, String name) {
        for (int i = 0; i < template.length; i++) {
            if (isPlaceholder(template[i])
                    && template[i].substring(1, template[i].length() - 1).equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
