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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** A type: primitive, class (possibly parameterized), array, type variable, wildcard, null or error. */
abstract class Type {
    enum Tag { BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, VOID, CLASS, ARRAY, TYPEVAR, WILDCARD,
        NULL, ERROR, INTERSECTION, INFERENCE }

    final Tag tag;

    Type(Tag tag) {
        this.tag = tag;
    }

    boolean isPrimitive() {
        return tag.ordinal() <= Tag.DOUBLE.ordinal();
    }

    boolean isNumeric() {
        return tag.ordinal() >= Tag.BYTE.ordinal() && tag.ordinal() <= Tag.DOUBLE.ordinal();
    }

    boolean isIntegral() {
        return tag == Tag.BYTE || tag == Tag.SHORT || tag == Tag.CHAR || tag == Tag.INT || tag == Tag.LONG;
    }

    boolean isReference() {
        return tag == Tag.CLASS || tag == Tag.ARRAY || tag == Tag.TYPEVAR || tag == Tag.NULL || tag == Tag.INTERSECTION
                || tag == Tag.INFERENCE || tag == Tag.WILDCARD;
    }

    boolean isErroneous() {
        return tag == Tag.ERROR;
    }

    /** Stack/local slots: 2 for long and double. */
    int slots() {
        return tag == Tag.LONG || tag == Tag.DOUBLE ? 2 : tag == Tag.VOID ? 0 : 1;
    }

    // ------------------------------------------------------------------ kinds

    static final class Prim extends Type {
        final String descriptor;
        final String name;

        Prim(Tag tag, String descriptor, String name) {
            super(tag);
            this.descriptor = descriptor;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final Prim BOOLEAN = new Prim(Tag.BOOLEAN, "Z", "boolean");
    static final Prim BYTE = new Prim(Tag.BYTE, "B", "byte");
    static final Prim SHORT = new Prim(Tag.SHORT, "S", "short");
    static final Prim CHAR = new Prim(Tag.CHAR, "C", "char");
    static final Prim INT = new Prim(Tag.INT, "I", "int");
    static final Prim LONG = new Prim(Tag.LONG, "J", "long");
    static final Prim FLOAT = new Prim(Tag.FLOAT, "F", "float");
    static final Prim DOUBLE = new Prim(Tag.DOUBLE, "D", "double");
    static final Prim VOID = new Prim(Tag.VOID, "V", "void");

    static final class NullType extends Type {
        NullType() {
            super(Tag.NULL);
        }

        @Override
        public String toString() {
            return "<null>";
        }
    }

    static final NullType NULL = new NullType();

    static final class ErrorType extends Type {
        ErrorType() {
            super(Tag.ERROR);
        }

        @Override
        public String toString() {
            return "<any>";
        }
    }

    static final ErrorType ERROR = new ErrorType();

    static final class ClassType extends Type {
        final ClassSymbol sym;
        /** Type arguments; empty for a non-generic class or a raw type. */
        final List<Type> args;
        /** The enclosing instance's type for an inner class, else null. */
        final ClassType outer;

        ClassType(ClassSymbol sym, List<Type> args, ClassType outer) {
            super(Tag.CLASS);
            this.sym = sym;
            this.args = args;
            this.outer = outer;
        }

        boolean isRaw() {
            return args.isEmpty() && !sym.typeParams().isEmpty();
        }

        boolean isParameterized() {
            return !args.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder(sym.javaName());
            if (!args.isEmpty()) {
                b.append('<');
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        b.append(',');
                    }
                    b.append(args.get(i));
                }
                b.append('>');
            }
            return b.toString();
        }
    }

    static final class ArrayType extends Type {
        final Type elem;

        ArrayType(Type elem) {
            super(Tag.ARRAY);
            this.elem = elem;
        }

        @Override
        public String toString() {
            return elem + "[]";
        }
    }

    static final class TypeVar extends Type {
        final String name;
        /** Upper bound (an IntersectionType for several bounds); set after construction. */
        Type bound;

        TypeVar(String name) {
            super(Tag.TYPEVAR);
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final class WildcardType extends Type {
        /** EXTENDS, SUPER or UNBOUND. */
        final int kind;
        final Type bound;
        static final int UNBOUND = 0;
        static final int EXTENDS = 1;
        static final int SUPER = 2;

        WildcardType(int kind, Type bound) {
            super(Tag.WILDCARD);
            this.kind = kind;
            this.bound = bound;
        }

        @Override
        public String toString() {
            return kind == UNBOUND ? "?" : kind == EXTENDS ? "? extends " + bound : "? super " + bound;
        }
    }

    static final class IntersectionType extends Type {
        final List<Type> bounds;

        IntersectionType(List<Type> bounds) {
            super(Tag.INTERSECTION);
            this.bounds = bounds;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < bounds.size(); i++) {
                if (i > 0) {
                    b.append(" & ");
                }
                b.append(bounds.get(i));
            }
            return b.toString();
        }
    }

    /** An inference variable standing for a method type parameter during inference. */
    static final class InferenceVar extends Type {
        final TypeVar origin;
        final List<Type> lower = new ArrayList<Type>();
        final List<Type> upper = new ArrayList<Type>();
        final List<Type> equal = new ArrayList<Type>();
        Type inst;

        InferenceVar(TypeVar origin) {
            super(Tag.INFERENCE);
            this.origin = origin;
        }

        @Override
        public String toString() {
            return inst != null ? inst.toString() : origin.name + "'";
        }
    }

    // ------------------------------------------------------------------ helpers

    static Type substitute(Type t, Map<TypeVar, Type> map) {
        if (map.isEmpty() || t == null) {
            return t;
        }
        switch (t.tag) {
            case TYPEVAR: {
                Type r = map.get(t);
                return r != null ? r : t;
            }
            case CLASS: {
                ClassType c = (ClassType) t;
                if (c.args.isEmpty() && c.outer == null) {
                    return c;
                }
                List<Type> args = new ArrayList<Type>(c.args.size());
                boolean changed = false;
                for (Type a : c.args) {
                    Type s = substitute(a, map);
                    changed |= s != a;
                    args.add(s);
                }
                ClassType outer = c.outer == null ? null : (ClassType) substitute(c.outer, map);
                changed |= outer != c.outer;
                return changed ? new ClassType(c.sym, args, outer) : c;
            }
            case ARRAY: {
                Type e = substitute(((ArrayType) t).elem, map);
                return e == ((ArrayType) t).elem ? t : new ArrayType(e);
            }
            case WILDCARD: {
                WildcardType w = (WildcardType) t;
                if (w.bound == null) {
                    return w;
                }
                Type b = substitute(w.bound, map);
                return b == w.bound ? w : new WildcardType(w.kind, b);
            }
            case INTERSECTION: {
                List<Type> bs = new ArrayList<Type>();
                for (Type b : ((IntersectionType) t).bounds) {
                    bs.add(substitute(b, map));
                }
                return new IntersectionType(bs);
            }
            default:
                return t;
        }
    }

    static Map<TypeVar, Type> mapping(List<TypeVar> from, List<Type> to) {
        Map<TypeVar, Type> map = new IdentityHashMap<TypeVar, Type>();
        if (to.size() == from.size()) {
            for (int i = 0; i < from.size(); i++) {
                map.put(from.get(i), to.get(i));
            }
        }
        return map;
    }

    static List<Type> list(Type... types) {
        List<Type> out = new ArrayList<Type>();
        Collections.addAll(out, types);
        return out;
    }
}
