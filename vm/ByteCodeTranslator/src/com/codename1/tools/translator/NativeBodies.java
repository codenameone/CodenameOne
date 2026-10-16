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
package com.codename1.tools.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The native sources split into function bodies that run only when something calls
/// them, and everything else.
///
/// A symbol named anywhere in the natives used to be a root: the C side might call it.
/// But much of what the natives name, they name inside the implementation of a Java
/// native -- `IOSNative.homeRefresh`'s C body reports back through
/// `IOSHomeCallbacks.refreshed` -- and that body only runs if Java calls the native. So
/// ReachabilityCull roots only what the rest of the text names ([#rootText]) and treats
/// what a body names as calls made by that body, followed once it is live:
///
/// - [#natives]: the C implementation of a Java native, live when the native is;
/// - [#helpers]: a `static` C function, live when a live body or the rest of its own file
///   names it. Only `static` ones: a file-local function can only be called from its
///   own file, so every use of it is in text this sees. A function with external
///   linkage may be called from generated code, which is written after the cull, so it
///   stays part of the root text.
///
/// The split is deliberately shallow. A top-level `{` whose preceding `)` closes a
/// parameter list opens a function body, which runs to the matching `}`. Strings,
/// character literals and comments are skipped. Anything the scan cannot follow --
/// braces that do not balance -- leaves that file whole, so everything in it stays a
/// root, as before. `#include` and `#import` lines are dropped from the root text: they
/// name a generated header, which declares a class and runs none of it.
final class NativeBodies {
    /// The natives with every recognised body blanked out, its name included, so a
    /// function does not root itself.
    final String[] rootText;
    /// A Java native's C symbol to its bodies (one per definition: the natives may define
    /// it more than once under different conditions).
    final Map<String, List<Body>> natives = new HashMap<String, List<Body>>();
    /// Per file, a static function's name to its body.
    final List<Map<String, String>> helpers = new ArrayList<Map<String, String>>();

    static final class Body {
        final int file;
        final String text;

        Body(int file, String text) {
            this.file = file;
            this.text = text;
        }
    }

    private NativeBodies(String[] rootText) {
        this.rootText = rootText;
    }

    static boolean enabled() {
        return !"false".equalsIgnoreCase(Util.getProperty("cn1.nativeBodyScope", "true"));
    }

    static NativeBodies parse(String[] sources, Set<String> nativeSymbols) {
        String[] root = new String[sources == null ? 0 : sources.length];
        NativeBodies nb = new NativeBodies(root);
        for (int i = 0; i < root.length; i++) {
            nb.helpers.add(new HashMap<String, String>());
            root[i] = sources[i] == null ? null : nb.split(i, sources[i], nativeSymbols);
        }
        return nb;
    }

    private String split(int file, String t, Set<String> nativeSymbols) {
        List<Object[]> found = new ArrayList<Object[]>();
        StringBuilder out = new StringBuilder(t.length());
        int n = t.length();
        int depth = 0;
        int copied = 0;
        // The body being collected: where its definition starts (the name) and where its
        // brace opens; -1 when none is.
        int defStart = -1;
        int bodyStart = -1;
        String name = null;
        boolean isNative = false;
        int i = 0;
        while (i < n) {
            char c = t.charAt(i);
            if (c == '/' && i + 1 < n && t.charAt(i + 1) == '/') {
                int e = t.indexOf('\n', i);
                i = e < 0 ? n : e;
                continue;
            }
            if (c == '/' && i + 1 < n && t.charAt(i + 1) == '*') {
                int e = t.indexOf("*/", i + 2);
                if (e < 0) {
                    return withoutIncludes(t);
                }
                i = e + 2;
                continue;
            }
            if (c == '"' || c == '\'') {
                int e = skipLiteral(t, i, c);
                if (e < 0) {
                    return withoutIncludes(t);
                }
                i = e;
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    int s = symbolBefore(t, i);
                    if (s >= 0) {
                        String sym = t.substring(s, identifierEnd(t, s));
                        if (nativeSymbols.contains(sym)) {
                            defStart = s;
                            bodyStart = i;
                            name = sym;
                            isNative = true;
                        } else if (isStaticDefinition(t, s)) {
                            defStart = s;
                            bodyStart = i;
                            name = sym;
                            isNative = false;
                        }
                    }
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth < 0) {
                    return withoutIncludes(t);
                }
                if (depth == 0 && name != null) {
                    found.add(new Object[] {name, isNative ? Boolean.TRUE : Boolean.FALSE,
                        t.substring(bodyStart, i + 1)});
                    out.append(t, copied, defStart);
                    copied = i + 1;
                    name = null;
                    defStart = -1;
                    bodyStart = -1;
                }
            }
            i++;
        }
        if (depth != 0) {
            return withoutIncludes(t);
        }
        out.append(t, copied, n);
        Map<String, String> fileHelpers = helpers.get(file);
        for (Object[] f : found) {
            String sym = (String) f[0];
            String body = (String) f[2];
            if (Boolean.TRUE.equals(f[1])) {
                List<Body> l = natives.get(sym);
                if (l == null) {
                    l = new ArrayList<Body>(1);
                    natives.put(sym, l);
                }
                l.add(new Body(file, body));
            } else {
                String prior = fileHelpers.get(sym);
                fileHelpers.put(sym, prior == null ? body : prior + "\n" + body);
            }
        }
        return withoutIncludes(out.toString());
    }

    /// Whether the declaration ending at the function name {@code nameStart} says
    /// `static`: the text back to the previous `;`, `}` or preprocessor line holds the
    /// word.
    private static boolean isStaticDefinition(String t, int nameStart) {
        int j = nameStart - 1;
        while (j >= 0) {
            char c = t.charAt(j);
            if (c == ';' || c == '}' || c == '{') {
                break;
            }
            if (c == '\n') {
                int k = j + 1;
                while (k < nameStart && (t.charAt(k) == ' ' || t.charAt(k) == '\t')) {
                    k++;
                }
                if (k < nameStart && t.charAt(k) == '#') {
                    // The line after a directive starts the declaration.
                    j = t.indexOf('\n', k);
                    break;
                }
            }
            j--;
        }
        String decl = t.substring(Math.max(0, j + 1), nameStart);
        int p = decl.indexOf("static");
        while (p >= 0) {
            boolean before = p == 0 || !isIdentifierChar(decl.charAt(p - 1));
            int after = p + "static".length();
            boolean afterOk = after >= decl.length() || !isIdentifierChar(decl.charAt(after));
            if (before && afterOk) {
                return true;
            }
            p = decl.indexOf("static", p + 1);
        }
        return false;
    }

    /// Drops `#include` and `#import` lines. They name a class's generated header, which
    /// declares it and runs none of it -- the #else trampolines of a switched-off feature
    /// include their callback class's header, and as a root that made the class look
    /// allocated and initialized in every app.
    private static String withoutIncludes(String t) {
        StringBuilder b = new StringBuilder(t.length());
        int pos = 0;
        while (pos < t.length()) {
            int end = t.indexOf('\n', pos);
            if (end < 0) {
                end = t.length();
            }
            int i = skipSpaces(t, pos, end);
            boolean include = i < end && t.charAt(i) == '#'
                    && (t.startsWith("include", skipSpaces(t, i + 1, end))
                            || t.startsWith("import", skipSpaces(t, i + 1, end)));
            if (!include) {
                b.append(t, pos, end);
            }
            if (end < t.length()) {
                b.append('\n');
            }
            pos = end + 1;
        }
        return b.toString();
    }

    private static int skipSpaces(String t, int i, int end) {
        while (i < end && (t.charAt(i) == ' ' || t.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    /// The index just past a string or character literal starting at {@code i}, or -1.
    private static int skipLiteral(String t, int i, char quote) {
        int j = i + 1;
        while (j < t.length()) {
            char c = t.charAt(j);
            if (c == '\\') {
                j += 2;
                continue;
            }
            if (c == quote) {
                return j + 1;
            }
            if (c == '\n') {
                // An apostrophe in running text that is not a literal at all (inside a
                // #error, say); treat it as a single character.
                return i + 1;
            }
            j++;
        }
        return -1;
    }

    /// The start of the function name whose parameter list closes just before the `{`
    /// at {@code brace}, or -1 if it is not `name(...) {`.
    private static int symbolBefore(String t, int brace) {
        int j = brace - 1;
        while (j >= 0 && Character.isWhitespace(t.charAt(j))) {
            j--;
        }
        if (j < 0 || t.charAt(j) != ')') {
            return -1;
        }
        int parens = 0;
        while (j >= 0) {
            char c = t.charAt(j);
            if (c == ')') {
                parens++;
            } else if (c == '(') {
                parens--;
                if (parens == 0) {
                    break;
                }
            } else if (c == '{' || c == '}' || c == ';') {
                return -1;
            }
            j--;
        }
        if (j < 0) {
            return -1;
        }
        j--;
        while (j >= 0 && Character.isWhitespace(t.charAt(j))) {
            j--;
        }
        int end = j + 1;
        while (j >= 0 && isIdentifierChar(t.charAt(j))) {
            j--;
        }
        return j + 1 < end ? j + 1 : -1;
    }

    private static int identifierEnd(String t, int s) {
        int e = s;
        while (e < t.length() && isIdentifierChar(t.charAt(e))) {
            e++;
        }
        return e;
    }

    static boolean isIdentifierChar(char c) {
        return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
