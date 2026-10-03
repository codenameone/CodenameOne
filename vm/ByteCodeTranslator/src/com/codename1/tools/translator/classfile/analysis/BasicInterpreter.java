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
