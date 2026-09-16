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
package com.codename1.security;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the browser store's Java update path with failed legacy cleanup. */
class JavascriptSecureStorageRollbackTest {
    @TempDir
    Path temporary;

    @Test
    void failedLegacyCleanupPreservesThePriorValueAndLaterWriters() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("../../Ports/JavaScriptPort/"
                + "src/main/java/com/codename1/impl/html5/HTML5SecureStorage.java")),
                StandardCharsets.UTF_8);
        StringBuilder methods = new StringBuilder();
        for (String signature : new String[] {"public boolean set(",
                "private void rollbackReplacement(", "private void releaseMigrationGate("}) {
            int start = source.indexOf(signature);
            assertTrue(start >= 0, "Missing production method: " + signature);
            int end = source.indexOf('{', start) + 1;
            int depth = 1;
            while (depth > 0 && end < source.length()) {
                char c = source.charAt(end++);
                if (c == '{') depth++;
                if (c == '}') depth--;
            }
            assertEquals(0, depth, "Unterminated production method: " + signature);
            methods.append(source.substring(start, end)).append('\n');
        }
        String fixture = new String(Files.readAllBytes(Paths.get(
                "src/test/resources/JavascriptStorageHarness.java.template")), StandardCharsets.UTF_8)
                .replace("/* PRODUCTION_METHODS */", methods.toString());
        Path file = temporary.resolve("JavascriptStorageHarness.java");
        Files.write(file, fixture.getBytes(StandardCharsets.UTF_8));
        assertNotNull(ToolProvider.getSystemJavaCompiler());
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", temporary.toString(), file.toString()));
        try (URLClassLoader loader = new URLClassLoader(new URL[] {temporary.toUri().toURL()}, null)) {
            loader.loadClass("JavascriptStorageHarness").getMethod("verify").invoke(null);
        }
    }
}
