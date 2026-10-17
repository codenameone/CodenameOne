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

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// `Math.floor` and `Math.ceil` are natives, and the C behind them converted
/// the argument to a 64 bit integer and back. A double from 2^63 up, an
/// infinity and a NaN have no such integer -- the conversion is undefined in C
/// -- and an integer has no negative zero, so `floor(1e20)` was not `1e20`,
/// `floor(NaN)` was a number and `ceil(-0.5)` was `+0.0`. `Math.round` is Java
/// over `floor`, so it saturated at the wrong end for the same arguments.
class MathFloorCeilIntegrationTest {

    /// One compiler: the bug is in the runtime's C, which no javac changes.
    static java.util.stream.Stream<CompilerHelper.CompilerConfig> oneCompilerConfig() {
        return CompilerHelper.getDiagonalCompilers().stream()
                .filter(CompilerHelper::isJavaApiCompatible)
                .limit(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("oneCompilerConfig")
    void floorAndCeilAnswerWhatTheJdkSpecifies(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("floor-sources");
        Path classesDir = Files.createTempDirectory("floor-classes");
        Path javaApiDir = Files.createTempDirectory("floor-java-api");
        Files.write(sourceDir.resolve("FloorApp.java"), source().getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("floor-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FloorApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "FloorApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("FloorApp"));
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("FAILED=0;"),
                "Math.floor/ceil/round differ from the JDK (one bit per failed check, lowest first):\n" + output);
    }

    /// Results are compared by their bits, which is the only comparison that
    /// tells `-0.0` from `0.0` and sees a NaN at all. The arguments come out
    /// of an array so that no compiler folds a call away.
    private static String source() {
        return "public class FloorApp {\n"
                + "    static long failed;\n"
                + "    static int check;\n"
                + "    static void same(double actual, double expected) {\n"
                + "        boolean ok = expected != expected ? actual != actual\n"
                + "                : Double.doubleToLongBits(actual) == Double.doubleToLongBits(expected);\n"
                + "        if (!ok) { failed |= 1L << check; }\n"
                + "        check++;\n"
                + "    }\n"
                + "    static void same(long actual, long expected) {\n"
                + "        if (actual != expected) { failed |= 1L << check; }\n"
                + "        check++;\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        double inf = 1.0 / zero(), nan = zero() / zero(), negZero = -1.0 / inf;\n"
                + "        double[] in = {1e20, -1e20, 9223372036854775808.0, -9223372036854777856.0, nan, inf, -inf,\n"
                + "                negZero, 0.0, -0.5, 0.5, 2.5, -2.5, 3.0, -3.0, 4503599627370497.0, 1e300};\n"
                + "        double[] floors = {1e20, -1e20, 9223372036854775808.0, -9223372036854777856.0, nan, inf, -inf,\n"
                + "                negZero, 0.0, -1.0, 0.0, 2.0, -3.0, 3.0, -3.0, 4503599627370497.0, 1e300};\n"
                + "        double[] ceils = {1e20, -1e20, 9223372036854775808.0, -9223372036854777856.0, nan, inf, -inf,\n"
                + "                negZero, 0.0, negZero, 1.0, 3.0, -2.0, 3.0, -3.0, 4503599627370497.0, 1e300};\n"
                + "        for (int i = 0; i < in.length; i++) {\n"
                + "            same(Math.floor(in[i]), floors[i]);\n"
                + "            same(Math.ceil(in[i]), ceils[i]);\n"
                + "        }\n"
                // round is Java over floor: it saturates, and NaN is zero.
                + "        same(Math.round(in[0]), Long.MAX_VALUE);\n"
                + "        same(Math.round(in[1]), Long.MIN_VALUE);\n"
                + "        same(Math.round(inf), Long.MAX_VALUE);\n"
                + "        same(Math.round(-inf), Long.MIN_VALUE);\n"
                + "        same(Math.round(nan), 0L);\n"
                + "        same(Math.round(in[11]), 3L);\n"
                + "        same(Math.round(in[12]), -2L);\n"
                + "        same(Math.round(in[9]), 0L);\n"
                + "        float big = (float) in[0];\n"
                + "        same((long) Math.round(big), (long) Integer.MAX_VALUE);\n"
                + "        same((long) Math.round(-big), (long) Integer.MIN_VALUE);\n"
                + "        same((long) Math.round((float) nan), 0L);\n"
                + "        System.out.println(\"CHECKS=\" + check + \";FAILED=\" + failed + \";\");\n"
                + "    }\n"
                + "    static double zero() { return System.currentTimeMillis() < 0 ? 1.0 : 0.0; }\n"
                + "}\n";
    }
}
