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

import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileClassIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    public void testFileClassMethods(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("file-test-sources");
        Path classesDir = Files.createTempDirectory("file-test-classes");
        Path javaFile = sourceDir.resolve("FileTestApp.java");

        Files.write(javaFile, fileTestAppSource().getBytes(StandardCharsets.UTF_8));

        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        List<String> compileArgs = new ArrayList<>();
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "FileTestApp.java compilation failed with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("file-test-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FileTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("FileTestApp-src");
        // The SHARED helper, not a private copy. The copy that used to live at the
        // bottom of this file matched the add_library line by its full argument list,
        // so the moment the generator gained an assembly glob it silently stopped
        // matching -- and this test built a library, then failed to run an executable
        // that was never asked for.
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(
                cmakeLists, srcRoot.getFileName().toString());

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

        Path executable = buildDir.resolve("FileTestApp");
        // Running the app. If it exits with 0, logic passed.
        CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
    }

    private String fileTestAppSource() {
        return "import java.io.File;\n" +
               "public class FileTestApp {\n" +
               "    public static void main(String[] args) {\n" +
               "        try {\n" +
               "            // Construct string manually to avoid constant pool issues in test env\n" +
               "            char[] pathChars = new char[]{'t','e','s','t','f','i','l','e','.','t','x','t'};\n" +
               "            String path = new String(pathChars);\n" +
               "            File f = new File(path);\n" +
               "            if (f.exists()) {\n" +
               "                f.delete();\n" +
               "            }\n" +
               "            boolean created = f.createNewFile();\n" +
               "            if (!created) throw new RuntimeException(\"Create failed\");\n" +
               "            if (!f.exists()) throw new RuntimeException(\"Exists failed\");\n" +
               "            if (f.isDirectory()) throw new RuntimeException(\"IsDirectory failed\");\n" +
               "            if (!f.delete()) throw new RuntimeException(\"Delete failed\");\n" +
               "            if (f.exists()) throw new RuntimeException(\"Delete verification failed\");\n" +
               // File.list(): nothing exercised it before, so the native listing --
               // rewritten from a racy count-then-refill pair into one enumeration --
               // was compiled but never RUN by this suite.
               "            char[] dirChars = new char[]{'l','s','d','i','r'};\n" +
               "            File dir = new File(new String(dirChars));\n" +
               "            dir.mkdir();\n" +
               "            char[] aChars = new char[]{'l','s','d','i','r','/','a'};\n" +
               "            char[] bChars = new char[]{'l','s','d','i','r','/','b'};\n" +
               "            File fa = new File(new String(aChars));\n" +
               "            File fb = new File(new String(bChars));\n" +
               "            fa.createNewFile();\n" +
               "            fb.createNewFile();\n" +
               "            String[] names = dir.list();\n" +
               "            if (names == null) throw new RuntimeException(\"list returned null\");\n" +
               "            if (names.length != 2) throw new RuntimeException(\"list length\");\n" +
               // A trailing null is what the old two-pass version produced when the
               // second walk saw fewer entries than the first.
               "            if (names[0] == null || names[1] == null) throw new RuntimeException(\"null entry\");\n" +
               // The result must be a String[], not a String: allocArray installs the
               // class it is handed as the ARRAY's own class.
               "            Object asObject = names;\n" +
               "            if (!(asObject instanceof String[])) throw new RuntimeException(\"not a String[]\");\n" +
               "            boolean sawA = false; boolean sawB = false;\n" +
               "            for (int i = 0; i < names.length; i++) {\n" +
               "                if (names[i].equals(new String(new char[]{'a'}))) sawA = true;\n" +
               "                if (names[i].equals(new String(new char[]{'b'}))) sawB = true;\n" +
               "            }\n" +
               "            if (!sawA || !sawB) throw new RuntimeException(\"missing entry\");\n" +
               "            fa.delete(); fb.delete(); dir.delete();\n" +
               // A REGRESSION GUARD, not a proof of atomicity -- stated plainly
               // because the difference is easy to misread. This checks the
               // uncontended path: createNewFile on an existing file returns false
               // and leaves it intact. The check-then-act version it replaced passes
               // this too, since access() succeeds and it returns before ever
               // reaching the fopen that would truncate. Verified by running it
               // against the old code: 5/5 green.
               //
               // The actual defect needs a file to appear BETWEEN the check and the
               // open, which one thread cannot produce, so no single-threaded test
               // can demonstrate it. The correctness argument is structural instead:
               // one O_EXCL syscall where there were two operations, with the kernel
               // deciding who wins. What this guards is that the rewrite did not
               // break the ordinary path.
               "            char[] exChars = new char[]{'e','x','c','l','.','t','x','t'};\n" +
               "            File ex = new File(new String(exChars));\n" +
               "            if (ex.exists()) ex.delete();\n" +
               "            if (!ex.createNewFile()) throw new RuntimeException(\"first create\");\n" +
               "            java.io.FileOutputStream os = new java.io.FileOutputStream(ex);\n" +
               "            os.write(new byte[]{1,2,3,4});\n" +
               "            os.close();\n" +
               "            if (ex.createNewFile()) throw new RuntimeException(\"second create returned true\");\n" +
               "            if (ex.length() != 4) throw new RuntimeException(\"existing file was truncated\");\n" +
               "            ex.delete();\n" +
               // renameTo was never exercised, which is how an implementation that
               // renamed the destination onto itself survived. Content is checked
               // too: the aliased version reported success while moving nothing.
               "            char[] rnChars = new char[]{'r','n','-','s','r','c'};\n" +
               "            char[] rdChars = new char[]{'r','n','-','d','s','t','-','l','o','n','g','e','r'};\n" +
               "            File rsrc = new File(new String(rnChars));\n" +
               "            File rdst = new File(new String(rdChars));\n" +
               "            if (rsrc.exists()) rsrc.delete();\n" +
               "            if (rdst.exists()) rdst.delete();\n" +
               "            rsrc.createNewFile();\n" +
               "            java.io.FileOutputStream ros = new java.io.FileOutputStream(rsrc);\n" +
               "            ros.write(new byte[]{7,7,7});\n" +
               "            ros.close();\n" +
               "            if (!rsrc.renameTo(rdst)) throw new RuntimeException(\"rename returned false\");\n" +
               "            if (rsrc.exists()) throw new RuntimeException(\"source still present\");\n" +
               "            if (!rdst.exists()) throw new RuntimeException(\"destination missing\");\n" +
               "            if (rdst.length() != 3) throw new RuntimeException(\"content not moved\");\n" +
               "            rdst.delete();\n" +
               "        } catch (Exception e) {\n" +
               "            // e.printStackTrace(); // Can't print stack trace without constants\n" +
               "            System.exit(1);\n" +
               "        }\n" +
               "    }\n" +
               "}";
    }

}
