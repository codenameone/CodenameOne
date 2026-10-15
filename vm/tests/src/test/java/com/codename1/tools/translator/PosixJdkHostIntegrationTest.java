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

/**
 * Runs {@code com.codename1.impl.PosixJdkHost} -- the JdkHost the self-hosted
 * translator, the backend and the VM tests install -- translated and natively
 * built, over a real file system.
 *
 * It also pins the defaults: before anything is installed, {@code JdkHost.get()}
 * must answer the plain-Java host, which names no native, so an application
 * target that never installs one cannot reach the file natives through it.
 */
public class PosixJdkHostIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    public void posixHostServesTheFileSystem(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("posix-host-sources");
        Path classesDir = Files.createTempDirectory("posix-host-classes");
        Path javaFile = sourceDir.resolve("PosixHostApp.java");
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
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "PosixHostApp.java compilation failed with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("posix-host-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "PosixHostApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        Path srcRoot = distDir.resolve("PosixHostApp-src");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(
                cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCmakeBuild(buildDir, distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("PosixHostApp"));
        String output = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("POSIX-HOST-OK"),
                "PosixJdkHost fixture failed, output was:\n" + output);
    }

    private static String appSource() {
        return "import com.codename1.impl.JdkHost;\n"
                + "import com.codename1.impl.PosixJdkHost;\n"
                + "import java.io.File;\n"
                + "import java.io.InputStream;\n"
                + "import java.io.OutputStream;\n"
                + "public class PosixHostApp {\n"
                + "    static void check(boolean ok, String what) {\n"
                + "        if (!ok) { System.out.println(\"FAIL \" + what); System.exit(1); }\n"
                + "    }\n"
                + "    static void write(JdkHost h, String path, boolean append, byte[] data) throws Exception {\n"
                + "        OutputStream o = h.fileWrite(path, append);\n"
                + "        o.write(data);\n"
                + "        o.close();\n"
                + "    }\n"
                + "    public static void main(String[] args) throws Exception {\n"
                // Nothing installed: the plain-Java defaults, which touch no native.
                + "        JdkHost d = JdkHost.get();\n"
                + "        check(d != null && !(d instanceof PosixJdkHost), \"defaults\");\n"
                + "        check(d.fileStat(\"anything\") == null, \"default stat\");\n"
                + "        check(!d.fileMkdir(\"anything\"), \"default mkdir\");\n"
                + "        boolean threw = false;\n"
                + "        try { d.fileRead(\"anything\"); } catch (java.io.IOException e) { threw = true; }\n"
                + "        check(threw, \"default read throws\");\n"
                + "        d.setPreference(\"k\", \"v\");\n"
                + "        check(\"v\".equals(d.preference(\"k\")), \"default preference\");\n"
                + "        d.deletePreference(\"k\");\n"
                + "        check(d.preference(\"k\") == null, \"default preference deleted\");\n"
                + "        check(d.providers(\"svc\").length == 0, \"no registry\");\n"
                + "\n"
                + "        JdkHost.install(new PosixJdkHost());\n"
                + "        JdkHost h = JdkHost.get();\n"
                + "        check(h instanceof PosixJdkHost, \"installed\");\n"
                + "        String home = h.appHome();\n"
                + "        check(home != null && home.length() > 0 && !home.endsWith(\".\"), \"home \" + home);\n"
                + "        check(h.fileStat(home) != null && h.fileStat(home).isDirectory(), \"home is a directory\");\n"
                + "        String dir = home + \"/posix-host-dir\";\n"
                + "        String a = dir + \"/a.txt\";\n"
                + "        String b = dir + \"/b.txt\";\n"
                + "        h.fileDelete(a); h.fileDelete(b); h.fileDelete(dir);\n"
                + "        check(h.fileStat(dir) == null, \"clean start\");\n"
                + "        check(h.fileMkdir(dir), \"mkdir\");\n"
                + "        check(!h.fileMkdir(dir), \"mkdir of an existing directory\");\n"
                + "        write(h, a, false, new byte[] {1, 2, 3});\n"
                + "        write(h, a, true, new byte[] {4});\n"
                + "        JdkHost.FileStat st = h.fileStat(a);\n"
                + "        check(st != null && !st.isDirectory() && st.length() == 4, \"stat after append\");\n"
                + "        check(st.lastModified() > 0, \"modified time\");\n"
                + "        InputStream in = h.fileRead(a);\n"
                + "        byte[] buf = new byte[8];\n"
                + "        int n = in.read(buf);\n"
                + "        in.close();\n"
                + "        check(n == 4 && buf[0] == 1 && buf[3] == 4, \"read back\");\n"
                + "        write(h, a, false, new byte[] {9});\n"
                + "        check(h.fileStat(a).length() == 1, \"overwrite truncates\");\n"
                + "        String[] names = h.fileList(dir);\n"
                + "        check(names != null && names.length == 1 && \"a.txt\".equals(names[0]), \"list\");\n"
                + "        check(h.fileList(a) == null, \"list of a file\");\n"
                + "        check(h.fileRename(a, b), \"rename\");\n"
                + "        check(h.fileStat(a) == null && h.fileStat(b) != null, \"renamed\");\n"
                + "        write(h, a, false, new byte[] {5});\n"
                + "        check(!h.fileRename(a, b), \"rename onto an existing file\");\n"
                + "        check(h.fileStat(b).length() == 1, \"destination untouched\");\n"
                + "        check(!h.fileDelete(dir), \"delete of a non-empty directory\");\n"
                + "        check(h.fileDelete(a) && h.fileDelete(b) && h.fileDelete(dir), \"delete\");\n"
                + "        check(!h.fileDelete(dir), \"delete of nothing\");\n"
                + "        threw = false;\n"
                + "        try { h.fileRead(a); } catch (java.io.IOException e) { threw = true; }\n"
                + "        check(threw, \"read of a missing file throws\");\n"
                // 0 is "unknown": the volume natives answer it unless the build
                // defines CN1_ENABLE_FILE_SYSTEM_STATS.
                + "        check(h.fileTotalSpace(home) >= 0 && h.fileUsableSpace(home) >= 0, \"space\");\n"
                + "        check(h.cachesDir() != null, \"caches dir\");\n"
                // Only where the source exists; the Windows hosts have none.
                + "        if (new File(\"/dev/urandom\").exists()) {\n"
                + "            byte[] r = new byte[64];\n"
                + "            h.randomBytes(r);\n"
                + "            boolean nonZero = false;\n"
                + "            for (int i = 0; i < r.length; i++) { if (r[i] != 0) { nonZero = true; } }\n"
                + "            check(nonZero, \"random bytes\");\n"
                + "        }\n"
                + "        JdkHost.install(null);\n"
                + "        check(!(JdkHost.get() instanceof PosixJdkHost), \"uninstalled\");\n"
                + "        System.out.println(\"POSIX-HOST-OK\");\n"
                + "    }\n"
                + "}\n";
    }
}
