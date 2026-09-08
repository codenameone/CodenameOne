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
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.objectweb.asm.Type;

/**
 * Generates a router for every `@RestController`, and the `main` that serves them.
 *
 * The shape is Spring's on purpose -- `@RestController`, `@GetMapping`,
 * `@PathVariable`, `@RequestParam`, `@RequestBody`, `@ResponseStatus` mean here what
 * they mean there -- so that reading one is enough to read the other. What differs is
 * where the work happens: Spring resolves a route by walking a registry at request
 * time, and this resolves it at build time into code that compares the request's own
 * bytes.
 *
 * That is the reason to generate rather than reflect. A handwritten
 * `if ("/healthz".equals(request.getTarget()))` builds a String for the target,
 * hashes it and compares it, on every request and for every route it tests before the
 * one that matches; and it is wrong the moment the client appends a query string. The
 * generated form holds each route as a `byte[]` constant and asks the Request whether
 * its path bytes are those bytes -- no String, no hash, and the query cannot break it.
 * A route with no path variables therefore allocates nothing at all, which is what
 * keeps the collector out of the request path.
 *
 * Reflection is not an option regardless: this code is translated to C, and
 * `Class.forName` on an obfuscated name does not survive that. Generating source that
 * the same compiler sees is what makes the wiring visible to the dead-code pass.
 */
public final class RestControllerAnnotationProcessor extends AbstractAnnotationProcessor {

    private static final String PKG = "Lcom/codename1/backend/annotations/";
    private static final String CONTROLLER = PKG + "RestController;";
    private static final String REQUEST_MAPPING = PKG + "RequestMapping;";
    private static final String PATH_VARIABLE = PKG + "PathVariable;";
    private static final String REQUEST_PARAM = PKG + "RequestParam;";
    private static final String REQUEST_HEADER = PKG + "RequestHeader;";
    private static final String REQUEST_BODY = PKG + "RequestBody;";
    private static final String RESPONSE_STATUS = PKG + "ResponseStatus;";

    /** Mapping annotation to the HTTP method it stands for. */
    private static final Map<String, String> MAPPINGS;
    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(PKG + "GetMapping;", "GET");
        m.put(PKG + "PostMapping;", "POST");
        m.put(PKG + "PutMapping;", "PUT");
        m.put(PKG + "DeleteMapping;", "DELETE");
        m.put(PKG + "PatchMapping;", "PATCH");
        MAPPINGS = Collections.unmodifiableMap(m);
    }

    private static final String REQUEST_TYPE = "com.codename1.backend.HttpServer.Request";
    private static final String RESPONSE_TYPE = "com.codename1.backend.HttpServer.Response";

    /** Where the generated bootstrap's name is left for the packaging goal to read. */
    public static final String MAIN_CLASS_RESOURCE = "META-INF/cn1-backend-main";

    private final TreeMap<String, Controller> controllers = new TreeMap<String, Controller>();

    private static final class Controller {
        String binaryName;
        String packageName;
        String simpleName;
        String routerSimpleName;
        List<String> basePaths = new ArrayList<String>();
        List<Route> routes = new ArrayList<Route>();
    }

    private static final class Route {
        String httpMethod;
        String pattern;
        String javaMethod;
        String returnJavaType;
        int status;
        List<Param> params = new ArrayList<Param>();
        /** The literal bytes before the first `{`; the whole pattern when there is none. */
        String prefix;
        /** For each variable, the literal that must follow it; "" when it runs to the end. */
        List<String> after = new ArrayList<String>();
    }

    private static final class Param {
        String kind;      // PATH, QUERY, HEADER, BODY, REQUEST
        String name;
        String javaType;
        String defaultValue;
        int variableIndex = -1;
    }

    @Override
    public Set<String> getAnnotationDescriptors() {
        return Collections.singleton(CONTROLLER);
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.getClassAnnotation(CONTROLLER) == null) {
            return;
        }
        if (cls.isInterface() || cls.isAbstract()) {
            ctx.error(cls, "@RestController must be a concrete class: " + cls.getBinaryName());
            return;
        }
        Controller controller = new Controller();
        controller.binaryName = cls.getBinaryName();
        controller.packageName = RestClientAnnotationProcessor.packageOf(controller.binaryName);
        controller.simpleName = RestClientAnnotationProcessor.simpleName(controller.binaryName);
        controller.routerSimpleName = controller.simpleName + "Router";
        controller.basePaths.addAll(pathsOf(cls.getClassAnnotation(REQUEST_MAPPING)));
        if (controller.basePaths.isEmpty()) {
            controller.basePaths.add("");
        }
        if (!hasNoArgConstructor(cls)) {
            ctx.error(cls, "@RestController needs a public no-argument constructor so the "
                    + "generated bootstrap can create it: " + controller.binaryName);
            return;
        }

        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() || m.isSynthetic() || m.isStatic() || !m.isPublic()) {
                continue;
            }
            String httpMethod = null;
            List<String> paths = null;
            for (Map.Entry<String, String> e : MAPPINGS.entrySet()) {
                AnnotationValues values = m.getAnnotation(e.getKey());
                if (values != null) {
                    if (httpMethod != null) {
                        ctx.error(cls, "More than one mapping annotation on "
                                + controller.binaryName + "." + m.getName());
                        return;
                    }
                    httpMethod = e.getValue();
                    paths = pathsOf(values);
                }
            }
            AnnotationValues mapping = m.getAnnotation(REQUEST_MAPPING);
            if (mapping != null) {
                if (httpMethod != null) {
                    ctx.error(cls, "@RequestMapping and a shorthand mapping on the same "
                            + "method: " + controller.binaryName + "." + m.getName());
                    return;
                }
                httpMethod = mapping.getStringOrDefault("method", "GET");
                paths = pathsOf(mapping);
            }
            if (httpMethod == null) {
                continue;
            }
            if (paths.isEmpty()) {
                paths = Collections.singletonList("");
            }
            for (String base : controller.basePaths) {
                for (String path : paths) {
                    Route route = buildRoute(cls, m, httpMethod, join(base, path), ctx);
                    if (route == null) {
                        return;
                    }
                    controller.routes.add(route);
                }
            }
        }
        if (controller.routes.isEmpty()) {
            ctx.error(cls, "@RestController declares no mapped methods: " + controller.binaryName);
            return;
        }
        controllers.put(controller.binaryName, controller);
    }

    /**
     * Splits a route pattern into the parts the generated matcher needs.
     *
     * `/notes/{id}/tags` becomes prefix `/notes/` and one variable followed by
     * `/tags`. The prefix is what the byte compare rejects on, which is most
     * requests for most routes, and it is why the split happens here rather than at
     * request time.
     */
    private Route buildRoute(AnnotatedClass cls, MethodInfo m, String httpMethod, String pattern,
            ProcessorContext ctx) {
        Route route = new Route();
        route.httpMethod = httpMethod;
        route.pattern = pattern.length() == 0 ? "/" : pattern;
        route.javaMethod = m.getName();

        int firstVar = route.pattern.indexOf('{');
        route.prefix = firstVar < 0 ? route.pattern : route.pattern.substring(0, firstVar);
        List<String> variableNames = new ArrayList<String>();
        int pos = firstVar;
        while (pos >= 0) {
            int close = route.pattern.indexOf('}', pos);
            if (close < 0) {
                ctx.error(cls, "Unclosed '{' in route " + route.pattern + " on "
                        + cls.getBinaryName() + "." + m.getName());
                return null;
            }
            variableNames.add(route.pattern.substring(pos + 1, close));
            int next = route.pattern.indexOf('{', close);
            route.after.add(next < 0 ? route.pattern.substring(close + 1)
                                     : route.pattern.substring(close + 1, next));
            pos = next;
        }

        Type[] paramTypes = Type.getArgumentTypes(m.getDescriptor());
        List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
        for (int i = 0; i < paramTypes.length; i++) {
            Param p = new Param();
            p.javaType = RestClientAnnotationProcessor.javaTypeFor(paramTypes[i], null);
            Map<String, AnnotationValues> annotations = i < paramAnnotations.size()
                    ? paramAnnotations.get(i) : Collections.<String, AnnotationValues>emptyMap();
            AnnotationValues pathVariable = annotations.get(PATH_VARIABLE);
            AnnotationValues requestParam = annotations.get(REQUEST_PARAM);
            AnnotationValues requestHeader = annotations.get(REQUEST_HEADER);
            AnnotationValues requestBody = annotations.get(REQUEST_BODY);
            if (pathVariable != null) {
                p.kind = "PATH";
                p.name = pathVariable.getStringOrDefault("value", "");
                p.defaultValue = pathVariable.getStringOrDefault("defaultValue", "");
                p.variableIndex = variableNames.indexOf(p.name);
                if (p.name.length() == 0) {
                    // Parameter names are not in the class file unless javac was told to
                    // keep them, and a router that guessed would bind the wrong value in
                    // silence. Naming it is one word and removes the whole question.
                    ctx.error(cls, "@PathVariable needs the variable name, as "
                            + "@PathVariable(\"id\"): " + cls.getBinaryName() + "."
                            + m.getName());
                    return null;
                }
                if (p.variableIndex < 0) {
                    ctx.error(cls, "@PathVariable(\"" + p.name + "\") does not appear in the "
                            + "route " + route.pattern + " on " + cls.getBinaryName() + "."
                            + m.getName());
                    return null;
                }
            } else if (requestParam != null) {
                p.kind = "QUERY";
                p.name = requestParam.getStringOrDefault("value", "");
                p.defaultValue = requestParam.getStringOrDefault("defaultValue", "");
                if (p.name.length() == 0) {
                    ctx.error(cls, "@RequestParam needs the parameter name: "
                            + cls.getBinaryName() + "." + m.getName());
                    return null;
                }
            } else if (requestHeader != null) {
                p.kind = "HEADER";
                p.name = requestHeader.getStringOrDefault("value", "");
                p.defaultValue = requestHeader.getStringOrDefault("defaultValue", "");
                if (p.name.length() == 0) {
                    ctx.error(cls, "@RequestHeader needs the header name: "
                            + cls.getBinaryName() + "." + m.getName());
                    return null;
                }
            } else if (requestBody != null) {
                p.kind = "BODY";
            } else if (REQUEST_TYPE.equals(p.javaType)) {
                // The escape hatch: a handler that needs something this binding does not
                // model takes the Request itself, exactly as it would have before.
                p.kind = "REQUEST";
            } else {
                ctx.error(cls, "Parameter " + (i + 1) + " of " + cls.getBinaryName() + "."
                        + m.getName() + " has no binding annotation. Annotate it with "
                        + "@PathVariable, @RequestParam, @RequestHeader or @RequestBody, "
                        + "or declare it as HttpServer.Request");
                return null;
            }
            if (!"REQUEST".equals(p.kind) && !isBindable(p.javaType, p.kind)) {
                ctx.error(cls, "Cannot bind " + p.javaType + " from the request on "
                        + cls.getBinaryName() + "." + m.getName() + ". Path, query and "
                        + "header values bind to String and the primitive types; a body "
                        + "binds to String or java.util.Map");
                return null;
            }
            route.params.add(p);
        }

        route.returnJavaType = RestClientAnnotationProcessor.javaTypeFor(
                Type.getReturnType(m.getDescriptor()), null);
        AnnotationValues status = m.getAnnotation(RESPONSE_STATUS);
        route.status = status == null ? 200 : status.getIntOrDefault("value", 200);
        return route;
    }

    private static boolean isBindable(String javaType, String kind) {
        if ("BODY".equals(kind)) {
            return "java.lang.String".equals(javaType) || "java.util.Map".equals(javaType)
                    || "java.util.List".equals(javaType);
        }
        return "java.lang.String".equals(javaType) || "int".equals(javaType)
                || "long".equals(javaType) || "boolean".equals(javaType)
                || "double".equals(javaType) || "float".equals(javaType)
                || "short".equals(javaType) || "byte".equals(javaType);
    }

    private static boolean hasNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && m.isPublic()
                    && Type.getArgumentTypes(m.getDescriptor()).length == 0) {
                return true;
            }
        }
        return false;
    }

    private static List<String> pathsOf(AnnotationValues values) {
        List<String> out = new ArrayList<String>();
        if (values == null) {
            return out;
        }
        Object value = values.get("value");
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                out.add(String.valueOf(item));
            }
        } else if (value instanceof Object[]) {
            for (Object item : (Object[]) value) {
                out.add(String.valueOf(item));
            }
        } else if (value != null) {
            out.add(String.valueOf(value));
        }
        return out;
    }

    /** Joins a class-level base with a method-level path, without doubling the slash. */
    private static String join(String base, String path) {
        String left = base == null ? "" : base.trim();
        String right = path == null ? "" : path.trim();
        while (left.endsWith("/")) {
            left = left.substring(0, left.length() - 1);
        }
        if (right.length() == 0) {
            return left.length() == 0 ? "/" : left;
        }
        if (!right.startsWith("/")) {
            right = "/" + right;
        }
        return left + right;
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors() || controllers.isEmpty()) {
            return;
        }
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (Controller c : controllers.values()) {
            sources.put(qualify(c.packageName, c.routerSimpleName), generateRouter(c));
        }
        Controller first = controllers.values().iterator().next();
        String bootstrap = qualify(first.packageName, "BackendApplication");
        sources.put(bootstrap, generateBootstrap(first.packageName));
        try {
            List<File> cp = new ArrayList<File>();
            cp.add(ctx.getOutputClassDir());
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new File(element));
            }
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
            ctx.emitResource(MAIN_CLASS_RESOURCE, asciiBytes(bootstrap));
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile the generated @RestController "
                    + "sources: " + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + controllers.size() + " @RestController router(s) and "
                + bootstrap);
    }

    private static byte[] asciiBytes(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required of every VM", err);
        }
    }

    private static String qualify(String pkg, String simple) {
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    /**
     * The router for one controller.
     *
     * Routes are emitted grouped by HTTP method and, within a group, static routes
     * before dynamic ones. A static route is a single byte compare; a dynamic one
     * pays for a prefix compare first, so an unrelated request leaves without
     * allocating anything.
     */
    private static String generateRouter(Controller c) {
        StringBuilder sb = new StringBuilder();
        if (c.packageName.length() > 0) {
            sb.append("package ").append(c.packageName).append(";\n\n");
        }
        sb.append("// Generated from @RestController on ").append(c.binaryName)
          .append(". Do not edit.\n");
        sb.append("public final class ").append(c.routerSimpleName)
          .append(" implements com.codename1.backend.HttpServer.Handler {\n\n");

        List<Route> ordered = new ArrayList<Route>(c.routes);
        Collections.sort(ordered, new java.util.Comparator<Route>() {
            public int compare(Route a, Route b) {
                int byMethod = a.httpMethod.compareTo(b.httpMethod);
                if (byMethod != 0) {
                    return byMethod;
                }
                // Static routes first: they answer without touching the heap, and a
                // dynamic route whose prefix also matches must not take the request
                // from one that matches exactly.
                int byKind = (a.after.isEmpty() ? 0 : 1) - (b.after.isEmpty() ? 0 : 1);
                if (byKind != 0) {
                    return byKind;
                }
                return b.pattern.length() - a.pattern.length();
            }
        });

        for (int i = 0; i < ordered.size(); i++) {
            Route route = ordered.get(i);
            sb.append("    private static final byte[] P").append(i).append(" = ")
              .append(byteArrayLiteral(route.after.isEmpty() ? route.pattern : route.prefix))
              .append("; // ").append(route.httpMethod).append(' ').append(route.pattern)
              .append('\n');
            if (!route.after.isEmpty()) {
                sb.append("    private static final String[] A").append(i).append(" = ")
                  .append(stringArrayLiteral(route.after)).append(";\n");
            }
        }

        sb.append("\n    private final ").append(c.simpleName).append(" impl;\n\n");
        sb.append("    public ").append(c.routerSimpleName).append("(").append(c.simpleName)
          .append(" impl) {\n        this.impl = impl;\n    }\n\n");

        // throws Exception because Handler does: a controller method that reads a
        // database throws IOException, and a router that could not pass it on would
        // force every handler to swallow its own errors.
        sb.append("    public com.codename1.backend.HttpServer.Response handle(\n")
          .append("            com.codename1.backend.HttpServer.Request request) throws Exception {\n");
        sb.append("        String httpMethod = request.getMethod();\n");

        String current = null;
        boolean open = false;
        for (int i = 0; i < ordered.size(); i++) {
            Route route = ordered.get(i);
            if (!route.httpMethod.equals(current)) {
                if (open) {
                    sb.append("        }\n");
                }
                sb.append("        if (\"").append(route.httpMethod)
                  .append("\".equals(httpMethod)) {\n");
                current = route.httpMethod;
                open = true;
            }
            emitRoute(sb, route, i, c);
        }
        if (open) {
            sb.append("        }\n");
        }
        sb.append("        // No route here. Returning null lets the server answer 404, and\n");
        sb.append("        // lets another router be tried first when several are chained.\n");
        sb.append("        return null;\n    }\n\n");
        emitRouterHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitRoute(StringBuilder sb, Route route, int index, Controller c) {
        String pad = "                ";
        if (route.after.isEmpty()) {
            sb.append("            if (request.pathIs(P").append(index).append(")) {\n");
        } else {
            sb.append("            if (request.pathStartsWith(P").append(index).append(")) {\n");
            sb.append(pad).append("String[] bound = bindPath(request.pathFrom(P").append(index)
              .append(".length), A").append(index).append(");\n");
            sb.append(pad).append("if (bound != null) {\n");
            pad = "                    ";
        }

        StringBuilder args = new StringBuilder();
        for (int i = 0; i < route.params.size(); i++) {
            Param p = route.params.get(i);
            if (i > 0) {
                args.append(", ");
            }
            args.append(argumentExpression(p));
        }

        String call = "impl." + route.javaMethod + "(" + args + ")";
        if ("void".equals(route.returnJavaType)) {
            sb.append(pad).append(call).append(";\n");
            sb.append(pad).append("return request.respond(").append(route.status)
              .append(", \"text/plain\", EMPTY);\n");
        } else if (RESPONSE_TYPE.equals(route.returnJavaType)) {
            // The handler built its own Response; a status annotation would be a lie
            // about something this router no longer controls.
            sb.append(pad).append("return ").append(call).append(";\n");
        } else if ("java.lang.String".equals(route.returnJavaType)) {
            sb.append(pad).append("String result = ").append(call).append(";\n");
            sb.append(pad).append("return result == null ? request.respond(404, \"text/plain\", EMPTY)\n");
            sb.append(pad).append("        : request.respond(").append(route.status)
              .append(", \"text/plain; charset=utf-8\", utf8(result));\n");
        } else {
            // Everything else is JSON. respondJson writes into the connection's own
            // Response, so a route that returns a value still allocates only that value.
            sb.append(pad).append("Object result = ").append(call).append(";\n");
            sb.append(pad).append("return result == null ? request.respond(404, \"text/plain\", EMPTY)\n");
            sb.append(pad).append("        : request.respondJson(").append(route.status)
              .append(", result);\n");
        }

        if (!route.after.isEmpty()) {
            sb.append("                }\n");
        }
        sb.append("            }\n");
    }

    private static String argumentExpression(Param p) {
        if ("REQUEST".equals(p.kind)) {
            return "request";
        }
        String raw;
        if ("PATH".equals(p.kind)) {
            raw = "bound[" + p.variableIndex + "]";
        } else if ("QUERY".equals(p.kind)) {
            raw = "request.queryParam(" + quote(p.name) + ")";
        } else if ("HEADER".equals(p.kind)) {
            raw = "request.getHeader(" + quote(p.name) + ")";
        } else {
            return bodyExpression(p);
        }
        return convert(p.javaType, raw, p.defaultValue);
    }

    private static String bodyExpression(Param p) {
        if ("java.lang.String".equals(p.javaType)) {
            return "request.getBody()";
        }
        if ("java.util.Map".equals(p.javaType)) {
            return "bodyAsMap(request.getBody())";
        }
        return "bodyAsList(request.getBody())";
    }

    /**
     * Converts a raw request value to the parameter's type.
     *
     * An absent value takes the annotation's default rather than throwing, which is
     * what Spring does and what a caller expects from `defaultValue`.
     */
    private static String convert(String javaType, String raw, String defaultValue) {
        String fallback = defaultValue == null ? "" : defaultValue;
        if ("java.lang.String".equals(javaType)) {
            return fallback.length() == 0 ? raw
                    : "orDefault(" + raw + ", " + quote(fallback) + ")";
        }
        String zero = "boolean".equals(javaType) ? "false" : "0";
        String literalDefault = fallback.length() == 0 ? zero
                : "to" + capitalize(javaType) + "(" + quote(fallback) + ", " + zero + ")";
        return "to" + capitalize(javaType) + "(" + raw + ", " + literalDefault + ")";
    }

    private static String capitalize(String javaType) {
        return Character.toUpperCase(javaType.charAt(0)) + javaType.substring(1);
    }

    /**
     * The helpers every router shares. Emitted into each router rather than into a
     * runtime class so that a module with no controllers links none of it, and so
     * the dead-code pass can drop whichever ones this controller never calls.
     */
    private static void emitRouterHelpers(StringBuilder sb) {
        sb.append("    private static final byte[] EMPTY = new byte[0];\n\n");
        sb.append("    /**\n");
        sb.append("     * Binds the path variables, or returns null when the rest of the path is\n");
        sb.append("     * not this route after all. `after[i]` is the literal that follows\n");
        sb.append("     * variable i, empty when the variable runs to the end.\n");
        sb.append("     */\n");
        sb.append("    private static String[] bindPath(String rest, String[] after) {\n");
        sb.append("        String[] out = new String[after.length];\n");
        sb.append("        int pos = 0;\n");
        sb.append("        for (int i = 0 ; i < after.length ; i++) {\n");
        sb.append("            String literal = after[i];\n");
        sb.append("            if (literal.length() == 0) {\n");
        sb.append("                String value = rest.substring(pos);\n");
        sb.append("                if (value.length() == 0 || value.indexOf('/') >= 0) {\n");
        sb.append("                    return null;\n");
        sb.append("                }\n");
        sb.append("                out[i] = decode(value);\n");
        sb.append("                return out;\n");
        sb.append("            }\n");
        sb.append("            int at = rest.indexOf(literal, pos);\n");
        sb.append("            if (at < 0) {\n");
        sb.append("                return null;\n");
        sb.append("            }\n");
        sb.append("            String value = rest.substring(pos, at);\n");
        sb.append("            // A variable is one segment. Without this, /notes/{id} would\n");
        sb.append("            // match /notes/1/2 and hand the method \"1/2\" as the id.\n");
        sb.append("            if (value.length() == 0 || value.indexOf('/') >= 0) {\n");
        sb.append("                return null;\n");
        sb.append("            }\n");
        sb.append("            out[i] = decode(value);\n");
        sb.append("            pos = at + literal.length();\n");
        sb.append("        }\n");
        sb.append("        return pos == rest.length() ? out : null;\n");
        sb.append("    }\n\n");

        sb.append("    /**\n");
        sb.append("     * Percent-decodes one path segment. The octets are gathered and decoded\n");
        sb.append("     * as a run: an escape carries one byte of UTF-8, and decoding them one\n");
        sb.append("     * at a time turns every non-ASCII value into mojibake.\n");
        sb.append("     */\n");
        sb.append("    private static String decode(String value) {\n");
        sb.append("        if (value.indexOf('%') < 0) {\n");
        sb.append("            return value;\n");
        sb.append("        }\n");
        sb.append("        byte[] out = new byte[value.length()];\n");
        sb.append("        int length = 0;\n");
        sb.append("        for (int i = 0 ; i < value.length() ; i++) {\n");
        sb.append("            char c = value.charAt(i);\n");
        sb.append("            if (c == '%' && i + 2 < value.length()) {\n");
        sb.append("                int hi = hex(value.charAt(i + 1));\n");
        sb.append("                int lo = hex(value.charAt(i + 2));\n");
        sb.append("                if (hi >= 0 && lo >= 0) {\n");
        sb.append("                    out[length++] = (byte)((hi << 4) | lo);\n");
        sb.append("                    i += 2;\n");
        sb.append("                    continue;\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("            out[length++] = (byte)c;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            return new String(out, 0, length, \"UTF-8\");\n");
        sb.append("        } catch (java.io.UnsupportedEncodingException err) {\n");
        sb.append("            return new String(out, 0, length);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private static int hex(char c) {\n");
        sb.append("        if (c >= '0' && c <= '9') { return c - '0'; }\n");
        sb.append("        if (c >= 'a' && c <= 'f') { return c - 'a' + 10; }\n");
        sb.append("        if (c >= 'A' && c <= 'F') { return c - 'A' + 10; }\n");
        sb.append("        return -1;\n");
        sb.append("    }\n\n");

        sb.append("    private static byte[] utf8(String value) {\n");
        sb.append("        try {\n");
        sb.append("            return value.getBytes(\"UTF-8\");\n");
        sb.append("        } catch (java.io.UnsupportedEncodingException err) {\n");
        sb.append("            return value.getBytes();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private static String orDefault(String value, String fallback) {\n");
        sb.append("        return value == null ? fallback : value;\n");
        sb.append("    }\n\n");

        // The numeric binders. A malformed value takes the default rather than
        // failing the request: a query string is user input, and 400 for "?page=x"
        // is a choice the handler should get to make.
        String[][] numeric = {
            {"Int", "int", "Integer.parseInt"},
            {"Long", "long", "Long.parseLong"},
            {"Double", "double", "Double.parseDouble"},
            {"Float", "float", "Float.parseFloat"},
            {"Short", "short", "Short.parseShort"},
            {"Byte", "byte", "Byte.parseByte"},
        };
        for (int i = 0; i < numeric.length; i++) {
            sb.append("    private static ").append(numeric[i][1]).append(" to")
              .append(numeric[i][0]).append("(String value, ").append(numeric[i][1])
              .append(" fallback) {\n");
            sb.append("        if (value == null || value.length() == 0) {\n");
            sb.append("            return fallback;\n");
            sb.append("        }\n");
            sb.append("        try {\n");
            sb.append("            return ").append(numeric[i][2]).append("(value.trim());\n");
            sb.append("        } catch (NumberFormatException err) {\n");
            sb.append("            return fallback;\n");
            sb.append("        }\n");
            sb.append("    }\n\n");
        }
        sb.append("    private static boolean toBoolean(String value, boolean fallback) {\n");
        sb.append("        if (value == null || value.length() == 0) {\n");
        sb.append("            return fallback;\n");
        sb.append("        }\n");
        sb.append("        // Case folding a token with toLowerCase() is locale sensitive and\n");
        sb.append("        // wrong on a Turkish device; equalsIgnoreCase is not.\n");
        sb.append("        return value.equalsIgnoreCase(\"true\") || value.equals(\"1\")\n");
        sb.append("                || value.equalsIgnoreCase(\"yes\") || value.equalsIgnoreCase(\"on\");\n");
        sb.append("    }\n\n");

        sb.append("    private static java.util.Map bodyAsMap(String body) {\n");
        sb.append("        if (body == null || body.length() == 0) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            return com.codename1.backend.Json.parseObject(body);\n");
        sb.append("        } catch (java.io.IOException err) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private static java.util.List bodyAsList(String body) {\n");
        sb.append("        if (body == null || body.length() == 0) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            Object parsed = com.codename1.backend.Json.parse(body);\n");
        sb.append("            // Never a cast: ParparVM's CHECKCAST is unchecked, so a wrong\n");
        sb.append("            // one reads the next instruction's fields out of the wrong\n");
        sb.append("            // object instead of throwing.\n");
        sb.append("            return parsed instanceof java.util.List ? (java.util.List)parsed : null;\n");
        sb.append("        } catch (java.io.IOException err) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("    }\n");
    }

    /**
     * The `main` the developer no longer writes.
     *
     * This is the half of the old API that was an implementation detail wearing a
     * user's clothes: every server opened with the same twenty lines -- read PORT,
     * start, install a shutdown handler, await termination -- and getting any of
     * them wrong produced a server that leaked connections on SIGTERM or exited
     * silently the moment main returned. Generating it means the controller is the
     * only thing anyone writes, and the lifecycle is the same in every project.
     */
    private String generateBootstrap(String packageName) {
        StringBuilder sb = new StringBuilder();
        if (packageName.length() > 0) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        sb.append("// Generated from the @RestController classes in this module. Do not edit.\n");
        sb.append("public final class BackendApplication {\n\n");
        sb.append("    private BackendApplication() {\n    }\n\n");
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        com.codename1.backend.Signals.installShutdownHandler();\n");
        sb.append("        int port = 8080;\n");
        sb.append("        String configured = System.getenv(\"PORT\");\n");
        sb.append("        if (configured != null && configured.length() > 0) {\n");
        sb.append("            try {\n");
        sb.append("                port = Integer.parseInt(configured.trim());\n");
        sb.append("            } catch (NumberFormatException err) {\n");
        sb.append("                throw new IllegalStateException(\"PORT is not a number: \"\n");
        sb.append("                        + configured);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        final com.codename1.backend.HttpServer.Handler[] routers =\n");
        sb.append("                new com.codename1.backend.HttpServer.Handler[] {\n");
        int index = 0;
        for (Controller c : controllers.values()) {
            sb.append("                    new ").append(qualify(c.packageName, c.routerSimpleName))
              .append("(new ").append(c.binaryName).append("())");
            sb.append(++index < controllers.size() ? ",\n" : "\n");
        }
        sb.append("                };\n");
        sb.append("        final com.codename1.backend.HttpServer server =\n");
        sb.append("                com.codename1.backend.HttpServer.start(null, port, 512, 16,\n");
        sb.append("                new com.codename1.backend.HttpServer.Handler() {\n");
        sb.append("            public com.codename1.backend.HttpServer.Response handle(\n");
        sb.append("                    com.codename1.backend.HttpServer.Request request)\n");
        sb.append("                    throws Exception {\n");
        sb.append("                for (int i = 0 ; i < routers.length ; i++) {\n");
        sb.append("                    com.codename1.backend.HttpServer.Response response =\n");
        sb.append("                            routers[i].handle(request);\n");
        sb.append("                    if (response != null) {\n");
        sb.append("                        return response;\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("                return null;\n");
        sb.append("            }\n");
        sb.append("        }, null);\n");
        sb.append("        com.codename1.backend.Signals.onShutdown(new Runnable() {\n");
        sb.append("            public void run() {\n");
        sb.append("                // Stop accepting, let what is in flight finish, then leave.\n");
        sb.append("                server.stop(10000);\n");
        sb.append("                System.exit(0);\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("        // Required: the host threads are detached, so a main that returned\n");
        sb.append("        // would end the process without a word.\n");
        sb.append("        server.awaitTermination();\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /** A route as a byte[] constant, which is what the request is compared against. */
    private static String byteArrayLiteral(String value) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7f) {
                // A route pattern is written in source, and a non-ASCII one would have
                // to be compared against its percent-encoded form on the wire.
                throw new IllegalArgumentException("Route patterns must be ASCII: " + value);
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append((int) c);
        }
        return sb.append("}").toString();
    }

    private static String stringArrayLiteral(List<String> values) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append("}").toString();
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c < 0x20 || c > 0x7e) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
