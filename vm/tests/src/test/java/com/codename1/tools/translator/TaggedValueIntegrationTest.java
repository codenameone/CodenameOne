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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The CI gate for tagged immediates.
 *
 * valueOf on Integer, Long, Double, Float, Character and Short returns an immediate with a
 * type code in its low three bits instead of a heap object, on every 64-bit-pointer target,
 * by default. Nothing about that is visible to a compiler or a linker: a tagged value that
 * resolves to the wrong class, or whose hashCode is computed as if it were an Integer,
 * produces a wrong answer with nothing thrown.
 *
 * BoxEdge is the torture that covers it, and it lived only in vm/benchmarks/run-gauntlet.sh
 * -- which NO workflow runs. This drives the same source through the translator and a real
 * compiler, so the feature is gated where it ships rather than only where a developer
 * remembers to look.
 *
 * The fixture is the benchmark source itself rather than a copy, so the two cannot drift.
 */
class TaggedValueIntegrationTest {

    /** Where the shared torture lives; read, not duplicated. */
    private static final Path BOX_EDGE_SOURCE =
            Paths.get("..", "benchmarks", "src", "com", "bench", "BoxEdge.java");

    /**
     * Every boxed type must behave EXACTLY like a host JVM, whether the build tags values or
     * boxes them on the heap -- the two representations are required to be indistinguishable,
     * which is precisely why this test needs a witness (below) to prove which one it ran.
     */
    @Test
    void everyBoxedTypeMatchesTheHostJvmWithAndWithoutTagging() throws Exception {
        List<CompilerHelper.CompilerConfig> configs = new ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);
        Assumptions.assumeTrue(Files.exists(BOX_EDGE_SOURCE),
                "BoxEdge source not present at " + BOX_EDGE_SOURCE.toAbsolutePath());

        // The reference JVM is chosen SEPARATELY from the toolchain that drives the
        // translator, and it has to be JDK 19 or newer. JDK 19 replaced Double.toString and
        // Float.toString with the shortest-round-tripping representation (JDK-4511638), and
        // ParparVM implements the new algorithm -- so an older reference reports a handful of
        // divergences that are the reference being out of date, not the VM being wrong:
        //   JDK 17: 4.6116860184273879E18    ParparVM and JDK 19+: 4.611686018427388E18
        // Both round-trip; only the second is the shortest such string.
        CompilerHelper.CompilerConfig reference = null;
        for (String v : new String[] { "25", "21", "19" }) {
            for (CompilerHelper.CompilerConfig candidate : CompilerHelper.getAvailableCompilers(v)) {
                if (CompilerHelper.getJdkMajor(candidate) >= 19) {
                    reference = candidate;
                    break;
                }
            }
            if (reference != null) {
                break;
            }
        }
        Assumptions.assumeTrue(reference != null,
                "Need JDK 19+ as the reference JVM: Double.toString changed to the shortest "
                        + "round-tripping form in 19, and ParparVM implements that form");

        Parser.cleanup();

        // The torture declares "package com.bench" for the benchmark harness; the translator
        // helper here fixes the application package, so it is compiled in the default package.
        // Dropping the declaration is the whole adaptation -- nothing else is rewritten, so a
        // change to the torture reaches this gate automatically.
        String source = new String(Files.readAllBytes(BOX_EDGE_SOURCE), StandardCharsets.UTF_8)
                .replace("package com.bench;", "");

        Path sourceDir = Files.createTempDirectory("boxedge-sources");
        Path classesDir = Files.createTempDirectory("boxedge-classes");
        Path javaApiDir = Files.createTempDirectory("boxedge-japi");
        Files.write(sourceDir.resolve("BoxEdge.java"), source.getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("boxedge-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "BoxEdgeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(
                cmakeLists, distDir.resolve("BoxEdgeApp-src").getFileName().toString());

        // Tagging is a COMPILE-time decision, so one translation feeds both arms.
        String tagged = buildAndRun(distDir, "tagged", "");
        String untagged = buildAndRun(distDir, "untagged", "-DCN1_DISABLE_TAGGED_INT");
        String host = runOnHostJvm(reference, sourceDir);

        assertTrue(host.split("\n", -1).length > 500,
                "The host reference produced too little output to be a real comparison");

        assertEquals(host, filtered(tagged),
                "The tagged build must be byte-identical to a host JVM");
        assertEquals(host, filtered(untagged),
                "The heap-boxing fallback must be byte-identical to a host JVM too -- it is what "
                        + "32-bit-pointer targets such as arm64_32 actually ship");

        // The witness. Without it every assertion above is satisfiable by a build in which
        // tagging never happened, which is exactly the failure this gate exists to exclude.
        assertEquals("111111", witness(tagged),
                "A default build must return an immediate from valueOf for all six boxed types "
                        + "(Integer/Long/Double/Float/Character/Short, in that order). If this reads "
                        + "100000 the five types added after Integer are not on; if it reads 000000 "
                        + "nothing is tagged and the comparison above proved nothing about the tagged "
                        + "path, because the two representations are required to be indistinguishable.");
        assertEquals("000000", witness(untagged),
                "-DCN1_DISABLE_TAGGED_INT must disable every tagged type, or the arms are not "
                        + "actually being compared");
    }

    /** Everything the fixture prints except the target-specific diagnostics. */
    private static String filtered(String output) {
        StringBuilder sb = new StringBuilder();
        for (String line : output.split("\n", -1)) {
            if (!line.startsWith("[")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /** Whether each boxed type became an immediate, as reported by the run itself. */
    private static String witness(String output) {
        for (String line : output.split("\n", -1)) {
            if (line.startsWith("[TAGGED] ")) {
                return line.substring("[TAGGED] ".length()).trim();
            }
        }
        return "<no [TAGGED] line: the fixture did not run to completion>";
    }

    private static String buildAndRun(Path distDir, String label, String cFlags) throws Exception {
        Path buildDir = distDir.resolve("build-" + label);
        Files.createDirectories(buildDir);
        List<String> configure = new ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        if (!cFlags.isEmpty()) {
            configure.add("-DCMAKE_C_FLAGS=" + cFlags);
        }
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(
                Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("BoxEdgeApp"));
        String output = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("BOXEDGE DONE"),
                "The " + label + " build did not run the torture to completion:\n" + output);
        return output;
    }

    /**
     * The same source compiled and run against a real JDK, which is the reference.
     * Must be JDK 19+ -- see the note at the call site about Double.toString.
     */
    private static String runOnHostJvm(CompilerHelper.CompilerConfig config, Path sourceDir)
            throws Exception {
        Path hostClasses = Files.createTempDirectory("boxedge-host");
        List<String> compileArgs = new ArrayList<>(Arrays.asList(
                "-nowarn",
                "-d", hostClasses.toString(),
                sourceDir.resolve("BoxEdge.java").toString()));
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "BoxEdge must compile against the host JDK");
        String output = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(config.jdkHome.resolve("bin")
                                .resolve(CompilerHelper.executableName("java")).toString(),
                        "-cp", hostClasses.toString(), "BoxEdge"),
                hostClasses);
        assertTrue(output.contains("BOXEDGE DONE"),
                "The host JVM did not run the torture to completion:\n" + output);
        return filtered(output);
    }
}
