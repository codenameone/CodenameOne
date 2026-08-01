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

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression guard for the issue-5482 low-memory allocation throttle.
 *
 * <p>After the OS reports memory pressure the VM raises {@code lowMemoryMode} and
 * slows allocators so the collector can catch up. That throttle used to park EVERY
 * legacy-path allocation for one millisecond, which is a hard ceiling of about 1000
 * allocations/second per thread rather than backpressure: an allocation-heavy loader
 * dropped from seconds to hours and looked like a hang, with no crash and no log.
 * It reproduced on iOS devices only -- nothing else raises {@code lowMemoryMode}, the
 * simulator on a large-RAM host essentially never delivers a memory warning, and
 * Android has no equivalent path -- so it read as "works everywhere but the device".</p>
 *
 * <p>The throttle is now capped at one park per thread per
 * {@code CN1_LOW_MEMORY_PARK_INTERVAL_MS}. The guard asserts that invariant directly
 * rather than measuring wall time: the VM reports
 * {@code [LOWMEM] parks=P throttledAllocations=T} and the app reports how long its
 * loop ran, so the budget scales with the runner instead of assuming a machine speed.
 * A regression that restores per-allocation parking shows up as {@code P == T} --
 * thousands of parks against a budget of a hundred or so.</p>
 *
 * <p>Memory pressure itself is produced by the {@code CN1_SIMULATE_MEMORY_WARNING_MS}
 * test hook, which raises the flag at a fixed cadence the way sustained
 * {@code didReceiveMemoryWarning} delivery does on a device. The hook only raises the
 * flag; it does not also force collection cycles the way the iOS handler does, which
 * keeps the measurement about the throttle rather than about GC frequency.</p>
 */
class LowMemoryThrottleIntegrationTest {

    /** Keep in sync with CN1_LOW_MEMORY_PARK_INTERVAL_MS in cn1_globals.h. */
    private static final long PARK_INTERVAL_MS = 10;

    /**
     * Warning cadence. Sustained pressure on a device re-delivers the warning
     * continuously, and each completed collection cycle lowers the flag again, so
     * the cadence has to be short relative to a cycle or the flag spends the run
     * down and the guard measures nothing (at 5ms only a fifth of this load's
     * allocations were still reaching the throttle).
     */
    private static final String WARNING_CADENCE_MS = "1";

    /**
     * Slack over the theoretical park budget (elapsed / interval): absorbs the
     * collector thread taking its own park in a window and the loop's start/end
     * falling mid-window. A regression is three orders of magnitude out, so a
     * generous factor costs the guard nothing.
     */
    private static final long PARK_BUDGET_FACTOR = 2;
    private static final long PARK_BUDGET_SLACK = 100;

    @Test
    void memoryWarningsDoNotStallAnAllocatingThread() throws Exception {
        Parser.cleanup();

        List<Path> tempDirs = new ArrayList<>();
        try {
            runThrottleLoad(tempDirs);
        } finally {
            for (Path dir : tempDirs) {
                deleteRecursively(dir);
            }
        }
    }

    private void runThrottleLoad(List<Path> tempDirs) throws Exception {
        Path sourceDir = Files.createTempDirectory("low-memory-throttle-sources");
        Path classesDir = Files.createTempDirectory("low-memory-throttle-classes");
        Path javaApiDir = Files.createTempDirectory("low-memory-throttle-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("LowMemoryThrottleApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for low-memory throttle integration test");
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
                "LowMemoryThrottleApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        String javaResult = extractLine(javaOutput, "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="),
                "JavaSE should produce RESULT=. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("low-memory-throttle-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LowMemoryThrottleApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "LowMemoryThrottleApp-src");

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

        Path executable = buildDir.resolve("LowMemoryThrottleApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");

        // Baseline: no memory warning, so the throttle must never engage at all.
        Map<String, String> baselineEnv = new HashMap<>();
        baselineEnv.put("CN1_LOG_LOWMEM_PARKS", "1");
        String baselineOutput = runVm(executable, buildDir, baselineEnv);
        assertEquals(javaResult, extractLine(baselineOutput, "RESULT="),
                "JavaSE and ParparVM should agree without memory pressure\n"
                        + "--- JavaSE ---\n" + javaOutput
                        + "\n--- ParparVM ---\n" + baselineOutput);
        assertEquals(0, parseCounter(baselineOutput, "throttledAllocations="),
                "No memory warning was delivered, so no allocation should reach the "
                        + "low-memory throttle at all. Output: " + baselineOutput);

        // Under pressure: same work, same answer, and a paced throttle.
        Map<String, String> pressureEnv = new HashMap<>();
        pressureEnv.put("CN1_LOG_LOWMEM_PARKS", "1");
        pressureEnv.put("CN1_SIMULATE_MEMORY_WARNING_MS", WARNING_CADENCE_MS);
        String pressureOutput = runVm(executable, buildDir, pressureEnv);

        assertTrue(pressureOutput.contains("LOW_MEMORY_THROTTLE_DONE"),
                "The load must finish under sustained memory warnings. Output: " + pressureOutput);
        assertEquals(javaResult, extractLine(pressureOutput, "RESULT="),
                "Throttling must not change the result\n"
                        + "--- JavaSE ---\n" + javaOutput
                        + "\n--- ParparVM under pressure ---\n" + pressureOutput);

        long throttled = parseCounter(pressureOutput, "throttledAllocations=");
        long parks = parseCounter(pressureOutput, "parks=");
        long elapsedMs = parseValue(pressureOutput, "ELAPSED_MS=");

        assertTrue(throttled > 0,
                "The simulated memory warning never reached an allocation, so this run "
                        + "proves nothing about the throttle -- check the "
                        + "CN1_SIMULATE_MEMORY_WARNING_MS hook. Output: " + pressureOutput);

        long budget = PARK_BUDGET_FACTOR * (elapsedMs / PARK_INTERVAL_MS) + PARK_BUDGET_SLACK;
        assertTrue(parks <= budget,
                "Low-memory throttle parked " + parks + " times over " + elapsedMs
                        + "ms (budget " + budget + ", one park per " + PARK_INTERVAL_MS
                        + "ms plus slack) across " + throttled + " throttled allocations."
                        + " This is the issue-5482 signature: parking every allocation"
                        + " instead of pacing the parks, which caps an allocating thread"
                        + " near 1000 allocations/second and reads as a hang on device."
                        + "\n--- ParparVM under pressure ---\n" + pressureOutput);

        System.err.println("[LowMemoryThrottleIntegrationTest] throttledAllocations=" + throttled
                + " parks=" + parks + " budget=" + budget + " elapsedMs=" + elapsedMs);
    }

    private long parseCounter(String output, String key) {
        for (String line : output.split("\\R")) {
            if (line.startsWith("[LOWMEM] parks=")) {
                for (String token : line.substring("[LOWMEM] ".length()).split("\\s+")) {
                    if (token.startsWith(key)) {
                        return Long.parseLong(token.substring(key.length()).trim());
                    }
                }
            }
        }
        fail("VM did not report " + key + " -- the CN1_LOG_LOWMEM_PARKS tracer did not fire, "
                + "so the guard cannot observe the throttle. Output: " + output);
        return -1;
    }

    private long parseValue(String output, String prefix) {
        String line = extractLine(output, prefix);
        if (line.isEmpty()) {
            fail("Missing " + prefix + " in output: " + output);
        }
        return Long.parseLong(line.substring(prefix.length()).trim());
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = LowMemoryThrottleIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/LowMemoryThrottleApp.java");
        assertNotNull(in, "LowMemoryThrottleApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir)
            throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve("java").toString();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaExe += ".exe";
        }
        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "LowMemoryThrottleApp");
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

    private String runVm(Path executable, Path workingDir, Map<String, String> env) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toString());
        builder.directory(workingDir.toFile());
        builder.environment().putAll(env);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        assertEquals(0, process.waitFor(),
                "ParparVM run should exit cleanly (env " + env + "). Output: " + output);
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

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        final java.io.IOException[] firstFailure = new java.io.IOException[1];
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (java.io.IOException e) {
                    if (firstFailure[0] == null) {
                        firstFailure[0] = e;
                    }
                }
            });
        } catch (java.io.IOException e) {
            if (firstFailure[0] == null) {
                firstFailure[0] = e;
            }
        }
        if (firstFailure[0] != null) {
            System.err.println("LowMemoryThrottleIntegrationTest: temp cleanup incomplete under "
                    + root + " (first failure: " + firstFailure[0] + ")");
        }
    }
}
