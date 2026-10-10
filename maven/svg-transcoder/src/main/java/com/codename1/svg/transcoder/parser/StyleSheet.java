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
package com.codename1.svg.transcoder.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The rules of an SVG's embedded {@code <style>} elements, and which of them
 * apply to an element.
 *
 * Exported artwork commonly colours its shapes through a class
 * ({@code .st0 { fill: #f00; }}) instead of an attribute on each one, so an
 * SVG read without its stylesheet comes out black.
 *
 * A selector is a type, {@code .class} or {@code #id}, any of them combined
 * ({@code path.a.b}), {@code *}, and chains of those with the descendant and
 * child ({@code >}) combinators; rules may list several, separated by commas.
 * A selector using anything else -- a pseudo-class, an attribute test, a
 * sibling combinator -- matches nothing, and at-rules are skipped whole.
 */
final class StyleSheet {

    /** What a selector can ask about an element. */
    static final class Element {
        final String name;
        final String id;
        final String[] classes;
        final Element parent;

        Element(String name, String id, String classAttr, Element parent) {
            this.name = name;
            this.id = id;
            this.classes = classAttr == null || classAttr.trim().isEmpty()
                    ? new String[0] : classAttr.trim().split("\\s+");
            this.parent = parent;
        }
    }

    private static final class Compound {
        String tag;
        String id;
        final List<String> classes = new ArrayList<String>();
        /** Whether this one must be a child, not any descendant, of the one before it. */
        boolean child;

        boolean matches(Element e) {
            if (tag != null && !tag.equals(e.name)) return false;
            if (id != null && !id.equals(e.id)) return false;
            for (String c : classes) {
                boolean found = false;
                for (String have : e.classes) {
                    if (c.equals(have)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }

    private static final class Rule {
        Compound[] chain;
        int specificity;
        String normal;
        String important;
    }

    private final List<Rule> rules = new ArrayList<Rule>();

    boolean isEmpty() {
        return rules.isEmpty();
    }

    /** Adds the rules of one {@code <style>} element. */
    void add(String css) {
        String text = stripComments(css);
        int pos = 0;
        while (pos < text.length()) {
            int open = text.indexOf('{', pos);
            if (open < 0) break;
            String prelude = text.substring(pos, open).trim();
            if (prelude.startsWith("@")) {
                int semi = prelude.indexOf(';');
                if (semi >= 0) {
                    // A statement such as @import, ending before this block.
                    pos = pos + text.substring(pos, open).indexOf(';') + 1;
                    continue;
                }
                pos = endOfBlock(text, open);
                continue;
            }
            int close = text.indexOf('}', open);
            if (close < 0) close = text.length();
            StringBuilder normal = new StringBuilder();
            StringBuilder important = new StringBuilder();
            for (String decl : text.substring(open + 1, close).split(";")) {
                int colon = decl.indexOf(':');
                if (colon <= 0) continue;
                String k = decl.substring(0, colon).trim();
                String v = decl.substring(colon + 1).trim();
                int bang = v.lastIndexOf('!');
                boolean imp = bang >= 0 && "important".equalsIgnoreCase(v.substring(bang + 1).trim());
                if (imp) v = v.substring(0, bang).trim();
                if (k.isEmpty() || v.isEmpty()) continue;
                (imp ? important : normal).append(k).append(':').append(v).append(';');
            }
            for (String selector : prelude.split(",")) {
                Compound[] chain = parseSelector(selector.trim());
                if (chain == null) continue;
                Rule rule = new Rule();
                rule.chain = chain;
                rule.normal = normal.toString();
                rule.important = important.toString();
                for (Compound c : chain) {
                    rule.specificity += (c.id != null ? 10000 : 0) + c.classes.size() * 100 + (c.tag != null ? 1 : 0);
                }
                rules.add(rule);
            }
            pos = Math.min(text.length(), close + 1);
        }
    }

    /**
     * The declarations that apply to an element, in the order they take
     * effect: {@code [0]} the ordinary ones, which an inline style overrides,
     * and {@code [1]} the {@code !important} ones, which override it. Either
     * may be empty; null when no rule matches.
     */
    String[] declarationsFor(Element e) {
        List<Rule> matched = null;
        for (Rule rule : rules) {
            if (matches(rule.chain, rule.chain.length - 1, e)) {
                if (matched == null) matched = new ArrayList<Rule>();
                matched.add(rule);
            }
        }
        if (matched == null) return null;
        // Stable, so rules of equal weight keep their order in the source
        // and the later one wins.
        Collections.sort(matched, new Comparator<Rule>() {
            @Override
            public int compare(Rule a, Rule b) {
                return a.specificity < b.specificity ? -1 : a.specificity > b.specificity ? 1 : 0;
            }
        });
        StringBuilder normal = new StringBuilder();
        StringBuilder important = new StringBuilder();
        for (Rule rule : matched) {
            normal.append(rule.normal);
            important.append(rule.important);
        }
        return new String[] {normal.toString(), important.toString()};
    }

    private static boolean matches(Compound[] chain, int index, Element e) {
        if (!chain[index].matches(e)) return false;
        if (index == 0) return true;
        if (chain[index].child) {
            return e.parent != null && matches(chain, index - 1, e.parent);
        }
        for (Element anc = e.parent; anc != null; anc = anc.parent) {
            if (matches(chain, index - 1, anc)) return true;
        }
        return false;
    }

    /** The compounds of a selector from left to right, or null for one that is not understood. */
    private static Compound[] parseSelector(String s) {
        if (s.isEmpty()) return null;
        List<Compound> chain = new ArrayList<Compound>();
        Compound cur = null;
        boolean child = false;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                cur = null;
                i++;
                continue;
            }
            if (c == '>') {
                if (chain.isEmpty() || child) return null;
                cur = null;
                child = true;
                i++;
                continue;
            }
            if (cur == null) {
                cur = new Compound();
                cur.child = child;
                child = false;
                chain.add(cur);
            }
            if (c == '*') {
                i++;
                continue;
            }
            int start = c == '.' || c == '#' ? i + 1 : i;
            int end = start;
            while (end < n && isIdentChar(s.charAt(end))) end++;
            if (end == start) return null;
            String ident = s.substring(start, end);
            if (c == '.') {
                cur.classes.add(ident);
            } else if (c == '#') {
                cur.id = ident;
            } else {
                cur.tag = ident;
            }
            i = end;
        }
        if (chain.isEmpty() || child) return null;
        return chain.toArray(new Compound[0]);
    }

    private static boolean isIdentChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
    }

    private static int endOfBlock(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) return i + 1;
            }
        }
        return text.length();
    }

    private static String stripComments(String css) {
        StringBuilder out = new StringBuilder(css.length());
        int i = 0;
        while (i < css.length()) {
            if (css.startsWith("/*", i)) {
                int end = css.indexOf("*/", i + 2);
                i = end < 0 ? css.length() : end + 2;
            } else if (css.startsWith("<!--", i)) {
                i += 4;
            } else if (css.startsWith("-->", i)) {
                i += 3;
            } else {
                out.append(css.charAt(i++));
            }
        }
        return out.toString();
    }
}
