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

import com.codename1.maven.annotations.*;

import org.objectweb.asm.Type;

import java.util.*;

/** A deliberately small, typed expression language. Its output is Java, not a runtime AST. */
final class MvcExpression {
    static final String HTML = "com.codename1.backend.mvc.Html.";

    static final class Value {
        final String code, type;

        Value(String code, String type) {
            this.code = code;
            this.type = type;
        }
    }

    private final ProcessorContext ctx;
    private final Map<String, Value> names;
    private final List<String> tokens = new ArrayList<String>();
    private int at;

    MvcExpression(ProcessorContext ctx, Map<String, Value> names) {
        this.ctx = ctx;
        this.names = names;
    }

    Value parse(String expression) {
        tokens.clear();
        at = 0;
        tokenize(expression);
        Value v = conditional();
        if (at != tokens.size()) throw error("Unexpected token " + tokens.get(at));
        return v;
    }

    private void tokenize(String s) {
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i++;
            if (c == '\'' || c == '"') {
                boolean ended = false;
                while (i < s.length()) {
                    char n = s.charAt(i++);
                    if (n == '\\') {
                        if (i < s.length()) i++;
                    } else if (n == c) {
                        ended = true;
                        break;
                    }
                }
                if (!ended) throw error("Unclosed string");
            } else if (Character.isJavaIdentifierStart(c)) {
                while (i < s.length() && Character.isJavaIdentifierPart(s.charAt(i))) i++;
            } else if (Character.isDigit(c)) {
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.'))
                    i++;
            } else if (i < s.length()
                    && Arrays.asList("==", "!=", "<=", ">=", "&&", "||")
                            .contains(s.substring(start, i + 1))) i++;
            else if (".[]()?:!<>+-".indexOf(c) < 0)
                throw error("Unsupported expression character " + c);
            tokens.add(s.substring(start, i));
        }
    }

    private Value conditional() {
        Value test = binary(0);
        if (take("?")) {
            Value yes = conditional();
            need(":");
            Value no = conditional();
            return new Value(
                    "(" + truth(test) + " ? " + yes.code + " : " + no.code + ")",
                    yes.type.equals(no.type) ? yes.type : "java.lang.Object");
        }
        return test;
    }

    private static final String[][] OPS = {
        {"||", "or"},
        {"&&", "and"},
        {"==", "!=", "eq", "ne"},
        {"<", ">", "<=", ">=", "lt", "gt", "le", "ge"},
        {"+", "-"}
    };

    private Value binary(int level) {
        if (level == OPS.length) return unary();
        Value left = binary(level + 1);
        while (at < tokens.size() && Arrays.asList(OPS[level]).contains(tokens.get(at))) {
            String op = tokens.get(at++);
            Value right = binary(level + 1);
            if (level < 2)
                left =
                        new Value(
                                "("
                                        + truth(left)
                                        + (level == 0 ? " || " : " && ")
                                        + truth(right)
                                        + ")",
                                "boolean");
            else if (level == 2) {
                String eq =
                        numeric(left.type) && numeric(right.type)
                                ? numericEquality(left, right)
                                : HTML + "equal(" + left.code + ", " + right.code + ")";
                left = new Value((op.equals("!=") || op.equals("ne") ? "!" : "") + eq, "boolean");
            } else {
                if (op.equals("lt")) op = "<";
                if (op.equals("gt")) op = ">";
                if (op.equals("le")) op = "<=";
                if (op.equals("ge")) op = ">=";
                if (level == 3 && (!numeric(left.type) || !numeric(right.type)))
                    throw error("Ordering requires numeric operands");
                if (level == 4
                        && !(numeric(left.type) && numeric(right.type))
                        && !(op.equals("+")
                                && (left.type.equals("java.lang.String")
                                        || right.type.equals("java.lang.String"))))
                    throw error("Unsupported arithmetic operands");
                left =
                        new Value(
                                "(" + left.code + " " + op + " " + right.code + ")",
                                level == 3
                                        ? "boolean"
                                        : (left.type.equals("java.lang.String")
                                                        || right.type.equals("java.lang.String")
                                                ? "java.lang.String"
                                                : "double"));
            }
        }
        return left;
    }

    private Value unary() {
        if (take("!") || take("not")) return new Value("!" + truth(unary()), "boolean");
        if (take("-")) {
            Value v = unary();
            if (!numeric(v.type)) throw error("Numeric operand required");
            return new Value("(-" + v.code + ")", v.type);
        }
        Value v;
        if (take("(")) {
            v = conditional();
            need(")");
        } else {
            if (at == tokens.size()) throw error("Expected expression");
            String t = tokens.get(at++);
            if (t.startsWith("'") || t.startsWith("\"")) {
                String raw = t.substring(1, t.length() - 1);
                StringBuilder decoded = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (c == '\\') {
                        c = raw.charAt(++i);
                        if (c == 'n') c = '\n';
                        else if (c == 'r') c = '\r';
                        else if (c == 't') c = '\t';
                    }
                    decoded.append(c);
                }
                v = new Value(MvcForms.q(decoded.toString()), "java.lang.String");
            } else if (t.matches("[0-9]+(\\.[0-9]+)?"))
                v = new Value(t, t.contains(".") ? "double" : "int");
            else if (t.equals("true") || t.equals("false")) v = new Value(t, "boolean");
            else if (t.equals("null")) v = new Value("null", "java.lang.Object");
            else {
                v = names.get(t);
                if (v == null) throw error("Undeclared model or local: " + t);
            }
        }
        while (at < tokens.size()) {
            if (take(".")) {
                if (at == tokens.size()) throw error("Expected property");
                v = property(v, tokens.get(at++));
            } else if (take("[")) {
                Value key = conditional();
                need("]");
                String raw = raw(v.type), item = element(v.type);
                if (v.type.endsWith("[]"))
                    v =
                            new Value(
                                    v.code + "[" + key.code + "]",
                                    v.type.substring(0, v.type.length() - 2));
                else if (raw.equals("java.util.List"))
                    v = checkedElement(v.code + ".get(" + key.code + ")", item);
                else if (raw.equals("java.util.Map"))
                    v = checkedElement(v.code + ".get(" + key.code + ")", mapValue(v.type));
                else throw error("Indexing requires a typed list, array or map");
            } else break;
        }
        return v;
    }

    private Value checkedElement(String code, String type) {
        String object = "((Object)(" + code + "))";
        return new Value(
                "("
                        + object
                        + " == null || "
                        + object
                        + " instanceof "
                        + raw(type)
                        + " ? ("
                        + type
                        + ")"
                        + object
                        + " : ("
                        + type
                        + ")"
                        + HTML
                        + "badModel("
                        + MvcForms.q("Expected indexed element of type " + type)
                        + "))",
                type);
    }

    Value property(Value owner, String name) {
        if (!name.matches("[A-Za-z_][A-Za-z0-9_]*") || name.equals("class"))
            throw error("Invalid property: " + name);
        String raw = raw(owner.type), type = null, access = null;
        if (name.equals("size")
                && Arrays.asList(
                                "java.util.List",
                                "java.util.Set",
                                "java.util.Collection",
                                "java.util.Map")
                        .contains(raw)) {
            type = "int";
            access = ".size()";
        } else if (name.equals("empty")
                && Arrays.asList(
                                "java.util.List",
                                "java.util.Set",
                                "java.util.Collection",
                                "java.util.Map",
                                "java.lang.String")
                        .contains(raw)) {
            type = "boolean";
            access = ".isEmpty()";
        } else if (name.equals("length") && raw.equals("java.lang.String")) {
            type = "int";
            access = ".length()";
        } else if (name.equals("length") && raw.endsWith("[]")) {
            type = "int";
            access = ".length";
        } else {
            String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            for (MvcTypes resolved : hierarchy(owner.type)) {
                AnnotatedClass cls = resolved.cls;
                for (MethodInfo m : cls.getMethods())
                    if (m.isPublic()
                            && !m.isStatic()
                            && Type.getArgumentTypes(m.getDescriptor()).length == 0
                            && (m.getName().equals("get" + cap)
                                    || (m.getName().equals("is" + cap)
                                            && Type.getReturnType(m.getDescriptor())
                                                    .equals(Type.BOOLEAN_TYPE)))) {
                        Type result = Type.getReturnType(m.getDescriptor());
                        if (result.equals(Type.VOID_TYPE)) continue;
                        type = resolved.member(result, m.getSignature(), true);
                        access = "." + m.getName() + "()";
                        break;
                    }
                if (access == null)
                    for (FieldInfo f : cls.getFields())
                        if (f.isPublic() && !f.isStatic() && f.getName().equals(name)) {
                            type =
                                    resolved.member(
                                            Type.getType(f.getDescriptor()),
                                            f.getSignature(),
                                            false);
                            access = "." + name;
                            break;
                        }
                if (access != null) break;
            }
        }
        if (access == null) throw error("No readable property '" + name + "' on " + owner.type);
        String boxed = box(type);
        return new Value(
                "("
                        + owner.code
                        + " == null ? null : ("
                        + boxed
                        + ")("
                        + owner.code
                        + access
                        + "))",
                boxed);
    }

    private List<MvcTypes> hierarchy(String type) {
        List<MvcTypes> result = new ArrayList<MvcTypes>();
        Deque<String> pending = new ArrayDeque<String>();
        Set<String> seen = new HashSet<String>();
        pending.add(type);
        while (!pending.isEmpty()) {
            String owner = pending.removeFirst();
            String name = raw(owner).replace('.', '/');
            if (!seen.add(name) || "java/lang/Object".equals(name)) continue;
            AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx, name);
            if (cls == null) continue;
            MvcTypes resolved = new MvcTypes(cls, owner);
            result.add(resolved);
            // Class declarations take precedence over inherited interface defaults.
            for (int i = 0; i < resolved.parents.size(); i++) {
                String parent = resolved.parents.get(i).toString();
                if (i == 0) pending.addFirst(parent);
                else pending.addLast(parent);
            }
        }
        return result;
    }

    private static String numericEquality(Value left, Value right) {
        String comparison = "(" + unboxNumber(left) + " == " + unboxNumber(right) + ")";
        boolean leftBoxed = left.type.startsWith("java.lang.");
        boolean rightBoxed = right.type.startsWith("java.lang.");
        if (leftBoxed && rightBoxed)
            return "("
                    + left.code
                    + " == null ? "
                    + right.code
                    + " == null : "
                    + right.code
                    + " != null && "
                    + comparison
                    + ")";
        if (leftBoxed) return "(" + left.code + " != null && " + comparison + ")";
        if (rightBoxed) return "(" + right.code + " != null && " + comparison + ")";
        return comparison;
    }

    private static String unboxNumber(Value value) {
        if (!value.type.startsWith("java.lang.")) return value.code;
        String primitive =
                value.type.substring("java.lang.".length()).toLowerCase(java.util.Locale.ROOT);
        if (primitive.equals("integer")) primitive = "int";
        return "(" + value.code + ")." + primitive + "Value()";
    }

    static String raw(String t) {
        int i = t.indexOf('<');
        return i < 0 ? t : t.substring(0, i);
    }

    static String element(String t) {
        if (t.endsWith("[]")) return t.substring(0, t.length() - 2);
        int i = t.indexOf('<');
        if (i < 0)
            throw new IllegalArgumentException(
                    "Collection requires an explicit element type: " + t);
        return t.substring(i + 1, t.length() - 1);
    }

    private static String mapValue(String t) {
        String inner = element(t);
        int depth = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<') depth++;
            if (c == '>') depth--;
            if (c == ',' && depth == 0) return inner.substring(i + 1).trim();
        }
        throw new IllegalArgumentException("Map requires key and value types");
    }

    static String box(String t) {
        String[] a = {"boolean", "byte", "short", "int", "long", "float", "double", "char"};
        String[] b = {
            "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "Character"
        };
        for (int i = 0; i < a.length; i++) if (a[i].equals(t)) return "java.lang." + b[i];
        return t;
    }

    private static boolean numeric(String t) {
        return Arrays.asList(
                        "byte",
                        "short",
                        "int",
                        "long",
                        "float",
                        "double",
                        "java.lang.Byte",
                        "java.lang.Short",
                        "java.lang.Integer",
                        "java.lang.Long",
                        "java.lang.Float",
                        "java.lang.Double")
                .contains(t);
    }

    static String truth(Value v) {
        return "boolean".equals(v.type) ? "(" + v.code + ")" : HTML + "truth(" + v.code + ")";
    }

    private boolean take(String t) {
        if (at < tokens.size() && tokens.get(at).equals(t)) {
            at++;
            return true;
        }
        return false;
    }

    private void need(String t) {
        if (!take(t)) throw error("Expected '" + t + "'");
    }

    private IllegalArgumentException error(String s) {
        return new IllegalArgumentException(s);
    }
}
