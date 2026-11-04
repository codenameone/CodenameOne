/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.core;

/// dart:core's Invocation: what a call that found no member looked like, handed to a
/// class's noSuchMethod. The runtime builds one for each dynamic miss (dart.runtime.Dyn),
/// so a noSuchMethod override can read the member name, arguments and kind of access.
/// Type arguments are not reified by this runtime, so typeArguments is always empty.
public final class Invocation {
    private static final int METHOD = 0;
    private static final int GETTER = 1;
    private static final int SETTER = 2;

    private final Symbol memberName;
    private final DartList<Object> positional;
    private final DartMap<Symbol, Object> named;
    private final int kind;

    private Invocation(Symbol memberName, DartList<Object> positional, DartMap<Symbol, Object> named,
            int kind) {
        this.memberName = memberName;
        this.positional = positional;
        this.named = named;
        this.kind = kind;
    }

    /// A method call `o.name(args...)`; named holds the named arguments by name, or null.
    public static Invocation method(String name, Object[] args, DartMap<String, Object> named) {
        DartList<Object> pos = new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC);
        if (args != null) {
            for (Object a : args) {
                pos.add(a);
            }
        }
        DartMap<Symbol, Object> nm = new DartMap<Symbol, Object>();
        if (named != null) {
            for (java.util.Map.Entry<String, Object> e : named.entrySet()) {
                nm.put(new Symbol(e.getKey()), e.getValue());
            }
        }
        return new Invocation(new Symbol(name), DartList.unmodifiable(pos), nm, METHOD);
    }

    /// A getter read `o.name`.
    public static Invocation getter(String name) {
        return new Invocation(new Symbol(name), DartList.unmodifiable(new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC)),
                new DartMap<Symbol, Object>(), GETTER);
    }

    /// A setter write `o.name = value`; the member name carries the trailing `=`, as in Dart.
    public static Invocation setter(String name, Object value) {
        DartList<Object> pos = new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC);
        pos.add(value);
        return new Invocation(new Symbol(name + "=", true), DartList.unmodifiable(pos),
                new DartMap<Symbol, Object>(), SETTER);
    }

    public Symbol memberName() {
        return memberName;
    }

    public DartList<Object> positionalArguments() {
        return positional;
    }

    public DartMap<Symbol, Object> namedArguments() {
        return named;
    }

    public DartList<Object> typeArguments() {
        return DartList.unmodifiable(new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC));
    }

    public boolean isMethod() {
        return kind == METHOD;
    }

    public boolean isGetter() {
        return kind == GETTER;
    }

    public boolean isSetter() {
        return kind == SETTER;
    }

    public boolean isAccessor() {
        return kind != METHOD;
    }

    @Override
    public String toString() {
        return "Instance of 'Invocation'";
    }
}
