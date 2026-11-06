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
        int width;
        /// LOOK, ATOMIC, BACKREF: the sub-program, or the case flag.
        Op[] sub;
        boolean icase;

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
        /// The text view, registers and backtracking stacks of the last run,
        /// handed back for the next one: a route table or a tokenizer runs
        /// one pattern per request, and these were most of what a run
        /// allocated. Taken with `getAndSet(null)`, so two threads running
        /// one pattern at once never share them; the loser builds its own.
        final java.util.concurrent.atomic.AtomicReference<Scratch> scratch =
                new java.util.concurrent.atomic.AtomicReference<Scratch>();

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
            return p;
        }));
        m.setAttr("run", new RunFn());
        return m;
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
            this.s = s;
            int n = s.codePointCount(0, s.length());
            if (n == s.length()) {
                cps = null;
            } else {
                cps = new int[n];
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

    private static boolean one(Op op, int c) {
        switch (op.k) {
            case CHAR:
                return c == op.c;
            case CHARI:
                return c == op.c || c == op.c2 || TypeMethods.mapCase(str(c), 0).equals(op.lower);
            case ANY:
                return c != '\n';
            case ANYALL:
                return true;
            default:
                break;
        }
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

    // ---- the machine -----------------------------------------------------

    /// A growable int stack: the trail holds (register, old value) pairs and
    /// the choice points six ints each.
    private static final class Ints {
        int[] a = NO_INTS;
        int n;

        void push(int v) {
            if (n == a.length) {
                int[] b = new int[n == 0 ? 24 : n * 2];
                System.arraycopy(a, 0, b, 0, n);
                a = b;
            }
            a[n++] = v;
        }
    }

    private static final int FRAME = 6;

    private static void choice(Ints st, int kind, int x, int y, int z, int w, int v) {
        st.push(kind);
        st.push(x);
        st.push(y);
        st.push(z);
        st.push(w);
        st.push(v);
    }

    private static void log(Ints trail, int[] regs, int n) {
        trail.push(n);
        trail.push(regs[n]);
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
                    int j = pos;
                    while (j < limit && one(op.one, t.at(j))) {
                        j++;
                    }
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
                    int[] inner = copy(regs);
                    int end;
                    if (op.behind) {
                        int s = pos - op.width;
                        end = s < 0 ? -1 : exec(op.sub, t, s, pos, inner, true, -1, li, new Ints(), new Ints());
                    } else {
                        end = exec(op.sub, t, pos, endpos, inner, false, -1, li, new Ints(), new Ints());
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
                    int[] inner = copy(regs);
                    int end = exec(op.sub, t, pos, endpos, inner, false, -1, li, new Ints(), new Ints());
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

    private static int[] copy(int[] regs) {
        int[] r = new int[regs.length];
        System.arraycopy(regs, 0, r, 0, regs.length);
        return r;
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

    /// `_engine.run`: mode 0 matches, 1 fully matches, 2 searches. The
    /// group registers then `lastindex`, or `NO_MATCH`.
    static int[] run(Prog p, String text, int pos, int endpos, int mode, int forbid) {
        Scratch sc = p.scratch.getAndSet(null);
        if (sc == null) {
            sc = new Scratch();
        }
        try {
            return run(p, sc, text, pos, endpos, mode, forbid);
        } finally {
            sc.text.s = null;
            sc.text.cps = null;
            p.scratch.set(sc);
        }
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
