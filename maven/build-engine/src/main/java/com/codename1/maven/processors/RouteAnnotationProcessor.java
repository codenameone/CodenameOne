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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Bytecode-driven `@Route` processor.
///
/// Scans the project's compiled classes for `@Route` annotations on Form
/// subclasses or static factory methods, validates each declaration fail-fast,
/// then generates `com.codename1.router.generated.Routes` as a Java source
/// file and compiles it on the spot via JSR 199 so the resulting `.class`
/// lands in the project's output directory and shadows the framework stub at
/// runtime.
///
/// The generated `Routes` class implements `com.codename1.router.RouteDispatcher`,
/// registers itself with `Display` from its static `bootstrap()` method, and
/// dispatches incoming URLs by matching against the recognised patterns,
/// extracting path variables, and invoking the matching constructor / factory.
///
/// Validation surfaces every offending class in a single build run via
/// `ProcessorContext#error`. No bytecode is written when any error is pending.
public final class RouteAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String ROUTE_DESC = "Lcom/codename1/annotations/Route;";
    public static final String ROUTES_DESC = "Lcom/codename1/annotations/Route$Routes;";
    public static final String ROUTE_PARAM_DESC = "Lcom/codename1/annotations/RouteParam;";

    static final String FORM_INTERNAL = "com/codename1/ui/Form";
    static final String FORM_BINARY = "com.codename1.ui.Form";
    static final String STRING_BINARY = "java.lang.String";

    /// Internal name of the generated class. Application code never references
    /// it directly; the framework loads it via `Display.init()`.
    static final String ROUTES_INTERNAL = "com/codename1/router/generated/Routes";
    static final String ROUTES_PACKAGE = "com.codename1.router.generated";
    static final String ROUTES_SIMPLE = "Routes";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(ROUTE_DESC);
        s.add(ROUTES_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    /// Accepted routes keyed by path pattern. TreeMap so the emitted source is
    /// deterministic regardless of class-scan order.
    private final TreeMap<String, Entry> accepted = new TreeMap<String, Entry>();

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
        if (cls.isSynthetic()) {
            return;
        }
        // Two paths: class-level @Route (constructor target) and method-level
        // @Route (static-factory target). A single class can hold both kinds.
        if (cls.getClassAnnotation(ROUTE_DESC) != null
                || cls.getClassAnnotation(ROUTES_DESC) != null) {
            processClassLevel(cls, ctx);
        }
        for (MethodInfo m : cls.getMethods()) {
            if (m.getAnnotation(ROUTE_DESC) != null || m.getAnnotation(ROUTES_DESC) != null) {
                processMethodLevel(cls, m, ctx);
            }
        }
    }

    private void processClassLevel(AnnotatedClass cls, ProcessorContext ctx) {
        if (cls.isAbstract() || cls.isInterface()) {
            ctx.error(cls, "@Route on a class requires a concrete Form subclass; "
                    + cls.getBinaryName() + " is abstract or an interface");
            return;
        }
        if (!extendsForm(cls, ctx)) {
            ctx.error(cls, "@Route classes must extend com.codename1.ui.Form (transitively); "
                    + cls.getBinaryName() + " extends " + dot(cls.getSuperInternalName()));
            return;
        }
        List<AnnotationValues> annotations = collectAnnotations(
                cls.getClassAnnotation(ROUTE_DESC),
                cls.getClassAnnotation(ROUTES_DESC));

        // Pick a constructor: prefer one whose parameters cover every path
        // variable in the pattern via @RouteParam.
        for (AnnotationValues av : annotations) {
            String pattern = patternOf(av, cls, ctx);
            if (pattern == null) {
                continue;
            }
            List<String> required = pathVarsOf(pattern);
            ConstructorBinding binding = pickConstructor(cls, required, ctx);
            if (binding == null) {
                return;
            }
            register(pattern, Entry.forClass(pattern, cls.getBinaryName(), binding), cls, ctx);
        }
    }

    private void processMethodLevel(AnnotatedClass cls, MethodInfo method, ProcessorContext ctx) {
        if (!method.isStatic() || !method.isPublic()) {
            ctx.error(cls, "@Route methods must be public static; "
                    + cls.getBinaryName() + "#" + method.getName() + " is not");
            return;
        }
        if (!returnsForm(method)) {
            ctx.error(cls, "@Route methods must return a Form (or a Form subtype); "
                    + cls.getBinaryName() + "#" + method.getName() + " returns "
                    + returnTypeBinary(method.getDescriptor()));
            return;
        }
        List<AnnotationValues> annotations = collectAnnotations(
                method.getAnnotation(ROUTE_DESC), method.getAnnotation(ROUTES_DESC));
        for (AnnotationValues av : annotations) {
            String pattern = patternOf(av, cls, ctx);
            if (pattern == null) {
                continue;
            }
            List<String> required = pathVarsOf(pattern);
            MethodBinding binding = bindMethod(cls, method, required, ctx);
            if (binding == null) {
                return;
            }
            register(pattern, Entry.forMethod(pattern, cls.getBinaryName(), method.getName(), binding),
                    cls, ctx);
        }
    }

    private void register(String pattern, Entry entry, AnnotatedClass cls, ProcessorContext ctx) {
        Entry prev = accepted.get(pattern);
        if (prev != null) {
            ctx.error(cls, "duplicate @Route pattern \"" + pattern + "\": already declared on "
                    + prev.targetDescription());
            return;
        }
        accepted.put(pattern, entry);
    }

    // ------------------------------------------------------------------------
    // Output
    // ------------------------------------------------------------------------

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) {
            return;
        }
        if (accepted.isEmpty()) {
            // No project-declared routes: do not emit Routes at all. Display
            // init() looks the class up reflectively and silently no-ops when
            // it's absent.
            return;
        }
        String source = generateRoutesSource(new ArrayList<Entry>(accepted.values()));
        File outDir = ctx.getOutputClassDir();
        // Find the classpath for compilation (framework jar provides Display +
        // RouteDispatcher; the project's own classes provide @Route targets).
        List<File> classpath = buildCompileClasspath(outDir);
        try {
            Map<String, String> srcs = JavaSourceCompiler.singleSource(
                    ROUTES_PACKAGE + "." + ROUTES_SIMPLE, source);
            JavaSourceCompiler.compile(srcs, outDir, classpath);
        } catch (IOException e) {
            throw new ProcessingException(
                    "Failed to compile generated " + ROUTES_PACKAGE + "." + ROUTES_SIMPLE
                            + ": " + e.getMessage(), e);
        }
        ctx.getLog().info("cn1: generated " + ROUTES_PACKAGE + "." + ROUTES_SIMPLE
                + " with " + accepted.size() + " route(s)");
    }

    private List<File> buildCompileClasspath(File outDir) {
        List<File> cp = new ArrayList<File>();
        // The project's own compiled classes — needed for @Route target types.
        cp.add(outDir);
        // Inherit whatever javac defaults to; the surrounding plugin invocation
        // already supplies the project's compile classpath via java.class.path,
        // which JavaSourceCompiler picks up.
        return cp;
    }

    // ------------------------------------------------------------------------
    // Source generation
    // ------------------------------------------------------------------------

    private static String generateRoutesSource(List<Entry> routes) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by the Codename One Maven plugin from @Route annotations.\n");
        sb.append("// Do not edit -- regenerated on every build.\n");
        sb.append("package ").append(ROUTES_PACKAGE).append(";\n\n");
        sb.append("import com.codename1.router.Navigation;\n");
        sb.append("import com.codename1.router.RouteDispatcher;\n");
        sb.append("import com.codename1.ui.Form;\n\n");
        sb.append("public final class ").append(ROUTES_SIMPLE)
                .append(" implements RouteDispatcher {\n\n");
        // Self-registering constructor: the application stub the builders
        // generate calls `new Routes()` directly before Display.init(), so
        // we install the dispatcher here. Direct symbol reference -- not
        // Class.forName -- so obfuscation rewrites the call site and the
        // class together and the binding survives in shipped builds.
        sb.append("    public Routes() {\n");
        sb.append("        Navigation.setDispatcher(this);\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    public Form dispatch(String url) {\n");
        sb.append("        if (url == null || url.length() == 0) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        String path = extractPath(url);\n");
        sb.append("        String[] segs = splitPath(path);\n");
        sb.append("        java.util.Map<String,String> q = null;\n");
        // Emit branches most-specific first so a literal route wins over a
        // catch-all that also matches.
        List<Entry> ordered = new ArrayList<Entry>(routes);
        Collections.sort(ordered, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                int diff = specificity(b.pattern) - specificity(a.pattern);
                if (diff != 0) {
                    return diff;
                }
                return a.pattern.compareTo(b.pattern);
            }
        });
        for (Entry e : ordered) {
            emitRouteBranch(sb, e);
        }
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        emitHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitRouteBranch(StringBuilder sb, Entry e) {
        String[] segs = patternSegments(e.pattern);
        boolean catchAll = segs.length > 0 && "**".equals(segs[segs.length - 1]);
        sb.append("        // ").append(e.pattern).append(" -> ").append(e.targetDescription()).append('\n');
        // Length check
        if (catchAll) {
            sb.append("        if (segs.length >= ").append(segs.length - 1).append(") {\n");
        } else {
            sb.append("        if (segs.length == ").append(segs.length).append(") {\n");
        }
        // Per-segment matching
        sb.append("            boolean match = true;\n");
        for (int i = 0; i < segs.length; i++) {
            String s = segs[i];
            if (s.startsWith(":") || "*".equals(s) || "**".equals(s)) {
                continue;
            }
            sb.append("            match = match && \"").append(escape(s)).append("\".equals(segs[")
                    .append(i).append("]);\n");
        }
        sb.append("            if (match) {\n");
        // Bind path vars
        Map<String, String> varToExpr = new LinkedHashMap<String, String>();
        for (int i = 0; i < segs.length; i++) {
            String s = segs[i];
            if (s.startsWith(":")) {
                varToExpr.put(s.substring(1), "segs[" + i + "]");
            } else if ("*".equals(s)) {
                varToExpr.put("*", "segs[" + i + "]");
            } else if ("**".equals(s)) {
                varToExpr.put("*", "joinFrom(segs, " + i + ")");
            }
        }
        // Pull query map for non-path bindings (lazy: only parse when matched).
        sb.append("                if (q == null) { q = parseQuery(url); }\n");
        // Build constructor / static factory call and return -- first match wins.
        sb.append("                return ").append(e.buildExpression(varToExpr)).append(";\n");
        sb.append("            }\n");
        sb.append("        }\n");
    }

    /// Specificity score used to order route branches in the generated
    /// dispatch method: literal segments outscore named params, params
    /// outscore catch-all wildcards. Mirrors the established ant-pattern
    /// scoring so the most specific route wins when several patterns match
    /// the same URL.
    private static int specificity(String pattern) {
        int score = 0;
        for (String s : patternSegments(pattern)) {
            if ("**".equals(s)) {
                score -= 100;
            } else if ("*".equals(s) || (s.length() > 0 && s.charAt(0) == ':')) {
                score += 1;
            } else {
                score += 10;
            }
        }
        return score;
    }

    private static void emitHelpers(StringBuilder sb) {
        sb.append("    private static String extractPath(String url) {\n");
        sb.append("        int h = url.indexOf('#');\n");
        sb.append("        if (h >= 0) { url = url.substring(0, h); }\n");
        sb.append("        int q = url.indexOf('?');\n");
        sb.append("        if (q >= 0) { url = url.substring(0, q); }\n");
        sb.append("        int s = url.indexOf(\"://\");\n");
        sb.append("        if (s >= 0) {\n");
        sb.append("            int slash = url.indexOf('/', s + 3);\n");
        sb.append("            return slash < 0 ? \"/\" : url.substring(slash);\n");
        sb.append("        }\n");
        sb.append("        int colon = url.indexOf(':');\n");
        sb.append("        if (colon > 0) {\n");
        sb.append("            String tail = url.substring(colon + 1);\n");
        sb.append("            return tail.length() == 0 ? \"/\"\n");
        sb.append("                    : (tail.charAt(0) == '/' ? tail : \"/\" + tail);\n");
        sb.append("        }\n");
        sb.append("        return url.length() == 0 || url.charAt(0) != '/' ? \"/\" + url : url;\n");
        sb.append("    }\n\n");
        sb.append("    private static String[] splitPath(String path) {\n");
        sb.append("        if (path == null || path.length() == 0 || \"/\".equals(path)) {\n");
        sb.append("            return new String[0];\n");
        sb.append("        }\n");
        sb.append("        String p = path.charAt(0) == '/' ? path.substring(1) : path;\n");
        sb.append("        if (p.length() > 0 && p.charAt(p.length() - 1) == '/') {\n");
        sb.append("            p = p.substring(0, p.length() - 1);\n");
        sb.append("        }\n");
        sb.append("        if (p.length() == 0) { return new String[0]; }\n");
        sb.append("        java.util.ArrayList<String> out = new java.util.ArrayList<String>();\n");
        sb.append("        int start = 0;\n");
        sb.append("        for (int i = 0; i < p.length(); i++) {\n");
        sb.append("            if (p.charAt(i) == '/') {\n");
        sb.append("                out.add(decode(p.substring(start, i)));\n");
        sb.append("                start = i + 1;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        out.add(decode(p.substring(start)));\n");
        sb.append("        return out.toArray(new String[out.size()]);\n");
        sb.append("    }\n\n");
        sb.append("    private static String joinFrom(String[] segs, int from) {\n");
        sb.append("        if (from >= segs.length) { return \"\"; }\n");
        sb.append("        StringBuilder sb = new StringBuilder();\n");
        sb.append("        for (int i = from; i < segs.length; i++) {\n");
        sb.append("            if (i > from) { sb.append('/'); }\n");
        sb.append("            sb.append(segs[i]);\n");
        sb.append("        }\n");
        sb.append("        return sb.toString();\n");
        sb.append("    }\n\n");
        sb.append("    private static java.util.Map<String,String> parseQuery(String url) {\n");
        sb.append("        java.util.LinkedHashMap<String,String> out = new java.util.LinkedHashMap<String,String>();\n");
        sb.append("        int q = url.indexOf('?');\n");
        sb.append("        if (q < 0) { return out; }\n");
        sb.append("        int hash = url.indexOf('#', q);\n");
        sb.append("        String query = hash < 0 ? url.substring(q + 1) : url.substring(q + 1, hash);\n");
        sb.append("        int start = 0;\n");
        sb.append("        for (int i = 0; i <= query.length(); i++) {\n");
        sb.append("            if (i == query.length() || query.charAt(i) == '&') {\n");
        sb.append("                if (i > start) {\n");
        sb.append("                    String pair = query.substring(start, i);\n");
        sb.append("                    int eq = pair.indexOf('=');\n");
        sb.append("                    if (eq < 0) {\n");
        sb.append("                        out.put(decode(pair), \"\");\n");
        sb.append("                    } else {\n");
        sb.append("                        out.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("                start = i + 1;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    private static String decode(String s) {\n");
        sb.append("        try { return com.codename1.io.Util.decode(s, \"UTF-8\", false); }\n");
        sb.append("        catch (Throwable t) { return s; }\n");
        sb.append("    }\n\n");
        sb.append("    private static String throwMissing(String name) {\n");
        sb.append("        throw new IllegalArgumentException(\n");
        sb.append("                \"deep link is missing required @RouteParam \\\"\" + name + \"\\\"\");\n");
        sb.append("    }\n");
    }

    private static String[] patternSegments(String pattern) {
        if (pattern == null || pattern.length() == 0 || "/".equals(pattern)) {
            return new String[0];
        }
        String p = pattern.charAt(0) == '/' ? pattern.substring(1) : pattern;
        if (p.length() > 0 && p.charAt(p.length() - 1) == '/') {
            p = p.substring(0, p.length() - 1);
        }
        return p.length() == 0 ? new String[0] : p.split("/");
    }

    // ------------------------------------------------------------------------
    // Validation helpers
    // ------------------------------------------------------------------------

    private static List<AnnotationValues> collectAnnotations(AnnotationValues single, AnnotationValues container) {
        List<AnnotationValues> out = new ArrayList<AnnotationValues>();
        if (single != null) {
            out.add(single);
        }
        if (container != null) {
            Object value = container.get("value");
            if (value instanceof List<?>) {
                for (Object item : (List<?>) value) {
                    if (item instanceof AnnotationValues) {
                        out.add((AnnotationValues) item);
                    }
                }
            }
        }
        return out;
    }

    private static String patternOf(AnnotationValues av, AnnotatedClass cls, ProcessorContext ctx) {
        String pattern = av.getString("value");
        if (pattern == null || pattern.length() == 0) {
            ctx.error(cls, "@Route value is required and must be a non-empty path");
            return null;
        }
        if (pattern.charAt(0) != '/') {
            ctx.error(cls, "@Route value must start with '/'; got: \"" + pattern + "\"");
            return null;
        }
        return pattern;
    }

    private static List<String> pathVarsOf(String pattern) {
        List<String> out = new ArrayList<String>();
        for (String s : patternSegments(pattern)) {
            if (s.startsWith(":")) {
                out.add(s.substring(1));
            } else if ("*".equals(s) || "**".equals(s)) {
                out.add("*");
            }
        }
        return out;
    }

    private static boolean extendsForm(AnnotatedClass cls, ProcessorContext ctx) {
        if (cls == null) {
            return false;
        }
        if (FORM_INTERNAL.equals(cls.getInternalName())) {
            return true;
        }
        String parent = cls.getSuperInternalName();
        while (parent != null) {
            if (FORM_INTERNAL.equals(parent)) {
                return true;
            }
            AnnotatedClass parentCls = ctx.lookup(parent);
            if (parentCls == null) {
                // Left the project (cn1-core, JDK, etc.). Be permissive for
                // anything in com/codename1/ui that ends in Form/Dialog.
                return parent.startsWith("com/codename1/ui/")
                        && (parent.endsWith("Form") || parent.endsWith("Dialog"));
            }
            parent = parentCls.getSuperInternalName();
        }
        return false;
    }

    private static boolean returnsForm(MethodInfo method) {
        String desc = method.getDescriptor();
        int close = desc.lastIndexOf(')');
        if (close < 0 || close + 1 >= desc.length()) {
            return false;
        }
        String ret = desc.substring(close + 1);
        if (!ret.startsWith("L") || !ret.endsWith(";")) {
            return false;
        }
        String internal = ret.substring(1, ret.length() - 1);
        // Permissive: any class whose internal name ends with Form is accepted.
        // The compiled call site verifies type-correctness at javac time.
        return internal.equals(FORM_INTERNAL) || internal.endsWith("Form")
                || internal.endsWith("Dialog");
    }

    private static String returnTypeBinary(String desc) {
        int close = desc.lastIndexOf(')');
        if (close < 0 || close + 1 >= desc.length()) {
            return "?";
        }
        String ret = desc.substring(close + 1);
        if (ret.startsWith("L") && ret.endsWith(";")) {
            return ret.substring(1, ret.length() - 1).replace('/', '.');
        }
        return ret;
    }

    // ------------------------------------------------------------------------
    // Parameter binding: read @RouteParam from constructor / method parameters
    // ------------------------------------------------------------------------

    private ConstructorBinding pickConstructor(AnnotatedClass cls, List<String> requiredPathVars,
                                                ProcessorContext ctx) {
        ConstructorBinding best = null;
        int bestScore = -1;
        for (MethodInfo m : cls.getMethods()) {
            if (!m.isConstructor() || !m.isPublic()) {
                continue;
            }
            ParamBinding[] params = parameterBindings(cls, m, ctx);
            if (params == null) {
                continue;
            }
            // Score: covers all required path vars + parameter count proximity.
            if (!covers(params, requiredPathVars)) {
                continue;
            }
            int score = params.length * 10 + coverageScore(params, requiredPathVars);
            if (score > bestScore) {
                bestScore = score;
                best = new ConstructorBinding(m.getDescriptor(), params);
            }
        }
        if (best == null) {
            ctx.error(cls, "@Route class " + cls.getBinaryName()
                    + " has no public constructor that binds every path variable via @RouteParam");
        }
        return best;
    }

    private MethodBinding bindMethod(AnnotatedClass cls, MethodInfo method,
                                      List<String> requiredPathVars, ProcessorContext ctx) {
        ParamBinding[] params = parameterBindings(cls, method, ctx);
        if (params == null) {
            return null;
        }
        if (!covers(params, requiredPathVars)) {
            ctx.error(cls, "@Route method " + cls.getBinaryName() + "#" + method.getName()
                    + " does not declare a @RouteParam for every path variable in its pattern");
            return null;
        }
        return new MethodBinding(method.getName(), method.getDescriptor(), params);
    }

    /// Reads the byte-code parameter annotations for `method`, mapping each
    /// parameter to its `@RouteParam` value when present. Returns null if any
    /// parameter is missing the annotation (so the caller knows to error).
    private ParamBinding[] parameterBindings(AnnotatedClass owningClass, MethodInfo method,
                                              ProcessorContext ctx) {
        String desc = method.getDescriptor();
        List<String> paramTypes = paramTypesOf(desc);
        // Re-parse the class file to extract per-parameter annotations -- we
        // don't keep them in the lightweight MethodInfo.
        Map<Integer, ParamMeta> meta;
        try {
            meta = readParameterAnnotations(owningClass, method);
        } catch (IOException e) {
            ctx.error(owningClass, "could not read parameter annotations: " + e.getMessage());
            return null;
        }
        boolean anyMissing = false;
        ParamBinding[] out = new ParamBinding[paramTypes.size()];
        for (int i = 0; i < out.length; i++) {
            ParamMeta m = meta.get(i);
            if (m == null) {
                ctx.error(owningClass, "@Route target " + owningClass.getBinaryName() + "#"
                        + method.getName() + " parameter #" + i
                        + " has no @RouteParam binding; every parameter must be annotated");
                anyMissing = true;
                continue;
            }
            if (!STRING_BINARY.equals(paramTypes.get(i))) {
                ctx.error(owningClass, "@RouteParam(\"" + m.name + "\") on "
                        + owningClass.getBinaryName() + "#" + method.getName()
                        + " parameter #" + i + " must be of type java.lang.String (was "
                        + paramTypes.get(i) + ")");
                anyMissing = true;
                continue;
            }
            out[i] = new ParamBinding(m.name, m.required);
        }
        return anyMissing ? null : out;
    }

    private static List<String> paramTypesOf(String desc) {
        List<String> out = new ArrayList<String>();
        int i = desc.indexOf('(') + 1;
        int end = desc.indexOf(')');
        while (i < end) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                out.add(desc.substring(i + 1, semi).replace('/', '.'));
                i = semi + 1;
            } else if (c == '[') {
                int j = i;
                while (desc.charAt(j) == '[') {
                    j++;
                }
                if (desc.charAt(j) == 'L') {
                    j = desc.indexOf(';', j);
                }
                out.add(desc.substring(i, j + 1));
                i = j + 1;
            } else {
                out.add(String.valueOf(c));
                i++;
            }
        }
        return out;
    }

    private Map<Integer, ParamMeta> readParameterAnnotations(AnnotatedClass cls, MethodInfo method)
            throws IOException {
        final Map<Integer, ParamMeta> out = new HashMap<Integer, ParamMeta>();
        File file = cls.getClassFile();
        if (file == null) {
            return out;
        }
        InputStream in = Files.newInputStream(file.toPath());
        try {
            ClassReader reader = new ClassReader(in);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                  String signature, String[] exceptions) {
                    if (!name.equals(method.getName()) || !descriptor.equals(method.getDescriptor())) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(
                                final int parameter, String desc, boolean visible) {
                            if (!ROUTE_PARAM_DESC.equals(desc)) {
                                return null;
                            }
                            final ParamMeta meta = new ParamMeta();
                            out.put(parameter, meta);
                            return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String n, Object v) {
                                    if ("value".equals(n) && v instanceof String) {
                                        meta.name = (String) v;
                                    } else if ("required".equals(n) && v instanceof Boolean) {
                                        meta.required = (Boolean) v;
                                    }
                                }
                            };
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } finally {
            in.close();
        }
        return out;
    }

    private static boolean covers(ParamBinding[] params, List<String> requiredPathVars) {
        Set<String> bound = new LinkedHashSet<String>();
        for (ParamBinding p : params) {
            bound.add(p.name);
        }
        for (String v : requiredPathVars) {
            if (!bound.contains(v)) {
                return false;
            }
        }
        return true;
    }

    private static int coverageScore(ParamBinding[] params, List<String> requiredPathVars) {
        int score = 0;
        for (ParamBinding p : params) {
            if (requiredPathVars.contains(p.name)) {
                score++;
            }
        }
        return score;
    }

    private static String dot(String internalName) {
        return internalName == null ? "null" : internalName.replace('/', '.');
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ------------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------------

    private static final class ParamMeta {
        String name;
        boolean required = true;
    }

    static final class ParamBinding {
        final String name;
        final boolean required;
        ParamBinding(String name, boolean required) {
            this.name = name;
            this.required = required;
        }
        String paramExpression(Map<String, String> pathExpressions) {
            String fromPath = pathExpressions.get(name);
            if (fromPath != null) {
                return fromPath;
            }
            // Fall back to query string.
            String def = required
                    ? "throwMissing(\"" + name + "\")"
                    : "null";
            return "q.containsKey(\"" + name + "\") ? q.get(\"" + name + "\") : " + def;
        }
    }

    static final class ConstructorBinding {
        final String descriptor;
        final ParamBinding[] params;
        ConstructorBinding(String descriptor, ParamBinding[] params) {
            this.descriptor = descriptor;
            this.params = params;
        }
    }

    static final class MethodBinding {
        final String name;
        final String descriptor;
        final ParamBinding[] params;
        MethodBinding(String name, String descriptor, ParamBinding[] params) {
            this.name = name;
            this.descriptor = descriptor;
            this.params = params;
        }
    }

    static final class Entry {
        final String pattern;
        final String targetClassBinary;
        final String methodName; // null for class-level
        final Object binding;

        private Entry(String pattern, String targetClassBinary, String methodName, Object binding) {
            this.pattern = pattern;
            this.targetClassBinary = targetClassBinary;
            this.methodName = methodName;
            this.binding = binding;
        }

        static Entry forClass(String pattern, String targetClassBinary, ConstructorBinding binding) {
            return new Entry(pattern, targetClassBinary, null, binding);
        }

        static Entry forMethod(String pattern, String targetClassBinary, String methodName,
                                MethodBinding binding) {
            return new Entry(pattern, targetClassBinary, methodName, binding);
        }

        String targetDescription() {
            return methodName == null ? targetClassBinary
                    : targetClassBinary + "#" + methodName;
        }

        String buildExpression(Map<String, String> pathExpressions) {
            ParamBinding[] params;
            if (binding instanceof ConstructorBinding) {
                params = ((ConstructorBinding) binding).params;
            } else {
                params = ((MethodBinding) binding).params;
            }
            StringBuilder args = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    args.append(", ");
                }
                args.append(params[i].paramExpression(pathExpressions));
            }
            if (methodName == null) {
                return "new " + targetClassBinary + "(" + args + ")";
            }
            return targetClassBinary + "." + methodName + "(" + args + ")";
        }
    }
}
