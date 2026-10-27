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

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.api.PythonTranspiler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a translated program cannot do fails the build at the line that
/// does it, never silently or at run time on a device.
class UnsupportedTest {
    /// Each case: the program, the line the error must name, and a fragment
    /// of the reason.
    private static final String[][] CASES = {
        {"x = 1\nprint(eval('x + 1'))\n", "2", "eval()"},
        {"exec('print(1)')\n", "1", "exec()"},
        {"def f():\n    return compile('1', 'f', 'eval')\n", "2", "compile()"},
        {"m = __import__('os')\n", "1", "__import__()"},
        {"run = eval\n", "1", "eval()"},
        {"import sys\n\ndef t(*a):\n    pass\n\nsys.settrace(t)\n", "6", "sys.settrace"},
        {"import sys\nprint(sys._getframe().f_lineno)\n", "2", "sys._getframe"},
        {"def f():\n    x = 1\n    return locals()\n", "3", "locals()"},
    };

    @TestFactory
    List<DynamicTest> rejected() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final String[] c : CASES) {
            tests.add(DynamicTest.dynamicTest(c[2] + " at line " + c[1], () -> {
                Map<String, String> sources = new TreeMap<String, String>();
                sources.put("main.py", c[0]);
                PythonSyntaxError e = assertThrows(PythonSyntaxError.class,
                        () -> PythonTranspiler.transpile(sources, "main"));
                assertEquals("main.py", e.file);
                assertEquals(Integer.parseInt(c[1]), e.line);
                assertTrue(e.reason.contains(c[2]), e.getMessage());
            }));
        }
        return tests;
    }

    /// A name the program binds itself is the program's own, not the builtin.
    @TestFactory
    List<DynamicTest> shadowed() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        String[] programs = {
            "def eval(s):\n    return s\nprint(eval('x'))\n",
            "import re\np = re.compile('a+')\n",
        };
        for (final String p : programs) {
            tests.add(DynamicTest.dynamicTest(p.substring(0, p.indexOf('\n')), () -> {
                Map<String, String> sources = new TreeMap<String, String>();
                sources.put("main.py", p);
                assertTrue(!PythonTranspiler.transpile(sources, "main").isEmpty());
            }));
        }
        return tests;
    }
}
