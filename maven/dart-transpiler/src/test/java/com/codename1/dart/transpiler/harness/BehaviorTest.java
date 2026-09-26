/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler.harness;

import com.codename1.dart.transpiler.api.GeneratedFile;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Behavioral execution tests: each dir under src/test/resources/behavior/
 * holds main.dart + expect.txt. The dart is transpiled, compiled with the
 * JDK 17 javac (JAVA17_HOME), executed, and stdout diffed against
 * expect.txt. Pins semantics: int math, double formatting, map ordering,
 * closure capture.
 *
 * <p>Skipped when JAVA17_HOME or the dart-runtime jar is unavailable.</p>
 */
public class BehaviorTest {

    private static final File BEHAVIOR_ROOT = new File("src/test/resources/behavior");

    @TestFactory
    public List<DynamicTest> behaviorCases() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        File[] cases = BEHAVIOR_ROOT.listFiles();
        if (cases != null) {
            for (File dir : cases) {
                if (dir.isDirectory()) {
                    tests.add(DynamicTest.dynamicTest(dir.getName(), () -> runCase(dir)));
                }
            }
        }
        assertTrue(!tests.isEmpty(), "no behavior cases found under " + BEHAVIOR_ROOT.getAbsolutePath());
        return tests;
    }

    private void runCase(File dir) throws Exception {
        File java17 = TestSupport.java17Home();
        File dartRuntime = TestSupport.findJar("codenameone-dart-runtime");
        File core = TestSupport.findJar("codenameone-core");
        assumeTrue(java17 != null, "JAVA17_HOME not set — skipping behavioral execution");
        assumeTrue(dartRuntime != null && core != null, "runtime jars not built — skipping");
        String rtClasspath = dartRuntime.getAbsolutePath() + File.pathSeparator + core.getAbsolutePath();
        // A case that touches a widget API needs the Flutter runtime to link against.
        // Cases that do not are unaffected by its presence, so this is unconditional
        // rather than another thing each case has to declare.
        File flutterRuntime = TestSupport.findJar("codenameone-flutter-runtime");
        if (flutterRuntime != null) {
            rtClasspath = rtClasspath + File.pathSeparator + flutterRuntime.getAbsolutePath();
        }

        List<File> stubEntries = new ArrayList<File>();
        if (flutterRuntime != null) {
            stubEntries.add(flutterRuntime);
        }
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"main.dart", TestSupport.read(new File(dir, "main.dart"))}
        }, stubEntries);
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());

        File work = Files.createTempDirectory("dart-behavior-" + dir.getName()).toFile();
        File srcDir = new File(work, "src/" + TestSupport.PKG.replace('.', '/'));
        List<String> javacArgs = new ArrayList<String>();
        for (GeneratedFile gf : r.files) {
            File out = new File(srcDir, gf.relativePath);
            TestSupport.write(out, gf.content);
            javacArgs.add(out.getAbsolutePath());
        }
        File runner = new File(work, "src/Runner.java");
        TestSupport.write(runner, "public class Runner {\n"
                + "    public static void main(String[] args) {\n"
                + "        " + TestSupport.PKG + ".MainLib.main$();\n"
                + "    }\n"
                + "}\n");
        javacArgs.add(runner.getAbsolutePath());

        File classes = new File(work, "classes");
        classes.mkdirs();
        List<String> javac = new ArrayList<String>();
        javac.add(new File(java17, "bin/javac").getAbsolutePath());
        javac.add("-cp");
        javac.add(rtClasspath);
        javac.add("-d");
        javac.add(classes.getAbsolutePath());
        javac.addAll(javacArgs);
        Object[] compileResult = TestSupport.run(javac, work);
        assertEquals(0, compileResult[0], "javac failed:\n" + compileResult[1]);

        List<String> java = new ArrayList<String>();
        java.add(new File(java17, "bin/java").getAbsolutePath());
        java.add("-cp");
        java.add(classes.getAbsolutePath() + File.pathSeparator + rtClasspath);
        java.add("Runner");
        Object[] runResult = TestSupport.run(java, work);
        assertEquals(0, runResult[0], "execution failed:\n" + runResult[1]);

        String expected = TestSupport.read(new File(dir, "expect.txt"));
        assertEquals(expected.trim().replace("\r\n", "\n"),
                ((String) runResult[1]).trim().replace("\r\n", "\n"),
                "behavioral output mismatch for " + dir.getName());
    }
}
