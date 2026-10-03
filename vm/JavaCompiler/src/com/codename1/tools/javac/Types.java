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
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Relations between types. Generic type arguments are tracked precisely enough to
 * infer method type arguments, type lambda parameters and insert the casts erasure
 * requires; assignments between parameterizations of compatible erasures are
 * accepted (an unchecked conversion) rather than rejected.
 */
final class Types {
    final Symtab symtab;

    Types(Symtab symtab) {
        this.symtab = symtab;
    }

    // ------------------------------------------------------------------ erasure and descriptors

    static Type erasure(Type t) {
        if (t == null) {
            return null;
        }
        switch (t.tag) {
            case CLASS: {
                Type.ClassType c = (Type.ClassType) t;
                return c.args.isEmpty() && c.outer == null ? c : c.sym.erasure();
            }
            case ARRAY: {
                Type e = erasure(((Type.ArrayType) t).elem);
                return e == ((Type.ArrayType) t).elem ? t : new Type.ArrayType(e);
            }
            case TYPEVAR: {
                Type.TypeVar tv = (Type.TypeVar) t;
                return tv.bound == null ? null : erasure(tv.bound);
            }
            case WILDCARD: {
                Type.WildcardType w = (Type.WildcardType) t;
                return w.kind == Type.WildcardType.EXTENDS ? erasure(w.bound) : null;
            }
            case INTERSECTION:
                return erasure(((Type.IntersectionType) t).bounds.get(0));
            case INFERENCE: {
                Type.InferenceVar iv = (Type.InferenceVar) t;
                return iv.inst != null ? erasure(iv.inst) : erasure(iv.origin);
            }
            default:
                return t;
        }
    }

    /** Erasure that never answers null (an unbounded variable erases to Object). */
    Type erased(Type t) {
        Type e = erasure(t);
        return e == null ? symtab.objectType : e;
    }

    static String descriptor(Type erased) {
        if (erased == null) {
            return "Ljava/lang/Object;";
        }
        switch (erased.tag) {
            case CLASS:
                return "L" + ((Type.ClassType) erased).sym.internalName + ";";
            case ARRAY:
                return "[" + descriptor(erasure(((Type.ArrayType) erased).elem));
            case NULL:
            case ERROR:
                return "Ljava/lang/Object;";
            default:
                if (erased instanceof Type.Prim) {
                    return ((Type.Prim) erased).descriptor;
                }
                return descriptor(erasure(erased));
        }
    }

    /** Internal name for checkcast/instanceof/anewarray: a class name or an array descriptor. */
    String internalName(Type t) {
        Type e = erased(t);
        if (e.tag == Type.Tag.ARRAY) {
            return descriptor(e);
        }
        if (e.tag == Type.Tag.CLASS) {
            return ((Type.ClassType) e).sym.internalName;
        }
        return "java/lang/Object";
    }

    // ------------------------------------------------------------------ supertypes

    /** The superclass of a class type with this type's arguments substituted. */
    Type supertype(Type.ClassType t) {
        Type sup = t.sym.superclass();
        if (sup == null) {
            return null;
        }
        if (t.isRaw()) {
            return erasure(sup);
        }
        return Type.substitute(sup, Type.mapping(t.sym.typeParams(), t.args));
    }

    List<Type> interfaces(Type.ClassType t) {
        List<Type> out = new ArrayList<Type>();
        boolean raw = t.isRaw();
        Map<Type.TypeVar, Type> map = raw ? null : Type.mapping(t.sym.typeParams(), t.args);
        for (Type i : t.sym.interfaces()) {
            out.add(raw ? erasure(i) : Type.substitute(i, map));
        }
        return out;
    }

    /** The view of {@code t} as an instance of {@code target}, with type arguments, or null. */
    Type.ClassType asSuper(Type t, ClassSymbol target) {
        if (t == null) {
            return null;
        }
        switch (t.tag) {
            case CLASS: {
                Type.ClassType c = (Type.ClassType) t;
                if (c.sym == target) {
                    return c;
                }
                Type sup = supertype(c);
                if (sup != null) {
                    Type.ClassType r = asSuper(sup, target);
                    if (r != null) {
                        return r;
                    }
                }
                for (Type i : interfaces(c)) {
                    Type.ClassType r = asSuper(i, target);
                    if (r != null) {
                        return r;
                    }
                }
                if (target == symtab.objectSym) {
                    return symtab.objectType;
                }
                return null;
            }
            case TYPEVAR:
                return asSuper(((Type.TypeVar) t).bound, target);
            case INTERSECTION:
                for (Type b : ((Type.IntersectionType) t).bounds) {
                    Type.ClassType r = asSuper(b, target);
                    if (r != null) {
                        return r;
                    }
                }
                return null;
            case WILDCARD:
                return asSuper(upperBound(t), target);
            case INFERENCE: {
                Type.InferenceVar iv = (Type.InferenceVar) t;
                return asSuper(iv.inst != null ? iv.inst : iv.origin.bound, target);
            }
            case ARRAY: {
                String n = target.internalName;
                if (n.equals("java/lang/Object") || n.equals("java/lang/Cloneable") || n.equals("java/io/Serializable")) {
                    return target.erasure();
                }
                return null;
            }
            default:
                return null;
        }
    }

    Type upperBound(Type t) {
        if (t == null) {
            return symtab.objectType;
        }
        if (t.tag == Type.Tag.WILDCARD) {
            Type.WildcardType w = (Type.WildcardType) t;
            return w.kind == Type.WildcardType.EXTENDS ? w.bound : symtab.objectType;
        }
        return t;
    }

    /** Erasure-level: does class {@code a} extend or implement {@code b}? */
    boolean isSubClass(ClassSymbol a, ClassSymbol b) {
        if (a == b || b == symtab.objectSym) {
            return true;
        }
        Set<ClassSymbol> seen = new LinkedHashSet<ClassSymbol>();
        return isSubClass(a, b, seen);
    }

    private boolean isSubClass(ClassSymbol a, ClassSymbol b, Set<ClassSymbol> seen) {
        if (a == b) {
            return true;
        }
        if (!seen.add(a)) {
            return false;
        }
        Type sup = a.superclass();
        if (sup != null && sup.tag == Type.Tag.CLASS && isSubClass(((Type.ClassType) sup).sym, b, seen)) {
            return true;
        }
        for (Type i : a.interfaces()) {
            if (i.tag == Type.Tag.CLASS && isSubClass(((Type.ClassType) i).sym, b, seen)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ subtyping

    boolean isSameType(Type a, Type b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.tag == Type.Tag.INFERENCE || b.tag == Type.Tag.INFERENCE) {
            return equalBound(a, b);
        }
        if (a.tag != b.tag) {
            return false;
        }
        switch (a.tag) {
            case CLASS: {
                Type.ClassType x = (Type.ClassType) a;
                Type.ClassType y = (Type.ClassType) b;
                if (x.sym != y.sym) {
                    return false;
                }
                if (x.args.isEmpty() || y.args.isEmpty()) {
                    return true;
                }
                if (x.args.size() != y.args.size()) {
                    return false;
                }
                for (int i = 0; i < x.args.size(); i++) {
                    if (!isSameType(x.args.get(i), y.args.get(i))) {
                        return false;
                    }
                }
                return true;
            }
            case ARRAY:
                return isSameType(((Type.ArrayType) a).elem, ((Type.ArrayType) b).elem);
            case WILDCARD: {
                Type.WildcardType x = (Type.WildcardType) a;
                Type.WildcardType y = (Type.WildcardType) b;
                return x.kind == y.kind && (x.bound == null ? y.bound == null : isSameType(x.bound, y.bound));
            }
            default:
                return a.tag.ordinal() <= Type.Tag.VOID.ordinal() || a.tag == Type.Tag.NULL;
        }
    }

    private boolean equalBound(Type a, Type b) {
        if (a.tag == Type.Tag.INFERENCE) {
            Type.InferenceVar iv = (Type.InferenceVar) a;
            if (iv.inst != null) {
                return isSameType(iv.inst, b);
            }
            if (iv != b) {
                iv.equal.add(b);
            }
            return true;
        }
        return equalBound(b, a);
    }

    /** Reference subtyping (no boxing); records bounds on inference variables. */
    boolean isSubtype(Type s, Type t) {
        if (s == t || s == null || t == null) {
            return true;
        }
        if (s.tag == Type.Tag.ERROR || t.tag == Type.Tag.ERROR) {
            return true;
        }
        if (t.tag == Type.Tag.INFERENCE) {
            Type.InferenceVar iv = (Type.InferenceVar) t;
            if (iv.inst != null) {
                return isSubtype(s, iv.inst);
            }
            if (s != iv && s.tag != Type.Tag.NULL) {
                iv.lower.add(s.isPrimitive() ? symtab.boxedType(s) : s);
            }
            return true;
        }
        if (s.tag == Type.Tag.INFERENCE) {
            Type.InferenceVar iv = (Type.InferenceVar) s;
            if (iv.inst != null) {
                return isSubtype(iv.inst, t);
            }
            iv.upper.add(t);
            return true;
        }
        if (s.isPrimitive() || t.isPrimitive()) {
            return s.isPrimitive() && t.isPrimitive() && isPrimitiveWidening(s, t);
        }
        if (s.tag == Type.Tag.NULL) {
            return true;
        }
        if (t.tag == Type.Tag.NULL) {
            return false;
        }
        if (t.tag == Type.Tag.INTERSECTION) {
            for (Type b : ((Type.IntersectionType) t).bounds) {
                if (!isSubtype(s, b)) {
                    return false;
                }
            }
            return true;
        }
        if (t.tag == Type.Tag.TYPEVAR) {
            if (s.tag == Type.Tag.TYPEVAR && isSubtypeVar((Type.TypeVar) s, (Type.TypeVar) t)) {
                return true;
            }
            // A type variable is only a supertype of itself (or of something bounded by it);
            // accept an erasure-compatible type as an unchecked conversion.
            return isSubtype(erased(s), erased(t)) && s.tag == Type.Tag.TYPEVAR;
        }
        if (t.tag == Type.Tag.WILDCARD) {
            Type.WildcardType w = (Type.WildcardType) t;
            if (w.kind == Type.WildcardType.SUPER) {
                return isSubtype(s, w.bound);
            }
            return isSubtype(s, upperBound(w));
        }
        if (s.tag == Type.Tag.TYPEVAR) {
            return isSubtype(((Type.TypeVar) s).bound, t);
        }
        if (s.tag == Type.Tag.INTERSECTION) {
            for (Type b : ((Type.IntersectionType) s).bounds) {
                if (isSubtype(b, t)) {
                    return true;
                }
            }
            return false;
        }
        if (s.tag == Type.Tag.WILDCARD) {
            return isSubtype(upperBound(s), t);
        }
        if (t.tag == Type.Tag.ARRAY) {
            if (s.tag != Type.Tag.ARRAY) {
                return false;
            }
            Type se = ((Type.ArrayType) s).elem;
            Type te = ((Type.ArrayType) t).elem;
            if (se.isPrimitive() || te.isPrimitive()) {
                return se.tag == te.tag;
            }
            return isSubtype(se, te);
        }
        if (t.tag != Type.Tag.CLASS) {
            return false;
        }
        Type.ClassType ct = (Type.ClassType) t;
        Type.ClassType sup = asSuper(s, ct.sym);
        if (sup == null) {
            return false;
        }
        if (ct.args.isEmpty() || sup.args.isEmpty()) {
            return true;
        }
        if (ct.args.size() != sup.args.size()) {
            return true;
        }
        for (int i = 0; i < ct.args.size(); i++) {
            if (!containsType(ct.args.get(i), sup.args.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isSubtypeVar(Type.TypeVar s, Type.TypeVar t) {
        Type b = s;
        for (int guard = 0; guard < 16 && b != null; guard++) {
            if (b == t) {
                return true;
            }
            if (b.tag != Type.Tag.TYPEVAR) {
                return false;
            }
            b = ((Type.TypeVar) b).bound;
        }
        return false;
    }

    /** Does type argument {@code formal} contain {@code actual}? */
    boolean containsType(Type formal, Type actual) {
        if (formal.tag == Type.Tag.WILDCARD) {
            Type.WildcardType w = (Type.WildcardType) formal;
            switch (w.kind) {
                case Type.WildcardType.UNBOUND:
                    return true;
                case Type.WildcardType.EXTENDS:
                    return isSubtype(upperBound(actual), w.bound);
                default: {
                    Type lowerOfActual = actual.tag == Type.Tag.WILDCARD
                            ? ((Type.WildcardType) actual).kind == Type.WildcardType.SUPER ? ((Type.WildcardType) actual).bound : null
                            : actual;
                    return lowerOfActual == null || isSubtype(w.bound, lowerOfActual);
                }
            }
        }
        if (formal.tag == Type.Tag.INFERENCE || actual.tag == Type.Tag.INFERENCE) {
            return isSameType(formal, actual);
        }
        if (actual.tag == Type.Tag.WILDCARD) {
            // Capture: List<?> passed where List<T> is wanted. Accept; erasure decides.
            return true;
        }
        if (formal.tag == Type.Tag.TYPEVAR || actual.tag == Type.Tag.TYPEVAR) {
            return isSameType(formal, actual) || isSubtype(erased(actual), erased(formal));
        }
        // Invariant, but a mismatch here is an unchecked conversion rather than an error. An Object
        // argument is what inference falls back to without enough context; accept it rather than fail.
        if (isSameType(formal, actual)) {
            return true;
        }
        Type ea = erased(actual);
        return isSubtype(ea, erased(formal)) || ea.tag == Type.Tag.CLASS && ((Type.ClassType) ea).sym == symtab.objectSym;
    }

    // ------------------------------------------------------------------ conversions

    static int rank(Type t) {
        switch (t.tag) {
            case BYTE: return 1;
            case SHORT: return 2;
            case CHAR: return 2;
            case INT: return 3;
            case LONG: return 4;
            case FLOAT: return 5;
            case DOUBLE: return 6;
            default: return 0;
        }
    }

    static boolean isPrimitiveWidening(Type from, Type to) {
        if (from.tag == to.tag) {
            return true;
        }
        if (from.tag == Type.Tag.BOOLEAN || to.tag == Type.Tag.BOOLEAN || from.tag == Type.Tag.VOID || to.tag == Type.Tag.VOID) {
            return false;
        }
        if (to.tag == Type.Tag.CHAR) {
            return false;
        }
        if (from.tag == Type.Tag.CHAR) {
            return to.tag == Type.Tag.INT || to.tag == Type.Tag.LONG || to.tag == Type.Tag.FLOAT || to.tag == Type.Tag.DOUBLE;
        }
        if (from.tag == Type.Tag.BYTE && to.tag == Type.Tag.SHORT) {
            return true;
        }
        return rank(from) < rank(to) && !(from.tag == Type.Tag.SHORT && to.tag == Type.Tag.CHAR);
    }

    /**
     * Assignment (method-invocation) compatibility. With {@code loose}, boxing and
     * unboxing are allowed; {@code constant} is the value of a constant expression,
     * which permits narrowing to byte/short/char when it fits.
     */
    boolean isAssignable(Type from, Type to, boolean loose, Object constant) {
        if (from == null || to == null || from.isErroneous() || to.isErroneous()) {
            return true;
        }
        if (from.tag == Type.Tag.VOID || to.tag == Type.Tag.VOID) {
            return false;
        }
        if (from.isPrimitive() && to.isPrimitive()) {
            if (isPrimitiveWidening(from, to)) {
                return true;
            }
            return constant != null && fitsConstant(constant, to) && from.isIntegral() && from.tag != Type.Tag.LONG;
        }
        if (from.isPrimitive()) {
            if (!loose) {
                return false;
            }
            if (to.tag == Type.Tag.INFERENCE) {
                return isSubtype(symtab.boxedType(from), to);
            }
            // Constant narrowing then boxing: Byte b = 1; Character c = 'x'.
            Type unboxedTarget = symtab.unboxedType(to);
            if (unboxedTarget != null && constant != null && from.tag == Type.Tag.INT
                    && (unboxedTarget.tag == Type.Tag.BYTE || unboxedTarget.tag == Type.Tag.SHORT
                    || unboxedTarget.tag == Type.Tag.CHAR) && fitsConstant(constant, unboxedTarget)) {
                return true;
            }
            return isSubtype(symtab.boxedType(from), to);
        }
        if (to.isPrimitive()) {
            if (!loose) {
                return false;
            }
            Type unboxed = symtab.unboxedType(from.tag == Type.Tag.TYPEVAR ? erased(from) : from);
            if (unboxed == null && from.tag == Type.Tag.INFERENCE) {
                isSubtype(from, symtab.boxedType(to));
                return true;
            }
            return unboxed != null && isPrimitiveWidening(unboxed, to);
        }
        return isSubtype(from, to);
    }

    static boolean fitsConstant(Object constant, Type to) {
        long v;
        if (constant instanceof Integer) {
            v = ((Integer) constant).intValue();
        } else if (constant instanceof Character) {
            v = ((Character) constant).charValue();
        } else {
            return false;
        }
        switch (to.tag) {
            case BYTE: return v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE;
            case SHORT: return v >= Short.MIN_VALUE && v <= Short.MAX_VALUE;
            case CHAR: return v >= 0 && v <= 0xFFFF;
            case INT: return true;
            default: return false;
        }
    }

    /** Casting conversion: permissive for reference types whose erasures could be related. */
    boolean isCastable(Type from, Type to) {
        if (from.isErroneous() || to.isErroneous()) {
            return true;
        }
        if (from.isPrimitive() && to.isPrimitive()) {
            return (from.tag == Type.Tag.BOOLEAN) == (to.tag == Type.Tag.BOOLEAN);
        }
        if (from.isPrimitive()) {
            return isSubtype(symtab.boxedType(from), to) || symtab.unboxedType(to) != null && isAssignable(from, symtab.unboxedType(to), false, null);
        }
        if (to.isPrimitive()) {
            Type unboxed = symtab.unboxedType(from);
            if (unboxed != null) {
                return isPrimitiveWidening(unboxed, to);
            }
            Type eb = erased(from);
            return eb.tag == Type.Tag.CLASS && (((Type.ClassType) eb).sym == symtab.objectSym
                    || isSubClass(symtab.require(internalName(symtab.boxedType(to))), ((Type.ClassType) eb).sym));
        }
        Type ef = erased(from);
        Type et = erased(to);
        if (ef.tag == Type.Tag.NULL) {
            return true;
        }
        if (ef.tag == Type.Tag.ARRAY || et.tag == Type.Tag.ARRAY) {
            if (ef.tag == Type.Tag.ARRAY && et.tag == Type.Tag.ARRAY) {
                Type fe = ((Type.ArrayType) ef).elem;
                Type te = ((Type.ArrayType) et).elem;
                if (fe.isPrimitive() || te.isPrimitive()) {
                    return fe.tag == te.tag;
                }
                return isCastable(fe, te);
            }
            Type other = ef.tag == Type.Tag.ARRAY ? et : ef;
            return other.tag == Type.Tag.CLASS && isSubtype(new Type.ArrayType(symtab.objectType), other);
        }
        if (ef.tag != Type.Tag.CLASS || et.tag != Type.Tag.CLASS) {
            return true;
        }
        ClassSymbol a = ((Type.ClassType) ef).sym;
        ClassSymbol b = ((Type.ClassType) et).sym;
        if (isSubClass(a, b) || isSubClass(b, a)) {
            return true;
        }
        // Unrelated classes are castable only if one is an interface and the other is not final.
        if (a.isInterface() && !b.isFinal() || b.isInterface() && !a.isFinal() || a.isInterface() && b.isInterface()) {
            return true;
        }
        return false;
    }

    /** Binary numeric promotion. */
    static Type binaryPromote(Type a, Type b) {
        if (a.tag == Type.Tag.DOUBLE || b.tag == Type.Tag.DOUBLE) {
            return Type.DOUBLE;
        }
        if (a.tag == Type.Tag.FLOAT || b.tag == Type.Tag.FLOAT) {
            return Type.FLOAT;
        }
        if (a.tag == Type.Tag.LONG || b.tag == Type.Tag.LONG) {
            return Type.LONG;
        }
        return Type.INT;
    }

    static Type unaryPromote(Type a) {
        if (a.tag == Type.Tag.BYTE || a.tag == Type.Tag.SHORT || a.tag == Type.Tag.CHAR) {
            return Type.INT;
        }
        return a;
    }

    /** Unboxes a box type for arithmetic; answers the type itself for primitives, null otherwise. */
    Type unboxedOrSelf(Type t) {
        if (t.isPrimitive()) {
            return t;
        }
        Type e = t.tag == Type.Tag.TYPEVAR || t.tag == Type.Tag.INFERENCE ? erased(t) : t;
        return symtab.unboxedType(e);
    }

    /** Least upper bound (simplified): a common supertype of all, preferring a shared class. */
    Type lub(List<Type> types) {
        List<Type> refs = new ArrayList<Type>();
        for (Type t : types) {
            if (t != null && t.tag != Type.Tag.NULL && !t.isErroneous()) {
                refs.add(t.isPrimitive() ? symtab.boxedType(t) : t);
            }
        }
        if (refs.isEmpty()) {
            return types.isEmpty() ? symtab.objectType : types.get(0);
        }
        Type first = refs.get(0);
        boolean allSame = true;
        for (Type t : refs) {
            if (!isSameTypeStrict(t, first)) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            return first;
        }
        for (Type candidate : refs) {
            boolean ok = true;
            for (Type t : refs) {
                if (!isSubtypeStrict(t, candidate)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return candidate;
            }
        }
        // All common erased supertypes, minimized: one gives a class/interface type, several an intersection.
        Type e0 = erased(first);
        if (e0.tag == Type.Tag.CLASS) {
            Set<ClassSymbol> closure = new LinkedHashSet<ClassSymbol>();
            for (ClassSymbol c = ((Type.ClassType) e0).sym; c != null; ) {
                closure.add(c);
                Type sup = c.superclass();
                c = sup != null && sup.tag == Type.Tag.CLASS ? ((Type.ClassType) sup).sym : null;
            }
            collectInterfaces(((Type.ClassType) e0).sym, closure);
            List<ClassSymbol> common = new ArrayList<ClassSymbol>();
            for (ClassSymbol c : closure) {
                if (c == symtab.objectSym) {
                    continue;
                }
                boolean all = true;
                for (Type t : refs) {
                    if (asSuper(t, c) == null) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    common.add(c);
                }
            }
            List<ClassSymbol> minimal = new ArrayList<ClassSymbol>();
            for (ClassSymbol c : common) {
                boolean dominated = false;
                for (ClassSymbol d : common) {
                    if (d != c && isSubClass(d, c)) {
                        dominated = true;
                        break;
                    }
                }
                if (!dominated && !isMarker(c)) {
                    minimal.add(c);
                }
            }
            if (minimal.size() == 1) {
                return commonView(refs, minimal.get(0));
            }
            if (minimal.size() > 1) {
                List<Type> bounds = new ArrayList<Type>();
                for (ClassSymbol c : minimal) {
                    if (!c.isInterface()) {
                        bounds.add(0, commonView(refs, c));
                    } else {
                        bounds.add(commonView(refs, c));
                    }
                }
                return new Type.IntersectionType(bounds);
            }
        }
        // Walk the erased superclass chain of the first type.
        Type e = erased(first);
        if (e.tag == Type.Tag.CLASS) {
            for (ClassSymbol c = ((Type.ClassType) e).sym; c != null; ) {
                boolean all = true;
                for (Type t : refs) {
                    if (asSuper(t, c) == null) {
                        all = false;
                        break;
                    }
                }
                if (all && c != symtab.objectSym) {
                    Type.ClassType view = asSuper(first, c);
                    return view != null ? view : c.erasure();
                }
                Type sup = c.superclass();
                c = sup != null && sup.tag == Type.Tag.CLASS ? ((Type.ClassType) sup).sym : null;
            }
            // A single interface they all share.
            Set<ClassSymbol> itfs = new LinkedHashSet<ClassSymbol>();
            collectInterfaces(((Type.ClassType) e).sym, itfs);
            for (ClassSymbol i : itfs) {
                boolean all = true;
                for (Type t : refs) {
                    if (asSuper(t, i) == null) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    Type.ClassType view = asSuper(first, i);
                    return view != null ? view : i.erasure();
                }
            }
        }
        return symtab.objectType;
    }

    /** Serializable/Cloneable/Comparable-only commonality is noise javac also drops from an inferred type's display. */
    private static boolean isMarker(ClassSymbol c) {
        String n = c.internalName;
        return n.equals("java/io/Serializable") || n.equals("java/lang/Cloneable") || n.equals("java/lang/constant/Constable")
                || n.equals("java/lang/constant/ConstantDesc");
    }

    /** c as seen from every type in refs: parameterized when they all agree, raw otherwise. */
    private Type commonView(List<Type> refs, ClassSymbol c) {
        Type.ClassType first = asSuper(refs.get(0), c);
        if (first == null || first.args.isEmpty()) {
            return c.erasure();
        }
        for (Type t : refs) {
            Type.ClassType v = asSuper(t, c);
            if (v == null || v.args.size() != first.args.size()) {
                return c.erasure();
            }
            for (int i = 0; i < v.args.size(); i++) {
                if (!isSameTypeStrict(v.args.get(i), first.args.get(i))) {
                    // Differing arguments: ? extends lub would be exact; the raw class is the safe approximation.
                    List<Type> wild = new ArrayList<Type>();
                    for (int k = 0; k < first.args.size(); k++) {
                        wild.add(new Type.WildcardType(Type.WildcardType.UNBOUND, null));
                    }
                    return new Type.ClassType(c, wild, null);
                }
            }
        }
        return first;
    }

    private void collectInterfaces(ClassSymbol c, Set<ClassSymbol> out) {
        for (Type i : c.interfaces()) {
            if (i.tag == Type.Tag.CLASS && out.add(((Type.ClassType) i).sym)) {
                collectInterfaces(((Type.ClassType) i).sym, out);
            }
        }
        Type sup = c.superclass();
        if (sup != null && sup.tag == Type.Tag.CLASS) {
            collectInterfaces(((Type.ClassType) sup).sym, out);
        }
    }

    /** Subtyping that does not record inference bounds. */
    boolean isSubtypeStrict(Type s, Type t) {
        if (containsInference(s) || containsInference(t)) {
            return isSubtype(erased(s), erased(t));
        }
        return isSubtype(s, t);
    }

    private boolean isSameTypeStrict(Type a, Type b) {
        if (containsInference(a) || containsInference(b)) {
            return a == b;
        }
        return isSameType(a, b);
    }

    static boolean containsInference(Type t) {
        if (t == null) {
            return false;
        }
        switch (t.tag) {
            case INFERENCE:
                return ((Type.InferenceVar) t).inst == null || containsInference(((Type.InferenceVar) t).inst);
            case CLASS:
                for (Type a : ((Type.ClassType) t).args) {
                    if (containsInference(a)) {
                        return true;
                    }
                }
                return ((Type.ClassType) t).outer != null && containsInference(((Type.ClassType) t).outer);
            case ARRAY:
                return containsInference(((Type.ArrayType) t).elem);
            case WILDCARD:
                return containsInference(((Type.WildcardType) t).bound);
            case INTERSECTION:
                for (Type b : ((Type.IntersectionType) t).bounds) {
                    if (containsInference(b)) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    /** Replaces solved inference variables by their instantiation. */
    Type resolveInference(Type t) {
        if (t == null) {
            return null;
        }
        switch (t.tag) {
            case INFERENCE: {
                Type.InferenceVar iv = (Type.InferenceVar) t;
                return iv.inst != null ? resolveInference(iv.inst) : t;
            }
            case CLASS: {
                Type.ClassType c = (Type.ClassType) t;
                if (c.args.isEmpty()) {
                    return c;
                }
                List<Type> args = new ArrayList<Type>();
                boolean changed = false;
                for (Type a : c.args) {
                    Type r = resolveInference(a);
                    changed |= r != a;
                    args.add(r);
                }
                return changed ? new Type.ClassType(c.sym, args, c.outer) : c;
            }
            case ARRAY: {
                Type e = resolveInference(((Type.ArrayType) t).elem);
                return e == ((Type.ArrayType) t).elem ? t : new Type.ArrayType(e);
            }
            case WILDCARD: {
                Type.WildcardType w = (Type.WildcardType) t;
                if (w.bound == null) {
                    return w;
                }
                Type b = resolveInference(w.bound);
                return b == w.bound ? w : new Type.WildcardType(w.kind, b);
            }
            default:
                return t;
        }
    }

    // ------------------------------------------------------------------ members

    /** The type of a member seen from {@code site}: the owner's type parameters replaced by site's arguments. */
    Map<Type.TypeVar, Type> memberMapping(Type site, ClassSymbol owner) {
        Map<Type.TypeVar, Type> map = new IdentityHashMap<Type.TypeVar, Type>();
        if (site == null) {
            return map;
        }
        Type.ClassType view = asSuper(site, owner);
        if (view == null || view.args.isEmpty()) {
            // Raw or non-generic: owner's type variables erase.
            if (view != null && view.isRaw()) {
                for (Type.TypeVar tv : owner.typeParams()) {
                    map.put(tv, erased(tv));
                }
            }
            return map;
        }
        List<Type.TypeVar> params = owner.typeParams();
        for (int i = 0; i < params.size() && i < view.args.size(); i++) {
            Type arg = view.args.get(i);
            // A wildcard argument is seen through its bound (a simplified capture).
            if (arg.tag == Type.Tag.WILDCARD) {
                Type.WildcardType w = (Type.WildcardType) arg;
                arg = w.kind == Type.WildcardType.EXTENDS ? w.bound
                        : w.kind == Type.WildcardType.SUPER ? new Type.WildcardType(Type.WildcardType.SUPER, w.bound)
                        : params.get(i).bound == null ? symtab.objectType : params.get(i).bound;
            }
            map.put(params.get(i), arg);
        }
        for (Type.ClassType o = view.outer; o != null; o = o.outer) {
            List<Type.TypeVar> op = o.sym.typeParams();
            for (int i = 0; i < op.size() && i < o.args.size(); i++) {
                map.put(op.get(i), o.args.get(i));
            }
        }
        return map;
    }

    /** The single abstract method of a functional interface, or null. */
    MethodSymbol functionalMethod(ClassSymbol itf) {
        if (!itf.isInterface()) {
            return null;
        }
        List<MethodSymbol> abstracts = new ArrayList<MethodSymbol>();
        collectAbstract(itf, abstracts, new LinkedHashSet<ClassSymbol>());
        MethodSymbol found = null;
        for (MethodSymbol m : abstracts) {
            if (isObjectMethod(m)) {
                continue;
            }
            if (found == null) {
                found = m;
            } else if (!(found.name.equals(m.name) && found.params.size() == m.params.size())) {
                return null;
            }
        }
        return found;
    }

    private void collectAbstract(ClassSymbol c, List<MethodSymbol> out, Set<ClassSymbol> seen) {
        if (!seen.add(c)) {
            return;
        }
        for (MethodSymbol m : c.methods()) {
            if ((m.flags & Symbol.ACC_ABSTRACT) != 0 && (m.flags & Symbol.ACC_STATIC) == 0) {
                boolean overridden = false;
                for (MethodSymbol o : out) {
                    if (o.name.equals(m.name) && o.params.size() == m.params.size()) {
                        overridden = true;
                    }
                }
                if (!overridden && !hasDefaultFor(seen, m)) {
                    out.add(m);
                }
            }
        }
        for (Type i : c.interfaces()) {
            if (i.tag == Type.Tag.CLASS) {
                collectAbstract(((Type.ClassType) i).sym, out, seen);
            }
        }
    }

    private boolean hasDefaultFor(Set<ClassSymbol> seen, MethodSymbol m) {
        for (ClassSymbol c : seen) {
            for (MethodSymbol d : c.methods()) {
                if ((d.flags & Symbol.DEFAULT_METHOD) != 0 && d.name.equals(m.name) && d.params.size() == m.params.size()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isObjectMethod(MethodSymbol m) {
        for (MethodSymbol om : symtab.objectSym.methods()) {
            if (om.name.equals(m.name) && om.params.size() == m.params.size() && (om.flags & Symbol.ACC_PUBLIC) != 0) {
                boolean same = true;
                for (int i = 0; i < om.params.size(); i++) {
                    if (!erased(om.params.get(i)).toString().equals(erased(m.params.get(i)).toString())) {
                        same = false;
                    }
                }
                if (same) {
                    return true;
                }
            }
        }
        return false;
    }
}
