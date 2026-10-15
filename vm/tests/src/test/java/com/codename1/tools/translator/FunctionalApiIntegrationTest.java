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
 * The java.util.function, java.util.stream and java.util.concurrent members that
 * live on classes the JDK owns -- Function and Comparator defaults, Collection.stream,
 * Map defaults, the executors -- run on ParparVM and on the JVM, and print the same.
 * Classes that can be renamed out of java.* are compared in maven/jdk-parity-tests
 * instead; these cannot, because the JDK's collections call them.
 */
class FunctionalApiIntegrationTest {

    @Test
    void functionDefaultsAndComparatorsMatchTheJvm() throws Exception {
        assertSameOnParparVM("FunctionEdgeApp");
    }

    @Test
    void streamsAndCollectionDefaultsMatchTheJvm() throws Exception {
        assertSameOnParparVM("StreamPipelineApp");
    }

    static String assertSameOnParparVM(String app) throws Exception {
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory(app + "-sources");
        Path classesDir = Files.createTempDirectory(app + "-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");
        Path source = sourceDir.resolve(app + ".java");
        Files.write(source, loadAppSource(app).getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for " + app);
        }
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
                app + " should compile. " + CompilerHelper.getLastErrorLog());

        String javaResult = extractResultLine(runJavaMain(config, classesDir, javaApiDir, app));
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce a RESULT line: " + javaResult);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Path outputDir = Files.createTempDirectory(app + "-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, app);
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, app + "-src");
        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B",
                buildDir.toString(), "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        Path executable = buildDir.resolve(app);
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");
        String vmOutput = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertEquals(javaResult, extractResultLine(vmOutput), "ParparVM output should match JavaSE. Output: " + vmOutput);
        return new String(Files.readAllBytes(distDir.resolve(app + "-src/" + app + ".c")), StandardCharsets.UTF_8);
    }

    private static String loadAppSource(String app) throws Exception {
        java.io.InputStream in = FunctionalApiIntegrationTest.class.getResourceAsStream(
                "/com/codename1/tools/translator/" + app + ".java");
        assertNotNull(in, app + ".java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private static String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir,
                                      String app) throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve("java").toString();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaExe += ".exe";
        }
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir, app);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        assertEquals(0, process.waitFor(), "JVM run should exit cleanly. Output: " + output);
        return output;
    }

    private static String extractResultLine(String output) {
        for (String line : output.split("\\R")) {
            if (line.startsWith("RESULT=")) {
                return line.trim();
            }
        }
        return "";
    }

    private static CompilerHelper.CompilerConfig selectCompiler() {
        String[] preferredTargets = {"11", "17", "21", "25", "1.8"};
        for (String target : preferredTargets) {
            for (CompilerHelper.CompilerConfig config : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        return null;
    }
}
