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

import bsh.cn1.GeneratedCN1Access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Faux reflection" completion model for the playground Java editor. Mirrors the type inference the
 * old Monaco integration performed in {@code editor.js}, but here it runs in Java against the same
 * {@link GeneratedCN1Access} index (the CN1-safe reflection surface). Given a receiver expression
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
            {"theme", "com.codename1.ui.util.Resources"},
            {"hostForm", "com.codename1.ui.Form"},
            {"previewRoot", "com.codename1.ui.Container"},
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

    // `Type name (= | ; | ,)` -- a local declaration, so `name` maps to `Type`.
    private static final Pattern DECLARATION =
            Pattern.compile("\\b([A-Z][A-Za-z0-9_$.<>\\[\\]]*)\\s+([a-zA-Z_$][A-Za-z0-9_$]*)\\s*(=|;|,)");
    private static final Pattern EXPLICIT_IMPORT =
            Pattern.compile("(?m)^\\s*import\\s+([a-zA-Z0-9_$.]+)\\s*;\\s*$");

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
        String[] classes = GeneratedCN1Access.INSTANCE.getIndexedClassNames();
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

    /// The identifier immediately before a trailing `receiver.` at the cursor, or "" if the cursor is
    /// not positioned after a member-access dot.
    String findReceiver(String code, int cursor) {
        int end = Math.max(0, Math.min(cursor, code.length()));
        String prefix = code.substring(0, end);
        int dot = prefix.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        for (int i = dot + 1; i < prefix.length(); i++) {
            if (!isIdentPart(prefix.charAt(i))) {
                return "";
            }
        }
        int i = dot;
        while (i > 0 && isIdentPart(prefix.charAt(i - 1))) {
            i--;
        }
        return prefix.substring(i, dot);
    }

    /// Simple-name -> qualified-name for every type in scope (default imports, explicit imports and
    /// the bound globals).
    Map<String, String> visibleTypes(String code) {
        Map<String, String> visible = new HashMap<String, String>();
        for (String pkg : DEFAULT_IMPORTS) {
            mergePackage(visible, pkg);
        }
        Matcher m = EXPLICIT_IMPORT.matcher(code);
        while (m.find()) {
            String imported = m.group(1);
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
        Matcher m = DECLARATION.matcher(code);
        String resolved = "";
        while (m.find()) {
            if (!receiver.equals(m.group(2))) {
                continue;
            }
            String typeToken = sanitizeTypeToken(m.group(1));
            String fqcn = visible.get(typeToken);
            if (fqcn == null) {
                fqcn = simpleToQualified.get(typeToken);
            }
            if (fqcn != null) {
                resolved = fqcn; // last declaration wins (closest to the reference is usually latest)
            }
        }
        return resolved;
    }

    /// Member display strings (fields, then method signatures) for the given qualified type.
    List<String> memberSignatures(String typeName) {
        List<String> out = new ArrayList<String>();
        if (typeName == null || typeName.length() == 0) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<String>();
        addAll(seen, GeneratedCN1Access.INSTANCE.getFieldNames(typeName));
        addAll(seen, GeneratedCN1Access.INSTANCE.getMethodSignatures(typeName));
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
