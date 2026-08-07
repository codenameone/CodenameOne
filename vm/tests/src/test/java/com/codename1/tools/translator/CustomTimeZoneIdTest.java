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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins vm/JavaAPI's custom GMT-offset id parsing against the JDK.
 *
 * java.util.TimeZone defines the custom-id syntax on GMT alone and in fixed
 * field widths. This parser was looser -- it accepted "UTC+5", "GMT+1:2" and
 * six-digit "GMT+013000" -- so the same id produced one offset here and a plain
 * GMT elsewhere. Since the JavaSE and Android ports reach the JDK's parser
 * directly, that is a cross-port split for identical application code, which is
 * exactly what this suite exists to catch.
 */
class CustomTimeZoneIdTest {

    @Test
    void customTimeZoneIdsResolveAsTheJdkDoes() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("custom-timezone-sources");
        Path classesDir = Files.createTempDirectory("custom-timezone-classes");
        Path javaApiDir = Files.createTempDirectory("custom-timezone-japi");

        Path source = sourceDir.resolve("CustomTimeZoneApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the Latin-1 Character test");
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
        assertEquals(0, compileResult,
                "CustomTimeZoneApp should compile. " + CompilerHelper.getLastErrorLog());

        String jdkResult = extractResultLine(runJavaMain(config, classesDir));
        assertTrue(jdkResult.startsWith("RESULT="),
                "The JDK leg should produce a RESULT line");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("custom-timezone-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "CustomTimeZoneApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "CustomTimeZoneApp-src");

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

        Path executable = buildDir.resolve("CustomTimeZoneApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");

        String vmResult = extractResultLine(
                CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir));

        assertEquals(jdkResult, vmResult, firstDifference(jdkResult, vmResult));
    }

    /** Names the offending id rather than dumping two long strings. */
    private String firstDifference(String expected, String actual) {
        String[] want = expected.substring("RESULT=".length()).split(";");
        String[] got = actual.substring(actual.indexOf('=') + 1).split(";");
        for (int i = 0; i < Math.min(want.length, got.length); i++) {
            if (!want[i].equals(got[i])) {
                return "Custom time zone offsets differ: JDK " + want[i]
                        + ", ParparVM " + got[i] + " (offsets in milliseconds)";
            }
        }
        return "ParparVM should resolve custom GMT offset ids as the JDK does";
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = CustomTimeZoneIdTest.class.getResourceAsStream(
                "/com/codename1/tools/translator/CustomTimeZoneApp.java");
        assertNotNull(in, "CustomTimeZoneApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir) throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve("java").toString();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaExe += ".exe";
        }

        ProcessBuilder pb = new ProcessBuilder(javaExe, "-cp", classesDir.toString(), "CustomTimeZoneApp");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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
