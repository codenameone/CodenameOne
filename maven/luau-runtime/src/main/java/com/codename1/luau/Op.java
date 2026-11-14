/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.luau;

/// The instruction set. Each opcode is followed in the code array by the
/// operands named in its comment.
final class Op {
    private Op() {
    }

    static final int NIL = 0;
    static final int TRUE = 1;
    static final int FALSE = 2;
    /// const index
    static final int K = 3;
    /// slot
    static final int LGET = 4;
    /// slot
    static final int LSET = 5;
    /// slot (holds a Cell)
    static final int CGET = 6;
    /// slot
    static final int CSET = 7;
    /// slot: a fresh Cell holding the popped value
    static final int CNEW = 8;
    /// upvalue index
    static final int UGET = 9;
    /// upvalue index
    static final int USET = 10;
    /// const index of the name
    static final int GGET = 11;
    /// const index of the name
    static final int GSET = 12;
    static final int IGET = 13;
    static final int ISET = 14;
    /// const index of the key
    static final int FGET = 15;
    /// const index of the key
    static final int FSET = 16;
    /// const index of the method name
    static final int SELF = 17;
    /// operator id
    static final int BIN = 18;
    /// operator id
    static final int UN = 19;
    /// target
    static final int JMP = 20;
    /// target: pop, jump when falsy
    static final int JF = 21;
    /// target: pop, jump when truthy
    static final int JT = 22;
    /// target: keep and jump when falsy, else pop
    static final int AND = 23;
    /// target: keep and jump when truthy, else pop
    static final int OR = 24;
    /// fixed args, multi flag, result count (-1 = all)
    static final int CALL = 25;
    /// fixed values, multi flag
    static final int RET = 26;
    /// result count (-1 = all)
    static final int VARARG = 27;
    /// proto index
    static final int CLOSURE = 28;
    /// array size hint
    static final int NEWT = 29;
    /// first index, fixed values, multi flag
    static final int TSETL = 30;
    static final int TSETK = 31;
    /// count
    static final int POP = 32;
    static final int DUP = 33;
    /// count of parts
    static final int CONCATN = 34;
    /// base slot
    static final int FORPREP = 35;
    /// base slot, exit target
    static final int FORTEST = 36;
    /// base slot
    static final int FORINC = 37;
    /// base slot
    static final int GFORPREP = 38;
    /// target: pop, jump when nil
    static final int JNIL = 39;
    /// const index of key (t v -> t)
    static final int TSETF = 40;

    static final int ADD = 0;
    static final int SUB = 1;
    static final int MUL = 2;
    static final int DIV = 3;
    static final int MOD = 4;
    static final int POW = 5;
    static final int IDIV = 6;
    static final int CONCAT = 7;
    static final int EQ = 8;
    static final int NE = 9;
    static final int LT = 10;
    static final int LE = 11;
    static final int GT = 12;
    static final int GE = 13;

    static final int NEG = 0;
    static final int NOT = 1;
    static final int LEN = 2;

    static int binop(String op) {
        switch (op) {
            case "+":
                return ADD;
            case "-":
                return SUB;
            case "*":
                return MUL;
            case "/":
                return DIV;
            case "%":
                return MOD;
            case "^":
                return POW;
            case "//":
                return IDIV;
            case "..":
                return CONCAT;
            case "==":
                return EQ;
            case "~=":
                return NE;
            case "<":
                return LT;
            case "<=":
                return LE;
            case ">":
                return GT;
            case ">=":
                return GE;
            default:
                return -1;
        }
    }
}
