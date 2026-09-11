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
package com.codename1.tools.translator;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The runtime self-test, run against the TRANSLATED arm of the backend.
 *
 * The Java SE half of this pair, {@link BackendJavaSeRuntimeTest}, has always
 * described itself as one of two -- but the class it named did not exist, so
 * every one of these assertions ran only on the JVM. That is the arm that does
 * NOT ship: the natives under vm/backend/impl/parparvm, the GC, and the buffer
 * handling are exactly what a JDK-classes run cannot speak for. This is the
 * other half.
 */
class BackendRuntimeSelfTest {

    @Test
    @DisplayName("the translated runtime passes the same checks as the Java SE one")
    void translatedSelfTest() throws Exception {
        if (CompilerHelper.isWindows()) {
            BackendTestSupport.skipOrFail("the server-side backend is POSIX-only for now");
        }
        Path backend = Paths.get("..", "backend").normalize().toAbsolutePath();
        BackendTestSupport.require(Files.isDirectory(backend), "vm/backend is not present");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to compile the backend");

        Path work = Files.createTempDirectory("backend-translated-selftest");
        Path binary = work.resolve("selftest");

        // The real build script, for the same reason the other native tests use
        // it: a test that builds differently from the product tests something
        // else.
        ProcessBuilder build = new ProcessBuilder("./build.sh", "SelfTest", "com.demo",
                binary.toString());
        build.directory(backend.toFile());
        build.environment().put("JDK_8_HOME", jdk8.toString());
        build.environment().put("JAVA_HOME", jdk8.toString());
        build.environment().put("CN1_BACKEND_DEMO", "demo/selftest");
        build.redirectErrorStream(true);
        Process built = build.start();
        String buildLog = readAll(built);
        if (!built.waitFor(20, TimeUnit.MINUTES) || built.exitValue() != 0
                || !Files.isExecutable(binary)) {
            BackendTestSupport.skipOrFail("could not build the self-test binary:\n"
                    + tail(buildLog));
        }

        ProcessBuilder run = new ProcessBuilder(binary.toString());
        // A real file: an in-memory database cannot be pooled, because every
        // connection would get one of its own.
        run.environment().put("CN1_SELFTEST_DB", work.resolve("pool.db").toString());
        if (System.getenv("CN1_SELFTEST_NETWORK") != null) {
            run.environment().put("CN1_SELFTEST_NETWORK", "1");
            String bundle = caBundle();
            if (bundle != null) {
                run.environment().put("CN1_SELFTEST_CA_BUNDLE", bundle);
            }
        }
        run.redirectErrorStream(true);
        Process p = run.start();
        String output = readAll(p);
        boolean ended = p.waitFor(10, TimeUnit.MINUTES);
        if (!ended) {
            p.destroyForcibly();
        }
        assertTrue(ended, "the self-test never finished:\n" + tail(output));
        assertTrue(output.indexOf("SELFTEST OK") >= 0,
                "the translated runtime failed its own checks:\n" + tail(output));
        // The same floor the Java SE half asserts, so a run that quietly stopped
        // early cannot pass by printing OK after three checks.
        int passed = passedCount(output);
        assertTrue(passed >= 80,
                "expected the full set of checks, only " + passed + " ran:\n" + tail(output));
    }

    /** Reads "passed=N" out of the self-test's own summary line. */
    private static int passedCount(String output) {
        int at = output.indexOf("passed=");
        if (at < 0) {
            return 0;
        }
        int end = at + "passed=".length();
        while (end < output.length() && Character.isDigit(output.charAt(end))) {
            end++;
        }
        try {
            return Integer.parseInt(output.substring(at + "passed=".length(), end));
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private static String readAll(Process p) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        java.io.InputStream in = p.getInputStream();
        for (;;) {
            int n = in.read(buffer);
            if (n < 0) {
                break;
            }
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    private static String tail(String text) {
        return text.length() > 3000 ? text.substring(text.length() - 3000) : text;
    }

    /**
     * A PEM bundle the self-test's CA-rotation check can copy and rotate.
     *
     * <p>DERIVED rather than merely passed through, because a check that needs an
     * environment variable nobody sets is a check that never runs -- it reports
     * "skipped" and reads as green. The variable still wins when it is set, for a
     * host whose bundle is somewhere else.
     */
    private static String caBundle() {
        String named = System.getenv("CN1_SELFTEST_CA_BUNDLE");
        if (named != null) {
            return named;
        }
        String[] candidates = {
            "/etc/ssl/certs/ca-certificates.crt",  // Debian, Ubuntu
            "/etc/pki/tls/certs/ca-bundle.crt",    // RHEL, Fedora
            "/etc/ssl/cert.pem",                   // macOS, Alpine
        };
        for (String candidate : candidates) {
            if (java.nio.file.Files.isReadable(java.nio.file.Paths.get(candidate))) {
                return candidate;
            }
        }
        return null;
    }

}
