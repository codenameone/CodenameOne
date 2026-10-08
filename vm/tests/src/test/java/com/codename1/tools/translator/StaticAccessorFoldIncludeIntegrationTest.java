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

/// A static accessor whose body is `GETSTATIC; CHECKCAST; ARETURN`, the shape
/// Kotlin compiles `emptyList()` to. optimize() drops the CHECKCAST in place, so
/// a caller emitted after the accessor's class saw a two-instruction body and
/// folded the call to the field read, while the dependency scan had seen three
/// instructions and never added the field owner's header. clang rejects the
/// resulting call to an undeclared `get_static_` function.
class StaticAccessorFoldIncludeIntegrationTest {

    // Callers named on both sides of the accessor's class, so at least one is
    // emitted after it whatever order the translator walks the classes in.
    private static final String SOURCE = "public class FoldApp {\n"
            + "    public static void main(String[] args) {\n"
            + "        int n = 0;\n"
            + "        n += CallA.use();\n"
            + "        n += CallB.use();\n"
            + "        n += CallC.use();\n"
            + "        n += CallD.use();\n"
            + "        n += CallE.use();\n"
            + "        n += CallF.use();\n"
            + "        n += CallG.use();\n"
            + "        n += CallH.use();\n"
            + "        n += CallJ.use();\n"
            + "        n += CallK.use();\n"
            + "        n += CallM.use();\n"
            + "        n += UseN.use();\n"
            + "        n += UseP.use();\n"
            + "        n += UseQ.use();\n"
            + "        n += UseR.use();\n"
            + "        n += UseS.use();\n"
            + "        n += UseT.use();\n"
            + "        n += UseU.use();\n"
            + "        n += UseV.use();\n"
            + "        n += UseW.use();\n"
            + "        n += UseX.use();\n"
            + "        n += UseY.use();\n"
            + "        report(n);\n"
            + "    }\n"
            + "    private static native void report(int value);\n"
            + "}\n"
            + "final class Holder implements Runnable {\n"
            + "    static final Object INSTANCE = new Holder();\n"
            + "    public void run() {\n"
            + "    }\n"
            + "}\n"
            + "final class Lists {\n"
            + "    static Runnable empty() {\n"
            + "        return (Runnable) Holder.INSTANCE;\n"
            + "    }\n"
            + "}\n"
            + "final class CallA {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallB {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallC {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallD {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallE {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallF {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallG {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallH {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallJ {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallK {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class CallM {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseN {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseP {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseQ {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseR {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseS {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseT {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseU {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseV {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseW {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseX {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n"
            + "final class UseY {\n"
            + "    static int use() {\n"
            + "        Object o = Lists.empty();\n"
            + "        return o == null ? 0 : 1;\n"
            + "    }\n"
            + "}\n";

    @Test
    void foldedAccessorIncludesTheFieldOwner() throws Exception {
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("staticfold-sources");
        Path classesDir = Files.createTempDirectory("staticfold-classes");
        Path javaApiDir = Files.createTempDirectory("staticfold-java-api");
        Files.write(sourceDir.resolve("FoldApp.java"), SOURCE.getBytes(StandardCharsets.UTF_8));

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
        args.add(sourceDir.resolve("FoldApp.java").toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, args), "FoldApp should compile");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.write(classesDir.resolve("native_staticfold.c"), ("#include \"cn1_globals.h\"\n"
                + "#include <stdio.h>\n"
                + "void FoldApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n"
                + "    printf(\"RESULT=%d\\n\", value);\n"
                + "}\n").getBytes(StandardCharsets.UTF_8));

        Path outputDir = Files.createTempDirectory("staticfold-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FoldApp");
        Path distDir = outputDir.resolve("dist");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(distDir.resolve("CMakeLists.txt"), "FoldApp-src");
        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        String output = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(buildDir.resolve("FoldApp").toString()), buildDir);
        assertTrue(output.contains("RESULT=22"), "the program should run: " + output);
    }
}
