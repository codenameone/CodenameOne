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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The security layer's cryptography, on both runtimes.
 *
 * Every primitive behind it is one Java signature over two implementations -- OpenSSL in the
 * translated binary, the JDK's providers on the JVM -- and the keys and tokens built on them
 * run on two class libraries. The unit tests in maven/backend exercise the JVM half alone, so
 * a native that returned the wrong digest, or padded a signature differently, would pass all
 * of them and ship.
 *
 * vm/backend/demo/authcheck prints what each primitive produced for fixed inputs, one VALUE
 * line each: digests, HMACs, derived keys, AES-GCM ciphertexts, the deterministic RSA
 * signatures and whole tokens signed with them. This requires the translated binary and the
 * JVM to print the same lines. The randomized schemes -- RSASSA-PSS and ECDSA -- cannot be
 * compared that way; for those both runtimes verify signatures the openssl command line made
 * once, and each verifies what it signs itself.
 */
class BackendSecurityTest {

    @Test
    @DisplayName("every primitive and token comes out the same on both runtimes")
    void bothRuntimesAgree() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the server-side backend is POSIX-only for now");
        }
        BackendTestSupport.require(Files.isDirectory(BackendTestSupport.backendDir()),
                "vm/backend is not present");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to build the backend");

        Path work = Files.createTempDirectory("backend-authcheck");
        Path binary = work.resolve("authcheck");
        String failure = BackendTestSupport.build("AuthCheck", "demo/authcheck", binary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        String translated = runTranslated(binary);
        assertOk("the translated runtime", translated);
        String local = runLocal(jdk8);
        assertOk("the local Java SE runtime", local);
        assertEquals(passedCount(local), passedCount(translated),
                "the two runtimes ran a different number of checks\n--- translated ---\n"
                        + translated + "\n--- local ---\n" + local);
        String values = values(translated);
        // A floor, so a demo that stopped printing cannot agree with itself about nothing.
        assertTrue(values.split("\n").length >= 70,
                "expected at least 70 VALUE lines from the translated runtime:\n" + translated);
        assertEquals(values(local), values,
                "the two runtimes computed different values for the same inputs");
    }

    /** The VALUE lines of a run, in order. */
    private static String values(String output) {
        StringBuilder lines = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.startsWith("VALUE ")) {
                lines.append(line.trim()).append('\n');
            }
        }
        return lines.toString();
    }

    private static String runTranslated(Path binary) throws Exception {
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.redirectErrorStream(true);
        Process p = run.start();
        boolean[] timedOut = new boolean[1];
        String output = BackendTestSupport.awaitOutput(p, 5, TimeUnit.MINUTES, timedOut);
        if (timedOut[0]) {
            fail("the translated authcheck did not finish:\n" + output);
        }
        return output;
    }

    private static String runLocal(Path jdk8) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_BACKEND_JAVA", System.getProperty("java.home"));
        env.put("CN1_BACKEND_DEMO", "demo/authcheck");
        env.put("CN1_BACKEND_JDBC_JARS", BackendTestSupport.jdbcJars());
        env.put("JDK_8_HOME", jdk8.toString());
        int[] status = new int[1];
        return BackendTestSupport.runBackendScript(
                new ArrayList<String>(Arrays.asList("./run-javase.sh", "com.demo.AuthCheck")),
                env, 600, status);
    }

    private static void assertOk(String which, String output) {
        assertTrue(output.indexOf("AUTHCHECK OK") >= 0, which + " failed:\n" + output);
        assertTrue(passedCount(output) >= 100,
                which + " ran only " + passedCount(output) + " checks:\n" + output);
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
}
