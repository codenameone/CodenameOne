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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression guard for the mark-worklist overflow spiral of issue #5537.
 *
 * <p>The GC's grace pass walks the BiBOP page registry every cycle and marks every object
 * allocated since the last one -- a fresh object can already be linked into the live
 * graph, so it and its subtree have to survive. How many that is depends on the mutator's
 * ALLOCATION RATE, not on the live set, and the pass pushed all of them onto a fixed
 * 65536-entry worklist before draining any of it. A thread churning small short-lived
 * objects produces several times that per cycle, so the worklist overflowed as a matter
 * of course.</p>
 *
 * <p>Overflow is survivable -- the dropped entries are already marked, and the belt
 * re-discovers their children -- but the belt is a full O(heap) rescan. It makes the
 * cycle several times longer, the mutator produces proportionally more fresh objects
 * before the next cycle, and that one overflows for certain: the collector never returns
 * to its fast path. Measured on this workload before the fix, on an idle laptop:
 * footprint 90MB to 6.2GB in 20 seconds against a live set of a few hundred bytes, cycle
 * time 6ms to 750ms, and every cycle after the first tip overflowing. On an iPad that is
 * a kill by the per-process ceiling; with #5563's pacing holding the process under that
 * ceiling it is instead a thread parked on nearly every allocation, which is the frozen
 * app the reporter saw next.</p>
 *
 * <p>The fix drains the worklist as the pass walks, so it cannot overflow by sheer
 * volume. What is asserted is that property -- ZERO overflow cycles -- rather than a
 * footprint number, because overflow is a property of the code while a peak is largely a
 * property of the runner.</p>
 *
 * <p>The main run is made under a simulated per-process ceiling
 * ({@code CN1_SIMULATE_PROC_MEMORY_LIMIT}), which is the situation the bug was reported
 * from. A second run of the same binary with NO ceiling covers the reporter's other
 * observation -- the simulator, where nothing kills the process and its footprint simply
 * climbed. That used to be out of scope here: off a ceiling the pacing cap is a fraction
 * of the HOST's free RAM, so on a roomy developer machine this workload was licensed to
 * accumulate more than ten gigabytes before anything stalled it. It is now bounded by a
 * multiple of the collection trigger once the process is already large, and the run pins
 * the free-memory reading ({@code CN1_SIMULATE_FREE_MEMORY}) so the guard means the same
 * thing on an idle machine and a busy one.</p>
 *
 * <p>Measured on this workload with the interleaved drain ablated out and everything else
 * in place, under the same ceiling: 77 of 150 cycles overflowed, the mutator parked 72
 * times, the run took 10.2s and rode 64MB from the ceiling. With the fix: 0 of 440 cycles
 * overflowed, zero parks, 6.8s, 174MB of headroom left. Both finish -- what a regression
 * costs here is the collector, not the result.</p>
 *
 * <p>The same workload later exposed the other half of the same failure, and this class
 * guards that too. With the worklist no longer overflowing, three quarters of the
 * collector's wall time turned out to be {@code cn1ConservativeResolve}: the guard on
 * gcMarkObject runs once per reference field the drain follows, and it answered by
 * binary-searching the page and extent snapshots. Marking therefore cost O(log heap) per
 * field -- the collector got slower as the heap grew, which is the reporter's "GC pauses
 * become more and more frequent and take longer, until they are effectively continuous".
 * The mutator allocated 4.8 collection triggers' worth during every cycle the collector
 * managed to finish, and the process settled at 447MB against a live set of a few hundred
 * bytes, 64MB below the ceiling that kills it. Both indices are now hash tables, and the
 * ratio is 1.04.</p>
 *
 * <p>That ratio is REPORTED rather than asserted -- see
 * {@link #TRIGGERS_PER_CYCLE_IS_DIAGNOSTIC_ONLY}. It measures how far the collector falls
 * behind the mutator, and under CPU contention that is a property of the runner rather
 * than of the code: a starved fixed collector and an unstarved broken one produce the
 * same number. Everything this class ASSERTS is a property of the code and is enforced on
 * every run, contended or not -- zero worklist overflows, the bound on full drains taken
 * inside a grace pass, staying under the ceiling, and the no-ceiling footprint bound.</p>
 *
 * <p>Tagged {@code benchmark}: it needs a translate-and-build and churns several GB.</p>
 */
@Tag("benchmark")
class GcOverflowSpiralIntegrationTest {

    /**
     * Bound on a single translated-binary run. The workload takes a few seconds; this is
     * two orders of magnitude above that, so it can only be reached by a run that is not
     * progressing -- which is itself one of the reported symptoms, so a timeout here is a
     * result rather than an infrastructure problem.
     */
    private static final long VM_RUN_TIMEOUT_SECONDS = 300;

    /**
     * Synthetic per-process ceiling, standing in for the iPad's. Well above what this
     * workload needs when the collector keeps up (it settles around 315MB), and the only
     * configuration in which the run is bounded on every host -- see the class comment.
     * {@code cn1ProcFootprintBytes} backs the hook with phys_footprint on Apple and
     * /proc/self/statm on Linux; where neither exists (Windows) the hook is inert and the
     * run falls back to the 72MB static volume cap, which bounds it just as well.
     */
    private static final long SIMULATED_LIMIT_BYTES = 512L * 1024 * 1024;

    /**
     * Bound on the no-ceiling run's peak footprint. Deliberately far above what the
     * workload needs (measured 163-321MB on a Mac with the cap bounded, and 72-200MB on
     * hosts where cn1_available_memory has no reading and the static cap applies) and far
     * below what an unbounded cap produces (15.6GB). A run between those is not a
     * borderline result, it is the cap keying off the wrong quantity again.
     */
    private static final long UNBOUNDED_PEAK_LIMIT_KB = 2L * 1024 * 1024;

    /**
     * Host free-memory reading pinned for the no-ceiling run. 32GB: a roomy developer
     * machine, which is the condition under which the unbounded cap does its worst
     * (measured with it: 13.8-15.7GB peak and 12.2-13.4s, against 819-861MB and 8.1-8.6s
     * with the bound -- the runaway is slower as well as larger, since a process
     * thrashing fifteen gigabytes pays for them).
     */
    private static final long SIMULATED_FREE_MEMORY_BYTES = 32L * 1024 * 1024 * 1024;

    /**
     * TRIGGERS-PER-CYCLE IS REPORTED, NOT ASSERTED, and this is where the reason lives.
     *
     * <p>The collector and the mutator do not slow down together under CPU contention.
     * The mutator is one hot allocation loop; a collection has to interleave a mark, a
     * sweep and a page walk with it, so the collector is the one that loses. Measured on
     * this workload: 1.04 with a core to itself, 2.3-2.7 with eight copies on twelve
     * cores, 3.3 with sixteen, and 4.4 on a four-vCPU CI runner running four test forks
     * (that job took 77954ms against the 5804ms this workload needs alone). Before the
     * resolver was made O(1) it was 4.0-4.8 at every one of those levels, because the old
     * collector was bound by its own cost rather than by the CPU it could get. The two
     * converge as the machine is oversubscribed, so no fixed threshold separates them
     * there.</p>
     *
     * <p>An earlier revision gated the assertion on this run's own elapsed time instead.
     * That was worse than not asserting: a collector regression makes the workload slow,
     * so the regression could trip its own gate and take the build green. A guard that
     * the fault disables is not a guard. What IS asserted is the footprint, below --
     * bounded by the code rather than by the runner, and measured to hold under the same
     * contention that destroys the ratio.</p>
     */
    private static final String TRIGGERS_PER_CYCLE_IS_DIAGNOSTIC_ONLY = "see the javadoc";

    @Test
    void aChurningWorkerNeverOverflowsTheMarkWorklist() throws Exception {
        Parser.cleanup();

        List<Path> tempDirs = new ArrayList<>();
        try {
            runSpiralLoad(tempDirs);
        } finally {
            for (Path dir : tempDirs) {
                deleteRecursively(dir);
            }
        }
    }

    private void runSpiralLoad(List<Path> tempDirs) throws Exception {
        Path sourceDir = Files.createTempDirectory("gc-overflow-sources");
        Path classesDir = Files.createTempDirectory("gc-overflow-classes");
        Path javaApiDir = Files.createTempDirectory("gc-overflow-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("GcOverflowSpiralApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the GC overflow spiral test");
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
                "GcOverflowSpiralApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaResult = extractLine(runJavaMain(config, classesDir, javaApiDir), "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce RESULT=");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("gc-overflow-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "GcOverflowSpiralApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "GcOverflowSpiralApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> cmakeArgs = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        cmakeArgs.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(cmakeArgs, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("GcOverflowSpiralApp"));
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");

        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_LOG_GC_OVERFLOW", "1");
        env.put("CN1_LOG_PACING_PARKS", "1");
        env.put("CN1_SIMULATE_PROC_MEMORY_LIMIT", Long.toString(SIMULATED_LIMIT_BYTES));
        String output = runVm(executable, buildDir, env);

        assertTrue(output.contains("GC_OVERFLOW_SPIRAL_DONE"),
                "The search must finish. Output: " + output);
        assertEquals(javaResult, extractLine(output, "RESULT="),
                "JavaSE and ParparVM should agree\n--- ParparVM ---\n" + output);

        long overflowCycles = parseTrace(output, "overflowCycles=");
        long graceDrains = parseTrace(output, "graceDrains=");
        long cycles = parseTrace(output, "cycles=");
        long peakKb = parseValue(output, "PEAK_FOOTPRINT_KB=");
        System.err.println("[GcOverflowSpiralIntegrationTest] cycles=" + cycles
                + " overflowCycles=" + overflowCycles + " graceDrains=" + graceDrains
                + " peakKb=" + peakKb + " elapsedMs=" + parseValue(output, "ELAPSED_MS="));

        // THE PROPERTY UNDER TEST. Not "few" overflows: none. This workload's live set is
        // one path through the tree, so nothing here can legitimately fill a worklist
        // sized for a whole constant pool -- every overflow would be pure allocation
        // volume, and each one costs the belt's full O(heap) rescan.
        assertEquals(0, overflowCycles,
                "The mark worklist overflowed on " + overflowCycles + " of " + cycles
                        + " collection cycles. Each overflow arms the belt's O(heap)"
                        + " rescan, which lengthens the cycle, which leaves more fresh"
                        + " objects for the next one to push -- the issue-5537 spiral, at"
                        + " the end of which the process is killed by the iOS memory"
                        + " ceiling or parked on nearly every allocation under it."
                        + "\n--- run ---\n" + output);

        // AND THAT THE FIX IS WHAT PREVENTED IT. Zero overflows is also what a run that
        // never allocated much would report, so the guard needs evidence that the pass
        // actually reached the drain threshold and drained instead of overflowing.
        assertTrue(graceDrains > 0,
                "The grace pass never drained mid-walk, so this workload never pushed"
                        + " enough to approach the worklist limit and the assertion above"
                        + " proves nothing. Check that the app still allocates small"
                        + " reference-carrying objects at volume.\n--- run ---\n" + output);

        // AND THAT THE PERIODIC DRAIN STAYED CHEAP. gcMarkDrain is not "drain the
        // worklist": every call also rescans allObjectsInHeap from index 0 and re-runs the
        // mark function of everything already marked. That is affordable for the ONE call
        // that ends each grace pass and quadratic for a caller that drains periodically --
        // the first cut at this fix pointed the interleaved drain at it, passed every test
        // here, and hung the Mac Catalyst screenshot suite outright, whose retained graph
        // is a live UI rather than a micro-benchmark's handful of objects. So the count of
        // full drains taken INSIDE a grace pass must track the number of passes (two per
        // cycle) and not the number of periodic drains, which is far larger.
        long graceFullDrains = parseTrace(output, "graceFullDrains=");
        long fullDrains = parseTrace(output, "fullDrains=");
        assertTrue(graceFullDrains <= 2 * cycles + 4,
                "The grace passes made " + graceFullDrains + " full heap-rescanning drains"
                        + " across " + cycles + " cycles, where two per cycle -- one to end"
                        + " each pass -- is all they may make. The extra ones are periodic"
                        + " drains calling gcMarkDrain instead of gcMarkDrainWorklist, which"
                        + " makes a pass quadratic in the heap; that is what hung the Mac"
                        + " Catalyst suite. (graceDrains=" + graceDrains + " fullDrains="
                        + fullDrains + ")\n--- run ---\n" + output);

        // AND THE USER-VISIBLE PROPERTY the collector is there to provide: a live set of
        // one path through a game tree stays inside a ceiling this workload never needs
        // to approach. Under a real ceiling, crossing it is the process being killed.
        assertTrue(peakKb < SIMULATED_LIMIT_BYTES / 1024,
                "Peaked at " + peakKb + "KB against a "
                        + (SIMULATED_LIMIT_BYTES / (1024 * 1024)) + "MB process budget,"
                        + " so on a device this process would have been killed.\n--- run ---\n"
                        + output);

        // AND THAT THE COLLECTOR ACTUALLY KEEPS UP, which is the property underneath
        // every number above. A collection is scheduled once the mutator has allocated a
        // trigger's worth, so a collector that finishes before the next trigger crossing
        // completes one cycle per trigger. One that does not simply coalesces the
        // crossings it missed into the cycle it did manage -- so bytes-allocated divided
        // by cycles-completed measures exactly how far behind it is, in units of the
        // trigger.
        //
        // That ratio is what this issue was about. Before the resolver index was made
        // O(1) the collector spent three quarters of its time inside
        // cn1ConservativeResolve, walking a binary search over the page and extent
        // snapshots once per reference field the drain followed. Its cost therefore grew
        // with the SIZE OF THE HEAP rather than with the live set: cycles stretched, the
        // mutator produced proportionally more garbage during each one, and the footprint
        // settled wherever the pacing let it -- on this workload 4.8 triggers per cycle
        // and a peak of 447MB against a live set of a few hundred bytes, riding the
        // ceiling with 64MB to spare. With the fix it is 1.04 and 116-165MB. (Both
        // measured on this workload, same host, same 512MB simulated ceiling: 130
        // cycles for 14.9GB allocated against 583 for the same 14.9GB.)
        //
        // REPORTED, NOT ASSERTED -- see TRIGGERS_PER_CYCLE_IS_DIAGNOSTIC_ONLY.
        long allocatedKb = parseTrace(output, "allocatedKb=");
        long triggerKb = parseTrace(output, "triggerKb=");
        assertTrue(cycles > 0 && triggerKb > 0,
                "The tracer reported no cycles or no trigger, so the ratio below cannot be"
                        + " computed.\n--- run ---\n" + output);
        double triggersPerCycle = (double) allocatedKb / (double) cycles / (double) triggerKb;
        long elapsedMs = parseValue(output, "ELAPSED_MS=");
        System.err.println("[GcOverflowSpiralIntegrationTest] triggersPerCycle="
                + String.format("%.2f", triggersPerCycle) + " (diagnostic; ~1 with a core"
                + " to itself, higher the more the runner is oversubscribed) elapsedMs="
                + elapsedMs);

        // AND THE SAME WORKLOAD WITH NO CEILING AT ALL, which is the reporter's other
        // observation: in the simulator nothing kills the process, so instead of dying it
        // just grew. Nothing here fails an app the way a jetsam kill does, which is why
        // this half went unguarded, but a collector that lets a four-megabyte live set
        // reach fifteen gigabytes is broken wherever it runs -- and it is what a developer
        // sees first, before ever reaching a device.
        //
        // WHAT THIS DOES AND DOES NOT PROVE. It is a BOUND, not a reproduction. Off a
        // ceiling the runaway is bistable: it needs the collector to lose the race early
        // and never be pulled back, which on a 12-core host took EIGHT concurrent copies
        // of this workload to provoke (two, three and four did not). A single process on
        // an idle machine stays under 350MB with the growth bound ablated out, so a green
        // run here is not evidence that the bound works -- what it catches is the gross
        // regression, a collector that has stopped keeping up at all. The bound itself was
        // measured under that eight-way contention: 13.8-15.7GB without it, 819-861MB with.
        //
        // Reuses the binary the run above already built; only the environment differs.
        // CN1_SIMULATE_FREE_MEMORY pins the host reading the pacing cap is a fraction of.
        // Without it this assertion is worthless in both directions: the cap is free RAM
        // over eight (or over two for a thread allocating hard), so the same binary on the
        // same machine reaches 15.7GB when the machine is idle-and-roomy and stays under
        // 300MB an hour later when it is not. Pinning it high is the hostile case.
        Map<String, String> unboundedEnv = new HashMap<String, String>();
        unboundedEnv.put("CN1_SIMULATE_FREE_MEMORY", Long.toString(SIMULATED_FREE_MEMORY_BYTES));
        String unbounded = runVm(executable, buildDir, unboundedEnv);
        assertTrue(unbounded.contains("GC_OVERFLOW_SPIRAL_DONE"),
                "The unbounded search must finish too. Output: " + unbounded);
        long unboundedPeakKb = parseValue(unbounded, "PEAK_FOOTPRINT_KB=");
        long unboundedElapsedMs = parseValue(unbounded, "ELAPSED_MS=");
        System.err.println("[GcOverflowSpiralIntegrationTest] noCeilingPeakKb=" + unboundedPeakKb
                + " elapsedMs=" + unboundedElapsedMs);
        // ENFORCED UNCONDITIONALLY, unlike the ratio above, because unlike the ratio this
        // is bounded by the code rather than by the runner. An earlier revision gated it
        // on elapsed time and was wrong twice over: the regression it guards makes the
        // run slow, so it could trip its own gate; and the gate was there because the
        // bound genuinely did not hold under starvation. It does now -- the footprint the
        // bound keys off is re-probed as allocations cross the pacing intervals instead of
        // once per collection, so a long cycle can no longer keep the clamp disarmed
        // through the one interval that matters. Measured across sixteen concurrent copies
        // on twelve cores: 871-994MB, against 735MB-15.7GB before that probe was fixed.
        assertTrue(unboundedPeakKb < UNBOUNDED_PEAK_LIMIT_KB,
                "With no process ceiling the workload peaked at " + unboundedPeakKb
                        + "KB against a live set of a few hundred bytes (run took "
                        + unboundedElapsedMs + "ms). The pacing cap is meant to be bounded"
                        + " by a multiple of the collection trigger"
                        + " (CN1_BIBOP_GC_MAX_CAP_MULTIPLIER), which tracks the heap; a"
                        + " number this size means it is tracking the HOST's free RAM"
                        + " again, and the app grows until the machine complains. If this"
                        + " fired on a heavily loaded runner, check cn1PacingPastGrowthFloor"
                        + " -- the bound depends on the footprint being re-probed rather"
                        + " than read from the once-per-cycle cache."
                        + "\n--- run ---\n" + unbounded);
    }

    private long parseTrace(String output, String key) {
        for (String line : output.split("\\R")) {
            if (line.startsWith("[GC-OVERFLOW] ")) {
                for (String token : line.trim().split("\\s+")) {
                    if (token.startsWith(key)) {
                        return Long.parseLong(token.substring(key.length()).trim());
                    }
                }
            }
        }
        fail("VM did not report " + key + " -- the CN1_LOG_GC_OVERFLOW tracer did not fire,"
                + " so the guard cannot observe whether the worklist overflowed. Output: "
                + output);
        return -1;
    }

    /** Same preference order as the other translate-and-build guards. */
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

    private long parseValue(String output, String prefix) {
        String line = extractLine(output, prefix);
        if (line.isEmpty()) {
            fail("Missing " + prefix + " in output: " + output);
        }
        return Long.parseLong(line.substring(prefix.length()).trim());
    }

    private String extractLine(String output, String prefix) {
        for (String line : output.split("\\R")) {
            if (line.startsWith(prefix)) {
                return line.trim();
            }
        }
        return "";
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = GcOverflowSpiralIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/GcOverflowSpiralApp.java");
        assertNotNull(in, "GcOverflowSpiralApp.java test resource should exist");
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
                "GcOverflowSpiralApp");
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

    /**
     * Runs the translated binary under a bounded wait, draining its output concurrently.
     * Both halves matter: a regressed collector's failure mode under a budget is a run
     * that never finishes, so the wait is bounded and the child killed on expiry, and the
     * drain runs on its own thread so a child that fills the pipe buffer cannot deadlock
     * against our wait.
     */
    private String runVm(Path executable, Path workingDir, Map<String, String> env) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toString());
        builder.directory(workingDir.toFile());
        builder.environment().putAll(env);
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
                // The stream ends abruptly when a timed-out child is destroyed; whatever
                // was captured before that is exactly what we want to report.
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
                "ParparVM run did not finish within " + VM_RUN_TIMEOUT_SECONDS + "s (env "
                        + env + "). For this guard that is a result: a collector stuck in"
                        + " the overflow spiral under a memory budget parks the allocating"
                        + " thread on nearly every allocation. Output so far: " + output);
        assertEquals(0, process.exitValue(),
                "ParparVM run should exit cleanly (env " + env + "). Output: " + output);
        return output;
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        // best effort cleanup of a temp tree
                    }
                });
    }
}
