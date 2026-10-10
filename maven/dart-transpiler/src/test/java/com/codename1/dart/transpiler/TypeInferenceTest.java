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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Type-inference behaviours exercised by the real new_gallery app: flow-based
 * {@code is}-promotion, ternary least-upper-bound, and cascade-diagnostic
 * suppression on a receiver whose type already fell to {@code dynamic} from a
 * reported root cause.
 */
public class TypeInferenceTest {

    private static long count(TestSupport.Result r, String code) {
        long n = 0;
        for (Diagnostic d : r.diags.asList()) {
            if (d.code.equals(code)) {
                n++;
            }
        }
        return n;
    }

    private static String generated(TestSupport.Result r, String simpleName) {
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            if (f.relativePath.endsWith(simpleName + ".java")) {
                return f.content;
            }
        }
        StringBuilder paths = new StringBuilder();
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            paths.append(f.relativePath).append(' ');
        }
        throw new AssertionError("no " + simpleName + " in: " + paths);
    }

    /** `other is T && other.member` must promote `other` to T (with a cast) — no error. */
    @Test
    public void isPromotionInAndChain() {
        String src =
                "class Point {\n" +
                "  final int x;\n" +
                "  final int y;\n" +
                "  Point(this.x, this.y);\n" +
                "  bool same(Object other) => other is Point && other.x == x && other.y == y;\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"point.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        String out = generated(r, "Point");
        assertTrue(out.contains("((Point) other)"), out);
    }

    /** `if (x is T) { x.member }` promotes x inside the then-branch. */
    @Test
    public void isPromotionInIfThen() {
        String src =
                "class Box {\n" +
                "  final int v;\n" +
                "  Box(this.v);\n" +
                "  int read(Object o) {\n" +
                "    if (o is Box) {\n" +
                "      return o.v;\n" +
                "    }\n" +
                "    return 0;\n" +
                "  }\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"box.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        String out = generated(r, "Box");
        assertTrue(out.contains("((Box) o)"), out);
    }

    /**
     * A chain rooted at an unresolved identifier (`Missing.a.b.c`) must report the
     * single root cause once (E0129) and NOT a cascade of member errors down the chain.
     */
    @Test
    public void unresolvedRootDoesNotCascade() {
        String src =
                "class User {\n" +
                "  void go() {\n" +
                "    print(Missing.a.b.c);\n" +
                "  }\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"user.dart", src}});
        assertEquals(1, count(r, "E0129"), "root: " + r.diags.asList());
        assertEquals(0, count(r, "E0132"), "no cascade: " + r.diags.asList());
    }

    /** An `assert(...)` statement in a method body is dropped, not flagged unsupported. */
    @Test
    public void assertStatementDropped() {
        String src =
                "class Guard {\n" +
                "  int half(int n) {\n" +
                "    assert(n > 0, 'must be positive');\n" +
                "    return n ~/ 2;\n" +
                "  }\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"guard.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        assertEquals(0, count(r, "E0115"), "assert should be dropped: " + r.diags.asList());
        String out = generated(r, "Guard");
        assertFalse(out.contains("assert("), out);
    }

    /** A ternary over two related types resolves to their common ancestor, not dynamic. */
    @Test
    public void ternaryLeastUpperBound() {
        String src =
                "class Animal { String noise() => 'x'; }\n" +
                "class Cat extends Animal {}\n" +
                "class Dog extends Animal {}\n" +
                "class Zoo {\n" +
                "  String pick(bool b) {\n" +
                "    var a = b ? Cat() : Dog();\n" +
                "    return a.noise();\n" +
                "  }\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"zoo.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }
}
