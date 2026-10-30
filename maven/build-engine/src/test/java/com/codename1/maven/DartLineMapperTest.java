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
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// A class transpiled from Dart reports the Dart files and lines.
public class DartLineMapperTest {
    /// The shape DartLineTable writes: a mark before each statement and the
    /// table, split over two constants, before the closing brace. Line 7 is
    /// a `part` file's statement (file index 1).
    private static final String SOURCE = "package my.app;\n"
            + "public class MainLib {\n"
            + "    static int f(int a) {\n"
            + "        //@dart:3 lib/main.dart\n"
            + "        int b = a + 1;\n"
            + "        //@dart:4 lib/part.dart\n"
            + "        int c = b * 2;\n"
            + "        //@dart:9 lib/main.dart\n"
            + "        return new Runnable() { public void run() { } }.hashCode() + c;\n"
            + "    }\n"
            + "    static final String $cn1DartLines = \"lib/main.dart\\nlib/part.dart\\n5:3,7:\";\n"
            + "    static final String $cn1DartLines1 = \"4:1,9:9\";\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void rewritesLinesSourceAndDropsTheTable() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder(), classes, "my/app/MainLib.java", SOURCE);
        assertEquals(2, DartLineMapper.apply(classes));

        Seen outer = scan(new File(classes, "my/app/MainLib.class"));
        assertEquals("main.dart", outer.source);
        assertFalse("the table is removed", outer.hasTable);
        // main.dart keeps its lines; part.dart's line 4 sits past main.dart's
        // last line (9), at 13.
        assertEquals("[3, 9, 13]", new TreeSet<Integer>(outer.lines).toString());
        assertEquals("SMAP\nMainLib.java\nDart\n*S Dart\n*F\n"
                + "+ 1 main.dart\nlib/main.dart\n+ 2 part.dart\nlib/part.dart\n"
                + "*L\n1#1,9:1\n1#2,4:10\n*E\n", outer.debug);

        // The anonymous class has no table of its own and takes its outer one.
        Seen inner = scan(new File(classes, "my/app/MainLib$1.class"));
        assertEquals("main.dart", inner.source);
        assertEquals("[9]", new TreeSet<Integer>(inner.lines).toString());

        // Gone from the constant pool too, not only as a field.
        String raw = new String(Files.readAllBytes(new File(classes, "my/app/MainLib.class").toPath()),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse("the table's strings are left in the class", raw.contains("cn1DartLines"));
        assertFalse("the table's strings are left in the class", raw.contains("5:3,7:"));

        // Running it again changes nothing: the table is gone.
        assertEquals(0, DartLineMapper.apply(classes));
    }

    /// A Dart identifier may hold a '$': `MainLib$Other` is a class of its
    /// own, not part of `MainLib`, and must not take its table.
    @Test
    public void aDollarInATopLevelNameIsNotNesting() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder(), classes, "my/app/MainLib.java", SOURCE,
                "my/app/MainLib$Other.java", "package my.app;\npublic class MainLib$Other { int f() { return 1; } }\n");
        assertEquals(2, DartLineMapper.apply(classes));
        assertEquals("MainLib$Other.java", scan(new File(classes, "my/app/MainLib$Other.class")).source);
    }

    /// Lines past what a u2 line number holds are not wrapped into another range.
    @Test
    public void filesPastTheLineLimitKeepNoRange() {
        DartLineMapper.Table t = new DartLineMapper.Table("a.dart\nb.dart\n1:60000,5:9000:1");
        assertEquals(60000, t.map(1));
        assertEquals(60000, t.map(5));
        assertFalse(t.smap("A.java"), t.smap("A.java").contains("1#2"));
    }

    @Test
    public void otherClassesAreLeftAlone() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder(), classes, "my/app/Plain.java",
                "package my.app;\npublic final class Plain { static final String $cn1DartLinesX = \"\"; }\n");
        File f = new File(classes, "my/app/Plain.class");
        byte[] before = Files.readAllBytes(f.toPath());
        assertEquals(0, DartLineMapper.apply(classes));
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(f.toPath())));
        assertEquals(0, DartLineMapper.apply(tmp.newFolder("empty")));
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
                s.hasTable |= DartLineMapper.index(name) >= 0;
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
