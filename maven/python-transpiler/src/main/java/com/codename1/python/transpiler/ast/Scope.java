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
package com.codename1.python.transpiler.ast;

import java.util.LinkedHashSet;
import java.util.Set;

/// A Python scope and where each of its names lives, computed by the scope
/// pass with CPython's rules (LEGB, `global`, `nonlocal`, class bodies that
/// do not enclose their methods, comprehensions as scopes of their own).
public final class Scope {

    public enum Kind { MODULE, FUNCTION, CLASS, LAMBDA, COMPREHENSION }

    /// Where a name resolves from inside a scope.
    public enum Binding {
        /// A plain Java local of this scope.
        LOCAL,
        /// A local of this scope that an inner scope captures: a cell.
        CELL,
        /// A cell owned by an enclosing function scope.
        FREE,
        /// A module global with a fixed slot.
        GLOBAL,
        /// Not assigned anywhere visible: a builtin, or a run-time global.
        BUILTIN,
        /// An attribute of the class namespace being built.
        CLASS
    }

    public final Kind kind;
    public final Scope parent;
    public final String name;
    public final Set<String> assigned = new LinkedHashSet<String>();
    public final Set<String> params = new LinkedHashSet<String>();
    public final Set<String> globals = new LinkedHashSet<String>();
    public final Set<String> nonlocals = new LinkedHashSet<String>();
    public final Set<String> used = new LinkedHashSet<String>();
    /// Locals captured by an inner scope.
    public final Set<String> cells = new LinkedHashSet<String>();
    /// Names a `del` or an `except ... as` clause can unbind.
    public final Set<String> deleted = new LinkedHashSet<String>();
    /// On the module scope: every `Name.attr` the module stores into or
    /// deletes, anywhere in it (`setattr(Name, ...)` too; `Name.*` when the
    /// attribute name is not a literal).
    public final Set<String> attrStores = new LinkedHashSet<String>();
    public boolean hasYield;
    /// Whether the body has a `yield` itself, not just an `await`: an async
    /// function with one is an async generator.
    public boolean yields;
    public boolean hasStarImport;
    /// The class this scope's function is a method of (for zero-arg super()).
    public Scope enclosingClass;
    /// Set on CLASS scopes: the Java variable holding the class being built.
    public String classVar;

    public Scope(Kind kind, Scope parent, String name) {
        this.kind = kind;
        this.parent = parent;
        this.name = name;
    }

    public boolean isFunctionLike() {
        return kind == Kind.FUNCTION || kind == Kind.LAMBDA || kind == Kind.COMPREHENSION;
    }

    /// Whether `n` is a local (plain or cell) of this scope.
    public boolean isLocal(String n) {
        if (globals.contains(n) || nonlocals.contains(n)) {
            return false;
        }
        return assigned.contains(n) || params.contains(n);
    }

    public Scope module() {
        Scope s = this;
        while (s.parent != null) {
            s = s.parent;
        }
        return s;
    }

    /// Resolves `n` as read or written from inside this scope.
    public Binding resolve(String n) {
        if (kind == Kind.MODULE) {
            return assigned.contains(n) ? Binding.GLOBAL : Binding.BUILTIN;
        }
        if (globals.contains(n)) {
            // The scope pass gives every `global` name a module slot.
            return Binding.GLOBAL;
        }
        if (kind == Kind.CLASS) {
            if (assigned.contains(n)) {
                return Binding.CLASS;
            }
            return enclosing(n);
        }
        if (nonlocals.contains(n)) {
            return Binding.FREE;
        }
        if (isLocal(n)) {
            return cells.contains(n) ? Binding.CELL : Binding.LOCAL;
        }
        return enclosing(n);
    }

    /// Resolution of a name this scope does not bind: the nearest enclosing
    /// function scope that binds it (skipping class bodies), else global.
    private Binding enclosing(String n) {
        for (Scope s = parent; s != null; s = s.parent) {
            if (s.kind == Kind.CLASS) {
                continue;
            }
            if (s.kind == Kind.MODULE) {
                return s.assigned.contains(n) ? Binding.GLOBAL : Binding.BUILTIN;
            }
            if (s.globals.contains(n)) {
                return Binding.GLOBAL;
            }
            if (s.isLocal(n) || s.nonlocals.contains(n)) {
                return Binding.FREE;
            }
        }
        return Binding.BUILTIN;
    }

    /// The function scope that owns the cell a FREE name refers to.
    public Scope owner(String n) {
        for (Scope s = parent; s != null; s = s.parent) {
            if (s.kind == Kind.CLASS || s.kind == Kind.MODULE) {
                continue;
            }
            if (s.isLocal(n)) {
                return s;
            }
        }
        return null;
    }
}
