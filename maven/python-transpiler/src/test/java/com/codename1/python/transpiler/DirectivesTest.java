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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a build leaves out, and how `cn1-python.properties` overrides it:
/// code the program does not reach is not compiled, code it reaches that
/// cannot be translated is a build error naming every place at once, and
/// `remove=` / `replace=` take a function out by name, however deep the
/// package it is in.
class DirectivesTest {
    // No function here has a name the runtime's own classes hold as a
    // string: one that did would be kept, as something Java may look up.
    private static final String TOOLS = "def run_code(text, env=None):\n    return eval(text, env)\n\n"
            + "def drop_everything(x):\n    exec(x)\n\n"
            + "class Box:\n    def load_text(self, text):\n        return eval(text)\n\n"
            + "def spread(text, *rest, flag=False, **more):\n    return text\n\n"
            + "def fine(a):\n    return a + 1\n";

    private static Map<String, String> program(String main, String directives) {
        Map<String, String> sources = new TreeMap<String, String>();
        sources.put("pkg/__init__.py", "");
        sources.put("pkg/tools.py", TOOLS);
        sources.put("main.py", main);
        if (directives != null) {
            sources.put("cn1-python.properties", directives);
        }
        return sources;
    }

    /// The runtime, as an application's class path has it: what the
    /// runtime may look up by name is what keeps a function nothing calls,
    /// and without the runtime to read nothing is left out at all.
    private static List<File> classPath() {
        List<File> cp = new ArrayList<File>();
        try {
            cp.add(new File(com.codename1.python.runtime.Py.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()));
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return cp;
    }

    private static final String CALLS_ALL = "from pkg import tools\nprint(tools.fine(1))\n"
            + "print(tools.run_code('1'))\nprint(tools.Box().load_text('x'))\ntools.drop_everything('pass')\n";

    @Test
    void everyReachedUseIsNamedInOneError() {
        PythonSyntaxError e = assertThrows(PythonSyntaxError.class,
                () -> PythonTranspiler.transpile(program(CALLS_ALL, null), "main", classPath()));
        assertEquals("pkg/tools.py", e.file);
        assertEquals(2, e.line);
        assertTrue(e.reason.contains("pkg/tools.py:2:"), e.reason);
        assertTrue(e.reason.contains("pkg/tools.py:5:"), e.reason);
        assertTrue(e.reason.contains("pkg/tools.py:9:"), e.reason);
        assertTrue(e.reason.contains("remove=pkg.tools.Box.load_text,pkg.tools.drop_everything,pkg.tools.run_code"), e.reason);
        assertTrue(e.reason.contains("replace="), e.reason);
    }

    @Test
    void codeNothingReachesIsLeftOut() {
        Map<String, String> out = PythonTranspiler.transpile(
                program("from pkg import tools\nprint(tools.fine(1))\n", null), "main", classPath());
        String java = module(out, "pkg.tools");
        assertTrue(java.contains("was left out of this build"), "the unreached functions say why they raise");
        assertTrue(java.contains("keep=pkg.tools.run_code"), "and how to keep them");
    }

    @Test
    void keepCompilesWhatNothingReaches() {
        PythonSyntaxError e = assertThrows(PythonSyntaxError.class, () -> PythonTranspiler.transpile(
                program("from pkg import tools\nprint(tools.fine(1))\n", "keep=pkg.tools.drop_everything\n"), "main",
                classPath()));
        assertTrue(e.reason.contains("exec() in pkg.tools.drop_everything"), e.reason);
        assertFalse(e.reason.contains("run_code"), e.reason);
    }

    @Test
    void removeLeavesAFunctionOutByName() {
        Map<String, String> out = PythonTranspiler.transpile(program(CALLS_ALL,
                "# everything that runs text\nremove=pkg.tools.drop_everything, pkg.tools.run_code\nremove=pkg.tools.Box.load_text\n"),
                "main", classPath());
        String java = module(out, "pkg.tools");
        assertTrue(java.contains("pkg.tools.drop_everything was left out of this build (remove= in cn1-python.properties)"));
        assertTrue(java.contains("pkg.tools.Box.load_text was left out of this build"));
        for (String k : out.keySet()) {
            assertFalse(k.contains("/ext/"), "a removal needs no Java: " + k);
        }
    }

    @Test
    void removeTakesAClass() {
        String java = module(PythonTranspiler.transpile(program(CALLS_ALL,
                "remove=pkg.tools.Box,pkg.tools.drop_everything,pkg.tools.run_code\n"), "main", classPath()), "pkg.tools");
        assertTrue(java.contains("pkg.tools.Box.load_text was left out of this build (remove=pkg.tools.Box in"), java);
    }

    @Test
    void removeTakesAModuleAndAPackage() {
        String java = module(PythonTranspiler.transpile(program(CALLS_ALL, "remove=pkg.tools\n"), "main",
                classPath()), "pkg.tools");
        assertTrue(java.contains("pkg.tools was left out of this build (remove=pkg.tools in"), java);
        assertFalse(java.contains("load_text"), "nothing of the module is compiled");
        java = module(PythonTranspiler.transpile(program(CALLS_ALL, "remove=pkg\n"), "main", classPath()),
                "pkg.tools");
        assertTrue(java.contains("pkg.tools was left out of this build (remove=pkg in"), java);
    }

    @Test
    void aNameThatIsNothingIsAnError() {
        PythonSyntaxError e = assertThrows(PythonSyntaxError.class, () -> PythonTranspiler.transpile(
                program(CALLS_ALL, "remove=pkg.tools.Missing\n"), "main", classPath()));
        assertTrue(e.reason.contains("is not a function, a class, a module or a package"), e.reason);
    }

    @Test
    void replaceGeneratesTheJavaToFillIn() {
        Map<String, String> out = PythonTranspiler.transpile(program(CALLS_ALL,
                "remove=pkg.tools.drop_everything\nreplace=pkg.tools.run_code,pkg.tools.Box.load_text\nreplace=pkg.tools.spread\n"), "main", classPath());
        String shim = out.get("com/codename1/python/ext/PkgToolsShims.java");
        assertNotNull(shim, out.keySet().toString());
        assertTrue(shim.contains("NativeModule.replace(\"pkg.tools.run_code\""), shim);
        assertTrue(shim.contains("NativeModule.replace(\"pkg.tools.Box.load_text\""), shim);
        assertTrue(shim.contains("// args: text, env"), shim);
        assertTrue(shim.contains("// args: self, text"), shim);
        assertTrue(shim.contains("// args: text, rest (a tuple), flag, more (a dict)"), shim);
        assertTrue(shim.contains("NativeModule.notReplaced(\"pkg.tools.run_code\")"), shim);
        boolean installed = false;
        for (Map.Entry<String, String> e : out.entrySet()) {
            installed |= e.getKey().endsWith("/PythonModules.java")
                    && e.getValue().contains("com.codename1.python.ext.PkgToolsShims.install()");
        }
        assertTrue(installed, "the program installs the replacements before any module runs");
        assertTrue(module(out, "pkg.tools").contains("\"__cn1_replaced__\"), \"pkg.tools.run_code\""));
    }

    @Test
    void aNameThatIsNoFunctionIsAnError() {
        PythonSyntaxError e = assertThrows(PythonSyntaxError.class, () -> PythonTranspiler.transpile(
                program(CALLS_ALL, "remove=pkg.tools.no_such\n"), "main", classPath()));
        assertTrue(e.getMessage().contains("pkg.tools.no_such"), e.getMessage());
        e = assertThrows(PythonSyntaxError.class, () -> PythonTranspiler.transpile(
                program(CALLS_ALL, "omit=pkg.tools.drop_everything\n"), "main", classPath()));
        assertEquals("cn1-python.properties", e.file);
        assertEquals(1, e.line);
    }

    /// The Java of the module `name`.
    private static String module(Map<String, String> out, String name) {
        String file = "/py_" + name.replace(".", "$2e") + ".java";
        for (Map.Entry<String, String> e : out.entrySet()) {
            if (e.getKey().endsWith(file)) {
                return e.getValue();
            }
        }
        throw new AssertionError("no " + file + " among " + out.keySet());
    }
}
