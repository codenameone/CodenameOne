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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles {@code cn1_debugger_objects.c} — the on-device debugger's decision
 * logic — together with a caller-supplied driver, and runs it.
 *
 * The debugger's rules about what it may dereference and which locals it may
 * report are the difference between a usable session and a process that dies
 * at a breakpoint, so they are worth testing directly rather than by
 * restatement. The unit is plain C and depends only on {@code cn1_globals.h},
 * whose single generated-header dependency a one-line stub satisfies, so it
 * builds and runs on a host.
 *
 * macOS only: the read probe rests on {@code vm_read_overwrite}, and iOS
 * debugging is a macOS activity regardless.
 */
final class NativeDebuggerHarness {

    private final Path executable;

    private NativeDebuggerHarness(Path executable) {
        this.executable = executable;
    }

    /** Builds a driver against the debugger unit. {@code driver} is C source. */
    static NativeDebuggerHarness compile(String name, String driver) throws Exception {
        Path dir = Files.createTempDirectory("cn1-debugger-" + name);
        // cn1_globals.h includes the translator-generated class-id header; the
        // only symbol it needs from it is the array-id base.
        Files.write(dir.resolve("cn1_class_method_index.h"),
                "#define cn1_array_start_offset 100000\n".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("driver.c"), driver.getBytes(StandardCharsets.UTF_8));
        // Symbols the translator emits per build that the unit references but
        // does not own. Definitions, not stubs of behaviour: the unit only ever
        // compares their addresses.
        Files.write(dir.resolve("generated_symbols.c"),
                GENERATED_SYMBOLS.getBytes(StandardCharsets.UTF_8));

        Path executable = dir.resolve(name);
        List<String> command = new ArrayList<>(Arrays.asList(
                "clang",
                "-DCN1_ON_DEVICE_DEBUG",
                "-I", dir.toString(),
                "-I", runtimeInclude().toString(),
                "-I", nativeSources().toString(),
                "-o", executable.toString(),
                dir.resolve("driver.c").toString(),
                dir.resolve("generated_symbols.c").toString(),
                nativeSources().resolve("cn1_debugger_objects.c").toString()));
        Result compiled = exec(command, dir);
        assertEquals(0, compiled.exitCode,
                "the debugger's decision logic should compile standalone:\n" + compiled.output);
        return new NativeDebuggerHarness(executable);
    }

    Result run(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(Arrays.asList(args));
        return exec(command, executable.getParent());
    }

    /** Runs one case and returns its trimmed stdout, failing if the probe died. */
    String verdict(String... args) throws Exception {
        Result result = run(args);
        assertEquals(0, result.exitCode,
                "probe died on " + Arrays.toString(args) + ": " + result.output);
        return result.output.trim();
    }

    /**
     * java.lang.Integer's clazz and the tagged-int proxy are emitted by the
     * translator into the app's own sources. The debugger unit references both
     * by symbol, so a host build has to supply them.
     */
    private static final String GENERATED_SYMBOLS =
            "#include \"cn1_globals.h\"\n"
          + "struct clazz class__java_lang_Integer = { 0 };\n"
          // The collector's mark entry point. Records whether a nominated
          // reference was reached, rather than buffering every mark -- the
          // table can hold thousands, so a fixed buffer would answer "was it
          // in the first N" instead of "was it marked".
          + "JAVA_OBJECT cn1MarkRootTarget = 0;\n"
          + "int cn1MarkRootTargetSeen = 0;\n"
          + "int cn1MarkedRootCount = 0;\n"
          + "void gcMarkObject(struct ThreadLocalData* t, JAVA_OBJECT o, JAVA_BOOLEAN force) {\n"
          + "    (void)t; (void)force;\n"
          + "    cn1MarkedRootCount++;\n"
          + "    if (o == cn1MarkRootTarget) cn1MarkRootTargetSeen = 1;\n"
          + "}\n"
          + "#if CN1_TAGGED_ACTIVE\n"
          + "struct JavaObjectPrototype cn1TaggedProxy = { 0 };\n"
          + "#endif\n";

    private static Path runtimeInclude() {
        return resolve(Paths.get("..", "ByteCodeTranslator", "src"));
    }

    private static Path nativeSources() {
        return resolve(Paths.get("..", "..", "Ports", "iOSPort", "nativeSources"));
    }

    private static Path resolve(Path relative) {
        Path path = relative.normalize().toAbsolutePath();
        assertTrue(Files.isDirectory(path), "expected sources at " + path);
        return path;
    }

    private static Result exec(List<String> command, Path workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) {
                captured.write(buffer, 0, read);
            }
        }
        int exitCode = process.waitFor();
        return new Result(exitCode, new String(captured.toByteArray(), StandardCharsets.UTF_8));
    }

    static final class Result {
        final int exitCode;
        final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
