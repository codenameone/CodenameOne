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
 * Heap-integrity gate for the concurrent collector (issue 5425 and the
 * grace-pass defects around it).
 *
 * <p>The other GC tests assert what the program computed. They cannot see the
 * failure this collector actually has: when a sweep frees memory a surviving
 * object still references, nothing diverges at the point of the bug -- the
 * dangling reference reads whatever object recycled the slot, and the damage
 * surfaces later, elsewhere, as corrupted data. That is how #5436's grace-pass
 * regression reached a user as "non word" dictionary entries and an impossible
 * NPE instead of as a failing test.</p>
 *
 * <p>This test builds the workload with {@code -DCN1_GC_VERIFY}, which poisons
 * and quarantines reclaimed memory and, after every sweep, walks each surviving
 * object through its own generated mark function to classify every reference
 * field. A field pointing into reclaimed memory aborts the process at the cycle
 * that created it.</p>
 *
 * <p>The second half of the test is the part that keeps the first half
 * meaningful: it re-injects the exact defect #5442 fixed
 * ({@code CN1_GC_FAULT=nograce} disables the grace-subtree pass) and requires
 * the verifier to catch it. A gate that has never been watched failing proves
 * nothing, and a build where the verification silently compiled out would
 * otherwise report a permanent, meaningless pass.</p>
 */
@Tag("benchmark")
class GcHeapIntegrityIntegrationTest {

    @Test
    void sweepNeverLeavesASurvivorPointingAtReclaimedMemory() throws Exception {
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
        Path sourceDir = Files.createTempDirectory("gc-verify-sources");
        Path classesDir = Files.createTempDirectory("gc-verify-classes");
        Path javaApiDir = Files.createTempDirectory("gc-verify-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("GcVerifyApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the GC heap-integrity test");
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
                "GcVerifyApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        String javaResult = extractLine(javaOutput, "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="),
                "JavaSE should produce RESULT=. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("gc-verify-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "GcVerifyApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "GcVerifyApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        // CMAKE_C_FLAGS composes with the target's own options, so the mandatory
        // -fwrapv / -fno-strict-aliasing set the generated project adds are kept.
        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang",
                "-DCMAKE_C_FLAGS=-DCN1_GC_VERIFY",
                "-DCMAKE_OBJC_FLAGS=-DCN1_GC_VERIFY"
        ), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("GcVerifyApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");

        // ---- 1. the gate ------------------------------------------------------
        Run clean = run(executable, buildDir, new HashMap<String, String>());
        assertEquals(0, clean.exit,
                "The verified build must finish without a heap-integrity abort.\n"
                        + violationExcerpt(clean.output)
                        + "\n--- full output ---\n" + clean.output);
        assertTrue(!clean.output.contains("DANGLING REFERENCE"),
                "The sweep left a surviving object pointing at reclaimed memory.\n"
                        + violationExcerpt(clean.output));
        assertTrue(clean.output.contains("GC_VERIFY_APP_DONE"),
                "The workload should run to completion. Output: " + clean.output);
        // A workload that never finishes a collection cycle never runs the
        // verifier, and "no violations" would then mean only that nothing was
        // ever checked -- the same hollow result the fault injection below
        // exists to rule out.
        assertTrue(verifyPasses(clean.output) > 0,
                "The verifier never ran: the workload completed no GC cycle, so the clean "
                        + "result above checked nothing. Output: " + tail(clean.output));
        assertEquals(javaResult, extractLine(clean.output, "RESULT="),
                "JavaSE and ParparVM should agree on the workload result");

        // ---- 2. proof that the gate can fail ----------------------------------
        // Without this, a build in which the verification compiled out (wrong
        // flag, #ifdef drift, a future refactor) would pass part 1 forever.
        Map<String, String> fault = new HashMap<String, String>();
        fault.put("CN1_GC_FAULT", "nograce");
        Run faulted = run(executable, buildDir, fault);
        assertTrue(faulted.output.contains("[GC-FAULT] grace-subtree pass DISABLED"),
                "Fault injection did not engage -- CN1_GC_VERIFY is not active in this build, "
                        + "so the clean run above proved nothing. Output: " + tail(faulted.output));
        assertTrue(faulted.output.contains("DANGLING REFERENCE"),
                "The verifier did NOT detect the re-injected grace-pass defect (issue 5425 / PR 5442). "
                        + "The heap-integrity gate is inert. Output: " + tail(faulted.output));
        assertTrue(faulted.exit != 0,
                "A detected dangling reference must fail the process, not just log. Exit was 0.");
    }

    private static final class Run {
        final int exit;
        final String output;

        Run(int exit, String output) {
            this.exit = exit;
            this.output = output;
        }
    }

    private Run run(Path executable, Path workingDir, Map<String, String> env) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toString());
        builder.directory(workingDir.toFile());
        // Both halves of this test are decided by environment variables, and a
        // developer debugging the collector has exactly those variables exported
        // (CN1_GC_VERIFY_SOFT downgrades the abort the faulted half asserts on,
        // CN1_GC_FAULT injects the defect the clean half asserts is absent). An
        // inherited knob would invert a result rather than fail loudly, so the
        // child starts from a known state and gets only what this test sets.
        builder.environment().keySet().removeIf(key -> key.startsWith("CN1_"));
        builder.environment().putAll(env);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        return new Run(process.waitFor(), output);
    }

    /** Completed verify passes, from the summary the verified build prints at exit. */
    private long verifyPasses(String output) {
        for (String line : output.split("\\R")) {
            int at = line.indexOf("SUMMARY passes=");
            if (at >= 0) {
                String rest = line.substring(at + "SUMMARY passes=".length());
                int end = 0;
                while (end < rest.length() && Character.isDigit(rest.charAt(end))) {
                    end++;
                }
                if (end > 0) {
                    return Long.parseLong(rest.substring(0, end));
                }
            }
        }
        return 0;
    }

    /** The verifier's own report is the useful part of a failure message. */
    private String violationExcerpt(String output) {
        StringBuilder sb = new StringBuilder();
        String[] lines = output.split("\\R");
        for (int i = 0; i < lines.length && sb.length() < 4000; i++) {
            if (lines[i].contains("GC-VERIFY")) {
                sb.append(lines[i]).append('\n');
                for (int j = i + 1; j < lines.length && j <= i + 4 && lines[j].startsWith("      "); j++) {
                    sb.append(lines[j]).append('\n');
                }
            }
        }
        return sb.length() == 0 ? "(no [GC-VERIFY] report in the output)" : sb.toString();
    }

    private String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 25);
        return String.join("\n", Arrays.copyOfRange(lines, from, lines.length));
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = GcHeapIntegrityIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/GcVerifyApp.java");
        assertNotNull(in, "GcVerifyApp.java test resource should exist");
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
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir, "GcVerifyApp");
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
            System.err.println("GcHeapIntegrityIntegrationTest: temp cleanup incomplete under "
                    + root + " (first failure: " + firstFailure[0] + ")");
        }
    }
}
