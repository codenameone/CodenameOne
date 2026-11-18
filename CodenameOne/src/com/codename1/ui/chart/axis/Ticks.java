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

/// "Nice number" tick generation: steps of 1, 2, 2.5 or 5 times a power of ten, so axis
/// labels read as round values instead of whatever the data range happened to divide into.
public final class Ticks {
    private Ticks() {
    }

    /// Returns a round step close to `rough`.
    ///
    /// #### Parameters
    ///
    /// - `rough`: the unrounded step, `range / desiredTickCount`
    ///
    /// #### Returns
    ///
    /// the nice step, always positive; 1 for a degenerate input
    public static double niceStep(double rough) {
        if (!(rough > 0) || Double.isInfinite(rough)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            return 1;
        }
        double exp = Math.floor(MathUtil.log10(rough));
        double base = MathUtil.pow(10, exp);
        // the tolerance keeps floating noise (0.20000000000000007) from rounding a step that is
        // already nice up to the next one
        double f = rough / base - 1e-9;
        double nf;
        if (f <= 1) {
            nf = 1;
        } else if (f <= 2) {
            nf = 2;
        } else if (f <= 2.5) {
            nf = 2.5;
        } else if (f <= 5) {
            nf = 5;
        } else {
            nf = 10;
        }
        return nf * base;
    }

    /// Generates ticks covering `[min, max]` with at most `maxCount` ticks.
    ///
    /// #### Parameters
    ///
    /// - `min`: the visible minimum
    ///
    /// - `max`: the visible maximum
    ///
    /// - `maxCount`: the largest number of ticks the space allows, at least 2
    ///
    /// #### Returns
    ///
    /// the tick values inside `[min, max]`, ascending
    public static double[] linear(double min, double max, int maxCount) {
        if (maxCount < 2) {
            maxCount = 2;
        }
        if (!(max > min)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            return new double[]{min};
        }
        if (Double.isInfinite(max - min)) {
            // a span past the double range has no round step; the ends are all that can be named
            return new double[]{min, max};
        }
        double step = niceStep((max - min) / (maxCount - 1));
        // a nice step can still yield maxCount + 1 ticks once aligned; widen until it fits
        int guard = 0;
        while (count(min, max, step) > maxCount && guard++ < 8) {
            step = niceStep(step * 1.5);
        }
        if (count(min, max, step) > maxCount) {
            return new double[]{min, max};
        }
        return fill(min, max, step);
    }

    /// The decimal places needed to print every multiple of `step` exactly.
    ///
    /// #### Parameters
    ///
    /// - `step`: the tick step
    ///
    /// #### Returns
    ///
    /// 0 for integral steps, otherwise the number of significant decimals (capped at 10)
    public static int decimalsFor(double step) {
        if (!(step > 0)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            return 0;
        }
        int d = 0;
        double s = step;
        while (d < 10 && Math.abs(s - Math.rint(s)) > 1e-9 * Math.max(1, Math.abs(s))) {
            s *= 10;
            d++;
        }
        return d;
    }

    static int count(double min, double max, double step) {
        double first = Math.ceil(min / step - 1e-9) * step;
        if (first > max) {
            return 0;
        }
        return (int) Math.floor((max - first) / step + 1e-9) + 1;
    }

    static double[] fill(double min, double max, double step) {
        int n = count(min, max, step);
        double first = Math.ceil(min / step - 1e-9) * step;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double v = first + i * step;
            // snap away floating noise such as 0.30000000000000004 and -0.0
            v = Math.rint(v / step) * step;
            if (v == 0) {
                v = 0;
            }
            out[i] = v;
        }
        return out;
    }
}
