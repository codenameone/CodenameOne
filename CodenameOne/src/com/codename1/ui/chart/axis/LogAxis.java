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
        if (min <= 0) {
            min = max > 1 ? 1 : max / 1000;
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
        double a = Math.floor(lg(getVisibleMin()) - 1e-9);
        double b = Math.ceil(lg(getVisibleMax()) + 1e-9);
        int decades = (int) (b - a);
        int every = Math.max(1, (int) Math.ceil(decades / (double) Math.max(1, maxCount - 1)));
        int n = 0;
        double[] tmp = new double[decades + 1];
        for (int e = (int) a; e <= (int) b; e += every) {
            double v = MathUtil.pow(10, e);
            if (v >= getVisibleMin() * 0.999999 && v <= getVisibleMax() * 1.000001) {
                tmp[n++] = v;
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
