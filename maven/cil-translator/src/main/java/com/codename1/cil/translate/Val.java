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
package com.codename1.cil.translate;

import com.codename1.cil.metadata.CilType;

/// One entry of the CIL evaluation stack, as the translator tracks it while
/// it walks a method. Immutable.
///
/// The kind says what the entry is on the JVM stack. A [#PAIR] is a managed
/// reference to something that is not a struct -- an array and an index into
/// it, two JVM slots. A [#TOKEN] is a method or field handle that exists only
/// at translation time and occupies no JVM slot at all.
final class Val {
    static final int I4 = 0;
    static final int I8 = 1;
    static final int R4 = 2;
    static final int R8 = 3;
    static final int REF = 4;
    static final int PAIR = 5;
    static final int TOKEN = 6;
    /// A managed reference to a field that is not a struct. The JVM stack
    /// holds the object the field belongs to, or nothing for a static, and
    /// the field itself is known only here.
    static final int FIELD = 7;

    /// Who else can see a struct value. `FRESH`: nobody, it was just made.
    /// `LOCAL`: a local of this method holds it. `ARG`: the caller does.
    /// `HEAP`: a field or an array element does. A store copies anything that
    /// is not `FRESH`; a return copies `ARG` and `HEAP`.
    static final int FRESH = 0;
    static final int LOCAL = 1;
    static final int ARG = 2;
    static final int HEAP = 3;

    final int kind;
    /// Best effort. Exact for a `PAIR` (the element type) and for the result
    /// of `newarr`; elsewhere the instruction that consumes the value names
    /// the type it needs.
    final CilType type;
    final int own;
    /// A value standing in for its own address: what `ldflda` of an `int`
    /// field becomes. Enough for a call that only reads through the address,
    /// which is all a method of a primitive can do; a store through it is
    /// rejected.
    final boolean valueAsAddress;
    final int token;
    final boolean virtualToken;
    final com.codename1.cil.metadata.CilAssembly.FieldRef field;
    final boolean staticField;

    private Val(int kind, CilType type, com.codename1.cil.metadata.CilAssembly.FieldRef field, boolean staticField) {
        this.kind = kind;
        this.type = type;
        this.own = HEAP;
        this.valueAsAddress = false;
        this.token = 0;
        this.virtualToken = false;
        this.field = field;
        this.staticField = staticField;
    }

    private Val(int kind, CilType type, int own, boolean valueAsAddress, int token, boolean virtualToken) {
        this.kind = kind;
        this.type = type;
        this.own = own;
        this.valueAsAddress = valueAsAddress;
        this.token = token;
        this.virtualToken = virtualToken;
        this.field = null;
        this.staticField = false;
    }

    static Val field(com.codename1.cil.metadata.CilAssembly.FieldRef field, CilType type, boolean isStatic) {
        return new Val(FIELD, type, field, isStatic);
    }

    static Val of(int kind, CilType type) {
        return new Val(kind, type, HEAP, false, 0, false);
    }

    static Val ref(CilType type, int own) {
        return new Val(REF, type, own, false, 0, false);
    }

    static Val pair(CilType element) {
        return new Val(PAIR, element, HEAP, false, 0, false);
    }

    static Val token(int token, boolean virtual) {
        return new Val(TOKEN, null, HEAP, false, token, virtual);
    }

    Val asAddress() {
        return new Val(kind, type, own, true, token, virtualToken);
    }

    Val owned(int newOwn) {
        return new Val(kind, type, newOwn, valueAsAddress, token, virtualToken);
    }

    int size() {
        if (kind == FIELD) {
            return staticField ? 0 : 1;
        }
        return kind == I8 || kind == R8 || kind == PAIR ? 2 : kind == TOKEN ? 0 : 1;
    }

    @Override
    public String toString() {
        String[] names = {"i4", "i8", "r4", "r8", "ref", "pair", "token", "field"};
        return names[kind] + (type == null ? "" : ":" + type);
    }
}
