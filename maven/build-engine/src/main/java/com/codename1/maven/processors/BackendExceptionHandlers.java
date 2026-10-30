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

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessorContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

/// `@ExceptionHandler` methods, on a controller or a `@ControllerAdvice` bean,
/// compiled to an `instanceof` chain.
///
/// The chain is ordered at build time, deepest declared type first, so the first
/// test that passes is the handler for the closest superclass of what was thrown:
/// every declared type that matches lies on the thrown type's one superclass
/// chain, and the deeper of two is the closer. That is the precedence Spring's
/// `ExceptionHandlerMethodResolver` computes at run time, decided here instead.
final class BackendExceptionHandlers {
    static final String PKG = "Lcom/codename1/backend/annotations/";
    static final String EXCEPTION_HANDLER = PKG + "ExceptionHandler;";
    static final String CONTROLLER_ADVICE = PKG + "ControllerAdvice;";
    static final String REST_CONTROLLER_ADVICE = PKG + "RestControllerAdvice;";
    private static final String RESPONSE_STATUS = PKG + "ResponseStatus;";
    private static final String REQUEST = "com.codename1.backend.HttpServer$Request";
    private static final String RESPONSE = "com.codename1.backend.HttpServer$Response";
    private static final String HANDLER_METHOD = "com.codename1.backend.web.HandlerMethod";
    private static final Set<String> JSON_RETURNS = new HashSet<String>(java.util.Arrays.asList(
            "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
            "java.util.List", "java.util.ArrayList", "java.util.LinkedList",
            "java.util.Collection", "java.util.Set", "java.util.HashSet",
            "java.util.LinkedHashSet", "com.codename1.backend.Json$Writable"));

    /// One handled type of one method.
    static final class Handler {
        String exception;
        int depth;
        int declared;
        String call;
    }

    private BackendExceptionHandlers() {
    }

    /// Whether `cls` is a `@ControllerAdvice` or `@RestControllerAdvice`.
    static AnnotationValues advice(AnnotatedClass cls) {
        AnnotationValues a = cls.getClassAnnotation(CONTROLLER_ADVICE);
        return a != null ? a : cls.getClassAnnotation(REST_CONTROLLER_ADVICE);
    }

    /// The generated adapter of an advice class.
    static String adapterName(String binary) {
        String pkg = RestClientAnnotationProcessor.packageOf(binary);
        String simple = binary.substring(pkg.length() == 0 ? 0 : pkg.length() + 1)
                .replace('$', '_') + "Cn1Advice";
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    /// The handlers of `cls`, deepest declared type first; null after an error.
    static List<Handler> collect(ProcessorContext ctx, AnnotatedClass cls) {
        List<Handler> out = new ArrayList<Handler>();
        Set<String> seen = new HashSet<String>();
        int declared = 0;
        for (MethodInfo m : cls.getMethods()) {
            AnnotationValues a = m.getAnnotation(EXCEPTION_HANDLER);
            if (a == null) {
                continue;
            }
            String where = cls.getBinaryName() + "." + m.getName();
            if (!m.isPublic() || m.isStatic()) {
                ctx.error(cls, "@ExceptionHandler " + where + " must be a public instance "
                        + "method: the generated code calls it from another class");
                return null;
            }
            Type[] params = Type.getArgumentTypes(m.getDescriptor());
            StringBuilder args = new StringBuilder();
            String exceptionParam = null;
            for (int i = 0; i < params.length; i++) {
                String t = params[i].getClassName();
                if (i > 0) {
                    args.append(", ");
                }
                if (REQUEST.equals(t)) {
                    args.append("request");
                } else if (HANDLER_METHOD.equals(t)) {
                    args.append("handler");
                } else if (exceptionParam == null && params[i].getSort() == Type.OBJECT) {
                    exceptionParam = t;
                    args.append("(").append(t.replace('$', '.')).append(") failure");
                } else {
                    ctx.error(cls, "@ExceptionHandler " + where + " takes a " + t + ". A handler "
                            + "takes the exception, the HttpServer.Request and the "
                            + "HandlerMethod, in any order, and nothing else");
                    return null;
                }
            }
            List<String> types = new ArrayList<String>();
            Object value = a.get("value");
            if (value instanceof List) {
                for (Object o : (List<?>) value) {
                    if (o instanceof Type) {
                        types.add(((Type) o).getClassName());
                    }
                }
            }
            if (types.isEmpty()) {
                if (exceptionParam == null) {
                    ctx.error(cls, "@ExceptionHandler " + where + " names no exception and "
                            + "takes none; say which it handles");
                    return null;
                }
                types.add(exceptionParam);
            }
            String status = String.valueOf(statusOf(m));
            String body = invocation(ctx, cls, m, args.toString(), status, where);
            if (body == null) {
                return null;
            }
            for (String type : types) {
                List<String> chain = superChain(ctx, type);
                if (chain == null || !chain.contains("java.lang.Throwable")) {
                    ctx.error(cls, "@ExceptionHandler " + where + " handles " + type
                            + ", which the build cannot resolve as an exception");
                    return null;
                }
                if (exceptionParam != null && !chain.contains(exceptionParam)) {
                    ctx.error(cls, "@ExceptionHandler " + where + " handles " + type
                            + " but its parameter is a " + exceptionParam + ", which it is not");
                    return null;
                }
                if (!seen.add(type)) {
                    ctx.error(cls, "Two @ExceptionHandler methods of " + cls.getBinaryName()
                            + " handle " + type + "; which one answers would be a guess");
                    return null;
                }
                Handler h = new Handler();
                h.exception = type;
                h.depth = chain.size();
                h.declared = declared++;
                h.call = body;
                out.add(h);
            }
        }
        Collections.sort(out, new Comparator<Handler>() {
            @Override
            public int compare(Handler x, Handler y) {
                return x.depth != y.depth ? y.depth - x.depth : x.declared - y.declared;
            }
        });
        return out;
    }

    private static int statusOf(MethodInfo m) {
        AnnotationValues s = m.getAnnotation(RESPONSE_STATUS);
        return s == null ? 200 : s.getIntOrDefault("value", 200);
    }

    /// The statements that call the handler and answer with what it returns.
    private static String invocation(ProcessorContext ctx, AnnotatedClass cls, MethodInfo m,
                                     String args, String status, String where) {
        String ret = Type.getReturnType(m.getDescriptor()).getClassName();
        String call = "impl." + m.getName() + "(" + args + ")";
        if (RESPONSE.equals(ret)) {
            // Null declines, and the next resolver is asked.
            return "return " + call + ";\n";
        }
        if ("void".equals(ret)) {
            return call + ";\nreturn request.respond(" + status + ", \"text/plain\", new byte[0]);\n";
        }
        if ("java.lang.String".equals(ret)) {
            return "String cn1Text = " + call + ";\n"
                    + "return request.respond(" + status + ", \"text/plain; charset=utf-8\",\n"
                    + "        cn1Text == null ? new byte[0] : cn1Text.getBytes(\"UTF-8\"));\n";
        }
        if (JSON_RETURNS.contains(ret)) {
            return "Object cn1Value = " + call + ";\n"
                    + "return cn1Value == null ? request.respond(" + status
                    + ", \"text/plain\", new byte[0])\n"
                    + "        : request.respondJson(" + status + ", cn1Value);\n";
        }
        ctx.error(cls, "@ExceptionHandler " + where + " returns a " + ret + ". Return an "
                + "HttpServer.Response, a String, a Map, a List, a Json.Writable, or nothing");
        return null;
    }

    /// The `instanceof` chain answering for `failure`, ending in `return null`.
    static String dispatch(List<Handler> handlers, String pad) {
        StringBuilder sb = new StringBuilder();
        for (Handler h : handlers) {
            sb.append(pad).append("if (failure instanceof ").append(h.exception.replace('$', '.'))
              .append(") {\n");
            for (String line : h.call.split("\n")) {
                sb.append(pad).append("    ").append(line).append('\n');
            }
            sb.append(pad).append("}\n");
        }
        sb.append(pad).append("return null;\n");
        return sb.toString();
    }

    /// The source of the resolver for an advice class.
    static String adapter(AnnotatedClass cls, List<Handler> handlers, AnnotationValues advice) {
        String binary = cls.getBinaryName();
        String name = adapterName(binary);
        String pkg = RestClientAnnotationProcessor.packageOf(name);
        String simple = name.substring(pkg.length() == 0 ? 0 : pkg.length() + 1);
        String source = cls.getSourceName();
        StringBuilder sb = new StringBuilder();
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated from @ControllerAdvice on ").append(binary).append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(simple)
          .append(" implements com.codename1.backend.web.ExceptionResolver {\n");
        sb.append("    private final ").append(source).append(" impl;\n\n");
        sb.append("    public ").append(simple).append('(').append(source)
          .append(" impl) {\n        this.impl = impl;\n    }\n\n");
        sb.append("    public com.codename1.backend.HttpServer.Response resolve(\n")
          .append("            com.codename1.backend.HttpServer.Request request,\n")
          .append("            com.codename1.backend.web.HandlerMethod handler,\n")
          .append("            Exception failure) throws Exception {\n");
        List<String> packages = new ArrayList<String>();
        Object bp = advice.get("basePackages");
        if (bp instanceof List) {
            for (Object o : (List<?>) bp) {
                if (o instanceof String && ((String) o).length() > 0) {
                    packages.add((String) o);
                }
            }
        }
        if (!packages.isEmpty()) {
            sb.append("        if (");
            for (int i = 0; i < packages.size(); i++) {
                sb.append(i == 0 ? "" : " && ").append("!handler.isInPackage(")
                  .append(RestControllerAnnotationProcessor.quote(packages.get(i))).append(')');
            }
            sb.append(") {\n            return null;\n        }\n");
        }
        sb.append(dispatch(handlers, "        "));
        sb.append("    }\n}\n");
        return sb.toString();
    }

    /// The binary names from `binary` up to `java.lang.Object`, or null when a
    /// link cannot be resolved. JDK classes come from the JDK running the build,
    /// which is not on the processor's class index.
    static List<String> superChain(ProcessorContext ctx, String binary) {
        List<String> out = new ArrayList<String>();
        String current = binary;
        while (current != null) {
            out.add(current);
            if ("java.lang.Object".equals(current)) {
                return out;
            }
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx,
                    current.replace('.', '/'));
            if (c != null) {
                String parent = c.getSuperInternalName();
                current = parent == null ? null : parent.replace('/', '.');
                continue;
            }
            if (!current.startsWith("java.")) {
                return null;
            }
            try {
                Class<?> k = Class.forName(current, false,
                        BackendExceptionHandlers.class.getClassLoader());
                for (Class<?> s = k.getSuperclass(); s != null; s = s.getSuperclass()) {
                    out.add(s.getName());
                }
                return out;
            } catch (ClassNotFoundException err) {
                return null;
            }
        }
        return out;
    }
}
