/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.dart.transpiler.harness;

import com.codename1.dart.transpiler.analyze.Program;
import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.codegen.JavaEmitter;
import com.codename1.dart.transpiler.parser.AstBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Shared helpers for the golden/compile/behavioral test harness.
 */
public final class TestSupport {

    public static final String PKG = "com.codename1.generated.flutter";

    private TestSupport() {
    }

    public static class Result {
        public final List<GeneratedFile> files;
        public final Diagnostics diags;

        Result(List<GeneratedFile> files, Diagnostics diags) {
            this.files = files;
            this.diags = diags;
        }
    }

    /** Transpiles a set of in-memory dart sources (fileName -> content). */
    public static Result transpile(String[][] sources) {
        return transpile(sources, java.util.Collections.<File>emptyList());
    }

    /**
     * Transpiles against the Dart stubs published by {@code stubEntries} — the jars or
     * class directories an app would have on its classpath.
     *
     * <p>This is how the real mojo resolves the API surface, and a case that touches a
     * widget needs it: the embedded stub set is only the built-ins plus material, so
     * anything a runtime module declares for itself (Curves, the animation family) is
     * unresolvable without the jar that declares it.</p>
     */
    public static Result transpile(String[][] sources, java.util.List<File> stubEntries) {
        Diagnostics diags = new Diagnostics();
        AstBuilder builder = new AstBuilder(diags);
        Program program = new Program();
        for (String[] s : sources) {
            program.add(builder.parse(s[0], s[1]));
        }
        // loadFromClasspath falls back to the embedded set when nothing contributes, so an
        // empty list keeps the previous behaviour exactly.
        StubRegistry stubs = StubRegistry.loadFromClasspath(stubEntries, diags);
        JavaEmitter emitter = new JavaEmitter(program, stubs, diags, PKG);
        return new Result(emitter.emit(), diags);
    }

    public static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    public static void write(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /** Finds JAVA17_HOME (env or tools/env.sh defaults); null if unavailable. */
    public static File java17Home() {
        String env = System.getenv("JAVA17_HOME");
        if (env != null && new File(env, "bin/javac").exists()) {
            return new File(env);
        }
        return null;
    }

    /** Locates a sibling-module or local-repo jar; null if not built yet. */
    public static File findJar(String artifactId) {
        String version = "8.0-SNAPSHOT";
        File[] candidates = new File[] {
                new File("../" + moduleDir(artifactId) + "/target/" + artifactId + "-" + version + ".jar"),
                new File("/tmp/cn1-local-repo/com/codenameone/" + artifactId + "/" + version + "/"
                        + artifactId + "-" + version + ".jar"),
                new File(System.getProperty("user.home"),
                        ".m2/repository/com/codenameone/" + artifactId + "/" + version + "/"
                                + artifactId + "-" + version + ".jar"),
        };
        for (File f : candidates) {
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private static String moduleDir(String artifactId) {
        if (artifactId.equals("codenameone-dart-runtime")) {
            return "dart-runtime";
        }
        if (artifactId.equals("codenameone-flutter-runtime")) {
            return "flutter-runtime";
        }
        if (artifactId.equals("codenameone-core")) {
            return "core";
        }
        return artifactId;
    }

    /** Runs a process, returns [exitCode, stdout+stderr]. */
    public static Object[] run(List<String> cmd, File dir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = p.getInputStream().read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        int code = p.waitFor();
        return new Object[] {code, new String(out.toByteArray(), StandardCharsets.UTF_8)};
    }
}
