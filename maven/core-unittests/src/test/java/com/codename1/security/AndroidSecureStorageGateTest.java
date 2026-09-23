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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the Android gate methods with real files and fault-injected writes, without Android. */
class AndroidSecureStorageGateTest {
    @TempDir
    static Path temporary;
    private static URLClassLoader loader;
    private static Class<?> harness;

    @BeforeAll
    static void compileProductionMethods() throws Exception {
        String android = new String(Files.readAllBytes(Paths.get("../../Ports/Android/src/"
                + "com/codename1/impl/android/AndroidSecureStorage.java")), StandardCharsets.UTF_8);
        StringBuilder methods = new StringBuilder();
        for (String signature : new String[] {"private String createUnderGate(",
                "private boolean removeUnderGate(", "private void resetPlainKey(",
                "private static void restoreGateMark(", "public boolean set("}) {
            int start = android.indexOf(signature);
            assertTrue(start >= 0, "Missing production method: " + signature);
            int open = android.indexOf('{', start);
            int depth = 1;
            int end = open + 1;
            while (depth > 0 && end < android.length()) {
                char c = android.charAt(end++);
                if (c == '{') depth++;
                if (c == '}') depth--;
            }
            assertEquals(0, depth, "Unterminated production method: " + signature);
            methods.append(android.substring(start, end)).append('\n');
        }
        // Replace only file construction so an IOException can land immediately after the
        // real byte becomes visible. All production control flow and rollback code is retained.
        String injected = methods.toString().replace("new java.io.RandomAccessFile(gate, \"rw\")",
                "openGate(gate)");
        Path template = Paths.get("src/test/resources/AndroidGateHarness.java.template");
        String fixture = new String(Files.readAllBytes(template), StandardCharsets.UTF_8)
                .replace("/* PRODUCTION_METHODS */", injected);
        Path source = temporary.resolve("AndroidGateHarness.java");
        Files.write(source, fixture.getBytes(StandardCharsets.UTF_8));
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", temporary.toString(), source.toString()));
        loader = new URLClassLoader(new URL[] {temporary.toUri().toURL()}, null);
        harness = loader.loadClass("AndroidGateHarness");
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) loader.close();
    }

    private void run(String name) throws Exception {
        harness.getMethod(name).invoke(null);
    }

    @Test
    void failedCreationRestoresBothEmptyAndRemovedGates() throws Exception {
        run("failedCreation");
    }

    @Test
    void failedSetRestoresBothThePreviousValueAndMark() throws Exception {
        run("failedSet");
    }

    @Test
    void partialTombstoneWritesPreserveThePreviousMarkAndValue() throws Exception {
        run("failedTombstone");
    }

    @Test
    void refusedAndThrowingPreferenceCommitsRestoreTheMark() throws Exception {
        run("failedPreferenceCommit");
    }

    @Test
    void resetKeepsValuesForGatesWhoseClearFailed() throws Exception {
        run("failedReset");
    }
}
