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
package com.codename1.hardening;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * JDK 9+ javac compiles {@code "a" + b} to an {@code invokedynamic} bound to
 * {@code StringConcatFactory}, keeping the literal fragments in the bootstrap recipe rather than an
 * {@code LDC}. The transform cannot encrypt those, so it must at least count them so the engine can
 * warn. JDK 8 (this module's build JDK) never emits that shape, so the fixture is assembled directly.
 *
 * <p>Recipe markers: U+0001 is an ordinary argument slot, U+0002 a constant slot that
 * pulls from the bootstrap arguments after the recipe.
 */
public class ConcatLiteralDetectionTest {

    private static final char ARG = '\u0001';
    private static final char CONST = '\u0002';

    private static final Handle CONCAT_BSM = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/StringConcatFactory",
            "makeConcatWithConstants",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)"
                    + "Ljava/lang/invoke/CallSite;",
            false);

    /** A class with one literal-bearing concat, one constant-arg concat, and one pure-dynamic concat. */
    private static byte[] fixture() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/codename1/hardening/fixture/Concat",
                null, "java/lang/Object", null);

        // "secret=" + x -> recipe "secret=" + ARG, the literal embedded directly in the recipe.
        MethodVisitor m1 = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "withInlineLiteral",
                "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        m1.visitCode();
        m1.visitVarInsn(Opcodes.ALOAD, 0);
        m1.visitInvokeDynamicInsn("makeConcatWithConstants", "(Ljava/lang/String;)Ljava/lang/String;",
                CONCAT_BSM, new Object[] {"secret=" + ARG});
        m1.visitInsn(Opcodes.ARETURN);
        m1.visitMaxs(1, 1);
        m1.visitEnd();

        // x + "-sep-" + y -> recipe ARG + CONST + ARG with the constant passed as a bootstrap argument.
        MethodVisitor m2 = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "withConstantArg",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", null, null);
        m2.visitCode();
        m2.visitVarInsn(Opcodes.ALOAD, 0);
        m2.visitVarInsn(Opcodes.ALOAD, 1);
        m2.visitInvokeDynamicInsn("makeConcatWithConstants",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                CONCAT_BSM, new Object[] {"" + ARG + CONST + ARG, "-sep-"});
        m2.visitInsn(Opcodes.ARETURN);
        m2.visitMaxs(2, 2);
        m2.visitEnd();

        // x + y, no literal at all (recipe is just the two argument markers): must NOT be counted.
        MethodVisitor m3 = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "pureDynamic",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", null, null);
        m3.visitCode();
        m3.visitVarInsn(Opcodes.ALOAD, 0);
        m3.visitVarInsn(Opcodes.ALOAD, 1);
        m3.visitInvokeDynamicInsn("makeConcatWithConstants",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                CONCAT_BSM, new Object[] {"" + ARG + ARG});
        m3.visitInsn(Opcodes.ARETURN);
        m3.visitMaxs(2, 2);
        m3.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void countsLiteralBearingConcatSitesOnly() {
        StringEncryptTransform t = new StringEncryptTransform(true, 42);
        t.transform(fixture());
        // withInlineLiteral (recipe literal) + withConstantArg (String bootstrap arg) = 2; pureDynamic = 0.
        assertEquals(2, t.getConcatLiteralCount());
    }
}
