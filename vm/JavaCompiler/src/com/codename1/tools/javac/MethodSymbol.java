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

import java.util.ArrayList;
import java.util.List;

/** A method or constructor ({@code <init>}). */
final class MethodSymbol extends Symbol {
    ClassSymbol owner;
    final List<Type.TypeVar> typeParams = new ArrayList<Type.TypeVar>();
    final List<Type> params = new ArrayList<Type>();
    Type returnType;
    final List<Type> thrown = new ArrayList<Type>();
    /** Source parameter symbols, when compiled from source. */
    List<VarSymbol> paramSyms;
    Tree.MethodDecl decl;
    /** The erased JVM descriptor; computed lazily for library methods from their class file. */
    String descriptor;
    /** For a constructor that starts with this(...): the constructor it delegates to, and where. */
    MethodSymbol thisCall;
    int thisCallPos;
    /** For a constructor without an explicit this()/super() call: the superclass constructor it calls. */
    MethodSymbol superCtor;
    /**
     * For a synthetic accessor the compiler adds to a class so an inner class can call
     * Outer.super.m(): the superclass method it invokes (with invokespecial), and the
     * class that names it.
     */
    MethodSymbol superAccessTarget;
    ClassSymbol superAccessQualifier;
    /** The anonymous class constructor's call to super was variable-arity. */

    MethodSymbol(String name, int flags, ClassSymbol owner) {
        super(name, flags);
        this.owner = owner;
    }

    boolean isConstructor() {
        return "<init>".equals(name);
    }

    boolean isVarargs() {
        return (flags & ACC_VARARGS) != 0;
    }

    String descriptor() {
        if (descriptor == null) {
            StringBuilder b = new StringBuilder("(");
            for (Type p : params) {
                b.append(Types.descriptor(Types.erasure(p)));
            }
            b.append(')').append(Types.descriptor(Types.erasure(returnType)));
            descriptor = b.toString();
        }
        return descriptor;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(isConstructor() ? owner.simpleName : name).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(params.get(i));
        }
        return b.append(')').toString();
    }
}
