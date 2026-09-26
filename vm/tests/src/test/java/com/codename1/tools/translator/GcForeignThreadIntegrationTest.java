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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A thread the VM did not start must not stop the collector from reclaiming.
 *
 * <p>Such a thread registers itself the first time it touches Java, and the collector then
 * captures its native-stack roots every cycle; a cycle that cannot capture a registered
 * thread's roots skips its sweep, printing {@value #SKIPPED_SWEEP}. Three ordinary
 * situations made that capture fail on every cycle, and each was enough to stop a whole
 * program reclaiming anything -- objectAllocation in a macOS gallery app went from 30ms to
 * 85-153ms with 8-9 skipped sweeps per run, and in a Linux container one exited thread
 * took it from a 46ms mean to 894-1021ms with 56 skipped:</p>
 *
 * <ul>
 *   <li>a thread that EXITED stayed registered, so the stop went to a thread that no
 *       longer existed. Its key now has a destructor that unregisters it;</li>
 *   <li>a libdispatch worker cannot be signalled at all -- pthread_kill answers ENOTSUP
 *       for a workqueue thread -- so on Apple it is now stopped with Mach instead;</li>
 *   <li>Windows has no stop signal and could not report another thread's stack bounds, so
 *       every thread's capture failed there. Threads now record their own bounds when they
 *       register, and one that is not parked is suspended.</li>
 *   <li>the VM's OWN threads: a thread inherits its creator's signal mask, so one started
 *       where the stop signal is blocked ignored it for life. In a macOS gallery app that
 *       was four VM threads timing out on every cycle ({@code site=5 lightweight=1
 *       stopErr=-1}). A thread now unblocks the stop signal when it registers.</li>
 * </ul>
 *
 * <p>The gate asserts no sweep is skipped, then rebuilds the same translation with the
 * fixes compiled out ({@code -DCN1_GC_NO_FOREIGN_THREAD_EXIT -DCN1_GC_NO_OS_SUSPEND
 * -DCN1_GC_NO_STOP_SIGNAL_UNBLOCK}) and REQUIRES skipped sweeps, so it cannot pass by never
 * exercising the path. The native half registers an exiting thread everywhere and a
 * libdispatch worker on Apple, and the churn runs on a worker started with the stop signal
 * blocked.</p>
 */
class GcForeignThreadIntegrationTest {

    /** What the collector prints for each cycle whose roots it could not capture. */
    private static final String SKIPPED_SWEEP = "incomplete native root capture";

    /** Six rounds of objectAllocation's churn finish in seconds; minutes means wedged. */
    private static final long VM_RUN_TIMEOUT_SECONDS = 600;

    @Test
    void aThreadTheVmDidNotStartDoesNotStopReclamation() throws Exception {
        Parser.cleanup();
        List<Path> tempDirs = new ArrayList<>();
        try {
            runGate(tempDirs);
        } finally {
            for (Path dir : tempDirs) {
                deleteRecursively(dir);
            }
        }
    }

    private void runGate(List<Path> tempDirs) throws Exception {
        Path sourceDir = Files.createTempDirectory("gc-foreign-sources");
        Path classesDir = Files.createTempDirectory("gc-foreign-classes");
        Path javaApiDir = Files.createTempDirectory("gc-foreign-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("GcForeignThreadApp.java");
        Files.write(source, loadResource("GcForeignThreadApp.java").getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the GC foreign-thread test");
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
                "GcForeignThreadApp should compile. " + CompilerHelper.getLastErrorLog());

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.write(classesDir.resolve("GcForeignThreadNative.c"),
                loadResource("GcForeignThreadNative.c").getBytes(StandardCharsets.UTF_8));
        Path outputDir = Files.createTempDirectory("gc-foreign-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "GcForeignThreadApp");
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "GcForeignThreadApp-src");

        // ---- 1. the gate ----------------------------------------------------------------
        Run fixed = run(build(distDir, tempDirs, "fixed", ""), distDir);
        assertEquals(0, fixed.exit, "The workload must finish. Output: " + tail(fixed.output));
        assertTrue(fixed.output.contains("GC_FOREIGN_THREAD_DONE"),
                "The workload should run to completion. Output: " + tail(fixed.output));
        assertTrue(fixed.output.contains("MASKED_WORKER=true") || CompilerHelper.isWindows(),
                "The churn should run on a worker started with the stop signal blocked. Output: "
                        + tail(fixed.output));
        assertTrue(registered(fixed.output) >= 1,
                "The native half must have registered a foreign thread, or this gate exercised"
                        + " nothing. Output: " + tail(fixed.output));
        int fixedSkips = count(fixed.output, SKIPPED_SWEEP);
        System.out.println("[GcForeignThread] fixed arm: foreignThreads=" + registered(fixed.output)
                + " skippedSweeps=" + fixedSkips);
        assertEquals(0, fixedSkips,
                "A cycle skipped its sweep because a registered foreign thread's roots could not be"
                        + " captured. Output: " + tail(fixed.output));

        // ---- 2. the same translation with both fixes compiled out must fail -----------
        Run faulted = run(build(distDir, tempDirs, "faulted",
                " -DCN1_GC_NO_FOREIGN_THREAD_EXIT -DCN1_GC_NO_OS_SUSPEND -DCN1_GC_NO_STOP_SIGNAL_UNBLOCK "),
                distDir);
        int faultedSkips = count(faulted.output, SKIPPED_SWEEP);
        System.out.println("[GcForeignThread] ablation arm: skippedSweeps=" + faultedSkips);
        assertTrue(faultedSkips > 0,
                "With the fixes compiled out the collector should skip sweeps; if it does not,"
                        + " this gate no longer reaches the path it guards. Output: "
                        + tail(faulted.output));
    }

    private static int registered(String output) {
        for (String line : output.split("\\R")) {
            if (line.startsWith("FOREIGN_THREADS=")) {
                try {
                    return Integer.parseInt(line.substring("FOREIGN_THREADS=".length()).trim());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static int count(String output, String needle) {
        int n = 0;
        for (int at = output.indexOf(needle); at >= 0; at = output.indexOf(needle, at + 1)) {
            n++;
        }
        return n;
    }

    private Path build(Path distDir, List<Path> tempDirs, String name, String cFlags) throws Exception {
        Path buildDir = Files.createTempDirectory("gc-foreign-build-" + name);
        tempDirs.add(buildDir);
        List<String> cmake = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        cmake.addAll(CompilerHelper.cmakeToolchainArgs());
        cmake.add(CompilerHelper.cFlagsArg(cFlags));
        CleanTargetIntegrationTest.runCommand(cmake, distDir);
        CleanTargetIntegrationTest.runCommand(
                Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        Path exe = buildDir.resolve(CompilerHelper.executableName("GcForeignThreadApp"));
        assertTrue(Files.exists(exe), "ParparVM build should produce a runnable executable at " + exe);
        return exe;
    }

    private static final class Run {
        final int exit;
        final String output;

        Run(int exit, String output) {
            this.exit = exit;
            this.output = output;
        }
    }

    private Run run(Path executable, Path workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toString());
        builder.directory(workingDir.toFile());
        builder.environment().keySet().removeIf(key -> key.startsWith("CN1_"));
        builder.redirectErrorStream(true);
        final Process process = builder.start();
        final StringBuilder captured = new StringBuilder();
        Thread drain = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (captured) {
                        captured.append(line).append('\n');
                    }
                }
            } catch (Exception e) {
                // A destroyed child ends the stream abruptly; what was captured is reported.
            }
        });
        drain.setDaemon(true);
        drain.start();
        boolean exited = process.waitFor(VM_RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
        drain.join(10_000);
        String output;
        synchronized (captured) {
            output = captured.toString();
        }
        assertTrue(exited, "The workload did not finish within " + VM_RUN_TIMEOUT_SECONDS
                + "s. Output so far:\n" + tail(output));
        return new Run(process.exitValue(), output);
    }

    private static String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 25);
        return String.join("\n", Arrays.copyOfRange(lines, from, lines.length));
    }

    private static String loadResource(String name) throws Exception {
        InputStream in = GcForeignThreadIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/" + name);
        assertNotNull(in, name + " test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
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

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (Exception ignore) {
            // Best effort; a leftover temp dir is not a test failure.
        }
    }
}
