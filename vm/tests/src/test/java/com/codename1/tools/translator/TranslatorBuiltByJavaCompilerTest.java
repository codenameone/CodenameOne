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
package com.codename1.tools.translator;

import com.codename1.tools.javac.RuntimeLibraryCompile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The strongest test the in-tree Java compiler (vm/JavaCompiler) has: it compiles the
 * whole translator -- a hundred files of real, generic, inner-class-heavy code -- and
 * that translator must then emit byte-identical output to the javac-built one. Each
 * miscompile it has caught (resolving to an inaccessible overload, casting a generic
 * Integer to Long before widening) turned into a crash or a wrong bundle here while the
 * corpus passed.
 */
class TranslatorBuiltByJavaCompilerTest {

    @Test
    void translatorCompiledByTheInTreeCompilerEmitsTheSameJavascript() throws Exception {
        Path jdk = null;
        for (CompilerHelper.CompilerConfig c : CompilerHelper.getAvailableCompilers("17")) {
            if (CompilerHelper.parseJavaMajor(c.jdkVersion) >= 17) {
                jdk = c.jdkHome;
                break;
            }
        }
        if (jdk == null) {
            fail("This test needs a JDK 17 or later (JDK_17_HOME, JDK_21_HOME or JDK_25_HOME); none was found.");
        }
        String javaExe = jdk.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();

        // 1. The translator's sources, compiled by the in-tree compiler.
        List<String> sources = new ArrayList<String>();
        try (Stream<Path> paths = Files.walk(Paths.get("..", "ByteCodeTranslator", "src"))) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> sources.add(p.toString()));
        }
        assertTrue(sources.size() > 50, "translator sources not found");
        Path ours = Files.createTempDirectory("translator-by-javac");
        List<String> cmd = new ArrayList<String>(Arrays.asList(javaExe, "-Xss8m", "-cp", System.getProperty("java.class.path"),
                RuntimeLibraryCompile.class.getName(), ours.toString()));
        cmd.addAll(sources);
        Result compiled = run(cmd);
        assertEquals(0, compiled.exit, "the in-tree compiler rejected the translator:\n" + compiled.out);

        // 2. A fixture application, compiled once against JavaAPI.
        CompilerHelper.CompilerConfig config = null;
        for (CompilerHelper.CompilerConfig c : CompilerHelper.getAvailableCompilers("1.8")) {
            if (CompilerHelper.isJavaApiCompatible(c)) {
                config = c;
                break;
            }
        }
        assertTrue(config != null, "no JDK can compile against JavaAPI");
        Path src = Files.createTempDirectory("tbjc-src");
        Path classes = Files.createTempDirectory("tbjc-classes");
        Path javaApi = Files.createTempDirectory("tbjc-javaapi");
        Files.write(src.resolve("JsFinallyRethrowApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsFinallyRethrowApp.java").getBytes(StandardCharsets.UTF_8));
        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, src, classes, javaApi);

        // 3. Both translators over it. Ours goes first on the class path; resources (the
        //    runtime JavaScript, headers) come from the javac-built translator either way.
        String reference = Paths.get(ByteCodeTranslator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Path outRef = Files.createTempDirectory("tbjc-ref");
        Path outOurs = Files.createTempDirectory("tbjc-ours");
        String[] tail = {"javascript", classes.toString(), "", "JsFinallyRethrowApp", "com.example", "JsFinallyRethrowApp", "1.0", "ios", "none"};
        Result r1 = run(translate(javaExe, reference, outRef, tail));
        assertEquals(0, r1.exit, "javac-built translator failed:\n" + r1.out);
        Result r2 = run(translate(javaExe, ours + java.io.File.pathSeparator + reference, outOurs, tail));
        assertEquals(0, r2.exit, "translator built by the in-tree compiler failed:\n" + r2.out);

        TreeMap<String, byte[]> a = tree(outRef);
        TreeMap<String, byte[]> b = tree(outOurs);
        assertTrue(a.size() > 10, "the reference translation is vacuous: " + a.keySet());
        assertEquals(a.keySet(), b.keySet(), "the two translations emitted different files");
        List<String> differ = new ArrayList<String>();
        for (String k : a.keySet()) {
            // The suspension report names one cause per method, picked by iterating an
            // identity-hashed set -- an order a different (equally correct) bytecode shifts.
            if (k.endsWith("-suspension-report.txt")) {
                continue;
            }
            if (!Arrays.equals(a.get(k), b.get(k))) {
                differ.add(k);
            }
        }
        assertTrue(differ.isEmpty(), "files differ between the two translators: " + differ);
    }

    private static List<String> translate(String java, String cp, Path out, String[] tail) {
        List<String> cmd = new ArrayList<String>(Arrays.asList(java, "-cp", cp, ByteCodeTranslator.class.getName()));
        for (String t : tail) {
            cmd.add(t.isEmpty() ? out.toString() : t);
        }
        return cmd;
    }

    private static TreeMap<String, byte[]> tree(Path root) throws Exception {
        TreeMap<String, byte[]> out = new TreeMap<String, byte[]>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                if (Files.isRegularFile(p)) {
                    out.put(root.relativize(p).toString(), Files.readAllBytes(p));
                }
            }
        }
        return out;
    }

    private static final class Result {
        int exit;
        String out;
    }

    private static Result run(List<String> cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        InputStream in = p.getInputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        if (!p.waitFor(10, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            fail("timed out: " + cmd.get(0));
        }
        Result r = new Result();
        r.exit = p.exitValue();
        String s = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        r.out = s.length() > 6000 ? s.substring(s.length() - 6000) : s;
        return r;
    }
}
