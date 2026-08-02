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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Differential test for {@code java.lang.String.format} (issue #5482).
 *
 * <p>{@code String.format} used to be a native method. Its Objective-C branch built an
 * argument vector by hand, threw away the string it formatted, and returned
 * {@code fromNSString([NSString init])} -- sending {@code init} to the {@code NSString}
 * class object, which aborts the process with
 * {@code +[NSString init]: cannot init a class object}. Because that branch is behind
 * {@code #if defined(__APPLE__) && defined(__OBJC__)} and this suite runs on Linux in CI,
 * nothing ever executed it. The C fallback that CI did run silently ignored width and
 * precision, so {@code "%.3f"} rendered every digit of the double.</p>
 *
 * <p>Formatting is now plain Java shared by every target, so this one test covers the
 * iOS behavior too. It runs the same program on the JVM and under ParparVM and requires
 * the two renderings to be identical, case by case -- which pins the output against the
 * real {@code java.util.Formatter}, not against a hand-written expectation.</p>
 */
class StringFormatIntegrationTest {

    @Test
    void formatOutputMatchesTheJdkCaseByCase() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("string-format-sources");
        Path classesDir = Files.createTempDirectory("string-format-classes");
        Path javaApiDir = Files.createTempDirectory("string-format-java-api");

        Path source = sourceDir.resolve("StringFormatApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the String.format integration test");
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

        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "StringFormatApp should compile against the JavaAPI");

        // The JVM run resolves java.lang.String from the boot class path, so this side is
        // the real java.util.Formatter no matter what is on the classpath.
        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        Map<String, String> expected = parseCases(javaOutput, "CASE|");
        assertFalse(expected.isEmpty(), "JVM run should emit cases. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("string-format-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "StringFormatApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "StringFormatApp-src");

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

        Path executable = buildDir.resolve("StringFormatApp");
        String parparOutput = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(executable.toString()), buildDir);

        // The Objective-C native aborted the process here rather than returning.
        assertTrue(parparOutput.contains("DONE"),
                "ParparVM run should complete String.format without dying. Output: " + parparOutput);

        Map<String, String> actual = parseCases(parparOutput, "CASE|");
        assertEquals(expected.keySet(), actual.keySet(),
                "ParparVM should emit exactly the cases the JVM emitted");

        List<String> differences = new ArrayList<>();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String parpar = actual.get(entry.getKey());
            if (!entry.getValue().equals(parpar)) {
                differences.add(entry.getKey()
                        + "\n    jdk     : " + entry.getValue()
                        + "\n    parparvm: " + parpar);
            }
        }
        assertTrue(differences.isEmpty(),
                "ParparVM String.format diverged from the JDK in " + differences.size()
                        + " case(s):\n" + String.join("\n", differences));

        // Cases that cannot go through the shared diff, because the JDK either formats
        // them (%a, %t, which ParparVM does not implement) or because the JDK's own
        // answer moved between versions (%0$s is accepted on 11, rejected from 16 on).
        // Either way the requirement is the same: a catchable Java exception, since the
        // point of issue #5482 was that a formatting problem took the whole process down.
        Map<String, String> expectedCn1Only = new LinkedHashMap<>();
        expectedCn1Only.put("hexFloat", "EX|java.util.UnknownFormatConversionException");
        expectedCn1Only.put("dateTime", "EX|java.util.UnknownFormatConversionException");
        expectedCn1Only.put("zeroArgumentIndex", "EX|java.util.IllegalFormatArgumentIndexException");

        Map<String, String> unsupported = parseCases(parparOutput, "CN1ONLY|");
        assertEquals(expectedCn1Only, unsupported,
                "Unsupported and version-dependent specifiers must throw, not crash or guess");
    }

    private Map<String, String> parseCases(String output, String prefix) {
        Map<String, String> cases = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            if (!line.startsWith(prefix)) {
                continue;
            }
            String body = line.substring(prefix.length());
            int separator = body.indexOf('|');
            assertTrue(separator > 0, "Malformed case line: " + line);
            cases.put(body.substring(0, separator), body.substring(separator + 1));
        }
        return cases;
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = StringFormatIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/StringFormatApp.java");
        assertNotNull(in, "StringFormatApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir)
            throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "StringFormatApp"
        );
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
}
