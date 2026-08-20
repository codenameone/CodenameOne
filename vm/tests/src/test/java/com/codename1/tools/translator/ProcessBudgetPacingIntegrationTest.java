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
 * Regression guard for the issue-5537 process-budget pacing fix.
 *
 * <p>The GC's backpressure decides how far a mutator may run ahead of the collector,
 * and it was sized against the DEVICE's free RAM. On a platform with a per-process
 * memory ceiling those are unrelated quantities: iOS kills an app that crosses its
 * dirty-memory limit -- roughly 1.4GB on an iPad -- with EXC_RESOURCE
 * (RESOURCE_TYPE_MEMORY), whatever the device has spare. cn1BibopPacingCap handed a
 * high-throughput thread half of the host-wide reclaimable figure, which on a
 * large-RAM iPad is gigabytes, so the mutator was licensed to run further ahead of
 * the collector than the process was allowed to exist. A deep game-tree search hit
 * that reproducibly on device while running fine in the simulator, on Android and on
 * Windows -- none of which impose such a ceiling.</p>
 *
 * <p>Two things were wrong and both are covered here. The cap now comes from
 * {@code cn1ProcessHeadroom} (os_proc_available_memory on iOS), and it is clamped to
 * half the REMAINING budget so the 72MB static floor cannot authorize 72MB of fresh
 * garbage with 10MB left to live. And the legacy allocation path -- everything above
 * CN1_BIBOP_MAX_OBJECT, which is every array a program allocates -- now has byte-based
 * backpressure at all; it previously had only an asynchronous trigger that scheduled a
 * cycle without ever making the allocating thread wait.</p>
 *
 * <p>The clamp only engages under a hard per-process budget, and the only targets that
 * impose one are iOS/tvOS/watchOS devices -- which is precisely why the defect survived
 * so long. The {@code CN1_SIMULATE_PROC_MEMORY_LIMIT} hook supplies a synthetic ceiling
 * so the mechanism is reachable on a machine CI can actually run, in the same spirit as
 * {@code CN1_SIMULATE_MEMORY_WARNING_MS} in {@link LowMemoryThrottleIntegrationTest}.</p>
 *
 * <p>The same binary is run twice over the same workload, so the only variable is
 * whether a budget is declared:</p>
 *
 * <ul>
 * <li><b>Control</b> -- no hook. The host-wide reading applies exactly as before and
 *     nothing may pace, which is asserted directly on the park counters. That is the
 *     evidence the fix costs nothing off iOS, and it is deterministic.</li>
 * <li><b>Treatment</b> -- a {@value #SIMULATED_LIMIT_MB}MB budget. The peak must stay
 *     inside it.</li>
 * </ul>
 *
 * <p>The control's PEAK is reported but deliberately not asserted on. Whether an
 * unpaced mutator actually outruns the collector is up to the scheduler: the identical
 * binary was measured peaking anywhere from 98MB to 815MB across runs on an idle
 * laptop. The bounded run's park COUNT is not asserted either, for the same reason
 * (0, 1, 2, 6 and 8 across repetitions). What does not vary is that the bound holds --
 * across every run measured the bounded peak stayed between 95MB and 216MB against a
 * 256MB budget -- and that the control paces nothing at all.</p>
 *
 * <p>Teeth were confirmed by ablation rather than assumed. With the legacy-path
 * backpressure removed and everything else in place, the bounded run peaks at 472MB
 * against the 256MB budget and this guard fails; with the fix whole it peaks at
 * 131MB.</p>
 *
 * <p>The bound asserted is not a tuned threshold, it is the property the clamp
 * provides: every pacing evaluation re-reads the LIVE remaining budget, so at footprint
 * F under budget L the thread is authorized to grow to F + (L-F)/2 = (L+F)/2, which is
 * below L for every F. Note what this does and does not say. The footprint RATCHETS --
 * successive pace points measured at 176MB, 216MB, 236MB under a 256MB budget --
 * because the cap bounds uncollected allocation volume while the footprint also carries
 * memory the collector has freed but not yet handed back. So the peak converges on the
 * ceiling rather than sitting at a fixed fraction of it, and a longer workload gets
 * closer to it. What holds regardless is that it converges from BELOW: each step is
 * half of what is left, so no amount of churn crosses the line. A regression that
 * restores host-wide sizing, or drops the legacy path's backpressure, does not converge
 * at all and blows straight through.</p>
 *
 * <p>Tagged {@code benchmark}: it needs a translate-and-build and churns
 * ~768MB through a live set of one block.</p>
 */
@Tag("benchmark")
class ProcessBudgetPacingIntegrationTest {

    /**
     * Synthetic per-process budget for the treatment run. Comfortably above the
     * baseline footprint of a translated hello-world plus one live block, so the
     * workload is never starved of room to run, and far below what the control run
     * reaches -- otherwise the guard would pass on a machine where the collector
     * happened to keep up.
     */
    private static final long SIMULATED_LIMIT_MB = 256;

    private static final long SIMULATED_LIMIT_BYTES = SIMULATED_LIMIT_MB * 1024 * 1024;
    private static final long SIMULATED_LIMIT_KB = SIMULATED_LIMIT_MB * 1024;

    /**
     * A second, deliberately tight budget whose only job is to prove the treatment path
     * actually runs. The peak assertion alone cannot: on a runner whose collector keeps
     * up unaided, a bounded run reaches neither the cap nor a single park, and would
     * report green with the clamp and the legacy backpressure both removed.
     *
     * <p>This budget is barely above the process's own structural floor -- about 98MB
     * of arena and mapped pages that no amount of pacing can move -- so the remaining
     * headroom is a few MB and the cap is half of that. The workload churns 768MB
     * through it, and the collector's own scheduling trigger is 24MB, so uncollected
     * volume cannot stay under a single-digit-MB cap for the length of the run: pacing
     * has to engage. Tightening it further only makes engagement more certain, because
     * headroom at or below zero drives the cap to zero.</p>
     *
     * <p>Its PEAK is deliberately not asserted. Below the structural floor there is
     * nothing left for backpressure to buy, and a peak assertion there would only be
     * checking that an already-doomed process stays doomed.</p>
     */
    private static final long TIGHT_LIMIT_MB = 120;

    private static final long TIGHT_LIMIT_BYTES = TIGHT_LIMIT_MB * 1024 * 1024;

    /**
     * The smallest pacing cap reachable WITHOUT a process budget, and therefore the
     * dividing line between the two sizings.
     *
     * <p>Off the budget path every branch of {@code cn1BibopPacingCap} takes the larger
     * of a fraction of host RAM and {@code base}, where {@code base} is
     * {@code bibopGcTriggerBytes * CN1_BIBOP_GC_HARD_CAP_MULTIPLIER}. The trigger is
     * adaptive but clamped to never fall below {@code CN1_BIBOP_GC_TRIGGER_BYTES} (24MB)
     * in either direction, so {@code base} is at least 3 x 24MB and the unbounded cap can
     * never be less. Only the process-budget clamp can produce a smaller one.</p>
     *
     * <p>That is what makes a cap value, unlike a park count, able to tell the two apart.
     * A park count cannot: this workload churns 768MB, which parks against a 72MB static
     * cap just as readily as against a budget-derived one, so a regression to host-wide
     * sizing would keep {@code legacyParks > 0} green while restoring the device bug --
     * iOS host-wide headroom yields a gigabyte-scale cap.</p>
     */
    private static final long STATIC_CAP_FLOOR_KB = 3 * 24 * 1024;

    /**
     * Keep in sync with CN1_PACING_HEADROOM_MARGIN in cn1_globals.m. A budgeted thread is
     * admitted only while the remaining budget still covers its block plus this, so under
     * sustained allocation the observed headroom settles just above it.
     */
    private static final long HEADROOM_MARGIN_KB = 64 * 1024;

    /**
     * Slack over the margin for the headroom assertion. Generous, because the assertion's
     * job is to separate a budget-derived figure from a host-wide one -- two readings
     * orders of magnitude apart -- not to pin down where exactly headroom bottomed out.
     */
    private static final long HEADROOM_MARGIN_SLACK_KB = 8 * 1024;

    /**
     * Bound on a single translated-binary run. The workload takes a few seconds; this is
     * two orders of magnitude above that, so it can only be reached by a run that is not
     * progressing. Generous on purpose -- it exists to turn a stall into a failure, not
     * to police performance.
     */
    private static final long VM_RUN_TIMEOUT_SECONDS = 300;

    @Test
    void anAllocatingThreadStaysInsideTheProcessMemoryBudget() throws Exception {
        Parser.cleanup();

        List<Path> tempDirs = new ArrayList<>();
        try {
            runPacingLoad(tempDirs);
        } finally {
            for (Path dir : tempDirs) {
                deleteRecursively(dir);
            }
        }
    }

    private void runPacingLoad(List<Path> tempDirs) throws Exception {
        Path sourceDir = Files.createTempDirectory("process-budget-pacing-sources");
        Path classesDir = Files.createTempDirectory("process-budget-pacing-classes");
        Path javaApiDir = Files.createTempDirectory("process-budget-pacing-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("ProcessBudgetPacingApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for process-budget pacing integration test");
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
                "ProcessBudgetPacingApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaResult = extractLine(runJavaMain(config, classesDir, javaApiDir), "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce RESULT=");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("process-budget-pacing-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ProcessBudgetPacingApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "ProcessBudgetPacingApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> cmakeArgs = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        cmakeArgs.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(cmakeArgs, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("ProcessBudgetPacingApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");

        // CONTROL: no declared budget, so cn1ProcessHeadroom reports "no limit", the
        // host-wide reading applies exactly as before and nothing here may pace.
        Map<String, String> controlEnv = new HashMap<String, String>();
        controlEnv.put("CN1_LOG_PACING_PARKS", "1");
        String controlOutput = runVm(executable, buildDir, controlEnv);
        assertEquals(javaResult, extractLine(controlOutput, "RESULT="),
                "JavaSE and ParparVM should agree\n--- ParparVM control ---\n" + controlOutput);
        long controlPeakKb = parseValue(controlOutput, "PEAK_FOOTPRINT_KB=");

        // TREATMENT: identical binary, identical work, a declared budget.
        Map<String, String> boundedEnv = new HashMap<String, String>();
        boundedEnv.put("CN1_LOG_PACING_PARKS", "1");
        boundedEnv.put("CN1_SIMULATE_PROC_MEMORY_LIMIT", Long.toString(SIMULATED_LIMIT_BYTES));
        String boundedOutput = runVm(executable, buildDir, boundedEnv);
        assertTrue(boundedOutput.contains("PROCESS_BUDGET_PACING_DONE"),
                "The load must finish under a memory budget, not stall. Output: " + boundedOutput);
        assertEquals(javaResult, extractLine(boundedOutput, "RESULT="),
                "Pacing must not change the result\n--- ParparVM bounded ---\n" + boundedOutput);
        long boundedPeakKb = parseValue(boundedOutput, "PEAK_FOOTPRINT_KB=");

        System.err.println("[ProcessBudgetPacingIntegrationTest] controlPeakKb=" + controlPeakKb
                + " boundedPeakKb=" + boundedPeakKb + " limitKb=" + SIMULATED_LIMIT_KB
                + " control " + pacing(controlOutput) + " bounded " + pacing(boundedOutput));

        // THE PROPERTY UNDER TEST. Under a declared budget the peak must stay inside
        // it. Not a tuned threshold: every pacing evaluation re-reads the live remaining
        // budget, so at footprint F under budget L the thread is authorized to reach
        // F + (L-F)/2 = (L+F)/2 < L, for every F. The peak converges on the ceiling
        // rather than settling at a fraction of it -- it also carries memory freed but
        // not yet returned -- but it converges from below and cannot cross. Measured
        // with the legacy-path backpressure ablated out and everything else in place,
        // the same run peaked at 472MB against this 256MB budget: dead on device.
        assertTrue(boundedPeakKb < SIMULATED_LIMIT_KB,
                "Under a declared " + SIMULATED_LIMIT_KB + "KB process budget the allocating"
                        + " thread peaked at " + boundedPeakKb + "KB, so the process would have"
                        + " been killed. This is the issue-5537 signature: backpressure sized"
                        + " against something other than the budget the process is metered"
                        + " against. The unbounded control peaked at " + controlPeakKb + "KB."
                        + "\n--- bounded ---\n" + boundedOutput);

        // AND THE OTHER DIRECTION, which is what keeps this fix from costing throughput
        // everywhere else: with no budget declared, no allocation may be paced at all.
        // Load-bearing rather than decorative. cn1_available_memory is a flat 100MB
        // placeholder off Apple, so an unbudgeted legacy path paced against it would sit
        // at the 72MB static floor and engage constantly -- measured, before the legacy
        // park was scoped to budgeted platforms, this control run parked 10 times on a
        // Linux CI runner that was never in any danger.
        // Unlike a peak, this is deterministic -- it is a property of the code rather
        // than of how loaded the machine is -- so it is the half of the guard that
        // cannot go quiet. (The bounded run's park count is NOT asserted for exactly
        // that reason: measured at 0, 1, 2 and 8 across repetitions of an identical run,
        // because whether the mutator outruns the collector at all is up to the
        // scheduler. Its peak is bounded either way, which is the property that matters.)
        assertEquals(0, parsePacing(controlOutput, "legacyParks="),
                "No process budget was declared, so the legacy path must not pace a single"
                        + " allocation -- pacing where there is no ceiling to respect is pure"
                        + " throughput loss on every non-iOS target.\n--- control ---\n"
                        + controlOutput);
        assertEquals(0, parsePacing(controlOutput, "bibopParks="),
                "No process budget was declared, so the BiBOP path must not pace either."
                        + "\n--- control ---\n" + controlOutput);

        // AND THAT THE TREATMENT PATH ACTUALLY RUNS. Everything above is consistent with
        // a collector that simply kept up and a fix that does nothing, so the guard needs
        // one run where pacing cannot be avoided. See TIGHT_LIMIT_MB.
        Map<String, String> tightEnv = new HashMap<String, String>();
        tightEnv.put("CN1_LOG_PACING_PARKS", "1");
        tightEnv.put("CN1_SIMULATE_PROC_MEMORY_LIMIT", Long.toString(TIGHT_LIMIT_BYTES));
        String tightOutput = runVm(executable, buildDir, tightEnv);
        long tightParks = parsePacing(tightOutput, "legacyParks=");
        System.err.println("[ProcessBudgetPacingIntegrationTest] tightLimitMb=" + TIGHT_LIMIT_MB
                + " " + pacing(tightOutput)
                + " peakKb=" + parseValue(tightOutput, "PEAK_FOOTPRINT_KB="));

        assertTrue(tightParks > 0,
                "A " + TIGHT_LIMIT_MB + "MB budget leaves a cap of a few MB, and this"
                        + " workload churns 768MB through it, so the legacy path must have"
                        + " paced at least once. Zero parks means the backpressure never"
                        + " engaged, which makes every other assertion here vacuous -- they"
                        + " would all pass with the fix removed.\n--- tight ---\n"
                        + tightOutput);

        // Backpressure must produce reclamation, not just delay. A park waits for the
        // cycle boundary that resets the volume counter, so if nothing schedules a cycle
        // the thread spins out its whole safety budget and resumes having achieved
        // nothing -- once per check, which at this cap is most of the run. That failure
        // shows up here as a run that does not finish rather than as a bad number.
        assertTrue(tightOutput.contains("PROCESS_BUDGET_PACING_DONE"),
                "The load must still finish under a budget tight enough to pace it on"
                        + " nearly every check. Not finishing is the signature of a park"
                        + " that waits on a collection nobody scheduled.\n--- tight ---\n"
                        + tightOutput);

        // AND THAT THE BUDGET IS WHAT SIZED THE CAP. Everything above is still consistent
        // with a cap that ignored the budget: 768MB of churn parks against the 72MB static
        // cap too, and that same static cap could hold the 256MB run under its limit. The
        // cap VALUE separates them, because 72MB is a hard floor everywhere except the
        // budget clamp -- see STATIC_CAP_FLOOR_KB.
        // The park counts above cannot, on their own, tell budget-driven admission from
        // the unbudgeted volume cap -- both park. boundedChecks can, because it counts
        // only decisions actually made against a live process budget. This is the pair of
        // assertions that would fail if cn1ProcessHeadroom regressed to reporting
        // host-wide memory: the control would start taking the bounded path, and the
        // tight run's observed headroom would no longer sit inside its declared budget.
        assertTrue(parsePacing(tightOutput, "boundedChecks=") > 0,
                "Under a " + TIGHT_LIMIT_MB + "MB budget no admission decision was made"
                        + " against a process budget at all, so the parks above prove"
                        + " nothing about the fix.\n--- tight ---\n" + tightOutput);

        long tightMinHeadroomKb = parsePacing(tightOutput, "minHeadroomKb=");
        long headroomBound = HEADROOM_MARGIN_KB + HEADROOM_MARGIN_SLACK_KB;
        assertTrue(tightMinHeadroomKb >= 0 && tightMinHeadroomKb < headroomBound,
                "Under a " + TIGHT_LIMIT_MB + "MB budget the least remaining headroom"
                        + " observed was " + tightMinHeadroomKb + "KB, not the ~"
                        + HEADROOM_MARGIN_KB + "KB margin that sustained allocation against"
                        + " a real budget settles at. The figure being metered is therefore"
                        + " not the budget: a host-wide reading is orders of magnitude"
                        + " larger, and on device that is the original bug."
                        + "\n--- tight ---\n" + tightOutput);

        assertEquals(0, parsePacing(controlOutput, "boundedChecks="),
                "No budget was declared, so no admission decision may be made against one."
                        + " A non-zero count means a ceiling is being inferred where none"
                        + " exists, which would throttle every non-iOS target.\n"
                        + "--- control ---\n" + controlOutput);

        long controlMinCapKb = parsePacing(controlOutput, "minCapKb=");
        assertTrue(controlMinCapKb >= STATIC_CAP_FLOOR_KB,
                "With no budget declared the BiBOP cap must come from the host-wide reading"
                        + " and its static floor, but the smallest computed was "
                        + controlMinCapKb + "KB, under " + STATIC_CAP_FLOOR_KB + "KB."
                        + "\n--- control ---\n" + controlOutput);
    }

    private long parsePacing(String output, String key) {
        String line = pacing(output);
        for (String token : line.split("\\s+")) {
            if (token.startsWith(key)) {
                return Long.parseLong(token.substring(key.length()).trim());
            }
        }
        fail("VM did not report " + key + " -- the CN1_LOG_PACING_PARKS tracer did not fire, so"
                + " the guard cannot observe whether backpressure engaged. Output: " + output);
        return -1;
    }

    private String pacing(String out) {
        for (String line : out.split("\\R")) {
            if (line.startsWith("[PACING] ")) {
                return line.trim();
            }
        }
        return "[PACING] <absent>";
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

    private String loadAppSource() throws Exception {
        java.io.InputStream in = ProcessBudgetPacingIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/ProcessBudgetPacingApp.java");
        assertNotNull(in, "ProcessBudgetPacingApp.java test resource should exist");
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
                "ProcessBudgetPacingApp");
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
     * Runs the translated binary under a bounded wait, draining its output on a separate
     * thread.
     *
     * <p>Both halves matter here and neither is boilerplate. The behaviour under test is
     * a thread PARKING, and the failure mode of a broken park is a stall -- a run that
     * exhausts its 10s pacing spin on every check, or deadlocks outright. Collecting the
     * stream on this thread would block until the child closed stdout, so that stall
     * would hang the surefire fork until the CI job's global timeout instead of failing:
     * the guard would stop reporting a regression and start eating the build. The wait is
     * therefore bounded and the child is killed on expiry.</p>
     *
     * <p>And the drain has to be concurrent rather than after the wait, or a child that
     * fills the pipe buffer blocks in write() while we block in waitFor(). Draining as we
     * go also means a killed run still yields whatever it printed, which is the only
     * diagnostic a stalled run leaves behind.</p>
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
                // The stream ends abruptly when we destroy a timed-out child. Whatever was
                // captured before that is exactly what we want to report.
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
                        + env + "). For this guard that is a result, not an infrastructure"
                        + " problem: the behaviour under test is a thread parking, and a"
                        + " park that waits on a collection nobody scheduled stalls exactly"
                        + " like this. Output so far: " + output);
        assertEquals(0, process.exitValue(),
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

    private void deleteRecursively(Path dir) throws Exception {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        Files.walk(dir)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Best effort: a leftover temp dir must not fail the guard.
                    }
                });
    }
}
