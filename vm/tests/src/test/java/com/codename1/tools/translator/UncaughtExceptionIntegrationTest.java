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
 * An exception no handler catches must end a clean-target program, loudly.
 *
 * It used to be discarded: throwException walked the try-block stack, found no
 * handler, and RETURNED -- so the generated code carried straight on with the
 * statement after the throw, with the method's locals in whatever state the
 * failed operation left them. On an app target something upstream (the EDT's own
 * catch) nearly always exists, which is why it went unnoticed for years. A clean
 * target has none, and the way this surfaced was a client whose TLS handshake was
 * rejected, after which the program kept going and segfaulted two statements
 * later on a null it should never have had.
 *
 * The four assertions below are the contract: execution stops AT the throw, the
 * message is printed, a stack trace naming the throwing frame is printed, and the
 * process exits non-zero. All four matter -- an exit code with no message is
 * unactionable in a log, a message with a zero exit makes CI call a failed run a
 * pass, and if execution continues past the throw the other three can all hold
 * while the bug is still there.
 */
class UncaughtExceptionIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void uncaughtExceptionIsFatal(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("uncaught-sources");
        Path classesDir = Files.createTempDirectory("uncaught-classes");
        Path javaApiDir = Files.createTempDirectory("uncaught-java-api");
        Files.write(sourceDir.resolve("UncaughtApp.java"),
                uncaughtSource().getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("uncaught-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "UncaughtApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "UncaughtApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        // Deliberately NOT runCommand: that asserts a zero exit, and a zero exit is
        // precisely the failure this test exists to catch.
        Path executable = buildDir.resolve(CompilerHelper.executableName("UncaughtApp"));
        ProcessBuilder run = new ProcessBuilder(executable.toString());
        run.redirectErrorStream(true);
        final Process p = run.start();
        // Drained on a separate thread. Reading inline blocks until the child closes
        // stdout, so a program that HANGS -- which is one of the regressions this test
        // exists to catch -- never reaches the timeout below, and the job sits until
        // CI kills it instead of failing here. A timeout the guarded failure prevents
        // from being evaluated is not a timeout.
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
        }, "uncaught-app-output");
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

        assertTrue(output.contains("UNCAUGHT_BEFORE"),
                "the program should have run up to the throw:\n" + output);
        assertTrue(output.contains("deliberate failure with a message"),
                "the exception's message must be reported, not just its type:\n" + output);
        assertTrue(output.contains("UncaughtApp.open"),
                "a stack trace naming the throwing frame must be reported:\n" + output);
        assertTrue(!output.contains("UNCAUGHT_AFTER"),
                "execution must stop at the throw, not carry on past it:\n" + output);
        assertEquals(1, p.exitValue(),
                "a program killed by an uncaught exception must not report success:\n" + output);
    }


    /**
     * open() throws with nothing above it that catches. UNCAUGHT_AFTER lines mark
     * every point the old behaviour would have carried on to.
     */
    private static String uncaughtSource() {
        return "public class UncaughtApp {\n"
                + "    static int open(int depth) {\n"
                + "        if (depth > 0) {\n"
                + "            return open(depth - 1);\n"
                + "        }\n"
                + "        throw new IllegalStateException(\"deliberate failure with a message\");\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"UNCAUGHT_BEFORE\");\n"
                + "        int r = open(2);\n"
                + "        System.out.println(\"UNCAUGHT_AFTER value \" + r);\n"
                + "    }\n"
                + "}\n";
    }
}
