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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// A constructor that stores a field nothing reads, inlined at its call site.
/// Dead field elimination deletes the field and the store in the constructor,
/// but the call site's inlined copy of the stores must not keep writing it:
/// the generated C then assigned a struct member that no longer exists.
class DeadFieldInlinedConstructorIntegrationTest {

    // Two constructors with the same signature, each storing a field nothing
    // reads: BytecodeMethod.equals ignores the declaring class, so a HashSet of
    // edited methods kept one of them and only that one's inlining plan was
    // refreshed. The other's call site still stored the removed field.
    private static final String SOURCE = "public class DeadFieldApp {\n"
            + "    static final class Peer {\n"
            + "        final int slot;\n"
            + "        final int windowId;\n"
            + "        Peer(int slot, int windowId) {\n"
            + "            this.slot = slot;\n"
            + "            this.windowId = windowId;\n"
            + "        }\n"
            + "    }\n"
            + "    static final class Other {\n"
            + "        final int value;\n"
            + "        final int unused;\n"
            + "        Other(int value, int unused) {\n"
            + "            this.value = value;\n"
            + "            this.unused = unused;\n"
            + "        }\n"
            + "    }\n"
            + "    static native int create(int id);\n"
            + "    static Object make(int windowId) {\n"
            + "        int slot = create(windowId);\n"
            + "        return new Peer(slot, windowId);\n"
            + "    }\n"
            + "    static Object other(int v) {\n"
            + "        int w = create(v);\n"
            + "        return new Other(w, v);\n"
            + "    }\n"
            + "    public static void main(String[] args) {\n"
            + "        Object o = make(41);\n"
            + "        Object p = other(0);\n"
            + "        int a = o instanceof Peer ? ((Peer) o).slot : -100;\n"
            + "        int b = p instanceof Other ? ((Other) p).value : -100;\n"
            + "        report(a + b - 1);\n"
            + "    }\n"
            + "    private static native void report(int value);\n"
            + "}\n";

    @Test
    void inlinedConstructorDoesNotStoreARemovedField() throws Exception {
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("deadfield-sources");
        Path classesDir = Files.createTempDirectory("deadfield-classes");
        Path javaApiDir = Files.createTempDirectory("deadfield-java-api");
        Files.write(sourceDir.resolve("DeadFieldApp.java"), SOURCE.getBytes(StandardCharsets.UTF_8));

        List<CompilerHelper.CompilerConfig> configs = CompilerHelper.getAvailableCompilers("1.8");
        if (configs.isEmpty()) {
            fail("No compiler available for target 1.8");
        }
        CompilerHelper.CompilerConfig config = configs.get(0);
        for (CompilerHelper.CompilerConfig c : configs) {
            if (CompilerHelper.getJdkMajor(c) < 9) {
                config = c;
                break;
            }
        }
        CompilerHelper.compileJavaAPI(javaApiDir, config);
        List<String> args = new ArrayList<>(Arrays.asList("-source", "1.8", "-target", "1.8"));
        args.add(CompilerHelper.useClasspath(config) ? "-classpath" : "-bootclasspath");
        args.add(javaApiDir.toString());
        args.add("-d");
        args.add(classesDir.toString());
        args.add(sourceDir.resolve("DeadFieldApp.java").toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, args), "DeadFieldApp should compile");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.write(classesDir.resolve("native_deadfield.c"), ("#include \"cn1_globals.h\"\n"
                + "#include <stdio.h>\n"
                + "JAVA_INT DeadFieldApp_create___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT id) {\n"
                + "    return id + 1;\n"
                + "}\n"
                + "void DeadFieldApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n"
                + "    printf(\"RESULT=%d\\n\", value);\n"
                + "}\n").getBytes(StandardCharsets.UTF_8));

        Path outputDir = Files.createTempDirectory("deadfield-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "DeadFieldApp");
        Path distDir = outputDir.resolve("dist");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(distDir.resolve("CMakeLists.txt"), "DeadFieldApp-src");
        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        String output = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(buildDir.resolve("DeadFieldApp").toString()), buildDir);
        assertTrue(output.contains("RESULT=42"), "the program should run: " + output);
    }
}
