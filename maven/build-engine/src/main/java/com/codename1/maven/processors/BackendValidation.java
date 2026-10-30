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
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Writes the checks behind `com.codename1.backend.validation`: the constraints
/// on a controller parameter as statements in the router, and one public static
/// method per validated class, in a generated `Cn1Validation` class of that
/// class's package -- the one place its package-private fields can be read.
///
/// Every check is a plain `if`; nothing reads an annotation at run time, which is
/// what lets the translated backend keep them.
final class BackendValidation {
    static final String PKG = "Lcom/codename1/backend/validation/";
    static final String VALID = PKG + "Valid;";
    static final String NOT_NULL = PKG + "NotNull;";
    static final String NOT_BLANK = PKG + "NotBlank;";
    static final String NOT_EMPTY = PKG + "NotEmpty;";
    static final String SIZE = PKG + "Size;";
    static final String MIN = PKG + "Min;";
    static final String MAX = PKG + "Max;";
    static final String VIOLATIONS = "com.codename1.backend.validation.Violations";
    static final String CLASS_NAME = "Cn1Validation";

    private static final List<String> CONSTRAINTS = Arrays.asList(NOT_NULL, NOT_BLANK,
            NOT_EMPTY, SIZE, MIN, MAX);
    private static final Set<String> COLLECTIONS = new HashSet<String>(Arrays.asList(
            "java.util.Collection", "java.util.List", "java.util.Set", "java.util.ArrayList",
            "java.util.LinkedList", "java.util.HashSet", "java.util.LinkedHashSet",
            "java.util.TreeSet", "java.util.SortedSet", "java.lang.Iterable"));
    private static final Set<String> MAPS = new HashSet<String>(Arrays.asList(
            "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap",
            "java.util.TreeMap", "java.util.SortedMap"));
    private static final Set<String> INTEGRAL = new HashSet<String>(Arrays.asList(
            "byte", "short", "int", "long", "java.lang.Byte", "java.lang.Short",
            "java.lang.Integer", "java.lang.Long"));
    private static final Set<String> PRIMITIVES = new HashSet<String>(Arrays.asList(
            "byte", "short", "int", "long", "char", "boolean", "float", "double"));

    private final ProcessorContext ctx;
    /// Binary name of each validated class, to the statements of its method.
    private final Map<String, String> methods = new LinkedHashMap<String, String>();
    private final Set<String> pending = new HashSet<String>();
    private int counter;

    BackendValidation(ProcessorContext ctx) {
        this.ctx = ctx;
    }

    /// Whether a parameter or field carries anything this class checks.
    static boolean constrained(Map<String, AnnotationValues> annotations) {
        if (annotations == null) {
            return false;
        }
        if (annotations.containsKey(VALID)) {
            return true;
        }
        for (String c : CONSTRAINTS) {
            if (annotations.containsKey(c)) {
                return true;
            }
        }
        return false;
    }

    /// The statements checking `expr` against `annotations`, reporting into
    /// `violations` at `pathExpr` (a Java expression of type String).
    ///
    /// @param type the value's Java type, with type arguments when known
    /// @param where for an error message
    /// @throws IllegalArgumentException for a constraint the type cannot carry
    String check(Map<String, AnnotationValues> annotations, String type, String expr,
                 String pathExpr, String violations, String pad, String where) {
        StringBuilder sb = new StringBuilder();
        String raw = erase(type);
        boolean primitive = PRIMITIVES.contains(raw);
        for (String c : CONSTRAINTS) {
            AnnotationValues a = annotations.get(c);
            if (a == null) {
                continue;
            }
            String code = simple(c);
            String message = RestControllerAnnotationProcessor.quote(
                    a.getStringOrDefault("message", ""));
            String failed;
            String fallback;
            if (NOT_NULL.equals(c)) {
                if (primitive) {
                    continue;          // a primitive is never null
                }
                failed = expr + " == null";
                fallback = "must not be null";
            } else if (NOT_BLANK.equals(c)) {
                requireText(raw, code, where);
                failed = VIOLATIONS + ".isBlank(" + expr + ")";
                fallback = "must not be blank";
            } else if (NOT_EMPTY.equals(c)) {
                failed = expr + " == null || " + length(raw, expr, code, where) + " == 0";
                fallback = "must not be empty";
            } else if (SIZE.equals(c)) {
                int min = intValue(a, "min", 0);
                int max = intValue(a, "max", Integer.MAX_VALUE);
                String len = length(raw, expr, code, where);
                failed = expr + " != null && (" + len + " < " + min + " || " + len + " > "
                        + max + ")";
                fallback = "size must be between " + min + " and " + max;
            } else {
                if (!INTEGRAL.contains(raw)) {
                    throw new IllegalArgumentException("@" + code + " applies to an integral "
                            + "number (byte, short, int, long or their boxes), and " + where
                            + " is a " + raw);
                }
                long bound = longValue(a, "value");
                boolean isMin = MIN.equals(c);
                failed = (primitive ? "" : expr + " != null && ") + expr
                        + (isMin ? " < " : " > ") + bound + "L";
                fallback = "must be " + (isMin ? "greater" : "less") + " than or equal to "
                        + bound;
            }
            sb.append(pad).append("if (").append(failed).append(") {\n");
            sb.append(pad).append("    ").append(violations).append(".add(").append(pathExpr)
              .append(", ").append(RestControllerAnnotationProcessor.quote(code)).append(", ")
              .append(message).append(", ").append(RestControllerAnnotationProcessor.quote(fallback))
              .append(");\n");
            sb.append(pad).append("}\n");
        }
        if (annotations.containsKey(VALID)) {
            sb.append(cascade(type, expr, pathExpr, violations, pad, where));
        }
        return sb.toString();
    }

    /// `@Valid`: descends into a class of the application, or into each element
    /// of a collection or array of them. A value with nothing to descend into --
    /// a String, a Map, a number -- is left alone, as Bean Validation does.
    private String cascade(String type, String expr, String pathExpr, String violations,
                           String pad, String where) {
        String raw = erase(type);
        StringBuilder sb = new StringBuilder();
        String element = null;
        boolean array = raw.endsWith("[]");
        if (array) {
            element = raw.substring(0, raw.length() - 2);
        } else if (COLLECTIONS.contains(raw)) {
            element = typeArgument(type);
        }
        if (element != null) {
            String elementRaw = erase(element);
            if (validator(elementRaw) == null) {
                return "";
            }
            String i = "cn1I" + (counter++);
            String e = "cn1E" + (counter++);
            sb.append(pad).append("if (").append(expr).append(" != null) {\n");
            sb.append(pad).append("    int ").append(i).append(" = 0;\n");
            if (array) {
                sb.append(pad).append("    for (").append(elementRaw.replace('$', '.'))
                  .append(' ').append(e).append(" : ").append(expr).append(") {\n");
                sb.append(pad).append("        if (").append(e).append(" != null) {\n");
                sb.append(pad).append("            ").append(validator(elementRaw)).append('(')
                  .append(e).append(", ").append(VIOLATIONS).append(".index(").append(pathExpr)
                  .append(", ").append(i).append("), ").append(violations).append(");\n");
            } else {
                // Tested, never merely cast: the translated runtime does not
                // check a cast, so a foreign element would be read as this type.
                sb.append(pad).append("    for (Object ").append(e).append(" : ").append(expr)
                  .append(") {\n");
                sb.append(pad).append("        if (").append(e).append(" instanceof ")
                  .append(elementRaw.replace('$', '.')).append(") {\n");
                sb.append(pad).append("            ").append(validator(elementRaw)).append("((")
                  .append(elementRaw.replace('$', '.')).append(") ").append(e).append(", ")
                  .append(VIOLATIONS).append(".index(").append(pathExpr).append(", ").append(i)
                  .append("), ").append(violations).append(");\n");
            }
            sb.append(pad).append("        }\n");
            sb.append(pad).append("        ").append(i).append("++;\n");
            sb.append(pad).append("    }\n");
            sb.append(pad).append("}\n");
            return sb.toString();
        }
        String call = validator(raw);
        if (call == null) {
            return "";
        }
        sb.append(pad).append("if (").append(expr).append(" != null) {\n");
        sb.append(pad).append("    ").append(call).append('(').append(expr).append(", ")
          .append(pathExpr).append(", ").append(violations).append(");\n");
        sb.append(pad).append("}\n");
        return sb.toString();
    }

    /// The static method validating `binary`, or null when it is not a class of
    /// the application with something to validate.
    private String validator(String binary) {
        if (binary.startsWith("java.") || PRIMITIVES.contains(binary) || binary.endsWith("[]")) {
            return null;
        }
        String pkg = RestClientAnnotationProcessor.packageOf(binary);
        String name = qualify(pkg, CLASS_NAME) + ".validate"
                + binary.substring(pkg.length() == 0 ? 0 : pkg.length() + 1).replace('$', '_');
        if (methods.containsKey(binary) || pending.contains(binary)) {
            return name;
        }
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                binary.replace('.', '/'));
        if (cls == null || cls.isEnum() || cls.isInterface()) {
            return null;
        }
        pending.add(binary);
        String body = classBody(cls, binary);
        pending.remove(binary);
        if (body == null) {
            return null;
        }
        methods.put(binary, body);
        return name;
    }

    /// The statements validating each constrained field of `cls` and its
    /// superclasses, read through `v`; null when no field is constrained.
    private String classBody(AnnotatedClass cls, String binary) {
        List<AnnotatedClass> chain = new ArrayList<AnnotatedClass>();
        for (AnnotatedClass c = cls; c != null; ) {
            chain.add(0, c);
            String parent = c.getSuperInternalName();
            if (parent == null || parent.startsWith("java/")) {
                break;
            }
            c = RestControllerAnnotationProcessor.resolveClass(ctx, parent);
        }
        String pkg = RestClientAnnotationProcessor.packageOf(binary);
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (AnnotatedClass c : chain) {
            for (FieldInfo f : c.getFields()) {
                if ((f.getAccess() & (Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)) != 0
                        || !constrained(f.getAnnotations())) {
                    continue;
                }
                String where = binary + "." + f.getName();
                String read = read(chain, c, f, pkg, cls.isRecord());
                if (read == null) {
                    throw new IllegalArgumentException(where + " carries a constraint, but is "
                            + "private with no public getter, so the generated check cannot "
                            + "read it. Add a getter or make the field package-private");
                }
                String type = RestClientAnnotationProcessor.javaTypeFor(
                        Type.getType(f.getDescriptor()), f.getSignature());
                AnnotationValues renamed = f.getAnnotation(BackendJsonCodecs.JSON_PROPERTY);
                String json = renamed == null ? null : renamed.getString("value");
                String property = json == null || json.length() == 0 ? f.getName() : json;
                String local = "f" + (counter++);
                sb.append("        final ").append(BackendJsonCodecs.source(type)).append(' ')
                  .append(local).append(" = ").append(read).append(";\n");
                sb.append(check(f.getAnnotations(), type, local, VIOLATIONS + ".path(path, "
                        + RestControllerAnnotationProcessor.quote(property) + ")", "violations",
                        "        ", where));
                any = true;
            }
        }
        return any ? sb.toString() : null;
    }

    private static String read(List<AnnotatedClass> chain, AnnotatedClass owner, FieldInfo f,
                               String pkg, boolean record) {
        if (record) {
            return "v." + f.getName() + "()";
        }
        if (f.isPublic() || (!f.isPrivate()
                && RestClientAnnotationProcessor.packageOf(owner.getBinaryName()).equals(pkg))) {
            return "v." + f.getName();
        }
        String cap = Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
        for (int i = chain.size() - 1; i >= 0; i--) {
            for (MethodInfo m : chain.get(i).getMethods()) {
                if (m.isPublic() && !m.isStatic() && ("()" + f.getDescriptor()).equals(m.getDescriptor())
                        && (("get" + cap).equals(m.getName())
                            || ("Z".equals(f.getDescriptor()) && ("is" + cap).equals(m.getName())))) {
                    return "v." + m.getName() + "()";
                }
            }
        }
        return null;
    }

    /// One `Cn1Validation` class per package that has a validated class.
    Map<String, String> sources() {
        Map<String, StringBuilder> byPackage = new LinkedHashMap<String, StringBuilder>();
        for (Map.Entry<String, String> e : methods.entrySet()) {
            String binary = e.getKey();
            String pkg = RestClientAnnotationProcessor.packageOf(binary);
            StringBuilder sb = byPackage.get(pkg);
            if (sb == null) {
                sb = new StringBuilder();
                if (pkg.length() > 0) {
                    sb.append("package ").append(pkg).append(";\n\n");
                }
                sb.append("// Generated from the validation constraints of this package. "
                        + "Do not edit.\n");
                sb.append("@com.codename1.backend.annotations.Generated\n");
                sb.append("public final class ").append(CLASS_NAME).append(" {\n");
                sb.append("    private ").append(CLASS_NAME).append("() {\n    }\n");
                byPackage.put(pkg, sb);
            }
            String simple = binary.substring(pkg.length() == 0 ? 0 : pkg.length() + 1);
            sb.append("\n    public static void validate").append(simple.replace('$', '_'))
              .append('(').append(binary.replace('$', '.')).append(" v, String path, ")
              .append(VIOLATIONS).append(" violations) {\n");
            sb.append(e.getValue());
            sb.append("    }\n");
        }
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, StringBuilder> e : byPackage.entrySet()) {
            out.put(qualify(e.getKey(), CLASS_NAME), e.getValue().append("}\n").toString());
        }
        return out;
    }

    private static void requireText(String raw, String code, String where) {
        if (!"java.lang.String".equals(raw) && !"java.lang.CharSequence".equals(raw)
                && !"java.lang.StringBuilder".equals(raw)) {
            throw new IllegalArgumentException("@" + code + " applies to text, and " + where
                    + " is a " + raw);
        }
    }

    private static String length(String raw, String expr, String code, String where) {
        if ("java.lang.String".equals(raw) || "java.lang.CharSequence".equals(raw)
                || "java.lang.StringBuilder".equals(raw)) {
            return expr + ".length()";
        }
        if (raw.endsWith("[]")) {
            return expr + ".length";
        }
        if (COLLECTIONS.contains(raw) && !"java.lang.Iterable".equals(raw) || MAPS.contains(raw)) {
            return expr + ".size()";
        }
        throw new IllegalArgumentException("@" + code + " applies to text, a collection, a "
                + "map or an array, and " + where + " is a " + raw);
    }

    private static int intValue(AnnotationValues a, String name, int fallback) {
        Object v = a.get(name);
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }

    private static long longValue(AnnotationValues a, String name) {
        Object v = a.get(name);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private static String simple(String descriptor) {
        return descriptor.substring(descriptor.lastIndexOf('/') + 1, descriptor.length() - 1);
    }

    static String erase(String type) {
        int lt = type.indexOf('<');
        if (lt < 0) {
            return type;
        }
        int gt = type.lastIndexOf('>');
        return type.substring(0, lt) + type.substring(gt + 1);
    }

    /// The single type argument of `java.util.List<X>`, or null.
    private static String typeArgument(String type) {
        int lt = type.indexOf('<');
        int gt = type.lastIndexOf('>');
        if (lt < 0 || gt < lt) {
            return null;
        }
        String arg = type.substring(lt + 1, gt).trim();
        if (arg.startsWith("? extends ")) {
            arg = arg.substring("? extends ".length());
        }
        return arg.startsWith("?") ? null : arg;
    }

    private static String qualify(String pkg, String simple) {
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }
}
