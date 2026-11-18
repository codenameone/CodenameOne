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

import java.util.ArrayList;

/// The functions that answer an array: filtering, sorting and reshaping. A cell shows the
/// first value of such a result, and a function it is handed to receives all of it.
final class ArrayFunctions {
    static final String RANGE_NAMES = "CHOOSECOLS CHOOSEROWS DROP EXPAND FILTER HSTACK RANDARRAY SEQUENCE SORT SORTBY"
            + " TAKE TOCOL TOROW UNIQUE VSTACK WRAPCOLS WRAPROWS";

    /// The most cells a generated array may have.
    private static final int MAX_CELLS = 1 << 20;

    private ArrayFunctions() {
    }

    private static Object[][] make(int rows, int cols) {
        if (rows < 1 || cols < 1 || (long) rows * cols > MAX_CELLS) {
            throw EvalError.VALUE;
        }
        return new Object[rows][cols];
    }

    private static Object[][] rows(Object[][] m, ArrayList<Integer> picked) {
        if (picked.isEmpty()) {
            throw EvalError.VALUE;
        }
        Object[][] out = new Object[picked.size()][];
        for (int i = 0; i < out.length; i++) {
            out[i] = m[picked.get(i).intValue()];
        }
        return out;
    }

    private static Object[][] transpose(Object[][] m) {
        int w = Evaluator.width(m);
        Object[][] out = new Object[w][m.length];
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < w; c++) {
                out[c][r] = m[r][c];
            }
        }
        return out;
    }

    /// Sorts row numbers by keys, keeping the order of rows that compare equal.
    private static void sort(Integer[] order, Object[][][] keys, int[] directions) {
        for (int i = 1; i < order.length; i++) {
            Integer moving = order[i];
            int j = i - 1;
            while (j >= 0 && compareRows(order[j].intValue(), moving.intValue(), keys, directions) > 0) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = moving;
        }
    }

    private static int compareRows(int x, int y, Object[][][] keys, int[] directions) {
        for (int k = 0; k < keys.length; k++) {
            Object p = keys[k][x][0];
            Object q = keys[k][y][0];
            int cmp;
            if (p == null || q == null) {
                // empty cells go last whichever way the rest is sorted
                if (p == q) { //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
                    continue;
                }
                return p == null ? 1 : -1;
            }
            cmp = Values.compare(p, q);
            if (cmp != 0) {
                return directions[k] < 0 ? Values.compare(q, p) : cmp;
            }
        }
        return 0;
    }

    private static Object[][] column(Object[][] m, int c) {
        Object[][] out = new Object[m.length][1];
        for (int r = 0; r < m.length; r++) {
            out[r][0] = m[r][c];
        }
        return out;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        if (a.length == 0) {
            throw EvalError.VALUE;
        }
        switch (Functions.index(SWITCH_1, name)) {
            case 0: { // SEQUENCE
                int rows = (int) ev.scalarNum(a, 0);
                int cols = (int) ev.scalarNum(a, 1, 1);
                double start = ev.scalarNum(a, 2, 1);
                double step = ev.scalarNum(a, 3, 1);
                Object[][] out = make(rows, cols);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        out[r][c] = Double.valueOf(start + step * ((double) r * cols + c));
                    }
                }
                return out;
            }
            case 1: { // RANDARRAY
                int rows = (int) ev.scalarNum(a, 0, 1);
                int cols = (int) ev.scalarNum(a, 1, 1);
                double min = ev.scalarNum(a, 2, 0);
                double max = ev.scalarNum(a, 3, 1);
                boolean whole = Evaluator.has(a, 4) && Values.bool(ev.scalar(a[4]));
                if (min > max) {
                    throw EvalError.VALUE;
                }
                Object[][] out = make(rows, cols);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        double v = whole ? Math.ceil(min) + Math.floor(Math.random() * (Math.floor(max) - Math.ceil(min) + 1))
                                : min + Math.random() * (max - min);
                        out[r][c] = Double.valueOf(v);
                    }
                }
                return out;
            }
            case 2: { // FILTER
                Object[][] m = ev.matrix(a[0]);
                Object[][] include = ev.matrix(Evaluator.arg(a, 1));
                boolean byRow = include.length == m.length && Evaluator.width(include) == 1;
                boolean byCol = !byRow && include.length == 1 && Evaluator.width(include) == Evaluator.width(m);
                if (!byRow && !byCol) {
                    throw EvalError.VALUE;
                }
                Object[][] source = byRow ? m : transpose(m);
                ArrayList<Integer> picked = new ArrayList<Integer>();
                for (int i = 0; i < source.length; i++) {
                    Object v = byRow ? include[i][0] : include[0][i];
                    if (v instanceof CellError) {
                        return v;
                    }
                    if (v != null && Values.bool(v)) {
                        picked.add(Integer.valueOf(i));
                    }
                }
                if (picked.isEmpty()) {
                    if (Evaluator.has(a, 2)) {
                        return a[2];
                    }
                    // Excel's own error for an empty result has no place in a file
                    throw EvalError.VALUE;
                }
                Object[][] out = rows(source, picked);
                return byRow ? out : transpose(out);
            }
            case 3: { // SORT
                Object[][] m = ev.matrix(a[0]);
                int index = (int) ev.scalarNum(a, 1, 1);
                int direction = (int) ev.scalarNum(a, 2, 1);
                boolean byCol = Evaluator.has(a, 3) && Values.bool(ev.scalar(a[3]));
                Object[][] source = byCol ? transpose(m) : m;
                if (index < 1 || index > Evaluator.width(source) || direction != 1 && direction != -1) {
                    throw EvalError.VALUE;
                }
                Object[][] out = sorted(source, new Object[][][] {column(source, index - 1)}, new int[] {direction});
                return byCol ? transpose(out) : out;
            }
            case 4: { // SORTBY
                Object[][] m = ev.matrix(a[0]);
                int count = a.length / 2;
                if (count < 1) {
                    throw EvalError.VALUE;
                }
                Object[][][] keys = new Object[count][][];
                int[] directions = new int[count];
                boolean byCol = false;
                for (int k = 0; k < count; k++) {
                    Object[][] by = ev.matrix(a[1 + 2 * k]);
                    if (by.length == 1 && Evaluator.width(by) == Evaluator.width(m) && m.length != 1) {
                        byCol = true;
                        by = transpose(by);
                    }
                    if (Evaluator.width(by) != 1 || by.length != (byCol ? Evaluator.width(m) : m.length)) {
                        throw EvalError.VALUE;
                    }
                    keys[k] = by;
                    directions[k] = (int) ev.scalarNum(a, 2 + 2 * k, 1);
                }
                Object[][] source = byCol ? transpose(m) : m;
                Object[][] out = sorted(source, keys, directions);
                return byCol ? transpose(out) : out;
            }
            case 5: { // UNIQUE
                Object[][] m = ev.matrix(a[0]);
                boolean byCol = Evaluator.has(a, 1) && Values.bool(ev.scalar(a[1]));
                boolean once = Evaluator.has(a, 2) && Values.bool(ev.scalar(a[2]));
                Object[][] source = byCol ? transpose(m) : m;
                ArrayList<Integer> picked = new ArrayList<Integer>();
                for (int i = 0; i < source.length; i++) {
                    int same = 0;
                    boolean earlier = false;
                    for (int j = 0; j < source.length; j++) {
                        if (sameRow(source[i], source[j])) {
                            same++;
                            if (j < i) {
                                earlier = true;
                            }
                        }
                    }
                    if (!earlier && (!once || same == 1)) {
                        picked.add(Integer.valueOf(i));
                    }
                }
                Object[][] out = rows(source, picked);
                return byCol ? transpose(out) : out;
            }
            case 6: // TOCOL
            case 7: { // TOROW
                Object[][] m = ev.matrix(a[0]);
                int ignore = (int) ev.scalarNum(a, 1, 0);
                boolean byCol = Evaluator.has(a, 2) && Values.bool(ev.scalar(a[2]));
                Object[][] source = byCol ? transpose(m) : m;
                ArrayList<Object> values = new ArrayList<Object>();
                for (Object[] line : source) {
                    for (Object v : line) {
                        if (v == null && (ignore & 1) != 0 || v instanceof CellError && (ignore & 2) != 0) {
                            continue;
                        }
                        values.add(v);
                    }
                }
                if (values.isEmpty()) {
                    throw EvalError.VALUE;
                }
                boolean down = "TOCOL".equals(name);
                Object[][] out = down ? new Object[values.size()][1] : new Object[1][values.size()];
                for (int i = 0; i < values.size(); i++) {
                    if (down) {
                        out[i][0] = values.get(i);
                    } else {
                        out[0][i] = values.get(i);
                    }
                }
                return out;
            }
            case 8: // VSTACK
            case 9: { // HSTACK
                boolean vertical = "VSTACK".equals(name);
                Object[][][] parts = new Object[a.length][][];
                int along = 0;
                int across = 0;
                for (int i = 0; i < a.length; i++) {
                    Object[][] m = ev.matrix(a[i]);
                    parts[i] = vertical ? m : transpose(m);
                    along += parts[i].length;
                    across = Math.max(across, Evaluator.width(parts[i]));
                }
                Object[][] out = make(along, across);
                int at = 0;
                for (Object[][] part : parts) {
                    for (Object[] partRow : part) {
                        for (int c = 0; c < across; c++) {
                            out[at][c] = c < partRow.length ? partRow[c] : CellError.NA;
                        }
                        at++;
                    }
                }
                return vertical ? out : transpose(out);
            }
            case 10: // TAKE
            case 11: { // DROP
                Object[][] m = ev.matrix(a[0]);
                boolean take = "TAKE".equals(name);
                int[] rowSpan = span(m.length, Evaluator.has(a, 1) ? Integer.valueOf((int) ev.scalarNum(a, 1)) : null, take);
                int[] colSpan = span(Evaluator.width(m), Evaluator.has(a, 2) ? Integer.valueOf((int) ev.scalarNum(a, 2)) : null,
                        take);
                Object[][] out = make(rowSpan[1] - rowSpan[0], colSpan[1] - colSpan[0]);
                for (int r = 0; r < out.length; r++) {
                    System.arraycopy(m[rowSpan[0] + r], colSpan[0], out[r], 0, out[r].length);
                }
                return out;
            }
            case 12: // CHOOSEROWS
            case 13: { // CHOOSECOLS
                Object[][] m = ev.matrix(a[0]);
                boolean byCol = "CHOOSECOLS".equals(name);
                Object[][] source = byCol ? transpose(m) : m;
                ArrayList<Integer> picked = new ArrayList<Integer>();
                for (int i = 1; i < a.length; i++) {
                    Object[][] indices = ev.matrix(a[i]);
                    for (Object[] line : indices) {
                        for (Object index : line) {
                            int n = (int) Values.num(index);
                            if (n < 0) {
                                n = source.length + n + 1;
                            }
                            if (n < 1 || n > source.length) {
                                throw EvalError.VALUE;
                            }
                            picked.add(Integer.valueOf(n - 1));
                        }
                    }
                }
                Object[][] out = rows(source, picked);
                return byCol ? transpose(out) : out;
            }
            case 14: { // EXPAND
                Object[][] m = ev.matrix(a[0]);
                int rows = (int) ev.scalarNum(a, 1, m.length);
                int cols = (int) ev.scalarNum(a, 2, Evaluator.width(m));
                if (rows < m.length || cols < Evaluator.width(m)) {
                    throw EvalError.VALUE;
                }
                Object pad = Evaluator.has(a, 3) ? ev.scalar(a[3]) : CellError.NA;
                Object[][] out = make(rows, cols);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        out[r][c] = r < m.length && c < m[r].length ? m[r][c] : pad;
                    }
                }
                return out;
            }
            case 15: // WRAPROWS
            case 16: { // WRAPCOLS
                Object[] vector = LookupFunctions.vector(ev, a[0]);
                int count = (int) ev.scalarNum(a, 1);
                if (count < 1) {
                    throw EvalError.NUM;
                }
                Object pad = Evaluator.has(a, 2) ? ev.scalar(a[2]) : CellError.NA;
                int lines = (vector.length + count - 1) / count;
                Object[][] out = make(lines, count);
                for (int i = 0; i < lines * count; i++) {
                    out[i / count][i % count] = i < vector.length ? vector[i] : pad;
                }
                return "WRAPROWS".equals(name) ? out : transpose(out);
            }
            default:
                return CellError.NAME;
        }
    }

    private static Object[][] sorted(Object[][] source, Object[][][] keys, int[] directions) {
        Integer[] order = new Integer[source.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = Integer.valueOf(i);
        }
        sort(order, keys, directions);
        Object[][] out = new Object[source.length][];
        for (int i = 0; i < out.length; i++) {
            out[i] = source[order[i].intValue()];
        }
        return out;
    }

    private static boolean sameRow(Object[] x, Object[] y) {
        for (int c = 0; c < x.length; c++) {
            Object p = x[c];
            Object q = y[c];
            if (p == null || q == null) {
                if (p != q) { //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
                    return false;
                }
            } else if (p instanceof CellError || q instanceof CellError) {
                if (p != q) { //NOPMD CompareObjectsWithEquals - the same object is meant, not an equal one
                    return false;
                }
            } else if (Values.compare(p, q) != 0) {
                return false;
            }
        }
        return true;
    }

    /// The part of a length that TAKE keeps or DROP leaves: [from, to).
    private static int[] span(int length, Integer countOrNull, boolean take) {
        if (countOrNull == null) {
            return new int[] {0, length};
        }
        int n = countOrNull.intValue();
        if (take) {
            if (n == 0) {
                throw EvalError.VALUE;
            }
            return n > 0 ? new int[] {0, Math.min(n, length)} : new int[] {Math.max(0, length + n), length};
        }
        int[] out = n >= 0 ? new int[] {Math.min(n, length), length} : new int[] {0, Math.max(0, length + n)};
        if (out[0] >= out[1]) {
            throw EvalError.VALUE;
        }
        return out;
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "SEQUENCE", "RANDARRAY", "FILTER", "SORT", "SORTBY", "UNIQUE", "TOCOL", "TOROW",
            "VSTACK", "HSTACK", "TAKE", "DROP", "CHOOSEROWS", "CHOOSECOLS", "EXPAND", "WRAPROWS",
            "WRAPCOLS");
}
