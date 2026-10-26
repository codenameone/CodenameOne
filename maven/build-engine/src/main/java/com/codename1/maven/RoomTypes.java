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
package com.codename1.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// Java types as the Room generator reads them from class-file descriptors
/// and generic signatures, and writes them back as source.
final class RoomTypes {

    private RoomTypes() {
    }

    /// A type: a primitive (`kind` is its descriptor letter), a class
    /// (`kind` 'L', with type arguments) or a type variable ('T'), with array
    /// dimensions and, as a type argument, a wildcard bound ('+', '-', '*').
    static final class JType {
        final char kind;
        final String name;
        final List<JType> args;
        final int dims;
        final char wildcard;

        JType(char kind, String name, List<JType> args, int dims, char wildcard) {
            this.kind = kind;
            this.name = name;
            this.args = args == null ? Collections.<JType>emptyList() : args;
            this.dims = dims;
            this.wildcard = wildcard;
        }

        static JType cls(String internal) {
            return new JType('L', internal, null, 0, (char) 0);
        }

        boolean isPrimitive() {
            return dims == 0 && kind != 'L' && kind != 'T';
        }

        boolean isVoid() {
            return dims == 0 && kind == 'V';
        }

        boolean isClass(String internal) {
            return dims == 0 && kind == 'L' && name.equals(internal);
        }

        boolean isArray() {
            return dims > 0;
        }

        JType element() {
            return new JType(kind, name, args, dims - 1, (char) 0);
        }

        JType arg(int i) {
            JType a = args.get(i);
            return a.wildcard == 0 ? a : new JType(a.kind, a.name, a.args, a.dims, (char) 0);
        }

        boolean hasTypeVariables() {
            if (kind == 'T') {
                return true;
            }
            for (JType a : args) {
                if (a != null && a.hasTypeVariables()) {
                    return true;
                }
            }
            return false;
        }

        JType subst(Map<String, JType> binding) {
            if (kind == 'T') {
                JType b = binding.get(name);
                if (b == null) {
                    return this;
                }
                return new JType(b.kind, b.name, b.args, b.dims + dims, wildcard);
            }
            if (args.isEmpty()) {
                return this;
            }
            List<JType> out = new ArrayList<JType>();
            for (JType a : args) {
                out.add(a == null ? null : a.subst(binding));
            }
            return new JType(kind, name, out, dims, wildcard);
        }

        /// The erased descriptor.
        String desc() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dims; i++) {
                sb.append('[');
            }
            if (kind == 'L') {
                sb.append('L').append(name).append(';');
            } else if (kind == 'T') {
                sb.append("Ljava/lang/Object;");
            } else {
                sb.append(kind);
            }
            return sb.toString();
        }

        /// Java source, generics included; primitives boxed when `boxed`.
        String src(boolean boxed) {
            StringBuilder sb = new StringBuilder();
            if (wildcard == '*') {
                return "?";
            }
            if (wildcard == '+') {
                sb.append("? extends ");
            } else if (wildcard == '-') {
                sb.append("? super ");
            }
            if (kind == 'L') {
                sb.append(sourceName(name));
                if (!args.isEmpty()) {
                    sb.append('<');
                    for (int i = 0; i < args.size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(args.get(i).src(true));
                    }
                    sb.append('>');
                }
            } else if (kind == 'T') {
                sb.append(name);
            } else if (dims == 0 && boxed) {
                sb.append(boxName(kind));
            } else {
                sb.append(primitiveName(kind));
            }
            for (int i = 0; i < dims; i++) {
                sb.append("[]");
            }
            return sb.toString();
        }

        String src() {
            return src(false);
        }

        /// The source of the erased type, for class literals and casts.
        String rawSrc() {
            StringBuilder sb = new StringBuilder(kind == 'L' ? sourceName(name)
                    : kind == 'T' ? "Object" : primitiveName(kind));
            for (int i = 0; i < dims; i++) {
                sb.append("[]");
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof JType && ((JType) o).src().equals(src());
        }

        @Override
        public int hashCode() {
            return src().hashCode();
        }

        @Override
        public String toString() {
            return src();
        }
    }

    static String sourceName(String internal) {
        return internal.replace('/', '.').replace('$', '.');
    }

    static String primitiveName(char c) {
        switch (c) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'F': return "float";
            case 'D': return "double";
            case 'V': return "void";
            default: throw new IllegalArgumentException("Not a primitive: " + c);
        }
    }

    static String boxName(char c) {
        switch (c) {
            case 'Z': return "Boolean";
            case 'B': return "Byte";
            case 'C': return "Character";
            case 'S': return "Short";
            case 'I': return "Integer";
            case 'J': return "Long";
            case 'F': return "Float";
            case 'D': return "Double";
            case 'V': return "Void";
            default: throw new IllegalArgumentException("Not a primitive: " + c);
        }
    }

    /// The primitive a box holds, or 0.
    static char unboxed(JType t) {
        if (t.dims != 0 || t.kind != 'L') {
            return 0;
        }
        String n = t.name;
        if (n.equals("java/lang/Boolean")) {
            return 'Z';
        } else if (n.equals("java/lang/Byte")) {
            return 'B';
        } else if (n.equals("java/lang/Character")) {
            return 'C';
        } else if (n.equals("java/lang/Short")) {
            return 'S';
        } else if (n.equals("java/lang/Integer")) {
            return 'I';
        } else if (n.equals("java/lang/Long")) {
            return 'J';
        } else if (n.equals("java/lang/Float")) {
            return 'F';
        } else if (n.equals("java/lang/Double")) {
            return 'D';
        }
        return 0;
    }

    /// A method's generic signature, or its descriptor when it has none.
    static final class MethodSig {
        final List<String> typeParams = new ArrayList<String>();
        final List<JType> params = new ArrayList<JType>();
        JType ret;
        final List<JType> exceptions = new ArrayList<JType>();
    }

    /// A class signature: its type parameters, superclass and interfaces.
    static final class ClassSig {
        final List<String> typeParams = new ArrayList<String>();
        JType superclass;
        final List<JType> interfaces = new ArrayList<JType>();
    }

    static MethodSig parseMethod(String sig) {
        Parser p = new Parser(sig);
        MethodSig m = new MethodSig();
        p.typeParams(m.typeParams);
        p.expect('(');
        while (p.peek() != ')') {
            m.params.add(p.type());
        }
        p.expect(')');
        m.ret = p.type();
        while (p.pos < sig.length() && p.peek() == '^') {
            p.pos++;
            m.exceptions.add(p.type());
        }
        return m;
    }

    static ClassSig parseClass(String sig) {
        Parser p = new Parser(sig);
        ClassSig c = new ClassSig();
        p.typeParams(c.typeParams);
        c.superclass = p.type();
        while (p.pos < sig.length()) {
            c.interfaces.add(p.type());
        }
        return c;
    }

    static JType parseField(String sig) {
        return new Parser(sig).type();
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) {
            this.s = s;
        }

        char peek() {
            return s.charAt(pos);
        }

        void expect(char c) {
            if (s.charAt(pos) != c) {
                throw new IllegalArgumentException("Bad signature " + s + " at " + pos);
            }
            pos++;
        }

        void typeParams(List<String> out) {
            if (pos >= s.length() || peek() != '<') {
                return;
            }
            pos++;
            while (peek() != '>') {
                int colon = s.indexOf(':', pos);
                out.add(s.substring(pos, colon));
                pos = colon;
                while (peek() == ':') {
                    pos++;
                    if (peek() != ':' && peek() != '>') {
                        char c = peek();
                        if (c == 'L' || c == 'T' || c == '[') {
                            type();
                        }
                    }
                }
            }
            pos++;
        }

        JType type() {
            int dims = 0;
            while (peek() == '[') {
                dims++;
                pos++;
            }
            char c = s.charAt(pos++);
            if (c == 'T') {
                int semi = s.indexOf(';', pos);
                String n = s.substring(pos, semi);
                pos = semi + 1;
                return new JType('T', n, null, dims, (char) 0);
            }
            if (c != 'L') {
                return new JType(c, null, null, dims, (char) 0);
            }
            StringBuilder name = new StringBuilder();
            List<JType> args = null;
            while (true) {
                char ch = s.charAt(pos++);
                if (ch == ';') {
                    break;
                }
                if (ch == '<') {
                    args = new ArrayList<JType>();
                    while (peek() != '>') {
                        char w = peek();
                        if (w == '*') {
                            pos++;
                            args.add(new JType('L', "java/lang/Object", null, 0, '*'));
                        } else if (w == '+' || w == '-') {
                            pos++;
                            JType t = type();
                            args.add(new JType(t.kind, t.name, t.args, t.dims, w));
                        } else {
                            args.add(type());
                        }
                    }
                    pos++;
                } else if (ch == '.') {
                    // An inner class of a parameterized outer: keep the
                    // innermost arguments only.
                    name.append('$');
                    args = null;
                } else {
                    name.append(ch);
                }
            }
            return new JType('L', name.toString(), args, dims, (char) 0);
        }
    }
}
