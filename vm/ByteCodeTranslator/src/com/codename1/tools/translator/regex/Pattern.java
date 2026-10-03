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
package com.codename1.tools.translator.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A compiled regular expression with {@code java.util.regex} semantics for the
 * syntax the translator's text passes use, so they behave identically on every
 * host -- including a self-hosted translator, whose class library has no regex.
 *
 * <p>Supported: literals and escapes ({@code \t \n \r \f \e}, escaped
 * metacharacters), {@code .}, character classes with ranges, negation and
 * {@code \d \s \w \D \S \W} inside or outside them, capturing and
 * non-capturing groups, alternation, the greedy quantifiers {@code * + ?} and
 * {@code {n}}, {@code {n,}}, {@code {n,m}}, lookahead {@code (?=)} and
 * {@code (?!)}, back-references {@code \1..\9}, and the anchors {@code ^} and
 * {@code $} (input, or region, bounds; {@code $} also before a final line
 * terminator). Anything else is rejected when the pattern is compiled rather
 * than matched differently.
 *
 * <p>Matching is backtracking, in the order {@code java.util.regex} explores:
 * alternatives left to right, greedy quantifiers longest first, and a repeated
 * group stops repeating once an iteration matches the empty string.
 */
public final class Pattern {
    private static final Map<String, Pattern> CACHE = new HashMap<String, Pattern>();

    private final String pattern;
    final Node root;
    final int groupCount;
    final int loopCount;
    /** A literal every match starts with, or null; used to skip ahead in find(). */
    final String prefix;

    private Pattern(String pattern) {
        this.pattern = pattern;
        Parser p = new Parser(pattern);
        Node body = p.parseAlternation();
        if (p.pos != pattern.length()) {
            throw p.error("Unmatched closing ')'");
        }
        Accept accept = new Accept();
        root = body.link(accept);
        groupCount = p.groups;
        loopCount = p.loops;
        prefix = literalPrefix(body);
    }

    /** Compiles a pattern; equal pattern strings share one compiled instance. */
    public static Pattern compile(String regex) {
        Pattern p = CACHE.get(regex);
        if (p == null) {
            p = new Pattern(regex);
            CACHE.put(regex, p);
        }
        return p;
    }

    public static boolean matches(String regex, CharSequence input) {
        return compile(regex).matcher(input).matches();
    }

    /** {@code input.replaceAll(regex, replacement)}. */
    public static String replaceAll(String input, String regex, String replacement) {
        return compile(regex).matcher(input).replaceAll(replacement);
    }

    public String pattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return pattern;
    }

    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input);
    }

    /** {@code String.split(regex)}: trailing empty strings are removed. */
    public String[] split(CharSequence input) {
        List<String> parts = new ArrayList<String>();
        Matcher m = matcher(input);
        int index = 0;
        while (m.find()) {
            if (index == 0 && m.start() == 0 && m.end() == 0) {
                continue;
            }
            parts.add(input.subSequence(index, m.start()).toString());
            index = m.end();
        }
        if (index == 0) {
            return new String[]{input.toString()};
        }
        parts.add(input.subSequence(index, input.length()).toString());
        int size = parts.size();
        while (size > 0 && parts.get(size - 1).length() == 0) {
            size--;
        }
        return parts.subList(0, size).toArray(new String[size]);
    }

    private static String literalPrefix(Node n) {
        StringBuilder b = new StringBuilder();
        for (Node cur = n; cur != null; cur = cur.unlinkedNext) {
            if (cur instanceof Literal) {
                b.append(((Literal) cur).c);
            } else {
                break;
            }
        }
        return b.length() == 0 ? null : b.toString();
    }

    // ------------------------------------------------------------------ nodes

    /**
     * A matcher node. Parsing builds sequences with {@code unlinkedNext}; compile
     * time {@link #link} wires every node to the node that follows it, so a node
     * matches itself and then hands the position to {@code next}.
     */
    abstract static class Node {
        Node next;
        /** The following node in the sequence it was parsed in, before linking. */
        Node unlinkedNext;

        /** Matches from position {@code i}; true once the whole remaining pattern has matched. */
        abstract boolean match(Matcher m, int i);

        /** Links this sequence so that it continues with {@code tail}; returns its first node. */
        Node link(Node tail) {
            Node rest = unlinkedNext == null ? tail : unlinkedNext.link(tail);
            linkSelf(rest);
            return this;
        }

        void linkSelf(Node rest) {
            next = rest;
        }

        /** True when this node always consumes exactly one character. */
        boolean isSingleChar() {
            return false;
        }

        /** For a single-character node: whether it accepts c. */
        boolean accepts(char c) {
            return false;
        }
    }

    /** The end of the pattern: records where the match ended. */
    static final class Accept extends Node {
        @Override
        boolean match(Matcher m, int i) {
            if (m.requireEnd && i != m.to) {
                return false;
            }
            m.matchEnd = i;
            return true;
        }
    }

    /** The end of a lookahead's body. */
    static final class LookEnd extends Node {
        @Override
        boolean match(Matcher m, int i) {
            return true;
        }
    }

    abstract static class CharNode extends Node {
        @Override
        boolean match(Matcher m, int i) {
            return i < m.to && accepts(m.text.charAt(i)) && next.match(m, i + 1);
        }

        @Override
        boolean isSingleChar() {
            return true;
        }
    }

    static final class Literal extends CharNode {
        final char c;

        Literal(char c) {
            this.c = c;
        }

        @Override
        boolean accepts(char ch) {
            return ch == c;
        }
    }

    static final class AnyChar extends CharNode {
        @Override
        boolean accepts(char c) {
            return !isLineTerminator(c);
        }
    }

    static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r' || c == '\u0085' || c == '\u2028' || c == '\u2029';
    }

    /** A character class: ranges and predefined classes, possibly negated. */
    static final class CharClass extends CharNode {
        private final StringBuilder ranges = new StringBuilder();
        private boolean digit;
        private boolean notDigit;
        private boolean space;
        private boolean notSpace;
        private boolean word;
        private boolean notWord;
        boolean negated;

        void addRange(char from, char to) {
            ranges.append(from).append(to);
        }

        void addPredefined(char kind) {
            switch (kind) {
                case 'd': digit = true; break;
                case 'D': notDigit = true; break;
                case 's': space = true; break;
                case 'S': notSpace = true; break;
                case 'w': word = true; break;
                default: notWord = true; break;
            }
        }

        @Override
        boolean accepts(char c) {
            return member(c) != negated;
        }

        private boolean member(char c) {
            for (int i = 0; i < ranges.length(); i += 2) {
                if (c >= ranges.charAt(i) && c <= ranges.charAt(i + 1)) {
                    return true;
                }
            }
            return digit && isDigit(c) || notDigit && !isDigit(c)
                    || space && isSpace(c) || notSpace && !isSpace(c)
                    || word && isWord(c) || notWord && !isWord(c);
        }
    }

    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    static boolean isWord(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }

    /** A greedy repeat of a single-character node: no recursion per character. */
    static final class CharRepeat extends Node {
        final Node atom;
        final int min;
        final int max;

        CharRepeat(Node atom, int min, int max) {
            this.atom = atom;
            this.min = min;
            this.max = max;
        }

        @Override
        boolean match(Matcher m, int i) {
            int limit = m.to;
            int n = 0;
            while (n < max && i + n < limit && atom.accepts(m.text.charAt(i + n))) {
                n++;
            }
            for (; n >= min; n--) {
                if (next.match(m, i + n)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Alternation; each alternative continues with the same tail. */
    static final class Branch extends Node {
        final List<Node> alternatives;

        Branch(List<Node> alternatives) {
            this.alternatives = alternatives;
        }

        @Override
        void linkSelf(Node rest) {
            next = rest;
            for (int i = 0; i < alternatives.size(); i++) {
                Node alt = alternatives.get(i);
                alternatives.set(i, alt == null ? rest : alt.link(rest));
            }
        }

        @Override
        boolean match(Matcher m, int i) {
            for (int k = 0; k < alternatives.size(); k++) {
                if (alternatives.get(k).match(m, i)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Opens capturing group {@code index}; its body links to a matching {@link GroupTail}. */
    static final class GroupHead extends Node {
        final int index;
        Node body;

        GroupHead(int index, Node body) {
            this.index = index;
            this.body = body;
        }

        @Override
        void linkSelf(Node rest) {
            next = rest;
            GroupTail tail = new GroupTail(index);
            tail.next = rest;
            body = body == null ? tail : body.link(tail);
        }

        @Override
        boolean match(Matcher m, int i) {
            int saved = m.pendingStart[index];
            m.pendingStart[index] = i;
            boolean r = body.match(m, i);
            m.pendingStart[index] = saved;
            return r;
        }
    }

    static final class GroupTail extends Node {
        final int index;

        GroupTail(int index) {
            this.index = index;
        }

        @Override
        boolean match(Matcher m, int i) {
            int oldStart = m.groups[2 * index];
            int oldEnd = m.groups[2 * index + 1];
            m.groups[2 * index] = m.pendingStart[index];
            m.groups[2 * index + 1] = i;
            if (next.match(m, i)) {
                return true;
            }
            m.groups[2 * index] = oldStart;
            m.groups[2 * index + 1] = oldEnd;
            return false;
        }
    }

    /** A non-capturing group: just a sequence spliced into the parent. */
    static final class Sequence extends Node {
        Node body;

        Sequence(Node body) {
            this.body = body;
        }

        @Override
        void linkSelf(Node rest) {
            next = rest;
            body = body == null ? rest : body.link(rest);
        }

        @Override
        boolean match(Matcher m, int i) {
            return body.match(m, i);
        }
    }

    /**
     * A greedy repeat of an arbitrary sub-pattern. Its body links back to this node,
     * so each iteration re-enters here; per-loop iteration count and start position
     * live in the matcher and are restored on backtracking.
     */
    static final class Loop extends Node {
        final int id;
        final int min;
        final int max;
        Node body;
        final LoopBack back;

        Loop(int id, Node body, int min, int max) {
            this.id = id;
            this.body = body;
            this.min = min;
            this.max = max;
            this.back = new LoopBack(this);
        }

        @Override
        void linkSelf(Node rest) {
            next = rest;
            body = body.link(back);
        }

        /** Entry: the first iteration, which is mandatory when min is at least one. */
        @Override
        boolean match(Matcher m, int i) {
            int savedCount = m.loopCount[id];
            int savedStart = m.loopStart[id];
            boolean r;
            if (max > 0) {
                m.loopCount[id] = 1;
                m.loopStart[id] = i;
                r = body.match(m, i);
                if (!r && min == 0) {
                    r = next.match(m, i);
                }
            } else {
                r = next.match(m, i);
            }
            m.loopCount[id] = savedCount;
            m.loopStart[id] = savedStart;
            return r;
        }

        /** After an iteration: iterate again, then fall through to what follows. */
        boolean iterate(Matcher m, int i) {
            int count = m.loopCount[id];
            int start = m.loopStart[id];
            // Only an iteration that consumed input may be followed by another, or an
            // empty one would repeat forever -- and that holds below the minimum too.
            if (i != start) {
                if (count < min) {
                    m.loopCount[id] = count + 1;
                    m.loopStart[id] = i;
                    boolean r = body.match(m, i);
                    m.loopCount[id] = count;
                    m.loopStart[id] = start;
                    return r;
                }
                if (count < max) {
                    m.loopCount[id] = count + 1;
                    m.loopStart[id] = i;
                    boolean r = body.match(m, i);
                    m.loopCount[id] = count;
                    m.loopStart[id] = start;
                    if (r) {
                        return true;
                    }
                }
            }
            return next.match(m, i);
        }
    }

    static final class LoopBack extends Node {
        final Loop loop;

        LoopBack(Loop loop) {
            this.loop = loop;
        }

        @Override
        boolean match(Matcher m, int i) {
            return loop.iterate(m, i);
        }
    }

    /** {@code (?=X)} or {@code (?!X)}: tests X without consuming input. */
    static final class Lookahead extends Node {
        Node body;
        final boolean negative;

        Lookahead(Node body, boolean negative) {
            this.body = body;
            this.negative = negative;
        }

        @Override
        void linkSelf(Node rest) {
            next = rest;
            LookEnd end = new LookEnd();
            body = body == null ? end : body.link(end);
        }

        @Override
        boolean match(Matcher m, int i) {
            boolean savedRequireEnd = m.requireEnd;
            m.requireEnd = false;
            int[] savedGroups = negative ? m.groups.clone() : null;
            boolean found = body.match(m, i);
            m.requireEnd = savedRequireEnd;
            if (negative) {
                System.arraycopy(savedGroups, 0, m.groups, 0, savedGroups.length);
                return !found && next.match(m, i);
            }
            return found && next.match(m, i);
        }
    }

    static final class BackReference extends Node {
        final int index;

        BackReference(int index) {
            this.index = index;
        }

        @Override
        boolean match(Matcher m, int i) {
            int start = m.groups[2 * index];
            int end = m.groups[2 * index + 1];
            if (start < 0) {
                return false;
            }
            int length = end - start;
            if (i + length > m.to) {
                return false;
            }
            for (int k = 0; k < length; k++) {
                if (m.text.charAt(start + k) != m.text.charAt(i + k)) {
                    return false;
                }
            }
            return next.match(m, i + length);
        }
    }

    static final class Begin extends Node {
        @Override
        boolean match(Matcher m, int i) {
            return i == m.from && next.match(m, i);
        }
    }

    static final class End extends Node {
        @Override
        boolean match(Matcher m, int i) {
            int to = m.to;
            if (i < to - 2) {
                return false;
            }
            if (i == to - 2) {
                return m.text.charAt(i) == '\r' && m.text.charAt(i + 1) == '\n' && next.match(m, i);
            }
            if (i < to) {
                char c = m.text.charAt(i);
                if (!isLineTerminator(c)) {
                    return false;
                }
                // Never between the two halves of a \r\n.
                if (c == '\n' && i > 0 && m.text.charAt(i - 1) == '\r') {
                    return false;
                }
            }
            return next.match(m, i);
        }
    }

    // ------------------------------------------------------------------ parser

    private static final class Parser {
        final String p;
        int pos;
        int groups;
        int loops;
        /** Groups whose closing parenthesis has been seen, for back-reference checks. */
        int closedGroups;

        Parser(String p) {
            this.p = p;
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " near index " + pos + ": " + p);
        }

        Node parseAlternation() {
            List<Node> alternatives = new ArrayList<Node>();
            alternatives.add(parseSequence());
            while (pos < p.length() && p.charAt(pos) == '|') {
                pos++;
                alternatives.add(parseSequence());
            }
            if (alternatives.size() == 1) {
                return alternatives.get(0);
            }
            return new Branch(alternatives);
        }

        /** Parses a sequence up to '|' or ')'; returns its first node (null if empty). */
        Node parseSequence() {
            Node first = null;
            Node last = null;
            while (pos < p.length()) {
                char c = p.charAt(pos);
                if (c == '|' || c == ')') {
                    break;
                }
                Node atom = parseQuantified();
                if (first == null) {
                    first = atom;
                } else {
                    last.unlinkedNext = atom;
                }
                last = atom;
            }
            return first;
        }

        Node parseQuantified() {
            Node atom = parseAtom();
            if (pos >= p.length()) {
                return atom;
            }
            char c = p.charAt(pos);
            int min;
            int max;
            if (c == '*') {
                min = 0;
                max = Integer.MAX_VALUE;
                pos++;
            } else if (c == '+') {
                min = 1;
                max = Integer.MAX_VALUE;
                pos++;
            } else if (c == '?') {
                min = 0;
                max = 1;
                pos++;
            } else if (c == '{' && pos + 1 < p.length() && isDigit(p.charAt(pos + 1))) {
                pos++;
                min = number();
                max = min;
                if (pos < p.length() && p.charAt(pos) == ',') {
                    pos++;
                    max = pos < p.length() && isDigit(p.charAt(pos)) ? number() : Integer.MAX_VALUE;
                }
                if (pos >= p.length() || p.charAt(pos) != '}') {
                    throw error("Unclosed counted closure");
                }
                pos++;
            } else {
                return atom;
            }
            if (pos < p.length() && (p.charAt(pos) == '?' || p.charAt(pos) == '+')) {
                throw error("Reluctant and possessive quantifiers are not supported");
            }
            if (atom.isSingleChar()) {
                return new CharRepeat(atom, min, max);
            }
            return new Loop(loops++, atom, min, max);
        }

        int number() {
            int n = 0;
            while (pos < p.length() && isDigit(p.charAt(pos))) {
                n = n * 10 + (p.charAt(pos) - '0');
                pos++;
            }
            return n;
        }

        Node parseAtom() {
            char c = p.charAt(pos);
            switch (c) {
                case '(':
                    return parseGroup();
                case '[':
                    pos++;
                    return parseClass();
                case '.':
                    pos++;
                    return new AnyChar();
                case '^':
                    pos++;
                    return new Begin();
                case '$':
                    pos++;
                    return new End();
                case '\\':
                    pos++;
                    return parseEscape();
                case '*':
                case '+':
                case '?':
                case '{':
                    throw error("Dangling meta character '" + c + "'");
                default:
                    pos++;
                    return new Literal(c);
            }
        }

        Node parseGroup() {
            pos++;
            if (p.startsWith("?:", pos)) {
                pos += 2;
                Node body = parseAlternation();
                expect(')');
                return new Sequence(body);
            }
            if (p.startsWith("?=", pos) || p.startsWith("?!", pos)) {
                boolean negative = p.charAt(pos + 1) == '!';
                pos += 2;
                Node body = parseAlternation();
                expect(')');
                return new Lookahead(body, negative);
            }
            if (pos < p.length() && p.charAt(pos) == '?') {
                throw error("Unsupported group construct");
            }
            int index = ++groups;
            Node body = parseAlternation();
            expect(')');
            closedGroups = Math.max(closedGroups, index);
            return new GroupHead(index, body);
        }

        void expect(char c) {
            if (pos >= p.length() || p.charAt(pos) != c) {
                throw error("Unclosed group");
            }
            pos++;
        }

        Node parseEscape() {
            if (pos >= p.length()) {
                throw error("Unexpected internal error");
            }
            char c = p.charAt(pos++);
            if (c >= '1' && c <= '9') {
                // Java takes the longest number that names an existing group.
                int ref = c - '0';
                while (pos < p.length() && isDigit(p.charAt(pos))) {
                    int longer = ref * 10 + (p.charAt(pos) - '0');
                    if (longer > groups) {
                        break;
                    }
                    ref = longer;
                    pos++;
                }
                if (ref > groups) {
                    throw error("Back-reference to a group that does not exist");
                }
                return new BackReference(ref);
            }
            if ("dDsSwW".indexOf(c) >= 0) {
                CharClass cls = new CharClass();
                cls.addPredefined(c);
                return cls;
            }
            return new Literal(escapedLiteral(c));
        }

        char escapedLiteral(char c) {
            switch (c) {
                case 't': return '\t';
                case 'n': return '\n';
                case 'r': return '\r';
                case 'f': return '\f';
                case 'e': return '\u001B';
                default:
                    if (isWord(c)) {
                        throw error("Unsupported escape \\" + c);
                    }
                    return c;
            }
        }

        Node parseClass() {
            CharClass cls = new CharClass();
            if (pos < p.length() && p.charAt(pos) == '^') {
                cls.negated = true;
                pos++;
            }
            boolean first = true;
            while (true) {
                if (pos >= p.length()) {
                    throw error("Unclosed character class");
                }
                char c = p.charAt(pos);
                if (c == ']' && !first) {
                    pos++;
                    return cls;
                }
                first = false;
                if (c == '[' || c == '&' && p.startsWith("&&", pos)) {
                    throw error("Nested classes and intersections are not supported");
                }
                char from;
                if (c == '\\') {
                    pos++;
                    char e = p.charAt(pos++);
                    if ("dDsSwW".indexOf(e) >= 0) {
                        cls.addPredefined(e);
                        continue;
                    }
                    from = escapedLiteral(e);
                } else {
                    from = c;
                    pos++;
                }
                if (pos + 1 < p.length() && p.charAt(pos) == '-' && p.charAt(pos + 1) != ']') {
                    pos++;
                    char to = p.charAt(pos++);
                    if (to == '\\') {
                        to = escapedLiteral(p.charAt(pos++));
                    }
                    if (to < from) {
                        throw error("Illegal character range");
                    }
                    cls.addRange(from, to);
                } else {
                    cls.addRange(from, from);
                }
            }
        }
    }
}
