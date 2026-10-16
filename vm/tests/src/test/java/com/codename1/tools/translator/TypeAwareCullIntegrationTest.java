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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The type-aware half of ReachabilityCull, and the CN1_CULL_TRAP safety net behind it.
///
/// An instance method is live only once something allocates its class. That is only
/// safe if every way of creating an object is seen, and the ways that are easy to miss
/// are the ones this app uses: a class created solely by Class.forName on a name built
/// at run time, and a method that runs on a subclass instance. Both must behave as on
/// the JVM. A class nothing allocates must still COMPILE when live code names its
/// methods -- the dispatch function is emitted, its body is the culled stub -- which is
/// what broke the first translation of the Flutter gallery.
///
/// The trap is checked for real rather than by reading the stub: the test calls one
/// directly and expects the process to abort, naming the method.
class TypeAwareCullIntegrationTest {

    private static final String APP = "TypeAwareCullApp";

    @Test
    void typeAwareCullKeepsEveryCreationPathAndTrapsItsStubs() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("rta-cull-sources");
        Path classesDir = Files.createTempDirectory("rta-cull-classes");
        Path javaApiDir = Files.createTempDirectory("rta-cull-java-api");

        Path source = sourceDir.resolve(APP + ".java");
        Files.write(source, loadSource(APP + ".java").getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the type-aware cull test");
        }
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
                APP + " should compile against the JavaAPI");

        Map<String, String> expected = parseCases(runJavaMain(config, classesDir, javaApiDir));
        assertEquals(4, expected.size(), "JVM run should emit every case: " + expected);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        // The type-aware arm, with the trap on.
        Path outputDir = Files.createTempDirectory("rta-cull-output");
        translate(classesDir, outputDir, null);
        Path distDir = outputDir.resolve("dist");
        Path srcDir = distDir.resolve(APP + "-src");

        String unit = read(srcDir.resolve("UnitShape.c"));
        assertTrue(unit.contains(stub("UnitShape.area()I")),
                "Nothing allocates UnitShape, so the area() every CullShape call reaches must be the culled stub");
        assertTrue(unit.contains(stub("UnitShape.side()I")),
                "callIfUnit names UnitShape.side() but can never reach it with a UnitShape");
        assertFalse(read(srcDir.resolve("ReflectedShape.c")).contains(stub("ReflectedShape.area()I")),
                "ReflectedShape is created only by Class.forName(...).newInstance(), and must be kept");
        assertFalse(read(srcDir.resolve("SquareShape.c")).contains(stub("SquareShape.area()I")),
                "SquareShape is allocated with NEW");
        assertFalse(read(srcDir.resolve("BaseShape.c")).contains(stub("BaseShape.inherited()I")),
                "BaseShape.inherited runs on a DerivedShape, so allocating the subclass keeps it");
        String main = read(srcDir.resolve(APP + ".c"));
        assertTrue(!main.contains("TypeAwareCullApp_helperOnlyUnitCalls___R_int")
                        || main.contains(stub(APP + ".helperOnlyUnitCalls()I")),
                "helperOnlyUnitCalls is called only from UnitShape.area's culled body");

        // A direct call into a stub, to prove the trap aborts rather than returning 0.
        Files.write(srcDir.resolve("UnitShape.c"), (unit
                + "\nint cn1TestCallCulled(void) { return (int)UnitShape_side___R_int(0, JAVA_NULL); }\n")
                .getBytes(StandardCharsets.UTF_8));
        Path nativeMain = srcDir.resolve(APP + ".c");
        String mainSignature = "int main(int argc, char *argv[]) {";
        assertTrue(main.contains(mainSignature), "Generated native main should exist");
        Files.write(nativeMain, main.replace(mainSignature,
                "extern int cn1TestCallCulled(void);\n" + mainSignature
                        + "\n    if (argc == 2 && strcmp(argv[1], \"--call-culled\") == 0)"
                        + " return cn1TestCallCulled();").getBytes(StandardCharsets.UTF_8));

        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, APP + "-src");
        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(APP);
        String out = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertFalse(out.contains("CN1 FATAL"), "No culled method may run: " + out);
        assertTrue(out.contains("DONE"), "ParparVM run should complete. Output: " + out);
        assertEquals(expected, parseCases(out), "The culled build must behave exactly as the JVM");

        ProcessBuilder pb = new ProcessBuilder(executable.toString(), "--call-culled");
        pb.directory(buildDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String trapped;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            trapped = reader.lines().collect(Collectors.joining("\n"));
        }
        assertNotEquals(0, p.waitFor(), "A trapped stub must abort. Output: " + trapped);
        assertTrue(trapped.contains("CN1 FATAL: called UnitShape.side()I, which the translator culled"),
                "The abort must name the culled method. Output: " + trapped);

        // Without the type-aware gate the same method keeps its body, so the stub
        // assertions above are about the gate and not about something else culling it.
        Path plainDir = Files.createTempDirectory("rta-cull-plain");
        translate(classesDir, plainDir, "false");
        String plainUnit = read(plainDir.resolve("dist").resolve(APP + "-src").resolve("UnitShape.c"));
        assertFalse(plainUnit.contains(stub("UnitShape.area()I")),
                "-Dcn1.cullRta=false must restore the signature-only answer");
    }

    private static void translate(Path classesDir, Path outputDir, String rta) throws Exception {
        String oldTrap = System.getProperty("cn1.cull.trap");
        String oldRta = System.getProperty("cn1.cullRta");
        System.setProperty("cn1.cull.trap", "true");
        if (rta == null) {
            System.clearProperty("cn1.cullRta");
        } else {
            System.setProperty("cn1.cullRta", rta);
        }
        try {
            Parser.cleanup();
            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, APP);
        } finally {
            restore("cn1.cull.trap", oldTrap);
            restore("cn1.cullRta", oldRta);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static String stub(String method) {
        return "cn1CulledMethodCalled(\"" + method + "\")";
    }

    private static String read(Path p) throws Exception {
        assertTrue(Files.exists(p), p + " should have been generated");
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private Map<String, String> parseCases(String output) {
        Map<String, String> cases = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            if (!line.startsWith("CASE|")) {
                continue;
            }
            String body = line.substring("CASE|".length());
            int separator = body.indexOf('|');
            assertTrue(separator > 0, "Malformed case line: " + line);
            cases.put(body.substring(0, separator), body.substring(separator + 1));
        }
        return cases;
    }

    private String loadSource(String name) throws Exception {
        java.io.InputStream in = TypeAwareCullIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/" + name);
        assertNotNull(in, name + " test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir)
            throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir, APP);
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
            for (CompilerHelper.CompilerConfig config : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        return null;
    }
}
