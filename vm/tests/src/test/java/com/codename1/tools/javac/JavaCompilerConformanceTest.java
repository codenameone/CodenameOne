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
package com.codename1.tools.javac;

import com.codename1.tools.translator.CompilerHelper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The in-tree Java compiler (vm/JavaCompiler) held to javac:
 * <ul>
 * <li>every corpus program, compiled by javac and by the compiler, prints the same
 * output and exits the same way under {@code -Xverify:all} -- so the class files,
 * StackMapTable included, pass the HotSpot verifier;</li>
 * <li>every negative case reports javac's diagnostic on javac's line;</li>
 * <li>no prefix of a real program makes the compiler throw or hang -- the Playground
 * compiles what is in the editor while it is being typed.</li>
 * </ul>
 */
class JavaCompilerConformanceTest {
    private static final Path TESTS = Paths.get("..", "JavaCompiler", "tests");

    @Test
    void corpusBehavesLikeJavac() throws Exception {
        // Pattern switch and record patterns need javac 21; the class files are
        // version 61, so they also run on 21 or later.
        Path jdk = jdkAtLeast(21);
        List<Path> corpus = javaFiles(TESTS.resolve("corpus"));
        assertTrue(corpus.size() >= 10, "corpus not found under " + TESTS.toAbsolutePath());
        String javac = jdk.resolve("bin").resolve(CompilerHelper.executableName("javac")).toString();
        String java = jdk.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        List<String> failures = new ArrayList<String>();
        for (Path src : corpus) {
            String name = src.getFileName().toString().replace(".java", "");
            Path ref = Files.createTempDirectory("javac-ref-" + name);
            Path ours = Files.createTempDirectory("javac-ours-" + name);
            Result rc = run(Arrays.asList(javac, "-nowarn", "-d", ref.toString(), src.toString()), null);
            assertEquals(0, rc.exit, "javac rejected corpus program " + name + ": " + rc.out);
            Result oc = run(Arrays.asList(java, "-cp", System.getProperty("java.class.path"),
                    RuntimeLibraryCompile.class.getName(), ours.toString(), src.toString()), null);
            if (oc.exit != 0) {
                failures.add(name + ": the compiler rejected it:\n" + oc.out);
                continue;
            }
            Result refRun = run(Arrays.asList(java, "-Xverify:all", "-Xss512k", "-cp", ref.toString(), name), ref);
            Result ourRun = run(Arrays.asList(java, "-Xverify:all", "-Xss512k", "-cp", ours.toString(), name), ours);
            if (refRun.exit != ourRun.exit || !refRun.out.equals(ourRun.out)) {
                failures.add(name + ": javac's program printed\n" + refRun.out + "(exit " + refRun.exit
                        + ")\nours printed\n" + ourRun.out + "(exit " + ourRun.exit + ")");
            }
        }
        if (!failures.isEmpty()) {
            fail(failures.size() + " of " + corpus.size() + " corpus programs differ from javac:\n"
                    + String.join("\n\n", failures));
        }
    }

    @Test
    void negativeCasesReportJavacsDiagnostics() throws Exception {
        List<Path> cases = javaFiles(TESTS.resolve("negative"));
        assertTrue(cases.size() >= 20, "negative cases not found under " + TESTS.toAbsolutePath());
        List<String> failures = new ArrayList<String>();
        for (Path src : cases) {
            String text = new String(Files.readAllBytes(src), StandardCharsets.UTF_8);
            // A line "// expect: LINE: message" (after the license header): javac's line
            // and (part of) its wording.
            int at = text.indexOf("\n// expect: ");
            assertTrue(at >= 0, src + " has no expectation line");
            String expect = text.substring(at + "\n// expect: ".length(), text.indexOf('\n', at + 1));
            int line = Integer.parseInt(expect.substring(0, expect.indexOf(':')).trim());
            String message = expect.substring(expect.indexOf(':') + 1).trim();
            JavaCompiler.Result r = new JavaCompiler(RuntimeLibraryCompile.RUNTIME)
                    .addSource(src.getFileName().toString(), text).compile();
            boolean found = false;
            for (Diagnostic d : r.getDiagnostics()) {
                if (d.error && d.line == line && d.message.contains(message)) {
                    found = true;
                }
            }
            if (!found || r.isSuccess() || !r.getClasses().isEmpty()) {
                failures.add(src.getFileName() + ": expected line " + line + ": " + message + ", got "
                        + r.getDiagnostics() + (r.getClasses().isEmpty() ? "" : " and class files"));
            }
        }
        if (!failures.isEmpty()) {
            fail(String.join("\n", failures));
        }
    }

    @Test
    void noPrefixOfAProgramMakesTheCompilerThrowOrHang() throws Exception {
        final List<Path> corpus = javaFiles(TESTS.resolve("corpus"));
        assertFalse(corpus.isEmpty());
        assertTimeoutPreemptively(Duration.ofMinutes(10), () -> {
            for (Path src : corpus) {
                String text = new String(Files.readAllBytes(src), StandardCharsets.UTF_8);
                // Every 7th prefix: each one cuts a token, a string or a block in a
                // different place, and all of them would take minutes.
                for (int i = 0; i <= text.length(); i += 7) {
                    String prefix = text.substring(0, i);
                    try {
                        new JavaCompiler(RuntimeLibraryCompile.RUNTIME).addSource("P.java", prefix).compile();
                    } catch (Throwable t) {
                        throw new AssertionError(src.getFileName() + ", the first " + i + " characters: the compiler threw "
                                + t + " instead of reporting a diagnostic; text ends ..."
                                + prefix.substring(Math.max(0, i - 60)), t);
                    }
                }
            }
        });
    }

    private static Path jdkAtLeast(int major) {
        for (CompilerHelper.CompilerConfig c : CompilerHelper.getAvailableCompilers(String.valueOf(major))) {
            if (CompilerHelper.parseJavaMajor(c.jdkVersion) >= major) {
                return c.jdkHome;
            }
        }
        fail("This test needs a JDK " + major + " or later (JDK_21_HOME or JDK_25_HOME); none was found.");
        return null;
    }

    private static List<Path> javaFiles(Path dir) throws IOException {
        List<Path> out = new ArrayList<Path>();
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.java")) {
                for (Path p : ds) {
                    out.add(p);
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    private static final class Result {
        int exit;
        String out;
    }

    private static Result run(List<String> cmd, Path dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        if (dir != null) {
            pb.directory(dir.toFile());
        }
        Process p = pb.start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        InputStream in = p.getInputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        if (!p.waitFor(5, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            fail("timed out: " + String.join(" ", cmd));
        }
        Result r = new Result();
        r.exit = p.exitValue();
        r.out = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        return r;
    }
}
