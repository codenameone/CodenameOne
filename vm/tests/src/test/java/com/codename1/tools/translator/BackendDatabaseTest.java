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
 * The database layer, against real engines, on both runtimes.
 *
 * vm/backend/demo/dbcheck runs one body of assertions -- types, binding,
 * transactions, rollback, error recovery -- and the Database facade claims a
 * handler cannot tell which engine answered it. The only way to hold that claim
 * is to run the same body against every engine and require the same result, so
 * that is what this does, on the translated binary AND on the local Java SE arm.
 *
 * SQLite always runs; it needs nothing installed. PostgreSQL and MySQL run when
 * CN1_DBCHECK_POSTGRES / CN1_DBCHECK_MYSQL name a server (CI supplies both as
 * service containers). Their absence is a skip on a developer machine, and a
 * FAILURE where the backend is required, so a CI runner that quietly lost its
 * database cannot report green.
 */
class BackendDatabaseTest {

    @Test
    @DisplayName("every configured database engine answers the same on both runtimes")
    void everyEngineAgrees() throws Exception {
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
                    + "was exercised; the server engines are the ones with a wire "
                    + "protocol to get wrong");
        }

        Path work = Files.createTempDirectory("backend-dbcheck");
        Path binary = work.resolve("dbcheck");
        String failure = BackendTestSupport.build("DbCheck", "demo/dbcheck", binary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        for (String url : urls) {
            String translated = runTranslated(binary, url);
            assertOk("the translated runtime", url, translated);
            String local = runLocal(url, jdk8);
            assertOk("the local Java SE runtime", url, local);
            // Not just "both said OK": the same number of checks has to have run,
            // or one runtime skipping half of them would still pass.
            assertEquals(passedCount(translated), passedCount(local),
                    "the two runtimes ran a different number of checks against "
                            + redact(url) + "\n--- translated ---\n" + translated
                            + "\n--- local ---\n" + local);
        }
    }

    private static void addIfSet(List<String> urls, String name) {
        String value = System.getenv(name);
        if (value != null && value.length() > 0) {
            urls.add(value);
        }
    }

    private static String runTranslated(Path binary, String url) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_DBCHECK_URL", url);
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().putAll(env);
        run.redirectErrorStream(true);
        Process p = run.start();
        String output = BackendTestSupport.readFully(p.getInputStream());
        if (!p.waitFor(5, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            fail("the translated dbcheck did not finish:\n" + output);
        }
        return output;
    }

    private static String runLocal(String url, Path jdk8) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_BACKEND_JAVA", System.getProperty("java.home"));
        env.put("CN1_BACKEND_DEMO", "demo/dbcheck");
        env.put("CN1_BACKEND_JDBC_JARS", BackendTestSupport.jdbcJars());
        env.put("CN1_DBCHECK_URL", url);
        env.put("JDK_8_HOME", jdk8.toString());
        int[] status = new int[1];
        return BackendTestSupport.runBackendScript(
                new ArrayList<String>(Arrays.asList("./run-javase.sh", "com.demo.DbCheck")),
                env, 600, status);
    }

    private static void assertOk(String which, String url, String output) {
        assertTrue(output.indexOf("DBCHECK OK") >= 0,
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
