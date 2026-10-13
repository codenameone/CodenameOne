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
import java.util.Map;

/**
 * The rules of an SVG's embedded {@code <style>} elements, and which of them
 * apply to an element.
 *
 * Exported artwork commonly colours its shapes through a class
 * ({@code .st0 { fill: #f00; }}) instead of an attribute on each one, so an
 * SVG read without its stylesheet comes out black.
 *
 * A selector is a type, {@code .class}, {@code #id} or an attribute test
 * ({@code [a]}, {@code [a=v]} and the {@code ~=}, {@code |=}, {@code ^=},
 * {@code $=} and {@code *=} forms), any of them combined
 * ({@code path.a[data-role="accent"]}), {@code *}, and chains of those with
 * the descendant and child ({@code >}) combinators; rules may list several,
 * separated by commas. A selector using anything else -- a pseudo-class, a
 * sibling combinator -- matches nothing, and at-rules are skipped whole.
 */
final class StyleSheet {

    /** What a selector can ask about an element. */
    static final class Element {
        final String name;
        final String id;
        final String[] classes;
        final Element parent;
        /** Every attribute by its local name, for the attribute tests. */
        final Map<String, String> attributes;

        Element(String name, String id, String classAttr, Element parent) {
            this(name, id, classAttr, parent, null);
        }

        Element(String name, String id, String classAttr, Element parent, Map<String, String> attributes) {
            this.attributes = attributes;
            this.name = name;
            this.id = id;
            this.classes = classAttr == null || classAttr.trim().isEmpty()
                    ? new String[0] : classAttr.trim().split("\\s+");
            this.parent = parent;
        }
    }

    /** One attribute test of a selector, such as {@code [data-role="accent"]}. */
    private static final class AttributeTest {
        String name;
        /** 0 when the attribute only has to be there, else the character before the {@code =}, or {@code =}. */
        char op;
        String value;

        boolean matches(Element e) {
            String have = e.attributes == null ? null : e.attributes.get(name);
            if (have == null) return false;
            switch (op) {
                case 0:
                    return true;
                case '=':
                    return have.equals(value);
                case '~':
                    if (value.isEmpty()) return false;
                    for (String word : have.trim().split("\\s+")) {
                        if (word.equals(value)) return true;
                    }
                    return false;
                case '|':
                    return have.equals(value) || have.startsWith(value + "-");
                case '^':
                    return !value.isEmpty() && have.startsWith(value);
                case '$':
                    return !value.isEmpty() && have.endsWith(value);
                default:
                    return !value.isEmpty() && have.indexOf(value) >= 0;
            }
        }
    }

    private static final class Compound {
        String tag;
        String id;
        final List<String> classes = new ArrayList<String>();
        final List<AttributeTest> attributes = new ArrayList<AttributeTest>();
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
            for (AttributeTest test : attributes) {
                if (!test.matches(e)) return false;
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

    /**
     * Whether a media query list takes in an image painted for a screen:
     * nothing at all, {@code all} or {@code screen}, with or without
     * {@code only}, and the opposite after {@code not}. A query that only
     * tests a feature holds as {@link #featuresHold} says.
     */
    static boolean mediaMatches(String media) {
        if (media == null || media.trim().isEmpty()) return true;
        for (String query : media.split(",")) {
            String q = query.trim();
            boolean not = false;
            if (q.regionMatches(true, 0, "only", 0, 4) && q.length() > 4 && isSpace(q.charAt(4))) {
                q = q.substring(4).trim();
            } else if (q.regionMatches(true, 0, "not", 0, 3) && q.length() > 3 && isSpace(q.charAt(3))) {
                not = true;
                q = q.substring(3).trim();
            }
            int end = 0;
            while (end < q.length() && isIdentChar(q.charAt(end))) end++;
            String type = q.substring(0, end);
            boolean ours = (type.isEmpty() || "all".equalsIgnoreCase(type) || "screen".equalsIgnoreCase(type))
                    && featuresHold(q.substring(end));
            if (ours != not) return true;
        }
        return false;
    }

    /**
     * Whether the features a query tests, {@code (max-width: 20px)}, hold.
     * An image is drawn in the light scheme, which is the one thing that is
     * known; a test of anything else -- a width, when the size the image
     * will be drawn at is not known here -- does not hold, so that rules
     * meant for some other setting are left out and not put in everywhere.
     */
    private static boolean featuresHold(String rest) {
        int from = 0;
        while (true) {
            int open = rest.indexOf('(', from);
            if (open < 0) return true;
            int close = rest.indexOf(')', open);
            if (close < 0) return false;
            String feature = rest.substring(open + 1, close);
            int colon = feature.indexOf(':');
            String name = (colon < 0 ? feature : feature.substring(0, colon)).trim();
            String value = colon < 0 ? "" : feature.substring(colon + 1).trim();
            if (!("prefers-color-scheme".equalsIgnoreCase(name) && "light".equalsIgnoreCase(value))) {
                return false;
            }
            from = close + 1;
        }
    }

    /** A property name as it is looked up: its ASCII letters in lower case, whatever the locale. */
    private static String asciiLower(String s) {
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) out = new StringBuilder(s);
                out.setCharAt(i, (char) (c + ('a' - 'A')));
            }
        }
        return out == null ? s : out.toString();
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
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
                int end = endOfBlock(text, open);
                if (prelude.regionMatches(true, 0, "@media", 0, 6) && mediaMatches(prelude.substring(6))) {
                    // The rules of a medium an image is drawn for are rules
                    // like any other; those of another are passed over.
                    add(text.substring(open + 1, Math.max(open + 1, end - 1)));
                }
                pos = end;
                continue;
            }
            int close = text.indexOf('}', open);
            if (close < 0) close = text.length();
            StringBuilder normal = new StringBuilder();
            StringBuilder important = new StringBuilder();
            for (String decl : text.substring(open + 1, close).split(";")) {
                int colon = decl.indexOf(':');
                if (colon <= 0) continue;
                // Property names are the same in any case: `FILL` is `fill`.
                String k = asciiLower(decl.substring(0, colon).trim());
                String v = decl.substring(colon + 1).trim();
                int bang = v.lastIndexOf('!');
                boolean imp = bang >= 0 && "important".equalsIgnoreCase(v.substring(bang + 1).trim());
                if (imp) v = v.substring(0, bang).trim();
                if (k.isEmpty() || v.isEmpty()) continue;
                (imp ? important : normal).append(k).append(':').append(v).append(';');
            }
            for (String selector : splitSelectors(prelude)) {
                Compound[] chain = parseSelector(selector.trim());
                if (chain == null) continue;
                Rule rule = new Rule();
                rule.chain = chain;
                rule.normal = normal.toString();
                rule.important = important.toString();
                for (Compound c : chain) {
                    rule.specificity += (c.id != null ? 10000 : 0) + (c.classes.size() + c.attributes.size()) * 100 + (c.tag != null ? 1 : 0);
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
            if (c == '[') {
                int close = endOfAttribute(s, i);
                if (close < 0) return null;
                AttributeTest test = parseAttribute(s.substring(i + 1, close));
                if (test == null) return null;
                cur.attributes.add(test);
                i = close + 1;
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

    /** The selectors of a rule: its prelude cut at the commas that are not inside an attribute test. */
    private static List<String> splitSelectors(String prelude) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        int i = 0;
        while (i < prelude.length()) {
            char c = prelude.charAt(i);
            if (c == '[') {
                int close = endOfAttribute(prelude, i);
                if (close < 0) break;
                i = close + 1;
                continue;
            }
            if (c == ',') {
                out.add(prelude.substring(from, i));
                from = i + 1;
            }
            i++;
        }
        out.add(prelude.substring(from));
        return out;
    }

    /** The index of the {@code ]} closing the attribute test opened at {@code open}, or -1. */
    private static int endOfAttribute(String s, int open) {
        char quote = 0;
        for (int i = open + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ']') {
                return i;
            }
        }
        return -1;
    }

    /** What is written between the brackets of an attribute test, or null when it is not understood. */
    private static AttributeTest parseAttribute(String body) {
        String t = body.trim();
        int end = 0;
        while (end < t.length() && isIdentChar(t.charAt(end))) end++;
        if (end == 0) return null;
        AttributeTest test = new AttributeTest();
        test.name = t.substring(0, end);
        String rest = t.substring(end).trim();
        if (rest.isEmpty()) return test;
        char first = rest.charAt(0);
        if (first == '=') {
            test.op = '=';
            rest = rest.substring(1).trim();
        } else if (rest.length() > 1 && rest.charAt(1) == '='
                && (first == '~' || first == '|' || first == '^' || first == '$' || first == '*')) {
            test.op = first;
            rest = rest.substring(2).trim();
        } else {
            return null;
        }
        if (rest.length() >= 2 && (rest.charAt(0) == '"' || rest.charAt(0) == '\'')) {
            // A quoted value; anything after it, such as a case flag, is
            // not understood.
            if (rest.charAt(rest.length() - 1) != rest.charAt(0)) return null;
            test.value = rest.substring(1, rest.length() - 1);
            return test;
        }
        for (int i = 0; i < rest.length(); i++) {
            if (!isIdentChar(rest.charAt(i))) return null;
        }
        if (rest.isEmpty()) return null;
        test.value = rest;
        return test;
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
