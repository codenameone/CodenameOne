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

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A frameless method that returns a value after several try blocks ending on
/// one label. Each such return ran a single END_TRY, which pops one block, and
/// left the others on the thread's try-block stack; a later throw then
/// longjmp'd into a frame that had already returned. The shape is the one
/// javac gives `try { r = f(); return r; } catch (A) ... catch (C) ... finally`.
public class FramelessTryReturnIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideRuntimeCompilerConfigs")
    public void returnAfterSharedTryEndUnwindsTheBlockStack(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("frameless-try-sources");
        Path classesDir = Files.createTempDirectory("frameless-try-classes");
        Path javaFile = sourceDir.resolve("FramelessTryApp.java");
        Files.write(javaFile, appSource().getBytes(StandardCharsets.UTF_8));
        Path javaApiDir = Files.createTempDirectory("java-api-classes");
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
        } else {
            compileArgs.add("-Xlint:-options");
            compileArgs.add("-bootclasspath");
        }
        compileArgs.add(javaApiDir.toString());
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs), "compilation failed with " + config);
        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("frameless-try-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FramelessTryApp");
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "FramelessTryApp-src");
        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        String out = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(buildDir.resolve("FramelessTryApp").toString()), buildDir);
        assertTrue(out.contains("caught late"), "the throw after the leaky returns was not caught:\n" + out);
        assertTrue(out.contains("calls 6"), out);
    }

    private static String appSource() {
        return "public class FramelessTryApp {\n"
                + "    static boolean running;\n"
                + "    static int calls;\n"
                + "    static Object make(int i) {\n"
                + "        if (i < 0) { throw new IllegalStateException(\"late\"); }\n"
                + "        calls++;\n"
                + "        return \"v\";\n"
                + "    }\n"
                + "    static Object resume(int i) {\n"
                + "        running = true;\n"
                + "        try {\n"
                + "            Object r = make(i);\n"
                + "            return r;\n"
                + "        } catch (IllegalStateException e) {\n"
                + "            throw e;\n"
                + "        } catch (RuntimeException e) {\n"
                + "            throw e;\n"
                + "        } catch (Error e) {\n"
                + "            throw e;\n"
                + "        } finally {\n"
                + "            running = false;\n"
                + "        }\n"
                + "    }\n"
                + "    static void leak() {\n"
                + "        resume(1);\n"
                + "        resume(2);\n"
                + "        resume(3);\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        leak();\n"
                + "        resume(4);\n"
                + "        try {\n"
                + "            for (int k = 0; k < 3; k++) { resume(k == 2 ? -1 : k); }\n"
                + "        } catch (IllegalStateException e) {\n"
                + "            System.out.println(\"caught \" + e.getMessage());\n"
                + "        }\n"
                + "        System.out.println(\"calls \" + calls);\n"
                + "    }\n"
                + "}\n";
    }
}
