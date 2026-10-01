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

/// The native sources as the C compiler will see them, for the feature switches the
/// builders own.
///
/// A Java method, field or class whose name appears in a native source is kept: the C
/// side may call it. The port's natives carry whole features -- HomeKit, Intents, CallKit,
/// Nearby, Health -- behind `#ifdef CN1_INCLUDE_<FEATURE>`, and a builder turns one on by
/// rewriting `//#define CN1_INCLUDE_<FEATURE>` into a `#define` before the translator runs.
/// A feature left off is compiled out, but its callbacks were still named in the text.
///
/// This drops the text of a branch that such a switch turns off, and only that. What it
/// knows about a macro:
///
/// - a `CN1_INCLUDE_` switch is OFF when no native source or header has a `#define` line
///   for it anywhere. A `-D` on the compiler command line is invisible here; the builders
///   never pass one for these switches;
/// - any other `CN1_` macro the natives `#define` (CN1_VPN_HAS_NE, CN1_CALL_HAS_CALLKIT)
///   is OFF when every one of those defines sat in dropped text. That is repeated until
///   nothing changes, and only ever turns more macros off;
/// - nothing is ever known ON: a `#define` that survives may sit under a condition this
///   cannot evaluate (`__has_include`, a target check, an `#undef` after it), so such a
///   macro, like every other macro, is unknown.
///
/// A condition is evaluated only as `#ifdef X`, `#ifndef X` or a conjunction of
/// `defined(X)` / `!defined(X)` terms: a branch is dropped when a term is known false, and
/// otherwise every branch is kept. An `#elif` makes the rest of its conditional unknown.
/// Inside a dropped branch only the conditional lines themselves remain. A file whose
/// conditionals do not balance is kept whole.
///
/// -Dcn1.nativeFeatureFilter=false keeps every native source unfiltered.
final class NativeFeatureFilter {
    private static final String SWITCH = "CN1_INCLUDE_";
    private static final String OWN = "CN1_";
    private static final int KEEP = 0;
    private static final int DROP = 1;
    private static final int UNKNOWN = 2;
    private static final int OFF = 0;
    private static final int ON = 1;
    private static final int NOT_KNOWN = -1;

    private NativeFeatureFilter() {
    }

    static boolean enabled() {
        return !"false".equalsIgnoreCase(Util.getProperty("cn1.nativeFeatureFilter", "true"));
    }

    /// Filters {@code sources} and {@code headers} together, returning {sources, headers}.
    static String[][] filterAll(String[] sources, String[] headers) {
        String[][] raw = {sources, headers};
        // Only OFF is ever known. A `#define` that survives may still sit under a condition
        // this cannot evaluate (`__has_include`, a target check, an `#undef` after it), so
        // a macro with one is unknown and both its branches stay.
        Map<String, Boolean> known = new HashMap<String, Boolean>();
        java.util.Set<String> definedAnywhere = definedMacros(raw);
        for (String name : definedAnywhere) {
            if (name.startsWith(SWITCH)) {
                known.put(name, null);
            }
        }
        String[][] out = raw;
        for (int round = 0; round < 8; round++) {
            out = new String[][] {filter(sources, known), filter(headers, known)};
            java.util.Set<String> surviving = definedMacros(out);
            boolean changed = false;
            for (String name : definedAnywhere) {
                if (name.startsWith(OWN) && !surviving.contains(name) && !Boolean.FALSE.equals(known.get(name))) {
                    // Every #define of it was in dropped text. Monotone: dropping more
                    // text can only turn more macros off, never one back on.
                    known.put(name, Boolean.FALSE);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return out;
    }

    /// Every macro some text has a `#define` line for.
    private static java.util.Set<String> definedMacros(String[][] texts) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        for (String[] group : texts) {
            if (group == null) {
                continue;
            }
            for (String t : group) {
                if (t == null) {
                    continue;
                }
                int pos = 0;
                while (pos < t.length()) {
                    int end = t.indexOf('\n', pos);
                    if (end < 0) {
                        end = t.length();
                    }
                    String[] d = directive(t, pos, end);
                    if (d != null && "define".equals(d[0]) && d[1].length() > 0) {
                        out.add(d[1]);
                    }
                    pos = end + 1;
                }
            }
        }
        return out;
    }

    static String[] filter(String[] texts, Map<String, Boolean> known) {
        if (texts == null) {
            return null;
        }
        String[] out = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            out[i] = texts[i] == null ? null : filter(texts[i], known);
        }
        return out;
    }

    /// {@code text} with the lines of every switched-off branch removed.
    static String filter(String text, Map<String, Boolean> known) {
        if (text.indexOf(SWITCH) < 0 && text.indexOf(OWN) < 0) {
            return text;
        }
        // Copied only from the first line actually dropped: most texts lose nothing, and
        // rebuilding an unchanged megabyte of C on every round was most of this pass.
        StringBuilder b = null;
        // One entry per open conditional: the state of the branch being read.
        List<int[]> stack = new ArrayList<int[]>();
        int dropping = 0;
        int pos = 0;
        while (pos < text.length()) {
            int end = text.indexOf('\n', pos);
            if (end < 0) {
                end = text.length();
            }
            String[] d = directive(text, pos, end);
            if (d != null) {
                String kind = d[0];
                if ("ifdef".equals(kind) || "ifndef".equals(kind) || "if".equals(kind)) {
                    int state = condition(kind, d[1], known);
                    stack.add(new int[] {state});
                    if (state == DROP) {
                        dropping++;
                    }
                } else if ("elif".equals(kind)) {
                    if (stack.isEmpty()) {
                        return text;
                    }
                    int[] top = stack.get(stack.size() - 1);
                    if (top[0] == DROP) {
                        dropping--;
                    }
                    top[0] = UNKNOWN;
                } else if ("else".equals(kind)) {
                    if (stack.isEmpty()) {
                        return text;
                    }
                    int[] top = stack.get(stack.size() - 1);
                    if (top[0] == DROP) {
                        dropping--;
                        top[0] = KEEP;
                    } else if (top[0] == KEEP) {
                        top[0] = DROP;
                        dropping++;
                    }
                } else if ("endif".equals(kind)) {
                    if (stack.isEmpty()) {
                        return text;
                    }
                    int[] top = stack.remove(stack.size() - 1);
                    if (top[0] == DROP) {
                        dropping--;
                    }
                }
            }
            // Inside a dropped branch only the conditional structure itself is kept; a
            // #define there is exactly what must disappear.
            if (dropping == 0 || (d != null && isConditional(d[0]))) {
                if (b != null) {
                    b.append(text, pos, end);
                }
            } else if (b == null) {
                b = new StringBuilder(text.length());
                b.append(text, 0, pos);
            }
            if (b != null && end < text.length()) {
                b.append('\n');
            }
            pos = end + 1;
        }
        return b != null && stack.isEmpty() ? b.toString() : text;
    }

    private static boolean isConditional(String kind) {
        return "if".equals(kind) || "ifdef".equals(kind) || "ifndef".equals(kind) || "elif".equals(kind)
                || "else".equals(kind) || "endif".equals(kind);
    }

    private static int condition(String kind, String rest, Map<String, Boolean> known) {
        if ("ifdef".equals(kind) || "ifndef".equals(kind)) {
            int on = knowledge(rest, known);
            if (on == NOT_KNOWN) {
                return UNKNOWN;
            }
            return (on == ON) != "ifndef".equals(kind) ? KEEP : DROP;
        }
        // A conjunction of defined() terms; anything else is unknown.
        boolean allTrue = true;
        int from = 0;
        while (true) {
            int and = rest.indexOf("&&", from);
            String term = (and < 0 ? rest.substring(from) : rest.substring(from, and)).trim();
            if (term.indexOf("||") >= 0) {
                return UNKNOWN;
            }
            int v = definedTerm(term, known);
            if (v == NOT_KNOWN) {
                allTrue = false;
            } else if (v == OFF) {
                return DROP;
            }
            if (and < 0) {
                break;
            }
            from = and + 2;
        }
        return allTrue ? KEEP : UNKNOWN;
    }

    /// The value of one `defined(X)` / `!defined(X)` / `defined X` term: ON, OFF or
    /// NOT_KNOWN.
    private static int definedTerm(String term, Map<String, Boolean> known) {
        boolean negate = false;
        String r = term;
        while (r.startsWith("(") && r.endsWith(")") && balanced(r.substring(1, r.length() - 1))) {
            r = r.substring(1, r.length() - 1).trim();
        }
        if (r.startsWith("!")) {
            negate = true;
            r = r.substring(1).trim();
        }
        if (!r.startsWith("defined")) {
            return NOT_KNOWN;
        }
        r = r.substring("defined".length()).trim();
        if (r.startsWith("(") && r.endsWith(")")) {
            r = r.substring(1, r.length() - 1).trim();
        }
        int on = knowledge(r, known);
        if (on == NOT_KNOWN) {
            return NOT_KNOWN;
        }
        return (on == ON) != negate ? ON : OFF;
    }

    private static boolean balanced(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static int knowledge(String macro, Map<String, Boolean> known) {
        if (!isIdentifier(macro)) {
            return NOT_KNOWN;
        }
        if (known.containsKey(macro)) {
            Boolean v = known.get(macro);
            return v == null ? NOT_KNOWN : v.booleanValue() ? ON : OFF;
        }
        // A switch nothing defines is off; any other macro nothing here defines may come
        // from a system header or the compiler, so it is unknown.
        return macro.startsWith(SWITCH) ? OFF : NOT_KNOWN;
    }

    private static boolean isIdentifier(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) {
                return false;
            }
        }
        return s.length() > 0;
    }

    /// {directive, rest} for a preprocessor line, or null. The rest drops a trailing
    /// comment, so `#endif // CN1_INCLUDE_X` and `#ifdef X /* why */` read as intended;
    /// for a `#define` it is just the macro's name.
    private static String[] directive(String t, int start, int end) {
        int i = start;
        while (i < end && (t.charAt(i) == ' ' || t.charAt(i) == '\t')) {
            i++;
        }
        if (i >= end || t.charAt(i) != '#') {
            return null;
        }
        i++;
        while (i < end && (t.charAt(i) == ' ' || t.charAt(i) == '\t')) {
            i++;
        }
        int w = i;
        while (i < end && t.charAt(i) >= 'a' && t.charAt(i) <= 'z') {
            i++;
        }
        String kind = t.substring(w, i);
        String rest = t.substring(i, end);
        int c = rest.indexOf("//");
        if (c >= 0) {
            rest = rest.substring(0, c);
        }
        c = rest.indexOf("/*");
        if (c >= 0) {
            rest = rest.substring(0, c);
        }
        rest = rest.trim();
        if ("define".equals(kind)) {
            int sp = 0;
            while (sp < rest.length() && rest.charAt(sp) != ' ' && rest.charAt(sp) != '\t'
                    && rest.charAt(sp) != '(') {
                sp++;
            }
            rest = rest.substring(0, sp);
        }
        return new String[] {kind, rest};
    }
}
