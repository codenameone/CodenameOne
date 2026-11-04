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

/// E0143: an untyped member whose inherited signatures have no combined member signature.
/// Dart rejects these programs ("Can't infer types for 'measure' as the overridden members
/// don't have a combined signature"); the transpiler used to take the first candidate.
public class CombinedSignatureDiagnosticTest {

    private static long count(TestSupport.Result r, String code) {
        long n = 0;
        for (Diagnostic d : r.diags.asList()) {
            if (d.code.equals(code)) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void conflictingInheritedSignaturesAreDiagnosed() {
        String src =
                "abstract class A { int measure(int x); }\n"
              + "abstract class B { String measure(int x); }\n"
              + "class C implements A, B {\n"
              + "  measure(x) => throw 0;\n"
              + "}\n"
              + "abstract class G { int get v; }\n"
              + "abstract class H { String get v; }\n"
              + "class I implements G, H { final v = throw 0; }\n"
              + "void main() { print(1); }\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertEquals(2, count(r, "E0143"), "expected E0143 at measure and at v: " + r.diags.asList());
    }

    @Test
    public void aCombinedSignatureIsNotDiagnosed() {
        // int measure(num) is a subtype of num measure(int): it is the combined signature.
        String src =
                "abstract class A { num measure(int x); }\n"
              + "abstract class B { int measure(num x); }\n"
              + "class C implements A, B {\n"
              + "  measure(x) => x.toInt();\n"
              + "}\n"
              + "void main() { print(C().measure(2)); }\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertEquals(0, count(r, "E0143"), r.diags.asList().toString());
    }
}
