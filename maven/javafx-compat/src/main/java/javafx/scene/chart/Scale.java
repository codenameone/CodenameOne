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
package javafx.scene.chart;

import java.util.ArrayList;
import java.util.List;

/// How one axis of a chart turns a value into a place along itself: a
/// band for each name of a category axis, or a range of numbers with a
/// tick every unit.
final class Scale {

    final Axis<?> axis;
    final boolean category;
    final List<String> names = new ArrayList<String>();
    double lower;
    double upper = 1;
    double unit = 1;

    private Scale(Axis<?> axis, boolean category) {
        this.axis = axis;
        this.category = category;
    }

    /// Builds the scale of an axis from the values the data has on it.
    ///
    /// @param zero whether a range taken from the data must hold zero
    ///     whatever the axis says, as the value axis of a bar chart does
    static Scale of(Axis<?> axis, List<Object> values, boolean zero) {
        if (axis instanceof ValueAxis) {
            Scale s = new Scale(axis, false);
            s.numbers((ValueAxis<?>) axis, values, zero);
            return s;
        }
        Scale s = new Scale(axis, true);
        if (axis instanceof CategoryAxis) {
            s.names.addAll(((CategoryAxis) axis).getCategories());
        }
        if (s.names.isEmpty()) {
            for (int i = 0; i < values.size(); i++) {
                String name = String.valueOf(values.get(i));
                if (!s.names.contains(name)) {
                    s.names.add(name);
                }
            }
        }
        return s;
    }

    private void numbers(ValueAxis<?> value, List<Object> values, boolean zero) {
        if (!value.isAutoRanging()) {
            lower = value.getLowerBound();
            upper = value.getUpperBound();
            unit = value.unit();
            double span = upper - lower;
            if (!(unit > 0) || span / unit > 200) {
                unit = span > 0 ? span / 10 : 1;
            }
        } else {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < values.size(); i++) {
                double v = number(values.get(i));
                if (v == v) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
            if (min > max) {
                min = 0;
                max = 100;
            }
            if (zero || value.zeroInRange()) {
                min = Math.min(min, 0);
                max = Math.max(max, 0);
            }
            if (max - min <= 0) {
                max = min + 1;
            }
            unit = niceUnit((max - min) / 10);
            lower = Math.floor(min / unit) * unit;
            upper = Math.ceil(max / unit) * unit;
            value.range(lower, upper);
        }
        if (value instanceof NumberAxis) {
            ((NumberAxis) value).cn1Unit = unit;
        }
    }

    /// The smallest of 1, 2, 2.5 and 5 times a power of ten that is at
    /// least the given step.
    static double niceUnit(double raw) {
        if (!(raw > 0) || Double.isInfinite(raw)) {
            return 1;
        }
        double power = 1;
        while (power > raw) {
            power /= 10;
        }
        while (power * 10 <= raw) {
            power *= 10;
        }
        double[] steps = {1, 2, 2.5, 5, 10};
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] * power >= raw * (1 - 1e-9)) {
                return steps[i] * power;
            }
        }
        return 10 * power;
    }

    static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
    }

    /// Where a value is along the axis, from 0 at its start to 1 at its
    /// end, or NaN when the axis has no place for it.
    double at(Object value) {
        if (category) {
            int index = names.indexOf(String.valueOf(value));
            return index < 0 ? Double.NaN : (index + 0.5) / names.size();
        }
        double span = upper - lower;
        return span > 0 ? (number(value) - lower) / span : Double.NaN;
    }

    /// Where zero is along a number axis, kept within the axis.
    double zero() {
        double span = upper - lower;
        if (category || !(span > 0)) {
            return 0;
        }
        return Math.max(0, Math.min(1, -lower / span));
    }

    /// The values the axis has a tick at: its names, or its numbers.
    List<Object> ticks() {
        List<Object> out = new ArrayList<Object>();
        if (category) {
            out.addAll(names);
            return out;
        }
        int count = (int) Math.floor((upper - lower) / unit + 1e-6);
        for (int i = 0; i <= count && i <= 400; i++) {
            out.add(Double.valueOf(lower + i * unit));
        }
        return out;
    }

    String text(Object tick) {
        return axis.text(tick);
    }
}
