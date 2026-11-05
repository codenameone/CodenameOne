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

import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// E0144: a class whose stub superclass implements an interface member with a primitive
/// Java return (Color's `int get value`, a `long`) while the program interface it also
/// implements declares that member untyped (an `Object` return). Java cannot express the
/// pair; the transpiler reports it against the Dart class instead of leaving javac to fail
/// on generated code.
public class StubInterfaceReturnConflictTest {

    private static java.util.List<Diagnostic> withCode(TestSupport.Result r, String code) {
        java.util.List<Diagnostic> out = new java.util.ArrayList<Diagnostic>();
        for (Diagnostic d : r.diags.asList()) {
            if (d.code.equals(code)) {
                out.add(d);
            }
        }
        return out;
    }

    @Test
    public void primitiveStubGetterAgainstUntypedInterfaceGetterIsDiagnosed() {
        String src =
                "import 'package:flutter/material.dart';\n"
              + "class Base { get value => 1; }\n"
              + "class Mixed extends Color implements Base { Mixed() : super(3); }\n"
              + "void main() { Base b = Mixed(); print(b.value); }\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        java.util.List<Diagnostic> found = withCode(r, "E0144");
        assertEquals(1, found.size(), "expected one E0144: " + r.diags.asList());
        String msg = found.get(0).message;
        assertTrue(msg.contains("Mixed") && msg.contains("Color") && msg.contains("Base")
                && msg.contains("'value'") && msg.contains("int get value"), msg);
    }

    @Test
    public void typedInterfaceGetterOrOverrideIsAccepted() {
        String src =
                "import 'package:flutter/material.dart';\n"
              + "class Base { int get value => 1; }\n"
              + "class Typed extends Color implements Base { Typed() : super(3); }\n"
              + "class Untyped { get value => 1; }\n"
              + "class Overrides extends Color implements Untyped {\n"
              + "  Overrides() : super(3);\n"
              + "  get value => 4;\n"
              + "}\n"
              + "void main() { print(Typed().value); print(Overrides().value); }\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertEquals(0, withCode(r, "E0144").size(), "unexpected E0144: " + r.diags.asList());
    }
}
