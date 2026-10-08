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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Class and method names with a '-'. Kotlin 1.4 to 1.7 name lambdas and the
/// classes inlined into them `onCreate$lambda-1` and
/// `Main$onCreate$lambda-1$$inlined$sortedBy$1`. The translator mangled '$'
/// into the C name but left the '-', so the generated C read
/// `..._lambda-1__inlined_sortedBy_1` as a subtraction and did not compile --
/// on every native target, for any Kotlin application with such a lambda.
///
/// javac cannot write those names, so the test compiles ordinary ones and
/// rewrites them in the class files: each replacement has the length of the
/// original, so no constant pool entry changes size.
class HyphenatedNamesIntegrationTest {

    private static final String SOURCE = "public class NameApp {\n"
            + "    static class Holderx1 {\n"
            + "        int value() { return 40; }\n"
            + "    }\n"
            + "    private static int calcx3(int x) { return x + 2; }\n"
            + "    public static void main(String[] args) {\n"
            + "        report(new Holderx1().value() + calcx3(0));\n"
            + "    }\n"
            + "    private static native void report(int value);\n"
            + "}\n";

    @Test
    void translatesClassAndMethodNamesWithAHyphen() throws Exception {
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("hyphen-sources");
        Path classesDir = Files.createTempDirectory("hyphen-classes");
        Path javaApiDir = Files.createTempDirectory("hyphen-java-api");
        Files.write(sourceDir.resolve("NameApp.java"), SOURCE.getBytes(StandardCharsets.UTF_8));

        List<CompilerHelper.CompilerConfig> configs = CompilerHelper.getAvailableCompilers("1.8");
        if (configs.isEmpty()) {
            fail("No compiler available for target 1.8");
        }
        CompilerHelper.CompilerConfig config = configs.get(0);
        for (CompilerHelper.CompilerConfig c : configs) {
            if (CompilerHelper.getJdkMajor(c) < 9) {
                config = c;
                break;
            }
        }
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> args = new ArrayList<>(Arrays.asList("-source", "1.8", "-target", "1.8"));
        args.add(CompilerHelper.useClasspath(config) ? "-classpath" : "-bootclasspath");
        args.add(javaApiDir.toString());
        args.add("-d");
        args.add(classesDir.toString());
        args.add(sourceDir.resolve("NameApp.java").toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, args), "NameApp should compile");

        rename(classesDir, "Holderx1", "Holder-1");
        rename(classesDir, "calcx3", "calc-3");
        assertTrue(Files.exists(classesDir.resolve("NameApp$Holder-1.class")), "the inner class was renamed");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.write(classesDir.resolve("native_report.c"), ("#include \"cn1_globals.h\"\n"
                + "#include <stdio.h>\n"
                + "void NameApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n"
                + "    printf(\"RESULT=%d\\n\", value);\n"
                + "}\n").getBytes(StandardCharsets.UTF_8));

        Path outputDir = Files.createTempDirectory("hyphen-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "NameApp");
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "NameApp-src");
        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        String output = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(buildDir.resolve("NameApp").toString()), buildDir);
        assertTrue(output.contains("RESULT=42"), "the program should run: " + output);
    }

    /// Replaces `from` with `to` (same length) in every class file and in the
    /// class file names.
    private static void rename(Path dir, String from, String to) throws Exception {
        assertEquals(from.length(), to.length());
        byte[] f = from.getBytes(StandardCharsets.UTF_8);
        byte[] t = to.getBytes(StandardCharsets.UTF_8);
        List<Path> classes = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.toString().endsWith(".class")).forEach(classes::add);
        }
        int replaced = 0;
        for (Path p : classes) {
            byte[] b = Files.readAllBytes(p);
            for (int i = 0; i + f.length <= b.length; i++) {
                if (Arrays.equals(Arrays.copyOfRange(b, i, i + f.length), f)) {
                    System.arraycopy(t, 0, b, i, t.length);
                    replaced++;
                }
            }
            Files.write(p, b);
            String name = p.getFileName().toString();
            if (name.contains(from)) {
                Files.move(p, p.resolveSibling(name.replace(from, to)));
            }
        }
        assertTrue(replaced > 0, "nothing named " + from + " was found to rename");
    }
}
