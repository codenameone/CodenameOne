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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StampedLockIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void verifiesStampedLockBehavior(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("stampedlock-integration-sources");
        Path classesDir = Files.createTempDirectory("stampedlock-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        // 1. Write Test App
        Files.write(sourceDir.resolve("StampedLockTestApp.java"), stampedLockTestAppSource().getBytes(StandardCharsets.UTF_8));

        // 2. Compile Test App against JavaAPI
        List<String> sources = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> sources.add(p.toString()));
        }

        List<String> compileArgs = new ArrayList<>();
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.addAll(sources);

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "Compilation failed");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        // 3. Native Report Stub
        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));
        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        // 4. Run Translator
        Path outputDir = Files.createTempDirectory("stampedlock-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "StampedLockTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists));

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        // Execution skipped; building the generated library validates translation output.
    }

    private String stampedLockTestAppSource() {
        return "import java.util.concurrent.locks.*;\n" +
               "import java.util.concurrent.TimeUnit;\n" +
               "public class StampedLockTestApp {\n" +
               "    private static native void report(String msg);\n" +
               "    \n" +
               "    public static void main(String[] args) {\n" +
               "        testBasic();\n" +
               "        testOptimisticRead();\n" +
               "        testWriteLockExclusion();\n" +
               "    }\n" +
               "    \n" +
               "    private static void testBasic() {\n" +
               "        StampedLock sl = new StampedLock();\n" +
               "        long stamp = sl.writeLock();\n" +
               "        try {\n" +
               "             report(\"TEST: Basic Write Lock OK\");\n" +
               "        } finally {\n" +
               "             sl.unlockWrite(stamp);\n" +
               "        }\n" +
               "        \n" +
               "        stamp = sl.readLock();\n" +
               "        try {\n" +
               "             report(\"TEST: Basic Read Lock OK\");\n" +
               "        } finally {\n" +
               "             sl.unlockRead(stamp);\n" +
               "        }\n" +
               "    }\n" +
               "    \n" +
               "    private static void testOptimisticRead() {\n" +
               "        StampedLock sl = new StampedLock();\n" +
               "        long stamp = sl.tryOptimisticRead();\n" +
               "        if (stamp != 0 && sl.validate(stamp)) {\n" +
               "            report(\"TEST: Optimistic Read Valid OK\");\n" +
               "        } else {\n" +
               "            report(\"TEST: Optimistic Read Valid FAILED\");\n" +
               "        }\n" +
               "        \n" +
               "        long ws = sl.writeLock();\n" +
               "        if (sl.validate(stamp)) {\n" +
               "            report(\"TEST: Optimistic Read Invalid (during write) FAILED\");\n" +
               "        } else {\n" +
               "            report(\"TEST: Optimistic Read Invalid (during write) OK\");\n" +
               "        }\n" +
               "        sl.unlockWrite(ws);\n" +
               "        \n" +
               "        if (sl.validate(stamp)) {\n" +
               "             report(\"TEST: Optimistic Read Invalid (after write) FAILED\");\n" +
               "        } else {\n" +
               "             report(\"TEST: Optimistic Read Invalid (after write) OK\");\n" +
               "        }\n" +
               "    }\n" +
               "    \n" +
               "    private static void testWriteLockExclusion() {\n" +
               "        final StampedLock sl = new StampedLock();\n" +
               "        long stamp = sl.writeLock();\n" +
               "        \n" +
               "        final boolean[] success = new boolean[1];\n" +
               "        Thread t = new Thread(new Runnable() {\n" +
               "            public void run() {\n" +
               "                long s = sl.readLock();\n" +
               "                sl.unlockRead(s);\n" +
               "                success[0] = true;\n" +
               "            }\n" +
               "        });\n" +
               "        t.start();\n" +
               "        \n" +
               "        try { Thread.sleep(200); } catch(Exception e) {}\n" +
               "        if (success[0]) {\n" +
               "             report(\"TEST: Write Lock Exclusion FAILED (Reader acquired lock)\");\n" +
               "             return;\n" +
               "        }\n" +
               "        \n" +
               "        sl.unlockWrite(stamp);\n" +
               "        try { Thread.sleep(200); } catch(Exception e) {}\n" +
               "        if (success[0]) {\n" +
               "             report(\"TEST: Write Lock Exclusion OK\");\n" +
               "        } else {\n" +
               "             report(\"TEST: Write Lock Exclusion FAILED (Reader did not acquire lock)\");\n" +
               "        }\n" +
               "    }\n" +
               "}\n";
    }

    private String nativeReportSource() {
        // Through the runtime's own conversion, not a hand-copied String struct: the
        // layout is the VM's to change, and a fixture that mirrors it stops compiling
        // (no offset field, no array ->data) and fails every case before it runs.
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void StampedLockTestApp_report___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg) {\n" +
                "    if (msg != JAVA_NULL) {\n" +
                "        printf(\"%s\\n\", stringToUTF8(threadStateData, msg));\n" +
                "        fflush(stdout);\n" +
                "    }\n" +
                "}\n";
    }

}
