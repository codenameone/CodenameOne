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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The uncooperative-mutator gate for the collector (issue #5537).
 *
 * <p>The mark phase stops each lightweight thread by raising {@code threadBlockedByGC} and
 * spinning on {@code threadActive}, and the translator emits no safepoint polls in
 * generated code -- not on method entry, not on loop back-edges. Every safepoint lives
 * inside a runtime function, so a Java loop that allocates nothing and enters no contended
 * monitor reaches none of them and that spin never ends. The reporter's debugger caught it
 * at {@code totalwait = 609491500} microseconds -- ten minutes with the whole VM stopped
 * behind one compute loop, because every other thread parks at its next allocation waiting
 * for a cycle that can never start.</p>
 *
 * <p>{@code cn1_globals.m} now bounds that spin at {@code CN1_GC_SAFEPOINT_WAIT_MAX_US} and
 * then freezes the thread with the same SIGUSR2 stop the collector already uses for genuine
 * native threads. This gate asserts the outcome and the mechanism, and then rebuilds the
 * same translated project with the escalation compiled out ({@code -DCN1_GC_NO_FORCE_STOP})
 * and requires both to fail -- a gate that has never been watched failing proves
 * nothing.</p>
 *
 * <p>Every threshold is a RATIO of the workload's own two measurements rather than a
 * wall-clock constant, because the failure is "a mutator was stalled for as long as the
 * spinner ran" and that duration is whatever the machine makes it. See
 * {@code GcUncooperativeThreadApp} for how the two are produced.</p>
 */
@Tag("benchmark")
class GcUncooperativeThreadIntegrationTest {

    /**
     * Share of the spin a mutator may be stalled for when the escalation works. The stall
     * it should actually see is one safepoint bound (CN1_GC_SAFEPOINT_WAIT_MAX_US, 250ms)
     * plus a mark, repeated per cycle -- so this is loose on purpose. The two regimes are
     * "a fraction of a second" and "the entire spin", and a threshold tight enough to
     * flake would be measuring the machine rather than the bug.
     *
     * <p>Measured on this arm: 0.06 alone, 0.13 with the whole benchmark suite running in
     * parallel on the same host. The ablation arm measures 0.98-1.00 in both, so the two
     * are separated by roughly an order of magnitude and the margin here is deliberate
     * headroom for a loaded runner rather than a number tuned against one.</p>
     */
    private static final double MAX_STALL_SHARE_FIXED = 0.25;

    /**
     * Share of the spin the ABLATION arm must exceed. A wedged VM stalls the allocator
     * from whenever the first cycle starts until the spinner ends, so the only thing that
     * keeps this below 1.0 is how long the workload takes to get going -- measured 0.98
     * and 1.00. Set at half that, because this half of the gate only has to establish
     * that the wedge still reproduces, not to measure it.
     */
    private static final double MIN_STALL_SHARE_FAULTED = 0.5;

    /**
     * Floor on the spin itself. Below this the ratios above stop separating anything: a
     * spin shorter than a collection cycle cannot express a stall that lasts one. The
     * fixture calibrates for 6s, so this only catches a machine or a build on which the
     * calibration collapsed.
     */
    private static final long MIN_SPIN_MS = 2000;

    /**
     * Ceiling on one translated run. The ablation arm's spinner is bounded by an iteration
     * count, so even a fully wedged VM finishes -- but reading the child to EOF on this
     * thread would still hang the fork if that ever stopped being true.
     */
    private static final long VM_RUN_TIMEOUT_SECONDS = 600;

    /** What the collector prints when it has to shoot a mutator. */
    private static final String FORCE_STOP_MARKER = "force-stopped thread";

    @Test
    void aComputeOnlyThreadDoesNotStallEverybodyElse() throws Exception {
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
        // The escalation needs POSIX signals, so CN1_GC_CAN_FORCE_STOP is deliberately not
        // defined on Windows and the runtime keeps the unbounded cooperative wait there.
        // The fixed arm therefore cannot emit the force-stop marker on Windows and this
        // gate would fail for a reason that says nothing about the code under test. Skipped
        // rather than weakened: the assertions are what make it a gate, and the platform
        // that can satisfy them is the platform the feature exists on.
        org.junit.jupiter.api.Assumptions.assumeFalse(CompilerHelper.isWindows(),
                "The forced-stop escalation is not compiled on Windows (no POSIX signals),"
                        + " so there is nothing for this gate to assert there.");

        Path sourceDir = Files.createTempDirectory("gc-uncoop-sources");
        Path classesDir = Files.createTempDirectory("gc-uncoop-classes");
        Path javaApiDir = Files.createTempDirectory("gc-uncoop-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("GcUncooperativeThreadApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the GC uncooperative-thread test");
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
                "GcUncooperativeThreadApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaResult = extractLine(runJavaMain(config, classesDir, javaApiDir), "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce RESULT=");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Path outputDir = Files.createTempDirectory("gc-uncoop-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "GcUncooperativeThreadApp");
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(
                cmakeLists, "GcUncooperativeThreadApp-src");

        // ---- 1. the gate ------------------------------------------------------
        Run fixed = run(build(distDir, tempDirs, "fixed", ""), distDir);
        assertEquals(0, fixed.exit, "The workload must finish. Output: " + tail(fixed.output));
        assertTrue(fixed.output.contains("GC_UNCOOPERATIVE_DONE"),
                "The workload should run to completion. Output: " + tail(fixed.output));
        assertEquals(javaResult, extractLine(fixed.output, "RESULT="),
                "JavaSE and ParparVM should agree on the workload result");

        long fixedSpin = value(fixed.output, "SPINMS=");
        long fixedStall = value(fixed.output, "MAXSTALL=");
        report("fixed", fixedSpin, fixedStall, fixed.output.contains(FORCE_STOP_MARKER));
        assertTrue(fixedSpin >= MIN_SPIN_MS,
                "The spin only lasted " + fixedSpin + "ms, so the stall ratio below compared"
                        + " nothing. Output: " + tail(fixed.output));

        // MECHANISM: the collector had to shoot the spinner. Without this the outcome check
        // would also pass on a VM where the spinner happened to reach a safepoint for some
        // unrelated reason, and the gate would quietly stop testing the escalation.
        assertTrue(fixed.output.contains(FORCE_STOP_MARKER),
                "The collector never reported a forced stop, so the spinner parked on its own"
                        + " and this run did not exercise the escalation at all. Output: "
                        + tail(fixed.output));

        // OUTCOME: nobody waited for the spinner.
        assertTrue(fixedStall <= fixedSpin * MAX_STALL_SHARE_FIXED,
                "A mutator was stalled " + fixedStall + "ms of a " + fixedSpin + "ms spin ("
                        + "share " + share(fixedStall, fixedSpin) + "), i.e. it spent the spin waiting"
                        + " for a collector waiting for a thread that reaches no safepoint."
                        + " Output: " + tail(fixed.output));

        // ---- 2. proof that the gate can fail ----------------------------------
        Run faulted = run(build(distDir, tempDirs, "faulted", "-DCN1_GC_NO_FORCE_STOP"), distDir);
        assertEquals(0, faulted.exit,
                "The ablation build must still finish -- its spinner is bounded by an"
                        + " iteration count. Output: " + tail(faulted.output));
        long faultedSpin = value(faulted.output, "SPINMS=");
        long faultedStall = value(faulted.output, "MAXSTALL=");
        report("faulted", faultedSpin, faultedStall, faulted.output.contains(FORCE_STOP_MARKER));
        assertTrue(faultedSpin >= MIN_SPIN_MS,
                "The ablation arm's spin only lasted " + faultedSpin + "ms. Output: "
                        + tail(faulted.output));
        assertFalse(faulted.output.contains(FORCE_STOP_MARKER),
                "-DCN1_GC_NO_FORCE_STOP still reported a forced stop, so the ablation did not"
                        + " remove the thing part 1 asserts. Output: " + tail(faulted.output));
        assertTrue(faultedStall >= faultedSpin * MIN_STALL_SHARE_FAULTED,
                "Without the escalation a mutator was stalled only " + faultedStall + "ms of a "
                        + faultedSpin + "ms spin (share " + share(faultedStall, faultedSpin) + "). The"
                        + " wedge did not reproduce, so part 1 proved nothing -- the workload"
                        + " has stopped reaching the state it is meant to create. Output: "
                        + tail(faulted.output));
    }

    /**
     * Both arms are printed on every run, passing or failing. The gate's whole claim is a
     * comparison between two numbers, and a comparison nobody can read afterwards is an
     * assertion on trust -- the same reason the steady-state gate prints its series.
     */
    private static void report(String arm, long spinMs, long stallMs, boolean forced) {
        System.out.println("[GC-UNCOOP] arm=" + arm + " spinMs=" + spinMs + " maxStallMs="
                + stallMs + " stallShare=" + share(stallMs, spinMs) + " forcedStop=" + forced);
    }

    private static String share(long part, long whole) {
        if (whole <= 0) {
            return "n/a";
        }
        return String.format("%.2f", (double) part / whole);
    }

    private static long value(String output, String prefix) {
        for (String line : output.split("\\R")) {
            if (line.startsWith(prefix)) {
                try {
                    return Long.parseLong(line.substring(prefix.length()).trim());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /** One build of the already-translated project, with its own flags and build dir. */
    private Path build(Path distDir, List<Path> tempDirs, String name, String cFlags) throws Exception {
        Path buildDir = Files.createTempDirectory("gc-uncoop-build-" + name);
        tempDirs.add(buildDir);
        List<String> cmake = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        cmake.addAll(CompilerHelper.cmakeToolchainArgs());
        // CMAKE_C_FLAGS composes with the target's own options, so the mandatory
        // -fwrapv / -fno-strict-aliasing the generated project adds are kept.
        cmake.add("-DCMAKE_C_FLAGS=" + cFlags);
        CleanTargetIntegrationTest.runCommand(cmake, distDir);
        CleanTargetIntegrationTest.runCommand(
                Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        Path exe = buildDir.resolve(CompilerHelper.executableName("GcUncooperativeThreadApp"));
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
        // A developer debugging the collector has CN1_* knobs exported, and several of them
        // would invert this result rather than fail loudly. Start from a known state.
        builder.environment().keySet().removeIf(key -> key.startsWith("CN1_"));
        builder.redirectErrorStream(true);
        final Process process = builder.start();

        // Drained concurrently: a child that fills the pipe buffer blocks in write() while
        // we block in waitFor(), and a wedged collector is precisely what this gate is
        // about -- blocking on EOF would turn a caught regression into a hung build.
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
                // The stream ends abruptly when a timed-out child is destroyed; whatever was
                // captured before that is exactly what should be reported.
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
        assertTrue(exited,
                "The workload did not finish within " + VM_RUN_TIMEOUT_SECONDS + "s. For this"
                        + " gate that is a result and not an infrastructure problem. Output so"
                        + " far:\n" + tail(output));
        return new Run(exited ? process.exitValue() : -1, output);
    }

    private String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 25);
        return String.join("\n", Arrays.copyOfRange(lines, from, lines.length));
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = GcUncooperativeThreadIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/GcUncooperativeThreadApp.java");
        assertNotNull(in, "GcUncooperativeThreadApp.java test resource should exist");
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
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "GcUncooperativeThreadApp");
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
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (java.io.IOException ignored) {
                    // best effort; the OS reclaims the temp tree
                }
            });
        } catch (java.io.IOException ignored) {
            // best effort
        }
    }
}
