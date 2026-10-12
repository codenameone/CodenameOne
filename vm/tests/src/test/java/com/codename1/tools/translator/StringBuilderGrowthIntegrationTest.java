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

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the issue-5963 reproducer through the actual generated native runtime.
 * CN1_TEST_EXTRA_CFLAGS can add AddressSanitizer for allocation-lifetime evidence. */
class StringBuilderGrowthIntegrationTest {
    @Test
    void concurrentGrowthDoesNotCorruptNativeStorage() throws Exception {
        Parser.cleanup();
        Path root = Files.createTempDirectory("builder-growth-");
        Path api = Files.createDirectories(root.resolve("api"));
        Path classes = Files.createDirectories(root.resolve("classes"));
        CompilerHelper.CompilerConfig config = null;
        for (String target : new String[]{"11", "17", "21", "1.8"}) {
            for (CompilerHelper.CompilerConfig candidate : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(candidate)) { config = candidate; break; }
            }
            if (config != null) break;
        }
        assertNotNull(config);
        CompilerHelper.compileJavaAPI(api, config);
        Path source = root.resolve("StringBuilderGrowthApp.java");
        try (java.io.InputStream in = getClass().getResourceAsStream("StringBuilderGrowthApp.java")) {
            assertNotNull(in);
            Files.copy(in, source);
        }
        List<String> args = new ArrayList<>(Arrays.asList("-source", config.targetVersion,
                "-target", config.targetVersion, CompilerHelper.useClasspath(config) ? "-classpath" : "-bootclasspath",
                api.toString(), "-d", classes.toString(), source.toString()));
        assertEquals(0, CompilerHelper.compile(config.jdkHome, args));
        CompilerHelper.copyDirectory(api, classes);
        CleanTargetIntegrationTest.runTranslator(classes, root.resolve("output"), "StringBuilderGrowthApp");
        Path dist = root.resolve("output/dist");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(dist.resolve("CMakeLists.txt"), "StringBuilderGrowthApp-src");
        Path build = dist.resolve("build");
        List<String> cmake = new ArrayList<>(Arrays.asList("cmake", "-S", dist.toString(), "-B", build.toString(),
                "-DCMAKE_BUILD_TYPE=RelWithDebInfo", "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"));
        cmake.addAll(CompilerHelper.extraCFlagArgs());
        CleanTargetIntegrationTest.runCommand(cmake, dist);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", build.toString(), "--parallel", "4"), dist);
        Path log = root.resolve("run.log");
        Process process = new ProcessBuilder(build.resolve("StringBuilderGrowthApp").toString())
                .directory(build.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(90, TimeUnit.SECONDS), "Native builder test timed out: " + log);
            String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("BUILDER_GROWTH_OK"), output);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
