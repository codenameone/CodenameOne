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
package com.codename1.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// A class translated from Python reports the Python file and lines.
public class PythonLineMapperTest {
    /// What the transpiler writes: a line comment before each statement and
    /// the table, computed from them, on the class declaration's line.
    private static final String SOURCE = "package com.codename1.generated.python;\n"
            + "public final class py_app$2eutil { private static final String $cn1PyLines = "
            + "\"app/util.py\\n5:3,7:4,9:9\";\n"
            + "    static int f(int a) {\n"
            + "        /* line 3 */\n"
            + "        int b = a + 1;\n"
            + "        /* line 4 */\n"
            + "        int c = b * 2;\n"
            + "        /* line 9 */\n"
            + "        return new Inner().g(c);\n"
            + "    }\n"
            + "    static final class Inner {\n"
            + "        int g(int x) {\n"
            + "            return x - 1;\n"
            + "        }\n"
            + "    }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void rewritesLinesSourceAndDropsTheTable() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder(), classes, "com/codename1/generated/python/py_app$2eutil.java", SOURCE);
        assertEquals(2, PythonLineMapper.apply(classes));

        File dir = new File(classes, PythonLineMapper.PACKAGE_DIR);
        Seen outer = scan(new File(dir, "py_app$2eutil.class"));
        assertEquals("util.py", outer.source);
        assertTrue(outer.debug, outer.debug.contains("\napp/util.py\n"));
        assertTrue(outer.debug, outer.debug.startsWith("SMAP\n"));
        assertFalse("the table is removed", outer.hasTable);
        assertTrue(outer.lines.toString(), outer.lines.contains(3));
        assertTrue(outer.lines.toString(), outer.lines.contains(4));
        assertTrue(outer.lines.toString(), outer.lines.contains(9));
        for (int line : outer.lines) {
            assertTrue("no Java line survives: " + outer.lines, line == 3 || line == 4 || line == 9);
        }
        // The nested class carries no table of its own and takes its
        // module's, its lines being past the last statement's.
        Seen inner = scan(new File(dir, "py_app$2eutil$Inner.class"));
        assertEquals("util.py", inner.source);
        assertEquals("[9]", new java.util.TreeSet<Integer>(inner.lines).toString());

        // Running it again changes nothing: the table is gone.
        assertEquals(0, PythonLineMapper.apply(classes));
    }

    @Test
    public void otherClassesAreLeftAlone() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder(), classes, "com/codename1/generated/python/PythonModules.java",
                "package com.codename1.generated.python;\npublic final class PythonModules { }\n");
        File f = new File(classes, PythonLineMapper.PACKAGE_DIR + "/PythonModules.class");
        byte[] before = Files.readAllBytes(f.toPath());
        assertEquals(0, PythonLineMapper.apply(classes));
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(f.toPath())));
        assertEquals(0, PythonLineMapper.apply(tmp.newFolder("empty")));
    }

    private static final class Seen {
        String source;
        String debug;
        boolean hasTable;
        final List<Integer> lines = new ArrayList<Integer>();
    }

    private static Seen scan(File f) throws Exception {
        final Seen s = new Seen();
        new ClassReader(Files.readAllBytes(f.toPath())).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visitSource(String source, String debug) {
                s.source = source;
                s.debug = debug;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String sig, Object v) {
                s.hasTable |= PythonLineMapper.FIELD.equals(name);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLineNumber(int line, Label start) {
                        s.lines.add(line);
                    }
                };
            }
        }, 0);
        return s;
    }
}
