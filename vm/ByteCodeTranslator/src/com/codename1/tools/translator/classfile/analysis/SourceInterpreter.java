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
// This file is a rewrite of ASM's org.objectweb.asm.tree.analysis.SourceInterpreter for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile.analysis;

import com.codename1.tools.translator.classfile.ConstantDynamic;
import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs a method over {@link SourceValue}s: each value carries the set of
 * instructions that may have produced it, and a join is the union.
 */
public class SourceInterpreter extends Interpreter<SourceValue> {

    @Override
    public SourceValue newValue(Type type) {
        if (type == Type.VOID_TYPE) {
            return null;
        }
        return new SourceValue(type == null ? 1 : type.getSize());
    }

    @Override
    public SourceValue newOperation(AbstractInsnNode insn) {
        int size;
        switch (insn.getOpcode()) {
            case Opcodes.LCONST_0: case Opcodes.LCONST_1: case Opcodes.DCONST_0: case Opcodes.DCONST_1:
                size = 2;
                break;
            case Opcodes.LDC: {
                Object value = insn.getConstant();
                if (value instanceof ConstantDynamic) {
                    size = ((ConstantDynamic) value).getSize();
                } else {
                    size = value instanceof Long || value instanceof Double ? 2 : 1;
                }
                break;
            }
            case Opcodes.GETSTATIC:
                size = Type.getType(insn.getDescriptor()).getSize();
                break;
            default:
                size = 1;
        }
        return new SourceValue(size, insn);
    }

    @Override
    public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
        return new SourceValue(value.getSize(), insn);
    }

    @Override
    public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
        int size;
        switch (insn.getOpcode()) {
            case Opcodes.LNEG: case Opcodes.DNEG: case Opcodes.I2L: case Opcodes.I2D: case Opcodes.L2D:
            case Opcodes.F2L: case Opcodes.F2D: case Opcodes.D2L:
                size = 2;
                break;
            case Opcodes.GETFIELD:
                size = Type.getType(insn.getDescriptor()).getSize();
                break;
            default:
                size = 1;
        }
        return new SourceValue(size, insn);
    }

    @Override
    public SourceValue binaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2) {
        int size;
        switch (insn.getOpcode()) {
            case Opcodes.LALOAD: case Opcodes.DALOAD: case Opcodes.LADD: case Opcodes.DADD: case Opcodes.LSUB:
            case Opcodes.DSUB: case Opcodes.LMUL: case Opcodes.DMUL: case Opcodes.LDIV: case Opcodes.DDIV:
            case Opcodes.LREM: case Opcodes.DREM: case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR:
            case Opcodes.LAND: case Opcodes.LOR: case Opcodes.LXOR:
                size = 2;
                break;
            default:
                size = 1;
        }
        return new SourceValue(size, insn);
    }

    @Override
    public SourceValue ternaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2, SourceValue value3) {
        return new SourceValue(1, insn);
    }

    @Override
    public SourceValue naryOperation(AbstractInsnNode insn, List<? extends SourceValue> values) {
        int size;
        if (insn.getOpcode() == Opcodes.MULTIANEWARRAY) {
            size = 1;
        } else {
            size = Type.getReturnType(insn.getDescriptor()).getSize();
        }
        return new SourceValue(size, insn);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, SourceValue value, SourceValue expected) {
    }

    @Override
    public SourceValue merge(SourceValue value1, SourceValue value2) {
        if (value1.size == value2.size && value1.insns.containsAll(value2.insns)) {
            return value1;
        }
        Set<AbstractInsnNode> union = new LinkedHashSet<AbstractInsnNode>(value1.insns);
        union.addAll(value2.insns);
        return new SourceValue(Math.min(value1.size, value2.size), union);
    }
}
