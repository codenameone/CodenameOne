// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//
// Codename One modifications Copyright (c) 2026, Codename One and/or its
// affiliates, distributed under the license above.
//
// This file is a rewrite of ASM's org.objectweb.asm.MethodVisitor for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
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
