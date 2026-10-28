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
        return transpile(sources, mainModule, classPath, nativeModules, DEFAULT_VIRTUAL_THREAD_STACK);
    }

    /// The C stack a served request's virtual thread gets unless the build asks
    /// for another: transpiled Python frames are large and `re` recurses, and
    /// the backend's own 64KB default overflowed on a FastAPI route match.
    public static final int DEFAULT_VIRTUAL_THREAD_STACK = 1024 * 1024;

    /// Parses a stack size: `1048576`, `1024KB` or `1MB` (suffix case ignored,
    /// blanks around the number allowed). -1 for anything else, for zero, or
    /// for a size that does not fit an int.
    ///
    /// @param raw the size as written
    /// @return the size in bytes, or -1
    public static int parseStackSize(String raw) {
        String s = raw == null ? "" : raw.trim();
        long unit = 1;
        if (s.length() > 2) {
            String suffix = s.substring(s.length() - 2);
            if ("KB".equalsIgnoreCase(suffix)) {
                unit = 1024;
            } else if ("MB".equalsIgnoreCase(suffix)) {
                unit = 1024 * 1024;
            }
            if (unit > 1) {
                s = s.substring(0, s.length() - 2).trim();
            }
        }
        if (s.length() == 0 || s.length() > 10) {
            return -1;
        }
        long n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        long bytes = n * unit;
        return bytes <= 0 || bytes > Integer.MAX_VALUE ? -1 : (int) bytes;
    }

    /// @param sources relative `.py` path to source text
    /// @param mainModule the module run as `__main__`, or null for a library
    /// @param classPath as for [#transpile(Map, String, List)]
    /// @param nativeModules as for [#transpile(Map, String, List, Map)]
    /// @param virtualThreadStack the bytes of C stack the generated server
    ///        asks each request's virtual thread to have (the build hint
    ///        `python.virtualThreadStack`); the backend only ever raises its
    ///        size, so `CN1_HTTP_VT_STACK` can still ask for more at start-up
    public static Map<String, String> transpile(Map<String, String> sources, String mainModule,
            List<java.io.File> classPath, Map<String, String> nativeModules, int virtualThreadStack) {
        if (virtualThreadStack <= 0) {
            throw new IllegalArgumentException("virtual thread stack must be positive: " + virtualThreadStack);
        }
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
        boolean db = false;
        boolean image = false;
        boolean serverImage = false;
        for (String text : sources.values()) {
            server |= text.contains("_cn1server");
            db |= text.contains("_cn1db");
            image |= text.contains("_cn1image");
        }
        if (server || db || image) {
            // A web framework is bundled: serve it over the backend's HTTP
            // server when the build has one (a backend build). Elsewhere
            // serving raises, and the frameworks' test clients still work.
            try (com.codename1.python.transpiler.codegen.ClassPathReader cp =
                    new com.codename1.python.transpiler.codegen.ClassPathReader(classPath)) {
                // The database bridge is wanted by any program reaching
                // _cn1db (SQLAlchemy's server dialects): a batch job or a
                // worker as much as a web application.
                boolean backend = cp.find("com/codename1/backend/HttpServer") != null;
                server &= backend;
                db = backend && (db || server);
                // PIL's engine: on a server the generated BackendImage over
                // com.codename1.backend.image (Pillow's exact pixels); on a
                // client PyImageNative over the UI Image, which a backend
                // build does not have (and leaves out of its classes).
                // Chosen by the class path rather than by a build target on
                // purpose: the transpiler runs once, in the common module's
                // generate-sources, before any target is known, and the
                // classes it writes serve every target built from them. What
                // separates a backend from a client application is the
                // artifact it compiles against -- codenameone-backend is on
                // a backend's class path and on no client's -- so the probe
                // answers exactly the question "can these classes reach
                // com.codename1.backend.image", which is what the engine
                // needs; a target name could disagree with the classes.
                serverImage = image && backend;
                image &= !backend;
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
            out.put(PACKAGE.replace('.', '/') + "/BackendServer.java", backendServer(virtualThreadStack));
        }
        if (db) {
            out.put(PACKAGE.replace('.', '/') + "/BackendDb.java", backendDb());
        }
        if (serverImage) {
            out.put(PACKAGE.replace('.', '/') + "/BackendImage.java", backendImage());
        }
        out.put(PACKAGE.replace('.', '/') + "/PythonModules.java", registry(classes, mainModule, interop != null, server, db,
                image, serverImage, nativeModules));
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
        Set<Node.Module> reduces = new HashSet<Node.Module>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Node.Module t = trees.get(e.getKey().replace('\\', '/'));
            if (t != null && mentionsOpen(e.getValue())) {
                opens.add(t);
            }
            if (t != null && mentionsReduction(e.getValue())) {
                reduces.add(t);
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
            if (reduces.contains(tree)) {
                // object.__reduce_ex__, __reduce__ and __getstate__ are
                // copyreg's functions, written in Python.
                imported.add("copyreg");
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
                    if (mentionsReduction(text)) {
                        // A bundled module (pickle, copy) reaching the
                        // reduction protocol needs copyreg as much as the
                        // program's own modules do.
                        reduces.add(t);
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

    /// The generated server's websocket connection class, on its own so
    /// a test can compile it against a fake server and drive its state
    /// machine.
    public static String webSocketSocketSource() {
        return ""
                + "    /**\n"
                + "     * One websocket connection driven by the Python endpoint, which runs on\n"
                + "     * Python's websocket event loop thread. The handshake waits for the\n"
                + "     * endpoint to accept or refuse, so a refusal is answered before the\n"
                + "     * upgrade and an accepted subprotocol is the one negotiated. Messages\n"
                + "     * the endpoint sends before the server has opened the session are kept\n"
                + "     * and sent from onOpen, in order.\n"
                + "     */\n"
                + "    static final class Socket implements WebSocket, PyServer.WebSocketSink {\n"
                + "        static long handshakeMillis = 30000L;\n"
                + "        private static final int DECIDING = 0;\n"
                + "        private static final int ACCEPTED = 1;\n"
                + "        private static final int REFUSED = 2;\n"
                + "        /** The handshake gave up; a late accept or message does nothing. */\n"
                + "        private static final int TIMED_OUT = 3;\n"
                + "        private static final Object[] NONE = new Object[0];\n"
                + "        /** Stands in the queue once the session is open and drained. */\n"
                + "        private static final Object[] OPEN = new Object[0];\n"
                + "        private final Object open;\n"
                + "        private final String target;\n"
                + "        private final String[] headers;\n"
                + "        // Atomics, never a monitor: this runs on backend virtual threads,\n"
                + "        // where Object.wait or a contended synchronized blocks the HOST\n"
                + "        // thread and wedges every task queued on it.\n"
                + "        private final java.util.concurrent.atomic.AtomicInteger state = new java.util.concurrent.atomic.AtomicInteger();\n"
                + "        /** Messages sent before onOpen, as an immutable array; OPEN after. */\n"
                + "        private final java.util.concurrent.atomic.AtomicReference queue = new java.util.concurrent.atomic.AtomicReference(NONE);\n"
                + "        /** Written before the queue becomes OPEN, which publishes it. */\n"
                + "        private WebSocketSession session;\n"
                + "        private Object connection;\n"
                + "        private String subprotocol;\n"
                + "        private String[] acceptHeaders;\n"
                + "        private WebSocket refusal;\n"
                + "\n"
                + "        Socket(Object open, String target, String[] headers) {\n"
                + "            this.open = open;\n"
                + "            this.target = target;\n"
                + "            this.headers = headers;\n"
                + "        }\n"
                + "\n"
                + "        /** Runs the endpoint up to its accept; answers this socket, or the refusal. */\n"
                + "        WebSocket handshake() {\n"
                + "            Object c = PyServer.openWebSocket(open, target, headers, this);\n"
                + "            connection = c;\n"
                + "            long end = System.currentTimeMillis() + handshakeMillis;\n"
                + "            int attempt = 0;\n"
                + "            while (state.get() == DECIDING) {\n"
                + "                if (System.currentTimeMillis() >= end) {\n"
                + "                    state.compareAndSet(DECIDING, TIMED_OUT);\n"
                + "                    break;\n"
                + "                }\n"
                + "                PyServer.pauseWaiter(attempt++);\n"
                + "            }\n"
                + "            if (state.get() != ACCEPTED) {\n"
                + "                PyServer.webSocketClosed(c, 1006, \"\");\n"
                + "                WebSocket r = refusal;\n"
                + "                return r != null ? r : HttpServer.refuseUpgrade(500, \"Internal Server Error\");\n"
                + "            }\n"
                + "            return this;\n"
                + "        }\n"
                + "\n"
                + "        public String[] getSubprotocols() {\n"
                + "            return subprotocol == null ? null : new String[] {subprotocol};\n"
                + "        }\n"
                + "\n"
                + "        public java.util.Map getHandshakeHeaders() {\n"
                + "            if (acceptHeaders == null || acceptHeaders.length == 0) {\n"
                + "                return null;\n"
                + "            }\n"
                + "            return pairs(acceptHeaders);\n"
                + "        }\n"
                + "\n"
                + "        static java.util.Map pairs(String[] h) {\n"
                + "            Map out = new LinkedHashMap();\n"
                + "            for (int i = 0; i + 1 < h.length; i += 2) {\n"
                + "                Object had = out.get(h[i]);\n"
                + "                if (had == null) {\n"
                + "                    out.put(h[i], h[i + 1]);\n"
                + "                } else {\n"
                + "                    java.util.List all = had instanceof java.util.List ? (java.util.List) had : new java.util.ArrayList();\n"
                + "                    if (all != had) {\n"
                + "                        all.add(had);\n"
                + "                    }\n"
                + "                    all.add(h[i + 1]);\n"
                + "                    out.put(h[i], all);\n"
                + "                }\n"
                + "            }\n"
                + "            return out;\n"
                + "        }\n"
                + "\n"
                + "        public void onOpen(WebSocketSession s) {\n"
                + "            session = s;\n"
                + "            // Drain until the queue is empty, then mark it OPEN in the same\n"
                + "            // compare-and-set: a message sent meanwhile is drained here, so\n"
                + "            // the order the endpoint sent them in is kept.\n"
                + "            while (true) {\n"
                + "                Object[] cur = (Object[]) queue.get();\n"
                + "                if (cur.length == 0) {\n"
                + "                    if (queue.compareAndSet(cur, OPEN)) {\n"
                + "                        return;\n"
                + "                    }\n"
                + "                } else if (queue.compareAndSet(cur, NONE)) {\n"
                + "                    for (int i = 0; i < cur.length; i++) {\n"
                + "                        deliver(cur[i]);\n"
                + "                    }\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "\n"
                + "        public void onText(WebSocketSession s, String message) {\n"
                + "            PyServer.webSocketText(connection, message);\n"
                + "        }\n"
                + "\n"
                + "        public void onBinary(WebSocketSession s, byte[] message, int offset, int length) {\n"
                + "            byte[] copy = new byte[length];\n"
                + "            System.arraycopy(message, offset, copy, 0, length);\n"
                + "            PyServer.webSocketBinary(connection, copy);\n"
                + "        }\n"
                + "\n"
                + "        public void onClose(WebSocketSession s, int code, String reason) {\n"
                + "            PyServer.webSocketClosed(connection, code, reason);\n"
                + "        }\n"
                + "\n"
                + "        public void accept(String subprotocol, String[] headers) {\n"
                + "            int st = state.get();\n"
                + "            if (st == TIMED_OUT || st == REFUSED) {\n"
                + "                // The server already answered the upgrade and delivered a 1006\n"
                + "                // disconnect to the endpoint's receive; an accept now must not\n"
                + "                // look like it worked.\n"
                + "                throw PyServer.webSocketGone(st == TIMED_OUT\n"
                + "                        ? \"websocket handshake timed out before accept\" : \"websocket was refused\");\n"
                + "            }\n"
                + "            if (st != DECIDING) {\n"
                + "                return;\n"
                + "            }\n"
                + "            this.subprotocol = subprotocol;\n"
                + "            this.acceptHeaders = headers;\n"
                + "            state.compareAndSet(DECIDING, ACCEPTED);\n"
                + "        }\n"
                + "\n"
                + "        public void deny(int status, String[] headers, byte[] body) {\n"
                + "            // Racing the timeout is benign: refusal is written before the\n"
                + "            // compare-and-set, and handshake reads it only after seeing\n"
                + "            // REFUSED; if the timeout won, refusal is never read and the\n"
                + "            // client gets the 500 refusal instead.\n"
                + "            if (state.get() != DECIDING) {\n"
                + "                return;\n"
                + "            }\n"
                + "            String type = null;\n"
                + "            for (int i = 0; i + 1 < headers.length; i += 2) {\n"
                + "                if (\"content-type\".equalsIgnoreCase(headers[i])) {\n"
                + "                    type = headers[i + 1];\n"
                + "                }\n"
                + "            }\n"
                + "            Map extra = pairs(headers);\n"
                + "            extra.remove(\"content-type\");\n"
                + "            extra.remove(\"content-length\");\n"
                + "            refusal = HttpServer.refuseUpgrade(new HttpServer.Response(status,\n"
                + "                    type == null ? \"text/plain; charset=utf-8\" : type, body, extra));\n"
                + "            state.compareAndSet(DECIDING, REFUSED);\n"
                + "        }\n"
                + "\n"
                + "        public void text(String value) {\n"
                + "            outgoing(value);\n"
                + "        }\n"
                + "\n"
                + "        public void binary(byte[] value) {\n"
                + "            outgoing(value);\n"
                + "        }\n"
                + "\n"
                + "        public void close(int code, String reason) {\n"
                + "            if (state.compareAndSet(DECIDING, REFUSED)) {\n"
                + "                // Closed before accepting: the upgrade is refused.\n"
                + "                return;\n"
                + "            }\n"
                + "            outgoing(new Object[] {Integer.valueOf(code), reason});\n"
                + "        }\n"
                + "\n"
                + "        private void outgoing(Object message) {\n"
                + "            int st = state.get();\n"
                + "            if (st != ACCEPTED) {\n"
                + "                // Before accept, or refused or timed out: nobody will open a\n"
                + "                // session, so the message cannot go anywhere. Raised, as\n"
                + "                // Starlette raises a send outside an accepted connection.\n"
                + "                throw PyServer.webSocketGone(st == DECIDING ? \"websocket send before accept\"\n"
                + "                        : st == TIMED_OUT ? \"websocket handshake timed out\" : \"websocket was refused\");\n"
                + "            }\n"
                + "            while (true) {\n"
                + "                Object[] cur = (Object[]) queue.get();\n"
                + "                if (cur == OPEN) {\n"
                + "                    deliver(message);\n"
                + "                    return;\n"
                + "                }\n"
                + "                Object[] next = new Object[cur.length + 1];\n"
                + "                System.arraycopy(cur, 0, next, 0, cur.length);\n"
                + "                next[cur.length] = message;\n"
                + "                if (queue.compareAndSet(cur, next)) {\n"
                + "                    return;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "\n"
                + "        private void deliver(Object message) {\n"
                + "            try {\n"
                + "                if (message instanceof String) {\n"
                + "                    session.sendText((String) message);\n"
                + "                } else if (message instanceof byte[]) {\n"
                + "                    session.sendBinary((byte[]) message);\n"
                + "                } else if (message instanceof Object[]) {\n"
                + "                    Object[] c = (Object[]) message;\n"
                + "                    Object code = c[0];\n"
                + "                    session.close(code instanceof Integer ? ((Integer) code).intValue() : 1000, String.valueOf(c[1]));\n"
                + "                }\n"
                + "            } catch (java.io.IOException e) {\n"
                + "                session.abort();\n"
                + "            }\n"
                + "        }\n"
                + "    }\n";
    }

    /// Whether `text` may call the builtin `open` (a cheap textual test:
    /// bundling `io` for a false positive costs only its size).
    /// Whether `text` names a method of the reduction protocol that
    /// `object` implements in `copyreg`. A textual test over the whole
    /// source, string literals included, so `getattr(o, "__reduce_ex__")`
    /// counts as much as `o.__reduce_ex__`; a name assembled at run time
    /// from pieces is not seen, and a module doing that imports copyreg (or
    /// copy/pickle, which do) itself.
    private static boolean mentionsReduction(String text) {
        return text != null && (text.contains("__reduce") || text.contains("__getstate__"));
    }

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

    /// The driver behind `_cn1db`, over the backend's `Database`: what
    /// SQLAlchemy's PostgreSQL, MySQL and MariaDB dialects run on. `?`
    /// placeholders are bound by the backend for every engine. Row values are
    /// narrowed to what the runtime hands Python (long, double, string, bytes,
    /// boolean); anything else -- a timestamp, a decimal -- arrives as its text.
    private static String backendDb() {
        return "// Generated by the Codename One Python transpiler.\n"
                + "package " + PACKAGE + ";\n\n"
                + "import com.codename1.backend.Database;\n"
                + "import com.codename1.python.runtime.PyDb;\n"
                + "import com.codename1.python.runtime.PySql;\n"
                + "import java.io.IOException;\n"
                + "import java.util.Iterator;\n"
                + "import java.util.List;\n"
                + "import java.util.Map;\n\n"
                + "final class BackendDb extends PyDb {\n"
                + "    static void install() {\n"
                + "        PyDb.install(new BackendDb());\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public PyDb.Connection open(String url) throws IOException {\n"
                + "        return new Conn(Database.open(url));\n"
                + "    }\n\n"
                + "    static Object value(Object v) {\n"
                + "        if (v == null || v instanceof Long || v instanceof Double || v instanceof String\n"
                + "                || v instanceof byte[] || v instanceof Boolean) {\n"
                + "            return v;\n"
                + "        }\n"
                + "        if (v instanceof Integer || v instanceof Short || v instanceof Byte) {\n"
                + "            return Long.valueOf(((Number) v).longValue());\n"
                + "        }\n"
                + "        if (v instanceof Float) {\n"
                + "            return Double.valueOf(((Float) v).doubleValue());\n"
                + "        }\n"
                + "        return String.valueOf(v);\n"
                + "    }\n\n"
                + "    static final class Conn extends PyDb.Connection {\n"
                + "        private final Database db;\n\n"
                + "        Conn(Database db) {\n"
                + "            this.db = db;\n"
                + "        }\n\n"
                + "        @Override\n"
                + "        public String dialect() {\n"
                + "            return db.dialect().getName();\n"
                + "        }\n\n"
                + "        @Override\n"
                + "        public PySql.Result run(String sql, Object[] params, boolean query) throws IOException {\n"
                + "            PySql.Result r = new PySql.Result();\n"
                + "            if (!query) {\n"
                + "                r.changes = db.execute(sql, params);\n"
                + "                r.lastRowId = db.lastInsertId();\n"
                + "                return r;\n"
                + "            }\n"
                + "            List rows = db.query(sql, params);\n"
                + "            for (int i = 0; i < rows.size(); i++) {\n"
                + "                Map row = (Map) rows.get(i);\n"
                + "                if (i == 0) {\n"
                + "                    r.columns = new String[row.size()];\n"
                + "                    int c = 0;\n"
                + "                    for (Iterator it = row.keySet().iterator(); it.hasNext();) {\n"
                + "                        r.columns[c++] = String.valueOf(it.next());\n"
                + "                    }\n"
                + "                }\n"
                + "                Object[] values = new Object[r.columns.length];\n"
                + "                for (int c = 0; c < values.length; c++) {\n"
                + "                    values[c] = value(row.get(r.columns[c]));\n"
                + "                }\n"
                + "                r.rows.add(values);\n"
                + "            }\n"
                + "            // DECIMAL, DATE and the like arrive as text; their kinds let\n"
                + "            // _cn1dbapi hand back decimal.Decimal and datetime values.\n"
                + "            r.types = Database.columnTypes(rows);\n"
                + "            return r;\n"
                + "        }\n\n"
                + "        @Override\n"
                + "        public void begin() throws IOException {\n"
                + "            db.beginTransaction();\n"
                + "        }\n\n"
                + "        @Override\n"
                + "        public void commit() throws IOException {\n"
                + "            db.commitTransaction();\n"
                + "        }\n\n"
                + "        @Override\n"
                + "        public void rollback() throws IOException {\n"
                + "            db.rollbackTransaction();\n"
                + "        }\n\n"
                + "        @Override\n"
                + "        public void close() {\n"
                + "            db.close();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    /// The bridge from `_cn1server.serve` to the backend's `HttpServer`.
    /// Requests run on a pool of worker threads; each holds the GIL only
    /// while Python runs (`PyServer.call`, `nextChunk`, `runAfter`), so one
    /// slow streamed response -- sleeping between chunks, or draining to a
    /// slow client -- leaves the other workers free. Background tasks run
    /// once the response has been handed back or its last chunk written.
    /// PIL's server engine (`BackendImage` and its FreeType layout), kept as
    /// a resource: it names `com.codename1.backend.image`, so it is compiled
    /// only into a backend build and the runtime never depends on the backend.
    private static String backendImage() {
        try (InputStream in = PythonTranspiler.class.getResourceAsStream("BackendImage.java.txt")) {
            if (in == null) {
                throw new IllegalStateException("BackendImage.java.txt is missing from the transpiler");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8).replace("@PACKAGE@", PACKAGE);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read BackendImage.java.txt: " + e.getMessage(), e);
        }
    }

    private static String backendServer(int virtualThreadStack) {
        return "// Generated by the Codename One Python transpiler.\n"
                + "package " + PACKAGE + ";\n\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "import com.codename1.backend.WebSocket;\n"
                + "import com.codename1.backend.WebSocketSession;\n"
                + "import com.codename1.python.runtime.PyServer;\n"
                + "import java.util.Iterator;\n"
                + "import java.util.LinkedHashMap;\n"
                + "import java.util.Map;\n\n"
                + "final class BackendServer extends PyServer {\n"
                + "    /**\n"
                + "     * Requests served at once. Python still runs one thread at a time\n"
                + "     * under the GIL; the others wait on sockets, sleeps and streams.\n"
                + "     */\n"
                + "    private static final int WORKERS = 16;\n\n"
                + "    static void install() {\n"
                // Handlers may run on virtual threads: the GIL must step them
                // aside rather than block their host thread (see Gil.Scheduler).
                + "        com.codename1.python.runtime.Gil.setScheduler(new com.codename1.python.runtime.Gil.Scheduler() {\n"
                + "            public long current() {\n"
                // The handle even while pinned in a monitor: the GIL's owner
                // key must not change between enter and exit.
                + "                return com.codename1.backend.VirtualThread.current();\n"
                + "            }\n\n"
                + "            public boolean pause() {\n"
                + "                if (!com.codename1.backend.VirtualThread.isVirtual()) {\n"
                + "                    return false;\n"
                + "                }\n"
                + "                com.codename1.backend.VirtualThread.yieldNow();\n"
                + "                return true;\n"
                + "            }\n"
                + "        });\n"
                // Transpiled Python frames are large and re recurses: the
                // backend's 64KB virtual-thread stack overflowed on a route
                // match. Mapped lazily, so this is address space, not memory.
                + "        HttpServer.requireVirtualThreadStack(" + virtualThreadStack + ");\n"
                + "        PyServer.install(new BackendServer());\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public void serve(final Object dispatch, String host, int port) {\n"
                + "        serve(dispatch, host, port, null);\n"
                + "    }\n\n"
                + "    static String[] headers(HttpServer.Request r) {\n"
                + "        Map in = r.getHeaders();\n"
                + "        String[] headers = new String[in.size() * 2];\n"
                + "        int at = 0;\n"
                + "        for (Iterator it = in.entrySet().iterator(); it.hasNext();) {\n"
                + "            Map.Entry e = (Map.Entry) it.next();\n"
                + "            headers[at++] = String.valueOf(e.getKey());\n"
                + "            headers[at++] = String.valueOf(e.getValue());\n"
                + "        }\n"
                + "        return headers;\n"
                + "    }\n\n"
                + webSocketSocketSource()
                + "\n"
                + "    /**\n"
                + "     * A streamed body: each chunk the application yields is written and\n"
                + "     * flushed as it is produced (chunked on HTTP/1.1). A client that goes\n"
                + "     * away, or a response the server never writes (HEAD), closes the\n"
                + "     * generator so its finally blocks run. Background tasks run once the\n"
                + "     * last chunk is out, and not at all for a client that went away.\n"
                + "     */\n"
                + "    static final class Chunks implements HttpServer.BodyWriter, java.io.Closeable {\n"
                + "        private final PyServer.Response res;\n"
                + "\n"
                + "        Chunks(PyServer.Response res) {\n"
                + "            this.res = res;\n"
                + "        }\n"
                + "\n"
                + "        public void writeTo(java.io.OutputStream out) throws java.io.IOException {\n"
                + "            boolean done = false;\n"
                + "            try {\n"
                + "                byte[] chunk;\n"
                + "                while ((chunk = PyServer.nextChunk(res)).length > 0) {\n"
                + "                    out.write(chunk);\n"
                + "                    out.flush();\n"
                + "                }\n"
                + "                done = true;\n"
                + "            } finally {\n"
                + "                if (done) {\n"
                + "                    PyServer.runAfterLater(res);\n"
                + "                } else {\n"
                + "                    PyServer.cancelAfter(res);\n"
                + "                    PyServer.closeStream(res);\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "\n"
                + "        public void close() {\n"
                + "            PyServer.closeStream(res);\n"
                + "            PyServer.runAfterLater(res);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void serve(final Object dispatch, String host, int port, final Object webSockets) {\n"
                + "        HttpServer.WebSocketRoutes routes = webSockets == null ? null : new HttpServer.WebSocketRoutes() {\n"
                + "            public void register(HttpServer.WebSocketRegistry registry) {\n"
                + "                registry.fallback(new HttpServer.WebSocketHandler() {\n"
                + "                    public WebSocket open(HttpServer.Request r) {\n"
                + "                        Socket socket = new Socket(webSockets, r.getTarget(), headers(r));\n"
                + "                        return socket.handshake();\n"
                + "                    }\n"
                + "                });\n"
                + "            }\n"
                + "        };\n"
                + "        HttpServer server;\n"
                + "        try {\n"
                + "            server = HttpServer.start(host, port, 512, WORKERS, new HttpServer.Handler() {\n"
                + "                public HttpServer.Response handle(HttpServer.Request r) {\n"
                + "                    PyServer.Response res = PyServer.call(dispatch,\n"
                + "                            PyServer.request(r.getMethod(), r.getTarget(), headers(r), r.getBodyBytes()));\n"
                + "                    Map extra = new LinkedHashMap();\n"
                + "                    String type = null;\n"
                + "                    for (int i = 0; i + 1 < res.headers.length; i += 2) {\n"
                + "                        if (\"content-type\".equalsIgnoreCase(res.headers[i])) {\n"
                + "                            type = res.headers[i + 1];\n"
                + "                        } else if (!\"content-length\".equalsIgnoreCase(res.headers[i])) {\n"
                + "                            extra.put(res.headers[i], res.headers[i + 1]);\n"
                + "                        }\n"
                + "                    }\n"
                + "                    if (res.isStreamed()) {\n"
                + "                        HttpServer.Response streamed = HttpServer.Response.stream(res.status, type, new Chunks(res));\n"
                + "                        for (Iterator it = extra.entrySet().iterator(); it.hasNext();) {\n"
                + "                            Map.Entry e = (Map.Entry) it.next();\n"
                + "                            streamed.header((String) e.getKey(), (String) e.getValue());\n"
                + "                        }\n"
                + "                        return streamed;\n"
                + "                    }\n"
                + "                    PyServer.runAfterLater(res);\n"
                + "                    return new HttpServer.Response(res.status, type, res.body, extra);\n"
                + "                }\n"
                + "            }, (com.codename1.backend.Tls) null, routes);\n"
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

    private static String registry(Map<String, String> classes, String mainModule, boolean interop, boolean server, boolean db,
            boolean image, boolean serverImage, Map<String, String> nativeModules) {
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
        if (db) {
            sb.append("        BackendDb.install();\n");
        }
        if (image) {
            sb.append("        com.codename1.python.runtime.PyImageNative.install();\n");
        }
        if (serverImage) {
            sb.append("        BackendImage.install();\n");
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
