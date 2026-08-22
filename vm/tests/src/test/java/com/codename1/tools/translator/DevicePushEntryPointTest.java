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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which class a pushed tree enters.
 *
 * <p>A real application has no {@code main}: it has a {@code Lifecycle}
 * subclass, and finding it is what makes pushing a project's own source tree
 * work at all.</p>
 */
class DevicePushEntryPointTest {

    private static final String LIFECYCLE = "com/codename1/system/Lifecycle";

    /**
     * The walk has to follow the whole chain. It was bounded at 64 edges, and a
     * hierarchy longer than that reported "no entry point" for a project that
     * plainly has one -- while the same bound made two deeper candidates tie,
     * so which one won depended on hash order.
     */
    @Test
    void aDeepHierarchyStillFindsItsEntryPoint() throws Exception {
        Path dir = Files.createTempDirectory("entry-point");
        List<File> classes = new ArrayList<File>();
        String parent = LIFECYCLE;
        for (int i = 0; i < 80; i++) {
            String name = "com/example/Base" + i;
            classes.add(write(dir, name, parent, true));
            parent = name;
        }
        classes.add(write(dir, "com/example/App", parent, false));

        assertEquals("com/example/App", DevicePush.findEntryPoint(classes),
                "an 81-deep Lifecycle descendant is still the entry point");
    }

    /** The deepest concrete descendant wins: BaseApp is not the application. */
    @Test
    void theDeepestConcreteDescendantWins() throws Exception {
        Path dir = Files.createTempDirectory("entry-point-depth");
        List<File> classes = new ArrayList<File>();
        classes.add(write(dir, "com/example/BaseApp", LIFECYCLE, false));
        classes.add(write(dir, "com/example/MyApp", "com/example/BaseApp", false));

        assertEquals("com/example/MyApp", DevicePush.findEntryPoint(classes));
    }

    /**
     * Two equally deep candidates must not depend on hash order: the same tree
     * pushed twice has to enter the same class.
     */
    @Test
    void aTieIsBrokenDeterministically() throws Exception {
        Path dir = Files.createTempDirectory("entry-point-tie");
        List<File> a = new ArrayList<File>();
        a.add(write(dir, "com/example/Zeta", LIFECYCLE, false));
        a.add(write(dir, "com/example/Alpha", LIFECYCLE, false));
        List<File> b = new ArrayList<File>(a);
        Collections.reverse(b);

        assertEquals("com/example/Alpha", DevicePush.findEntryPoint(a));
        assertEquals(DevicePush.findEntryPoint(a), DevicePush.findEntryPoint(b),
                "the order the files were read in must not decide");
    }

    /**
     * Two mains is a real shape -- an application plus a diagnostic launcher --
     * and {@code listFiles()} has no defined order, so returning the first one
     * seen made identical sources push different programs on different
     * machines, or after a rebuild.
     */
    @Test
    void severalMainsAreChosenBetweenDeterministically() throws Exception {
        Path dir = Files.createTempDirectory("entry-point-mains");
        List<File> classes = new ArrayList<File>();
        classes.add(writeWithMain(dir, "com/example/Tool"));
        classes.add(writeWithMain(dir, "com/example/App"));
        List<File> reversed = new ArrayList<File>(classes);
        Collections.reverse(reversed);

        assertEquals("com/example/App", DevicePush.findEntryPoint(classes));
        assertEquals(DevicePush.findEntryPoint(classes), DevicePush.findEntryPoint(reversed),
                "the order the files were read in must not decide");
    }

    private static File writeWithMain(Path dir, String internalName) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main",
                "([Ljava/lang/String;)V", null, null).visitEnd();
        cw.visitEnd();
        Path out = dir.resolve(internalName.replace('/', '_') + ".class");
        Files.write(out, cw.toByteArray());
        return out.toFile();
    }

    private static File write(Path dir, String internalName, String superName, boolean isAbstract)
            throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | (isAbstract ? Opcodes.ACC_ABSTRACT : 0),
                internalName, null, superName, null);
        cw.visitEnd();
        Path out = dir.resolve(internalName.replace('/', '_') + ".class");
        Files.write(out, cw.toByteArray());
        return out.toFile();
    }
}
