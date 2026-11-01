/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.dart.transpiler.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DartLineTableTest {
    @Test
    void pairsJavaLinesWithDartLinesAndAddsNoLineAbove() {
        String java = "class A {\n"
                + "    void f() {\n"
                + "        //@dart:3 lib/a.dart\n"
                + "        x();\n"
                + "        y();\n"
                + "        //@dart:7 lib/p.dart\n"
                + "        z();\n"
                + "    }\n"
                + "}\n";
        String out = DartLineTable.apply(java, "lib/a.dart");
        assertEquals(java.substring(0, java.lastIndexOf('}')),
                out.substring(0, java.lastIndexOf('}')), "no line above the table moves");
        assertTrue(out.contains("static final String $cn1DartLines = \"lib/a.dart\\nlib/p.dart\\n4:3,7:7:1\";"), out);
    }

    @Test
    void longTableIsSplitAndUnmarkedFileIsUnchanged() {
        StringBuilder java = new StringBuilder("class A {\n");
        for (int i = 1; i < 6000; i++) {
            java.append("//@dart:").append(i).append(" a.dart\nx();\n");
        }
        java.append("}\n");
        String out = DartLineTable.apply(java.toString(), "a.dart");
        assertTrue(out.contains("$cn1DartLines1 = "), "split past one constant");
        String plain = "class B {\n}\n";
        assertEquals(plain, DartLineTable.apply(plain, "b.dart"));
    }
}
