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
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HeavyLoadBenchmarkCompilationTest {
    @TempDir
    Path tempDir;

    @Test
    void compilesValidSample() throws Exception {
        Path sources = Files.createDirectory(tempDir.resolve("sources"));
        Path classes = Files.createDirectory(tempDir.resolve("classes"));
        Files.write(sources.resolve("Sample.java"),
                "public class Sample {}".getBytes(StandardCharsets.UTF_8));

        HeavyLoadBenchmarkTest.compileSample(ToolProvider.getSystemJavaCompiler(),
                classes.toString(), sources, classes);

        assertTrue(Files.isRegularFile(classes.resolve("Sample.class")));
    }

    @Test
    void rejectsCompilerErrorsWithDiagnostics() throws Exception {
        Path sources = Files.createDirectory(tempDir.resolve("sources"));
        Path classes = Files.createDirectory(tempDir.resolve("classes"));
        Files.write(sources.resolve("Sample.java"),
                "public class Sample { MissingType field; }".getBytes(StandardCharsets.UTF_8));

        AssertionError failure = assertThrows(AssertionError.class,
                () -> HeavyLoadBenchmarkTest.compileSample(ToolProvider.getSystemJavaCompiler(),
                        classes.toString(), sources, classes));

        assertTrue(failure.getMessage().contains("refusing to benchmark an incomplete workload"));
        assertTrue(failure.getMessage().contains("MissingType"));
        assertFalse(Files.exists(classes.resolve("Sample.class")));
    }

    @Test
    void rejectsEmptyWorkload() throws Exception {
        Path sources = Files.createDirectory(tempDir.resolve("sources"));
        Path classes = Files.createDirectory(tempDir.resolve("classes"));

        AssertionError failure = assertThrows(AssertionError.class,
                () -> HeavyLoadBenchmarkTest.compileSample(ToolProvider.getSystemJavaCompiler(),
                        classes.toString(), sources, classes));

        assertTrue(failure.getMessage().contains("contains no Java sources"));
    }
}
