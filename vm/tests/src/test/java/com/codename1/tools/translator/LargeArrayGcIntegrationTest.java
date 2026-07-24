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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

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
 * Regression guard for the issue-5425 large-array GC storm (final Dtest shape).
 *
 * <p>Large arrays (above CN1_BIBOP_MAX_OBJECT) always take the legacy
 * allocation path. Before the fix, their allocation volume re-armed the GC
 * thread's 200ms high-frequency loop through the pre-BiBOP 1MB threshold, so a
 * workload that retains a few hundred 10K byte[] blocks -- creating NO garbage
 * -- kept full collection cycles starting at wall-clock cadence over the whole
 * survivor set for the entire run. The fixed VM collects only when allocation
 * VOLUME demands it (24MB budget on both the BiBOP and legacy paths).</p>
 *
 * <p>The guard counts collection CYCLES (the VM's env-gated CN1_GC_LOG_CYCLES
 * tracer, one [GC-CYCLE] stderr line per cycle) rather than phase wall times:
 * cycle count is deterministic and load-independent, while wall-time inflation
 * only appears when cycles contend with the mutator, which depends on core
 * count and runner load. Measured on this workload: unfixed 15 cycles, fixed
 * 6, exactly stable across runs -- the gate is 10. The app's phases are
 * stretched to fixed wall durations so the number of re-arm windows is
 * machine-independent; the fixed VM's count is volume-driven and therefore
 * also machine-independent. Correctness is pinned by comparing RESULT=
 * against the same program on the host JVM.</p>
 */
@Tag("benchmark")
class LargeArrayGcIntegrationTest {

    /** Cycle budget: fixed VM measures 6 (volume-driven), unfixed 15+ (cadence-driven). */
    private static final int MAX_GC_CYCLES = 10;

    @Test
    void largeArrayLoadStaysFlatAcrossRounds() throws Exception {
        Parser.cleanup();

        List<Path> tempDirs = new ArrayList<>();
        try {
            runLargeArrayLoad(tempDirs);
        } finally {
            // The translated CMake build tree is large; don't let repeated runs
            // accumulate it on self-hosted runners.
            for (Path dir : tempDirs) {
                deleteRecursively(dir);
            }
        }
    }

    private void runLargeArrayLoad(List<Path> tempDirs) throws Exception {
        Path sourceDir = Files.createTempDirectory("large-array-gc-sources");
        Path classesDir = Files.createTempDirectory("large-array-gc-classes");
        Path javaApiDir = Files.createTempDirectory("large-array-gc-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("LargeArrayGcApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for large-array GC integration test");
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
        assertEquals(0, compileResult, "LargeArrayGcApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        String javaResult = extractLine(javaOutput, "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce RESULT=. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("large-array-gc-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LargeArrayGcApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "LargeArrayGcApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("LargeArrayGcApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");
        String vmOutput = runVmBenchmarkWithRetry(executable, buildDir);
        String vmResult = extractLine(vmOutput, "RESULT=");
        assertEquals(javaResult, vmResult,
                "JavaSE and ParparVM should produce identical RESULT signatures\n"
                        + "--- JavaSE output ---\n" + javaOutput
                        + "\n--- ParparVM output ---\n" + vmOutput);
        assertTrue(vmOutput.contains("LARGE_ARRAY_GC_DONE"),
                "ParparVM run should complete all rounds. Output: " + vmOutput);

        // Storm detector: count collection cycles over the whole run. On the
        // fixed VM, collection is volume-driven -- this workload's ~7MB/round
        // crosses the 24MB budget about every third round (6 cycles measured).
        // On the regressed VM the pre-BiBOP 1MB re-arm keeps cycles starting at
        // wall-clock cadence for the entire run (15 measured). Cycle count is
        // deterministic and independent of runner speed and load.
        int cycles = 0;
        for (String line : vmOutput.split("\\R")) {
            if (line.contains("[GC-CYCLE]")) {
                cycles++;
            }
        }
        assertTrue(cycles > 0,
                "Expected at least one [GC-CYCLE] line -- CN1_GC_LOG_CYCLES tracer "
                        + "did not fire; the guard cannot observe the collector. Output: " + vmOutput);
        assertTrue(cycles <= MAX_GC_CYCLES,
                "GC ran " + cycles + " cycles over the large-array load (budget " + MAX_GC_CYCLES
                        + "). This is the issue-5425 storm signature: legacy/large allocations "
                        + "re-arming continuous collection cycles through the pre-BiBOP 1MB "
                        + "isHighFrequencyGC threshold instead of the 24MB volume budget."
                        + "\n--- ParparVM output ---\n" + vmOutput);

        System.err.println("[LargeArrayGcIntegrationTest] cycles=" + cycles);
        System.err.println("[LargeArrayGcIntegrationTest] JavaSE\n" + javaOutput);
        System.err.println("[LargeArrayGcIntegrationTest] ParparVM\n" + vmOutput);
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (java.io.IOException ignore) {
                    // best effort -- a locked file must not fail the test
                }
            });
        } catch (java.io.IOException ignore) {
        }
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = LargeArrayGcIntegrationTest.class.getResourceAsStream("/com/codename1/tools/translator/LargeArrayGcApp.java");
        assertNotNull(in, "LargeArrayGcApp.java test resource should exist");
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
                "LargeArrayGcApp"
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

    private String extractLine(String output, String prefix) {
        for (String line : output.split("\\R")) {
            if (line.startsWith(prefix)) {
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

    private String runVmBenchmarkWithRetry(Path executable, Path workingDir) throws Exception {
        final int maxAttempts = 4;
        AssertionFailedError lastSegfaultFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return runWithCycleTracer(executable, workingDir);
            } catch (AssertionFailedError failure) {
                if (!looksLikeSegmentationFault(failure)) {
                    throw failure;
                }
                lastSegfaultFailure = failure;
                if (attempt < maxAttempts) {
                    Thread.sleep(100L * attempt);
                }
            }
        }
        throw lastSegfaultFailure;
    }

    /**
     * Like CleanTargetIntegrationTest.runCommand, but with CN1_GC_LOG_CYCLES=1 in
     * the environment so the VM emits one [GC-CYCLE] stderr line per collection
     * cycle (stderr is merged into the returned output).
     */
    private String runWithCycleTracer(Path executable, Path workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toString());
        builder.directory(workingDir.toFile());
        builder.environment().put("CN1_GC_LOG_CYCLES", "1");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exit = process.waitFor();
        if (exit != 0) {
            // Controlled message: the retry logic keys on the VM_RUN_EXIT marker
            // instead of JUnit's assertion-message format, which is not a stable API.
            throw new AssertionFailedError(
                    "VM_RUN_EXIT(" + exit + ") for " + executable + "\nOutput:\n" + output);
        }
        return output;
    }

    private boolean looksLikeSegmentationFault(AssertionFailedError failure) {
        String message = failure.getMessage();
        if (message == null) {
            return false;
        }
        // 139 = 128+SIGSEGV from the shell-style exit code the JVM reports for a
        // crashed child process.
        return message.contains("VM_RUN_EXIT(139)") || message.contains("Segmentation fault");
    }
}
