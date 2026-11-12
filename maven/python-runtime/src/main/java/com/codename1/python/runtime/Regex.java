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
package com.codename1.python.runtime;

/// `_cn1re`: the backtracking machine of `re/_engine.py`, in Java.
///
/// `re/_parser.py` still compiles a pattern to its tuple program; this runs
/// that program. It is the Python `_exec` instruction for instruction --
/// the same opcodes, choice points, trail and register layout -- so the two
/// cannot disagree about what matches. Run as transpiled Python, every step
/// of the machine was generic Python arithmetic and a tuple per choice point;
/// a URL router matches a pattern per request, so that was most of what a
/// Flask request allocated.
///
/// Text is indexed by code point, as Python indexes a `str`: a string with
/// no surrogate pair is read as its chars, any other as its code points.
final class Regex {
    private static final int CHAR = 1;
    private static final int CHARI = 2;
    private static final int ANY = 3;
    private static final int ANYALL = 4;
    private static final int SET = 5;
    private static final int AT = 6;
    private static final int SPLIT = 7;
    private static final int JMP = 8;
    private static final int SAVE = 9;
    private static final int BACKREF = 10;
    private static final int LOOK = 11;
    private static final int ATOMIC = 12;
    private static final int MATCH = 13;
    private static final int MARK = 14;
    private static final int PROGRESS = 15;
    private static final int GROUPREF_EXISTS = 16;
    private static final int REPEAT_ONE = 17;
    private static final int MIN_REPEAT_ONE = 18;

    private static final int AT_BEGIN = 0;
    private static final int AT_BOL = 1;
    private static final int AT_END = 2;
    private static final int AT_EOL = 3;
    private static final int AT_EOLM = 4;
    private static final int AT_BOUNDARY = 5;
    private static final int AT_NONBOUNDARY = 6;

    private static final int ALNUM = Unicode.ALPHA | Unicode.DECIMAL | Unicode.DIGIT | Unicode.NUMERIC;

    private Regex() {
    }

    /// One instruction. Which fields mean what depends on `k`, as the
    /// tuple's positions do in `_engine.py`.
    static final class Op {
        int k;
        /// CHAR: the code point. CHARI: the lower case one, or -1 when the
        /// lower case form is not one code point.
        int c;
        /// CHARI: the upper case code point, or -1.
        int c2;
        /// CHARI: the lower case form, compared with each char's own.
        String lower;
        /// SPLIT, JMP: targets. SAVE, MARK, BACKREF, GROUPREF_EXISTS,
        /// PROGRESS: the register or group, then the target.
        int a;
        int b;
        /// REPEAT_ONE, MIN_REPEAT_ONE: bounds; `hi` -1 for unbounded.
        int lo;
        int hi;
        Op one;
        /// SET.
        int[] chars;
        boolean[] ascii;
        int[] ranges;
        String cats;
        boolean neg;
        boolean rcase;
        boolean unicode;
        /// AT: the assertion. LOOK: behind, negative, width.
        int at;
        boolean behind;
        /// A LOOK whose program is single-char tests and then MATCH (`(?<!\\)`,
        /// `(?=px)`, `(?<![a-z])`): it is answered in line, by those tests,
        /// with no nested run, register copy or stacks.
        boolean simple;
        int width;
        /// LOOK, ATOMIC, BACKREF: the sub-program, or the case flag.
        Op[] sub;
        boolean icase;
        /// SET: the whole answer for each ASCII char, categories, ranges and
        /// negation included, filled on first use; a `\s` or `\w` otherwise
        /// asks the category table per char.
        boolean[] full;

        Op() {
            chars = NO_INTS;
            ascii = NO_ASCII;
            ranges = NO_INTS;
            cats = "";
        }
    }

    private static final int[] NO_INTS = new int[0];
    private static final boolean[] NO_ASCII = new boolean[128];
    /// What `run` answers when nothing matched.
    private static final int[] NO_MATCH = new int[0];

    /// A compiled pattern: the program and what `run` needs of the code
    /// object around it.
    static final class Prog extends PyObject {
        private static final PyType TYPE = new PyType("_cn1re.Prog", new PyType[] {Py.OBJECT_TYPE}, null);
        Op[] ops;
        int groups;
        int marks;
        String prefix;
        boolean anchored;
        /// The ops one of which must match the first character of any match,
        /// or null when a match can begin some other way (an assertion, an
        /// optional item, a backreference). A search skips every position
        /// none of them matches instead of running the program there, as
        /// sre's charset prefix does; MarkItDown's HTML and Markdown
        /// patterns spent most of their time failing at every offset.
        Op[] first;
        /// For each ASCII char, whether one of the char-consuming [#first]
        /// ops matches it; all false when there is no first set.
        boolean[] firstAscii = NO_ASCII;
        /// Whether [#first] holds an assertion, which the table cannot answer.
        boolean firstAt;
        /// The text view, registers and backtracking stacks of the last run,
        /// handed back for the next one: a route table or a tokenizer runs
        /// one pattern per request, and these were most of what a run
        /// allocated. A plain field: Python runs under the [Gil], one thread
        /// at a time, and a run never releases it. Taken (left null) for the
        /// run, so a run nested in another builds its own.
        Scratch scratch;
        /// 1 for a str pattern, 2 for bytes, 0 until [RegexMatch] first asks.
        int kind;
        /// Compiled replacement templates by text, as sre caches them; at
        /// most [RegexMatch#TEMPLATES].
        java.util.HashMap<String, PyList> templates;
        /// The same for bytes replacements, keyed by their Latin-1 text.
        java.util.HashMap<String, PyList> byteTemplates;

        @Override
        public PyType type() {
            return TYPE;
        }
    }

    private static final class Scratch {
        final Text text = new Text();
        final Ints stack = new Ints();
        final Ints trail = new Ints();
        int[] regs = NO_INTS;
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1re", new String[0]);
        m.setAttr("compile", PyBuiltin.positional("compile", a -> {
            Prog p = new Prog();
            p.ops = program(a[0]);
            p.groups = (int) Py.index(a[1]);
            p.marks = (int) Py.index(a[2]);
            p.prefix = Py.isTrue(a[3]) ? Py.toStr(a[3]) : null;
            p.anchored = Py.isTrue(a[4]);
            p.first = firstOps(p.ops);
            if (p.first != null) {
                p.firstAscii = new boolean[128];
                for (Op op : p.first) {
                    if (op.k == AT) {
                        p.firstAt = true;
                        continue;
                    }
                    for (int c = 0; c < 128; c++) {
                        p.firstAscii[c] |= one(op, c);
                    }
                }
            }
            return p;
        }));
        m.setAttr("run", new RunFn());
        m.setAttr("match", new MatchFn());
        m.setAttr("pattern_match", patternMethod("match", 0));
        m.setAttr("pattern_fullmatch", patternMethod("fullmatch", 1));
        m.setAttr("pattern_search", patternMethod("search", 2));
        // The module's own callers (re/__init__.py) pass every argument, so each
        // entry also has its array-free form for exactly that many.
        m.setAttr("iter", PyBuiltin.positional("iter", a -> RegexMatch.search(prog(a[0]), a[1], a[2], a[3],
                a[4], (int) Py.index(a[5]), 2, (int) Py.index(a[6])))
                .with7((a0, a1, a2, a3, a4, a5, a6) -> RegexMatch.search(prog(a0), a1, a2, a3, a4,
                        (int) Py.index(a5), 2, (int) Py.index(a6))));
        m.setAttr("set_hooks", PyBuiltin.fixed3("set_hooks", (a0, a1, a2) -> {
            RegexMatch.expandHook = a0;
            RegexMatch.templateHook = a1;
            RegexMatch.subHook = a2;
            return Py.NONE;
        }));
        m.setAttr("sub", PyBuiltin.positional("sub", a -> RegexMatch.sub(prog(a[0]), a[1], a[2], a[3],
                a[4], Py.isTrue(a[5])))
                .with6((a0, a1, a2, a3, a4, a5) -> RegexMatch.sub(prog(a0), a1, a2, a3, a4, Py.isTrue(a5))));
        m.setAttr("Match", RegexMatch.TYPE);
        m.setAttr("subn", PyBuiltin.positional("subn", a -> subn(prog(a[0]), Py.toStr(a[1]),
                (PyList) a[2], (int) Py.index(a[3])))
                .with4((a0, a1, a2, a3) -> subn(prog(a0), Py.toStr(a1), (PyList) a2, (int) Py.index(a3))));
        m.setAttr("split", PyBuiltin.fixed3("split", (a0, a1, a2) -> split(prog(a0), Py.toStr(a1),
                (int) Py.index(a2))));
        m.setAttr("findall", PyBuiltin.positional("findall", a -> findall(prog(a[0]), Py.toStr(a[1]),
                (int) Py.index(a[2]), (int) Py.index(a[3])))
                .with4((a0, a1, a2, a3) -> findall(prog(a0), Py.toStr(a1), (int) Py.index(a2), (int) Py.index(a3))));
        return m;
    }

    /// See `Prog.first`.
    private static Op[] firstOps(Op[] ops) {
        java.util.ArrayList<Op> out = new java.util.ArrayList<Op>();
        boolean[] seen = new boolean[ops.length];
        int[] work = new int[ops.length + 1];
        int n = 0;
        work[n++] = 0;
        while (n > 0) {
            int pc = work[--n];
            while (true) {
                if (pc < 0 || pc >= ops.length) {
                    return null;
                }
                if (seen[pc]) {
                    break;
                }
                seen[pc] = true;
                Op op = ops[pc];
                if (op.k == CHAR || op.k == CHARI || op.k == ANY || op.k == ANYALL || op.k == SET) {
                    out.add(op);
                    break;
                }
                if (op.k == AT) {
                    // Zero width: a match starting here needs the assertion
                    // to hold here. markdownify's `(\s|^)...` escapes have
                    // one, and ran the program at every offset without it.
                    out.add(op);
                    break;
                }
                if (op.k == REPEAT_ONE || op.k == MIN_REPEAT_ONE) {
                    out.add(op.one);
                    if (op.lo >= 1) {
                        break;
                    }
                    // Optional: a match begins with the item or with whatever
                    // follows it. markdownify's `[\t \r\n]*[\r\n][\t \r\n]*`
                    // ran the program at every offset without this, where sre
                    // skips to the next whitespace.
                    pc++;
                    continue;
                }
                if (op.k == SAVE || op.k == LOOK) {
                    // A lookaround is zero width too: whatever follows it must
                    // still match from the same offset, so its first set is
                    // a necessary condition. `(?<![a-z])\d+` ran the whole
                    // program at every offset without this.
                    pc++;
                } else if (op.k == JMP) {
                    pc = op.a;
                } else if (op.k == SPLIT) {
                    if (n == work.length) {
                        return null;
                    }
                    work[n++] = op.b;
                    pc = op.a;
                } else {
                    return null;
                }
            }
        }
        for (Op op : out) {
            if (op.k == ANY || op.k == ANYALL) {
                // Nearly every character qualifies; the filter costs more
                // than it skips.
                return null;
            }
        }
        return out.isEmpty() ? null : out.toArray(new Op[0]);
    }

    private static Op[] program(Object prog) {
        Object[] src = Py.toArray(prog);
        Op[] out = new Op[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = op(Py.toArray(src[i]));
        }
        return out;
    }

    private static int cp(Object s) {
        String v = Py.toStr(s);
        if (v.length() == 1) {
            return v.charAt(0);
        }
        if (v.length() == 2 && Character.isHighSurrogate(v.charAt(0))) {
            return v.codePointAt(0);
        }
        return -1;
    }

    private static int num(Object o) {
        return o instanceof PyNone ? -1 : (int) Py.index(o);
    }

    private static Op op(Object[] t) {
        Op o = new Op();
        o.k = (int) Py.index(t[0]);
        switch (o.k) {
            case CHAR:
                o.c = cp(t[1]);
                break;
            case CHARI:
                o.lower = Py.toStr(t[1]);
                o.c = cp(t[1]);
                o.c2 = cp(t[2]);
                break;
            case SET:
                set(o, t);
                break;
            case AT:
                o.at = atKind(Py.toStr(t[1]));
                o.unicode = Py.isTrue(t[2]);
                break;
            case SPLIT:
            case JMP:
            case SAVE:
            case MARK:
                o.a = (int) Py.index(t[1]);
                o.b = t.length > 2 ? (int) Py.index(t[2]) : 0;
                break;
            case PROGRESS:
            case GROUPREF_EXISTS:
                o.a = (int) Py.index(t[1]);
                o.b = (int) Py.index(t[2]);
                break;
            case BACKREF:
                o.a = (int) Py.index(t[1]);
                o.icase = Py.isTrue(t[2]);
                break;
            case REPEAT_ONE:
            case MIN_REPEAT_ONE:
                o.one = op(Py.toArray(t[1]));
                o.lo = (int) Py.index(t[2]);
                o.hi = num(t[3]);
                break;
            case LOOK:
                o.sub = program(t[1]);
                o.behind = Py.isTrue(t[2]);
                o.neg = Py.isTrue(t[3]);
                o.width = num(t[4]);
                o.simple = simpleLook(o.sub);
                break;
            case ATOMIC:
                o.sub = program(t[1]);
                break;
            default:
                break;
        }
        return o;
    }

    private static int atKind(String s) {
        if ("begin".equals(s)) {
            return AT_BEGIN;
        }
        if ("bol".equals(s)) {
            return AT_BOL;
        }
        if ("end".equals(s)) {
            return AT_END;
        }
        if ("eol".equals(s)) {
            return AT_EOL;
        }
        if ("eolm".equals(s)) {
            return AT_EOLM;
        }
        return "boundary".equals(s) ? AT_BOUNDARY : AT_NONBOUNDARY;
    }

    private static void set(Op o, Object[] t) {
        Object[] chars = Py.toArray(t[1]);
        int[] cs = new int[chars.length];
        int n = 0;
        for (Object c : chars) {
            int v = cp(c);
            // A form longer than one code point (the upper case of a
            // sharp s) never equals one character of the text.
            if (v >= 0) {
                cs[n++] = v;
            }
        }
        int[] sorted = new int[n];
        System.arraycopy(cs, 0, sorted, 0, n);
        java.util.Arrays.sort(sorted);
        o.chars = sorted;
        o.ascii = new boolean[128];
        for (int v : sorted) {
            if (v < 128) {
                o.ascii[v] = true;
            }
        }
        Object[] ranges = Py.toArray(t[2]);
        o.ranges = new int[ranges.length * 2];
        for (int i = 0; i < ranges.length; i++) {
            Object[] r = Py.toArray(ranges[i]);
            o.ranges[2 * i] = (int) Py.index(r[0]);
            o.ranges[2 * i + 1] = (int) Py.index(r[1]);
        }
        o.cats = Py.toStr(t[3]);
        o.neg = Py.isTrue(t[4]);
        o.rcase = Py.isTrue(t[5]);
        o.unicode = Py.isTrue(t[6]);
    }

    // ---- characters ------------------------------------------------------

    /// The text, by code point.
    private static final class Text {
        String s;
        int[] cps;

        Text() {
        }

        Text(String s) {
            reset(s);
        }

        void reset(String s) {
            reset(s, -1);
        }

        /// `pairs` is the text's surrogate pair count when the caller has it
        /// already (a match measured the text for its bounds), -1 otherwise.
        void reset(String s, int known) {
            if (s == this.s) { //NOPMD CompareObjectsWithEquals - the same text, whose count is known
                // A substitution or findall runs once per match over one
                // text, and a text of up to 256 chars is never cached, so
                // each run counted its pairs again.
                return;
            }
            this.s = s;
            // PyStr keeps the count for the last long string, so a pattern
            // run again and again over one text (a scanner's match(text,
            // pos)) does not count it on every call.
            int pairs = known >= 0 ? known : PyStr.surrogates(s);
            if (pairs == 0) {
                cps = null;
            } else {
                cps = new int[s.length() - pairs];
                int i = 0;
                int j = 0;
                while (i < s.length()) {
                    int c = s.codePointAt(i);
                    cps[j++] = c;
                    i += c >= 0x10000 ? 2 : 1;
                }
            }
        }

        int at(int i) {
            return cps == null ? s.charAt(i) : cps[i];
        }
    }

    /// `str.isalnum`, `isdecimal` and `isspace` of one character, which
    /// `str` answers char by char: a code point outside the BMP is a
    /// surrogate pair there, and neither half is in any class.
    private static boolean is(int c, int mask) {
        return c < 0x10000 && Unicode.is((char) c, mask);
    }

    private static String str(int c) {
        if (c < 0x10000) {
            return String.valueOf((char) c);
        }
        int v = c - 0x10000;
        return new String(new char[] {(char) (0xD800 + (v >> 10)), (char) (0xDC00 + (v & 0x3FF))});
    }

    private static boolean cat(char k, int c, boolean unicode) {
        char lk = k >= 'A' && k <= 'Z' ? (char) (k + 32) : k;
        boolean r;
        if (lk == 'd') {
            r = unicode ? is(c, Unicode.DECIMAL) : c >= '0' && c <= '9';
        } else if (lk == 'w') {
            r = unicode ? is(c, ALNUM) || c == '_' : asciiWord(c);
        } else {
            r = unicode ? is(c, Unicode.SPACE) : c == ' ' || c >= 9 && c <= 13;
        }
        return k == lk ? r : !r;
    }

    private static boolean asciiWord(int c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }

    private static boolean inRanges(int[] ranges, int o) {
        for (int i = 0; i < ranges.length; i += 2) {
            if (ranges[i] <= o && o <= ranges[i + 1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean simpleLook(Op[] sub) {
        if (sub == null) {
            return false;
        }
        int n = sub.length;
        if (n < 2 || sub[n - 1].k != MATCH) {
            return false;
        }
        for (int i = 0; i < n - 1; i++) {
            int k = sub[i].k;
            if (k != CHAR && k != CHARI && k != ANY && k != ANYALL && k != SET) {
                return false;
            }
        }
        return true;
    }

    /// A simple LOOK's own tests (see `Op.simple`), before the negation.
    private static boolean lookHit(Op op, Text t, int pos, int endpos) {
        Op[] sub = op.sub;
        if (sub == null) {
            return false;
        }
        int n = sub.length - 1;
        int from = op.behind ? pos - n : pos;
        if (from < 0 || from + n > (op.behind ? pos : endpos)) {
            return false;
        }
        for (int q = 0; q < n; q++) {
            if (!one(sub[q], t.at(from + q))) {
                return false;
            }
        }
        return true;
    }

    private static boolean one(Op op, int c) {
        switch (op.k) {
            case CHAR:
                return c == op.c;
            case CHARI:
                if (c == op.c || c == op.c2) {
                    return true;
                }
                if (c < 128) {
                    // An ASCII char lowers to one ASCII char, which equals the
                    // pattern's lower case form only when that form is this
                    // one code point; no strings are made to find out.
                    return (c >= 'A' && c <= 'Z' ? c + 32 : c) == op.c;
                }
                return TypeMethods.mapCase(str(c), 0).equals(op.lower);
            case ANY:
                return c != '\n';
            case ANYALL:
                return true;
            default:
                break;
        }
        if (c < 128) {
            return table(op)[c];
        }
        return set(op, c);
    }

    /// The ASCII answers of a SET-like op (see [Op#full]), or null for one
    /// of the kinds [#one] answers directly.
    private static boolean[] table(Op op) {
        switch (op.k) {
            case CHAR:
            case CHARI:
            case ANY:
            case ANYALL:
            case AT:
                return null;
            default:
                break;
        }
        boolean[] f = op.full;
        if (f == null) {
            f = new boolean[128];
            for (int i = 0; i < 128; i++) {
                f[i] = set(op, i);
            }
            op.full = f;
        }
        return f;
    }

    /// How far from `pos` (up to `limit`) the chars all match `op`.
    private static int span(Op op, Text t, int pos, int limit) {
        int j = pos;
        boolean[] f = t.cps == null ? table(op) : null;
        if (f != null) {
            // The common case, a \s+ or [^<]*: the string read directly, an
            // ASCII char answered by one load.
            String s = t.s;
            while (j < limit) {
                char c = s.charAt(j);
                if (c < 128 ? !f[c] : !set(op, c)) {
                    break;
                }
                j++;
            }
            return j;
        }
        if (t.cps == null) {
            // The kinds with no table, on the string itself: `.*` is a search
            // for the next line end and a repeated literal a run of one char.
            String s = t.s;
            if (op.k == ANYALL) {
                return limit;
            }
            if (op.k == ANY) {
                if (limit == s.length()) {
                    int nl = s.indexOf('\n', j);
                    return nl < 0 ? limit : nl;
                }
                // A bounded repeat is walked: a search would read on to a line
                // end that may be the whole text away, once per attempt.
                while (j < limit && s.charAt(j) != '\n') {
                    j++;
                }
                return j;
            }
            if (op.k == CHAR) {
                int c = op.c;
                while (j < limit && s.charAt(j) == c) {
                    j++;
                }
                return j;
            }
        }
        while (j < limit && one(op, t.at(j))) {
            j++;
        }
        return j;
    }

    private static boolean set(Op op, int c) {
        boolean r = c < 128 ? op.ascii[c] : java.util.Arrays.binarySearch(op.chars, c) >= 0;
        if (!r && op.ranges.length > 0) {
            r = inRanges(op.ranges, c);
            if (!r && op.rcase) {
                String s = str(c);
                for (int m = 0; m < 2 && !r; m++) {
                    String alt = TypeMethods.mapCase(s, m);
                    if (alt.codePointCount(0, alt.length()) == 1) {
                        r = inRanges(op.ranges, alt.codePointAt(0));
                    }
                }
            }
        }
        if (!r) {
            String cats = op.cats;
            for (int i = 0; i < cats.length(); i++) {
                if (cat(cats.charAt(i), c, op.unicode)) {
                    r = true;
                    break;
                }
            }
        }
        return r != op.neg;
    }

    private static boolean word(Text t, int i, int endpos, boolean unicode) {
        if (i < 0 || i >= endpos) {
            return false;
        }
        int c = t.at(i);
        return unicode ? is(c, ALNUM) || c == '_' : asciiWord(c);
    }

    private static boolean at(int kind, Text t, int pos, int endpos, boolean unicode) {
        switch (kind) {
            case AT_BEGIN:
                return pos == 0;
            case AT_BOL:
                return pos == 0 || t.at(pos - 1) == '\n';
            case AT_END:
                return pos == endpos;
            case AT_EOL:
                return pos == endpos || pos == endpos - 1 && t.at(pos) == '\n';
            case AT_EOLM:
                return pos == endpos || t.at(pos) == '\n';
            default:
                break;
        }
        boolean b = word(t, pos - 1, endpos, unicode) != word(t, pos, endpos, unicode);
        return kind == AT_BOUNDARY ? b : !b;
    }

    /// `_cn1re.run(prog, text, pos, endpos, mode, forbid)`. Called once per
    /// match attempt with all six arguments, so it takes them directly
    /// rather than as the argument array a [PyBuiltin] would build.
    private static Prog prog(Object o) {
        if (!(o instanceof Prog)) {
            throw Py.typeError("_cn1re needs a compiled program");
        }
        return (Prog) o;
    }

    private static final class RunFn extends PyObject {
        @Override
        public PyType type() {
            return Py.BUILTIN_TYPE;
        }

        @Override
        public Object call6(Object prog, Object text, Object pos, Object endpos, Object mode, Object forbid) {
            if (!(prog instanceof Prog)) {
                throw Py.typeError("_cn1re.run() needs a compiled program");
            }
            int[] regs = run((Prog) prog, Py.toStr(text), (int) Py.index(pos), (int) Py.index(endpos),
                    (int) Py.index(mode), (int) Py.index(forbid));
            if (regs.length == 0) {
                return Py.NONE;
            }
            Object[] out = new Object[regs.length];
            for (int i = 0; i < regs.length; i++) {
                out[i] = Long.valueOf(regs[i]);
            }
            return new PyList(out);
        }

        @Override
        public Object call(Object[] args, String[] kwNames) {
            if (args.length != 6 || kwNames.length != 0) {
                throw Py.typeError("_cn1re.run() takes exactly 6 positional arguments");
            }
            return call6(args[0], args[1], args[2], args[3], args[4], args[5]);
        }

        @Override
        public String repr() {
            return "<built-in function run>";
        }
    }

    /// `_cn1re.match(prog, pattern, string, pos, endpos, mode)`: see [RegexMatch].
    /// `self._prog` of a `re.Pattern`, read through one cached site.
    private static final AttrSite PROG = new AttrSite("_prog");

    /// `re.Pattern.match`/`fullmatch`/`search` themselves, stored on the
    /// class: `pattern.match(s)` is then one call into Java with the pattern
    /// prepended, with no Python frame between, as sre's methods are C.
    private static PyBuiltin patternMethod(final String name, final int mode) {
        return new PyBuiltin(name, (a, kw) -> {
            Object string = null;
            Object pos = 0L;
            Object end = Py.NONE;
            int np = a.length - kw.length;
            if (np < 1 || np > 4) {
                throw Py.typeError("Pattern." + name + "() takes from 1 to 3 positional arguments but "
                        + (np - 1) + " were given");
            }
            if (np > 1) {
                string = a[1];
            }
            if (np > 2) {
                pos = a[2];
            }
            if (np > 3) {
                end = a[3];
            }
            for (int i = 0; i < kw.length; i++) {
                Object v = a[np + i];
                if ("string".equals(kw[i]) && np <= 1) {
                    string = v;
                } else if ("pos".equals(kw[i]) && np <= 2) {
                    pos = v;
                } else if ("endpos".equals(kw[i]) && np <= 3) {
                    end = v;
                } else {
                    throw Py.typeError("Pattern." + name + "() got an unexpected keyword argument '" + kw[i] + "'");
                }
            }
            if (string == null) {
                throw Py.typeError("Pattern." + name + "() missing required argument 'string' (pos 1)");
            }
            return RegexMatch.match(PROG.get(a[0]), a[0], string, pos, end, mode);
        }).with2((self, string) -> RegexMatch.match(PROG.get(self), self, string, 0L, Py.NONE, mode))
                .with3((self, string, pos) -> RegexMatch.match(PROG.get(self), self, string, pos, Py.NONE, mode))
                .with4((self, string, pos, end) -> RegexMatch.match(PROG.get(self), self, string, pos, end, mode));
    }

    private static final class MatchFn extends PyObject {
        @Override
        public PyType type() {
            return Py.BUILTIN_TYPE;
        }

        @Override
        public Object call6(Object prog, Object pattern, Object string, Object pos, Object endpos, Object mode) {
            return RegexMatch.match(prog, pattern, string, pos, endpos, (int) Py.index(mode));
        }

        @Override
        public Object call(Object[] args, String[] kwNames) {
            if (args.length != 6 || kwNames.length != 0) {
                throw Py.typeError("_cn1re.match() takes exactly 6 positional arguments");
            }
            return call6(args[0], args[1], args[2], args[3], args[4], args[5]);
        }

        @Override
        public String repr() {
            return "<built-in function match>";
        }
    }

    // ---- the machine -----------------------------------------------------

    /// A growable int stack: the trail holds (register, old value) pairs and
    /// the choice points six ints each.
    private static final class Ints {
        int[] a = NO_INTS;
        int n;
        /// What a lookaround or atomic group run from this stack's exec works
        /// in: its copy of the registers and its own two stacks, kept here
        /// rather than allocated per attempt. A lookbehind in a searched
        /// pattern is tried at every position. The inner run is over before
        /// the outer one goes on, so one set per nesting level serves all of
        /// them; a nested one hangs off the inner stack the same way.
        private int[] subRegs = NO_INTS;
        private Ints subStack;
        private Ints subTrail;

        int[] inner(int[] regs) {
            int[] r = subRegs;
            if (r.length != regs.length) {
                r = new int[regs.length];
                subRegs = r;
            }
            System.arraycopy(regs, 0, r, 0, regs.length);
            return r;
        }

        Ints subStack() {
            Ints x = subStack;
            if (x == null) {
                x = new Ints();
                subStack = x;
            }
            return x;
        }

        Ints subTrail() {
            Ints x = subTrail;
            if (x == null) {
                x = new Ints();
                subTrail = x;
            }
            return x;
        }

        /// The array with room for `need` entries in all; grown when short.
        int[] room(int need) {
            int[] b = a;
            if (need > b.length) {
                int cap = b.length == 0 ? 24 : b.length * 2;
                b = new int[cap < need ? need : cap];
                System.arraycopy(a, 0, b, 0, n);
                a = b;
            }
            return b;
        }
    }

    private static final int FRAME = 6;

    // A choice frame is six ints and a trail entry two, written at once after
    // one capacity check: one call per push made these the engine's hottest
    // lines, several per character a repeat gives back.
    private static void choice(Ints st, int kind, int x, int y, int z, int w, int v) {
        int n = st.n;
        int[] a = st.room(n + FRAME);
        a[n] = kind;
        a[n + 1] = x;
        a[n + 2] = y;
        a[n + 3] = z;
        a[n + 4] = w;
        a[n + 5] = v;
        st.n = n + FRAME;
    }

    private static void log(Ints trail, int[] regs, int r) {
        int n = trail.n;
        int[] a = trail.room(n + 2);
        a[n] = r;
        a[n + 1] = regs[r];
        trail.n = n + 2;
    }

    /// Runs `prog` from `start`; the end position, or -1.
    private static int exec(Op[] prog, Text t, int start, int endpos, int[] regs, boolean full, int forbid, int li,
            Ints stack, Ints trail) {
        // One pair of stacks serves every start position a search tries;
        // they are emptied rather than allocated per attempt.
        stack.n = 0;
        trail.n = 0;
        int pc = 0;
        int pos = start;
        while (true) {
            Op op = prog[pc];
            int k = op.k;
            boolean ok = true;
            switch (k) {
                case CHAR:
                    if (pos < endpos && t.at(pos) == op.c) {
                        pos++;
                        pc++;
                        continue;
                    }
                    ok = false;
                    break;
                case CHARI:
                case ANY:
                case ANYALL:
                case SET:
                    if (pos < endpos && one(op, t.at(pos))) {
                        pos++;
                        pc++;
                        continue;
                    }
                    ok = false;
                    break;
                case SPLIT:
                    choice(stack, 0, op.b, pos, trail.n, 0, 0);
                    pc = op.a;
                    continue;
                case JMP:
                    pc = op.a;
                    continue;
                case SAVE: {
                    int n = op.a;
                    log(trail, regs, n);
                    regs[n] = pos;
                    if ((n & 1) != 0) {
                        log(trail, regs, li);
                        regs[li] = n >> 1;
                    }
                    pc++;
                    continue;
                }
                case REPEAT_ONE: {
                    int limit = op.hi < 0 ? endpos : Math.min(endpos, pos + op.hi);
                    int j = span(op.one, t, pos, limit);
                    if (j - pos < op.lo) {
                        ok = false;
                        break;
                    }
                    if (j > pos + op.lo) {
                        choice(stack, 1, pc + 1, pos + op.lo, j, trail.n, 0);
                    }
                    pos = j;
                    pc++;
                    continue;
                }
                case MIN_REPEAT_ONE: {
                    int j = pos;
                    while (j < pos + op.lo) {
                        if (j >= endpos || !one(op.one, t.at(j))) {
                            break;
                        }
                        j++;
                    }
                    if (j < pos + op.lo) {
                        ok = false;
                        break;
                    }
                    // The repeat's own pc names its single-character test
                    // and its upper bound when the choice is resumed.
                    choice(stack, 2, pc + 1, j, op.lo, trail.n, pc);
                    pos = j;
                    pc++;
                    continue;
                }
                case AT:
                    if (at(op.at, t, pos, endpos, op.unicode)) {
                        pc++;
                        continue;
                    }
                    ok = false;
                    break;
                case MATCH:
                    if (full && pos != endpos || forbid >= 0 && pos == start && start == forbid) {
                        ok = false;
                        break;
                    }
                    return pos;
                case MARK: {
                    int n = li + 1 + op.a;
                    log(trail, regs, n);
                    regs[n] = pos;
                    pc++;
                    continue;
                }
                case PROGRESS:
                    pc = regs[li + 1 + op.a] == pos ? op.b : pc + 1;
                    continue;
                case BACKREF: {
                    int s = regs[2 * op.a];
                    int e = regs[2 * op.a + 1];
                    if (s < 0 || e < 0) {
                        ok = false;
                        break;
                    }
                    int n = e - s;
                    if (pos + n <= endpos && same(t, s, pos, n, op.icase)) {
                        pos += n;
                        pc++;
                        continue;
                    }
                    ok = false;
                    break;
                }
                case GROUPREF_EXISTS:
                    pc = regs[2 * op.a] >= 0 && regs[2 * op.a + 1] >= 0 ? pc + 1 : op.b;
                    continue;
                case LOOK: {
                    if (op.simple) {
                        if (lookHit(op, t, pos, endpos) != op.neg) {
                            pc++;
                            continue;
                        }
                        ok = false;
                        break;
                    }
                    int[] inner = stack.inner(regs);
                    int end;
                    if (op.behind) {
                        int s = pos - op.width;
                        end = s < 0 ? -1 : exec(op.sub, t, s, pos, inner, true, -1, li, stack.subStack(), stack.subTrail());
                    } else {
                        end = exec(op.sub, t, pos, endpos, inner, false, -1, li, stack.subStack(), stack.subTrail());
                    }
                    if (op.neg) {
                        if (end < 0) {
                            pc++;
                            continue;
                        }
                        ok = false;
                        break;
                    }
                    if (end < 0) {
                        ok = false;
                        break;
                    }
                    adopt(regs, inner, trail);
                    pc++;
                    continue;
                }
                case ATOMIC: {
                    int[] inner = stack.inner(regs);
                    int end = exec(op.sub, t, pos, endpos, inner, false, -1, li, stack.subStack(), stack.subTrail());
                    if (end < 0) {
                        ok = false;
                        break;
                    }
                    adopt(regs, inner, trail);
                    pos = end;
                    pc++;
                    continue;
                }
                default:
                    break;
            }
            if (ok) {
                throw Py.runtimeError("internal error: opcode " + k);
            }
            // Backtrack to the most recent choice point.
            int tl = 0;
            boolean resumed = false;
            while (!resumed) {
                if (stack.n == 0) {
                    return -1;
                }
                stack.n -= FRAME;
                int[] e = stack.a;
                int f = stack.n;
                int kind = e[f];
                if (kind == 0) {
                    pc = e[f + 1];
                    pos = e[f + 2];
                    tl = e[f + 3];
                    resumed = true;
                } else if (kind == 1) {
                    int nxt = e[f + 1];
                    int minpos = e[f + 2];
                    int cur = e[f + 3] - 1;
                    tl = e[f + 4];
                    // Only stop where the next literal could match.
                    Op nop = prog[nxt];
                    if (nop.k == CHAR) {
                        int c = nop.c;
                        while (cur >= minpos && (cur >= endpos || t.at(cur) != c)) {
                            cur--;
                        }
                    }
                    if (cur >= minpos) {
                        if (cur > minpos) {
                            choice(stack, 1, nxt, minpos, cur, tl, 0);
                        }
                        pc = nxt;
                        pos = cur;
                        resumed = true;
                    }
                } else {
                    int nxt = e[f + 1];
                    int cur = e[f + 2];
                    int count = e[f + 3];
                    tl = e[f + 4];
                    Op rep = prog[e[f + 5]];
                    if (!(rep.hi >= 0 && count >= rep.hi || cur >= endpos || !one(rep.one, t.at(cur)))) {
                        choice(stack, 2, nxt, cur + 1, count + 1, tl, e[f + 5]);
                        pc = nxt;
                        pos = cur + 1;
                        resumed = true;
                    }
                }
            }
            while (trail.n > tl) {
                trail.n -= 2;
                regs[trail.a[trail.n]] = trail.a[trail.n + 1];
            }
        }
    }

    /// `text[a:a+n] == text[b:b+n]`, or the two lowered when `icase`.
    private static boolean same(Text t, int a, int b, int n, boolean icase) {
        boolean eq = true;
        for (int i = 0; i < n; i++) {
            if (t.at(a + i) != t.at(b + i)) {
                eq = false;
                break;
            }
        }
        if (eq || !icase) {
            return eq;
        }
        return TypeMethods.mapCase(slice(t, a, n), 0).equals(TypeMethods.mapCase(slice(t, b, n), 0));
    }

    private static String slice(Text t, int a, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(str(t.at(a + i)));
        }
        return sb.toString();
    }

    private static void adopt(int[] regs, int[] inner, Ints trail) {
        for (int n = 0; n < regs.length; n++) {
            if (regs[n] != inner[n]) {
                log(trail, regs, n);
                regs[n] = inner[n];
            }
        }
    }

    /// `prefix` in `t` at or after `start`, before `endpos`; or -1.
    private static int find(Text t, String prefix, int start, int endpos) {
        if (t.cps == null) {
            int f = t.s.indexOf(prefix, start);
            return f >= 0 && f + prefix.length() <= endpos ? f : -1;
        }
        int[] p = new int[prefix.codePointCount(0, prefix.length())];
        int at = 0;
        int k = 0;
        while (at < prefix.length()) {
            int c = prefix.codePointAt(at);
            p[k++] = c;
            at += c >= 0x10000 ? 2 : 1;
        }
        int m = p.length;
        for (int i = start; i + m <= endpos; i++) {
            boolean hit = true;
            for (int j = 0; j < m; j++) {
                if (t.at(i + j) != p[j]) {
                    hit = false;
                    break;
                }
            }
            if (hit) {
                return i;
            }
        }
        return -1;
    }

    /// The first position at or after `start` whose character one of
    /// `first` matches, or -1: every match consumes a character first.
    private static int skip(Op[] first, Prog p, Text t, int start, int endpos) {
        if (first.length == 1 && t.cps == null && first[0].k == CHAR && endpos == t.s.length()) {
            return t.s.indexOf(first[0].c, start);
        }
        if (t.cps == null) {
            // One merged table for every class the match can begin with,
            // and the assertions (`(\s|^)`) asked only where it says no.
            boolean[] f = p.firstAscii;
            boolean ats = p.firstAt;
            String s = t.s;
            for (int i = start; i <= endpos; i++) {
                if (i < endpos) {
                    char c = s.charAt(i);
                    if (c < 128 ? f[c] : anyFirst(first, c)) {
                        return i;
                    }
                } else if (!ats) {
                    return -1;
                }
                if (ats) {
                    for (Op op : first) {
                        if (op.k == AT && at(op.at, t, i, endpos, op.unicode)) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }
        for (int i = start; i <= endpos; i++) {
            int c = i < endpos ? t.at(i) : -1;
            for (Op op : first) {
                if (op.k == AT) {
                    if (at(op.at, t, i, endpos, op.unicode)) {
                        return i;
                    }
                } else if (c >= 0 && (op.k == CHAR ? c == op.c : one(op, c))) {
                    return i;
                }
            }
        }
        return -1;
    }

    /// Whether a char-consuming op of `first` matches the non-ASCII `c`.
    private static boolean anyFirst(Op[] first, int c) {
        for (Op op : first) {
            if (op.k != AT && one(op, c)) {
                return true;
            }
        }
        return false;
    }

    /// `_engine.run`: mode 0 matches, 1 fully matches, 2 searches. The
    /// group registers then `lastindex`, or `NO_MATCH`.
    static int[] run(Prog p, String text, int pos, int endpos, int mode, int forbid) {
        return run(p, text, -1, pos, endpos, mode, forbid);
    }

    /// [#run] for a text whose surrogate pair count the caller already took.
    static int[] run(Prog p, String text, int pairs, int pos, int endpos, int mode, int forbid) {
        Scratch sc = acquire(p);
        try {
            sc.text.reset(text, pairs);
            return run(p, sc, text, pos, endpos, mode, forbid);
        } finally {
            release(p, sc);
        }
    }

    /// The pattern's scratch, held by one loop over one text: a loop that
    /// runs once per match keeps it, so the text is prepared once.
    private static Scratch acquire(Prog p) {
        Scratch sc = p.scratch;
        p.scratch = null;
        return sc == null ? new Scratch() : sc;
    }

    private static void release(Prog p, Scratch sc) {
        sc.text.s = null;
        sc.text.cps = null;
        p.scratch = sc;
    }

    /// `pattern.subn(template, text, count)` with the loop in Java: `template`
    /// is a list of literal strings and group numbers, as re's
    /// `_compile_template` makes it. No Match object is made per match --
    /// the Python loop made one, and a generator step, for each.
    static Object subn(Prog p, String text, PyList template, int count) {
        Scratch sc = acquire(p);
        try {
            // Counted once here; the runs below find the text already set.
            Text tx = sc.text;
            tx.reset(text);
            int n = tx.cps == null ? text.length() : tx.cps.length;
            int[] regs = count >= 0 ? run(p, sc, text, 0, n, 2, -1) : new int[0];
            if (regs.length == 0) {
                return Py.tuple(text, Long.valueOf(0));
            }
            return subnLoop(p, sc, text, n, regs, template, count);
        } finally {
            release(p, sc);
        }
    }

    /// The replacing loop, outside the caller's try/finally: javac parks a
    /// value returned from inside a try in a spare local, and when that slot
    /// is the builder's the translator rightly keeps the builder on the
    /// heap, where every append takes its lock.
    private static Object subnLoop(Prog p, Scratch sc, String text, int n, int[] first, PyList template, int count) {
        int[] regs = first;
        // Made once, after the first match, and never reassigned: ParparVM
        // keeps a builder on the stack only when its local holds nothing
        // else and only StringBuilder methods receive it (no helper), and one
        // on the heap pays a lock on every append. A builder also keeps a
        // Latin-1 text Latin-1, which a char array would widen. Without
        // surrogates a code point index is the char index, so the pieces are
        // appended straight from the text instead of as substrings.
        StringBuilder out = new StringBuilder(text.length() + 16);
        boolean plain = n == text.length();
        int parts = template.size();
        int last = 0;
        int done = 0;
        while (true) {
            int s = regs[0];
            int e = regs[1];
            if (plain) {
                out.append(text, last, s);
            } else {
                out.append(cut(sc.text, last, s));
            }
            for (int k = 0; k < parts; k++) {
                Object part = template.get(k);
                if (part instanceof String) {
                    out.append((String) part);
                } else {
                    int g = (int) Py.index(part);
                    int gs = regs[2 * g];
                    int ge = regs[2 * g + 1];
                    if (gs >= 0 && ge >= 0) {
                        if (plain) {
                            out.append(text, gs, ge);
                        } else {
                            out.append(cut(sc.text, gs, ge));
                        }
                    }
                }
            }
            last = e;
            done++;
            if (e > n || count != 0 && done >= count) {
                break;
            }
            regs = run(p, sc, text, e, n, 2, s == e ? e : -1);
            if (regs.length == 0) {
                break;
            }
        }
        if (plain) {
            out.append(text, last, n);
        } else {
            out.append(cut(sc.text, last, n));
        }
        return Py.tuple(out.toString(), Long.valueOf(done));
    }

    /// `pattern.subn(fn, text, count)` for a callable `fn`, with the loop in
    /// Java: one Match per match handed to `fn`, as sre does, and no generator
    /// step, piece list or join around it. markdownify indents every quoted
    /// line through `re_line_with_content.sub(fn, text)`. As in sre, `fn`
    /// answering None adds nothing, and anything but a str is a TypeError.
    static Object subCall(Prog p, Object pattern, String text, Object fn, int count) {
        return subCall(p, pattern, null, text, fn, count);
    }

    /// [#subCall] over a bytes subject's Latin-1 `text`: the callable gets
    /// bytes matches and returns bytes.
    static Object subCall(Prog p, Object pattern, PyBytes subject, String text, Object fn, int count) {
        Scratch sc = acquire(p);
        try {
            // Counted once here; the runs below find the text already set.
            Text tx = sc.text;
            tx.reset(text);
            int n = tx.cps == null ? text.length() : tx.cps.length;
            int[] regs = count >= 0 ? run(p, sc, text, 0, n, 2, -1) : new int[0];
            if (regs.length == 0) {
                return Py.tuple(text, Long.valueOf(0));
            }
            return subCallLoop(p, sc, pattern, subject, text, n, regs, fn, count);
        } finally {
            release(p, sc);
        }
    }

    /// The replacing loop, outside the caller's try/finally: javac parks a
    /// value returned from inside a try in a spare local, and when that slot
    /// is the builder's the translator rightly keeps the builder on the
    /// heap, where every append takes its lock.
    private static Object subCallLoop(Prog p, Scratch sc, Object pattern, PyBytes subject, String text, int n, int[] first,
            Object fn, int count) {
        int[] regs = first;
        StringBuilder out = new StringBuilder(text.length() + 16);
        boolean plain = n == text.length();
        PyObject f = Py.obj(fn);
        int last = 0;
        int done = 0;
        while (true) {
            int s = regs[0];
            int e = regs[1];
            if (plain) {
                out.append(text, last, s);
            } else {
                out.append(cut(sc.text, last, s));
            }
            // The callable may run this same pattern; it then builds its
            // own scratch, since this one is taken.
            Object r = f.call1(new RegexMatch(pattern, subject != null ? subject : text, text, subject != null, 0, n,
                    p.groups, regs));
            if (subject != null) {
                if (!(r instanceof PyBytes)) {
                    throw Py.typeError("expected a bytes-like object, " + Py.typeOf(r).name + " found");
                }
                byte[] v = ((PyBytes) r).value;
                out.append(Codecs.latin1(v, 0, v.length));
            } else if (r instanceof String) {
                out.append((String) r);
            } else if (r != Py.NONE) { //NOPMD CompareObjectsWithEquals - None is a singleton
                throw Py.typeError("expected str instance, " + Py.typeOf(r).name + " found");
            }
            last = e;
            done++;
            if (e > n || count != 0 && done >= count) {
                break;
            }
            regs = run(p, sc, text, e, n, 2, s == e ? e : -1);
            if (regs.length == 0) {
                break;
            }
        }
        if (plain) {
            out.append(text, last, n);
        } else {
            out.append(cut(sc.text, last, n));
        }
        return Py.tuple(out.toString(), Long.valueOf(done));
    }

    /// `pattern.findall(text, pos, endpos)` with the loop in Java: the whole
    /// match, the one group, or a tuple of groups, an unmatched group as "".
    static Object findall(Prog p, String text, int pos, int endpos) {
        Scratch sc = acquire(p);
        try {
            PyList out = Py.list();
            int at = pos;
            int forbid = -1;
            while (at <= endpos) {
                int[] regs = run(p, sc, text, at, endpos, 2, forbid);
                if (regs.length == 0) {
                    break;
                }
                if (p.groups == 0) {
                    out.append(cut(sc.text, regs[0], regs[1]));
                } else if (p.groups == 1) {
                    out.append(group(sc.text, regs, 1));
                } else {
                    Object[] g = new Object[p.groups];
                    for (int k = 0; k < g.length; k++) {
                        g[k] = group(sc.text, regs, k + 1);
                    }
                    out.append(Py.tuple(g));
                }
                int s = regs[0];
                int e = regs[1];
                forbid = s == e ? e : -1;
                at = e;
            }
            return out;
        } finally {
            release(p, sc);
        }
    }

    /// `pattern.split(text, maxsplit)` for a str, with the loop in Java as
    /// sre's is C: no generator step and no Match per piece. An unmatched
    /// group is None here, where findall makes it "".
    static Object split(Prog p, String text, int maxsplit) {
        PyList out = Py.list();
        if (maxsplit < 0) {
            out.append(text);
            return out;
        }
        Scratch sc = acquire(p);
        try {
            Text t = sc.text;
            t.reset(text);
            int endpos = t.cps == null ? text.length() : t.cps.length;
            int at = 0;
            int last = 0;
            int forbid = -1;
            int n = 0;
            while (at <= endpos && (maxsplit == 0 || n < maxsplit)) {
                int[] regs = run(p, sc, text, at, endpos, 2, forbid);
                if (regs.length == 0) {
                    break;
                }
                int st = regs[0];
                int e = regs[1];
                out.append(cut(t, last, st));
                for (int k = 1; k <= p.groups; k++) {
                    int gs = regs[2 * k];
                    int ge = regs[2 * k + 1];
                    out.append(gs < 0 || ge < 0 ? Py.NONE : cut(t, gs, ge));
                }
                last = e;
                forbid = st == e ? e : -1;
                at = e;
                n++;
            }
            out.append(cut(t, last, endpos));
            return out;
        } finally {
            release(p, sc);
        }
    }

    private static String group(Text t, int[] regs, int g) {
        int s = regs[2 * g];
        int e = regs[2 * g + 1];
        return s < 0 || e < 0 ? "" : cut(t, s, e);
    }

    /// Code points `from` to `to` of the run's text. The text was measured
    /// when it was set (`cps` is null when it has no surrogate pair), so a
    /// piece is a plain substring; PyStr.substr counted the pairs again for
    /// every piece.
    private static String cut(Text t, int from, int to) {
        return t.cps == null ? t.s.substring(from, to) : PyStr.substr(t.s, from, to);
    }

    private static int[] run(Prog p, Scratch sc, String text, int pos, int endpos, int mode, int forbid) {
        Text t = sc.text;
        t.reset(text);
        int size = 2 * (p.groups + 1);
        boolean full = mode == 1;
        int start = pos;
        int need = size + 1 + p.marks;
        if (sc.regs.length != need) {
            sc.regs = new int[need];
        }
        int[] regs = sc.regs;
        Ints stack = sc.stack;
        Ints trail = sc.trail;
        while (start <= endpos) {
            if (mode == 2 && p.prefix != null) {
                int found = find(t, p.prefix, start, endpos);
                if (found < 0) {
                    return NO_MATCH;
                }
                start = found;
            } else if (mode == 2 && p.first != null) {
                start = skip(p.first, p, t, start, endpos);
                if (start < 0) {
                    return NO_MATCH;
                }
            }
            java.util.Arrays.fill(regs, -1);
            int end = exec(p.ops, t, start, endpos, regs, full, forbid, size, stack, trail);
            if (end >= 0) {
                regs[0] = start;
                regs[1] = end;
                int[] out = new int[size + 1];
                System.arraycopy(regs, 0, out, 0, size + 1);
                return out;
            }
            if (mode != 2 || p.anchored) {
                return NO_MATCH;
            }
            start++;
        }
        return NO_MATCH;
    }
}
