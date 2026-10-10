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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dart:collection mixins are registered as transpiler built-in stubs, so a class that applies
 * {@code with IterableMixin<T>} resolves the mixin (no E0402) and inherited mixin members resolve.
 */
public class IterableMixinTest {

    @Test
    public void iterableMixinIsRecognized() {
        String src =
                "class BoardPoint {}\n"
                        + "class Board extends Object with IterableMixin<BoardPoint> {\n"
                        + "  final List<BoardPoint> _points = [];\n"
                        + "  @override\n"
                        + "  Iterator<BoardPoint> get iterator => _points.iterator;\n"
                        + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"board.dart", src}});
        String diagnostics = r.diags.asList().toString();
        assertFalse(diagnostics.contains("E0402"), "unexpected unknown-mixin error: " + diagnostics);
    }

    @Test
    public void inheritedMixinMemberResolvesInBareCall() {
        // forEach is contributed by IterableMixin as an inherited default; a bare call must resolve.
        String src =
                "class BoardPoint {}\n"
                        + "class Board extends Object with IterableMixin<BoardPoint> {\n"
                        + "  final List<BoardPoint> _points = [];\n"
                        + "  @override\n"
                        + "  Iterator<BoardPoint> get iterator => _points.iterator;\n"
                        + "  void dump() {\n"
                        + "    forEach((p) => print(p));\n"
                        + "  }\n"
                        + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"board.dart", src}});
        String diagnostics = r.diags.asList().toString();
        assertFalse(diagnostics.contains("E0402"), "unknown-mixin error: " + diagnostics);
        assertTrue(!diagnostics.contains("E0137") || !diagnostics.contains("forEach"),
                "forEach should resolve via the mixin: " + diagnostics);
    }

    @Test
    public void inheritedMixinMemberResolvesOnReceiver() {
        // board.forEach(...) — a call on a typed receiver must resolve through the applied mixin.
        String src =
                "class BoardPoint {}\n"
                        + "class Board extends Object with IterableMixin<BoardPoint> {\n"
                        + "  final List<BoardPoint> _points = [];\n"
                        + "  @override\n"
                        + "  Iterator<BoardPoint> get iterator => _points.iterator;\n"
                        + "}\n"
                        + "void paint(Board board) {\n"
                        + "  board.forEach((p) => print(p));\n"
                        + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"board.dart", src}});
        String diagnostics = r.diags.asList().toString();
        assertTrue(!diagnostics.contains("E0137") || !diagnostics.contains("forEach"),
                "forEach on a Board receiver should resolve via the mixin: " + diagnostics);
    }
}
