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

package com.codenameone.playground;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Completion model for the playground Java editor. Mirrors the type inference the previous
 * browser-based editor performed in JavaScript, but here it runs in Java against
 * {@link PlaygroundApi}, the API stubs the compiler itself compiles against. Given a receiver expression
 * like {@code button.} it resolves the receiver's declared type and lists that type's methods and
 * fields; with no receiver it offers the in-scope globals, visible type names and keywords.
 */
final class PlaygroundCompletionModel {

    private static final String[] DEFAULT_IMPORTS = {
            "com.codename1.ui", "com.codename1.ui.layouts", "com.codename1.components",
            "com.codename1.ui.geom", "java.lang", "java.io", "java.util"
    };

    private static final String[][] GLOBALS = {
            {"ctx", "com.codenameone.playground.PlaygroundContext"},
            {"Display", "com.codename1.ui.Display"},
            {"UIManager", "com.codename1.ui.plaf.UIManager"},
            {"FontImage", "com.codename1.ui.FontImage"},
            {"CN", "com.codename1.ui.CN"},
            {"BoxLayout", "com.codename1.ui.layouts.BoxLayout"},
            {"BorderLayout", "com.codename1.ui.layouts.BorderLayout"},
            {"FlowLayout", "com.codename1.ui.layouts.FlowLayout"},
            {"GridLayout", "com.codename1.ui.layouts.GridLayout"},
            {"LayeredLayout", "com.codename1.ui.layouts.LayeredLayout"},
            {"Style", "com.codename1.ui.plaf.Style"}
    };

    private static final String[] KEYWORDS = {
            "import", "new", "return", "if", "else", "for", "while", "class", "void", "int",
            "boolean", "final", "static", "public", "private"
    };

    private static PlaygroundCompletionModel instance;

    private final Map<String, String> globals = new HashMap<String, String>();
    private final Map<String, List<String>> packageToSimples = new HashMap<String, List<String>>();
    private final Map<String, String> simpleToQualified = new HashMap<String, String>();

    static synchronized PlaygroundCompletionModel get() {
        if (instance == null) {
            instance = new PlaygroundCompletionModel();
        }
        return instance;
    }

    private PlaygroundCompletionModel() {
        for (String[] g : GLOBALS) {
            globals.put(g[0], g[1]);
        }
        String[] classes = PlaygroundApi.classNames();
        for (String fqcn : classes) {
            int lastDot = fqcn.lastIndexOf('.');
            String pkg = lastDot < 0 ? "" : fqcn.substring(0, lastDot);
            String simple = lastDot < 0 ? fqcn : fqcn.substring(lastDot + 1);
            List<String> list = packageToSimples.get(pkg);
            if (list == null) {
                list = new ArrayList<String>();
                packageToSimples.put(pkg, list);
            }
            list.add(simple);
            if (!simpleToQualified.containsKey(simple)) {
                simpleToQualified.put(simple, fqcn);
            }
        }
    }

    /// The qualified type whose members belong at the cursor, when the cursor is in a
    /// member position -- right after `expr.`, optionally with part of a name typed.
    /// Answers null when it is not in a member position, and "" when it is but the
    /// type cannot be worked out: the caller then offers nothing, because names that
    /// are not members of the receiver are wrong there, not merely unhelpful.
    ///
    /// The receiver is read as a chain, left to right: a variable, global or type
    /// (`b`, `ctx`, `Style`), a constructor call (`new Button("x")`) or a string
    /// literal, followed by any number of `.field` and `.method(args)` steps, each
    /// typed from the API's own class files ([PlaygroundApi#memberType]). So
    /// `b.getAllStyles().` offers [com.codename1.ui.plaf.Style]'s members. A method's
    /// overloads are told apart by argument count only.
    String receiverType(String code, int cursor) {
        int end = Math.max(0, Math.min(cursor, code.length()));
        int p = end;
        while (p > 0 && isIdentPart(code.charAt(p - 1))) {
            p--;
        }
        int dot = skipSpaceBack(code, p);
        if (dot == 0 || code.charAt(dot - 1) != '.') {
            return null;
        }
        dot--;
        if (dot > 0 && (code.charAt(dot - 1) >= '0' && code.charAt(dot - 1) <= '9') && numberEndsAt(code, dot)) {
            return null; // a decimal point, as in 1.5
        }
        // Steps right to left: {kind, name, argc}. kind: n(ew), s(tring), c(all), f(ield/identifier).
        List<String[]> steps = new ArrayList<String[]>();
        int i = skipSpaceBack(code, dot);
        while (true) {
            if (i == 0) {
                return "";
            }
            char c = code.charAt(i - 1);
            int start;
            if (c == '"') {
                int open = stringStart(code, i - 1);
                if (open < 0) {
                    return "";
                }
                steps.add(new String[] {"s", "", ""});
                break;
            } else if (c == ')') {
                int open = matchingOpenParen(code, i - 1);
                if (open < 0) {
                    return "";
                }
                int argc = argumentCount(code.substring(open + 1, i - 1));
                int nameEnd = skipSpaceBack(code, open);
                if (nameEnd > 0 && code.charAt(nameEnd - 1) == '>') {
                    nameEnd = skipTypeArgumentsBack(code, nameEnd);
                }
                start = nameEnd;
                while (start > 0 && isIdentPart(code.charAt(start - 1))) {
                    start--;
                }
                if (start == nameEnd) {
                    return ""; // a cast or a parenthesized expression
                }
                String name = code.substring(start, nameEnd);
                int before = skipSpaceBack(code, start);
                if (before >= 3 && code.startsWith("new", before - 3)
                        && (before == 3 || !isIdentPart(code.charAt(before - 4)))) {
                    steps.add(new String[] {"n", name, ""});
                    break;
                }
                steps.add(new String[] {"c", name, String.valueOf(argc)});
            } else if (isIdentPart(c)) {
                start = i;
                while (start > 0 && isIdentPart(code.charAt(start - 1))) {
                    start--;
                }
                steps.add(new String[] {"f", code.substring(start, i), ""});
            } else {
                return "";
            }
            int prev = skipSpaceBack(code, start);
            if (prev == 0 || code.charAt(prev - 1) != '.') {
                break;
            }
            i = skipSpaceBack(code, prev - 1);
        }

        Map<String, String> visible = visibleTypes(code);
        String type = "";
        for (int k = steps.size() - 1; k >= 0; k--) {
            String[] step = steps.get(k);
            boolean first = k == steps.size() - 1;
            if (first) {
                if ("s".equals(step[0])) {
                    type = "java.lang.String";
                } else if ("n".equals(step[0])) {
                    type = qualify(step[1], visible);
                } else if ("f".equals(step[0])) {
                    type = inferType(step[1], code, visible);
                } else {
                    type = ""; // a script method; its return type is not looked up
                }
            } else if ("c".equals(step[0])) {
                type = PlaygroundApi.memberType(type, step[1] + "/" + step[2]);
            } else {
                type = PlaygroundApi.memberType(type, step[1]);
            }
            if (type.length() == 0) {
                return "";
            }
        }
        return type;
    }

    private String qualify(String simple, Map<String, String> visible) {
        String fqcn = visible.get(simple);
        if (fqcn == null) {
            fqcn = simpleToQualified.get(simple);
        }
        return fqcn == null ? "" : fqcn;
    }

    /// True when the digits ending at `end` are a whole number token, not the tail of
    /// an identifier such as `label2`.
    private static boolean numberEndsAt(String code, int end) {
        int k = end;
        while (k > 0 && code.charAt(k - 1) >= '0' && code.charAt(k - 1) <= '9') {
            k--;
        }
        return k == 0 || !isIdentPart(code.charAt(k - 1));
    }

    /// The index of the quote opening the string literal whose closing quote is at
    /// `close`, or -1.
    private static int stringStart(String code, int close) {
        for (int k = close - 1; k >= 0; k--) {
            if (code.charAt(k) == '"' && !escaped(code, k)) {
                return k;
            }
            if (code.charAt(k) == '\n') {
                return -1;
            }
        }
        return -1;
    }

    private static boolean escaped(String code, int at) {
        int slashes = 0;
        for (int k = at - 1; k >= 0 && code.charAt(k) == '\\'; k--) {
            slashes++;
        }
        return (slashes & 1) == 1;
    }

    /// The `(` matching the `)` at `close`, skipping string and char literals, or -1.
    private static int matchingOpenParen(String code, int close) {
        int depth = 0;
        for (int k = close; k >= 0; k--) {
            char c = code.charAt(k);
            if ((c == '"' || c == '\'') && !escaped(code, k)) {
                int q = k - 1;
                while (q >= 0 && !(code.charAt(q) == c && !escaped(code, q))) {
                    q--;
                }
                if (q < 0) {
                    return -1;
                }
                k = q;
            } else if (c == ')') {
                depth++;
            } else if (c == '(') {
                depth--;
                if (depth == 0) {
                    return k;
                }
            }
        }
        return -1;
    }

    /// Skips `<...>` ending at `end` (exclusive), as in `new ArrayList<String>(`.
    private static int skipTypeArgumentsBack(String code, int end) {
        int depth = 0;
        for (int k = end - 1; k >= 0; k--) {
            char c = code.charAt(k);
            if (c == '>') {
                depth++;
            } else if (c == '<') {
                depth--;
                if (depth == 0) {
                    return skipSpaceBack(code, k);
                }
            }
        }
        return end;
    }

    /// The number of arguments in an argument list's text: top-level commas, outside
    /// literals and nested brackets, plus one -- or zero for an empty list.
    private static int argumentCount(String args) {
        if (args.trim().length() == 0) {
            return 0;
        }
        int n = 1;
        int depth = 0;
        for (int k = 0; k < args.length(); k++) {
            char c = args.charAt(k);
            if (c == '"' || c == '\'') {
                int q = k + 1;
                while (q < args.length() && !(args.charAt(q) == c && !escaped(args, q))) {
                    q++;
                }
                k = q;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                n++;
            }
        }
        return n;
    }

    /// Simple-name -> qualified-name for every type in scope (default imports, explicit imports and
    /// the bound globals).
    Map<String, String> visibleTypes(String code) {
        Map<String, String> visible = new HashMap<String, String>();
        for (String pkg : DEFAULT_IMPORTS) {
            mergePackage(visible, pkg);
        }
        for (String line : splitLines(code)) {
            String imported = importedName(line);
            if (imported == null) {
                continue;
            }
            if (imported.endsWith(".*")) {
                mergePackage(visible, imported.substring(0, imported.length() - 2));
            } else {
                visible.put(imported.substring(imported.lastIndexOf('.') + 1), imported);
            }
        }
        for (Map.Entry<String, String> g : globals.entrySet()) {
            visible.put(g.getKey(), g.getValue());
        }
        return visible;
    }

    /// If `line` is exactly an `import a.b.C;` (or `import a.b.*;`) statement, returns the imported
    /// name (`a.b.C` / `a.b.*`); otherwise null. Manual parse -- java.util.regex is not CN1-safe.
    private static String importedName(String line) {
        String s = line.trim();
        if (!s.startsWith("import ") || !s.endsWith(";")) {
            return null;
        }
        String body = s.substring("import ".length(), s.length() - 1).trim();
        if (body.length() == 0) {
            return null;
        }
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!isIdentPart(c) && c != '.' && c != '*') {
                return null;
            }
        }
        return body;
    }

    /// Resolves the qualified type of a receiver token: a bound global, a directly referenced type, or
    /// a locally declared variable. Returns "" when the type is unknown.
    String inferType(String receiver, String code, Map<String, String> visible) {
        if (receiver == null || receiver.length() == 0) {
            return "";
        }
        if (globals.containsKey(receiver)) {
            return globals.get(receiver);
        }
        if (visible.containsKey(receiver)) {
            return visible.get(receiver);
        }
        return declaredType(code, receiver, visible);
    }

    /// Scans for a local declaration `Type receiver (= | ; | ,)` and resolves the type. Manual parse --
    /// java.util.regex is not CN1-safe. The last matching declaration wins.
    private String declaredType(String code, String receiver, Map<String, String> visible) {
        String resolved = "";
        int from = 0;
        while (true) {
            int at = code.indexOf(receiver, from);
            if (at < 0) {
                break;
            }
            from = at + receiver.length();
            // receiver must be a whole identifier
            if (at > 0 && isIdentPart(code.charAt(at - 1))) {
                continue;
            }
            int after = from;
            if (after < code.length() && isIdentPart(code.charAt(after))) {
                continue;
            }
            after = skipSpace(code, after);
            if (after >= code.length()) {
                continue;
            }
            char sep = code.charAt(after);
            if (sep != '=' && sep != ';' && sep != ',') {
                continue;
            }
            // the token before `receiver` must be a capitalized type
            int typeEnd = skipSpaceBack(code, at);
            int typeStart = typeEnd;
            while (typeStart > 0 && isTypeChar(code.charAt(typeStart - 1))) {
                typeStart--;
            }
            if (typeStart >= typeEnd) {
                continue;
            }
            char first = code.charAt(typeStart);
            if (first < 'A' || first > 'Z') {
                continue;
            }
            String typeToken = sanitizeTypeToken(code.substring(typeStart, typeEnd));
            String fqcn = visible.get(typeToken);
            if (fqcn == null) {
                fqcn = simpleToQualified.get(typeToken);
            }
            if (fqcn != null) {
                resolved = fqcn;
            }
        }
        return resolved;
    }

    private static boolean isTypeChar(char c) {
        return isIdentPart(c) || c == '.' || c == '<' || c == '>' || c == '[' || c == ']';
    }

    private static int skipSpace(String s, int i) {
        while (i < s.length() && isSpace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int skipSpaceBack(String s, int i) {
        while (i > 0 && isSpace(s.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                out.add(text.substring(start, i));
                start = i + 1;
            }
        }
        out.add(text.substring(start));
        return out;
    }

    /// Member display strings (fields, then method signatures) for the given qualified type.
    List<String> memberSignatures(String typeName) {
        List<String> out = new ArrayList<String>();
        if (typeName == null || typeName.length() == 0) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<String>();
        addAll(seen, PlaygroundApi.fieldNames(typeName));
        addAll(seen, PlaygroundApi.methodSignatures(typeName));
        out.addAll(seen);
        return out;
    }

    /// In-scope global variable names (ctx, theme, ...) offered when the cursor is not after a receiver.
    Map<String, String> globals() {
        return globals;
    }

    /// Type simple names known to the index (for constructor / static references).
    Set<String> typeSimpleNames() {
        return simpleToQualified.keySet();
    }

    String[] keywords() {
        return KEYWORDS;
    }

    /// True when a capitalized token names a type that is in scope (imported / default-imported /
    /// bound) or known anywhere in the CN1 index.
    boolean isKnownType(String token, Map<String, String> visible) {
        return visible.containsKey(token) || simpleToQualified.containsKey(token);
    }

    private void mergePackage(Map<String, String> target, String pkg) {
        List<String> names = packageToSimples.get(pkg);
        if (names == null) {
            return;
        }
        for (String simple : names) {
            target.put(simple, pkg + "." + simple);
        }
    }

    private static String sanitizeTypeToken(String value) {
        String v = value;
        int lt = v.indexOf('<');
        if (lt >= 0) {
            v = v.substring(0, lt);
        }
        v = v.replace("[]", "");
        int lastDot = v.lastIndexOf('.');
        return lastDot < 0 ? v : v.substring(lastDot + 1);
    }

    private static void addAll(Set<String> target, String[] values) {
        if (values == null) {
            return;
        }
        for (String v : values) {
            if (v != null && v.length() > 0) {
                target.add(v);
            }
        }
    }

    static boolean isIdentPart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '_' || c == '$';
    }
}
