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

/// A compiled function: its code, constants, nested functions and how it
/// captures variables from the function around it.
final class Proto {
    int[] code = new int[0];
    int[] lines;
    Object[] consts = new Object[0];
    Proto[] protos = new Proto[0];
    int numParams;
    boolean vararg;
    int numSlots;
    /// For each upvalue: true when it is a local slot of the enclosing
    /// function, false when it is one of that function's own upvalues.
    boolean[] upFromLocal = new boolean[0];
    int[] upIndex = new int[0];
    String name;
    String chunk;
    int lineDefined;

    int lineAt(int pc) {
        if (lines == null || pc < 0) {
            return lineDefined;
        }
        return lines[Math.min(pc, lines.length - 1)];
    }
}
