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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Schema migrations, against real engines, on both runtimes.
 *
 * vm/backend/demo/migratecheck migrates a database and prints the history it wrote. The
 * migration engine is one body of code, but what it stands on is not: the checksum comes from
 * each runtime's own CRC32, the history table's DDL goes through each engine's wire client, and
 * the lock is each engine's own. So "it passed on the JVM against SQLite" says little, and this
 * requires the translated binary and the JVM to print the SAME history against every engine --
 * and every engine to print the same history as every other.
 *
 * SQLite always runs. PostgreSQL and MySQL run when CN1_DBCHECK_POSTGRES / CN1_DBCHECK_MYSQL
 * name a server, the same variables BackendDatabaseTest reads; their absence is a skip on a
 * developer machine and a FAILURE where the backend is required.
 */
class BackendMigrationTest {

    @Test
    @DisplayName("migrations write the same history on both runtimes and every engine")
    void everyEngineAndRuntimeAgree() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the server-side backend is POSIX-only for now");
        }
        BackendTestSupport.require(Files.isDirectory(BackendTestSupport.backendDir()),
                "vm/backend is not present");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to build the backend");

        List<String> urls = new ArrayList<String>();
        urls.add(":memory:");
        addIfSet(urls, "CN1_DBCHECK_POSTGRES");
        addIfSet(urls, "CN1_DBCHECK_MYSQL");
        if (urls.size() == 1 && BackendTestSupport.isRequired()) {
            fail("CN1_DBCHECK_POSTGRES and CN1_DBCHECK_MYSQL are unset, so only SQLite "
                    + "was exercised; the session lock and the failed-migration marker are "
                    + "server-engine behaviour");
        }

        Path work = Files.createTempDirectory("backend-migratecheck");
        Path binary = work.resolve("migratecheck");
        String failure = BackendTestSupport.build("MigrateCheck", "demo/migratecheck", binary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        String reference = null;
        for (String url : urls) {
            String translated = runTranslated(binary, url);
            assertOk("the translated runtime", url, translated);
            String local = runLocal(url, jdk8);
            assertOk("the local Java SE runtime", url, local);
            assertEquals(passedCount(translated), passedCount(local),
                    "the two runtimes ran a different number of checks against "
                            + redact(url) + "\n--- translated ---\n" + translated
                            + "\n--- local ---\n" + local);
            String history = history(translated);
            assertTrue(history.split("\n").length == 4,
                    "expected four HISTORY lines from " + redact(url) + ":\n" + translated);
            assertEquals(history(local), history,
                    "the two runtimes wrote a different history against " + redact(url));
            if (reference == null) {
                reference = history;
            } else {
                assertEquals(reference, history, redact(url)
                        + " recorded a different history than " + redact(urls.get(0)));
            }
        }
    }

    /** The HISTORY lines of a run, in order. */
    private static String history(String output) {
        StringBuilder lines = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.startsWith("HISTORY ")) {
                lines.append(line.trim()).append('\n');
            }
        }
        return lines.toString();
    }

    private static void addIfSet(List<String> urls, String name) {
        String value = System.getenv(name);
        if (value != null && value.length() > 0) {
            urls.add(value);
        }
    }

    private static String runTranslated(Path binary, String url) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_MIGRATECHECK_URL", url);
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().putAll(env);
        run.redirectErrorStream(true);
        Process p = run.start();
        boolean[] timedOut = new boolean[1];
        String output = BackendTestSupport.awaitOutput(p, 5, TimeUnit.MINUTES, timedOut);
        if (timedOut[0]) {
            fail("the translated migratecheck did not finish:\n" + output);
        }
        return output;
    }

    private static String runLocal(String url, Path jdk8) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_BACKEND_JAVA", System.getProperty("java.home"));
        env.put("CN1_BACKEND_DEMO", "demo/migratecheck");
        env.put("CN1_BACKEND_JDBC_JARS", BackendTestSupport.jdbcJars());
        env.put("CN1_MIGRATECHECK_URL", url);
        env.put("JDK_8_HOME", jdk8.toString());
        int[] status = new int[1];
        return BackendTestSupport.runBackendScript(
                new ArrayList<String>(Arrays.asList("./run-javase.sh", "com.demo.MigrateCheck")),
                env, 600, status);
    }

    private static void assertOk(String which, String url, String output) {
        assertTrue(output.indexOf("MIGRATECHECK OK") >= 0,
                which + " failed against " + redact(url) + ":\n" + output);
        assertTrue(passedCount(output) >= 20,
                which + " ran only " + passedCount(output) + " checks against "
                        + redact(url) + ":\n" + output);
    }

    /** A URL carries a password, and this output ends up in a CI log. */
    private static String redact(String url) {
        int at = url.indexOf('@');
        int scheme = url.indexOf("://");
        if (at < 0 || scheme < 0) {
            return url;
        }
        return url.substring(0, scheme + 3) + "***" + url.substring(at);
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
