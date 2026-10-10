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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An object must not be retired at frame exit while something can still reach it.
 *
 * <p>The translator stamps an object dead when the frame that allocated it returns, if
 * it has proved the object never left that frame. The collector does not second-guess
 * the stamp: a retired object reachable only from a thread's roots is not marked again,
 * and the next sweep finalizes and frees it. With a {@code Thread} that is the thread's
 * own native state, released by {@code Thread.finalize()} while the thread is running on
 * it -- an intermittent use-after-free in a server's scheduler thread, whose only crime
 * was {@code Thread t = new Thread(r); t.setDaemon(true); t.start();}.</p>
 *
 * <p>The proof had five holes, and {@code FrameRetireLiveReceiverApp} has one method for
 * each: a native callee, an abstract one, an override the call site does not name, a
 * callee that leaks {@code this} one call further down, and a member of a local cluster
 * read back out of the object holding it. None of them may be retired. The generated C
 * says so directly and deterministically, so that is asserted first; the program is then
 * run, and counts every object finalized while it was still reachable.</p>
 *
 * <p>The gate cannot pass by retirement having stopped altogether: a sixth method builds
 * an object that really is frame local, and it must still be retired.</p>
 */
class FrameRetireLiveReceiverIntegrationTest {

    private static final String APP = "FrameRetireLiveReceiverApp";

    /** What the translator writes beside an allocation it retires at frame exit. */
    private static final String RETIRED = "frame-exit retired";

    private static final long VM_RUN_TIMEOUT_SECONDS = 600;

    @Test
    void aReachableObjectIsNotRetiredAtFrameExit() throws Exception {
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
        Path sourceDir = Files.createTempDirectory("frame-retire-sources");
        Path classesDir = Files.createTempDirectory("frame-retire-classes");
        Path javaApiDir = Files.createTempDirectory("frame-retire-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve(APP + ".java");
        Files.write(source, loadResource(APP + ".java").getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the frame-exit retirement test");
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
                APP + " should compile. " + CompilerHelper.getLastErrorLog());

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Path outputDir = Files.createTempDirectory("frame-retire-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, APP);
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        // ---- 1. what the translator decided, read from the C it wrote -------------------
        String generated = new String(
                Files.readAllBytes(distDir.resolve(APP + "-src").resolve(APP + ".c")),
                StandardCharsets.UTF_8);
        String[] reachable = {"nativeReceiver", "abstractReceiver", "overridden", "transitive",
                "clusterRead"};
        for (String method : reachable) {
            assertFalse(function(generated, method).contains(RETIRED),
                    method + "() allocates an object that is still reachable after it returns,"
                            + " and the translator retires it at frame exit:\n"
                            + function(generated, method));
        }
        assertTrue(function(generated, "control").contains(RETIRED),
                "control() allocates an object nothing else can reach. If that is no longer"
                        + " retired, this gate has stopped testing the analysis it guards:\n"
                        + function(generated, "control"));

        // ---- 2. and what happens when it runs --------------------------------------------
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, APP + "-src");
        Path buildDir = Files.createTempDirectory("frame-retire-build");
        tempDirs.add(buildDir);
        List<String> cmake = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        cmake.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(cmake, distDir);
        CleanTargetIntegrationTest.runCommand(
                Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        Path exe = buildDir.resolve(CompilerHelper.executableName(APP));
        assertTrue(Files.exists(exe), "ParparVM build should produce a runnable executable at " + exe);

        String output = run(exe, distDir);
        String result = null;
        for (String line : output.split("\\R")) {
            if (line.startsWith("FRAME_RETIRE_LIVE ")) {
                result = line;
            }
        }
        assertNotNull(result, "The workload should report its counts. Output: " + tail(output));
        System.out.println("[FrameRetireLiveReceiver] " + result);
        assertTrue(count(result, "started") > 0 && count(result, "finalizedDead") > 0,
                "Threads must have been started and finalizers must have run, or the counts"
                        + " below prove nothing: " + result);
        assertEquals(0, count(result, "finalizedLiveThread"),
                "A Thread object was finalized while its thread was alive: " + result);
        assertEquals(0, count(result, "finalizedLive"),
                "An object was finalized while it was still reachable: " + result);
        assertEquals(count(result, "started"), count(result, "finished"),
                "Every started thread should run to completion: " + result);
    }

    /** The generated C of one static method of the app, from its signature to its end. */
    private static String function(String generated, String method) {
        String name = APP + "_" + method + "___";
        int at = -1;
        for (int from = generated.indexOf(name); from >= 0; from = generated.indexOf(name, from + 1)) {
            int lineStart = generated.lastIndexOf('\n', from) + 1;
            int lineEnd = generated.indexOf('\n', from);
            // The definition, not a call: it starts in column 0 with the return type
            // and its line ends by opening the body.
            if (generated.startsWith("JAVA_", lineStart) && lineEnd > 0
                    && generated.substring(from, lineEnd).trim().endsWith("{")) {
                at = lineStart;
                break;
            }
        }
        assertTrue(at >= 0, "The generated C should define " + method + "()");
        int end = generated.indexOf("\n}\n", at);
        assertTrue(end > at, "The definition of " + method + "() should be closed");
        return generated.substring(at, end + 2);
    }

    private static int count(String result, String key) {
        for (String field : result.split(" ")) {
            if (field.startsWith(key + "=")) {
                return Integer.parseInt(field.substring(key.length() + 1).trim());
            }
        }
        fail("No " + key + " in: " + result);
        return -1;
    }

    private String run(Path executable, Path workingDir) throws Exception {
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
        return output;
    }

    private static String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 25);
        return String.join("\n", Arrays.copyOfRange(lines, from, lines.length));
    }

    private static String loadResource(String name) throws Exception {
        InputStream in = FrameRetireLiveReceiverIntegrationTest.class
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
