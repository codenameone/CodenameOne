/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.luau;

/// Luau string patterns: character classes, sets, the four quantifiers,
/// anchors, captures, position captures, back references, `%b` and `%f`.
final class LuaPattern {
    private static final int MAX_CAPTURES = 32;
    private static final int CAP_POSITION = -2;
    private static final int CAP_UNFINISHED = -1;
    private static final int MAX_DEPTH = 200;

    final String src;
    final String pat;
    int level;
    final int[] capStart = new int[MAX_CAPTURES];
    final int[] capLen = new int[MAX_CAPTURES];
    private int depth;
    private final Interp interp;
    private int pending;

    /// Hard cap on matcher steps per pattern operation when no interpreter
    /// budget applies. Depth alone does not bound backtracking: a pattern of
    /// thirty `a*` against forty `a`s stays shallow and is exponential.
    static final long MAX_MATCH_STEPS = 10000000L;
    private long matchSteps;

    LuaPattern(String src, String pat) {
        this(null, src, pat);
    }

    LuaPattern(Interp interp, String src, String pat) {
        this.interp = interp;
        this.src = src;
        this.pat = pat;
    }

    /// Counts one matcher step, charging the interpreter budget in batches
    /// of 256 so the per-step cost stays a counter increment.
    private void step() {
        if (++matchSteps > MAX_MATCH_STEPS) { // NOPMD AssignmentInOperand - step counter
            throw LuaError.rt("pattern too complex");
        }
        if (interp != null && ++pending >= 256) { // NOPMD AssignmentInOperand - batched charge
            interp.charge(pending);
            pending = 0;
        }
    }

    /// Tries to match at `s`; returns the end index or -1.
    int match(int s, int p) {
        step();
        if (++depth > MAX_DEPTH) { // NOPMD AssignmentInOperand - depth counter and scan loop
            throw LuaError.rt("pattern too complex");
        }
        try {
            while (true) {
                if (p == pat.length()) {
                    return s;
                }
                char pc = pat.charAt(p);
                if (pc == '(') {
                    if (p + 1 < pat.length() && pat.charAt(p + 1) == ')') {
                        return startCapture(s, p + 2, CAP_POSITION);
                    }
                    return startCapture(s, p + 1, CAP_UNFINISHED);
                }
                if (pc == ')') {
                    return endCapture(s, p + 1);
                }
                if (pc == '$' && p + 1 == pat.length()) {
                    return s == src.length() ? s : -1;
                }
                if (pc == '%' && p + 1 < pat.length()) {
                    char nx = pat.charAt(p + 1);
                    if (nx == 'b') {
                        s = matchBalance(s, p + 2);
                        if (s < 0) {
                            return -1;
                        }
                        p += 4;
                        continue;
                    }
                    if (nx == 'f') {
                        p += 2;
                        if (p >= pat.length() || pat.charAt(p) != '[') {
                            throw LuaError.rt("missing '[' after '%f' in pattern");
                        }
                        int ep = classEnd(p);
                        char prev = s == 0 ? '\0' : src.charAt(s - 1);
                        char cur = s < src.length() ? src.charAt(s) : '\0';
                        if (!matchClass(prev, p, ep - 1) && matchClass(cur, p, ep - 1)) {
                            p = ep;
                            continue;
                        }
                        return -1;
                    }
                    if (nx >= '0' && nx <= '9') {
                        s = matchCapture(s, nx);
                        if (s < 0) {
                            return -1;
                        }
                        p += 2;
                        continue;
                    }
                }
                int ep = classEnd(p);
                boolean m = s < src.length() && singleMatch(src.charAt(s), p, ep);
                char q = ep < pat.length() ? pat.charAt(ep) : '\0';
                if (q == '?') {
                    if (m) {
                        int r = match(s + 1, ep + 1);
                        if (r >= 0) {
                            return r;
                        }
                    }
                    p = ep + 1;
                    continue;
                }
                if (q == '+') {
                    return m ? maxExpand(s + 1, p, ep) : -1;
                }
                if (q == '*') {
                    return maxExpand(s, p, ep);
                }
                if (q == '-') {
                    return minExpand(s, p, ep);
                }
                if (!m) {
                    return -1;
                }
                s++;
                p = ep;
            }
        } finally {
            depth--;
        }
    }

    private int maxExpand(int s, int p, int ep) {
        int i = 0;
        while (s + i < src.length() && singleMatch(src.charAt(s + i), p, ep)) {
            i++;
        }
        while (i >= 0) {
            int r = match(s + i, ep + 1);
            if (r >= 0) {
                return r;
            }
            i--;
        }
        return -1;
    }

    private int minExpand(int s, int p, int ep) {
        while (true) {
            int r = match(s, ep + 1);
            if (r >= 0) {
                return r;
            }
            if (s < src.length() && singleMatch(src.charAt(s), p, ep)) {
                s++;
            } else {
                return -1;
            }
        }
    }

    private int startCapture(int s, int p, int what) {
        if (level >= MAX_CAPTURES) {
            throw LuaError.rt("too many captures");
        }
        capStart[level] = s;
        capLen[level] = what;
        level++;
        int r = match(s, p);
        if (r < 0) {
            level--;
        }
        return r;
    }

    private int endCapture(int s, int p) {
        int l = -1;
        for (int i = level - 1; i >= 0; i--) {
            if (capLen[i] == CAP_UNFINISHED) {
                l = i;
                break;
            }
        }
        if (l < 0) {
            throw LuaError.rt("invalid pattern capture");
        }
        capLen[l] = s - capStart[l];
        int r = match(s, p);
        if (r < 0) {
            capLen[l] = CAP_UNFINISHED;
        }
        return r;
    }

    private int matchBalance(int s, int p) {
        if (p + 1 >= pat.length()) {
            throw LuaError.rt("missing arguments to '%b'");
        }
        if (s >= src.length() || src.charAt(s) != pat.charAt(p)) {
            return -1;
        }
        char b = pat.charAt(p);
        char e = pat.charAt(p + 1);
        int cont = 1;
        int i = s + 1;
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == e) {
                if (--cont == 0) { // NOPMD AssignmentInOperand - depth counter and scan loop
                    return i + 1;
                }
            } else if (c == b) {
                cont++;
            }
            i++;
        }
        return -1;
    }

    private int matchCapture(int s, char d) {
        int l = d - '1';
        if (l < 0 || l >= level || capLen[l] == CAP_UNFINISHED) {
            throw LuaError.rt("invalid capture index %" + d);
        }
        int len = capLen[l];
        if (src.length() - s >= len && src.regionMatches(capStart[l], src, s, len)) {
            return s + len;
        }
        return -1;
    }

    private int classEnd(int p) {
        char c = pat.charAt(p++);
        if (c == '%') {
            if (p >= pat.length()) {
                throw LuaError.rt("malformed pattern (ends with '%')");
            }
            return p + 1;
        }
        if (c == '[') {
            if (p < pat.length() && pat.charAt(p) == '^') {
                p++;
            }
            do {
                if (p >= pat.length()) {
                    throw LuaError.rt("malformed pattern (missing ']')");
                }
                char cc = pat.charAt(p++);
                if (cc == '%') {
                    p++;
                }
            } while (p >= pat.length() || pat.charAt(p) != ']');
            return p + 1;
        }
        return p;
    }

    private boolean singleMatch(char c, int p, int ep) {
        char pc = pat.charAt(p);
        switch (pc) {
            case '.':
                return true;
            case '%':
                return classMatch(c, pat.charAt(p + 1));
            case '[':
                return matchClass(c, p, ep - 1);
            default:
                return pc == c;
        }
    }

    static boolean classMatch(char c, char cl) {
        boolean r;
        switch (lower(cl)) {
            case 'a':
                r = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
                break;
            case 'd':
                r = c >= '0' && c <= '9';
                break;
            case 'l':
                r = c >= 'a' && c <= 'z';
                break;
            case 's':
                r = c == ' ' || c >= 9 && c <= 13;
                break;
            case 'u':
                r = c >= 'A' && c <= 'Z';
                break;
            case 'w':
                r = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
                break;
            case 'x':
                r = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
                break;
            case 'p':
                r = c > 32 && c < 127 && !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9');
                break;
            case 'c':
                r = c < 32 || c == 127;
                break;
            case 'g':
                r = c > 32 && c < 127;
                break;
            default:
                return cl == c;
        }
        if (cl >= 'A' && cl <= 'Z') {
            return !r;
        }
        return r;
    }

    private static char lower(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
    }

    /// `p` at the `[`, `ec` at the closing `]`.
    private boolean matchClass(char c, int p, int ec) {
        boolean sig = true;
        p++;
        if (pat.charAt(p) == '^') {
            sig = false;
            p++;
        }
        while (p < ec) {
            char pc = pat.charAt(p);
            if (pc == '%' && p + 1 < ec) {
                p++;
                if (classMatch(c, pat.charAt(p))) {
                    return sig;
                }
                p++;
            } else if (p + 2 < ec && pat.charAt(p + 1) == '-') {
                if (pc <= c && c <= pat.charAt(p + 2)) {
                    return sig;
                }
                p += 3;
            } else {
                if (pc == c) {
                    return sig;
                }
                p++;
            }
        }
        return !sig;
    }

    /// Capture `i` as a Luau value; the whole match when there are none.
    Object capture(int i, int s, int e) {
        if (i >= level) {
            if (i == 0) {
                return src.substring(s, e);
            }
            throw LuaError.rt("invalid capture index %" + (i + 1));
        }
        if (capLen[i] == CAP_POSITION) {
            return Interp.num(capStart[i] + 1);
        }
        if (capLen[i] == CAP_UNFINISHED) {
            throw LuaError.rt("unfinished capture");
        }
        return src.substring(capStart[i], capStart[i] + capLen[i]);
    }

    Object[] captures(int s, int e, boolean wholeIfNone) {
        int n = level == 0 && wholeIfNone ? 1 : level;
        Object[] r = new Object[n];
        for (int i = 0; i < n; i++) {
            r[i] = capture(i, s, e);
        }
        return r;
    }
}
