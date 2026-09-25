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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StreamApiIntegrationTest {

    @Test
    void streamEdgeCasesMatchBetweenJavaSEAndParparVM() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("stream-integration-sources");
        Path classesDir = Files.createTempDirectory("stream-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Path source = sourceDir.resolve("StreamEdgeApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for stream integration test");
        }

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> compileArgs = new ArrayList<>();
        compileArgs.add("-source");
        compileArgs.add(config.targetVersion);
        compileArgs.add("-target");
        compileArgs.add(config.targetVersion);
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-classpath");
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(source.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "StreamEdgeApp should compile");

        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        String javaResult = extractResultLine(javaOutput);
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce a RESULT line. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("stream-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "StreamEdgeApp");

        Path distDir = outputDir.resolve("dist");
        String generated = new String(Files.readAllBytes(distDir.resolve("StreamEdgeApp-src/StreamEdgeApp.c")), StandardCharsets.UTF_8);
        assertTrue(generated.split("fused array stream", -1).length >= 9,
                "All eight immediate helper pipelines must actually lower to C");
        for (String helper : Arrays.asList("fusedMatch", "fusedCount", "fusedForEach", "fusedAll",
                "fusedNone", "fusedZero", "fusedThrow", "fusedMutation")) {
            int start = generated.indexOf(" StreamEdgeApp_" + helper + "_");
            assertTrue(start >= 0);
            int end = generated.indexOf("\nJAVA_", start);
            String body = generated.substring(start, end < 0 ? generated.length() : end);
            assertTrue(body.contains("fused array stream"), helper);
            assertFalse(body.contains("lambda$factory"), helper + " must not allocate callback objects");
            assertFalse(body.contains("virtual_java_util_stream"), helper + " must not dispatch stream stages");
        }
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "StreamEdgeApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("StreamEdgeApp");
        String parparOutput = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        String parparResult = extractResultLine(parparOutput);
        assertTrue(parparResult.startsWith("RESULT="), "ParparVM execution should produce a RESULT line. Output: " + parparOutput);

        assertEquals(javaResult, parparResult,
                "JavaSE and ParparVM should emit identical result lines for stream edge cases");
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = StreamApiIntegrationTest.class.getResourceAsStream("/com/codename1/tools/translator/StreamEdgeApp.java");
        assertNotNull(in, "StreamEdgeApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir) throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve("java").toString();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaExe += ".exe";
        }

        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "StreamEdgeApp"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "JVM run should exit cleanly. Output: " + output);
        return output;
    }

    private String extractResultLine(String output) {
        for (String line : output.split("\\R")) {
            if (line.startsWith("RESULT=")) {
                return line.trim();
            }
        }
        return "";
    }

    private CompilerHelper.CompilerConfig selectCompiler() {
        String[] preferredTargets = {"11", "17", "21", "25", "1.8"};
        for (String target : preferredTargets) {
            List<CompilerHelper.CompilerConfig> configs = CompilerHelper.getAvailableCompilers(target);
            for (CompilerHelper.CompilerConfig config : configs) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        return null;
    }
}
