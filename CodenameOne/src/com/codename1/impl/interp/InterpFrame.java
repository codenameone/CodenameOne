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
package com.codename1.impl.interp;

/// One interpreted call frame: locals, operand stack, and where execution is.
///
/// Primitives live in `prim` (a `long`, with float/double held as raw bits) and
/// references in `refs`; a slot uses one or the other, never both. That avoids
/// boxing every arithmetic result, which for a stack machine is the difference
/// between an allocation per operation and none.
///
/// Category-2 values occupy two slots exactly as the JVM specifies. Modelling
/// them as one slot would be simpler until `dup2`, `pop2` and `dup2_x1` -- whose
/// meaning is defined in terms of slots, not values -- quietly did the wrong
/// thing for `long` and `double`.
///
/// @author Shai Almog
final class InterpFrame {
    final InterpMethod method;
    final long[] prim;
    final Object[] refs;
    final long[] stackPrim;
    final Object[] stackRefs;

    int sp;

    /// Index of the instruction being executed, for stack traces and for
    /// matching the exception table.
    int insn;

    /// Where to carry on after a `monitorexit` handed control back to the
    /// enclosing level. See the monitor handling in InterpRuntime.
    int resumeInsn;

    InterpFrame(InterpMethod method) {
        this.method = method;
        int locals = Math.max(method.maxLocals, 1);
        this.prim = new long[locals];
        this.refs = new Object[locals];
        int stack = Math.max(method.maxStack, 1) + 2;
        this.stackPrim = new long[stack];
        this.stackRefs = new Object[stack];
    }

    void pushInt(int v) {
        stackRefs[sp] = null;
        stackPrim[sp++] = v;
    }

    void pushLong(long v) {
        stackRefs[sp] = null;
        stackPrim[sp++] = v;
        stackRefs[sp] = null;
        stackPrim[sp++] = 0;
    }

    void pushFloat(float v) {
        pushInt(Float.floatToIntBits(v));
    }

    void pushDouble(double v) {
        pushLong(Double.doubleToLongBits(v));
    }

    void pushRef(Object v) {
        stackPrim[sp] = 0;
        stackRefs[sp++] = v;
    }

    int popInt() {
        return (int)stackPrim[--sp];
    }

    long popLong() {
        sp -= 2;
        return stackPrim[sp];
    }

    float popFloat() {
        return Float.intBitsToFloat(popInt());
    }

    double popDouble() {
        return Double.longBitsToDouble(popLong());
    }

    Object popRef() {
        return stackRefs[--sp];
    }

    /// Pushes a value of the given kind from its raw representation. Sub-int
    /// kinds are pushed as ints, which is how the JVM stores them.
    void pushKind(int kind, long raw, Object ref) {
        switch (kind) {
            case InterpOpcodes.RET_VOID:
                break;
            case InterpOpcodes.RET_LONG:
            case InterpOpcodes.RET_DOUBLE:
                pushLong(raw);
                break;
            case InterpOpcodes.RET_OBJECT:
                pushRef(ref);
                break;
            default:
                pushInt((int)raw);
                break;
        }
    }

    void setLocalInt(int index, int v) {
        prim[index] = v;
        refs[index] = null;
    }

    void setLocalLong(int index, long v) {
        prim[index] = v;
        refs[index] = null;
        if (index + 1 < prim.length) {
            prim[index + 1] = 0;
            refs[index + 1] = null;
        }
    }

    void setLocalRef(int index, Object v) {
        prim[index] = 0;
        refs[index] = v;
    }
}
