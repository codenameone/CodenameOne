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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/// `java.util.regex.Pattern` for the Codename One runtime: a backtracking
/// matcher written for this module, with the JDK's syntax and the JDK's
/// answers for the constructs it has.
///
/// #### Supported
///
/// - Literals, `.`, and every escape for a character: `\t \n \r \f \a \e`,
///   `\0nn`, `\xhh`, `\x{h...}`, backslash-u with four hex digits, `\cX`,
///   and `\Q...\E`.
/// - Character classes with ranges, negation, nested unions and `&&`
///   intersections; `\d \D \w \W \s \S \h \H \v \V`; `\R`.
/// - The POSIX classes (`\p{Alpha}`, `\p{Punct}`, ...), the `java...`
///   classes, and the Unicode properties a device can answer: `L`, `Lu`,
///   `Ll`, `N`/`Nd`, `IsAlphabetic`, `IsLetter`, `IsDigit`, `IsUppercase`,
///   `IsLowercase`, `IsWhite_Space`.
/// - Greedy, reluctant and possessive quantifiers.
/// - Capturing, non-capturing and named groups, back references by number
///   and by name, alternation, atomic groups.
/// - Lookahead and bounded lookbehind, positive and negative.
/// - `^ $ \A \z \Z \G \b \B`.
/// - `CASE_INSENSITIVE`, `MULTILINE`, `DOTALL`, `COMMENTS`, `LITERAL`,
///   `UNIX_LINES` and `UNICODE_CASE`, as arguments and inline (`(?i)`,
///   `(?i:...)`, `(?-i)`).
///
/// #### Refused
///
/// These throw [PatternSyntaxException] (the flags an
/// `UnsupportedOperationException`) naming the construct, when the pattern
/// is compiled. Nothing is accepted and then matched differently.
///
/// - Unicode scripts, blocks and general categories other than the ones
///   above (`\p{IsGreek}`, `\p{InArrows}`, `\p{Sc}`, ...).
/// - `\X`, `\N{name}`, `\b{g}`.
/// - `CANON_EQ` and `UNICODE_CHARACTER_CLASS` (`(?U)`).
/// - A code point above U+FFFF inside a character class.
///
/// #### Differences
///
/// Text is matched one UTF-16 `char` at a time: `.` and a character class
/// take half of a surrogate pair. Which characters are letters is what
/// [JdkStrings#isLetter(char)] answers, the scripts a device font is likely
/// to have rather than all of Unicode. A group repeated many thousand times
/// over a long text recurses once for each repetition, as the JDK's does,
/// on a stack that is smaller on a device.
public final class Pattern {

    public static final int UNIX_LINES = 0x01;
    public static final int CASE_INSENSITIVE = 0x02;
    public static final int COMMENTS = 0x04;
    public static final int MULTILINE = 0x08;
    public static final int LITERAL = 0x10;
    public static final int DOTALL = 0x20;
    public static final int UNICODE_CASE = 0x40;
    public static final int CANON_EQ = 0x80;
    public static final int UNICODE_CHARACTER_CLASS = 0x100;

    private final String regex;
    private final int flags;
    final Node root;
    final int groupCount;
    final int loopCount;
    final Map<String, Integer> names;

    private Pattern(String regex, int flags) {
        if (regex == null) {
            throw new NullPointerException();
        }
        if ((flags & CANON_EQ) != 0) {
            throw new UnsupportedOperationException("Pattern.CANON_EQ is not supported by the Codename One runtime");
        }
        if ((flags & UNICODE_CHARACTER_CLASS) != 0) {
            throw new UnsupportedOperationException(
                    "Pattern.UNICODE_CHARACTER_CLASS is not supported by the Codename One runtime");
        }
        this.regex = regex;
        this.flags = flags;
        Parser parser = new Parser(regex, flags);
        this.root = parser.compile();
        this.groupCount = parser.groups;
        this.loopCount = parser.loops;
        this.names = parser.names;
    }

    public static Pattern compile(String regex) {
        return new Pattern(regex, 0);
    }

    public static Pattern compile(String regex, int flags) {
        return new Pattern(regex, flags);
    }

    public String pattern() {
        return regex;
    }

    @Override
    public String toString() {
        return regex;
    }

    public int flags() {
        return flags;
    }

    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input);
    }

    public static boolean matches(String regex, CharSequence input) {
        return compile(regex).matcher(input).matches();
    }

    public String[] split(CharSequence input) {
        return split(input, 0);
    }

    /// Splits `input` around the matches of this pattern, by the JDK's
    /// rules for `limit`.
    public String[] split(CharSequence input, int limit) {
        int index = 0;
        boolean limited = limit > 0;
        List<String> list = new ArrayList<String>();
        Matcher m = matcher(input);
        while (m.find()) {
            if (!limited || list.size() < limit - 1) {
                if (index == 0 && m.start() == 0 && m.end() == 0) {
                    // No empty leading piece for a zero-width match at the start.
                    continue;
                }
                list.add(input.subSequence(index, m.start()).toString());
                index = m.end();
            } else if (list.size() == limit - 1) {
                list.add(input.subSequence(index, input.length()).toString());
                index = m.end();
            }
        }
        if (index == 0) {
            return new String[] {input.toString()};
        }
        if (!limited || list.size() < limit) {
            list.add(input.subSequence(index, input.length()).toString());
        }
        int size = list.size();
        if (limit == 0) {
            while (size > 0 && list.get(size - 1).length() == 0) {
                size--;
            }
        }
        String[] out = new String[size];
        for (int i = 0; i < size; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    public Stream<String> splitAsStream(CharSequence input) {
        String[] pieces = split(input, 0);
        // The stream drops the one empty piece an empty input splits into.
        if (pieces.length == 1 && pieces[0].length() == 0 && input.length() == 0) {
            return JdkCollections.stream(new String[0]);
        }
        return JdkCollections.stream(pieces);
    }

    /// A pattern that matches `s` and nothing else.
    public static String quote(String s) {
        int slashE = s.indexOf("\\E");
        if (slashE < 0) {
            return "\\Q" + s + "\\E";
        }
        StringBuilder sb = new StringBuilder(s.length() * 2);
        sb.append("\\Q");
        int current = 0;
        while (slashE >= 0) {
            sb.append(s.substring(current, slashE));
            current = slashE + 2;
            sb.append("\\E\\\\E\\Q");
            slashE = s.indexOf("\\E", current);
        }
        sb.append(s.substring(current));
        sb.append("\\E");
        return sb.toString();
    }

    public Predicate<String> asPredicate() {
        return new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return matcher(s).find();
            }
        };
    }

    public Predicate<String> asMatchPredicate() {
        return new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return matcher(s).matches();
            }
        };
    }

    // ------------------------------------------------------------------
    // Characters
    // ------------------------------------------------------------------

    static boolean lineTerminator(char c, boolean unix) {
        if (c == '\n') {
            return true;
        }
        return !unix && (c == '\r' || c == 0x85 || c == 0x2028 || c == 0x2029);
    }

    static boolean asciiLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    static boolean wordChar(char c) {
        return c == '_' || JdkStrings.isLetterOrDigit(c);
    }

    /// Whether `a` and `b` are the same character when case is ignored:
    /// for ASCII letters only, or by the device's case tables.
    static boolean sameIgnoringCase(char a, char b, boolean unicode) {
        if (a == b) {
            return true;
        }
        if (a < 0x80 && b < 0x80) {
            return asciiLetter(a) && (a ^ 0x20) == b;
        }
        if (!unicode) {
            return false;
        }
        char ua = Character.toUpperCase(a);
        char ub = Character.toUpperCase(b);
        return ua == ub || Character.toLowerCase(ua) == Character.toLowerCase(ub);
    }

    // ------------------------------------------------------------------
    // The compiled form: a chain of nodes, each matching at a position and
    // then asking the next one to match the rest.
    // ------------------------------------------------------------------

    abstract static class Node {
        Node next;

        abstract boolean match(Matcher m, int i);
    }

    /// The end of the whole pattern.
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

    /// The end of a pattern inside another: an atomic group or a lookaround.
    static final class SubEnd extends Node {
        @Override
        boolean match(Matcher m, int i) {
            if (m.endAt >= 0 && i != m.endAt) {
                return false;
            }
            m.subEnd = i;
            return true;
        }
    }

    static final class Pass extends Node {
        @Override
        boolean match(Matcher m, int i) {
            return next.match(m, i);
        }
    }

    /// A node that takes exactly one character.
    abstract static class Single extends Node {
        abstract boolean is(char c);

        @Override
        boolean match(Matcher m, int i) {
            return i < m.to && is(m.text.charAt(i)) && next.match(m, i + 1);
        }
    }

    static final class Char extends Single {
        final char c;

        Char(char c) {
            this.c = c;
        }

        @Override
        boolean is(char ch) {
            return ch == c;
        }
    }

    static final class CharIgnoringCase extends Single {
        final char c;
        final boolean unicode;

        CharIgnoringCase(char c, boolean unicode) {
            this.c = c;
            this.unicode = unicode;
        }

        @Override
        boolean is(char ch) {
            return sameIgnoringCase(ch, c, unicode);
        }
    }

    static final class Any extends Single {
        final boolean all;
        final boolean unix;

        Any(boolean all, boolean unix) {
            this.all = all;
            this.unix = unix;
        }

        @Override
        boolean is(char c) {
            return all || !lineTerminator(c, unix);
        }
    }

    static final class InClass extends Single {
        final CharSet set;

        InClass(CharSet set) {
            this.set = set;
        }

        @Override
        boolean is(char c) {
            return set.has(c);
        }
    }

    /// A set of characters: ranges, the predicates a device answers, other
    /// sets united with it, and one it is intersected with.
    static final class CharSet {
        static final int LETTER = 1;
        static final int UPPER = 2;
        static final int LOWER = 4;
        static final int DIGIT = 8;
        static final int WHITESPACE = 16;
        static final int LETTER_OR_DIGIT = 32;

        private char[] ranges = new char[8];
        private int count;
        private int kinds;
        private final List<CharSet> united = new ArrayList<CharSet>();
        CharSet and;
        boolean negate;
        /// 0 for exact, 1 to ignore the case of ASCII letters, 2 of any.
        final int ignoreCase;

        CharSet(int ignoreCase) {
            this.ignoreCase = ignoreCase;
        }

        CharSet add(int lo, int hi) {
            if (count + 2 > ranges.length) {
                char[] grown = new char[ranges.length * 2];
                System.arraycopy(ranges, 0, grown, 0, count);
                ranges = grown;
            }
            ranges[count++] = (char) lo;
            ranges[count++] = (char) hi;
            return this;
        }

        CharSet add(int c) {
            return add(c, c);
        }

        CharSet kind(int kind) {
            kinds |= kind;
            return this;
        }

        CharSet unite(CharSet other) {
            united.add(other);
            return this;
        }

        CharSet not() {
            CharSet out = new CharSet(0);
            out.united.add(this);
            out.negate = true;
            return out;
        }

        boolean isEmpty() {
            return count == 0 && kinds == 0 && united.isEmpty();
        }

        private boolean inRanges(char c) {
            for (int k = 0; k < count; k += 2) {
                if (c >= ranges[k] && c <= ranges[k + 1]) {
                    return true;
                }
            }
            return false;
        }

        private boolean inKinds(char c) {
            if (kinds == 0) {
                return false;
            }
            return (kinds & LETTER) != 0 && JdkStrings.isLetter(c)
                    || (kinds & UPPER) != 0 && Character.isUpperCase(c)
                    || (kinds & LOWER) != 0 && Character.isLowerCase(c)
                    || (kinds & DIGIT) != 0 && Character.isDigit(c)
                    || (kinds & WHITESPACE) != 0 && Character.isWhitespace(c)
                    || (kinds & LETTER_OR_DIGIT) != 0 && JdkStrings.isLetterOrDigit(c);
        }

        boolean has(char c) {
            boolean in = inRanges(c) || inKinds(c);
            if (!in && ignoreCase != 0 && count > 0) {
                if (c < 0x80) {
                    in = asciiLetter(c) && inRanges((char) (c ^ 0x20));
                } else if (ignoreCase == 2) {
                    in = inRanges(Character.toUpperCase(c)) || inRanges(Character.toLowerCase(c));
                }
            }
            for (int k = 0; !in && k < united.size(); k++) {
                in = united.get(k).has(c);
            }
            if (in && and != null) {
                in = and.has(c);
            }
            return in != negate;
        }
    }

    /// One character, repeated. Counted in a loop rather than by recursion,
    /// so `.*` over a long text costs no stack.
    static final class SingleLoop extends Node {
        final Single atom;
        final int min;
        final int max;
        final int mode;

        SingleLoop(Single atom, int min, int max, int mode) {
            this.atom = atom;
            this.min = min;
            this.max = max < 0 ? Integer.MAX_VALUE : max;
            this.mode = mode;
        }

        @Override
        boolean match(Matcher m, int i) {
            CharSequence text = m.text;
            int to = m.to;
            int n = 0;
            int j = i;
            if (mode == Parser.RELUCTANT) {
                while (n < min) {
                    if (j >= to || !atom.is(text.charAt(j))) {
                        return false;
                    }
                    j++;
                    n++;
                }
                while (true) {
                    if (next.match(m, j)) {
                        return true;
                    }
                    if (n >= max || j >= to || !atom.is(text.charAt(j))) {
                        return false;
                    }
                    j++;
                    n++;
                }
            }
            while (n < max && j < to && atom.is(text.charAt(j))) {
                j++;
                n++;
            }
            if (n < min) {
                return false;
            }
            if (mode == Parser.POSSESSIVE) {
                return next.match(m, j);
            }
            while (true) {
                if (next.match(m, j)) {
                    return true;
                }
                if (n == min) {
                    return false;
                }
                n--;
                j--;
            }
        }
    }

    /// Anything else, repeated. The body ends in a [LoopTail] that comes
    /// back here to decide between another round and the rest.
    static final class Loop extends Node {
        Node body;
        final int id;
        final int min;
        final int max;
        final boolean reluctant;

        Loop(int id, int min, int max, boolean reluctant) {
            this.id = id;
            this.min = min;
            this.max = max < 0 ? Integer.MAX_VALUE : max;
            this.reluctant = reluctant;
        }

        @Override
        boolean match(Matcher m, int i) {
            int count = m.loopCount[id];
            int position = m.loopStart[id];
            m.loopCount[id] = 0;
            m.loopStart[id] = -1;
            boolean matched = step(m, i);
            m.loopCount[id] = count;
            m.loopStart[id] = position;
            return matched;
        }

        boolean step(Matcher m, int i) {
            int count = m.loopCount[id];
            if (count < min) {
                return round(m, i, count);
            }
            if (reluctant) {
                return next.match(m, i) || count < max && round(m, i, count);
            }
            return count < max && round(m, i, count) || next.match(m, i);
        }

        private boolean round(Matcher m, int i, int count) {
            int position = m.loopStart[id];
            if (count >= min && i == position) {
                // The round before this one took nothing; another would
                // take nothing for ever.
                return false;
            }
            m.loopCount[id] = count + 1;
            m.loopStart[id] = i;
            boolean matched = body.match(m, i);
            m.loopCount[id] = count;
            m.loopStart[id] = position;
            return matched;
        }
    }

    static final class LoopTail extends Node {
        final Loop loop;

        LoopTail(Loop loop) {
            this.loop = loop;
        }

        @Override
        boolean match(Matcher m, int i) {
            return loop.step(m, i);
        }
    }

    static final class Branch extends Node {
        final Node[] alternatives;

        Branch(Node[] alternatives) {
            this.alternatives = alternatives;
        }

        @Override
        boolean match(Matcher m, int i) {
            for (Node alternative : alternatives) {
                if (alternative.match(m, i)) {
                    return true;
                }
            }
            return false;
        }
    }

    static final class GroupOpen extends Node {
        final int group;

        GroupOpen(int group) {
            this.group = group;
        }

        @Override
        boolean match(Matcher m, int i) {
            int saved = m.opened[group];
            m.opened[group] = i;
            if (next.match(m, i)) {
                return true;
            }
            m.opened[group] = saved;
            return false;
        }
    }

    static final class GroupClose extends Node {
        final int group;

        GroupClose(int group) {
            this.group = group;
        }

        @Override
        boolean match(Matcher m, int i) {
            int start = m.starts[group];
            int end = m.ends[group];
            m.starts[group] = m.opened[group];
            m.ends[group] = i;
            if (next.match(m, i)) {
                return true;
            }
            m.starts[group] = start;
            m.ends[group] = end;
            return false;
        }
    }

    static final class BackReference extends Node {
        final int group;
        final boolean ignoreCase;
        final boolean unicode;

        BackReference(int group, boolean ignoreCase, boolean unicode) {
            this.group = group;
            this.ignoreCase = ignoreCase;
            this.unicode = unicode;
        }

        @Override
        boolean match(Matcher m, int i) {
            if (group >= m.starts.length) {
                // A reference to a group the pattern never defines.
                return false;
            }
            int start = m.starts[group];
            int end = m.ends[group];
            if (start < 0) {
                return false;
            }
            int length = end - start;
            if (i + length > m.to) {
                return false;
            }
            for (int k = 0; k < length; k++) {
                char a = m.text.charAt(start + k);
                char b = m.text.charAt(i + k);
                if (a != b && !(ignoreCase && sameIgnoringCase(a, b, unicode))) {
                    return false;
                }
            }
            return next.match(m, i + length);
        }
    }

    static final class Anchor extends Node {
        static final int BEGIN = 0;
        static final int LINE_START = 1;
        static final int LINE_END = 2;
        static final int END_OR_FINAL_TERMINATOR = 3;
        static final int END = 4;
        static final int LAST_MATCH = 5;
        static final int WORD = 6;
        static final int NOT_WORD = 7;

        final int kind;
        final boolean unix;

        Anchor(int kind, boolean unix) {
            this.kind = kind;
            this.unix = unix;
        }

        private boolean lineEnd(Matcher m, int i, boolean multiline) {
            CharSequence text = m.text;
            int end = m.to;
            if (unix) {
                if (i < end) {
                    if (text.charAt(i) != '\n') {
                        return false;
                    }
                    return multiline || i == end - 1;
                }
                return true;
            }
            if (!multiline) {
                if (i < end - 2) {
                    return false;
                }
                if (i == end - 2 && (text.charAt(i) != '\r' || text.charAt(i + 1) != '\n')) {
                    return false;
                }
            }
            if (i < end) {
                char c = text.charAt(i);
                if (c == '\n') {
                    // Never between the two characters of one line end.
                    return !(i > 0 && text.charAt(i - 1) == '\r');
                }
                return c == '\r' || c == 0x85 || c == 0x2028 || c == 0x2029;
            }
            return true;
        }

        private boolean lineStart(Matcher m, int i) {
            if (i == m.to) {
                // Not at the end of the input, even after a line end.
                return false;
            }
            if (i > m.from) {
                char c = m.text.charAt(i - 1);
                if (!lineTerminator(c, unix)) {
                    return false;
                }
                if (c == '\r' && m.text.charAt(i) == '\n') {
                    return false;
                }
            }
            return true;
        }

        @Override
        boolean match(Matcher m, int i) {
            boolean here;
            switch (kind) {
                case BEGIN:
                    here = i == m.from;
                    break;
                case LINE_START:
                    here = lineStart(m, i);
                    break;
                case LINE_END:
                    here = lineEnd(m, i, true);
                    break;
                case END_OR_FINAL_TERMINATOR:
                    here = lineEnd(m, i, false);
                    break;
                case END:
                    here = i == m.to;
                    break;
                case LAST_MATCH:
                    here = i == m.oldLast;
                    break;
                default:
                    boolean left = i > m.from && wordChar(m.text.charAt(i - 1));
                    boolean right = i < m.to && wordChar(m.text.charAt(i));
                    here = (left != right) == (kind == WORD);
                    break;
            }
            return here && next.match(m, i);
        }
    }

    /// `(?>X)`: what `X` took first is kept, whatever follows.
    static final class Atomic extends Node {
        final Node body;

        Atomic(Node body) {
            this.body = body;
        }

        @Override
        boolean match(Matcher m, int i) {
            int[] groups = m.saveGroups();
            int endAt = m.endAt;
            m.endAt = -1;
            boolean matched = body.match(m, i);
            m.endAt = endAt;
            if (!matched) {
                return false;
            }
            if (next.match(m, m.subEnd)) {
                return true;
            }
            m.restoreGroups(groups);
            return false;
        }
    }

    static final class Look extends Node {
        final Node body;
        final boolean behind;
        final boolean negative;
        final int min;
        final int max;

        Look(Node body, boolean behind, boolean negative, int min, int max) {
            this.body = body;
            this.behind = behind;
            this.negative = negative;
            this.min = min;
            this.max = max;
        }

        @Override
        boolean match(Matcher m, int i) {
            int[] groups = m.saveGroups();
            int endAt = m.endAt;
            boolean found = false;
            if (behind) {
                int lowest = Math.max(m.from, i - max);
                for (int j = i - min; !found && j >= lowest; j--) {
                    m.endAt = i;
                    found = body.match(m, j);
                }
            } else {
                m.endAt = -1;
                found = body.match(m, i);
            }
            m.endAt = endAt;
            if (found == negative) {
                if (found) {
                    m.restoreGroups(groups);
                }
                return false;
            }
            if (negative) {
                m.restoreGroups(groups);
            }
            if (next.match(m, i)) {
                return true;
            }
            m.restoreGroups(groups);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // The parser
    // ------------------------------------------------------------------

    /// A piece of the chain being built: where it starts, the node whose
    /// `next` is still open, and how many characters it can take.
    private static final class Piece {
        Node first;
        Node last;
        int min;
        /// -1 for no upper bound.
        int max;
        /// Whether there is one way only for this to match at a position.
        boolean deterministic = true;

        Piece(Node first, Node last, int min, int max) {
            this.first = first;
            this.last = last;
            this.min = min;
            this.max = max;
        }

        static Piece of(Node node, int length) {
            return new Piece(node, node, length, length);
        }

        static Piece empty() {
            return of(new Pass(), 0);
        }

        Piece unbounded() {
            max = -1;
            return this;
        }

        void then(Piece other) {
            last.next = other.first;
            last = other.last;
            deterministic &= other.deterministic;
            min = sum(min, other.min);
            max = max < 0 || other.max < 0 ? -1 : sum(max, other.max);
        }

        private static int sum(int a, int b) {
            long s = (long) a + b;
            return s > 1000000000L ? 1000000000 : (int) s;
        }
    }

    /// Pieces put one after another.
    private static final class Seq {
        private Piece all;

        void then(Piece next) {
            if (all == null) {
                all = next;
            } else {
                all.then(next);
            }
        }

        Piece piece() {
            return all == null ? Piece.empty() : all;
        }
    }

    private static final class Parser {
        static final int GREEDY = 0;
        static final int RELUCTANT = 1;
        static final int POSSESSIVE = 2;

        private final String p;
        private int at;
        private int flags;
        int groups;
        int loops;
        final Map<String, Integer> names = new HashMap<String, Integer>();

        Parser(String pattern, int flags) {
            this.p = pattern;
            this.flags = flags;
        }

        private PatternSyntaxException error(String description) {
            return new PatternSyntaxException(description, p, Math.min(at, p.length()));
        }

        private PatternSyntaxException unsupported(String construct) {
            return error(construct + " is not supported by the Codename One runtime");
        }

        private boolean has(int flag) {
            return (flags & flag) != 0;
        }

        Node compile() {
            Piece whole;
            if (has(LITERAL)) {
                Seq all = new Seq();
                for (at = 0; at < p.length(); at++) {
                    all.then(literal(p.charAt(at)));
                }
                whole = all.piece();
            } else {
                whole = alternatives();
                if (at < p.length()) {
                    throw error("Unmatched closing ')'");
                }
            }
            whole.last.next = new Accept();
            return whole.first;
        }

        private Piece literal(int c) {
            if (c > 0xffff) {
                // Two chars in the text, so two nodes here.
                int v = c - 0x10000;
                Piece pair = Piece.of(new Char((char) (0xd800 + (v >> 10))), 1);
                pair.then(Piece.of(new Char((char) (0xdc00 + (v & 0x3ff))), 1));
                return pair;
            }
            char ch = (char) c;
            if (has(CASE_INSENSITIVE)) {
                boolean unicode = has(UNICODE_CASE);
                boolean cased = ch < 0x80 ? asciiLetter(ch)
                        : unicode && (Character.toUpperCase(ch) != ch || Character.toLowerCase(ch) != ch);
                if (cased) {
                    return Piece.of(new CharIgnoringCase(ch, unicode), 1);
                }
            }
            return Piece.of(new Char(ch), 1);
        }

        private CharSet newSet() {
            return new CharSet(has(CASE_INSENSITIVE) ? has(UNICODE_CASE) ? 2 : 1 : 0);
        }

        private void skipComments() {
            if (!has(COMMENTS)) {
                return;
            }
            while (at < p.length()) {
                char c = p.charAt(at);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0b) {
                    at++;
                } else if (c == '#') {
                    while (at < p.length() && !lineTerminator(p.charAt(at), false)) {
                        at++;
                    }
                } else {
                    return;
                }
            }
        }

        /// `X|Y|Z`, up to a closing parenthesis or the end.
        private Piece alternatives() {
            List<Piece> all = new ArrayList<Piece>();
            all.add(sequence());
            while (at < p.length() && p.charAt(at) == '|') {
                at++;
                all.add(sequence());
            }
            if (all.size() == 1) {
                return all.get(0);
            }
            // Alternatives of one character each are one character class,
            // which repeats without recursion.
            CharSet merged = new CharSet(0);
            boolean mergeable = true;
            for (Piece piece : all) {
                Node n = piece.first;
                if (piece.first != piece.last) {
                    mergeable = false;
                } else if (n instanceof Char) {
                    merged.add(((Char) n).c);
                } else if (n instanceof InClass) {
                    merged.unite(((InClass) n).set);
                } else {
                    mergeable = false;
                }
            }
            if (mergeable) {
                return Piece.of(new InClass(merged), 1);
            }
            Node[] heads = new Node[all.size()];
            Pass join = new Pass();
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int k = 0; k < heads.length; k++) {
                Piece piece = all.get(k);
                heads[k] = piece.first;
                piece.last.next = join;
                min = Math.min(min, piece.min);
                max = max < 0 || piece.max < 0 ? -1 : Math.max(max, piece.max);
            }
            Piece branch = new Piece(new Branch(heads), join, min, max);
            branch.deterministic = false;
            return branch;
        }

        private Piece sequence() {
            Seq out = new Seq();
            while (true) {
                skipComments();
                if (at >= p.length()) {
                    return out.piece();
                }
                char c = p.charAt(at);
                if (c == '|' || c == ')') {
                    return out.piece();
                }
                if (c == '\\' && at + 1 < p.length() && p.charAt(at + 1) == 'Q') {
                    at += 2;
                    int end = p.indexOf("\\E", at);
                    if (end < 0) {
                        end = p.length();
                    }
                    // A quantifier after the quotation applies to its last
                    // character, so that one goes through the usual path.
                    for (; at < end - 1; at++) {
                        out.then(literal(p.charAt(at)));
                    }
                    if (at < end) {
                        Piece last = literal(p.charAt(at));
                        at = Math.min(end + 2, p.length());
                        out.then(quantified(last));
                    } else {
                        at = Math.min(end + 2, p.length());
                    }
                    continue;
                }
                if (c == '\\' && at + 1 < p.length() && p.charAt(at + 1) == 'E') {
                    at += 2;
                    continue;
                }
                out.then(quantified(atom()));
            }
        }

        private Piece atom() {
            char c = p.charAt(at);
            switch (c) {
                case '(':
                    return group();
                case '[':
                    at++;
                    return Piece.of(new InClass(characterClass()), 1);
                case '.':
                    at++;
                    return Piece.of(new Any(has(DOTALL), has(UNIX_LINES)), 1);
                case '^':
                    at++;
                    return Piece.of(new Anchor(has(MULTILINE) ? Anchor.LINE_START : Anchor.BEGIN, has(UNIX_LINES)), 0);
                case '$':
                    at++;
                    return Piece.of(new Anchor(has(MULTILINE) ? Anchor.LINE_END : Anchor.END_OR_FINAL_TERMINATOR,
                            has(UNIX_LINES)), 0);
                case '\\':
                    at++;
                    return escape();
                case '*':
                case '+':
                case '?':
                    throw error("Dangling meta character '" + c + "'");
                case '{':
                    throw error("Illegal repetition");
                default:
                    at++;
                    if (c >= 0xd800 && c <= 0xdbff && at < p.length() && p.charAt(at) >= 0xdc00
                            && p.charAt(at) <= 0xdfff) {
                        // A code point written out is one atom, so a
                        // quantifier after it repeats both halves.
                        Piece pair = Piece.of(new Char(c), 1);
                        pair.then(Piece.of(new Char(p.charAt(at++)), 1));
                        return pair;
                    }
                    return literal(c);
            }
        }

        private Piece quantified(Piece piece) {
            skipComments();
            if (at >= p.length()) {
                return piece;
            }
            int min;
            int max;
            char c = p.charAt(at);
            if (c == '*') {
                min = 0;
                max = -1;
                at++;
            } else if (c == '+') {
                min = 1;
                max = -1;
                at++;
            } else if (c == '?') {
                min = 0;
                max = 1;
                at++;
            } else if (c == '{') {
                at++;
                int start = at;
                min = number();
                if (at == start) {
                    throw error("Illegal repetition");
                }
                max = min;
                if (at < p.length() && p.charAt(at) == ',') {
                    at++;
                    if (at < p.length() && p.charAt(at) == '}') {
                        max = -1;
                    } else {
                        int from = at;
                        max = number();
                        if (at == from) {
                            throw error("Illegal repetition");
                        }
                        if (max < min) {
                            throw error("Illegal repetition range");
                        }
                    }
                }
                if (at >= p.length() || p.charAt(at) != '}') {
                    throw error("Unclosed counted closure");
                }
                at++;
            } else {
                return piece;
            }
            int mode = GREEDY;
            if (at < p.length() && p.charAt(at) == '?') {
                mode = RELUCTANT;
                at++;
            } else if (at < p.length() && p.charAt(at) == '+') {
                mode = POSSESSIVE;
                at++;
            }
            int lowest = (int) Math.min(1000000000L, (long) piece.min * min);
            int highest = max < 0 || piece.max < 0 ? -1 : (int) Math.min(1000000000L, (long) piece.max * max);
            if (max == 0) {
                highest = 0;
            }
            Piece out;
            if (min == 0 && piece.max == 0 && piece.deterministic && piece.first instanceof GroupOpen) {
                // A group that can only be empty, repeated from zero times:
                // the JDK takes no round of it, so the group stays unset.
                out = Piece.empty();
            } else if (piece.first == piece.last && piece.first instanceof Single) {
                out = new Piece(null, null, lowest, highest);
                out.first = new SingleLoop((Single) piece.first, min, max, mode);
                out.last = out.first;
            } else {
                Loop loop = new Loop(loops++, min, max, mode == RELUCTANT);
                loop.body = piece.first;
                piece.last.next = new LoopTail(loop);
                if (mode == POSSESSIVE) {
                    loop.next = new SubEnd();
                    Node atomic = new Atomic(loop);
                    out = new Piece(atomic, atomic, lowest, highest);
                } else {
                    out = new Piece(loop, loop, lowest, highest);
                }
            }
            out.deterministic = piece.deterministic && min == max;
            skipComments();
            if (at < p.length()) {
                char after = p.charAt(at);
                if (after == '*' || after == '+' || after == '?') {
                    throw error("Dangling meta character '" + after + "'");
                }
                if (after == '{') {
                    return quantified(out);
                }
            }
            return out;
        }

        private int number() {
            long n = 0;
            while (at < p.length() && p.charAt(at) >= '0' && p.charAt(at) <= '9') {
                n = n * 10 + (p.charAt(at) - '0');
                if (n > Integer.MAX_VALUE) {
                    throw error("Illegal repetition range");
                }
                at++;
            }
            return (int) n;
        }

        private Piece group() {
            at++;
            int saved = flags;
            Piece out;
            if (at < p.length() && p.charAt(at) == '?') {
                at++;
                if (at >= p.length()) {
                    throw error("Unknown inline modifier");
                }
                char c = p.charAt(at);
                if (c == ':') {
                    at++;
                    out = body();
                } else if (c == '=' || c == '!') {
                    at++;
                    out = look(false, c == '!');
                } else if (c == '>') {
                    at++;
                    Piece inner = body();
                    inner.last.next = new SubEnd();
                    Node atomic = new Atomic(inner.first);
                    out = new Piece(atomic, atomic, inner.min, inner.max);
                } else if (c == '<') {
                    at++;
                    if (at < p.length() && (p.charAt(at) == '=' || p.charAt(at) == '!')) {
                        boolean negative = p.charAt(at) == '!';
                        at++;
                        out = look(true, negative);
                    } else {
                        String name = groupName('>');
                        if (names.containsKey(name)) {
                            throw error("Named capturing group <" + name + "> is already defined");
                        }
                        int group = ++groups;
                        names.put(name, Integer.valueOf(group));
                        out = capture(group);
                    }
                } else {
                    // (?flags) for the rest of the enclosing group, or
                    // (?flags:X) for X alone.
                    inlineFlags();
                    if (at >= p.length()) {
                        throw error("Unknown inline modifier");
                    }
                    if (p.charAt(at) == ')') {
                        at++;
                        return Piece.empty();
                    }
                    if (p.charAt(at) != ':') {
                        throw error("Unknown inline modifier");
                    }
                    at++;
                    out = body();
                }
            } else {
                out = capture(++groups);
            }
            flags = saved;
            return out;
        }

        private Piece body() {
            Piece inner = alternatives();
            if (at >= p.length() || p.charAt(at) != ')') {
                throw error("Unclosed group");
            }
            at++;
            return inner;
        }

        private Piece capture(int group) {
            Piece out = Piece.of(new GroupOpen(group), 0);
            out.then(body());
            out.then(Piece.of(new GroupClose(group), 0));
            return out;
        }

        private Piece look(boolean behind, boolean negative) {
            Piece inner = body();
            if (behind && inner.max < 0) {
                throw error("Look-behind group does not have an obvious maximum length");
            }
            inner.last.next = new SubEnd();
            return Piece.of(new Look(inner.first, behind, negative, inner.min, inner.max), 0);
        }

        private String groupName(char close) {
            int start = at;
            while (at < p.length() && p.charAt(at) != close) {
                char c = p.charAt(at);
                boolean letter = asciiLetter(c);
                if (!(letter || at > start && c >= '0' && c <= '9')) {
                    throw error(at == start ? "capturing group name does not start with a Latin letter"
                            : "named capturing group is missing trailing '" + close + "'");
                }
                at++;
            }
            if (at >= p.length()) {
                throw error("named capturing group is missing trailing '" + close + "'");
            }
            if (at == start) {
                throw error("named capturing group has 0 length name");
            }
            String name = p.substring(start, at);
            at++;
            return name;
        }

        private void inlineFlags() {
            boolean on = true;
            while (at < p.length()) {
                char c = p.charAt(at);
                int flag;
                switch (c) {
                    case 'i':
                        flag = CASE_INSENSITIVE;
                        break;
                    case 'm':
                        flag = MULTILINE;
                        break;
                    case 's':
                        flag = DOTALL;
                        break;
                    case 'd':
                        flag = UNIX_LINES;
                        break;
                    case 'u':
                        flag = UNICODE_CASE;
                        break;
                    case 'x':
                        flag = COMMENTS;
                        break;
                    case 'c':
                        throw unsupported("The inline flag (?c), canonical equivalence,");
                    case 'U':
                        throw unsupported("The inline flag (?U), Unicode character classes,");
                    case '-':
                        if (!on) {
                            return;
                        }
                        on = false;
                        at++;
                        continue;
                    default:
                        return;
                }
                flags = on ? flags | flag : flags & ~flag;
                at++;
            }
        }

        private int hex(int digits) {
            if (at + digits > p.length()) {
                throw error("Illegal hexadecimal escape sequence");
            }
            int v = 0;
            for (int k = 0; k < digits; k++) {
                int d = Character.digit(p.charAt(at++), 16);
                if (d < 0) {
                    throw error("Illegal hexadecimal escape sequence");
                }
                v = v * 16 + d;
            }
            return v;
        }

        /// The character an escape names, with `at` after the letter that
        /// follows the backslash; -1 when the letter is not one of those.
        private int escapedChar(char e) {
            switch (e) {
                case 't':
                    return '\t';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 'f':
                    return '\f';
                case 'a':
                    return 7;
                case 'e':
                    return 0x1b;
                case '0': {
                    int v = 0;
                    int digits = 0;
                    while (at < p.length() && digits < 3 && p.charAt(at) >= '0' && p.charAt(at) <= '7') {
                        int d = p.charAt(at) - '0';
                        if (digits == 2 && v > 037) {
                            break;
                        }
                        v = v * 8 + d;
                        digits++;
                        at++;
                    }
                    if (digits == 0) {
                        throw error("Illegal octal escape sequence");
                    }
                    return v;
                }
                case 'x':
                    if (at < p.length() && p.charAt(at) == '{') {
                        at++;
                        int v = 0;
                        int start = at;
                        while (at < p.length() && p.charAt(at) != '}') {
                            int d = Character.digit(p.charAt(at), 16);
                            if (d < 0 || v > 0x10ffff) {
                                throw error("Illegal hexadecimal escape sequence");
                            }
                            v = v * 16 + d;
                            at++;
                        }
                        if (at >= p.length() || at == start || v > 0x10ffff) {
                            throw error("Illegal hexadecimal escape sequence");
                        }
                        at++;
                        return v;
                    }
                    return hex(2);
                case 'u':
                    if (at + 4 > p.length()) {
                        throw error("Illegal Unicode escape sequence");
                    }
                    for (int k = 0; k < 4; k++) {
                        if (Character.digit(p.charAt(at + k), 16) < 0) {
                            throw error("Illegal Unicode escape sequence");
                        }
                    }
                    return hex(4);
                case 'c':
                    if (at >= p.length()) {
                        throw error("Illegal control escape sequence");
                    }
                    return p.charAt(at++) ^ 64;
                default:
                    return -1;
            }
        }

        /// The class an escape names, or null.
        private CharSet escapedClass(char e) {
            switch (e) {
                case 'd':
                    return new CharSet(0).add('0', '9');
                case 'D':
                    return new CharSet(0).add('0', '9').not();
                case 'w':
                    return word();
                case 'W':
                    return word().not();
                case 's':
                    return new CharSet(0).add(' ').add('\t', '\r');
                case 'S':
                    return new CharSet(0).add(' ').add('\t', '\r').not();
                case 'h':
                    return horizontal();
                case 'H':
                    return horizontal().not();
                case 'v':
                    return vertical();
                case 'V':
                    return vertical().not();
                case 'p':
                case 'P': {
                    CharSet set = property();
                    return e == 'P' ? set.not() : set;
                }
                default:
                    return null;
            }
        }

        private static CharSet word() {
            return new CharSet(0).add('a', 'z').add('A', 'Z').add('0', '9').add('_');
        }

        private static CharSet horizontal() {
            return new CharSet(0).add(' ').add('\t').add(0xa0).add(0x1680).add(0x180e).add(0x2000, 0x200a)
                    .add(0x202f).add(0x205f).add(0x3000);
        }

        private static CharSet vertical() {
            return new CharSet(0).add('\n', '\r').add(0x85).add(0x2028, 0x2029);
        }

        private CharSet property() {
            String name;
            if (at < p.length() && p.charAt(at) == '{') {
                int close = p.indexOf('}', at);
                if (close < 0) {
                    throw error("Unclosed character family");
                }
                name = p.substring(at + 1, close);
                at = close + 1;
            } else if (at < p.length()) {
                name = String.valueOf(p.charAt(at++));
            } else {
                throw error("Illegal character family");
            }
            CharSet s = new CharSet(0);
            if ("Lower".equals(name)) {
                return s.add('a', 'z');
            }
            if ("Upper".equals(name)) {
                return s.add('A', 'Z');
            }
            if ("ASCII".equals(name)) {
                return s.add(0, 0x7f);
            }
            if ("Alpha".equals(name)) {
                return s.add('a', 'z').add('A', 'Z');
            }
            if ("Digit".equals(name)) {
                return s.add('0', '9');
            }
            if ("Alnum".equals(name)) {
                return s.add('a', 'z').add('A', 'Z').add('0', '9');
            }
            if ("Punct".equals(name)) {
                return s.add('!', '/').add(':', '@').add('[', '`').add('{', '~');
            }
            if ("Graph".equals(name)) {
                return s.add('!', '~');
            }
            if ("Print".equals(name)) {
                return s.add(' ', '~');
            }
            if ("Blank".equals(name)) {
                return s.add(' ').add('\t');
            }
            if ("Cntrl".equals(name)) {
                return s.add(0, 0x1f).add(0x7f);
            }
            if ("XDigit".equals(name)) {
                return s.add('0', '9').add('a', 'f').add('A', 'F');
            }
            if ("Space".equals(name)) {
                return s.add(' ').add('\t', '\r');
            }
            if ("javaLowerCase".equals(name) || "Ll".equals(name) || "IsLl".equals(name)
                    || "IsLowercase".equals(name)) {
                return s.kind(CharSet.LOWER);
            }
            if ("javaUpperCase".equals(name) || "Lu".equals(name) || "IsLu".equals(name)
                    || "IsUppercase".equals(name)) {
                return s.kind(CharSet.UPPER);
            }
            if ("javaWhitespace".equals(name) || "IsWhite_Space".equals(name) || "IsWhiteSpace".equals(name)) {
                return s.kind(CharSet.WHITESPACE);
            }
            if ("javaLetter".equals(name) || "L".equals(name) || "IsL".equals(name) || "IsLetter".equals(name)
                    || "IsAlphabetic".equals(name) || "javaAlphabetic".equals(name)) {
                return s.kind(CharSet.LETTER);
            }
            if ("javaLetterOrDigit".equals(name)) {
                return s.kind(CharSet.LETTER_OR_DIGIT);
            }
            if ("javaDigit".equals(name) || "N".equals(name) || "Nd".equals(name) || "IsN".equals(name)
                    || "IsNd".equals(name) || "IsDigit".equals(name)) {
                return s.kind(CharSet.DIGIT);
            }
            throw unsupported("The character property {" + name + "}");
        }

        private Piece escape() {
            if (at >= p.length()) {
                throw error("Unexpected internal error");
            }
            char e = p.charAt(at++);
            boolean unix = has(UNIX_LINES);
            switch (e) {
                case 'A':
                    return Piece.of(new Anchor(Anchor.BEGIN, unix), 0);
                case 'z':
                    return Piece.of(new Anchor(Anchor.END, unix), 0);
                case 'Z':
                    return Piece.of(new Anchor(Anchor.END_OR_FINAL_TERMINATOR, unix), 0);
                case 'G':
                    return Piece.of(new Anchor(Anchor.LAST_MATCH, unix), 0);
                case 'b':
                    if (at < p.length() && p.charAt(at) == '{') {
                        throw unsupported("The boundary \\b{...}");
                    }
                    return Piece.of(new Anchor(Anchor.WORD, unix), 0);
                case 'B':
                    return Piece.of(new Anchor(Anchor.NOT_WORD, unix), 0);
                case 'X':
                    throw unsupported("The grapheme cluster \\X");
                case 'N':
                    throw unsupported("The named character \\N{...}");
                case 'R': {
                    // \r\n as one, or any single line end; atomic, so the
                    // pair is never split to let what follows take the \n.
                    Piece crlf = Piece.of(new Char('\r'), 1);
                    crlf.then(Piece.of(new Char('\n'), 1));
                    Piece one = Piece.of(new InClass(vertical()), 1);
                    Pass join = new Pass();
                    crlf.last.next = join;
                    one.last.next = join;
                    join.next = new SubEnd();
                    Node atomic = new Atomic(new Branch(new Node[] {crlf.first, one.first}));
                    return new Piece(atomic, atomic, 1, 2);
                }
                case 'k': {
                    if (at >= p.length() || p.charAt(at) != '<') {
                        throw error("\\k is not followed by '<' for named capturing group");
                    }
                    at++;
                    String name = groupName('>');
                    Integer group = names.get(name);
                    if (group == null) {
                        throw error("named capturing group <" + name + "> does not exist");
                    }
                    return Piece.of(new BackReference(group.intValue(), has(CASE_INSENSITIVE), has(UNICODE_CASE)), 0)
                            .unbounded();
                }
                default:
                    break;
            }
            if (e >= '1' && e <= '9') {
                int group = e - '0';
                while (at < p.length() && p.charAt(at) >= '0' && p.charAt(at) <= '9') {
                    int more = group * 10 + (p.charAt(at) - '0');
                    if (more > groups) {
                        break;
                    }
                    group = more;
                    at++;
                }
                return Piece.of(new BackReference(group, has(CASE_INSENSITIVE), has(UNICODE_CASE)), 0).unbounded();
            }
            CharSet set = escapedClass(e);
            if (set != null) {
                return Piece.of(new InClass(set), 1);
            }
            int c = escapedChar(e);
            if (c >= 0) {
                return literal(c);
            }
            if (asciiLetter(e)) {
                at--;
                throw error("Illegal/unsupported escape sequence");
            }
            return literal(e);
        }

        /// A class, with `at` after its opening bracket; consumes the
        /// closing one.
        private CharSet characterClass() {
            boolean negate = false;
            if (at < p.length() && p.charAt(at) == '^') {
                negate = true;
                at++;
            }
            CharSet set = classBody();
            set.negate = negate;
            return set;
        }

        private CharSet classBody() {
            CharSet set = newSet();
            boolean first = true;
            while (true) {
                if (has(COMMENTS)) {
                    skipComments();
                }
                if (at >= p.length()) {
                    throw error("Unclosed character class");
                }
                char c = p.charAt(at);
                if (c == ']' && !first) {
                    at++;
                    return set;
                }
                first = false;
                if (c == '[') {
                    at++;
                    set.unite(characterClass());
                    continue;
                }
                if (c == '&' && at + 1 < p.length() && p.charAt(at + 1) == '&') {
                    at += 2;
                    if (at < p.length() && p.charAt(at) == ']') {
                        // Nothing to intersect with.
                        continue;
                    }
                    // Everything after && is the other operand, up to the
                    // bracket that closes this class.
                    CharSet right = classBody();
                    if (set.isEmpty()) {
                        return right;
                    }
                    set.and = right;
                    return set;
                }
                int lo;
                if (c == '\\') {
                    at++;
                    if (at >= p.length()) {
                        throw error("Unclosed character class");
                    }
                    char e = p.charAt(at++);
                    if (e == 'Q') {
                        int end = p.indexOf("\\E", at);
                        if (end < 0) {
                            throw error("Unclosed character class");
                        }
                        for (; at < end; at++) {
                            set.add(p.charAt(at));
                        }
                        at = end + 2;
                        continue;
                    }
                    if (e == 'E') {
                        continue;
                    }
                    CharSet named = escapedClass(e);
                    if (named != null) {
                        set.unite(named);
                        continue;
                    }
                    lo = escapedChar(e);
                    if (lo < 0) {
                        if (asciiLetter(e) || e >= '0' && e <= '9') {
                            at--;
                            throw error("Illegal/unsupported escape sequence");
                        }
                        lo = e;
                    }
                } else {
                    at++;
                    lo = c;
                }
                int hi = lo;
                if (at + 1 < p.length() && p.charAt(at) == '-' && p.charAt(at + 1) != ']'
                        && p.charAt(at + 1) != '[') {
                    at++;
                    char h = p.charAt(at++);
                    if (h == '\\') {
                        if (at >= p.length()) {
                            throw error("Unclosed character class");
                        }
                        char e = p.charAt(at++);
                        hi = escapedChar(e);
                        if (hi < 0) {
                            if (asciiLetter(e)) {
                                at--;
                                throw error("Illegal character range");
                            }
                            hi = e;
                        }
                    } else {
                        hi = h;
                    }
                    if (hi < lo) {
                        throw error("Illegal character range");
                    }
                }
                if (hi > 0xffff) {
                    throw unsupported("A character above U+FFFF in a character class");
                }
                set.add(lo, hi);
            }
        }
    }
}
