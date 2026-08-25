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
 * Steady-state gate for the collector (issue #5537).
 *
 * <p>Every other GC test here measures a PEAK under load, and a peak cannot express the
 * failure this issue reported. {@code GcOverflowSpiralIntegrationTest} asserts peak &lt; 2GB
 * over 50 bounded rounds; a heap that grows forever at a modest rate passes it. The
 * reporter's build climbed 500MB to 5GB over five minutes against a live set of a few
 * hundred objects, with GC pauses lengthening until they were continuous -- so the property
 * that had to be asserted, and never was, is that the growth STOPS.</p>
 *
 * <p>The mechanism found underneath it: the SATB write barrier logged a reference on every
 * object store during a mark, and on a churn workload essentially every logged reference
 * was to a FRESH object -- one allocated after the snapshot was taken, which the sweep's
 * grace rule keeps regardless. The log's size is therefore mutation rate x cycle duration,
 * and draining it is part of the cycle, so a longer cycle produced a longer log which
 * produced a longer cycle. Measured before the fix: 2,718,413 fresh references of
 * 2,718,448 logged in one cycle, 282ms of a 327ms mark, page count climbing without bound.
 * Both symptoms fall out of that one loop.</p>
 *
 * <p>This gate builds the workload with {@code -DCN1_GC_CONFORM}, which adds the
 * {@code [GCPROBE]} series and changes no allocator behaviour -- deliberately NOT
 * {@code CN1_GC_VERIFY}, which forces {@code cn1BibopReleaseOffset()} to 0 and so compiles
 * out the page-release and major-sweep paths this measurement depends on.</p>
 *
 * <p>Two assertions, one on the mechanism and one on the outcome, and then a second run
 * that re-injects the defect ({@code -DCN1_SATB_LOG_FRESH}) and requires both to fail. A
 * gate that has never been watched failing proves nothing.</p>
 */
@Tag("benchmark")
class GcSteadyStateIntegrationTest {

    /**
     * Logged references per cycle, as a multiple of the live legacy population. The barrier
     * should only see references the snapshot actually needs, which is bounded by the live
     * set; before the fix it was bounded by the ALLOCATION RATE and ran to millions. The
     * multiple is deliberately loose -- the two regimes are five orders of magnitude apart,
     * so this cannot be made tight enough to flake without also being wrong.
     */
    private static final double MAX_SATB_REFS_PER_LIVE_OBJECT = 4.0;

    /**
     * How much the page heap may still grow in the second half of the run, relative to the
     * first. Zero would be wrong: a run reaches its working set at its own pace and a
     * partially-filled arena is 64 pages. A COMPOUNDING heap doubles here.
     */
    private static final double MAX_SECOND_HALF_PAGE_GROWTH = 0.25;

    /** Cycles needed before the comparison means anything. Anti-vacuousness. */
    private static final int MIN_CYCLES = 24;

    /**
     * Synthetic per-process budget for the ceiling scenario. Well above what this workload
     * needs, so that whatever the process settles at is the pacing policy's doing and not
     * the workload's.
     */
    private static final long CEILING_MB = 1400;

    /**
     * The share of that budget the collector must keep free (CN1_PACING_RESERVE_SHIFT).
     * Asserted at half, because the assertion is about which REGIME the process is in --
     * defending a reserve, or converging on the admission margin -- and those are 300MB
     * and 63MB apart. A tolerance tight enough to distinguish 300 from 280 would be
     * measuring the runner.
     */
    private static final long RESERVE_MB = CEILING_MB / 4;
    private static final long MIN_HEADROOM_MB = RESERVE_MB / 2;

    @Test
    void aChurningWorkloadReachesAWorkingSetAndStaysThere() throws Exception {
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
        Path sourceDir = Files.createTempDirectory("gc-steady-sources");
        Path classesDir = Files.createTempDirectory("gc-steady-classes");
        Path javaApiDir = Files.createTempDirectory("gc-steady-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("GcSteadyStateApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the GC steady-state test");
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
                "GcSteadyStateApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaResult = extractLine(runJavaMain(config, classesDir, javaApiDir), "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce RESULT=");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Path outputDir = Files.createTempDirectory("gc-steady-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "GcSteadyStateApp");
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "GcSteadyStateApp-src");

        // ---- 1. the gate ------------------------------------------------------
        Path fixed = build(distDir, tempDirs, "fixed", "-DCN1_GC_CONFORM");
        Run clean = run(fixed, distDir);
        assertEquals(0, clean.exit, "The workload must finish. Output: " + tail(clean.output));
        assertTrue(clean.output.contains("GC_STEADY_STATE_DONE"),
                "The workload should run to completion. Output: " + tail(clean.output));
        assertEquals(javaResult, extractLine(clean.output, "RESULT="),
                "JavaSE and ParparVM should agree on the workload result");
        Series good = Series.parse(clean.output);
        assertTrue(good.cycles >= MIN_CYCLES,
                "Only " + good.cycles + " collection cycles ran, so the comparison below "
                        + "measured nothing. Output: " + tail(clean.output));
        assertTrue(good.satbRefsPerLiveObject() <= MAX_SATB_REFS_PER_LIVE_OBJECT,
                describe("The SATB log is sized by the allocation rate, not by the live set",
                        good));
        assertTrue(good.secondHalfPageGrowth() <= MAX_SECOND_HALF_PAGE_GROWTH,
                describe("The page heap is still compounding in the second half of the run",
                        good));

        // ---- 2. proof that the gate can fail ----------------------------------
        // CN1_SATB_LOG_FRESH is the escape hatch that restores the pre-fix barrier, so it
        // doubles as the fault injection: without this half, a build in which the probe or
        // the filter silently compiled out would pass part 1 forever.
        Path faulty = build(distDir, tempDirs, "faulted", "-DCN1_GC_CONFORM -DCN1_SATB_LOG_FRESH");
        Run faulted = run(faulty, distDir);
        Series bad = Series.parse(faulted.output);
        assertTrue(bad.cycles >= MIN_CYCLES,
                "The faulted build produced no [GCPROBE] series, so CN1_GC_CONFORM is not "
                        + "active and the clean run above proved nothing. Output: " + tail(faulted.output));
        assertTrue(bad.satbRefsPerLiveObject() > MAX_SATB_REFS_PER_LIVE_OBJECT,
                "Re-injecting the unfiltered SATB barrier did NOT blow the log budget, so "
                        + "this gate is inert. " + describe("faulted run", bad));
        assertTrue(bad.satbRefsPerLiveObject() > good.satbRefsPerLiveObject() * 10,
                "The fresh-reference filter should cut the log by orders of magnitude. "
                        + describe("fixed", good) + " " + describe("faulted", bad));

        // ---- 3. under a per-process ceiling, the collector defends a reserve ----
        // Budget headroom is not a footprint bound: admission answers "is there budget
        // left", so on its own it keeps saying yes until the budget is gone and the
        // process converges on ceiling-minus-margin however small its live set is. That
        // is survivable only until something else spends out of the same budget, which
        // on iOS the renderer does.
        Map<String, String> ceiling = new HashMap<>();
        ceiling.put("CN1_SIMULATE_PROC_MEMORY_LIMIT", Long.toString(CEILING_MB * 1024 * 1024));
        Run bounded = run(fixed, distDir, ceiling);
        assertEquals(0, bounded.exit,
                "The workload must finish under a ceiling. Output: " + tail(bounded.output));
        long boundedHeadroomMb = minHeadroomMb(bounded.output);
        assertTrue(boundedHeadroomMb >= 0,
                "No [PACING] report under a simulated ceiling -- the budgeted path never "
                        + "ran, so this scenario measured nothing. Output: " + tail(bounded.output));
        assertTrue(boundedHeadroomMb >= MIN_HEADROOM_MB,
                "Under a " + CEILING_MB + "MB budget the collector should defend about "
                        + RESERVE_MB + "MB of headroom, but the smallest seen was "
                        + boundedHeadroomMb + "MB -- the process is riding the kill line.");

        // ---- 4. proof that scenario 3 can fail ---------------------------------
        Path noReserve = build(distDir, tempDirs, "noreserve",
                "-DCN1_GC_CONFORM -DCN1_PACING_NO_RESERVE");
        Run unbounded = run(noReserve, distDir, ceiling);
        long unboundedHeadroomMb = minHeadroomMb(unbounded.output);
        assertTrue(unboundedHeadroomMb >= 0,
                "No [PACING] report from the no-reserve build. Output: " + tail(unbounded.output));
        assertTrue(unboundedHeadroomMb < MIN_HEADROOM_MB,
                "Compiling the reserve out did NOT put the process back on the admission "
                        + "margin (smallest headroom " + unboundedHeadroomMb + "MB), so this "
                        + "scenario is inert.");
    }

    /** Smallest headroom the pacing tracer saw, in MB, or -1 if it never reported. */
    private long minHeadroomMb(String output) {
        for (String line : output.split("\\R")) {
            int at = line.indexOf("minHeadroomKb=");
            if (at < 0) {
                continue;
            }
            String rest = line.substring(at + "minHeadroomKb=".length());
            int end = 0;
            if (end < rest.length() && rest.charAt(end) == '-') {
                end++;
            }
            while (end < rest.length() && Character.isDigit(rest.charAt(end))) {
                end++;
            }
            long kb = Long.parseLong(rest.substring(0, end));
            return kb < 0 ? -1 : kb / 1024;
        }
        return -1;
    }

    /** One build of the already-translated project, with its own flags and build dir. */
    private Path build(Path distDir, List<Path> tempDirs, String name, String cFlags) throws Exception {
        Path buildDir = Files.createTempDirectory("gc-steady-build-" + name);
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
        Path exe = buildDir.resolve(CompilerHelper.executableName("GcSteadyStateApp"));
        assertTrue(Files.exists(exe), "ParparVM build should produce a runnable executable at " + exe);
        return exe;
    }

    /** The [GCPROBE] series, reduced to the two things this gate decides on. */
    private static final class Series {
        int cycles;
        long satbRefsTotal;
        long liveObjectsMax;
        long pagesAtStart;
        long pagesAtMid;
        long pagesAtEnd;

        static Series parse(String output) {
            List<Map<String, Long>> rows = new ArrayList<>();
            for (String line : output.split("\\R")) {
                if (!line.startsWith("[GCPROBE] v=1")) {
                    continue;
                }
                Map<String, Long> row = new HashMap<>();
                for (String token : line.split("\\s+")) {
                    int eq = token.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    try {
                        row.put(token.substring(0, eq),
                                (long) Double.parseDouble(token.substring(eq + 1)));
                    } catch (NumberFormatException ignored) {
                        // v=1 and any future non-numeric field
                    }
                }
                rows.add(row);
            }
            Series s = new Series();
            s.cycles = rows.size();
            if (rows.isEmpty()) {
                return s;
            }
            // The first fifth is start-up: the retained population is still being built and
            // the page pool has not reached its working set, so it describes neither regime.
            int from = rows.size() / 5;
            int mid = (from + rows.size()) / 2;
            for (int i = from; i < rows.size(); i++) {
                s.satbRefsTotal += rows.get(i).getOrDefault("satbRefs", 0L);
                s.liveObjectsMax = Math.max(s.liveObjectsMax, rows.get(i).getOrDefault("legUsed", 0L));
            }
            s.pagesAtStart = rows.get(from).getOrDefault("pgTotal", 0L);
            s.pagesAtMid = rows.get(mid).getOrDefault("pgTotal", 0L);
            s.pagesAtEnd = rows.get(rows.size() - 1).getOrDefault("pgTotal", 0L);
            return s;
        }

        /** Logged references per cycle, per live object. Bounded by the live set once the
         * barrier stops logging things the snapshot never contained. */
        double satbRefsPerLiveObject() {
            if (cycles == 0 || liveObjectsMax == 0) {
                return Double.MAX_VALUE;
            }
            return ((double) satbRefsTotal / cycles) / liveObjectsMax;
        }

        /** Second-half page growth as a fraction of first-half page growth's endpoint. A
         * heap that has reached a working set adds almost nothing here; a compounding one
         * adds at least as much as it did in the first half. */
        double secondHalfPageGrowth() {
            if (pagesAtMid == 0) {
                return Double.MAX_VALUE;
            }
            return (double) (pagesAtEnd - pagesAtMid) / pagesAtMid;
        }
    }

    private String describe(String what, Series s) {
        return what + ": cycles=" + s.cycles
                + " satbRefs/cycle/liveObject=" + String.format("%.3f", s.satbRefsPerLiveObject())
                + " (total=" + s.satbRefsTotal + ", live=" + s.liveObjectsMax + ")"
                + " pages " + s.pagesAtStart + " -> " + s.pagesAtMid + " -> " + s.pagesAtEnd
                + " (second-half growth " + String.format("%.3f", s.secondHalfPageGrowth()) + ")";
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
        return run(executable, workingDir, new HashMap<String, String>());
    }

    private Run run(Path executable, Path workingDir, Map<String, String> env) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toString());
        builder.directory(workingDir.toFile());
        // A developer debugging the collector has CN1_* knobs exported, and several of them
        // (CN1_SIMULATE_FREE_MEMORY, CN1_GC_FAULT) would invert this result rather than fail
        // loudly. Start the child from a known state and give it only what this test sets.
        builder.environment().keySet().removeIf(key -> key.startsWith("CN1_"));
        builder.environment().put("CN1_GC_PROBE", "1");
        builder.environment().put("CN1_LOG_PACING_PARKS", "1");
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

    private String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 25);
        return String.join("\n", Arrays.copyOfRange(lines, from, lines.length));
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = GcSteadyStateIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/GcSteadyStateApp.java");
        assertNotNull(in, "GcSteadyStateApp.java test resource should exist");
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
                classesDir + System.getProperty("path.separator") + javaApiDir, "GcSteadyStateApp");
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
