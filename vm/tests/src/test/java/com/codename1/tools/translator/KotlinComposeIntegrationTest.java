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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Kotlin coroutines and the Jetpack Compose runtime, translated by ParparVM, must
/// print exactly what they print on HotSpot.
///
/// The fixture (`ComposeCoroutinesApp.kt`) is compiled with the Kotlin compiler and
/// the Compose compiler plugin, then run twice: on HotSpot against the runtime jars,
/// and as a native executable translated from those same jars plus JavaAPI. The jars
/// are copied by `maven-dependency-plugin` into `target/kmp-runtime` and
/// `target/kmp-compiler` and are never on this module's class path (see the pom).
///
/// Lines starting with `BENCH` are timings the fixture writes to stderr; they are
/// printed for information and excluded from the comparison.
class KotlinComposeIntegrationTest {
    private static final String RESOURCES = "/com/codename1/tools/translator/";
    private static final String MAIN = "ComposeCoroutinesApp";

    @Test
    void composeAndCoroutinesMatchHotSpot() throws Exception {
        Parser.cleanup();
        List<CompilerHelper.CompilerConfig> jdks = CompilerHelper.getAvailableCompilers("11");
        Assumptions.assumeTrue(!jdks.isEmpty(), "Kotlin compiler needs a JDK 11 or newer");
        Assumptions.assumeTrue(onPath("cmake") && onPath("clang"), "cmake and clang are required");
        Path jdk = jdks.get(0).jdkHome;
        String java = jdk.resolve("bin/java").toString();

        Path target = Paths.get("target").toAbsolutePath();
        List<Path> runtime = jars(target.resolve("kmp-runtime"));
        List<Path> compiler = jars(target.resolve("kmp-compiler"));
        Path plugin = compiler.stream()
                .filter(p -> p.getFileName().toString().startsWith("kotlin-compose-compiler-plugin"))
                .findFirst().orElseThrow(() -> new AssertionError("Compose compiler plugin jar missing"));
        List<Path> kotlinc = new ArrayList<>(compiler);
        kotlinc.remove(plugin);

        Path work = Files.createTempDirectory("kotlin-compose-integration-");
        Path ktSource = copyResource(work, MAIN + ".kt");
        Path javaSource = copyResource(work, MAIN + ".java");
        Path kotlinOut = Files.createDirectories(work.resolve("kotlin-out"));
        Path appOut = Files.createDirectories(work.resolve("app-out"));

        run(work, 600, java, "-cp", classpath(kotlinc), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                "-no-stdlib", "-no-reflect", "-jvm-target", "11", "-Xplugin=" + plugin,
                "-cp", classpath(runtime), "-d", kotlinOut.toString(), ktSource.toString());
        run(work, 300, jdk.resolve("bin/javac").toString(), "--release", "8", "-cp", kotlinOut.toString(),
                "-d", appOut.toString(), javaSource.toString());

        List<Path> hotspotCp = new ArrayList<>(Arrays.asList(appOut, kotlinOut));
        hotspotCp.addAll(runtime);
        String hotspot = run(work, 300, java, "-cp", classpath(hotspotCp), MAIN);

        // One class directory for the translator: every runtime jar, JavaAPI, the app.
        Path classes = Files.createDirectories(work.resolve("classes"));
        for (Path jar : runtime) {
            unzip(jar, classes);
        }
        Path api = Files.createDirectories(work.resolve("api"));
        CompilerHelper.compileJavaAPI(api, CompilerHelper.getAvailableCompilers("1.8").get(0));
        CompilerHelper.copyDirectory(api, classes);
        CompilerHelper.copyDirectory(kotlinOut, classes);
        CompilerHelper.copyDirectory(appOut, classes);

        Path output = Files.createDirectories(work.resolve("translated"));
        CleanTargetIntegrationTest.runTranslator(classes, output, MAIN);
        Path dist = output.resolve("dist");
        Path build = Files.createDirectories(dist.resolve("build"));
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(dist.resolve("CMakeLists.txt"), MAIN + "-src");
        List<String> configure = new ArrayList<>(Arrays.asList("cmake", "-S", dist.toString(), "-B",
                build.toString(), "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, dist);
        CleanTargetIntegrationTest.runCmakeBuild(build, dist);
        String parparvm = run(build, 300, build.resolve(MAIN).toString());

        report("HotSpot", hotspot);
        report("ParparVM", parparvm);
        String expected = withoutBench(hotspot);
        assertTrue(expected.contains("DONE"), "HotSpot run did not finish:\n" + hotspot);
        assertTrue(expected.contains("CHECK"), "HotSpot run printed no checks:\n" + hotspot);
        assertEquals(expected, withoutBench(parparvm));
    }

    private static boolean onPath(String tool) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (Files.isExecutable(Paths.get(dir, tool)) || Files.isExecutable(Paths.get(dir, tool + ".exe"))) {
                return true;
            }
        }
        return false;
    }

    private static List<Path> jars(Path dir) throws IOException {
        assertTrue(Files.isDirectory(dir), dir + " is filled by maven-dependency-plugin; run through Maven");
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> out = s.filter(p -> p.toString().endsWith(".jar")).sorted().collect(Collectors.toList());
            assertFalse(out.isEmpty(), "no jars in " + dir);
            return out;
        }
    }

    private static String classpath(List<Path> entries) {
        return entries.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }

    private Path copyResource(Path dir, String name) throws IOException {
        Path out = dir.resolve(name);
        try (InputStream in = getClass().getResourceAsStream(RESOURCES + name)) {
            assertNotNull(in, name);
            Files.copy(in, out);
        }
        return out;
    }

    /// Keeps `META-INF/services` (coroutines finds its dispatchers there) and drops
    /// the rest of `META-INF` and every `module-info.class`.
    private static void unzip(Path jar, Path dest) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.endsWith("module-info.class")
                        || (name.startsWith("META-INF/") && !name.startsWith("META-INF/services/"))) {
                    continue;
                }
                Path out = dest.resolve(name).normalize();
                assertTrue(out.startsWith(dest), name);
                Files.createDirectories(out.getParent());
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static String withoutBench(String output) {
        StringBuilder b = new StringBuilder();
        for (String line : output.split("\r?\n")) {
            if (!line.startsWith("BENCH")) {
                b.append(line).append('\n');
            }
        }
        return b.toString();
    }

    private static void report(String vm, String output) {
        for (String line : output.split("\r?\n")) {
            if (line.startsWith("BENCH")) {
                System.out.println(vm + ": " + line);
            }
        }
    }

    private static String run(Path dir, long seconds, String... command) throws Exception {
        Path log = Files.createTempFile(dir, "kmp-command-", ".log");
        Process process = new ProcessBuilder(command).directory(dir.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        process.getOutputStream().close();
        if (!process.waitFor(seconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Timed out: " + Arrays.toString(command));
        }
        String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), Arrays.toString(command) + "\n" + output);
        return output;
    }
}
