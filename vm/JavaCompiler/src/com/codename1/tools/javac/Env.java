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

import java.util.HashMap;
import java.util.Map;

/**
 * The context an expression or statement is attributed in: the enclosing class
 * and method, local variables in scope, and whether {@code this} is available.
 * Environments nest; a lambda or local class body opens a new one whose locals
 * from outside are captured.
 */
final class Env {
    final Env outer;
    final Tree.CompilationUnit unit;
    final ClassSymbol enclClass;
    MethodSymbol enclMethod;
    /** Locals declared directly in this scope. */
    private Map<String, VarSymbol> locals;
    /** Local classes declared directly in this scope. */
    private Map<String, ClassSymbol> localClasses;
    /** Inside a static context: no {@code this}. */
    boolean isStatic;
    /** A lambda body boundary: locals from outside are captured. */
    Tree.Lambda lambda;
    /** A class boundary (the env of a class body): locals from outside are captured by that class. */
    boolean classBoundary;
    /** Label of the statement being attributed (for break/continue checks). */
    String label;
    /** The innermost enclosing switch expression, for yield. */
    Tree.Switch switchExpression;
    /** Expected return type inside a lambda body whose return type is being inferred; null when unknown. */
    Type lambdaReturn;
    /** Return expressions' types collected for a lambda body (implicit return type). */
    java.util.List<Type> lambdaReturns;
    /** In a constructor before super()/this(): only static access to the instance. */
    boolean ctorPrologue;
    /** The env of a method body (its parameters live here): returns stop looking outward here. */
    boolean methodBoundary;
    /** Exception types a try statement's catch clauses handle, for the body's env. */
    java.util.List<Type> caught;
    /** Inside a loop or switch: break (and for loops, continue) is legal. */
    boolean breakable;
    boolean continuable;
    /** The yield values collected for {@link #switchExpression}. */
    java.util.List<Tree> yields;

    Env(Env outer, Tree.CompilationUnit unit, ClassSymbol enclClass, MethodSymbol enclMethod) {
        this.outer = outer;
        this.unit = unit;
        this.enclClass = enclClass;
        this.enclMethod = enclMethod;
    }

    /** A nested block scope in the same method. */
    Env dup() {
        Env e = new Env(this, unit, enclClass, enclMethod);
        e.isStatic = isStatic;
        e.switchExpression = switchExpression;
        e.yields = yields;
        e.ctorPrologue = ctorPrologue;
        e.breakable = breakable;
        e.continuable = continuable;
        return e;
    }

    void enterLocal(VarSymbol v) {
        if (locals == null) {
            locals = new HashMap<String, VarSymbol>();
        }
        locals.put(v.name, v);
    }

    void enterLocalClass(ClassSymbol c) {
        if (localClasses == null) {
            localClasses = new HashMap<String, ClassSymbol>();
        }
        localClasses.put(c.simpleName, c);
    }

    VarSymbol localHere(String name) {
        return locals == null ? null : locals.get(name);
    }

    ClassSymbol localClassHere(String name) {
        return localClasses == null ? null : localClasses.get(name);
    }

    /** The innermost enclosing environment that belongs to a class body (no outer locals). */
    Env classEnv() {
        Env e = this;
        while (e != null && !e.classBoundary) {
            e = e.outer;
        }
        return e;
    }
}
