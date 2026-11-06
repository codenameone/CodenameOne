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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Every stub class mapped to a Java callback interface (an interface whose one abstract
 * method is {@code call}) supports value tear-offs: {@code FormFieldSetter<String> s = h.save}
 * becomes a {@code DartFnSigs} tear-off with Dart's {@code ==}. The guard enumerates the
 * interfaces from the runtime jar itself, so a new one without a {@code call} declaration,
 * or with a signature the emitter cannot spell, fails here rather than in an application.
 */
public class StubCallbackTearOffTest {

    private static final Pattern STUB = Pattern.compile(
            "@JavaName\\('([\\w.$]+)'\\)\\s*class\\s+(\\w+)(<\\s*(\\w+)\\s*>)?\\s*\\{(.*?)\\n\\}", Pattern.DOTALL);
    private static final Pattern CALL = Pattern.compile("external\\s+([^;]*?)\\s+call\\(([^)]*)\\);");

    @Test
    public void everyStubCallbackInterfaceSupportsValueTearOffs() throws Exception {
        File runtime = TestSupport.findJar("codenameone-flutter-runtime");
        File core = TestSupport.findJar("codenameone-core");
        TestSupport.prerequisite(runtime != null && core != null, "flutter runtime or core jar missing");
        assumeTrue(runtime != null && core != null);
        List<URL> urls = new ArrayList<URL>();
        urls.add(runtime.toURI().toURL());
        urls.add(core.toURI().toURL());
        for (String e : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(new File(e).toURI().toURL());
        }
        Map<String, String> found = new LinkedHashMap<String, String>();
        StringBuilder members = new StringBuilder();
        StringBuilder uses = new StringBuilder();
        List<String> missing = new ArrayList<String>();
        try (URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]), null);
             ZipFile zip = new ZipFile(runtime)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                if (!ze.getName().startsWith("META-INF/dart/") || !ze.getName().endsWith(".dart")) {
                    continue;
                }
                Matcher m = STUB.matcher(read(zip, ze));
                while (m.find()) {
                    Class<?> c;
                    try {
                        c = Class.forName(m.group(1), false, cl);
                    } catch (ClassNotFoundException | LinkageError ex) {
                        continue;
                    }
                    if (!c.isInterface() || !isCallSam(c)) {
                        continue;
                    }
                    String name = m.group(2);
                    Matcher call = CALL.matcher(m.group(5));
                    if (!call.find()) {
                        missing.add(name + " (" + ze.getName() + ") declares no call");
                        continue;
                    }
                    String tv = m.group(4);
                    String ret = subst(call.group(1), tv);
                    String params = subst(call.group(2), tv);
                    int k = found.size();
                    found.put(name, m.group(1));
                    members.append("  ").append(ret).append(" m").append(k).append('(').append(params).append(") ")
                            .append("void".equals(ret) ? "{}" : "=> throw 0;").append('\n');
                    uses.append("  ").append(name).append(tv == null ? "" : "<String>").append(" v").append(k)
                            .append(" = h.m").append(k).append(";\n  print(v").append(k).append(" == h.m").append(k)
                            .append(");\n");
                }
            }
        }
        assertTrue(missing.isEmpty(), "callback interfaces without a call declaration: " + missing);
        assertTrue(found.size() >= 9, "expected the known callback interfaces, found " + found.keySet());
        String src = "import 'package:flutter/material.dart';\nclass H {\n" + members + "}\nfinal H h = H();\n"
                + "void main() {\n" + uses + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                Collections.singletonList(runtime));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList() + "\n" + src);
        StringBuilder all = new StringBuilder();
        for (GeneratedFile gf : r.files) {
            all.append(gf.content);
        }
        for (Map.Entry<String, String> e : found.entrySet()) {
            assertTrue(Pattern.compile("extends dart\\.runtime\\.TearOff implements " + Pattern.quote(e.getValue())
                    + "\\b").matcher(all).find(), e.getKey() + " has no value tear-off\n" + src);
        }
    }

    @Test
    public void genericAndBuilderCallbackTearOffsRunWithDartEquality() throws Exception {
        File runtime = TestSupport.findJar("codenameone-flutter-runtime");
        File core = TestSupport.findJar("codenameone-core");
        File java17 = TestSupport.java17Home();
        TestSupport.prerequisite(runtime != null && core != null && java17 != null,
                "flutter runtime jar, core jar or JDK 17 missing");
        assumeTrue(runtime != null && core != null && java17 != null);
        String src =
                "import 'package:flutter/material.dart';\n"
              + "class H {\n"
              + "  String log = '';\n"
              + "  void save(String? v) { log += 's$v '; }\n"
              + "  String? check(String? v) => v == null ? 'none' : null;\n"
              + "  Widget closed(BuildContext c, VoidCallback open) => const SizedBox();\n"
              + "  List<CustomPainterSemantics> sem(Size s) => <CustomPainterSemantics>[];\n"
              + "  void down(TapDownDetails d) {}\n"
              + "}\n"
              + "final H h = H();\n"
              + "final H g = H();\n"
              + "void main() {\n"
              + "  FormFieldSetter<String> a = h.save;\n"
              + "  FormFieldSetter<String> b = h.save;\n"
              + "  print('${a == b}');\n"
              + "  a('x');\n"
              + "  b('y');\n"
              + "  print(h.log);\n"
              + "  FormFieldValidator<String> v = h.check;\n"
              + "  print('${v(null)} ${v('z')}');\n"
              + "  CloseContainerBuilder c1 = h.closed;\n"
              + "  CloseContainerBuilder c2 = h.closed;\n"
              + "  SemanticsBuilderCallback s1 = h.sem;\n"
              + "  SemanticsBuilderCallback s2 = h.sem;\n"
              + "  print('${c1 == c2} ${s1 == s2}');\n"
              + "  final l = <GestureTapDownCallback>[h.down];\n"
              + "  GestureTapDownCallback other = g.down;\n"
              + "  GestureTapDownCallback same = h.down;\n"
              + "  print('${l.remove(other)} ${l.remove(same)} ${l.length}');\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                Collections.singletonList(runtime));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        File work = Files.createTempDirectory("dart-stub-callback-tearoff").toFile();
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
        assertEquals(0, run[0], "run failed:\n" + run[1]);
        // What Dart prints for this program: tear-offs of one method from one receiver are
        // equal whatever interface holds them, and a different receiver's is not.
        assertEquals("true\nsx sy \nnone null\ntrue true\nfalse true 0",
                String.valueOf(run[1]).trim().replace("\r", ""), all.toString());
    }

    private static boolean isCallSam(Class<?> c) {
        int n = 0;
        boolean call = false;
        try {
            for (Method m : c.getMethods()) {
                if (Modifier.isAbstract(m.getModifiers())) {
                    n++;
                    call |= "call".equals(m.getName());
                }
            }
        } catch (LinkageError e) {
            throw new AssertionError("cannot inspect " + c.getName(), e);
        }
        return n == 1 && call;
    }

    private static String subst(String s, String typeVar) {
        return typeVar == null ? s : s.replaceAll("\\b" + typeVar + "\\b", "String");
    }

    private static String read(ZipFile zip, ZipEntry ze) throws java.io.IOException {
        try (InputStream in = zip.getInputStream(ze)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) {
                out.write(b, 0, n);
            }
            return out.toString("UTF-8");
        }
    }
}
