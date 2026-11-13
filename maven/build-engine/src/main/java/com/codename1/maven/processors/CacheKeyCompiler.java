/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

/// Compiles the key expression of `@Cacheable`, `@CachePut` and `@CacheEvict`
/// into a Java expression over the helper's arguments `a0..aN`.
///
/// The SpEL subset Spring code keys caches with, and nothing more: a parameter
/// by name (`#id`) or position (`#p0`, `#a0`), a chain of properties of one
/// (`#order.customer.id`, each a public getter, record accessor or public
/// field), a quoted literal (`'all'`), a number, `#root.methodName`, joined with
/// `+`. Anything else is refused with the reason, which the build reports: a
/// key that silently meant something else would be a cache answering one
/// caller with another's data.
final class CacheKeyCompiler {
    private final ProcessorContext ctx;
    private final MethodInfo method;
    private final Type[] args;
    private final Map<String, Integer> names;

    CacheKeyCompiler(ProcessorContext ctx, MethodInfo method, Type[] args,
                     Map<String, Integer> names) {
        this.ctx = ctx;
        this.method = method;
        this.args = args;
        this.names = names;
    }

    /// Thrown with why an expression cannot be compiled.
    static final class Refused extends Exception {
        private static final long serialVersionUID = 1L;

        Refused(String message) {
            super(message);
        }
    }

    /// The Java expression, a `String`, that `expression` keys by; empty means
    /// Spring's default key over every parameter.
    String compile(String expression) throws Refused {
        String e = expression == null ? "" : expression.trim();
        if (e.length() == 0) {
            StringBuilder sb = new StringBuilder(
                    "com.codename1.impl.backend.CacheRuntime.key(new Object[] {");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                keyable(args[i], "parameter " + (i + 1));
                sb.append(box(args[i], "a" + i));
            }
            return sb.append("})").toString();
        }
        List<String> terms = split(e);
        StringBuilder sb = new StringBuilder();
        for (String term : terms) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(term(term.trim()));
        }
        // "" + makes a lone literal a String; every parameter term is already one.
        return terms.size() == 1 ? sb.toString() : "(\"\" + " + sb + ")";
    }

    /// The terms between top-level `+`, with quoted text kept whole.
    private static List<String> split(String e) throws Refused {
        List<String> out = new ArrayList<String>();
        int start = 0;
        boolean quoted = false;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (c == '+' && !quoted) {
                out.add(e.substring(start, i));
                start = i + 1;
            }
        }
        if (quoted) {
            throw new Refused("a quoted literal is not closed");
        }
        out.add(e.substring(start));
        for (String t : out) {
            if (t.trim().length() == 0) {
                throw new Refused("a '+' has nothing on one side");
            }
        }
        return out;
    }

    private String term(String t) throws Refused {
        if (t.length() >= 2 && t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\'') {
            String text = t.substring(1, t.length() - 1);
            if (text.indexOf('\'') >= 0) {
                throw new Refused("a literal cannot contain a quote");
            }
            return BackendSources.quote(text);
        }
        if (isNumber(t)) {
            return BackendSources.quote(t);
        }

        if (t.length() < 2 || t.charAt(0) != '#') {
            throw new Refused("\"" + t + "\" is not a parameter (#name), a quoted literal or "
                    + "#root.methodName");
        }
        String[] path = t.substring(1).split("\\.", -1);
        if ("root".equals(path[0])) {
            if (path.length == 2 && "methodName".equals(path[1])) {
                return BackendSources.quote(method.getName());
            }
            throw new Refused("of #root only #root.methodName is supported");
        }
        int index = parameter(path[0]);
        StringBuilder expr = new StringBuilder("a").append(index);
        Type type = args[index];
        for (int i = 1; i < path.length; i++) {
            String prop = path[i];
            if (!isIdentifier(prop)) {
                throw new Refused("\"" + t + "\" is not a chain of property names; method "
                        + "calls, indexes and operators are not supported");
            }
            if (type.getSort() != Type.OBJECT) {
                throw new Refused("\"" + prop + "\" is read from a " + type.getClassName()
                        + ", which has no properties");
            }
            String[] step = property(type.getInternalName(), prop);
            if (step == null) {
                throw new Refused(type.getClassName() + " has no public getter, accessor or "
                        + "field \"" + prop + "\"");
            }
            expr.append('.').append(step[0]);
            type = Type.getType(step[1]);
        }
        keyable(type, "\"" + t + "\"");
        return "com.codename1.impl.backend.CacheRuntime.part(" + box(type, expr.toString()) + ")";
    }

    private int parameter(String name) throws Refused {
        Integer at = names.get(name);
        if (at != null) {
            return at.intValue();
        }
        if (name.length() > 1 && (name.charAt(0) == 'p' || name.charAt(0) == 'a')
                && isNumber(name.substring(1))) {
            int i = Integer.parseInt(name.substring(1));
            if (i < args.length) {
                return i;
            }
        }
        throw new Refused("\"#" + name + "\" names no parameter of " + method.getName()
                + (names.isEmpty() ? "; the class was compiled without parameter names, so "
                        + "use #p0, #p1 ... or compile with -parameters" : ""));
    }

    /// Refuses a key part whose text does not tell two values apart: an
    /// array, or an object without a `toString()` of its own, keys by an
    /// identity hash -- a new key on every call that never hits, and that
    /// after a restart can be the key another object's result was kept under.
    private void keyable(Type type, String what) throws Refused {
        if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
            return;
        }
        String hint = "; key by one of its properties instead, for example key = \"#p0.id\"";
        if (type.getSort() == Type.ARRAY) {
            throw new Refused(what + " is an array, whose text is its identity" + hint);
        }
        String n = type.getInternalName();
        if (VALUE_TYPES.contains(n)) {
            return;
        }
        String current = n;
        for (int depth = 0; current != null && !"java/lang/Object".equals(current)
                && depth < 32; depth++) {
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx, current);
            if (c == null) {
                break;
            }
            if (c.isEnum() || c.isRecord()) {
                return;
            }
            for (MethodInfo m : c.getMethods()) {
                if ("toString".equals(m.getName()) && "()Ljava/lang/String;".equals(m.getDescriptor())
                        && !m.isStatic()) {
                    return;
                }
            }
            current = c.getSuperInternalName();
        }
        throw new Refused(what + " is a " + type.getClassName() + ", which has no toString() "
                + "of its own, so its text is its identity" + hint);
    }

    private static final java.util.Set<String> VALUE_TYPES = new java.util.HashSet<String>(
            java.util.Arrays.asList("java/lang/String", "java/lang/Boolean", "java/lang/Character",
                    "java/lang/Byte", "java/lang/Short", "java/lang/Integer", "java/lang/Long",
                    "java/lang/Float", "java/lang/Double", "java/math/BigInteger",
                    "java/math/BigDecimal", "java/util/UUID", "java/lang/CharSequence"));

    /// The Java member access reading `prop` of `owner` and its descriptor, or
    /// null when it has none the helper may read.
    private String[] property(String owner, String prop) {
        String cap = Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
        String current = owner;
        for (int depth = 0; current != null && depth < 32; depth++) {
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx, current);
            if (c == null) {
                return null;
            }
            String[] names = {"get" + cap, "is" + cap, prop};
            for (String n : names) {
                for (MethodInfo m : c.getMethods()) {
                    if (m.isPublic() && !m.isStatic() && n.equals(m.getName())
                            && m.getDescriptor().startsWith("()")
                            && !m.getDescriptor().endsWith(")V")
                            && (!n.startsWith("is") || n.equals(prop)
                                || m.getDescriptor().equals("()Z"))) {
                        return new String[] {n + "()", Type.getReturnType(m.getDescriptor())
                                .getDescriptor()};
                    }
                }
            }
            for (FieldInfo f : c.getFields()) {
                if (f.isPublic() && !f.isStatic() && prop.equals(f.getName())) {
                    return new String[] {prop, f.getDescriptor()};
                }
            }
            current = c.getSuperInternalName();
        }
        return null;
    }

    private static String box(Type t, String expr) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "Boolean.valueOf(" + expr + ")";
            case Type.CHAR: return "Character.valueOf(" + expr + ")";
            case Type.BYTE: return "Byte.valueOf(" + expr + ")";
            case Type.SHORT: return "Short.valueOf(" + expr + ")";
            case Type.INT: return "Integer.valueOf(" + expr + ")";
            case Type.LONG: return "Long.valueOf(" + expr + ")";
            case Type.FLOAT: return "Float.valueOf(" + expr + ")";
            case Type.DOUBLE: return "Double.valueOf(" + expr + ")";
            default: return expr;
        }
    }

    private static boolean isNumber(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifier(String s) {
        if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
