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

/** Parses JVM generic signatures (JVMS 4.7.9.1) and plain descriptors into types. */
final class SignatureParser {
    private final Symtab symtab;
    private String s;
    private int i;
    /** Type variables in scope, innermost first: method's, then the class's, then outer classes'. */
    private final List<Type.TypeVar> scope = new ArrayList<Type.TypeVar>();

    SignatureParser(Symtab symtab) {
        this.symtab = symtab;
    }

    private void scopeFor(ClassSymbol c) {
        scope.clear();
        for (ClassSymbol k = c; k != null; k = k.outer) {
            if (k != c) {
                k.complete();
            }
            scope.addAll(k == c ? classParamsUnsafe(k) : k.typeParams());
        }
    }

    /** During a class's own completion its parameters are being built; read them without completing. */
    private List<Type.TypeVar> classParamsUnsafe(ClassSymbol c) {
        return c.completing ? pending : c.typeParams();
    }

    private List<Type.TypeVar> pending = new ArrayList<Type.TypeVar>();

    void classSignature(ClassSymbol c, String sig) {
        s = sig;
        i = 0;
        pending = new ArrayList<Type.TypeVar>();
        scopeFor(c);
        if (peek() == '<') {
            List<Type.TypeVar> params = typeParameters();
            pending = params;
            c.typeParams().addAll(params);
            // Bounds may refer to the parameters themselves.
            scope.addAll(0, params);
        }
        c.setSuperclass(classTypeSignature());
        while (i < s.length()) {
            c.interfaces().add(classTypeSignature());
        }
    }

    void methodSignature(MethodSymbol m, String sig, ClassSymbol owner) {
        s = sig;
        i = 0;
        scopeFor(owner);
        if (peek() == '<') {
            List<Type.TypeVar> params = typeParameters();
            m.typeParams.addAll(params);
        }
        scope.addAll(0, m.typeParams);
        resolvePendingBounds(m.typeParams);
        expect('(');
        while (peek() != ')') {
            m.params.add(typeSignature());
        }
        expect(')');
        m.returnType = peek() == 'V' ? next(Type.VOID) : typeSignature();
        while (i < s.length() && peek() == '^') {
            i++;
            m.thrown.add(typeSignature());
        }
    }

    Type fieldSignature(String sig, ClassSymbol owner, MethodSymbol method) {
        s = sig;
        i = 0;
        scopeFor(owner);
        if (method != null) {
            scope.addAll(0, method.typeParams);
        }
        return typeSignature();
    }

    private Type next(Type t) {
        i++;
        return t;
    }

    private char peek() {
        return s.charAt(i);
    }

    private void expect(char c) {
        if (s.charAt(i) != c) {
            throw new IllegalArgumentException("Bad signature " + s + " at " + i);
        }
        i++;
    }

    /** Bounds are parsed after all parameters are known (they may be mutually recursive). */

    private List<Type.TypeVar> typeParameters() {
        expect('<');
        List<Type.TypeVar> params = new ArrayList<Type.TypeVar>();
        List<int[]> boundRanges = new ArrayList<int[]>();
        while (peek() != '>') {
            int colon = s.indexOf(':', i);
            Type.TypeVar tv = new Type.TypeVar(s.substring(i, colon));
            params.add(tv);
            i = colon;
            int start = i;
            // Skip the bounds; they are parsed once every parameter of the list exists.
            while (peek() == ':') {
                i++;
                if (peek() != ':') {
                    skipFieldType();
                }
            }
            boundRanges.add(new int[]{start, i});
        }
        expect('>');
        int resume = i;
        scope.addAll(0, params);
        for (int k = 0; k < params.size(); k++) {
            i = boundRanges.get(k)[0];
            int end = boundRanges.get(k)[1];
            List<Type> bounds = new ArrayList<Type>();
            while (i < end) {
                expect(':');
                if (i < end && peek() != ':') {
                    bounds.add(typeSignature());
                }
            }
            Type.TypeVar tv = params.get(k);
            if (bounds.isEmpty()) {
                tv.bound = symtab.objectType;
            } else if (bounds.size() == 1) {
                tv.bound = bounds.get(0);
            } else {
                tv.bound = new Type.IntersectionType(bounds);
            }
        }
        scope.subList(0, params.size()).clear();
        i = resume;
        return params;
    }

    private void resolvePendingBounds(List<Type.TypeVar> params) {
        // Bounds were resolved in typeParameters(); nothing pending.
    }

    private void skipFieldType() {
        char c = peek();
        if (c == 'L') {
            int depth = 0;
            while (true) {
                char d = s.charAt(i++);
                if (d == '<') {
                    depth++;
                } else if (d == '>') {
                    depth--;
                } else if (d == ';' && depth == 0) {
                    return;
                }
            }
        } else if (c == 'T') {
            i = s.indexOf(';', i) + 1;
        } else if (c == '[') {
            i++;
            skipFieldType();
        } else {
            i++;
        }
    }

    Type typeSignature() {
        char c = peek();
        switch (c) {
            case 'Z': return next(Type.BOOLEAN);
            case 'B': return next(Type.BYTE);
            case 'S': return next(Type.SHORT);
            case 'C': return next(Type.CHAR);
            case 'I': return next(Type.INT);
            case 'J': return next(Type.LONG);
            case 'F': return next(Type.FLOAT);
            case 'D': return next(Type.DOUBLE);
            case 'V': return next(Type.VOID);
            case '[':
                i++;
                return new Type.ArrayType(typeSignature());
            case 'T': {
                int semi = s.indexOf(';', i);
                String name = s.substring(i + 1, semi);
                i = semi + 1;
                for (Type.TypeVar tv : scope) {
                    if (tv.name.equals(name)) {
                        return tv;
                    }
                }
                return symtab.objectType;
            }
            case 'L':
                return classTypeSignature();
            default:
                throw new IllegalArgumentException("Bad signature " + s + " at " + i);
        }
    }

    private Type.ClassType classTypeSignature() {
        expect('L');
        StringBuilder name = new StringBuilder();
        Type.ClassType result = null;
        while (true) {
            char c = peek();
            if (c == ';') {
                i++;
                if (result == null) {
                    return symtab.type(name.toString());
                }
                return result;
            }
            if (c == '<') {
                ClassSymbol sym = symtab.require(name.toString());
                List<Type> args = typeArguments();
                result = new Type.ClassType(sym, args, result);
                continue;
            }
            if (c == '.') {
                i++;
                if (result == null) {
                    result = symtab.type(name.toString());
                }
                name.append('$');
                Type.ClassType outerType = result;
                int start = i;
                while (peek() != '<' && peek() != ';' && peek() != '.') {
                    i++;
                }
                name.append(s, start, i);
                ClassSymbol inner = symtab.require(name.toString());
                if (peek() == '<') {
                    List<Type> args = typeArguments();
                    result = new Type.ClassType(inner, args, outerType.args.isEmpty() ? null : outerType);
                } else {
                    result = new Type.ClassType(inner, new ArrayList<Type>(), outerType.args.isEmpty() ? null : outerType);
                }
                continue;
            }
            name.append(c);
            i++;
        }
    }

    private List<Type> typeArguments() {
        expect('<');
        List<Type> args = new ArrayList<Type>();
        while (peek() != '>') {
            char c = peek();
            if (c == '*') {
                i++;
                args.add(new Type.WildcardType(Type.WildcardType.UNBOUND, null));
            } else if (c == '+') {
                i++;
                args.add(new Type.WildcardType(Type.WildcardType.EXTENDS, typeSignature()));
            } else if (c == '-') {
                i++;
                args.add(new Type.WildcardType(Type.WildcardType.SUPER, typeSignature()));
            } else {
                args.add(typeSignature());
            }
        }
        expect('>');
        return args;
    }

    Type descriptorType(String desc) {
        s = desc;
        i = 0;
        scope.clear();
        return typeSignature();
    }

    List<Type> descriptorParams(String desc) {
        s = desc;
        i = 1;
        scope.clear();
        List<Type> out = new ArrayList<Type>();
        while (peek() != ')') {
            out.add(typeSignature());
        }
        return out;
    }
}
