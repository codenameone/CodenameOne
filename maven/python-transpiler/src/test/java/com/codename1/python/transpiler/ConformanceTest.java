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
package com.codename1.python.transpiler;

import com.codename1.python.transpiler.api.PythonTranspiler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Transpiles each program under `conformance/`, compiles the Java against
/// the runtime, runs it and compares its output with what CPython printed
/// for the same program (the `.out` file beside it). A directory is a
/// multi-module program whose entry module is `main`.
///
/// Every program runs in a class loader of its own that also holds the
/// runtime, so the module registry starts empty each time.
class ConformanceTest {
    private static final Path ROOT = Paths.get("src/test/resources/conformance");

    @TestFactory
    List<DynamicTest> programs() throws IOException {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        try (Stream<Path> s = Files.list(ROOT)) {
            for (Path p : s.sorted().collect(Collectors.toList())) {
                String n = p.getFileName().toString();
                // -Dconformance.only=name runs that one program.
                String only = System.getProperty("conformance.only");
                if (only != null && !only.isEmpty() && !n.equals(only) && !n.equals(only + ".py")) {
                    continue;
                }
                if (Files.isDirectory(p) || n.endsWith(".py")) {
                    final Path program = p;
                    tests.add(DynamicTest.dynamicTest(n, () -> run(program)));
                }
            }
        }
        return tests;
    }

    private static void run(Path program) throws Exception {
        String name = program.getFileName().toString();
        String stem = name.endsWith(".py") ? name.substring(0, name.length() - 3) : name;
        Map<String, String> sources = new TreeMap<String, String>();
        if (Files.isDirectory(program)) {
            try (Stream<Path> s = Files.walk(program)) {
                for (Path p : s.filter(x -> x.toString().endsWith(".py")).collect(Collectors.toList())) {
                    sources.put(program.relativize(p).toString().replace(File.separatorChar, '/'), read(p));
                }
            }
        } else {
            sources.put("main.py", read(program));
        }
        // An http_ program talks to HttpTestServer, whose address it reads
        // from the module _testbase.
        HttpTestServer server = stem.startsWith("http_") ? new HttpTestServer(0) : null;
        if (server != null) {
            sources.put("_testbase.py", "BASE = \"http://127.0.0.1:" + server.port() + "\"\n");
        }
        try {
            run(stem, sources, server != null);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private static void run(String stem, Map<String, String> sources, boolean http) throws Exception {
        Path work = Files.createTempDirectory("py-conf-" + stem);
        Path gen = work.resolve("gen");
        Path cls = work.resolve("classes");
        Files.createDirectories(cls);
        List<String> args = new ArrayList<String>();
        String runtime = location(com.codename1.python.runtime.Py.class);
        String core = location(com.codename1.io.Util.class);
        // Programs may import Java classes: the JDK's, and the core's.
        List<File> classPath = Collections.singletonList(new File(core));
        for (Map.Entry<String, String> e : PythonTranspiler.transpile(sources, "main", classPath).entrySet()) {
            Path f = gen.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.write(f, e.getValue().getBytes(StandardCharsets.US_ASCII));
            args.add(f.toString());
        }
        args.add(0, "-nowarn");
        args.add(1, "--release");
        args.add(2, "17");
        args.add(3, "-cp");
        args.add(4, runtime + File.pathSeparator + core);
        args.add(5, "-d");
        args.add(6, cls.toString());
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int rc = javac.run(null, diagnostics, diagnostics, args.toArray(new String[0]));
        assertEquals(0, rc, "generated Java did not compile:\n" + diagnostics.toString("UTF-8"));

        URL[] urls = {cls.toUri().toURL(), new File(runtime).toURI().toURL(), new File(core).toURI().toURL(),
                new File(location(JvmFileSystem.class)).toURI().toURL()};
        Path fsRoot = work.resolve("fs");
        Files.createDirectories(fsRoot.resolve("home"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream saved = System.out;
        PrintStream savedErr = System.err;
        String failure = null;
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            System.setOut(new PrintStream(out, true, "UTF-8"));
            System.setErr(new PrintStream(err, true, "UTF-8"));
            try {
                Class<?> fsType = loader.loadClass("com.codename1.python.runtime.PyFileSystem");
                Object fs = loader.loadClass(JvmFileSystem.class.getName()).getConstructor(String.class)
                        .newInstance(fsRoot.toString());
                fsType.getMethod("install", fsType).invoke(null, fs);
                if (http) {
                    Class<?> httpType = loader.loadClass("com.codename1.python.runtime.PyHttp");
                    httpType.getMethod("install", httpType)
                            .invoke(null, loader.loadClass(JvmHttp.class.getName()).getConstructor().newInstance());
                }
                Object status = loader.loadClass(PythonTranspiler.PACKAGE + ".PythonModules").getMethod("runMain")
                        .invoke(null);
                // runMain reports an uncaught Python exception on stderr and
                // answers the status the interpreter would exit with.
                if (!Integer.valueOf(0).equals(status)) {
                    failure = "exit status " + status + ":\n" + err.toString("UTF-8");
                }
            } catch (InvocationTargetException e) {
                failure = String.valueOf(e.getCause());
                if (!failure.startsWith("com.codename1.python.runtime.PyException")) {
                    // A Java failure: name the frames that recur, which is
                    // where a runaway recursion is.
                    StackTraceElement[] st = e.getCause().getStackTrace();
                    Map<String, Integer> seen = new TreeMap<String, Integer>();
                    for (int i = 0; i < Math.min(st.length, 400); i++) {
                        seen.merge(st[i].toString(), 1, Integer::sum);
                    }
                    StringBuilder b = new StringBuilder(failure);
                    for (Map.Entry<String, Integer> en : seen.entrySet()) {
                        if (en.getValue() > 3 || st.length < 60) {
                            b.append("\n    ").append(en.getValue()).append(" x ").append(en.getKey());
                        }
                    }
                    failure = b.toString();
                }
            }
        } finally {
            System.out.flush();
            System.setOut(saved);
            System.setErr(savedErr);
        }
        String expected = read(ROOT.resolve(stem + ".out"));
        String actual = out.toString("UTF-8");
        assertEquals(expected, actual, failure == null ? "output differs from CPython" : "raised " + failure);
        assertTrue(failure == null, "raised " + failure);
    }

    private static String location(Class<?> c) throws Exception {
        return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
