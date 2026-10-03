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

import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression coverage for the transpiler-level gaps closed for the new_gallery
 * drive: nested local functions, function-value {@code .call()} invocation,
 * function-typed field invocation, and instance method tear-offs.
 */
public class TranspilerRemainTest {

    @Test
    public void nestedLocalFunctionCalledAndTornOff() {
        // A local function called directly, and one passed by name (tear-off).
        String src =
                "class C {\n"
              + "  int compute(int seed) {\n"
              + "    int square(int x) {\n"
              + "      return x * x;\n"
              + "    }\n"
              + "    void log(int v) {\n"
              + "      print(v);\n"
              + "    }\n"
              + "    log(square(seed));\n"
              + "    return square(seed) + 1;\n"
              + "  }\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    @Test
    public void nestedLocalFunctionCapturesMutableLocal() {
        // The nested function mutates an outer local -> must be boxed like a lambda.
        String src =
                "class C {\n"
              + "  int run() {\n"
              + "    int total = 0;\n"
              + "    void add(int x) {\n"
              + "      total = total + x;\n"
              + "    }\n"
              + "    add(2);\n"
              + "    add(3);\n"
              + "    return total;\n"
              + "  }\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    @Test
    public void functionTypedFieldInvocation() {
        String src =
                "class Btn {\n"
              + "  final void Function() onTap;\n"
              + "  final void Function(String value) onChanged;\n"
              + "  Btn(this.onTap, this.onChanged);\n"
              + "  void fire() {\n"
              + "    onTap();\n"
              + "    onChanged('hi');\n"
              + "  }\n"
              + "}\n"
              + "class Host {\n"
              + "  final Btn b;\n"
              + "  Host(this.b);\n"
              + "  void go() {\n"
              + "    b.onTap();\n"
              + "    b.onChanged('x');\n"
              + "  }\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    @Test
    public void explicitCallAndNullAwareCall() {
        String src =
                "class C {\n"
              + "  void run(void Function()? cb, int Function(int) f) {\n"
              + "    cb?.call();\n"
              + "    final int y = f.call(3);\n"
              + "    print(y);\n"
              + "  }\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    @Test
    public void instanceMethodTearOff() {
        String src =
                "class Src {\n"
              + "  void handle(bool? v) {}\n"
              + "}\n"
              + "class Wire {\n"
              + "  final Src s;\n"
              + "  Wire(this.s);\n"
              + "  void Function(bool?) wireUp() {\n"
              + "    return s.handle;\n"
              + "  }\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }
}
