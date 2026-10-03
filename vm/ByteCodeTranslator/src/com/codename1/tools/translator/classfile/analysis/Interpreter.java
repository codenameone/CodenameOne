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

import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import com.codename1.tools.translator.classfile.tree.TryCatchBlockNode;
import java.util.List;

/**
 * The value semantics an {@link Analyzer} runs a method under. {@link Frame}
 * decides which operation an instruction is; the interpreter decides what value it
 * produces, and how two values meeting at a join combine.
 */
public abstract class Interpreter<V extends Value> {

    /** A value of the given type, or the uninitialized value for null; null for void. */
    public abstract V newValue(Type type);

    public V newParameterValue(boolean isInstanceMethod, int local, Type type) {
        return newValue(type);
    }

    public V newReturnTypeValue(Type type) {
        return newValue(type);
    }

    public V newEmptyValue(int local) {
        return newValue(null);
    }

    public V newExceptionValue(TryCatchBlockNode tryCatchBlockNode, Frame<V> handlerFrame, Type exceptionType) {
        return newValue(exceptionType);
    }

    /** ACONST_NULL, xCONST_n, BIPUSH, SIPUSH, LDC, JSR, GETSTATIC, NEW. */
    public abstract V newOperation(AbstractInsnNode insn) throws AnalyzerException;

    /** xLOAD, xSTORE, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP. */
    public abstract V copyOperation(AbstractInsnNode insn, V value) throws AnalyzerException;

    /**
     * Negations, IINC, conversions, IFxx, switches, returns, PUTSTATIC, GETFIELD,
     * NEWARRAY, ANEWARRAY, ARRAYLENGTH, ATHROW, CHECKCAST, INSTANCEOF, monitors,
     * IFNULL, IFNONNULL.
     */
    public abstract V unaryOperation(AbstractInsnNode insn, V value) throws AnalyzerException;

    /** Array loads, binary arithmetic, comparisons, IF_xCMPxx, PUTFIELD. */
    public abstract V binaryOperation(AbstractInsnNode insn, V value1, V value2) throws AnalyzerException;

    /** Array stores. */
    public abstract V ternaryOperation(AbstractInsnNode insn, V value1, V value2, V value3) throws AnalyzerException;

    /** Invocations, INVOKEDYNAMIC, MULTIANEWARRAY. */
    public abstract V naryOperation(AbstractInsnNode insn, List<? extends V> values) throws AnalyzerException;

    /** xRETURN: the returned value against the method's return value. */
    public abstract void returnOperation(AbstractInsnNode insn, V value, V expected) throws AnalyzerException;

    /** The combination of two values at a join; returns {@code value1} when nothing changes. */
    public abstract V merge(V value1, V value2);
}
