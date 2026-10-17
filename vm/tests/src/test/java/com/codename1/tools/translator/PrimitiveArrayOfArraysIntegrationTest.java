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

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// An array of primitive arrays -- `int[][]`, `float[][]` -- is an array of
/// references, and the native runtime sized its elements as the primitive:
/// `System.arraycopy` moved four bytes for each `int[]` (so half the
/// references arrived and the rest stayed null) and `clone()` made an array
/// half the size. Growing an `int[][]` by the usual copy-into-a-bigger-one
/// then read null out of an index that had just been filled.
class PrimitiveArrayOfArraysIntegrationTest {

    /// One compiler: the bug is in the runtime's C, which no javac changes.
    static java.util.stream.Stream<CompilerHelper.CompilerConfig> oneCompilerConfig() {
        return CompilerHelper.getDiagonalCompilers().stream()
                .filter(CompilerHelper::isJavaApiCompatible)
                .limit(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("oneCompilerConfig")
    void copyingAndCloningKeepEveryInnerArray(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("arrays-sources");
        Path classesDir = Files.createTempDirectory("arrays-classes");
        Path javaApiDir = Files.createTempDirectory("arrays-java-api");
        Files.write(sourceDir.resolve("ArraysApp.java"), source().getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("arrays-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ArraysApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "ArraysApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("ArraysApp"));
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=63"),
                "an array of primitive arrays lost inner arrays when copied or cloned:\n" + output);
    }

    private static String source() {
        return "public class ArraysApp {\n"
                + "    static int sum(int[][] a) {\n"
                + "        int s = 0;\n"
                + "        for (int i = 0; i < a.length; i++) {\n"
                + "            if (a[i] == null) { return -1; }\n"
                + "            for (int j = 0; j < a[i].length; j++) { s += a[i][j]; }\n"
                + "        }\n"
                + "        return s;\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        int score = 0;\n"
                + "        int[][] rows = new int[5][];\n"
                + "        for (int i = 0; i < rows.length; i++) { rows[i] = new int[] {i, i * 10}; }\n"
                // Every reference has to arrive, not the first half of them.
                + "        int[][] grown = new int[10][];\n"
                + "        System.arraycopy(rows, 0, grown, 0, rows.length);\n"
                + "        boolean all = true;\n"
                + "        for (int i = 0; i < rows.length; i++) { all &= grown[i] == rows[i]; }\n"
                + "        if (all) { score |= 1; }\n"
                + "        if (grown[5] == null && grown[9] == null) { score |= 2; }\n"
                // A clone is as long as its source and shares its inner arrays.
                + "        int[][] copy = (int[][]) rows.clone();\n"
                + "        if (copy.length == 5 && sum(copy) == 110 && copy[4] == rows[4]) { score |= 4; }\n"
                // The same for another primitive and for three dimensions.
                + "        float[][] f = new float[3][];\n"
                + "        for (int i = 0; i < f.length; i++) { f[i] = new float[] {i + 0.5f}; }\n"
                + "        float[][] g = new float[6][];\n"
                + "        System.arraycopy(f, 0, g, 3, 3);\n"
                + "        if (g[3] == f[0] && g[4] == f[1] && g[5] == f[2] && g[5][0] == 2.5f) { score |= 8; }\n"
                + "        long[][][] deep = new long[4][][];\n"
                + "        for (int i = 0; i < deep.length; i++) { deep[i] = new long[][] {{i}, {i + 1L}}; }\n"
                + "        long[][][] deeper = new long[8][][];\n"
                + "        System.arraycopy(deep, 0, deeper, 0, 4);\n"
                + "        if (deeper[3] == deep[3] && deeper[3][1][0] == 4L) { score |= 16; }\n"
                // An overlapping move within one array, which a list of rows does.
                + "        System.arraycopy(grown, 0, grown, 1, 5);\n"
                + "        if (grown[1] == rows[0] && grown[5] == rows[4] && grown[0] == rows[0]) { score |= 32; }\n"
                + "        System.out.println(\"RESULT=\" + score);\n"
                + "    }\n"
                + "}\n";
    }
}
