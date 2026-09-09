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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compiles the transpiled counter app with the real JDK 17 javac against the
 * dart-runtime and flutter-runtime jars — proves the emitter's output links
 * against the hand-written runtime API (mirrors svg-transcoder's
 * CompileGeneratedSourceTest).
 */
public class CompileGeneratedTest {

    @Test
    public void m2DemoOutputCompilesAgainstRuntimes() throws Exception {
        compileFixture("src/test/resources/fixtures/m2_demo.dart");
    }

    @Test
    public void counterAppOutputCompilesAgainstRuntimes() throws Exception {
        compileFixture("src/test/resources/fixtures/counter_main.dart");
    }

    private void compileFixture(String fixture) throws Exception {
        File java17 = TestSupport.java17Home();
        File dartRuntime = TestSupport.findJar("codenameone-dart-runtime");
        File flutterRuntime = TestSupport.findJar("codenameone-flutter-runtime");
        File core = TestSupport.findJar("codenameone-core");
        assumeTrue(java17 != null, "JAVA17_HOME not set — skipping compile check");
        assumeTrue(dartRuntime != null && flutterRuntime != null && core != null,
                "runtime jars not built — skipping compile check");

        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"main.dart", TestSupport.read(new File(fixture))}
        });
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());

        File work = Files.createTempDirectory("dart-compile-check").toFile();
        File srcDir = new File(work, TestSupport.PKG.replace('.', '/'));
        List<String> args = new ArrayList<String>();
        for (GeneratedFile gf : r.files) {
            File out = new File(srcDir, gf.relativePath);
            TestSupport.write(out, gf.content);
            args.add(out.getAbsolutePath());
        }
        File classes = new File(work, "classes");
        classes.mkdirs();
        List<String> javac = new ArrayList<String>();
        javac.add(new File(java17, "bin/javac").getAbsolutePath());
        javac.add("-cp");
        javac.add(dartRuntime.getAbsolutePath() + File.pathSeparator
                + flutterRuntime.getAbsolutePath() + File.pathSeparator + core.getAbsolutePath());
        javac.add("-d");
        javac.add(classes.getAbsolutePath());
        javac.addAll(args);
        Object[] result = TestSupport.run(javac, work);
        assertEquals(0, result[0], "generated counter app failed to compile:\n" + result[1]);
    }
}
