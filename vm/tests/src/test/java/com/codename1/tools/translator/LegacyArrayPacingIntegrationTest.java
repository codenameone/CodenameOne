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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Synthetic reproduction harness for issue 5537 (iOS app killed with
 * EXC_RESOURCE / RESOURCE_TYPE_MEMORY at about 1.4GB resident near the end of a
 * deep, garbage-heavy game-tree search).
 *
 * <p>{@code LegacyArrayPacingApp} holds its LIVE set fixed at 4MB and sweeps the
 * mutator's allocation RATE across both allocator paths -- BiBOP page heap
 * (blocks at or under CN1_BIBOP_MAX_OBJECT) and the legacy calloc path (above
 * it) -- with identical loops, identical live bytes and identical wall duration
 * per arm. The app reports its own phys_footprint at each phase boundary and
 * tracks the peak inside each arm, which turns "the app uses too much memory"
 * into a falsifiable question: does memory track the LIVE SET, or does it track
 * how fast the program allocates?</p>
 *
 * <p>Measured on an M-series host running this test under Maven, 4GB churned per
 * unbounded arm against a 4MB live set:</p>
 *
 * <pre>
 *   arm            baseKB     peakKB  settledKB  releasedKB   growthKB
 *   bibop@full       5572     112848     114320      114324     107276
 *   legacy@full    114340    2471968     787496      263720    2357628
 *   bibop@2048     247496     280256     280256      280248      32760
 *   legacy@2048    280248    1769232     301564      301564    1488984
 *   bibop@512      285192     285220     285212      285212         28
 *   legacy@512     285216     384928     300708      300708      99712
 *   bibop@128      292552     292552     292552      292560          0
 *   legacy@128     292556     318492     308144      308144      25936
 * </pre>
 *
 * <p>Three things fall out of that table. First, resident memory is a function
 * of allocation RATE, not of the live set: the live set is 4MB in every row, and
 * growth ranges from 0 to 2.3GB. Second, the two paths behave completely
 * differently -- the BiBOP path is flat at 128 and 512MB/s because
 * cn1BibopPacingCap actually parks the mutator, while the legacy path grows at
 * EVERY rate tested, because CN1_LEGACY_GC_TRIGGER_BYTES only schedules an
 * asynchronous System.gc() and the sole legacy backpressure is a COUNT of
 * outstanding slots (CN1_MAX_HEAP_SIZE), which a workload of large arrays never
 * approaches. That is the issue-5537 mechanism, and it matches the reporter's
 * faulting frame sitting inside memmove's 16KB-and-above copy loop.</p>
 *
 * <p>Third, the paths differ in what they give BACK. The legacy path's peaks do
 * subside once the ring is dropped and collections are forced (2.4GB peak down
 * to 264MB released) because free() returns large blocks to the OS. The BiBOP
 * path's do not: its settled and released columns never fall below its peak,
 * because reclaimed pages go to a reuse pool that has no munmap or madvise path
 * at all, so a burst permanently raises the process floor for its lifetime.</p>
 *
 * <p>The knee is machine-dependent -- it is set by how fast the collector
 * completes a cycle relative to the mutator -- which is exactly why this kills
 * an iPad and not the Xcode simulator: the device's collector is slower, its
 * live set larger, and its jetsam ceiling roughly 1.4GB instead of a desktop's
 * many gigabytes. The same effect is visible here as runner load: legacy@512
 * grew 0KB on an idle host and 99712KB when this test ran alongside a Maven
 * build; bibop@128 grew 0KB alone and 261512KB under a full parallel suite. So
 * every rate-limited row is REPORTED rather than gated -- their variability is
 * the finding, and asserting on it would just make the test flaky. The only gate
 * is that the UNBOUNDED arms still blow past the live set, which is what makes
 * this a reproduction at all.</p>
 *
 * <p>Tagged {@code benchmark}: it takes about a minute of wall time on top of a
 * translate-and-build, and the unbounded arms deliberately drive resident size
 * into the gigabytes (bounded by the app's FULL_RATE_BYTE_CAP).</p>
 */
@Tag("benchmark")
class LegacyArrayPacingIntegrationTest {

    /** Keep in sync with LIVE_BYTES in LegacyArrayPacingApp. */
    private static final long LIVE_KB = 4 * 1024;

    /**
     * Growth above which a rate-limited arm is called out in the log as showing
     * the issue-5537 shape. Not a gate -- see the report-only block in the test
     * body.
     */
    private static final long GUARDED_GROWTH_BUDGET_KB = 64 * 1024;

    /**
     * An unbounded arm is expected to blow well past the live set -- that IS the
     * issue-5537 reproduction. If it stops doing so, either a fix landed (in
     * which case turn this into a bound and gate every rate) or the app is no
     * longer allocating hard enough to be a reproduction at all.
     */
    private static final long REPRODUCTION_MIN_GROWTH_KB = 8 * LIVE_KB;

    @Test
    void residentMemoryTracksAllocationRateRatherThanLiveSet() throws Exception {
        Parser.cleanup();

        List<Path> tempDirs = new ArrayList<>();
        try {
            runPacingSweep(tempDirs);
        } finally {
            for (Path dir : tempDirs) {
                deleteRecursively(dir);
            }
        }
    }

    private void runPacingSweep(List<Path> tempDirs) throws Exception {
        Path sourceDir = Files.createTempDirectory("legacy-array-pacing-sources");
        Path classesDir = Files.createTempDirectory("legacy-array-pacing-classes");
        Path javaApiDir = Files.createTempDirectory("legacy-array-pacing-javaapi");
        tempDirs.add(sourceDir);
        tempDirs.add(classesDir);
        tempDirs.add(javaApiDir);

        Path source = sourceDir.resolve("LegacyArrayPacingApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the legacy-array pacing harness");
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
                "LegacyArrayPacingApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        String javaResult = extractLine(javaOutput, "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="),
                "JavaSE should produce RESULT=. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("legacy-array-pacing-output");
        tempDirs.add(outputDir);
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LegacyArrayPacingApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "LegacyArrayPacingApp-src");

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

        Path executable = buildDir.resolve("LegacyArrayPacingApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");

        String vmOutput = runVm(executable, buildDir);

        assertTrue(vmOutput.contains("LEGACY_ARRAY_PACING_DONE"),
                "The pacing sweep must run to completion. Output: " + vmOutput);
        assertEquals(javaResult, extractLine(vmOutput, "RESULT="),
                "The rate-limited arms must compute the same answer on both runtimes, so a "
                        + "well-behaved footprint cannot come from quietly allocating less\n"
                        + "--- JavaSE ---\n" + javaOutput
                        + "\n--- ParparVM ---\n" + vmOutput);

        Map<String, Map<String, Long>> marks = parseMarks(vmOutput);
        Map<String, long[]> table = new LinkedHashMap<>();
        StringBuilder report = new StringBuilder();
        report.append(String.format("%-14s %10s %10s %11s %11s %10s%n",
                "arm", "baseKB", "peakKB", "settledKB", "releasedKB", "growthKB"));

        for (Map.Entry<String, Map<String, Long>> entry : marks.entrySet()) {
            Map<String, Long> m = entry.getValue();
            if (!m.containsKey("BEGIN") || !m.containsKey("PEAK")) {
                continue;
            }
            long base = m.containsKey("BASELINE") ? m.get("BASELINE") : -1;
            long peak = m.containsKey("PEAK") ? m.get("PEAK") : -1;
            long settled = m.containsKey("SETTLED") ? m.get("SETTLED") : -1;
            long released = m.containsKey("RELEASED") ? m.get("RELEASED") : -1;
            long growth = peak - base;
            table.put(entry.getKey(), new long[]{base, peak, settled, released, growth});
            report.append(String.format("%-14s %10d %10d %11d %11d %10d%n",
                    entry.getKey(), base, peak, settled, released, growth));
        }

        assertTrue(table.size() >= 8,
                "Expected both paths at every swept rate, got " + table.keySet()
                        + "\n--- ParparVM ---\n" + vmOutput);

        System.err.println("[LegacyArrayPacingIntegrationTest] live set " + LIVE_KB
                + "KB, phys_footprint\n" + report);

        // A platform whose Runtime memory natives are still stubs reports 0 for
        // every phase. There is nothing to measure then, and failing would be
        // reporting a porting gap as a memory regression.
        long anyFootprint = 0;
        for (long[] row : table.values()) {
            anyFootprint = Math.max(anyFootprint, row[1]);
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(anyFootprint > 0,
                "This target cannot report phys_footprint through Runtime, so the sweep cannot "
                        + "be measured here.\n" + report);

        // REPORT ONLY, for BOTH paths. It is tempting to gate the BiBOP rows,
        // which are flat (0KB and 28KB) whenever this test runs alone. They are
        // not flat when the full suite runs it alongside a dozen other forks:
        // measured 261512KB of growth for bibop@128 under `mvn test`, because
        // the knee is set by how fast the collector completes a cycle relative
        // to the mutator, and contention moves it. That load-dependence IS the
        // finding of this harness, so gating on it would be asserting the one
        // thing it exists to show is variable. The legacy rows are worse again:
        // they grow at every rate because CN1_LEGACY_GC_TRIGGER_BYTES only
        // schedules an asynchronous System.gc() and the sole legacy backpressure
        // is a COUNT of outstanding slots (CN1_MAX_HEAP_SIZE), which a workload
        // of large arrays never approaches.
        for (Map.Entry<String, long[]> entry : table.entrySet()) {
            int rate = rateOf(entry.getKey());
            long growth = entry.getValue()[4];
            if (rate != 0 && growth > GUARDED_GROWTH_BUDGET_KB) {
                System.err.println("[LegacyArrayPacingIntegrationTest] ISSUE-5537 SHAPE: arm "
                        + entry.getKey() + " grew resident memory by " + growth + "KB against a "
                        + LIVE_KB + "KB live set at a merely " + rate + "MB/s allocation rate. On a "
                        + "device the collector is slower and the jetsam ceiling is about 1.4GB, so "
                        + "the rate at which this happens is well inside what a real search "
                        + "sustains.");
            }
        }

        // REPRODUCTION: the unbounded arms are the issue-5537 shape.
        long worstUnbounded = 0;
        for (Map.Entry<String, long[]> entry : table.entrySet()) {
            if (rateOf(entry.getKey()) == 0) {
                worstUnbounded = Math.max(worstUnbounded, entry.getValue()[4]);
            }
        }
        assertTrue(worstUnbounded >= REPRODUCTION_MIN_GROWTH_KB,
                "No unbounded arm exceeded " + REPRODUCTION_MIN_GROWTH_KB + "KB of resident growth "
                        + "(worst was " + worstUnbounded + "KB), so this run is not reproducing "
                        + "issue 5537. Either the mutator can no longer outrun the collector -- in "
                        + "which case a fix landed and this assertion should become a bound applied "
                        + "to EVERY rate -- or the app is no longer allocating hard enough to be a "
                        + "reproduction.\n" + report);
    }

    /** Target rate encoded in an arm name, or 0 for the unbounded arms. */
    private int rateOf(String armName) {
        String suffix = armName.substring(armName.indexOf('@') + 1);
        return "full".equals(suffix) ? 0 : Integer.parseInt(suffix);
    }

    private Map<String, Map<String, Long>> parseMarks(String output) {
        Pattern p = Pattern.compile(
                "ARM_(BASELINE|BEGIN|PEAK|SETTLED|RELEASED) name=(\\S+) tMs=\\d+ footprintKb=(\\d+)");
        Map<String, Map<String, Long>> marks = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                Map<String, Long> phases = marks.get(m.group(2));
                if (phases == null) {
                    phases = new LinkedHashMap<>();
                    marks.put(m.group(2), phases);
                }
                phases.put(m.group(1), Long.parseLong(m.group(3)));
            }
        }
        return marks;
    }

    /** Runs the translated binary. Memory is reported by the app, not sampled here. */
    private String runVm(Path executable, Path workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(executable.toAbsolutePath().toString());
        builder.directory(workingDir.toFile());
        builder.environment().put("CN1_GC_LOG_CYCLES", "1");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        assertEquals(0, process.waitFor(),
                "ParparVM run should exit cleanly. Output: " + output);
        return output;
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = LegacyArrayPacingIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/LegacyArrayPacingApp.java");
        assertNotNull(in, "LegacyArrayPacingApp.java test resource should exist");
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
                "LegacyArrayPacingApp");
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
            System.err.println("LegacyArrayPacingIntegrationTest: temp cleanup incomplete under "
                    + root + " (first failure: " + firstFailure[0] + ")");
        }
    }
}
