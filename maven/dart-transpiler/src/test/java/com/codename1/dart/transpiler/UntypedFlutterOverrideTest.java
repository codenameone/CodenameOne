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

/// Dart's override inference against the Flutter stubs: a widget written with no types on
/// its overrides (`build(context)`, `createState()`, `didUpdateWidget(old)`, `initState()`,
/// `dispose()`) takes them from the framework members it overrides, so its Java methods
/// override the runtime's (javac rejects the class otherwise: State.build is abstract).
public class UntypedFlutterOverrideTest {

    @Test
    public void untypedWidgetOverridesCompileAndRun() throws Exception {
        File runtime = TestSupport.findJar("codenameone-flutter-runtime");
        File core = TestSupport.findJar("codenameone-core");
        File java17 = TestSupport.java17Home();
        TestSupport.prerequisite(runtime != null && core != null && java17 != null,
                "flutter runtime jar, core jar or JDK 17 missing");
        assumeTrue(runtime != null && core != null && java17 != null);
        String src =
                "import 'package:flutter/material.dart';\n"
              + "class Tally extends StatefulWidget {\n"
              + "  final int start;\n"
              + "  const Tally({super.key, this.start = 0});\n"
              + "  @override\n"
              + "  createState() => TallyState();\n"
              + "}\n"
              + "class TallyState extends State<Tally> {\n"
              + "  var count = 0;\n"
              + "  final log = <String>[];\n"
              + "  @override\n"
              + "  initState() {\n"
              + "    super.initState();\n"
              + "    log.add('init');\n"
              + "  }\n"
              + "  @override\n"
              + "  didUpdateWidget(old) {\n"
              + "    super.didUpdateWidget(old);\n"
              + "    count = old.start + 1;\n"
              + "    log.add('update ${old.start}');\n"
              + "  }\n"
              + "  @override\n"
              + "  build(context) => Text('count $count');\n"
              + "  @override\n"
              + "  dispose() {\n"
              + "    log.add('dispose');\n"
              + "    super.dispose();\n"
              + "  }\n"
              + "}\n"
              + "class Label extends StatelessWidget {\n"
              + "  final text;\n"
              + "  const Label(this.text, {super.key});\n"
              + "  @override\n"
              + "  build(context) => Text(text);\n"
              + "}\n"
              + "void main() {\n"
              + "  final w = Tally(start: 4);\n"
              + "  final s = w.createState();\n"
              + "  final TallyState t = s as TallyState;\n"
              + "  t.initState();\n"
              + "  t.didUpdateWidget(Tally(start: 9));\n"
              + "  t.dispose();\n"
              + "  print(t.log.join(','));\n"
              + "  print(t.count);\n"
              + "  print(Label('x').text);\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                Collections.singletonList(runtime));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        File work = Files.createTempDirectory("dart-untyped-flutter-override").toFile();
        File srcDir = new File(work, "src/" + TestSupport.PKG.replace('.', '/'));
        List<String> javac = new ArrayList<String>();
        javac.add(new File(java17, "bin/javac").getAbsolutePath());
        String cp = runtime.getAbsolutePath() + File.pathSeparator + core.getAbsolutePath() + File.pathSeparator
                + System.getProperty("java.class.path");
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
        assertEquals(0, run[0], "run failed:\n" + run[1] + "\n" + all);
        assertEquals("init,update 9,dispose\n10\nx",
                String.valueOf(run[1]).trim().replace("\r", ""), all.toString());
    }
}
