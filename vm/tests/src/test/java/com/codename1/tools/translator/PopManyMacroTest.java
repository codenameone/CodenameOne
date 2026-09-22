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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PopManyMacroTest {
    @TempDir Path directory;

    @Test
    void stackReplacementMacrosCompileAsPlainCAndPreserveResults() throws Exception {
        Path runtime = Paths.get("../ByteCodeTranslator/src").toAbsolutePath();
        Files.write(directory.resolve("cn1_class_method_index.h"),
                "#define cn1_array_start_offset 100000\n".getBytes(StandardCharsets.UTF_8));
        Path source = directory.resolve("probe.c");
        try (java.io.InputStream input = getClass().getResourceAsStream("PopManyMacro.c")) {
            assertNotNull(input);
            Files.copy(input, source);
        }
        Path executable = directory.resolve("probe");
        run(Arrays.asList("clang", "-std=c11", "-Werror=implicit-function-declaration",
                "-I", runtime.toString(), "-I", directory.toString(),
                source.toString(), "-o", executable.toString()));
        run(Arrays.asList(executable.toString()));
    }

    private void run(List<String> command) throws Exception {
        Path output = directory.resolve("output.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(output.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Timed out: " + command);
        }
        assertEquals(0, process.exitValue(), new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
    }
}
