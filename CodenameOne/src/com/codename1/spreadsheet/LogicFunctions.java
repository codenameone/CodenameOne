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

/// The logical and information functions. The ones that choose which argument to evaluate
/// -- IF, IFERROR and their kin -- are the evaluator's own.
final class LogicFunctions {
    static final String SCALAR_NAMES = "FALSE ISEVEN ISODD NA NOT TRUE";

    static final String RANGE_NAMES = "AND ERROR.TYPE ISBLANK ISERR ISERROR ISLOGICAL ISNA ISNONTEXT ISNUMBER ISREF"
            + " ISTEXT N OR T TYPE XOR";

    private LogicFunctions() {
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // TRUE
                return Boolean.TRUE;
            case 1: // FALSE
                return Boolean.FALSE;
            case 2: // NA
                return CellError.NA;
            case 3: // NOT
                return Boolean.valueOf(!Values.bool(Evaluator.arg(a, 0) == Evaluator.MISSING ? null : a[0])); //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            case 4: // ISEVEN
            case 5: { // ISODD
                double x = Math.floor(Math.abs(Evaluator.num(a, 0)));
                boolean even = x - 2 * Math.floor(x / 2) == 0;
                return Boolean.valueOf("ISEVEN".equals(name) == even);
            }
            case 6: // AND
            case 7: // OR
            case 8: // XOR
                return combine(name, ev, a);
            case 9: // ISREF
                return Boolean.valueOf(Evaluator.arg(a, 0) instanceof Ref);
            case 10: { // TYPE
                Object arg = Evaluator.arg(a, 0);
                if (arg instanceof Object[][] || arg instanceof Ref && !((Ref) arg).isSingleCell()) {
                    return Double.valueOf(64);
                }
                Object v = ev.scalar(arg);
                if (v instanceof String) {
                    return Double.valueOf(2);
                }
                if (v instanceof Boolean) {
                    return Double.valueOf(4);
                }
                return Double.valueOf(v instanceof CellError ? 16 : 1);
            }
            default:
                break;
        }
        // The rest answer for one value, and for every value of an array or a range.
        Object arg = Evaluator.arg(a, 0);
        if (arg instanceof Object[][] || arg instanceof Ref && !((Ref) arg).isSingleCell()) {
            Object[][] m = ev.matrix(arg);
            Object[][] out = new Object[m.length][Evaluator.width(m)];
            for (int r = 0; r < m.length; r++) {
                for (int c = 0; c < out[r].length; c++) {
                    out[r][c] = one(name, m[r][c]);
                }
            }
            return out;
        }
        return one(name, ev.scalar(arg));
    }

    private static Object one(String name, Object v) {
        switch (Functions.index(SWITCH_2, name)) {
            case 0: // ISBLANK
                return Boolean.valueOf(v == null);
            case 1: // ISERR
                return Boolean.valueOf(v instanceof CellError && v != CellError.NA); //NOPMD CompareObjectsWithEquals - an error value is a constant
            case 2: // ISERROR
                return Boolean.valueOf(v instanceof CellError);
            case 3: // ISNA
                return Boolean.valueOf(v == CellError.NA); //NOPMD CompareObjectsWithEquals - an error value is a constant
            case 4: // ISLOGICAL
                return Boolean.valueOf(v instanceof Boolean);
            case 5: // ISNUMBER
                return Boolean.valueOf(v instanceof Double);
            case 6: // ISTEXT
                return Boolean.valueOf(v instanceof String);
            case 7: // ISNONTEXT
                return Boolean.valueOf(!(v instanceof String));
            case 8: // ERROR.TYPE
                return v instanceof CellError ? (Object) Double.valueOf(((CellError) v).getType()) : CellError.NA;
            case 9: // N
                if (v instanceof Double || v instanceof CellError) {
                    return v;
                }
                return Double.valueOf(v instanceof Boolean && ((Boolean) v).booleanValue() ? 1 : 0);
            case 10: // T
                if (v instanceof CellError) {
                    return v;
                }
                return v instanceof String ? v : "";
            default:
                return CellError.NAME;
        }
    }

    /// AND, OR and XOR. Text and empty cells inside a range are passed over; text typed as
    /// an argument has to read as a logical value.
    private static Object combine(String name, Evaluator ev, Object[] a) {
        int trues = 0;
        int total = 0;
        for (Object arg : a) {
            if (arg instanceof Ref || arg instanceof Object[][]) {
                Object[][] m = ev.matrix(arg);
                for (Object[] line : m) {
                    for (Object v : line) {
                        if (v instanceof CellError) {
                            return v;
                        }
                        if (v instanceof Boolean || v instanceof Double) {
                            total++;
                            if (Values.bool(v)) {
                                trues++;
                            }
                        }
                    }
                }
            } else if (arg != Evaluator.MISSING && arg != null) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
                if (arg instanceof CellError) {
                    return arg;
                }
                total++;
                if (Values.bool(arg)) {
                    trues++;
                }
            }
        }
        if (total == 0) {
            return CellError.VALUE;
        }
        if ("AND".equals(name)) {
            return Boolean.valueOf(trues == total);
        }
        if ("OR".equals(name)) {
            return Boolean.valueOf(trues > 0);
        }
        return Boolean.valueOf((trues & 1) == 1);
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "TRUE", "FALSE", "NA", "NOT", "ISEVEN", "ISODD", "AND", "OR", "XOR", "ISREF", "TYPE");

    private static final java.util.HashMap<String, Integer> SWITCH_2 = Functions.switchTable(
            "ISBLANK", "ISERR", "ISERROR", "ISNA", "ISLOGICAL", "ISNUMBER", "ISTEXT", "ISNONTEXT",
            "ERROR.TYPE", "N", "T");
}
