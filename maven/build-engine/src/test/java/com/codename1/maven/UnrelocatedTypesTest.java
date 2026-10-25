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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// The check the integration tests run over a remapped classes directory:
/// a type left under a toolkit's package is reported, text that merely
/// spells one is not.
public class UnrelocatedTypesTest {

    /// Spells Swing's names every way a string can, and names no Swing type.
    private static final String WORDS = "package com.acme;\n"
            + "@Deprecated\n"
            + "public class Words {\n"
            + "    public static final String PATH = \"javax/swing/JTable\";\n"
            + "    String descriptor() { return \"Ljavax/swing/JTable;\" + \"(Ljava/awt/Color;)V\"; }\n"
            + "    String dotted() { return \"javax.swing.JTable\".concat(\" javax/swing/plaf/basic/icon.png\"); }\n"
            + "    Object resource() { return Words.class.getResource(\"/javax/swing/x.properties\"); }\n"
            + "}\n";

    private static final String FIELD = "package com.acme;\n"
            + "public class Holder { javax.swing.JTable table; }\n";

    private static final String SIGNATURE = "package com.acme;\n"
            + "public class Generic { java.util.List<java.awt.Color> colors() { return null; } }\n";

    private static final String BODY = "package com.acme;\n"
            + "public class Body {\n"
            + "    Object make() { return new javax.swing.JLabel(\"x\"); }\n"
            + "    Class<?> literal() { return java.awt.Point[].class; }\n"
            + "}\n";

    private static final List<String> SWING = Arrays.asList("javax/swing/", "java/awt/");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File compile(String... nameThenSource) throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compile(tmp.newFolder(), classes, nameThenSource);
        return classes;
    }

    @Test
    public void textThatSpellsAToolkitNameIsNotAReference() throws Exception {
        File classes = compile("com/acme/Words.java", WORDS);
        // The bytes are there, which is what a search of the file trips on.
        assertTrue(new String(Files.readAllBytes(new File(classes, "com/acme/Words.class").toPath()), "ISO-8859-1")
                .contains("Ljavax/swing/JTable;"));
        assertTrue(UnrelocatedTypes.scan(classes, SWING).isEmpty());
    }

    @Test
    public void aTypeIsFoundWhereverAClassNamesIt() throws Exception {
        File classes = compile("com/acme/Words.java", WORDS, "com/acme/Holder.java", FIELD,
                "com/acme/Generic.java", SIGNATURE, "com/acme/Body.java", BODY);
        Map<String, Set<String>> found = UnrelocatedTypes.scan(classes, SWING);
        assertEquals(Arrays.asList("com/acme/Body.class", "com/acme/Generic.class", "com/acme/Holder.class"),
                new java.util.ArrayList<String>(found.keySet()));
        assertEquals("[javax/swing/JTable]", found.get("com/acme/Holder.class").toString());
        // Only in the generic signature: the erased descriptor is List.
        assertEquals("[java/awt/Color]", found.get("com/acme/Generic.class").toString());
        assertEquals("[java/awt/Point, javax/swing/JLabel]", found.get("com/acme/Body.class").toString());
        // A package is matched whole: java/awt is not java/awtx.
        assertTrue(UnrelocatedTypes.scan(classes, Arrays.asList("javax/swi/", "java/aw/")).isEmpty());
    }

    @Test
    public void theProgramPrintsOneLineAClassAndAnswersByItsStatus() throws Exception {
        File classes = compile("com/acme/Words.java", WORDS, "com/acme/Holder.java", FIELD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(1, UnrelocatedTypes.run(new String[] {classes.getPath(), "javax/swing", "java/awt/"},
                new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8")));
        assertEquals("com/acme/Holder.class\tjavax/swing/JTable\n", out.toString("UTF-8").replace("\r", ""));

        // A jar is read as the directory is.
        File jar = CompatRemapperTest.jar(new File(tmp.newFolder(), "app.jar"),
                "com/acme/Holder.class", Files.readAllBytes(new File(classes, "com/acme/Holder.class").toPath()),
                "com/acme/Words.class", Files.readAllBytes(new File(classes, "com/acme/Words.class").toPath()),
                "notes.txt", "javax/swing/JTable".getBytes("UTF-8"));
        out.reset();
        assertEquals(1, UnrelocatedTypes.run(new String[] {jar.getPath(), "javax/swing"},
                new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8")));
        assertEquals("com/acme/Holder.class\tjavax/swing/JTable\n", out.toString("UTF-8").replace("\r", ""));

        out.reset();
        assertEquals(0, UnrelocatedTypes.run(new String[] {classes.getPath(), "javafx"},
                new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8")));
        assertEquals("", out.toString("UTF-8"));
        assertEquals(2, UnrelocatedTypes.run(new String[] {classes.getPath()},
                new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8")));
        assertEquals(2, UnrelocatedTypes.run(new String[] {new File(classes, "none").getPath(), "javafx"},
                new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8")));
    }
}
