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
package com.codename1.tools.translator.classfile;

/**
 * Receives a method as {@link ClassReader} decodes it: parameters, the annotation
 * default, annotations, parameter annotations, then -- for a method with code --
 * {@code visitCode}, every try/catch block, the instructions in offset order with
 * each referenced label and its line numbers ahead of the instruction at that
 * offset, the local variables, {@code visitMaxs}; and finally {@code visitEnd}.
 *
 * <p>Instructions arrive in their canonical forms: {@code ILOAD_0} is
 * {@code visitVarInsn(ILOAD, 0)}, {@code GOTO_W} is {@code GOTO}, {@code LDC_W} and
 * {@code LDC2_W} are {@code LDC}, and a {@code WIDE} prefix is folded away.
 */
public abstract class MethodVisitor {
    protected MethodVisitor mv;

    protected MethodVisitor() {
    }

    protected MethodVisitor(MethodVisitor mv) {
        this.mv = mv;
    }

    public void visitParameter(String name, int access) {
        if (mv != null) {
            mv.visitParameter(name, access);
        }
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return mv != null ? mv.visitAnnotationDefault() : null;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return mv != null ? mv.visitAnnotation(desc, visible) : null;
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return mv != null ? mv.visitParameterAnnotation(parameter, desc, visible) : null;
    }

    public void visitCode() {
        if (mv != null) {
            mv.visitCode();
        }
    }

    public void visitInsn(int opcode) {
        if (mv != null) {
            mv.visitInsn(opcode);
        }
    }

    public void visitIntInsn(int opcode, int operand) {
        if (mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
    }

    public void visitVarInsn(int opcode, int var) {
        if (mv != null) {
            mv.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(int opcode, String type) {
        if (mv != null) {
            mv.visitTypeInsn(opcode, type);
        }
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        if (mv != null) {
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
    }

    public void visitJumpInsn(int opcode, Label label) {
        if (mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
    }

    public void visitLabel(Label label) {
        if (mv != null) {
            mv.visitLabel(label);
        }
    }

    public void visitLdcInsn(Object value) {
        if (mv != null) {
            mv.visitLdcInsn(value);
        }
    }

    public void visitIincInsn(int var, int increment) {
        if (mv != null) {
            mv.visitIincInsn(var, increment);
        }
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        if (mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        if (mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (mv != null) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
    }

    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (mv != null) {
            mv.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }

    public void visitLineNumber(int line, Label start) {
        if (mv != null) {
            mv.visitLineNumber(line, start);
        }
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        if (mv != null) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }

    public void visitEnd() {
        if (mv != null) {
            mv.visitEnd();
        }
    }
}
