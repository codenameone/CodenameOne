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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * An instance-method tear-off held as a stub's own callback interface
 * ({@code GestureDragStartCallback}, a Java interface rather than a Funcs SAM) is a value
 * with Dart's {@code ==}: two tear-offs of one method from one receiver are equal, so a
 * listener list finds and removes it. A receiver that is a call is evaluated once.
 * Dart prints {@code true / true 0 / made 1} for the program below (Flutter's typedefs).
 */
public class CallbackInterfaceTearOffTest {

    @Test
    public void callbackInterfaceTearOffsCompareByReceiverAndMethod() throws Exception {
        File runtime = TestSupport.findJar("codenameone-flutter-runtime");
        File java17 = TestSupport.java17Home();
        TestSupport.prerequisite(runtime != null && java17 != null, "flutter runtime jar or JDK 17 missing");
        assumeTrue(runtime != null && java17 != null);
        String src =
                "import 'package:flutter/material.dart';\n"
              + "class Pad {\n"
              + "  int n = 0;\n"
              + "  void start(DragStartDetails d) { n++; }\n"
              + "}\n"
              + "final Pad pad = Pad();\n"
              + "int made = 0;\n"
              + "Pad find() { made++; return pad; }\n"
              + "void main() {\n"
              + "  GestureDragStartCallback a = pad.start;\n"
              + "  GestureDragStartCallback b = find().start;\n"
              + "  print('${a == b}');\n"
              + "  final l = <GestureDragStartCallback>[a];\n"
              + "  GestureDragStartCallback c = pad.start;\n"
              + "  print('${l.remove(c)} ${l.length}');\n"
              + "  print('made $made');\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                Collections.singletonList(runtime));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        File work = Files.createTempDirectory("dart-callback-tearoff").toFile();
        File srcDir = new File(work, "src/" + TestSupport.PKG.replace('.', '/'));
        List<String> javac = new ArrayList<String>();
        javac.add(new File(java17, "bin/javac").getAbsolutePath());
        String cp = runtime.getAbsolutePath() + File.pathSeparator + System.getProperty("java.class.path");
        javac.add("-cp");
        javac.add(cp);
        javac.add("-encoding");
        javac.add("UTF-8");
        File classes = new File(work, "classes");
        classes.mkdirs();
        javac.add("-d");
        javac.add(classes.getAbsolutePath());
        StringBuilder all = new StringBuilder();
        for (GeneratedFile gf : r.files) {
            File out = new File(srcDir, gf.relativePath);
            TestSupport.write(out, gf.content);
            javac.add(out.getAbsolutePath());
            all.append(gf.content);
        }
        org.junit.jupiter.api.Assertions.assertTrue(all.indexOf("implements com.codename1.flutter.gestures"
                + ".GestureDragStartCallback") >= 0, all.toString());
        File runner = new File(work, "src/Runner.java");
        TestSupport.write(runner, "public class Runner {\n    public static void main(String[] a) {\n        "
                + TestSupport.PKG + ".MainLib.main$();\n    }\n}\n");
        javac.add(runner.getAbsolutePath());
        Object[] c = TestSupport.run(javac, work);
        assertEquals(0, c[0], "javac failed:\n" + c[1] + "\n" + all);
        List<String> java = new ArrayList<String>();
        java.add(new File(java17, "bin/java").getAbsolutePath());
        java.add("-cp");
        java.add(classes.getAbsolutePath() + File.pathSeparator + cp);
        java.add("Runner");
        Object[] run = TestSupport.run(java, work);
        assertEquals(0, run[0], "run failed:\n" + run[1]);
        assertEquals("true\ntrue 0\nmade 1", String.valueOf(run[1]).trim().replace("\r", ""), all.toString());
    }
}
