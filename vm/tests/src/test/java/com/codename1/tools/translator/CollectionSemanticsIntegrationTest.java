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
 * Pins ArrayList and IdentityHashMap against a real JDK after both were changed to
 * stop allocating.
 *
 * <p>ArrayList no longer allocates a backing array in its no-arg constructor -- it
 * shares a zero-length one until the first growth, which then allocates exactly ten
 * so a small list stays in the size class it always occupied. IdentityHashMap's key
 * and value iterators no longer build an Entry per step; only entrySet does, which is
 * the only view where a caller can observe one. java.util.HashMap already had that
 * split and this map had been missed.</p>
 *
 * <p>Both changes are invisible when they work and produce wrong answers at the
 * edges when they do not -- an empty list that reports the wrong size, a null key
 * that reads back as the table's sentinel -- so the JDK is used as the oracle rather
 * than a hand-written expectation.</p>
 */
class CollectionSemanticsIntegrationTest {

    @Test
    void collectionSemanticsMatchTheJvm() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("collection-semantics-sources");
        Path classesDir = Files.createTempDirectory("collection-semantics-classes");
        Path javaApiDir = Files.createTempDirectory("collection-semantics-java-api");

        Path source = sourceDir.resolve("CollectionSemanticsApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the collection semantics integration test");
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
                "CollectionSemanticsApp should compile against the JavaAPI");

        Map<String, String> expected = parseCases(runJavaMain(config, classesDir, javaApiDir));
        assertFalse(expected.isEmpty(), "JVM run should emit cases");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("collection-semantics-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "CollectionSemanticsApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "CollectionSemanticsApp-src");

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

        Path executable = buildDir.resolve("CollectionSemanticsApp");
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
                "Collection semantics diverged from the JVM:\n" + String.join("\n", differences));

        // Named explicitly so a regression points at the change rather than at a
        // generic diff.
        assertEquals("0", actual.get("empty.size"), "a list never added to must be empty");
        assertEquals("IndexOutOfBounds", actual.get("empty.get0"),
                "get(0) on an empty list must still throw");
        assertEquals("1", actual.get("ihm.keyNulls"),
                "the key iterator must hand back the null key as null, not the table's sentinel");
        assertEquals("1", actual.get("ihm.valNulls"),
                "the value iterator must hand back a null value as null");
        assertEquals("500", actual.get("ihm.bigSeen"),
                "key iteration must survive a rehash");
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
        java.io.InputStream in = CollectionSemanticsIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/CollectionSemanticsApp.java");
        assertNotNull(in, "CollectionSemanticsApp.java test resource should exist");
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
                "CollectionSemanticsApp"
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
