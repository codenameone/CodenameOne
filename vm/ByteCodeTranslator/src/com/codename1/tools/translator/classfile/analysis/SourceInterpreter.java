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
