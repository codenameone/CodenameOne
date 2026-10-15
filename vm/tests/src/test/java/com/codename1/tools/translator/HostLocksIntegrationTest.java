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
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The PosixJdkHost, File and lock programs, built ONCE per compiler
/// configuration instead of once per program per configuration.
///
/// They used to be five tests over the same compiler matrix, each paying for its
/// own JavaAPI compile, translation and native build. The program lives in
/// `src/test/resources/com/codename1/tools/translator/hostlocks`; `HostLocksApp`
/// says which parts run and why the lock programs are only built. What each old
/// test asserted is still asserted:
///
/// - the program compiles with the configuration's compiler and target,
/// - the translator emits a CMake project and it builds,
/// - the run exits 0 (`FileTestApp` exits 1 on any failure; `runCommand` fails
///   on a non-zero exit), and
/// - it prints `POSIX-HOST-OK`, which `PosixHostApp` prints only after checking
///   the JdkHost defaults and then the installed PosixJdkHost.
public class HostLocksIntegrationTest {

    private static final String DIR = "/com/codename1/tools/translator/hostlocks";

    @ParameterizedTest
    @MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void hostFileAndLockProgramsBuildAndRun(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        URL url = HostLocksIntegrationTest.class.getResource(DIR + "/HostLocksApp.java");
        assertNotNull(url, "hostlocks program resources should be on the test class path");
        Path programDir = Paths.get(url.toURI()).getParent();

        Path work = Files.createTempDirectory("hostlocks");
        Path classesDir = Files.createDirectories(work.resolve("classes"));
        Path javaApiDir = Files.createDirectories(work.resolve("javaapi"));

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
        try (Stream<Path> files = Files.list(programDir)) {
            compileArgs.addAll(files.filter(f -> f.toString().endsWith(".java"))
                    .map(Path::toString).sorted().collect(Collectors.toList()));
        }
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "the hostlocks program should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(programDir.resolve("native_report.c"), classesDir.resolve("native_report.c"));

        Path outputDir = Files.createDirectories(work.resolve("output"));
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "HostLocksApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "HostLocksApp-src");

        Path buildDir = Files.createDirectories(distDir.resolve("build"));
        List<String> configure = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCmakeBuild(buildDir, distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("HostLocksApp"));
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("POSIX-HOST-OK"), "PosixJdkHost fixture failed, output was:\n" + output);
    }
}
