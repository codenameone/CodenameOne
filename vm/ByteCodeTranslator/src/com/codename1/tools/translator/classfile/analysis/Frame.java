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
// This file is a rewrite of ASM's org.objectweb.asm.tree.analysis.Frame for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile.analysis;

import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import java.util.ArrayList;
import java.util.List;

/**
 * The locals and operand stack before an instruction. A long or double is ONE
 * stack entry whose size is 2, so the category-sensitive stack instructions
 * ({@code POP2}, {@code DUP2} and the {@code _X} forms) are resolved here by size.
 */
public class Frame<V extends Value> {
    private V returnValue;
    private final V[] values;
    private final int numLocals;
    private int numStack;

    @SuppressWarnings("unchecked")
    public Frame(int numLocals, int maxStack) {
        this.values = (V[]) new Value[numLocals + maxStack];
        this.numLocals = numLocals;
    }

    public Frame(Frame<? extends V> frame) {
        this(frame.numLocals, frame.values.length - frame.numLocals);
        init(frame);
    }

    public Frame<V> init(Frame<? extends V> frame) {
        returnValue = frame.returnValue;
        System.arraycopy(frame.values, 0, values, 0, values.length);
        numStack = frame.numStack;
        return this;
    }

    public void setReturn(V v) {
        returnValue = v;
    }

    public int getLocals() {
        return numLocals;
    }

    public int getMaxStackSize() {
        return values.length - numLocals;
    }

    public V getLocal(int index) {
        if (index >= numLocals) {
            throw new IndexOutOfBoundsException("Trying to get an inexistant local variable " + index);
        }
        return values[index];
    }

    public void setLocal(int index, V value) {
        if (index >= numLocals) {
            throw new IndexOutOfBoundsException("Trying to set an inexistant local variable " + index);
        }
        values[index] = value;
    }

    public int getStackSize() {
        return numStack;
    }

    public V getStack(int index) {
        return values[numLocals + index];
    }

    public void clearStack() {
        numStack = 0;
    }

    public V pop() {
        if (numStack == 0) {
            throw new IndexOutOfBoundsException("Cannot pop operand off an empty stack.");
        }
        return values[numLocals + --numStack];
    }

    public void push(V value) {
        if (numLocals + numStack >= values.length) {
            throw new IndexOutOfBoundsException("Insufficient maximum stack size.");
        }
        values[numLocals + numStack++] = value;
    }

    /** Merges another frame into this one; returns whether anything changed. */
    public boolean merge(Frame<? extends V> frame, Interpreter<V> interpreter) throws AnalyzerException {
        if (numStack != frame.numStack) {
            throw new AnalyzerException(null, "Incompatible stack heights");
        }
        boolean changed = false;
        for (int i = 0; i < numLocals + numStack; i++) {
            V v = interpreter.merge(values[i], frame.values[i]);
            if (!v.equals(values[i])) {
                values[i] = v;
                changed = true;
            }
        }
        return changed;
    }

    private void store(AbstractInsnNode insn, int var, Interpreter<V> interpreter) throws AnalyzerException {
        V value = interpreter.copyOperation(insn, pop());
        setLocal(var, value);
        if (value.getSize() == 2) {
            setLocal(var + 1, interpreter.newEmptyValue(var + 1));
        }
        if (var > 0) {
            Value previous = getLocal(var - 1);
            if (previous != null && previous.getSize() == 2) {
                setLocal(var - 1, interpreter.newEmptyValue(var - 1));
            }
        }
    }

    private void illegal(AbstractInsnNode insn, String what) throws AnalyzerException {
        throw new AnalyzerException(insn, "Illegal use of " + what);
    }

    /** Applies one instruction to this frame. */
    public void execute(AbstractInsnNode insn, Interpreter<V> interpreter) throws AnalyzerException {
        V value1;
        V value2;
        V value3;
        V value4;
        int op = insn.getOpcode();
        switch (op) {
            case Opcodes.NOP:
                break;
            case Opcodes.ACONST_NULL: case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1:
            case Opcodes.ICONST_2: case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
            case Opcodes.LCONST_0: case Opcodes.LCONST_1: case Opcodes.FCONST_0: case Opcodes.FCONST_1:
            case Opcodes.FCONST_2: case Opcodes.DCONST_0: case Opcodes.DCONST_1: case Opcodes.BIPUSH:
            case Opcodes.SIPUSH: case Opcodes.LDC:
                push(interpreter.newOperation(insn));
                break;
            case Opcodes.ILOAD: case Opcodes.LLOAD: case Opcodes.FLOAD: case Opcodes.DLOAD: case Opcodes.ALOAD:
                push(interpreter.copyOperation(insn, getLocal(insn.getLocal())));
                break;
            case Opcodes.ISTORE: case Opcodes.LSTORE: case Opcodes.FSTORE: case Opcodes.DSTORE: case Opcodes.ASTORE:
                store(insn, insn.getLocal(), interpreter);
                break;
            case Opcodes.IALOAD: case Opcodes.LALOAD: case Opcodes.FALOAD: case Opcodes.DALOAD:
            case Opcodes.AALOAD: case Opcodes.BALOAD: case Opcodes.CALOAD: case Opcodes.SALOAD:
                value2 = pop();
                value1 = pop();
                push(interpreter.binaryOperation(insn, value1, value2));
                break;
            case Opcodes.IASTORE: case Opcodes.LASTORE: case Opcodes.FASTORE: case Opcodes.DASTORE:
            case Opcodes.AASTORE: case Opcodes.BASTORE: case Opcodes.CASTORE: case Opcodes.SASTORE:
                value3 = pop();
                value2 = pop();
                value1 = pop();
                interpreter.ternaryOperation(insn, value1, value2, value3);
                break;
            case Opcodes.POP:
                if (pop().getSize() == 2) {
                    illegal(insn, "POP");
                }
                break;
            case Opcodes.POP2:
                if (pop().getSize() == 1 && pop().getSize() != 1) {
                    illegal(insn, "POP2");
                }
                break;
            case Opcodes.DUP:
                value1 = pop();
                if (value1.getSize() != 1) {
                    illegal(insn, "DUP");
                }
                push(value1);
                push(interpreter.copyOperation(insn, value1));
                break;
            case Opcodes.DUP_X1:
                value1 = pop();
                value2 = pop();
                if (value1.getSize() != 1 || value2.getSize() != 1) {
                    illegal(insn, "DUP_X1");
                }
                push(interpreter.copyOperation(insn, value1));
                push(value2);
                push(value1);
                break;
            case Opcodes.DUP_X2:
                value1 = pop();
                if (value1.getSize() == 1 && executeDupX2(insn, value1, interpreter)) {
                    break;
                }
                illegal(insn, "DUP_X2");
                break;
            case Opcodes.DUP2:
                value1 = pop();
                if (value1.getSize() == 1) {
                    value2 = pop();
                    if (value2.getSize() == 1) {
                        push(value2);
                        push(value1);
                        push(interpreter.copyOperation(insn, value2));
                        push(interpreter.copyOperation(insn, value1));
                        break;
                    }
                } else {
                    push(value1);
                    push(interpreter.copyOperation(insn, value1));
                    break;
                }
                illegal(insn, "DUP2");
                break;
            case Opcodes.DUP2_X1:
                value1 = pop();
                if (value1.getSize() == 1) {
                    value2 = pop();
                    if (value2.getSize() == 1) {
                        value3 = pop();
                        if (value3.getSize() == 1) {
                            push(interpreter.copyOperation(insn, value2));
                            push(interpreter.copyOperation(insn, value1));
                            push(value3);
                            push(value2);
                            push(value1);
                            break;
                        }
                    }
                } else {
                    value2 = pop();
                    if (value2.getSize() == 1) {
                        push(interpreter.copyOperation(insn, value1));
                        push(value2);
                        push(value1);
                        break;
                    }
                }
                illegal(insn, "DUP2_X1");
                break;
            case Opcodes.DUP2_X2:
                value1 = pop();
                if (value1.getSize() == 1) {
                    value2 = pop();
                    if (value2.getSize() == 1) {
                        value3 = pop();
                        if (value3.getSize() == 1) {
                            value4 = pop();
                            if (value4.getSize() == 1) {
                                push(interpreter.copyOperation(insn, value2));
                                push(interpreter.copyOperation(insn, value1));
                                push(value4);
                                push(value3);
                                push(value2);
                                push(value1);
                                break;
                            }
                        } else {
                            push(interpreter.copyOperation(insn, value2));
                            push(interpreter.copyOperation(insn, value1));
                            push(value3);
                            push(value2);
                            push(value1);
                            break;
                        }
                    }
                } else if (executeDupX2(insn, value1, interpreter)) {
                    break;
                }
                illegal(insn, "DUP2_X2");
                break;
            case Opcodes.SWAP:
                value2 = pop();
                value1 = pop();
                if (value1.getSize() != 1 || value2.getSize() != 1) {
                    illegal(insn, "SWAP");
                }
                push(interpreter.copyOperation(insn, value2));
                push(interpreter.copyOperation(insn, value1));
                break;
            case Opcodes.IADD: case Opcodes.LADD: case Opcodes.FADD: case Opcodes.DADD:
            case Opcodes.ISUB: case Opcodes.LSUB: case Opcodes.FSUB: case Opcodes.DSUB:
            case Opcodes.IMUL: case Opcodes.LMUL: case Opcodes.FMUL: case Opcodes.DMUL:
            case Opcodes.IDIV: case Opcodes.LDIV: case Opcodes.FDIV: case Opcodes.DDIV:
            case Opcodes.IREM: case Opcodes.LREM: case Opcodes.FREM: case Opcodes.DREM:
            case Opcodes.ISHL: case Opcodes.LSHL: case Opcodes.ISHR: case Opcodes.LSHR:
            case Opcodes.IUSHR: case Opcodes.LUSHR: case Opcodes.IAND: case Opcodes.LAND:
            case Opcodes.IOR: case Opcodes.LOR: case Opcodes.IXOR: case Opcodes.LXOR:
            case Opcodes.LCMP: case Opcodes.FCMPL: case Opcodes.FCMPG: case Opcodes.DCMPL: case Opcodes.DCMPG:
                value2 = pop();
                value1 = pop();
                push(interpreter.binaryOperation(insn, value1, value2));
                break;
            case Opcodes.INEG: case Opcodes.LNEG: case Opcodes.FNEG: case Opcodes.DNEG:
            case Opcodes.I2L: case Opcodes.I2F: case Opcodes.I2D: case Opcodes.L2I: case Opcodes.L2F:
            case Opcodes.L2D: case Opcodes.F2I: case Opcodes.F2L: case Opcodes.F2D: case Opcodes.D2I:
            case Opcodes.D2L: case Opcodes.D2F: case Opcodes.I2B: case Opcodes.I2C: case Opcodes.I2S:
                push(interpreter.unaryOperation(insn, pop()));
                break;
            case Opcodes.IINC: {
                int var = insn.getLocal();
                setLocal(var, interpreter.unaryOperation(insn, getLocal(var)));
                break;
            }
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE: case Opcodes.IFGT:
            case Opcodes.IFLE: case Opcodes.TABLESWITCH: case Opcodes.LOOKUPSWITCH: case Opcodes.PUTSTATIC:
            case Opcodes.ATHROW: case Opcodes.MONITORENTER: case Opcodes.MONITOREXIT: case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                interpreter.unaryOperation(insn, pop());
                break;
            case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE: case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
            case Opcodes.PUTFIELD:
                value2 = pop();
                value1 = pop();
                interpreter.binaryOperation(insn, value1, value2);
                break;
            case Opcodes.GOTO:
                break;
            case Opcodes.JSR:
                push(interpreter.newOperation(insn));
                break;
            case Opcodes.RET:
                break;
            case Opcodes.IRETURN: case Opcodes.LRETURN: case Opcodes.FRETURN: case Opcodes.DRETURN:
            case Opcodes.ARETURN:
                value1 = pop();
                interpreter.unaryOperation(insn, value1);
                interpreter.returnOperation(insn, value1, returnValue);
                break;
            case Opcodes.RETURN:
                if (returnValue != null) {
                    throw new AnalyzerException(insn, "Incompatible return type");
                }
                break;
            case Opcodes.GETSTATIC:
                push(interpreter.newOperation(insn));
                break;
            case Opcodes.GETFIELD:
                push(interpreter.unaryOperation(insn, pop()));
                break;
            case Opcodes.INVOKEVIRTUAL: case Opcodes.INVOKESPECIAL: case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEINTERFACE:
                executeInvoke(insn, insn.getDescriptor(), op != Opcodes.INVOKESTATIC, interpreter);
                break;
            case Opcodes.INVOKEDYNAMIC:
                executeInvoke(insn, insn.getDescriptor(), false, interpreter);
                break;
            case Opcodes.NEW:
                push(interpreter.newOperation(insn));
                break;
            case Opcodes.NEWARRAY: case Opcodes.ANEWARRAY: case Opcodes.ARRAYLENGTH: case Opcodes.CHECKCAST:
            case Opcodes.INSTANCEOF:
                push(interpreter.unaryOperation(insn, pop()));
                break;
            case Opcodes.MULTIANEWARRAY: {
                List<V> dims = new ArrayList<V>();
                for (int i = insn.getDimensions(); i > 0; i--) {
                    dims.add(0, pop());
                }
                push(interpreter.naryOperation(insn, dims));
                break;
            }
            default:
                throw new AnalyzerException(insn, "Illegal opcode " + op);
        }
    }

    /** The size-1-on-top half of DUP_X2 and DUP2_X2: returns false if the shape is illegal. */
    private boolean executeDupX2(AbstractInsnNode insn, V value1, Interpreter<V> interpreter) throws AnalyzerException {
        V value2 = pop();
        if (value2.getSize() == 1) {
            V value3 = pop();
            if (value3.getSize() == 1) {
                push(interpreter.copyOperation(insn, value1));
                push(value3);
                push(value2);
                push(value1);
                return true;
            }
        } else {
            push(interpreter.copyOperation(insn, value1));
            push(value2);
            push(value1);
            return true;
        }
        return false;
    }

    private void executeInvoke(AbstractInsnNode insn, String desc, boolean hasReceiver, Interpreter<V> interpreter)
            throws AnalyzerException {
        List<V> args = new ArrayList<V>();
        for (int i = Type.getArgumentTypes(desc).length; i > 0; i--) {
            args.add(0, pop());
        }
        if (hasReceiver) {
            args.add(0, pop());
        }
        if (Type.getReturnType(desc) == Type.VOID_TYPE) {
            interpreter.naryOperation(insn, args);
        } else {
            push(interpreter.naryOperation(insn, args));
        }
    }
}
