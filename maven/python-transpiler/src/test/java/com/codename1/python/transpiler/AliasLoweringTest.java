/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.codegen.AliasLowering;
import com.codename1.python.transpiler.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// [AliasLowering]: a literal alias becomes a property, and the computed
/// methods of `Alias` go only when no alias call is left.
class AliasLoweringTest {

    private static final String BASE = "class Descriptor:\n    pass\nclass Alias(Descriptor):\n"
            + "    def __init__(self, alias):\n        self.alias = alias\n"
            + "    def __set__(self, instance, value):\n        setattr(instance, self.alias, value)\n"
            + "    def __get__(self, instance, cls):\n        return getattr(instance, self.alias)\n";

    private static List<String> members(Node.Module m, String cls) {
        List<String> out = new ArrayList<String>();
        for (Node.Stmt s : m.body) {
            if (s instanceof Node.ClassDef && cls.equals(((Node.ClassDef) s).name)) {
                for (Node.Stmt b : ((Node.ClassDef) s).body) {
                    out.add(b.getClass().getSimpleName()
                            + (b instanceof Node.FunctionDef ? ":" + ((Node.FunctionDef) b).name : ""));
                }
            }
        }
        return out;
    }

    private static Map<String, Node.Module> program(String user) {
        Map<String, Node.Module> t = new LinkedHashMap<String, Node.Module>();
        t.put("openpyxl/descriptors/base.py", Parser.parse(BASE, "openpyxl/descriptors/base.py"));
        t.put("openpyxl/x.py", Parser.parse(user, "openpyxl/x.py"));
        return t;
    }

    @Test
    void literalAliasesBecomePropertiesAndMethodsGo() {
        Map<String, Node.Module> t = program("from openpyxl.descriptors import Alias\n"
                + "class C:\n    u = Alias('underline')\n");
        AliasLowering.rewrite(t);
        assertEquals("[FunctionDef:u, FunctionDef:u]", members(t.get("openpyxl/x.py"), "C").toString());
        assertEquals("[FunctionDef:__init__]", members(t.get("openpyxl/descriptors/base.py"), "Alias").toString());
    }

    @Test
    void computedAliasKeepsMethods() {
        Map<String, Node.Module> t = program("from openpyxl.descriptors import Alias\n"
                + "N = 'underline'\nclass C:\n    u = Alias(N)\n");
        AliasLowering.rewrite(t);
        assertEquals("[Assign]", members(t.get("openpyxl/x.py"), "C").toString());
        assertEquals(3, members(t.get("openpyxl/descriptors/base.py"), "Alias").size());
    }
}
