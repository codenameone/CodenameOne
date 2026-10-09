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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// The array class of a type an application only names with a class
/// literal: ParparVM has none unless some bytecode names the array type,
/// so the remap generates a class that does.
public class ArrayClassGeneratorTest {

    /// A listener type of the application's own, handed to a method that
    /// makes an array of it by reflection, and never made an array of by
    /// the application: a method that returns `Listener[]` and a cast to
    /// it are not enough for a device.
    private static final String SOURCE = "package q;\n"
            + "public class Model {\n"
            + "    public interface Listener extends java.util.EventListener { }\n"
            + "    public static class Other { }\n"
            + "    public Listener[] listeners() {\n"
            + "        return (Listener[]) java.lang.reflect.Array.newInstance(Listener.class, 0);\n"
            + "    }\n"
            + "    public Object made() { return new Other[0]; }\n"
            + "    public Class<?> already() { return String[].class; }\n"
            + "}\n";

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private static List<String> arrayLiterals(byte[] cls) {
        final List<String> out = new ArrayList<String>();
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type && ((Type) value).getSort() == Type.ARRAY) {
                            out.add(((Type) value).getDescriptor());
                        }
                    }
                };
            }
        }, 0);
        return out;
    }

    @Test
    public void aClassLiteralGetsItsArrayClass() throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compile(tmp.newFolder(), classes, "q/Model.java", SOURCE);
        List<String> app = Arrays.asList("q/Model", "q/Model$Listener", "q/Model$Other");
        // The application's own classes name the array of neither type
        // with a literal; that is the case a device threw for.
        assertFalse(arrayLiterals(Files.readAllBytes(new File(classes, "q/Model.class").toPath()))
                .contains("[Lq/Model$Listener;"));

        assertEquals(1, new ArrayClassGenerator(classes).run(app));
        File holder = new File(classes, ArrayClassGenerator.HOLDER + ".class");
        byte[] first = Files.readAllBytes(holder.toPath());
        assertEquals(Arrays.asList("[Lq/Model$Listener;"), arrayLiterals(first));

        // It is a class a device can load: it verifies and runs.
        CompatFixtures.Defining loader = new CompatFixtures.Defining(new java.net.URLClassLoader(
                new java.net.URL[] {classes.toURI().toURL()}, null));
        loader.define(first).getMethod("types").invoke(null);

        // A second run, which finds the holder among the classes, writes
        // the same bytes.
        List<String> again = new ArrayList<String>(app);
        again.add(ArrayClassGenerator.HOLDER);
        assertEquals(1, new ArrayClassGenerator(classes).run(again));
        assertArrayEquals(first, Files.readAllBytes(holder.toPath()));
    }

    @Test
    public void withoutAClassLiteralThereIsNoHolder() throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compile(tmp.newFolder(), classes, "q/Plain.java",
                "package q;\npublic class Plain { public int one() { return 1; } }\n");
        File holder = new File(classes, ArrayClassGenerator.HOLDER + ".class");
        holder.getParentFile().mkdirs();
        Files.write(holder.toPath(), new byte[] {1});
        assertEquals(0, new ArrayClassGenerator(classes).run(Arrays.asList("q/Plain")));
        assertFalse(holder.exists());
        assertTrue(new File(classes, "q/Plain.class").isFile());
    }
}
