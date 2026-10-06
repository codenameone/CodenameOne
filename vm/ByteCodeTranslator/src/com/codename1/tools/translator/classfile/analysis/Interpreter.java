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
// This file is a rewrite of ASM's org.objectweb.asm.tree.analysis.Interpreter for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
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
