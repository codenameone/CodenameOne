/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler.api;

import com.codename1.python.transpiler.analyze.ScopeBuilder;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Scope;
import com.codename1.python.transpiler.codegen.Closed;
import com.codename1.python.transpiler.codegen.Emitter;
import com.codename1.python.transpiler.parser.Parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Transpiles a set of Python modules to Java sources.
///
/// Input is keyed by the path of each `.py` file relative to its source root
/// (`pkg/mod.py`, `pkg/__init__.py`), which is what fixes its module name.
/// Output is keyed by the path of each generated `.java` file relative to the
/// generated-sources root: one class per module, and a `PythonModules`
/// registry whose `registerAll()` makes every module importable and whose
/// `runMain()` runs the entry module as `__main__`.
public final class PythonTranspiler {
    /// The package every generated class is placed in.
    public static final String PACKAGE = "com.codename1.generated.python";

    /// Where the standard library modules written in Python live, as
    /// resources of this jar. A program gets the ones it imports, and only
    /// those; a module of its own with the same name wins.
    private static final String STDLIB = "/com/codename1/python/stdlib/";

    /// Where the popular third-party packages live (`requests`, `pydantic`,
    /// `attrs`, `yaml`, `dateutil`, `numpy`...): the package's public API
    /// written for this runtime, found the same way as the standard library
    /// and after it. The real packages build methods with `exec` or call C,
    /// neither of which a translated program can do.
    private static final String SITE = "/com/codename1/python/site/";

    private PythonTranspiler() {
    }

    /// Whether the runtime provides the top-level module `name` itself -- a
    /// standard library module, or a package Codename One maps (`requests`,
    /// `numpy`, `flask`) -- so a requirement on it needs nothing downloaded.
    public static boolean provides(String name) {
        for (String root : new String[] {STDLIB, SITE}) {
            if (PythonTranspiler.class.getResource(root + name + ".py") != null
                    || PythonTranspiler.class.getResource(root + name + "/__init__.py") != null) {
                return true;
            }
        }
        return false;
    }

    /// @param sources relative `.py` path to source text
    /// @param mainModule the module run as `__main__`, or null for a library
    public static Map<String, String> transpile(Map<String, String> sources, String mainModule) {
        return transpile(sources, mainModule, java.util.Collections.<java.io.File>emptyList());
    }

    /// @param sources relative `.py` path to source text
    /// @param mainModule the module run as `__main__`, or null for a library
    /// @param classPath the jars and class directories a program's
    ///        `from java.<package> import <Class>` resolves against. When it
    ///        carries a class library (an application's `java-runtime`),
    ///        `java.*` resolves there alone, so an API the device lacks is a
    ///        build error; otherwise `java.*` comes from the running JDK
    public static Map<String, String> transpile(Map<String, String> sources, String mainModule,
            List<java.io.File> classPath) {
        return transpile(sources, mainModule, classPath, java.util.Collections.<String, String>emptyMap());
    }

    /// @param sources relative `.py` path to source text
    /// @param mainModule the module run as `__main__`, or null for a library
    /// @param classPath as for [#transpile(Map, String, List)]
    /// @param nativeModules modules written in Java in place of a C
    ///        extension, by dotted module name: the fully qualified class
    ///        whose static `install()` registers it (a
    ///        `com.codename1.python.runtime.NativeModule`). The registry
    ///        installs each before any module runs
    public static Map<String, String> transpile(Map<String, String> sources, String mainModule,
            List<java.io.File> classPath, Map<String, String> nativeModules) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        Map<String, String> classes = new LinkedHashMap<String, String>();
        Map<String, Node.Module> trees = new LinkedHashMap<String, Node.Module>();
        List<List<Node.Stmt>> bodies = new ArrayList<List<Node.Stmt>>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String path = e.getKey().replace('\\', '/');
            if (path.endsWith(".py")) {
                Node.Module tree = Parser.parse(e.getValue(), path);
                trees.put(path, tree);
                bodies.add(tree.body);
            }
        }
        boolean closed = Closed.check(bodies);
        String interop = null;
        if (com.codename1.python.transpiler.codegen.JavaInteropGenerator.usesJava(trees.values())) {
            try (com.codename1.python.transpiler.codegen.ClassPathReader cp =
                    new com.codename1.python.transpiler.codegen.ClassPathReader(classPath)) {
                interop = com.codename1.python.transpiler.codegen.JavaInteropGenerator.generate(trees, cp, PACKAGE);
            } catch (IOException e) {
                throw new IllegalStateException("cannot read the class path: " + e.getMessage(), e);
            }
        }
        Set<String> stdlib = addStdlib(sources, trees);
        boolean server = false;
        for (String text : sources.values()) {
            if (text.contains("_cn1server")) {
                server = true;
                break;
            }
        }
        if (server) {
            // A web framework is bundled: serve it over the backend's HTTP
            // server when the build has one (a backend build). Elsewhere
            // serving raises, and the frameworks' test clients still work.
            try (com.codename1.python.transpiler.codegen.ClassPathReader cp =
                    new com.codename1.python.transpiler.codegen.ClassPathReader(classPath)) {
                server = cp.find("com/codename1/backend/HttpServer") != null;
            } catch (IOException e) {
                throw new IllegalStateException("cannot read the class path: " + e.getMessage(), e);
            }
        }
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String path = e.getKey().replace('\\', '/');
            if (!path.endsWith(".py")) {
                continue;
            }
            String stem = path.substring(0, path.length() - 3);
            boolean isPackage = "__init__".equals(stem) || stem.endsWith("/__init__");
            if (isPackage) {
                stem = "__init__".equals(stem) ? "" : stem.substring(0, stem.length() - "/__init__".length());
            }
            if (stem.isEmpty()) {
                continue;
            }
            String module = stem.replace('/', '.');
            String cls = className(module);
            Node.Module tree = trees.get(path);
            Scope scope = ScopeBuilder.build(tree, path);
            // The standard library is written to be correct without the
            // whole-program types, and is left out of the closed-world check.
            String java = new Emitter(path, module, isPackage, PACKAGE, cls, scope)
                    .closed(closed && !stdlib.contains(path)).emit(tree);
            java = withLineMap(java, path.replace('\\', '/'));
            out.put(PACKAGE.replace('.', '/') + "/" + cls + ".java", java);
            classes.put(module, cls);
        }
        if (mainModule != null && !classes.containsKey(mainModule)) {
            throw new IllegalArgumentException("main module '" + mainModule + "' is not among the sources");
        }
        if (interop != null) {
            out.put(PACKAGE.replace('.', '/') + "/JavaInterop.java", interop);
        }
        if (server) {
            out.put(PACKAGE.replace('.', '/') + "/BackendServer.java", backendServer());
        }
        out.put(PACKAGE.replace('.', '/') + "/PythonModules.java", registry(classes, mainModule, interop != null, server,
                nativeModules));
        return out;
    }

    /// Adds to `sources` (and `trees`) every standard library module the
    /// program imports, directly or through another such module, and
    /// returns their paths.
    private static Set<String> addStdlib(Map<String, String> sources, Map<String, Node.Module> trees) {
        Set<String> modules = new HashSet<String>();
        for (String path : sources.keySet()) {
            modules.add(moduleOf(path.replace('\\', '/')));
        }
        Set<String> added = new HashSet<String>();
        Deque<Node.Module> work = new ArrayDeque<Node.Module>(trees.values());
        Map<String, String> found = new LinkedHashMap<String, String>();
        Set<Node.Module> opens = new HashSet<Node.Module>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Node.Module t = trees.get(e.getKey().replace('\\', '/'));
            if (t != null && mentionsOpen(e.getValue())) {
                opens.add(t);
            }
        }
        while (!work.isEmpty()) {
            Node.Module tree = work.pop();
            List<String> imported = new ArrayList<String>();
            String own = "";
            for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
                if (e.getValue() == tree) { // NOPMD CompareObjectsWithEquals - identity is meant
                    own = e.getKey();
                    break;
                }
            }
            imports(tree.body, imported, moduleOf(own), own.endsWith("__init__.py"));
            if (opens.contains(tree)) {
                // The builtin open() is io.open, written in Python.
                imported.add("io");
            }
            for (String name : imported) {
                String prefix = null;
                for (String part : name.split("\\.")) {
                    prefix = prefix == null ? part : prefix + "." + part;
                    if (modules.contains(prefix)) {
                        continue;
                    }
                    String base = prefix.replace('.', '/');
                    String path = base + ".py";
                    String text = resource(path);
                    if (text == null) {
                        path = base + "/__init__.py";
                        text = resource(path);
                    }
                    if (text == null) {
                        // A module the runtime provides in Java, or none.
                        break;
                    }
                    modules.add(prefix);
                    added.add(path);
                    Node.Module t = Parser.parse(text, path);
                    if (mentionsOpen(text)) {
                        opens.add(t);
                    }
                    trees.put(path, t);
                    found.put(path, text);
                    work.push(t);
                }
            }
        }
        sources.putAll(found);
        return added;
    }

    /// Whether `text` may call the builtin `open` (a cheap textual test:
    /// bundling `io` for a false positive costs only its size).
    private static boolean mentionsOpen(String text) {
        if (text == null) {
            return false;
        }
        int i = text.indexOf("open");
        while (i >= 0) {
            char before = i == 0 ? ' ' : text.charAt(i - 1);
            int after = i + 4;
            char next = after < text.length() ? text.charAt(after) : ' ';
            if (before != '.' && !Character.isJavaIdentifierPart(before) && !Character.isJavaIdentifierPart(next)) {
                return true;
            }
            i = text.indexOf("open", i + 4);
        }
        return false;
    }

    private static String moduleOf(String path) {
        String stem = path.endsWith(".py") ? path.substring(0, path.length() - 3) : path;
        if (stem.endsWith("/__init__")) {
            stem = stem.substring(0, stem.length() - "/__init__".length());
        }
        return stem.replace('/', '.');
    }

    /// Every module name `body` imports, at any depth, with relative imports
    /// resolved against `self` (a package when `pkg`).
    private static void imports(List<Node.Stmt> body, List<String> out, String self, boolean pkg) {
        for (Node.Stmt s : body) {
            if (s instanceof Node.Import) {
                for (Node.Alias a : ((Node.Import) s).names) {
                    out.add(a.name);
                }
            } else if (s instanceof Node.ImportFrom) {
                Node.ImportFrom f = (Node.ImportFrom) s;
                String from = f.level == 0 ? f.module : relative(self, pkg, f.level, f.module);
                if (from != null && !from.isEmpty()) {
                    out.add(from);
                    for (Node.Alias a : f.names) {
                        // `from pkg import sub` may name a submodule.
                        if (!"*".equals(a.name)) {
                            out.add(from + "." + a.name);
                        }
                    }
                }
            } else if (s instanceof Node.FunctionDef) {
                imports(((Node.FunctionDef) s).body, out, self, pkg);
            } else if (s instanceof Node.ClassDef) {
                imports(((Node.ClassDef) s).body, out, self, pkg);
            } else {
                for (List<Node.Stmt> b : bodiesOf(s)) {
                    imports(b, out, self, pkg);
                }
            }
        }
    }

    /// The absolute name of `from <dots><module> import`, or null when it
    /// climbs out of the top-level package (the emitter reports that).
    private static String relative(String self, boolean pkg, int level, String module) {
        String base = self;
        int up = pkg ? level - 1 : level;
        for (int i = 0; i < up; i++) {
            int dot = base.lastIndexOf('.');
            if (dot < 0) {
                return null;
            }
            base = base.substring(0, dot);
        }
        if (module == null) {
            return base;
        }
        return base.isEmpty() ? module : base + "." + module;
    }

    private static List<List<Node.Stmt>> bodiesOf(Node.Stmt s) {
        List<List<Node.Stmt>> r = new ArrayList<List<Node.Stmt>>();
        if (s instanceof Node.If) {
            r.add(((Node.If) s).body);
            r.add(((Node.If) s).orelse);
        } else if (s instanceof Node.While) {
            r.add(((Node.While) s).body);
            r.add(((Node.While) s).orelse);
        } else if (s instanceof Node.For) {
            r.add(((Node.For) s).body);
            r.add(((Node.For) s).orelse);
        } else if (s instanceof Node.With) {
            r.add(((Node.With) s).body);
        } else if (s instanceof Node.Try) {
            Node.Try t = (Node.Try) s;
            r.add(t.body);
            for (Node.ExceptHandler h : t.handlers) {
                r.add(h.body);
            }
            r.add(t.orelse);
            r.add(t.finalbody);
        } else if (s instanceof Node.Match) {
            for (Node.MatchCase c : ((Node.Match) s).cases) {
                r.add(c.body);
            }
        }
        return r;
    }

    private static String resource(String path) {
        String s = read(STDLIB + path);
        return s != null ? s : read(SITE + path);
    }

    private static String read(String resource) {
        InputStream found = PythonTranspiler.class.getResourceAsStream(resource); // NOPMD CloseResource - closed by the try below
        if (found == null) {
            return null;
        }
        try (InputStream in = found) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                b.write(buf, 0, n);
            }
            return new String(b.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the standard library module " + resource, e);
        }
    }

    /// The Java class of a module: `py_` and the module name, with every
    /// character a Java identifier cannot hold (including the dots) written
    /// as `$` and its hex code. Python names never contain `$`, so two
    /// modules never share a class.
    public static String className(String module) {
        StringBuilder sb = new StringBuilder("py_");
        for (int i = 0; i < module.length(); i++) {
            char c = module.charAt(i);
            if (c < 128 && (Character.isLetterOrDigit(c) || c == '_')) {
                sb.append(c);
            } else {
                sb.append('$').append(Integer.toHexString(c));
            }
        }
        return sb.toString();
    }

    /// The bridge from `_cn1server.serve` to the backend's `HttpServer`. One
    /// worker thread: Python code runs one request at a time, as under the
    /// GIL, and the server's event loop still accepts and parses
    /// connections concurrently around it.
    private static String backendServer() {
        return "// Generated by the Codename One Python transpiler.\n"
                + "package " + PACKAGE + ";\n\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "import com.codename1.python.runtime.PyServer;\n"
                + "import java.util.Iterator;\n"
                + "import java.util.LinkedHashMap;\n"
                + "import java.util.Map;\n\n"
                + "final class BackendServer extends PyServer {\n"
                + "    static void install() {\n"
                + "        PyServer.install(new BackendServer());\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public void serve(final Object dispatch, String host, int port) {\n"
                + "        HttpServer server;\n"
                + "        try {\n"
                + "            server = HttpServer.start(host, port, 512, 1, new HttpServer.Handler() {\n"
                + "                public HttpServer.Response handle(HttpServer.Request r) {\n"
                + "                    Map in = r.getHeaders();\n"
                + "                    String[] headers = new String[in.size() * 2];\n"
                + "                    int at = 0;\n"
                + "                    for (Iterator it = in.entrySet().iterator(); it.hasNext();) {\n"
                + "                        Map.Entry e = (Map.Entry) it.next();\n"
                + "                        headers[at++] = String.valueOf(e.getKey());\n"
                + "                        headers[at++] = String.valueOf(e.getValue());\n"
                + "                    }\n"
                + "                    PyServer.Response res = PyServer.call(dispatch,\n"
                + "                            PyServer.request(r.getMethod(), r.getTarget(), headers, r.getBodyBytes()));\n"
                + "                    Map extra = new LinkedHashMap();\n"
                + "                    String type = null;\n"
                + "                    for (int i = 0; i + 1 < res.headers.length; i += 2) {\n"
                + "                        if (\"content-type\".equalsIgnoreCase(res.headers[i])) {\n"
                + "                            type = res.headers[i + 1];\n"
                + "                        } else if (!\"content-length\".equalsIgnoreCase(res.headers[i])) {\n"
                + "                            extra.put(res.headers[i], res.headers[i + 1]);\n"
                + "                        }\n"
                + "                    }\n"
                + "                    return new HttpServer.Response(res.status, type, res.body, extra);\n"
                + "                }\n"
                + "            });\n"
                + "        } catch (Exception e) {\n"
                + "            throw new RuntimeException(\"cannot serve on port \" + port + \": \" + e.getMessage());\n"
                + "        }\n"
                + "        System.out.println(\"serving on port \" + server.getPort());\n"
                + "        server.awaitTermination();\n"
                + "    }\n"
                + "}\n";
    }

    /// The name of the constant that carries a module's line table: the
    /// Python file, a newline, then `javaLine:pythonLine` pairs at every
    /// line where the Python line changes. The build's class post-processing
    /// (`PythonLineMapper` in build-engine) rewrites the class's line
    /// numbers and source file from it and removes the constant.
    public static final String LINE_MAP_FIELD = "$cn1PyLines";

    /// Adds the line table, computed from the line comments the emitter
    /// wrote before each statement, to the class declaration's own line so
    /// that no line moves.
    static String withLineMap(String java, String pyFile) {
        StringBuilder pairs = new StringBuilder();
        int line = 0;
        int py = 0;
        int last = -1;
        int start = 0;
        while (start < java.length()) {
            int end = java.indexOf('\n', start);
            if (end < 0) {
                end = java.length();
            }
            line++;
            int i = start;
            while (i < end && java.charAt(i) == ' ') {
                i++;
            }
            if (java.startsWith(Emitter.LINE_MARK, i)) {
                int stop = java.indexOf(' ', i + Emitter.LINE_MARK.length());
                py = Integer.parseInt(java.substring(i + Emitter.LINE_MARK.length(), stop));
            } else if (py > 0 && py != last && i < end) {
                pairs.append(pairs.length() == 0 ? "" : ",").append(line).append(':').append(py);
                last = py;
            }
            start = end + 1;
        }
        String decl = "public final class ";
        int at = java.indexOf(decl);
        int brace = at < 0 ? -1 : java.indexOf("{\n", at);
        if (brace < 0 || pairs.length() == 0) {
            return java;
        }
        String value = pyFile + "\n" + pairs;
        StringBuilder lit = new StringBuilder("\"");
        for (int k = 0; k < value.length(); k++) {
            char c = value.charAt(k);
            if (c == '\n') {
                lit.append("\\n");
            } else if (c == '"' || c == '\\') {
                lit.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                lit.append(String.format("\\u%04x", (int) c));
            } else {
                lit.append(c);
            }
        }
        lit.append('"');
        return java.substring(0, brace + 1) + " private static final String " + LINE_MAP_FIELD + " = " + lit + ";"
                + java.substring(brace + 1);
    }

    private static String registry(Map<String, String> classes, String mainModule, boolean interop, boolean server,
            Map<String, String> nativeModules) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by the Codename One Python transpiler.\n");
        sb.append("package ").append(PACKAGE).append(";\n\n");
        sb.append("/// Every transpiled Python module of this build.\n");
        sb.append("public final class PythonModules {\n");
        sb.append("    private static boolean registered;\n\n");
        sb.append("    private PythonModules() {\n    }\n\n");
        sb.append("    /// Makes every module importable; idempotent.\n");
        sb.append("    public static void registerAll() {\n");
        sb.append("        if (registered) {\n            return;\n        }\n");
        sb.append("        registered = true;\n");
        if (interop) {
            sb.append("        JavaInterop.register();\n");
        }
        if (server) {
            sb.append("        BackendServer.install();\n");
        }
        if (classes.containsKey("_cn1_http")) {
            sb.append("        com.codename1.python.runtime.Cn1Http.installDefault();\n");
        }
        if (classes.containsKey("sqlite3")) {
            sb.append("        com.codename1.python.runtime.Cn1Sql.installDefault();\n");
        }
        for (String cls : new java.util.TreeSet<String>(nativeModules.values())) {
            sb.append("        ").append(cls).append(".install();\n");
        }
        for (String cls : classes.values()) {
            sb.append("        ").append(cls).append(".register();\n");
        }
        sb.append("    }\n\n");
        sb.append("    /// Runs the entry module as `__main__` and returns the status the\n");
        sb.append("    /// interpreter would exit with. An exception the program does not\n");
        sb.append("    /// catch is reported on stderr, never thrown: on a device the\n");
        sb.append("    /// application goes on, and only a command line exits with it.\n");
        sb.append("    public static int runMain() {\n");
        sb.append("        registerAll();\n");
        if (mainModule != null) {
            sb.append("        com.codename1.python.runtime.Gil.enter();\n");
            sb.append("        try {\n");
            sb.append("            ").append(classes.get(mainModule)).append(".exec(\"__main__\");\n");
            sb.append("            return 0;\n");
            sb.append("        } catch (com.codename1.python.runtime.PyException e) {\n");
            sb.append("            return com.codename1.python.runtime.StdModules.exitStatus(e);\n");
            sb.append("        } finally {\n");
            sb.append("            com.codename1.python.runtime.StdModules.runExitHandlers();\n");
            sb.append("            com.codename1.python.runtime.Gil.exit();\n");
            sb.append("        }\n");
        } else {
            sb.append("        return 0;\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
