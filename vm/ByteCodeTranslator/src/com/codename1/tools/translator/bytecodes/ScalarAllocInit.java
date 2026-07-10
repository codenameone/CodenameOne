/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.tools.translator.bytecodes;

import java.util.List;

/**
 * Replaces the {@code INVOKESPECIAL X.<init>} of a scalar-replaced
 * {@code @StackAllocate} object: it assigns the constructor arguments directly
 * into the method-local struct {@code __cn1sr_<id>} instead of calling the
 * constructor.
 *
 * <p>Two render modes, both operand-stack-balanced against the original
 * {@code (args..., INVOKESPECIAL)} (which pushed the args and popped them):</p>
 * <ul>
 *   <li><b>folded</b> (preferred): {@link #fold} has pulled the argument
 *       expressions in directly, so it emits
 *       {@code __cn1sr_<id>.field = <argExpr>;} with no operand-stack traffic at
 *       all -- the struct stays in registers (clang SROA). The folded arg
 *       instructions are removed from the stream, so nothing pushes the args.</li>
 *   <li><b>fallback</b>: if the args could not be reduced to pure assignable
 *       expressions, the args are still pushed normally and this pops them
 *       (reverse order) into the fields.</li>
 * </ul>
 *
 * @author Codename One
 */
public class ScalarAllocInit extends Instruction {
    private final int structId;
    private final String[] members; // by ctor-arg position
    private final char[] quals;      // by ctor-arg position: l/d/f/i

    // Non-null once the preceding argument expressions have been folded in.
    private Instruction[] argOps;

    public ScalarAllocInit(int structId, String[] members, char[] quals) {
        super(-1);
        this.structId = structId;
        this.members = members;
        this.quals = quals;
    }

    private static String popMacro(char q) {
        switch (q) {
            case 'l': return "POP_LONG()";
            case 'd': return "POP_DOUBLE()";
            case 'f': return "POP_FLOAT()";
            default:  return "POP_INT()";
        }
    }

    /**
     * Attempts to pull the {@code members.length} preceding instructions (the
     * constructor arguments, in order) in as pure assignable expressions. Mirrors
     * {@code Field.tryReduce}: if every arg reduces to an assignable expression,
     * they are captured and removed and this instruction renders the direct
     * register-friendly assignment form. Returns the new index of this instruction
     * or -1 if it could not fold (then the operand-stack fallback is used).
     */
    public int fold(List<Instruction> instructions, int index) {
        if (argOps != null) {
            return -1;
        }
        int n = members.length;
        if (index < n) {
            return -1;
        }
        Instruction[] ops = new Instruction[n];
        for (int p = 0; p < n; p++) {
            Instruction arg = instructions.get(index - n + p);
            if (!(arg instanceof AssignableExpression)) {
                return -1;
            }
            StringBuilder dummy = new StringBuilder();
            if (!((AssignableExpression) arg).assignTo(null, dummy)) {
                return -1;
            }
            ops[p] = arg;
        }
        this.argOps = ops;
        for (int p = 0; p < n; p++) {
            instructions.remove(index - n);
        }
        return index - n;
    }

    @Override
    public void addDependencies(List<String> dependencyList) {
        if (argOps != null) {
            for (Instruction op : argOps) {
                op.addDependencies(dependencyList);
            }
        }
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        appendInstruction(b);
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        b.append("    /* scalar-replaced <init> -> __cn1sr_").append(structId).append(" */\n");
        if (argOps != null) {
            for (int p = 0; p < members.length; p++) {
                StringBuilder expr = new StringBuilder();
                ((AssignableExpression) argOps[p]).assignTo(null, expr);
                b.append("    __cn1sr_").append(structId).append(".").append(members[p])
                        .append(" = ").append(expr.toString().trim()).append(";\n");
            }
            return;
        }
        // fallback: pop the args (top of stack is the last arg) into the fields
        for (int p = members.length - 1; p >= 0; p--) {
            b.append("    __cn1sr_").append(structId).append(".").append(members[p])
                    .append(" = ").append(popMacro(quals[p])).append(";\n");
        }
    }
}
