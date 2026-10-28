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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.codegen.ClassPathReader.ClassInfo;
import com.codename1.python.transpiler.codegen.ClassPathReader.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/// Writes `JavaInterop`, the program's [JavaBridge][jb]: every Java class a
/// program imports with `from java.<package> import <Class>`, reached by
/// generated `instanceof` tests and direct calls -- no reflection, which
/// ParparVM does not keep.
///
/// The bridge is as small as the program: it covers the imported classes,
/// the classes their used members hand back, and of those only the members
/// whose names the program spells as an attribute (`x.add`). Overloads are
/// tried most specific first, by the Python value's type; a Python callable
/// passed where a single-method interface is expected becomes an instance
/// of it.
///
/// [jb]: com.codename1.python.runtime.JavaBridge
public final class JavaInteropGenerator {
    private static final String RT = "com.codename1.python.runtime.";
    private static final String JB = RT + "JavaBridge";

    private final ClassPathReader cp;
    /// Imported classes, by internal name, numbered in order.
    /// Static fields per generated getter method.
    private static final int STATIC_CHUNK = 256;

    private final Map<String, Integer> imported = new LinkedHashMap<String, Integer>();
    /// Python module (`java.com.codename1.ui`) to its imported names.
    private final Map<String, Map<String, String>> modules = new TreeMap<String, Map<String, String>>();
    private final Set<String> names = new HashSet<String>();
    /// Every public class whose instances the bridge dispatches on.
    private final Set<String> universe = new LinkedHashSet<String>();
    private final Map<String, Integer> adapters = new LinkedHashMap<String, Integer>();
    private final StringBuilder adapterCode = new StringBuilder(); // NOPMD AvoidStringBufferField - the generator's output

    private JavaInteropGenerator(ClassPathReader cp) {
        this.cp = cp;
    }

    /// Whether any of `trees` imports from a `java.` package.
    public static boolean usesJava(Collection<Node.Module> trees) {
        for (Node.Module t : trees) {
            for (Node n : all(t)) {
                if (n instanceof Node.ImportFrom && isJava(((Node.ImportFrom) n).module, ((Node.ImportFrom) n).level)) {
                    return true;
                }
                if (n instanceof Node.Import) {
                    for (Node.Alias a : ((Node.Import) n).names) {
                        if (isJava(a.name, 0)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isJava(String module, int level) {
        return level == 0 && module != null && module.startsWith("java.");
    }

    /// Generates the bridge class source for `trees` (path to tree).
    public static String generate(Map<String, Node.Module> trees, ClassPathReader cp, String pkg) {
        JavaInteropGenerator g = new JavaInteropGenerator(cp);
        g.collect(trees);
        g.close();
        return g.emit(pkg);
    }

    private void collect(Map<String, Node.Module> trees) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            for (Node n : all(e.getValue())) {
                if (n instanceof Node.Attribute) {
                    names.add(((Node.Attribute) n).attr);
                } else if (n instanceof Node.Import) {
                    for (Node.Alias a : ((Node.Import) n).names) {
                        if (isJava(a.name, 0)) {
                            throw new PythonSyntaxError(e.getKey(), n.line, 0, "import the Java classes by name: "
                                    + "from " + a.name + " import <Class>");
                        }
                    }
                } else if (n instanceof Node.ImportFrom) {
                    Node.ImportFrom f = (Node.ImportFrom) n;
                    if (!isJava(f.module, f.level)) {
                        continue;
                    }
                    String pkg = f.module.substring(5).replace('.', '/');
                    for (Node.Alias a : f.names) {
                        if ("*".equals(a.name)) {
                            throw new PythonSyntaxError(e.getKey(), n.line, 0,
                                    "Java classes must be imported by name, not with *");
                        }
                        String internal = pkg + "/" + a.name.replace('.', '$');
                        ClassInfo c = cp.find(internal);
                        if (c == null) {
                            throw new PythonSyntaxError(e.getKey(), n.line, 0, "no Java class "
                                    + internal.replace('/', '.') + " on the class path");
                        }
                        if (!c.is(ClassPathReader.PUBLIC)) {
                            throw new PythonSyntaxError(e.getKey(), n.line, 0, "Java class "
                                    + internal.replace('/', '.') + " is not public");
                        }
                        if (!imported.containsKey(internal)) {
                            imported.put(internal, imported.size());
                        }
                        modules.computeIfAbsent(f.module, k -> new TreeMap<String, String>()).put(a.name, internal);
                    }
                }
            }
        }
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            for (Node n : all(e.getValue())) {
                if (n instanceof Node.ClassDef) {
                    for (Node.Expr b : ((Node.ClassDef) n).bases) {
                        if (b instanceof Node.Name && javaName(((Node.Name) b).id)) {
                            throw new PythonSyntaxError(e.getKey(), n.line, 0, "a Python class cannot extend the "
                                    + "Java class " + ((Node.Name) b).id + "; wrap it, or pass a function where "
                                    + "the Java API takes an interface");
                        }
                    }
                }
            }
        }
    }

    private boolean javaName(String id) {
        for (Map<String, String> m : modules.values()) {
            if (m.containsKey(id)) {
                return true;
            }
        }
        return false;
    }

    /// Grows the universe to every class a used member can hand back.
    private void close() {
        List<String> work = new ArrayList<String>(imported.keySet());
        while (!work.isEmpty()) {
            String c = work.remove(work.size() - 1);
            if (!universe.add(c)) {
                continue;
            }
            for (String s : supertypes(c)) {
                if (isPublic(s)) {
                    work.add(s);
                }
            }
            for (Member m : visibleMethods(c)) {
                if (!names.contains(m.name) && !(imported.containsKey(c) && "<init>".equals(m.name))) {
                    continue;
                }
                addType(returnType(m.desc), work);
                for (String p : params(m.desc)) {
                    String sam = samOf(p);
                    if (sam != null) {
                        Member sm = samMethod(p);
                        for (String q : params(sm.desc)) {
                            addType(q, work);
                        }
                    }
                }
            }
            for (Member f : visibleFields(c)) {
                if (names.contains(f.name)) {
                    addType(f.desc, work);
                }
            }
        }
    }

    private void addType(String desc, List<String> work) {
        if (desc.startsWith("L")) {
            String n = desc.substring(1, desc.length() - 1);
            if (!universe.contains(n) && isPublic(n)) {
                work.add(n);
            }
        }
    }

    private boolean isPublic(String internal) {
        ClassInfo c = cp.find(internal);
        return c != null && c.is(ClassPathReader.PUBLIC) && !internal.contains("$$");
    }

    private List<String> supertypes(String c) {
        List<String> r = new ArrayList<String>();
        ClassInfo i = cp.find(c);
        if (i == null) {
            return r;
        }
        if (i.superName != null) {
            r.add(i.superName);
        }
        r.addAll(i.interfaces);
        return r;
    }

    /// Public methods a reference of type `c` can call that `c` declares
    /// itself, or inherits from a non-public superclass (whose methods Java
    /// lets a caller reach only through `c`).
    private List<Member> visibleMethods(String c) {
        List<Member> r = new ArrayList<Member>();
        Set<String> seen = new HashSet<String>();
        String cur = c;
        while (cur != null) {
            ClassInfo i = cp.find(cur);
            if (i == null) {
                break;
            }
            for (Member m : i.methods) {
                if (m.is(ClassPathReader.PUBLIC) && !m.is(ClassPathReader.BRIDGE) && !m.is(ClassPathReader.SYNTHETIC)
                        && !"<clinit>".equals(m.name) && (cur.equals(c) || !"<init>".equals(m.name))
                        && seen.add(m.name + m.desc)) {
                    r.add(m);
                }
            }
            if (i.superName == null || isPublic(i.superName)) {
                break;
            }
            cur = i.superName;
        }
        return r;
    }

    private List<Member> visibleFields(String c) {
        List<Member> r = new ArrayList<Member>();
        String cur = c;
        while (cur != null) {
            ClassInfo i = cp.find(cur);
            if (i == null) {
                break;
            }
            for (Member f : i.fields) {
                if (f.is(ClassPathReader.PUBLIC) && !f.is(ClassPathReader.SYNTHETIC)) {
                    r.add(f);
                }
            }
            if (i.superName == null || isPublic(i.superName)) {
                break;
            }
            cur = i.superName;
        }
        return r;
    }

    // ---- descriptors -----------------------------------------------------

    static List<String> params(String desc) {
        List<String> r = new ArrayList<String>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            int s = i;
            while (desc.charAt(i) == '[') {
                i++;
            }
            if (desc.charAt(i) == 'L') {
                i = desc.indexOf(';', i);
            }
            i++;
            r.add(desc.substring(s, i));
        }
        return r;
    }

    static String returnType(String desc) {
        return desc.substring(desc.indexOf(')') + 1);
    }

    static String javaType(String d) {
        switch (d.charAt(0)) {
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            case 'B': return "byte";
            case 'C': return "char";
            case 'Z': return "boolean";
            case 'D': return "double";
            case 'F': return "float";
            case 'V': return "void";
            case '[': return javaType(d.substring(1)) + "[]";
            default: return d.substring(1, d.length() - 1).replace('/', '.').replace('$', '.');
        }
    }

    /// The single abstract method's interface if `desc` names a functional
    /// interface whose method it declares itself, else null.
    private String samOf(String desc) {
        if (!desc.startsWith("L")) {
            return null;
        }
        String n = desc.substring(1, desc.length() - 1);
        return samMethod(desc) != null && isPublic(n) ? n : null;
    }

    private Member samMethod(String desc) {
        if (!desc.startsWith("L")) {
            return null;
        }
        ClassInfo c = cp.find(desc.substring(1, desc.length() - 1));
        if (c == null || !c.is(ClassPathReader.INTERFACE)) {
            return null;
        }
        Member found = null;
        for (Member m : c.methods) {
            if (m.is(ClassPathReader.ABSTRACT) && !objectMethod(m)) {
                if (found != null) {
                    return null;
                }
                found = m;
            }
        }
        if (found == null) {
            return null;
        }
        for (String s : c.interfaces) {
            if (hasAbstract(s)) {
                return null;
            }
        }
        return found;
    }

    private boolean hasAbstract(String iface) {
        ClassInfo c = cp.find(iface);
        if (c == null) {
            return false;
        }
        for (Member m : c.methods) {
            if (m.is(ClassPathReader.ABSTRACT) && !objectMethod(m)) {
                return true;
            }
        }
        for (String s : c.interfaces) {
            if (hasAbstract(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean objectMethod(Member m) {
        return ("equals".equals(m.name) && "(Ljava/lang/Object;)Z".equals(m.desc))
                || ("hashCode".equals(m.name) && "()I".equals(m.desc))
                || ("toString".equals(m.name) && "()Ljava/lang/String;".equals(m.desc));
    }

    // ---- argument checks and conversions --------------------------------

    /// How well a Python value fits a parameter: lower is more specific.
    private int rank(String d) {
        switch (d.charAt(0)) {
            case 'Z': case 'C':
                return 0;
            case 'I': return 1;
            case 'J': return 2;
            case 'S': case 'B': return 3;
            case 'F': return 5;
            case 'D': return 4;
            case '[': return 6;
            default:
                break;
        }
        if ("Ljava/lang/Object;".equals(d)) {
            return 20;
        }
        if ("Ljava/lang/String;".equals(d)) {
            return 4;
        }
        if (samOf(d) != null) {
            return 8;
        }
        ClassInfo c = cp.find(d.substring(1, d.length() - 1));
        return c != null && c.is(ClassPathReader.INTERFACE) ? 12 : 10 - Math.min(depth(d.substring(1, d.length() - 1)), 5);
    }

    private int depth(String c) {
        int n = 0;
        ClassInfo i = cp.find(c);
        while (i != null && i.superName != null) {
            n++;
            i = cp.find(i.superName);
        }
        return n;
    }

    private String check(String d, String e) {
        switch (d.charAt(0)) {
            case 'I': case 'J': case 'S': case 'B':
                return JB + ".isInt(" + e + ")";
            case 'D': case 'F':
                return JB + ".isReal(" + e + ")";
            case 'Z':
                return "(" + e + " instanceof " + RT + "PyBool)";
            case 'C':
                return JB + ".isChar(" + e + ")";
            default:
                break;
        }
        if (d.startsWith("[")) {
            return "(" + JB + ".toJava(" + e + ") instanceof " + javaType(d) + ")";
        }
        String t = javaType(d);
        switch (d) {
            case "Ljava/lang/Object;":
                return "true";
            case "Ljava/lang/String;":
            case "Ljava/lang/CharSequence;":
                return "(" + JB + ".isNone(" + e + ") || " + e + " instanceof String)";
            case "Ljava/lang/Integer;": case "Ljava/lang/Long;": case "Ljava/lang/Short;": case "Ljava/lang/Byte;":
                return "(" + JB + ".isNone(" + e + ") || " + JB + ".isInt(" + e + "))";
            case "Ljava/lang/Double;": case "Ljava/lang/Float;":
                return "(" + JB + ".isNone(" + e + ") || " + JB + ".isReal(" + e + "))";
            case "Ljava/lang/Boolean;":
                return "(" + JB + ".isNone(" + e + ") || " + e + " instanceof " + RT + "PyBool)";
            case "Ljava/lang/Character;":
                return "(" + JB + ".isNone(" + e + ") || " + JB + ".isChar(" + e + "))";
            case "Ljava/util/List;": case "Ljava/util/Collection;": case "Ljava/lang/Iterable;":
                return "(" + JB + ".isSequence(" + e + ") || " + JB + ".toJava(" + e + ") instanceof " + t
                        + " || " + JB + ".isNone(" + e + "))";
            case "Ljava/util/Map;":
                return "(" + e + " instanceof " + RT + "PyDict || " + JB + ".toJava(" + e + ") instanceof " + t
                        + " || " + JB + ".isNone(" + e + "))";
            default:
                break;
        }
        String c = "(" + JB + ".isNone(" + e + ") || " + JB + ".toJava(" + e + ") instanceof " + t;
        if (samOf(d) != null) {
            c += " || " + JB + ".isFunction(" + e + ")";
        }
        return c + ")";
    }

    private String convert(String d, String e) {
        switch (d.charAt(0)) {
            case 'I': return "(int) " + JB + ".asLong(" + e + ")";
            case 'J': return JB + ".asLong(" + e + ")";
            case 'S': return "(short) " + JB + ".asLong(" + e + ")";
            case 'B': return "(byte) " + JB + ".asLong(" + e + ")";
            case 'D': return JB + ".asDouble(" + e + ")";
            case 'F': return "(float) " + JB + ".asDouble(" + e + ")";
            case 'Z': return JB + ".asBoolean(" + e + ")";
            case 'C': return JB + ".asChar(" + e + ")";
            default:
                break;
        }
        String t = javaType(d);
        String none = JB + ".isNone(" + e + ") ? null : ";
        switch (d) {
            case "Ljava/lang/Object;":
                return JB + ".toJava(" + e + ")";
            case "Ljava/lang/Integer;":
                return "(" + none + "Integer.valueOf((int) " + JB + ".asLong(" + e + ")))";
            case "Ljava/lang/Long;":
                return "(" + none + "Long.valueOf(" + JB + ".asLong(" + e + ")))";
            case "Ljava/lang/Short;":
                return "(" + none + "Short.valueOf((short) " + JB + ".asLong(" + e + ")))";
            case "Ljava/lang/Byte;":
                return "(" + none + "Byte.valueOf((byte) " + JB + ".asLong(" + e + ")))";
            case "Ljava/lang/Double;":
                return "(" + none + "Double.valueOf(" + JB + ".asDouble(" + e + ")))";
            case "Ljava/lang/Float;":
                return "(" + none + "Float.valueOf((float) " + JB + ".asDouble(" + e + ")))";
            case "Ljava/lang/Boolean;":
                return "(" + none + "Boolean.valueOf(" + JB + ".asBoolean(" + e + ")))";
            case "Ljava/lang/Character;":
                return "(" + none + "Character.valueOf(" + JB + ".asChar(" + e + ")))";
            case "Ljava/util/List;": case "Ljava/util/Collection;": case "Ljava/lang/Iterable;":
                return "((" + t + ") " + JB + ".toJavaList(" + e + "))";
            case "Ljava/util/Map;":
                return "((" + t + ") " + JB + ".toJavaMap(" + e + "))";
            default:
                break;
        }
        if (samOf(d) != null) {
            return adapter(d) + "(" + e + ")";
        }
        return "((" + t + ") " + JB + ".toJava(" + e + "))";
    }

    /// The Java value `e` of type `d` as Python sees it.
    private static String toPy(String d, String e) {
        switch (d.charAt(0)) {
            case 'I': case 'J': case 'S': case 'B':
                return RT + "PyInt.of((long) " + e + ")";
            case 'D': case 'F':
                return RT + "Py.ofFloat(" + e + ")";
            case 'Z':
                return RT + "Py.bool(" + e + ")";
            case 'C':
                return "String.valueOf(" + e + ")";
            default:
                return JB + ".toPy(" + e + ")";
        }
    }

    /// A Python callable's result returned from an adapter as type `d`.
    private String fromPy(String d, String e) {
        if (d.length() == 1 || "Ljava/lang/Object;".equals(d)) {
            return convert(d, e);
        }
        return "((" + javaType(d) + ") " + JB + ".toJava(" + e + "))";
    }

    /// The method that wraps a Python callable as functional interface `d`.
    private String adapter(String d) {
        Integer id = adapters.get(d);
        if (id != null) {
            return "adapt" + id;
        }
        id = adapters.size();
        adapters.put(d, id);
        String t = javaType(d);
        Member m = samMethod(d);
        List<String> ps = params(m.desc);
        String ret = returnType(m.desc);
        StringBuilder sb = adapterCode;
        sb.append("    @SuppressWarnings({\"rawtypes\", \"unchecked\"})\n");
        sb.append("    static ").append(t).append(" adapt").append(id).append("(final Object fn) {\n");
        sb.append("        Object j = ").append(JB).append(".toJava(fn);\n");
        sb.append("        if (j == null || j instanceof ").append(t).append(") {\n");
        sb.append("            return (").append(t).append(") j;\n        }\n");
        sb.append("        return new ").append(t).append("() {\n");
        sb.append("            @Override\n            public ").append(javaType(ret)).append(' ').append(m.name).append('(');
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < ps.size(); i++) {
            if (i > 0) {
                sb.append(", ");
                args.append(", ");
            }
            sb.append(javaType(ps.get(i))).append(" p").append(i);
            args.append(toPy(ps.get(i), "p" + i));
        }
        sb.append(") {\n");
        String call = JB + ".callPy(fn, new Object[] {" + args + "})";
        if ("V".equals(ret)) {
            sb.append("                ").append(call).append(";\n");
        } else {
            sb.append("                Object r = ").append(call).append(";\n");
            sb.append("                return ").append(fromPy(ret, "r")).append(";\n");
        }
        sb.append("            }\n        };\n    }\n\n");
        return "adapt" + id;
    }

    // ---- emission ---------------------------------------------------------

    /// Appends the overload tests of `ms`, called on `recv` (a class name for
    /// static calls and `new` for constructors).
    private void overloads(StringBuilder sb, List<Member> ms, String recv, String name, String ind, boolean ctor) {
        List<Member> sorted = new ArrayList<Member>(ms);
        sorted.sort(Comparator.comparingInt((Member m) -> params(m.desc).size()).thenComparingInt(m -> {
            int s = 0;
            for (String p : params(m.desc)) {
                s += rank(p);
            }
            return s;
        }));
        for (Member m : sorted) {
            List<String> ps = params(m.desc);
            emitCall(sb, m, ps, recv, name, ind, ctor, false);
        }
        for (Member m : sorted) {
            if (m.is(ClassPathReader.VARARGS)) {
                emitCall(sb, m, params(m.desc), recv, name, ind, ctor, true);
            }
        }
    }

    private void emitCall(StringBuilder sb, Member m, List<String> ps, String recv, String name, String ind,
            boolean ctor, boolean spread) {
        int n = ps.size();
        StringBuilder cond = new StringBuilder();
        StringBuilder call = new StringBuilder();
        int fixed = spread ? n - 1 : n;
        cond.append(spread ? "args.length >= " + fixed : "args.length == " + n);
        for (int i = 0; i < fixed; i++) {
            String c = check(ps.get(i), "args[" + i + "]");
            if (!"true".equals(c)) {
                cond.append(" && ").append(c);
            }
            if (i > 0) {
                call.append(", ");
            }
            call.append(convert(ps.get(i), "args[" + i + "]"));
        }
        sb.append(ind).append("if (").append(cond).append(") {\n");
        String in = ind + "    ";
        if (spread) {
            String el = ps.get(n - 1).substring(1);
            sb.append(in).append("boolean ok = true;\n");
            sb.append(in).append("for (int i = ").append(fixed).append("; i < args.length; i++) {\n");
            sb.append(in).append("    if (!").append(check(el, "args[i]")).append(") {\n");
            sb.append(in).append("        ok = false;\n").append(in).append("        break;\n");
            sb.append(in).append("    }\n").append(in).append("}\n");
            sb.append(in).append("if (ok) {\n");
            String arr = javaType(el);
            String newArr = arr.contains("[") ? "new " + arr.substring(0, arr.indexOf('[')) + "[args.length - "
                    + fixed + "]" + arr.substring(arr.indexOf('[')) : "new " + arr + "[args.length - " + fixed + "]";
            sb.append(in).append("    ").append(javaType(ps.get(n - 1))).append(" va = ").append(newArr).append(";\n");
            sb.append(in).append("    for (int i = ").append(fixed).append("; i < args.length; i++) {\n");
            sb.append(in).append("        va[i - ").append(fixed).append("] = ").append(convert(el, "args[i]")).append(";\n");
            sb.append(in).append("    }\n");
            if (fixed > 0) {
                call.append(", ");
            }
            call.append("va");
            in = in + "    ";
        }
        String ret = ctor ? "L;" : returnType(m.desc);
        String invocation = ctor ? "new " + recv + "(" + call + ")" : recv + "." + name + "(" + call + ")";
        if ("V".equals(ret)) {
            sb.append(in).append(invocation).append(";\n");
            sb.append(in).append("return ").append(RT).append("Py.NONE;\n");
        } else {
            sb.append(in).append("return ").append(toPy(ret, invocation)).append(";\n");
        }
        if (spread) {
            sb.append(ind).append("    }\n");
        }
        sb.append(ind).append("}\n");
    }

    private static String signatures(List<Member> ms, String name) {
        Set<String> s = new TreeSet<String>();
        for (Member m : ms) {
            StringBuilder b = new StringBuilder(name).append('(');
            List<String> ps = params(m.desc);
            for (int i = 0; i < ps.size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                String t = javaType(ps.get(i));
                b.append(t.substring(t.lastIndexOf('.') + 1));
            }
            s.add(b.append(')').toString());
        }
        return String.join(" or ", s);
    }

    private static String q(String s) {
        return "\"" + s + "\"";
    }

    private static void tryOpen(StringBuilder sb) {
        sb.append("        try {\n");
    }

    private static void tryClose(StringBuilder sb) {
        sb.append("        } catch (").append(RT).append("PyException e) {\n            throw e;\n");
        sb.append("        } catch (Exception e) {\n            throw ").append(JB).append(".javaError(e);\n        }\n");
    }

    private String emit(String pkg) {
        List<String> ids = new ArrayList<String>(imported.keySet());
        // Subtypes before their supertypes, so an override is found first
        // and an `instanceof` of the wider type does not take it.
        List<String> order = new ArrayList<String>(universe);
        Map<String, Integer> depths = new LinkedHashMap<String, Integer>();
        for (String c : order) {
            depths.put(c, typeDepth(c, new HashSet<String>()));
        }
        order.sort(Comparator.comparingInt((String c) -> -depths.get(c)).thenComparing(c -> c));

        Map<String, Map<String, List<Member>>> instanceMethods = new TreeMap<String, Map<String, List<Member>>>();
        Map<String, Map<String, Member>> instanceFields = new TreeMap<String, Map<String, Member>>();
        for (String c : order) {
            for (Member m : visibleMethods(c)) {
                if (!m.is(ClassPathReader.STATIC) && !"<init>".equals(m.name) && names.contains(m.name)) {
                    instanceMethods.computeIfAbsent(m.name, k -> new LinkedHashMap<String, List<Member>>())
                            .computeIfAbsent(c, k -> new ArrayList<Member>()).add(m);
                }
            }
            for (Member f : visibleFields(c)) {
                if (!f.is(ClassPathReader.STATIC) && names.contains(f.name)) {
                    instanceFields.computeIfAbsent(f.name, k -> new LinkedHashMap<String, Member>()).putIfAbsent(c, f);
                }
            }
        }

        StringBuilder body = new StringBuilder();
        // construct
        body.append("    @Override\n    public Object construct(int cls, Object[] args) {\n");
        body.append("        switch (cls) {\n");
        for (String c : ids) {
            ClassInfo ci = cp.find(c);
            if (!ci.is(ClassPathReader.ABSTRACT) && !ci.is(ClassPathReader.INTERFACE)) {
                body.append("            case ").append(imported.get(c)).append(": return new").append(imported.get(c))
                        .append("(args);\n");
            }
        }
        body.append("            default: return null;\n        }\n    }\n\n");
        for (String c : ids) {
            ClassInfo ci = cp.find(c);
            if (ci.is(ClassPathReader.ABSTRACT) || ci.is(ClassPathReader.INTERFACE)) {
                continue;
            }
            List<Member> ctors = new ArrayList<Member>();
            for (Member m : visibleMethods(c)) {
                if ("<init>".equals(m.name)) {
                    ctors.add(m);
                }
            }
            body.append("    @SuppressWarnings({\"rawtypes\", \"unchecked\"})\n");
            body.append("    private static Object new").append(imported.get(c)).append("(Object[] args) {\n");
            tryOpen(body);
            overloads(body, ctors, javaType("L" + c + ";"), null, "            ", true);
            tryClose(body);
            body.append("        throw ").append(JB).append(".noOverload(").append(q(simple(c))).append(", \"__init__\", args, ")
                    .append(q(signatures(ctors, simple(c)))).append(");\n    }\n\n");
        }

        // statics
        Map<Integer, Map<String, List<Member>>> staticMethods = new TreeMap<Integer, Map<String, List<Member>>>();
        Map<Integer, Map<String, Member>> staticFields = new TreeMap<Integer, Map<String, Member>>();
        for (String c : ids) {
            int id = imported.get(c);
            String cur = c;
            while (cur != null && cp.find(cur) != null) {
                for (Member m : visibleMethods(cur)) {
                    if (m.is(ClassPathReader.STATIC) && names.contains(m.name)) {
                        staticMethods.computeIfAbsent(id, k -> new TreeMap<String, List<Member>>())
                                .computeIfAbsent(m.name, k -> new ArrayList<Member>()).add(m);
                    }
                }
                for (Member f : visibleFields(cur)) {
                    if (f.is(ClassPathReader.STATIC)) {
                        staticFields.computeIfAbsent(id, k -> new TreeMap<String, Member>()).putIfAbsent(f.name, f);
                    }
                }
                ClassInfo ci = cp.find(cur);
                cur = ci.superName;
                if (cur != null && !isPublic(cur)) {
                    break;
                }
            }
        }
        // One method per class, and per STATIC_CHUNK fields of it, each
        // falling through to the next: a class such as FontImage has
        // thousands of constants, which in one method pass the JVM's 64 KB
        // limit on a method's code.
        StringBuilder fieldGetters = new StringBuilder();
        body.append("    @Override\n    public Object getStatic(int cls, String name) {\n");
        body.append("        switch (cls) {\n");
        for (Map.Entry<Integer, Map<String, Member>> e : staticFields.entrySet()) {
            String c = javaType("L" + ids.get(e.getKey()) + ";");
            String fn = "getStatic" + e.getKey() + "_";
            body.append("            case ").append(e.getKey()).append(": return ").append(fn).append("0(name);\n");
            List<Member> fields = new ArrayList<Member>(e.getValue().values());
            for (int chunk = 0; chunk * STATIC_CHUNK < fields.size(); chunk++) {
                fieldGetters.append("    private static Object ").append(fn).append(chunk).append("(String name) {\n");
                fieldGetters.append("        switch (name) {\n");
                int end = Math.min(fields.size(), (chunk + 1) * STATIC_CHUNK);
                for (Member f : fields.subList(chunk * STATIC_CHUNK, end)) {
                    fieldGetters.append("            case ").append(q(f.name)).append(": return ")
                            .append(toPy(f.desc, c + "." + f.name)).append(";\n");
                }
                fieldGetters.append("            default: return ")
                        .append(end < fields.size() ? fn + (chunk + 1) + "(name)" : "null").append(";\n");
                fieldGetters.append("        }\n    }\n\n");
            }
        }
        body.append("            default: return null;\n        }\n    }\n\n").append(fieldGetters);
        body.append("    @Override\n    public boolean hasStatic(int cls, String name) {\n");
        body.append("        switch (cls) {\n");
        for (Map.Entry<Integer, Map<String, List<Member>>> e : staticMethods.entrySet()) {
            body.append("            case ").append(e.getKey()).append(":\n                switch (name) {\n");
            for (String n : e.getValue().keySet()) {
                body.append("                    case ").append(q(n)).append(":\n");
            }
            body.append("                        return true;\n                    default: return false;\n                }\n");
        }
        body.append("            default: return false;\n        }\n    }\n\n");
        body.append("    @Override\n    public Object callStatic(int cls, String name, Object[] args) {\n");
        int sid = 0;
        StringBuilder statics = new StringBuilder();
        body.append("        switch (cls) {\n");
        for (Map.Entry<Integer, Map<String, List<Member>>> e : staticMethods.entrySet()) {
            String c = javaType("L" + ids.get(e.getKey()) + ";");
            body.append("            case ").append(e.getKey()).append(":\n                switch (name) {\n");
            for (Map.Entry<String, List<Member>> me : e.getValue().entrySet()) {
                String fn = "static" + (sid++);
                body.append("                    case ").append(q(me.getKey())).append(": return ").append(fn).append("(args);\n");
                statics.append("    @SuppressWarnings({\"rawtypes\", \"unchecked\"})\n");
                statics.append("    private static Object ").append(fn).append("(Object[] args) {\n");
                tryOpen(statics);
                overloads(statics, me.getValue(), c, me.getKey(), "            ", false);
                tryClose(statics);
                statics.append("        throw ").append(JB).append(".noOverload(").append(q(simple(ids.get(e.getKey()))))
                        .append(", ").append(q(me.getKey())).append(", args, ")
                        .append(q(signatures(me.getValue(), me.getKey()))).append(");\n    }\n\n");
            }
            body.append("                    default: return null;\n                }\n");
        }
        body.append("            default: return null;\n        }\n    }\n\n").append(statics);

        // instance members
        body.append("    @Override\n    public Object getField(Object target, String name) {\n");
        body.append("        switch (name) {\n");
        for (Map.Entry<String, Map<String, Member>> e : instanceFields.entrySet()) {
            body.append("            case ").append(q(e.getKey())).append(":\n");
            for (Map.Entry<String, Member> ce : e.getValue().entrySet()) {
                String t = javaType("L" + ce.getKey() + ";");
                body.append("                if (target instanceof ").append(t).append(") {\n");
                body.append("                    return ").append(toPy(ce.getValue().desc, "((" + t + ") target)." + e.getKey()))
                        .append(";\n                }\n");
            }
            body.append("                return null;\n");
        }
        body.append("            default: return null;\n        }\n    }\n\n");
        body.append("    @Override\n    public boolean hasMethod(Object target, String name) {\n");
        body.append("        switch (name) {\n");
        for (Map.Entry<String, Map<String, List<Member>>> e : instanceMethods.entrySet()) {
            body.append("            case ").append(q(e.getKey())).append(":\n                return ");
            boolean first = true;
            for (String c : e.getValue().keySet()) {
                if (!first) {
                    body.append("\n                        || ");
                }
                first = false;
                body.append("target instanceof ").append(javaType("L" + c + ";"));
            }
            body.append(";\n");
        }
        body.append("            default: return false;\n        }\n    }\n\n");
        body.append("    @Override\n    public Object invoke(Object target, String name, Object[] args) {\n");
        body.append("        switch (name) {\n");
        int iid = 0;
        StringBuilder invokers = new StringBuilder();
        for (Map.Entry<String, Map<String, List<Member>>> e : instanceMethods.entrySet()) {
            String fn = "invoke" + (iid++);
            body.append("            case ").append(q(e.getKey())).append(": return ").append(fn).append("(target, args);\n");
            invokers.append("    @SuppressWarnings({\"rawtypes\", \"unchecked\"})\n");
            invokers.append("    private static Object ").append(fn).append("(Object target, Object[] args) {\n");
            tryOpen(invokers);
            List<Member> all = new ArrayList<Member>();
            for (Map.Entry<String, List<Member>> ce : e.getValue().entrySet()) {
                String t = javaType("L" + ce.getKey() + ";");
                invokers.append("            if (target instanceof ").append(t).append(") {\n");
                overloads(invokers, ce.getValue(), "((" + t + ") target)", e.getKey(), "                ", false);
                invokers.append("            }\n");
                all.addAll(ce.getValue());
            }
            tryClose(invokers);
            invokers.append("        throw ").append(JB).append(".noOverload(target.getClass().getName(), ")
                    .append(q(e.getKey())).append(", args, ").append(q(signatures(all, e.getKey()))).append(");\n    }\n\n");
        }
        body.append("            default: return null;\n        }\n    }\n\n").append(invokers);

        body.append("    @Override\n    public boolean isInstance(int cls, Object value) {\n");
        body.append("        switch (cls) {\n");
        for (String c : ids) {
            body.append("            case ").append(imported.get(c)).append(": return value instanceof ")
                    .append(javaType("L" + c + ";")).append(";\n");
        }
        body.append("            default: return false;\n        }\n    }\n\n");
        body.append("    @Override\n    public String className(int cls) {\n        return NAMES[cls];\n    }\n\n");

        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by the Codename One Python transpiler.\n");
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("/// The Java classes this program imports, bridged without reflection.\n");
        sb.append("@SuppressWarnings({\"rawtypes\", \"unchecked\", \"deprecation\", \"removal\"})\n");
        sb.append("public final class JavaInterop extends ").append(JB).append(" {\n");
        sb.append("    private static final String[] NAMES = {");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(i > 0 ? ", " : "").append(q(simple(ids.get(i))));
        }
        sb.append("};\n\n");
        sb.append("    /// Installs the bridge and makes each `java.<package>` importable.\n");
        sb.append("    public static void register() {\n");
        sb.append("        JavaInterop b = new JavaInterop();\n");
        sb.append("        ").append(JB).append(".install(b);\n");
        Set<String> pkgs = new TreeSet<String>();
        for (String m : modules.keySet()) {
            String p = m;
            while (p.contains(".")) {
                pkgs.add(p);
                p = p.substring(0, p.lastIndexOf('.'));
            }
            pkgs.add(p);
        }
        for (String p : pkgs) {
            sb.append("        {\n");
            sb.append("            ").append(RT).append("PyModule m = ").append(RT).append("PyModule.begin(")
                    .append(q(p)).append(", new String[0]);\n");
            Map<String, String> cls = modules.get(p);
            if (cls != null) {
                for (Map.Entry<String, String> e : cls.entrySet()) {
                    sb.append("            m.setAttr(").append(q(e.getKey())).append(", b.type(")
                            .append(imported.get(e.getValue())).append("));\n");
                }
            }
            sb.append("        }\n");
        }
        sb.append("    }\n\n");
        sb.append(body).append(adapterCode);
        sb.append("}\n");
        return sb.toString();
    }

    private int typeDepth(String c, Set<String> seen) {
        if (!seen.add(c)) {
            return 0;
        }
        int d = 0;
        for (String s : supertypes(c)) {
            if (cp.find(s) != null) {
                d = Math.max(d, 1 + typeDepth(s, seen));
            }
        }
        seen.remove(c);
        return d;
    }

    private static String simple(String internal) {
        String s = internal.substring(internal.lastIndexOf('/') + 1);
        return s.substring(s.lastIndexOf('$') + 1);
    }

    // ---- tree walk --------------------------------------------------------

    static List<Node> all(Node root) {
        List<Node> out = new ArrayList<Node>();
        walk(root, out, java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>()));
        return out;
    }

    private static void walk(Object o, List<Node> out, Set<Object> seen) {
        if (o instanceof Node) {
            if (!seen.add(o)) {
                return;
            }
            out.add((Node) o);
            for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.getType() == String.class) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                        walk(f.get(o), out, seen);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        } else if (o instanceof Collection) {
            for (Object x : (Collection<?>) o) {
                walk(x, out, seen);
            }
        }
    }
}
