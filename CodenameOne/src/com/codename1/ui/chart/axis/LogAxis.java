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
package com.codename1.ui.chart.axis;

import com.codename1.util.MathUtil;

/// A base 10 logarithmic axis for data spanning orders of magnitude. Values at or below
/// zero cannot be placed and are clamped to the bottom of the axis.
public class LogAxis extends Axis {
    /// Creates a logarithmic axis.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of `Axis.BOTTOM`, `Axis.LEFT`, `Axis.RIGHT` or `Axis.TOP`
    public LogAxis(int position) {
        super(position);
    }

    private static double lg(double v) {
        return MathUtil.log10(Math.max(v, 1e-300));
    }

    @Override
    protected double[] adjustBounds(double min, double max) {
        // a logarithm has no zero or negatives: a range given or found there falls back to
        // the positive decades the data could still show
        if (!(max > 0)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            max = 10;
        }
        if (!(min > 0) || min >= max) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            min = max > 1 ? Math.min(1, max / 10) : max / 1000;
        }
        double lo = MathUtil.pow(10, Math.floor(lg(min)));
        double hi = MathUtil.pow(10, Math.ceil(lg(max)));
        if (hi <= lo) {
            hi = lo * 10;
        }
        return new double[]{lo, hi};
    }

    @Override
    protected double forward(double value) {
        return lg(value);
    }

    @Override
    protected double inverse(double t) {
        return MathUtil.pow(10, t);
    }

    @Override
    public double[] ticks(int maxCount) {
        double vmin = getVisibleMin() > 0 ? getVisibleMin() : getDataMin();
        double vmax = getVisibleMax();
        double a = Math.floor(lg(vmin) - 1e-9);
        double b = Math.ceil(lg(vmax) + 1e-9);
        int decades = (int) (b - a);
        int every = Math.max(1, (int) Math.ceil(decades / (double) Math.max(1, maxCount - 1)));
        double[] out = decadeTicks(vmin, vmax, (int) a, (int) b, every, POWERS);
        if (out.length < 2) {
            // zoomed in to less than a decade or two: 1, 2 and 5 of each decade
            out = decadeTicks(vmin, vmax, (int) a, (int) b, 1, ONE_TWO_FIVE);
        }
        if (out.length < 2 || out.length > Math.max(2, maxCount)) {
            // under two decades the axis is close to linear, and round linear steps read
            // better than one lone power of ten (or more 1-2-5 ticks than fit)
            double[] lin = Ticks.linear(vmin, vmax, maxCount);
            if (lin.length >= 2) {
                out = lin;
            }
        }
        return out;
    }

    private static final double[] POWERS = {1};
    private static final double[] ONE_TWO_FIVE = {1, 2, 5};

    private static double[] decadeTicks(double vmin, double vmax, int a, int b, int every, double[] mantissas) {
        double[] tmp = new double[((b - a) / every + 1) * mantissas.length];
        int n = 0;
        for (int e = a; e <= b; e += every) {
            double p = MathUtil.pow(10, e);
            for (double m : mantissas) {
                double v = m * p;
                if (v >= vmin * 0.999999 && v <= vmax * 1.000001) {
                    tmp[n++] = v;
                }
            }
        }
        double[] out = new double[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    @Override
    protected String defaultFormat(double value, double step) {
        if (value >= 1e6) {
            return compact(value, value);
        }
        return super.defaultFormat(value, 0);
    }
}
