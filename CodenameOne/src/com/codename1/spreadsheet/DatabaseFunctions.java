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

/// The database functions: a list with a header row, a field and a block of criteria whose
/// rows are alternatives and whose columns must all hold.
final class DatabaseFunctions {
    static final String RANGE_NAMES = "DAVERAGE DCOUNT DCOUNTA DGET DMAX DMIN DPRODUCT DSTDEV DSTDEVP DSUM DVAR DVARP";

    private DatabaseFunctions() {
    }

    private static int field(Object[][] db, Object field) {
        if (field instanceof Double) {
            int n = (int) ((Double) field).doubleValue();
            if (n < 1 || n > Evaluator.width(db)) {
                throw EvalError.VALUE;
            }
            return n - 1;
        }
        if (field instanceof String) {
            for (int c = 0; c < db[0].length; c++) {
                if (db[0][c] != null && Values.text(db[0][c]).equalsIgnoreCase((String) field)) {
                    return c;
                }
            }
        }
        throw EvalError.VALUE;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        if (a.length < 3) {
            throw EvalError.VALUE;
        }
        Object[][] db = ev.matrix(a[0]);
        Object[][] criteria = ev.matrix(a[2]);
        if (db.length == 0 || criteria.length < 2) {
            throw EvalError.VALUE;
        }
        Object fieldArg = ev.scalar(a[1]);
        boolean anyField = fieldArg == null && ("DCOUNT".equals(name) || "DCOUNTA".equals(name));
        int column = anyField ? -1 : field(db, fieldArg);
        // each criteria column names a column of the list
        int[] columns = new int[criteria[0].length];
        for (int c = 0; c < columns.length; c++) {
            columns[c] = criteria[0][c] == null ? -1 : field(db, Values.text(criteria[0][c]));
        }
        Criteria[][] tests = new Criteria[criteria.length - 1][columns.length];
        for (int r = 1; r < criteria.length; r++) {
            for (int c = 0; c < columns.length; c++) {
                Object v = criteria[r][c];
                tests[r - 1][c] = v == null || columns[c] < 0 ? null : new Criteria(v);
            }
        }
        DoubleList numbers = new DoubleList();
        int filled = 0;
        Object single = null;
        int matches = 0;
        for (int r = 1; r < db.length; r++) {
            boolean hit = false;
            for (int t = 0; t < tests.length && !hit; t++) {
                boolean all = true;
                for (int c = 0; c < columns.length && all; c++) {
                    if (tests[t][c] != null && !tests[t][c].matches(db[r][columns[c]])) {
                        all = false;
                    }
                }
                hit = all;
            }
            if (!hit) {
                continue;
            }
            matches++;
            if (anyField) {
                filled++;
                numbers.add(0);
                continue;
            }
            Object v = db[r][column];
            single = v;
            if (v instanceof CellError) {
                return v;
            }
            if (v != null) {
                filled++;
            }
            if (v instanceof Double) {
                numbers.add(((Double) v).doubleValue());
            }
        }
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // DSUM
                return Double.valueOf(numbers.sum());
            case 1: // DAVERAGE
                return Double.valueOf(numbers.mean());
            case 2: // DCOUNT
                return Double.valueOf(numbers.size);
            case 3: // DCOUNTA
                return Double.valueOf(filled);
            case 4: // DMAX
                return Double.valueOf(StatFunctions.max(numbers));
            case 5: // DMIN
                return Double.valueOf(StatFunctions.min(numbers));
            case 6: // DPRODUCT
                return Double.valueOf(StatFunctions.aggregate(6, numbers, 0));
            case 7: // DSTDEV
                return Double.valueOf(Math.sqrt(StatFunctions.variance(numbers, true)));
            case 8: // DSTDEVP
                return Double.valueOf(Math.sqrt(StatFunctions.variance(numbers, false)));
            case 9: // DVAR
                return Double.valueOf(StatFunctions.variance(numbers, true));
            case 10: // DVARP
                return Double.valueOf(StatFunctions.variance(numbers, false));
            case 11: // DGET
                if (matches == 0) {
                    throw EvalError.VALUE;
                }
                if (matches > 1) {
                    throw EvalError.NUM;
                }
                return single;
            default:
                return CellError.NAME;
        }
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "DSUM", "DAVERAGE", "DCOUNT", "DCOUNTA", "DMAX", "DMIN", "DPRODUCT", "DSTDEV",
            "DSTDEVP", "DVAR", "DVARP", "DGET");
}
