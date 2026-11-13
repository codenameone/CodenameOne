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
 * Mail, on both runtimes.
 *
 * The mail support is Java over the backend's own sockets and TLS, which are natives with two
 * implementations, and its encoders run on two class libraries. The unit tests in
 * maven/backend exercise the JVM half alone, so a STARTTLS upgrade that only works on the
 * JDK's sockets, or a Base64 line that wraps differently on the translated class library,
 * would pass all of them and ship.
 *
 * vm/backend/demo/mailcheck is self-contained: it starts an SMTP server of its own inside the
 * process, sends to it through the real transport, and compares the bytes that arrived. It
 * also runs the outbox against a database file and the security flows that end in a mail.
 * This requires the translated binary and the JVM to pass the same number of checks.
 */
class BackendMailTest {

    @Test
    @DisplayName("mail is encoded, sent, retried and captured the same on both runtimes")
    void bothRuntimesAgree() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the server-side backend is POSIX-only for now");
        }
        BackendTestSupport.require(Files.isDirectory(BackendTestSupport.backendDir()),
                "vm/backend is not present");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to build the backend");

        Path work = Files.createTempDirectory("backend-mailcheck");
        Path binary = work.resolve("mailcheck");
        // Standalone: the check uses neither the shared contract nor demo/common.
        String failure = BackendTestSupport.build("MailCheck", "demo/mailcheck", binary, jdk8,
                true);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        String translated = runTranslated(binary, work);
        assertOk("the translated runtime", translated);
        String local = runLocal(jdk8);
        assertOk("the local Java SE runtime", local);
        assertEquals(passedCount(local), passedCount(translated),
                "the two runtimes ran a different number of checks\n--- translated ---\n"
                        + translated + "\n--- local ---\n" + local);
    }

    private static String runTranslated(Path binary, Path work) throws Exception {
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        // The check keeps its database file in the working directory.
        run.directory(work.toFile());
        run.redirectErrorStream(true);
        Process p = run.start();
        boolean[] timedOut = new boolean[1];
        String output = BackendTestSupport.awaitOutput(p, 5, TimeUnit.MINUTES, timedOut);
        if (timedOut[0]) {
            fail("the translated mailcheck did not finish:\n" + output);
        }
        return output;
    }

    private static String runLocal(Path jdk8) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_BACKEND_JAVA", System.getProperty("java.home"));
        env.put("CN1_BACKEND_DEMO", "demo/mailcheck");
        env.put("CN1_BACKEND_JDBC_JARS", BackendTestSupport.jdbcJars());
        env.put("JDK_8_HOME", jdk8.toString());
        int[] status = new int[1];
        return BackendTestSupport.runBackendScript(
                new ArrayList<String>(Arrays.asList("./run-javase.sh", "com.demo.MailCheck")),
                env, 600, status);
    }

    private static void assertOk(String which, String output) {
        assertTrue(output.indexOf("MAILCHECK OK") >= 0, which + " failed:\n" + output);
        // A floor, so a check that stopped checking cannot agree with itself about nothing.
        assertTrue(passedCount(output) >= 200,
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
