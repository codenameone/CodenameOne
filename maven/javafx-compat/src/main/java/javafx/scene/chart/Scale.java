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

import com.codename1.fxcompat.runtime.Fonts;

import javafx.geometry.Side;
import javafx.scene.text.Font;

/// One axis laid out along a length of pixels: the range or the bands it
/// settled on, where each value is along it, its ticks and their labels,
/// and how much room it needs across itself.
///
/// A place along the axis is measured from the left end of a horizontal
/// axis and from the top end of a vertical one, so that a greater number
/// is further right or further up.
final class Scale {

    /// The room kept between two tick labels when a tick unit is chosen.
    private static final double LABEL_ROOM = 6;

    final Axis<?> axis;
    final boolean category;
    final Side side;
    final boolean horizontal;
    final double length;
    final Font font;
    final List<String> names = new ArrayList<String>();
    final List<Object> ticks = new ArrayList<Object>();
    final List<String> labels = new ArrayList<String>();
    /// The places of the minor ticks along the axis.
    final List<Double> minors = new ArrayList<Double>();
    double lower;
    double upper = 1;
    double unit = 1;
    /// The place of the first category and the distance to the next.
    double first;
    double spacing;
    /// The rotation the tick labels are drawn with, in degrees.
    double rotation;
    /// The room the axis takes across itself: its tick marks, the gap,
    /// the tick labels and its own label.
    double thickness;

    private Scale(Axis<?> axis, Side side, double length) {
        this.axis = axis;
        this.category = !(axis instanceof ValueAxis);
        this.side = side;
        this.horizontal = side.isHorizontal();
        this.length = length;
        Font f = axis.getTickLabelFont();
        this.font = f == null ? Font.font(10) : f;
        this.rotation = axis.getTickLabelRotation();
    }

    /// Lays an axis out along a length from the values the data has on
    /// it.
    ///
    /// @param zero whether a range taken from the data must hold zero
    ///     whatever the axis says, as the value axis of a bar chart does
    static Scale of(Axis<?> axis, Side side, List<Object> values, boolean zero, double length) {
        Scale s = new Scale(axis, side, Math.max(0, length));
        if (axis instanceof ValueAxis) {
            s.numbers((ValueAxis<?>) axis, values, zero);
        } else {
            s.categories(values);
        }
        s.measure();
        return s;
    }

    /// The width and height of the box around a text turned by the
    /// rotation of the tick labels.
    double[] box(String text) {
        double w = Fonts.width(font, text);
        double h = Fonts.lineHeight(font);
        if (rotation == 0) {
            return new double[] {w, h};
        }
        double r = Math.toRadians(rotation);
        double cos = Math.abs(Math.cos(r));
        double sin = Math.abs(Math.sin(r));
        return new double[] {w * cos + h * sin, w * sin + h * cos};
    }

    private void categories(List<Object> values) {
        if (axis instanceof CategoryAxis) {
            names.addAll(((CategoryAxis) axis).getCategories());
        }
        if (names.isEmpty()) {
            for (int i = 0; i < values.size(); i++) {
                String name = String.valueOf(values.get(i));
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
        }
        double start = 0;
        double end = 0;
        boolean gap = true;
        if (axis instanceof CategoryAxis) {
            CategoryAxis c = (CategoryAxis) axis;
            start = c.getStartMargin();
            end = c.getEndMargin();
            gap = c.isGapStartAndEnd();
        }
        int count = names.size();
        int bands = gap ? count : count - 1;
        spacing = bands > 0 ? (length - start - end) / bands : 0;
        first = start + (gap ? spacing / 2 : 0);
        for (int i = 0; i < count; i++) {
            ticks.add(names.get(i));
            labels.add(axis.text(names.get(i)));
        }
        // Labels that do not fit side by side along a horizontal axis
        // are stood on end, and those of a vertical one laid flat.
        double widest = 0;
        double last = 0;
        for (int i = 0; i < count; i++) {
            double[] b = box(labels.get(i));
            double size = horizontal && rotation == 0 ? b[0] : b[1];
            if (i == 0) {
                last = size / 2;
            } else {
                widest = Math.max(widest, last + LABEL_ROOM + size / 2);
            }
        }
        if (start + widest * count + end > length) {
            if (horizontal && rotation != 90) {
                rotation = 90;
            } else if (!horizontal && rotation != 0) {
                rotation = 0;
            }
        }
    }

    private void numbers(ValueAxis<?> value, List<Object> values, boolean zero) {
        if (!value.isAutoRanging()) {
            lower = value.getLowerBound();
            upper = value.getUpperBound();
            unit = value.unit();
        } else {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < values.size(); i++) {
                double v = number(values.get(i));
                if (v == v && !Double.isInfinite(v)) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
            if (min > max) {
                // No data: the range the axis has is rounded.
                min = Math.min(value.getLowerBound(), value.getUpperBound());
                max = Math.max(value.getLowerBound(), value.getUpperBound());
            }
            if (zero || value.zeroInRange()) {
                if (max < 0) {
                    max = 0;
                } else if (min > 0) {
                    min = 0;
                }
            }
            autoRange(value, min, max);
            value.range(lower, upper, unit);
        }
        numberTicks(value);
    }

    /// Ten to a power, exact for the small powers a tick unit has.
    static double pow10(int exponent) {
        double p = 1;
        for (int i = exponent < 0 ? -exponent : exponent; i > 0; i--) {
            p *= 10;
        }
        return exponent < 0 ? 1 / p : p;
    }

    /// Chooses the bounds and the tick unit for a range of data.
    private void autoRange(ValueAxis<?> value, double min, double max) {
        int fit = Math.max(2, (int) Math.floor(length / (font.getSize() * 2)));
        int minor = Math.max(1, value.getMinorTickCount());
        double range = max - min;
        // A range too small to tell its minor ticks apart is no range.
        if (range != 0 && range / ((double) fit * minor) <= Math.abs(min) * 2.3e-16) {
            range = 0;
        }
        double padded = range == 0 ? (min == 0 ? 2 : Math.abs(min) * 0.02) : Math.abs(range) * 1.02;
        double pad = (padded - range) / 2;
        double from = min - pad;
        double to = max + pad;
        // The padding never carries a bound across zero.
        if ((from < 0 && min >= 0) || (from > 0 && min <= 0)) {
            from = 0;
        }
        if ((to < 0 && max >= 0) || (to > 0 && max <= 0)) {
            to = 0;
        }
        double step = padded / fit;
        if (!(step > 0) || Double.isInfinite(step)) {
            lower = from;
            upper = from + 1;
            unit = 1;
            value.format(0, false);
            return;
        }
        for (int round = 0; round < 64; round++) {
            int exponent = 0;
            while (exponent > -300 && pow10(exponent) > step) {
                exponent--;
            }
            while (exponent < 300 && pow10(exponent + 1) <= step) {
                exponent++;
            }
            double mantissa = step / pow10(exponent);
            double ratio = mantissa;
            if (mantissa > 5) {
                exponent++;
                ratio = 1;
            } else if (mantissa > 1) {
                ratio = mantissa > 2.5 ? 5 : 2.5;
            }
            if (exponent > 1) {
                value.format(0, true);
            } else if (exponent == 1) {
                value.format(0, false);
            } else {
                boolean fraction = !whole(ratio);
                value.format(-exponent + (fraction ? 1 : 0), false);
            }
            unit = exponent < 0 ? ratio / pow10(-exponent) : ratio * pow10(exponent);
            lower = Math.floor(from / unit) * unit;
            upper = Math.ceil(to / unit) * unit;
            int count = (int) Math.ceil((upper - lower) / unit);
            // The room the labels of these ticks need along the axis.
            double widest = 0;
            double last = 0;
            for (int i = 0; i < count && i <= 400; i++) {
                double[] b = box(axis.text(Double.valueOf(lower + i * unit)));
                double size = horizontal ? b[0] : b[1];
                if (i == 0) {
                    last = size / 2;
                } else {
                    widest = Math.max(widest, last + LABEL_ROOM + size / 2);
                }
            }
            double needed = (count - 1) * widest;
            step = unit;
            if (fit == 2 && needed > length) {
                break;
            }
            if (needed > length || count > 20) {
                step *= 2;
            } else {
                break;
            }
        }
        if (!(upper > lower)) {
            upper = lower + unit;
        }
    }

    /// Whether a number has no fraction.
    private static boolean whole(double value) {
        return value - Math.floor(value) <= 0;
    }

    private void numberTicks(ValueAxis<?> value) {
        double span = upper - lower;
        if (!(span > 0)) {
            ticks.add(Double.valueOf(lower));
        } else {
            ticks.add(Double.valueOf(lower));
            if (unit > 0 && lower + unit < upper && span / unit <= 2000) {
                // A whole tick unit puts its ticks on whole numbers.
                boolean whole = whole(unit);
                double start = whole ? Math.ceil(lower) : lower + unit;
                int count = (int) Math.ceil((upper - start) / unit);
                for (int i = 0; i < count; i++) {
                    double major = start + i * unit;
                    if (major >= upper) {
                        break;
                    }
                    if (major > lower) {
                        ticks.add(Double.valueOf(major));
                    }
                }
            }
            ticks.add(Double.valueOf(upper));
        }
        for (int i = 0; i < ticks.size(); i++) {
            labels.add(axis.text(ticks.get(i)));
        }
        int parts = Math.max(1, value.getMinorTickCount());
        if (!value.isMinorTickVisible() || !(value.getMinorTickLength() > 0) || parts < 2 || !(span > 0)
                || !(unit > 0)) {
            return;
        }
        // Minor ticks are left out where they would be a pixel apart.
        int wanted = (ticks.size() - 1) * (parts - 1);
        if (length <= (ticks.size() + wanted) * 2.0) {
            return;
        }
        double step = unit / parts;
        if (span / step > 10000) {
            return;
        }
        boolean whole = whole(unit);
        double start = whole ? Math.ceil(lower) : lower;
        if (whole) {
            // The part of a tick unit before the first whole number.
            for (int i = 1; i < parts; i++) {
                double v = start - unit + i * step;
                if (v > lower && v < start) {
                    minors.add(Double.valueOf(along(v)));
                }
            }
        }
        int count = (int) Math.ceil((upper - start) / unit);
        for (int i = 0; i < count; i++) {
            double major = start + i * unit;
            double next = Math.min(major + unit, upper);
            for (int j = 1; j < parts; j++) {
                double v = major + j * step;
                if (v >= next) {
                    break;
                }
                minors.add(Double.valueOf(along(v)));
            }
        }
    }

    /// Works out the room the axis takes across itself.
    private void measure() {
        double across = 0;
        if (axis.isTickLabelsVisible()) {
            for (int i = 0; i < labels.size(); i++) {
                double[] b = box(labels.get(i));
                across = Math.max(across, horizontal ? b[1] : b[0]);
            }
        }
        thickness = across + axis.getTickLabelGap() + tickLength() + labelHeight();
    }

    /// The length of a tick mark, zero when none are drawn.
    double tickLength() {
        return axis.isTickMarkVisible() && axis.getTickLength() > 0 ? axis.getTickLength() : 0;
    }

    /// The room the label of the axis takes across it, zero when it has
    /// none: a line of text and four pixels between it and the ticks.
    double labelHeight() {
        String label = axis.getLabel();
        return label == null || label.length() == 0 ? 0 : Fonts.lineHeight(Chart.font(12)) + 4;
    }

    static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
    }

    private double along(double number) {
        double span = upper - lower;
        if (!(span > 0)) {
            return Double.NaN;
        }
        double along = (number - lower) * length / span;
        return horizontal ? along : length - along;
    }

    /// Where a value is along the axis, or NaN when the axis has no
    /// place for it.
    double at(Object value) {
        if (category) {
            int index = names.indexOf(String.valueOf(value));
            if (index < 0) {
                return Double.NaN;
            }
            double along = first + index * spacing;
            return horizontal ? along : length - along;
        }
        return along(number(value));
    }

    /// Where zero is along a number axis, NaN when zero is not in its
    /// range.
    double zero() {
        if (category || lower > 0 || upper < 0) {
            return Double.NaN;
        }
        return along(0);
    }

    /// Where a bar starts along a number axis: at zero, or at the end of
    /// the axis nearest to zero when zero is not in its range.
    double base() {
        if (category) {
            return 0;
        }
        return along(lower > 0 ? lower : upper < 0 ? upper : 0);
    }
}
