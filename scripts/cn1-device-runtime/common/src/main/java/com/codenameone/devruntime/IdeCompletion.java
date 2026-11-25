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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.devruntime;

import com.codename1.tools.javac.StubLibrary;
import com.codename1.tools.translator.classfile.ClassReader;
import com.codename1.tools.translator.classfile.ClassVisitor;
import com.codename1.tools.translator.classfile.FieldVisitor;
import com.codename1.tools.translator.classfile.MethodVisitor;
import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeCompletionProvider;
import com.codename1.ui.CodeEditor;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Code completion for the on-device editor, answered from the same stub
/// library the compiler builds against -- so what it offers is exactly what
/// will compile.
///
/// This is a text-level completer, not a second compiler. It knows:
///
/// - after a dot, the members of the receiver's type, where the receiver is a
///   class name (static members), `this`/`super`, a variable or field whose
///   declaration it can find in the text, or a chain of calls and fields from
///   one of those, typed through the stubs' descriptors;
/// - otherwise, the Java keywords, the identifiers already in the file and the
///   simple names of every public class in the API.
///
/// Anything it cannot type yields no member list rather than a guess.
final class IdeCompletion implements CodeCompletionProvider {
    private static final int ACC_PUBLIC = 0x0001;
    private static final int ACC_PROTECTED = 0x0004;
    private static final int ACC_STATIC = 0x0008;
    private static final int ACC_SYNTHETIC = 0x1000;
    private static final int MAX_RESULTS = 120;

    private static final String[] KEYWORDS = {
        "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
        "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "new", "null", "package", "private", "protected", "public",
        "return", "short", "static", "super", "switch", "synchronized", "this", "throw",
        "throws", "true", "false", "try", "void", "volatile", "while", "var"
    };

    /// Simple name to the internal names that carry it; built once, lazily.
    private static Map<String, List<String>> simpleNames;
    private static final Map<String, ClassInfo> CLASSES = new HashMap<String, ClassInfo>();

    /// One member of an API class, as the stub declares it.
    static final class Member {
        final String name;
        final String desc;
        final boolean method;
        final boolean isStatic;
        final String owner;

        Member(String owner, String name, String desc, boolean method, boolean isStatic) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.method = method;
            this.isStatic = isStatic;
        }
    }

    static final class ClassInfo {
        String superName;
        String[] interfaces = new String[0];
        final List<Member> members = new ArrayList<Member>();
    }

    public void getCompletions(CodeEditor editor, String code, int cursor,
            SuccessCallback<List<CodeCompletion>> results) {
        List<CodeCompletion> out;
        try {
            out = complete(code, Math.max(0, Math.min(cursor, code.length())));
        } catch (RuntimeException err) {
            // Completion is a convenience: a parse it cannot follow must never
            // take the editor down with it.
            out = new ArrayList<CodeCompletion>();
        }
        results.onSucess(out);
    }

    /// Visible for tests: the proposals at `cursor` in `code`.
    static List<CodeCompletion> complete(String code, int cursor) {
        int start = cursor;
        while (start > 0 && Character.isJavaIdentifierPart(code.charAt(start - 1))) {
            start--;
        }
        String prefix = code.substring(start, cursor);
        int dot = skipSpaceBack(code, start);
        if (dot > 0 && code.charAt(dot - 1) == '.') {
            return memberCompletions(code, dot - 1, prefix);
        }
        if (prefix.length() == 0 || isInCommentOrString(code, start)) {
            return new ArrayList<CodeCompletion>();
        }
        return plainCompletions(code, start, prefix);
    }

    // ---- plain identifiers ----

    private static List<CodeCompletion> plainCompletions(String code, int start, String prefix) {
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        Set<String> seen = new HashSet<String>();
        // regionMatches rather than toLowerCase: the latter folds I to a
        // dotless i on a Turkish device and the prefix stops matching.
        boolean afterImport = lineStartsWith(code, start, "import");

        // Identifiers already in the file come first: they are what is being
        // typed again most of the time.
        for (String id : identifiersIn(code, start, prefix.length())) {
            if (id.length() > prefix.length() && id.regionMatches(true, 0, prefix, 0, prefix.length()) && seen.add(id)) {
                out.add(new CodeCompletion(id).setType("variable"));
            }
        }
        if (!afterImport) {
            for (String k : KEYWORDS) {
                if (k.startsWith(prefix) && k.length() > prefix.length() && seen.add(k)) {
                    out.add(new CodeCompletion(k).setType("keyword"));
                }
            }
        }
        Map<String, List<String>> names = simpleNames();
        if (names != null && Character.isUpperCase(prefix.charAt(0))) {
            List<String> sorted = new ArrayList<String>();
            for (String n : names.keySet()) {
                if (n.startsWith(prefix) && !seen.contains(n)) {
                    sorted.add(n);
                }
            }
            java.util.Collections.sort(sorted);
            for (String n : sorted) {
                if (out.size() >= MAX_RESULTS) {
                    break;
                }
                List<String> owners = names.get(n);
                seen.add(n);
                if (afterImport) {
                    // One entry per package: "import List" must let the user
                    // pick java.util.List over com.codename1.ui.List, not
                    // insert whichever was indexed first.
                    for (String owner : owners) {
                        out.add(new CodeCompletion(n, owner.replace('/', '.').replace('$', '.') + ";")
                                .setType("class").setDetail(packageOf(owner)));
                    }
                    continue;
                }
                String pkg = packageOf(owners.get(0));
                out.add(new CodeCompletion(n, n).setType("class")
                        .setDetail(owners.size() > 1 ? pkg + " (+" + (owners.size() - 1) + ")" : pkg));
            }
        }
        return out;
    }

    private static List<String> identifiersIn(String code, int skipAt, int skipLen) {
        List<String> ids = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipLiteral(code, i);
                continue;
            }
            if (c == '/' && i + 1 < n && (code.charAt(i + 1) == '/' || code.charAt(i + 1) == '*')) {
                i = skipComment(code, i);
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int s = i;
                while (i < n && Character.isJavaIdentifierPart(code.charAt(i))) {
                    i++;
                }
                if (s != skipAt) {
                    String id = code.substring(s, i);
                    if (seen.add(id)) {
                        ids.add(id);
                    }
                }
                continue;
            }
            i++;
        }
        return ids;
    }

    // ---- members after a dot ----

    private static List<CodeCompletion> memberCompletions(String code, int dotPos, String prefix) {
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        Receiver r = resolveExpression(code, dotPos);
        if (r == null || r.type == null) {
            return out;
        }
        Set<String> seen = new HashSet<String>();
        List<Member> all = new ArrayList<Member>();
        collectMembers(r.type, all, new HashSet<String>());
        if (r.staticOnly) {
            // Nested types are reachable through the class name too.
            Map<String, List<String>> names = simpleNames();
            if (names != null) {
                String inner = r.type + "$";
                for (List<String> owners : names.values()) {
                    for (String o : owners) {
                        if (o.startsWith(inner) && o.indexOf('$', inner.length()) < 0) {
                            String simple = o.substring(inner.length());
                            if (simple.startsWith(prefix) && seen.add("C" + simple)) {
                                out.add(new CodeCompletion(simple).setType("class"));
                            }
                        }
                    }
                }
            }
        }
        for (Member m : all) {
            if (r.staticOnly && !m.isStatic) {
                continue;
            }
            if (!m.name.startsWith(prefix) || "<init>".equals(m.name) || "<clinit>".equals(m.name)) {
                continue;
            }
            String key = m.method ? m.name + m.desc.substring(0, m.desc.indexOf(')') + 1) : "F" + m.name;
            if (!seen.add(key)) {
                continue;
            }
            if (m.method) {
                String params = paramList(m.desc);
                out.add(new CodeCompletion(m.name + "(" + params + ")",
                        m.name + (params.length() == 0 ? "()" : "("))
                        .setType("method").setDetail(simpleType(returnType(m.desc))));
            } else {
                out.add(new CodeCompletion(m.name).setType("field").setDetail(simpleType(m.desc)));
            }
        }
        java.util.Collections.sort(out, new java.util.Comparator<CodeCompletion>() {
            public int compare(CodeCompletion a, CodeCompletion b) {
                return a.getDisplayText().compareTo(b.getDisplayText());
            }
        });
        return out.size() > MAX_RESULTS ? new ArrayList<CodeCompletion>(out.subList(0, MAX_RESULTS)) : out;
    }

    static final class Receiver {
        final String type;
        final boolean staticOnly;

        Receiver(String type, boolean staticOnly) {
            this.type = type;
            this.staticOnly = staticOnly;
        }
    }

    /// Types the expression ending just before `end` (the dot): a primary
    /// followed by any number of `.name` and `.name(...)` steps.
    private static Receiver resolveExpression(String code, int end) {
        List<String> steps = new ArrayList<String>();
        int i = skipSpaceBack(code, end);
        while (true) {
            boolean call = false;
            if (i > 0 && code.charAt(i - 1) == ')') {
                int open = matchBack(code, i - 1, '(', ')');
                if (open < 0) {
                    return null;
                }
                i = skipSpaceBack(code, open);
                call = true;
            } else if (i > 0 && code.charAt(i - 1) == '>') {
                // A generic call such as obj.<T>m() is rare enough to skip.
                return null;
            }
            int s = i;
            while (s > 0 && Character.isJavaIdentifierPart(code.charAt(s - 1))) {
                s--;
            }
            if (s == i) {
                return null;
            }
            String name = code.substring(s, i);
            int before = skipSpaceBack(code, s);
            boolean isNew = before >= 3 && code.startsWith("new", before - 3)
                    && (before == 3 || !Character.isJavaIdentifierPart(code.charAt(before - 4)));
            if (isNew && call) {
                steps.add(0, "new " + name);
                break;
            }
            steps.add(0, call ? name + "()" : name);
            if (before > 0 && code.charAt(before - 1) == '.') {
                i = skipSpaceBack(code, before - 1);
                continue;
            }
            break;
        }
        String first = steps.get(0);
        Receiver cur;
        if (first.startsWith("new ")) {
            String t = resolveClass(code, first.substring(4));
            cur = t == null ? null : new Receiver(t, false);
        } else if ("this".equals(first)) {
            String sup = declaredSuperclass(code);
            cur = sup == null ? null : new Receiver(sup, false);
        } else if ("super".equals(first)) {
            String sup = declaredSuperclass(code);
            cur = sup == null ? null : new Receiver(sup, false);
        } else if (first.endsWith("()")) {
            // An unqualified call: one of this class's inherited methods.
            String sup = declaredSuperclass(code);
            cur = sup == null ? null : step(new Receiver(sup, false), first);
        } else {
            String declared = declaredTypeOf(code, first, end);
            if (declared != null) {
                String t = resolveClass(code, declared);
                cur = t == null ? null : new Receiver(t, false);
            } else if (Character.isUpperCase(first.charAt(0))) {
                String t = resolveClass(code, first);
                cur = t == null ? null : new Receiver(t, true);
            } else {
                // Possibly the first segment of a qualified name: java.util.Foo.
                cur = null;
                StringBuilder q = new StringBuilder(first);
                for (int k = 1; k < steps.size(); k++) {
                    q.append('/').append(steps.get(k));
                    if (classInfo(q.toString()) != null) {
                        cur = new Receiver(q.toString(), true);
                        steps = steps.subList(k, steps.size());
                        break;
                    }
                }
            }
        }
        for (int k = 1; k < steps.size() && cur != null; k++) {
            cur = step(cur, steps.get(k));
        }
        return cur;
    }

    private static Receiver step(Receiver from, String seg) {
        boolean call = seg.endsWith("()");
        String name = call ? seg.substring(0, seg.length() - 2) : seg;
        List<Member> all = new ArrayList<Member>();
        collectMembers(from.type, all, new HashSet<String>());
        for (Member m : all) {
            if (m.method == call && m.name.equals(name)) {
                String t = objectType(call ? returnType(m.desc) : m.desc);
                return t == null ? null : new Receiver(t, false);
            }
        }
        if (!call && from.staticOnly && classInfo(from.type + "$" + name) != null) {
            return new Receiver(from.type + "$" + name, true);
        }
        return null;
    }

    /// The type written in front of `name` in its nearest declaration before
    /// `limit` -- a local, a parameter, a field, a for-each variable -- or the
    /// class instantiated by `var name = new X(...)`.
    static String declaredTypeOf(String code, String name, int limit) {
        int from = Math.min(limit, code.length());
        while (true) {
            int at = code.lastIndexOf(name, from - 1);
            if (at < 0) {
                break;
            }
            from = at;
            int after = at + name.length();
            if ((at > 0 && Character.isJavaIdentifierPart(code.charAt(at - 1)))
                    || (after < code.length() && Character.isJavaIdentifierPart(code.charAt(after)))) {
                continue;
            }
            int next = skipSpace(code, after);
            char nc = next < code.length() ? code.charAt(next) : ';';
            if (nc != '=' && nc != ';' && nc != ',' && nc != ')' && nc != ':') {
                continue;
            }
            if (nc == '=' && next + 1 < code.length() && code.charAt(next + 1) == '=') {
                continue;
            }
            int e = skipSpaceBack(code, at);
            if (e == at || code.charAt(e - 1) == '.') {
                // No space between it and what precedes it, or a member access
                // (an import's last segment, obj.name): not a declaration.
                if (e == at && e > 0 && code.charAt(e - 1) != ']' && code.charAt(e - 1) != '>') {
                    continue;
                }
                if (e > 0 && code.charAt(e - 1) == '.') {
                    continue;
                }
            }
            // Skip array brackets and a generic argument list behind the type.
            while (e > 0 && code.charAt(e - 1) == ']') {
                int o = matchBack(code, e - 1, '[', ']');
                if (o < 0) {
                    break;
                }
                e = skipSpaceBack(code, o);
            }
            if (e > 0 && code.charAt(e - 1) == '>') {
                int o = matchBack(code, e - 1, '<', '>');
                if (o < 0) {
                    continue;
                }
                e = skipSpaceBack(code, o);
            }
            int s = e;
            while (s > 0 && (Character.isJavaIdentifierPart(code.charAt(s - 1)) || code.charAt(s - 1) == '.')) {
                s--;
            }
            String type = code.substring(s, e);
            if (type.length() == 0 || isKeywordNotType(type)) {
                continue;
            }
            if ("var".equals(type) && nc == '=') {
                int n = skipSpace(code, next + 1);
                if (code.startsWith("new", n)) {
                    int ts = skipSpace(code, n + 3);
                    int te = ts;
                    while (te < code.length()
                            && (Character.isJavaIdentifierPart(code.charAt(te)) || code.charAt(te) == '.')) {
                        te++;
                    }
                    return te > ts ? code.substring(ts, te) : null;
                }
                return null;
            }
            if (Character.isLowerCase(type.charAt(0)) && type.indexOf('.') < 0) {
                // A primitive (int x) or a stray expression -- nothing to complete.
                return null;
            }
            return type;
        }
        return null;
    }

    private static boolean isKeywordNotType(String s) {
        return "return".equals(s) || "new".equals(s) || "throw".equals(s) || "case".equals(s)
                || "else".equals(s) || "instanceof".equals(s);
    }

    private static String declaredSuperclass(String code) {
        int at = code.indexOf(" extends ");
        if (at < 0) {
            return "java/lang/Object";
        }
        int s = skipSpace(code, at + 9);
        int e = s;
        while (e < code.length() && (Character.isJavaIdentifierPart(code.charAt(e)) || code.charAt(e) == '.')) {
            e++;
        }
        return resolveClass(code, code.substring(s, e));
    }

    /// Resolves a simple or qualified name written in `code` to an internal
    /// name, the way javac would: explicit imports, then java.lang, then the
    /// file's own package and on-demand imports, then any unique match.
    static String resolveClass(String code, String written) {
        if (written == null || written.length() == 0) {
            return null;
        }
        if (written.indexOf('.') > 0) {
            String q = written.replace('.', '/');
            if (classInfo(q) != null) {
                return q;
            }
            // Outer.Inner written through an import of Outer.
            int dot = written.indexOf('.');
            String outer = resolveClass(code, written.substring(0, dot));
            if (outer != null) {
                String inner = outer + "$" + written.substring(dot + 1).replace('.', '$');
                return classInfo(inner) != null ? inner : null;
            }
            return null;
        }
        List<String> wildcard = new ArrayList<String>();
        int i = 0;
        while ((i = code.indexOf("import ", i)) >= 0) {
            int semi = code.indexOf(';', i);
            if (semi < 0) {
                break;
            }
            String imp = code.substring(i + 7, semi).trim();
            i = semi;
            if (imp.startsWith("static ")) {
                continue;
            }
            if (imp.endsWith(".*")) {
                wildcard.add(imp.substring(0, imp.length() - 2).replace('.', '/'));
            } else if (imp.endsWith("." + written) || imp.equals(written)) {
                String q = imp.replace('.', '/');
                if (classInfo(q) != null) {
                    return q;
                }
                int last = q.lastIndexOf('/');
                String nested = q.substring(0, last) + "$" + q.substring(last + 1);
                if (classInfo(nested) != null) {
                    return nested;
                }
            }
        }
        if (classInfo("java/lang/" + written) != null) {
            return "java/lang/" + written;
        }
        String pkg = filePackage(code);
        if (pkg.length() > 0 && classInfo(pkg + "/" + written) != null) {
            return pkg + "/" + written;
        }
        for (String w : wildcard) {
            if (classInfo(w + "/" + written) != null) {
                return w + "/" + written;
            }
        }
        Map<String, List<String>> names = simpleNames();
        if (names != null) {
            List<String> owners = names.get(written);
            if (owners != null && !owners.isEmpty()) {
                // Ambiguous without an import; the compiler will insist on one,
                // but completion can still offer the likeliest class's members.
                for (String o : owners) {
                    if (o.startsWith("com/codename1/ui/")) {
                        return o;
                    }
                }
                return owners.get(0);
            }
        }
        return null;
    }

    private static String filePackage(String code) {
        int p = code.indexOf("package ");
        if (p < 0) {
            return "";
        }
        int semi = code.indexOf(';', p);
        return semi < 0 ? "" : code.substring(p + 8, semi).trim().replace('.', '/');
    }

    // ---- the stub library ----

    private static synchronized Map<String, List<String>> simpleNames() {
        if (simpleNames != null) {
            return simpleNames;
        }
        StubLibrary lib = library();
        if (lib == null) {
            return null;
        }
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (String n : lib.classNames()) {
            if (n.startsWith("com/codename1/impl/") || n.indexOf("/internal/") >= 0) {
                continue;
            }
            int slash = n.lastIndexOf('/');
            String simple = n.substring(slash + 1);
            int dollar = simple.lastIndexOf('$');
            if (dollar >= 0) {
                simple = simple.substring(dollar + 1);
                if (simple.length() == 0 || Character.isDigit(simple.charAt(0))) {
                    continue;
                }
            }
            List<String> owners = map.get(simple);
            if (owners == null) {
                owners = new ArrayList<String>(1);
                map.put(simple, owners);
            }
            // java.* first, then com.codename1.*, so the first owner is the
            // likeliest one when a name exists in several packages.
            if (n.startsWith("java/")) {
                owners.add(0, n);
            } else {
                owners.add(n);
            }
        }
        simpleNames = map;
        return map;
    }

    /// Builds the index ahead of the first keystroke; safe to call from any
    /// thread, and a no-op once built.
    static void warmUp() {
        simpleNames();
    }

    /// Set by tests run off the device, where there is no Display to read
    /// the bundled library through.
    static StubLibrary testLibrary;

    private static StubLibrary library() {
        if (testLibrary != null) {
            return testLibrary;
        }
        try {
            return IdeBuild.library();
        } catch (java.io.IOException err) {
            return null;
        }
    }

    static synchronized ClassInfo classInfo(String internal) {
        if (CLASSES.containsKey(internal)) {
            return CLASSES.get(internal);
        }
        StubLibrary lib = library();
        byte[] bytes = lib == null ? null : lib.classBytes(internal);
        ClassInfo info = null;
        if (bytes != null) {
            final ClassInfo ci = new ClassInfo();
            final String owner = internal;
            new ClassReader(bytes).accept(new ClassVisitor() {
                @Override
                public void visit(int version, int access, String name, String signature,
                        String superName, String[] interfaces) {
                    ci.superName = superName;
                    if (interfaces != null) {
                        ci.interfaces = interfaces;
                    }
                }

                @Override
                public FieldVisitor visitField(int access, String name, String desc,
                        String signature, Object value) {
                    if (visible(access)) {
                        ci.members.add(new Member(owner, name, desc, false, (access & ACC_STATIC) != 0));
                    }
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                        String signature, String[] exceptions) {
                    if (visible(access)) {
                        ci.members.add(new Member(owner, name, desc, true, (access & ACC_STATIC) != 0));
                    }
                    return null;
                }
            }, 0);
            info = ci;
        }
        CLASSES.put(internal, info);
        return info;
    }

    private static boolean visible(int access) {
        return (access & (ACC_PUBLIC | ACC_PROTECTED)) != 0 && (access & ACC_SYNTHETIC) == 0;
    }

    private static void collectMembers(String type, List<Member> into, Set<String> visited) {
        if (type == null || !visited.add(type)) {
            return;
        }
        ClassInfo ci = classInfo(type);
        if (ci == null) {
            return;
        }
        into.addAll(ci.members);
        collectMembers(ci.superName, into, visited);
        for (String itf : ci.interfaces) {
            collectMembers(itf, into, visited);
        }
        if (ci.superName == null && !"java/lang/Object".equals(type)) {
            collectMembers("java/lang/Object", into, visited);
        }
    }

    // ---- descriptors ----

    private static String returnType(String desc) {
        return desc.substring(desc.indexOf(')') + 1);
    }

    private static String objectType(String desc) {
        return desc.startsWith("L") && desc.endsWith(";") ? desc.substring(1, desc.length() - 1) : null;
    }

    private static String paramList(String desc) {
        StringBuilder sb = new StringBuilder();
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
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(simpleType(desc.substring(s, i)));
        }
        return sb.toString();
    }

    static String simpleType(String desc) {
        int dims = 0;
        while (dims < desc.length() && desc.charAt(dims) == '[') {
            dims++;
        }
        String base = desc.substring(dims);
        String name;
        switch (base.length() == 0 ? 'V' : base.charAt(0)) {
            case 'Z': name = "boolean"; break;
            case 'B': name = "byte"; break;
            case 'C': name = "char"; break;
            case 'S': name = "short"; break;
            case 'I': name = "int"; break;
            case 'J': name = "long"; break;
            case 'F': name = "float"; break;
            case 'D': name = "double"; break;
            case 'V': name = "void"; break;
            default:
                String n = base.substring(1, base.length() - 1);
                n = n.substring(n.lastIndexOf('/') + 1);
                name = n.substring(n.lastIndexOf('$') + 1);
        }
        StringBuilder sb = new StringBuilder(name);
        for (int k = 0; k < dims; k++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static String packageOf(String internal) {
        int slash = internal.lastIndexOf('/');
        return slash < 0 ? "" : internal.substring(0, slash).replace('/', '.');
    }

    // ---- text scanning ----

    private static int skipSpaceBack(String code, int i) {
        while (i > 0 && Character.isWhitespace(code.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static int skipSpace(String code, int i) {
        while (i < code.length() && Character.isWhitespace(code.charAt(i))) {
            i++;
        }
        return i;
    }

    /// The index of the `open` matching the `close` at `closeAt`, or -1.
    private static int matchBack(String code, int closeAt, char open, char close) {
        int depth = 0;
        for (int i = closeAt; i >= 0; i--) {
            char c = code.charAt(i);
            if (c == close) {
                depth++;
            } else if (c == open) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean lineStartsWith(String code, int at, String word) {
        int ls = code.lastIndexOf('\n', at - 1) + 1;
        return code.substring(ls, at).trim().startsWith(word);
    }

    private static int skipLiteral(String code, int i) {
        char q = code.charAt(i);
        int j = i + 1;
        while (j < code.length() && code.charAt(j) != q && code.charAt(j) != '\n') {
            if (code.charAt(j) == '\\') {
                j++;
            }
            j++;
        }
        return j + 1;
    }

    private static int skipComment(String code, int i) {
        if (code.charAt(i + 1) == '/') {
            int nl = code.indexOf('\n', i);
            return nl < 0 ? code.length() : nl + 1;
        }
        int e = code.indexOf("*/", i + 2);
        return e < 0 ? code.length() : e + 2;
    }

    private static boolean isInCommentOrString(String code, int at) {
        int i = 0;
        while (i < at) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                int e = skipLiteral(code, i);
                if (e > at) {
                    return true;
                }
                i = e;
            } else if (c == '/' && i + 1 < code.length()
                    && (code.charAt(i + 1) == '/' || code.charAt(i + 1) == '*')) {
                int e = skipComment(code, i);
                if (e > at) {
                    return true;
                }
                i = e;
            } else {
                i++;
            }
        }
        return false;
    }
}
