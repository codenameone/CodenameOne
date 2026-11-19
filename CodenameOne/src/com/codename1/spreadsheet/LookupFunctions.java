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

/// The lookup and reference functions.
final class LookupFunctions {
    static final String RANGE_NAMES = "ADDRESS AREAS COLUMNS HLOOKUP INDEX INDIRECT LOOKUP MATCH OFFSET ROWS SHEET"
            + " SHEETS TRANSPOSE VLOOKUP XLOOKUP XMATCH";

    private LookupFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // ROWS
            case 1: { // COLUMNS
                Object arg = Evaluator.arg(a, 0);
                boolean rows = "ROWS".equals(name);
                if (arg instanceof Ref) {
                    Ref r = (Ref) arg;
                    return d(rows ? r.rows() : r.cols());
                }
                if (arg instanceof CellError) {
                    return arg;
                }
                Object[][] m = ev.matrix(arg);
                return d(rows ? m.length : Evaluator.width(m));
            }
            case 2: { // AREAS
                Object arg = Evaluator.arg(a, 0);
                if (!(arg instanceof Ref)) {
                    throw EvalError.VALUE;
                }
                int count = 0;
                for (Ref r = (Ref) arg; r != null; r = r.next) {
                    count++;
                }
                return d(count);
            }
            case 3: { // SHEET
                Object arg = Evaluator.arg(a, 0);
                if (arg instanceof Ref) {
                    return d(ev.workbook.indexOf(((Ref) arg).sheet) + 1);
                }
                if (arg instanceof String) {
                    Sheet s = ev.workbook.getSheet((String) arg);
                    if (s == null) {
                        throw EvalError.NA;
                    }
                    return d(ev.workbook.indexOf(s) + 1);
                }
                return d(ev.workbook.indexOf(ev.sheet) + 1);
            }
            case 4: { // SHEETS
                Object arg = Evaluator.arg(a, 0);
                if (arg instanceof Ref) {
                    Ref r = (Ref) arg;
                    return d(r.lastSheet == null ? 1 : ev.workbook.indexOf(r.lastSheet) - ev.workbook.indexOf(r.sheet) + 1);
                }
                return d(ev.workbook.getSheetCount());
            }
            case 5: // ADDRESS
                return address(ev, a);
            case 6: { // TRANSPOSE
                Object[][] m = ev.matrix(Evaluator.arg(a, 0));
                int w = Evaluator.width(m);
                Object[][] out = new Object[w][m.length];
                for (int r = 0; r < m.length; r++) {
                    for (int c = 0; c < w; c++) {
                        out[c][r] = m[r][c];
                    }
                }
                return out;
            }
            case 7: // INDEX
                return index(ev, a);
            case 8: { // OFFSET
                Object arg = Evaluator.arg(a, 0);
                if (!(arg instanceof Ref)) {
                    throw arg instanceof CellError ? EvalError.of((CellError) arg) : EvalError.VALUE;
                }
                Ref base = (Ref) arg;
                int r = base.r1 + (int) ev.scalarNum(a, 1);
                int c = base.c1 + (int) ev.scalarNum(a, 2, 0);
                int h = (int) ev.scalarNum(a, 3, base.rows());
                int w = (int) ev.scalarNum(a, 4, base.cols());
                if (h == 0 || w == 0) {
                    throw EvalError.REF;
                }
                // a negative height or width extends up or to the left of the anchor
                if (h < 0) {
                    r += h + 1;
                    h = -h;
                }
                if (w < 0) {
                    c += w + 1;
                    w = -w;
                }
                if (r < 0 || c < 0 || r + h - 1 > Node.MAX_ROW || c + w - 1 > Node.MAX_COL) {
                    throw EvalError.REF;
                }
                return new Ref(base.sheet, null, r, c, r + h - 1, c + w - 1);
            }
            case 9: { // INDIRECT
                String text = ev.scalarText(a, 0).trim();
                if (Evaluator.has(a, 1) && !Values.bool(ev.scalar(a[1]))) {
                    // R1C1 notation is not read
                    throw EvalError.REF;
                }
                Node n;
                try {
                    n = FormulaParser.parse(text, false);
                } catch (FormulaSyntaxException e) {
                    throw EvalError.REF;
                }
                if (n.kind != Node.REF && n.kind != Node.NAME) {
                    throw EvalError.REF;
                }
                Object v = ev.eval(n);
                if (!(v instanceof Ref)) {
                    throw EvalError.REF;
                }
                return v;
            }
            case 10: { // MATCH
                Object key = ev.scalar(Evaluator.arg(a, 0));
                Object[] vector = vector(ev, Evaluator.arg(a, 1));
                int type = (int) ev.scalarNum(a, 2, 1);
                // a sorted list is searched the way the lookups search it: among equal entries the
                // last one answers, and MATCH(1E+99, list) is the place of the last number
                int at = find(key, vector, type == 0 ? 0 : type > 0 ? -1 : 1, type == 0,
                        type == 0 ? 1 : type > 0 ? 2 : -2);
                if (at < 0) {
                    throw EvalError.NA;
                }
                return d(at + 1);
            }
            case 11: { // XMATCH
                Object key = ev.scalar(Evaluator.arg(a, 0));
                Object[] vector = vector(ev, Evaluator.arg(a, 1));
                int mode = (int) ev.scalarNum(a, 2, 0);
                int at = find(key, vector, mode == 2 ? 0 : mode, mode == 2, (int) ev.scalarNum(a, 3, 1));
                if (at < 0) {
                    throw EvalError.NA;
                }
                return d(at + 1);
            }
            case 12: // VLOOKUP
            case 13: { // HLOOKUP
                boolean vertical = "VLOOKUP".equals(name);
                Object key = ev.scalar(Evaluator.arg(a, 0));
                Object[][] table = ev.matrix(Evaluator.arg(a, 1));
                int n = (int) ev.scalarNum(a, 2);
                boolean approximate = !Evaluator.has(a, 3) || Values.bool(ev.scalar(a[3]));
                if (n < 1) {
                    throw EvalError.VALUE;
                }
                if (n > (vertical ? Evaluator.width(table) : table.length)) {
                    throw EvalError.REF;
                }
                Object[] keys = vertical ? column(table, 0) : table.length == 0 ? new Object[0] : table[0];
                int at = find(key, keys, approximate ? -1 : 0, !approximate, approximate ? 2 : 1);
                if (at < 0) {
                    throw EvalError.NA;
                }
                return vertical ? table[at][n - 1] : table[n - 1][at];
            }
            case 14: { // LOOKUP
                Object key = ev.scalar(Evaluator.arg(a, 0));
                Object[][] table = ev.matrix(Evaluator.arg(a, 1));
                boolean byColumn = table.length >= Evaluator.width(table);
                Object[] keys = byColumn ? column(table, 0) : table[0];
                int at = find(key, keys, -1, false, 2);
                if (at < 0) {
                    throw EvalError.NA;
                }
                if (Evaluator.has(a, 2)) {
                    Object[] results = vector(ev, a[2]);
                    if (at >= results.length) {
                        throw EvalError.NA;
                    }
                    return results[at];
                }
                return byColumn ? table[at][Evaluator.width(table) - 1] : table[table.length - 1][at];
            }
            case 15: { // XLOOKUP
                Object key = ev.scalar(Evaluator.arg(a, 0));
                Object[][] lookup = ev.matrix(Evaluator.arg(a, 1));
                boolean down = Evaluator.width(lookup) == 1;
                Object[] keys = down ? column(lookup, 0) : lookup[0];
                Object[][] results = ev.shaped2(Evaluator.arg(a, 2), down, keys.length);
                int mode = (int) ev.scalarNum(a, 4, 0);
                int at = find(key, keys, mode == 2 ? 0 : mode, mode == 2, (int) ev.scalarNum(a, 5, 1));
                if (at < 0) {
                    if (Evaluator.has(a, 3)) {
                        return a[3];
                    }
                    throw EvalError.NA;
                }
                if (down) {
                    if (Evaluator.width(results) == 1) {
                        return results[at][0];
                    }
                    return new Object[][] {results[at]};
                }
                if (results.length == 1) {
                    return results[0][at];
                }
                Object[][] out = new Object[results.length][1];
                for (int r = 0; r < results.length; r++) {
                    out[r][0] = results[r][at];
                }
                return out;
            }
            default:
                return CellError.NAME;
        }
    }

    private static Object[] column(Object[][] m, int c) {
        Object[] out = new Object[m.length];
        for (int r = 0; r < m.length; r++) {
            out[r] = m[r][c];
        }
        return out;
    }

    /// A row or a column of values as one list.
    static Object[] vector(Evaluator ev, Object arg) {
        if (arg instanceof CellError) {
            throw EvalError.of((CellError) arg);
        }
        Object[][] m = ev.matrix(arg);
        if (m.length == 1) {
            return m[0];
        }
        if (Evaluator.width(m) != 1) {
            throw EvalError.NA;
        }
        return column(m, 0);
    }

    /// Finds a key in a list.
    ///
    /// #### Parameters
    ///
    /// - `nearest`: 0 for the key itself, -1 to settle for the largest value below it, 1 for
    ///   the smallest above it.
    /// - `wildcards`: a text key may hold `*` and `?`.
    /// - `search`: 1 first to last, -1 last to first, 2 a binary search of an ascending
    ///   list, -2 of a descending one.
    ///
    /// #### Returns
    ///
    /// the index, or -1
    static int find(Object key, Object[] list, int nearest, boolean wildcards, int search) {
        if (key instanceof CellError) {
            throw EvalError.of((CellError) key);
        }
        if (key == null) {
            key = Double.valueOf(0);
        }
        int n = list.length;
        if (search == 2 || search == -2) {
            // the list is sorted: the last entry not past the key
            int lo = 0;
            int hi = n - 1;
            int best = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                Object v = list[mid];
                int probe = mid;
                // an entry of another type cannot be compared: look to its left
                while (probe >= lo && !sameType(v, key)) {
                    probe--;
                    v = probe >= lo ? list[probe] : null;
                }
                if (probe < lo) {
                    lo = mid + 1;
                    continue;
                }
                // a list sorted downwards is searched as an upward one read the other way
                int cmp = search == -2 ? Values.compare(key, v) : Values.compare(v, key);
                if (cmp <= 0) {
                    best = probe;
                    lo = mid + 1;
                } else {
                    hi = probe - 1;
                }
            }
            if (best < 0) {
                if (nearest == 1 && n > 0 && search == 2 && sameType(list[0], key)) {
                    return 0;
                }
                return -1;
            }
            int cmp = Values.compare(list[best], key);
            if (cmp == 0 || nearest == -1 && search == 2 || nearest == 1 && search == -2) {
                return best;
            }
            if (nearest != 0 && best + 1 < n && sameType(list[best + 1], key)) {
                return best + 1;
            }
            return -1;
        }
        boolean pattern = wildcards && key instanceof String && Criteria.hasWildcards((String) key);
        int bestIndex = -1;
        Object bestValue = null;
        for (int k = 0; k < n; k++) {
            int i = search < 0 ? n - 1 - k : k;
            Object v = list[i];
            if (!sameType(v, key)) {
                continue;
            }
            if (pattern) {
                if (Criteria.wildcardMatch((String) key, 0, (String) v, 0)) {
                    return i;
                }
                continue;
            }
            int cmp = Values.compare(v, key);
            if (cmp == 0) {
                return i;
            }
            if (nearest < 0 && cmp < 0 && (bestIndex < 0 || Values.compare(v, bestValue) > 0)
                    || nearest > 0 && cmp > 0 && (bestIndex < 0 || Values.compare(v, bestValue) < 0)) {
                bestIndex = i;
                bestValue = v;
            }
        }
        return bestIndex;
    }

    private static boolean sameType(Object v, Object key) {
        if (key instanceof Double) {
            return v instanceof Double;
        }
        if (key instanceof String) {
            return v instanceof String;
        }
        return key instanceof Boolean && v instanceof Boolean;
    }

    private static Object index(Evaluator ev, Object[] a) {
        Object arg = Evaluator.arg(a, 0);
        if (arg instanceof CellError) {
            return arg;
        }
        int row = (int) ev.scalarNum(a, 1, 0);
        int col = (int) ev.scalarNum(a, 2, 0);
        if (row < 0 || col < 0) {
            throw EvalError.VALUE;
        }
        if (arg instanceof Ref) {
            Ref ref = (Ref) arg;
            int area = (int) ev.scalarNum(a, 3, 1);
            for (int i = 1; i < area && ref != null; i++) {
                ref = ref.next;
            }
            if (ref == null || area < 1) {
                throw EvalError.REF;
            }
            int rows = ref.rows();
            int cols = ref.cols();
            if (!Evaluator.has(a, 2) && rows == 1) {
                // one row and one index: the index is the column
                col = row;
                row = 1;
            } else if (!Evaluator.has(a, 2) && cols == 1) {
                col = 1;
            }
            if (row > rows || col > cols) {
                throw EvalError.REF;
            }
            int r1 = row == 0 ? ref.r1 : ref.r1 + row - 1;
            int r2 = row == 0 ? ref.r2 : r1;
            int c1 = col == 0 ? ref.c1 : ref.c1 + col - 1;
            int c2 = col == 0 ? ref.c2 : c1;
            return new Ref(ref.sheet, null, r1, c1, r2, c2);
        }
        Object[][] m = ev.matrix(arg);
        int rows = m.length;
        int cols = Evaluator.width(m);
        if (!Evaluator.has(a, 2) && rows == 1) {
            col = row;
            row = 1;
        } else if (!Evaluator.has(a, 2) && cols == 1) {
            col = 1;
        }
        if (row > rows || col > cols) {
            throw EvalError.REF;
        }
        if (row > 0 && col > 0) {
            return m[row - 1][col - 1];
        }
        if (row > 0) {
            return new Object[][] {m[row - 1]};
        }
        if (col > 0) {
            Object[][] out = new Object[rows][1];
            for (int r = 0; r < rows; r++) {
                out[r][0] = m[r][col - 1];
            }
            return out;
        }
        return m;
    }

    private static Object address(Evaluator ev, Object[] a) {
        int row = (int) ev.scalarNum(a, 0);
        int col = (int) ev.scalarNum(a, 1);
        int abs = (int) ev.scalarNum(a, 2, 1);
        if (row < 1 || col < 1 || row > Node.MAX_ROW + 1 || col > Node.MAX_COL + 1 || abs < 1 || abs > 4) {
            throw EvalError.VALUE;
        }
        if (Evaluator.has(a, 3) && !Values.bool(ev.scalar(a[3]))) {
            throw EvalError.VALUE;
        }
        StringBuilder sb = new StringBuilder();
        if (Evaluator.has(a, 4)) {
            FormulaParser.appendSheetName(sb, ev.scalarText(a, 4));
            sb.append('!');
        }
        if (abs == 1 || abs == 3) {
            sb.append('$');
        }
        FormulaParser.appendColumn(sb, col - 1);
        if (abs == 1 || abs == 2) {
            sb.append('$');
        }
        sb.append(row);
        return sb.toString();
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "ROWS", "COLUMNS", "AREAS", "SHEET", "SHEETS", "ADDRESS", "TRANSPOSE", "INDEX",
            "OFFSET", "INDIRECT", "MATCH", "XMATCH", "VLOOKUP", "HLOOKUP", "LOOKUP", "XLOOKUP");
}
