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
package com.codename1.spreadsheet;

/// A condition as COUNTIF, SUMIFS and the database functions take it: a value to equal, or
/// text that starts with a comparison (`">=10"`, `"<>done"`), where `*` and `?` in text are
/// wildcards and `~` takes the special meaning off the character after it.
final class Criteria {
    private static final int EQ = 0;
    private static final int NE = 1;
    private static final int LT = 2;
    private static final int GT = 3;
    private static final int LE = 4;
    private static final int GE = 5;

    private final int op;
    /// A Double, a Boolean, a String pattern, or null to match empty cells.
    private final Object target;

    private boolean explicitEquals;

    Criteria(Object criterion) {
        if (criterion instanceof CellError) {
            throw EvalError.of((CellError) criterion);
        }
        if (criterion == null || criterion == Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            op = EQ;
            target = Double.valueOf(0);
            return;
        }
        if (!(criterion instanceof String)) {
            op = EQ;
            target = criterion;
            return;
        }
        String s = (String) criterion;
        int o = EQ;
        int skip = 0;
        if (s.startsWith(">=")) {
            o = GE;
            skip = 2;
        } else if (s.startsWith("<=")) {
            o = LE;
            skip = 2;
        } else if (s.startsWith("<>")) {
            o = NE;
            skip = 2;
        } else if (s.startsWith(">")) {
            o = GT;
            skip = 1;
        } else if (s.startsWith("<")) {
            o = LT;
            skip = 1;
        } else if (s.startsWith("=")) {
            skip = 1;
        }
        op = o;
        explicitEquals = o == EQ && skip == 1;
        String rest = s.substring(skip);
        if (rest.length() == 0) {
            // "" and "=" match empty cells, "<>" the cells that hold something
            target = null;
            return;
        }
        Double number = Values.parseNumber(rest);
        CellError error = rest.charAt(0) == '#' ? CellError.valueOf(rest) : null;
        if (error != null) {
            // "#N/A" as a condition counts the cells that hold that error
            target = error;
        } else if (number != null) {
            target = number;
        } else if ("TRUE".equalsIgnoreCase(rest)) {
            target = Boolean.TRUE;
        } else if ("FALSE".equalsIgnoreCase(rest)) {
            target = Boolean.FALSE;
        } else {
            target = rest;
        }
    }

    boolean matches(Object v) {
        if (target instanceof CellError) {
            return target.equals(v) ? op == EQ : op == NE;
        }
        if (v instanceof CellError) {
            return false;
        }
        if (target == null) {
            // "=" is a cell with nothing in it, "<>" one with anything, the empty text of
            // ="" included; "" alone takes both kinds of empty
            if (op == NE) {
                return v != null;
            }
            return explicitEquals ? v == null : v == null || "".equals(v);
        }
        if (target instanceof Double) {
            double t = ((Double) target).doubleValue();
            double x;
            if (v instanceof Double) {
                x = ((Double) v).doubleValue();
            } else if (v instanceof String && (op == EQ || op == NE)) {
                // text that reads as the number is equal to it, and so is not unequal to it:
                // the two tests are each other's opposite
                Double parsed = Values.parseNumber((String) v);
                if (parsed == null) {
                    return op == NE;
                }
                x = parsed.doubleValue();
            } else {
                return op == NE;
            }
            return test(x < t ? -1 : x > t ? 1 : 0);
        }
        if (target instanceof Boolean) {
            if (!(v instanceof Boolean)) {
                return op == NE;
            }
            return test(Values.compare(v, target));
        }
        if (!(v instanceof String)) {
            // an empty cell is not text, and neither is a number
            return op == NE;
        }
        String text = (String) v;
        String pattern = (String) target;
        if (op == EQ || op == NE) {
            boolean hit = wildcardMatch(pattern, 0, text, 0);
            return op == EQ ? hit : !hit;
        }
        return test(Values.compareText(text, pattern));
    }

    private boolean test(int cmp) {
        switch (op) {
            case EQ:
                return cmp == 0;
            case NE:
                return cmp != 0;
            case LT:
                return cmp < 0;
            case GT:
                return cmp > 0;
            case LE:
                return cmp <= 0;
            default:
                return cmp >= 0;
        }
    }

    static boolean hasWildcards(String pattern) {
        return pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0 || pattern.indexOf('~') >= 0;
    }

    /// Matches text against a pattern with `*`, `?` and `~`, ignoring case.
    static boolean wildcardMatch(String pattern, int p, String text, int t) {
        int pn = pattern.length();
        int tn = text.length();
        int starP = -1;
        int starT = -1;
        while (t < tn) {
            if (p < pn) {
                char c = pattern.charAt(p);
                if (c == '*') {
                    starP = p++;
                    starT = t;
                    continue;
                }
                boolean literal = false;
                if (c == '~' && p + 1 < pn) {
                    c = pattern.charAt(++p);
                    literal = true;
                }
                if (!literal && c == '?'
                        || Character.toUpperCase(c) == Character.toUpperCase(text.charAt(t))) {
                    p++;
                    t++;
                    continue;
                }
                if (literal) {
                    p--;
                }
            }
            if (starP < 0) {
                return false;
            }
            p = starP + 1;
            t = ++starT;
        }
        while (p < pn && pattern.charAt(p) == '*') {
            p++;
        }
        return p == pn;
    }

    /// The cells of the ranges past the rows and columns that hold data, when an empty cell
    /// passes every condition: `COUNTIF(A:A, "")` counts the empty cells of the whole column,
    /// as Excel does, though the mask covers only the cells that hold data. 0 otherwise.
    static double blanksBeyondData(Evaluator ev, Object[] args, int from, int to) {
        long full = -1;
        long filled = -1;
        for (int i = from; i + 1 < to; i += 2) {
            if (!(args[i] instanceof Ref) || !new Criteria(ev.scalar(args[i + 1])).matches(null)) {
                return 0;
            }
            Ref whole = (Ref) args[i];
            Ref data = whole.clip();
            long w = (long) whole.rows() * whole.cols();
            long d = (long) data.rows() * data.cols();
            if (full >= 0 && (w != full || d != filled)) {
                return 0;
            }
            full = w;
            filled = d;
        }
        return full < 0 ? 0 : full - filled;
    }

    /// The cells that pass every condition of a list of range and criterion pairs, as rows
    /// and columns of the first range. Throws `#VALUE!` when the ranges differ in shape.
    static boolean[][] mask(Evaluator ev, Object[] args, int from, int to) {
        if (to > args.length || to - from < 2) {
            throw EvalError.VALUE;
        }
        boolean[][] mask = null;
        for (int i = from; i + 1 < to; i += 2) {
            Object[][] m = ev.matrix(args[i]);
            Criteria criteria = new Criteria(ev.scalar(args[i + 1]));
            if (mask == null) {
                mask = new boolean[m.length][Evaluator.width(m)];
                for (int r = 0; r < mask.length; r++) {
                    for (int c = 0; c < mask[r].length; c++) {
                        mask[r][c] = true;
                    }
                }
            } else if (m.length != mask.length || Evaluator.width(m) != Evaluator.width2(mask)) {
                throw EvalError.VALUE;
            }
            for (int r = 0; r < mask.length; r++) {
                for (int c = 0; c < mask[r].length; c++) {
                    if (mask[r][c] && !criteria.matches(m[r][c])) {
                        mask[r][c] = false;
                    }
                }
            }
        }
        if (mask == null) {
            throw EvalError.VALUE;
        }
        return mask;
    }
}
