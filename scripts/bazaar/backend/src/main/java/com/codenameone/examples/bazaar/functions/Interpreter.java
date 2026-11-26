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

/// Runs a verified [Program] over a [FunctionInput] in one loop.
///
/// Every instruction costs one unit of fuel; a run that spends its budget
/// stops with an error. Longs and booleans share a `long[]` stack and strings
/// a `String[]` beside it, indexed by the same stack pointer: the verifier
/// proved which one each slot is, so nothing is cast, boxed or allocated per
/// instruction. Arithmetic that would overflow, and division by zero, are
/// errors rather than whatever the CPU does with them.
public final class Interpreter {
    /// Instructions one run may execute.
    public static final int DEFAULT_FUEL = 50000;

    private Interpreter() {
    }

    /// Runs `p` over `in`, appending its operations to `out`.
    public static void run(Program p, FunctionInput in, int fuel, FunctionOutput out)
            throws FunctionException {
        if (in.target != p.target) {
            throw new FunctionException("The input is for another target");
        }
        final int[] code = p.code;
        final long[] longPool = p.longs;
        final String[] stringPool = p.strings;
        int locals = p.localTypes.length();
        long[] ln = new long[locals];
        String[] ls = new String[locals];
        for (int iter = 0; iter < locals; iter++) {
            ls[iter] = "";
        }
        long[] sn = new long[Program.MAX_STACK];
        String[] ss = new String[Program.MAX_STACK];
        int sp = -1;
        int pc = 0;
        int left = fuel;
        while (true) {
            if (--left < 0) {
                throw new FunctionException("The function ran out of fuel (" + fuel
                        + " instructions)");
            }
            int op = code[pc];
            switch (op) {
                case Program.HALT:
                    return;
                case Program.PUSH_L:
                    sn[++sp] = longPool[code[pc + 1]];
                    pc += 2;
                    break;
                case Program.PUSH_S:
                    ss[++sp] = stringPool[code[pc + 1]];
                    pc += 2;
                    break;
                case Program.LOAD: {
                    int at = code[pc + 1];
                    sp++;
                    sn[sp] = ln[at];
                    ss[sp] = ls[at];
                    pc += 2;
                    break;
                }
                case Program.STORE: {
                    int at = code[pc + 1];
                    ln[at] = sn[sp];
                    ls[at] = ss[sp];
                    sp--;
                    pc += 2;
                    break;
                }
                case Program.SCALAR: {
                    int slot = code[pc + 1];
                    sp++;
                    sn[sp] = in.longs[slot];
                    ss[sp] = in.strings[slot];
                    pc += 2;
                    break;
                }
                case Program.FIELD: {
                    int list = code[pc + 1];
                    int field = code[pc + 2];
                    long row = ln[code[pc + 3]];
                    if (row < 0 || row >= in.sizes[list]) {
                        throw new FunctionException("A field is read outside its loop");
                    }
                    sp++;
                    long[] numbers = in.numbers[list][field];
                    if (numbers != null) {
                        sn[sp] = numbers[(int) row];
                    } else {
                        ss[sp] = in.texts[list][field][(int) row];
                    }
                    pc += 4;
                    break;
                }
                case Program.ADD: {
                    long b = sn[sp--];
                    long a = sn[sp];
                    long r = a + b;
                    if (((a ^ r) & (b ^ r)) < 0) {
                        throw overflow();
                    }
                    sn[sp] = r;
                    pc++;
                    break;
                }
                case Program.SUB: {
                    long b = sn[sp--];
                    long a = sn[sp];
                    long r = a - b;
                    if (((a ^ b) & (a ^ r)) < 0) {
                        throw overflow();
                    }
                    sn[sp] = r;
                    pc++;
                    break;
                }
                case Program.MUL: {
                    long b = sn[sp--];
                    long a = sn[sp];
                    sn[sp] = multiply(a, b);
                    pc++;
                    break;
                }
                case Program.DIV:
                case Program.MOD: {
                    long b = sn[sp--];
                    long a = sn[sp];
                    // Never handed to the CPU: x/0 and MIN/-1 trap on some
                    // targets and answer garbage on others.
                    if (b == 0) {
                        throw new FunctionException("Division by zero");
                    }
                    if (b == -1) {
                        if (op == Program.DIV) {
                            if (a == Long.MIN_VALUE) {
                                throw overflow();
                            }
                            sn[sp] = -a;
                        } else {
                            sn[sp] = 0;
                        }
                    } else {
                        sn[sp] = op == Program.DIV ? a / b : a % b;
                    }
                    pc++;
                    break;
                }
                case Program.NEG:
                    if (sn[sp] == Long.MIN_VALUE) {
                        throw overflow();
                    }
                    sn[sp] = -sn[sp];
                    pc++;
                    break;
                case Program.MIN: {
                    long b = sn[sp--];
                    sn[sp] = Math.min(sn[sp], b);
                    pc++;
                    break;
                }
                case Program.MAX: {
                    long b = sn[sp--];
                    sn[sp] = Math.max(sn[sp], b);
                    pc++;
                    break;
                }
                case Program.LT: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] < b ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.LE: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] <= b ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.GT: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] > b ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.GE: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] >= b ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.EQ: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] == b ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.NE: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] != b ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.SEQ:
                case Program.SNE: {
                    String b = ss[sp--];
                    boolean same = ss[sp].equals(b);
                    sn[sp] = same == (op == Program.SEQ) ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.CONTAINS: {
                    String b = ss[sp--];
                    sn[sp] = ss[sp].indexOf(b) >= 0 ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.STARTS: {
                    String b = ss[sp--];
                    sn[sp] = ss[sp].startsWith(b) ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.AND: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] != 0 && b != 0 ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.OR: {
                    long b = sn[sp--];
                    sn[sp] = sn[sp] != 0 || b != 0 ? 1 : 0;
                    pc++;
                    break;
                }
                case Program.NOT:
                    sn[sp] = sn[sp] == 0 ? 1 : 0;
                    pc++;
                    break;
                case Program.JMP:
                    pc = code[pc + 1];
                    break;
                case Program.JZ:
                    pc = sn[sp--] == 0 ? code[pc + 1] : pc + 2;
                    break;
                case Program.FOR_INIT:
                    ln[code[pc + 1]] = 0;
                    pc += 2;
                    break;
                case Program.LOOP:
                    pc = ln[code[pc + 2]] >= in.sizes[code[pc + 1]] ? code[pc + 3] : pc + 4;
                    break;
                case Program.INC:
                    ln[code[pc + 1]]++;
                    pc += 2;
                    break;
                case Program.EMIT: {
                    int e = code[pc + 1];
                    String sig = p.target.signature(e);
                    int slot = out.add(e);
                    for (int a = sig.length() - 1; a >= 0; a--) {
                        if (sig.charAt(a) == Target.STRING) {
                            out.text(slot, a, ss[sp]);
                        } else {
                            out.number(slot, a, sn[sp]);
                        }
                        sp--;
                    }
                    pc += 2;
                    break;
                }
                default:
                    throw new FunctionException("Unknown opcode " + op);
            }
        }
    }

    private static long multiply(long a, long b) throws FunctionException {
        // MIN * -1 is checked first: dividing MIN by -1 traps on x86-64.
        if ((a == Long.MIN_VALUE && b == -1) || (b == Long.MIN_VALUE && a == -1)) {
            throw overflow();
        }
        long r = a * b;
        long ax = a < 0 ? -a : a;
        long bx = b < 0 ? -b : b;
        if ((ax | bx) >>> 31 != 0 && b != 0 && r / b != a) {
            throw overflow();
        }
        return r;
    }

    private static FunctionException overflow() {
        return new FunctionException("A number grew out of range");
    }
}
