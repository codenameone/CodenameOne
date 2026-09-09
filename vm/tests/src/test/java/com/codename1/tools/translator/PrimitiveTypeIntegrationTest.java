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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins the nine primitive class objects -- {@code Integer.TYPE} and friends -- against
 * the JVM.
 *
 * <p>These were all null on ParparVM until the primitive {@code struct clazz} objects
 * existed. javac lowers a primitive class literal to a read of the boxed type's own
 * {@code TYPE} field, so {@code TYPE = int.class} compiled to
 * {@code getstatic TYPE; putstatic TYPE} and left the field null. Nothing threw: a
 * {@code Map} keyed on them collapsed to a single entry and answered every lookup with
 * whatever had been stored last, which is exactly how the translator's own
 * primitive-to-C-type maps in {@code Util} would have typed every primitive alike.</p>
 *
 * <p>Comparing against a real JVM rather than a hard-coded expectation is deliberate --
 * the failure mode here was self-consistent and silent, so only an independent
 * reference catches it.</p>
 */
class PrimitiveTypeIntegrationTest {

    @Test
    void primitiveClassObjectsMatchTheJvm() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("primitive-type-sources");
        Path classesDir = Files.createTempDirectory("primitive-type-classes");
        Path javaApiDir = Files.createTempDirectory("primitive-type-java-api");

        Path source = sourceDir.resolve("PrimitiveTypeApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the primitive type integration test");
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

        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "PrimitiveTypeApp should compile against the JavaAPI");

        Map<String, String> expected = parseCases(runJavaMain(config, classesDir, javaApiDir));
        assertFalse(expected.isEmpty(), "JVM run should emit cases");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("primitive-type-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "PrimitiveTypeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "PrimitiveTypeApp-src");

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

        Path executable = buildDir.resolve("PrimitiveTypeApp");
        String parparOutput = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(executable.toString()), buildDir);
        assertTrue(parparOutput.contains("DONE"),
                "ParparVM run should complete. Output: " + parparOutput);

        Map<String, String> actual = parseCases(parparOutput);
        assertEquals(expected.keySet(), actual.keySet(), "ParparVM should emit the same cases");

        List<String> differences = new ArrayList<>();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            if (!entry.getValue().equals(actual.get(entry.getKey()))) {
                differences.add(entry.getKey()
                        + "\n    jvm     : " + entry.getValue()
                        + "\n    parparvm: " + actual.get(entry.getKey()));
            }
        }
        assertTrue(differences.isEmpty(),
                "Primitive class objects diverged from the JVM:\n" + String.join("\n", differences));

        // Stated explicitly so a regression names the original symptom rather than
        // showing up only as a generic diff.
        assertEquals("int", actual.get("name.int"), "Integer.TYPE must be the int class");
        assertEquals("void", actual.get("name.void"), "Void.TYPE must be void, not java.lang.Void");
        assertEquals("9", actual.get("distinctIdentities"),
                "the nine primitive class objects must be distinct");
        assertEquals("9", actual.get("mapSize"),
                "a Map keyed on the nine must hold nine entries, not collapse onto null");
        assertEquals("0", actual.get("lookupFailures"),
                "each primitive class must look up its own value");
    }

    private Map<String, String> parseCases(String output) {
        Map<String, String> cases = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            if (!line.startsWith("CASE|")) {
                continue;
            }
            String body = line.substring("CASE|".length());
            int separator = body.indexOf('|');
            assertTrue(separator > 0, "Malformed case line: " + line);
            cases.put(body.substring(0, separator), body.substring(separator + 1));
        }
        return cases;
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = PrimitiveTypeIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/PrimitiveTypeApp.java");
        assertNotNull(in, "PrimitiveTypeApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir)
            throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "PrimitiveTypeApp"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        assertEquals(0, process.waitFor(), "JVM run should exit cleanly. Output: " + output);
        return output;
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
