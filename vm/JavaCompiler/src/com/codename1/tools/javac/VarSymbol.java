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
package com.codename1.tools.javac;

/** A field, parameter or local variable. */
final class VarSymbol extends Symbol {
    enum Kind { FIELD, PARAM, LOCAL, EXCEPTION, RESOURCE }

    final Kind kind;
    Type type;
    /** The declaring class for a field. */
    ClassSymbol owner;
    /** Compile-time constant value (Integer, Long, Float, Double, String, Boolean, Character). */
    Object constValue;
    /** Number of assignments after declaration, for effectively-final checks. */
    int assignCount;
    boolean hasInitializer;
    /** A source field's declaration, for computing its constant value on demand. */
    Tree.VarDef decl;
    /** 0 = not computed, 1 = computing (cycle guard), 2 = done. */
    int constState;

    VarSymbol(String name, int flags, Type type, Kind kind) {
        super(name, flags);
        this.type = type;
        this.kind = kind;
    }

    boolean isEffectivelyFinal() {
        return isFinal() || assignCount == 0 || assignCount == 1 && !hasInitializer && kind == Kind.LOCAL;
    }

    @Override
    public String toString() {
        return name;
    }
}
