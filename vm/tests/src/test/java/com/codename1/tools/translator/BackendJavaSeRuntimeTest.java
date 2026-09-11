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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same runtime self-test, run against the LOCAL Java SE arm of the backend.
 *
 * vm/backend/src is one copy of the protocol logic and vm/backend/impl holds the
 * two implementations under it -- natives for the translated target, JDK classes
 * for the local dev loop. A dev loop that behaves differently from production is
 * worse than no dev loop, so the same assertions run on both, and the counts have
 * to match: {@link BackendRuntimeSelfTest} is the translated half of this pair.
 */
class BackendJavaSeRuntimeTest {

    @Test
    @DisplayName("the local Java SE runtime passes the same checks as the translated one")
    void javaSeSelfTest() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the server-side backend is POSIX-only for now");
        }
        BackendTestSupport.require(Files.isDirectory(BackendTestSupport.backendDir()),
                "vm/backend is not present");

        Path work = Files.createTempDirectory("backend-javase-selftest");
        Map<String, String> env = new HashMap<String, String>();
        // Not needed to RUN the local arm -- it is JDK classes all the way down --
        // but run-javase.sh generates the shared contract when it is missing, and
        // that goes through maven and a JDK 8. Passed when there is one; the local
        // loop still works without it once gen/ exists.
        Path jdk8 = BackendTestSupport.findJdk8();
        if (jdk8 != null) {
            env.put("JDK_8_HOME", jdk8.toString());
        }
        // The test's own JVM, so this does not depend on what is on PATH.
        env.put("CN1_BACKEND_JAVA", System.getProperty("java.home"));
        env.put("CN1_BACKEND_DEMO", "demo/selftest");
        env.put("CN1_BACKEND_JDBC_JARS", BackendTestSupport.jdbcJars());
        // A real file: an in-memory database cannot be pooled, since every
        // connection would get its own.
        env.put("CN1_SELFTEST_DB", work.resolve("pool.db").toString());
        // Small enough that the response-size check can serve one byte past it
        // without serving the 64MB default. Every other web check here fetches
        // a few hundred bytes, so the bound is invisible to them.
        env.put("CN1_WEB_MAX_RESPONSE_MB", "1");
        if (System.getenv("CN1_SELFTEST_NETWORK") != null) {
            env.put("CN1_SELFTEST_NETWORK", "1");
            String bundle = caBundle();
            if (bundle != null) {
                env.put("CN1_SELFTEST_CA_BUNDLE", bundle);
            }
        }

        List<String> command = new ArrayList<String>(Arrays.asList(
                "./run-javase.sh", "com.demo.SelfTest"));
        int[] status = new int[1];
        String output = BackendTestSupport.runBackendScript(command, env, 600, status);

        // generate-contract.sh needs codenameone-core and the maven plugin in the
        // local repository, and says so by name rather than letting maven fail on an
        // artifact nobody asked for. A job that has not installed them has an
        // environment gap rather than a broken runtime, so it is skipped the same way
        // the database tests skip without their service containers -- and
        // CN1_BACKEND_REQUIRED still turns that skip into a failure where the backend
        // is meant to be exercised.
        if (output.indexOf("Install it first:") >= 0) {
            BackendTestSupport.skipOrFail(
                    "the local Maven repository has no codenameone-core to build the "
                            + "contract against:\n" + output);
        }

        assertTrue(output.indexOf("SELFTEST OK") >= 0,
                "the local Java SE runtime reported failures:\n" + output);
        assertEquals(0, status[0], output);
        int passed = passedCount(output);
        assertTrue(passed >= 80,
                "expected the full set of checks, only " + passed + " ran:\n" + output);
    }

    private static int passedCount(String output) {
        int at = output.indexOf("passed=");
        if (at < 0) {
            return -1;
        }
        int end = output.indexOf(' ', at);
        try {
            return Integer.parseInt(output.substring(at + "passed=".length(),
                    end < 0 ? output.length() : end).trim());
        } catch (NumberFormatException err) {
            return -1;
        }
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
