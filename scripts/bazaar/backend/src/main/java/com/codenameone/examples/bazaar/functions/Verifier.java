/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.functions;

/// Proves a [Program] safe to run before it ever runs, whoever made it:
///
/// - every opcode is known and its operands are inside the code;
/// - every jump lands on the start of an instruction;
/// - every pool index, local, scalar, list and field exists in its target,
///   and every emit is one of its target's;
/// - the type of every stack slot is the same on every path into an
///   instruction, every operation gets the types it needs, and the stack
///   never grows past [Program#MAX_STACK] nor pops below empty;
/// - no path runs off the end of the code.
///
/// So the interpreter checks none of it, and needs no casts: the only
/// failures left at run time are fuel, output, arithmetic and a loop index
/// past its list.
final class Verifier {
    private static final int T_LONG = 1;
    private static final int T_STRING = 2;
    private static final int T_BOOL = 3;

    private Verifier() {
    }

    private static int typeOf(char c) {
        if (c == Target.LONG) {
            return T_LONG;
        }
        if (c == Target.STRING) {
            return T_STRING;
        }
        return c == Target.BOOL ? T_BOOL : 0;
    }

    static void verify(Program p) throws FunctionException {
        int[] code = p.code;
        int n = code.length;
        if (n == 0 || n > Program.MAX_CODE) {
            throw new FunctionException("The program is empty or too long");
        }
        if (p.localTypes.length() > Program.MAX_LOCALS) {
            throw new FunctionException("The program has too many locals");
        }
        for (int iter = 0; iter < p.localTypes.length(); iter++) {
            if (typeOf(p.localTypes.charAt(iter)) == 0) {
                throw new FunctionException("A local has no type");
            }
        }
        boolean[] start = new boolean[n];
        int pc = 0;
        while (pc < n) {
            int ops = Program.operands(code[pc]);
            if (ops < 0) {
                throw new FunctionException("Unknown opcode " + code[pc] + " at " + pc);
            }
            start[pc] = true;
            pc += 1 + ops;
        }
        if (pc != n) {
            throw new FunctionException("The last instruction is cut short");
        }
        int[] depth = new int[n];
        long[] types = new long[n];
        for (int iter = 0; iter < n; iter++) {
            depth[iter] = -1;
        }
        int[] work = new int[n];
        int pending = 0;
        depth[0] = 0;
        work[pending++] = 0;
        while (pending > 0) {
            pc = work[--pending];
            int d = depth[pc];
            long t = types[pc];
            int op = code[pc];
            int next = pc + 1 + Program.operands(op);
            int jump = -1;
            boolean falls = true;
            switch (op) {
                case Program.HALT:
                    falls = false;
                    break;
                case Program.PUSH_L:
                    pool(code[pc + 1], p.longs.length);
                    t = push(t, d++, T_LONG);
                    break;
                case Program.PUSH_S:
                    pool(code[pc + 1], p.strings.length);
                    t = push(t, d++, T_STRING);
                    break;
                case Program.LOAD:
                    t = push(t, d++, typeOf(local(p, code[pc + 1])));
                    break;
                case Program.STORE:
                    t = pop(t, --d, typeOf(local(p, code[pc + 1])), pc);
                    break;
                case Program.SCALAR: {
                    int slot = code[pc + 1];
                    if (slot < 0 || slot >= Target.SCALARS.length) {
                        throw new FunctionException("No such scalar at " + pc);
                    }
                    t = push(t, d++, typeOf(Target.scalarType(slot)));
                    break;
                }
                case Program.FIELD: {
                    int list = code[pc + 1];
                    int field = code[pc + 2];
                    if (list < 0 || list >= p.target.listCount() || field < 0
                            || field >= p.target.fieldCount(list)) {
                        throw new FunctionException("No such field at " + pc);
                    }
                    if (typeOf(local(p, code[pc + 3])) != T_LONG) {
                        throw new FunctionException("A field is read with no loop index at " + pc);
                    }
                    t = push(t, d++, typeOf(p.target.fieldType(list, field)));
                    break;
                }
                case Program.ADD:
                case Program.SUB:
                case Program.MUL:
                case Program.DIV:
                case Program.MOD:
                case Program.MIN:
                case Program.MAX:
                    t = pop(t, --d, T_LONG, pc);
                    t = pop(t, --d, T_LONG, pc);
                    t = push(t, d++, T_LONG);
                    break;
                case Program.NEG:
                    t = pop(t, --d, T_LONG, pc);
                    t = push(t, d++, T_LONG);
                    break;
                case Program.LT:
                case Program.LE:
                case Program.GT:
                case Program.GE:
                case Program.EQ:
                case Program.NE:
                    t = pop(t, --d, T_LONG, pc);
                    t = pop(t, --d, T_LONG, pc);
                    t = push(t, d++, T_BOOL);
                    break;
                case Program.SEQ:
                case Program.SNE:
                case Program.CONTAINS:
                case Program.STARTS:
                    t = pop(t, --d, T_STRING, pc);
                    t = pop(t, --d, T_STRING, pc);
                    t = push(t, d++, T_BOOL);
                    break;
                case Program.AND:
                case Program.OR:
                    t = pop(t, --d, T_BOOL, pc);
                    t = pop(t, --d, T_BOOL, pc);
                    t = push(t, d++, T_BOOL);
                    break;
                case Program.NOT:
                    t = pop(t, --d, T_BOOL, pc);
                    t = push(t, d++, T_BOOL);
                    break;
                case Program.JMP:
                    jump = code[pc + 1];
                    falls = false;
                    break;
                case Program.JZ:
                    t = pop(t, --d, T_BOOL, pc);
                    jump = code[pc + 1];
                    break;
                case Program.FOR_INIT:
                case Program.INC:
                    if (typeOf(local(p, code[pc + 1])) != T_LONG) {
                        throw new FunctionException("A loop index is not a number at " + pc);
                    }
                    break;
                case Program.LOOP: {
                    int list = code[pc + 1];
                    if (list < 0 || list >= p.target.listCount()
                            || typeOf(local(p, code[pc + 2])) != T_LONG) {
                        throw new FunctionException("A loop has no list at " + pc);
                    }
                    jump = code[pc + 3];
                    break;
                }
                case Program.EMIT: {
                    int e = code[pc + 1];
                    if (e < 0 || e >= p.target.emits.length) {
                        throw new FunctionException("No such emit at " + pc);
                    }
                    String sig = p.target.signature(e);
                    for (int a = sig.length() - 1; a >= 0; a--) {
                        t = pop(t, --d, typeOf(sig.charAt(a)), pc);
                    }
                    break;
                }
                default:
                    throw new FunctionException("Unknown opcode at " + pc);
            }
            if (d < 0) {
                throw new FunctionException("The stack runs empty at " + pc);
            }
            if (d > Program.MAX_STACK) {
                throw new FunctionException("The stack grows too deep at " + pc);
            }
            if (jump >= 0 || op == Program.JMP || op == Program.JZ || op == Program.LOOP) {
                if (jump < 0 || jump >= n || !start[jump]) {
                    throw new FunctionException("A jump lands outside the code at " + pc);
                }
                pending = merge(jump, d, t, depth, types, work, pending);
            }
            if (falls) {
                if (next >= n) {
                    throw new FunctionException("The program runs off its end");
                }
                pending = merge(next, d, t, depth, types, work, pending);
            }
        }
    }

    private static int merge(int at, int d, long t, int[] depth, long[] types, int[] work,
            int pending) throws FunctionException {
        if (depth[at] < 0) {
            depth[at] = d;
            types[at] = t;
            work[pending++] = at;
        } else if (depth[at] != d || types[at] != t) {
            throw new FunctionException("Two paths meet with different stacks at " + at);
        }
        return pending;
    }

    private static char local(Program p, int index) throws FunctionException {
        if (index < 0 || index >= p.localTypes.length()) {
            throw new FunctionException("No such local " + index);
        }
        return p.localTypes.charAt(index);
    }

    private static void pool(int index, int size) throws FunctionException {
        if (index < 0 || index >= size) {
            throw new FunctionException("No such constant " + index);
        }
    }

    private static long push(long t, int at, int type) throws FunctionException {
        if (at >= Program.MAX_STACK) {
            throw new FunctionException("The stack grows too deep");
        }
        long mask = 3L << (2 * at);
        return (t & ~mask) | ((long) type << (2 * at));
    }

    private static long pop(long t, int at, int type, int pc) throws FunctionException {
        if (at < 0) {
            throw new FunctionException("The stack runs empty at " + pc);
        }
        if (((t >>> (2 * at)) & 3L) != type) {
            throw new FunctionException("A value has the wrong type at " + pc);
        }
        return t & ~(3L << (2 * at));
    }
}
