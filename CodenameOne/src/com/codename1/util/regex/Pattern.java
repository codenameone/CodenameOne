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
package com.codename1.util.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
///   classes, and the Unicode properties `L`, `Lu`, `Ll`, `N`/`Nd`,
///   `IsAlphabetic`, `IsLetter`, `IsDigit`, `IsUppercase`, `IsLowercase`,
///   `IsWhite_Space`.
/// - Unicode scripts (`\p{IsGreek}`, `\p{script=Latn}`), blocks
///   (`\p{InGreek}`, `\p{block=Arrows}`) and general categories (`\p{Sc}`,
///   `\p{IsPd}`, `\p{gc=Mn}`), from the Unicode 15.0 tables of
///   [UnicodeTables].
/// - The binary properties `IsDigit`, `IsPunctuation`, `IsTitlecase`, `IsControl`,
///   `IsAssigned`, `IsHex_Digit`, `IsJoin_Control`,
///   `IsNoncharacter_Code_Point`, `IsWord`, `IsAlnum`, `IsBlank`, `IsGraph`
///   and `IsPrint`, in any case.
/// - `UNICODE_CHARACTER_CLASS` (`(?U)`): `\d \w \s \b` and the POSIX
///   classes take their Unicode definitions, as on the JDK.
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
/// - Other binary properties (`\p{IsEmoji}`, `\p{IsIdeographic}`, ...).
/// - `\X`, `\N{name}`, `\b{g}`.
/// - `CANON_EQ`.
///
/// #### Differences
///
/// Text is matched one code point at a time, as on the JDK: `.` and a
/// character class take a whole surrogate pair. The Unicode properties and
/// categories answer from the tables, for all of Unicode; `javaLetter` and
/// its kin answer this class's own letter test, the scripts a device font is
/// likely to have. A group repeated many thousand times
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

    private static final int ALL_FLAGS = UNIX_LINES | CASE_INSENSITIVE | COMMENTS | MULTILINE | LITERAL
            | DOTALL | UNICODE_CASE | CANON_EQ | UNICODE_CHARACTER_CLASS;

    private final String regex;
    private final int flags;
    final Node root;
    final int groupCount;
    final int loopCount;
    final Map<String, Integer> names;
    /// Whether the pattern names a supplementary code point or a surrogate.
    /// Only then does a search skip the positions inside a surrogate pair,
    /// as the JDK's does: otherwise `x*` finds an empty match between the
    /// halves there too.
    final boolean supplementary;

    private Pattern(String regex, int flags) {
        if (regex == null) {
            throw new NullPointerException();
        }
        // As in the JDK: a bit no flag constant names is a caller's mistake,
        // not something to ignore.
        if ((flags & ~ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("Unknown flag 0x" + Integer.toHexString(flags & ~ALL_FLAGS));
        }
        if ((flags & CANON_EQ) != 0) {
            throw new UnsupportedOperationException("Pattern.CANON_EQ is not supported by the Codename One runtime");
        }
        this.regex = regex;
        Parser parser = new Parser(regex, flags);
        this.root = parser.compile();
        this.supplementary = parser.supplementary;
        // As in the JDK, flags() reports the top-level state the parse ended
        // in: (?i) outside every group counts, (?i) inside one does not.
        this.flags = parser.topLevelFlags();
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

    /// The pieces [#split(CharSequence)] would answer, found one at a time
    /// as the stream asks for them, as the JDK's are: the search starts on
    /// the first request, so a `limit` stops it and a change to a mutable
    /// input before then is seen.
    public Stream<String> splitAsStream(final CharSequence input) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Pieces(this, input),
                Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /// The pieces of one [#splitAsStream(CharSequence)]. Empty pieces are
    /// held back until a non-empty one follows them, because trailing ones
    /// are dropped, as `split` drops them.
    private static final class Pieces implements Iterator<String> {
        private final Pattern pattern;
        private final CharSequence input;
        private Matcher matcher;
        private int current;
        private String pending;
        private int empties;
        private String next;

        Pieces(Pattern pattern, CharSequence input) {
            this.pattern = pattern;
            this.input = input;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = advance();
            }
            return next != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String out = next;
            next = null;
            return out;
        }

        private String advance() {
            if (matcher == null) {
                matcher = pattern.matcher(input);
                // An empty input is one empty piece, as split answers.
                if (input.length() == 0) {
                    empties = 1;
                }
            }
            while (true) {
                if (empties > 0) {
                    empties--;
                    return "";
                }
                if (pending != null) {
                    String out = pending;
                    pending = null;
                    return out;
                }
                if (current == input.length()) {
                    return null;
                }
                while (pending == null && matcher.find()) {
                    String piece = input.subSequence(current, matcher.start()).toString();
                    current = matcher.end();
                    if (piece.length() > 0) {
                        pending = piece;
                    } else if (current > 0) {
                        // No empty leading piece for a zero-width match at the start.
                        empties++;
                    }
                }
                if (pending == null) {
                    String last = input.subSequence(current, input.length()).toString();
                    current = input.length();
                    if (last.length() == 0) {
                        // Only trailing empty pieces were owed: dropped.
                        empties = 0;
                        return null;
                    }
                    pending = last;
                }
            }
        }
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

    /// The code point at `i`: a surrogate pair when both halves lie before
    /// `to`, otherwise the single char.
    static int codePointAt(CharSequence text, int i, int to) {
        char c = text.charAt(i);
        if (c >= 0xd800 && c <= 0xdbff && i + 1 < to) {
            char d = text.charAt(i + 1);
            if (d >= 0xdc00 && d <= 0xdfff) {
                return ((c - 0xd800) << 10) + (d - 0xdc00) + 0x10000;
            }
        }
        return c;
    }

    /// The code point that ends just before `i`, no further back than `from`.
    static int codePointBefore(CharSequence text, int i, int from) {
        char d = text.charAt(i - 1);
        if (d >= 0xdc00 && d <= 0xdfff && i - 2 >= from) {
            char c = text.charAt(i - 2);
            if (c >= 0xd800 && c <= 0xdbff) {
                return ((c - 0xd800) << 10) + (d - 0xdc00) + 0x10000;
            }
        }
        return d;
    }

    /// A word character under `UNICODE_CHARACTER_CLASS`, as the JDK defines
    /// it: alphabetic, a mark, a decimal digit, a connector, or a joiner.
    static boolean unicodeWordChar(int c) {
        if (UnicodeTables.alphabetic(c)) {
            return true;
        }
        int type = UnicodeTables.category(c);
        return type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                || type == Character.COMBINING_SPACING_MARK || type == Character.DECIMAL_DIGIT_NUMBER
                || type == Character.CONNECTOR_PUNCTUATION || c == 0x200c || c == 0x200d;
    }

    /// `White_Space` under `UNICODE_CHARACTER_CLASS`.
    static boolean unicodeWhiteSpace(int c) {
        int type = UnicodeTables.category(c);
        return type == Character.SPACE_SEPARATOR || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR || c >= 0x9 && c <= 0xd || c == 0x85;
    }

    static boolean lineTerminator(int c, boolean unix) {
        if (c == '\n') {
            return true;
        }
        return !unix && (c == '\r' || c == 0x85 || c == 0x2028 || c == 0x2029);
    }

    static boolean asciiLetter(int c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    /// A word character for `\b` and `\B`: the ASCII `[a-zA-Z0-9_]` that
    /// `\w` matches. The JDK has defined the boundary this way since 19
    /// (JDK-8264160); before, it took any Unicode letter or digit, so `\b`
    /// fell inside a word `\w+` could not match. The Unicode reading comes
    /// only with `UNICODE_CHARACTER_CLASS`, which this runtime refuses.
    static boolean wordChar(int c) {
        return c == '_' || asciiLetter(c) || c >= '0' && c <= '9';
    }

    static boolean isLetterOrDigit(int c) {
        return isLetter(c) || Character.isDigit(c);
    }

    /// Whether `c` is a letter: the scripts a device font is likely to
    /// have rather than all of Unicode. Written out here because
    /// `Character.isLetter` is not available on every device runtime.
    static boolean isLetter(int c) {
        if (c > 0xffff) {
            return Character.isLetter(c);
        }
        if (c < 0x80) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }
        if (c < 0x100) {
            return c == 0xaa || c == 0xb5 || c == 0xba || c >= 0xc0 && c != 0xd7 && c != 0xf7;
        }
        return c <= 0x24f // Latin Extended
                || c >= 0x370 && c <= 0x3ff && c != 0x37e && c != 0x387 // Greek
                || c >= 0x400 && c <= 0x481 || c >= 0x48a && c <= 0x52f // Cyrillic
                || c >= 0x531 && c <= 0x556 || c >= 0x561 && c <= 0x587 // Armenian
                || c >= 0x5d0 && c <= 0x5ea // Hebrew
                || c >= 0x620 && c <= 0x64a || c >= 0x671 && c <= 0x6d3 // Arabic
                || c >= 0x904 && c <= 0x939 // Devanagari
                || c >= 0xe01 && c <= 0xe30 // Thai
                || c >= 0x10a0 && c <= 0x10ff && c != 0x10fb // Georgian
                || c >= 0x1e00 && c <= 0x1fff // Latin and Greek Extended Additional
                || c >= 0x3041 && c <= 0x3096 || c >= 0x30a1 && c <= 0x30fa // Kana
                || c >= 0x3400 && c <= 0x4dbf || c >= 0x4e00 && c <= 0x9fff // CJK
                || c >= 0xac00 && c <= 0xd7a3; // Hangul
    }

    /// Whether `a` and `b` are the same character when case is ignored:
    /// for ASCII letters only, or by the device's case tables.
    static boolean sameIgnoringCase(int a, int b, boolean unicode) {
        if (a == b) {
            return true;
        }
        if (a < 0x80 && b < 0x80) {
            return asciiLetter(a) && (a ^ 0x20) == b;
        }
        if (!unicode) {
            return false;
        }
        int ua = Character.toUpperCase(a);
        int ub = Character.toUpperCase(b);
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

    /// A node that takes exactly one code point: one char, or both halves of
    /// a surrogate pair.
    abstract static class Single extends Node {
        abstract boolean is(int c);

        /// The fewest and most chars this can take: 1 and 2 unless it names
        /// one code point.
        int minChars() {
            return 1;
        }

        int maxChars() {
            return 2;
        }

        @Override
        boolean match(Matcher m, int i) {
            if (i >= m.to) {
                return false;
            }
            int c = codePointAt(m.text, i, m.to);
            return is(c) && next.match(m, i + (c > 0xffff ? 2 : 1));
        }
    }

    static final class Char extends Single {
        final int c;

        Char(int c) {
            this.c = c;
        }

        @Override
        int minChars() {
            return c > 0xffff ? 2 : 1;
        }

        @Override
        int maxChars() {
            return minChars();
        }

        @Override
        boolean is(int ch) {
            return ch == c;
        }
    }

    static final class CharIgnoringCase extends Single {
        final int c;
        final boolean unicode;

        CharIgnoringCase(int c, boolean unicode) {
            this.c = c;
            this.unicode = unicode;
        }

        @Override
        boolean is(int ch) {
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
        boolean is(int c) {
            return all || !lineTerminator(c, unix);
        }
    }

    static final class InClass extends Single {
        final CharSet set;

        InClass(CharSet set) {
            this.set = set;
        }

        @Override
        boolean is(int c) {
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
        static final int TITLE = 64;
        // UNICODE_CHARACTER_CLASS, all answered from UnicodeTables.
        static final int U_ALPHABETIC = 128;
        static final int U_LOWER = 256;
        static final int U_UPPER = 512;
        static final int U_WORD = 1024;
        static final int U_WHITE_SPACE = 2048;
        static final int U_HEX_DIGIT = 4096;
        static final int U_GRAPH = 8192;
        static final int U_PRINT = 16384;
        static final int U_BLANK = 32768;

        private int[] ranges = new int[8];
        /// General categories, one bit for each `Character.getType` value.
        int categories;
        /// A script index into [UnicodeTables], or -1.
        int script = -1;
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
                int[] grown = new int[ranges.length * 2];
                System.arraycopy(ranges, 0, grown, 0, count);
                ranges = grown;
            }
            ranges[count++] = lo;
            ranges[count++] = hi;
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
            return count == 0 && kinds == 0 && categories == 0 && script < 0 && united.isEmpty();
        }

        private boolean inRanges(int c) {
            for (int k = 0; k < count; k += 2) {
                if (c >= ranges[k] && c <= ranges[k + 1]) {
                    return true;
                }
            }
            return false;
        }

        private boolean inKinds(int c) {
            if (categories != 0 && (categories & (1 << UnicodeTables.category(c))) != 0) {
                return true;
            }
            if (script >= 0 && UnicodeTables.scriptOf(c) == script) {
                return true;
            }
            if (kinds == 0) {
                return false;
            }
            if (kinds >= U_ALPHABETIC && inUnicodeKinds(c)) {
                return true;
            }
            return (kinds & LETTER) != 0 && isLetter(c)
                    || (kinds & UPPER) != 0 && Character.isUpperCase(c)
                    || (kinds & LOWER) != 0 && Character.isLowerCase(c)
                    || (kinds & DIGIT) != 0 && Character.isDigit(c)
                    || (kinds & WHITESPACE) != 0 && Character.isWhitespace(c)
                    || (kinds & LETTER_OR_DIGIT) != 0 && isLetterOrDigit(c)
                    || (kinds & TITLE) != 0 && Character.isTitleCase(c);
        }

        /// The `UNICODE_CHARACTER_CLASS` predicates, as the JDK's
        /// `CharPredicates` define them.
        private boolean inUnicodeKinds(int c) {
            int type = UnicodeTables.category(c);
            boolean digit = type == Character.DECIMAL_DIGIT_NUMBER;
            boolean graph = type != Character.SPACE_SEPARATOR && type != Character.LINE_SEPARATOR
                    && type != Character.PARAGRAPH_SEPARATOR && type != Character.CONTROL
                    && type != Character.SURROGATE && type != Character.UNASSIGNED;
            boolean blank = type == Character.SPACE_SEPARATOR || c == '\t';
            return (kinds & U_ALPHABETIC) != 0 && UnicodeTables.alphabetic(c)
                    || (kinds & U_LOWER) != 0 && UnicodeTables.lowercase(c)
                    || (kinds & U_UPPER) != 0 && UnicodeTables.uppercase(c)
                    || (kinds & U_WORD) != 0 && unicodeWordChar(c)
                    || (kinds & U_WHITE_SPACE) != 0 && unicodeWhiteSpace(c)
                    || (kinds & U_HEX_DIGIT) != 0 && (digit || c >= '0' && c <= '9' || c >= 'a' && c <= 'f'
                            || c >= 'A' && c <= 'F' || c >= 0xff10 && c <= 0xff19 || c >= 0xff21 && c <= 0xff26
                            || c >= 0xff41 && c <= 0xff46)
                    || (kinds & U_GRAPH) != 0 && graph
                    || (kinds & U_PRINT) != 0 && (graph || blank) && type != Character.CONTROL
                    || (kinds & U_BLANK) != 0 && blank;
        }

        boolean has(int c) {
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
                    if (j >= to) {
                        return false;
                    }
                    int c = codePointAt(text, j, to);
                    if (!atom.is(c)) {
                        return false;
                    }
                    j += c > 0xffff ? 2 : 1;
                    n++;
                }
                while (true) {
                    if (next.match(m, j)) {
                        return true;
                    }
                    if (n >= max || j >= to) {
                        return false;
                    }
                    int c = codePointAt(text, j, to);
                    if (!atom.is(c)) {
                        return false;
                    }
                    j += c > 0xffff ? 2 : 1;
                    n++;
                }
            }
            while (n < max && j < to) {
                int c = codePointAt(text, j, to);
                if (!atom.is(c)) {
                    break;
                }
                j += c > 0xffff ? 2 : 1;
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
                // Back over one code point. Going forward from i paired every
                // pair it met, so a pair ending here was taken whole.
                j -= codePointBefore(text, j, i) > 0xffff ? 2 : 1;
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
        /// `\b` and `\B` under `UNICODE_CHARACTER_CLASS`: Unicode words.
        boolean unicode;

        Anchor(int kind, boolean unix) {
            this.kind = kind;
            this.unix = unix;
        }

        private boolean word(int c) {
            return unicode ? unicodeWordChar(c) : wordChar(c);
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
                    boolean left = i > m.from && word(codePointBefore(m.text, i, m.from));
                    boolean right = i < m.to && word(codePointAt(m.text, i, m.to));
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
        boolean supplementary;

        Parser(String pattern, int flags) {
            this.p = pattern;
            this.flags = flags;
            for (int k = 0; k < pattern.length() && !supplementary; k++) {
                char c = pattern.charAt(k);
                supplementary = c >= 0xd800 && c <= 0xdfff;
            }
        }

        /// Records a code point the pattern names, and returns it.
        private int note(int c) {
            if (c > 0xffff || c >= 0xd800 && c <= 0xdfff) {
                supplementary = true;
            }
            return c;
        }

        private PatternSyntaxException error(String description) {
            return new PatternSyntaxException(description, p, Math.min(at, p.length()));
        }

        private PatternSyntaxException unsupported(String construct) {
            return error(construct + " is not supported by the Codename One runtime");
        }

        private boolean has(int flag) {
            // UNICODE_CHARACTER_CLASS implies UNICODE_CASE, as on the JDK.
            if (flag == UNICODE_CASE && (flags & UNICODE_CHARACTER_CLASS) != 0) {
                return true;
            }
            return (flags & flag) != 0;
        }

        /// A piece for one code point, as wide in chars as it can be.
        private static Piece single(Single s) {
            return new Piece(s, s, s.minChars(), s.maxChars());
        }

        /// The flags in force after the whole pattern, which a group's own
        /// inline flags never reach because the group restores them.
        int topLevelFlags() {
            return flags;
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
            note(c);
            if (has(CASE_INSENSITIVE)) {
                boolean unicode = has(UNICODE_CASE);
                boolean cased = c < 0x80 ? asciiLetter(c)
                        : unicode && (Character.toUpperCase(c) != c || Character.toLowerCase(c) != c);
                if (cased) {
                    return single(new CharIgnoringCase(c, unicode));
                }
            }
            return single(new Char(c));
        }

        /// The code point at `at` in the pattern, consuming it.
        private int patternCodePoint() {
            int c = codePointAt(p, at, p.length());
            at += c > 0xffff ? 2 : 1;
            return c;
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
                if (piece.first != piece.last) { //NOPMD CompareObjectsWithEquals - one node, by identity
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
                return single(new InClass(merged));
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
                    // code point, so that one goes through the usual path.
                    if (at >= end) {
                        at = Math.min(end + 2, p.length());
                    }
                    while (at < end) {
                        int cp = codePointAt(p, at, end);
                        at += cp > 0xffff ? 2 : 1;
                        if (at < end) {
                            out.then(literal(cp));
                        } else {
                            Piece last = literal(cp);
                            at = Math.min(end + 2, p.length());
                            out.then(quantified(last));
                            break;
                        }
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
                    return single(new InClass(characterClass()));
                case '.':
                    at++;
                    return single(new Any(has(DOTALL), has(UNIX_LINES)));
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
                    // A code point written out is one atom, so a quantifier
                    // after it repeats both halves.
                    return literal(patternCodePoint());
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
            if (min == 0 && max != 1 && piece.max == 0 && piece.deterministic && piece.first instanceof GroupOpen) {
                // A group that can only be empty, repeated from zero times:
                // the JDK takes no round of it, so the group stays unset. Not
                // for at most one round (? and {0,1}): there the JDK tries its
                // round, and "(^)?" sets the group to the empty string.
                out = Piece.empty();
            } else if (piece.first == piece.last && piece.first instanceof Single) { //NOPMD CompareObjectsWithEquals
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
                        flag = UNICODE_CHARACTER_CLASS;
                        break;
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
                        if (digits == 2 && v > 0x1f) {
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
                    int unit = hex(4);
                    // Two escapes that spell a surrogate pair are one code
                    // point, as on the JDK.
                    if (unit >= 0xd800 && unit <= 0xdbff && at + 6 <= p.length() && p.charAt(at) == '\\'
                            && p.charAt(at + 1) == 'u') {
                        int low = 0;
                        for (int k = 2; k < 6 && low >= 0; k++) {
                            int d = Character.digit(p.charAt(at + k), 16);
                            low = d < 0 ? -1 : low * 16 + d;
                        }
                        if (low >= 0xdc00 && low <= 0xdfff) {
                            at += 6;
                            return ((unit - 0xd800) << 10) + (low - 0xdc00) + 0x10000;
                        }
                    }
                    return unit;
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
            if (has(UNICODE_CHARACTER_CLASS)) {
                switch (e) {
                    case 'd':
                        return categories(1 << Character.DECIMAL_DIGIT_NUMBER);
                    case 'D':
                        return categories(1 << Character.DECIMAL_DIGIT_NUMBER).not();
                    case 'w':
                        return new CharSet(0).kind(CharSet.U_WORD);
                    case 'W':
                        return new CharSet(0).kind(CharSet.U_WORD).not();
                    case 's':
                        return new CharSet(0).kind(CharSet.U_WHITE_SPACE);
                    case 'S':
                        return new CharSet(0).kind(CharSet.U_WHITE_SPACE).not();
                    default:
                        break;
                }
            }
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
            // Under CASE_INSENSITIVE the case classes match either case, as
            // they have on the JDK since 16 (JDK-8214245): the ASCII ones become
            // the ASCII letters, the Unicode ones any cased letter.
            boolean anyCase = has(CASE_INSENSITIVE);
            CharSet unicode = unicodeProperty(name, anyCase);
            if (unicode != null) {
                return unicode;
            }
            // The Unicode binary properties come from the tables, so they
            // answer exactly what the JDK does for every code point.
            if (name.startsWith("Is")) {
                CharSet binary = binaryProperty(name.substring(2));
                if (binary != null) {
                    return binary;
                }
            }
            if (category(name.startsWith("Is") ? name.substring(2) : name) != 0) {
                return tableProperty(name);
            }
            if ("Lower".equals(name)) {
                return anyCase ? s.add('a', 'z').add('A', 'Z') : s.add('a', 'z');
            }
            if ("Upper".equals(name)) {
                return anyCase ? s.add('a', 'z').add('A', 'Z') : s.add('A', 'Z');
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
                return anyCase ? s.kind(CharSet.LOWER | CharSet.UPPER | CharSet.TITLE) : s.kind(CharSet.LOWER);
            }
            if ("javaUpperCase".equals(name) || "Lu".equals(name) || "IsLu".equals(name)
                    || "IsUppercase".equals(name)) {
                return anyCase ? s.kind(CharSet.LOWER | CharSet.UPPER | CharSet.TITLE) : s.kind(CharSet.UPPER);
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
            CharSet table = tableProperty(name);
            if (table != null) {
                return table;
            }
            throw unsupported("The character property {" + name + "}");
        }

        private static CharSet categories(int mask) {
            CharSet s = new CharSet(0);
            s.categories = mask;
            return s;
        }

        /// The POSIX classes under `UNICODE_CHARACTER_CLASS`, or null.
        private CharSet unicodeProperty(String name, boolean anyCase) {
            if (!has(UNICODE_CHARACTER_CLASS)) {
                return null;
            }
            CharSet s = new CharSet(0);
            if ("Lower".equals(name)) {
                return anyCase ? s.kind(CharSet.U_LOWER | CharSet.U_UPPER).unite(categories(1 << Character.TITLECASE_LETTER))
                        : s.kind(CharSet.U_LOWER);
            }
            if ("Upper".equals(name)) {
                return anyCase ? s.kind(CharSet.U_LOWER | CharSet.U_UPPER).unite(categories(1 << Character.TITLECASE_LETTER))
                        : s.kind(CharSet.U_UPPER);
            }
            if ("Alpha".equals(name)) {
                return s.kind(CharSet.U_ALPHABETIC);
            }
            if ("Digit".equals(name)) {
                return categories(1 << Character.DECIMAL_DIGIT_NUMBER);
            }
            if ("Alnum".equals(name)) {
                return s.kind(CharSet.U_ALPHABETIC).unite(categories(1 << Character.DECIMAL_DIGIT_NUMBER));
            }
            if ("Punct".equals(name)) {
                return categories(PUNCTUATION);
            }
            if ("Graph".equals(name)) {
                return s.kind(CharSet.U_GRAPH);
            }
            if ("Print".equals(name)) {
                return s.kind(CharSet.U_PRINT);
            }
            if ("Blank".equals(name)) {
                return s.kind(CharSet.U_BLANK);
            }
            if ("Cntrl".equals(name)) {
                return categories(1 << Character.CONTROL);
            }
            if ("XDigit".equals(name)) {
                return s.kind(CharSet.U_HEX_DIGIT);
            }
            if ("Space".equals(name)) {
                return s.kind(CharSet.U_WHITE_SPACE);
            }
            return null;
        }

        private static final int PUNCTUATION = 1 << Character.CONNECTOR_PUNCTUATION | 1 << Character.DASH_PUNCTUATION
                | 1 << Character.START_PUNCTUATION | 1 << Character.END_PUNCTUATION
                | 1 << Character.OTHER_PUNCTUATION | 1 << Character.INITIAL_QUOTE_PUNCTUATION
                | 1 << Character.FINAL_QUOTE_PUNCTUATION;

        /// The general category masks, by the names the JDK accepts.
        private static int category(String name) {
            String[] single = {"Cn", "Lu", "Ll", "Lt", "Lm", "Lo", "Mn", "Me", "Mc", "Nd", "Nl", "No", "Zs", "Zl",
                "Zp", "Cc", "Cf", null, "Co", "Cs", "Pd", "Ps", "Pe", "Pc", "Po", "Sm", "Sc", "Sk", "So", "Pi", "Pf"};
            for (int k = 0; k < single.length; k++) {
                if (name.equals(single[k])) {
                    return 1 << k;
                }
            }
            if (name.length() == 1) {
                int mask = 0;
                for (int k = 0; k < single.length; k++) {
                    if (single[k] != null && single[k].charAt(0) == name.charAt(0)) {
                        mask |= 1 << k;
                    }
                }
                return mask;
            }
            if ("LC".equals(name)) {
                return 1 << Character.UPPERCASE_LETTER | 1 << Character.LOWERCASE_LETTER
                        | 1 << Character.TITLECASE_LETTER;
            }
            return 0;
        }

        /// Scripts, blocks and general categories, from [UnicodeTables];
        /// null for a name that is none of them.
        private CharSet tableProperty(String name) {
            int eq = name.indexOf('=');
            if (eq >= 0) {
                String key = name.substring(0, eq);
                String value = name.substring(eq + 1);
                if ("script".equals(key) || "sc".equals(key)) {
                    return script(value);
                }
                if ("block".equals(key) || "blk".equals(key)) {
                    return block(value);
                }
                if ("general_category".equals(key) || "gc".equals(key)) {
                    int mask = category(value);
                    return mask == 0 ? null : caseless(mask);
                }
                return null;
            }
            if (name.startsWith("In")) {
                return block(name.substring(2));
            }
            if (name.startsWith("Is")) {
                String rest = name.substring(2);
                CharSet binary = binaryProperty(rest);
                if (binary != null) {
                    return binary;
                }
                int mask = category(rest);
                if (mask != 0) {
                    return caseless(mask);
                }
                return script(rest);
            }
            int mask = category(name);
            return mask == 0 ? null : caseless(mask);
        }

        /// The set a general category names. Under `CASE_INSENSITIVE` a case
        /// category matches every cased character, as the JDK's does.
        private CharSet caseless(int mask) {
            int cased = 1 << Character.UPPERCASE_LETTER | 1 << Character.LOWERCASE_LETTER
                    | 1 << Character.TITLECASE_LETTER;
            return categories(has(CASE_INSENSITIVE) && (mask & cased) != 0 ? mask | cased : mask);
        }

        /// The JDK's binary properties that these tables can answer, named
        /// without regard to case as the JDK reads them; null for any other.
        private CharSet binaryProperty(String name) {
            StringBuilder folded = new StringBuilder(name.length());
            for (int k = 0; k < name.length(); k++) {
                char c = name.charAt(k);
                folded.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
            }
            String key = folded.toString();
            CharSet s = new CharSet(0);
            if ("ALPHABETIC".equals(key)) {
                return s.kind(CharSet.U_ALPHABETIC);
            }
            if ("DIGIT".equals(key)) {
                return categories(1 << Character.DECIMAL_DIGIT_NUMBER);
            }
            if ("PUNCTUATION".equals(key)) {
                return categories(PUNCTUATION);
            }
            if ("CONTROL".equals(key)) {
                return categories(1 << Character.CONTROL);
            }
            if ("ASSIGNED".equals(key)) {
                return categories(1 << Character.UNASSIGNED).not();
            }
            if ("HEXDIGIT".equals(key) || "HEX_DIGIT".equals(key)) {
                return s.kind(CharSet.U_HEX_DIGIT);
            }
            if ("JOINCONTROL".equals(key) || "JOIN_CONTROL".equals(key)) {
                return s.add(0x200c, 0x200d);
            }
            if ("NONCHARACTERCODEPOINT".equals(key) || "NONCHARACTER_CODE_POINT".equals(key)) {
                s.add(0xfdd0, 0xfdef);
                for (int plane = 0; plane <= 0x10; plane++) {
                    s.add(plane * 0x10000 + 0xfffe, plane * 0x10000 + 0xffff);
                }
                return s;
            }
            if ("WHITESPACE".equals(key) || "WHITE_SPACE".equals(key)) {
                return s.kind(CharSet.U_WHITE_SPACE);
            }
            if ("WORD".equals(key)) {
                return s.kind(CharSet.U_WORD);
            }
            if ("ALNUM".equals(key)) {
                return s.kind(CharSet.U_ALPHABETIC).unite(categories(1 << Character.DECIMAL_DIGIT_NUMBER));
            }
            if ("BLANK".equals(key)) {
                return s.kind(CharSet.U_BLANK);
            }
            if ("GRAPH".equals(key)) {
                return s.kind(CharSet.U_GRAPH);
            }
            if ("PRINT".equals(key)) {
                return s.kind(CharSet.U_PRINT);
            }
            boolean anyCase = has(CASE_INSENSITIVE);
            int cased = 1 << Character.TITLECASE_LETTER;
            if ("LOWERCASE".equals(key)) {
                return anyCase ? s.kind(CharSet.U_LOWER | CharSet.U_UPPER).unite(categories(cased)) : s.kind(CharSet.U_LOWER);
            }
            if ("UPPERCASE".equals(key)) {
                return anyCase ? s.kind(CharSet.U_LOWER | CharSet.U_UPPER).unite(categories(cased)) : s.kind(CharSet.U_UPPER);
            }
            if ("TITLECASE".equals(key)) {
                return anyCase ? s.kind(CharSet.U_LOWER | CharSet.U_UPPER).unite(categories(cased)) : categories(cased);
            }
            if ("LETTER".equals(key)) {
                return categories(1 << Character.UPPERCASE_LETTER | 1 << Character.LOWERCASE_LETTER
                        | cased | 1 << Character.MODIFIER_LETTER | 1 << Character.OTHER_LETTER);
            }
            return null;
        }

        private static CharSet script(String name) {
            int index = UnicodeTables.script(name);
            if (index < 0) {
                return null;
            }
            CharSet s = new CharSet(0);
            s.script = index;
            return s;
        }

        private static CharSet block(String name) {
            int[] range = UnicodeTables.block(name);
            return range == null ? null : new CharSet(0).add(range[0], range[1]);
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
                case 'b': {
                    // Only \b{g} is a grapheme boundary; \b{2} is a word
                    // boundary under a counted quantifier, as in the JDK.
                    if (at + 1 < p.length() && p.charAt(at) == '{' && p.charAt(at + 1) == 'g') {
                        throw unsupported("The boundary \\b{g}");
                    }
                    Anchor word = new Anchor(Anchor.WORD, unix);
                    word.unicode = has(UNICODE_CHARACTER_CLASS);
                    return Piece.of(word, 0);
                }
                case 'B': {
                    Anchor word = new Anchor(Anchor.NOT_WORD, unix);
                    word.unicode = has(UNICODE_CHARACTER_CLASS);
                    return Piece.of(word, 0);
                }
                case 'X':
                    throw unsupported("The grapheme cluster \\X");
                case 'N':
                    throw unsupported("The named character \\N{...}");
                case 'R': {
                    // \r\n as one, or any single line end. Not atomic: when
                    // what follows needs the \n, the pair splits and \R takes
                    // the \r alone, as Java 9 and later do ("\R\R" matches
                    // "\r\n").
                    Piece crlf = Piece.of(new Char('\r'), 1);
                    crlf.then(Piece.of(new Char('\n'), 1));
                    Piece one = single(new InClass(vertical()));
                    Pass join = new Pass();
                    crlf.last.next = join;
                    one.last.next = join;
                    Piece lineEnd = new Piece(new Branch(new Node[] {crlf.first, one.first}), join, 1, 2);
                    lineEnd.deterministic = false;
                    return lineEnd;
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
                return single(new InClass(set));
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
                        while (at < end) {
                            int cp = codePointAt(p, at, end);
                            set.add(note(cp));
                            at += cp > 0xffff ? 2 : 1;
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
                    lo = patternCodePoint();
                }
                int hi = lo;
                if (at + 1 < p.length() && p.charAt(at) == '-' && p.charAt(at + 1) != ']'
                        && p.charAt(at + 1) != '[') {
                    at++;
                    int h = patternCodePoint();
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
                note(lo);
                note(hi);
                if (lo < 0xd800 && hi > 0xdfff) {
                    supplementary = true;
                }
                set.add(lo, hi);
            }
        }
    }
}
