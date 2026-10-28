/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Runs `test_cn1dbapi.py` under CPython: the SQL scanning and value parsing
/// of the bundled `_cn1dbapi` driver, which needs a server database to run
/// transpiled but whose pure functions import under CPython with a stub
/// `_cn1db`. Skipped where no `python3` is installed, except on CI.
class Cn1DbApiTest {
    @Test
    void pureFunctions() throws IOException, InterruptedException {
        Path site = Paths.get("src/main/resources/com/codename1/python/site/sqlalchemy").toAbsolutePath();
        Path stubs = Paths.get("src/test/resources/cn1dbapi").toAbsolutePath();
        Path log = Files.createTempFile("cn1dbapi", ".log");
        Process p;
        try {
            p = new ProcessBuilder("python3", "-B", stubs.resolve("test_cn1dbapi.py").toString(),
                    site.toString(), stubs.toString()).redirectErrorStream(true)
                    .redirectOutput(log.toFile()).start();
        } catch (IOException e) {
            assumeTrue(System.getenv("CI") != null, "python3 is not installed");
            throw e;
        }
        boolean done = p.waitFor(120, TimeUnit.SECONDS);
        String out = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        Files.delete(log);
        assertEquals("ok" + System.lineSeparator(), done ? out : "timed out" + File.separator + out);
        assertEquals(0, p.exitValue());
    }
}
