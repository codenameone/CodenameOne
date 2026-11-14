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

/// One activation of a compiled function on a coroutine's stack.
final class Frame {
    final LuaClosure cl;
    final int[] code;
    final Object[] consts;
    final Object[] slots;
    Object[] stk = new Object[16];
    int sp;
    int pc;
    Object[] varargs;
    /// Number of values the last open expression left on the stack.
    int multi;
    /// Results the caller expects, -1 for all.
    final int nret;
    /// 0 normal, 1 pcall, 2 xpcall.
    int protect;
    Object handler;

    Frame(LuaClosure cl, int nret) {
        this.cl = cl;
        this.code = cl.proto.code;
        this.consts = cl.proto.consts;
        this.slots = new Object[Math.max(1, cl.proto.numSlots)];
        this.nret = nret;
    }

    void push(Object v) {
        if (sp == stk.length) {
            Object[] n = new Object[sp * 2];
            System.arraycopy(stk, 0, n, 0, sp);
            stk = n;
        }
        stk[sp++] = v;
    }

    void ensure(int extra) {
        if (sp + extra > stk.length) {
            Object[] n = new Object[Math.max(stk.length * 2, sp + extra + 8)];
            System.arraycopy(stk, 0, n, 0, sp);
            stk = n;
        }
    }

    int line() {
        return cl.proto.lineAt(pc - 1);
    }
}
