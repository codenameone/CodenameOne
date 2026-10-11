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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class UnsupportedBootstrapTest {
    @AfterEach void cleanup() { Parser.cleanup(); }

    @Test void rejectsOnlySurvivingUnsupportedMethods(@TempDir Path dir) throws Exception {
        for (boolean constant : new boolean[] {false, true}) {
            Parser.cleanup();
            ClassWriter cw = new ClassWriter(0);
            cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Library", null, "java/lang/Object", null);
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "unsupported", "()V", null, null);
            mv.visitCode();
            Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "example/Bootstrap", "unknown", "()I", false);
            if (constant) mv.visitLdcInsn(new ConstantDynamic("dynamicValue", "I", bootstrap));
            else mv.visitInvokeDynamicInsn("dynamicValue", "()I", bootstrap);
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
            cw.visitEnd();
            Path file = dir.resolve("Library.class");
            Files.write(file, cw.toByteArray());
            // Parsing must tolerate the instruction until reachability is known.
            Parser.parse(file.toFile());
            BytecodeMethod method = Parser.getClassObject("Library").getMethods().get(0);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, method::verifySupportedBootstraps);
            assertTrue(error.getMessage().contains("Library.unsupported()V"), error.getMessage());
            assertTrue(error.getMessage().contains(constant ? "dynamic constant dynamicValue" : "example/Bootstrap.unknown"));
            method.setEliminated(true);
            assertDoesNotThrow(method::verifySupportedBootstraps);
        }
    }
}
