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
// This file is a rewrite of ASM's org.objectweb.asm.tree.analysis.BasicInterpreter for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile.analysis;

import com.codename1.tools.translator.classfile.ConstantDynamic;
import com.codename1.tools.translator.classfile.Handle;
import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import java.util.List;

/** Runs a method over {@link BasicValue}s: every value reduced to its category. */
public class BasicInterpreter extends Interpreter<BasicValue> {

    @Override
    public BasicValue newValue(Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return BasicValue.INT_VALUE;
            case Type.FLOAT:
                return BasicValue.FLOAT_VALUE;
            case Type.LONG:
                return BasicValue.LONG_VALUE;
            case Type.DOUBLE:
                return BasicValue.DOUBLE_VALUE;
            default:
                return BasicValue.REFERENCE_VALUE;
        }
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case Opcodes.ACONST_NULL:
                return BasicValue.REFERENCE_VALUE;
            case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1: case Opcodes.ICONST_2:
            case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
            case Opcodes.BIPUSH: case Opcodes.SIPUSH:
                return BasicValue.INT_VALUE;
            case Opcodes.LCONST_0: case Opcodes.LCONST_1:
                return BasicValue.LONG_VALUE;
            case Opcodes.FCONST_0: case Opcodes.FCONST_1: case Opcodes.FCONST_2:
                return BasicValue.FLOAT_VALUE;
            case Opcodes.DCONST_0: case Opcodes.DCONST_1:
                return BasicValue.DOUBLE_VALUE;
            case Opcodes.LDC: {
                Object value = insn.getConstant();
                if (value instanceof Integer) {
                    return BasicValue.INT_VALUE;
                } else if (value instanceof Float) {
                    return BasicValue.FLOAT_VALUE;
                } else if (value instanceof Long) {
                    return BasicValue.LONG_VALUE;
                } else if (value instanceof Double) {
                    return BasicValue.DOUBLE_VALUE;
                } else if (value instanceof String || value instanceof Type || value instanceof Handle) {
                    return BasicValue.REFERENCE_VALUE;
                } else if (value instanceof ConstantDynamic) {
                    return newValue(Type.getType(((ConstantDynamic) value).getDescriptor()));
                }
                throw new AnalyzerException(insn, "Illegal LDC value " + value);
            }
            case Opcodes.JSR:
                return BasicValue.RETURNADDRESS_VALUE;
            case Opcodes.GETSTATIC:
                return newValue(Type.getType(insn.getDescriptor()));
            case Opcodes.NEW:
                return BasicValue.REFERENCE_VALUE;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) {
        return value;
    }

    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case Opcodes.INEG: case Opcodes.IINC: case Opcodes.L2I: case Opcodes.F2I: case Opcodes.D2I:
            case Opcodes.I2B: case Opcodes.I2C: case Opcodes.I2S: case Opcodes.ARRAYLENGTH: case Opcodes.INSTANCEOF:
                return BasicValue.INT_VALUE;
            case Opcodes.FNEG: case Opcodes.I2F: case Opcodes.L2F: case Opcodes.D2F:
                return BasicValue.FLOAT_VALUE;
            case Opcodes.LNEG: case Opcodes.I2L: case Opcodes.F2L: case Opcodes.D2L:
                return BasicValue.LONG_VALUE;
            case Opcodes.DNEG: case Opcodes.I2D: case Opcodes.L2D: case Opcodes.F2D:
                return BasicValue.DOUBLE_VALUE;
            case Opcodes.GETFIELD:
                return newValue(Type.getType(insn.getDescriptor()));
            case Opcodes.NEWARRAY: case Opcodes.ANEWARRAY: case Opcodes.CHECKCAST:
                return BasicValue.REFERENCE_VALUE;
            default:
                return null;
        }
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) {
        switch (insn.getOpcode()) {
            case Opcodes.IALOAD: case Opcodes.BALOAD: case Opcodes.CALOAD: case Opcodes.SALOAD:
            case Opcodes.IADD: case Opcodes.ISUB: case Opcodes.IMUL: case Opcodes.IDIV: case Opcodes.IREM:
            case Opcodes.ISHL: case Opcodes.ISHR: case Opcodes.IUSHR: case Opcodes.IAND: case Opcodes.IOR:
            case Opcodes.IXOR: case Opcodes.LCMP: case Opcodes.FCMPL: case Opcodes.FCMPG: case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return BasicValue.INT_VALUE;
            case Opcodes.FALOAD: case Opcodes.FADD: case Opcodes.FSUB: case Opcodes.FMUL: case Opcodes.FDIV:
            case Opcodes.FREM:
                return BasicValue.FLOAT_VALUE;
            case Opcodes.LALOAD: case Opcodes.LADD: case Opcodes.LSUB: case Opcodes.LMUL: case Opcodes.LDIV:
            case Opcodes.LREM: case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR: case Opcodes.LAND:
            case Opcodes.LOR: case Opcodes.LXOR:
                return BasicValue.LONG_VALUE;
            case Opcodes.DALOAD: case Opcodes.DADD: case Opcodes.DSUB: case Opcodes.DMUL: case Opcodes.DDIV:
            case Opcodes.DREM:
                return BasicValue.DOUBLE_VALUE;
            case Opcodes.AALOAD:
                return BasicValue.REFERENCE_VALUE;
            default:
                return null;
        }
    }

    @Override
    public BasicValue ternaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3) {
        return null;
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) {
        if (insn.getOpcode() == Opcodes.MULTIANEWARRAY) {
            return newValue(Type.getType(insn.getDescriptor()));
        }
        return newValue(Type.getReturnType(insn.getDescriptor()));
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, BasicValue value, BasicValue expected) {
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        if (!value1.equals(value2)) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        return value1;
    }
}
