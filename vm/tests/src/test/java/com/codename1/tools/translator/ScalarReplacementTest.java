/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scalar replacement folds a non-escaping primitive-only object into a C struct
 * local and rewrites its field reads into direct member accesses. Both halves of
 * that rewrite are pattern matches over an instruction list, and both used to be
 * matched in ways that ignored where the instructions actually sit:
 *
 * <ul>
 *   <li>the ALOAD/GETFIELD lookahead skipped every label, including branch
 *       targets, so a field read shared by two paths (a ternary receiver) was
 *       folded into one path's struct -- the other path then read the wrong
 *       object and left its receiver on the operand stack;</li>
 *   <li>the constructor and trivial-getter shape checks read another method's
 *       LIVE instruction list, which optimize() folds, so the decision depended
 *       on the order the classes happened to be parsed in.</li>
 * </ul>
 */
class ScalarReplacementTest {
    private static final String VALUE_CLASS = "com/example/SrValue";
    private static final String HOST_CLASS = "com/example/SrHost";

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    /**
     * {@code (c ? p : q).x} puts a branch target between "ALOAD q" and the
     * GETFIELD both paths run. That GETFIELD must survive as a real field read.
     */
    @Test
    void sharedFieldReadBehindAJoinIsNotFoldedIntoOneObject() throws Exception {
        for (boolean valueClassFirst : new boolean[]{true, false}) {
            Parser.cleanup();
            String ternary = cFunctionBody(translateHost(valueClassFirst), "_ternary___");
            assertTrue(ternary.contains("get_field_com_example_SrValue_x"),
                    "the field read shared by both ternary arms must stay a real GETFIELD, was:\n" + ternary);
            assertFalse(ternary.contains("scalar-replaced GETFIELD"),
                    "a GETFIELD reachable from two paths must not be folded into one path's struct, was:\n" + ternary);
        }
    }

    /**
     * Guards the test above against passing for the wrong reason: the pass must
     * still fire on the straight-line shapes it exists for, through a direct
     * field read and through a trivial getter.
     */
    @Test
    void straightLineAllocationsAreStillScalarReplaced() throws Exception {
        for (boolean valueClassFirst : new boolean[]{true, false}) {
            Parser.cleanup();
            String hostCode = translateHost(valueClassFirst);
            assertTrue(cFunctionBody(hostCode, "_simple___").contains("scalar-replaced GETFIELD"),
                    "a non-escaping allocation read through a field access should still be scalar replaced");
            assertTrue(cFunctionBody(hostCode, "_viaGetter___").contains("scalar-replaced GETFIELD"),
                    "a non-escaping allocation read through a trivial getter should still be scalar replaced");
        }
    }

    /**
     * The value class carries the constructor and the getter whose shapes decide
     * whether the host's allocations are scalar replaced. Parsing it after the
     * host means it has already been optimized by the time the host is, so a
     * matcher that reads its live instruction list produces different output --
     * the same sources translating differently between two builds.
     */
    @Test
    void translationIsIndependentOfClassParseOrder() throws Exception {
        String valueFirst = translateHost(true);
        Parser.cleanup();
        String hostFirst = translateHost(false);

        assertEquals(withoutLabelNames(valueFirst), withoutLabelNames(hostFirst),
                "generated C must not depend on the order the classes were parsed in");
    }

    /**
     * Emitted C label names are derived from the ASM {@code Label} identity hash, so
     * they differ between two runs of the same input and carry no meaning. Everything
     * else in the output does.
     */
    private String withoutLabelNames(String code) {
        return code.replaceAll("label_L\\w+", "label_L");
    }

    /**
     * Parses the two classes in the requested order and returns the generated C
     * for the host class.
     */
    private String translateHost(boolean valueClassFirst) throws Exception {
        Path valueFile = writeValueClass();
        Path hostFile = writeHostClass();
        if (valueClassFirst) {
            Parser.parse(valueFile.toFile());
            Parser.parse(hostFile.toFile());
        } else {
            Parser.parse(hostFile.toFile());
            Parser.parse(valueFile.toFile());
        }

        ByteCodeClass objectClass = new ByteCodeClass("java_lang_Object", "java/lang/Object");
        ByteCodeClass value = Parser.getClassObject("com_example_SrValue");
        ByteCodeClass host = Parser.getClassObject("com_example_SrHost");
        for (ByteCodeClass cls : Arrays.asList(value, host)) {
            cls.setBaseClassObject(objectClass);
            cls.setBaseInterfacesObject(Collections.<ByteCodeClass>emptyList());
            cls.updateAllDependencies();
        }

        // generateCCode is what runs optimize(), so emitting in parse order is what
        // decides whether the host sees SrValue's ctor and getter raw or already
        // folded -- exactly the ordering the real translator walks.
        List<ByteCodeClass> classes = Arrays.asList(objectClass, value, host);
        if (valueClassFirst) {
            value.generateCCode(classes);
            return host.generateCCode(classes);
        }
        String hostCode = host.generateCCode(classes);
        value.generateCCode(classes);
        return hostCode;
    }

    /** The text of the first generated C function whose name contains {@code marker}. */
    private String cFunctionBody(String code, String marker) {
        int nameAt = code.indexOf(marker);
        assertTrue(nameAt > 0, "generated code has no function matching " + marker + ":\n" + code);
        int start = code.indexOf('{', nameAt);
        int end = code.indexOf("\n}", start);
        assertTrue(start > 0 && end > start, "could not delimit the body of " + marker);
        return code.substring(start, end);
    }

    /** {@code final class SrValue { final int x; SrValue(int) ; int getX(); }} */
    private Path writeValueClass() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                VALUE_CLASS, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "x", "I", null, null).visitEnd();

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ILOAD, 1);
        init.visitFieldInsn(Opcodes.PUTFIELD, VALUE_CLASS, "x", "I");
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(2, 2);
        init.visitEnd();

        MethodVisitor getX = cw.visitMethod(Opcodes.ACC_PUBLIC, "getX", "()I", null, null);
        getX.visitCode();
        getX.visitVarInsn(Opcodes.ALOAD, 0);
        getX.visitFieldInsn(Opcodes.GETFIELD, VALUE_CLASS, "x", "I");
        getX.visitInsn(Opcodes.IRETURN);
        getX.visitMaxs(1, 1);
        getX.visitEnd();

        cw.visitEnd();
        return writeClassFile(VALUE_CLASS, cw);
    }

    /**
     * {@code simple(v)} and {@code viaGetter(v)} are the straight-line shapes the
     * pass targets; {@code ternary(c)} is javac's lowering of
     * {@code return (c ? new SrValue(1) : new SrValue(2)).x;} -- the GETFIELD sits
     * behind a join both arms branch into.
     */
    private Path writeHostClass() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                HOST_CLASS, null, "java/lang/Object", null);

        MethodVisitor simple = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "simple", "(I)I", null, null);
        simple.visitCode();
        newValue(simple, Opcodes.ILOAD, 0);
        simple.visitVarInsn(Opcodes.ASTORE, 1);
        simple.visitVarInsn(Opcodes.ALOAD, 1);
        simple.visitFieldInsn(Opcodes.GETFIELD, VALUE_CLASS, "x", "I");
        simple.visitInsn(Opcodes.IRETURN);
        simple.visitMaxs(3, 2);
        simple.visitEnd();

        MethodVisitor viaGetter = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "viaGetter", "(I)I", null, null);
        viaGetter.visitCode();
        newValue(viaGetter, Opcodes.ILOAD, 0);
        viaGetter.visitVarInsn(Opcodes.ASTORE, 1);
        viaGetter.visitVarInsn(Opcodes.ALOAD, 1);
        viaGetter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, VALUE_CLASS, "getX", "()I", false);
        viaGetter.visitInsn(Opcodes.IRETURN);
        viaGetter.visitMaxs(3, 2);
        viaGetter.visitEnd();

        MethodVisitor ternary = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "ternary", "(Z)I", null, null);
        Label elseArm = new Label();
        Label join = new Label();
        ternary.visitCode();
        newValueConst(ternary, Opcodes.ICONST_1);
        ternary.visitVarInsn(Opcodes.ASTORE, 1);
        newValueConst(ternary, Opcodes.ICONST_2);
        ternary.visitVarInsn(Opcodes.ASTORE, 2);
        ternary.visitVarInsn(Opcodes.ILOAD, 0);
        ternary.visitJumpInsn(Opcodes.IFEQ, elseArm);
        ternary.visitVarInsn(Opcodes.ALOAD, 1);
        ternary.visitJumpInsn(Opcodes.GOTO, join);
        ternary.visitLabel(elseArm);
        ternary.visitVarInsn(Opcodes.ALOAD, 2);
        ternary.visitLabel(join);
        ternary.visitFieldInsn(Opcodes.GETFIELD, VALUE_CLASS, "x", "I");
        ternary.visitInsn(Opcodes.IRETURN);
        ternary.visitMaxs(3, 3);
        ternary.visitEnd();

        cw.visitEnd();
        return writeClassFile(HOST_CLASS, cw);
    }

    private void newValue(MethodVisitor mv, int loadOpcode, int slot) {
        mv.visitTypeInsn(Opcodes.NEW, VALUE_CLASS);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(loadOpcode, slot);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, VALUE_CLASS, "<init>", "(I)V", false);
    }

    private void newValueConst(MethodVisitor mv, int constOpcode) {
        mv.visitTypeInsn(Opcodes.NEW, VALUE_CLASS);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(constOpcode);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, VALUE_CLASS, "<init>", "(I)V", false);
    }

    private Path writeClassFile(String internalName, ClassWriter cw) throws Exception {
        Path outputDir = Files.createTempDirectory("parparvm-scalar-replacement");
        Path classFile = outputDir.resolve(internalName + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, cw.toByteArray());
        return classFile;
    }
}
