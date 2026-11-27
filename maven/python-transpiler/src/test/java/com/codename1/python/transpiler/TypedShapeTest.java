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

import com.codename1.python.transpiler.api.PythonTranspiler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The shape of the typed code for patterns the hero apps were measured on.
/// Each of these is an optimization whose loss changes no output: the
/// program still runs, through the generic path, only slower. The
/// conformance programs check what the code answers; this checks that the
/// fast form is still the one emitted.
class TypedShapeTest {

    private static String main(String program) throws Exception {
        Map<String, String> sources = new TreeMap<String, String>();
        sources.put("main.py", program);
        List<File> classPath = new ArrayList<File>();
        classPath.add(new File(location(com.codename1.io.Util.class)));
        classPath.add(new File(location(com.codename1.python.runtime.Py.class)));
        Map<String, String> out = PythonTranspiler.transpile(sources, "main", classPath,
                new TreeMap<String, String>());
        for (Map.Entry<String, String> e : out.entrySet()) {
            if (e.getKey().endsWith("/py_main.java")) {
                return e.getValue();
            }
        }
        throw new AssertionError("no py_main.java among " + out.keySet());
    }

    /// The typed method whose name ends with `tail`, through the next
    /// method of the class.
    private static String typedBody(String j, String tail) {
        int at = j.indexOf("static ");
        while (at >= 0) {
            int eol = j.indexOf('\n', at);
            String head = j.substring(at, eol);
            if (head.contains(" TM_") && head.contains(tail)) {
                int end = j.indexOf("\n    static ", eol);
                return j.substring(at, end < 0 ? j.length() : end);
            }
            at = j.indexOf("static ", eol);
        }
        throw new AssertionError("no typed method ending " + tail);
    }

    private static String location(Class<?> c) throws Exception {
        return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    }

    @Test
    void constantBoolIsMarked() throws Exception {
        String j = main("class T:\n    def __bool__(self):\n        \"\"\"Always.\"\"\"\n        return True\n\n"
                + "print(bool(T()))\n");
        assertTrue(j.contains(".constant(Py.TRUE)"), "a __bool__ that only returns True is marked constant");
    }

    @Test
    void nextUnderStopIterationIsDirect() throws Exception {
        String j = main("def first(it):\n    try:\n        v = next(it)\n    except StopIteration:\n"
                + "        return None\n    return v\n\nprint(first(iter([1])))\n");
        assertTrue(j.contains("Builtins.nextOrStop("), "next() in try/except StopIteration calls the runtime");
        assertFalse(j.contains("\"__cn1_next_or_stop__\""), "the desugared helper is not looked up by name");
    }

    @Test
    void generatorPropertyLoopIsTyped() throws Exception {
        String j = main("class Node:\n    def __init__(self, name, kids):\n        self.name = name\n"
                + "        self.kids = kids\n\n    @property\n    def children(self):\n"
                + "        return (k for k in self.kids)\n\n"
                + "def names(n):\n    out = []\n    for c in n.children:\n        out.append(c.name)\n"
                + "    return out\n\nprint(names(Node('r', [Node('a', []), Node('b', [])])))\n");
        assertTrue(Pattern.compile("L\\d+_Node v_c = null;").matcher(j).find(),
                "the loop variable over a generator property is held typed");
    }

    @Test
    void sharedKeywordCallIsVirtual() throws Exception {
        String j = main("class F:\n    def match(self, e, _known=False):\n        return _known or e == 1\n\n"
                + "    def run(self, xs):\n        return [x for x in xs if self.match(x, _known=False)]\n\n"
                + "class G(F):\n    def match(self, e, _known=False):\n        return e == 2\n\n"
                + "print(F().run([1, 2]), G().run([1, 2]))\n");
        String run = typedBody(j, "_F_run(");
        assertTrue(run.contains(".tv_match_2("), "a keyword call every override binds alike is a typed virtual call");
        assertFalse(run.contains("callKw"), "the typed body makes no keyword call at run time");
    }

    @Test
    void localNamedGlobalsKeepsModuleTyped() throws Exception {
        // bs4 keeps a frame's globals in a local named `globals`; only the
        // builtin itself hands the namespace out.
        String src = "class Node:\n    def __init__(self, name, kids):\n        self.name = name\n"
                + "        self.kids = kids\n\n    @property\n    def children(self):\n"
                + "        return (k for k in self.kids)\n\n"
                + "def names(n):\n    out = []\n    for c in n.children:\n        out.append(c.name)\n"
                + "    return out\n\n"
                + "def frame(d):\n    globals = d\n    return globals\n\n"
                + "print(names(Node('r', [Node('a', []), Node('b', [])])), frame({}))\n";
        Pattern typed = Pattern.compile("L\\d+_Node v_c = null;");
        assertTrue(typed.matcher(main(src)).find(), "a local named globals leaves the module's globals stable");
        assertFalse(typed.matcher(main(src.replace("globals = d\n    return globals", "return globals()"))).find(),
                "the builtin itself still does not");
    }
}
