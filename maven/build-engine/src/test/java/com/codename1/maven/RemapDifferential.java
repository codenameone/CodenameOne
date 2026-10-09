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
package com.codename1.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// Runs one piece of Java twice -- as javac compiled it against the JDK, and
/// after the remap step pointed it at the stand-ins a device has -- and
/// answers both sets of results for a test to compare.
///
/// The piece is the body of a method that adds what it computed to a list
/// named `out`. Source that needs a newer language level than the JVM running
/// the tests is compiled and run by the JDK that `JAVA17_HOME` names; with
/// neither, [#available] says so and the test is skipped.
final class RemapDifferential {

    private static final String IMPORTS = "import java.io.*;\n"
            + "import java.nio.charset.*;\n"
            + "import java.util.*;\n"
            + "import java.util.concurrent.*;\n"
            + "import java.util.concurrent.atomic.*;\n"
            + "import java.util.function.*;\n"
            + "import java.util.regex.*;\n"
            + "import java.util.stream.*;\n"
            + "import java.time.*;\n"
            + "import java.time.format.*;\n"
            + "import java.time.temporal.*;\n"
            + "import java.net.URLDecoder;\n"
            + "import java.net.URLEncoder;\n"
            + "import java.security.*;\n";

    private final File work;
    private final int release;
    private final List<String> sources = new ArrayList<String>();
    private final List<String> resources = new ArrayList<String>();

    /// `release` is the language level the source needs: 8, 17 or 21.
    RemapDifferential(File work, int release) {
        this.work = work;
        this.release = release;
    }

    /// The class path of this test, with the headless implementation ahead
    /// of everything: its `ImplementationFactory` has to be found before the
    /// simulator's, which would open a window.
    private static String testClassPath() {
        String path = System.getProperty("surefire.test.class.path");
        if (path == null || path.length() == 0) {
            path = System.getProperty("java.class.path");
        }
        StringBuilder first = new StringBuilder();
        StringBuilder rest = new StringBuilder();
        for (String entry : path.split(File.pathSeparator)) {
            StringBuilder to = entry.contains("compat-testing") ? first : rest;
            to.append(entry).append(File.pathSeparator);
        }
        return first.append(rest).toString();
    }

    private static int running() {
        String v = System.getProperty("java.specification.version");
        return v.startsWith("1.") ? Integer.parseInt(v.substring(2)) : Integer.parseInt(v);
    }

    private File otherJdk() {
        String[] names = release > 17 ? new String[] {"JAVA21_HOME", "JAVA25_HOME"}
                : new String[] {"JAVA17_HOME", "JAVA21_HOME", "JAVA25_HOME"};
        for (String name : names) {
            String home = System.getenv(name);
            if (home != null && new File(home, "bin/javac").isFile()) {
                return new File(home);
            }
        }
        return null;
    }

    /// Whether a JDK that can compile and run this language level is here.
    boolean available() {
        return running() >= release || otherJdk() != null;
    }

    /// A further source file of package `q`, beside the body.
    RemapDifferential source(String name, String text) {
        sources.add("q/" + name + ".java");
        sources.add("package q;\n" + IMPORTS + text);
        return this;
    }

    /// A file on the class path of both runs, beside the classes: a
    /// `META-INF/services` file, say. Both runs are then forked, since a
    /// loader that only defines the fixture's classes offers no resources.
    RemapDifferential resource(String path, String text) {
        resources.add(path);
        resources.add(text);
        return this;
    }

    /// The results of `body` on the JDK, then after the remap.
    String[][] run(String body) throws Exception {
        sources.add("q/D.java");
        sources.add("package q;\n" + IMPORTS
                + "public class D {\n"
                + "    public static List<Object> run() throws Exception {\n"
                + "        List<Object> out = new ArrayList<>();\n"
                + body
                + "        return out;\n"
                + "    }\n"
                + "    static String text(Object o) {\n"
                + "        if (o instanceof Object[]) { return Arrays.deepToString((Object[]) o); }\n"
                + "        if (o instanceof byte[]) { return Arrays.toString((byte[]) o); }\n"
                + "        if (o instanceof int[]) { return Arrays.toString((int[]) o); }\n"
                + "        if (o instanceof long[]) { return Arrays.toString((long[]) o); }\n"
                + "        if (o instanceof char[]) { return Arrays.toString((char[]) o); }\n"
                + "        return String.valueOf(o);\n"
                + "    }\n"
                + "    public static String[] texts() throws Exception {\n"
                + "        List<Object> all = run();\n"
                + "        String[] t = new String[all.size()];\n"
                + "        for (int i = 0; i < t.length; i++) { t[i] = text(all.get(i)); }\n"
                + "        return t;\n"
                + "    }\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        StringBuilder sb = new StringBuilder();\n"
                + "        for (String s : texts()) {\n"
                + "            for (int i = 0; i < s.length(); i++) {\n"
                + "                char c = s.charAt(i);\n"
                + "                if (c == '\\\\') { sb.append(\"\\\\\\\\\"); }\n"
                + "                else if (c == '\\n') { sb.append(\"\\\\n\"); }\n"
                + "                else if (c == '\\r') { sb.append(\"\\\\r\"); }\n"
                + "                else if (c < ' ' || c > '~') { sb.append(\"\\\\x\").append((int) c).append(';'); }\n"
                + "                else { sb.append(c); }\n"
                + "            }\n"
                + "            sb.append('\\n');\n"
                + "        }\n"
                + "        System.out.print(sb);\n"
                + "    }\n"
                + "}\n");
        File src = new File(work, "src");
        File original = new File(work, "original");
        File remapped = new File(work, "remapped");
        assertTrue(original.mkdirs() && remapped.mkdirs());
        List<String> files = new ArrayList<String>();
        for (int i = 0; i < sources.size(); i += 2) {
            File f = new File(src, sources.get(i));
            assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
            Files.write(f.toPath(), sources.get(i + 1).getBytes("UTF-8"));
            files.add(f.getAbsolutePath());
        }
        boolean inProcess = running() >= release;
        File jdk = inProcess ? null : otherJdk();
        compile(jdk, original, files);
        for (int i = 0; i < resources.size(); i += 2) {
            for (File root : new File[] {original, remapped}) {
                File f = new File(root, resources.get(i));
                assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
                Files.write(f.toPath(), resources.get(i + 1).getBytes("UTF-8"));
            }
        }
        if (jdk == null && !resources.isEmpty()) {
            // The JDK running the tests, in a process of its own.
            File home = new File(System.getProperty("java.home"));
            jdk = new File(home, "bin/java").isFile() ? home : home.getParentFile();
            // Compiled above already, in this process.
        }

        ClassRelocator relocator = new ClassRelocator(CompatLayers.SWING);
        File[] classes = new File(original, "q").listFiles();
        assertNotNull(classes);
        assertTrue(new File(remapped, "q").isDirectory() || new File(remapped, "q").mkdirs());
        for (File c : classes) {
            byte[] out = relocator.remap(Files.readAllBytes(c.toPath()));
            Files.write(new File(remapped, "q/" + c.getName()).toPath(), out);
        }
        return new String[][] {execute(jdk, original, false), execute(jdk, remapped, true)};
    }

    /// Runs `body` both ways and requires the same results, of which there
    /// have to be at least `atLeast`.
    void same(String body, int atLeast, String... gone) throws Exception {
        String[][] both = run(body);
        // What the remap was to replace is named by no class any more, so
        // the second run cannot have been the JDK's own classes again.
        File[] classes = new File(work, "remapped/q").listFiles();
        assertNotNull(classes);
        for (File c : classes) {
            String constants = new String(Files.readAllBytes(c.toPath()), "ISO-8859-1");
            for (String name : gone) {
                assertTrue(c.getName() + " still names " + name, constants.indexOf(name) < 0);
            }
        }
        assertEquals(Arrays.asList(both[0]).toString().replace(", ", ",\n "),
                Arrays.asList(both[1]).toString().replace(", ", ",\n "));
        assertTrue("only " + both[0].length + " results", both[0].length >= atLeast);
    }

    private void compile(File jdk, File classes, List<String> files) throws Exception {
        List<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(classes.getAbsolutePath());
        args.add("-g");
        args.add("-nowarn");
        args.add("-encoding");
        args.add("UTF-8");
        if (jdk == null) {
            if (running() > 8) {
                args.add("--release");
                args.add(String.valueOf(release));
            }
            args.addAll(files);
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            assertNotNull("These tests need a JDK, not a JRE", javac);
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            int result = javac.run(null, null, errors, args.toArray(new String[args.size()]));
            assertEquals(new String(errors.toByteArray(), "UTF-8"), 0, result);
            return;
        }
        args.add(0, new File(jdk, "bin/javac").getAbsolutePath());
        args.add("--release");
        args.add(String.valueOf(release));
        args.addAll(files);
        String[] result = fork(args);
        assertEquals(result[1], "0", result[0]);
    }

    private String[] execute(File jdk, File classes, boolean device) throws Exception {
        if (jdk == null) {
            if (device) {
                // What the stand-ins run on: Codename One, with no display.
                com.codename1.compat.testing.HeadlessImplementation.install();
            }
            // Only the fixture's own classes are defined here; everything
            // else, the stand-ins included, is the test's class path.
            Loader loader = new Loader(getClass().getClassLoader(), classes);
            try {
                return (String[]) loader.loadClass("q.D").getMethod("texts").invoke(null);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
        List<String> args = new ArrayList<String>();
        args.add(new File(jdk, "bin/java").getAbsolutePath());
        args.add("-Djava.awt.headless=true");
        args.add("-cp");
        args.add(classes.getAbsolutePath() + File.pathSeparator + testClassPath());
        // Started through a class the remap never saw: every call in the
        // fixture is redirected, the one that would end the process included.
        args.add(RemapDifferentialMain.class.getName());
        args.add(device ? "device" : "jdk");
        String[] result = fork(args);
        assertEquals(result[1], "0", result[0]);
        String text = result[1];
        if (text.length() == 0) {
            return new String[0];
        }
        return text.substring(0, text.length() - 1).split("\n", -1);
    }

    private static final long FORK_TIMEOUT = 120000L;

    private String[] fork(List<String> args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        // A fork that never ends is a failure of this test, not of the build
        // that waits for it.
        Thread watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(FORK_TIMEOUT);
                    p.destroyForcibly();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "remap-differential-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = p.getInputStream();
            byte[] buffer = new byte[8192];
            for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                out.write(buffer, 0, n);
            }
            return new String[] {String.valueOf(p.waitFor()), new String(out.toByteArray(), "UTF-8")};
        } finally {
            watchdog.interrupt();
        }
    }

    private static final class Loader extends ClassLoader {
        private final File classes;

        Loader(ClassLoader parent, File classes) {
            super(parent);
            this.classes = classes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            File f = new File(classes, name.replace('.', '/') + ".class");
            if (!f.isFile()) {
                throw new ClassNotFoundException(name);
            }
            try {
                byte[] b = Files.readAllBytes(f.toPath());
                return defineClass(name, b, 0, b.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
