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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs a translated program as a command-line program under Node with
 * {@code vm/selfhost/js/run-program.js}, the runner the self-hosted translator uses:
 * java.io reaches the real file system through the runtime's file natives and a
 * host-supplied {@code jvm.fileSystem}, the environment through {@code jvm.env}, and
 * the program's exit status and standard output are the Node process's own.
 */
class JavascriptProgramRunnerTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void runsAProgramThatWorksOnFilesAndTheEnvironment(CompilerHelper.CompilerConfig config) throws Exception {
        Path runner = Paths.get("..", "selfhost", "js", "run-program.js").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isRegularFile(runner), "run-program.js not found at " + runner);
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("js-program-src");
        Path classesDir = Files.createTempDirectory("js-program-classes");
        Path javaApiDir = Files.createTempDirectory("js-program-javaapi");
        Files.write(sourceDir.resolve("JsFileIoProgram.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsFileIoProgram.java").getBytes(StandardCharsets.UTF_8));
        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);
        Path outputDir = Files.createTempDirectory("js-program-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsFileIoProgram");
        Path bundle = outputDir.resolve("dist").resolve("JsFileIoProgram-js");

        Path workDir = Files.createTempDirectory("js-program-files");
        ProcessBuilder pb = new ProcessBuilder("node", runner.toString(), bundle.toString(), workDir.toString());
        pb.environment().put("CN1_JS_PROGRAM_PROBE", "from-host");
        Process process = pb.start();
        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());
        int status = process.waitFor();

        assertEquals(63, status, "exit status is the program's System.exit mask. stdout=" + stdout + " stderr=" + stderr);
        assertTrue(stdout.contains("sb:42"), "println(Object) must print toString(), not the class name: " + stdout);
        assertTrue(stdout.contains("mask:63"), stdout);
        assertFalse(Files.exists(workDir.resolve("a").resolve("b")), "the program deleted what it created");
        assertTrue(Files.isDirectory(workDir.resolve("a")), "the program created its directories on the real file system");
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
