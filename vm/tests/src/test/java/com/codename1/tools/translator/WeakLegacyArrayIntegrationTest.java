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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** A weak cache must clear large arrays before the legacy sweep frees them. */
@Tag("benchmark")
class WeakLegacyArrayIntegrationTest {
    @Test
    void freshLegacyReferentsAreClearedBeforeTheirStorageIsReused() throws Exception {
        Parser.cleanup();
        CompilerHelper.CompilerConfig compiler = null;
        for (String version : new String[]{"11", "17", "21", "25", "1.8"}) {
            for (CompilerHelper.CompilerConfig candidate : CompilerHelper.getAvailableCompilers(version)) {
                if (CompilerHelper.isJavaApiCompatible(candidate)) { compiler = candidate; break; }
            }
            if (compiler != null) break;
        }
        assertNotNull(compiler, "A compatible JDK is required");
        Path root = Files.createTempDirectory("weak-legacy-array-");
        try {
            Path api = Files.createDirectory(root.resolve("api"));
            Path classes = Files.createDirectory(root.resolve("classes"));
            Path source = root.resolve("WeakLegacyArrayApp.java");
            try (java.io.InputStream in = getClass().getResourceAsStream("WeakLegacyArrayApp.java")) {
                assertNotNull(in);
                Files.copy(in, source);
            }
            CompilerHelper.compileJavaAPI(api, compiler);
            List<String> args = Arrays.asList("-source", compiler.targetVersion,
                    "-target", compiler.targetVersion,
                    CompilerHelper.useClasspath(compiler) ? "-classpath" : "-bootclasspath",
                    api.toString(), "-d", classes.toString(), source.toString());
            assertEquals(0, CompilerHelper.compile(compiler.jdkHome, args), CompilerHelper.getLastErrorLog());
            CompilerHelper.copyDirectory(api, classes);
            Path output = Files.createDirectory(root.resolve("output"));
            CleanTargetIntegrationTest.runTranslator(classes, output, "WeakLegacyArrayApp");
            Path dist = output.resolve("dist");
            CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(dist.resolve("CMakeLists.txt"), "WeakLegacyArrayApp-src");
            Path build = Files.createDirectory(dist.resolve("build"));
            CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", dist.toString(),
                    "-B", build.toString(), "-DCMAKE_BUILD_TYPE=Release",
                    "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), dist);
            CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", build.toString()), dist);
            Path log = root.resolve("run.log");
            ProcessBuilder pb = new ProcessBuilder(build.resolve("WeakLegacyArrayApp").toString());
            pb.environment().put("CN1_GC_MARK_THREADS", "1");
            pb.redirectErrorStream(true).redirectOutput(log.toFile());
            Process process = pb.start();
            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("Weak-array workload timed out");
            }
            String text = new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), text);
            assertTrue(text.contains("WEAK_LEGACY_ARRAY_OK"), text);
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
