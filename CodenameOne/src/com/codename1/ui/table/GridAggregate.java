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
package com.codename1.ui.table;

/// What a group row of a `DataGrid` shows in a column: the values of the
/// group's rows folded into one.
///
/// `fold` is called once per row of the group, in order, with what it
/// returned for the rows before; the first call gets null. `finish` turns
/// the last result into what is shown. An aggregate keeps no state of its
/// own, so one instance serves every group and every column.
public abstract class GridAggregate {

    /// The sum of the numbers; other values are skipped.
    public static final GridAggregate SUM = new Numeric(Numeric.SUM);

    /// The mean of the numbers; other values are skipped.
    public static final GridAggregate AVERAGE = new Numeric(Numeric.AVERAGE);

    /// The smallest value in the column's order.
    public static final GridAggregate MIN = new Extreme(false);

    /// The largest value in the column's order.
    public static final GridAggregate MAX = new Extreme(true);

    /// How many rows have a value that is not null.
    public static final GridAggregate COUNT = new Numeric(Numeric.COUNT);

    /// Folds one more value into the result so far.
    ///
    /// #### Parameters
    ///
    /// - `column`: the column, for its order
    /// - `soFar`: what the previous call returned, null on the first
    /// - `value`: the row's value
    public abstract Object fold(GridColumn column, Object soFar, Object value);

    /// What is shown for the folded result; by default the result itself.
    public Object finish(Object soFar) {
        return soFar;
    }

    private static final class Numeric extends GridAggregate {
        static final int SUM = 0;
        static final int AVERAGE = 1;
        static final int COUNT = 2;
        private final int op;

        Numeric(int op) {
            this.op = op;
        }

        @Override
        public Object fold(GridColumn column, Object soFar, Object value) {
            double[] acc = soFar instanceof double[] ? (double[]) soFar : new double[3];
            if (op == COUNT) {
                if (value != null) {
                    acc[1]++;
                }
            } else if (value instanceof Number) {
                acc[0] += ((Number) value).doubleValue();
                acc[1]++;
                if (!(value instanceof Double || value instanceof Float)) {
                    acc[2] = 1;
                }
            }
            return acc;
        }

        @Override
        public Object finish(Object soFar) {
            if (!(soFar instanceof double[])) {
                return op == COUNT ? Integer.valueOf(0) : null;
            }
            double[] acc = (double[]) soFar;
            if (op == COUNT) {
                return Integer.valueOf((int) acc[1]);
            }
            if (acc[1] == 0) {
                return null;
            }
            if (op == AVERAGE) {
                return Double.valueOf(acc[0] / acc[1]);
            }
            // A sum of whole numbers stays whole.
            if (acc[2] != 0 && Math.abs(acc[0]) < 9.0e15) {
                return Long.valueOf((long) acc[0]);
            }
            return Double.valueOf(acc[0]);
        }
    }

    private static final class Extreme extends GridAggregate {
        private final boolean max;

        Extreme(boolean max) {
            this.max = max;
        }

        @Override
        public Object fold(GridColumn column, Object soFar, Object value) {
            if (value == null) {
                return soFar;
            }
            if (soFar == null) {
                return value;
            }
            int c = column.compare(value, soFar);
            return (max ? c > 0 : c < 0) ? value : soFar;
        }
    }
}
