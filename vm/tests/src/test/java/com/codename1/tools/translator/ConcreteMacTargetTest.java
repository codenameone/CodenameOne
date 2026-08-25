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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Covers the {@code mac} arm of {@code @Concrete} and the superclass walk that
/// resolves a devirtualized call against the concrete type's whole hierarchy.
///
/// Both are silent-failure paths. A macOS build left on the iOS attribute still
/// compiles and links -- it just binds every call to the iOS implementation and
/// never runs the macOS override. A resolver that only looks at the concrete
/// class's own declarations still produces correct code -- it just gives up on
/// devirtualizing everything the class inherits.
class ConcreteMacTargetTest {

    private static final String ANNOTATION_DESC = "Lcom/codename1/annotations/Concrete;";

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    @AfterEach
    void resetTarget() {
        ByteCodeClass.setConcreteTarget(null);
        Parser.cleanup();
    }

    @Test
    void macTargetPrefersTheMacAttribute() throws Exception {
        ByteCodeClass.setConcreteTarget("mac");

        ByteCodeClass cls = parseAnnotated("com/example/AnnotatedAll",
                "com.example.IosImpl", "com.example.WinImpl",
                "com.example.LinuxImpl", "com.example.MacImpl");

        assertEquals("com/example/MacImpl", cls.getConcreteClass(),
                "the mac target must honour @Concrete.mac(), not name()");
    }

    @Test
    void macTargetWithoutAMacAttributeFallsBackToThePortableBase() throws Exception {
        ByteCodeClass.setConcreteTarget("mac");

        ByteCodeClass cls = parseAnnotated("com/example/AnnotatedNoMac",
                "com.example.IosImpl", "com.example.WinImpl",
                "com.example.LinuxImpl", null);

        // Deliberately unset rather than falling through to name(): an absent
        // mac() means "no macOS specialization", and pulling in the iOS class
        // would translate a type the macOS build never compiles.
        assertNull(cls.getConcreteClass(),
                "an absent mac() must leave the concrete unset, not fall back to name()");
    }

    @Test
    void otherTargetsAreUnaffectedByTheMacAttribute() throws Exception {
        ByteCodeClass.setConcreteTarget(null);
        assertEquals("com/example/IosImpl", parseAnnotated("com/example/AnnotatedIos",
                "com.example.IosImpl", "com.example.WinImpl",
                "com.example.LinuxImpl", "com.example.MacImpl").getConcreteClass());

        Parser.cleanup();
        ByteCodeClass.setConcreteTarget("win");
        assertEquals("com/example/WinImpl", parseAnnotated("com/example/AnnotatedWin",
                "com.example.IosImpl", "com.example.WinImpl",
                "com.example.LinuxImpl", "com.example.MacImpl").getConcreteClass());

        Parser.cleanup();
        ByteCodeClass.setConcreteTarget("linux");
        assertEquals("com/example/LinuxImpl", parseAnnotated("com/example/AnnotatedLinux",
                "com.example.IosImpl", "com.example.WinImpl",
                "com.example.LinuxImpl", "com.example.MacImpl").getConcreteClass());
    }

    @Test
    void concreteDeclaringClassWalksUpToTheInheritedDeclaration() throws Exception {
        // Super declares run()V; Sub extends Super and does not override it.
        // This is the MacImplementation-extends-IOSImplementation shape.
        Parser.parse(writeClass("com/example/Super", cw -> {
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    "com/example/Super", null, "java/lang/Object", null);
            emitDefaultConstructor(cw, "java/lang/Object");
            emitEmptyMethod(cw, "run");
            cw.visitEnd();
        }).toFile());
        Parser.parse(writeClass("com/example/Sub", cw -> {
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    "com/example/Sub", null, "com/example/Super", null);
            emitDefaultConstructor(cw, "com/example/Super");
            cw.visitEnd();
        }).toFile());

        ByteCodeClass sub = Parser.getClassObject("com_example_Sub");
        ByteCodeClass superCls = Parser.getClassObject("com_example_Super");
        assertNotNull(sub);
        assertNotNull(superCls);
        sub.setBaseClassObject(superCls);

        assertFalse(sub.hasDeclaredMethod("run", "()V"),
                "the fixture is only meaningful if Sub does not declare run()");
        assertSame(superCls, ByteCodeClass.findConcreteDeclaringClass(sub, "run", "()V"),
                "an inherited method must resolve to the declaring superclass, not to null");
    }

    @Test
    void concreteDeclaringClassPrefersTheOverrideOverTheInheritedOne() throws Exception {
        Parser.parse(writeClass("com/example/Base2", cw -> {
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    "com/example/Base2", null, "java/lang/Object", null);
            emitDefaultConstructor(cw, "java/lang/Object");
            emitEmptyMethod(cw, "run");
            cw.visitEnd();
        }).toFile());
        Parser.parse(writeClass("com/example/Derived2", cw -> {
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    "com/example/Derived2", null, "com/example/Base2", null);
            emitDefaultConstructor(cw, "com/example/Base2");
            emitEmptyMethod(cw, "run");
            cw.visitEnd();
        }).toFile());

        ByteCodeClass derived = Parser.getClassObject("com_example_Derived2");
        ByteCodeClass base = Parser.getClassObject("com_example_Base2");
        derived.setBaseClassObject(base);

        assertSame(derived, ByteCodeClass.findConcreteDeclaringClass(derived, "run", "()V"),
                "the walk must stop at the nearest declaration, which is the override");
    }

    @Test
    void concreteDeclaringClassReturnsNullWhenNothingDeclaresIt() throws Exception {
        Parser.parse(writeClass("com/example/Empty", cw -> {
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    "com/example/Empty", null, "java/lang/Object", null);
            emitDefaultConstructor(cw, "java/lang/Object");
            cw.visitEnd();
        }).toFile());

        ByteCodeClass empty = Parser.getClassObject("com_example_Empty");
        assertNull(ByteCodeClass.findConcreteDeclaringClass(empty, "absent", "()V"));
        assertNull(ByteCodeClass.findConcreteDeclaringClass(null, "absent", "()V"));
    }

    private ByteCodeClass parseAnnotated(String internalName, String name, String win,
            String linux, String mac) throws Exception {
        Path classFile = writeClass(internalName, cw -> {
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    internalName, null, "java/lang/Object", null);
            AnnotationVisitor av = cw.visitAnnotation(ANNOTATION_DESC, false);
            av.visit("name", name);
            if (win != null) {
                av.visit("win", win);
            }
            if (linux != null) {
                av.visit("linux", linux);
            }
            if (mac != null) {
                av.visit("mac", mac);
            }
            av.visitEnd();
            emitDefaultConstructor(cw, "java/lang/Object");
            cw.visitEnd();
        });
        Parser.parse(classFile.toFile());
        ByteCodeClass cls = Parser.getClassObject(internalName.replace('/', '_'));
        assertNotNull(cls, "annotated class should be parsed");
        return cls;
    }

    private static void emitDefaultConstructor(ClassWriter cw, String superName) {
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
    }

    private static void emitEmptyMethod(ClassWriter cw, String name) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
    }

    private Path writeClass(String internalName, ClassEmitter emitter) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        emitter.accept(cw);
        Path outputDir = Files.createTempDirectory("parparvm-concrete-mac");
        Path classFile = outputDir.resolve(internalName + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, cw.toByteArray());
        return classFile;
    }

    @FunctionalInterface
    private interface ClassEmitter {
        void accept(ClassWriter cw);
    }
}
