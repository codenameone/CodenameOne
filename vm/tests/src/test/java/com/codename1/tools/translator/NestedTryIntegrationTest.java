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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A throw from inside a catch handler must reach a try that lexically encloses it.
 *
 * It did not. Nested try/catch compiles to two exception-table entries that begin at
 * the SAME instruction, so both register a begin at the same label. TryCatch emitted
 * them in table order and each one asked LabelInstruction for the catch depth of its
 * own end label as it went -- so the inner region, emitted first, computed that depth
 * while only its own begin was registered, and cached 1 where the answer was 2. The
 * depth is what END_TRY restores tryBlockOffset to, so entering the INNER handler
 * deregistered the OUTER try along with it, and the next throw walked a stack that no
 * longer had the enclosing handler on it.
 *
 * <pre>
 *   try { try { throw a; } catch (E e) { throw e; } } catch (E e) { }
 * </pre>
 *
 * left the method with the exception uncaught -- on every platform, in four lines of
 * Java. The fix is to defer the computation to label emission, by which time every
 * try/catch in the method has registered.
 *
 * The third assertion is the one that keeps this honest: it re-translates with
 * {@code cn1.legacyCatchDepth}, which restores the eager computation, and REQUIRES the
 * escape to come back. A gate for a codegen bug that cannot be shown to fail is a gate
 * that silently stops covering the thing it was written for.
 */
class NestedTryIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void throwFromHandlerReachesTheEnclosingTry(CompilerHelper.CompilerConfig config) throws Exception {
        String fixed = buildAndRun(config, "nested-fixed", false);
        assertTrue(fixed.contains("rethrowSame 6"),
                "a rethrow from inside a catch must reach the enclosing try:\n" + fixed);
        assertTrue(fixed.contains("rethrowNew 7"),
                "a NEW exception thrown from inside a catch must reach it too:\n" + fixed);
        assertTrue(fixed.contains("deepNest 15"),
                "three levels must unwind one at a time:\n" + fixed);
        assertTrue(fixed.contains("NESTED_DONE"),
                "the program must run to completion:\n" + fixed);

        // Non-vacuity: with the old eager computation restored, the same source must
        // fail the same way it used to. If this ever starts passing, the assertions
        // above have stopped covering the defect and this test is decoration.
        String legacy = buildAndRun(config, "nested-legacy", true);
        assertTrue(!legacy.contains("NESTED_DONE"),
                "the pre-fix catch depth must still reproduce the escape, or this test\n"
                + "no longer demonstrates what it asserts:\n" + legacy);
    }

    private String buildAndRun(CompilerHelper.CompilerConfig config, String tag, boolean legacyDepth)
            throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory(tag + "-sources");
        Path classesDir = Files.createTempDirectory(tag + "-classes");
        Path javaApiDir = Files.createTempDirectory(tag + "-java-api");
        Files.write(sourceDir.resolve("NestedTryApp.java"),
                nestedSource().getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory(tag + "-output");
        String previous = System.getProperty("cn1.legacyCatchDepth");
        if (legacyDepth) {
            System.setProperty("cn1.legacyCatchDepth", "true");
        }
        try {
            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "NestedTryApp");
        } finally {
            if (previous == null) {
                System.clearProperty("cn1.legacyCatchDepth");
            } else {
                System.setProperty("cn1.legacyCatchDepth", previous);
            }
        }

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "NestedTryApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        // Deliberately NOT runCommand: the legacy arm ends on an uncaught exception,
        // and a non-zero exit is the observation, not a failure of the harness.
        Path executable = buildDir.resolve(CompilerHelper.executableName("NestedTryApp"));
        ProcessBuilder run = new ProcessBuilder(executable.toString());
        run.redirectErrorStream(true);
        final Process p = run.start();
        // Drained off-thread: reading inline blocks until the child closes stdout, so a
        // program that hangs would never reach the timeout below.
        final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        Thread drain = new Thread(new Runnable() {
            public void run() {
                byte[] chunk = new byte[4096];
                int read;
                try {
                    while ((read = p.getInputStream().read(chunk)) > 0) {
                        synchronized (buf) { buf.write(chunk, 0, read); }
                    }
                } catch (java.io.IOException ignored) {
                    // expected when the process is destroyed under the reader
                }
            }
        }, tag + "-output");
        drain.setDaemon(true);
        drain.start();

        boolean finished = p.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
        }
        drain.join(TimeUnit.SECONDS.toMillis(30));
        String output;
        synchronized (buf) { output = new String(buf.toByteArray(), StandardCharsets.UTF_8); }
        if (!finished) {
            fail("the program did not finish:\n" + output);
        }
        if (!legacyDepth) {
            assertEquals(0, p.exitValue(), "every throw here is caught:\n" + output);
        }
        return output;
    }

    /**
     * Each method prints a number the JVM also produces, so the expectations are the
     * JLS' answers rather than this VM's. rethrowSame is the minimal case; rethrowNew
     * distinguishes rethrowing the SAME object from throwing a new one (the handler
     * reached the same way either way, but only one of them re-enters athrow on a
     * local); deepNest checks that three levels unwind one at a time rather than all
     * at once, which a depth off by one the other way would also break.
     */
    private static String nestedSource() {
        return "public class NestedTryApp {\n"
                + "    static int rethrowSame() {\n"
                + "        int r = 0;\n"
                + "        try {\n"
                + "            try {\n"
                + "                throw new IllegalStateException(\"a\");\n"
                + "            } catch (IllegalStateException e) {\n"
                + "                r += 2;\n"
                + "                throw e;\n"
                + "            }\n"
                + "        } catch (IllegalStateException e) {\n"
                + "            r += 4;\n"
                + "        }\n"
                + "        return r;\n"
                + "    }\n"
                + "    static int rethrowNew() {\n"
                + "        int r = 0;\n"
                + "        try {\n"
                + "            try {\n"
                + "                r += 1;\n"
                + "                throw new IllegalStateException(\"inner\");\n"
                + "            } catch (IllegalStateException e) {\n"
                + "                r += 2;\n"
                + "                throw new IllegalArgumentException(\"outer\");\n"
                + "            }\n"
                + "        } catch (IllegalArgumentException e) {\n"
                + "            r += 4;\n"
                + "        }\n"
                + "        return r;\n"
                + "    }\n"
                + "    static int deepNest() {\n"
                + "        int r = 0;\n"
                + "        try {\n"
                + "            try {\n"
                + "                try {\n"
                + "                    throw new IllegalStateException(\"1\");\n"
                + "                } catch (IllegalStateException e) {\n"
                + "                    r += 1;\n"
                + "                    throw new IllegalArgumentException(\"2\");\n"
                + "                }\n"
                + "            } catch (IllegalArgumentException e) {\n"
                + "                r += 2;\n"
                + "                throw new UnsupportedOperationException(\"3\");\n"
                + "            }\n"
                + "        } catch (UnsupportedOperationException e) {\n"
                + "            r += 4;\n"
                + "        }\n"
                + "        return r + 8;\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"rethrowSame \" + rethrowSame());\n"
                + "        System.out.println(\"rethrowNew \" + rethrowNew());\n"
                + "        System.out.println(\"deepNest \" + deepNest());\n"
                + "        System.out.println(\"NESTED_DONE\");\n"
                + "    }\n"
                + "}\n";
    }
}
