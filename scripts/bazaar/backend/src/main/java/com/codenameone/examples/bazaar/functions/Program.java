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

import com.codename1.backend.Json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A compiled function: a flat opcode array, its constant pools and the
/// type of every local. Made once by [FunctionCompiler], stored as JSON
/// ([#encode]) and checked by [Verifier] each time it is loaded.
///
/// Each instruction is its opcode followed by its operands ([#operands]).
/// Booleans travel as longs (0 or 1); strings in a parallel stack.
public final class Program {
    public static final int HALT = 0;
    public static final int PUSH_L = 1;
    public static final int PUSH_S = 2;
    public static final int LOAD = 3;
    public static final int STORE = 4;
    public static final int SCALAR = 5;
    /// `list, field, loopLocal`.
    public static final int FIELD = 6;
    public static final int ADD = 7;
    public static final int SUB = 8;
    public static final int MUL = 9;
    public static final int DIV = 10;
    public static final int MOD = 11;
    public static final int NEG = 12;
    public static final int MIN = 13;
    public static final int MAX = 14;
    public static final int LT = 15;
    public static final int LE = 16;
    public static final int GT = 17;
    public static final int GE = 18;
    public static final int EQ = 19;
    public static final int NE = 20;
    public static final int SEQ = 21;
    public static final int SNE = 22;
    public static final int CONTAINS = 23;
    public static final int STARTS = 24;
    public static final int AND = 25;
    public static final int OR = 26;
    public static final int NOT = 27;
    public static final int JMP = 28;
    /// Pops a boolean; jumps when it is false.
    public static final int JZ = 29;
    /// `local`: sets a loop index to zero.
    public static final int FOR_INIT = 30;
    /// `list, local, target`: jumps to `target` once the index is past the list.
    public static final int LOOP = 31;
    /// `local`: adds one to a loop index.
    public static final int INC = 32;
    /// `emit`: pops the emit's arguments ([Target#signature]).
    public static final int EMIT = 33;
    static final int OPCODES = 34;

    private static final int[] OPERANDS = {0, 1, 1, 1, 1, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 3, 1, 1};

    /// The deepest the stack may get.
    public static final int MAX_STACK = 32;
    /// The most locals a program may have.
    public static final int MAX_LOCALS = 64;
    /// The longest program, in ints.
    public static final int MAX_CODE = 8192;

    public final Target target;
    final int[] code;
    final long[] longs;
    final String[] strings;
    /// One letter per local: `L`, `S` or `B`.
    final String localTypes;

    Program(Target target, int[] code, long[] longs, String[] strings, String localTypes) {
        this.target = target;
        this.code = code;
        this.longs = longs;
        this.strings = strings;
        this.localTypes = localTypes;
    }

    /// The operand count of `opcode`, or -1 for no such opcode.
    static int operands(int opcode) {
        return opcode < 0 || opcode >= OPCODES ? -1 : OPERANDS[opcode];
    }

    public int codeLength() {
        return code.length;
    }

    /// The program as JSON, for storage.
    public String encode() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("target", target.name);
        List<Object> c = new ArrayList<Object>();
        for (int iter = 0; iter < code.length; iter++) {
            c.add(Integer.valueOf(code[iter]));
        }
        m.put("code", c);
        List<Object> l = new ArrayList<Object>();
        for (int iter = 0; iter < longs.length; iter++) {
            l.add(Long.valueOf(longs[iter]));
        }
        m.put("longs", l);
        List<Object> s = new ArrayList<Object>();
        for (int iter = 0; iter < strings.length; iter++) {
            s.add(strings[iter]);
        }
        m.put("strings", s);
        m.put("locals", localTypes);
        return Json.write(m);
    }

    /// A stored program, verified; throws when it is malformed or unsafe.
    public static Program decode(String json) throws FunctionException {
        Map m;
        try {
            m = Json.parseObject(json);
        } catch (IOException bad) {
            throw new FunctionException("The stored program is not readable");
        }
        if (m == null) {
            throw new FunctionException("The stored program is empty");
        }
        Object t = m.get("target");
        Target target = t instanceof String ? Target.named((String) t) : null;
        Object locals = m.get("locals");
        if (target == null || !(locals instanceof String)) {
            throw new FunctionException("The stored program names no target");
        }
        long[] c = numbers(m.get("code"), MAX_CODE);
        int[] code = new int[c.length];
        for (int iter = 0; iter < c.length; iter++) {
            if (c[iter] < Integer.MIN_VALUE || c[iter] > Integer.MAX_VALUE) {
                throw new FunctionException("The stored program is malformed");
            }
            code[iter] = (int) c[iter];
        }
        long[] longs = numbers(m.get("longs"), MAX_CODE);
        Object s = m.get("strings");
        if (!(s instanceof List)) {
            throw new FunctionException("The stored program is malformed");
        }
        List sl = (List) s;
        String[] strings = new String[sl.size()];
        for (int iter = 0; iter < strings.length; iter++) {
            Object v = sl.get(iter);
            if (!(v instanceof String)) {
                throw new FunctionException("The stored program is malformed");
            }
            strings[iter] = (String) v;
        }
        Program p = new Program(target, code, longs, strings, (String) locals);
        Verifier.verify(p);
        return p;
    }

    private static long[] numbers(Object value, int max) throws FunctionException {
        if (!(value instanceof List) || ((List) value).size() > max) {
            throw new FunctionException("The stored program is malformed");
        }
        List list = (List) value;
        long[] out = new long[list.size()];
        for (int iter = 0; iter < out.length; iter++) {
            Object v = list.get(iter);
            if (!(v instanceof Number)) {
                throw new FunctionException("The stored program is malformed");
            }
            out[iter] = ((Number) v).longValue();
        }
        return out;
    }
}
