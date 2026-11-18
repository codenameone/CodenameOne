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

import com.codename1.util.MathUtil;
import java.util.ArrayList;

/// The counting, averaging and descriptive statistics functions.
final class StatFunctions {
    static final String SCALAR_NAMES = "FISHER FISHERINV STANDARDIZE";

    static final String RANGE_NAMES = "AGGREGATE AVEDEV AVERAGE AVERAGEA AVERAGEIF AVERAGEIFS CORREL COUNT COUNTA"
            + " COUNTBLANK COUNTIF COUNTIFS COVAR COVARIANCE.P COVARIANCE.S DEVSQ FORECAST FORECAST.LINEAR FREQUENCY"
            + " GEOMEAN HARMEAN INTERCEPT KURT LARGE MAX MAXA MAXIFS MEDIAN MIN MINA MINIFS MODE MODE.MULT MODE.SNGL"
            + " PEARSON PERCENTILE PERCENTILE.EXC PERCENTILE.INC PERCENTRANK PERCENTRANK.EXC PERCENTRANK.INC QUARTILE"
            + " QUARTILE.EXC QUARTILE.INC RANK RANK.AVG RANK.EQ RSQ SKEW SKEW.P SLOPE SMALL STDEV STDEV.P STDEV.S STDEVA"
            + " STDEVP STDEVPA STEYX SUBTOTAL TRIMMEAN VAR VAR.P VAR.S VARA VARP VARPA";

    private StatFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    private static DoubleList all(Evaluator ev, Object[] a, int from, boolean countTextAndLogicals) {
        DoubleList list = new DoubleList();
        for (int i = from; i < a.length; i++) {
            ev.numbers(a[i], list, countTextAndLogicals);
        }
        return list;
    }

    static double variance(DoubleList list, boolean sample) {
        int n = sample ? list.size - 1 : list.size;
        if (n <= 0) {
            throw EvalError.DIV0;
        }
        return list.devSq() / n;
    }

    static double max(DoubleList list) {
        double m = list.size == 0 ? 0 : list.data[0];
        for (int i = 1; i < list.size; i++) {
            if (list.data[i] > m) {
                m = list.data[i];
            }
        }
        return m;
    }

    static double min(DoubleList list) {
        double m = list.size == 0 ? 0 : list.data[0];
        for (int i = 1; i < list.size; i++) {
            if (list.data[i] < m) {
                m = list.data[i];
            }
        }
        return m;
    }

    static double median(DoubleList list) {
        if (list.size == 0) {
            throw EvalError.NUM;
        }
        double[] s = list.sorted();
        int mid = s.length / 2;
        return (s.length & 1) == 1 ? s[mid] : (s[mid - 1] + s[mid]) / 2;
    }

    static double percentile(DoubleList list, double k, boolean exclusive) {
        int n = list.size;
        if (n == 0 || k < 0 || k > 1) {
            throw EvalError.NUM;
        }
        double[] s = list.sorted();
        double pos;
        if (exclusive) {
            pos = k * (n + 1) - 1;
            if (pos < 0 || pos > n - 1) {
                throw EvalError.NUM;
            }
        } else {
            pos = k * (n - 1);
        }
        int lower = (int) Math.floor(pos);
        if (lower >= n - 1) {
            return s[n - 1];
        }
        return s[lower] + (pos - lower) * (s[lower + 1] - s[lower]);
    }

    /// The value that occurs most often; the one met first wins a tie.
    static double mode(DoubleList list) {
        double best = 0;
        int bestCount = 1;
        for (int i = 0; i < list.size; i++) {
            int count = 0;
            for (int j = 0; j < list.size; j++) {
                if (list.data[j] == list.data[i]) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                best = list.data[i];
            }
        }
        if (bestCount < 2) {
            throw EvalError.NA;
        }
        return best;
    }

    static double nth(DoubleList list, double k, boolean largest) {
        int index = (int) Math.ceil(k);
        if (index < 1 || index > list.size) {
            throw EvalError.NUM;
        }
        double[] s = list.sorted();
        return largest ? s[s.length - index] : s[index - 1];
    }

    /// The functions SUBTOTAL and AGGREGATE select by number.
    static double aggregate(int fn, DoubleList list, double k) {
        switch (fn) {
            case 1:
                return list.mean();
            case 2:
                return list.size;
            case 4:
                return max(list);
            case 5:
                return min(list);
            case 6: {
                double p = 1;
                for (int i = 0; i < list.size; i++) {
                    p *= list.data[i];
                }
                return list.size == 0 ? 0 : p;
            }
            case 7:
                return Math.sqrt(variance(list, true));
            case 8:
                return Math.sqrt(variance(list, false));
            case 9:
                return list.sum();
            case 10:
                return variance(list, true);
            case 11:
                return variance(list, false);
            case 12:
                return median(list);
            case 13:
                return mode(list);
            case 14:
                return nth(list, k, true);
            case 15:
                return nth(list, k, false);
            case 16:
                return percentile(list, k, false);
            case 17:
                return percentile(list, k / 4, false);
            case 18:
                return percentile(list, k, true);
            case 19:
                return percentile(list, k / 4, true);
            default:
                throw EvalError.VALUE;
        }
    }

    /// The pairs of two ranges of the same shape in which both values are numbers.
    static double[][] pairs(Evaluator ev, Object ya, Object xa) {
        Object[][] y = ev.matrix(ya);
        Object[][] x = ev.matrix(xa);
        if (y.length != x.length || Evaluator.width(y) != Evaluator.width(x)) {
            throw EvalError.NA;
        }
        DoubleList ys = new DoubleList();
        DoubleList xs = new DoubleList();
        for (int r = 0; r < y.length; r++) {
            for (int c = 0; c < y[r].length; c++) {
                Object p = y[r][c];
                Object q = x[r][c];
                if (p instanceof CellError) {
                    throw EvalError.of((CellError) p);
                }
                if (q instanceof CellError) {
                    throw EvalError.of((CellError) q);
                }
                if (p instanceof Double && q instanceof Double) {
                    ys.add(((Double) p).doubleValue());
                    xs.add(((Double) q).doubleValue());
                }
            }
        }
        double[] yd = new double[ys.size];
        double[] xd = new double[xs.size];
        System.arraycopy(ys.data, 0, yd, 0, ys.size);
        System.arraycopy(xs.data, 0, xd, 0, xs.size);
        return new double[][] {yd, xd};
    }

    /// Sums over pairs: [n, mean y, mean x, Syy, Sxx, Sxy].
    static double[] moments(double[][] pairs) {
        double[] y = pairs[0];
        double[] x = pairs[1];
        int n = y.length;
        if (n == 0) {
            throw EvalError.DIV0;
        }
        double my = 0;
        double mx = 0;
        for (int i = 0; i < n; i++) {
            my += y[i];
            mx += x[i];
        }
        my /= n;
        mx /= n;
        double syy = 0;
        double sxx = 0;
        double sxy = 0;
        for (int i = 0; i < n; i++) {
            syy += (y[i] - my) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            sxy += (x[i] - mx) * (y[i] - my);
        }
        return new double[] {n, my, mx, syy, sxx, sxy};
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: { // STANDARDIZE
                double sd = Evaluator.num(a, 2);
                if (sd <= 0) {
                    throw EvalError.NUM;
                }
                return d((Evaluator.num(a, 0) - Evaluator.num(a, 1)) / sd);
            }
            case 1: { // FISHER
                double x = Evaluator.num(a, 0);
                if (x <= -1 || x >= 1) {
                    throw EvalError.NUM;
                }
                return d(0.5 * MathUtil.log((1 + x) / (1 - x)));
            }
            case 2: { // FISHERINV
                double e = MathUtil.exp(2 * Evaluator.num(a, 0));
                return d((e - 1) / (e + 1));
            }
            case 3: // AVERAGE
                return d(all(ev, a, 0, false).mean());
            case 4: // AVERAGEA
                return d(all(ev, a, 0, true).mean());
            case 5: // COUNT
                return d(count(ev, a, 0));
            case 6: // COUNTA
                return d(count(ev, a, 1));
            case 7: // COUNTBLANK
                return d(count(ev, a, 2));
            case 8: // MAX
                return d(max(all(ev, a, 0, false)));
            case 9: // MAXA
                return d(max(all(ev, a, 0, true)));
            case 10: // MIN
                return d(min(all(ev, a, 0, false)));
            case 11: // MINA
                return d(min(all(ev, a, 0, true)));
            case 12: // MEDIAN
                return d(median(all(ev, a, 0, false)));
            case 13: // MODE
            case 14: // MODE.SNGL
                return d(mode(all(ev, a, 0, false)));
            case 15: // MODE.MULT
                return modeMult(all(ev, a, 0, false));
            case 16: // STDEV
            case 17: // STDEV.S
                return d(Math.sqrt(variance(all(ev, a, 0, false), true)));
            case 18: // STDEVP
            case 19: // STDEV.P
                return d(Math.sqrt(variance(all(ev, a, 0, false), false)));
            case 20: // STDEVA
                return d(Math.sqrt(variance(all(ev, a, 0, true), true)));
            case 21: // STDEVPA
                return d(Math.sqrt(variance(all(ev, a, 0, true), false)));
            case 22: // VAR
            case 23: // VAR.S
                return d(variance(all(ev, a, 0, false), true));
            case 24: // VARP
            case 25: // VAR.P
                return d(variance(all(ev, a, 0, false), false));
            case 26: // VARA
                return d(variance(all(ev, a, 0, true), true));
            case 27: // VARPA
                return d(variance(all(ev, a, 0, true), false));
            case 28: // DEVSQ
                return d(all(ev, a, 0, false).devSq());
            case 29: { // AVEDEV
                DoubleList list = all(ev, a, 0, false);
                double m = list.mean();
                double total = 0;
                for (int i = 0; i < list.size; i++) {
                    total += Math.abs(list.data[i] - m);
                }
                return d(total / list.size);
            }
            case 30: { // GEOMEAN
                DoubleList list = all(ev, a, 0, false);
                if (list.size == 0) {
                    throw EvalError.NUM;
                }
                double logs = 0;
                for (int i = 0; i < list.size; i++) {
                    logs += MathFunctions.ln(list.data[i]);
                }
                return d(MathUtil.exp(logs / list.size));
            }
            case 31: { // HARMEAN
                DoubleList list = all(ev, a, 0, false);
                if (list.size == 0) {
                    throw EvalError.NUM;
                }
                double total = 0;
                for (int i = 0; i < list.size; i++) {
                    if (list.data[i] <= 0) {
                        throw EvalError.NUM;
                    }
                    total += 1 / list.data[i];
                }
                return d(list.size / total);
            }
            case 32: // LARGE
            case 33: { // SMALL
                DoubleList list = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), list, false);
                return d(nth(list, ev.scalarNum(a, 1), "LARGE".equals(name)));
            }
            case 34: // PERCENTILE
            case 35: // PERCENTILE.INC
            case 36: // PERCENTILE.EXC
            case 37: // QUARTILE
            case 38: // QUARTILE.INC
            case 39: { // QUARTILE.EXC
                DoubleList list = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), list, false);
                double k = ev.scalarNum(a, 1);
                boolean exclusive = name.endsWith(".EXC");
                if (name.charAt(0) == 'Q') {
                    k = Math.floor(k);
                    if (k < 0 || k > 4 || exclusive && (k == 0 || k == 4)) {
                        throw EvalError.NUM;
                    }
                    k /= 4;
                }
                return d(percentile(list, k, exclusive));
            }
            case 40: // PERCENTRANK
            case 41: // PERCENTRANK.INC
            case 42: // PERCENTRANK.EXC
                return percentRank(ev, a, name.endsWith(".EXC"));
            case 43: // RANK
            case 44: // RANK.EQ
            case 45: { // RANK.AVG
                double x = ev.scalarNum(a, 0);
                DoubleList list = new DoubleList();
                ev.numbers(Evaluator.arg(a, 1), list, false);
                boolean ascending = ev.scalarNum(a, 2, 0) != 0;
                int before = 0;
                int same = 0;
                for (int i = 0; i < list.size; i++) {
                    double v = list.data[i];
                    if (v == x) {
                        same++;
                    } else if (ascending ? v < x : v > x) {
                        before++;
                    }
                }
                if (same == 0) {
                    throw EvalError.NA;
                }
                return d("RANK.AVG".equals(name) ? before + (same + 1) / 2.0 : before + 1);
            }
            case 46: { // TRIMMEAN
                DoubleList list = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), list, false);
                double percent = ev.scalarNum(a, 1);
                if (percent < 0 || percent >= 1 || list.size == 0) {
                    throw EvalError.NUM;
                }
                int cut = (int) Math.floor(list.size * percent / 2);
                double[] s = list.sorted();
                double total = 0;
                for (int i = cut; i < s.length - cut; i++) {
                    total += s[i];
                }
                return d(total / (s.length - 2 * cut));
            }
            case 47: { // KURT
                DoubleList list = all(ev, a, 0, false);
                double n = list.size;
                if (n < 4) {
                    throw EvalError.DIV0;
                }
                double sd = Math.sqrt(variance(list, true));
                if (sd == 0) {
                    throw EvalError.DIV0;
                }
                double m = list.mean();
                double total = 0;
                for (int i = 0; i < list.size; i++) {
                    double z = (list.data[i] - m) / sd;
                    total += z * z * z * z;
                }
                return d(n * (n + 1) / ((n - 1) * (n - 2) * (n - 3)) * total - 3 * (n - 1) * (n - 1) / ((n - 2) * (n - 3)));
            }
            case 48: // SKEW
            case 49: { // SKEW.P
                DoubleList list = all(ev, a, 0, false);
                double n = list.size;
                boolean population = "SKEW.P".equals(name);
                if (n < 3 && !population || n == 0) {
                    throw EvalError.DIV0;
                }
                double sd = Math.sqrt(variance(list, !population));
                if (sd == 0) {
                    throw EvalError.DIV0;
                }
                double m = list.mean();
                double total = 0;
                for (int i = 0; i < list.size; i++) {
                    double z = (list.data[i] - m) / sd;
                    total += z * z * z;
                }
                return d(population ? total / n : n / ((n - 1) * (n - 2)) * total);
            }
            case 50: // CORREL
            case 51: // PEARSON
            case 52: { // RSQ
                double[] m = moments(pairs(ev, Evaluator.arg(a, 0), Evaluator.arg(a, 1)));
                if (m[3] == 0 || m[4] == 0) {
                    throw EvalError.DIV0;
                }
                double r = m[5] / Math.sqrt(m[3] * m[4]);
                return d("RSQ".equals(name) ? r * r : r);
            }
            case 53: // COVAR
            case 54: // COVARIANCE.P
            case 55: { // COVARIANCE.S
                double[] m = moments(pairs(ev, Evaluator.arg(a, 0), Evaluator.arg(a, 1)));
                double n = name.endsWith(".S") ? m[0] - 1 : m[0];
                if (n <= 0) {
                    throw EvalError.DIV0;
                }
                return d(m[5] / n);
            }
            case 56: // SLOPE
            case 57: // INTERCEPT
            case 58: { // STEYX
                double[] m = moments(pairs(ev, Evaluator.arg(a, 0), Evaluator.arg(a, 1)));
                if (m[4] == 0) {
                    throw EvalError.DIV0;
                }
                double slope = m[5] / m[4];
                if ("SLOPE".equals(name)) {
                    return d(slope);
                }
                if ("INTERCEPT".equals(name)) {
                    return d(m[1] - slope * m[2]);
                }
                if (m[0] < 3) {
                    throw EvalError.DIV0;
                }
                return d(Math.sqrt(Math.max(0, m[3] - m[5] * m[5] / m[4]) / (m[0] - 2)));
            }
            case 59: // FORECAST
            case 60: { // FORECAST.LINEAR
                double x = ev.scalarNum(a, 0);
                double[] m = moments(pairs(ev, Evaluator.arg(a, 1), Evaluator.arg(a, 2)));
                if (m[4] == 0) {
                    throw EvalError.DIV0;
                }
                double slope = m[5] / m[4];
                return d(m[1] - slope * m[2] + slope * x);
            }
            case 61: { // FREQUENCY
                DoubleList data = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), data, false);
                DoubleList binList = new DoubleList();
                ev.numbers(Evaluator.arg(a, 1), binList, false);
                double[] bins = binList.sorted();
                Object[][] out = new Object[bins.length + 1][1];
                int[] counts = new int[bins.length + 1];
                for (int i = 0; i < data.size; i++) {
                    int b = 0;
                    while (b < bins.length && data.data[i] > bins[b]) {
                        b++;
                    }
                    counts[b]++;
                }
                for (int i = 0; i < counts.length; i++) {
                    out[i][0] = d(counts[i]);
                }
                return out;
            }
            case 62: { // COUNTIF
                boolean[][] mask = Criteria.mask(ev, a, 0, 2);
                return d(countTrue(mask));
            }
            case 63: // COUNTIFS
                return d(countTrue(Criteria.mask(ev, a, 0, a.length)));
            case 64: { // AVERAGEIF
                if (a.length < 2) {
                    throw EvalError.VALUE;
                }
                boolean[][] mask = Criteria.mask(ev, a, 0, 2);
                return d(masked(ev, mask, Evaluator.has(a, 2) ? a[2] : a[0]).mean());
            }
            case 65: // AVERAGEIFS
            case 66: // MAXIFS
            case 67: { // MINIFS
                if (a.length < 3) {
                    throw EvalError.VALUE;
                }
                DoubleList list = masked(ev, Criteria.mask(ev, a, 1, a.length), a[0]);
                if ("AVERAGEIFS".equals(name)) {
                    return d(list.mean());
                }
                return d("MAXIFS".equals(name) ? max(list) : min(list));
            }
            case 68: { // SUBTOTAL
                int fn = (int) ev.scalarNum(a, 0);
                if (fn > 100) {
                    fn -= 100;
                }
                if (fn == 3) {
                    return d(count(ev, a, 1) - 1);
                }
                if (fn < 1 || fn > 11) {
                    throw EvalError.VALUE;
                }
                DoubleList list = new DoubleList();
                ev.skipSubtotals = true;
                try {
                    for (int i = 1; i < a.length; i++) {
                        ev.numbers(a[i], list, false);
                    }
                } finally {
                    ev.skipSubtotals = false;
                }
                return d(aggregate(fn, list, 0));
            }
            case 69: { // AGGREGATE
                int fn = (int) ev.scalarNum(a, 0);
                int options = (int) ev.scalarNum(a, 1, 0);
                boolean ignoreErrors = options == 2 || options == 3 || options == 6 || options == 7;
                boolean takesK = fn >= 14;
                DoubleList list = new DoubleList();
                int last = takesK ? Math.min(a.length, 3) : a.length;
                int counted = 0;
                for (int i = 2; i < last; i++) {
                    Object[][] m = ev.matrix(a[i]);
                    for (Object[] line : m) {
                        for (Object v : line) {
                            if (v instanceof CellError) {
                                if (!ignoreErrors) {
                                    throw EvalError.of((CellError) v);
                                }
                            } else if (v instanceof Double) {
                                list.add(((Double) v).doubleValue());
                                counted++;
                            } else if (v != null && !"".equals(v)) {
                                counted++;
                            }
                        }
                    }
                }
                if (fn == 3) {
                    return d(counted);
                }
                return d(aggregate(fn, list, takesK ? ev.scalarNum(a, 3) : 0));
            }
            default:
                return CellError.NAME;
        }
    }

    /// Counts over arguments: mode 0 the numbers, 1 everything that is not empty, 2 the
    /// empty cells and empty texts.
    private static int count(Evaluator ev, Object[] a, int mode) {
        int count = 0;
        for (Object arg : a) {
            if (arg instanceof Ref || arg instanceof Object[][]) {
                if (arg instanceof Ref) {
                    Ref ref = (Ref) arg;
                    if (mode == 2) {
                        // every cell of the range counts, the ones past the data included
                        count += (ref.rows() - ref.clip().rows()) * ref.cols()
                                + (ref.cols() - ref.clip().cols()) * ref.clip().rows();
                    }
                }
                Object[][] m = ev.matrix(arg);
                for (Object[] line : m) {
                    for (Object v : line) {
                        if (mode == 0) {
                            if (v instanceof Double) {
                                count++;
                            }
                        } else if (mode == 1) {
                            if (v != null) {
                                count++;
                            }
                        } else if (v == null || "".equals(v)) {
                            count++;
                        }
                    }
                }
            } else if (arg != Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
                if (mode == 0) {
                    if (arg instanceof Double || arg instanceof Boolean
                            || arg instanceof String && Values.parseNumber((String) arg) != null) {
                        count++;
                    }
                } else if (mode == 1) {
                    if (arg != null) {
                        count++;
                    }
                } else if (arg == null || "".equals(arg)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countTrue(boolean[][] mask) {
        int count = 0;
        for (boolean[] line : mask) {
            for (boolean hit : line) {
                if (hit) {
                    count++;
                }
            }
        }
        return count;
    }

    private static DoubleList masked(Evaluator ev, boolean[][] mask, Object valueRange) {
        Object[][] values = ev.shaped(valueRange, mask.length, Evaluator.width2(mask));
        DoubleList list = new DoubleList();
        for (int r = 0; r < mask.length; r++) {
            for (int c = 0; c < mask[r].length; c++) {
                if (mask[r][c] && values[r][c] instanceof Double) {
                    list.add(((Double) values[r][c]).doubleValue());
                }
            }
        }
        return list;
    }

    private static Object modeMult(DoubleList list) {
        ArrayList<Double> modes = new ArrayList<Double>();
        int best = 2;
        for (int i = 0; i < list.size; i++) {
            int count = 0;
            boolean seen = false;
            for (int j = 0; j < list.size; j++) {
                if (list.data[j] == list.data[i]) {
                    if (j < i) {
                        seen = true;
                    }
                    count++;
                }
            }
            if (seen || count < best) {
                continue;
            }
            if (count > best) {
                best = count;
                modes.clear();
            }
            modes.add(d(list.data[i]));
        }
        if (modes.isEmpty()) {
            throw EvalError.NA;
        }
        Object[][] out = new Object[modes.size()][1];
        for (int i = 0; i < out.length; i++) {
            out[i][0] = modes.get(i);
        }
        return out;
    }

    private static Object percentRank(Evaluator ev, Object[] a, boolean exclusive) {
        DoubleList list = new DoubleList();
        ev.numbers(Evaluator.arg(a, 0), list, false);
        double x = ev.scalarNum(a, 1);
        int significance = (int) ev.scalarNum(a, 2, 3);
        if (list.size == 0 || significance < 1) {
            throw EvalError.NUM;
        }
        double[] s = list.sorted();
        int n = s.length;
        if (x < s[0] || x > s[n - 1]) {
            throw EvalError.NA;
        }
        double rank;
        int below = 0;
        while (below < n && s[below] < x) {
            below++;
        }
        if (s[below] == x) {
            rank = below;
        } else {
            rank = below - 1 + (x - s[below - 1]) / (s[below] - s[below - 1]);
        }
        double result;
        if (exclusive) {
            result = (rank + 1) / (n + 1);
        } else {
            result = n == 1 ? 1 : rank / (n - 1);
        }
        return d(MathFunctions.roundDirected(result, significance, false));
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "STANDARDIZE", "FISHER", "FISHERINV", "AVERAGE", "AVERAGEA", "COUNT", "COUNTA",
            "COUNTBLANK", "MAX", "MAXA", "MIN", "MINA", "MEDIAN", "MODE", "MODE.SNGL", "MODE.MULT",
            "STDEV", "STDEV.S", "STDEVP", "STDEV.P", "STDEVA", "STDEVPA", "VAR", "VAR.S", "VARP",
            "VAR.P", "VARA", "VARPA", "DEVSQ", "AVEDEV", "GEOMEAN", "HARMEAN", "LARGE", "SMALL",
            "PERCENTILE", "PERCENTILE.INC", "PERCENTILE.EXC", "QUARTILE", "QUARTILE.INC",
            "QUARTILE.EXC", "PERCENTRANK", "PERCENTRANK.INC", "PERCENTRANK.EXC", "RANK", "RANK.EQ",
            "RANK.AVG", "TRIMMEAN", "KURT", "SKEW", "SKEW.P", "CORREL", "PEARSON", "RSQ", "COVAR",
            "COVARIANCE.P", "COVARIANCE.S", "SLOPE", "INTERCEPT", "STEYX", "FORECAST",
            "FORECAST.LINEAR", "FREQUENCY", "COUNTIF", "COUNTIFS", "AVERAGEIF", "AVERAGEIFS",
            "MAXIFS", "MINIFS", "SUBTOTAL", "AGGREGATE");
}
